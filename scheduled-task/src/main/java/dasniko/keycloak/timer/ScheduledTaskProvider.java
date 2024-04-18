package dasniko.keycloak.timer;

import org.keycloak.provider.Provider;
import org.keycloak.timer.ScheduledTask;

public interface ScheduledTaskProvider extends Provider {

	ScheduledTask getScheduledTask();

	@Override
	default void close() {
	}

}
