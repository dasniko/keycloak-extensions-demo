package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.UpdatePassword;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;


@AutoService(RequiredActionFactory.class)
public class CustomUpdatePassword extends UpdatePassword {

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;
	protected static final String DISALLOWED_IDENTITY_PROVIDERS = "disallowedIdPs";

	static {
		CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
			.property()
			.name(DISALLOWED_IDENTITY_PROVIDERS)
			.label("Disallowed for IdPs")
			.helpText("Disallow password update for users with a federated identity from on of the selected identity providers.")
			.type(ProviderConfigProperty.IDENTITY_PROVIDER_MULTI_LIST_TYPE)
			.add()
			.build();
	}

	@Override
	public List<ProviderConfigProperty> getConfigMetadata() {
		List<ProviderConfigProperty> properties = new ArrayList<>(super.getConfigMetadata());
		properties.addAll(CONFIG_PROPERTIES);
		return List.copyOf(properties);
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		if (isUpdatePasswordAllowed(context)) {
			super.requiredActionChallenge(context);
		}
	}

	@Override
	public void processAction(RequiredActionContext context) {
		if (isUpdatePasswordAllowed(context)) {
			super.processAction(context);
		}
	}

	@Override
	public int order() {
		return super.order() + 10;
	}

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return this;
	}

	private boolean isUpdatePasswordAllowed(RequiredActionContext context) {
		List<String> disallowedProviders = getDisallowedProviders(context);
		if (disallowedProviders.isEmpty()) return true;

		boolean userHasDisallowedFederatedIdentity = context.getSession().users()
			.getFederatedIdentitiesStream(context.getRealm(), context.getUser())
			.anyMatch(identity -> disallowedProviders.contains(identity.getIdentityProvider()));
		if (userHasDisallowedFederatedIdentity) {
			context.challenge(context.form().setError("credentialUpdateNotAllowed", PasswordCredentialModel.TYPE)
				.createErrorPage(Response.Status.FORBIDDEN));
			return false;
		}
		return true;
	}

	private List<String> getDisallowedProviders(RequiredActionContext context) {
		RequiredActionConfigModel config = context.getConfig();
		if (config == null) return List.of();

		String configValue = config.getConfigValue(DISALLOWED_IDENTITY_PROVIDERS, null);
		if (configValue == null || configValue.isBlank()) return List.of();
		return List.of(configValue.split(Constants.CFG_DELIMITER));
	}

}
