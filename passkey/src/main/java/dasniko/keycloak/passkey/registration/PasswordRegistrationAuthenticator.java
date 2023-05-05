package dasniko.keycloak.passkey.registration;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains all the logic when the password-registration.ftl file is rendered or interacted with.
 * password-registration.ftl displays a page with two input fields. One for the password, the other for password confirmation.
 * <p>
 * This authenticator is supposed to be used in the registration flow.
 * Purpose: Create a user account upon successfully entering the password form.
 * <p>
 * IMPORTANT: This authenticator can only be used when {@link RegistrationUserCreationNoAccount} is used in the registration form,
 * as we rely on data submitted in the session authentication notes. It is also required that the authenticator defined in
 * {@link PasskeyOrPasswordRegistrationAuthenticator} precedes this authenticator in the registration flow,
 * as we check which setup type has been chosen.
 **/
public class PasswordRegistrationAuthenticator implements Authenticator {

	private static final String TPL_CODE = "password-registration.ftl";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
	}

	/**
	 * Implemented similarly to the validate method of {@link org.keycloak.authentication.forms.RegistrationPassword}.
	 */
	@Override
	public void action(AuthenticationFlowContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		List<FormMessage> errors = new ArrayList<>();

		// Check password validity
		if (Validation.isBlank(formData.getFirst(RegistrationPage.FIELD_PASSWORD))) {
			errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, Messages.MISSING_PASSWORD));
		} else if (!formData.getFirst(RegistrationPage.FIELD_PASSWORD).equals(formData.getFirst(RegistrationPage.FIELD_PASSWORD_CONFIRM))) {
			errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD_CONFIRM, Messages.INVALID_PASSWORD_CONFIRM));
		}
		if (formData.getFirst(RegistrationPage.FIELD_PASSWORD) != null) {
			PolicyError err = context.getSession().getProvider(PasswordPolicyManagerProvider.class)
				.validate(context.getRealm().isRegistrationEmailAsUsername()
						? formData.getFirst(RegistrationPage.FIELD_EMAIL)
						: formData.getFirst(RegistrationPage.FIELD_USERNAME),
					formData.getFirst(RegistrationPage.FIELD_PASSWORD));
			if (err != null) {
				errors.add(new FormMessage(RegistrationPage.FIELD_PASSWORD, err.getMessage(), err.getParameters()));
			}
		}

		if (errors.isEmpty()) {
			// Create an user account, if the submitted password is valid!
			String password = formData.getFirst(RegistrationPage.FIELD_PASSWORD);
			createUserAccount(context, password);
			context.success();
		} else {
			context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
				context.form().setErrors(errors).createForm(TPL_CODE));
		}
	}

	private void createUserAccount(AuthenticationFlowContext context, String password) {
		Utils.createUserFromAuthSessionNotes(context);
		UserModel user = context.getUser();
		user.credentialManager().updateCredential(UserCredentialModel.password(password, false));
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return false;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}
}
