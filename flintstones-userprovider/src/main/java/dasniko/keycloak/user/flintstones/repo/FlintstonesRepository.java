package dasniko.keycloak.user.flintstones.repo;

import lombok.SneakyThrows;
import org.keycloak.common.util.SecretGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesRepository {

	private final List<FlintstoneUser> users = new ArrayList<>();

	@SneakyThrows
	FlintstonesRepository() {
		users.add(new FlintstoneUser("12345", "fred.flintstone@flintstones.com", "Fred", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("23456", "wilma.flintstone@flintstones.com", "Wilma", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("34567", "pebbles.flintstone@flintstones.com", "Pebbles", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("45678", "barney.rubble@flintstones.com", "Barney", "Rubble", true, List.of("stoneage")));
		users.add(new FlintstoneUser("56789", "betty.rubble@flintstones.com", "Betty", "Rubble", true, null));
		users.add(new FlintstoneUser("67890", "bambam.rubble@flintstones.com", "Bam Bam", "Rubble", false, null));
	}

	List<FlintstoneUser> getAllUsers() {
		return users;
	}

	int getUsersCount() {
		return users.size();
	}

	FlintstoneUser findUserById(String id) {
		return users.stream()
			.filter(user -> user.getId().equalsIgnoreCase(id))
			.findFirst().orElse(null);
	}

	private FlintstoneUser findUserByUsernameOrEmailInternal(String username) {
		return users.stream()
			.filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(username))
			.findFirst().orElse(null);
	}

	FlintstoneUser findUserByUsernameOrEmail(String username) {
		FlintstoneUser user = findUserByUsernameOrEmailInternal(username);
		return user != null ? user.clone() : null;
	}

	List<FlintstoneUser> findUsers(String query) {
		return users.stream()
			.filter(user -> query.equalsIgnoreCase("*") || user.getUsername().contains(query) || user.getEmail().contains(query))
			.collect(Collectors.toList());
	}

	List<FlintstoneUser> findUsersByGroupname(String groupName) {
		return users.stream().filter(user -> user.getGroups().contains(groupName)).toList();
	}

	boolean validateCredentials(String id, String password) {
		return findUserById(id).getPassword().equals(password);
	}

	boolean updateCredentials(String id, String password) {
		findUserById(id).setPassword(password);
		return true;
	}

	void createUser(FlintstoneUser user) {
		user.setId(SecretGenerator.getInstance().randomString(5));
		user.setCreated(System.currentTimeMillis());
		users.add(user);
	}

	void updateUser(FlintstoneUser user) {
		FlintstoneUser existing = findUserByUsernameOrEmailInternal(user.getUsername());
		existing.setEmail(user.getEmail());
		existing.setFirstName(user.getFirstName());
		existing.setLastName(user.getLastName());
		existing.setEnabled(user.isEnabled());
	}

	boolean removeUser(String id) {
		return users.removeIf(p -> p.getId().equals(id));
	}

}
