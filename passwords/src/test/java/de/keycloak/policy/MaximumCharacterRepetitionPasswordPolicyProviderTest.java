package de.keycloak.policy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.policy.PolicyError;

public class MaximumCharacterRepetitionPasswordPolicyProviderTest {

	@ParameterizedTest
	@CsvSource(value = {
		"hallo,2,true",
		"halllo,2,false",
		"halllo,3,true",
		"aaa,2,false",
		"aabb,2,true",
		"aabbb,2,false",
		"aabbb,3,true",
		"aabaa,2,true",
		"aa!!!bb,2,false"
	})
	public void testPolicy(String password, int max, boolean shouldBeValid) {
		PolicyError error = MaximumCharacterRepetitionPasswordPolicyProvider.checkPassword(password, max);
		if (shouldBeValid) {
			assert error == null;
		} else {
			assert error != null;
		}
	}
}
