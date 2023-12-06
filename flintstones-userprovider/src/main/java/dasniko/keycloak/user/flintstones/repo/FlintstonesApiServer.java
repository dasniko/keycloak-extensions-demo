package dasniko.keycloak.user.flintstones.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FlintstonesApiServer {

	private static final int PORT = 8000;
	private final ObjectMapper mapper = new ObjectMapper();
	private HttpServer server;

	public FlintstonesApiServer() {
		start();
	}

	public static void main(String[] args) {
		new FlintstonesApiServer();
	}

	@SneakyThrows
	private void start() {
		long start = System.currentTimeMillis();

		server = HttpServer.create(new InetSocketAddress(PORT), 0);
		server.createContext("/users", new FlintstonesHandler(mapper, this::writeResponse));
		server.createContext("/groups", new GroupsHandler(this::writeResponse));
		server.setExecutor(null);
		server.start();

		log.info("{} started on port {} in {} ms.", this.getClass().getSimpleName(), PORT, System.currentTimeMillis() - start);
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static class FlintstonesHandler implements HttpHandler {
		private final FlintstonesRepository repository = new FlintstonesRepository();
		private final ObjectMapper mapper;
		private final TriConsumer<HttpExchange, Object, Integer> responseWriter;

		public FlintstonesHandler(ObjectMapper mapper, TriConsumer<HttpExchange, Object, Integer> responseWriter) {
			this.mapper = mapper;
			this.responseWriter = responseWriter;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			String uriString = exchange.getRequestURI().toString();
			log.debug("Received request for {} - {}", method, uriString);
			long start = System.currentTimeMillis();

			String userId = null;
			String credentials = null;
			String[] parts = exchange.getRequestURI().getPath().split("/");
			if (parts.length >= 3) {
				userId = parts[2];
			}
			if (parts.length >= 4) {
				credentials = parts[3];
			}

			InputStream requestBody = exchange.getRequestBody();

			Object entity = null;
			int status = 200;

			if (userId == null) {
				List<FlintstoneUser> users = List.of();
				if ("GET".equalsIgnoreCase(method)) {
					String query = exchange.getRequestURI().getQuery();
					if (query != null) {
						Map<String, String> queryParams = Arrays.stream(query.split("&"))
							.map(s -> s.split("=")).collect(Collectors.toMap(k -> k[0], v -> v[1]));
						if (queryParams.containsKey("username")) {
							FlintstoneUser user = repository.findUserByUsernameOrEmail(queryParams.get("username"));
							if (user != null) {
								users = List.of(user);
							}
						} else if (queryParams.containsKey("email")) {
							FlintstoneUser user = repository.findUserByUsernameOrEmail(queryParams.get("email"));
							if (user != null) {
								users = List.of(user);
							}
						} else if (queryParams.containsKey("search")) {
							users = repository.findUsers(queryParams.get("search"));
						} else {
							users = repository.getAllUsers();
						}
					} else {
						users = repository.getAllUsers();
					}
					entity = users;
				} else if ("POST".equalsIgnoreCase(method)) {
					FlintstoneUser flintstoneUser = mapper.readValue(requestBody, FlintstoneUser.class);
					repository.createUser(flintstoneUser);
					status = 201;
				}
			} else if (credentials == null) {
				if ("GET".equalsIgnoreCase(method)) {
					if ("count".equalsIgnoreCase(userId)) {
						entity = Map.of("count", repository.getUsersCount());
					} else {
						entity = repository.findUserById(userId);
						if (entity == null) {
							status = 404;
						}
					}
				} else if ("PUT".equalsIgnoreCase(method)) {
					FlintstoneUser flintstoneUser = mapper.readValue(requestBody, FlintstoneUser.class);
					repository.updateUser(flintstoneUser);
					status = 204;
				} else if ("DELETE".equalsIgnoreCase(method)) {
					boolean result = repository.removeUser(userId);
					status = result ? 204 : 400;
				}
			} else {
				Credential credential = mapper.readValue(requestBody, Credential.class);
				status = 400;
				boolean result;
				if ("PUT".equalsIgnoreCase(method)) {
					result = repository.updateCredentials(userId, credential.getValue());
					status = result ? 204 : 400;
				}
				if ("POST".equalsIgnoreCase(method)) {
					result = repository.validateCredentials(userId, credential.getValue());
					status = result ? 204 : 400;
				}
			}

			responseWriter.accept(exchange, entity, status);

			log.debug("Processed request for {} - {} in {} ms.", method, uriString, System.currentTimeMillis() - start);
		}
	}

	private static class GroupsHandler implements HttpHandler {
		private final FlintstonesRepository repository = new FlintstonesRepository();
		private final TriConsumer<HttpExchange, Object, Integer> responseWriter;

		public GroupsHandler(TriConsumer<HttpExchange, Object, Integer> responseWriter) {
			this.responseWriter = responseWriter;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			String uriString = exchange.getRequestURI().toString();
			log.debug("Received request for {} - {}", method, uriString);
			long start = System.currentTimeMillis();

			Object entity;
			int status = 200;

			List<FlintstoneUser> users = List.of();
			if ("GET".equalsIgnoreCase(method)) {
				String query = exchange.getRequestURI().getQuery();
				if (query != null) {
					Map<String, String> queryParams = Arrays.stream(query.split("&"))
						.map(s -> s.split("=")).collect(Collectors.toMap(k -> k[0], v -> v[1]));
					if (queryParams.containsKey("name")) {
						users = repository.findUsersByGroupname(queryParams.get("name"));
					}
				}
			}
			entity = users;

			responseWriter.accept(exchange, entity, status);

			log.debug("Processed request for {} - {} in {} ms.", method, uriString, System.currentTimeMillis() - start);
		}
	}

	@SneakyThrows
	private void writeResponse(HttpExchange exchange, Object entity, int status) {
		byte[] bytes = mapper.writeValueAsBytes(entity);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(bytes);
		os.flush();
		os.close();
	}
}
