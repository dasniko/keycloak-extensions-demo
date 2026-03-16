package de.keycloak.credentials;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class OtpMigrationAdminResource {

	private final KeycloakSession session;
	private final RealmModel realm;
	private final AdminPermissionEvaluator auth;

	@POST
	@Path("import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response importSeed(OtpImportRequest request) {
		auth.users().requireManage();

		UserModel user = session.users().getUserByUsername(realm, request.username());

		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND)
				.entity(Map.of("error", "user not found: " + request.username()))
				.build();
		}

		try {
			OtpMigrationService service = new OtpMigrationService();
			String credentialId = service.migrateOtpSeed(realm, user, request.seed());
			return Response.ok(Map.of(
				"username", request.username(),
				"credentialId", credentialId,
				"status", "migrated"
			)).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity(Map.of("error", e.getMessage()))
				.build();
		}
	}

	@POST
	@Path("bulk-import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response bulkImport(List<OtpImportRequest> requests) {
		auth.users().requireManage();

		OtpMigrationService service = new OtpMigrationService();

		List<Map<String, Object>> results = requests.stream().map(request -> {
			try {
				UserModel user = session.users().getUserByUsername(realm, request.username());
				if (user == null) {
					return Map.<String, Object>of(
						"username", request.username(),
						"status", "error",
						"error", "User not found");
				}
				String credentialId = service.migrateOtpSeed(realm, user, request.seed());
				return Map.<String, Object>of(
					"username", request.username(),
					"credentialId", credentialId,
					"status", "migrated");
			} catch (Exception e) {
				return Map.<String, Object>of(
					"username", request.username(),
					"status", "error",
					"error", e.getMessage());
			}
		}).toList();

		return Response.ok(results).build();
	}

}
