package dasniko.keycloak.requiredaction;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MobileNumberRequiredActionFactory implements RequiredActionFactory {

	@Override
	public RequiredActionProvider create(KeycloakSession keycloakSession) {
		return new MobileNumberRequiredAction();
	}

	@Override
	public String getDisplayText() {
		return "Update mobile number";
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return MobileNumberRequiredAction.PROVIDER_ID;
	}

}
