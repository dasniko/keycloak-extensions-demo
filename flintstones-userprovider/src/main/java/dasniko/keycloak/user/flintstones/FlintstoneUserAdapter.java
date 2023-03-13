package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import lombok.Getter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstoneUserAdapter extends AbstractUserAdapter {

	@Getter
	private final FlintstoneUser user;

	public FlintstoneUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, FlintstoneUser user) {
		super(session, realm, model);
		this.storageId = new StorageId(storageProviderModel.getId(), user.getUsername());
		this.user = user;
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
	public String getFirstName() {
		return user.getFirstName();
	}

	@Override
	public String getLastName() {
		return user.getLastName();
	}

	@Override
	public String getEmail() {
		return user.getEmail();
	}

	@Override
	public boolean isEmailVerified() {
		return true;
	}

	@Override
	public SubjectCredentialManager credentialManager() {
		return new LegacyUserCredentialManager(session, realm, this);
	}

	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		user.setEnabled(enabled);
	}

	@Override
	public Long getCreatedTimestamp() {
		return user.getCreated();
	}

	@Override
	public void setAttribute(String name, List<String> values) {
		if (!values.isEmpty()) {
			switch (name) {
				case UserModel.LAST_NAME:
					user.setLastName(values.get(0));
					break;
				case UserModel.FIRST_NAME:
					user.setFirstName(values.get(0));
					break;
				case UserModel.EMAIL:
					user.setEmail(values.get(0));
					break;
				default:
					setSingleAttribute(name, values.get(0));
			}
		}
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
		attributes.add(UserModel.USERNAME, getUsername());
		attributes.add(UserModel.EMAIL, getEmail());
		attributes.add(UserModel.FIRST_NAME, getFirstName());
		attributes.add(UserModel.LAST_NAME, getLastName());
		return attributes;
	}

	@Override
	protected Set<RoleModel> getRoleMappingsInternal() {
		if (user.getRoles() != null) {
			return user.getRoles().stream()
				.map(roleName -> new FlintstoneUserRoleModel(roleName, realm)).collect(Collectors.toSet());
		}
		return Set.of();
	}

	@Override
	public void removeRequiredAction(String action) {
		if (action.equals(RequiredAction.UPDATE_PASSWORD.name())) {
			return;
		}
		super.removeRequiredAction(action);
	}
}
