package de.keycloak.provider;

import de.keycloak.util.BuildDetails;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

@SuppressWarnings("unused")
public interface DefaultServerInfoAware extends ServerInfoAwareProviderFactory {
    @Override
    default Map<String, String> getOperationalInfo() {
        return BuildDetails.get();
    }
}
