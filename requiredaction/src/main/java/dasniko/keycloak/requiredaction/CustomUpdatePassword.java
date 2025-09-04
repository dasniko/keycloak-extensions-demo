package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.UpdatePassword;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;


@AutoService(RequiredActionFactory.class)
public class CustomUpdatePassword extends UpdatePassword {

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;
	protected static final String DISALLOW_FOR_FEDERATED_IDENTITIES = "disallowForFedId";

	static {
		CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
			.property()
			.name(DISALLOW_FOR_FEDERATED_IDENTITIES)
			.label("Disallow for federated identities")
			.helpText("...")
			.type(ProviderConfigProperty.BOOLEAN_TYPE)
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
		if (!isUpdatePasswordDisallowedForFederatedIdentitiesByConfig(context)) return true;

		KeycloakSession session = context.getSession();
		boolean userHasFederatedIdentity = session.users().getFederatedIdentitiesStream(context.getRealm(), context.getUser())
			.anyMatch(identity -> session.identityProviders().getByAlias(identity.getIdentityProvider()) != null);
		if (userHasFederatedIdentity) {
			context.challenge(context.form().setError("passwordUpdateNotAllowed").createErrorPage(Response.Status.FORBIDDEN));
			return false;
		}
		return true;
	}

	private boolean isUpdatePasswordDisallowedForFederatedIdentitiesByConfig(RequiredActionContext context) {
		return Boolean.parseBoolean(context.getConfig().getConfigValue(DISALLOW_FOR_FEDERATED_IDENTITIES, Boolean.toString(false)));
	}

}
