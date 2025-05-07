package dasniko.keycloak.user.flintstones;

import lombok.NonNull;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserModelTransaction extends AbstractKeycloakTransaction {

	private final Consumer<UserModel> userConsumer;
	private final List<UserModel> loadedUsers = new ArrayList<>();

	public UserModelTransaction(Consumer<UserModel> userConsumer) {
		this.userConsumer = userConsumer;
	}

	public void addUser(UserModel userModel) {
		loadedUsers.add(userModel);
	}

	public UserModel findUser(@NonNull String identifier) {
		return loadedUsers.stream()
			.filter(user -> user.getId().equals(identifier) || user.getUsername().equalsIgnoreCase(identifier) || user.getEmail().equalsIgnoreCase(identifier))
			.findFirst().orElse(null);
	}

	@Override
	protected void commitImpl() {
		loadedUsers.forEach(userConsumer);
	}

	@Override
	protected void rollbackImpl() {
		// maybe do some more checks on loadedUsers...
		loadedUsers.clear();
	}
}
