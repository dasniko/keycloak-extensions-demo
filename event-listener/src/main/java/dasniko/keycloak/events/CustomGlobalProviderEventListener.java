package dasniko.keycloak.events;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;

@Slf4j
@AutoService(EventListenerProviderFactory.class)
public class CustomGlobalProviderEventListener implements EventListenerProvider, EventListenerProviderFactory {

	public static final String PROVIDER_ID = "globalProviderEvents";

	@Override
	public void onEvent(Event event) {
	}

	@Override
	public void onEvent(AdminEvent adminEvent, boolean b) {
	}

	@Override
	public EventListenerProvider create(KeycloakSession keycloakSession) {
		return this;
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		factory.register(fired -> {
			log.trace("ProviderEvent fired: {}", fired.getClass().getName());
			if (fired instanceof UserModel.UserRemovedEvent event) {
				log.info("***** UserRemovedEvent: {} - {} - {}", event.getRealm().getName(), event.getUser().getUsername(), event.getUser().getId());
			}
		});
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public boolean isGlobal() {
		return true;
	}
}
