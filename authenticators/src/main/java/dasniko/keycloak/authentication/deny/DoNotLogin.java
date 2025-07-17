package dasniko.keycloak.authentication.deny;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@AutoService(AuthenticatorFactory.class)
public class DoNotLogin implements AuthenticatorFactory, Authenticator {

	public static final String PROVIDER_ID = "do-not-login";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		UserModel user = context.getUser();
		if (user != null) {
			user.setEnabled(false);
			context.clearUser();
		}
		Response challenge = context.form().setInfo("infoMessage").createInfoPage();
		context.failure(AuthenticationFlowError.USER_DISABLED, challenge);
	}

	@Override
	public void action(AuthenticationFlowContext authenticationFlowContext) {
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
		return false;
	}

	@Override
	public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
	}

	@Override
	public String getDisplayType() {
		return "Do not login";
	}

	@Override
	public String getReferenceCategory() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
		AuthenticationExecutionModel.Requirement.REQUIRED,
		AuthenticationExecutionModel.Requirement.DISABLED
	};

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
		return "Access will be always denied. Not with an error page, but with an info page. Useful e.g. in case of registration and user should not be automatically authenticated.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of();
	}

	@Override
	public Authenticator create(KeycloakSession keycloakSession) {
		return this;
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
