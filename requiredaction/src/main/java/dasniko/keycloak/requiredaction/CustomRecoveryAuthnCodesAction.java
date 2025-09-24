package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.RecoveryAuthnCodesAction;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dasniko.keycloak.requiredaction.RequiredActionUtil.CFG_REQUIRED_ACTIONS;
import static dasniko.keycloak.requiredaction.RequiredActionUtil.getCredentialTypes;

@AutoService(RequiredActionFactory.class)
public class CustomRecoveryAuthnCodesAction extends RecoveryAuthnCodesAction {

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

	static {
		CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
			.property()
			.name(CFG_REQUIRED_ACTIONS)
			.label("Required Actions")
			.helpText("On which required actions the action should be automatically triggered, if user does not yet has recovery codes configured.")
			.type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
			.options(
				UserModel.RequiredAction.CONFIGURE_TOTP.name(),
				WebAuthnRegisterFactory.PROVIDER_ID
			)
			.add()
			.build();
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		if (skipEvaluation(context)) {
			return;
		}

		UserModel user = context.getUser();
		KeycloakSession session = context.getSession();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		Map<String, String> credentialTypes = getCredentialTypes(context);
		if (credentialTypes.keySet().stream().anyMatch(type -> user.credentialManager().isConfiguredFor(type))
			&& !user.credentialManager().isConfiguredFor(getCredentialType(session, authSession))) {
			authSession.addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return this;
	}

	@Override
	public List<ProviderConfigProperty> getConfigMetadata() {
		return Stream.concat(
			super.getConfigMetadata().stream(),
			List.copyOf(CONFIG_PROPERTIES).stream()
		).toList();
	}

	@Override
	public int order() {
		return super.order() + 10;
	}

	private boolean skipEvaluation(RequiredActionContext context) {
		RequiredActionConfigModel config = context.getConfig();
		String actions = config.getConfigValue(CFG_REQUIRED_ACTIONS);
		return actions == null || actions.isBlank();
	}

}
