package dasniko.keycloak.authentication.conditional;

import com.google.auto.service.AutoService;
import de.keycloak.util.AuthenticatorUtil;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(AuthenticatorFactory.class)
public class ConditionalAuthNoteAuthenticator extends AbstractConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-auth-note";

	public static final String CONF_AUTH_NOTE_NAME = "auth_note_name";
	public static final String CONF_AUTH_NOTE_VALUE = "auth_note_value";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String noteName = AuthenticatorUtil.getConfig(context, CONF_AUTH_NOTE_NAME, "");
		String noteValue = AuthenticatorUtil.getConfig(context, CONF_AUTH_NOTE_VALUE, "");
		String authNote = context.getAuthenticationSession().getAuthNote(noteName);

		return isNegateOutput(context) != noteValue.equals(authNote);
	}

	@Override
	public String getDisplayType() {
		return "Condition - Authentication Session Note";
	}

	@Override
	public String getHelpText() {
		return "Flow is executed only if there is an auth session note with the expected value.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty authNoteName = new ProviderConfigProperty();
		authNoteName.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteName.setName(CONF_AUTH_NOTE_NAME);
		authNoteName.setLabel("AuthNote name");
		authNoteName.setHelpText("Name of the AuthNote to check");

		ProviderConfigProperty authNoteExpectedValue = new ProviderConfigProperty();
		authNoteExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteExpectedValue.setName(CONF_AUTH_NOTE_VALUE);
		authNoteExpectedValue.setLabel("AuthNote value");
		authNoteExpectedValue.setHelpText("Expected value in the AuthNote");

		return List.of(authNoteName, authNoteExpectedValue, negateOutputConfProperty);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
