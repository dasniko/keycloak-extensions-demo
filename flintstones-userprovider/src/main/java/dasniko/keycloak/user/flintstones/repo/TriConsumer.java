package dasniko.keycloak.user.flintstones.repo;

@FunctionalInterface
interface TriConsumer<T, U, V> {
	void accept(T t, U u, V v);
}
