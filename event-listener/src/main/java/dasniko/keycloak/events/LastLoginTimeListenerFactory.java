package dasniko.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class LastLoginTimeListenerFactory implements EventListenerProviderFactory {

	public static final String PROVIDER_ID = "last-login-time";

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new LastLoginTimeListener(session);
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
