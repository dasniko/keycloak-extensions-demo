package de.keycloak.credentials;

import org.apache.commons.codec.binary.Base32;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class OtpMigrationService {

	private static final Base32 BASE32 = new Base32();

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * OTPPolicy.getAlgorithm() → "SHA1", "SHA256", "SHA512"
	 * Mac.getInstance()        → "HmacSHA1", "HmacSHA256", "HmacSHA512"
	 */
	private static String toMacAlgorithm(String policyAlgorithm) {
		if (policyAlgorithm == null) {
			return "HmacSHA1";
		}
		if (policyAlgorithm.startsWith("Hmac")) {
			return policyAlgorithm;
		}
		return switch (policyAlgorithm.toUpperCase()) {
			case "SHA1" -> "HmacSHA1";
			case "SHA256" -> "HmacSHA256";
			case "SHA512" -> "HmacSHA512";
			default -> throw new IllegalArgumentException("Unknown OTP algorithm: " + policyAlgorithm);
		};
	}

	private static boolean isValidBase32Seed(String seed) {
		try {
			byte[] decoded = BASE32.decode(seed.toUpperCase(Locale.US).replaceAll("\\s", ""));
			return decoded.length > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public String migrateOtpSeed(RealmModel realm, UserModel user, String externalBase32Seed) {

		if (!isValidBase32Seed(externalBase32Seed)) {
			throw new IllegalArgumentException("Invalid Base32 seed: " + externalBase32Seed);
		}

		removeExistingOtpCredentials(user);

		String secretValue = createSecretFromExternalBase32Seed(externalBase32Seed);

		OTPPolicy policy = realm.getOTPPolicy();
		OTPCredentialModel model = OTPCredentialModel.createTOTP(
			"ext:" + secretValue,
			policy.getDigits(),
			policy.getPeriod(),
			toMacAlgorithm(policy.getAlgorithm())
		);
		model.setUserLabel("Migrated from external system");

		CredentialModel stored = user.credentialManager().createStoredCredential(model);

		// verification: bytes have to identical after save
		byte[] storedBytes;
		String storedValue = OTPCredentialModel.createFromCredentialModel(stored).getOTPSecretData().getValue();

		if (storedValue.startsWith("ext:")) {
			storedBytes = Base64.getDecoder().decode(storedValue.substring(4));
		} else {
			storedBytes = storedValue.getBytes(StandardCharsets.US_ASCII);
		}

		byte[] rawBytes = getRawBytesFromExternalBase32Seed(externalBase32Seed);
		if (!Arrays.equals(rawBytes, storedBytes)) {
			throw new IllegalStateException(
				"Bytes after saving not identical! expected: %s, saved: %s".formatted(bytesToHex(rawBytes), bytesToHex(storedBytes)));
		}

		return stored.getId();
	}

	byte[] getRawBytesFromExternalBase32Seed(String externalBase32Seed) {
		return BASE32.decode(externalBase32Seed.toUpperCase(Locale.US).replaceAll("\\s", ""));
	}

	String createSecretFromExternalBase32Seed(String externalBase32Seed) {
		return Base64.getEncoder().encodeToString(getRawBytesFromExternalBase32Seed(externalBase32Seed));
	}

	private void removeExistingOtpCredentials(UserModel user) {
		List<CredentialModel> existing = user.credentialManager()
			.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
			.toList();
		existing.forEach(c ->
			user.credentialManager().removeStoredCredentialById(c.getId()));
	}
}
