package de.keycloak.resource.otp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.Map;

@RequiredArgsConstructor
public class OtpValidationResourceProvider implements RealmResourceProvider {

	private final KeycloakSession session;

	@Override
	public Object getResource() {
		return this;
	}

	@Override
	public void close() {
	}

	@POST
	@Path("validate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response validate(OtpValidationRequest request) {
		// TODO: add auth-check

		if (request.username() == null || request.otp() == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(Map.of("error", "username and otp are required"))
				.build();
		}

		RealmModel realm = session.getContext().getRealm();

		UserModel user = session.users().getUserByUsername(realm, request.username());
		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND)
				.entity(Map.of("error", "user not found: " + request.username()))
				.build();
		}

		String credentialId = user.credentialManager()
			.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
			.map(CredentialModel::getId)
			.findFirst()
			.orElse(null);

		if (credentialId == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(Map.of("error", "user has no OTP credential configured"))
				.build();
		}

		OTPCredentialProvider otpProvider = (OTPCredentialProvider) session.getProvider(
			CredentialProvider.class, OTPCredentialProviderFactory.PROVIDER_ID);

		boolean valid = otpProvider.isValid(realm, user,
			new UserCredentialModel(credentialId, OTPCredentialModel.TYPE, request.otp()));

		return Response.ok(Map.of("valid", valid)).build();
	}

}
