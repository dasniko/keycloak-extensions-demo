package dasniko.keycloak.user.peanuts.external;

import lombok.Data;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Data
public class Peanut {
	private String username;
	private String firstName;
	private String lastName;
	private String email;
	private String birthday;
	private String gender;
	private List<String> groups;
	private List<String> roles;
}
