package dasniko.keycloak.user.flintstones;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.UserModel;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class UserModelTransaction extends AbstractKeycloakTransaction {

	private final Consumer<UserModel> userConsumer;
	private final Set<UserModel> txUsers = new LinkedHashSet<>();

	public UserModelTransaction(Consumer<UserModel> userConsumer) {
		this.userConsumer = userConsumer;
	}

	public void addUser(UserModel userModel) {
		txUsers.add(userModel);
	}

	public UserModel findUser(@NonNull String identifier) {
		UserModel txUser = txUsers.stream()
			.filter(user -> user.getId().equals(identifier) || user.getUsername().equalsIgnoreCase(identifier) || identifier.equalsIgnoreCase(user.getEmail()))
			.findFirst().orElse(null);
		if (txUser != null) {
			log.info("Found user with identifier {} in txUsers", identifier);
		}
		return txUser;
	}

	@Override
	protected void commitImpl() {
		txUsers.forEach(userConsumer);
	}

	@Override
	protected void rollbackImpl() {
		// maybe do some more checks on loadedUsers...
		txUsers.clear();
	}
}
