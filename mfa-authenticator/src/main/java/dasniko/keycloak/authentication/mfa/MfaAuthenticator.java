package dasniko.keycloak.authentication.mfa;

import dasniko.keycloak.requiredaction.MobileNumberRequiredAction;
import dasniko.keycloak.sms.SmsProvider;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Slf4j
public class MfaAuthenticator implements Authenticator {

	private static final String TPL_CODE = "login-mfa.ftl";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		KeycloakSession session = context.getSession();
		UserModel user = context.getUser();

		int length = getInt(config, MfaConstants.CONFIG_PROPERTY_LENGTH, MfaConstants.CONFIG_PROPERTY_LENGTH_DEFAULT);
		int ttl = getInt(config, MfaConstants.CONFIG_PROPERTY_TTL, MfaConstants.CONFIG_PROPERTY_TTL_DEFAULT);

		String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.setAuthNote(MfaConstants.AUTH_NOTE_CODE, code);
		authSession.setAuthNote(MfaConstants.AUTH_NOTE_EXPIRATION, Integer.toString(Time.currentTime() + ttl));

		try {
			Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
			Locale locale = session.getContext().resolveLocale(user);
			String mfaAuthText = theme.getMessages(locale).getProperty("mfaAuthText");
			String mfaText = String.format(mfaAuthText, code, Math.floorDiv(ttl, 60));

			String providerId = get(config, MfaConstants.CONFIG_PROPERTY_PROVIDER, MfaConstants.CONFIG_PROPERTY_PROVIDER_DEFAULT);
			SmsProvider smsProvider = session.getProvider(SmsProvider.class, providerId);
			smsProvider.sendMessage(getMobileNumber(user), mfaText);

			context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
		} catch (IOException e) {
			context.failure(AuthenticationFlowError.INTERNAL_ERROR,
				context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		String code = authSession.getAuthNote(MfaConstants.AUTH_NOTE_CODE);
		String expiration = authSession.getAuthNote(MfaConstants.AUTH_NOTE_EXPIRATION);

		if (code == null || expiration == null) {
			context.failure(AuthenticationFlowError.INTERNAL_ERROR,
				context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
			return;
		}

		boolean isValid = enteredCode.equals(code);
		if (isValid) {
			if (Integer.parseInt(expiration) < Time.currentTime()) {
				// expired
				context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
					context.form().setError("mfaAuthCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
			} else {
				// valid
				context.success();
			}
		} else {
			// invalid
			AuthenticationExecutionModel execution = context.getExecution();
			if (execution.isRequired()) {
				context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
					context.form().setAttribute("realm", context.getRealm())
						.setError("mfaAuthCodeInvalid").createForm(TPL_CODE));
			} else if (execution.isConditional() || execution.isAlternative()) {
				context.attempted();
			}
		}
	}

	@Override
	public boolean requiresUser() {
		return true;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return getMobileNumber(user) != null;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
		// here I'm referencing the mobile number required action in the "requiredaction" module
		session.getContext().getAuthenticationSession().addRequiredAction(MobileNumberRequiredAction.PROVIDER_ID);
	}

	@Override
	public void close() {
	}

	private String getMobileNumber(UserModel user) {
		return user.getFirstAttribute("mobile_number");
	}

	private String get(AuthenticatorConfigModel configModel, String key, String defaultValue) {
		if (configModel != null) {
			Map<String, String> config = configModel.getConfig();
			if (config != null) {
				return config.getOrDefault(key, defaultValue);
			}
			return defaultValue;
		}
		return defaultValue;
	}

	private int getInt(AuthenticatorConfigModel configModel, String key, String defaultValue) {
		return Integer.parseInt(get(configModel, key, defaultValue));
	}

}
