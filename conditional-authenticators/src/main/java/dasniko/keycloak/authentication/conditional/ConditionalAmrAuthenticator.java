package dasniko.keycloak.authentication.conditional;

import com.google.auto.service.AutoService;
import de.keycloak.util.AuthenticatorUtil;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.utils.AmrUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

@JBossLog
@AutoService(AuthenticatorFactory.class)
public class ConditionalAmrAuthenticator extends AbstractConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-amr";

	static final String CONF_AMR_VALUE = "amr_value";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String expectedValue = AuthenticatorUtil.getConfig(context, CONF_AMR_VALUE, "");
		List<String> amrValues = getAmr(context);
		boolean negateOutput = AuthenticatorUtil.getConfig(context, ConditionalAuthNoteAuthenticatorFactory.CONF_NOT, Boolean.FALSE);

		return negateOutput != amrValues.contains(expectedValue);
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public String getDisplayType() {
		return "Condition - Authentication Method Reference";
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
	public String getHelpText() {
		return "...some help text...";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty amrExpectedValue = new ProviderConfigProperty();
		amrExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
		amrExpectedValue.setName(CONF_AMR_VALUE);
		amrExpectedValue.setLabel("AMR value");
		amrExpectedValue.setHelpText("Expected authenticator method reference value.");

		return List.of(amrExpectedValue, negateOutputConfProperty);
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
