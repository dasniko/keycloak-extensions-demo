package dasniko.keycloak.authentication.conditional;

import com.google.auto.service.AutoService;
import de.keycloak.util.AuthenticatorUtil;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

@JBossLog
@AutoService(AuthenticatorFactory.class)
public class ConditionalCidrAuthenticator extends AbstractConditionalAuthenticator {

	public static final String PROVIDER_ID = "conditional-cidr";

	static final String CONFIG_CIDR = "cidr";

	@Override
	public boolean matchCondition(AuthenticationFlowContext context) {
		String remoteAddress = context.getConnection().getRemoteAddr();
		String cidrString = AuthenticatorUtil.getConfig(context, CONFIG_CIDR, "0.0.0.0/0");
		String[] cidrs = cidrString.split(",");
		boolean isInSubnet = false;
		for (String cidr : cidrs) {
			if (isIpInSubnet(remoteAddress, cidr.trim())) {
				isInSubnet = true;
			}
		}
		log.debugf("Configured CIDRs: %s - Remote IP Address: %s - isInSubnet: %s", cidrString, remoteAddress, isInSubnet);

		boolean negateOutput = AuthenticatorUtil.getConfig(context, ConditionalAuthNoteAuthenticatorFactory.CONF_NOT, Boolean.FALSE);
		return negateOutput != isInSubnet;
	}

	@Override
	public String getDisplayType() {
		return "Condition - CIDR";
	}

	@Override
	public String getHelpText() {
		return "Evaluates if the request originates from a given CIDR address.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		ProviderConfigProperty cidrConfig = new ProviderConfigProperty(CONFIG_CIDR, "CIDRs",
			"Allowed CIDR entries of the requesting systems (multiple values comma separated).",
			ProviderConfigProperty.STRING_TYPE, "0.0.0.0/0");

		return List.of(cidrConfig, negateOutputConfProperty);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@SneakyThrows
	protected boolean isIpInSubnet(String ip, String subnet) {
		if (!subnet.contains("/")) {
			subnet += "/32";
		}
		String[] subnetParts = subnet.split("/");
		InetAddress subnetAddress = InetAddress.getByName(subnetParts[0]);
		InetAddress address = InetAddress.getByName(ip);
		int maskSize = Integer.parseInt(subnetParts[1]);
		int maxMaskSize = subnetAddress instanceof Inet4Address ? 32 : 128;
		BigInteger maskBits = BigInteger.ONE.shiftLeft(maskSize).subtract(BigInteger.ONE).shiftLeft(maxMaskSize - maskSize);
		return new BigInteger(address.getAddress()).xor(new BigInteger(subnetAddress.getAddress())).and(maskBits).equals(BigInteger.ZERO);
	}

}
