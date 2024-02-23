package dasniko.keycloak.initializer.timer;

import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import dasniko.keycloak.initializer.InitializerProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.cronutils.model.CronType.UNIX;

@Slf4j
public abstract class AbstractCronProvider implements InitializerProviderFactory {

	private static boolean enabled;
	private static String cronExpression;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private Optional<ZonedDateTime> nextExecution = Optional.empty();

	@Override
	public void init(Config.Scope config) {
		enabled = config.getBoolean("enabled", false);
		cronExpression = config.get("cron-expression", "*/1 * * * *"); // UNIX format, every minute
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		log.info("{} '{}' is {}.", this.getClass().getSimpleName(), getId(), enabled ? "enabled" : "disabled");
		if (!enabled) {
			return;
		}

		CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(UNIX);
		CronParser parser = new CronParser(cronDefinition);
		ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
		nextExecution = executionTime.nextExecution(ZonedDateTime.now());

		KeycloakModelUtils.runJobInTransaction(factory, session -> {
			TimerProvider timerProvider = session.getProvider(TimerProvider.class);
			timerProvider.schedule(() -> {
				if (nextExecution.isPresent()) {
					ZonedDateTime next = nextExecution.get();
					Optional<ZonedDateTime> nextAfterExecution = executionTime.nextExecution(next);
					if (nextAfterExecution.isPresent()) {
						ZonedDateTime now = ZonedDateTime.now();
						ZonedDateTime nextAfter = nextAfterExecution.get();
						if (now.isAfter(next) && now.isBefore(nextAfter)) {
							KeycloakModelUtils.runJobInTransaction(factory, this::run);
							nextExecution = executionTime.nextExecution(now);
						}
					}
				}
			}, 30000, getId());
		});
	}

	abstract void run(KeycloakSession session);

}
