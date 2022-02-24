package dasniko.keycloak.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class AwsSnsEventListenerProviderFactory implements EventListenerProviderFactory {

	public static final String PROVIDER_ID = "aws-sns-publisher";
	private static final ObjectMapper mapper = new ObjectMapper();

	private SnsClient sns;

	@Override
	public EventListenerProvider create(KeycloakSession keycloakSession) {
		if (null == sns) {
			sns = SnsClient.create();
		}
		return new AwsSnsEventListenerProvider(keycloakSession, sns, mapper);
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
