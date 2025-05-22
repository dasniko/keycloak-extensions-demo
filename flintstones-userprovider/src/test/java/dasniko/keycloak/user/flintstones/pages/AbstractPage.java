package dasniko.keycloak.user.flintstones.pages;

import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class AbstractPage {

	final HtmlPage page;

	AbstractPage(HtmlPage page) {
		this.page = page;
		verifyPage();
	}

	void checkHttpStatusOk() {
		assertThat(page.getWebResponse().getStatusCode(), is(200));
	}

	@NotNull HtmlButton getButton(HtmlForm form, String label) {
		Optional<HtmlElement> submit = form.getFormElements().stream().filter(element -> element instanceof HtmlButton && element.getTextContent().trim().equals(label)).findFirst();
		assertThat(submit.isPresent(), is(true));
		return (HtmlButton) submit.get();
	}

	void verifyPage() {
		checkHttpStatusOk();
	}

	<T extends AbstractPage> @NotNull T preparePage(Class<T> nextPageType, HtmlPage nextPage) {
		try {
			return nextPageType.getDeclaredConstructor(HtmlPage.class).newInstance(nextPage);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException("Can't create page " + nextPageType.getName(), e);
		}
	}

}
