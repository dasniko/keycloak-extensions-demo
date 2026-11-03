package dasniko.keycloak.tokenmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupUserRolesMapperTest {

	private static final String REALM = KeycloakContainer.MASTER_REALM;
	private static final String TEST_CLIENT = "lookup-test-client";
	private static final String TEST_CLIENT_SECRET = "secret";
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

	private static KeycloakContainer keycloak;
	private static RealmResource realm;
	private static ClientResource testClient;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	static void setUp() {
		keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly").withDefaultProviderClasses();
		keycloak.start();
		realm = keycloak.getKeycloakAdminClient().realm(REALM);

		ClientRepresentation roleClient = new ClientRepresentation();
		roleClient.setClientId(ROLE_CLIENT);
		String roleClientId = CreatedResponseUtil.getCreatedId(realm.clients().create(roleClient));
		realm.clients().get(roleClientId).roles().create(role(CURRENT_CLIENT_ROLE));
		realm.clients().get(roleClientId).roles().create(role(LOOKUP_CLIENT_ROLE));
		realm.roles().create(role(CURRENT_REALM_ROLE));
		realm.roles().create(role(LOOKUP_REALM_ROLE));

		String currentUserId = createUser(CURRENT_USER, roleClientId, CURRENT_REALM_ROLE, CURRENT_CLIENT_ROLE);
		String lookupUserId = createUser(LOOKUP_USER, roleClientId, LOOKUP_REALM_ROLE, LOOKUP_CLIENT_ROLE);
		// clients where only one of the users has a role, to tell apart whose roles the audience is derived from
		String disabledUserId = createUser(DISABLED_USER, roleClientId, LOOKUP_REALM_ROLE, LOOKUP_CLIENT_ROLE);
		UserRepresentation disabledUser = realm.users().get(disabledUserId).toRepresentation();
		disabledUser.setEnabled(false);
		realm.users().get(disabledUserId).update(disabledUser);
		assignRoleOfNewClient(CURRENT_ONLY_CLIENT, currentUserId);
		assignRoleOfNewClient(LOOKUP_ONLY_CLIENT, lookupUserId);

		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(TEST_CLIENT);
		client.setSecret(TEST_CLIENT_SECRET);
		client.setPublicClient(false);
		client.setDirectAccessGrantsEnabled(true);
		testClient = realm.clients().get(CreatedResponseUtil.getCreatedId(realm.clients().create(client)));
	}

	@AfterAll
	static void tearDown() {
		keycloak.stop();
	}

	@AfterEach
	void removeMappers() {
		testClient.getProtocolMappers().getMappers().forEach(m -> testClient.getProtocolMappers().delete(m.getId()));
	}

	@Test
	void mergesRolesOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "true"));

		AccessToken token = accessToken();

		assertTrue(token.getRealmAccess().getRoles().containsAll(Set.of(CURRENT_REALM_ROLE, LOOKUP_REALM_ROLE)));
		assertEquals(Set.of(CURRENT_CLIENT_ROLE, LOOKUP_CLIENT_ROLE), token.getResourceAccess(ROLE_CLIENT).getRoles());
	}

	@Test
	void replacesRolesWithRolesOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));

		AccessToken token = accessToken();

		assertTrue(token.getRealmAccess().getRoles().contains(LOOKUP_REALM_ROLE));
		assertFalse(token.getRealmAccess().getRoles().contains(CURRENT_REALM_ROLE));
		assertEquals(Set.of(LOOKUP_CLIENT_ROLE), token.getResourceAccess(ROLE_CLIENT).getRoles());
	}

	@Test
	void addsAudienceOfLookupUserWhenMerging() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "true"));

		assertTrue(Set.of(accessToken().getAudience()).containsAll(Set.of(CURRENT_ONLY_CLIENT, LOOKUP_ONLY_CLIENT, ROLE_CLIENT)));
		assertTrue(toList(introspect(tokenString()).get("aud")).containsAll(Set.of(CURRENT_ONLY_CLIENT, LOOKUP_ONLY_CLIENT)));
	}

	@Test
	void replacesAudienceWithAudienceOfLookupUser() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));

		Set<String> audience = Set.of(accessToken().getAudience());
		assertTrue(audience.containsAll(Set.of(LOOKUP_ONLY_CLIENT, ROLE_CLIENT)));
		assertFalse(audience.contains(CURRENT_ONLY_CLIENT));
		// an explicitly configured audience is not derived from roles and must survive
		assertTrue(audience.contains(TEST_CLIENT));

		List<String> introspectedAudience = toList(introspect(tokenString()).get("aud"));
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

		AccessToken token = accessToken();

		assertNull(token.getRealmAccess());
		assertTrue(token.getResourceAccess().isEmpty() || token.getResourceAccess(ROLE_CLIENT) == null);
		@SuppressWarnings("unchecked")
		Map<String, Object> my = (Map<String, Object>) token.getOtherClaims().get("my");
		assertTrue(((List<?>) my.get("realm")).contains("r_" + LOOKUP_REALM_ROLE));
		@SuppressWarnings("unchecked")
		Map<String, Object> clients = (Map<String, Object>) my.get("clients");
		assertEquals(List.of(ROLE_CLIENT + ":" + LOOKUP_CLIENT_ROLE), clients.get(ROLE_CLIENT));
	}

	@Test
	void leavesTokenUnchangedIfLookupUserNotFound() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, "does-not-exist", LookupUserRolesMapper.MERGE, "false"));

		AccessToken token = accessToken();

		assertTrue(token.getRealmAccess().getRoles().contains(CURRENT_REALM_ROLE));
		assertEquals(Set.of(CURRENT_CLIENT_ROLE), token.getResourceAccess(ROLE_CLIENT).getRoles());
	}

	@Test
	void leavesTokenUnchangedIfLookupUserDisabled() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, DISABLED_USER, LookupUserRolesMapper.MERGE, "false"));

		AccessToken token = accessToken();

		assertTrue(token.getRealmAccess().getRoles().contains(CURRENT_REALM_ROLE));
		assertFalse(token.getRealmAccess().getRoles().contains(LOOKUP_REALM_ROLE));
		assertEquals(Set.of(CURRENT_CLIENT_ROLE), token.getResourceAccess(ROLE_CLIENT).getRoles());
		assertTrue(toList(introspect(tokenString()).at("/realm_access/roles")).contains(CURRENT_REALM_ROLE));
	}

	@Test
	void appliesToIntrospectionResponse() throws Exception {
		addMapper(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER, LookupUserRolesMapper.MERGE, "false"));

		JsonNode introspection = introspect(tokenString());

		assertTrue(toList(introspection.at("/realm_access/roles")).contains(LOOKUP_REALM_ROLE));
		assertFalse(toList(introspection.at("/realm_access/roles")).contains(CURRENT_REALM_ROLE));
		assertEquals(List.of(LOOKUP_CLIENT_ROLE), toList(introspection.at("/resource_access/" + ROLE_CLIENT + "/roles")));
	}

	@Test
	void doesNotApplyIfNotIncludedInAccessToken() throws Exception {
		Map<String, String> config = new HashMap<>(Map.of(LookupUserRolesMapper.LOOKUP_USERNAME, LOOKUP_USER,
			LookupUserRolesMapper.MERGE, "false"));
		addMapper(config, false, true);

		assertTrue(accessToken().getRealmAccess().getRoles().contains(CURRENT_REALM_ROLE));
		assertTrue(toList(introspect(tokenString()).at("/realm_access/roles")).contains(LOOKUP_REALM_ROLE));
	}

	private void addMapper(Map<String, String> config) {
		addMapper(config, true, true);
	}

	private void addMapper(Map<String, String> config, boolean accessToken, boolean introspection) {
		ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
		mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		mapper.setProtocolMapper(LookupUserRolesMapper.PROVIDER_ID);
		mapper.setName("lookup-user-roles");
		Map<String, String> mapperConfig = new HashMap<>(config);
		mapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(accessToken));
		mapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, String.valueOf(introspection));
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
		mapper.setName("self-audience");
		mapper.setConfig(Map.of("included.client.audience", TEST_CLIENT, OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true"));
		try (Response response = testClient.getProtocolMappers().createMapper(mapper)) {
			assertEquals(201, response.getStatus());
		}
	}

	private String tokenString() throws IOException, InterruptedException {
		String body = "grant_type=password&username=" + CURRENT_USER + "&password=" + PASSWORD;
		HttpResponse<String> response = post("/token", body);
		assertEquals(200, response.statusCode());
		return objectMapper.readTree(response.body()).get("access_token").asText();
	}

	private AccessToken accessToken() throws VerificationException, IOException, InterruptedException {
		return TokenVerifier.create(tokenString(), AccessToken.class).parse().getToken();
	}

	private JsonNode introspect(String token) throws IOException, InterruptedException {
		HttpResponse<String> response = post("/token/introspect", "token=" + token);
		assertEquals(200, response.statusCode());
		return objectMapper.readTree(response.body());
	}

	private HttpResponse<String> post(String endpoint, String body) throws IOException, InterruptedException {
		String basic = Base64.getEncoder().encodeToString((TEST_CLIENT + ":" + TEST_CLIENT_SECRET).getBytes());
		HttpRequest request = HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl() + "/realms/" + REALM
				+ "/protocol/openid-connect" + endpoint))
			.header("Authorization", "Basic " + basic)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		try (HttpClient http = HttpClient.newHttpClient()) {
			return http.send(request, HttpResponse.BodyHandlers.ofString());
		}
	}

	private static List<String> toList(JsonNode node) {
		return node.valueStream().map(JsonNode::asText).toList();
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
