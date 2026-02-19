package de.keycloak.policy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

import java.util.List;

@RequiredArgsConstructor
public class CustomPasswordPolicyManagerProvider implements PasswordPolicyManagerProvider {

	private final KeycloakSession session;

	@Setter(onParam_ = @NonNull)
	private List<String> policiesToSkip = List.of();

	@Override
	public PolicyError validate(RealmModel realm, UserModel user, String password) {
		PasswordPolicy policy = getPasswordPolicy(session, realm, user);

		try {
			RealmModel fakeRealm = new FakeRealm(realm, policy);
			session.getContext().setRealm(fakeRealm);

			for (PasswordPolicyProvider p : getProviders(session, fakeRealm)) {
				PolicyError policyError = p.validate(realm, user, password);
				if (policyError != null) {
					return policyError;
				}
			}
		} finally {
			session.getContext().setRealm(realm);
		}

		return null;
	}

	@Override
	public PolicyError validate(String user, String password) {
		for (PasswordPolicyProvider p : getProviders(session, session.getContext().getRealm())) {
			PolicyError policyError = p.validate(user, password);
			if (policyError != null) {
				return policyError;
			}
		}
		return null;
	}

	@Override
	public void close() {
	}

	private List<PasswordPolicyProvider> getProviders(KeycloakSession session, RealmModel realm) {
		return realm.getPasswordPolicy().getPolicies().stream()
			.filter(policiesToSkip::contains)
			.map(id -> session.getProvider(PasswordPolicyProvider.class, id))
			.toList();
	}

	private PasswordPolicy getPasswordPolicy(KeycloakSession session, RealmModel realm, UserModel user) {
		PasswordPolicy policy = realm.getPasswordPolicy();
		if (user != null && user.getFirstAttribute("passwordPolicy") != null) {
			policy = PasswordPolicy.parse(session,user.getFirstAttribute("passwordPolicy"));
		}
		return policy;
	}

}
