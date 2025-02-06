package dasniko.keycloak.authentication.redirect;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(AuthenticatorFactory.class)
public class UsernamePasswordWithRegistrationRedirectFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "auth-usr-pwd-form-w-redirect";
    public static final UsernamePasswordWithRegistrationRedirectForm SINGLETON = new UsernamePasswordWithRegistrationRedirectForm();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Username Password Form w/ redirect";
    }

    @Override
    public String getHelpText() {
        return "Validates a username and password from login form and redirects user to registration if username is not known.";
    }

}
