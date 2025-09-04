package de.keycloak.policy;

import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RealmModelDelegate;

class FakeRealm extends RealmModelDelegate {

	private PasswordPolicy passwordPolicy;

	FakeRealm(RealmModel delegate, PasswordPolicy passwordPolicy) {
		super(delegate);
		this.passwordPolicy = passwordPolicy;
	}

	@Override
	public PasswordPolicy getPasswordPolicy() {
		return passwordPolicy;
	}

	@Override
	public void setPasswordPolicy(PasswordPolicy policy) {
		this.passwordPolicy = policy;
	}

}
