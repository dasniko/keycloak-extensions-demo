package dasniko.keycloak.timer;

import com.google.auto.service.AutoService;
import org.keycloak.models.KeycloakSession;

@AutoService(ScheduledTaskProviderFactory.class)
public class ExampleScheduledTaskProviderFactory extends ScheduledTaskProviderFactory {

	public static final String PROVIDER_ID = "example";

	@Override
	public ScheduledTaskProvider create(KeycloakSession session) {
		return new ExampleScheduledTaskProvider();
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

}
