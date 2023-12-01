package dasniko.keycloak.authentication.mfa;

import dasniko.keycloak.sms.logging.LoggingSmsProviderFactory;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MfaAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "custom-mfa";

	private static final Authenticator SINGLETON = new MfaAuthenticator();

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "Multi-Factor-Authentication";
	}

	@Override
	public String getHelpText() {
		return "Validates an OTP printed to StdOut. This is only a very simplified example!!!";
	}

	@Override
	public String getReferenceCategory() {
		return "otp";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty length = new ProviderConfigProperty();
		length.setName(MfaConstants.CONFIG_PROPERTY_LENGTH);
		length.setLabel("Code length");
		length.setHelpText("The number of digits of the generated code.");
		length.setType(ProviderConfigProperty.STRING_TYPE);
		length.setDefaultValue("6");

		ProviderConfigProperty ttl = new ProviderConfigProperty();
		ttl.setName(MfaConstants.CONFIG_PROPERTY_TTL);
		ttl.setLabel("Time-to-live");
		ttl.setHelpText("The time to live in seconds for the code to be valid.");
		ttl.setType(ProviderConfigProperty.STRING_TYPE);
		ttl.setDefaultValue("300");

		ProviderConfigProperty provider = new ProviderConfigProperty();
		provider.setName(MfaConstants.CONFIG_PROPERTY_PROVIDER);
		provider.setLabel("Provider ID");
		provider.setHelpText("ID of the SMS provider implementation to use.");
		provider.setType(ProviderConfigProperty.STRING_TYPE);
		provider.setDefaultValue(LoggingSmsProviderFactory.PROVIDER_ID);

		return List.of(length, ttl);
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
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

}
