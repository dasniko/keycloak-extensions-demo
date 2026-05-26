package dasniko.keycloak.authentication.conditional;

import com.google.auto.service.AutoService;
import de.keycloak.util.AuthenticatorUtil;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@AutoService(AuthenticatorFactory.class)
public class ConditionalHeaderAuthenticator extends AbstractConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-custom-header";

	static final String CONF_HEADER_NAME = "header_name";
	static final String CONF_HEADER_EXPECTED_VALUE = "header_expected_value";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String headerName = AuthenticatorUtil.getConfig(context, CONF_HEADER_NAME, "");
		String headerValue = AuthenticatorUtil.getConfig(context, CONF_HEADER_EXPECTED_VALUE, "");

		HttpHeaders httpHeaders = context.getHttpRequest().getHttpHeaders();
		String customHeader = httpHeaders.getHeaderString(headerName);

		return isNegateOutput(context) != headerValue.equalsIgnoreCase(customHeader);
	}

	@Override
	public String getDisplayType() {
		return "Condition - Custom Header";
	}

	@Override
	public String getHelpText() {
		return "Flow is executed only if...";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty authNoteName = new ProviderConfigProperty();
		authNoteName.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteName.setName(CONF_HEADER_NAME);
		authNoteName.setLabel("Header name");
		authNoteName.setHelpText("Name of the header to check");
		authNoteName.setDefaultValue("X-Custom-Header");

		ProviderConfigProperty authNoteExpectedValue = new ProviderConfigProperty();
		authNoteExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
		authNoteExpectedValue.setName(CONF_HEADER_EXPECTED_VALUE);
		authNoteExpectedValue.setLabel("Expected header value");
		authNoteExpectedValue.setHelpText("Expected value in the header");
		authNoteExpectedValue.setDefaultValue("my-custom-value");

		return List.of(authNoteName, authNoteExpectedValue, negateOutputConfProperty);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

}
