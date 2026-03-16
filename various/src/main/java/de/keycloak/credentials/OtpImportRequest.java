package de.keycloak.credentials;

import lombok.Data;

@Data
public class OtpImportRequest {
	private String username;
	private String seed;
}
