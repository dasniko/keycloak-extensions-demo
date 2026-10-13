package dasniko.keycloak.broker.oidc;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class CustomOIDCIdentityProviderConfig extends OIDCIdentityProviderConfig {

	static final String SEND_LOGOUT_HINT_ON_LOGOUT = "sendLogoutHintOnLogout";
	static final String LOG_TOKENS = "logTokens";

	public CustomOIDCIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
		super(identityProviderModel);
	}

	boolean isSendLogoutHintOnLogout() {
		return Boolean.parseBoolean(getConfig().get(SEND_LOGOUT_HINT_ON_LOGOUT));
	}

	boolean isLogTokens() {
		return Boolean.parseBoolean(getConfig().get(LOG_TOKENS));
	}
}
