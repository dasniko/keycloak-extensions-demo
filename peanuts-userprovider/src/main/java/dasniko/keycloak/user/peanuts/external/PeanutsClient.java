package dasniko.keycloak.user.peanuts.external;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PeanutsClient {

	@GET
	List<Peanut> getPeanuts(@QueryParam("search") String search, @QueryParam("first") int first, @QueryParam("max") int max);

	@GET
	@Path("/count")
	Integer getPeanutsCount();

	@GET
	@Path("/{id}")
	Peanut getPeanutById(@PathParam("id") String id);

	@GET
	@Path("/{id}/credentials")
	CredentialData getCredentialData(@PathParam("id") String id);

	@PUT
	@Path("/{id}/credentials")
	Response updateCredentialData(@PathParam("id") String id, CredentialData credentialData);

}
