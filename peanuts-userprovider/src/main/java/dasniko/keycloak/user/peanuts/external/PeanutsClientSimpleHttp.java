package dasniko.keycloak.user.peanuts.external;

import com.fasterxml.jackson.core.type.TypeReference;
import dasniko.keycloak.user.peanuts.Constants;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class PeanutsClientSimpleHttp implements PeanutsClient {

	private final CloseableHttpClient httpClient;
	private final String baseUrl;
	private final String basicUsername;
	private final String basicPassword;

	public PeanutsClientSimpleHttp(KeycloakSession session, ComponentModel model) {
		this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
		this.baseUrl = model.get(Constants.BASE_URL);
		this.basicUsername = model.get(Constants.AUTH_USERNAME);
		this.basicPassword = model.get(Constants.AUTH_PASSWORD);
	}

	@Override
	@SneakyThrows
	public List<Peanut> getPeanuts(String search, int first, int max) {
		SimpleHttp simpleHttp = SimpleHttp.doGet(baseUrl, httpClient).authBasic(basicUsername, basicPassword)
			.param("first", String.valueOf(first))
			.param("max", String.valueOf(max));
		if (search != null) {
			simpleHttp.param("search", search);
		}
		return simpleHttp.asJson(new TypeReference<>() {});
	}

	@Override
	@SneakyThrows
	public Integer getPeanutsCount() {
		String url = String.format("%s/count", baseUrl);
		String count = SimpleHttp.doGet(url, httpClient).authBasic(basicUsername, basicPassword).asString();
		return Integer.valueOf(count);
	}

	@Override
	@SneakyThrows
	public Peanut getPeanutById(String id) {
		String url = String.format("%s/%s", baseUrl, id);
		SimpleHttp.Response response = SimpleHttp.doGet(url, httpClient).authBasic(basicUsername, basicPassword).asResponse();
		if (response.getStatus() == 404) {
			throw new WebApplicationException(response.getStatus());
		}
		return response.asJson(Peanut.class);
	}

	@Override
	@SneakyThrows
	public CredentialData getCredentialData(String id) {
		String url = String.format("%s/%s/credentials", baseUrl, id);
		SimpleHttp.Response response = SimpleHttp.doGet(url, httpClient).authBasic(basicUsername, basicPassword).asResponse();
		if (response.getStatus() == 404) {
			throw new WebApplicationException(response.getStatus());
		}
		return response.asJson(CredentialData.class);
	}

	@Override
	@SneakyThrows
	public Response updateCredentialData(String id, CredentialData credentialData) {
		String url = String.format("%s/%s/credentials", baseUrl, id);
		int status = SimpleHttp.doPut(url, httpClient).authBasic(basicUsername, basicPassword).json(credentialData).asStatus();
		return Response.status(status).build();
	}

}
