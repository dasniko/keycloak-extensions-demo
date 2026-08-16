package dasniko.keycloak.authentication.devicecookie;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(AuthenticatorFactory.class)
public class DeviceCookieAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "device-cookie";

	static final String CONFIG_PROPERTY_COOKIE_MAX_AGE = "cookieMaxAge";
	static final int CONFIG_PROPERTY_COOKIE_MAX_AGE_DEFAULT = 60 * 60 * 24 * 30; // 30 days
	static final String CONFIG_PROPERTY_CODE_LENGTH = "codeLength";
	static final int CONFIG_PROPERTY_CODE_LENGTH_DEFAULT = 6;
	static final String CONFIG_PROPERTY_CODE_TTL = "codeTtl";
	static final int CONFIG_PROPERTY_CODE_TTL_DEFAULT = 300; // 5 minutes

	private static final Authenticator SINGLETON = new DeviceCookieAuthenticator();

	private static final List<ProviderConfigProperty> configProperties = ProviderConfigurationBuilder.create()
		.property()
		.name(CONFIG_PROPERTY_COOKIE_MAX_AGE)
		.label("Cookie lifetime (seconds)")
		.helpText("How long a confirmed device is remembered, in seconds.")
		.type(ProviderConfigProperty.STRING_TYPE)
		.defaultValue(String.valueOf(CONFIG_PROPERTY_COOKIE_MAX_AGE_DEFAULT))
		.add()
		.property()
		.name(CONFIG_PROPERTY_CODE_LENGTH)
		.label("Confirmation code length")
		.helpText("The number of digits of the code sent to the user's email address.")
		.type(ProviderConfigProperty.STRING_TYPE)
		.defaultValue(String.valueOf(CONFIG_PROPERTY_CODE_LENGTH_DEFAULT))
		.add()
		.property()
		.name(CONFIG_PROPERTY_CODE_TTL)
		.label("Confirmation code time-to-live (seconds)")
		.helpText("How long the emailed confirmation code/link is valid, in seconds.")
		.type(ProviderConfigProperty.STRING_TYPE)
		.defaultValue(String.valueOf(CONFIG_PROPERTY_CODE_TTL_DEFAULT))
		.add()
		.build();

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayType() {
		return "Trusted Device Cookie";
	}

	@Override
	public String getHelpText() {
		return "Succeeds if a cookie identifying a previously confirmed device/browser is present and valid for the current user. "
			+ "Otherwise, a confirmation code is emailed to the user, who must enter it (or click the link in the email) to confirm "
			+ "the device, which is then remembered via a cookie for subsequent logins.";
	}

	@Override
	public String getReferenceCategory() {
		return "device";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

}
