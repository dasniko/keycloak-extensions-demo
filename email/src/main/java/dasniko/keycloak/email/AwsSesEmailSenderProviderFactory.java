package dasniko.keycloak.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class AwsSesEmailSenderProviderFactory implements EmailSenderProviderFactory {

	public static final String PROVIDER_ID = "aws-ses";

	private final SesClient ses = SesClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public EmailSenderProvider create(KeycloakSession session) {
		return new AwsSesEmailSenderProvider(ses, objectMapper);
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
