package dasniko.keycloak.user.flintstones.repo;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

@AutoService(RealmResourceProviderFactory.class)
public class FlintstonesApiResourceProvider implements RealmResourceProviderFactory, RealmResourceProvider {

	public static final String PROVIDER_ID = "flintstones";

	private final FlintstonesRepository repository = new FlintstonesRepository();

	@Override
	public RealmResourceProvider create(KeycloakSession session) {
		return this;
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory sessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public Object getResource() {
		return new FlintstonesApiResource(repository);
	}
}
