package de.keycloak.credential.hash;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

@RequiredArgsConstructor
public class SHA1HashProvider implements PasswordHashProvider {

	@Override
	public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
		return SHA1HashProviderFactory.PROVIDER_ID.equals(credential.getPasswordCredentialData().getAlgorithm());
	}

	@Override
	public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
		String encodedPassword = encodePassword(rawPassword);
		return PasswordCredentialModel.createFromValues(SHA1HashProviderFactory.PROVIDER_ID, null, iterations, encodedPassword);
	}

	@Override
	public boolean verify(String rawPassword, PasswordCredentialModel credential) {
		String encodedPassword = encodePassword(rawPassword);
		String hash = credential.getPasswordSecretData().getValue();
		return encodedPassword.equals(hash);
	}

	@Override
	public void close() {
	}

	protected String encodePassword(String rawPassword) {
		return DigestUtils.sha1Hex(rawPassword);
	}

}
