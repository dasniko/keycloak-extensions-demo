package de.keycloak.credentials;

public record OtpImportRequest(String username, String seed) {
}
