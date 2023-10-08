package dasniko.keycloak.passkey.authentication;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;

@AutoService(AuthenticatorFactory.class)
public class WebAuthnConditionalAuthenticatorFactory extends WebAuthnPasswordlessAuthenticatorFactory {

	public static final String PROVIDER_ID = "webauthn-conditional-auth";

	@Override
	public String getDisplayType() {
		return "WebAuthn Conditional Authenticator (Passkey or Username/Password)";
	}

	@Override
	public String getHelpText() {
		return "Authenticator which uses either Passkey/WebAuthn or Username & Password, depending on what user chooses and browser supports.";
	}

	private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
		AuthenticationExecutionModel.Requirement.REQUIRED
	};

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return new WebAuthnConditionalAuthenticator(session);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
