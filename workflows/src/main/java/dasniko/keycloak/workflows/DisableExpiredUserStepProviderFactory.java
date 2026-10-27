package dasniko.keycloak.workflows;

import com.google.auto.service.AutoService;
import de.keycloak.provider.DefaultServerInfoAware;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowStepProviderFactory;

import java.util.Set;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(WorkflowStepProviderFactory.class)
public class DisableExpiredUserStepProviderFactory implements WorkflowStepProviderFactory<DisableExpiredUserStepProvider>, DefaultServerInfoAware {

	public static final String PROVIDER_ID = "disable-expired-user";

	@Override
	public DisableExpiredUserStepProvider create(KeycloakSession session, ComponentModel model) {
		return new DisableExpiredUserStepProvider(session, model);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public Set<ResourceType> getSupportedResourceTypes() {
		return Set.of(ResourceType.USERS);
	}

	@Override
	public String getHelpText() {
		return "Disables the user if the date(time) in the configured attribute lies in the past";
	}
}
