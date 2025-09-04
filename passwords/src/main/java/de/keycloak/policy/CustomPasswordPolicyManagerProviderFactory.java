package de.keycloak.policy;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PasswordPolicyManagerProviderFactory;

@AutoService(PasswordPolicyManagerProviderFactory.class)
public class CustomPasswordPolicyManagerProviderFactory implements PasswordPolicyManagerProviderFactory {

	public static final String PROVIDER_ID = "custom";

	@Override
	public PasswordPolicyManagerProvider create(KeycloakSession session) {
		return new CustomPasswordPolicyManagerProvider(session);
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
