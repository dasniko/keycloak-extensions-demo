package dasniko.keycloak.passkey.authentication;

import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

@Slf4j
public class WebAuthnConditionalAuthenticator extends WebAuthnPasswordlessAuthenticator {

	public WebAuthnConditionalAuthenticator(KeycloakSession session) {
		super(session);
	}

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		UsernamePasswordForm usernamePasswordForm = new UsernamePasswordForm();
		usernamePasswordForm.authenticate(context);

		super.authenticate(context);

		LoginFormsProvider form = context.form();
		form.setAttribute("login", new LoginBean(null)); // from LoginFormsProvider.createResponse(page)
		context.challenge(form.createForm("webauthn-conditional-authenticate.ftl"));
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

		String credentialTypeUsed = WebAuthnCredentialModel.TYPE_PASSWORDLESS;
		if (formData.containsKey("username") && formData.containsKey("password")) {
			UsernamePasswordForm usernamePasswordForm = new UsernamePasswordForm();
			usernamePasswordForm.action(context);
			credentialTypeUsed = PasswordCredentialModel.TYPE;
		} else {
			super.action(context);
		}
		context.getAuthenticationSession().setAuthNote("credentialTypeUsed", credentialTypeUsed);

	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

}
