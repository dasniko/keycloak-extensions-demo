package dasniko.keycloak.resource.admin;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@Testcontainers
public class MyAdminRealmResourceProviderTest {

	@Container
	private static final KeycloakContainer keycloak =
		new KeycloakContainer().withProviderClassesFrom("target/classes");

	@Test
	public void testEndpoint() {
		Keycloak keycloakClient = keycloak.getKeycloakAdminClient();
		AccessTokenResponse accessTokenResponse = keycloakClient.tokenManager().getAccessToken();

		given().baseUri(keycloak.getAuthServerUrl())
			.basePath("/admin/realms/master/" + MyAdminRealmResourceProvider.PROVIDER_ID)
			.auth().oauth2(accessTokenResponse.getToken())
			.when().get("users")
			.then().statusCode(200)
			.body("size()", is(1));
	}

}
