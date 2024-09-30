package dasniko.keycloak.actiontoken;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionTokenHandler;
import org.keycloak.services.managers.AuthenticationManager;

@AutoService(ActionTokenHandlerFactory.class)
public class CustomResetCredentialsActionTokenHandler extends ResetCredentialsActionTokenHandler {

	@Override
	public Response handleToken(ResetCredentialsActionToken token, ActionTokenContext tokenContext) {
		tokenContext.getAuthenticationSession().setAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
		return super.handleToken(token, tokenContext);
	}

	@Override
	public int order() {
		return super.order() + 1;
	}

}
