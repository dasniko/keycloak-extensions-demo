package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.policy.MaxAuthAgePasswordPolicyProviderFactory;

@RequiredArgsConstructor
@AutoService(RequiredActionFactory.class)
public class UpdateTotp extends org.keycloak.authentication.requiredactions.UpdateTotp {

	private final KeycloakSession session;

	private boolean forceReauthentication = false;

	@Deprecated
	public UpdateTotp() {
		this(null);
	}

	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		return new UpdateTotp(session);
	}

	@Override
	public void init(Config.Scope config) {
		super.init(config);
		this.forceReauthentication = config.getBoolean("force-reauthentication", false);
	}

	@Override
	public int getMaxAuthAge() {

		if (session == null || !forceReauthentication) {
			// session is null, support for legacy implementation, fallback to default maxAuthAge
			return MaxAuthAgePasswordPolicyProviderFactory.DEFAULT_MAX_AUTH_AGE;
		}

		int maxAge = session.getContext().getRealm().getPasswordPolicy().getMaxAuthAge();
		if (maxAge < 0) {
			// passwordPolicy is not present fallback to default maxAuthAge
			return MaxAuthAgePasswordPolicyProviderFactory.DEFAULT_MAX_AUTH_AGE;
		}

		return maxAge;
	}

	@Override
	public int order() {
		return 100;
	}
}
