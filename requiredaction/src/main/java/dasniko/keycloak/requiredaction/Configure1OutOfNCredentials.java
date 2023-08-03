package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;

@AutoService(RequiredActionFactory.class)
public class Configure1OutOfNCredentials implements RequiredActionFactory, RequiredActionProvider {

	public static final String PROVIDER_ID = "configure1OutOfNCreds";

	// map of key-value pairs: key = credential type, value = associated required action id
	// { "otp": "CONFIGURE_TOTP", "webauthn": "webauthn-register" }
	private static final Map<String, String> credentialTypes = Map.of(
		OTPCredentialModel.TYPE, UserModel.RequiredAction.CONFIGURE_TOTP.name(),
		WebAuthnCredentialModel.TYPE_TWOFACTOR, WebAuthnRegisterFactory.PROVIDER_ID
	);

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// self registering if user doesn't have already one out of the configured credential types
		UserModel user = context.getUser();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		if (credentialTypes.keySet().stream().noneMatch(type -> user.credentialManager().isConfiguredFor(type))
			&& user.getRequiredActionsStream().noneMatch(credentialTypes::containsValue)
			&& authSession.getRequiredActions().stream().noneMatch(credentialTypes::containsValue)) {
				authSession.addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// initial form
		LoginFormsProvider form = context.form();
		form.setAttribute("realm", context.getRealm());
		form.setAttribute("user", context.getUser());
		form.setAttribute("credentialOptions", credentialTypes);
		context.challenge(form.createForm("config-1-out-of-n-creds.ftl"));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String requiredActionName = formData.getFirst("requiredActionName");

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.addRequiredAction(requiredActionName);

		authSession.removeRequiredAction(PROVIDER_ID);
		context.success();
	}

	@Override
	public String getDisplayText() {
		return "Configure 1 out of N Credentials";
	}

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return this;
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
