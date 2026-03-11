package de.keycloak.util;

import lombok.experimental.UtilityClass;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Map;

@UtilityClass
public class AuthenticatorUtil {

	@SuppressWarnings({"unchecked", "unused"})
	public static <T> T getConfig(AuthenticationFlowContext context, String configKey, T defaultValue) {
		T value = defaultValue;

		AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
		if (configModel != null) {
			Map<String, String> config = configModel.getConfig();
			if (config != null && config.containsKey(configKey)) {
				String configValue = config.get(configKey);
				value = switch (value) {
					case Boolean b -> (T) Boolean.valueOf(configValue);
					case Long l -> (T) Long.valueOf(configValue);
					case Integer i -> (T) Integer.valueOf(configValue);
					case null, default -> (T) configValue;
				};
			}
		}

		return value;
	}

}
