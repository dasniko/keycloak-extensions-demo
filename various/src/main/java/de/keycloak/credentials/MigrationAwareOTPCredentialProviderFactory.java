package de.keycloak.credentials;

import com.google.auto.service.AutoService;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(CredentialProviderFactory.class)
public class MigrationAwareOTPCredentialProviderFactory implements CredentialProviderFactory<MigrationAwareOTPCredentialProvider> {

	@Override
	public MigrationAwareOTPCredentialProvider create(KeycloakSession session) {
		return new MigrationAwareOTPCredentialProvider(session);
	}

	/**
	 * Gleiche ID wie Standard-Provider → überschreibt diesen vollständig.
	 */
	@Override
	public String getId() {
		return OTPCredentialProviderFactory.PROVIDER_ID;
	}

	@Override
	public int order() {
		return CredentialProviderFactory.super.order() + 10;
	}
}
