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
	private boolean emailVerified;
	private String firstName;
	private String lastName;
	private String password;
	private boolean enabled;
	private Long created;
	private List<String> groups;
	private List<String> roles;
	private String pictureUrl;

	public FlintstoneUser(String id, String firstName, String lastName, boolean enabled, List<String> roles) {
		this.id = id;
		this.email = firstName.toLowerCase().replace(" ", "") + "." + lastName.toLowerCase() + "@bedrock.com";
		this.username = email.substring(0, email.indexOf("."));
		this.firstName = firstName;
		this.lastName = lastName;
		this.password = firstName.toLowerCase();
		this.enabled = enabled;
		this.created = System.currentTimeMillis();
		this.groups = List.of(lastName.toUpperCase() + "_FAMILY");
		this.roles = roles;
		this.pictureUrl = "https://dasniko-public.s3.eu-central-1.amazonaws.com/" + this.username + ".png";
	}

}
