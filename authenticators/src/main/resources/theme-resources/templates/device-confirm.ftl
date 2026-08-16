<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
	<#if section = "header">
		${msg("deviceConfirmTitle")}
	<#elseif section = "form">
		<form id="kc-device-confirm-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
			<div class="${properties.kcFormGroupClass!}">
				<p>${msg("deviceConfirmText")}</p>
			</div>
			<div class="${properties.kcFormGroupClass!}">
				<div class="${properties.kcLabelWrapperClass!}">
					<label for="code" class="${properties.kcLabelClass!}">${msg("deviceConfirmCodeLabel")}</label>
				</div>
				<div class="${properties.kcInputWrapperClass!}">
					<input type="text" id="code" name="code" class="${properties.kcInputClass!}" autofocus autocomplete="off"/>
				</div>
			</div>
			<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
				<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("deviceConfirmSubmit")}"/>
			</div>
		</form>
	</#if>
</@layout.registrationLayout>
