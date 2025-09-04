package dasniko.keycloak.email;

import lombok.RequiredArgsConstructor;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.services.ServicesLogger;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.keycloak.utils.StringUtil.isBlank;
import static org.keycloak.utils.StringUtil.isNotBlank;

@RequiredArgsConstructor
public class AwsSesEmailSenderProviderAlternative implements EmailSenderProvider {

	private final SesClient ses;

	@Override
	public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {

		String from = config.get("from");
		String fromDisplayName = config.get("fromDisplayName");
		String replyTo = config.get("replyTo");
		String replyToDisplayName = config.get("replyToDisplayName");

		try {
			if (isBlank(from)) {
				throw new Exception("Missing 'from' email address.");
			}

			SendEmailRequest.Builder sendEmailRequest = SendEmailRequest.builder()
				.destination(
					Destination.builder().toAddresses(address).build()
				)
				.message(Message.builder()
					.subject(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(subject).build())
					.body(Body.builder()
						.html(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(htmlBody).build())
						.text(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(textBody).build())
						.build()
					)
					.build()
				)
				.source(buildEmailAddress(from, fromDisplayName));

			if (isNotBlank(replyTo)) {
				sendEmailRequest.replyToAddresses(
					List.of(buildEmailAddress(replyTo, replyToDisplayName)));
			}

			ses.sendEmail(sendEmailRequest.build());

		} catch (Exception e) {
			ServicesLogger.LOGGER.failedToSendEmail(e);
			throw new EmailException(e.getMessage(), e);
		}
	}

	@Override
	public void validate(Map<String, String> config) throws EmailException {
	}

	private String buildEmailAddress(String email, String displayName) {
		return isBlank(displayName) ? email : "%s <%s>".formatted(displayName, email);
	}

	@Override
	public void close() {
	}

}
