package dasniko.keycloak.authentication.domain;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.regex.Pattern;

public class EmailDomainAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String email = user.getEmail();

        String errorMessage = context.getAuthenticatorConfig() != null ?
                context.getAuthenticatorConfig().getConfig().get("errorMessage")
                : "Email nicht erlaubt";

        String regex = context.getAuthenticatorConfig() != null ?
                context.getAuthenticatorConfig().getConfig().get("allowedDomainRegex") : null;

        if (email == null || regex == null || regex.trim().isEmpty()) {
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form().setError("Email or regex not configured").createErrorPage(Response.Status.BAD_REQUEST));
            return;
        }

        if (!Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(email).matches()) {
            LoginFormsProvider form = context.form();
            form.setError(errorMessage);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    form.createErrorPage(Response.Status.UNAUTHORIZED));
            return;
        }

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {}

    @Override
    public boolean requiresUser() { return true; }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public void close() {}
}
