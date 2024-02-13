package dasniko.keycloak.initializer.realm;

import com.google.auto.service.AutoService;
import dasniko.keycloak.initializer.InitializerProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.provider.ProviderEvent;

@Slf4j
@AutoService(InitializerProviderFactory.class)
public class RealmInitializerProvider implements InitializerProviderFactory {

	public static final String PROVIDER_ID = "kcInitializer";

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		factory.register(
			(ProviderEvent providerEvent) -> {
				if (providerEvent instanceof RealmModel.RealmPostCreateEvent event) {
					RealmModel createdRealm = event.getCreatedRealm();
					if (createdRealm.getName().equalsIgnoreCase("master")) {
						return;
					}

					AuthenticationFlows.createAuthFlows(createdRealm);

					final String ROLE_USER = "user";
					RoleModel userRole = createdRealm.getRole(ROLE_USER);
					if (userRole == null) {
						log.info("Create role {} in realm {}, as it doesn't exist.", ROLE_USER, createdRealm.getName());
						userRole = createdRealm.addRole(ROLE_USER);
					}
					log.info("Add role {} to default roles of realm {}", ROLE_USER, createdRealm.getName());
					createdRealm.addToDefaultRoles(userRole);
				}
			});
	}

}
