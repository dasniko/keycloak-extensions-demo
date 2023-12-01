package dasniko.keycloak.sms.logging;

import dasniko.keycloak.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingSmsProvider implements SmsProvider {

	@Override
	public boolean sendMessage(String phoneNumber, String message) {
		log.warn("Would send message to {}: {}", phoneNumber, message);
		return true;
	}

}
