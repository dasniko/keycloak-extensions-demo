package dasniko.keycloak.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.Theme;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class JsonEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

	public JsonEmailTemplateProvider(KeycloakSession session) {
		super(session);
	}

	@Override
	protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
		try {
			Locale locale = session.getContext().resolveLocale(user, Boolean.parseBoolean(String.valueOf(attributes.get(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER))));
			Theme theme = getTheme();
			Properties messages = theme.getEnhancedMessages(realm, locale);

			attributes.put("locale", locale);
			attributes.put("templateName", template.replace(".ftl", ""));
			attributes.put("realm", Map.of(
				"name", realm.getName(),
				"displayName", realm.getDisplayName() != null ? realm.getDisplayName() : ""
			));

			attributes.put("userId", user.getId());
			attributes.put("user", new ProfileBean(user, session));

			String subject = new MessageFormat(messages.getProperty(subjectKey, subjectKey), locale).format(subjectAttributes.toArray());
			attributes.put("subject", subject);
			attributes.put("subjectKey", subjectKey);
			attributes.put("subjectAttributes", subjectAttributes);

			String jsonString = JsonSerialization.writeValueAsString(attributes);
			return new EmailTemplate(subject, jsonString, jsonString);
		} catch (IOException e) {
			throw new EmailException("Failed to create JSON output for email", e);
		}
	}

}
