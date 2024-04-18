package dasniko.keycloak.timer;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.timer.ScheduledTask;

@Slf4j
public class ExampleScheduledTaskProvider implements ScheduledTaskProvider {

	@Override
	public ScheduledTask getScheduledTask() {
		return session -> {
			//noinspection deprecation
			ClusterProvider cluster = session.getProvider(ClusterProvider.class);
			cluster.executeIfNotExecuted(ExampleScheduledTaskProviderFactory.PROVIDER_ID + "::scheduled", 10000, () -> {
				log.info("I'm being executed... realm: {}", session.getContext().getRealm());
				return null;
			});
		};
	}

}
