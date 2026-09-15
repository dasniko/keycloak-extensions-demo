package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.mappers.AttributeMappings;
import dasniko.keycloak.user.flintstones.repo.FlintstonesApiResourceProvider;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import de.keycloak.test.pages.AccountManagementPage;
import de.keycloak.test.pages.LoginWithUsernameAndPasswordPage;
import de.keycloak.test.pages.UpdatePasswordPage;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.AfterAll;
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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeMetadata;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
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
import static org.hamcrest.Matchers.nullValue;
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
	static final String PEBBLES_ID = "34567";
	static final String PEBBLES_PICTURE = "https://dasniko-public.s3.eu-central-1.amazonaws.com/pebbles.png";

	// never contacted, only needed to link users to
	private static final String IDP_ALIAS = "test-idp";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
		.withEnv("KC_SPI_EVENTS_LISTENER__JBOSS_LOGGING__SUCCESS_LEVEL", "info")
		.withEnv("KC_LOG_LEVEL", "INFO,dasniko:debug")
		.withProviderClassesFrom("target/classes", "../utils/target/classes");

	private static Keycloak admin;

	@BeforeAll
	static void beforeAll() {
		admin = keycloak.getKeycloakAdminClient();

		initTestRealm(keycloak, REALM,
			realm -> {
				realm.setLoginWithEmailAllowed(true);
				realm.setResetPasswordAllowed(true);
			},
			(admin, realmRep) -> {
				RealmResource realm = admin.realm(REALM);

				ClientRepresentation client = new ClientRepresentation();
				client.setEnabled(true);
				client.setClientId("api-client");
				client.setServiceAccountsEnabled(true);
				realm.clients().create(client).close();

				ComponentRepresentation componentRep = new ComponentRepresentation();
				componentRep.setProviderId(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
				componentRep.setName(FlintstonesUserStorageProviderFactory.PROVIDER_ID);
				componentRep.setProviderType(UserStorageProvider.class.getTypeName());

				MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
				config.add(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL,
					"http://localhost:8080/realms/master/" + FlintstonesApiResourceProvider.PROVIDER_ID);
				config.add(FlintstonesUserStorageProviderFactory.CLIENT_ID, "api-client");
				config.add(FlintstonesUserStorageProviderFactory.SUPPORTED_CREDENTIAL_TYPES, "password");
				config.add(FlintstonesUserStorageProviderFactory.USER_CREATION_ENABLED, "true");
				config.add(FlintstonesUserStorageProviderFactory.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
				config.add(AttributeMappings.CONFIG_KEY, """
					[
					  { "name": "picture", "field": "pictureUrl" },
					  { "name": "avatar", "field": "pictureUrl", "readOnly": true },
					  { "name": "phone", "field": "phoneNumbers", "multivalued": true },
					  { "name": "yearOfBirth", "field": "yearOfBirth", "type": "integer" },
					  { "name": "addressJson", "field": "address", "type": "json", "readOnly": true, "group": "user-metadata" },
					  { "name": "city", "field": "address", "property": "city", "group": "user-metadata" }
					]""");
				config.add("enabled", "true");
				componentRep.setConfig(config);

				realm.components().add(componentRep).close();

				IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
				idp.setAlias(IDP_ALIAS);
				idp.setProviderId("oidc");
				idp.setConfig(Map.of(
					"clientId", "keycloak",
					"clientSecret", "secret",
					"clientAuthMethod", "client_secret_post",
					"authorizationUrl", "https://idp.invalid/auth",
					"tokenUrl", "https://idp.invalid/token"));
				try (Response response = realm.identityProviders().create(idp)) {
					assertThat("creating identity provider " + IDP_ALIAS, response.getStatus(), is(201));
				}
			}
		);

		keycloak.disableLightweightAccessTokenForAdminCliClient(REALM);
	}

	@AfterAll
	static void afterAll() {
		admin.close();
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
		UsersResource usersResource = admin.realm(REALM).users();
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
		UsersResource usersResource = admin.realm(REALM).users();
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
		UsersResource usersResource = admin.realm(REALM).users();
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
		UserRepresentation fred = user(FRED).toRepresentation();
		assertThat(fred.firstAttribute("picture"), is(FRED_PICTURE));

		fred.singleAttribute("picture", "https://example.com/fred-new.png");
		user(FRED).update(fred);

		assertThat(user(FRED).toRepresentation().firstAttribute("picture"), is("https://example.com/fred-new.png"));
	}

	@Test
	@Order(9)
	public void testMultivaluedMappedAttributeKeepsItsJsonArray() {
		UserRepresentation betty = user(BETTY).toRepresentation();
		assertThat(betty.getAttributes().get("phone"), contains("+1-555-" + BETTY_ID, "+1-666-" + BETTY_ID));

		betty.getAttributes().put("phone", List.of("+1-777-1", "+1-777-2", "+1-777-3"));
		user(BETTY).update(betty);

		assertThat(user(BETTY).toRepresentation().getAttributes().get("phone"), contains("+1-777-1", "+1-777-2", "+1-777-3"));

		// and the external record still holds a JSON array, not a joined string
		given().when().get(flintstonesApiUrl("/users/" + BETTY_ID))
			.then().statusCode(200)
			.body("phoneNumbers", contains("+1-777-1", "+1-777-2", "+1-777-3"));
	}

	@Test
	@Order(10)
	public void testNumericMappedAttributeKeepsItsJsonType() {
		UserRepresentation barney = user(BARNEY).toRepresentation();
		assertThat(barney.firstAttribute("yearOfBirth"), is("1960"));

		barney.singleAttribute("yearOfBirth", "1959");
		user(BARNEY).update(barney);

		assertThat(user(BARNEY).toRepresentation().firstAttribute("yearOfBirth"), is("1959"));

		// written back as a JSON number, not as the string Keycloak keeps internally
		given().when().get(flintstonesApiUrl("/users/" + BARNEY_ID))
			.then().statusCode(200)
			.body("yearOfBirth", is(1959));
	}

	@Test
	@Order(11)
	public void testReadOnlyMappedAttributeIsReadableButRejectedForWriting() {
		UserRepresentation pebbles = user(PEBBLES).toRepresentation();
		// both mappings read the same external field, only 'picture' may write it
		assertThat(pebbles.firstAttribute("picture"), is(PEBBLES_PICTURE));
		assertThat(pebbles.firstAttribute("avatar"), is(PEBBLES_PICTURE));

		// the write condition set by decorateUserProfile() makes Keycloak drop the change before it reaches the adapter
		pebbles.singleAttribute("avatar", "https://example.com/ignored.png");
		user(PEBBLES).update(pebbles);

		UserRepresentation updated = user(PEBBLES).toRepresentation();
		assertThat(updated.firstAttribute("avatar"), is(PEBBLES_PICTURE));
		assertThat(updated.firstAttribute("picture"), is(PEBBLES_PICTURE));
	}

	@Test
	@Order(12)
	public void testComplexValueIsExposedAsSerializedJson() {
		UserRepresentation pebbles = user(PEBBLES).toRepresentation();

		assertThat(pebbles.firstAttribute("addressJson"),
			is("{\"street\":\"" + PEBBLES_ID + " Cobblestone Way\",\"city\":\"Bedrock\"}"));
	}

	@Test
	@Order(13)
	public void testProjectedMemberIsWrittenBackAsAReplacedObject() {
		UserRepresentation betty = user(BETTY).toRepresentation();
		assertThat(betty.firstAttribute("city"), is("Bedrock"));

		// an update that leaves the projected member alone leaves the object intact — Keycloak does not push unchanged
		// attributes down to the provider (AttributeMapping#changes covers the case where a caller does)
		betty.setLastName("Rubble-Smith");
		user(BETTY).update(betty);
		given().when().get(flintstonesApiUrl("/users/" + BETTY_ID))
			.then().statusCode(200)
			.body("address.street", is(BETTY_ID + " Cobblestone Way"));

		// changing it does rewrite the object, and replace semantics drop everything else it held
		betty.singleAttribute("city", "Rock Vegas");
		user(BETTY).update(betty);

		assertThat(user(BETTY).toRepresentation().firstAttribute("city"), is("Rock Vegas"));
		given().when().get(flintstonesApiUrl("/users/" + BETTY_ID))
			.then().statusCode(200)
			.body("address.city", is("Rock Vegas"))
			.body("address.street", is(nullValue()));
	}

	@Test
	@Order(14)
	public void testMappedAttributeIsPlacedInTheConfiguredUserProfileGroup() {
		UserRepresentation pebbles = user(PEBBLES).toRepresentation(true);

		assertThat(attributeMetadata(pebbles, "addressJson").getGroup(), is("user-metadata"));
		assertThat(attributeMetadata(pebbles, "city").getGroup(), is("user-metadata"));
		// a mapping without a group stays ungrouped
		assertThat(attributeMetadata(pebbles, "picture").getGroup(), is(nullValue()));
	}

	@Test
	@Order(15)
	public void testUnknownUserProfileGroupIsRejected() {
		ComponentResource componentResource = flintstonesComponent();

		ComponentRepresentation rep = componentResource.toRepresentation();
		rep.getConfig().putSingle(AttributeMappings.CONFIG_KEY, """
			[{ "name": "picture", "field": "pictureUrl", "group": "no-such-group" }]""");

		assertThrows(BadRequestException.class, () -> componentResource.update(rep));
	}

	@Test
	@Order(16)
	public void testAttributeStaysVisibleWhenItsGroupIsRemovedFromTheRealm() {
		// the mapping was valid when it was saved; dropping the group afterwards must not make the attribute disappear
		UPConfig config = users().userProfile().getConfiguration();
		config.setGroups(List.of());
		users().userProfile().update(config);

		UserRepresentation pebbles = user(PEBBLES).toRepresentation(true);

		assertThat(attributeMetadata(pebbles, "addressJson").getGroup(), is(nullValue()));
		assertThat(pebbles.firstAttribute("city"), is("Bedrock"));
	}

	@Test
	@Order(17)
	public void testInvalidAttributeMappingsAreRejected() {
		ComponentResource componentResource = flintstonesComponent();

		ComponentRepresentation rep = componentResource.toRepresentation();
		rep.getConfig().putSingle(AttributeMappings.CONFIG_KEY, """
			[{ "name": "email", "field": "mail" }]""");

		assertThrows(BadRequestException.class, () -> componentResource.update(rep));
	}

	@Test
	@Order(18)
	void searchByIdpLinkResolvesTheApiUserFromFederatedStorage() {
		// the link of an API user is kept in Keycloak's federated storage, the API knows nothing about it;
		// this is the lookup a broker backchannel logout does
		UserResource tester = admin.realm(REALM).users().get(getUser(admin, REALM, FRED).getId());
		FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
		link.setIdentityProvider(IDP_ALIAS);
		link.setUserId("idp-fred");
		link.setUserName(FRED);
		try (Response response = tester.addFederatedIdentity(IDP_ALIAS, link)) {
			assertThat(response.getStatus(), is(204));
		}

		try {
			assertThat(searchByIdpLink(IDP_ALIAS, "idp-fred"), contains(FRED));
			assertThat(countByIdpLink(IDP_ALIAS, "idp-fred"), is(1));
		} finally {
			tester.removeFederatedIdentity(IDP_ALIAS);
		}
	}

	@Test
	@Order(19)
	void searchByIdpLinkIsNotForwardedToApi() {
		// without the link criteria, which the API can't apply, the API would answer with an unfiltered user list
		assertThat(searchByIdpLink(IDP_ALIAS, "no-such-idp-user"), is(empty()));
		assertThat(countByIdpLink(IDP_ALIAS, "no-such-idp-user"), is(0));

		// federated storage can't list all users of an IdP
		assertThat(searchByIdpLink(IDP_ALIAS, null), is(empty()));
	}


	private static String flintstonesApiUrl(String path) {
		return keycloak.getAuthServerUrl() + "/realms/master/" + FlintstonesApiResourceProvider.PROVIDER_ID + path;
	}

	private static UsersResource users() {
		return keycloak.getKeycloakAdminClient().realm(REALM).users();
	}

	private static UserResource user(String username) {
		UsersResource users = users();
		List<UserRepresentation> found = users.searchByUsername(username, true);
		assertThat(found, hasSize(1));
		return users.get(found.getFirst().getId());
	}

	private static UserProfileAttributeMetadata attributeMetadata(UserRepresentation user, String name) {
		UserProfileAttributeMetadata metadata = user.getUserProfileMetadata().getAttributeMetadata(name);
		if (metadata == null) {
			throw new AssertionError("attribute '" + name + "' is not on the user profile");
		}
		return metadata;
	}

	private static ComponentResource flintstonesComponent() {
		RealmResource realm = keycloak.getKeycloakAdminClient().realm(REALM);
		String componentId = realm.components()
			.query(null, UserStorageProvider.class.getName(), FlintstonesUserStorageProviderFactory.PROVIDER_ID)
			.getFirst().getId();
		return realm.components().component(componentId);
	}

	private static List<String> searchByIdpLink(String idpAlias, String idpUserId) {
		return admin.realm(REALM).users()
			.search(null, null, null, null, null, idpAlias, idpUserId, null, null, null, true)
			.stream().map(UserRepresentation::getUsername).toList();
	}

	private static int countByIdpLink(String idpAlias, String idpUserId) {
		return admin.realm(REALM).users().count(null, null, null, null, null, null, null, idpAlias, idpUserId, null);
	}

}
