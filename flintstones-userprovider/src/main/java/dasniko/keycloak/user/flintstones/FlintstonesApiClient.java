package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import de.keycloak.util.ThrowingFunction;
import de.keycloak.util.TokenUtils;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.tracing.TracingProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
public class FlintstonesApiClient {

	private final SimpleHttp simpleHttp;
	private final String baseUrl;
	private final String token;
	private final TracingProvider tracing;

	public FlintstonesApiClient(KeycloakSession session, ComponentModel model) {
		this.simpleHttp = SimpleHttp.create(session);
		this.baseUrl = model.get(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL);
		String clientId = model.get(FlintstonesUserStorageProviderFactory.CLIENT_ID);
		this.token = clientId != null ? TokenUtils.getServiceAccountToken(session, clientId) : "";
		this.tracing = session.getProvider(TracingProvider.class);
	}

	public List<FlintstoneUser> searchUsers(Map<String, String> params, Integer first, Integer max) {
		return trace("searchUsers", () -> {
			String url = String.format("%s/users", baseUrl);
			return searchUsersRequest(url, params, first, max);
		});
	}

	public Integer usersCount(Map<String, String> params) {
		return trace("usersCount", () -> {
			String url = String.format("%s/users/count", baseUrl);
			SimpleHttpRequest request = prepareGetRequest(url);
			params.forEach(request::param);

			return handleRequest(request, response -> {
				Map<String, Integer> payload = response.asJson(new TypeReference<>() {});
				return payload.getOrDefault("count", 0);
			});
		});
	}

	public FlintstoneUser createUser(FlintstoneUser user) {
		return trace("createUser", () -> {
			String url = String.format("%s/users", baseUrl);
			SimpleHttpRequest request = simpleHttp.doPost(url).auth(token).json(user);

			return handleRequest(request, response -> response.asJson(FlintstoneUser.class));
		});
	}

	public FlintstoneUser getUserById(String userId) {
		return trace("getUserById", () -> {
			String url = String.format("%s/users/%s", baseUrl, userId);
			SimpleHttpRequest request = prepareGetRequest(url);

			return handleRequest(request, response -> response.asJson(FlintstoneUser.class));
		});
	}

	public FlintstoneUser getUserByUsername(String username) {
		List<FlintstoneUser> users = getUserByUsernameOrEmail("username", username);
		return users.isEmpty() ? null : users.getFirst();
	}

	public FlintstoneUser getUserByEmail(String email) {
		List<FlintstoneUser> users = getUserByUsernameOrEmail("email", email);
		return users.isEmpty() ? null : users.getFirst();
	}

	private List<FlintstoneUser> getUserByUsernameOrEmail(String field, String value) {
		return trace("getUserByUsernameOrEmail:" + field, () -> {
			String url = String.format("%s/users", baseUrl);
			Map<String, String> params = Map.of(field, value, "exactMatch", Boolean.TRUE.toString());
			return searchUsersRequest(url, params, 0, 1);
		});
	}

	public boolean updateUser(FlintstoneUser user) {
		return trace("updateUser", () -> {
			String url = String.format("%s/users/%s", baseUrl, user.getId());
			SimpleHttpRequest request = simpleHttp.doPut(url).auth(token).json(user);
			return handleRequestWithNoContentResponse(request);
		});
	}

	public boolean deleteUser(String userId) {
		return trace("deleteUser", () -> {
			String url = String.format("%s/users/%s", baseUrl, userId);
			SimpleHttpRequest request = simpleHttp.doDelete(url).auth(token);
			return handleRequestWithNoContentResponse(request);
		});
	}

	public List<Credential> getCredentials(String userId) {
		return trace("getCredentials", () -> {
			String url = String.format("%s/users/%s/credentials", baseUrl, userId);
			SimpleHttpRequest request = prepareGetRequest(url);
			return handleRequest(request, response -> response.asJson(new TypeReference<>() {}));
		});
	}

	public boolean verifyCredentials(String userId, Credential credential) {
		return trace("verifyCredentials", () -> {
			String url = String.format("%s/users/%s/credentials/verify", baseUrl, userId);
			SimpleHttpRequest request = simpleHttp.doPost(url).auth(token).json(credential);
			return handleRequestWithNoContentResponse(request);
		});
	}

	public boolean updateCredentials(String userId, Credential credential) {
		return trace("updateCredentials", () -> {
			String url = String.format("%s/users/%s/credentials", baseUrl, userId);
			SimpleHttpRequest request = simpleHttp.doPut(url).auth(token).json(credential);
			return handleRequestWithNoContentResponse(request);
		});
	}

	public List<FlintstoneUser> searchGroupMembers(String name, Integer first, Integer max) {
		return trace("searchGroupMembers", () -> {
			String url = String.format("%s/groups/members", baseUrl);
			return searchUsersRequest(url, Map.of("name", name), first, max);
		});
	}

	public List<FlintstoneUser> searchRoleMembers(String name, Integer first, Integer max) {
		return trace("searchRoleMembers", () -> {
			String url = String.format("%s/roles/members", baseUrl);
			return searchUsersRequest(url, Map.of("name", name), first, max);
		});
	}

	private List<FlintstoneUser> searchUsersRequest(String url, Map<String, String> params, Integer first, Integer max) {
		SimpleHttpRequest request = prepareGetRequest(url);
		params.forEach(request::param);
		if (first != null && first >= 0) {
			request.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			request.param("max", String.valueOf(max));
		}

		return handleRequest(request, response -> response.asJson(new TypeReference<>() {
		}));
	}

	private SimpleHttpRequest prepareGetRequest(String url) {
		return simpleHttp.doGet(url).auth(token).acceptJson();
	}

	private <T> T handleRequest(SimpleHttpRequest request, ThrowingFunction<SimpleHttpResponse, T, IOException> responseFunction) {
		AtomicReference<T> result = new AtomicReference<>();
		try (SimpleHttpResponse response = request.asResponse()) {
			if (response.getStatus() >= 300) {
				log.error("Error response from server: {}, error: {}, url: {}", response.getStatus(), response.asString(), request.getUrl());
			} else {
				result.set(responseFunction.apply(response));
			}
		} catch (IOException e) {
			String message = "Exception during calling url %s: %s".formatted(request.getUrl(), e.getMessage());
			log.error(message, e);
		}
		return result.get();
	}

	private boolean handleRequestWithNoContentResponse(SimpleHttpRequest request) {
		return Optional.ofNullable(handleRequest(request, response -> response.getStatus() == 204)).orElse(false);
	}

	private <T> T trace(String spanSuffix, Supplier<T> execution) {
		return tracing.trace(FlintstonesApiClient.class, spanSuffix, (Span span) -> execution.get());
	}

}
