package de.keycloak.login;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.AuthorizationCodeGrant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@Testcontainers
public class AuthCodeGrantTest {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer();

	@Test
	public void testAuthCodeGrant() {
		String baseUrl = keycloak.getAuthServerUrl();
		String realm = KeycloakContainer.MASTER_REALM;
		String clientId = "account-console";
		String clientSecret = null;
		String redirectUri = "%s/realms/%s/account".formatted(baseUrl, realm);
		String username = keycloak.getAdminUsername();
		String password = keycloak.getAdminPassword();

		AccessTokenResponse tokenResponse =
			AuthorizationCodeGrant.getTokenResponse(baseUrl, realm, clientId, clientSecret, redirectUri, username, password);
		log.info(tokenResponse.getToken());
	}
}
