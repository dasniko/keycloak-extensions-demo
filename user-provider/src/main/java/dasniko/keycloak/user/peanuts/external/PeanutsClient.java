package dasniko.keycloak.user.peanuts.external;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
