package dasniko.keycloak.user.flintstones.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.userprofile.UserProfileUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parsing, validation and caching of the attribute mapping definitions configured on the user storage provider.
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public final class AttributeMappings {

	public static final String CONFIG_KEY = "attributeMappings";

	public static final String CONFIG_EXAMPLE = """
		[
		  { "name": "picture", "field": "pictureUrl" },
		  { "name": "phone", "field": "phoneNumbers", "multivalued": true },
		  { "name": "loginCount", "field": "logins", "type": "integer", "readOnly": true }
		]""";

	private static final String NOTE_KEY = AttributeMappings.class.getName();
	private static final ObjectReader READER = JsonSerialization.mapper.readerFor(new TypeReference<List<AttributeMapping>>() {});

	private AttributeMappings() {
	}

	/**
	 * Returns the mappings configured on the given provider model. The parsed result is cached on the model, keyed by the raw
	 * configuration string, so a configuration update invalidates it.
	 */
	public static List<AttributeMapping> get(ComponentModel model) {
		String raw = model.get(CONFIG_KEY, "");
		Cached cached = model.getNote(NOTE_KEY);
		if (cached != null && cached.raw().equals(raw)) {
			return cached.mappings();
		}

		List<AttributeMapping> mappings = parse(raw);
		model.setNote(NOTE_KEY, new Cached(raw, mappings));
		return mappings;
	}

	/**
	 * @throws IllegalArgumentException if the configuration is not a well-formed list of mapping definitions
	 */
	public static List<AttributeMapping> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		try {
			List<AttributeMapping> mappings = READER.readValue(raw);
			return mappings == null ? List.of() : List.copyOf(mappings);
		} catch (IOException e) {
			throw new IllegalArgumentException(rootCauseMessage(e), e);
		}
	}

	/**
	 * @throws ComponentValidationException if the configuration cannot be parsed or is semantically invalid
	 */
	public static void validate(String raw) throws ComponentValidationException {
		List<AttributeMapping> mappings;
		try {
			mappings = parse(raw);
		} catch (IllegalArgumentException e) {
			throw new ComponentValidationException("Invalid attribute mappings: " + e.getMessage());
		}

		Set<String> names = new HashSet<>();
		Set<String> writableFields = new HashSet<>();
		List<String> errors = new ArrayList<>();

		for (AttributeMapping mapping : mappings) {
			String label = mapping.name() == null || mapping.name().isBlank() ? "<unnamed>" : mapping.name();

			if (mapping.name() == null || mapping.name().isBlank()) {
				errors.add("a mapping is missing the 'name' of the Keycloak attribute");
			} else if (UserProfileUtil.isRootAttribute(mapping.name())) {
				errors.add("'%s' is a root attribute and is always mapped by the provider itself".formatted(label));
			} else if (!names.add(mapping.name())) {
				errors.add("'%s' is mapped more than once".formatted(label));
			}

			if (mapping.field() == null || mapping.field().isBlank()) {
				errors.add("mapping '%s' is missing the 'field' of the external user record".formatted(label));
			} else if (!mapping.readOnly() && !writableFields.add(mapping.field())) {
				errors.add("field '%s' is written by more than one mapping".formatted(mapping.field()));
			}

			if (mapping.delimiter() != null && !mapping.multivalued()) {
				errors.add("mapping '%s' declares a 'delimiter' but is not 'multivalued'".formatted(label));
			}
		}

		if (!errors.isEmpty()) {
			throw new ComponentValidationException("Invalid attribute mappings: " + String.join("; ", errors));
		}
	}

	/**
	 * Returns the most specific message we can offer: the first cause that is not a Jackson wrapper — typically the validation
	 * message from {@link ValueType#from(String)} — or Jackson's own message if the whole chain is Jackson's.
	 */
	private static String rootCauseMessage(IOException e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (!(cause instanceof JsonProcessingException)) {
				return cause.getMessage();
			}
		}
		return e instanceof JsonProcessingException jpe ? jpe.getOriginalMessage() : e.getMessage();
	}

	private record Cached(String raw, List<AttributeMapping> mappings) {
	}
}
