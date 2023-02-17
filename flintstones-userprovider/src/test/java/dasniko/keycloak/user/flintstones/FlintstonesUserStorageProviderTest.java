package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
@Testcontainers
@TestMethodOrder(MethodOrderer.MethodName.class)
public class FlintstonesUserStorageProviderTest {

	static final String REALM = "flintstones";

	static final String FRED_FLINTSTONE = "fred.flintstone";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmImportFile("/flintstones-realm.json")
		.withEnv("KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_SUCCESS_LEVEL", "info")
		.withProviderClassesFrom("target/classes");


	@ParameterizedTest
	@ValueSource(strings = {KeycloakContainer.MASTER_REALM, REALM})
	public void testRealms(String realm) {
		String accountServiceUrl = given().pathParam("realm", realm)
			.when().get(keycloak.getAuthServerUrl() + "realms/{realm}")
			.then().statusCode(200).body("realm", equalTo(realm))
			.extract().path("account-service");

		given().when().get(accountServiceUrl).then().statusCode(200);
	}

	@ParameterizedTest
	@ValueSource(strings = {"fred.flintstone@flintstones.com", FRED_FLINTSTONE})
	public void testLoginAsUserAndCheckAccessToken(String userIdentifier) throws IOException {
		String accessTokenString = requestToken(userIdentifier, "fred")
			.then().statusCode(200).extract().path("access_token");

		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

		byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
		Map<String, Object> payload = mapper.readValue(tokenPayload, typeRef);

		assertThat(payload.get("preferred_username"), is(FRED_FLINTSTONE));
		assertThat(payload.get("email"), is("fred.flintstone@flintstones.com"));
		assertThat(payload.get("given_name"), is("Fred"));
		assertThat(payload.get("family_name"), is("Flintstone"));
	}

	@Test
	public void testLoginAsUserWithInvalidPassword() {
		requestToken(FRED_FLINTSTONE, "invalid").then().statusCode(401);
	}

	@Test
	public void testUpdatePassword() {
		// call update password action directly
		String authEndpoint = getOpenIDConfiguration().extract().path("authorization_endpoint");
		ExtractableResponse<Response> response = given()
			.queryParam("response_type", "code")
			.queryParam("client_id", "account")
			.queryParam("redirect_uri", keycloak.getAuthServerUrl() + "realms/" + REALM + "/account")
			.queryParam("scope", "openid")
			.queryParam("kc_action", "UPDATE_PASSWORD")
			.when().get(authEndpoint)
			.then().statusCode(200).extract();
		Map<String, String> cookies = response.cookies();
		String formUrl = response.htmlPath().getString("html.body.div.div.div.div.div.div.form.@action");

		// authenticate
		String location = given().cookies(cookies)
			.contentType("application/x-www-form-urlencoded")
			.formParam("username", FRED_FLINTSTONE)
			.formParam("password", "fred")
			.when().post(formUrl)
			.then().statusCode(302)
			.extract().header("Location");

		// get form for password update
		formUrl = given().cookies(cookies)
			.when().get(location)
			.then().statusCode(200)
			.extract().htmlPath().getString("html.body.div.div.div.div.form.@action");

		// update password
		given().cookies(cookies)
			.contentType("application/x-www-form-urlencoded")
			.formParam("username", FRED_FLINTSTONE)
			.formParam("password-new", "changed")
			.formParam("password-confirm", "changed")
			.when().post(formUrl)
			.then().statusCode(302)
			.extract().header("Location");

		// test new password
		requestToken(FRED_FLINTSTONE, "changed").then().statusCode(200);
	}

	@Test
	public void testAccessingUsersAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.search("fred", 0, 10);
		assertThat(users, is(not(empty())));
		assertThat(users, hasSize(1));

		String userId = users.get(0).getId();
		UserResource userResource = usersResource.get(userId);
		assertThat(userResource.toRepresentation().getUsername(), is(FRED_FLINTSTONE));
	}

	@Test
	public void testSearchAllUsersAndRemoveUserAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.search("*", 0, 10);
		assertThat(users, is(not(empty())));
		assertThat(users, hasSize(6));

		usersResource.delete(users.get(users.size() - 1).getId()).close();

		users = usersResource.search("*", 0 , 10);
		assertThat(users, hasSize(5));

		UserRepresentation newUser = new UserRepresentation();
		newUser.setUsername("mr.slate");
		newUser.setFirstName("Mr.");
		newUser.setLastName("Slate");
		newUser.setEmail("mr.slate@stonequarry.com");
		newUser.setEnabled(true);
		usersResource.create(newUser).close();

		users = usersResource.search("*", 0 , 10);
		assertThat(users, hasSize(6));

		requestToken("mr.slate", "mr.").then().statusCode(200);
	}

	@Test
	public void testUpdateUserAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.search("wilma", 0, 10);
		assertThat(users, hasSize(1));

		String wilmaId = users.get(0).getId();
		UserRepresentation updated = new UserRepresentation();
		updated.setLastName("Feuerstein");

		usersResource.get(wilmaId).update(updated);

		UserRepresentation updatedWilma = usersResource.get(wilmaId).toRepresentation();
		assertThat(updatedWilma.getLastName(), is("Feuerstein"));
	}

	private Response requestToken(String username, String password) {
		String tokenEndpoint = getOpenIDConfiguration().extract().path("token_endpoint");
		return given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("username", username)
			.formParam("password", password)
			.formParam("grant_type", "password")
			.formParam("client_id", KeycloakContainer.ADMIN_CLI_CLIENT)
			.formParam("scope", "openid")
			.when().post(tokenEndpoint);
	}

	private ValidatableResponse getOpenIDConfiguration() {
		return given().pathParam("realm", REALM)
			.when().get(keycloak.getAuthServerUrl() + "realms/{realm}/.well-known/openid-configuration")
			.then().statusCode(200);
	}

}
