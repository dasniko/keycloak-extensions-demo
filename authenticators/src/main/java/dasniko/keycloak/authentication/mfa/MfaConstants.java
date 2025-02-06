package dasniko.keycloak.authentication.mfa;

import dasniko.keycloak.sms.logging.LoggingSmsProviderFactory;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MfaConstants {
	String CONFIG_PROPERTY_LENGTH = "length";
	int CONFIG_PROPERTY_LENGTH_DEFAULT = 6;
	String CONFIG_PROPERTY_TTL = "ttl";
	int CONFIG_PROPERTY_TTL_DEFAULT = 300;
	String CONFIG_PROPERTY_PROVIDER = "provider";
	String CONFIG_PROPERTY_PROVIDER_DEFAULT = LoggingSmsProviderFactory.PROVIDER_ID;
	String AUTH_NOTE_CODE = "mfa-code";
	String AUTH_NOTE_EXPIRATION  = "mfa-expiration";
}
