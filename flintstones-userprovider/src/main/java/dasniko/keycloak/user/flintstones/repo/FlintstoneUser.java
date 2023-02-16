package dasniko.keycloak.user.flintstones.repo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Data
@NoArgsConstructor
public class FlintstoneUser {

	private String id;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String password;
	private boolean enabled;
	private Long created;
	private List<String> roles;

	public FlintstoneUser(String id, String firstName, String lastName, boolean enabled, List<String> roles) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.username = (firstName + "." + lastName).replaceAll("\\s", "").toLowerCase();
		this.email = this.username + "@flintstones.com";
		this.password = firstName.toLowerCase();
		this.enabled = enabled;
		this.created = System.currentTimeMillis();
		this.roles = roles;
	}

}
