package dasniko.keycloak.authentication.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.HttpHeaders;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class CustomConditionalAuthenticator implements ConditionalAuthenticator {

	static final CustomConditionalAuthenticator SINGLETON = new CustomConditionalAuthenticator();

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		Map<String, String> config = context.getAuthenticatorConfig().getConfig();
		String headerName = config.get(CustomConditionalAuthenticatorFactory.CONF_HEADER_NAME);
		String headerValue = config.get(CustomConditionalAuthenticatorFactory.CONF_HEADER_EXPECTED_VALUE);
		boolean negateOutput = Boolean.parseBoolean(config.get(CustomConditionalAuthenticatorFactory.CONF_NOT));

		HttpHeaders httpHeaders = context.getHttpRequest().getHttpHeaders();
		String customHeader = httpHeaders.getHeaderString(headerName);

		return negateOutput != headerValue.equalsIgnoreCase(customHeader);
	}

	@Override
	public void action(AuthenticationFlowContext authenticationFlowContext) {
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
	}

	@Override
	public void close() {
	}
}
