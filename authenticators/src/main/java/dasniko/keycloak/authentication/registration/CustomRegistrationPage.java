package dasniko.keycloak.authentication.registration;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.FormAuthenticator;
import org.keycloak.authentication.FormAuthenticatorFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@AutoService(FormAuthenticatorFactory.class)
public class CustomRegistrationPage implements FormAuthenticator, FormAuthenticatorFactory {

	public static final String PROVIDER_ID = "custom-registration-page";

	@Override
	public Response render(FormContext context, LoginFormsProvider form) {
		String registrationCode = "4711";
		MultivaluedMap<String, String> queryParameters = context.getHttpRequest().getUri().getQueryParameters();
		if (queryParameters.containsKey(RegistrationCode.REGISTRATION_CODE)) {
			registrationCode = queryParameters.getFirst(RegistrationCode.REGISTRATION_CODE);
		}
		form.setAttribute(RegistrationCode.REGISTRATION_CODE, registrationCode);
		return form.createForm("registration-code.ftl");
	}

	@Override
	public String getDisplayType() {
		return "Custom Registration Page";
	}

	@Override
	public String getReferenceCategory() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
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
		return "custom registration page help text";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of();
	}

	@Override
	public FormAuthenticator create(KeycloakSession session) {
		return this;
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
