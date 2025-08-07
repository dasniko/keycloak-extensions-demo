package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
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

@AutoService(RequiredActionFactory.class)
public class MfaEnrollment implements RequiredActionFactory, RequiredActionProvider {

  public static final String PROVIDER_ID = "mfaEnrollment";

  private static final String CFG_REQUIRED_ACTIONS = "requiredActions";
  private static final String CFG_CONDITIONAL_ROLE = "conditionalRole";
  private static final String CFG_CONDITIONAL_ROLE_NEGATE = "conditionalRoleNegate";
  private static final String MFA_ENROLLMENT_TPL = "mfa-enrollment.ftl";

  private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

  static {
    CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
      .property()
      .name(CFG_REQUIRED_ACTIONS)
      .label("mfaEnrollmentActionsLabel")
      .helpText("mfaEnrollmentActionsHelp")
      .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
      .options(
        UserModel.RequiredAction.CONFIGURE_TOTP.name(),
        UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name(),
        WebAuthnRegisterFactory.PROVIDER_ID
      )
      .defaultValue(String.join(Constants.CFG_DELIMITER, UserModel.RequiredAction.CONFIGURE_TOTP.name(), WebAuthnRegisterFactory.PROVIDER_ID))
      .add()
      .property()
      .name(CFG_CONDITIONAL_ROLE)
      .label("mfaEnrollmentRoleLabel")
      .helpText("mfaEnrollmentRoleHelp")
      .type(ProviderConfigProperty.ROLE_TYPE)
      .add()
      .property()
      .name(CFG_CONDITIONAL_ROLE_NEGATE)
      .label("mfaEnrollmentRoleNegateLabel")
      .helpText("mfaEnrollmentRoleNegateHelp")
      .type(ProviderConfigProperty.BOOLEAN_TYPE)
      .add()
      .build();
  }

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
  public void evaluateTriggers(RequiredActionContext context) {
    if (skipEvaluation(context)) {
      return;
    }

    // self registering if the user doesn't already have one out of the configured credential types configured
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
    context.challenge(form.createForm(MFA_ENROLLMENT_TPL));
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
    return "MFA Enrollment";
  }

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return this;
	}

	@Override
  public List<ProviderConfigProperty> getConfigMetadata() {
    List<ProviderConfigProperty> properties = new ArrayList<>(List.copyOf(MAX_AUTH_AGE_CONFIG_PROPERTIES));
    properties.addAll(CONFIG_PROPERTIES);
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
    String cacheKey = getId() + "_credentialTypes";
    //noinspection unchecked
    Map<String, String> cachedCredentialTypes = session.getAttribute(cacheKey, Map.class);
    if (cachedCredentialTypes != null) {
      return cachedCredentialTypes;
    }

    RequiredActionConfigModel config = context.getConfig();
    AuthenticationSessionModel authSession = context.getAuthenticationSession();

    String requiredActionsString = config.getConfigValue(CFG_REQUIRED_ACTIONS);
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
    session.setAttribute(cacheKey, credentialTypes);
    return credentialTypes;
  }

  private boolean skipEvaluation(RequiredActionContext context) {
    UserModel user = context.getUser();
    RequiredActionConfigModel config = context.getConfig();
    String conditionalRole = config.getConfigValue(CFG_CONDITIONAL_ROLE);
    boolean negate = Boolean.parseBoolean(config.getConfigValue(CFG_CONDITIONAL_ROLE_NEGATE, "false"));
    if (conditionalRole != null && !conditionalRole.isBlank()) {
      boolean hasRole = user.hasRole(context.getRealm().getRole(conditionalRole));
      return hasRole == negate;
    }
    return false;
  }
}
