package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.FlintstonesRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesUserStorageProviderFactory implements UserStorageProviderFactory<FlintstonesUserStorageProvider> {

	public static final String PROVIDER_ID = "the-flintstones";

	private FlintstonesRepository repository;

	@Override
	public FlintstonesUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		return new FlintstonesUserStorageProvider(session, model, repository);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		repository = new FlintstonesRepository();
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return ProviderConfigurationBuilder.create()
			.property("myParam", "My Param", "Some Description", ProviderConfigProperty.STRING_TYPE, "some value", null)
			.build();
	}
}
