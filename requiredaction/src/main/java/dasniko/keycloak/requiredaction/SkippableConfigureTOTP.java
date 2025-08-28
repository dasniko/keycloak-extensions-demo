package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.requiredactions.UpdateTotp;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.validate.ValidationError;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@JBossLog
@AutoService(RequiredActionFactory.class)
public class SkippableConfigureTOTP extends UpdateTotp {

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;
	protected static final String SKIP_DEADLINE_KEY = "skip_deadline";

	static {
		CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
			.property()
			.name(SKIP_DEADLINE_KEY)
			.label("Skip Deadline")
			.helpText("OTP configuration will be skippable (cancelable) until the given date, in ISO-8601 format (e.g. '2025-01-01'). If there's no date given, OTP configuration will not be skippable.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.defaultValue("2025-01-01")
			.add()
			.build();
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		if (isFutureDeadline(context.getConfig())) {
			makeActionSkippable(context);
		}
		super.requiredActionChallenge(context);
	}

	@Override
	public String getDisplayText() {
		return "Configure OTP (Skippable)";
	}

	@Override
	public List<ProviderConfigProperty> getConfigMetadata() {
		List<ProviderConfigProperty> properties = new ArrayList<>(super.getConfigMetadata());
		properties.addAll(CONFIG_PROPERTIES);
		return List.copyOf(properties);
	}

	@Override
	public void validateConfig(KeycloakSession session, RealmModel realm, RequiredActionConfigModel model) {
		try {
			isFutureDeadline(model);
		} catch (Exception ex) {
			throw new ValidationException(new ValidationError(getId(), SKIP_DEADLINE_KEY, "error-invalid-value"));
		}
	}

	@Override
	public int order() {
		return 100;
	}

	protected boolean isFutureDeadline(RequiredActionConfigModel config) {
		if (config != null) {
			if (config.containsConfigKey(SKIP_DEADLINE_KEY)) {
				String deadlineString = config.getConfigValue(SKIP_DEADLINE_KEY);
				if (deadlineString != null && !deadlineString.isEmpty()) {
					LocalDate deadlineDate = LocalDate.parse(deadlineString, DateTimeFormatter.ISO_LOCAL_DATE);
					return deadlineDate.isAfter(LocalDate.now());
				}
			}
		}
		return false;
	}

	private void makeActionSkippable(RequiredActionContext context) {
		context.getAuthenticationSession().setClientNote(Constants.KC_ACTION_EXECUTING, getId());
		context.getAuthenticationSession().setClientNote(Constants.KC_ACTION_ENFORCED, Boolean.FALSE.toString());
	}
}
