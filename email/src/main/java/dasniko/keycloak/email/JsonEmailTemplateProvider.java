package dasniko.keycloak.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class JsonEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

	private final ObjectMapper mapper;

	public JsonEmailTemplateProvider(KeycloakSession session, ObjectMapper mapper) {
		super(session);
		this.mapper = mapper;
	}

	@Override
	protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
		try {
			attributes.put("subjectAttributes", subjectAttributes);
			attributes.put("templateName", template.replace(".ftl", ""));
			attributes.put("realm", realm.getName());
			String jsonString = mapper.writeValueAsString(attributes);
			return new EmailTemplate(subjectKey, jsonString, null);
		} catch (JsonProcessingException e) {
			throw new EmailException("Failed to create JSON output for email", e);
		}
	}

}
