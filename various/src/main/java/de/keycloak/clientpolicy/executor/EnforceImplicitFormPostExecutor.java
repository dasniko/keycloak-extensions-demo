package de.keycloak.clientpolicy.executor;

import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

@Slf4j
public class EnforceImplicitFormPostExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

	@Override
	public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
		if (context.getEvent() == ClientPolicyEvent.AUTHORIZATION_REQUEST) {
			AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext) context;
			executeOnAuthorizationRequest(authorizationRequestContext.getAuthorizationEndpointRequest());
		}
	}

	@Override
	public String getProviderId() {
		return EnforceImplicitFormPostExecutorFactory.PROVIDER_ID;
	}

	private void executeOnAuthorizationRequest(AuthorizationEndpointRequest authorizationEndpointRequest) throws ClientPolicyException {
		String responseType = authorizationEndpointRequest.getResponseType();
		String responseMode = authorizationEndpointRequest.getResponseMode(); // form_post
		if (responseType.contains("token") && !responseMode.equalsIgnoreCase("form_post")) {
			throw new ClientPolicyException("responseModeNotAllowed",
				"The requested response_mode %s is not allowed for implicit flow, only form_post is allowed.".formatted(responseMode),
				Response.Status.BAD_REQUEST);
		}
	}

}
