package dasniko.keycloak.timer;

import com.google.auto.service.AutoService;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

@AutoService(Spi.class)
public class ScheduledTaskSpi implements Spi {
	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public String getName() {
		return "scheduled-task";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return ScheduledTaskProvider.class;
	}

	@Override
	public Class<? extends ProviderFactory<ScheduledTaskProvider>> getProviderFactoryClass() {
		return ScheduledTaskProviderFactory.class;
	}
}
