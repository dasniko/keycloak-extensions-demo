package dasniko.keycloak.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
			attributes.put("subjectAttributes", subjectAttributes);
			attributes.put("templateName", template.replace(".ftl", ""));
			attributes.put("realm", realm.getName());
			String jsonString = JsonSerialization.writeValueAsString(attributes);
			return new EmailTemplate(subjectKey, jsonString, null);
		} catch (IOException e) {
			throw new EmailException("Failed to create JSON output for email", e);
		}
	}

}
