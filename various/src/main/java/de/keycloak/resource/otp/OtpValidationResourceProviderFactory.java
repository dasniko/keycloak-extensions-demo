package de.keycloak.resource.otp;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

@AutoService(RealmResourceProviderFactory.class)
public class OtpValidationResourceProviderFactory implements RealmResourceProviderFactory {

	public static final String PROVIDER_ID = "otp-validation";

	@Override
	public RealmResourceProvider create(KeycloakSession session) {
		return new OtpValidationResourceProvider(session);
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
