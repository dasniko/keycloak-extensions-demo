package dasniko.keycloak.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
class DemoRepository {

	private final List<DemoUser> users;

	DemoRepository() {
		Long created = System.currentTimeMillis();
		List<String> roles = Collections.singletonList("stoneage");
		users = Arrays.asList(
			new DemoUser("1", "Fred", "Flintstone", true, created, roles),
			new DemoUser("2", "Wilma", "Flintstone", true, created, roles),
			new DemoUser("3", "Pebbles", "Flintstone", true, created, roles),
			new DemoUser("4", "Barney", "Rubble", true, created, roles),
			new DemoUser("5", "Betty", "Rubble", true, created, Collections.emptyList()),
			new DemoUser("6", "Bam Bam", "Rubble", false, created, Collections.emptyList())
		);
	}

	List<DemoUser> getAllUsers() {
		return users;
	}

	int getUsersCount() {
		return users.size();
	}

	DemoUser findUserById(String id) {
		return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
	}

	DemoUser findUserByUsernameOrEmail(String username) {
		return users.stream()
			.filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(username))
			.findFirst().orElse(null);
	}

	List<DemoUser> findUsers(String query) {
		return users.stream()
			.filter(user -> user.getUsername().contains(query) || user.getEmail().contains(query))
			.collect(Collectors.toList());
	}

	boolean validateCredentials(String username, String password) {
		return findUserByUsernameOrEmail(username).getPassword().equals(password);
	}

	boolean updateCredentials(String username, String password) {
		findUserByUsernameOrEmail(username).setPassword(password);
		return true;
	}

}
