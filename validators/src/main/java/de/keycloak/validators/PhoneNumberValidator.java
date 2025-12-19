package de.keycloak.validators;

import com.google.auto.service.AutoService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.ValidatorFactory;

import java.util.List;

/**
 * Validator for phone numbers using libphonenumber.
 * Validates that the input is a semantically correct phone number for a configurable region.
 *
 * Configuration:
 * - region: ISO 3166-1 alpha-2 country code (e.g., "DE", "US", "GB"). Defaults to "DE".
 */
@AutoService(ValidatorFactory.class)
public class PhoneNumberValidator extends AbstractStringValidator implements ConfiguredProvider {

	public static final String PROVIDER_ID = "phone-number";
	public static final String MESSAGE_INVALID_PHONE = "error-invalid-phone-number";
	public static final String CONFIG_REGION = "region";
	public static final String DEFAULT_REGION = "DE";

	private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	@Override
	public String getHelpText() {
		return "Validates that the input is a semantically correct phone number for the configured region";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty regionProperty = new ProviderConfigProperty();
		regionProperty.setName(CONFIG_REGION);
		regionProperty.setLabel("Region");
		regionProperty.setHelpText("ISO 3166-1 alpha-2 country code (e.g., DE, US, GB, FR, AT, CH)");
		regionProperty.setType(ProviderConfigProperty.STRING_TYPE);
		regionProperty.setDefaultValue(DEFAULT_REGION);

		return List.of(regionProperty);
	}

	@Override
	protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
		if (value == null || value.trim().isEmpty()) {
			// Let other validators handle required field validation
			return;
		}

		// Get the configured region or use default
		String region = config.getStringOrDefault(CONFIG_REGION, DEFAULT_REGION);

		try {
			// Parse the phone number with the configured region
			Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(value, region);

			// Validate that it's a valid number for the configured region
			if (!phoneUtil.isValidNumberForRegion(phoneNumber, region)) {
				context.addError(new ValidationError(
					PROVIDER_ID,
					inputHint,
					MESSAGE_INVALID_PHONE
				));
			}
		} catch (NumberParseException e) {
			// Invalid phone number format
			context.addError(new ValidationError(
				PROVIDER_ID,
				inputHint,
				MESSAGE_INVALID_PHONE
			));
		}
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
