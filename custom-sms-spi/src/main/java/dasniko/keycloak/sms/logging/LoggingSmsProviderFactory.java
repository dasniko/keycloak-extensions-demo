package dasniko.keycloak.sms.logging;

import com.google.auto.service.AutoService;
import dasniko.keycloak.sms.SmsProvider;
import dasniko.keycloak.sms.SmsProviderFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(SmsProviderFactory.class)
public class LoggingSmsProviderFactory implements SmsProviderFactory {

	public static final String PROVIDER_ID = "logger";

	private static final SmsProvider SINGLETON = new LoggingSmsProvider();

	@Override
	public SmsProvider create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
