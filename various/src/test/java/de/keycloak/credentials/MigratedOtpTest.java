package de.keycloak.credentials;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.credential.OTPCredentialModel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigratedOtpTest {

	@ParameterizedTest
	@ValueSource(strings = {
		"J22P3RMFN42NLYA4ITC6UWB7ALXRP4FC",
		"4AX6H2BEF4F2ZKZAM7AONSB6TUMPZA25",
		"DYP77T5HPGNQLMBIG7WFOGH3XRZV4FDQ",
		"IVDT62FYUP35XJVA5E72Y4EYVWLEUMD4",
		"NQD2BBB32LEJQI2H",
		"IOM6JCJ5AHZKIYNX",
	})
	public void test(String seed) throws Exception {
		System.out.println("Seed: " + seed);
		String totp = computeTotpFromExternalSeed(seed);
		System.out.println("TOTP: " + totp);

		OtpMigrationService service = new OtpMigrationService();
		String secret = "ext:" + service.createSecretFromExternalBase32Seed(seed);
		System.out.println("Secret: " + secret);

		OTPCredentialModel otpCredentialModel = OTPCredentialModel.createTOTP(secret, 6, 30, "HmacSHA1");
		MigrationAwareOTPCredentialProvider provider = new MigrationAwareOTPCredentialProvider(null);
		byte[] secretBytes = provider.resolveSecretBytes(otpCredentialModel);
		boolean isValid = provider.isTotpValid(otpCredentialModel.getOTPCredentialData(), secretBytes, totp, getPolicy());
		assertTrue(isValid);
	}

	private String computeTotpFromExternalSeed(String base32Seed) throws Exception {
		byte[] rawBytes = new Base32().decode(base32Seed.toUpperCase());

		long timeStep = Instant.now().getEpochSecond() / 30L;
		byte[] msg = new byte[8];
		for (int i = 7; i >= 0; i--) {
			msg[i] = (byte) (timeStep & 0xff);
			timeStep >>= 8;
		}
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(new SecretKeySpec(rawBytes, "RAW"));
		byte[] hash = mac.doFinal(msg);

		int offset = hash[hash.length - 1] & 0x0F;
		int code = ((hash[offset]     & 0x7F) << 24)
			| ((hash[offset + 1] & 0xFF) << 16)
			| ((hash[offset + 2] & 0xFF) <<  8)
			| ((hash[offset + 3] & 0xFF));
		return String.format("%06d", code % 1_000_000);
	}

	private OTPPolicy getPolicy() {
		OTPPolicy policy = new OTPPolicy();
		policy.setDigits(6);
		policy.setPeriod(30);
		policy.setAlgorithm("SHA1");
		policy.setLookAheadWindow(1);
		return policy;
	}
}
