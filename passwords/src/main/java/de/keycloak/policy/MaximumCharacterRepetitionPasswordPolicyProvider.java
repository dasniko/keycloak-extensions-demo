package de.keycloak.policy;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

@RequiredArgsConstructor
public class MaximumCharacterRepetitionPasswordPolicyProvider implements PasswordPolicyProvider {

	private static final String ERROR_MESSAGE = "invalidPasswordMaximumCharacterRepetitionMessage";

	private final KeycloakContext context;

	@Override
	public PolicyError validate(RealmModel realm, UserModel user, String password) {
		return validate(user.getUsername(), password);
	}

	@Override
	public PolicyError validate(String user, String password) {
		int max = context.getRealm().getPasswordPolicy().getPolicyConfig(MaximumCharacterRepetitionPasswordPolicyProviderFactory.PROVIDER_ID);
		if (max < 1) {
			throw new IllegalStateException("The max. repetitive count value must not be lower than 1.");
		}

		return checkPassword(password, max);
	}

	@Override
	public Object parseConfig(String value) {
		return parseInteger(value, 2);
	}

	@Override
	public void close() {
	}

	protected static PolicyError checkPassword(String password, int max) {
		int count = 1;
		for (int i = 1; i < password.length(); i++) {
			count = (password.charAt(i) == password.charAt(i - 1)) ? count + 1 : 1;
			if (count > max) {
				return new PolicyError(ERROR_MESSAGE, max);
			}
		}
		return null;
	}

}
