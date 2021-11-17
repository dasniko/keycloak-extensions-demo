package dasniko.keycloak.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class DemoUserStorageProviderFactory implements UserStorageProviderFactory<DemoUserStorageProvider> {

	public static final String PROVIDER_ID = "demo-user-provider";

	@Override
	public DemoUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		// here you can setup the user storage provider, initiate some connections, etc.
		DemoRepository repository = new DemoRepository();
		return new DemoUserStorageProvider(session, model, repository);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return ProviderConfigurationBuilder.create()
			.property("myParam", "My Param", "Some Description", ProviderConfigProperty.STRING_TYPE, "some value", null)
			.build();
	}
}
