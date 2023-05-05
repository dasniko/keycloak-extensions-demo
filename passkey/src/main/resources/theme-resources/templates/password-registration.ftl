<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("setupPassword")}
    <#elseif section = "form">
			<form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">

				<div class="${properties.kcFormGroupClass!}">
					<div class="${properties.kcLabelWrapperClass!}">
						<label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
					</div>
					<div class="${properties.kcInputWrapperClass!}">
						<input type="password" id="password" class="${properties.kcInputClass!}" name="password"
									 autocomplete="new-password"
									 aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
						/>

              <#if messagesPerField.existsError('password')>
								<span id="input-error-password" class="${properties.kcInputErrorMessageClass!}"
											aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password'))?no_esc}
                            </span>
              </#if>
					</div>
				</div>

				<div class="${properties.kcFormGroupClass!}">
					<div class="${properties.kcLabelWrapperClass!}">
						<label for="password-confirm"
									 class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
					</div>
					<div class="${properties.kcInputWrapperClass!}">
						<input type="password" id="password-confirm" class="${properties.kcInputClass!}"
									 name="password-confirm"
									 aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
						/>

              <#if messagesPerField.existsError('password-confirm')>
								<span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}"
											aria-live="polite">
                                ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                            </span>
              </#if>
					</div>
					<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
						<input
							class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
							type="submit" value="${msg("doRegister")}"/>
					</div>
				</div>
			</form>
    </#if>

</@layout.registrationLayout>
