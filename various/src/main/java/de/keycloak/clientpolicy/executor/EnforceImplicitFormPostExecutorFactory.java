package de.keycloak.clientpolicy.executor;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

import java.util.List;

@AutoService(ClientPolicyExecutorProviderFactory.class)
public class EnforceImplicitFormPostExecutorFactory implements ClientPolicyExecutorProviderFactory {

	public static final String PROVIDER_ID = "enforce-implicit-form-post";

	@Override
	public String getHelpText() {
		return "";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return List.of();
	}

	@Override
	public EnforceImplicitFormPostExecutor create(KeycloakSession session) {
		return new EnforceImplicitFormPostExecutor();
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
