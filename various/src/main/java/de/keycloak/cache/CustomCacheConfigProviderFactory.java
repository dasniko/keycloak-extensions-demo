package de.keycloak.cache;

import com.google.auto.service.AutoService;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.spi.infinispan.CacheEmbeddedConfigProviderFactory;
import org.keycloak.spi.infinispan.impl.embedded.DefaultCacheEmbeddedConfigProviderFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@AutoService(CacheEmbeddedConfigProviderFactory.class)
public class CustomCacheConfigProviderFactory extends DefaultCacheEmbeddedConfigProviderFactory {

	public static final String CACHE_NAME = "my-cache";

	/**
	 * Using the cache:
	 * Cache<String, String> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("my-cache");
	 * String v = cache.computeIfAbsent("key", k -> "value");
	 */

	@Override
	protected ConfigurationBuilderHolder createConfiguration(KeycloakSessionFactory factory) throws IOException {
		ConfigurationBuilderHolder holder = super.createConfiguration(factory);
		ConfigurationBuilder builder = holder.newConfigurationBuilder(CACHE_NAME);
		boolean clustered = holder.getGlobalConfigurationBuilder()
			.build().transport().transport() != null;
		if (clustered) {
			builder.clustering().cacheMode(CacheMode.DIST_SYNC)
				.hash().numOwners(2);
		} else {
			builder.clustering().cacheMode(CacheMode.LOCAL);
		}
		builder.expiration().lifespan(5, TimeUnit.SECONDS)
			.memory().maxCount(10);
		return holder;
	}

	@Override
	public int order() {
		return super.order() + 10;
	}
}
