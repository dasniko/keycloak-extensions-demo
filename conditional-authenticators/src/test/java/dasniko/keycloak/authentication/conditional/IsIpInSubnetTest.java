package dasniko.keycloak.authentication.conditional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class IsIpInSubnetTest {

	private final ConditionalCidrAuthenticator cut = new ConditionalCidrAuthenticator();

	@ParameterizedTest
	@CsvSource({
		"1.2.3.4, 0.0.0.0/0, true",
		"1.2.3.4, 1.2.3.0/24, true",
		"1.2.3.4, 1.2.4.0/24, false",
		"1.2.3.4, 1.2.3.4, true",
		"127.0.0.1, 127.0.0.1/32, true",
		"127.1.0.1, 127.1.0.2/32, false",
		"66.249.95.200, 66.249.80.0/20, true",
		"164.0.0.1, 66.102.0.0/20, false",
		"216.239.16.255, 216.239.0.0/16, true",
		"216.58.192.11, 216.58.192.0/19, true",
		"207.126.159.255, 207.126.144.0/20, true",
		"55.55.55.55, 55.55.55.0/24, true",
	})
	public void testIsIpInSubnet(String ip, String subnet, boolean expected) {
		boolean result = cut.isIpInSubnet(ip, subnet);
		Assertions.assertEquals(expected, result);
	}

}
