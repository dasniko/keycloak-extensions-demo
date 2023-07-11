package dasniko.keycloak.user.peanuts;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@RequiredArgsConstructor
public class UserGroupModel implements GroupModel {

	private final String name;

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String s) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public void setSingleAttribute(String s, String s1) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public void setAttribute(String s, List<String> list) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public void removeAttribute(String s) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public String getFirstAttribute(String s) {
		return null;
	}

	@Override
	public Stream<String> getAttributeStream(String s) {
		return Stream.empty();
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		return Map.of();
	}

	@Override
	public GroupModel getParent() {
		return null;
	}

	@Override
	public String getParentId() {
		return null;
	}

	@Override
	public Stream<GroupModel> getSubGroupsStream() {
		return Stream.empty();
	}

	@Override
	public void setParent(GroupModel groupModel) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public void addChild(GroupModel groupModel) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public void removeChild(GroupModel groupModel) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public Stream<RoleModel> getRealmRoleMappingsStream() {
		return Stream.empty();
	}

	@Override
	public Stream<RoleModel> getClientRoleMappingsStream(ClientModel clientModel) {
		return Stream.empty();
	}

	@Override
	public boolean hasRole(RoleModel roleModel) {
		return false;
	}

	@Override
	public void grantRole(RoleModel roleModel) {
		throw new ReadOnlyException("group is read only");
	}

	@Override
	public Stream<RoleModel> getRoleMappingsStream() {
		return Stream.empty();
	}

	@Override
	public void deleteRoleMapping(RoleModel roleModel) {
		throw new ReadOnlyException("group is read only");
	}
}
