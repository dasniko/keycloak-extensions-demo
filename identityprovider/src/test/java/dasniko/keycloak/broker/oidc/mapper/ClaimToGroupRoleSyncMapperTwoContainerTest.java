package dasniko.keycloak.broker.oidc.mapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full end-to-end test using a single Keycloak container with two realms:
 * - "idp" realm: acts as the external OIDC identity provider; has a real user and injects a
 *   "groups" claim via a hardcoded claim mapper on the broker client.
 * - "consumer" realm: has our extension loaded; configured to broker logins from the "idp" realm
 *   using ClaimToGroupRoleSyncMapper.
 *
 * Within one Keycloak instance, realm-to-realm brokering works exactly like an external IdP.
 * HtmlUnit drives the full browser-based OIDC broker flow.
 */
@Slf4j
@Testcontainers
class ClaimToGroupRoleSyncMapperTwoContainerTest extends TestBase {

	private static final String CONSUMER_REALM = "consumer";
	private static final String IDP_REALM = "idp";
	private static final String IDP_ALIAS = "realm-idp";
	private static final String CLIENT_ID = "consumer-client";
	private static final String IDP_CLIENT_ID = "keycloak-broker";
	private static final String IDP_USER = "idp-user";
	private static final String IDP_USER_PASSWORD = "password";

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
		.withRealmImportFile("idp-realm.json")
		.withProviderClassesFrom("target/classes", "../utils/target/classes");

	@BeforeAll
	static void setUpConsumerRealm() {
		Keycloak admin = keycloak.getKeycloakAdminClient();
		createConsumerRealm(admin);
		createConsumerClient(admin);
		createIdpInConsumerRealm(admin);
		addClaimSyncMapper(admin);
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	@Test
	void fullBrokerFlow_newUser_getsGroupsFromIdpClaim() throws Exception {
		loginViaBroker();

		// idp-realm.json injects groups = ["engineering", "qa"] for all tokens issued to the broker client
		Keycloak admin = keycloak.getKeycloakAdminClient();
		var federatedUser = admin.realm(CONSUMER_REALM).users()
			.searchByUsername(IDP_USER, true)
			.stream().findFirst().orElse(null);

		assertNotNull(federatedUser, "Federated user should have been created in consumer realm after broker login");

		var groups = admin.realm(CONSUMER_REALM).users().get(federatedUser.getId()).groups();
		assertTrue(groups.stream().anyMatch(g -> "engineering".equals(g.getName())),
			"User should be in 'engineering' group");
		assertTrue(groups.stream().anyMatch(g -> "qa".equals(g.getName())),
			"User should be in 'qa' group");
	}

	// -------------------------------------------------------------------------
	// Setup helpers
	// -------------------------------------------------------------------------

	private static void createConsumerRealm(Keycloak admin) {
		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm(CONSUMER_REALM);
		realm.setEnabled(true);
		admin.realms().create(realm);
	}

	private static void createConsumerClient(Keycloak admin) {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(CLIENT_ID);
		client.setPublicClient(true);
		client.setEnabled(true);
		client.setStandardFlowEnabled(true);
		client.setRedirectUris(List.of("*"));
		client.setWebOrigins(List.of("*"));
		admin.realm(CONSUMER_REALM).clients().create(client).close();
	}

	private static void createIdpInConsumerRealm(Keycloak admin) {
		// authorizationUrl: external URL so HtmlUnit (on host) can follow the browser redirect.
		// tokenUrl/jwksUrl: internal port so Keycloak (inside Docker) can call itself without going through the host port mapping.
		// issuer: external URL to match the iss claim in tokens (set by Keycloak based on the browser-facing request).
		String externalBase = keycloak.getAuthServerUrl() + "/realms/" + IDP_REALM;
		String internalBase = "http://localhost:8080/realms/" + IDP_REALM;

		IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
		idp.setAlias(IDP_ALIAS);
		idp.setProviderId("oidc");
		idp.setEnabled(true);
		idp.setFirstBrokerLoginFlowAlias("first broker login");

		Map<String, String> config = new HashMap<>();
		config.put("authorizationUrl", externalBase + "/protocol/openid-connect/auth");
		config.put("tokenUrl", internalBase + "/protocol/openid-connect/token");
		config.put("jwksUrl", internalBase + "/protocol/openid-connect/certs");
		config.put("issuer", externalBase);
		config.put("clientId", IDP_CLIENT_ID);
		config.put("clientSecret", "broker-secret");
		config.put("validateSignature", "true");
		config.put("useJwksUrl", "true");
		config.put("syncMode", "FORCE");
		idp.setConfig(config);

		admin.realm(CONSUMER_REALM).identityProviders().create(idp).close();
	}

	private static void addClaimSyncMapper(Keycloak admin) {
		IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
		mapper.setName("groups-sync");
		mapper.setIdentityProviderMapper(ClaimToGroupRoleSyncMapper.PROVIDER_ID);
		mapper.setIdentityProviderAlias(IDP_ALIAS);

		Map<String, String> config = new HashMap<>();
		config.put("claim", "groups");
		config.put(ClaimToGroupRoleSyncMapper.CONFIG_MAPPER_TYPE, "GROUPS");
		config.put("syncMode", "FORCE");
		mapper.setConfig(config);

		admin.realm(CONSUMER_REALM).identityProviders().get(IDP_ALIAS).addMapper(mapper).close();
	}

	// -------------------------------------------------------------------------
	// Browser flow
	// -------------------------------------------------------------------------

	private void loginViaBroker() throws Exception {
		String loginUrl = keycloak.getAuthServerUrl()
			+ "/realms/" + CONSUMER_REALM + "/protocol/openid-connect/auth"
			+ "?client_id=" + CLIENT_ID
			+ "&response_type=code"
			+ "&scope=openid"
			+ "&redirect_uri=" + keycloak.getAuthServerUrl() + "/realms/" + CONSUMER_REALM + "/account";

		try (WebClient webClient = new WebClient()) {
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			webClient.getOptions().setRedirectEnabled(true);

			// consumer realm login page — shows the IdP button
			HtmlPage loginPage = webClient.getPage(loginUrl);
			log.debug("Login page URL: {}", loginPage.getUrl());

			// click the social/IdP button to initiate broker flow
			HtmlPage idpLoginPage = loginPage.<HtmlAnchor>getHtmlElementById("social-" + IDP_ALIAS).click();
			log.debug("IdP login page URL: {}", idpLoginPage.getUrl());

			// fill in credentials on the IdP realm's login form
			HtmlInput usernameInput = idpLoginPage.querySelector("input[name='username']");
			HtmlInput passwordInput = idpLoginPage.querySelector("input[name='password']");
			assertNotNull(usernameInput, "Username input not found on IdP login page");
			assertNotNull(passwordInput, "Password input not found on IdP login page");

			usernameInput.setValueAttribute(IDP_USER);
			passwordInput.setValueAttribute(IDP_USER_PASSWORD);

			HtmlPage postLoginPage = idpLoginPage.<org.htmlunit.html.HtmlElement>querySelector("input[type='submit'],button[type='submit']").click();
			log.debug("Post-login page URL: {}", postLoginPage.getUrl());

			// "Review profile" page appears on first broker login — just submit to confirm
			if (postLoginPage.getUrl().toString().contains("login-actions")) {
				org.htmlunit.html.HtmlElement confirmButton = postLoginPage.querySelector("input[type='submit'],button[type='submit']");
				if (confirmButton != null) {
					confirmButton.click();
				}
			}
		}
	}
}
