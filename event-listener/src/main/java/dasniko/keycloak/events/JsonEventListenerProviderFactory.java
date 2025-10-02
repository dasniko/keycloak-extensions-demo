package dasniko.keycloak.events;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@AutoService(EventListenerProviderFactory.class)
public class JsonEventListenerProviderFactory implements EventListenerProviderFactory {

	public static final String PROVIDER_ID = "json-logging";

	private Level successLevel;
	private Level errorLevel;

	@Override
	public EventListenerProvider create(KeycloakSession keycloakSession) {
		return new JsonEventListenerProvider(keycloakSession, successLevel, errorLevel);
	}

	@Override
	public void init(Config.Scope config) {
		successLevel = Level.valueOf(config.get("success-level", "debug").toUpperCase());
		errorLevel = Level.valueOf(config.get("error-level", "warn").toUpperCase());
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public List<ProviderConfigProperty> getConfigMetadata() {
		String[] logLevels = Arrays.stream(Level.values())
			.map(Level::name)
			.map(String::toLowerCase)
			.sorted(Comparator.naturalOrder())
			.toArray(String[]::new);
		return ProviderConfigurationBuilder.create()
			.property()
			.name("success-level")
			.type("string")
			.helpText("The log level for success messages.")
			.options(logLevels)
			.defaultValue("debug")
			.add()
			.property()
			.name("error-level")
			.type("string")
			.helpText("The log level for error messages.")
			.options(logLevels)
			.defaultValue("warn")
			.add()
			.build();
	}
}
