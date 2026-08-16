package dasniko.keycloak.authentication.devicecookie;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.SecureContextResolver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_CODE_LENGTH;
import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_CODE_LENGTH_DEFAULT;
import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_CODE_TTL;
import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_CODE_TTL_DEFAULT;
import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_COOKIE_MAX_AGE;
import static dasniko.keycloak.authentication.devicecookie.DeviceCookieAuthenticatorFactory.CONFIG_PROPERTY_COOKIE_MAX_AGE_DEFAULT;
import static de.keycloak.util.AuthenticatorUtil.getConfig;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class DeviceCookieAuthenticator implements Authenticator {

	private static final String COOKIE_NAME = "KC_DEVICE";
	private static final String USER_ATTRIBUTE_TRUSTED_DEVICES = "trusted_devices";
	private static final int DEVICE_ID_LENGTH = 20;
	private static final int DEVICE_SECRET_LENGTH = 32;
	private static final String TPL_DEVICE_CONFIRM = "device-confirm.ftl";
	private static final String EMAIL_TEMPLATE = "device-confirm-email.ftl";
	private static final String QUERY_PARAM_CODE = "code";
	private static final String AUTH_NOTE_CODE = "device-confirm-code";
	private static final String AUTH_NOTE_EXPIRATION = "device-confirm-expiration";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		Cookie deviceCookie = context.getHttpRequest().getHttpHeaders().getCookies().get(COOKIE_NAME);

		if (deviceCookie != null && isTrustedDevice(context.getUser(), deviceCookie.getValue())) {
			context.success();
			return;
		}

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		if (authSession.getAuthNote(AUTH_NOTE_CODE) == null) {
			sendConfirmationCode(context);
			return;
		}

		// code already sent: either the user clicked the link in the email (code is in the query string)
		// or this is just a (re)load of the confirmation form, in which case we don't resend anything
		String linkCode = context.getHttpRequest().getUri().getQueryParameters().getFirst(QUERY_PARAM_CODE);
		if (linkCode != null) {
			verifyCode(context, linkCode);
		} else {
			context.challenge(context.form().createForm(TPL_DEVICE_CONFIRM));
		}
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst(QUERY_PARAM_CODE);
		verifyCode(context, enteredCode);
	}

	private void sendConfirmationCode(AuthenticationFlowContext context) {
		RealmModel realm = context.getRealm();
		UserModel user = context.getUser();

		int codeLength = getConfig(context, CONFIG_PROPERTY_CODE_LENGTH, CONFIG_PROPERTY_CODE_LENGTH_DEFAULT);
		int codeTtl = getConfig(context, CONFIG_PROPERTY_CODE_TTL, CONFIG_PROPERTY_CODE_TTL_DEFAULT);

		String code = SecretGenerator.getInstance().randomString(codeLength, SecretGenerator.DIGITS);
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.setAuthNote(AUTH_NOTE_CODE, code);
		authSession.setAuthNote(AUTH_NOTE_EXPIRATION, Long.toString(Time.currentTimeSeconds() + codeTtl));

		String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam(QUERY_PARAM_CODE, code).build().toString();

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("name", user.getUsername());
		attributes.put("code", code);
		attributes.put("link", link);
		attributes.put("ttlMinutes", Math.floorDiv(codeTtl, 60));

		EmailTemplateProvider emailTemplateProvider = context.getSession().getProvider(EmailTemplateProvider.class);
		emailTemplateProvider.setRealm(realm);
		emailTemplateProvider.setUser(user);

		try {
			emailTemplateProvider.send("deviceConfirmEmailSubject", EMAIL_TEMPLATE, attributes);
			context.challenge(context.form().createForm(TPL_DEVICE_CONFIRM));
		} catch (EmailException e) {
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
		}
	}

	private void verifyCode(AuthenticationFlowContext context, String submittedCode) {
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		String code = authSession.getAuthNote(AUTH_NOTE_CODE);
		String expiration = authSession.getAuthNote(AUTH_NOTE_EXPIRATION);

		if (code == null || expiration == null) {
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
			return;
		}

		if (submittedCode == null || !MessageDigest.isEqual(
			code.getBytes(StandardCharsets.UTF_8), submittedCode.getBytes(StandardCharsets.UTF_8))) {
			AuthenticationExecutionModel execution = context.getExecution();
			if (execution.isRequired()) {
				context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
					context.form().setError("deviceConfirmCodeInvalid").createForm(TPL_DEVICE_CONFIRM));
			} else if (execution.isConditional() || execution.isAlternative()) {
				context.attempted();
			}
			return;
		}

		// the code matched: it must not be usable again, regardless of the outcome below
		authSession.removeAuthNote(AUTH_NOTE_CODE);
		authSession.removeAuthNote(AUTH_NOTE_EXPIRATION);

		if (Long.parseLong(expiration) < Time.currentTimeSeconds()) {
			context.failure(AuthenticationFlowError.EXPIRED_CODE,
				context.form().setError("deviceConfirmCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
			return;
		}

		confirmDevice(context);
	}

	private void confirmDevice(AuthenticationFlowContext context) {
		UserModel user = context.getUser();
		long now = Time.currentTimeSeconds();

		int maxAge = getConfig(context, CONFIG_PROPERTY_COOKIE_MAX_AGE, CONFIG_PROPERTY_COOKIE_MAX_AGE_DEFAULT);
		long expiresAt = now + maxAge;

		String deviceId = SecretGenerator.getInstance().randomString(DEVICE_ID_LENGTH, SecretGenerator.ALPHANUM);
		String secret = SecretGenerator.getInstance().randomString(DEVICE_SECRET_LENGTH, SecretGenerator.ALPHANUM);

		// prune expired devices while we're writing the attribute anyway, so the list doesn't grow forever
		List<String> trustedDevices = user.getAttributeStream(USER_ATTRIBUTE_TRUSTED_DEVICES)
			.filter(entry -> !isExpired(entry, now))
			.collect(Collectors.toCollection(ArrayList::new));
		trustedDevices.add(deviceId + ":" + hash(secret) + ":" + expiresAt);
		user.setAttribute(USER_ATTRIBUTE_TRUSTED_DEVICES, trustedDevices);

		setDeviceCookie(context, deviceId + "." + secret, maxAge);
		context.success();
	}

	private boolean isTrustedDevice(UserModel user, String cookieValue) {
		int separatorIndex = cookieValue.indexOf('.');
		if (separatorIndex < 0) {
			return false;
		}

		String deviceId = cookieValue.substring(0, separatorIndex);
		byte[] expectedHash = hash(cookieValue.substring(separatorIndex + 1)).getBytes(StandardCharsets.UTF_8);
		long now = Time.currentTimeSeconds();

		return user.getAttributeStream(USER_ATTRIBUTE_TRUSTED_DEVICES)
			.map(entry -> entry.split(":", 3))
			.filter(parts -> parts.length == 3 && parts[0].equals(deviceId) && Long.parseLong(parts[2]) >= now)
			.anyMatch(parts -> MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), expectedHash));
	}

	private boolean isExpired(String entry, long now) {
		String[] parts = entry.split(":", 3);
		return parts.length != 3 || Long.parseLong(parts[2]) < now;
	}

	private void setDeviceCookie(AuthenticationFlowContext context, String value, int maxAge) {
		NewCookie deviceCookie = new NewCookie.Builder(COOKIE_NAME)
			.version(1)
			.value(value)
			.path(AuthenticationManager.getRealmCookiePath(context.getRealm(), context.getUriInfo()))
			.maxAge(maxAge)
			.secure(SecureContextResolver.isSecureContext(context.getSession()))
			.httpOnly(true)
			.sameSite(NewCookie.SameSite.LAX)
			.build();

		context.getSession().getContext().getHttpResponse().setCookieIfAbsent(deviceCookie);
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
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
