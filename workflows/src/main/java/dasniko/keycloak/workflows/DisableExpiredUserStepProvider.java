package dasniko.keycloak.workflows;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowStepProvider;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Disables the user if the ISO date or date-time in the attribute named by the {@value #ATTRIBUTE_KEY} step setting
 * lies in the past. Values without an offset (a plain date or a local date-time) are interpreted in the server's time zone.
 *
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class DisableExpiredUserStepProvider implements WorkflowStepProvider {

	public static final String ATTRIBUTE_KEY = "attribute";

	private final KeycloakSession session;
	private final ComponentModel stepModel;

	public DisableExpiredUserStepProvider(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.stepModel = model;
	}

	@Override
	public void run(WorkflowExecutionContext context) {
		String attributeName = stepModel.getConfig().getFirst(ATTRIBUTE_KEY);
		if (attributeName == null || attributeName.isBlank()) {
			log.warn("Missing required configuration option '{}' in {}", ATTRIBUTE_KEY, DisableExpiredUserStepProviderFactory.PROVIDER_ID);
			return;
		}

		UserModel user = session.users().getUserById(session.getContext().getRealm(), context.getResourceId());
		if (user == null) {
			log.debug("User {} not found, nothing to do", context.getResourceId());
			return;
		}
		if (!user.isEnabled()) {
			log.debug("User {} is already disabled, nothing to do", user.getId());
			return;
		}

		String value = user.getFirstAttribute(attributeName);
		if (value == null || value.isBlank()) {
			log.debug("Attribute '{}' of user {} is not set or empty, nothing to do", attributeName, user.getId());
			return;
		}

		Instant expiry;
		try {
			expiry = parse(value.trim());
		} catch (DateTimeException e) {
			log.warn("Attribute '{}' of user {} is not an ISO date(time), user is left untouched", attributeName, user.getId());
			return;
		}

		if (expiry.isBefore(Instant.now())) {
			log.debug("Disabling user {}, attribute '{}' is in the past", user.getId(), attributeName);
			user.setEnabled(false);
		}
	}

	static Instant parse(String value) {
		ZoneId serverZone = ZoneId.systemDefault();
		if (value.indexOf('T') < 0) {
			return LocalDate.parse(value).atStartOfDay(serverZone).toInstant();
		}
		return ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME.withZone(serverZone)).toInstant();
	}

	@Override
	public void close() {
	}
}
