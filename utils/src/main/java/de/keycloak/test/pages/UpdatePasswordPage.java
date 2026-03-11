package de.keycloak.test.pages;

import org.hamcrest.Matchers;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class UpdatePasswordPage extends AbstractPage {

	public UpdatePasswordPage(HtmlPage page) {
		super(page);
	}

	@Override
	void verifyPage() {
		super.verifyPage();
		assertThat(page.getElementById("kc-passwd-update-form"), Matchers.notNullValue());
	}

	public <T extends AbstractPage> T setPasswordTo(String newPassword, Class<T> nextPageType) throws IOException {
		HtmlForm form = page.getForms().getFirst();
		form.getInputByName("password-new").type(newPassword);
		form.getInputByName("password-confirm").type(newPassword);
		HtmlPage nextPage = getButton(form, "Submit").click();
		return preparePage(nextPageType, nextPage);
	}
}
