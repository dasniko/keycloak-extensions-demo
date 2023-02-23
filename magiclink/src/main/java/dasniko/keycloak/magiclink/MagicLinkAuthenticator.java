package dasniko.keycloak.magiclink;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class MagicLinkAuthenticator implements Authenticator {

	private static final String SESSION_KEY = "magic-email-key";
	private static final String QUERY_PARAM = "magickey";
	private static final String MAGIC_LINK_TEMPLATE = "magic-link.ftl";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		String sessionKey = context.getAuthenticationSession().getAuthNote(SESSION_KEY);
		if (sessionKey != null) {
			String requestKey = context.getHttpRequest().getUri().getQueryParameters().getFirst(QUERY_PARAM);
			if (requestKey != null) {
				if (requestKey.equals(sessionKey)) {
					context.success();
				} else {
					context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
				}
			} else {
				displayMagicLinkSuccessPage(context);
			}
		} else {
			sendMagicLink(context);
		}
	}

	@Override
	public void action(AuthenticationFlowContext context) {
	}

	private void sendMagicLink(AuthenticationFlowContext context) {
		RealmModel realm = context.getRealm();
		UserModel user = context.getUser();
		if (user == null) {
			// if user is null, we don't want to allow for username guessing
			// so, we just say it's all ok and stop here
			displayMagicLinkSuccessPage(context);
			return;
		}

		String key = KeycloakModelUtils.generateId();
		context.getAuthenticationSession().setAuthNote(SESSION_KEY, key);

		EmailTemplateProvider emailTemplateProvider = context.getSession().getProvider(EmailTemplateProvider.class);
		emailTemplateProvider.setRealm(realm);
		emailTemplateProvider.setUser(user);

		String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam(QUERY_PARAM, key).build().toString();
		// for further processing we need a mutable map here
		Map<String, Object> msgParams = new HashMap<>();
		msgParams.put("name", user.getUsername());
		msgParams.put("link", link);

		try {
			emailTemplateProvider.send("magicLinkEmailSubject", MAGIC_LINK_TEMPLATE, msgParams);
			displayMagicLinkSuccessPage(context);
		} catch (EmailException e) {
			log.error("Exception during sending email occurred.", e);
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
		}
	}

	private void displayMagicLinkSuccessPage(AuthenticationFlowContext context) {
		context.challenge(context.form().setInfo("magicLinkText").createInfoPage());
	}

	@Override
	public boolean requiresUser() {
		return true;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}

}
