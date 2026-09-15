package dasniko.keycloak.user.flintstones;

import dasniko.keycloak.user.flintstones.mappers.AttributeMappings;
import dasniko.keycloak.user.flintstones.repo.FlintstoneUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Covers the adapter's handling of read-only mappings, which the container tests cannot reach: Keycloak's user profile drops writes
 * to read-only attributes before they get to the adapter.
 *
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
public class FlintstoneUserAdapterTest {

	private FlintstoneUser user;
	private FlintstoneUserAdapter adapter;

	@BeforeEach
	public void setUp() {
		ComponentModel model = new ComponentModel();
		model.put(AttributeMappings.CONFIG_KEY, """
			[
			  { "name": "picture", "field": "pictureUrl" },
			  { "name": "loginCount", "field": "logins", "type": "integer", "readOnly": true }
			]""");

		user = new FlintstoneUser();
		user.setId("1");
		user.setAttribute("pictureUrl", "https://example.com/fred.png");
		user.setAttribute("logins", 42);

		// no session or realm needed: mapped attributes are resolved from the component model alone
		adapter = new FlintstoneUserAdapter(null, null, model, user);
	}

	@Test
	public void readOnlyAttributeIsUpdatedInMemoryWithoutMarkingDirty() {
		adapter.setSingleAttribute("loginCount", "43");

		assertThat(adapter.getFirstAttribute("loginCount"), is("43"));
		assertThat(user.getAttribute("logins"), is(43));
		assertThat(adapter.isDirty(), is(false));
	}

	@Test
	public void removingReadOnlyAttributeClearsItInMemoryWithoutMarkingDirty() {
		adapter.removeAttribute("loginCount");

		assertThat(adapter.getFirstAttribute("loginCount"), nullValue());
		assertThat(adapter.isDirty(), is(false));
	}

	@Test
	public void writableAttributeMarksDirty() {
		adapter.setSingleAttribute("picture", "https://example.com/wilma.png");

		assertThat(adapter.getFirstAttribute("picture"), is("https://example.com/wilma.png"));
		assertThat(adapter.isDirty(), is(true));
	}

	@Test
	public void readOnlyWriteDoesNotResetDirtyFromAnEarlierWritableWrite() {
		adapter.setSingleAttribute("picture", "https://example.com/wilma.png");
		adapter.setAttribute("loginCount", List.of("43"));

		assertThat(adapter.isDirty(), is(true));
	}
}
