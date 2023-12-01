package dasniko.keycloak.authentication.mfa;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MfaConstants {
	String CONFIG_PROPERTY_LENGTH = "length";
	String CONFIG_PROPERTY_TTL = "ttl";
	String CONFIG_PROPERTY_PROVIDER = "provider";
	String AUTH_NOTE_CODE = "mfa-code";
	String AUTH_NOTE_EXPIRATION  = "mfa-expiration";
}
