package dasniko.keycloak.user.flintstones.repo;

import org.keycloak.common.util.SecretGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesRepository {

	private final List<FlintstoneUser> users = new ArrayList<>();
	private final Comparator<FlintstoneUser> byUsername = Comparator.comparing(FlintstoneUser::getUsername);

	FlintstonesRepository() {
		List<String> roles = List.of("STONEAGE");
		users.add(new FlintstoneUser("12345", "Fred", "Flintstone", true, roles));
		users.add(new FlintstoneUser("23456", "Wilma", "Flintstone", true, roles));
		users.add(new FlintstoneUser("34567", "Pebbles", "Flintstone", true, null));
		users.add(new FlintstoneUser("45678", "Barney", "Rubble", true, roles));
		users.add(new FlintstoneUser("56789", "Betty", "Rubble", true, roles));
		users.add(new FlintstoneUser("67890", "Bam Bam", "Rubble", false, null));
	}

	List<FlintstoneUser> getAllUsers() {
		return users.stream().sorted(byUsername).toList();
	}

	int getUsersCount(String query) {
		if (query != null && !query.isEmpty()) {
			return (int) users.stream()
				.filter(user -> query.equalsIgnoreCase("*") || user.getUsername().contains(query) || user.getEmail().contains(query))
				.count();
		}
		return users.size();
	}

	FlintstoneUser findUserById(String id) {
		return users.stream()
			.filter(user -> user.getId().equalsIgnoreCase(id))
			.findFirst().orElse(null);
	}

	private Optional<FlintstoneUser> findUserByUsernameOrEmailInternal(String username, boolean exactMatch) {
		if (!exactMatch) {
			return users.stream()
				.filter(user -> user.getUsername().contains(username) || user.getEmail().contains(username))
				.findFirst();
		}
		return users.stream()
			.filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(username))
			.findFirst();
	}

	FlintstoneUser findUserByUsernameOrEmail(String username, boolean exactMatch) {
		return findUserByUsernameOrEmailInternal(username, exactMatch).orElse(null);
	}

	List<FlintstoneUser> findUsers(String query) {
		return users.stream()
			.filter(user -> query.equalsIgnoreCase("*") || user.getUsername().contains(query) || user.getEmail().contains(query))
			.sorted(byUsername).toList();
	}

	List<FlintstoneUser> findUsersByGroupname(String groupName) {
		return findUsersByFilter(user -> user.getGroups().contains(groupName));
	}

	List<FlintstoneUser> findUsersByRolename(String roleName) {
		return findUsersByFilter(user -> user.getRoles() != null && user.getRoles().contains(roleName));
	}

	private List<FlintstoneUser> findUsersByFilter(Predicate<FlintstoneUser> filter) {
		return users.stream().filter(filter).sorted(byUsername).toList();
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
		FlintstoneUser existing = findUserByUsernameOrEmailInternal(user.getUsername(), true).orElseThrow();
		existing.setEmail(user.getEmail());
		existing.setEmailVerified(user.isEmailVerified());
		existing.setFirstName(user.getFirstName());
		existing.setLastName(user.getLastName());
		existing.setEnabled(user.isEnabled());
		existing.setPictureUrl(user.getPictureUrl());
	}

	boolean removeUser(String id) {
		return users.removeIf(p -> p.getId().equals(id));
	}

}
