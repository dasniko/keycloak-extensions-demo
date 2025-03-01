package dasniko.keycloak.user.flintstones.pages;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;

public class UpdatePasswordPage extends AbstractPage {

	public UpdatePasswordPage(HtmlPage page) {
		super(page);
	}

	@Override
	void verifyPage() {
		super.verifyPage();
		MatcherAssert.assertThat(page.getElementById("kc-passwd-update-form"), Matchers.notNullValue());
	}

	public <T extends AbstractPage> T setPasswordTo(String newPassword, Class<T> nextPageType) throws IOException {
		HtmlForm form2 = page.getForms().getFirst();
		form2.getInputByName("password-new").type(newPassword);
		form2.getInputByName("password-confirm").type(newPassword);
		HtmlPage nextPage = getButton(form2, "Submit").click();
		return preparePage(nextPageType, nextPage);
	}
}
