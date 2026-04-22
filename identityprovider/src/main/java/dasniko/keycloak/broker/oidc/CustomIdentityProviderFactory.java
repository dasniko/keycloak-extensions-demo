package dasniko.keycloak.broker.oidc;

import com.google.auto.service.AutoService;
import de.keycloak.provider.DefaultServerInfoAware;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

import static dasniko.keycloak.broker.oidc.CustomOIDCIdentityProviderConfig.SEND_LOGOUT_HINT_ON_LOGOUT;

@AutoService(IdentityProviderFactory.class)
public class CustomIdentityProviderFactory extends OIDCIdentityProviderFactory implements DefaultServerInfoAware {

	@Override
	public OIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
		return new CustomIdentityProvider(session, new OIDCIdentityProviderConfig(model));
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return ProviderConfigurationBuilder.create()
			.property()
			.name(SEND_LOGOUT_HINT_ON_LOGOUT)
			.label("Send 'logout_hint' in logout requests")
			.helpText("If enabled, the 'logout_hint' parameter in the logout request will be added with the value of the 'login_hint' parameter from the id_token.")
			.type(ProviderConfigProperty.BOOLEAN_TYPE)
			.add()
			.build();
	}

	@Override
	public int order() {
		return super.order() + 10;
	}
}
