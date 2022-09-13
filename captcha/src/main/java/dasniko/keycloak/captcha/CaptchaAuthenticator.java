package dasniko.keycloak.captcha;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class CaptchaAuthenticator implements Authenticator {

	private static final String RESULT_FIELD = "captcha.result";
	private static final String ERROR_MESSAGE = "captcha.result.error";
	private static final String TEMPLATE = "captcha.ftl";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		Response response = prepareCaptcha(context, null);
		context.challenge(response);
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		String mathResult = authSession.getAuthNote(RESULT_FIELD);
		int result = Integer.parseInt(mathResult);

		MultivaluedMap<String, String> formParameters = context.getHttpRequest().getDecodedFormParameters();
		String enteredResult = formParameters.getFirst("result");
		int enteredResultNumber = Integer.parseInt(enteredResult);

		if (result == enteredResultNumber) {
			context.success();
		} else {
			FormMessage errorMessage = new FormMessage(ERROR_MESSAGE);
			Response response = prepareCaptcha(context, errorMessage);
			context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, response);
		}
	}

	private static Response prepareCaptcha(AuthenticationFlowContext context, FormMessage errorMessage) {
		AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
		Map<String, String> config = authenticatorConfig.getConfig();
		int lowerBound = Integer.parseInt(config.get("lowerBound"));
		int upperBound = Integer.parseInt(config.get("upperBound"));
		int firstNumber = getRandomNumber(lowerBound, upperBound);
		int secondNumber = getRandomNumber(lowerBound, upperBound);
		int result = firstNumber + secondNumber;

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.setAuthNote(RESULT_FIELD, Integer.toString(result));

		LoginFormsProvider formsProvider = context.form()
			.setAttribute("firstOperand", firstNumber)
			.setAttribute("secondOperand", secondNumber);
		if (errorMessage != null) {
			formsProvider.addError(errorMessage);
		}
		return formsProvider.createForm(TEMPLATE);
	}

	private static int getRandomNumber(int lowerBound, int upperBound) {
		return (int) ((Math.random() * (upperBound - lowerBound)) + lowerBound);
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
