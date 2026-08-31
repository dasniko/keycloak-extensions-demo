package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.mappers.AttributeMappings;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import de.keycloak.test.pages.AccountManagementPage;
import de.keycloak.test.pages.LoginWithUsernameAndPasswordPage;
import de.keycloak.test.pages.UpdatePasswordPage;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlintstonesUserStorageProviderTest extends TestBase {

	static final String REALM = "flintstones";
	static final String FRED = "fred";
	static final String FRED_PICTURE = "https://dasniko-public.s3.eu-central-1.amazonaws.com/fred.png";
	// users only touched by their own test, so the mapping tests stay independent of the ordering above
	static final String BETTY = "betty";
	static final String BETTY_ID = "56789";
	static final String BARNEY = "barney";
	static final String BARNEY_ID = "45678";
	static final String PEBBLES = "pebbles";
	static final String PEBBLES_PICTURE = "https://dasniko-public.s3.eu-central-1.amazonaws.com/pebbles.png";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
		.withEnv("KC_SPI_EVENTS_LISTENER__JBOSS_LOGGING__SUCCESS_LEVEL", "info")
		.withEnv("KC_LOG_LEVEL", "INFO,dasniko:debug")
		.withProviderClassesFrom("target/classes", "../utils/target/classes");

	@BeforeAll
	static void beforeAll() {
		initTestRealm(keycloak, REALM,
			realm -> {
				realm.setLoginWithEmailAllowed(true);
				realm.setResetPasswordAllowed(true);
			},
			(kcAdmin, realmRep) -> {
				ClientRepresentation client = new ClientRepresentation();
				client.setEnabled(true);
				client.setClientId("api-client");
				client.setServiceAccountsEnabled(true);
				kcAdmin.realm(REALM).clients().create(client).close();

				ComponentRepresentation componentRep = new ComponentRepresentation();
				componentRep.setProviderId(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
				componentRep.setName(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
				componentRep.setProviderType(UserStorageProvider.class.getTypeName());

				MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
				config.add(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL, "http://localhost:8080/realms/master/flintstones");
				config.add(FlintstonesUserStorageProviderFactory.CLIENT_ID, "api-client");
				config.add(FlintstonesUserStorageProviderFactory.SUPPORTED_CREDENTIAL_TYPES, "password");
				config.add(FlintstonesUserStorageProviderFactory.USER_CREATION_ENABLED, "true");
				config.add(FlintstonesUserStorageProviderFactory.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
				config.add(AttributeMappings.CONFIG_KEY, """
					[
					  { "name": "picture", "field": "pictureUrl" },
					  { "name": "avatar", "field": "pictureUrl", "readOnly": true },
					  { "name": "phone", "field": "phoneNumbers", "multivalued": true },
					  { "name": "yearOfBirth", "field": "yearOfBirth", "type": "integer" }
					]""");
				config.add("enabled", "true");
				componentRep.setConfig(config);

				kcAdmin.realm(REALM).components().add(componentRep).close();
			}
		);

		keycloak.disableLightweightAccessTokenForAdminCliClient(REALM);
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
	@ValueSource(strings = {"fred.flintstone@bedrock.com", FRED})
	public void testLoginAsUserAndCheckAccessToken(String userIdentifier) throws IOException {
		String accessTokenString = requestToken(keycloak, REALM, userIdentifier, "fred", 200)
			.extract().path("access_token");

		byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
		Map<String, Object> payload = mapper.readValue(tokenPayload, new TypeReference<>() {});

		assertThat(payload.get("preferred_username"), is(FRED));
		assertThat(payload.get("email"), is("fred.flintstone@bedrock.com"));
		assertThat(payload.get("given_name"), is("Fred"));
		assertThat(payload.get("family_name"), is("Flintstone"));
	}

	@Test
	@Order(3)
	public void testLoginAsUserWithInvalidPassword() {
		requestToken(keycloak, REALM, FRED, "invalid", 400);
	}

	@Test
	@Order(4)
	public void testUpdatePassword() throws URISyntaxException, IOException {
		// call update password action directly
		String authEndpoint = getOpenIDConfiguration(keycloak, REALM).extract().path("authorization_endpoint");

		try (final WebClient webClient = new WebClient()) {
			URIBuilder startUri = new URIBuilder(authEndpoint)
				.addParameter(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
				.addParameter(OAuth2Constants.CLIENT_ID, "account")
				.addParameter(OAuth2Constants.REDIRECT_URI, keycloak.getAuthServerUrl() + "/realms/" + REALM + "/account")
				.addParameter(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
				.addParameter("kc_action", "UPDATE_PASSWORD");

			LoginWithUsernameAndPasswordPage loginPage = LoginWithUsernameAndPasswordPage.build(webClient, startUri.build().toURL());
			UpdatePasswordPage updatePasswordPage = loginPage.signInWithUsernameAndPassword(FRED, "fred", UpdatePasswordPage.class);
			updatePasswordPage.setPasswordTo("changed", AccountManagementPage.class);
		}

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

		usersResource.delete(users.getFirst().getId()).close();

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

	@Test
	@Order(8)
	public void testMappedAttributeIsReadFromAndWrittenBackToTheExternalSource() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();

		String fredId = usersResource.searchByUsername(FRED, true).getFirst().getId();
		UserRepresentation fred = usersResource.get(fredId).toRepresentation();
		assertThat(fred.firstAttribute("picture"), is(FRED_PICTURE));

		fred.singleAttribute("picture", "https://example.com/fred-new.png");
		usersResource.get(fredId).update(fred);

		UserRepresentation updated = usersResource.get(fredId).toRepresentation();
		assertThat(updated.firstAttribute("picture"), is("https://example.com/fred-new.png"));
	}

	@Test
	@Order(9)
	public void testMultivaluedMappedAttributeKeepsItsJsonArray() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();

		String bettyId = usersResource.searchByUsername(BETTY, true).getFirst().getId();
		UserRepresentation betty = usersResource.get(bettyId).toRepresentation();
		assertThat(betty.getAttributes().get("phone"), contains("+1-555-" + BETTY_ID, "+1-666-" + BETTY_ID));

		betty.getAttributes().put("phone", List.of("+1-777-1", "+1-777-2", "+1-777-3"));
		usersResource.get(bettyId).update(betty);

		UserRepresentation updated = usersResource.get(bettyId).toRepresentation();
		assertThat(updated.getAttributes().get("phone"), contains("+1-777-1", "+1-777-2", "+1-777-3"));

		// and the external record still holds a JSON array, not a joined string
		given().when().get(flintstonesApiUrl("/users/" + BETTY_ID))
			.then().statusCode(200)
			.body("phoneNumbers", contains("+1-777-1", "+1-777-2", "+1-777-3"));
	}

	@Test
	@Order(10)
	public void testNumericMappedAttributeKeepsItsJsonType() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();

		String barneyId = usersResource.searchByUsername(BARNEY, true).getFirst().getId();
		UserRepresentation barney = usersResource.get(barneyId).toRepresentation();
		assertThat(barney.firstAttribute("yearOfBirth"), is("1960"));

		barney.singleAttribute("yearOfBirth", "1959");
		usersResource.get(barneyId).update(barney);

		assertThat(usersResource.get(barneyId).toRepresentation().firstAttribute("yearOfBirth"), is("1959"));

		// written back as a JSON number, not as the string Keycloak keeps internally
		given().when().get(flintstonesApiUrl("/users/" + BARNEY_ID))
			.then().statusCode(200)
			.body("yearOfBirth", is(1959));
	}

	@Test
	@Order(11)
	public void testReadOnlyMappedAttributeIsReadableButRejectedForWriting() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		UsersResource usersResource = kcAdmin.realm(REALM).users();

		String pebblesId = usersResource.searchByUsername(PEBBLES, true).getFirst().getId();
		UserRepresentation pebbles = usersResource.get(pebblesId).toRepresentation();
		// both mappings read the same external field, only 'picture' may write it
		assertThat(pebbles.firstAttribute("picture"), is(PEBBLES_PICTURE));
		assertThat(pebbles.firstAttribute("avatar"), is(PEBBLES_PICTURE));

		// the write condition set by decorateUserProfile() makes Keycloak drop the change before it reaches the adapter
		pebbles.singleAttribute("avatar", "https://example.com/ignored.png");
		usersResource.get(pebblesId).update(pebbles);

		UserRepresentation updated = usersResource.get(pebblesId).toRepresentation();
		assertThat(updated.firstAttribute("avatar"), is(PEBBLES_PICTURE));
		assertThat(updated.firstAttribute("picture"), is(PEBBLES_PICTURE));
	}

	@Test
	@Order(12)
	public void testInvalidAttributeMappingsAreRejected() {
		Keycloak kcAdmin = keycloak.getKeycloakAdminClient();
		String componentId = kcAdmin.realm(REALM).components()
			.query(null, UserStorageProvider.class.getName(), FlintstonesUserStorageProviderFactory.PROVIDER_ID)
			.getFirst().getId();
		ComponentResource componentResource = kcAdmin.realm(REALM).components().component(componentId);

		ComponentRepresentation rep = componentResource.toRepresentation();
		rep.getConfig().putSingle(AttributeMappings.CONFIG_KEY, """
			[{ "name": "email", "field": "mail" }]""");

		assertThrows(BadRequestException.class, () -> componentResource.update(rep));
	}

	private static String flintstonesApiUrl(String path) {
		return keycloak.getAuthServerUrl() + "/realms/master/flintstones" + path;
	}

}
