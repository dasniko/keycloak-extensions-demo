package dasniko.keycloak.user.flintstones;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import de.keycloak.util.TokenUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

@Slf4j
public class FlintstonesApiClient {

	private final KeycloakSession session;
	private final String baseUrl;
	private final String token;

	public FlintstonesApiClient(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.baseUrl = model.get(FlintstonesUserStorageProviderFactory.USER_API_BASE_URL);
		String clientId = model.get(FlintstonesUserStorageProviderFactory.CLIENT_ID);
		this.token = clientId != null ? TokenUtils.generateServiceAccountAccessToken(session, clientId, null, null) : "";
	}

	@SneakyThrows
	public List<FlintstoneUser> searchUsers(String search, Integer first, Integer max) {
		String url = String.format("%s/users", baseUrl);
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (first != null && first >= 0) {
			simpleHttp.param("first", String.valueOf(first));
		}
		if (max != null && max >= 0) {
			simpleHttp.param("max", String.valueOf(max));
		}
		if (search != null) {
			simpleHttp.param("search", search);
		}
		return simpleHttp.asJson(new TypeReference<>() {});
	}

	@SneakyThrows
	public Integer usersCount(String search) {
		String url = String.format("%s/users/count", baseUrl);
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (search != null) {
			simpleHttp.param("search", search);
		}
		Map<String, Integer> count = simpleHttp.asJson(new TypeReference<>() {});
		return count.getOrDefault("count", 0) ;
	}

	@SneakyThrows
	public FlintstoneUser createUser(FlintstoneUser user) {
		String url = String.format("%s/users", baseUrl);
		try (SimpleHttp.Response response = SimpleHttp.doPost(url, session).auth(token).json(user).asResponse()) {
			if (response.getStatus() == 201) {
				return response.asJson(FlintstoneUser.class);
			} else {
				log.warn("User could not be created. Reason: {}", response.asString());
				return null;
			}
		}
	}

	@SneakyThrows
	public FlintstoneUser getUserById(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		try (SimpleHttp.Response response = prepareGetRequest(url).asResponse()) {
			if (response.getStatus() >= 400) {
				return null;
			}
			return response.asJson(FlintstoneUser.class);
		}
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

	@SneakyThrows
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
		return simpleHttp.asJson(new TypeReference<>() {});
	}

	@SneakyThrows
	public void updateUser(FlintstoneUser user) {
		String url = String.format("%s/users/%s", baseUrl, user.getId());
		SimpleHttp.doPut(url, session).auth(token).json(user).asStatus();
	}

	@SneakyThrows
	public boolean deleteUser(String userId) {
		String url = String.format("%s/users/%s", baseUrl, userId);
		return SimpleHttp.doDelete(url, session).auth(token).asStatus() == 204;
	}

	@SneakyThrows
	public boolean verifyCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials/verify", baseUrl, userId);
		return SimpleHttp.doPost(url, session).auth(token).json(credential).asStatus() == 204;
	}

	@SneakyThrows
	public boolean updateCredentials(String userId, Credential credential) {
		String url = String.format("%s/users/%s/credentials", baseUrl, userId);
		return SimpleHttp.doPut(url, session).auth(token).json(credential).asStatus() == 204;
	}

	@SneakyThrows
	public List<FlintstoneUser> searchGroupMembers(String name, int first, int max) {
		String url = String.format("%s/groups/members", baseUrl);
		SimpleHttp simpleHttp = prepareGetRequest(url);
		if (name != null) {
			simpleHttp.param("name", name);
		}
		if (first >= 0) {
			simpleHttp.param("first", String.valueOf(first));
		}
		if (max >= 0) {
			simpleHttp.param("max", String.valueOf(max));
		}
		return simpleHttp.asJson(new TypeReference<>() {});
	}

	private SimpleHttp prepareGetRequest(String url) {
		return SimpleHttp.doGet(url, session).auth(token).acceptJson();
	}
}
