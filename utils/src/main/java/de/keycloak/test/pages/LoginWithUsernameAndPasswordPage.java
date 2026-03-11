package de.keycloak.test.pages;

import org.hamcrest.Matchers;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;

public class LoginWithUsernameAndPasswordPage extends AbstractPage {

	public LoginWithUsernameAndPasswordPage(HtmlPage page) {
		super(page);
	}

	public static LoginWithUsernameAndPasswordPage build(WebClient webClient, URL url) throws IOException {
		return new LoginWithUsernameAndPasswordPage(webClient.getPage(url));
	}

	@Override
	void verifyPage() {
		super.verifyPage();
		assertThat(page.getElementById("kc-form-login"), Matchers.notNullValue());
	}

	public <T extends AbstractPage> T signInWithUsernameAndPassword(String username, String password, Class<T> nextPageType) throws IOException {
		HtmlForm form = page.getForms().getFirst();
		form.getInputByName("username").type(username);
		form.getInputByName("password").type(password);
		HtmlPage nextPage = getButton(form, "Sign In").click();
		return preparePage(nextPageType, nextPage);
	}

}
