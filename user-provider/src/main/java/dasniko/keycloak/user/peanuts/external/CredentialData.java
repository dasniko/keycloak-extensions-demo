package dasniko.keycloak.user.peanuts.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.keycloak.common.util.Base64;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {

	private String value;
	private String salt ;
	private String algorithm;
	private Integer iterations;
	private String type;

	@SneakyThrows
	public PasswordCredentialModel toPasswordCredentialModel() {
		return PasswordCredentialModel.createFromValues(
			this.getAlgorithm(), Base64.decode(this.getSalt()), this.getIterations(), this.getValue());
	}

	public static CredentialData fromPasswordCredentialModel(PasswordCredentialModel pcm) {
		return new CredentialData(
			pcm.getPasswordSecretData().getValue(),
			Base64.encodeBytes(pcm.getPasswordSecretData().getSalt()),
			pcm.getPasswordCredentialData().getAlgorithm(),
			pcm.getPasswordCredentialData().getHashIterations(),
			PasswordCredentialModel.TYPE
		);
	}
}
