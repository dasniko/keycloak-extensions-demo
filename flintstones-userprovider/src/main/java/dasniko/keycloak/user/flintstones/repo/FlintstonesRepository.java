package dasniko.keycloak.user.flintstones.repo;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesRepository {

	private final List<FlintstoneUser> users = new ArrayList<>();

	@SneakyThrows
	FlintstonesRepository() {
		users.add(new FlintstoneUser("66671638-b48a-4bab-8f37-d20efea42ce3", "fred.flintstone@flintstones.com", "Fred", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("ced34250-cb88-4cc0-87e8-6fca25a926e3", "wilma.flintstone@flintstones.com", "Wilma", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("12df5d9c-c6ac-48e8-8086-b10ab7985a65", "pebbles.flintstone@flintstones.com", "Pebbles", "Flintstone", true, List.of("stoneage")));
		users.add(new FlintstoneUser("1b6c083b-3e14-40ef-897c-a0d3cdba22ed", "barney.rubble@flintstones.com", "Barney", "Rubble", true, List.of("stoneage")));
		users.add(new FlintstoneUser("42c88684-c585-4d83-b748-7a49213c4690", "betty.rubble@flintstones.com", "Betty", "Rubble", true, null));
		users.add(new FlintstoneUser("08950eb2-920b-48f5-bbe0-9694e8ad6fc4", "bambam.rubble@flintstones.com", "Bam Bam", "Rubble", false, null));
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

	boolean validateCredentials(String id, String password) {
		return findUserById(id).getPassword().equals(password);
	}

	boolean updateCredentials(String id, String password) {
		findUserById(id).setPassword(password);
		return true;
	}

	void createUser(FlintstoneUser user) {
		user.setId(UUID.randomUUID().toString());
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
