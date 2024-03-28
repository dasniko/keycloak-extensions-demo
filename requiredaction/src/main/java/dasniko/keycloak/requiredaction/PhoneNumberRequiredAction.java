package dasniko.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;

import java.util.function.Consumer;

import static org.keycloak.representations.IDToken.PHONE_NUMBER;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(RequiredActionFactory.class)
public class PhoneNumberRequiredAction implements
	RequiredActionFactory, RequiredActionProvider {

	public static final String PROVIDER_ID = "phone-number";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// you would implement something like the following, if this required action should be "self registering" at the user
		// if (context.getUser().getFirstAttribute(PHONE_NUMBER_FIELD) == null) {
		// 	context.getUser().addRequiredAction(PROVIDER_ID);
		//  context.getAuthenticationSession().addRequiredAction(PROVIDER_ID);
		// }
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// show initial form
		context.challenge(createForm(context));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String phoneNumber = formData.getFirst(PHONE_NUMBER);

		if (Validation.isBlank(phoneNumber) || phoneNumber.length() < 5) {
			context.challenge(createForm(context, form -> form.addError(new FormMessage(PHONE_NUMBER, "phoneNumberInvalid"))));
			return;
		}

		UserModel user = context.getUser();
		user.setSingleAttribute(PHONE_NUMBER, phoneNumber);
		user.removeRequiredAction(PROVIDER_ID);
		context.getAuthenticationSession().removeRequiredAction(PROVIDER_ID);
		context.success();
	}

	@Override
	public RequiredActionProvider create(KeycloakSession keycloakSession) {
		return this;
	}

	@Override
	public String getDisplayText() {
		return "Update phone number";
	}

	@Override
	public void init(Config.Scope scope) {
	}

	@Override
	public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	private Response createForm(RequiredActionContext context) {
		return createForm(context, null);
	}

	private Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formConsumer) {
		LoginFormsProvider form = context.form()
			.setAttribute("username", context.getUser().getUsername())
			.setAttribute(PHONE_NUMBER, context.getUser().getFirstAttribute(PHONE_NUMBER));

		if (formConsumer != null) {
			formConsumer.accept(form);
		}

		return form.createForm("update-phone-number.ftl");
	}

}
