package dasniko.keycloak.friendlycaptcha;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;


public class FriendlyCaptchaUsernamePasswordAuthenticator implements Authenticator {

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		// TODO implement me
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		// TODO implement me
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}

}
