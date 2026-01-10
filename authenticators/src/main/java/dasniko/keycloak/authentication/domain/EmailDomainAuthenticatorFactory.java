package dasniko.keycloak.authentication.domain;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

@AutoService(AuthenticatorFactory.class)
public class EmailDomainAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "email-domain-regex-authenticator";
    private static final EmailDomainAuthenticator SINGLETON = new EmailDomainAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Email Domain Regex Authenticator";
    }

    @Override
    public String getHelpText() {
        return "Authenticates users only if their email matches the configured regex.";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> props = new ArrayList<>();
        ProviderConfigProperty regex = new ProviderConfigProperty();
        regex.setName("allowedDomainRegex");
        regex.setLabel("Allowed Email Regex");
        regex.setType(ProviderConfigProperty.STRING_TYPE);
        regex.setHelpText("Regular expression for allowed email domains, e.g. ^.*@(firma\\.de|partner\\.com)$");
        props.add(regex);

        ProviderConfigProperty errorMessage = new ProviderConfigProperty();
        errorMessage.setName("errorMessage");
        errorMessage.setLabel("Fehlermeldung");
        errorMessage.setType(ProviderConfigProperty.STRING_TYPE);
        errorMessage.setHelpText("Fehlermeldung für den User, wenn Email nicht erlaubt ist");
        props.add(errorMessage);

        return props;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public String getReferenceCategory() {
        return "Email Domain Restriction";
    }
}
