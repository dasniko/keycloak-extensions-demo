package dasniko.keycloak.actiontoken;

import lombok.Data;

@Data
public class CustomTokenRequest {
	private String username;
	private String email;
	private String clientId;
	private String redirectUri;
	private String scope = null;
	private String nonce = null;
	private String state = null;
	private Integer expirationSeconds = null;
	private boolean forceCreate = false;
	private boolean updateProfile = false;
	private boolean updatePassword = false;
	private boolean reuse = false;
}
