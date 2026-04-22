package dasniko.keycloak.broker.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;

@Slf4j
public class CustomIdentityProvider extends OIDCIdentityProvider {

	public CustomIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
		super(session, config);
	}

	@Override
	public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
		if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().isEmpty()) return null;
		String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
		if (getConfig().isBackchannelSupported()) {
			backchannelLogout(userSession, idToken);
			return null;
		} else {
			String sessionId = userSession.getId();
			UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl())
				.queryParam("state", sessionId);
			if (getConfig().isSendIdTokenOnLogout() && idToken != null) {
				logoutUri.queryParam("id_token_hint", idToken);
			}
			// start custom code
			String loginHint = extractLoginHintFromIdToken(idToken);
			if (getConfig().isSendLogoutHintOnLogout() && StringUtil.isNotBlank(loginHint)) {
				logoutUri.queryParam("logout_hint", loginHint);
			}
			// end custom code
			if (getConfig().isSendClientIdOnLogout()) {
				logoutUri.queryParam("client_id", getConfig().getClientId());
			}
			String redirect = RealmsResource.brokerUrl(uriInfo)
				.path(IdentityBrokerService.class, "getEndpoint")
				.path(OIDCEndpoint.class, "logoutResponse")
				.build(realm.getName(), getConfig().getAlias()).toString();
			logoutUri.queryParam("post_logout_redirect_uri", redirect);
			return Response.status(302).location(logoutUri.build()).build();
		}
	}

	@Override
	public CustomOIDCIdentityProviderConfig getConfig() {
		return (CustomOIDCIdentityProviderConfig) super.getConfig();
	}

	protected String extractLoginHintFromIdToken(String idToken) {
		if (StringUtil.isBlank(idToken)) return null;
		try {
			JsonNode jsonNode = JsonSerialization.readValue(parseTokenInput(idToken, false), JsonNode.class);
			return jsonNode.path("login_hint").asText();
		} catch (IOException | IdentityBrokerException e) {
			logger.warn("Failed to extract loginHint from id_token.", e);
			return null;
		}
	}

}
