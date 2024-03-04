package dasniko.keycloak.user.peanuts;

import dasniko.keycloak.user.peanuts.external.Peanut;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class UserAdapter extends AbstractUserAdapter {

	private final Peanut user;

	public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, Peanut user) {
		super(session, realm, model);
		this.storageId = new StorageId(storageProviderModel.getId(), user.getUsername());
		this.user = user;
	}

	@Override
	public String getUsername() {
		return user.getUsername();
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
	public SubjectCredentialManager credentialManager() {
		return new UserCredentialManager(session, realm, this);
	}

	@Override
	public String getFirstAttribute(String name) {
		List<String> list = getAttributes().getOrDefault(name, List.of());
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
		attributes.add(UserModel.USERNAME, getUsername());
		attributes.add(UserModel.EMAIL, getEmail());
		attributes.add(UserModel.FIRST_NAME, getFirstName());
		attributes.add(UserModel.LAST_NAME, getLastName());
		attributes.add("birthday", user.getBirthday());
		attributes.add("gender", user.getGender());
		return attributes;
	}

	@Override
	public Stream<String> getAttributeStream(String name) {
		Map<String, List<String>> attributes = getAttributes();
		return (attributes.containsKey(name)) ? attributes.get(name).stream() : Stream.empty();
	}

	@Override
	protected Set<GroupModel> getGroupsInternal() {
		if (user.getGroups() != null) {
			return user.getGroups().stream().map(UserGroupModel::new).collect(Collectors.toSet());
		}
		return Set.of();
	}

	@Override
	protected Set<RoleModel> getRoleMappingsInternal() {
		if (user.getRoles() != null) {
			return user.getRoles().stream().map(roleName -> new UserRoleModel(roleName, realm)).collect(Collectors.toSet());
		}
		return Set.of();
	}

	@Override
	public Stream<String> getRequiredActionsStream() {
		return getFederatedStorage().getRequiredActionsStream(realm, this.getId());
	}

	@Override
	public void addRequiredAction(String action) {
		getFederatedStorage().addRequiredAction(realm, this.getId(), action);
	}

	@Override
	public void removeRequiredAction(String action) {
		getFederatedStorage().removeRequiredAction(realm, this.getId(), action);
	}

	@Override
	public void addRequiredAction(RequiredAction action) {
		getFederatedStorage().addRequiredAction(realm, this.getId(), action.name());
	}

	@Override
	public void removeRequiredAction(RequiredAction action) {
		getFederatedStorage().removeRequiredAction(realm, this.getId(), action.name());
	}


	@Override
	public void setAttribute(String name, List<String> values) {
		// intended, see commit message
	}

	@Override
	public void setFirstName(String firstName) {
		// intended, see commit message
	}

	@Override
	public void setLastName(String lastName) {
		// intended, see commit message
	}

	@Override
	public void setEmail(String email) {
		// intended, see commit message
	}

	@Override
	public void setEmailVerified(boolean verified) {
		// intended, see commit message
	}

	@Override
	public void setEnabled(boolean enabled) {
		// intended, see commit message
	}

	@Override
	public void setCreatedTimestamp(Long timestamp) {
		// intended, see commit message
	}

	@Override
	public void setSingleAttribute(String name, String value) {
		// intended, see commit message
	}

	@Override
	public void removeAttribute(String name) {
		// intended, see commit message
	}

	UserFederatedStorageProvider getFederatedStorage() {
		return UserStorageUtil.userFederatedStorage(session);
	}

}
