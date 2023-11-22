package dasniko.keycloak.user.flintstones;

import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UserModelTransaction extends AbstractKeycloakTransaction {

	private final Consumer<UserModel> userConsumer;
	private final Map<String, UserModel> loadedUsers = new HashMap<>();

	public UserModelTransaction(Consumer<UserModel> userConsumer) {
		this.userConsumer = userConsumer;
	}

	public void addUser(String identifier, UserModel userModel) {
		loadedUsers.put(identifier, userModel);
	}

	public UserModel findUser(String identifier) {
		return loadedUsers.get(identifier);
	}

	@Override
	protected void commitImpl() {
		loadedUsers.values().forEach(userConsumer);
	}

	@Override
	protected void rollbackImpl() {
		// maybe do some more checks on loadedUsers...
		loadedUsers.clear();
	}
}
