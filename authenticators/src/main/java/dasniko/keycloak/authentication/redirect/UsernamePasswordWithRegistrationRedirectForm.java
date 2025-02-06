package dasniko.keycloak.authentication.redirect;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.Constants;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.LoginActionsService;

import java.net.URI;

@JBossLog
public class UsernamePasswordWithRegistrationRedirectForm extends UsernamePasswordForm {

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
		/* start custom code */
		String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
		if (username != null && !username.isBlank()) {
			boolean isValidUser = validateUser(context, formData);
			if (!isValidUser) {
				log.debugf("Unknown username '%s', redirecting to registration.", username);
				redirect(context);
				return;
			}
		}
		/* end custom code */
        if (!validateForm(context, formData)) {
            return;
        }
        context.success();
    }

	private void redirect(AuthenticationFlowContext context) {
		String clientId = context.getAuthenticationSession().getClient().getClientId();
		String tabId = context.getAuthenticationSession().getTabId();
		String clientData = AuthenticationProcessor.getClientData(context.getSession(), context.getAuthenticationSession());
		URI location = Urls.loginActionsBase(context.getUriInfo().getBaseUri())
			.path(LoginActionsService.class, "registerPage")
			.replaceQueryParam(Constants.CLIENT_ID, clientId)
			.replaceQueryParam(Constants.TAB_ID, tabId)
			.replaceQueryParam(Constants.CLIENT_DATA, clientData)
			.build(context.getRealm().getName());
		log.debugf("Redirecting to URL %s", location);
		Response response = Response.seeOther(location).build();
		context.forceChallenge(response);
	}

}
