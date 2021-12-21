package dasniko.keycloak.user.external;

import lombok.Value;

import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Value
public class Peanut {
    String username;
    String firstName;
    String lastName;
    String email;
    String birthday;
    String gender;
    List<String> groups;
    List<String> roles;
}
