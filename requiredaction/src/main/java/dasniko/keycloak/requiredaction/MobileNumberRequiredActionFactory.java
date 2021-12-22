package dasniko.keycloak.requiredaction;

import org.keycloak.Config;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MobileNumberRequiredActionFactory implements RequiredActionFactory, DisplayTypeRequiredActionFactory {

	@Override
	public RequiredActionProvider create(KeycloakSession keycloakSession) {
		return new MobileNumberRequiredAction();
	}

	@Override
	public RequiredActionProvider createDisplay(KeycloakSession keycloakSession, String displayType) {
		return create(keycloakSession);
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
