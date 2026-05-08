package dasniko.keycloak.broker.oidc.mapper;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@AutoService(IdentityProviderMapper.class)
public class ClaimToGroupRoleSyncMapper extends AbstractClaimMapper {

	public static final String PROVIDER_ID = "oidc-claim-to-group-role-sync-mapper";
	public static final String CONFIG_MAPPER_TYPE = "mapper.type";
	public static final String CONFIG_CLIENT_ID = "client.id";

	private static final String[] COMPATIBLE_PROVIDERS = {
		OIDCIdentityProviderFactory.PROVIDER_ID,
		KeycloakOIDCIdentityProviderFactory.PROVIDER_ID
	};

	private static final Set<IdentityProviderSyncMode> SYNC_MODES = EnumSet.allOf(IdentityProviderSyncMode.class);

	private enum SyncType {
		GROUPS, REALM_ROLES, CLIENT_ROLES
	}

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
		.property()
			.name(CLAIM)
			.label("Claim")
			.helpText("Name of the token claim containing group/role names. Supports nested claims via '.'-notation.")
			.type(ProviderConfigProperty.STRING_TYPE)
		.add()
		.property()
			.name(CONFIG_MAPPER_TYPE)
			.label("Sync Type")
			.helpText("Whether to sync groups, realm roles, or client roles.")
			.type(ProviderConfigProperty.LIST_TYPE)
			.options(Arrays.stream(SyncType.values()).map(Enum::name).toList())
			.defaultValue(SyncType.GROUPS.name())
		.add()
		.property()
			.name(CONFIG_CLIENT_ID)
			.label("Client ID")
			.helpText("Client ID for client role sync. Only used when Sync Type is CLIENT_ROLES.")
			.type(ProviderConfigProperty.STRING_TYPE)
		.add()
		.build();

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String[] getCompatibleProviders() {
		return COMPATIBLE_PROVIDERS;
	}

	@Override
	public String getDisplayCategory() {
		return "Group Importer";
	}

	@Override
	public String getDisplayType() {
		return "Claim to Group/Role Sync";
	}

	@Override
	public String getHelpText() {
		return "Reads a claim value (string or array) from the IdP token and syncs the user's group or role memberships. " +
			"Groups/roles are auto-created if missing. Memberships no longer present in the claim are removed; " +
			"for groups, only direct top-level memberships are managed.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIG_PROPERTIES;
	}

	@Override
	public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
		return SYNC_MODES.contains(syncMode);
	}

	@Override
	public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
			IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		syncGroupsOrRoles(session, realm, user, mapperModel, context);
	}

	@Override
	public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user,
			IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		syncGroupsOrRoles(session, realm, user, mapperModel, context);
	}

	@Override
	public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
			IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		syncGroupsOrRoles(session, realm, user, mapperModel, context);
	}

	private void syncGroupsOrRoles(KeycloakSession session, RealmModel realm, UserModel user,
			IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		Set<String> claimValues = extractClaimValues(mapperModel, context);
		if (claimValues == null) {
			return;
		}
		switch (getSyncType(mapperModel)) {
			case GROUPS -> syncGroups(session, realm, user, claimValues);
			case REALM_ROLES -> syncRealmRoles(realm, user, claimValues);
			case CLIENT_ROLES -> {
				ClientModel client = resolveClient(realm, mapperModel);
				if (client != null) {
					syncClientRoles(client, user, claimValues);
				}
			}
		}
	}

	// Returns null if the claim is absent (skip sync), empty set if claim is present but empty.
	private Set<String> extractClaimValues(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		Object raw = getClaimValue(mapperModel, context);
		if (raw == null) {
			return null;
		}
		Set<String> values = new HashSet<>();
		if (raw instanceof String s) {
			values.add(s);
		} else if (raw instanceof List<?> list) {
			list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(values::add);
		}
		return values;
	}

	private void syncGroups(KeycloakSession session, RealmModel realm, UserModel user, Set<String> claimValues) {
		claimValues.forEach(name -> user.joinGroup(getOrCreateGroup(session, realm, name)));
		user.getGroupsStream()
			.filter(g -> g.getParentId() == null)
			.filter(g -> !claimValues.contains(g.getName()))
			.forEach(user::leaveGroup);
	}

	private void syncRealmRoles(RealmModel realm, UserModel user, Set<String> claimValues) {
		claimValues.forEach(name -> user.grantRole(getOrCreateRole(realm, name)));
		user.getRealmRoleMappingsStream()
			.filter(r -> !claimValues.contains(r.getName()))
			.forEach(user::deleteRoleMapping);
	}

	private void syncClientRoles(ClientModel client, UserModel user, Set<String> claimValues) {
		claimValues.forEach(name -> user.grantRole(getOrCreateRole(client, name)));
		user.getClientRoleMappingsStream(client)
			.filter(r -> !claimValues.contains(r.getName()))
			.forEach(user::deleteRoleMapping);
	}

	private GroupModel getOrCreateGroup(KeycloakSession session, RealmModel realm, String name) {
		return session.groups().getTopLevelGroupsStream(realm, name, true, null, null)
			.findFirst()
			.orElseGet(() -> {
				try {
					return session.groups().createGroup(realm, name);
				} catch (ModelDuplicateException e) {
					return session.groups().getTopLevelGroupsStream(realm, name, true, null, null)
						.findFirst().orElseThrow(() -> e);
				}
			});
	}

	private RoleModel getOrCreateRole(RoleContainerModel roleContainer, String name) {
		RoleModel existing = roleContainer.getRole(name);
		if (existing != null) return existing;
		try {
			return roleContainer.addRole(name);
		} catch (ModelDuplicateException e) {
			return roleContainer.getRole(name);
		}
	}

	private ClientModel resolveClient(RealmModel realm, IdentityProviderMapperModel mapperModel) {
		String clientId = mapperModel.getConfig().get(CONFIG_CLIENT_ID);
		if (clientId == null || clientId.isBlank()) {
			log.warn("Mapper '{}' has type CLIENT_ROLES but no client ID configured", mapperModel.getName());
			return null;
		}
		ClientModel client = realm.getClientByClientId(clientId);
		if (client == null) {
			log.warn("Client '{}' referenced by mapper '{}' not found in realm '{}'", clientId, mapperModel.getName(), realm.getName());
		}
		return client;
	}

	private SyncType getSyncType(IdentityProviderMapperModel mapperModel) {
		String raw = mapperModel.getConfig().getOrDefault(CONFIG_MAPPER_TYPE, SyncType.GROUPS.name());
		try {
			return SyncType.valueOf(raw);
		} catch (IllegalArgumentException e) {
			log.warn("Unknown mapper type '{}' in mapper '{}', defaulting to GROUPS", raw, mapperModel.getName());
			return SyncType.GROUPS;
		}
	}
}
