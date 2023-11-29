package dasniko.keycloak.resource.admin;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

@AutoService(AdminRealmResourceProviderFactory.class)
public class MyAdminRealmResourceProviderFactory implements AdminRealmResourceProviderFactory, EnvironmentDependentProviderFactory {

	public static final String PROVIDER_ID = "my-admin-rest-resource";

	@Override
	public AdminRealmResourceProvider create(KeycloakSession session) {
		return new MyAdminRealmResourceProvider();
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public boolean isSupported() {
		return Profile.isFeatureEnabled(Profile.Feature.ADMIN2);
	}

}
