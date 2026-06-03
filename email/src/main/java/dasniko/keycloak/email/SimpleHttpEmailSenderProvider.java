package dasniko.keycloak.email;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@RequiredArgsConstructor
public class SimpleHttpEmailSenderProvider implements EmailSenderProvider {

	private final KeycloakSession session;

	@Override
	public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
		try {
			Map<String, Object> payload = JsonSerialization.readValue(textBody, new TypeReference<>() {});
			int status = SimpleHttp.create(session)
				.doPost(SimpleHttpEmailSenderProviderFactory.url)
				.json(payload).asStatus();
			if (status != 200) {
				throw new RuntimeException("Failed to send email.");
			}
		} catch (Exception e) {
			ServicesLogger.LOGGER.error(e);
			throw new EmailException("Error when attempting to send the email to the server. More information is available in the server log.", e);
		}
	}

	@Override
	public void validate(Map<String, String> map) {
	}

	@Override
	public void close() {
	}

}
