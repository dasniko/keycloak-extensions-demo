package de.keycloak.util;

import lombok.experimental.UtilityClass;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.stream.Stream;

@UtilityClass
public class ComponentUtil {

	public static ComponentModel getComponentModel(KeycloakSession session, Class<Provider> providerClass, String providerId) {
		RealmModel realm = session.getContext().getRealm();
		return getComponents(realm, providerClass, providerId)
			.findFirst()
			.orElseThrow(() ->
				new IllegalStateException(
					"No provider component with providerId '%s' found for realm '%s'. Did you forget to register it?"
						.formatted(providerId, realm.getName()))
			);
	}

	public static Stream<ComponentModel> getComponents(RealmModel realm, Class<Provider> providerClass, String providerId) {
		return realm.getComponentsStream(realm.getId(), providerClass.getName())
			.filter(cm -> cm.getProviderId().equals(providerId));
	}

}
