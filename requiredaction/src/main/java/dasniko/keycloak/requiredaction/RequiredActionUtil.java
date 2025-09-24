package dasniko.keycloak.requiredaction;

import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequiredActionUtil {

	static final String CFG_REQUIRED_ACTIONS = "requiredActions";

	static Map<String, String> getCredentialTypes(RequiredActionContext context) {
		KeycloakSession session = context.getSession();
		String cacheKey = context.getConfig().getProviderId() + "_credentialTypes";
		//noinspection unchecked
		Map<String, String> cachedCredentialTypes = session.getAttribute(cacheKey, Map.class);
		if (cachedCredentialTypes != null) {
			return cachedCredentialTypes;
		}

		RequiredActionConfigModel config = context.getConfig();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();

		String requiredActionsString = config.getConfigValue(CFG_REQUIRED_ACTIONS);
		List<String> requiredActions = Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(requiredActionsString));

		Map<String, String> credentialTypes = new LinkedHashMap<>();
		session.getKeycloakSessionFactory()
			.getProviderFactoriesStream(RequiredActionProvider.class)
			.forEach(providerFactory -> {
				String providerId = providerFactory.getId();
				if (requiredActions.contains(providerId)) {
					RequiredActionProvider action = (RequiredActionProvider) providerFactory.create(session);
					if (action instanceof CredentialRegistrator) {
						String credentialType = ((CredentialRegistrator) action).getCredentialType(session, authSession);
						credentialTypes.put(credentialType, providerId);
					}
				}
			});
		session.setAttribute(cacheKey, credentialTypes);
		return credentialTypes;
	}

}
