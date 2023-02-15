package dasniko.keycloak.user.peanuts;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@RequiredArgsConstructor
public class UserRoleModel implements RoleModel {

	private final String name;
	private final RealmModel realm;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void setDescription(String s) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public String getId() {
		return name;
	}

	@Override
	public void setName(String s) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public void addCompositeRole(RoleModel roleModel) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public void removeCompositeRole(RoleModel roleModel) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public Stream<RoleModel> getCompositesStream(String s, Integer integer, Integer integer1) {
		return Stream.empty();
	}

	@Override
	public boolean isClientRole() {
		return false;
	}

	@Override
	public String getContainerId() {
		return realm.getId();
	}

	@Override
	public RoleContainerModel getContainer() {
		return realm;
	}

	@Override
	public boolean hasRole(RoleModel roleModel) {
		return this.equals(roleModel) || this.name.equals(roleModel.getName());
	}

	@Override
	public void setSingleAttribute(String s, String s1) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public void setAttribute(String s, List<String> list) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public void removeAttribute(String s) {
		throw new ReadOnlyException("role is read only");
	}

	@Override
	public Stream<String> getAttributeStream(String s) {
		return Stream.empty();
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		return Map.of();
	}
}
