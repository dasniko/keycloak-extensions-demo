package dasniko.keycloak.events;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import software.amazon.awssdk.services.sns.SnsClient;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class AwsSnsEventListenerProvider implements EventListenerProvider {

    private static final String ENVVAR_TOPICARN = "AWS_EVENTS_SNS_TOPICARN";

    private final SnsClient sns;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::sendAdminEvent, this::sendEvent);

    public AwsSnsEventListenerProvider(KeycloakSession session, SnsClient sns) {
        this.sns = sns;
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

    private void sendEvent(Event event) {
        publish(event);
    }

    private void sendAdminEvent(AdminEvent event, boolean includeRepresentation) {
        publish(event);
    }

    private String getTopicArn() {
        return System.getenv(ENVVAR_TOPICARN);
    }

    private void publish(Object event) {
        if (getTopicArn() == null) {
            log.warn("No topicArn specified. Can not send event to AWS SNS! Set environment variable {}", ENVVAR_TOPICARN);
            return;
        }

        String payload;
        try {
            payload = JsonSerialization.writeValueAsString(event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        sns.publish(builder -> builder.topicArn(getTopicArn()).message(payload));
    }
}
