package org.keycloak.broker.provider.util;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

public class SimpleHttpClient {

	public static SimpleHttp doGet(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doGet(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

	public static SimpleHttp doPost(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doPost(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

	public static SimpleHttp doPut(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doPut(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

	public static SimpleHttp doPatch(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doPatch(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

	public static SimpleHttp doDelete(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doDelete(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

	public static SimpleHttp doHead(String url, KeycloakSession session) {
		HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
		return SimpleHttp.doHead(url, provider.getHttpClient(), provider.getMaxConsumedResponseSize());
	}

}
