/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dasniko.keycloak.passkey.registration;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationUserCreation;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;

import java.util.List;

@AutoService(FormActionFactory.class)
public class RegistrationUserCreationNoAccount extends RegistrationUserCreation {

	public static final String PROVIDER_ID = "user-creation-no-account";

	@Override
	public String getHelpText() {
		return "This action is not allowed to contain other built-in actions in this registration form, as other built-in action require that the user account is already set/created. This form does not create a user account!";
	}

	/**
	 * Copied from {@link org.keycloak.authentication.forms.RegistrationProfile} as the validation check of
	 * {@link RegistrationUserCreation} does not check for invalid emails.
	 */
	@Override
	public void validate(ValidationContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

		context.getEvent().detail(Details.REGISTER_METHOD, "form");

		UserProfileProvider profileProvider = context.getSession().getProvider(UserProfileProvider.class);
		UserProfile profile = profileProvider.create(UserProfileContext.REGISTRATION, formData);

		// We check if the email address is already in use.
		// If yes, we return an error that is displayed to the user.
		try {
			profile.validate();
		} catch (ValidationException pve) {
			List<FormMessage> errors = Validation.getFormErrorsFromValidation(pve.getErrors());

			if (pve.hasError(Messages.EMAIL_EXISTS, Messages.INVALID_EMAIL)) {
				context.getEvent().detail(Details.EMAIL, profile.getAttributes().getFirst(UserModel.EMAIL));
			}

			if (pve.hasError(Messages.EMAIL_EXISTS)) {
				context.error(Errors.EMAIL_IN_USE);
			} else {
				context.error(Errors.INVALID_REGISTRATION);
			}

			context.validationError(formData, errors);
			return;
		}

		super.validate(context);
	}

	@Override
	public void success(FormContext context) {
		// Following successful filling of the form, we store the required user information in the authentication session notes.
		// This stored information is then retrieved at a later time to create the user account.
		Utils.storeUserDataInAuthSessionNotes(context);
	}

	@Override
	public String getDisplayType() {
		return "Registration User Creation with no Account";
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return ConfigurableAuthenticatorFactory.REQUIREMENT_CHOICES;
	}


	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
