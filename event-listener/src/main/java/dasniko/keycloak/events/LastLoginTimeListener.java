package dasniko.keycloak.events;

import lombok.RequiredArgsConstructor;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class LastLoginTimeListener implements EventListenerProvider {

	private final KeycloakSession session;

	@Override
	public void onEvent(Event event) {
		if (event.getType().equals(EventType.LOGIN)) {
			UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
			if (user != null) {
				user.setSingleAttribute(LastLoginTimeListenerFactory.attributeName,
					Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			}
		}
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
	}

	@Override
	public void close() {
	}

}
