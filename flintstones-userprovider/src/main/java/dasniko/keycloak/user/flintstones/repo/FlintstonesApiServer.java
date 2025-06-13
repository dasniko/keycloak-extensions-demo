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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("LoggingSimilarMessage")
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

		FlintstonesRepository repository = new FlintstonesRepository();
		server = HttpServer.create(new InetSocketAddress(PORT), 0);
		server.createContext("/users", new FlintstonesHandler(repository, mapper, this::writeResponse));
		server.createContext("/groups", new GroupsHandler(repository, this::writeResponse));
		server.createContext("/roles", new RolesHandler(repository, this::writeResponse));
		server.setExecutor(null);
		server.start();

		log.info("{} started on port {} in {} ms.", this.getClass().getSimpleName(), PORT, System.currentTimeMillis() - start);
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private record FlintstonesHandler(FlintstonesRepository repository, ObjectMapper mapper,
																		TriConsumer<HttpExchange, Object, Integer> responseWriter) implements HttpHandler {

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
							boolean exactMatch = Boolean.parseBoolean(queryParams.getOrDefault("exactMatch", "false"));
							if (queryParams.containsKey("username")) {
								FlintstoneUser user = repository.findUserByUsernameOrEmail(queryParams.get("username"), exactMatch);
								if (user != null) {
									users = List.of(user);
								}
							} else if (queryParams.containsKey("email")) {
								FlintstoneUser user = repository.findUserByUsernameOrEmail(queryParams.get("email"), exactMatch);
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
						entity = repository.findUserByUsernameOrEmail(flintstoneUser.getUsername(), true);
						status = 201;
					}
				} else if (credentials == null) {
					if ("GET".equalsIgnoreCase(method)) {
						if ("count".equalsIgnoreCase(userId)) {
							String search = null;
							String query = exchange.getRequestURI().getQuery();
							if (query != null) {
								Map<String, String> queryParams = Arrays.stream(query.split("&"))
									.map(s -> s.split("=")).collect(Collectors.toMap(k -> k[0], v -> v[1]));
								search = queryParams.getOrDefault("search", null);
							}
							entity = Map.of("count", repository.getUsersCount(search));
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

	private static class GroupsHandler extends ResourceHandler {
		private final FlintstonesRepository repository;

		public GroupsHandler(FlintstonesRepository repository, TriConsumer<HttpExchange, Object, Integer> responseWriter) {
			super(responseWriter);
			this.repository = repository;
		}

		@Override
		Function<String, List<FlintstoneUser>> getUsersFromRepository() {
			return repository::findUsersByGroupname;
		}
	}

	private static class RolesHandler extends ResourceHandler {
		private final FlintstonesRepository repository;

		public RolesHandler(FlintstonesRepository repository, TriConsumer<HttpExchange, Object, Integer> responseWriter) {
			super(responseWriter);
			this.repository = repository;
		}

		@Override
		Function<String, List<FlintstoneUser>> getUsersFromRepository() {
			return repository::findUsersByRolename;
		}
	}

	private abstract static class ResourceHandler implements HttpHandler {
		private final TriConsumer<HttpExchange, Object, Integer> responseWriter;

		public ResourceHandler(TriConsumer<HttpExchange, Object, Integer> responseWriter) {
			this.responseWriter = responseWriter;
		}

		abstract Function<String, List<FlintstoneUser>> getUsersFromRepository();

		@Override
		public void handle(HttpExchange exchange) {
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
						users = getUsersFromRepository().apply(queryParams.get("name"));
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
