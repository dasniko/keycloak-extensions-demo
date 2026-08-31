package dasniko.keycloak.user.flintstones.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.component.ComponentValidationException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class AttributeMappingsTest {

	@Test
	public void parsesMinimalDefinitionWithDefaults() {
		List<AttributeMapping> mappings = AttributeMappings.parse("""
			[{ "name": "picture", "field": "pictureUrl" }]""");

		assertThat(mappings, contains(new AttributeMapping("picture", "pictureUrl", ValueType.STRING, false, false, null)));
	}

	@Test
	public void parsesFullDefinition() {
		List<AttributeMapping> mappings = AttributeMappings.parse("""
			[{ "name": "phone", "field": "phoneNumbers", "type": "integer", "multivalued": true, "readOnly": true, "delimiter": "," }]""");

		AttributeMapping mapping = mappings.getFirst();
		assertThat(mapping.type(), is(ValueType.INTEGER));
		assertThat(mapping.multivalued(), is(true));
		assertThat(mapping.readOnly(), is(true));
		assertThat(mapping.delimiter(), is(","));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	public void blankConfigurationYieldsNoMappings(String raw) {
		assertThat(AttributeMappings.parse(raw), is(empty()));
	}

	@Test
	public void nullConfigurationYieldsNoMappings() {
		assertThat(AttributeMappings.parse(null), is(empty()));
	}

	@Test
	public void validConfigurationPasses() {
		assertDoesNotThrow(() -> AttributeMappings.validate(AttributeMappings.CONFIG_EXAMPLE));
	}

	@Test
	public void rejectsMalformedJson() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("not json"));
		assertThat(e.getMessage(), containsString("Invalid attribute mappings"));
	}

	@Test
	public void rejectsUnknownProperty() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "picture", "field": "pictureUrl", "tpye": "string" }]"""));
		assertThat(e.getMessage(), containsString("tpye"));
	}

	@Test
	public void rejectsUnknownType() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "picture", "field": "pictureUrl", "type": "date" }]"""));
		assertThat(e.getMessage(), containsString("unknown type 'date'"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"username", "email", "firstName", "lastName", "locale"})
	public void rejectsRootAttributes(String name) {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "%s", "field": "whatever" }]""".formatted(name)));
		assertThat(e.getMessage(), containsString("root attribute"));
	}

	@Test
	public void rejectsDuplicateAttributeNames() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "picture", "field": "pictureUrl" }, { "name": "picture", "field": "avatar" }]"""));
		assertThat(e.getMessage(), containsString("'picture' is mapped more than once"));
	}

	@Test
	public void rejectsTwoWritableMappingsOntoTheSameField() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "picture", "field": "pictureUrl" }, { "name": "avatar", "field": "pictureUrl" }]"""));
		assertThat(e.getMessage(), containsString("written by more than one mapping"));
	}

	@Test
	public void allowsASecondReadOnlyMappingOntoTheSameField() {
		assertDoesNotThrow(() -> AttributeMappings.validate("""
			[{ "name": "picture", "field": "pictureUrl" }, { "name": "avatar", "field": "pictureUrl", "readOnly": true }]"""));
	}

	@Test
	public void rejectsMissingName() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "field": "pictureUrl" }]"""));
		assertThat(e.getMessage(), containsString("missing the 'name'"));
	}

	@Test
	public void rejectsMissingField() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "picture" }]"""));
		assertThat(e.getMessage(), containsString("missing the 'field'"));
	}

	@Test
	public void rejectsDelimiterOnSingleValuedMapping() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "phone", "field": "phones", "delimiter": "," }]"""));
		assertThat(e.getMessage(), containsString("not 'multivalued'"));
	}

	@Test
	public void reportsAllProblemsAtOnce() {
		ComponentValidationException e = assertThrows(ComponentValidationException.class,
			() -> AttributeMappings.validate("""
				[{ "name": "email", "field": "mail" }, { "name": "picture" }]"""));
		assertThat(e.getMessage(), containsString("root attribute"));
		assertThat(e.getMessage(), containsString("missing the 'field'"));
	}

	@Test
	public void emptyArrayIsValid() {
		assertDoesNotThrow(() -> AttributeMappings.validate("[]"));
		assertThat(AttributeMappings.parse("[]"), is(empty()));
	}

	@Test
	public void nullDelimiterIsNormalisedFromEmptyString() {
		List<AttributeMapping> mappings = AttributeMappings.parse("""
			[{ "name": "phone", "field": "phones", "multivalued": true, "delimiter": "" }]""");
		assertThat(mappings.getFirst().delimiter(), is(nullValue()));
	}
}
