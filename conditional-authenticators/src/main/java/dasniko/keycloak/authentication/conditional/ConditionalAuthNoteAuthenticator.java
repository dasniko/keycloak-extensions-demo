package dasniko.keycloak.authentication.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static de.keycloak.util.AuthenticatorUtil.getConfig;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class ConditionalAuthNoteAuthenticator implements ConditionalAuthenticator {

	static final ConditionalAuthNoteAuthenticator SINGLETON = new ConditionalAuthNoteAuthenticator();

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String noteName = getConfig(context, ConditionalAuthNoteAuthenticatorFactory.CONF_AUTH_NOTE_NAME, "");
		String noteValue = getConfig(context, ConditionalAuthNoteAuthenticatorFactory.CONF_AUTH_NOTE_VALUE, "");
		boolean negateOutput = getConfig(context, ConditionalAuthNoteAuthenticatorFactory.CONF_NOT, Boolean.FALSE);

		String authNote = context.getAuthenticationSession().getAuthNote(noteName);

		return negateOutput != noteValue.equals(authNote);
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
