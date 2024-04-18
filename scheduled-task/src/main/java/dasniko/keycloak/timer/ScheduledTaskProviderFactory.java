package dasniko.keycloak.timer;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.timer.TimerProvider;

@Slf4j
public abstract class ScheduledTaskProviderFactory implements ProviderFactory<ScheduledTaskProvider> {

	protected static int intervalSeconds = -1;

	protected static final String CONFIG_INTERVAL_SECONDS = "intervalSeconds";
	protected static final int CONFIG_INTERVAL_SECONDS_DEFAULT = -1;

	private KeycloakSessionFactory keycloakSessionFactory;

	@Override
	public void init(Config.Scope config) {
		intervalSeconds = config.getInt(CONFIG_INTERVAL_SECONDS, CONFIG_INTERVAL_SECONDS_DEFAULT);
	}

	@Override
	public final void postInit(KeycloakSessionFactory keycloakSessionFactory) {
		this.keycloakSessionFactory = keycloakSessionFactory;

		if (intervalSeconds <= 0) {
			log.info("Scheduled task {} provider is disabled.", getId());
			return;
		}

		keycloakSessionFactory.register((event) -> {
			if (event instanceof PostMigrationEvent) {
				KeycloakSession session = keycloakSessionFactory.create();
				TimerProvider provider = session.getProvider(TimerProvider.class);
				ScheduledTaskProvider stp = create(session);
				provider.scheduleTask(stp.getScheduledTask(), getInterval(), getId());
			}
		});
	}

	@Override
	public final void close() {
		try (KeycloakSession session = keycloakSessionFactory.create()) {
			session.getProvider(TimerProvider.class).cancelTask(getId());
		}
	}

	protected int getInterval() {
		return intervalSeconds * 1000;
	}

}
