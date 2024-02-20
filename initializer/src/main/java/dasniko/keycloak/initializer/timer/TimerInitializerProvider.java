package dasniko.keycloak.initializer.timer;

import com.google.auto.service.AutoService;
import dasniko.keycloak.initializer.InitializerProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

@Slf4j
@AutoService(InitializerProviderFactory.class)
public class TimerInitializerProvider implements InitializerProviderFactory {

	public static final String PROVIDER_ID = "timer";

	private static Long interval;

	@Override
	public void init(Config.Scope config) {
		interval = config.getLong("interval", 60000L);
	}
	@Override
	public void postInit(KeycloakSessionFactory factory) {
		KeycloakModelUtils.runJobInTransaction(factory, session -> {
			TimerProvider timerProvider = session.getProvider(TimerProvider.class);
			timerProvider.schedule(() -> log.info("Timer is being executed..."), interval, PROVIDER_ID);
		});
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
