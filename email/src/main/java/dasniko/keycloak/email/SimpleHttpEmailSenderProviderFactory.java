package dasniko.keycloak.email;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(EmailSenderProviderFactory.class)
public class SimpleHttpEmailSenderProviderFactory implements EmailSenderProviderFactory {

	public static final String PROVIDER_ID = "simple-http";

	static String url;

	@Override
	public EmailSenderProvider create(KeycloakSession session) {
		return new SimpleHttpEmailSenderProvider(session);
	}

	@Override
	public void init(Config.Scope config) {
		url = config.get("url");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
