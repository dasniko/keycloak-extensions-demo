package dasniko.keycloak.events;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
public class LastLoginTimeListenerTest extends TestBase {

	private static final String REALM = "demo";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmImportFile("demo-realm.json")
		.withNightly()
		.withEnv("KC_SPI_EVENTS_LISTENER_LAST_LOGIN_TIME_ATTRIBUTE_NAME", "lastLogin")
		.withProviderClassesFrom("target/classes");

	@Test
	public void testLastLoginTime() {
		Keycloak admin = keycloak.getKeycloakAdminClient();

		// enable unmanaged attributes to be able to fetch the custom user attribute
		UPConfig upConfig = admin.realm(REALM).users().userProfile().getConfiguration();
		upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
		admin.realm(REALM).users().userProfile().update(upConfig);

		// check user has no attributes
		List<UserRepresentation> users = admin.realm(REALM).users().searchByUsername("test", true);
		UserRepresentation testUser = users.getFirst();
		Map<String, List<String>> attributes = testUser.getAttributes();
		assertNull(attributes);

		// configure custom events listener
		RealmEventsConfigRepresentation eventsConfig = new RealmEventsConfigRepresentation();
		eventsConfig.setEventsListeners(List.of(LastLoginTimeListenerFactory.PROVIDER_ID));
		admin.realm(REALM).updateRealmEventsConfig(eventsConfig);

		// "login" user
		requestToken(keycloak, REALM, "test", "test");

		// check user has last-login-time attribute
		testUser = admin.realm(REALM).users().searchByUsername("test", true).getFirst();
		String lastLoginTime = testUser.firstAttribute("lastLogin");
		assertNotNull(lastLoginTime);
	}

}
