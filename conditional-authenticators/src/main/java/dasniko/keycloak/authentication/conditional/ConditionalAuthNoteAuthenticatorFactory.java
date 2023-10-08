package dasniko.keycloak.authentication.conditional;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(AuthenticatorFactory.class)
public class ConditionalAuthNoteAuthenticatorFactory implements ConditionalAuthenticatorFactory {

	public static final String PROVIDER_ID = "conditional-auth-note";

	public static final String CONF_AUTH_NOTE_NAME = "auth_note_name";
	public static final String CONF_AUTH_NOTE_VALUE = "auth_note_value";
	public static final String CONF_NOT = "not";

	@Override
	public ConditionalAuthenticator getSingleton() {
		return ConditionalAuthNoteAuthenticator.SINGLETON;
	}

	@Override
	public String getDisplayType() {
		return "Condition - Authentication Session Note";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return new AuthenticationExecutionModel.Requirement[] {
			AuthenticationExecutionModel.Requirement.REQUIRED,
			AuthenticationExecutionModel.Requirement.DISABLED
		};
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
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

		ProviderConfigProperty negateOutput = new ProviderConfigProperty();
		negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
		negateOutput.setName(CONF_NOT);
		negateOutput.setLabel("Negate output");
		negateOutput.setHelpText("Apply a NOT to the check result");

		return List.of(authNoteName, authNoteExpectedValue, negateOutput);
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
