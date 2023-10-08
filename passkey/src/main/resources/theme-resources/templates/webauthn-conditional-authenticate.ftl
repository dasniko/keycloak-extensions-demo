<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
			<div id="kc-form">
				<div id="kc-form-wrapper">
            <#if realm.password>
							<form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                  <#if !usernameHidden??>
										<div class="${properties.kcFormGroupClass!}">
											<label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

											<input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" autofocus type="text" autocomplete="username webauthn"
														 aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
											/>

                        <#if messagesPerField.existsError('username','password')>
													<span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>

										</div>
                  </#if>

								<div class="${properties.kcFormGroupClass!}">
									<label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>

									<input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
												 aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
									/>

                    <#if usernameHidden?? && messagesPerField.existsError('username','password')>
											<span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>

								</div>

								<div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
									<div id="kc-form-options">
                      <#if realm.rememberMe && !usernameHidden??>
												<div class="checkbox">
													<label>
                              <#if login.rememberMe??>
																<input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                              <#else>
																<input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                              </#if>
													</label>
												</div>
                      </#if>
									</div>
									<div class="${properties.kcFormOptionsWrapperClass!}">
                      <#if realm.resetPasswordAllowed>
												<span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                      </#if>
									</div>

								</div>

								<div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
									<input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
									<input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
									<input tabindex="5" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" id="authenticateWebAuthnButton" type="button" onclick="startWebAuthnAuthentication()" value="${kcSanitize(msg("webauthn-doAuthenticate"))}"/>
								</div>
							</form>
            </#if>
				</div>

			</div>
			<form id="webauth" action="${url.loginAction}" method="post">
				<input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
				<input type="hidden" id="authenticatorData" name="authenticatorData"/>
				<input type="hidden" id="signature" name="signature"/>
				<input type="hidden" id="credentialId" name="credentialId"/>
				<input type="hidden" id="userHandle" name="userHandle"/>
				<input type="hidden" id="error" name="error"/>
			</form>

			<script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
			<script type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
			<script type="text/javascript">
				let abortController;

				function startWebAuthnAuthentication() {
					if (abortController) {
						abortController.abort('restart');
					}
					doAuthenticate(true);
				}

				async function doAuthenticate(forceAuthentication) {

					// Check if WebAuthn is supported by this browser
					if (!window.PublicKeyCredential) {
						$("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
						$("#webauth").submit();
						return;
					}

					let isCMA = false;
					if (!forceAuthentication) {
						if (!window.PublicKeyCredential.isConditionalMediationAvailable) {
							// do not prepare/initiate conditional mediation
							return;
						} else {
							isCMA = await window.PublicKeyCredential.isConditionalMediationAvailable();
						}
					}

					abortController = new AbortController();

					const challenge = "${challenge}";
					const userVerification = "${userVerification}";
					const rpId = "${rpId}";
					const publicKey = {
						rpId : rpId,
						challenge: base64url.decode(challenge, { loose: true }),
					};

					const createTimeout = ${createTimeout};
					if (createTimeout !== 0) publicKey.timeout = createTimeout * 1000;

					if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

					try {
						const result = await navigator.credentials.get({
							publicKey,
							signal: isCMA ? abortController.signal : undefined,
							mediation: isCMA ? "conditional" : undefined,
						});
						window.result = result;

						const clientDataJSON = result.response.clientDataJSON;
						const authenticatorData = result.response.authenticatorData;
						const signature = result.response.signature;

						$("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), { pad: false }));
						$("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), { pad: false }));
						$("#signature").val(base64url.encode(new Uint8Array(signature), { pad: false }));
						$("#credentialId").val(result.id);
						if(result.response.userHandle) {
							$("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), { pad: false }));
						}
						$("#webauth").submit();
					} catch (err) {
						if (err === 'restart') {
							return;
						}
						$("#error").val(err);
						$("#webauth").submit();
					}
				}

				doAuthenticate();
			</script>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
					<div id="kc-registration-container">
						<div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6"
																								 href="${url.registrationUrl}">${msg("doRegister")}</a></span>
						</div>
					</div>
        </#if>
    <#elseif section = "socialProviders" >
        <#if realm.password && social.providers??>
					<div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
						<hr/>
						<h4>${msg("identity-provider-login-label")}</h4>

						<ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                <#list social.providers as p>
									<li>
										<a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
											 type="button" href="${p.loginUrl}">
                        <#if p.iconClasses?has_content>
													<i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
													<span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                        <#else>
													<span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                        </#if>
										</a>
									</li>
                </#list>
						</ul>
					</div>
        </#if>
    </#if>

</@layout.registrationLayout>
