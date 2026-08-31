package dasniko.keycloak.user.flintstones.mappers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A single attribute mapping between a Keycloak user attribute and a field of the external user record.
 *
 * @param name        the attribute name on the Keycloak side
 * @param field       the field name on the external (API) side
 * @param type        the JSON type of the external value, {@link ValueType#STRING} if omitted
 * @param multivalued whether the attribute can hold more than one value
 * @param readOnly    whether the attribute may be written back to the external source
 * @param delimiter   if set, a multivalued attribute is stored externally as a single string joined by this delimiter
 *                    instead of as a JSON array
 * @param property    if set, the external value is a complex object and only this member of it is mapped. Writing replaces the
 *                    object with one built from this member alone — the other members are not preserved
 * @param group       if set, the name of the user profile attribute group the attribute is shown in. The group must be declared
 *                    in the realm's user profile configuration
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public record AttributeMapping(
	String name,
	String field,
	ValueType type,
	boolean multivalued,
	boolean readOnly,
	String delimiter,
	String property,
	String group
) {

	public AttributeMapping {
		type = type == null ? ValueType.STRING : type;
		delimiter = delimiter == null || delimiter.isEmpty() ? null : delimiter;
		property = property == null || property.isBlank() ? null : property;
		group = group == null || group.isBlank() ? null : group;
	}

	/**
	 * Deserializes one mapping definition. The type is taken as a plain string and resolved by {@link ValueType#from(String)}, so an
	 * unknown type name yields our own message instead of Jackson's.
	 */
	@JsonCreator
	public static AttributeMapping create(
		@JsonProperty("name") String name,
		@JsonProperty("field") String field,
		@JsonProperty("type") String type,
		@JsonProperty("multivalued") boolean multivalued,
		@JsonProperty("readOnly") boolean readOnly,
		@JsonProperty("delimiter") String delimiter,
		@JsonProperty("property") String property,
		@JsonProperty("group") String group
	) {
		return new AttributeMapping(name, field, ValueType.from(type), multivalued, readOnly, delimiter, property, group);
	}

	/**
	 * Converts a value received from the external API into Keycloak attribute values.
	 *
	 * @return the attribute values, never {@code null}, empty if the external value was {@code null}
	 * @throws IllegalArgumentException if a value does not match the configured {@link #type()}
	 */
	public List<String> toAttributeValues(Object externalValue) {
		if (externalValue == null) {
			return List.of();
		}

		List<Object> raw = new ArrayList<>();
		if (externalValue instanceof Collection<?> collection) {
			raw.addAll(collection);
		} else if (multivalued && delimiter != null) {
			for (String part : String.valueOf(externalValue).split(delimiter, -1)) {
				raw.add(part);
			}
		} else {
			raw.add(externalValue);
		}

		List<String> values = new ArrayList<>(raw.size());
		for (Object value : raw) {
			Object projected = project(value);
			if (projected != null) {
				values.add(type.toAttributeValue(projected));
			}
			if (!multivalued && !values.isEmpty()) {
				// a single-valued mapping keeps the first value only, so a sloppy source cannot smuggle in extra values
				break;
			}
		}
		return List.copyOf(values);
	}

	/**
	 * Converts Keycloak attribute values into the representation the external API expects.
	 *
	 * @return the external value, {@code null} if there is no value to write
	 * @throws IllegalArgumentException if a value does not match the configured {@link #type()}
	 */
	public Object toExternalValue(List<String> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}

		if (!multivalued) {
			return wrap(type.toExternalValue(values.getFirst()));
		}

		if (delimiter != null) {
			values.forEach(type::toExternalValue);
			return String.join(delimiter, values);
		}

		return values.stream().map(type::toExternalValue).map(this::wrap).toList();
	}

	/**
	 * Whether writing {@code values} would actually change anything.
	 * <p>
	 * The comparison happens in Keycloak attribute space, not against the external value: a projected mapping rebuilds the object
	 * from a single member, so the old and the new object always differ and the record would be truncated on every write.
	 *
	 * @param currentExternalValue the value currently held by the external record, may be {@code null}
	 */
	public boolean changes(Object currentExternalValue, List<String> values) {
		List<String> current;
		try {
			current = toAttributeValues(currentExternalValue);
		} catch (IllegalArgumentException e) {
			// we cannot make sense of what is there, so let the write through
			return true;
		}
		return !current.equals(values == null ? List.of() : values);
	}

	/**
	 * Reads the mapped member out of a complex external value, or returns the value itself when no {@link #property()} is configured.
	 */
	private Object project(Object value) {
		if (property == null || value == null) {
			return value;
		}
		if (value instanceof Map<?, ?> map) {
			return map.get(property);
		}
		throw new IllegalArgumentException(
			"expected an object to read '%s' from, but got a %s".formatted(property, value.getClass().getSimpleName()));
	}

	/**
	 * Rebuilds the external object from the mapped member. Replace semantics: whatever else the object held is not carried over.
	 */
	private Object wrap(Object value) {
		return property == null ? value : Map.of(property, value);
	}
}
