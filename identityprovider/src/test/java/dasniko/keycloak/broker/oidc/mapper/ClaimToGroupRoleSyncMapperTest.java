package dasniko.keycloak.broker.oidc.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.JsonWebToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dasniko.keycloak.broker.oidc.mapper.ClaimToGroupRoleSyncMapper.CONFIG_CLIENT_ID;
import static dasniko.keycloak.broker.oidc.mapper.ClaimToGroupRoleSyncMapper.CONFIG_MAPPER_TYPE;
import static org.keycloak.broker.oidc.mappers.AbstractClaimMapper.CLAIM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimToGroupRoleSyncMapperTest {

	@Mock KeycloakSession session;
	@Mock GroupProvider groupProvider;
	@Mock RealmModel realm;
	@Mock UserModelStub user;
	@Mock GroupModel groupAdmin;
	@Mock GroupModel groupEditor;
	@Mock RoleModel roleAdmin;
	@Mock RoleModel roleEditor;
	@Mock ClientModel client;

	ClaimToGroupRoleSyncMapper mapper;
	IdentityProviderMapperModel mapperModel;

	// UserModel is a very large interface — stub only what the tests actually need
	interface UserModelStub extends org.keycloak.models.UserModel {
	}

	@BeforeEach
	void setUp() {
		mapper = new ClaimToGroupRoleSyncMapper();
		mapperModel = new IdentityProviderMapperModel();
		mapperModel.setName("test-mapper");
	}

	private BrokeredIdentityContext contextWithClaim(String claimName, Object claimValue) {
		IdentityProviderModel idpModel = new IdentityProviderModel();
		idpModel.setEnabled(true);
		BrokeredIdentityContext ctx = new BrokeredIdentityContext("ext-user-id", idpModel);
		JsonWebToken idToken = new JsonWebToken();
		idToken.setOtherClaims(claimName, claimValue);
		ctx.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, idToken);
		return ctx;
	}

	private BrokeredIdentityContext contextWithoutClaim() {
		IdentityProviderModel idpModel = new IdentityProviderModel();
		idpModel.setEnabled(true);
		return new BrokeredIdentityContext("ext-user-id", idpModel);
	}

	// -------------------------------------------------------------------------
	// Groups sync
	// -------------------------------------------------------------------------

	@Nested
	class GroupSync {

		@BeforeEach
		void setUpGroupConfig() {
			mapperModel.setConfig(Map.of(
				CLAIM, "groups",
				CONFIG_MAPPER_TYPE, "GROUPS"
			));
		}

		private void stubGroupProvider() {
			when(session.groups()).thenReturn(groupProvider);
		}

		@Test
		void newUser_assignsGroupsFromArrayClaim() {
			stubGroupProvider();
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("admin"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupAdmin));
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("editor"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupEditor));
			when(user.getGroupsStream()).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("groups", List.of("admin", "editor")));

			verify(user).joinGroup(groupAdmin);
			verify(user).joinGroup(groupEditor);
		}

		@Test
		void newUser_assignsGroupsFromSingleStringClaim() {
			stubGroupProvider();
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("admin"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupAdmin));
			when(user.getGroupsStream()).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("groups", "admin"));

			verify(user).joinGroup(groupAdmin);
		}

		@Test
		void newUser_autocreatesGroupIfMissing() {
			stubGroupProvider();
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("admin"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.empty());
			when(groupProvider.createGroup(realm, "admin")).thenReturn(groupAdmin);
			when(user.getGroupsStream()).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("groups", "admin"));

			verify(groupProvider).createGroup(realm, "admin");
			verify(user).joinGroup(groupAdmin);
		}

		@Test
		void updateUser_removesTopLevelGroupNoLongerInClaim() {
			stubGroupProvider();
			when(groupAdmin.getName()).thenReturn("admin");
			when(groupAdmin.getParentId()).thenReturn(null);
			when(groupEditor.getName()).thenReturn("editor");
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("editor"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupEditor));
			// user currently has both groups; claim now only contains "editor"
			when(user.getGroupsStream()).thenReturn(Stream.of(groupAdmin, groupEditor));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("groups", "editor"));

			verify(user).leaveGroup(groupAdmin);
			verify(user, never()).leaveGroup(groupEditor);
		}

		@Test
		void updateUser_doesNotTouchNestedGroups() {
			stubGroupProvider();
			when(groupAdmin.getParentId()).thenReturn("some-parent-id"); // nested, not top-level
			when(groupEditor.getName()).thenReturn("editor");
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("editor"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupEditor));
			when(user.getGroupsStream()).thenReturn(Stream.of(groupAdmin, groupEditor));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("groups", "editor"));

			verify(user, never()).leaveGroup(groupAdmin);
		}

		@Test
		void updateUser_joinGroupCalledIdempotently() {
			// joinGroup is called unconditionally; Keycloak's JPA layer is idempotent
			stubGroupProvider();
			when(groupAdmin.getName()).thenReturn("admin");
			when(groupAdmin.getParentId()).thenReturn(null);
			when(groupProvider.getTopLevelGroupsStream(eq(realm), eq("admin"), eq(true), isNull(), isNull()))
				.thenReturn(Stream.of(groupAdmin));
			when(user.getGroupsStream()).thenReturn(Stream.of(groupAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("groups", "admin"));

			verify(user).joinGroup(groupAdmin);
			verify(user, never()).leaveGroup(any());
		}

		@Test
		void absentClaim_skipsSync() {
			mapper.updateBrokeredUser(session, realm, user, mapperModel, contextWithoutClaim());

			verify(session, never()).groups();
			verify(user, never()).joinGroup(any());
			verify(user, never()).leaveGroup(any());
		}

		@Test
		void emptyClaim_removesAllTopLevelGroups() {
			when(groupAdmin.getParentId()).thenReturn(null);
			when(user.getGroupsStream()).thenReturn(Stream.of(groupAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("groups", List.of()));

			verify(user).leaveGroup(groupAdmin);
		}
	}

	// -------------------------------------------------------------------------
	// Realm role sync
	// -------------------------------------------------------------------------

	@Nested
	class RealmRoleSync {

		@BeforeEach
		void setUpRealmRoleConfig() {
			mapperModel.setConfig(Map.of(
				CLAIM, "roles",
				CONFIG_MAPPER_TYPE, "REALM_ROLES"
			));
		}

		@Test
		void newUser_grantsRolesFromClaim() {
			when(realm.getRole("admin")).thenReturn(roleAdmin);
			when(realm.getRole("editor")).thenReturn(roleEditor);
			when(user.getRealmRoleMappingsStream()).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", List.of("admin", "editor")));

			verify(user).grantRole(roleAdmin);
			verify(user).grantRole(roleEditor);
		}

		@Test
		void newUser_autocreatesRoleIfMissing() {
			when(realm.getRole("admin")).thenReturn(null);
			when(realm.addRole("admin")).thenReturn(roleAdmin);
			when(user.getRealmRoleMappingsStream()).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "admin"));

			verify(realm).addRole("admin");
			verify(user).grantRole(roleAdmin);
		}

		@Test
		void updateUser_removesRealmRoleNoLongerInClaim() {
			when(roleAdmin.getName()).thenReturn("admin");
			when(realm.getRole("editor")).thenReturn(roleEditor);
			when(user.getRealmRoleMappingsStream()).thenReturn(Stream.of(roleAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "editor"));

			verify(user).deleteRoleMapping(roleAdmin);
			verify(user, never()).deleteRoleMapping(roleEditor);
		}

		@Test
		void absentClaim_skipsSync() {
			mapper.updateBrokeredUser(session, realm, user, mapperModel, contextWithoutClaim());

			verify(user, never()).grantRole(any());
			verify(user, never()).deleteRoleMapping(any());
		}

		@Test
		void emptyClaim_removesAllRealmRoles() {
			when(roleAdmin.getName()).thenReturn("admin");
			when(user.getRealmRoleMappingsStream()).thenReturn(Stream.of(roleAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("roles", List.of()));

			verify(user).deleteRoleMapping(roleAdmin);
		}
	}

	// -------------------------------------------------------------------------
	// Client role sync
	// -------------------------------------------------------------------------

	@Nested
	class ClientRoleSync {

		@BeforeEach
		void setUpClientRoleConfig() {
			mapperModel.setConfig(Map.of(
				CLAIM, "roles",
				CONFIG_MAPPER_TYPE, "CLIENT_ROLES",
				CONFIG_CLIENT_ID, "my-client"
			));
		}

		@Test
		void newUser_grantsClientRolesFromClaim() {
			when(realm.getClientByClientId("my-client")).thenReturn(client);
			when(client.getRole("admin")).thenReturn(roleAdmin);
			when(client.getRole("editor")).thenReturn(roleEditor);
			when(user.getClientRoleMappingsStream(client)).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", List.of("admin", "editor")));

			verify(user).grantRole(roleAdmin);
			verify(user).grantRole(roleEditor);
		}

		@Test
		void newUser_autocreatesClientRoleIfMissing() {
			when(realm.getClientByClientId("my-client")).thenReturn(client);
			when(client.getRole("admin")).thenReturn(null);
			when(client.addRole("admin")).thenReturn(roleAdmin);
			when(user.getClientRoleMappingsStream(client)).thenReturn(Stream.empty());

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "admin"));

			verify(client).addRole("admin");
			verify(user).grantRole(roleAdmin);
		}

		@Test
		void updateUser_removesClientRoleNoLongerInClaim() {
			when(realm.getClientByClientId("my-client")).thenReturn(client);
			when(roleAdmin.getName()).thenReturn("admin");
			when(client.getRole("editor")).thenReturn(roleEditor);
			when(user.getClientRoleMappingsStream(client)).thenReturn(Stream.of(roleAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "editor"));

			verify(user).deleteRoleMapping(roleAdmin);
		}

		@Test
		void emptyClaim_removesAllClientRoles() {
			when(realm.getClientByClientId("my-client")).thenReturn(client);
			when(roleAdmin.getName()).thenReturn("admin");
			when(user.getClientRoleMappingsStream(client)).thenReturn(Stream.of(roleAdmin));

			mapper.updateBrokeredUser(session, realm, user, mapperModel,
				contextWithClaim("roles", List.of()));

			verify(user).deleteRoleMapping(roleAdmin);
		}

		@Test
		void missingClientId_skipsSync() {
			mapperModel.setConfig(Map.of(
				CLAIM, "roles",
				CONFIG_MAPPER_TYPE, "CLIENT_ROLES"
				// no CLIENT_ID configured
			));

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "admin"));

			verify(user, never()).grantRole(any());
		}

		@Test
		void clientNotFound_skipsSync() {
			when(realm.getClientByClientId("my-client")).thenReturn(null);

			mapper.importNewUser(session, realm, user, mapperModel,
				contextWithClaim("roles", "admin"));

			verify(user, never()).grantRole(any());
		}
	}

	// -------------------------------------------------------------------------
	// Legacy sync mode mirrors updateBrokeredUser
	// -------------------------------------------------------------------------

	@Test
	void legacySyncMode_performsSameFullSync() {
		mapperModel.setConfig(Map.of(
			CLAIM, "groups",
			CONFIG_MAPPER_TYPE, "GROUPS"
		));
		// claim is empty → no assign step → session.groups() not called; only removal step runs
		when(groupAdmin.getParentId()).thenReturn(null);
		when(user.getGroupsStream()).thenReturn(Stream.of(groupAdmin));

		mapper.updateBrokeredUserLegacy(session, realm, user, mapperModel,
			contextWithClaim("groups", List.of()));

		verify(user).leaveGroup(groupAdmin);
		verify(session, never()).groups();
	}
}
