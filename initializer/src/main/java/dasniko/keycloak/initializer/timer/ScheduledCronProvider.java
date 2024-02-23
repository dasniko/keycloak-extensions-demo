package dasniko.keycloak.initializer.timer;

import com.google.auto.service.AutoService;
import dasniko.keycloak.initializer.InitializerProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;

@Slf4j
@AutoService(InitializerProviderFactory.class)
public class ScheduledCronProvider extends AbstractCronProvider {

	public static final String PROVIDER_ID = "timer";

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	void run(KeycloakSession session) {
		log.info("Timer task is being executed...");
	}

}
