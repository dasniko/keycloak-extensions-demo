package de.keycloak.credential.hash;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SHA1HashProviderTest {

	@ParameterizedTest
	@CsvSource(value = {
		"test,a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
		"password,5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8",
		"mySuperStrongP4ssw0rd,1901d2be3b254e3f11f196c6093de30f340c6123"
	})
	public void testHash(String password, String hash) {
		SHA1HashProvider cut = new SHA1HashProvider();
		String encoded = cut.encodePassword(password);
		Assertions.assertEquals(hash, encoded);
	}

}
