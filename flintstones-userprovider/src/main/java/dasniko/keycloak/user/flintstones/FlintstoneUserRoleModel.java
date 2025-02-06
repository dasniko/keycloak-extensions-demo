package dasniko.keycloak.user.flintstones;

import de.keycloak.models.AbstractRoleModel;
import org.keycloak.models.RoleContainerModel;

public class FlintstoneUserRoleModel extends AbstractRoleModel {
	public FlintstoneUserRoleModel(String name, RoleContainerModel container) {
		super(name, container);
	}
}
