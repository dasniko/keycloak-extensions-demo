package dasniko.keycloak.actiontoken;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // required for JSON de-/serialization
public class CustomActionToken extends DefaultActionToken {

	public static final String TOKEN_TYPE = "custom-action";

	private String redirectUri;
	private boolean reuse;
	private String scope;
	private String state;

	public CustomActionToken(String userId, String clientId, int expirationInSeconds, boolean reuse,
													 String redirectUri, String scope, String nonce, String state) {
		super(userId, TOKEN_TYPE, expirationInSeconds, uuidOf(nonce));
		this.issuedFor = clientId;
		this.redirectUri = redirectUri;
		this.reuse = reuse;
		this.scope = scope;
		this.state = state;
	}

	static UUID uuidOf(String s) {
		try {
			return UUID.fromString(s);
		} catch (Exception ignored) {
		}
		return null;
	}
}
