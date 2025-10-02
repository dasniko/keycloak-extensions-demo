package dasniko.keycloak.events;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Map;

@Slf4j(topic = "org.keycloak.events")
public class JsonEventListenerProvider implements EventListenerProvider {

	private final KeycloakSession session;
	private final Level successLevel;
	private final Level errorLevel;
	private final EventListenerTransaction tx = new EventListenerTransaction(this::sendAdminEvent, this::logEvent);

	public JsonEventListenerProvider(KeycloakSession session, Level successLevel, Level errorLevel) {
		this.session = session;
		this.successLevel = successLevel;
		this.errorLevel = errorLevel;

		session.getTransactionManager().enlistAfterCompletion(tx);
	}

	@Override
	public void onEvent(Event event) {
		tx.addEvent(event);
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		tx.addAdminEvent(event, includeRepresentation);
	}

	@Override
	public void close() {
	}

	private void logEvent(Event event) {
		Level level = event.getError() != null ? errorLevel : successLevel;

		if (log.isEnabledForLevel(level)) {
			String s = null;
			try {
				Map<String, Object> map = JsonSerialization.mapper.convertValue(event, new TypeReference<>() {});

				AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
				if(authSession!=null) {
					map.put("authSessionParentId", authSession.getParentSession().getId());
					map.put("authSessionTabId", authSession.getTabId());
				}

				if (log.isTraceEnabled()) {
					setKeycloakContext(map);
				}

				s = JsonSerialization.writeValueAsString(map);
			} catch (IOException e) {
				log.error("Error while trying to JSONify event %s".formatted(ToStringBuilder.reflectionToString(event)), e);
			}

			log.atLevel(log.isTraceEnabled() ? Level.TRACE : level).log(s);
		}
	}

	private void sendAdminEvent(AdminEvent event, boolean includeRepresentation) {
		Level level = event.getError() != null ? errorLevel : successLevel;

		if (log.isEnabledForLevel(level)) {
			String s = null;
			try {
				Map<String, Object> map = JsonSerialization.mapper.convertValue(event, new TypeReference<>() {
				});

				if (log.isTraceEnabled()) {
					setKeycloakContext(map);
				}

				s = JsonSerialization.writeValueAsString(map);
			} catch (IOException e) {
				log.error("Error while trying to JSONify admin event %s".formatted(ToStringBuilder.reflectionToString(event)), e);
			}

			log.atLevel(log.isTraceEnabled() ? Level.TRACE : level).log(s);
		}
	}

	private void setKeycloakContext(Map<String, Object> map) {
		KeycloakContext context = session.getContext();
		UriInfo uriInfo = context.getUri();
		if (uriInfo != null) {
			map.put("requestUri", uriInfo.getRequestUri().toString());
		}
		HttpHeaders headers = context.getRequestHeaders();
		if (headers != null) {
			map.put("cookies", headers.getCookies());
		}
	}

}
