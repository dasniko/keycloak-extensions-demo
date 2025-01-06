package dasniko.keycloak.authentication.conditional;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

public abstract class AbstractConditionalAuthenticator implements ConditionalAuthenticatorFactory, ConditionalAuthenticator {

	static final String CONF_NOT = "not";

	static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED};

	ProviderConfigProperty negateOutputConfProperty = new ProviderConfigProperty(CONF_NOT, "Negate output", "Apply a NOT to the check result", ProviderConfigProperty.BOOLEAN_TYPE, false);

	@Override
	public ConditionalAuthenticator getSingleton() {
		return this;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
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
	public void action(AuthenticationFlowContext authenticationFlowContext) {
	}

	@Override
	public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
	}

}
