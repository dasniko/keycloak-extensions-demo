package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.Credential;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class FlintstonesUserStorageProvider implements UserStorageProvider,
	UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator,
	UserRegistrationProvider {

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
		return PasswordCredentialModel.TYPE.equals(credentialType);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		return supportsCredentialType(credentialType);
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel cred)) {
			return false;
		}
		Credential credential = new Credential("password", cred.getChallengeResponse());
		return apiClient.verifyCredentials(StorageId.externalId(user.getId()), credential);
	}

	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel cred)) {
			return false;
		}

		if (usePasswordPolicy()) {
			PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
			if (passwordPolicy != null) {
				for (String policy : passwordPolicy.getPolicies()) {
					PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, policy);
					if (provider != null) {
						PolicyError policyError = provider.validate(user.getUsername(), cred.getChallengeResponse());
						if (policyError != null) {
							throw new ModelException(policyError.getMessage(), policyError.getParameters());
						}
					}
				}
			}
		}

		Credential credential = new Credential("password", cred.getChallengeResponse());
		return apiClient.updateCredentials(StorageId.externalId(user.getId()), credential);
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
		UserModel adapter = tx.findUser(id);
		if (adapter == null) {
			String externalId = StorageId.externalId(id);
			FlintstoneUser user = apiClient.getUserById(externalId);
			log.debug("Received user data for externalId <{}> from repository: {}", externalId, user);
			if (user != null) {
				adapter = new FlintstoneUserAdapter(session, realm, model, user);
				tx.addUser(id, adapter);
			}
		} else {
			log.debug("Found user data for {} in loadedUsers.", id);
		}
		return adapter;
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		return findUser(realm, username, apiClient::getUserByUsername);
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		return findUser(realm, email, apiClient::getUserByEmail);
	}

	private UserModel findUser(RealmModel realm, String identifier, Function<String, FlintstoneUser> fnFindUser) {
		UserModel adapter = null;
		FlintstoneUser user = fnFindUser.apply(identifier);
		log.debug("Received user data for identifier <{}> from repository: {}", identifier, user);
		if (user != null) {
			adapter = new FlintstoneUserAdapter(session, realm, model, user);
			tx.addUser(adapter.getId(), adapter);
		}
		return adapter;
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		return apiClient.usersCount();
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		List<FlintstoneUser> result;
		if (params.containsKey(UserModel.USERNAME)) {
			result = List.of(apiClient.getUserByUsername(params.get(UserModel.USERNAME)));
		} else {
			result = apiClient.searchUsers(params.getOrDefault(UserModel.SEARCH, null), firstResult, maxResults);
		}
		return result.stream().map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
		return apiClient.searchGroupMembers(group.getName(), firstResult, maxResults)
			.stream().map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
		return Stream.empty();
	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		if (syncUsers()) {
			FlintstoneUser flintstoneUser = new FlintstoneUser();
			flintstoneUser.setUsername(username);
			apiClient.createUser(flintstoneUser);
			flintstoneUser = apiClient.getUserByUsername(username);
			FlintstoneUserAdapter newUser = new FlintstoneUserAdapter(session, realm, model, flintstoneUser);
			tx.addUser(username, newUser);
			return newUser;
		}
		return null;
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		String externalId = StorageId.externalId(user.getId());
		return apiClient.deleteUser(externalId);
	}

	@Override
	public void close() {
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
			apiClient.updateUser(userAdapter.getUser());
		}
	}

}
