package dasniko.keycloak.friendlycaptcha;

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
public class FriendlyCaptchaUsernamePasswordAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "friendly-captcha";

	private static final Authenticator SINGLETON = new FriendlyCaptchaUsernamePasswordAuthenticator();

	@Override
	public String getDisplayType() {
		return null; // TODO implement me
	}

	@Override
	public String getReferenceCategory() {
		return "captcha";
	}

	@Override
	public boolean isConfigurable() {
		return false; // TODO implement me
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
		return null; // TODO implement me
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return null; // TODO implement me
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
