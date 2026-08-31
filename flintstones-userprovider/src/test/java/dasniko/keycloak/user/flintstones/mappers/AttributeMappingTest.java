package dasniko.keycloak.user.flintstones.mappers;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		return new AttributeMapping("attr", "field", type, multivalued, false, delimiter, null);
	}

	private static AttributeMapping projection(ValueType type, boolean multivalued, String property) {
		return new AttributeMapping("attr", "field", type, multivalued, false, null, property);
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
	public void readsAComplexValueAsASerializedJsonString() {
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("street", "301 Cobblestone Way");
		address.put("city", "Bedrock");

		assertThat(mapping(ValueType.JSON, false, null).toAttributeValues(address),
			contains("{\"street\":\"301 Cobblestone Way\",\"city\":\"Bedrock\"}"));
	}

	@Test
	public void writesASerializedJsonStringBackAsAComplexValue() {
		Object written = mapping(ValueType.JSON, false, null).toExternalValue(List.of("{\"city\":\"Bedrock\"}"));
		assertThat(written, is(Map.of("city", "Bedrock")));
	}

	@Test
	public void rejectsWritingAMalformedJsonString() {
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.JSON, false, null).toExternalValue(List.of("{oops")));
	}

	@Test
	public void projectsASingleMemberOutOfAComplexValue() {
		Map<String, Object> address = Map.of("street", "301 Cobblestone Way", "city", "Bedrock");
		assertThat(projection(ValueType.STRING, false, "city").toAttributeValues(address), contains("Bedrock"));
	}

	@Test
	public void projectsAMemberOutOfEachElementOfACollection() {
		List<Map<String, Object>> groups = List.of(Map.of("id", 1, "name", "quarry"), Map.of("id", 2, "name", "lodge"));
		assertThat(projection(ValueType.STRING, true, "name").toAttributeValues(groups), contains("quarry", "lodge"));
	}

	@Test
	public void projectsATypedMember() {
		assertThat(projection(ValueType.INTEGER, false, "id").toAttributeValues(Map.of("id", 7)), contains("7"));
	}

	@Test
	public void projectingAnAbsentMemberYieldsNoValues() {
		assertThat(projection(ValueType.STRING, false, "city").toAttributeValues(Map.of("street", "x")), is(empty()));
	}

	@Test
	public void rejectsProjectingOutOfSomethingThatIsNotAnObject() {
		assertThrows(IllegalArgumentException.class,
			() -> projection(ValueType.STRING, false, "city").toAttributeValues("just a string"));
	}

	@Test
	public void writingAProjectedMemberReplacesTheObject() {
		assertThat(projection(ValueType.STRING, false, "city").toExternalValue(List.of("Rock Vegas")),
			is(Map.of("city", "Rock Vegas")));
	}

	@Test
	public void writingAMultivaluedProjectionYieldsOneMinimalObjectPerValue() {
		assertThat(projection(ValueType.STRING, true, "name").toExternalValue(List.of("quarry", "lodge")),
			is(List.of(Map.of("name", "quarry"), Map.of("name", "lodge"))));
	}

	@Test
	public void rewritingAProjectedMemberWithItsCurrentValueIsNotAChange() {
		Map<String, Object> address = Map.of("street", "301 Cobblestone Way", "city", "Bedrock");
		AttributeMapping mapping = projection(ValueType.STRING, false, "city");

		// comparing the rebuilt object against the stored one would report a change here and truncate the record
		assertThat(mapping.changes(address, List.of("Bedrock")), is(false));
		assertThat(mapping.changes(address, List.of("Rock Vegas")), is(true));
	}

	@Test
	public void clearingAValueIsAChangeOnlyWhenThereWasOne() {
		AttributeMapping mapping = mapping(ValueType.STRING, false, null);

		assertThat(mapping.changes("x", List.of()), is(true));
		assertThat(mapping.changes(null, List.of()), is(false));
		assertThat(mapping.changes(null, null), is(false));
	}

	@Test
	public void aValueTheSourceCannotDeliverCountsAsAChange() {
		assertThat(mapping(ValueType.INTEGER, false, null).changes("not a number", List.of("42")), is(true));
	}

	@Test
	public void typedValuesCompareByTheirAttributeRepresentation() {
		assertThat(mapping(ValueType.INTEGER, false, null).changes(1960, List.of("1960")), is(false));
		assertThat(mapping(ValueType.BOOLEAN, false, null).changes(true, List.of("true")), is(false));
		assertThat(mapping(ValueType.INTEGER, false, null).changes(1960, List.of("1959")), is(true));
	}

	@Test
	public void rejectsWritingAValueThatDoesNotMatchTheDeclaredType() {
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.INTEGER, false, null).toExternalValue(List.of("nope")));
		assertThrows(IllegalArgumentException.class, () -> mapping(ValueType.LONG, true, ",").toExternalValue(List.of("1", "x")));
	}
}
