package dasniko.keycloak.resource.admin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

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
	public Response bulkAddRequiredAction(Map<String, String> payload) {
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

	@DELETE
	@Path("users/{userId}/invalidate")
	public Response invalidateUser(@PathParam("userId") String userId) {
		// do the authorization with the existing admin permissions
		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireManage();

		// search user with a given userId
		UserModel user = session.users().getUserById(realm, userId);
		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		// evict user in current realm from cache
		UserCache userCache = session.getProvider(UserCache.class);
		if (userCache != null) {
			userCache.evict(realm, user);
		}

		return Response.noContent().build();
	}

}
