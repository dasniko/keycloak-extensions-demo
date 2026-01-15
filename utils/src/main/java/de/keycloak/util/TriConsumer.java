package de.keycloak.util;

@FunctionalInterface
interface TriConsumer<T, U, V> {
	void accept(T t, U u, V v);
}
