package de.keycloak.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.ValidatableResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

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
}
