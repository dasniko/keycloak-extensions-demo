package de.keycloak.test;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.utils.MediaType;

import java.net.URI;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AuthorizationCodeGrant {

	public static AccessTokenResponse getTokenResponse(String baseUrl, String realm,
																										 String clientId, String clientSecret, String redirectUri,
																										 String username, String password) {

		/*
			NOTES: works only well with:
			- default browser authentication flow, no recognition of variants, like e.g. OTP
			- default (base) theme as the form url will be parsed from the created html code based on id attribute
		 */

		String[] pkce = preparePkce();
		ExtractableResponse<Response> response = given()
			.queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
			.queryParam(OAuth2Constants.CLIENT_ID, clientId)
			.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
			.queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
			.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, OAuth2Constants.PKCE_METHOD_S256)
			.queryParam(OAuth2Constants.CODE_CHALLENGE, pkce[0])
			.pathParam("realm-name", realm)
			.when().get(baseUrl + ServiceUrlConstants.AUTH_PATH)
			.then().statusCode(200).extract();
		Map<String, String> cookies = response.cookies();
		String formUrl = response.htmlPath().getString("**.find { it.@id == 'kc-form-login' }.@action");

		response = given().cookies(cookies)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.formParam(OAuth2Constants.USERNAME, username)
			.formParam(OAuth2Constants.PASSWORD, password)
			.when().post(formUrl)
			.then().statusCode(302)
			.extract();

		String code = getCodeFromResponse(response);

		RequestSpecification spec = given()
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.formParam(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE)
			.formParam(OAuth2Constants.CLIENT_ID, clientId)
			.formParam(OAuth2Constants.REDIRECT_URI, redirectUri)
			.formParam(OAuth2Constants.CODE, code)
			.formParam(OAuth2Constants.CODE_VERIFIER, pkce[1]);
		if (clientSecret != null) {
			spec.formParam(OAuth2Constants.CLIENT_SECRET, clientSecret);
		}

		return spec.pathParam("realm-name", realm)
			.when().post(baseUrl + ServiceUrlConstants.TOKEN_PATH)
			.then().statusCode(200).extract().body().as(AccessTokenResponse.class);
	}

	@SneakyThrows
	private static String getCodeFromResponse(ExtractableResponse<Response> response) {
		List<NameValuePair> params = URLEncodedUtils.parse(new URI(response.header(HttpHeaders.LOCATION)), Charset.defaultCharset());
		return params.stream().filter(p -> p.getName().equals(OAuth2Constants.CODE)).findFirst().orElseThrow().getValue();
	}

	private static String[] preparePkce() {
		String codeVerifier = RandomStringUtils.random(64, 0, 0, true, true, null, new SecureRandom());
		byte[] sha256 = DigestUtils.sha256(codeVerifier);
		String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256);
		return new String[] {codeChallenge, codeVerifier};
	}

}
