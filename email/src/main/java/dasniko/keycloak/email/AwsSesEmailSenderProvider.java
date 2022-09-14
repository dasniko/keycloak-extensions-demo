package dasniko.keycloak.email;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.services.ServicesLogger;
import software.amazon.awssdk.services.ses.SesClient;

import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@RequiredArgsConstructor
public class AwsSesEmailSenderProvider implements EmailSenderProvider {

	private final SesClient ses;
	private final ObjectMapper mapper;

	@Override
	public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
		try {
			// textBody contains the serialized JSON document
			Map<String, Object> attributes = mapper.readValue(textBody, new TypeReference<>() {});
			attributes.put("subject", subject);
			String templateName = (String) attributes.get("templateName");
			String templateData = mapper.writeValueAsString(attributes);

			ses.sendTemplatedEmail(builder -> builder
				.source(config.get("from"))
				.replyToAddresses(config.get("replyTo"))
				.destination(db -> db.toAddresses(address))
				.template(templateName)
				.templateData(templateData)
			);
		} catch (Exception e) {
			ServicesLogger.LOGGER.failedToSendEmail(e);
			throw new EmailException(e);
		}
	}

	@Override
	public void close() {
	}

}
