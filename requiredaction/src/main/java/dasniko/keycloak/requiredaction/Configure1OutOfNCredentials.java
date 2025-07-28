package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoService(RequiredActionFactory.class)
public class Configure1OutOfNCredentials implements RequiredActionFactory, RequiredActionProvider {

	public static final String PROVIDER_ID = "configure1OutOfNCreds";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// self registering if user doesn't have already one out of the configured credential types
		UserModel user = context.getUser();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();

		Map<String, String> credentialTypes = getCredentialTypes(context);
		if (credentialTypes.keySet().stream().noneMatch(type -> user.credentialManager().isConfiguredFor(type))
			&& user.getRequiredActionsStream().noneMatch(credentialTypes::containsValue)
			&& authSession.getRequiredActions().stream().noneMatch(credentialTypes::containsValue)) {
				authSession.addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// initial form
		Map<String, String> credentialTypes = getCredentialTypes(context);
		LoginFormsProvider form = context.form()
			.setAttribute("realm", context.getRealm())
			.setAttribute("user", context.getUser())
			.setAttribute("credentialOptions", credentialTypes);
		context.challenge(form.createForm("config-1-out-of-n-creds.ftl"));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String requiredActionName = formData.getFirst("requiredActionName");

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.addRequiredAction(requiredActionName);

		authSession.removeRequiredAction(PROVIDER_ID);
		context.success();
	}

	@Override
	public String getDisplayText() {
		return "Configure 1 out of N Credentials";
	}

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return this;
	}

	private static final List<ProviderConfigProperty> configProperties;
	static {
		configProperties = ProviderConfigurationBuilder.create()
			.property()
			.name("requiredActions")
			.label("configureCredentialsActionsLabel")
			.helpText("configureCredentialsActionsHelp")
			.type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
			.options(
				UserModel.RequiredAction.CONFIGURE_TOTP.name(),
				UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name(),
				WebAuthnRegisterFactory.PROVIDER_ID
			)
			.defaultValue(String.join(Constants.CFG_DELIMITER, UserModel.RequiredAction.CONFIGURE_TOTP.name(), WebAuthnRegisterFactory.PROVIDER_ID))
			.add()
			.build();
	}

	@Override
	public List<ProviderConfigProperty> getConfigMetadata() {
		List<ProviderConfigProperty> properties = new ArrayList<>(List.copyOf(MAX_AUTH_AGE_CONFIG_PROPERTIES));
		properties.addAll(configProperties);
		return List.copyOf(properties);
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

	private Map<String, String> getCredentialTypes(RequiredActionContext context) {
		KeycloakSession session = context.getSession();
		RequiredActionConfigModel config = context.getConfig();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();

		String requiredActionsString = config.getConfigValue("requiredActions");
		List<String> requiredActions = Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(requiredActionsString));

		Map<String, String> credentialTypes = new LinkedHashMap<>();
		session.getKeycloakSessionFactory()
			.getProviderFactoriesStream(RequiredActionProvider.class)
			.forEach(providerFactory -> {
				String providerId = providerFactory.getId();
				if (requiredActions.contains(providerId)) {
					RequiredActionProvider action = (RequiredActionProvider) providerFactory.create(session);
					if (action instanceof CredentialRegistrator) {
						String credentialType = ((CredentialRegistrator) action).getCredentialType(session, authSession);
						credentialTypes.put(credentialType, providerId);
					}
				}
			});
		return credentialTypes;
	}
}
