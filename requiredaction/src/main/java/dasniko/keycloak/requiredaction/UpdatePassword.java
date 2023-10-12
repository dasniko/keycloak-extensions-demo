package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;

@AutoService(RequiredActionFactory.class)
public class UpdatePassword extends org.keycloak.authentication.requiredactions.UpdatePassword {

	private boolean forceReauthentication;

	@Override
	public int getMaxAuthAge() {
		return forceReauthentication ? 0 : super.getMaxAuthAge();
	}

	@Override
	public void init(Config.Scope config) {
		super.init(config);
		this.forceReauthentication = config.getBoolean("force-reauthentication", false);
	}

	@Override
	public int order() {
		return 100;
	}
}
