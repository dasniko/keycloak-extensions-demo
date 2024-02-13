package dasniko.keycloak.actiontoken;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

@Slf4j
@RequiredArgsConstructor
public class CustomTokenResource {

	private final KeycloakSession session;
	private final RealmModel realm;
	private final AdminPermissionEvaluator auth;

	@POST
	@Path("")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public CustomTokenResponse createTokenLink(CustomTokenRequest payload) {
		// do the authorization with the existing admin permissions, here we need the role 'manage-users'
		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireManage();

		String clientId = payload.getClientId();
		ClientModel client = session.clients().getClientByClientId(realm, clientId);
		if (client == null) {
			throw new NotFoundException("clientId %s not found.".formatted(clientId));
		}

		String redirectUri = payload.getRedirectUri();
		if (!validateRedirectUri(client, redirectUri)) {
			throw new BadRequestException("redirectUri %s disallowed by client.".formatted(redirectUri));
		}

		String emailOrUsername = payload.getEmail();
		boolean forceCreate = payload.isForceCreate();
		if (payload.getUsername() != null) {
			emailOrUsername = payload.getUsername();
			forceCreate = false;
		}

		UserModel user = getOrCreateUser(emailOrUsername, forceCreate, payload.isUpdateProfile(), payload.isUpdatePassword());
		if (user == null) {
			throw new NotFoundException("User with email/username %s not found, and forceCreate is off.".formatted(emailOrUsername));
		}

		CustomActionToken token = createActionToken(
			user, clientId, payload.getExpirationSeconds(), redirectUri, payload.isReuse(),
			payload.getScope(), payload.getNonce(), payload.getState());

		String tokenUrl = getActionTokenUrl(token);

		CustomTokenResponse response = new CustomTokenResponse();
		response.setUserId(user.getId());
		response.setLink(tokenUrl);
		return response;
	}

	private boolean validateRedirectUri(ClientModel client, String redirectUri) {
		String redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
		return redirectUri.equals(redirect);
	}

	private UserModel getOrCreateUser(String email, boolean forceCreate, boolean updateProfile, boolean updatePassword) {
		UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, email);
		if (user == null && forceCreate) {
			user = session.users().addUser(realm, email);
			user.setEnabled(true);
			user.setEmail(email);
			registerEvent(user);
			if (updatePassword) {
				user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
			}
			if (updateProfile) {
				user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
			}
		}
		return user;
	}

	private void registerEvent(UserModel user) {
		ClientConnection clientConnection = session.getContext().getConnection();
		EventBuilder eventBuilder = new EventBuilder(realm, session, clientConnection).realm(realm);
		eventBuilder
			.event(EventType.REGISTER)
			.detail(Details.REGISTER_METHOD, CustomActionToken.TOKEN_TYPE)
			.detail(Details.USERNAME, user.getUsername())
			.detail(Details.EMAIL, user.getEmail())
			.user(user)
			.success();
	}

	private CustomActionToken createActionToken(UserModel user, String clientId, Integer expiration, String redirectUri,
																							boolean reuse, String scope, String nonce, String state) {
		int expirationInSecs = (expiration != null && expiration > 0) ? expiration : (60 * 60 * 24);
		int absoluteExpirationInSecs = Time.currentTime() + expirationInSecs;
		return new CustomActionToken(user.getId(), clientId, absoluteExpirationInSecs, reuse, redirectUri, scope, nonce, state);
	}

	private String getActionTokenUrl(CustomActionToken token) {
		KeycloakContext context = session.getContext();
		UriInfo uriInfo = context.getUri();

		// creating this kind of token for the admin (master) realm is of high risk, thus we don't allow this
		String adminRealm = Config.getAdminRealm();
		if (adminRealm.equals(realm.getName())) {
			throw new IllegalStateException("This token type is not allowed for realm %s".formatted(adminRealm));
		}

		// If you are using a different realm to call this method than the one you want to create the action token,
		// we need to temporarily set the session context realm to the latter one, because the SignatureProvider
		// uses the keys from the current sessionContextRealm.
		RealmModel sessionContextRealm = session.getContext().getRealm();
		session.getContext().setRealm(realm);

		// now do the work
		String tokenString = token.serialize(session, realm, uriInfo);
		UriBuilder uriBuilder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), tokenString, token.issuedFor, "");

		// and then reset the realm to the proper one
		session.getContext().setRealm(sessionContextRealm);

		return uriBuilder.build(realm.getName()).toString();
	}

}
