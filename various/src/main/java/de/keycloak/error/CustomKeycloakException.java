package de.keycloak.error;

public class CustomKeycloakException extends RuntimeException {
	public CustomKeycloakException(String message) {
		super(message);
	}
}
