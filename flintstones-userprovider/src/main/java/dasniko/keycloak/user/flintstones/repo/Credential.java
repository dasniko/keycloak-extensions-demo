package dasniko.keycloak.user.flintstones.repo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credential {
	private String type;
	private String value;
}
