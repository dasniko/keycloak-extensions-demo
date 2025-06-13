package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import de.keycloak.util.ThrowingConsumer;
import de.keycloak.util.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.tracing.TracingProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FlintstonesApiClient {

	private final KeycloakSession session;
	private final String baseUrl;
	private final String token;
	private final TracingProvider tracing;

	public FlintstonesApiClient(KeycloakSession session, ComponentModel model) {
		this.session = session;
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
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (search != null) {
			simpleHttp.param("search", search);
		}

		AtomicInteger count = new AtomicInteger(0);
		handleRequest(simpleHttp, "usersCount", response -> {
			Map<String, Integer> payload = simpleHttp.asJson(new TypeReference<>() {});
			count.set(payload.getOrDefault("count", 0));
		});

		return count.get();
	}

	public FlintstoneUser createUser(FlintstoneUser user) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttp simpleHttp = SimpleHttp.doPost(url, session).auth(token).json(user);

		AtomicReference<FlintstoneUser> createdUser = new AtomicReference<>();
		handleRequest(simpleHttp, "createUser", response -> createdUser.set(response.asJson(FlintstoneUser.class)));

		return createdUser.get();
	}

	public FlintstoneUser getUserById(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		SimpleHttp simpleHttp = prepareGetRequest(url);

		AtomicReference<FlintstoneUser> user = new AtomicReference<>();
		handleRequest(simpleHttp, "getUserById", response -> user.set(response.asJson(FlintstoneUser.class)));

		return user.get();
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
		SimpleHttp simpleHttp = prepareGetRequest(url);
		simpleHttp.param(field, value);
		simpleHttp.param("exactMatch", String.valueOf(exactMatch));
		if (first != null && first >= 0) {
			simpleHttp.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			simpleHttp.param("max", String.valueOf(max));
		}

		AtomicReference<List<FlintstoneUser>> users = new AtomicReference<>(List.of());
		handleRequest(simpleHttp, "getUserByUsernameOrEmail:" + field, response -> users.set(response.asJson(new TypeReference<>() {})));
		return users.get();
	}

	public boolean updateUser(FlintstoneUser user) {
		String url = String.format("%s/users/%s", baseUrl, user.getId());
		SimpleHttp simpleHttp = SimpleHttp.doPut(url, session).auth(token).json(user);
		return handleRequestWithNoContentResponse(simpleHttp, "updateUser");
	}

	public boolean deleteUser(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		SimpleHttp simpleHttp = SimpleHttp.doDelete(url, session).auth(token);
		return handleRequestWithNoContentResponse(simpleHttp, "deleteUser");
	}

	public boolean verifyCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials/verify", baseUrl, userId);
		SimpleHttp simpleHttp = SimpleHttp.doPost(url, session).auth(token).json(credential);
		return handleRequestWithNoContentResponse(simpleHttp, "verifyCredentials");
	}

	public boolean updateCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials", baseUrl, userId);
		SimpleHttp simpleHttp = SimpleHttp.doPut(url, session).auth(token).json(credential);
		return handleRequestWithNoContentResponse(simpleHttp, "updateCredentials");
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
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (name != null) {
			simpleHttp.param("name", name);
		}
		if (first != null && first >= 0) {
			simpleHttp.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			simpleHttp.param("max", String.valueOf(max));
		}
		if (search != null) {
			simpleHttp.param("search", search);
		}

		AtomicReference<List<FlintstoneUser>> users = new AtomicReference<>(List.of());
		handleRequest(simpleHttp, spanSuffix, response -> users.set(response.asJson(new TypeReference<>() {})));
		return users.get();
	}

	private SimpleHttp prepareGetRequest(String url) {
		return SimpleHttp.doGet(url, session).auth(token).acceptJson();
	}

	private void handleRequest(SimpleHttp simpleHttp, String spanSuffix, ThrowingConsumer<SimpleHttp.Response, IOException> responseConsumer) {
		tracing.trace(FlintstonesApiClient.class, spanSuffix, span -> {
			try (SimpleHttp.Response response = simpleHttp.asResponse()) {
				if (response.getStatus() >= 300) {
					log.error("Error response from server: {}, error: {}, url: {}", response.getStatus(), response.asString(), simpleHttp.getUrl());
				} else {
					responseConsumer.accept(response);
				}
			} catch (IOException e) {
				String message = "Exception during calling url %s: %s".formatted(simpleHttp.getUrl(), e.getMessage());
				log.error(message, e);
				span.recordException(e);
			}
		});
	}

	private boolean handleRequestWithNoContentResponse(SimpleHttp simpleHttp, String spanSuffix) {
		AtomicBoolean success = new AtomicBoolean(false);
		handleRequest(simpleHttp, spanSuffix, response -> success.set(response.getStatus() == 204));
		return success.get();
	}

}
