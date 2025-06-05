package dasniko.keycloak.events;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
public class LastLoginTimeListenerTest extends TestBase {

	private static final String REALM = "demo";

	@ParameterizedTest
	@ValueSource(strings = {"26.2", "nightly"})
	public void testLastLoginTime(String tag) {
		KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:" + tag)
			.withImagePullPolicy(PullPolicy.alwaysPull())
			.withRealmImportFile("demo-realm.json")
			.withDefaultProviderClasses();

		try {
			keycloak.start();

			Keycloak admin = keycloak.getKeycloakAdminClient();

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
			assertNotNull(testUser.getAttributes(), "No user attributes");
			String lastLoginTime = testUser.firstAttribute("lastLoginTime");
			assertNotNull(lastLoginTime, "No last login time");
		} finally {
			keycloak.stop();
		}
	}

}
