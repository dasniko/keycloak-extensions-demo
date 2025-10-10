package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import de.keycloak.util.ThrowingFunction;
import de.keycloak.util.TokenUtils;
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
		this.token = clientId != null ? TokenUtils.generateServiceAccountAccessToken(session, clientId, null, null) : "";
		this.tracing = session.getProvider(TracingProvider.class);
	}

	public List<FlintstoneUser> searchUsers(String search, Integer first, Integer max) {
		String url = String.format("%s/users", baseUrl);
		return searchUsersRequest(url, search, null, first, max, "searchUsers");
	}

	public Integer usersCount(String search) {
		String url = String.format("%s/users/count", baseUrl);
		SimpleHttpRequest request = prepareGetRequest(url);
		if (search != null) {
			request.param("search", search);
		}

		return handleRequest(request, "usersCount", response -> {
			Map<String, Integer> payload = request.asJson(new TypeReference<>() {});
			return payload.getOrDefault("count", 0);
		});
	}

	public FlintstoneUser createUser(FlintstoneUser user) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttpRequest request = simpleHttp.doPost(url).auth(token).json(user);

		return handleRequest(request, "createUser", response -> response.asJson(FlintstoneUser.class));
	}

	public FlintstoneUser getUserById(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		SimpleHttpRequest request = prepareGetRequest(url);

		return handleRequest(request, "getUserById", response -> response.asJson(FlintstoneUser.class));
	}

	public FlintstoneUser getUserByUsername(String username) {
		List<FlintstoneUser> users = getUserByUsernameOrEmail("username", username, true, 0, 1);
		return users.isEmpty() ? null : users.getFirst();
	}

	public List<FlintstoneUser> searchUsersByUsername(String username, Integer firstResult, Integer maxResults) {
		return getUserByUsernameOrEmail("username", username, false, firstResult, maxResults);
	}

	public FlintstoneUser getUserByEmail(String email) {
		List<FlintstoneUser> users = getUserByUsernameOrEmail("email", email, true, 0, 1);
		return users.isEmpty() ? null : users.getFirst();
	}

	public List<FlintstoneUser> searchUsersByEmail(String email, Integer firstResult, Integer maxResults) {
		return getUserByUsernameOrEmail("email", email, false, firstResult, maxResults);
	}

	private List<FlintstoneUser> getUserByUsernameOrEmail(String field, String value, boolean exactMatch, Integer first, Integer max) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttpRequest request = prepareGetRequest(url);
		request.param(field, value);
		request.param("exactMatch", String.valueOf(exactMatch));
		if (first != null && first >= 0) {
			request.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			request.param("max", String.valueOf(max));
		}

		return handleRequest(request, "getUserByUsernameOrEmail:" + field, response -> response.asJson(new TypeReference<>() {}));
	}

	public boolean updateUser(FlintstoneUser user) {
		String url = String.format("%s/users/%s", baseUrl, user.getId());
		SimpleHttpRequest request = simpleHttp.doPut(url).auth(token).json(user);
		return handleRequestWithNoContentResponse(request, "updateUser");
	}

	public boolean deleteUser(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		SimpleHttpRequest request = simpleHttp.doDelete(url).auth(token);
		return handleRequestWithNoContentResponse(request, "deleteUser");
	}

	public boolean verifyCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials/verify", baseUrl, userId);
		SimpleHttpRequest request = simpleHttp.doPost(url).auth(token).json(credential);
		return handleRequestWithNoContentResponse(request, "verifyCredentials");
	}

	public boolean updateCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials", baseUrl, userId);
		SimpleHttpRequest request = simpleHttp.doPut(url).auth(token).json(credential);
		return handleRequestWithNoContentResponse(request, "updateCredentials");
	}

	public List<FlintstoneUser> searchGroupMembers(String name, Integer first, Integer max) {
		String url = String.format("%s/groups/members", baseUrl);
		return searchUsersRequest(url, null, name, first, max, "searchGroupMembers");
	}

	public List<FlintstoneUser> searchRoleMembers(String name, Integer first, Integer max) {
		String url = String.format("%s/roles/members", baseUrl);
		return searchUsersRequest(url, null, name, first, max, "searchRoleMembers");
	}

	private List<FlintstoneUser> searchUsersRequest(String url, String search, String name, Integer first, Integer max, String spanSuffix) {
		SimpleHttpRequest request = prepareGetRequest(url);
		if (name != null) {
			request.param("name", name);
		}
		if (first != null && first >= 0) {
			request.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			request.param("max", String.valueOf(max));
		}
		if (search != null) {
			request.param("search", search);
		}

		return handleRequest(request, spanSuffix, response -> response.asJson(new TypeReference<>() {}));
	}

	private SimpleHttpRequest prepareGetRequest(String url) {
		return simpleHttp.doGet(url).auth(token).acceptJson();
	}

	private <T> T handleRequest(SimpleHttpRequest request, String spanSuffix, ThrowingFunction<SimpleHttpResponse, T, IOException> responseFunction) {
		AtomicReference<T> result = new AtomicReference<>();
		tracing.trace(FlintstonesApiClient.class, spanSuffix, span -> {
			try (SimpleHttpResponse response = request.asResponse()) {
				if (response.getStatus() >= 300) {
					log.error("Error response from server: {}, error: {}, url: {}", response.getStatus(), response.asString(), request.getUrl());
				} else {
					result.set(responseFunction.apply(response));
				}
			} catch (IOException e) {
				String message = "Exception during calling url %s: %s".formatted(request.getUrl(), e.getMessage());
				log.error(message, e);
				span.recordException(e);
			}
		});
		return result.get();
	}

	private boolean handleRequestWithNoContentResponse(SimpleHttpRequest request, String spanSuffix) {
		return Optional.ofNullable(handleRequest(request, spanSuffix, response -> response.getStatus() == 204))
			.orElse(false);
	}

}
