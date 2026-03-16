package de.keycloak.resource.otp;

public record OtpValidationRequest(String username, String otp) {
}
