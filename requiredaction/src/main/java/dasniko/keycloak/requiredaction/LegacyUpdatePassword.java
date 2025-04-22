package dasniko.keycloak.requiredaction;

import org.keycloak.Config;
import org.keycloak.authentication.requiredactions.UpdatePassword;

/**
 * @deprecated Since Keycloak v23 this is possible with native password policies.
 * No more need to maintain a custom override!
 */
@Deprecated
//@AutoService(RequiredActionFactory.class)
public class LegacyUpdatePassword extends UpdatePassword {

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
