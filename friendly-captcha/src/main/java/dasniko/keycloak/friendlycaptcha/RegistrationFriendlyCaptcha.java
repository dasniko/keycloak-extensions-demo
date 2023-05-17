package dasniko.keycloak.friendlycaptcha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.theme.Theme;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoService(FormActionFactory.class)
public class RegistrationFriendlyCaptcha implements FormAction, FormActionFactory, ConfiguredProvider {

	public static final String PROVIDER_ID = "friendly-captcha-action";

	private static final String FRIENDLY_CAPTCHA_SOLUTION = "frc-captcha-solution";

	private static final String FRIENDLY_CAPTCHA_NOT_CONFIGURED = "friendlyCaptchaNotConfigured";
	private static final String FRIENDLY_CAPTCHA_FAILED = "friendlyCaptchaFailed";

	private static final String SITE_KEY = "fc.site.key";
	private static final String API_KEY = "fc.api.key";
	private static final String USE_EU_API = "fc.use.eu";
	private static final String JS_LIB_PATH = "fc.js.lib.path";

	private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
		AuthenticationExecutionModel.Requirement.REQUIRED,
		AuthenticationExecutionModel.Requirement.DISABLED
	};

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = List.of(
		new ProviderConfigProperty(SITE_KEY, "Friendly Captcha Site Key", "Friendly Captcha Site Key", ProviderConfigProperty.STRING_TYPE, ""),
		new ProviderConfigProperty(API_KEY, "Friendly Captcha API Key", "Friendly Captcha API Key", ProviderConfigProperty.STRING_TYPE, ""),
		new ProviderConfigProperty(USE_EU_API, "Dedicated EU-Endpoint", "Use dedicated EU-Endpoints only (needs to be enabled in your plan!).",
			ProviderConfigProperty.BOOLEAN_TYPE, false),
		new ProviderConfigProperty(JS_LIB_PATH, "Path to Widget-Library", "Path to local Friendly Challenge Widget library in used theme under resources-folder.",
			ProviderConfigProperty.STRING_TYPE, "/js/friendly-challenge-widget.min.js")
	);

	@Override
	public String getDisplayType() {
		return "Friendly Captcha";
	}

	@Override
	public String getReferenceCategory() {
		return "captcha";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public void buildPage(FormContext context, LoginFormsProvider form) {
		AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
		String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser()).toLanguageTag();
		if (captchaConfig == null || captchaConfig.getConfig() == null
			|| captchaConfig.getConfig().get(SITE_KEY) == null
			|| captchaConfig.getConfig().get(API_KEY) == null
		) {
			form.addError(new FormMessage(null, FRIENDLY_CAPTCHA_NOT_CONFIGURED));
			return;
		}
		String siteKey = captchaConfig.getConfig().get(SITE_KEY);
		form.setAttribute("friendlyCaptchaRequired", true);
		form.setAttribute("friendlyCaptchaSiteKey", siteKey);
		form.setAttribute("friendlyCaptchaLanguage", userLanguageTag);
		form.setAttribute("friendlyCaptchaDataPuzzleEndpoint",
			String.format("https://%s/api/v1/puzzle", getFriendlyCaptchaHost(captchaConfig)));
		form.addScript(getResourcePath(context) + captchaConfig.getConfig().get(JS_LIB_PATH));
	}

	@Override
	public void validate(ValidationContext context) {
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		List<FormMessage> errors = new ArrayList<>();
		boolean success = false;
		context.getEvent().detail(Details.REGISTER_METHOD, "form");

		String solution = formData.getFirst(FRIENDLY_CAPTCHA_SOLUTION);
		if (!Validation.isBlank(solution)) {
			success = validateCaptcha(context, solution);
		}
		if (success) {
			context.success();
		} else {
			errors.add(new FormMessage(null, FRIENDLY_CAPTCHA_FAILED));
			formData.remove(FRIENDLY_CAPTCHA_SOLUTION);
			context.error(Errors.INVALID_REGISTRATION);
			context.validationError(formData, errors);
			context.excludeOtherErrors();
		}
	}

	private boolean validateCaptcha(ValidationContext context, String solution) {
		boolean success = false;

		AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
		String secret = captchaConfig.getConfig().get(API_KEY);
		String sitekey = captchaConfig.getConfig().get(SITE_KEY);
		String apiUrl = String.format("https://%s/api/v1/siteverify", getFriendlyCaptchaHost(captchaConfig));

		CloseableHttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
		HttpPost post = new HttpPost(apiUrl);
		List<NameValuePair> formparams = new LinkedList<>();
		formparams.add(new BasicNameValuePair("solution", solution));
		formparams.add(new BasicNameValuePair("secret", secret));
		formparams.add(new BasicNameValuePair("sitekey", sitekey));
		log.trace("Sending friendly captcha verify request to {}: {}", apiUrl, formparams);
		try {
			UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
			post.setEntity(form);
			try (CloseableHttpResponse response = httpClient.execute(post)) {
				int status = response.getStatusLine().getStatusCode();
				InputStream content = response.getEntity().getContent();
				try {
					TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
					Map<String, Object> json = JsonSerialization.readValue(content, typeRef);
					log.trace("Received friendly captcha verify response {}: {}", status, json);
					//noinspection unchecked
					List<String> errors = (List<String>) json.getOrDefault("errors", List.of());
					if (status == HttpStatus.SC_OK) {
						Object val = json.get("success");
						success = Boolean.TRUE.equals(val);
					} else {
						// according to the "verification best practices" from the friendly captcha docs,
						// we'll accept all non-200 responses as valid, but will log the cause
						log.warn("Unexpected response status for friendly captcha verify request: {}, errors: {}", status, errors);
						success = true;
					}
				} finally {
					EntityUtils.consumeQuietly(response.getEntity());
				}
			}
		} catch (IOException e) {
			log.error("Friendly Captcha failed", e);
		}
		return success;
	}

	private String getFriendlyCaptchaHost(AuthenticatorConfigModel captchaConfig) {
		boolean useEUApi = Boolean.parseBoolean(captchaConfig.getConfig().get(USE_EU_API));
		return useEUApi ? "eu-api.friendlycaptcha.eu" : "api.friendlycaptcha.com";
	}

	@SneakyThrows
	private String getResourcePath(FormContext context) {
		Theme theme = context.getSession().theme().getTheme(Theme.Type.LOGIN);
		UrlBean urlBean = new UrlBean(context.getRealm(), theme, new URI(""), null);
		return urlBean.getResourcesPath();
	}

	@Override
	public void success(FormContext context) {
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}


	@Override
	public void close() {
	}

	@Override
	public FormAction create(KeycloakSession session) {
		return this;
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		return "Adds Friendly Captcha button. Captchas verify that the entity that is registering is a human. This can only be used on the internet and must be configured after you add it.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return CONFIG_PROPERTIES;
	}
}
