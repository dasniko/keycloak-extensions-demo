package dasniko.keycloak.requiredaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RequiredActionConfigModel;

import java.util.Map;

public class SkippableConfigureTOTPTest {

	@Test
	public void testIsPastDeadline() {
		RequiredActionConfigModel config = new RequiredActionConfigModel();
		Map<String, String> map = Map.of(SkippableConfigureTOTP.SKIP_DEADLINE_KEY, "2025-01-01");
		config.setConfig(map);

		SkippableConfigureTOTP cut = new SkippableConfigureTOTP();
		Assertions.assertFalse(cut.isFutureDeadline(config));
	}

	@Test
	public void testIsFutureDeadline() {
		RequiredActionConfigModel config = new RequiredActionConfigModel();
		Map<String, String> map = Map.of(SkippableConfigureTOTP.SKIP_DEADLINE_KEY, "2099-12-31");
		config.setConfig(map);

		SkippableConfigureTOTP cut = new SkippableConfigureTOTP();
		Assertions.assertTrue(cut.isFutureDeadline(config));
	}

	@Test
	public void testIsEmptyDeadline() {
		RequiredActionConfigModel config = new RequiredActionConfigModel();
		Map<String, String> map = Map.of(SkippableConfigureTOTP.SKIP_DEADLINE_KEY, "");
		config.setConfig(map);

		SkippableConfigureTOTP cut = new SkippableConfigureTOTP();
		Assertions.assertFalse(cut.isFutureDeadline(config));
	}

	@Test
	public void testIsNullDeadline() {
		RequiredActionConfigModel config = new RequiredActionConfigModel();
		Map<String, String> map = Map.of();
		config.setConfig(map);

		SkippableConfigureTOTP cut = new SkippableConfigureTOTP();
		Assertions.assertFalse(cut.isFutureDeadline(config));
	}

	@Test
	public void testIsNullConfig() {
		RequiredActionConfigModel config = new RequiredActionConfigModel();

		SkippableConfigureTOTP cut = new SkippableConfigureTOTP();
		Assertions.assertFalse(cut.isFutureDeadline(config));
	}

}
