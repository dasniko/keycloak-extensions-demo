package de.keycloak.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@RequiredArgsConstructor
public class HibpPasswordPolicyProvider implements PasswordPolicyProvider {

	private static final String HIBP_URL = "https://api.pwnedpasswords.com/range/";
	private static final String ERROR_MESSAGE = "invalidPasswordHibpMessage";

	private final KeycloakSession session;

	@Override
	public PolicyError validate(RealmModel realm, UserModel user, String password) {
		return validate(user.getUsername(), password);
	}

	@Override
	public PolicyError validate(String user, String password) {
		int maxOccurrences = session.getContext().getRealm().getPasswordPolicy().getPolicyConfig(HibpPasswordPolicyProviderFactory.PROVIDER_ID);
		if (maxOccurrences < 0) {
			throw new IllegalStateException("The maximum occurrences value must not be lower than 0.");
		}

		return checkPassword(password, maxOccurrences);
	}

	@Override
	public Object parseConfig(String value) {
		return parseInteger(value, 0);
	}

	@Override
	public void close() {
	}

	private PolicyError checkPassword(String password, int max) {
		String sha1 = DigestUtils.sha1Hex(password);
		String prefix = sha1.substring(0, 5);
		String suffix = sha1.substring(5).toUpperCase();

		try (SimpleHttp.Response response = SimpleHttp.doGet(HIBP_URL + prefix, session).asResponse()) {
			if (response.getStatus() == 200) {
				String body = response.asString();
				String[] lines = body.split("\r\n");
				for (String line : lines) {
					if (line.startsWith(suffix)) {
						String[] parts = line.split(":");
						if (parts.length == 2) {
							String count = parts[1];
							if (Integer.parseInt(count) > max) {
								return new PolicyError(ERROR_MESSAGE, count);
							}
						}
					}
				}
			} else {
				log.warn("Could not retrieve HIBP data. Status code: {}", response.getStatus());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return null;
	}

}
