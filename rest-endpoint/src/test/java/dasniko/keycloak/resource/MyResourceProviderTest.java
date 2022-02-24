package dasniko.keycloak.resource;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@Testcontainers
public class MyResourceProviderTest {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer().withProviderClassesFrom("target/classes");

	@Test
	public void testAnonymousEndpoint() {
		givenSpec()
			.when().get("hello")
			.then().statusCode(200)
			.body("hello", is("master"));
	}

	@Test
	public void testAuthenticatedEndpoint() {
		Keycloak keycloakClient = keycloak.getKeycloakAdminClient();
		AccessTokenResponse accessTokenResponse = keycloakClient.tokenManager().getAccessToken();

		givenSpec()
			.auth().oauth2(accessTokenResponse.getToken())
			.when().get("hello-auth")
			.then().statusCode(200)
			.body("hello", is("admin"));
	}

	@Test
	public void testAuthenticatedEndpointUnauthenticated() {
		givenSpec()
			.when().get("hello-auth")
			.then().statusCode(401);
	}

	private RequestSpecification givenSpec() {
		return given().baseUri(keycloak.getAuthServerUrl()).basePath("/realms/master/" + MyResourceProviderFactory.PROVIDER_ID);
	}

}
