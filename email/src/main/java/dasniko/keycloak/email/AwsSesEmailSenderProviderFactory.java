package dasniko.keycloak.email;

import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class AwsSesEmailSenderProviderFactory implements EmailSenderProviderFactory {

	public static final String PROVIDER_ID = "aws-ses";

	@Override
	public EmailSenderProvider create(KeycloakSession session) {
		return null;
	}

	@Override
	public void init(Config.Scope config) {
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
