package dasniko.keycloak.sms;

import org.keycloak.provider.Provider;

public interface SmsProvider extends Provider {

	boolean sendMessage(String phoneNumber, String message);

	@Override
	default void close() {
	}

}
