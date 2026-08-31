package dasniko.keycloak.tokenmapper;

import com.google.auto.service.AutoService;
import de.keycloak.provider.DefaultServerInfoAware;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;

@AutoService(ProtocolMapper.class)
public class SessionNotesPrefixProtocolMapper extends AbstractOIDCProtocolMapper
	implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, DefaultServerInfoAware {

	public static final String PROVIDER_ID = "oidc-sessionnote-mapper";

	String SESSION_ATTRIBUTE_PREFIX = "dasniko.saml_prop_";

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

	static {
		OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, SessionNotesPrefixProtocolMapper.class);
	}

	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIG_PROPERTIES;
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "User Session Notes with Prefix";
	}

	@Override
	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	@Override
	public String getHelpText() {
		return "Map all user session notes with a specific prefix to token claims.";
	}

	@Override
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
		userSession.getNotes().entrySet().stream()
			.filter(e -> e.getKey().startsWith(SESSION_ATTRIBUTE_PREFIX))
			.forEach(e -> {
				mappingModel.getConfig().put(TOKEN_CLAIM_NAME, e.getKey().replace(SESSION_ATTRIBUTE_PREFIX, ""));
				OIDCAttributeMapperHelper.mapClaim(token, mappingModel, e.getValue());
			});
	}
}
