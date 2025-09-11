<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true; section>
	<#if section = "header">
		<#if messageHeader??>
			${kcSanitize(msg("${messageHeader}"))?no_esc}
		<#else>
			${msg("registerTitle")}
		</#if>
	<#elseif section = "form">
		<form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
			<div class="${properties.kcFormGroupClass!}">
				<div class="${properties.kcLabelWrapperClass!}">
					<label for="registrationCode" class="${properties.kcLabelClass!}">${msg("registrationCodeTitle")}*</label>
				</div>
				<div class="${properties.kcInputWrapperClass!}">
					<input type="text" id="registrationCode" name="registrationCode" value="${registrationCode!''}" class="${properties.kcInputClass!}" autofocus/>
				</div>
				<#if messagesPerField.existsError('registrationCode')>
					<span id="input-error-registrationCode" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
		      ${kcSanitize(messagesPerField.get('registrationCode'))?no_esc}
				</span>
				</#if>
			</div>

			<div class="${properties.kcFormGroupClass!}">
				<div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
					<div class="${properties.kcFormOptionsWrapperClass!}">
						<span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
					</div>
				</div>
				<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
					<input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegister")}"/>
				</div>
			</div>
		</form>
	</#if>
</@layout.registrationLayout>
