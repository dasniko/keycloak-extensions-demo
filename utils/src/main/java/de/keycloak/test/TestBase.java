package de.keycloak.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.ValidatableResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@SuppressWarnings({"unused", "SameParameterValue", "UnusedReturnValue"})
public class TestBase {

	protected final ObjectMapper mapper = new ObjectMapper();
	protected final TypeReference<HashMap<String, Object>> mapTypeRef = new TypeReference<>() {};

	protected ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String username, String password) {
		return requestToken(keycloak, realm, username, password, 200);
	}

	protected ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String clientId, String username, String password) {
		return requestToken(keycloak, realm, clientId, username, password, 200);
	}

	protected ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String username, String password, int expectedStatusCode) {
		return requestToken(keycloak, realm, KeycloakContainer.ADMIN_CLI_CLIENT, username, password, expectedStatusCode);
	}

	protected ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String clientId, String username, String password, int expectedStatusCode) {
		String tokenEndpoint = getOpenIDConfiguration(keycloak, realm)
			.extract().path("token_endpoint");
		return given()
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.formParam(OAuth2Constants.USERNAME, username)
			.formParam(OAuth2Constants.PASSWORD, password)
			.formParam(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
			.formParam(OAuth2Constants.CLIENT_ID, clientId)
			.formParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
			.when().post(tokenEndpoint)
			.then().statusCode(expectedStatusCode);
	}

	protected ValidatableResponse getOpenIDConfiguration(KeycloakContainer keycloak, String realm) {
		return given().pathParam("realm-name", realm)
			.when().get(keycloak.getAuthServerUrl() + ServiceUrlConstants.DISCOVERY_URL)
			.then().statusCode(200);
	}

	protected Map<String, Object> parseToken(String token) throws IOException {
		byte[] tokenPayload = Base64.getDecoder().decode(token.split("\\.")[1]);
		return mapper.readValue(tokenPayload, mapTypeRef);
	}

	protected UserRepresentation getUser(Keycloak admin, String realm, String username) {
		List<UserRepresentation> users = admin.realm(realm).users().searchByUsername(username, true);
		assertThat(users.size(), not(equalTo(0)));
		return users.getFirst();
	}

	protected void updateUser(Keycloak admin, String realm, String username, Consumer<UserRepresentation> consumer) {
		UserRepresentation user = getUser(admin, realm, username);
		consumer.accept(user);
		admin.realm(realm).users().get(user.getId()).update(user);
	}

	protected static void initTestRealm(KeycloakContainer keycloak, String realmName, Consumer<RealmRepresentation> realmInitCustomizer, BiConsumer<Keycloak, RealmRepresentation> realmUpdater) {
		Keycloak admin = keycloak.getKeycloakAdminClient();

		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm(realmName);
		realm.setEnabled(true);

		realmInitCustomizer.accept(realm);

		admin.realms().create(realm);

		realmUpdater.accept(admin, realm);
	}

}
