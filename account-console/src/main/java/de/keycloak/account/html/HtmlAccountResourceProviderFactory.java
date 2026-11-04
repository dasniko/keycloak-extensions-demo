package de.keycloak.account.html;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.Config.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resource.AccountResourceProviderFactory;
import org.keycloak.theme.Theme;

import java.io.IOException;

@AutoService(AccountResourceProviderFactory.class)
public class HtmlAccountResourceProviderFactory implements AccountResourceProviderFactory {

	public static final String PROVIDER_ID = "html-account";

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public AccountResourceProvider create(KeycloakSession session) {
		RealmModel realm = session.getContext().getRealm();
		ClientModel client = getAccountManagementClient(realm);
		Theme theme = getTheme(session);
		return new HtmlAccountResourceProvider(session, client, theme);
	}

	@Override
	public void init(Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	private Theme getTheme(KeycloakSession session) {
		try {
			return session.theme().getTheme(Theme.Type.ACCOUNT);
		} catch (IOException e) {
			throw new InternalServerErrorException(e);
		}
	}

	private ClientModel getAccountManagementClient(RealmModel realm) {
		ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
		if (client == null || !client.isEnabled()) {
			throw new NotFoundException("account management not enabled");
		}
		return client;
	}
}
