package de.keycloak.credential;

import de.keycloak.policy.CustomPasswordPolicyManagerProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;

import java.util.List;

@JBossLog
public class CustomPasswordCredentialProvider extends PasswordCredentialProvider {

    public CustomPasswordCredentialProvider(KeycloakSession session, Meter.MeterProvider<Counter> meterProvider, boolean metricsEnabled, boolean withRealmInMetric, boolean withAlgorithmInMetric, boolean withHashingStrengthInMetric, boolean withOutcomeInMetric) {
        super(session, meterProvider, metricsEnabled, withRealmInMetric, withAlgorithmInMetric, withHashingStrengthInMetric, withOutcomeInMetric);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        boolean isValid = super.isValid(realm, user, input);

        if (isValid) {
            // After the password has been validated successfully, check that it still matches the realm's password policy.
            if (!passwordConformsToPolicy(session, realm, user, input.getChallengeResponse())) {
                log.debug("User password no longer matches password policy, enforce update password action.");
                addUpdatePasswordAction(user);
            }
        }

        return isValid;
    }

	private boolean passwordConformsToPolicy(KeycloakSession session, RealmModel realm, UserModel user, String password) {
		PasswordPolicyManagerProvider pwdPolicyManager = session.getProvider(PasswordPolicyManagerProvider.class);
		if (pwdPolicyManager instanceof CustomPasswordPolicyManagerProvider) {
			((CustomPasswordPolicyManagerProvider) pwdPolicyManager).setPoliciesToSkip(List.of(PasswordPolicy.PASSWORD_HISTORY_ID));
		}
		PolicyError policyError = pwdPolicyManager.validate(realm, user, password);
		return policyError == null;
	}

	private void addUpdatePasswordAction(UserModel user) {
		if (user.getRequiredActionsStream().noneMatch(s -> s.contains(UserModel.RequiredAction.UPDATE_PASSWORD.name()))) {
			user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
		}
	}
}
