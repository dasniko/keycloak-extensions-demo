package dasniko.keycloak.tokenmapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class LookupUserRolesMapperTest extends TestBase {

	private static final String REALM = "lookup-test";
	/** public client, so that {@link TestBase#requestToken} can be used (it sends no client secret) */
	private static final String TEST_CLIENT = "lookup-test-client";
	/** confidential client used for token introspection */
	private static final String INTROSPECTION_CLIENT = "lookup-introspection-client";
	private static final String INTROSPECTION_CLIENT_SECRET = "secret";
	private static final String ROLE_CLIENT = "lookup-role-client";
	private static final String CURRENT_ONLY_CLIENT = "current-only-client";
	private static final String LOOKUP_ONLY_CLIENT = "lookup-only-client";
	private static final String DISABLED_USER = "disabled-lookup-user";
	private static final String PASSWORD = "password";
	private static final String CURRENT_USER = "current-user";
	private static final String LOOKUP_USER = "lookup-user";
	private static final String CURRENT_REALM_ROLE = "current-realm-role";
	private static final String LOOKUP_REALM_ROLE = "lookup-realm-role";
	private static final String CURRENT_CLIENT_ROLE = "current-client-role";
	private static final String LOOKUP_CLIENT_ROLE = "lookup-client-role";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
		.withDefaultProviderClasses();

	private static RealmResource realm;
	private static ClientResource testClient;

	@BeforeAll
	static void setUp() {
		initTestRealm(keycloak, REALM, null, (admin, realmRepresentation) -> {
			realm = admin.realm(REALM);
			createTestData();
		});
	}

	private static void createTestData() {
		ClientRepresentation roleClient = new ClientRepresentation();
		roleClient.setClientId(ROLE_CLIENT);
		String roleClientId = CreatedResponseUtil.getCreatedId(realm.clients().create(roleClient));
		realm.clients().get(roleClientId).roles().create(role(CURRENT_CLIENT_ROLE));
		realm.clients().get(roleClientId).roles().create(role(LOOKUP_CLIENT_ROLE));
		realm.roles().create(role(CURRENT_REALM_ROLE));
		realm.roles().create(role(LOOKUP_REALM_ROLE));

		String currentUserId = createUser(CURRENT_USER, roleClientId, CURRENT_REALM_ROLE, CURRENT_CLIENT_ROLE);
		String lookupUserId = createUser(LOOKUP_USER, roleClientId, LOOKUP_REALM_ROLE, LOOKUP_CLIENT_ROLE);
		String disabledUserId = createUser(DISABLED_USER, roleClientId, LOOKUP_REALM_ROLE, LOOKUP_CLIENT_ROLE);
		UserRepresentation disabledUser = realm.users().get(disabledUserId).toRepresentation();
		disabledUser.setEnabled(false);
		realm.users().get(disabledUserId).update(disabledUser);
		// clients where only one of the users has a role, to tell apart whose roles the audience is derived from
		assignRoleOfNewClient(CURRENT_ONLY_CLIENT, currentUserId);
		assignRoleOfNewClient(LOOKUP_ONLY_CLIENT, lookupUserId);

		ClientRepresentation introspectionClient = new ClientRepresentation();
		introspectionClient.setClientId(INTROSPECTION_CLIENT);
		introspectionClient.setSecret(INTROSPECTION_CLIENT_SECRET);
		introspectionClient.setPublicClient(false);
		realm.clients().create(introspectionClient).close();

		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(TEST_CLIENT);
		client.setPublicClient(true);
		client.setDirectAccessGrantsEnabled(true);
		testClient = realm.clients().get(CreatedResponseUtil.getCreatedId(realm.clients().create(client)));
	}

	@AfterEach
	void removeMappers() {
		testClient.getProtocolMappers().getMappers().forEach(m -> testClient.getProtocolMappers().delete(m.getId()));
	}

	@Test
	void mergesRolesOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "true"));

		Map<String, Object> token = accessToken();

		assertTrue(strings(token, "realm_access", "roles").containsAll(Set.of(CURRENT_REALM_ROLE, LOOKUP_REALM_ROLE)));
		assertEquals(Set.of(CURRENT_CLIENT_ROLE, LOOKUP_CLIENT_ROLE), Set.copyOf(strings(token, "resource_access", ROLE_CLIENT, "roles")));
	}

	@Test
	void replacesRolesWithRolesOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));

		Map<String, Object> token = accessToken();

		assertTrue(strings(token, "realm_access", "roles").contains(LOOKUP_REALM_ROLE));
		assertFalse(strings(token, "realm_access", "roles").contains(CURRENT_REALM_ROLE));
		assertEquals(List.of(LOOKUP_CLIENT_ROLE), strings(token, "resource_access", ROLE_CLIENT, "roles"));
	}

	@Test
	void addsAudienceOfLookupUserWhenMerging() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "true"));
		String token = tokenString();

		assertTrue(strings(parseToken(token), "aud").containsAll(Set.of(CURRENT_ONLY_CLIENT, LOOKUP_ONLY_CLIENT, ROLE_CLIENT)));
		assertTrue(strings(introspect(token), "aud").containsAll(Set.of(CURRENT_ONLY_CLIENT, LOOKUP_ONLY_CLIENT)));
	}

	@Test
	void replacesAudienceWithAudienceOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));
		String token = tokenString();

		List<String> audience = strings(parseToken(token), "aud");
		assertTrue(audience.containsAll(Set.of(LOOKUP_ONLY_CLIENT, ROLE_CLIENT)));
		assertFalse(audience.contains(CURRENT_ONLY_CLIENT));
		// an explicitly configured audience is not derived from roles and must survive
		assertTrue(audience.contains(INTROSPECTION_CLIENT));

		List<String> introspectedAudience = strings(introspect(token), "aud");
		assertTrue(introspectedAudience.contains(LOOKUP_ONLY_CLIENT));
		assertFalse(introspectedAudience.contains(CURRENT_ONLY_CLIENT));
	}

	@Test
	void writesToCustomClaimsWithPrefixes() throws Exception {
		addMapper(Map.of(
			LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER,
			LookupUserRolesMapper.MERGE, "false",
			LookupUserRolesMapper.REALM_CLAIM_NAME, "my.realm",
			LookupUserRolesMapper.REALM_ROLE_PREFIX, "r_",
			LookupUserRolesMapper.CLIENT_CLAIM_NAME, "my.clients.${client_id}",
			LookupUserRolesMapper.CLIENT_ROLE_PREFIX, "${client_id}:"));

		Map<String, Object> token = accessToken();

		assertNull(token.get("realm_access"));
		assertTrue(strings(token, "resource_access", ROLE_CLIENT, "roles").isEmpty());
		assertTrue(strings(token, "my", "realm").contains("r_" + LOOKUP_REALM_ROLE));
		assertEquals(List.of(ROLE_CLIENT + ":" + LOOKUP_CLIENT_ROLE), strings(token, "my", "clients", ROLE_CLIENT));
	}

	@Test
	void leavesTokenUnchangedIfLookupUserNotFound() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, "does-not-exist", LookupUserRolesMapper.MERGE, "false"));

		Map<String, Object> token = accessToken();

		assertTrue(strings(token, "realm_access", "roles").contains(CURRENT_REALM_ROLE));
		assertEquals(List.of(CURRENT_CLIENT_ROLE), strings(token, "resource_access", ROLE_CLIENT, "roles"));
	}

	@Test
	void leavesTokenUnchangedIfLookupUserDisabled() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, DISABLED_USER, LookupUserRolesMapper.MERGE, "false"));
		String tokenString = tokenString();

		Map<String, Object> token = parseToken(tokenString);

		assertTrue(strings(token, "realm_access", "roles").contains(CURRENT_REALM_ROLE));
		assertFalse(strings(token, "realm_access", "roles").contains(LOOKUP_REALM_ROLE));
		assertEquals(List.of(CURRENT_CLIENT_ROLE), strings(token, "resource_access", ROLE_CLIENT, "roles"));
		assertTrue(strings(introspect(tokenString), "realm_access", "roles").contains(CURRENT_REALM_ROLE));
	}

	@Test
	void appliesToIntrospectionResponse() {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));

		Map<String, Object> introspection = introspect(tokenString());

		assertTrue(strings(introspection, "realm_access", "roles").contains(LOOKUP_REALM_ROLE));
		assertFalse(strings(introspection, "realm_access", "roles").contains(CURRENT_REALM_ROLE));
		assertEquals(List.of(LOOKUP_CLIENT_ROLE), strings(introspection, "resource_access", ROLE_CLIENT, "roles"));
	}

	@Test
	void doesNotApplyIfNotIncludedInAccessToken() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"), false);
		String token = tokenString();

		assertTrue(strings(parseToken(token), "realm_access", "roles").contains(CURRENT_REALM_ROLE));
		assertTrue(strings(introspect(token), "realm_access", "roles").contains(LOOKUP_REALM_ROLE));
	}

	private void addMapper(Map<String, String> config) {
		addMapper(config, true);
	}

	private void addMapper(Map<String, String> config, boolean includeInAccessToken) {
		ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		mapper.setProtocolMapper(LookupUserRolesMapper.PROVIDER_ID);
		mapper.setName("lookup-user-roles");
		Map<String, String> mapperConfig = new HashMap<>(config);
		mapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(includeInAccessToken));
		mapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
		mapper.setConfig(mapperConfig);
		try (Response response = testClient.getProtocolMappers().createMapper(mapper)) {
			assertEquals(201, response.getStatus());
		}
		addAudienceMapper();
	}

	/** Token introspection requires the introspecting client in the audience, which the role-less test client doesn't get otherwise. */
	private void addAudienceMapper() {
		ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		mapper.setProtocolMapper("oidc-audience-mapper");
		mapper.setName("introspection-client-audience");
		mapper.setConfig(Map.of("included.client.audience", INTROSPECTION_CLIENT, OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true"));
		try (Response response = testClient.getProtocolMappers().createMapper(mapper)) {
			assertEquals(201, response.getStatus());
		}
	}

	private String tokenString() {
		return requestToken(keycloak, REALM, TEST_CLIENT, CURRENT_USER, PASSWORD).extract().path("access_token");
	}

	private Map<String, Object> accessToken() throws IOException {
		return parseToken(tokenString());
	}

	private Map<String, Object> introspect(String token) {
		String introspectionEndpoint = getOpenIDConfiguration(keycloak, REALM).extract().path("introspection_endpoint");
		return given()
			.auth().preemptive().basic(INTROSPECTION_CLIENT, INTROSPECTION_CLIENT_SECRET)
			.formParam("token", token)
			.when().post(introspectionEndpoint)
			.then().statusCode(200)
			.extract().jsonPath().getMap("$");
	}

	/** Follows the path through nested claims and returns the value there as a list of strings (single values too), empty if absent. */
	@SuppressWarnings("unchecked")
	private static List<String> strings(Map<String, Object> claims, String... path) {
		Object current = claims;
		for (String component : path) {
			if (!(current instanceof Map<?, ?> map) || (current = ((Map<String, Object>) map).get(component)) == null) {
				return List.of();
			}
		}
		return current instanceof Collection<?> values ? values.stream().map(String::valueOf).toList() : List.of(String.valueOf(current));
	}

	private static RoleRepresentation role(String name) {
		RoleRepresentation role = new RoleRepresentation();
		role.setName(name);
		return role;
	}

	private static void assignRoleOfNewClient(String clientId, String userId) {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(clientId);
		String id = CreatedResponseUtil.getCreatedId(realm.clients().create(client));
		realm.clients().get(id).roles().create(role("some-role"));
		realm.users().get(userId).roles().clientLevel(id)
			.add(List.of(realm.clients().get(id).roles().get("some-role").toRepresentation()));
	}

	private static String createUser(String username, String roleClientId, String realmRole, String clientRole) {
		UserRepresentation user = new UserRepresentation();
		user.setUsername(username);
		user.setEnabled(true);
		user.setEmail(username + "@example.com");
		user.setFirstName(username);
		user.setLastName(username);
		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(PASSWORD);
		credential.setTemporary(false);
		user.setCredentials(List.of(credential));
		String id = CreatedResponseUtil.getCreatedId(realm.users().create(user));
		realm.users().get(id).roles().realmLevel().add(List.of(realm.roles().get(realmRole).toRepresentation()));
		realm.users().get(id).roles().clientLevel(roleClientId)
			.add(List.of(realm.clients().get(roleClientId).roles().get(clientRole).toRepresentation()));
		return id;
	}
}
