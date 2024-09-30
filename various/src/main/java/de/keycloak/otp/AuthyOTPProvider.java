package de.keycloak.otp;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.otp.OTPApplicationProvider;
import org.keycloak.authentication.otp.OTPApplicationProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;

@AutoService(OTPApplicationProviderFactory.class)
public class AuthyOTPProvider implements OTPApplicationProviderFactory, OTPApplicationProvider {

    @Override
    public OTPApplicationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return "authy";
    }

    @Override
    public String getName() {
        return "totpAppAuthyName";
    }

    @Override
    public boolean supports(OTPPolicy policy) {
			if (policy.getDigits() != 6) {
				return false;
			}

			if (!policy.getAlgorithm().equals("HmacSHA1")) {
            return false;
        }

        return policy.getType().equals("totp") && policy.getPeriod() == 30;
    }

    @Override
    public void close() {
    }

}
