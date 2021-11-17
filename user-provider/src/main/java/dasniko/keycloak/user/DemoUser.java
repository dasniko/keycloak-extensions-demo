package dasniko.keycloak.user;

import lombok.Data;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Data
public class DemoUser {

	private String id;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String password;
	private boolean enabled;
	private Long created;
	private List<String> roles;

	public DemoUser(String id, String firstName, String lastName, boolean enabled, Long created, List<String> roles) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.username = (firstName + "." + lastName).toLowerCase();
		this.email = (firstName.replaceAll("\\s", "") + "." + lastName + "@flintstones.com").toLowerCase();
		this.password = firstName.toLowerCase();
		this.enabled = enabled;
		this.created = created;
		this.roles = roles;
	}

}
