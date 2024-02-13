package dasniko.keycloak.passkey.authentication;

import de.keycloak.util.AuthenticationMethodReference;
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

		AuthenticationMethodReference authenticationMethodReference = AuthenticationMethodReference.USER;
		if (formData.containsKey("username") && formData.containsKey("password")) {
			UsernamePasswordForm usernamePasswordForm = new UsernamePasswordForm();
			usernamePasswordForm.action(context);
			authenticationMethodReference = AuthenticationMethodReference.PWD;
		} else {
			super.action(context);
		}
		context.getAuthenticationSession().setAuthNote("amr", authenticationMethodReference.value);

	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

}
