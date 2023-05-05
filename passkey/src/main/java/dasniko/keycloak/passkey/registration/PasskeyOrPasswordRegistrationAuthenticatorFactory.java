package dasniko.keycloak.passkey.registration;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@AutoService(AuthenticatorFactory.class)
public class PasskeyOrPasswordRegistrationAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "passkey-or-password-registration";

	private static final PasskeyOrPasswordRegistrationAuthenticator SINGLETON = new PasskeyOrPasswordRegistrationAuthenticator();

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "Passkey or Password Registration";
	}

	@Override
	public String getReferenceCategory() {
		return "password";
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public String getHelpText() {
		return "Displays two buttons labeled with: 'setup with passkey' and 'setup with password'. Should not be used in combination with built-in authenticators/forms. ";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return null;
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
