package dasniko.keycloak.actiontoken;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static io.restassured.RestAssured.given;

@Testcontainers
public class CustomActionTokenTest {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
		.withDefaultProviderClasses();

	@BeforeAll
	static void beforeAll() {
		Keycloak admin = keycloak.getKeycloakAdminClient();
		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm("demo");
		realm.setEnabled(true);
		realm.setRegistrationEmailAsUsername(true);
		admin.realms().create(realm);
	}

	@Test
	public void testCustomActionToken() throws IOException {
		Keycloak admin = keycloak.getKeycloakAdminClient();
		String accessTokenString = admin.tokenManager().getAccessTokenString();

		Map<String, Object> payload = Map.of(
			"email", "test@keycloak.de",
			"clientId", "account-console",
			"redirectUri", keycloak.getAuthServerUrl() + "/realms/demo/account",
			"scope", "openid",
			"forceCreate", true
		);

		String actionTokenLink = given()
			.baseUri(keycloak.getAuthServerUrl())
			.basePath("/admin/realms/demo/" + CustomTokenResourceProvider.PROVIDER_ID)
			.auth().oauth2(accessTokenString)
			.contentType("application/json")
			.body(payload)
			.when().post()
			.then().statusCode(200)
			.extract().body().path("link");

		URL url = new URL(actionTokenLink);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setInstanceFollowRedirects(false);
		int responseCode = con.getResponseCode();
		Assertions.assertEquals(302, responseCode);
		con.disconnect();
	}

}
