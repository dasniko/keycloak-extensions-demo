<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mobile_number'); section>
    <#if section = "header">
        ${msg("updateMobileTitle")}
    <#elseif section = "form">
			<h2>${msg("updateMobileHello",(username!''))}</h2>
			<p>${msg("updateMobileText")}</p>
			<form id="kc-mobile-update-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
				<div class="${properties.kcFormGroupClass!}">
					<div class="${properties.kcLabelWrapperClass!}">
						<label for="mobile_number"class="${properties.kcLabelClass!}">${msg("updateMobileFieldLabel")}</label>
					</div>
					<div class="${properties.kcInputWrapperClass!}">
						<input type="tel" id="mobile_number" name="mobile_number" class="${properties.kcInputClass!}"
									 value="${mobile_number}" required aria-invalid="<#if messagesPerField.existsError('mobile_number')>true</#if>"/>
              <#if messagesPerField.existsError('mobile_number')>
								<span id="input-error-mobile-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
										${kcSanitize(messagesPerField.get('mobile_number'))?no_esc}
								</span>
              </#if>
					</div>
				</div>
				<div class="${properties.kcFormGroupClass!}">
					<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
						<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
					</div>
				</div>
			</form>
    </#if>
</@layout.registrationLayout>
