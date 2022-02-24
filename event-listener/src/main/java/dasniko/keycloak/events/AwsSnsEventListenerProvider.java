package dasniko.keycloak.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class AwsSnsEventListenerProvider implements EventListenerProvider {

    private static final String ENVVAR_TOPICARN = "AWS_EVENTS_SNS_TOPICARN";

    private final SnsClient sns;
    private final ObjectMapper mapper;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::sendAdminEvent, this::sendEvent);

    public AwsSnsEventListenerProvider(KeycloakSession session, SnsClient sns, ObjectMapper mapper) {
        this.sns = sns;
        this.mapper = mapper;

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

        String payload = null;
        try {
            payload = mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String message = payload;
        sns.publish(builder -> builder.topicArn(getTopicArn()).message(message));
    }
}
