package de.keycloak.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.services.error.KeycloakErrorHandler;
import org.keycloak.utils.MediaType;

import javax.annotation.Priority;

@Provider
@Priority(1)
public class CustomKeycloakErrorHandler extends KeycloakErrorHandler {

	@Override
	public Response toResponse(Throwable throwable) {
		if (throwable instanceof CustomKeycloakException) {
			return Response.status(Response.Status.BAD_REQUEST)
				.type(MediaType.TEXT_HTML_UTF_8_TYPE)
				.entity(throwable.getMessage())
				.build();
		}
		return super.toResponse(throwable);
	}

}
