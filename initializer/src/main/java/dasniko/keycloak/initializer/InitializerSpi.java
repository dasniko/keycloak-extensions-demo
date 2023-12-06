package dasniko.keycloak.initializer;

import com.google.auto.service.AutoService;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

@AutoService(Spi.class)
public class InitializerSpi implements Spi {
	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public String getName() {
		return "initializer";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return Provider.class;
	}

	@Override
	public Class<? extends ProviderFactory> getProviderFactoryClass() {
		return InitializerProviderFactory.class;
	}
}
