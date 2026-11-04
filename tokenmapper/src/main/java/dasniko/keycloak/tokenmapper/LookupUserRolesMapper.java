package dasniko.keycloak.tokenmapper;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Writes the realm and client roles of <em>another</em> user (looked up by username) into the access token and the token
 * introspection response, either merged with the roles of the current user or replacing them.
 * <p>
 * The roles are resolved exactly like the built-in role mappers do: composite roles are expanded and the result is restricted
 * by the scope of the requesting client (full scope allowed, role scope mappings of the client and its client scopes).
 */
@Slf4j
@AutoService(ProtocolMapper.class)
public class LookupUserRolesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

	public static final String PROVIDER_ID = "oidc-lookup-user-roles-mapper";

	static final String LOOKUP_USERNAME = "lookup.username";
	static final String MERGE = "merge";
	static final String REALM_ROLE_PREFIX = "realm.role.prefix";
	static final String REALM_CLAIM_NAME = "realm.claim.name";
	static final String CLIENT_ROLE_PREFIX = "client.role.prefix";
	static final String CLIENT_CLAIM_NAME = "client.claim.name";

	static final String DEFAULT_REALM_CLAIM_NAME = "realm_access.roles";
	static final String DEFAULT_CLIENT_CLAIM_NAME = "resource_access.${client_id}.roles";

	private static final Pattern CLIENT_ID_PATTERN = Pattern.compile(Pattern.quote("${client_id}"));

	private static final List<ProviderConfigProperty> configProperties = ProviderConfigurationBuilder.create()
		.property()
			.name(LOOKUP_USERNAME)
			.label("Lookup Username")
			.helpText("Username of the user whose realm and client roles are written to the token.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.add()
		.property()
			.name(MERGE)
			.label("Merge with current user's roles")
			.helpText("If on, the roles of the looked-up user are added to the roles of the current user. "
				+ "If off, the roles of the looked-up user replace them: the 'realm_access' and 'resource_access' claims "
				+ "and the claims configured below are cleared before the roles are written.")
			.type(ProviderConfigProperty.BOOLEAN_TYPE)
			.defaultValue("true")
			.add()
		.property()
			.name(REALM_CLAIM_NAME)
			.label("Realm roles claim name")
			.helpText("Claim path for the realm roles, e.g. 'realm_access.roles'. Nested claims are separated by dots.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.defaultValue(DEFAULT_REALM_CLAIM_NAME)
			.add()
		.property()
			.name(REALM_ROLE_PREFIX)
			.label("Realm role prefix")
			.helpText("A prefix for each realm role.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.add()
		.property()
			.name(CLIENT_CLAIM_NAME)
			.label("Client roles claim name")
			.helpText("Claim path for the client roles, e.g. 'resource_access.${client_id}.roles'. "
				+ "'${client_id}' is replaced by the client ID owning the roles; roles of all clients are mapped.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.defaultValue(DEFAULT_CLIENT_CLAIM_NAME)
			.add()
		.property()
			.name(CLIENT_ROLE_PREFIX)
			.label("Client role prefix")
			.helpText("A prefix for each client role. '${client_id}' is replaced by the client ID owning the role.")
			.type(ProviderConfigProperty.STRING_TYPE)
			.add()
		.build();

	static {
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, LookupUserRolesMapper.class);
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
		return "Lookup User Roles";
	}

	@Override
	public String getHelpText() {
		return "Map the realm and client roles of another user (found by username) to the access token and the token introspection response.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	/**
	 * Must run after the realm and client role mappers (priority 40) to be able to merge into or replace their result,
	 * but before the script mapper (priority 50), which expects the final roles in the token.
	 */
	@Override
	public int getPriority() {
		return ProtocolMapperUtils.PRIORITY_ROLE_MAPPER + 1;
	}

	@Override
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession session,
	                        ClientSessionContext clientSessionCtx) {
		Map<String, String> config = mappingModel.getConfig();
		String username = config.get(LOOKUP_USERNAME);
		RealmModel realm = session.getContext().getRealm();

		if (username == null || username.isBlank()) {
			log.warn("Mapper '{}' in realm '{}' has no lookup username configured, leaving token unchanged", mappingModel.getName(),
				realm.getName());
			return;
		}

		UserModel lookupUser = session.users().getUserByUsername(realm, username.trim());
		if (lookupUser == null || !lookupUser.isEnabled()) {
			// the username itself is deliberately not logged
			log.warn("Mapper '{}' in realm '{}': configured lookup user not found or disabled, leaving token unchanged",
				mappingModel.getName(), realm.getName());
			return;
		}

		String realmClaim = config.getOrDefault(REALM_CLAIM_NAME, DEFAULT_REALM_CLAIM_NAME);
		String clientClaim = config.getOrDefault(CLIENT_CLAIM_NAME, DEFAULT_CLIENT_CLAIM_NAME);
		String realmPrefix = config.get(REALM_ROLE_PREFIX);
		String clientPrefix = config.get(CLIENT_ROLE_PREFIX);
		boolean merge = Boolean.parseBoolean(config.getOrDefault(MERGE, "true"));

		ClientModel client = clientSessionCtx.getClientSession().getClient();
		Set<RoleModel> lookupRoles = TokenManager.getAccess(lookupUser, client, clientSessionCtx.getClientScopesStream());
		Set<String> lookupRealmRoles = new TreeSet<>();
		Map<String, Set<String>> lookupClientRoles = new TreeMap<>();
		resolve(lookupRoles, lookupRealmRoles, lookupClientRoles);

		// Clients the current user has roles in; their claims (and audiences) may already have been written by other mappers
		Set<String> currentClientIds = new LinkedHashSet<>();
		if (!merge) {
			Set<RoleModel> currentRoles = TokenManager.getAccess(userSession.getUser(), client, clientSessionCtx.getClientScopesStream());
			currentClientIds.addAll(resolve(currentRoles, new TreeSet<>(), new TreeMap<>()));
			Set<String> clientIds = new LinkedHashSet<>(lookupClientRoles.keySet());
			clientIds.addAll(currentClientIds);
			clear(token, realmClaim, clientClaim, clientIds);
		}

		if (!lookupRealmRoles.isEmpty()) {
			mapRoles(token, realmClaim, null, prefixed(realmPrefix, lookupRealmRoles));
		}
		lookupClientRoles.forEach((clientId, roles) -> {
			String effectivePrefix = clientPrefix == null || clientPrefix.isBlank() ? clientPrefix : replaceClientId(clientPrefix, clientId);
			mapRoles(token, clientClaim, clientId, prefixed(effectivePrefix, roles));
		});

		if (token instanceof AccessToken accessToken && isAudienceResolved(clientSessionCtx)) {
			adjustAudience(accessToken, client.getClientId(), currentClientIds, lookupClientRoles.keySet());
		}
	}

	/**
	 * The audience resolve mapper (priority 30) runs before this mapper and derives the audience from the current user's roles.
	 * We only touch the audience if that mapper is active, otherwise the audience was never role-based.
	 * <p>
	 * {@link #setClaim} doesn't know whether the access token or the introspection response is built, so "active" means included in
	 * at least one of the two. If the audience resolve mapper covers only one of them, the audience of the other one is adjusted too.
	 */
	private static boolean isAudienceResolved(ClientSessionContext clientSessionCtx) {
		return clientSessionCtx.getProtocolMappersStream()
			.filter(m -> AudienceResolveProtocolMapper.PROVIDER_ID.equals(m.getProtocolMapper()))
			// like the audience resolve mapper itself: an unset flag means 'included'
			.anyMatch(m -> m.getConfig() == null
				|| !"false".equals(m.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN))
				|| !"false".equals(m.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION)));
	}

	/**
	 * Mirrors the audience resolve mapper for the looked-up user's roles: every client with roles in the token is an audience,
	 * except the requesting client itself. In replace mode, the audiences derived from the current user's roles are removed first.
	 * (An audience for the same client added by an explicit audience mapper is removed as well, we can't tell them apart.)
	 */
	private static void adjustAudience(AccessToken token, String requestingClientId, Set<String> removeClientIds, Set<String> addClientIds) {
		if (!removeClientIds.isEmpty() && token.getAudience() != null) {
			String[] remaining = Arrays.stream(token.getAudience()).filter(aud -> !removeClientIds.contains(aud)).toArray(String[]::new);
			token.audience(remaining.length == 0 ? null : remaining);
		}
		addClientIds.stream().filter(id -> !id.equals(requestingClientId)).forEach(token::addAudience);
	}

	/**
	 * Splits the roles into realm roles and enabled clients' roles.
	 *
	 * @return the IDs of all enabled clients owning at least one of the roles
	 */
	private static Set<String> resolve(Set<RoleModel> roles, Set<String> realmRoles, Map<String, Set<String>> clientRoles) {
		for (RoleModel role : roles) {
			if (role.getContainer() instanceof ClientModel app) {
				if (app.isEnabled()) {
					clientRoles.computeIfAbsent(app.getClientId(), k -> new TreeSet<>()).add(role.getName());
				}
			} else {
				realmRoles.add(role.getName());
			}
		}
		return clientRoles.keySet();
	}

	private static List<String> prefixed(String prefix, Set<String> roles) {
		if (prefix == null || prefix.isEmpty()) {
			return new ArrayList<>(roles);
		}
		return roles.stream().map(role -> prefix + role).collect(Collectors.toCollection(ArrayList::new));
	}

	private static String replaceClientId(String template, String clientId) {
		return CLIENT_ID_PATTERN.matcher(template).replaceAll(Matcher.quoteReplacement(clientId));
	}

	/** The client ID is used as a claim path component, so dots and backslashes in it must be escaped. */
	private static String claimPath(String claimName, String clientId) {
		if (clientId == null) {
			return claimName;
		}
		String escaped = clientId.replace("\\", "\\\\").replace(".", "\\.");
		return CLIENT_ID_PATTERN.matcher(claimName).replaceAll(Matcher.quoteReplacement(escaped));
	}

	private static void mapRoles(IDToken token, String claimName, String clientId, List<String> roles) {
		List<String> path = JsonUtils.splitClaimPath(claimPath(claimName, clientId));
		if (path.isEmpty()) {
			return;
		}

		// roles in the typed 'realm_access' / 'resource_access' properties of the access token
		if (token instanceof AccessToken accessToken) {
			AccessToken.Access access = null;
			if (path.size() == 2 && "realm_access".equals(path.get(0)) && "roles".equals(path.get(1))) {
				access = accessToken.getRealmAccess();
				if (access == null) {
					access = new AccessToken.Access();
					accessToken.setRealmAccess(access);
				}
			} else if (path.size() == 3 && "resource_access".equals(path.get(0)) && "roles".equals(path.get(2))) {
				access = accessToken.addAccess(path.get(1));
			}
			if (access != null) {
				roles.forEach(access::addRole);
				return;
			}
		}

		JsonUtils.mapClaim(path, roles, token.getOtherClaims(), true);
	}

	private static void clear(IDToken token, String realmClaim, String clientClaim, Set<String> clientIds) {
		if (token instanceof AccessToken accessToken) {
			accessToken.setRealmAccess(null);
			accessToken.setResourceAccess(null);
		}
		removeClaim(token.getOtherClaims(), JsonUtils.splitClaimPath(realmClaim));
		if (CLIENT_ID_PATTERN.matcher(clientClaim).find()) {
			clientIds.forEach(clientId -> removeClaim(token.getOtherClaims(), JsonUtils.splitClaimPath(claimPath(clientClaim, clientId))));
		} else {
			removeClaim(token.getOtherClaims(), JsonUtils.splitClaimPath(clientClaim));
		}
	}

	/** Removes the claim at the given path and the parent objects left empty by that. */
	@SuppressWarnings("unchecked")
	private static void removeClaim(Map<String, Object> claims, List<String> path) {
		if (path.isEmpty()) {
			return;
		}
		String head = path.getFirst();
		if (path.size() == 1) {
			claims.remove(head);
			return;
		}
		if (claims.get(head) instanceof Map<?, ?> nested) {
			removeClaim((Map<String, Object>) nested, path.subList(1, path.size()));
			if (nested.isEmpty()) {
				claims.remove(head);
			}
		}
	}
}
