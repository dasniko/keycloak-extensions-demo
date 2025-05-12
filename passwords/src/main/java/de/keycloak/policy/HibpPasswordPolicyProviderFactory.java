package de.keycloak.policy;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;

@AutoService(PasswordPolicyProviderFactory.class)
public class HibpPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory {

	public static final String PROVIDER_ID = "hibp";

	@Override
	public String getDisplayName() {
		return "Have I Been Pwned? (BreachDB)";
	}

	@Override
	public String getConfigType() {
		return PasswordPolicyProvider.INT_CONFIG_TYPE;
	}

	@Override
	public String getDefaultConfigValue() {
		return "0";
	}

	@Override
	public boolean isMultiplSupported() {
		return false;
	}

	@Override
	public PasswordPolicyProvider create(KeycloakSession session) {
		return new HibpPasswordPolicyProvider(session);
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
