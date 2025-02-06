package de.keycloak.models;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractRoleModel implements RoleModel {

	private final String name;
	private final RoleContainerModel container;

	@Override
	public String getId() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public String getDescription() {
		return "${role_%s}".formatted(name);
	}

	@Override
	public void setDescription(String description) {
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public void addCompositeRole(RoleModel role) {
	}

	@Override
	public void removeCompositeRole(RoleModel role) {
	}

	@Override
	public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
		return Stream.empty();
	}

	@Override
	public boolean isClientRole() {
		return container instanceof ClientModel;
	}

	@Override
	public String getContainerId() {
		return container.getId();
	}

	@Override
	public RoleContainerModel getContainer() {
		return container;
	}

	@Override
	public boolean hasRole(RoleModel role) {
		return this.equals(role) || this.name.equals(role.getName());
	}

	@Override
	public void setSingleAttribute(String name, String value) {
	}

	@Override
	public void setAttribute(String name, List<String> values) {
	}

	@Override
	public void removeAttribute(String name) {
	}

	@Override
	public Stream<String> getAttributeStream(String name) {
		return null;
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		return null;
	}
}
