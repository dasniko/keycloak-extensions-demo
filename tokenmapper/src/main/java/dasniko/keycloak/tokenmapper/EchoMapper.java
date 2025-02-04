package dasniko.keycloak.tokenmapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoService(ProtocolMapper.class)
public class EchoMapper extends AbstractOIDCProtocolMapper implements UserInfoTokenMapper {

	public static final String PROVIDER_ID = "echo-mapper";

	private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

	static final String URL = "url";
	static final String URL_DEFAULT = "https://postman-echo.com/get";

	static {
		configProperties.add(new ProviderConfigProperty(URL, "Echo URL", "URL of external echo service", ProviderConfigProperty.STRING_TYPE, URL_DEFAULT));
		OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, EchoMapper.class);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getDisplayType() {
		return "Echo Mapper";
	}

	@Override
	public String getHelpText() {
		return "Map data from an external echo service to claims.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	@SneakyThrows
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
		String url = mappingModel.getConfig().getOrDefault(URL, URL_DEFAULT);
		String username = userSession.getUser().getUsername();
		log.debug("Requesting URL: {}?username={}", url, username);
		Map<String, Object> echo = SimpleHttp.doGet(url, keycloakSession).param("username", username).acceptJson().asJson(new TypeReference<>() {});
		OIDCAttributeMapperHelper.mapClaim(token, mappingModel, echo);
	}
}
