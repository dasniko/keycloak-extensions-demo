package dasniko.keycloak.actiontoken;

import lombok.Data;

@Data
public class CustomTokenResponse {
	private String userId;
	private String link;
}
