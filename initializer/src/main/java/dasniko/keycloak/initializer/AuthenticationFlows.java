package dasniko.keycloak.initializer;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class AuthenticationFlows {

	private static final String STEP_UP_FLOW = "browser-step-up";

	static void createAuthFlows(RealmModel realm) {
		AuthenticationFlowModel flow = realm.getFlowByAlias(STEP_UP_FLOW);
		if (flow != null) {
			log.info("Flow {} exists. Skipping.", STEP_UP_FLOW);
			return;
		}

		log.info("Creating auth flow for {}", STEP_UP_FLOW);
		flow = new AuthenticationFlowModel();
		flow.setAlias(STEP_UP_FLOW);
		flow.setProviderId("basic-flow");
		flow.setDescription("browser based step-up authentication");
		flow.setTopLevel(true);
		flow = realm.addAuthenticationFlow(flow);

		addAlternativeExecutionToFlow(realm, flow, CookieAuthenticatorFactory.PROVIDER_ID);
		addAlternativeExecutionToFlow(realm, flow, IdentityProviderAuthenticatorFactory.PROVIDER_ID);

		// forms
		AuthenticationFlowModel forms = new AuthenticationFlowModel();
		forms.setTopLevel(false);
		forms.setAlias("%s %s".formatted(STEP_UP_FLOW, "forms"));
		forms.setProviderId("basic-flow");
		forms = realm.addAuthenticationFlow(forms);

		AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
		execution.setParentFlow(flow.getId());
		execution.setFlowId(forms.getId());
		execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
		execution.setAuthenticatorFlow(true);
		execution.setPriority(getNextPriority(realm, flow));
		realm.addAuthenticatorExecution(execution);

		addCondtionalToFlow(realm, forms, "basic / silver condition", UsernamePasswordFormFactory.PROVIDER_ID, "silver", "1", "36000");
		addCondtionalToFlow(realm, forms, "advanced / gold condition", OTPFormAuthenticatorFactory.PROVIDER_ID, "gold", "2", "10");

	}

	private static void addAlternativeExecutionToFlow(RealmModel realm, AuthenticationFlowModel flow, String providerId) {
		log.info("Adding execution {} for auth flow for {}", providerId, flow.getAlias());
		AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
		execution.setParentFlow(flow.getId());
		execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
		execution.setPriority(getNextPriority(realm, flow));
		execution.setAuthenticatorFlow(false);
		execution.setAuthenticator(providerId);
		realm.addAuthenticatorExecution(execution);
	}

	private static void addCondtionalToFlow(RealmModel realm, AuthenticationFlowModel flow,
																					String alias, String conditionalProviderId,
																					String configAlias, String loaConditionLevel, String loaMaxAge) {
		log.info("Adding conditional branch {} for auth flow {}", alias, flow.getAlias());
		AuthenticationFlowModel conditional = new AuthenticationFlowModel();
		conditional.setTopLevel(false);
		conditional.setAlias(alias);
		conditional.setProviderId("basic-flow");
		realm.addAuthenticationFlow(conditional);

		AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
		execution.setParentFlow(flow.getId());
		execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
		execution.setFlowId(conditional.getId());
		execution.setPriority(getNextPriority(realm, flow));
		execution.setAuthenticatorFlow(true);
		realm.addAuthenticatorExecution(execution);

		execution = new AuthenticationExecutionModel();
		execution.setParentFlow(conditional.getId());
		execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
		execution.setAuthenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
		execution.setPriority(getNextPriority(realm, flow));
		execution.setAuthenticatorFlow(false);

		AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
		configModel.setAlias(configAlias);
		Map<String, String> config = new HashMap<>();
		config.put("loa-condition-level", loaConditionLevel);
		config.put("loa-max-age", loaMaxAge);
		configModel.setConfig(config);
		configModel = realm.addAuthenticatorConfig(configModel);

		execution.setAuthenticatorConfig(configModel.getId());
		realm.addAuthenticatorExecution(execution);

		execution = new AuthenticationExecutionModel();
		execution.setParentFlow(conditional.getId());
		execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
		execution.setAuthenticator(conditionalProviderId);
		execution.setPriority(getNextPriority(realm, flow));
		execution.setAuthenticatorFlow(false);
		realm.addAuthenticatorExecution(execution);
	}

	private static int getNextPriority(RealmModel realm, AuthenticationFlowModel parentFlow) {
		List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(parentFlow.getId()).toList();
		return executions.isEmpty() ? 0 : executions.get(executions.size() - 1).getPriority() + 1;
	}

}
