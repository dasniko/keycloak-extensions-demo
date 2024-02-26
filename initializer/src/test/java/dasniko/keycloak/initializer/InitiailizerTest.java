package dasniko.keycloak.initializer;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.keycloak.test.TestBase;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

@Testcontainers
public class InitiailizerTest extends TestBase {

	public static final String ISSUER = "https://auth.keycloak.de";

	private static final List<File> dependencies = Maven.resolver()
			.loadPomFromFile("./pom.xml")
			.resolve("net.bytebuddy:byte-buddy-agent")
			.withoutTransitivity().asList(File.class);

	@ParameterizedTest
	@ValueSource(strings = { ISSUER, "" })
	public void testIssuer(String issuerValue) {
		final KeycloakContainer keycloak = new KeycloakContainer()
			.withProviderClassesFrom("target/classes")
			.withProviderLibsFrom(dependencies)
			.withEnv("KC_SPI_INITIALIZER_ISSUER_BASE_URI", issuerValue)
//			.withDebugFixedPort(8787, true)
			;
		keycloak.start();

		String issuer = getOpenIDConfiguration(keycloak, "master").extract().path("issuer");
		if (issuerValue.isEmpty()) {
			assertThat(issuer, startsWith(keycloak.getAuthServerUrl()));
		} else {
			assertThat(issuer, startsWith(issuerValue));
		}

		keycloak.stop();
	}

}
