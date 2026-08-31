package dasniko.keycloak.user.flintstones.mappers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Conversion between the external JSON representation and Keycloak's {@code List<String>} attribute values.
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class AttributeMappingTest {

	private static AttributeMapping mapping(ValueType type, boolean multivalued, String delimiter) {
		return new AttributeMapping("attr", "field", type, multivalued, false, delimiter);
	}

	@Test
	public void readsSingleValue() {
		assertThat(mapping(ValueType.STRING, false, null).toAttributeValues("https://example.com/x.png"),
			contains("https://example.com/x.png"));
	}

	@Test
	public void readsNullAsNoValues() {
		assertThat(mapping(ValueType.STRING, false, null).toAttributeValues(null), is(empty()));
	}

	@Test
	public void readsJsonArrayIntoMultipleValues() {
		assertThat(mapping(ValueType.STRING, true, null).toAttributeValues(List.of("a", "b")), contains("a", "b"));
	}

	@Test
	public void readsDelimitedStringIntoMultipleValues() {
		assertThat(mapping(ValueType.STRING, true, ",").toAttributeValues("a,b,c"), contains("a", "b", "c"));
	}

	@Test
	public void singleValuedMappingKeepsOnlyTheFirstValueOfAnArray() {
		assertThat(mapping(ValueType.STRING, false, null).toAttributeValues(List.of("a", "b")), contains("a"));
	}

	@Test
	public void readsTypedValuesAsStrings() {
		assertThat(mapping(ValueType.INTEGER, false, null).toAttributeValues(42), contains("42"));
		assertThat(mapping(ValueType.BOOLEAN, false, null).toAttributeValues(true), contains("true"));
		assertThat(mapping(ValueType.LONG, false, null).toAttributeValues(1234567890123L), contains("1234567890123"));
	}

	@Test
	public void rejectsAValueThatDoesNotMatchTheDeclaredType() {
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.INTEGER, false, null).toAttributeValues("nope"));
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.BOOLEAN, false, null).toAttributeValues("yes"));
	}

	@Test
	public void writesSingleValue() {
		assertThat(mapping(ValueType.STRING, false, null).toExternalValue(List.of("x")), is("x"));
	}

	@Test
	public void writesTypedValues() {
		assertThat(mapping(ValueType.INTEGER, false, null).toExternalValue(List.of("42")), is(42));
		assertThat(mapping(ValueType.LONG, false, null).toExternalValue(List.of("42")), is(42L));
		assertThat(mapping(ValueType.BOOLEAN, false, null).toExternalValue(List.of("true")), is(true));
	}

	@Test
	public void writesMultipleValuesAsJsonArray() {
		assertThat(mapping(ValueType.STRING, true, null).toExternalValue(List.of("a", "b")), is(List.of("a", "b")));
	}

	@Test
	public void writesMultipleValuesAsDelimitedString() {
		assertThat(mapping(ValueType.STRING, true, ",").toExternalValue(List.of("a", "b")), is("a,b"));
	}

	@Test
	public void writesNoValuesAsNull() {
		assertThat(mapping(ValueType.STRING, false, null).toExternalValue(List.of()), is(nullValue()));
		assertThat(mapping(ValueType.STRING, false, null).toExternalValue(null), is(nullValue()));
	}

	@Test
	public void rejectsWritingAValueThatDoesNotMatchTheDeclaredType() {
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.INTEGER, false, null).toExternalValue(List.of("nope")));
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.LONG, true, ",").toExternalValue(List.of("1", "x")));
	}
}
