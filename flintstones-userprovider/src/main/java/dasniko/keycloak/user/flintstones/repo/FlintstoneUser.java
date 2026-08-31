package dasniko.keycloak.user.flintstones.repo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Data
@NoArgsConstructor
public class FlintstoneUser {

	private String id;
	private String username;
	private String email;
	private boolean emailVerified;
	private String firstName;
	private String lastName;
	private boolean enabled;
	private Long created;

	private List<String> groups;
	private List<String> roles;

	private String password;
	private String otp;

	/**
	 * Everything the API returns beyond the fields above. Keeping it open means the provider does not need to know the full schema
	 * of the external user record: unknown fields survive a read-modify-write instead of being silently dropped, and new fields can
	 * be mapped to Keycloak attributes by configuration alone.
	 */
	private final Map<String, Object> attributes = new LinkedHashMap<>();

	public FlintstoneUser(String id, String firstName, String lastName, int yearOfBirth, boolean enabled, List<String> roles) {
		this.id = id;
		this.email = firstName.toLowerCase().replace(" ", "") + "." + lastName.toLowerCase() + "@bedrock.com";
		this.username = email.substring(0, email.indexOf("."));
		this.firstName = firstName;
		this.lastName = lastName;
		this.enabled = enabled;
		this.created = System.currentTimeMillis();

		this.groups = List.of(lastName.toUpperCase() + "_FAMILY");
		this.roles = roles;

		this.password = firstName.toLowerCase();
		this.otp = id;

		setAttribute("pictureUrl", "https://dasniko-public.s3.eu-central-1.amazonaws.com/" + this.username + ".png");
		setAttribute("phoneNumbers", List.of("+1-555-" + id, "+1-666-" + id));
		setAttribute("yearOfBirth", yearOfBirth);

		// a complex value: mapped either as a serialized JSON string, or by projecting a single member out of it
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("street", id + " Cobblestone Way");
		address.put("city", "Bedrock");
		setAttribute("address", address);
	}

	@JsonAnyGetter
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@JsonAnySetter
	public void setAttribute(String name, Object value) {
		if (value == null) {
			attributes.remove(name);
		} else {
			attributes.put(name, value);
		}
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void replaceAttributes(Map<String, Object> newAttributes) {
		Map<String, Object> copy = new LinkedHashMap<>(newAttributes);
		attributes.clear();
		attributes.putAll(copy);
	}

}
