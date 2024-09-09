package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.utils.MediaType;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlintstonesUserStorageProviderTest extends TestBase {

	static final String REALM = "flintstones";
	static final String FRED = "fred";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
		.withEnv("KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_SUCCESS_LEVEL", "info")
		.withEnv("KC_LOG_LEVEL", "INFO,dasniko:debug")
		.withProviderClassesFrom("target/classes", "../utils/target/classes");

	@BeforeAll
	static void beforeAll() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();

		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm(REALM);
		realm.setEnabled(true);
		realm.setLoginWithEmailAllowed(true);
		realm.setResetPasswordAllowed(true);
		kcAdmin.realms().create(realm);

		keycloak.disableLightweightAccessTokenForAdminCliClient(REALM);

		ComponentRepresentation componentRep = new ComponentRepresentation();
		componentRep.setProviderId(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
		componentRep.setName(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
		componentRep.setProviderType(UserStorageProvider.class.getTypeName());

		MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
		config.add(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL, "http://localhost:8000");
		config.add(FlintstonesUserStorageProviderFactory.USER_CREATION_ENABLED, "true");
		config.add("enabled", "true");
		componentRep.setConfig(config);

		kcAdmin.realm(REALM).components().add(componentRep).close();
	}


	@Order(1)
	@ParameterizedTest
	@ValueSource(strings = {KeycloakContainer.MASTER_REALM, REALM})
	public void testRealms(String realm) {
		String accountServiceUrl = given().pathParam("realm-name", realm)
			.when().get(keycloak.getAuthServerUrl() + ServiceUrlConstants.REALM_INFO_PATH)
			.then().statusCode(200).body("realm", equalTo(realm))
			.extract().path("account-service");

		given().when().get(accountServiceUrl).then().statusCode(200);
	}

	@Order(2)
	@ParameterizedTest
	@ValueSource(strings = {"fred.flintstone@flintstones.com", FRED})
	public void testLoginAsUserAndCheckAccessToken(String userIdentifier) throws IOException {
		String accessTokenString = requestToken(keycloak, REALM, userIdentifier, "fred", 200)
			.extract().path("access_token");

		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

		byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
		Map<String, Object> payload = mapper.readValue(tokenPayload, typeRef);

		assertThat(payload.get("preferred_username"), is(FRED));
		assertThat(payload.get("email"), is("fred.flintstone@flintstones.com"));
		assertThat(payload.get("given_name"), is("Fred"));
		assertThat(payload.get("family_name"), is("Flintstone"));
	}

	@Test
	@Order(3)
	public void testLoginAsUserWithInvalidPassword() {
		requestToken(keycloak, REALM, FRED, "invalid", 401);
	}

	@Test
	@Order(4)
	public void testUpdatePassword() {
		// call update password action directly
		String authEndpoint = getOpenIDConfiguration(keycloak, REALM).extract().path("authorization_endpoint");
		ExtractableResponse<Response> response = given()
			.queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
			.queryParam(OAuth2Constants.CLIENT_ID, "account")
			.queryParam(OAuth2Constants.REDIRECT_URI, keycloak.getAuthServerUrl() + "/realms/" + REALM + "/account")
			.queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
			.queryParam("kc_action", "UPDATE_PASSWORD")
			.when().get(authEndpoint)
			.then().statusCode(200).extract();
		Map<String, String> cookies = response.cookies();
		String formUrl = response.htmlPath().getString("**.find { it.@id == 'kc-form-login' }.@action");

		// authenticate
		String location = given().cookies(cookies)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.formParam(OAuth2Constants.USERNAME, FRED)
			.formParam(OAuth2Constants.PASSWORD, "fred")
			.when().post(formUrl)
			.then().statusCode(302)
			.extract().header(HttpHeaders.LOCATION);

		// get form for password update
		formUrl = given().cookies(cookies)
			.when().get(location)
			.then().statusCode(200)
			.extract().htmlPath().getString("**.find { it.@id == 'kc-passwd-update-form' }.@action");

		// update password
		given().cookies(cookies)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.formParam(OAuth2Constants.USERNAME, FRED)
			.formParam("password-new", "changed")
			.formParam("password-confirm", "changed")
			.when().post(formUrl)
			.then().statusCode(302)
			.extract().header(HttpHeaders.LOCATION);

		// test new password
		requestToken(keycloak, REALM, FRED, "changed", 200);
	}

	@Test
	@Order(5)
	public void testAccessingUsersAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.searchByUsername("fred", true);
		assertThat(users, is(not(empty())));
		assertThat(users, hasSize(1));

		String userId = users.getFirst().getId();
		UserResource userResource = usersResource.get(userId);
		assertThat(userResource.toRepresentation().getUsername(), is(FRED));
	}

	@Test
	@Order(6)
	public void testSearchAllUsersAndRemoveUserAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.search("*", 0, 10);
		assertThat(users, is(not(empty())));
		assertThat(users, hasSize(6));

		usersResource.delete(users.getLast().getId()).close();

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

		CredentialRepresentation cred = new CredentialRepresentation();
		cred.setType(CredentialRepresentation.PASSWORD);
		cred.setValue("mr.");
		String userId = usersResource.searchByUsername("mr.slate", true).getFirst().getId();
		usersResource.get(userId).resetPassword(cred);

		requestToken(keycloak, REALM, "mr.slate", "mr.", 200);
	}

	@Test
	@Order(7)
	public void testUpdateUserAsAdmin() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();
		List<UserRepresentation> users = usersResource.searchByUsername("wilma", true);
		assertThat(users, hasSize(1));

		UserRepresentation wilma = users.getFirst();
		wilma.setLastName("Feuerstein");

		usersResource.get(wilma.getId()).update(wilma);

		UserRepresentation updatedWilma = usersResource.get(wilma.getId()).toRepresentation();
		assertThat(updatedWilma.getLastName(), is("Feuerstein"));
	}

}
