package dasniko.keycloak.email;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import software.amazon.awssdk.services.ses.SesClient;

import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@RequiredArgsConstructor
public class AwsSesEmailSenderProvider implements EmailSenderProvider {

	private final SesClient ses;

	@Override
	public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
		try {
			// textBody contains the serialized JSON document
			Map<String, Object> attributes = JsonSerialization.readValue(textBody, new TypeReference<>() {});
			attributes.put("subject", subject);
			String templateName = (String) attributes.get("templateName");
			String templateData = JsonSerialization.writeValueAsString(attributes);

			ses.sendTemplatedEmail(builder -> builder
				.source(config.get("from"))
				.replyToAddresses(config.get("replyTo"))
				.destination(db -> db.toAddresses(address))
				.template(templateName)
				.templateData(templateData)
			);
		} catch (Exception e) {
			ServicesLogger.LOGGER.failedToSendEmail(e);
			throw new EmailException(e.getMessage(), e);
		}
	}

	@Override
	public void validate(Map<String, String> map) {
	}

	@Override
	public void close() {
	}

}
