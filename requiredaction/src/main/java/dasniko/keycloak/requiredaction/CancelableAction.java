package dasniko.keycloak.requiredaction;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.models.Constants.KC_ACTION_ENFORCED;
import static org.keycloak.models.Constants.KC_ACTION_EXECUTING;

interface CancelableAction extends RequiredActionProvider {

	default void initiatedActionCanceled(KeycloakSession session, AuthenticationSessionModel authSession) {
		removeActionCancelable(authSession);
	}

	/**
	 * Call this method in requiredActionChallenge() before doing actual challenge
	 */
	default void setActionCancelable(RequiredActionContext context) {
		if (isAuthFlowInitiated(context)) {
			context.getAuthenticationSession().setClientNote(KC_ACTION_EXECUTING, context.getConfig().getProviderId());
			context.getAuthenticationSession().setClientNote(KC_ACTION_ENFORCED, Boolean.FALSE.toString());
		}
	}

	default void removeActionCancelable(AuthenticationSessionModel authSession) {
		String providerId = authSession.getClientNote(KC_ACTION_EXECUTING);
		if (providerId != null) {
			authSession.removeRequiredAction(providerId);
			authSession.removeClientNote(KC_ACTION_EXECUTING);
			authSession.removeClientNote(KC_ACTION_ENFORCED);
		}
	}

	/**
	 * Returns {@code true} if this required action was triggered by the auth flow (step-up), rather than being
	 * a pending action already on the user's account. Concretely: the action is present in the current
	 * authentication session but absent from the user's persistent required-action list.
	 */
	private boolean isAuthFlowInitiated(RequiredActionContext context) {
		return context.getAuthenticationSession().getRequiredActions().contains(context.getConfig().getProviderId()) &&
			context.getUser().getRequiredActionsStream().noneMatch(a -> a.equals(context.getConfig().getProviderId()));
	}

}
