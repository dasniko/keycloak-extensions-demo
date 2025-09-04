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

import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class CustomPasswordPolicyManagerProvider implements PasswordPolicyManagerProvider {

	private final KeycloakSession session;

	@Setter(onParam_ = @NonNull)
	private List<String> policiesToSkip = List.of();

	@Override
	public PolicyError validate(RealmModel realm, UserModel user, String password) {
		return null;
	}

	@Override
	public PolicyError validate(String user, String password) {
		for (PasswordPolicyProvider p : getProviders(session)) {
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

	private List<PasswordPolicyProvider> getProviders(KeycloakSession session) {
		return getProviders(session, session.getContext().getRealm());
	}

	private List<PasswordPolicyProvider> getProviders(KeycloakSession session, RealmModel realm) {
		LinkedList<PasswordPolicyProvider> list = new LinkedList<>();
		PasswordPolicy policy = realm.getPasswordPolicy();
		for (String id : policy.getPolicies()) {
			if (!policiesToSkip.contains(id)) {
				PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, id);
				list.add(provider);
			}
		}
		return list;
	}

}
