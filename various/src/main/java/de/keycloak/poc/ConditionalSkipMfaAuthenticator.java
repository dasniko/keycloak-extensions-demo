package de.keycloak.poc;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@AutoService(AuthenticatorFactory.class)
public class ConditionalSkipMfaAuthenticator implements ConditionalAuthenticatorFactory, ConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-skip-mfa";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String skipMfa = context.getSession().getContext().getClient().getAttribute("skipMfa");
		return skipMfa == null || !skipMfa.equals("true");
	}

	@Override
	public void action(AuthenticationFlowContext authenticationFlowContext) {
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
	}

	@Override
	public ConditionalAuthenticator getSingleton() {
		return this;
	}

	@Override
	public String getDisplayType() {
		return "Condition - Do NOT skip MFA";
	}

	@Override
	public boolean isConfigurable() {
		return false;
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
		return "...some help text...";
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

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

}
