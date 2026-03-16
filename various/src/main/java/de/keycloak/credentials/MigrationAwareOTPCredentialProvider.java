package de.keycloak.credentials;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.TimeBasedOTP;

import java.util.Base64;

@JBossLog
public class MigrationAwareOTPCredentialProvider extends OTPCredentialProvider {

	public MigrationAwareOTPCredentialProvider(KeycloakSession session) {
		super(session);
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
		if (!(credentialInput instanceof UserCredentialModel)) {
			return false;
		}
		String challengeResponse = credentialInput.getChallengeResponse();
		if (challengeResponse == null) {
			return false;
		}
		if (ObjectUtil.isBlank(credentialInput.getCredentialId())) {
			log.debugf("CredentialId is null when validating credential of user %s", user.getUsername());
			return false;
		}

		return user.credentialManager()
			.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
			.anyMatch(stored -> validateCredential(stored, challengeResponse, realm, user));
	}

	private boolean validateCredential(CredentialModel credential, String challengeResponse, RealmModel realm, UserModel user) {
		OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromCredentialModel(credential);
		OTPCredentialData credentialData = otpCredentialModel.getOTPCredentialData();
		OTPPolicy policy = realm.getOTPPolicy();

		if (OTPCredentialModel.HOTP.equals(credentialData.getSubType())) {
			HmacOTP validator = new HmacOTP(credentialData.getDigits(), credentialData.getAlgorithm(), policy.getLookAheadWindow());
			int counter = validator.validateHOTP(challengeResponse, otpCredentialModel.getDecodedSecret(), credentialData.getCounter());
			if (counter < 0) {
				return false;
			}
			otpCredentialModel.updateCounter(counter);
			user.credentialManager().updateStoredCredential(otpCredentialModel);
			return true;
		} else if (OTPCredentialModel.TOTP.equals(credentialData.getSubType())) {
			byte[] secretBytes = resolveSecretBytes(otpCredentialModel);
			final boolean isValid = isTotpValid(credentialData, secretBytes, challengeResponse, policy);

			if (isValid) {
				if (policy.isCodeReusable()) {
					return true;
				}

				SingleUseObjectProvider singleUseStore = session.singleUseObjects();
				final long validLifespan = (long) credentialData.getPeriod() * (2L * policy.getLookAheadWindow() + 1);
				final String searchKey = credential.getId() + "." + challengeResponse;

				return singleUseStore.putIfAbsent(searchKey, validLifespan);
			}
		}
		return false;
	}

	/* package-private for testability */
	boolean isTotpValid(OTPCredentialData credentialData, byte[] secretBytes, String enteredTotp, OTPPolicy policy) {
		TimeBasedOTP validator = new TimeBasedOTP(credentialData.getAlgorithm(), credentialData.getDigits(), credentialData.getPeriod(), policy.getLookAheadWindow());
		return validator.validateTOTP(enteredTotp, secretBytes);
	}

	/**
	 * Eindeutige Unterscheidung via Marker-Flag — keine Heuristik.
	 */
	byte[] resolveSecretBytes(OTPCredentialModel otpCredentialModel) {
		String storedValue = otpCredentialModel.getOTPSecretData().getValue();

		if (storedValue.startsWith("ext:")) {
			// Migriertes externes Secret → Base64-Decode der Raw-Bytes
			try {
				return Base64.getDecoder().decode(storedValue.substring(4));
			} catch (Exception e) {
				throw new IllegalStateException("Invalid Base64 value in migrated OTP credential.", e);
			}
		}

		// KC-nativer Wert
		return otpCredentialModel.getDecodedSecret();
	}
}
