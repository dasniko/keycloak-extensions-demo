package de.keycloak.credential.hash;

import com.google.auto.service.AutoService;
import org.keycloak.Config.Scope;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(PasswordHashProviderFactory.class)
public class SHA1HashProviderFactory implements PasswordHashProviderFactory {

	public static final String PROVIDER_ID = "sha1";

	@Override
	public PasswordHashProvider create(KeycloakSession session) {
		return new SHA1HashProvider(getId());
	}

	@Override
	public void init(Scope config) {
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
