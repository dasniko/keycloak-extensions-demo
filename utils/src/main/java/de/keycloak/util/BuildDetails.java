package de.keycloak.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BuildDetails {

	private static final List<String> propertyNames = List.of("git.branch", "git.build.time", "git.build.version", "git.commit.id.abbrev");

	public static Map<String, String> get() {
		Map<String, String> buildDetails = new HashMap<>();
		java.util.Properties gitProperties = new java.util.Properties();
		try {
			gitProperties.load(Objects.requireNonNull(BuildDetails.class.getClassLoader().getResourceAsStream("git.properties")));
			propertyNames.forEach(propertyName -> buildDetails.put(propertyName, gitProperties.getProperty(propertyName, "n/a")));
		} catch (Exception e) {
			buildDetails.put("build.time", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
			buildDetails.put("exception", e.getMessage());
		}

		return buildDetails;
	}

}
