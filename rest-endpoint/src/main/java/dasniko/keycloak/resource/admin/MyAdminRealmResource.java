package dasniko.keycloak.resource.admin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
//@Path("/admin/realms/{realm}/" + MyAdminRealmResourceProvider.PROVIDER_ID)
public class MyAdminRealmResource {

	private final KeycloakSession session;
	private final RealmModel realm;
	private final AdminPermissionEvaluator auth;

	@GET
	@Path("users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListOfUsers() {
		// do the authorization with the existing admin permissions (e.g. realm management roles)
		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireQuery();

		// collect/manipulate data accordingly to your requirements
		List<Map<String, String>> userList = session.users()
			.searchForUserStream(realm, Map.of(UserModel.SEARCH, "*"))
			.filter(userModel -> userModel.getServiceAccountClientLink() == null)
			.map(userModel -> Map.of("username", userModel.getUsername()))
			.toList();

		// then return the desired result
		return Response.ok(userList).build();
	}

	@PUT
	@Path("users/required-action")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response buldAddRequiredAction(Map<String, String> payload) {
		// do the authorization with the existing admin permissions
		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireManage();

		// search users and iterate over the result to add the required action
		if (payload.containsKey("action")) {
			session.users()
				.searchForUserStream(realm, Map.of(UserModel.SEARCH, payload.getOrDefault("search", "*")))
				.filter(userModel -> userModel.getServiceAccountClientLink() == null)
				.forEach(user -> user.addRequiredAction(payload.get("action")));
		}

		return Response.noContent().build();
	}

}
