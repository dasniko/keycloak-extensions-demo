package dasniko.keycloak.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class JsonEmailTemplateProviderFactory implements EmailTemplateProviderFactory {

	public static final String PROVIDER_ID = "json";

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public EmailTemplateProvider create(KeycloakSession session) {
		return new JsonEmailTemplateProvider(session, mapper);
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
