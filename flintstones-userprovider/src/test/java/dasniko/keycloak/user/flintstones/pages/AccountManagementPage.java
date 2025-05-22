package dasniko.keycloak.user.flintstones.pages;

import org.htmlunit.html.HtmlPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AccountManagementPage extends AbstractPage {

	AccountManagementPage(HtmlPage page) {
		super(page);
	}

	@Override
	void verifyPage() {
		super.verifyPage();
		assertThat(page.getTitleText(), equalTo("Account Management"));
	}
}
