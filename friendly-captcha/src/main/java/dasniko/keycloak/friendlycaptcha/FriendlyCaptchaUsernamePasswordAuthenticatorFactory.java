package dasniko.keycloak.friendlycaptcha;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;


@AutoService(AuthenticatorFactory.class)
public class FriendlyCaptchaUsernamePasswordAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "friendly-captcha-login";

	static final String SITE_KEY = "fc.site.key";
	static final String API_KEY = "fc.api.key";

	private static final Authenticator SINGLETON = new FriendlyCaptchaUsernamePasswordAuthenticator();
	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

	static {
		ProviderConfigProperty property;
		property = new ProviderConfigProperty();
		property.setName(SITE_KEY);
		property.setLabel("Friendly Captcha Site Key");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setHelpText("Friendly Captcha Site Key");
		CONFIG_PROPERTIES.add(property);
		property = new ProviderConfigProperty();
		property.setName(API_KEY);
		property.setLabel("Friendly Captcha API Key");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setHelpText("Friendly Captcha API Key");
		CONFIG_PROPERTIES.add(property);
	}

	@Override
	public String getDisplayType() {
		return "Friendly Captcha Username Password Form";
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
		return new AuthenticationExecutionModel.Requirement[0];
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public String getHelpText() {
		return "Adds Friendly Captcha to Username Password Form. Captchas verify that the entity that is authenticating is a human. This can only be used on the internet and must be configured after you add it.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIG_PROPERTIES;
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

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
