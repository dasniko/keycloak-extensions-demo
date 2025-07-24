package dasniko.keycloak.resource;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.function.Function;

public class AuthHelper {

	public static AuthenticationManager.AuthResult getAuthResult(KeycloakSession session, Function<AuthenticationManager.AuthResult, Boolean> authFn) {
		AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
		if (auth == null) {
			throw new NotAuthorizedException("Bearer");
		} else if (!authFn.apply(auth)) {
			throw new ForbiddenException();
		}
		return auth;
	}

	public static Auth getAuth(KeycloakSession session, Function<AuthenticationManager.AuthResult, Boolean> authFn) {
		return getAuth(session, getAuthResult(session, authFn));
	}

	public static Auth getAuth(KeycloakSession session, String clientId, Function<AuthenticationManager.AuthResult, Boolean> authFn) {
		return getAuth(session, getAuthResult(session, authFn), clientId);
	}

	public static Auth getAuth(KeycloakSession session, AuthenticationManager.AuthResult authResult) {
		return getAuth(session, authResult, null);
	}

	public static Auth getAuth(KeycloakSession session, AuthenticationManager.AuthResult authResult, String clientId) {
		RealmModel realm = session.getContext().getRealm();
		ClientModel client;
		if (clientId == null) {
			client = authResult.getClient();
		} else {
			client = realm.getClientByClientId(clientId);
		}
		return new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), false);
	}

}
