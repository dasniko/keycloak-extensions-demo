package dasniko.keycloak.user.flintstones.repo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credential {
	static final String TYPE_PASSWORD = "password";
	static final String TYPE_OTP = "otp";
	private String type;
	private String value;
}
