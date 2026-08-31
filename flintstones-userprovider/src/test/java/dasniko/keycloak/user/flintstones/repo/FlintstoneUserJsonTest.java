package dasniko.keycloak.user.flintstones.repo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Pins the {@code @JsonAnyGetter}/{@code @JsonAnySetter} behaviour of {@link FlintstoneUser} — in particular that Lombok's
 * {@code @Data} does not additionally expose the backing map as a nested {@code attributes} property.
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstoneUserJsonTest {

	@Test
	public void unknownFieldsAreCapturedAsAttributes() throws IOException {
		String json = """
			{
			  "id": "12345",
			  "username": "fred",
			  "pictureUrl": "https://example.com/fred.png",
			  "phoneNumbers": ["+1 555 0100", "+1 555 0101"],
			  "logins": 42
			}""";

		FlintstoneUser user = JsonSerialization.readValue(json, FlintstoneUser.class);

		assertThat(user.getId(), is("12345"));
		assertThat(user.getUsername(), is("fred"));
		assertThat(user.getAttribute("pictureUrl"), is("https://example.com/fred.png"));
		assertThat(user.getAttribute("logins"), is(42));
		assertThat((List<?>) user.getAttribute("phoneNumbers"), contains("+1 555 0100", "+1 555 0101"));
	}

	@Test
	public void attributesAreSerializedFlatAndNotAsNestedProperty() throws IOException {
		FlintstoneUser user = new FlintstoneUser();
		user.setId("12345");
		user.setUsername("fred");
		user.setAttribute("pictureUrl", "https://example.com/fred.png");

		JsonNode node = JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsString(user));

		assertThat(node.get("pictureUrl").asText(), is("https://example.com/fred.png"));
		assertThat("backing map must not leak as a nested property", node.get("attributes"), is(nullValue()));
	}

	@Test
	public void unknownFieldsSurviveAReadModifyWrite() throws IOException {
		String json = """
			{
			  "id": "12345",
			  "username": "fred",
			  "somethingThisExtensionKnowsNothingAbout": { "nested": true }
			}""";

		FlintstoneUser user = JsonSerialization.readValue(json, FlintstoneUser.class);
		user.setLastName("Flintstone");

		JsonNode node = JsonSerialization.mapper.readTree(JsonSerialization.writeValueAsString(user));

		assertThat(node.get("lastName").asText(), is("Flintstone"));
		assertThat(node.get("somethingThisExtensionKnowsNothingAbout").get("nested").asBoolean(), is(true));
	}

	@Test
	public void settingAnAttributeToNullRemovesIt() {
		FlintstoneUser user = new FlintstoneUser();
		user.setAttribute("pictureUrl", "https://example.com/fred.png");
		user.setAttribute("pictureUrl", null);

		assertThat(user.getAttributes(), is(Map.of()));
	}
}
