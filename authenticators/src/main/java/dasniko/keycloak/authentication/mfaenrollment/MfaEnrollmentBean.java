package dasniko.keycloak.authentication.mfaenrollment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.RequiredActionProviderModel;

import java.util.List;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("ClassCanBeRecord") // we need a bean with a getter for freemarker usage
public class MfaEnrollmentBean {
	private final List<RequiredActionProviderModel> requiredActions;
}
