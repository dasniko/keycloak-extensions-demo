package dasniko.keycloak.user.flintstones;

import com.google.auto.service.AutoService;
import dasniko.keycloak.user.flintstones.repo.FlintstonesApiServer;
import de.keycloak.util.BuildDetails;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@AutoService(UserStorageProviderFactory.class)
public class FlintstonesUserStorageProviderFactory implements UserStorageProviderFactory<FlintstonesUserStorageProvider>, ServerInfoAwareProviderFactory {

	public static final String PROVIDER_ID = "the-flintstones";

	static final String USER_API_BASE_URL = "apiBaseUrl";
	static final String CLIENT_ID = "clientId";
	static final String EDIT_MODE = "editMode";
	static final String USER_IMPORT = "importUsers";
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
			.property(USER_API_BASE_URL, "apiBaseUrl", "apiBaseUrlHelp", ProviderConfigProperty.STRING_TYPE, "http://localhost:8000", null)
			.property(CLIENT_ID, "apiClientId", "apiClientIdHelp", ProviderConfigProperty.CLIENT_LIST_TYPE, "", null)
			.property(EDIT_MODE, "editMode", "editModeHelp", ProviderConfigProperty.LIST_TYPE, UserStorageProvider.EditMode.READ_ONLY, List.of(UserStorageProvider.EditMode.READ_ONLY.name(), UserStorageProvider.EditMode.WRITABLE.name()))
			.property(USER_IMPORT, "importUsers", "importUsersHelp", ProviderConfigProperty.BOOLEAN_TYPE, "false", null)
			.property(USER_CREATION_ENABLED, "syncRegistrations", "syncRegistrationsHelp", ProviderConfigProperty.BOOLEAN_TYPE, "false", null)
			.property(USE_PASSWORD_POLICY, "validatePasswordPolicy", "validatePasswordPolicyHelp", ProviderConfigProperty.BOOLEAN_TYPE, "false", null)
			.build();
	}

	@Override
	public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
		if (config.getId() == null) {
			config.setId(KeycloakModelUtils.generateShortId());
		}

		if (config.get(EDIT_MODE).equals(UserStorageProvider.EditMode.READ_ONLY.name()) && config.get(USER_CREATION_ENABLED, false)) {
			throw new ComponentValidationException("Cannot set 'syncRegistrations' to true if 'editMode' is set to 'READ_ONLY'");
		}

		if (config.get(EDIT_MODE).equals(UserStorageProvider.EditMode.READ_ONLY.name()) && config.get(USE_PASSWORD_POLICY, false)) {
			throw new ComponentValidationException("Cannot set 'validatePasswordPolicy' to true if 'editMode' is set to 'READ_ONLY'");
		}
	}

	@Override
	public void close() {
		if (apiServer != null) {
			apiServer.stop();
		}
	}

	@Override
	public Map<String, String> getOperationalInfo() {
		return BuildDetails.get();
	}
}
