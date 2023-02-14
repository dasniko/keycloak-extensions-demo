package dasniko.keycloak.events;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
public class LastLoginTimeListenerTest {

	private static final String REALM = "demo";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmImportFile("demo-realm.json")
		.withProviderClassesFrom("target/classes");

	@Test
	public void testLastLoginTime() {
		Keycloak admin = keycloak.getKeycloakAdminClient();

		// check user has no attributes
		List<UserRepresentation> users = admin.realm(REALM).users().searchByUsername("test", true);
		UserRepresentation testUser = users.get(0);
		Map<String, List<String>> attributes = testUser.getAttributes();
		assertNull(attributes);

		// configure custom events listener
		RealmEventsConfigRepresentation eventsConfig = new RealmEventsConfigRepresentation();
		eventsConfig.setEventsListeners(List.of("jboss-logging", "last-login-time"));
		admin.realm(REALM).updateRealmEventsConfig(eventsConfig);

		// "login" user
		String tokenEndpoint = given().when().get(keycloak.getAuthServerUrl() + "realms/" + REALM + "/.well-known/openid-configuration")
			.then().statusCode(200).extract().path("token_endpoint");
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("username", "test")
			.formParam("password", "test")
			.formParam("grant_type", "password")
			.formParam("client_id", KeycloakContainer.ADMIN_CLI_CLIENT)
			.formParam("scope", "openid")
			.when().post(tokenEndpoint);

		// check user has last-login-time attribute
		testUser = admin.realm(REALM).users().searchByUsername("test", true).get(0);
		String lastLoginTime = testUser.firstAttribute("lastLoginTime");
		assertNotNull(lastLoginTime);
	}

}
