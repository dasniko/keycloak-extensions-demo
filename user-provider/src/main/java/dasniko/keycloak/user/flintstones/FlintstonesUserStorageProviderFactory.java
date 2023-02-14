package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.FlintstonesRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesUserStorageProviderFactory implements UserStorageProviderFactory<FlintstonesUserStorageProvider> {

    @Override
    public FlintstonesUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        // here you can setup the user storage provider, initiate some connections, etc.

        FlintstonesRepository repository = new FlintstonesRepository();

        return new FlintstonesUserStorageProvider(session, model, repository);
    }

    @Override
    public String getId() {
        return "the-flintstones";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property("myParam", "My Param", "Some Description", ProviderConfigProperty.STRING_TYPE, "some value", null)
                .build();
    }
}
