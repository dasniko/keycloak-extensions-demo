package de.keycloak.util;

import lombok.experimental.UtilityClass;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Optional;

@UtilityClass
public class AuthenticatorUtil {

	@SuppressWarnings({"unchecked", "unused"})
	public static <T> T getConfig(AuthenticationFlowContext context, String configKey, T defaultValue) {
		return Optional.ofNullable(context.getAuthenticatorConfig())
			.map(AuthenticatorConfigModel::getConfig)
			.filter(config -> config.containsKey(configKey))
			.map(config -> config.get(configKey))
			.map(configValue -> switch (defaultValue) {
				case Boolean b -> (T) Boolean.valueOf(configValue);
				case Long l -> (T) Long.valueOf(configValue);
				case Integer i -> (T) Integer.valueOf(configValue);
				case null, default -> (T) configValue;
			})
			.orElse(defaultValue);
	}

}
