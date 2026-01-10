package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Slf4j
@Getter
public class FlintstoneUserAdapter extends AbstractUserAdapterFederatedStorage {

	private static final String ATTR_PICTURE = "picture";

	private final FlintstoneUser user;

	private boolean dirty;

	public FlintstoneUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, FlintstoneUser user) {
		super(session, realm, model);
		this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
		this.user = user;
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public void setUsername(String username) {
		dirty = dirty || !Objects.equals(username, user.getUsername());
		user.setUsername(username);
	}

	@Override
	public String getFirstName() {
		return user.getFirstName();
	}

	@Override
	public void setFirstName(String firstName) {
		dirty = dirty || !Objects.equals(firstName, user.getFirstName());
		user.setFirstName(firstName);
	}

	@Override
	public String getLastName() {
		return user.getLastName();
	}

	@Override
	public void setLastName(String lastName) {
		dirty = dirty || !Objects.equals(lastName, user.getLastName());
		user.setLastName(lastName);
	}

	@Override
	public String getEmail() {
		return user.getEmail();
	}

	@Override
	public void setEmail(String email) {
		dirty = dirty || !Objects.equals(email, user.getEmail());
		user.setEmail(email);
	}

	@Override
	public boolean isEmailVerified() {
		boolean trustEmail = storageProviderModel.get(FlintstonesUserStorageProviderFactory.TRUST_EMAIL, false);
		return trustEmail || user.isEmailVerified();
	}

	@Override
	public void setEmailVerified(boolean verified) {
		dirty = dirty || verified != user.isEmailVerified();
		user.setEmailVerified(verified);
	}

	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		dirty = dirty || enabled != user.isEnabled();
		user.setEnabled(enabled);
	}

	@Override
	public Long getCreatedTimestamp() {
		return user.getCreated();
	}

	@Override
	public void setCreatedTimestamp(Long timestamp) {
		user.setCreated(timestamp);
	}

	@Override
	public void setAttribute(String name, List<String> values) {
		String value = values != null && !values.isEmpty() ? values.getFirst() : null;
		switch (name) {
			case UserModel.USERNAME -> setUsername(value);
			case UserModel.LAST_NAME -> setLastName(value);
			case UserModel.FIRST_NAME -> setFirstName(value);
			case UserModel.EMAIL -> setEmail(value);
			case ATTR_PICTURE -> setPicture(value);
			default -> super.setAttribute(name, values);
		}
	}

	@Override
	public void setSingleAttribute(String name, String value) {
		switch (name) {
			case UserModel.USERNAME -> setUsername(value);
			case UserModel.LAST_NAME -> setLastName(value);
			case UserModel.FIRST_NAME -> setFirstName(value);
			case UserModel.EMAIL -> setEmail(value);
			case ATTR_PICTURE -> setPicture(value);
			default -> super.setSingleAttribute(name, value);
		}
	}

	@Override
	public String getFirstAttribute(String name) {
		return switch (name) {
			case UserModel.USERNAME -> getUsername();
			case UserModel.LAST_NAME -> getLastName();
			case UserModel.FIRST_NAME -> getFirstName();
			case UserModel.EMAIL -> getEmail();
			case ATTR_PICTURE -> getPicture();
			default -> super.getFirstAttribute(name);
		};
	}

	@Override
	public Stream<String> getAttributeStream(String name) {
		return switch (name) {
			case UserModel.USERNAME -> Stream.of(getUsername());
			case UserModel.LAST_NAME -> Stream.of(getLastName());
			case UserModel.FIRST_NAME -> Stream.of(getFirstName());
			case UserModel.EMAIL -> Stream.of(getEmail());
			case "picture" -> Stream.of(getPicture());
			default -> super.getAttributeStream(name);
		};
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		MultivaluedHashMap<String, String> attributes = getFederatedStorage().getAttributes(realm, this.getId());
		if (attributes == null) {
			attributes = new MultivaluedHashMap<>();
		}
		attributes.add(UserModel.USERNAME, getUsername());
		attributes.add(UserModel.EMAIL, getEmail());
		attributes.add(UserModel.FIRST_NAME, getFirstName());
		attributes.add(UserModel.LAST_NAME, getLastName());
		attributes.add(ATTR_PICTURE, getPicture());
		return attributes;
	}

	@Override
	protected Set<GroupModel> getGroupsInternal() {
		Set<GroupModel> groups = new HashSet<>();
		if (user.getGroups() != null) {
			for (String groupName : user.getGroups()) {
				GroupModel group = session.groups().getGroupByName(realm, null, groupName);
				if (group == null) {
					group = session.groups().createGroup(realm, groupName);
				}
				groups.add(group);
			}
		}
		return groups;
	}

	@Override
	protected Set<RoleModel> getRoleMappingsInternal() {
		Set<RoleModel> roles = new HashSet<>();
		if (user.getRoles() != null) {
			for (String roleName : user.getRoles()) {
				RoleModel role = session.roles().getRealmRole(realm, roleName);
				if (role == null) {
					role = session.roles().addRealmRole(realm, roleName);
				}
				roles.add(role);
			}
		}
		return roles;
	}

	private String getPicture() {
		return user.getPictureUrl();
	}

	private void setPicture(String pictureUrl) {
		dirty = dirty || !Objects.equals(pictureUrl, user.getPictureUrl());
		user.setPictureUrl(pictureUrl);
	}
}
