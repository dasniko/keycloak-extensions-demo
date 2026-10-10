package de.keycloak.provider;

import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public interface DefaultServerInfoAware extends ServerInfoAwareProviderFactory {

	@Override
	default Map<String, String> getOperationalInfo() {
		return getBuildDetails();
	}

	private Map<String, String> getBuildDetails() {
		List<String> propertyNames = List.of("git.branch", "git.build.time", "git.build.version", "git.commit.id.abbrev");
		Map<String, String> buildDetails = new HashMap<>();
		Class<?> clazz = this.getClass();
		buildDetails.put("className", clazz.getCanonicalName());
		java.util.Properties gitProperties = new java.util.Properties();
		try {
			gitProperties.load(Objects.requireNonNull(clazz.getClassLoader().getResourceAsStream("git.properties")));
			propertyNames.forEach(propertyName -> buildDetails.put(propertyName, gitProperties.getProperty(propertyName, "n/a")));
		} catch (Exception e) {
			buildDetails.put("build.time", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
			buildDetails.put("exception", e.getMessage());
		}

		return buildDetails;
	}
}
