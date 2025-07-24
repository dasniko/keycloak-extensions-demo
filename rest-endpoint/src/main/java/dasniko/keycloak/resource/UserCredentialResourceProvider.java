package dasniko.keycloak.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserCredentialResourceProvider implements RealmResourceProvider {

	private final Auth auth;

	public UserCredentialResourceProvider(KeycloakSession session) {
		this.auth = AuthHelper.getAuth(session, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, auth -> auth.getToken().hasAudience(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID));
	}

	@Override
	public Object getResource() {
		return this;
	}

	@Override
	public void close() {
	}

	@POST
	@Path("{credentialId}/setAsDefault")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setDefaultCredential(@PathParam("credentialId") String credentialId) {
		auth.require(AccountRoles.MANAGE_ACCOUNT);

		SubjectCredentialManager cm = auth.getUser().credentialManager();
		CredentialModel passwordCredential = cm.getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE).findFirst().orElse(null);
		if (passwordCredential != null) {
			cm.moveStoredCredentialTo(credentialId, passwordCredential.getId());
			return Response.noContent().build();
		}
		return Response.status(Response.Status.BAD_REQUEST).build();
	}

}
