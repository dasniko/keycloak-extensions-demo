package de.keycloak.util;

/**
 * As of <a href="https://www.rfc-editor.org/rfc/rfc8176.html">Authentication Method Reference Values</a>
 */
public enum AuthenticationMethodReference {
	FACE("face"),
	FPT("fpt"),
	GEO("geo"),
	HWK("hwk"),
	IRIS("iris"),
	KBA("kba"),
	MCA("mca"),
	MFA("mfa"),
	OTP("otp"),
	PIN("pin"),
	PWD("pwd"),
	RBA("rba"),
	RETINA("retina"),
	SC("sc"),
	SMS("sms"),
	SWK("swk"),
	TEL("tel"),
	USER("user"),
	VBM("vbm"),
	WIA("wia"),
	;

	public final String value;

	AuthenticationMethodReference(String value) {
		this.value = value;
	}
}
