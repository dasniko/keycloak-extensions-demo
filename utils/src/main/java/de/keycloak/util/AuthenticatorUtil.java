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
				if (value instanceof Boolean) {
					value = (T) Boolean.valueOf(configValue);
				} else if (value instanceof Long) {
					value = (T) Long.valueOf(configValue);
				} else {
					value = (T) configValue;
				}
			}
		}

		return value;
	}

}
