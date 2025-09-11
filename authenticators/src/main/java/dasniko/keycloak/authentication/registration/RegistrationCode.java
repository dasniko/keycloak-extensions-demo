package dasniko.keycloak.authentication.registration;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoService(FormActionFactory.class)
public class RegistrationCode implements FormAction, FormActionFactory {

	public static final String PROVIDER_ID = "registration-code";
	public static final String REGISTRATION_CODE = "registrationCode";

	@Override
	public void validate(ValidationContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String registrationCode = formData.getFirst(REGISTRATION_CODE);
		if (registrationCode == null || registrationCode.isBlank()) {
			List<FormMessage> errors = new ArrayList<>();
			errors.add(new FormMessage(REGISTRATION_CODE, "registrationCodeRequired"));
			context.error("registrationCodeInvalid");
			context.validationError(formData, errors);
			return;
		}
		context.getAuthenticationSession().setAuthNote(REGISTRATION_CODE, registrationCode);
		context.success();
	}

	@Override
	public void success(FormContext context) {
		// do nothing here for poc
		// e.g., enable user if everything is successful
		log.info("Registration code validation successful: {}", context.getAuthenticationSession().getAuthNote(REGISTRATION_CODE));
	}

	@Override
	public String getDisplayType() {
		return "Registration Code";
	}

	@Override
	public String getReferenceCategory() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return CustomRegistrationPage.REQUIREMENT_CHOICES;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public void buildPage(FormContext context, LoginFormsProvider form) {
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public String getHelpText() {
		return "...help text...";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of();
	}

	@Override
	public FormAction create(KeycloakSession session) {
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
