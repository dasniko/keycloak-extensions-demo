package dasniko.keycloak.requiredaction;

import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.function.Consumer;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MobileNumberRequiredAction implements RequiredActionProvider {

	public static final String PROVIDER_ID = "mobile-number-ra";

	private static final String MOBILE_NUMBER_FIELD = "mobile_number";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		if (context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD) == null) {
			context.getUser().addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// show initial form
		context.challenge(createForm(context, null));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form

		UserModel user = context.getUser();

		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String mobileNumber = formData.getFirst(MOBILE_NUMBER_FIELD);

		if (Validation.isBlank(mobileNumber) || mobileNumber.length() < 5) {
			context.challenge(createForm(context, form -> form.addError(new FormMessage(MOBILE_NUMBER_FIELD, "Invalid input"))));
			return;
		}

		user.setSingleAttribute(MOBILE_NUMBER_FIELD, mobileNumber);
		user.removeRequiredAction(PROVIDER_ID);

		context.success();
	}

	@Override
	public void close() {
	}

	private Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formConsumer) {
		LoginFormsProvider form = context.form();
		form.setAttribute("username", context.getUser().getUsername());

		String mobileNumber = context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD);
		form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);

		if (formConsumer != null) {
			formConsumer.accept(form);
		}

		return form.createForm("update-mobile-number.ftl");
	}

}
