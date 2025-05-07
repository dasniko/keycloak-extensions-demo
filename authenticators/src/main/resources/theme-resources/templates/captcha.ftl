<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
								<form id="kc-form-login" action="${url.loginAction}" method="post">
										<h3>
												<span>${msg("captcha.doTheMath")}</span>
										</h3>
										<div class="${properties.kcFormGroupClass!}">
												<label for="result" class="${properties.kcLabelClass!}">${msg("captcha.task", firstOperand, secondOperand)}</label>
												<input tabindex="1" id="result" class="${properties.kcInputClass!}" name="result" value=""  type="text" autofocus autocomplete="off"/>
										</div>

										<div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
												<input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
										</div>
								</form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
