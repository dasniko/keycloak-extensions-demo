package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.mappers.AttributeMapping;
import dasniko.keycloak.user.flintstones.mappers.AttributeMappings;
import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.userprofile.AttributeGroupMetadata;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileDecorator;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.UserProfileUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FlintstonesUserStorageProvider implements UserStorageProvider,
	UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator,
	UserRegistrationProvider, UserProfileDecorator {

	/**
	 * Common prefix of Keycloak's own query keys ({@link UserModel#SEARCH}, {@link UserModel#IDP_ALIAS}, ...). These keys carry no
	 * meaning for the external API and must not be forwarded to it; matching the prefix also covers keys added in later versions.
	 */
	private static final String INTERNAL_QUERY_KEY_PREFIX = "keycloak.session.realm.users.query.";

	/**
	 * The internal query keys with a counterpart in the external API, translated rather than dropped.
	 */
	private static final Map<String, String> TRANSLATED_QUERY_KEYS = Map.of(UserModel.SEARCH, "search", UserModel.EXACT, "exactMatch");

	private final KeycloakSession session;
	private final ComponentModel model;
	private final FlintstonesApiClient apiClient;

	// user handling in this transaction
	UserModelTransaction tx = new UserModelTransaction(this::updateUser);

	public FlintstonesUserStorageProvider(KeycloakSession session, ComponentModel model, FlintstonesApiClient apiClient) {
		this.session = session;
		this.model = model;
		this.apiClient = apiClient;
		session.getTransactionManager().enlistAfterCompletion(tx);
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		return model.getConfig()
			.getOrDefault(FlintstonesUserStorageProviderFactory.SUPPORTED_CREDENTIAL_TYPES, List.of(PasswordCredentialModel.TYPE))
			.contains(credentialType);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		return supportsCredentialType(credentialType);
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		String credentialType = input.getType();
		if (!supportsCredentialType(credentialType) || !(input instanceof UserCredentialModel cred)) {
			return false;
		}

		TracingProvider tracing = session.getProvider(TracingProvider.class);
		tracing.startSpan(FlintstonesUserStorageProvider.class, "isValid");

		Credential credential = new Credential(credentialType, cred.getChallengeResponse());
		boolean isValid = apiClient.verifyCredentials(StorageId.externalId(user.getId()), credential);
		tracing.endSpan();
		return isValid;
	}

	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		String credentialType = input.getType();
		if (!supportsCredentialType(credentialType) || !(input instanceof UserCredentialModel cred)) {
			return false;
		}

		if (!isWritable()) {
			log.warn("Edit mode is read-only. Skipping credential update for user.");
			return false;
		}

		TracingProvider tracing = session.getProvider(TracingProvider.class);
		tracing.startSpan(FlintstonesUserStorageProvider.class, "updateCredential");

		if (PasswordCredentialModel.TYPE.equals(credentialType) && usePasswordPolicy()) {
			PolicyError policyError = session.getProvider(PasswordPolicyManagerProvider.class)
				.validate(realm, user, cred.getChallengeResponse());
			if (policyError != null) {
				ModelException exception = new ModelException(policyError.getMessage(), policyError.getParameters());
				tracing.error(exception);
				tracing.endSpan();
				throw exception;
			}
		}

		Credential credential = new Credential(credentialType, cred.getChallengeResponse());
		boolean success = apiClient.updateCredentials(StorageId.externalId(user.getId()), credential);
		tracing.endSpan();
		return success;
	}

	@Override
	public Stream<CredentialModel> getCredentials(RealmModel realm, UserModel user) {
		return apiClient.getCredentials(StorageId.externalId(user.getId()))
			.stream()
			.filter(c -> supportsCredentialType(c.getType()))
			.map(c -> {
				CredentialModel cm = new CredentialModel();
				cm.setType(c.getType());
				cm.setCreatedDate(0L);
				cm.setFederationLink(user.getFederationLink());
				cm.setCredentialData(c.getValue());
				cm.setSecretData(c.getValue());
				return cm;
			});
	}

	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
	}

	@Override
	public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
		return Stream.empty();
	}

	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		TracingProvider tracing = session.getProvider(TracingProvider.class);
		tracing.startSpan(FlintstonesUserStorageProvider.class, "getUserById");

		UserModel adapter = tx.findUser(id);
		if (adapter == null) {
			String externalId = StorageId.externalId(id);
			adapter = findUser(realm, externalId, apiClient::getUserById);
		} else {
			log.debug("Found user data for {} in loadedUsers.", id);
		}

		tracing.endSpan();

		return adapter;
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		UserModel user = tx.findUser(username);
		if (user == null) {
			user = findUser(realm, username, apiClient::getUserByUsername);
		} else {
			log.debug("Found user data for {} in loadedUsers.", username);
		}
		return user;
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		UserModel user = tx.findUser(email);
		if (user == null) {
			user = findUser(realm, email, apiClient::getUserByEmail);
		} else {
			log.debug("Found user data for {} in loadedUsers.", email);
		}
		return user;
	}

	private UserModel findUser(RealmModel realm, String identifier, Function<String, FlintstoneUser> fnFindUser) {
		TracingProvider tracing = session.getProvider(TracingProvider.class);
		tracing.startSpan(FlintstonesUserStorageProvider.class, "findUser");

		UserModel adapter = null;
		FlintstoneUser user = fnFindUser.apply(identifier);
		log.debug("Received user data for identifier <{}> from repository: {}", identifier, user);
		if (user != null) {
			adapter = new FlintstoneUserAdapter(session, realm, model, user);
			tx.addUser(adapter);
		}

		tracing.endSpan();

		return adapter;
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		return getUsersCount(realm, Map.of());
	}

	@Override
	public int getUsersCount(RealmModel realm, Map<String, String> params) {
		if (isBrokerLinkQuery(params)) {
			return findByBrokerLink(realm, params).isPresent() ? 1 : 0;
		}
		return apiClient.usersCount(toExternalQuery(params));
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		if (isBrokerLinkQuery(params)) {
			return findByBrokerLink(realm, params).stream();
		}

		TracingProvider tracing = session.getProvider(TracingProvider.class);
		tracing.startSpan(FlintstonesUserStorageProvider.class, "searchForUserStream");

		List<FlintstoneUser> result = apiClient.searchUsers(toExternalQuery(params), firstResult, maxResults);
		Stream<UserModel> stream = result.stream().map(user -> new FlintstoneUserAdapter(session, realm, model, user));
		tracing.endSpan();
		return stream;
	}

	@Override
	public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
		return apiClient.searchGroupMembers(group.getName(), firstResult, maxResults)
			.stream().map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
		return apiClient.searchRoleMembers(role.getName(), firstResult, maxResults)
			.stream().map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
		// only reached by X.509 client cert auth (mapped to custom attributes) and the OID4VC DID uniqueness validator
		return searchForUserStream(realm, Map.of(attrName, attrValue), 0, Constants.DEFAULT_MAX_RESULTS + 1);
	}

	/**
	 * Broker links of flintstone users live in Keycloak's federated storage, not in API, so API can't answer a query by IdP link;
	 * forwarding it without the link criteria would return an unfiltered user list, e.g. on every broker backchannel logout.
	 */
	private static boolean isBrokerLinkQuery(Map<String, String> params) {
		return params.containsKey(UserModel.IDP_ALIAS) || params.containsKey(UserModel.IDP_USER_ID);
	}

	private Optional<UserModel> findByBrokerLink(RealmModel realm, Map<String, String> params) {
		String alias = params.get(UserModel.IDP_ALIAS);
		String idpUserId = params.get(UserModel.IDP_USER_ID);
		if (alias == null || idpUserId == null) {
			// federated storage can only look up one exact link, not list all users of an IdP
			return Optional.empty();
		}
		UserModel user = session.users().getUserByFederatedIdentity(realm, new FederatedIdentityModel(alias, idpUserId, null));
		// local users are already returned by Keycloak's local storage; only answer for users of this provider
		return Optional.ofNullable(user).filter(u -> model.getId().equals(StorageId.providerId(u.getId())));
	}

	/**
	 * Converts Keycloak's query parameters into the ones the external API expects.
	 * <p>
	 * Derives a new map instead of editing the argument: {@code UserStorageManager} hands the same instance to every provider of
	 * the realm and to the count pass, and some callers pass an immutable {@code Map.of(...)}.
	 */
	private static Map<String, String> toExternalQuery(Map<String, String> params) {
		Map<String, String> query = new HashMap<>(params);
		query.keySet().removeIf(key -> key.startsWith(INTERNAL_QUERY_KEY_PREFIX));
		TRANSLATED_QUERY_KEYS.forEach((internal, external) -> {
			String value = params.get(internal);
			if (value != null) {
				query.put(external, value);
			}
		});
		return query;
	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		if (isWritable() && syncUsers()) {
			FlintstoneUser flintstoneUser = new FlintstoneUser();
			flintstoneUser.setUsername(username);
			flintstoneUser = apiClient.createUser(flintstoneUser);
			if (flintstoneUser == null) {
				return null;
			}
			FlintstoneUserAdapter newUser = new FlintstoneUserAdapter(session, realm, model, flintstoneUser);
			tx.addUser(newUser);
			return newUser;
		} else {
			log.debug("Edit mode is read-only or syncUsers is disabled. Skipping creation for user {}.", username);
		}
		return null;
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		if (isWritable()) {
			String externalId = StorageId.externalId(user.getId());
			return apiClient.deleteUser(externalId);
		}
		log.warn("Edit mode is read-only. Skipping removal for user {}.", user.getId());
		return false;
	}

	/**
	 * Declares the mapped attributes on the user profile. Without this, they would be unmanaged attributes and therefore invisible
	 * unless the realm sets an {@code unmanagedAttributePolicy}.
	 */
	@Override
	public List<AttributeMetadata> decorateUserProfile(String providerId, UserProfileMetadata metadata) {
		int guiOrder = (int) metadata.getAttributes().stream()
			.map(AttributeMetadata::getName)
			.distinct()
			.count();

		List<AttributeMapping> mappings = AttributeMappings.get(model);
		Map<String, AttributeGroupMetadata> groups = mappings.stream().anyMatch(mapping -> mapping.group() != null)
			? declaredAttributeGroups()
			: Map.of();

		List<AttributeMetadata> metadatas = new ArrayList<>();
		for (AttributeMapping mapping : mappings) {
			AttributeMetadata attributeMetadata =
				UserProfileUtil.createAttributeMetadata(mapping.name(), metadata, guiOrder++, model.getName());
			if (attributeMetadata != null) {
				attributeMetadata.setMultivalued(mapping.multivalued());
				if (mapping.readOnly() || !isWritable()) {
					attributeMetadata.addWriteCondition(AttributeMetadata.ALWAYS_FALSE);
				}
				applyGroup(attributeMetadata, mapping, groups);
				metadatas.add(attributeMetadata);
			}
		}
		return metadatas;
	}

	/**
	 * An attribute referring to a group the user profile does not declare ends up in no group bucket at all and is therefore not
	 * rendered. The mapping is validated on save, but the group can be removed from the realm afterwards, so fall back to showing
	 * the attribute without a group rather than letting it disappear.
	 */
	private void applyGroup(AttributeMetadata attributeMetadata, AttributeMapping mapping, Map<String, AttributeGroupMetadata> groups) {
		if (mapping.group() == null) {
			return;
		}
		AttributeGroupMetadata group = groups.get(mapping.group());
		if (group != null) {
			attributeMetadata.setAttributeGroupMetadata(group);
		} else {
			log.warn("User profile of realm {} declares no attribute group '{}', showing attribute '{}' without a group.",
				session.getContext().getRealm().getName(), mapping.group(), mapping.name());
		}
	}

	private Map<String, AttributeGroupMetadata> declaredAttributeGroups() {
		return session.getProvider(UserProfileProvider.class).getConfiguration().getGroups().stream()
			.collect(Collectors.toMap(UPGroup::getName, group -> new AttributeGroupMetadata(
				group.getName(), group.getDisplayHeader(), group.getDisplayDescription(), group.getAnnotations()),
				(first, second) -> first));
	}

	@Override
	public void close() {
	}

	private boolean isWritable() {
		return model.get(FlintstonesUserStorageProviderFactory.EDIT_MODE, EditMode.READ_ONLY.name()).equals(EditMode.WRITABLE.name());
	}

	@SuppressWarnings("unused")
	private boolean importUsers() {
		return model.get(FlintstonesUserStorageProviderFactory.USER_IMPORT, false);
	}

	private boolean syncUsers() {
		return model.get(FlintstonesUserStorageProviderFactory.USER_CREATION_ENABLED, false);
	}

	private boolean usePasswordPolicy() {
		return model.get(FlintstonesUserStorageProviderFactory.USE_PASSWORD_POLICY, false);
	}

	private void updateUser(UserModel user) {
		FlintstoneUserAdapter userAdapter = (FlintstoneUserAdapter) user;
		if (userAdapter.isDirty()) {
			if (isWritable()) {
				if (!apiClient.updateUser(userAdapter.getUser())) {
					throw new RuntimeException("Failed to update user " + userAdapter.getUser().getUsername());
				}
			} else {
				log.warn("Edit mode is read-only. Skipping update for user {}.", userAdapter.getUser().getId());
			}
		}
	}

}
