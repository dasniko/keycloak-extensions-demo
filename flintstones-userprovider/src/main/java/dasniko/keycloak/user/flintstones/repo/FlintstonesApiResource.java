package dasniko.keycloak.user.flintstones.repo;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FlintstonesApiResource {

	private final FlintstonesRepository repository;

	@GET
	@Path("users")
	public Response findUsers(@QueryParam("search") String search,
														@QueryParam("username") String username,
														@QueryParam("email") String email,
														@QueryParam("exactMatch") boolean exactMatch) {
		List<FlintstoneUser> users = List.of();

		if (username != null || email != null) {
			String usernameOrEmail = username != null ? username : email;
			FlintstoneUser user = repository.findUserByUsernameOrEmail(usernameOrEmail, exactMatch);
			if (user != null) {
				users = List.of(user);
			}
		} else if (search != null) {
			users = repository.findUsers(search);
		} else {
			users = repository.getAllUsers();
		}

		return Response.ok(users).build();
	}

	@POST
	@Path("users")
	public Response createUser(FlintstoneUser user) {
		repository.createUser(user);
		FlintstoneUser newUser = repository.findUserByUsernameOrEmail(user.getUsername(), true);
		return Response.created(null).entity(newUser).build();
	}

	@GET
	@Path("users/{id}")
	public Response getUserById(@PathParam("id") String id) {
		FlintstoneUser user = repository.findUserById(id);
		Response.ResponseBuilder responseBuilder = user == null ? Response.status(Response.Status.NOT_FOUND) : Response.ok(user);
		return responseBuilder.build();
	}

	@PUT
	@Path("users/{id}")
	public Response updateUser(@PathParam("id") String id, FlintstoneUser user) {
		if (user == null || !id.equals(user.getId())) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		repository.updateUser(user);
		return Response.noContent().build();
	}

	@DELETE
	@Path("users/{id}")
	public Response deleteUser(@PathParam("id") String id) {
		boolean success = repository.removeUser(id);
		Response.Status status = success ? Response.Status.NO_CONTENT : Response.Status.BAD_REQUEST;
		return Response.status(status).build();
	}

	@POST
	@Path("users/{id}/credentials/verify")
	public Response verifyCredentials(@PathParam("id") String id, Credential credential) {
		boolean success = repository.validateCredentials(id, credential.getValue());
		Response.Status status = success ? Response.Status.NO_CONTENT : Response.Status.BAD_REQUEST;
		return Response.status(status).build();
	}

	@PUT
	@Path("users/{id}/credentials")
	public Response updateCredentials(@PathParam("id") String id, Credential credential) {
		boolean success = repository.updateCredentials(id, credential.getValue());
		Response.Status status = success ? Response.Status.NO_CONTENT : Response.Status.BAD_REQUEST;
		return Response.status(status).build();
	}

	@GET
	@Path("users/count")
	public Response countUsers(@QueryParam("search") String search) {
		int count = repository.getUsersCount(search);
		return Response.ok(Map.of("count", count)).build();
	}

	@GET
	@Path("groups/members")
	public Response getGroupMembers(@QueryParam("name") String name) {
		List<FlintstoneUser> users = repository.findUsersByGroupname(name);
		return Response.ok(users).build();
	}

	@GET
	@Path("roles/members")
	public Response getRoleMembers(@QueryParam("name") String name) {
		List<FlintstoneUser> users = repository.findUsersByRolename(name);
		return Response.ok(users).build();
	}


}
