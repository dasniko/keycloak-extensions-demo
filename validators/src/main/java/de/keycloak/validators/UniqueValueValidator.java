package de.keycloak.validators;

import com.google.auto.service.AutoService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.ValidatorFactory;

import java.util.List;

@AutoService(ValidatorFactory.class)
public class UniqueValueValidator extends AbstractStringValidator implements ConfiguredProvider {

	public static final String PROVIDER_ID = "unique-value";

	@Override
	public String getHelpText() {
		return "Ensures a unique attribute value over all users in the realm.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of();
	}

	@Override
	protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
		KeycloakSession session = context.getSession();
		UserModel contextUser = (UserModel)context.getAttributes().get(UserModel.class.getName());
		boolean exists = session.users()
			.searchForUserByUserAttributeStream(session.getContext().getRealm(), inputHint, value)
			.anyMatch(user -> !user.getId().equals(contextUser.getId()));
		if (exists) {
			context.addError(new ValidationError(PROVIDER_ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE));
		}
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
