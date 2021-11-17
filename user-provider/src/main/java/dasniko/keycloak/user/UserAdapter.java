package dasniko.keycloak.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {

	private final DemoUser user;
	private final Set<RoleModel> userRoles = new HashSet<>();

	public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DemoUser user) {
		super(session, realm, model);
		this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
		this.user = user;
		setFirstName(user.getFirstName());
		setLastName(user.getLastName());
		setEmail(user.getEmail());
		setEnabled(user.isEnabled());
		setCreatedTimestamp(user.getCreated());
		user.getRoles().forEach(role -> userRoles.add(new UserRoleModel(role, realm)));
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public void setUsername(String username) {
		user.setUsername(username);
	}

	@Override
	protected Set<RoleModel> getFederatedRoleMappings() {
		return userRoles;
	}
}
