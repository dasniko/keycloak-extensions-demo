package dasniko.keycloak.authentication.mfa;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import jakarta.ws.rs.core.Response;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
public class MfaAuthenticatorTest extends TestBase {

	private static final String REALM = "mfa";
	private static final String FLOW_ALIAS = "mfa-browser";
	private static final String CLIENT_ID = "test-client";
	private static final String REDIRECT_URI = "http://localhost/callback";
	private static final String USERNAME = "test";
	private static final String PASSWORD = "test";
	private static final int TTL_SECONDS = 10;

	// written by LoggingSmsProvider, the default provider of MfaAuthenticator, see "mfaAuthText" in messages_en.properties
	private static final Pattern CODE_PATTERN = Pattern.compile("MFA code: (\\d+)");

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
		.withProviderClassesFrom(
			"target/classes",
			"../custom-sms-spi/target/classes",
			"../requiredaction/target/classes",
			"../utils/target/classes");

	private WebClient webClient;

	@BeforeAll
	static void beforeAll() {
		initTestRealm(keycloak, REALM, null, (admin, realmRep) -> {
			RealmResource realm = admin.realm(REALM);
			createMfaBrowserFlow(realm);
			createClient(realm);
			createUser(realm);
		});
	}

	@BeforeEach
	void setUp() {
		webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		// stop at the redirect back to the client, the callback url does not exist
		webClient.getOptions().setRedirectEnabled(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	}

	@AfterEach
	void tearDown() {
		webClient.close();
	}

	@Test
	public void testValidCode() throws Exception {
		int logOffset = keycloak.getLogs().length();
		HtmlPage mfaPage = signIn();
		String code = awaitMfaCode(logOffset);

		Page result = submitCode(mfaPage, code);

		assertThat(result.getWebResponse().getStatusCode(), is(302));
		String location = result.getWebResponse().getResponseHeaderValue("Location");
		assertThat(location, startsWith(REDIRECT_URI));
		assertThat(location, containsString(OAuth2Constants.CODE + "="));
	}

	@Test
	public void testInvalidCode() throws Exception {
		int logOffset = keycloak.getLogs().length();
		HtmlPage mfaPage = signIn();
		String code = awaitMfaCode(logOffset);
		String wrongCode = code.equals("000000") ? "111111" : "000000";

		Page result = submitCode(mfaPage, wrongCode);

		assertThat(result.getWebResponse().getStatusCode(), is(200));
		HtmlPage resultPage = (HtmlPage) result;
		assertThat(resultPage.getElementById("kc-sms-code-login-form"), notNullValue());
		assertThat(resultPage.asNormalizedText(), containsString("Invalid code entered, please enter it again."));
	}

	@Test
	public void testExpiredCode() throws Exception {
		int logOffset = keycloak.getLogs().length();
		HtmlPage mfaPage = signIn();
		String code = awaitMfaCode(logOffset);

		Thread.sleep((TTL_SECONDS + 2) * 1000L);
		Page result = submitCode(mfaPage, code);

		assertThat(result.getWebResponse().getStatusCode(), is(400));
		assertThat(((HtmlPage) result).asNormalizedText(), containsString("The code has expired."));
	}

	private HtmlPage signIn() throws IOException {
		String authUrl = "%s/realms/%s/protocol/openid-connect/auth?response_type=code&client_id=%s&redirect_uri=%s&scope=openid"
			.formatted(keycloak.getAuthServerUrl(), REALM, CLIENT_ID, REDIRECT_URI);
		HtmlPage loginPage = webClient.getPage(authUrl);
		HtmlForm loginForm = (HtmlForm) loginPage.getElementById("kc-form-login");
		assertThat(loginForm, notNullValue());
		loginForm.getInputByName("username").type(USERNAME);
		loginForm.getInputByName("password").type(PASSWORD);

		HtmlPage mfaPage = loginPage.getHtmlElementById("kc-login").click();
		assertThat(mfaPage.getElementById("kc-sms-code-login-form"), notNullValue());
		return mfaPage;
	}

	private Page submitCode(HtmlPage mfaPage, String code) throws IOException {
		HtmlForm form = (HtmlForm) mfaPage.getElementById("kc-sms-code-login-form");
		form.getInputByName("code").type(code);
		return form.getOneHtmlElementByAttribute("input", "type", "submit").click();
	}

	private static String awaitMfaCode(int logOffset) throws InterruptedException {
		for (int i = 0; i < 50; i++) {
			Matcher matcher = CODE_PATTERN.matcher(keycloak.getLogs().substring(logOffset));
			String code = null;
			while (matcher.find()) {
				code = matcher.group(1);
			}
			if (code != null) {
				return code;
			}
			Thread.sleep(100);
		}
		return fail("No MFA code found in server log");
	}

	private static void createMfaBrowserFlow(RealmResource realm) {
		AuthenticationManagementResource flows = realm.flows();

		AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
		flow.setAlias(FLOW_ALIAS);
		flow.setProviderId("basic-flow");
		flow.setTopLevel(true);
		flow.setBuiltIn(false);
		flows.createFlow(flow).close();

		flows.addExecution(FLOW_ALIAS, Map.of("provider", "auth-username-password-form"));
		flows.addExecution(FLOW_ALIAS, Map.of("provider", MfaAuthenticatorFactory.PROVIDER_ID));

		for (AuthenticationExecutionInfoRepresentation execution : flows.getExecutions(FLOW_ALIAS)) {
			execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
			flows.updateExecutions(FLOW_ALIAS, execution);

			if (MfaAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId())) {
				AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
				config.setAlias("mfa-config");
				config.setConfig(Map.of(MfaConstants.CONFIG_PROPERTY_TTL, String.valueOf(TTL_SECONDS)));
				flows.newExecutionConfig(execution.getId(), config).close();
			}
		}

		RealmRepresentation realmRep = realm.toRepresentation();
		realmRep.setBrowserFlow(FLOW_ALIAS);
		realm.update(realmRep);
	}

	private static void createClient(RealmResource realm) {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(CLIENT_ID);
		client.setPublicClient(true);
		client.setStandardFlowEnabled(true);
		client.setRedirectUris(List.of(REDIRECT_URI));
		try (Response response = realm.clients().create(client)) {
			assertThat(response.getStatus(), is(201));
		}
	}

	private static void createUser(RealmResource realm) {
		// the mobile number is stored in an attribute not declared in the user profile
		UPConfig upConfig = realm.users().userProfile().getConfiguration();
		upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
		realm.users().userProfile().update(upConfig);

		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(PASSWORD);
		credential.setTemporary(false);

		UserRepresentation user = new UserRepresentation();
		user.setUsername(USERNAME);
		user.setEmail("test@keycloak.de");
		user.setEmailVerified(true);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setEnabled(true);
		user.setCredentials(List.of(credential));
		user.setAttributes(Map.of("mobile_number", List.of("+491701234567")));
		try (Response response = realm.users().create(user)) {
			assertThat(response.getStatus(), is(201));
		}
	}

}
