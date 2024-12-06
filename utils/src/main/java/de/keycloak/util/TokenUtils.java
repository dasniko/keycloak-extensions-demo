package de.keycloak.util;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.function.Consumer;

import static org.keycloak.models.UserSessionModel.SessionPersistenceState.TRANSIENT;

@SuppressWarnings("unused")
public class TokenUtils {

	public static String generateServiceAccountAccessToken(KeycloakSession session, String clientId, String scope, Consumer<AccessToken> tokenAdjuster) {

		var context = session.getContext();
		var realm = context.getRealm();
		var client = session.clients().getClientByClientId(realm, clientId);

		if (client == null) {
			throw new IllegalStateException("client not found");
		}

		if (!client.isServiceAccountsEnabled()) {
			throw new IllegalStateException("service account not enabled");
		}

		var clientUser = session.users().getServiceAccount(client);
		var clientUsername = clientUser.getUsername();

		// we need to remember the current authSession since createAuthenticationSession changes the current authSession in the context
		var currentAuthSession = context.getAuthenticationSession();
		var currentClient = context.getClient();

		try {
			context.setClient(client);
			var rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
			var authSession = rootAuthSession.createAuthenticationSession(client);

			authSession.setAuthenticatedUser(clientUser);
			authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
			authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
			authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

			var clientConnection = context.getConnection();
			var sessionId = authSession.getParentSession().getId();
			var remoteAddr = clientConnection.getRemoteAddr();
			var userSession = session.sessions().createUserSession(sessionId, realm, clientUser, clientUsername, //
				remoteAddr, ServiceAccountConstants.CLIENT_AUTH, false, null, null, TRANSIENT);

			AuthenticationManager.setClientScopesInSession(session, authSession);
			var clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

			// Notes about client details
			userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
			userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
			userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, remoteAddr);

			var tokenManager = new TokenManager();
			var event = new EventBuilder(realm, session, clientConnection);
			var responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSessionCtx);
			responseBuilder.generateAccessToken();

			if (tokenAdjuster != null) {
				tokenAdjuster.accept(responseBuilder.getAccessToken());
			}

			var accessTokenResponse = responseBuilder.build();
			return accessTokenResponse.getToken();
		} finally {
			// reset current authentication session
			context.setAuthenticationSession(currentAuthSession);
			context.setClient(currentClient);
		}
	}

	public static String generateAccessToken(KeycloakSession session, UserSessionModel userSession, String clientId, String scope, Consumer<AccessToken> tokenAdjuster) {

		KeycloakContext context = session.getContext();
		RealmModel realm = userSession.getRealm();
		ClientModel client = session.clients().getClientByClientId(realm, clientId);
		String issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

		RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
		AuthenticationSessionModel iamAuthSession = rootAuthSession.createAuthenticationSession(client);

		iamAuthSession.setAuthenticatedUser(userSession.getUser());
		iamAuthSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		iamAuthSession.setClientNote(OIDCLoginProtocol.ISSUER, issuer);
		iamAuthSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

		ClientConnection connection = context.getConnection();
		UserSessionModel iamUserSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, userSession.getUser(), userSession.getUser().getUsername(), connection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, TRANSIENT);

		AuthenticationManager.setClientScopesInSession(session, iamAuthSession);
		ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, iamUserSession, iamAuthSession);

		// Notes about client details
		userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
		userSession.setNote(ServiceAccountConstants.CLIENT_HOST, connection.getRemoteHost());
		userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, connection.getRemoteAddr());

		TokenManager tokenManager = new TokenManager();

		EventBuilder eventBuilder = new EventBuilder(realm, session, connection);
		TokenManager.AccessTokenResponseBuilder tokenResponseBuilder = tokenManager.responseBuilder(realm, client, eventBuilder, session, iamUserSession, clientSessionCtx);
		AccessToken accessToken = tokenResponseBuilder.generateAccessToken().getAccessToken();

		if (tokenAdjuster != null) {
			tokenAdjuster.accept(accessToken);
		}

		AccessTokenResponse tokenResponse = tokenResponseBuilder.build();

		return tokenResponse.getToken();
	}

	public static String generateAccessToken(KeycloakSession session, RealmModel realm, UserModel user, String clientId, String scope, Consumer<AccessToken> tokenAdjuster) {

		KeycloakContext context = session.getContext();
		ClientModel client = session.clients().getClientByClientId(realm, clientId);
		String issuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

		RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
		AuthenticationSessionModel iamAuthSession = rootAuthSession.createAuthenticationSession(client);

		iamAuthSession.setAuthenticatedUser(user);
		iamAuthSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
		iamAuthSession.setClientNote(OIDCLoginProtocol.ISSUER, issuer);
		iamAuthSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

		ClientConnection connection = context.getConnection();
		UserSessionModel iamUserSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, user, user.getUsername(), connection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, TRANSIENT);

		AuthenticationManager.setClientScopesInSession(session, iamAuthSession);
		ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, iamUserSession, iamAuthSession);

		// Notes about client details
		iamUserSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
		iamUserSession.setNote(ServiceAccountConstants.CLIENT_HOST, connection.getRemoteHost());
		iamUserSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, connection.getRemoteAddr());

		TokenManager tokenManager = new TokenManager();

		EventBuilder eventBuilder = new EventBuilder(realm, session, connection);
		TokenManager.AccessTokenResponseBuilder tokenResponseBuilder = tokenManager.responseBuilder(realm, client, eventBuilder, session, iamUserSession, clientSessionCtx);
		AccessToken accessToken = tokenResponseBuilder.generateAccessToken().getAccessToken();

		if (tokenAdjuster != null) {
			tokenAdjuster.accept(accessToken);
		}

		AccessTokenResponse tokenResponse = tokenResponseBuilder.build();

		return tokenResponse.getToken();
	}
}
