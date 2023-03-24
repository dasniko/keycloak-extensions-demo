package dasniko.keycloak.authentication.conditional;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class CustomConditionalAuthenticatorFactory implements ConditionalAuthenticatorFactory {

	public static final String PROVIDER_ID = "conditional-custom-header";

	public static final String CONF_HEADER_NAME = "header_name";
	public static final String CONF_HEADER_EXPECTED_VALUE = "header_expected_value";
	public static final String CONF_NOT = "not";

	@Override
	public ConditionalAuthenticator getSingleton() {
		return CustomConditionalAuthenticator.SINGLETON;
	}

	@Override
	public String getDisplayType() {
		return "Condition - Custom Header";
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
		return "Flow is executed only if...";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty authNoteName = new ProviderConfigProperty();
		authNoteName.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteName.setName(CONF_HEADER_NAME);
		authNoteName.setLabel("Header name");
		authNoteName.setHelpText("Name of the header to check");
		authNoteName.setDefaultValue("X-Custom-Header");

		ProviderConfigProperty authNoteExpectedValue = new ProviderConfigProperty();
		authNoteExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteExpectedValue.setName(CONF_HEADER_EXPECTED_VALUE);
		authNoteExpectedValue.setLabel("Expected header value");
		authNoteExpectedValue.setHelpText("Expected value in the header");
		authNoteExpectedValue.setDefaultValue("my-custom-value");

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
