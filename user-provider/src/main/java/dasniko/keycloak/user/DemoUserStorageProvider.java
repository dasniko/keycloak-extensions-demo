package dasniko.keycloak.user;

import dasniko.keycloak.user.external.CredentialData;
import dasniko.keycloak.user.external.Peanut;
import dasniko.keycloak.user.external.PeanutsClient;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@JBossLog
public class DemoUserStorageProvider implements UserStorageProvider,
	UserLookupProvider.Streams, UserQueryProvider.Streams,
	CredentialInputUpdater, CredentialInputValidator,
	UserRegistrationProvider {

	private final KeycloakSession session;
	private final ComponentModel model;
	private final PeanutsClient client;

	protected Map<String, UserModel> loadedUsers = new HashMap<>();

	public DemoUserStorageProvider(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.model = model;

		CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
		ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
		ResteasyClient resteasyClient = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
		ResteasyWebTarget target = resteasyClient.target(model.get(Constants.BASE_URL));
		target.register(new BasicAuthentication(model.get(Constants.AUTH_USERNAME), model.get(Constants.AUTH_PASSWORD)));
		this.client = target.proxyBuilder(PeanutsClient.class).classloader(this.getClass().getClassLoader()).build();
	}

	@Override
	public void close() {
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
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}

		CredentialData credentialData;
		try {
			credentialData = client.getCredentialData(StorageId.externalId(user.getId()));
			log.debugf("Received credential data for userId %s: %s", user.getId(), credentialData);
			if (credentialData == null) {
				return false;
			}
		} catch (WebApplicationException e) {
			log.errorf(e, "Request to verify credentials for userId %s failed with response status %d",
				user.getId(), e.getResponse().getStatus());
			return false;
		}

		UserCredentialModel cred = (UserCredentialModel) input;

		PasswordCredentialModel passwordCredentialModel = credentialData.toPasswordCredentialModel();
		PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class, credentialData.getAlgorithm());
		boolean isValid = passwordHashProvider.verify(cred.getChallengeResponse(), passwordCredentialModel);
		log.debugf("Password validation result: %b", isValid);
		return isValid;
	}

	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		log.debugf("Try to update credentials type %s for user %s.", input.getType(), user.getId());
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
			return false;
		}

		UserCredentialModel cred = (UserCredentialModel) input;

		PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
		PasswordHashProvider passwordHashProvider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
		PasswordCredentialModel passwordCredentialModel =
			passwordHashProvider.encodedCredential(cred.getChallengeResponse(), passwordPolicy.getHashIterations());

		CredentialData credentialData = CredentialData.fromPasswordCredentialModel(passwordCredentialModel);

		log.debugf("Sending updateCredential request for userId %s", user.getId());
		log.tracef("Payload for updateCredential request: %s", credentialData);
		try {
			Response updateResponse = client.updateCredentialData(StorageId.externalId(user.getId()), credentialData);
			return updateResponse.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL);
		} catch (WebApplicationException e) {
			log.warnf("Credential data update for user %s failed with response %s", user.getId(), e.getResponse().getStatus());
			return false;
		}
	}

	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
	}

	@Override
	public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
		return Set.of();
	}

	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		log.debugf("getUserById: %s", id);
		return findUser(realm, StorageId.externalId(id));
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		log.debugf("getUserByUsername: %s", username);
		return findUser(realm, username);
	}

	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		log.debugf("getUserByEmail: %s", email);
		return findUser(realm, email);
	}

	private UserModel findUser(RealmModel realm, String identifier) {
		UserModel adapter = loadedUsers.get(identifier);
		if (adapter == null) {
			try {
				Peanut peanut = client.getPeanutById(identifier);
				adapter = new UserAdapter(session, realm, model, peanut);
				loadedUsers.put(identifier, adapter);
			} catch (WebApplicationException e) {
				log.warnf("User with identifier '%s' could not be found, response from server: %s", identifier, e.getResponse().getStatus());
			}
		} else {
			log.debugf("Found user data for %s in loadedUsers.", identifier);
		}
		return adapter;
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		return client.getPeanutsCount();
	}

	@Override
	public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
		log.debugf("getUsersStream, first=%d, max=%d", firstResult, maxResults);
		return toUserModelStream(client.getPeanuts(null, firstResult, maxResults), realm);
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
		log.debugf("searchForUserStream, search=%s, first=%d, max=%d", search, firstResult, maxResults);
		return toUserModelStream(client.getPeanuts(search, firstResult, maxResults), realm);
	}

	@Override
	public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
		log.debugf("searchForUserStream, params=%s, first=%d, max=%d", params, firstResult, maxResults);
		return toUserModelStream(client.getPeanuts(null, firstResult, maxResults), realm);
	}

	private Stream<UserModel> toUserModelStream(List<Peanut> peanuts, RealmModel realm) {
		log.debugf("Received %d users from provider", peanuts.size());
		return peanuts.stream().map(user -> new UserAdapter(session, realm, model, user));
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
		return null;
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		return false;
	}
}
