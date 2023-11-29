package dasniko.keycloak.user.flintstones;

import com.google.auto.service.AutoService;
import dasniko.keycloak.user.flintstones.repo.FlintstonesApiServer;
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
@AutoService(UserStorageProviderFactory.class)
public class FlintstonesUserStorageProviderFactory implements UserStorageProviderFactory<FlintstonesUserStorageProvider> {

	public static final String PROVIDER_ID = "the-flintstones";

	static final String USER_API_BASE_URL = "apiBaseUrl";
	static final String USER_CREATION_ENABLED = "userCreation";
	static final String USE_PASSWORD_POLICY = "usePasswordPolicy";

	private FlintstonesApiServer apiServer;

	@Override
	public FlintstonesUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		FlintstonesApiClient apiClient = new FlintstonesApiClient(session, model);
		return new FlintstonesUserStorageProvider(session, model, apiClient);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		apiServer = new FlintstonesApiServer();
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return ProviderConfigurationBuilder.create()
			.property(USER_API_BASE_URL, "API Base URL", "", ProviderConfigProperty.STRING_TYPE, "http://localhost:8000", null)
			.property(USER_CREATION_ENABLED, "Sync Registrations", "Should newly created users be created within this store?", ProviderConfigProperty.BOOLEAN_TYPE, "false", null)
			.property(USE_PASSWORD_POLICY, "Validate password policy", "Determines if Keycloak should validate the password with the realm password policy before updating it.", ProviderConfigProperty.BOOLEAN_TYPE, "false", null)
			.build();
	}

	@Override
	public void close() {
		if (apiServer != null) {
			apiServer.stop();
		}
	}
}
