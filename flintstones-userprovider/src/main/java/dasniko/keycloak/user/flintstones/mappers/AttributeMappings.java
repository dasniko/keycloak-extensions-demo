package dasniko.keycloak.user.flintstones.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.UserProfileUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
		  { "name": "loginCount", "field": "logins", "type": "integer", "readOnly": true },
		  { "name": "addressJson", "field": "address", "type": "json", "readOnly": true },
		  { "name": "city", "field": "address", "property": "city", "group": "user-metadata" }
		]""";

	private static final String NOTE_KEY = AttributeMappings.class.getName();
	private static final ObjectReader READER = JsonSerialization.mapper.readerFor(new TypeReference<List<AttributeMapping>>() {});

	private AttributeMappings() {
	}

	/**
	 * Returns the mappings configured on the given provider model.
	 * <p>
	 * The parsed result is cached on the model, keyed by the raw configuration string, so a configuration update invalidates it.
	 * Note that the cache does not outlive the {@link org.keycloak.models.KeycloakSession}: {@code ComponentModel.notes} is
	 * transient and is not carried over by the copy constructor, and the storage manager wraps the realm-cached model in a fresh
	 * {@code UserStorageProviderModel} per lookup. It therefore saves the repeated parses within one request, not across requests.
	 */
	public static List<AttributeMapping> get(ComponentModel model) {
		return cached(model).mappings();
	}

	/**
	 * The same mappings as {@link #get(ComponentModel)}, indexed by the Keycloak attribute name. Names are unique, enforced by
	 * {@link #validate(String)}.
	 */
	public static Map<String, AttributeMapping> byName(ComponentModel model) {
		return cached(model).byName();
	}

	private static Cached cached(ComponentModel model) {
		String raw = model.get(CONFIG_KEY, "");
		Cached cached = model.getNote(NOTE_KEY);
		if (cached != null && cached.raw().equals(raw)) {
			return cached;
		}

		cached = new Cached(raw, parse(raw));
		model.setNote(NOTE_KEY, cached);
		return cached;
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
	 * Validates the configuration, including the checks that need the realm's user profile configuration.
	 *
	 * @throws ComponentValidationException if the configuration cannot be parsed or is semantically invalid
	 */
	public static void validate(KeycloakSession session, RealmModel realm, String raw) throws ComponentValidationException {
		validateGroups(session, realm, validate(raw));
	}

	/**
	 * Structural validation, independent of the realm.
	 *
	 * @return the parsed mappings, so a caller that validates further does not have to parse again
	 * @throws ComponentValidationException if the configuration cannot be parsed or is semantically invalid
	 */
	public static List<AttributeMapping> validate(String raw) throws ComponentValidationException {
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
			String name = mapping.name();
			boolean unnamed = name == null || name.isBlank();
			String label = unnamed ? "<unnamed>" : name;

			if (unnamed) {
				errors.add("a mapping is missing the 'name' of the Keycloak attribute");
			} else if (UserProfileUtil.isRootAttribute(name)) {
				errors.add("'%s' is a root attribute of the user profile and cannot be mapped".formatted(label));
			} else if (!names.add(name)) {
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

			if (mapping.delimiter() != null && mapping.type() == ValueType.JSON) {
				errors.add("mapping '%s' cannot combine 'delimiter' with type 'json'".formatted(label));
			}

			if (mapping.delimiter() != null && mapping.property() != null) {
				errors.add("mapping '%s' cannot combine 'delimiter' with 'property', a delimited string holds no objects".formatted(label));
			}
		}

		if (!errors.isEmpty()) {
			throw new ComponentValidationException("Invalid attribute mappings: " + String.join("; ", errors));
		}
		return mappings;
	}

	/**
	 * An attribute pointing at a group the user profile does not declare is not rendered at all, so a typo here would make the
	 * attribute silently disappear. Reject it while the admin is still looking at the form.
	 */
	private static void validateGroups(KeycloakSession session, RealmModel realm, List<AttributeMapping> mappings)
			throws ComponentValidationException {
		Set<String> wanted = mappings.stream()
			.map(AttributeMapping::group)
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(TreeSet::new));
		if (wanted.isEmpty()) {
			return;
		}

		RealmModel contextRealm = session.getContext().getRealm();
		if (contextRealm == null || !contextRealm.getId().equals(realm.getId())) {
			// the user profile provider resolves its configuration from the realm in the session context; if that is not the
			// realm being configured we cannot check, and decorateUserProfile() falls back to ungrouped anyway
			return;
		}

		UPConfig config = session.getProvider(UserProfileProvider.class).getConfiguration();
		Set<String> declared = config.getGroups().stream().map(UPGroup::getName).collect(Collectors.toSet());
		wanted.removeAll(declared);

		if (!wanted.isEmpty()) {
			throw new ComponentValidationException(
				"Unknown user profile attribute group(s): %s. Declare them under Realm settings -> User profile first."
					.formatted(String.join(", ", wanted)));
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
		// the loop returns on the first non-Jackson cause, starting at e itself, so getting here means e is Jackson's own
		return ((JsonProcessingException) e).getOriginalMessage();
	}

	private record Cached(String raw, List<AttributeMapping> mappings, Map<String, AttributeMapping> byName) {

		Cached(String raw, List<AttributeMapping> mappings) {
			this(raw, mappings, mappings.stream()
				.filter(mapping -> mapping.name() != null)
				.collect(Collectors.toUnmodifiableMap(AttributeMapping::name, mapping -> mapping, (first, second) -> first)));
		}
	}
}
