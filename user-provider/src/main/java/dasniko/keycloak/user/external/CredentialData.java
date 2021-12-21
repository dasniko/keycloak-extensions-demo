package dasniko.keycloak.user.external;

import lombok.SneakyThrows;
import lombok.Value;
import org.keycloak.common.util.Base64;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Value
public class CredentialData {
    String value;
    String salt ;
    String algorithm;
    Integer iterations;
    String type;

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
