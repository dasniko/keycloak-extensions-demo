package dasniko.keycloak.authentication.broker;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpConfirmOverrideLinkAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(AuthenticatorFactory.class)
public class IdpOverrideExistingLinkAuthenticatorFactory extends IdpConfirmOverrideLinkAuthenticatorFactory {

	public static final String PROVIDER_ID = "idp-override-existing-link";

	private static final Authenticator SINGLETON = new IdpOverrideExistingLinkAuthenticator();

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public String getDisplayType() {
		return "Automatically override existing IdP link";
	}

	@Override
	public String getReferenceCategory() {
		return "autoOverrideLink";
	}

	@Override
	public String getHelpText() {
		return "Automatically override the link if there is an existing broker user linked to the account.";
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
