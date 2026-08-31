package dasniko.keycloak.user.flintstones.mappers;

import java.util.Arrays;
import java.util.Locale;

/**
 * The JSON type an attribute has in the payload of the external user API.
 * <p>
 * Keycloak stores every user attribute as {@code List<String>}, so the type is what tells us how to turn a string back into the
 * representation the API expects, and how to validate what the API sent us.
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public enum ValueType {

	STRING {
		@Override
		Object toExternalValue(String value) {
			return value;
		}
	},

	INTEGER {
		@Override
		Object toExternalValue(String value) {
			return Integer.valueOf(value);
		}
	},

	LONG {
		@Override
		Object toExternalValue(String value) {
			return Long.valueOf(value);
		}
	},

	BOOLEAN {
		@Override
		Object toExternalValue(String value) {
			if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("'%s' is not a boolean".formatted(value));
			}
			return Boolean.valueOf(value);
		}
	};

	/**
	 * Converts a Keycloak attribute value into the representation the external API expects.
	 *
	 * @throws IllegalArgumentException if the value cannot be represented as this type
	 */
	abstract Object toExternalValue(String value);

	/**
	 * Converts a value received from the external API into its Keycloak attribute representation.
	 *
	 * @throws IllegalArgumentException if the value is not a valid instance of this type
	 */
	String toAttributeValue(Object value) {
		String string = String.valueOf(value);
		// round-trip through the external representation, so a mistyped source value is rejected instead of silently stringified
		toExternalValue(string);
		return string;
	}

	/**
	 * Resolves a type name from the mapping configuration, case-insensitively, defaulting to {@link #STRING}.
	 *
	 * @throws IllegalArgumentException if the name is not a known type
	 */
	public static ValueType from(String value) {
		if (value == null || value.isBlank()) {
			return STRING;
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("unknown type '%s', expected one of %s".formatted(value, Arrays.toString(values())), e);
		}
	}
}
