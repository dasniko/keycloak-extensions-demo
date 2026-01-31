package dasniko.keycloak.authentication.broker;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.IdpConfirmOverrideLinkAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class IdpOverrideExistingLinkAuthenticator extends IdpConfirmOverrideLinkAuthenticator {

	@Override
	protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
		RealmModel realm = context.getRealm();
		KeycloakSession session = context.getSession();
		AuthenticationSessionModel authSession = context.getAuthenticationSession();

		UserModel user = getExistingUser(session, realm, authSession);

		String providerAlias = brokerContext.getIdpConfig().getAlias();
		FederatedIdentityModel federatedIdentity = session.users().getFederatedIdentity(realm, user, providerAlias);

		if (federatedIdentity != null) {
			authSession.setAuthNote(OVERRIDE_LINK, "true");
		}

		context.success();
	}
}
