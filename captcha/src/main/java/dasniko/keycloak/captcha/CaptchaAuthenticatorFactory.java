package dasniko.keycloak.captcha;

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
public class CaptchaAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "captcha";

	private String lowerBound;
	private String upperBound;

	private final Authenticator SINGLETON = new CaptchaAuthenticator();

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public String getDisplayType() {
		return "Captcha Authenticator";
	}

	@Override
	public String getReferenceCategory() {
		return "captcha";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public String getHelpText() {
		return "";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of(
			new ProviderConfigProperty("lowerBound", "Lower Bound", "", ProviderConfigProperty.STRING_TYPE, this.lowerBound),
			new ProviderConfigProperty("upperBound", "Upper Bound", "", ProviderConfigProperty.STRING_TYPE, this.upperBound)
		);
	}

	@Override
	public void init(Config.Scope config) {
		this.lowerBound = config.get("lowerBound", "0");
		this.upperBound = config.get("upperBound", "20");
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
