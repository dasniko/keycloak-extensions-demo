package de.keycloak.poc;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.AmrUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

@JBossLog
@AutoService(AuthenticatorFactory.class)
public class ConditionalMfaRequiredAuthenticator implements ConditionalAuthenticatorFactory, ConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-mfa-required";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		boolean hasOtpAlreadyFulfilled = getAmr(context).contains("otp");
		String skipMfa = context.getSession().getContext().getClient().getAttribute("skipMfa");
		return !hasOtpAlreadyFulfilled && !(skipMfa != null && skipMfa.equals("true"));
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
		return "Condition - MFA required on Cookie auth";
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

	protected List<String> getAmr(AuthenticationFlowContext context) {
		Map<String, String> userSessionNotes = context.getAuthenticationSession().getUserSessionNotes();
		Map<String, Integer> executions = AuthenticatorUtils.parseCompletedExecutions(userSessionNotes.get(Constants.AUTHENTICATORS_COMPLETED));
		log.debugf("found the following completed authentication executions: %s", executions.toString());
		List<String> refs = AmrUtils.getAuthenticationExecutionReferences(executions, context.getRealm());
		log.debugf("amr %s set in session", refs);
		return refs;
	}

}
