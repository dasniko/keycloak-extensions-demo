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

	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String password;
	private boolean enabled;
	private Long created;
	private List<String> roles;

	public FlintstoneUser(String email, String firstName, String lastName, boolean enabled, List<String> roles) {
		this.username = email.substring(0, email.indexOf("@"));
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.password = firstName.toLowerCase();
		this.enabled = enabled;
		this.created = System.currentTimeMillis();
		this.roles = roles;
	}

}
