package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.RequiredActionFactory;

@AutoService(RequiredActionFactory.class)
public class UpdatePassword extends org.keycloak.authentication.requiredactions.UpdatePassword {

	@Override
	public int getMaxAuthAge() {
		return 0;
	}

	@Override
	public int order() {
		return 100;
	}
}
