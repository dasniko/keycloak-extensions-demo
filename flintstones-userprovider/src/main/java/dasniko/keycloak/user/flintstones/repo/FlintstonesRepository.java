package dasniko.keycloak.user.flintstones.repo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstonesRepository {

    private final List<FlintstoneUser> users;

    public FlintstonesRepository() {
        Long created = System.currentTimeMillis();
        List<String> roles = Collections.singletonList("stoneage");
        users = Arrays.asList(
                new FlintstoneUser("1", "Fred", "Flintstone", true, created, roles),
                new FlintstoneUser("2", "Wilma", "Flintstone", true, created, roles),
                new FlintstoneUser("3", "Pebbles", "Flintstone", true, created, roles),
                new FlintstoneUser("4", "Barney", "Rubble", true, created, roles),
                new FlintstoneUser("5", "Betty", "Rubble", true, created, Collections.emptyList()),
                new FlintstoneUser("6", "Bam Bam", "Rubble", false, created, Collections.emptyList())
        );
    }

    public List<FlintstoneUser> getAllUsers() {
        return users;
    }

    public int getUsersCount() {
        return users.size();
    }

    public FlintstoneUser findUserById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
    }

    public FlintstoneUser findUserByUsernameOrEmail(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    public List<FlintstoneUser> findUsers(String query) {
        return users.stream()
                .filter(user -> query.equalsIgnoreCase("*") || user.getUsername().contains(query) || user.getEmail().contains(query))
                .collect(Collectors.toList());
    }

    public boolean validateCredentials(String username, String password) {
        return findUserByUsernameOrEmail(username).getPassword().equals(password);
    }

    public boolean updateCredentials(String username, String password) {
        findUserByUsernameOrEmail(username).setPassword(password);
        return true;
    }

}
