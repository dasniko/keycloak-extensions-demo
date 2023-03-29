package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import dasniko.keycloak.user.flintstones.repo.FlintstonesRepository;
import lombok.RequiredArgsConstructor;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Slf4j
@RequiredArgsConstructor
public class FlintstonesUserStorageProvider implements UserStorageProvider,
	UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator,
	UserRegistrationProvider {

	private final KeycloakSession session;
	private final ComponentModel model;
	private final FlintstonesRepository repository;

	// map of loaded users in this transaction
	protected Map<String, UserModel> loadedUsers = new HashMap<>();

	private final List<FlintstoneUserAdapter> newUsers = new ArrayList<>();

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
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}
		UserCredentialModel cred = (UserCredentialModel) input;
		return repository.validateCredentials(user.getUsername(), cred.getChallengeResponse());
	}

	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}
		UserCredentialModel cred = (UserCredentialModel) input;

		if (model.get(FlintstonesUserStorageProviderFactory.USE_PASSWORD_POLICY, false)) {
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

		return repository.updateCredentials(user.getUsername(), cred.getChallengeResponse());
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
		String externalId = StorageId.externalId(id);
		return findUser(realm, externalId, repository::findUserByUsernameOrEmail);
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		return findUser(realm, username, repository::findUserByUsernameOrEmail);
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		return getUserByUsername(realm, email);
	}

	private UserModel findUser(RealmModel realm, String identifier, Function<String, FlintstoneUser> fnFindUser) {
		UserModel adapter = loadedUsers.get(identifier);
		if (adapter == null) {
			FlintstoneUser user = fnFindUser.apply(identifier);
			log.debug("Received user data for identifier <{}> from repository: {}", identifier, user);
			if (user != null) {
				adapter = new FlintstoneUserAdapter(session, realm, model, user);
				loadedUsers.put(identifier, adapter);
			}
		} else {
			log.debug("Found user data for {} in loadedUsers.", identifier);
		}
		return adapter;
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		return repository.getUsersCount();
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
		return repository.findUsers(search).stream()
			.map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
		return searchForUserStream(realm, search);
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		return repository.getAllUsers().stream()
			.map(user -> new FlintstoneUserAdapter(session, realm, model, user));
	}

	@Override
	public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
		return Stream.empty();
	}

	@Override
	public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
		return Stream.empty();
	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		if (model.get(FlintstonesUserStorageProviderFactory.USER_CREATION_ENABLED, false)) {
			FlintstoneUser flintstoneUser = new FlintstoneUser();
			flintstoneUser.setUsername(username);
			FlintstoneUserAdapter newUser = new FlintstoneUserAdapter(session, realm, model, flintstoneUser);
			newUsers.add(newUser);
			loadedUsers.put(username, newUser);
			return newUser;
		}
		return null;
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		String externalId = StorageId.externalId(user.getId());
		return repository.removeUser(externalId);
	}

	@Override
	public void close() {
		for (FlintstoneUserAdapter newUser : newUsers) {
			repository.addUser(newUser.getUser());
		}
		if (newUsers.size() > 0) {
			newUsers.subList(0, newUsers.size()).clear();
		}
	}
}
