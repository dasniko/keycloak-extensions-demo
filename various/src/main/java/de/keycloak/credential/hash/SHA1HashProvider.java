package de.keycloak.credential.hash;

import lombok.RequiredArgsConstructor;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
public class SHA1HashProvider implements PasswordHashProvider {

	private final String providerId;

	@Override
	public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
		return this.providerId.equals(credential.getPasswordCredentialData().getAlgorithm());
	}

	@Override
	public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
		String encodedPassword = encodePassword(rawPassword);
		return PasswordCredentialModel.createFromValues(this.providerId, null, iterations, encodedPassword);
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
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(rawPassword.getBytes(StandardCharsets.UTF_8));
			byte[] digest = md.digest();

			// convert the digest byte[] to BigInteger
			BigInteger aux = new BigInteger(1, digest);
			// convert BigInteger to 40-char lowercase string using leading 0s
			return String.format("%040x", aux);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Credential could not be encoded", e);
		}
	}

}
