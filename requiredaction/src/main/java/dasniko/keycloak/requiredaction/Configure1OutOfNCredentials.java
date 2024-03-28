package dasniko.keycloak.requiredaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.util.Map;

@Slf4j
@AutoService(RequiredActionFactory.class)
public class Configure1OutOfNCredentials implements RequiredActionFactory, RequiredActionProvider, ServerInfoAwareProviderFactory {

	public static final String PROVIDER_ID = "configure1OutOfNCreds";

	// map of key-value pairs: key = credential type, value = associated required action id
	// { "otp": "CONFIGURE_TOTP", "webauthn": "webauthn-register" }
	private static Map<String, String> credentialTypes = Map.of(
		OTPCredentialModel.TYPE, UserModel.RequiredAction.CONFIGURE_TOTP.name(),
		WebAuthnCredentialModel.TYPE_TWOFACTOR, WebAuthnRegisterFactory.PROVIDER_ID
	);

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// self registering if user doesn't have already one out of the configured credential types
		UserModel user = context.getUser();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		if (credentialTypes.keySet().stream().noneMatch(type -> user.credentialManager().isConfiguredFor(type))
			&& user.getRequiredActionsStream().noneMatch(credentialTypes::containsValue)
			&& authSession.getRequiredActions().stream().noneMatch(credentialTypes::containsValue)) {
				authSession.addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// initial form
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

	@Override
	public void init(Config.Scope config) {
		String typesString = config.get("typesString", " { \"otp\": \"CONFIGURE_TOTP\", \"webauthn\": \"webauthn-register\" }");
		ObjectMapper mapper = new ObjectMapper();
		try {
			credentialTypes = mapper.readValue(typesString, new TypeReference<>() {});
		} catch (IOException e) {
			log.warn("Couldn't parse typesString: {}", typesString);
		}
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

	@Override
	public Map<String, String> getOperationalInfo() {
		return credentialTypes;
	}
}
