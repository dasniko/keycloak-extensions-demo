<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("magicLinkTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
								<form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
										<div class="${properties.kcFormGroupClass!}">
												<label for="username" class="${properties.kcLabelClass!}">
														<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
												</label>
												<input tabindex="1" id="username"
															 aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
															 class="${properties.kcInputClass!}" name="username"
															 type="text" autofocus autocomplete="off"/>
												<#if messagesPerField.existsError('username')>
														<span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
																${kcSanitize(messagesPerField.get('username'))?no_esc}
														</span>
												</#if>
										</div>
										<div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
												<input tabindex="4"
															 class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
															 name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
										</div>
								</form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
