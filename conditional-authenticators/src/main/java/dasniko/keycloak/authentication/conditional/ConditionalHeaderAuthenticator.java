package dasniko.keycloak.authentication.conditional;

import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static de.keycloak.util.AuthenticatorUtil.getConfig;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class ConditionalHeaderAuthenticator implements ConditionalAuthenticator {

	static final ConditionalHeaderAuthenticator SINGLETON = new ConditionalHeaderAuthenticator();

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String headerName = getConfig(context, ConditionalHeaderAuthenticatorFactory.CONF_HEADER_NAME, "");
		String headerValue = getConfig(context, ConditionalHeaderAuthenticatorFactory.CONF_HEADER_EXPECTED_VALUE, "");
		boolean negateOutput = getConfig(context, ConditionalHeaderAuthenticatorFactory.CONF_NOT, Boolean.FALSE);

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
