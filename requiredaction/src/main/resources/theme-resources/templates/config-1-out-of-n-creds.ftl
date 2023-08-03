<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
			${msg("configureCredentialsTitle")}
    <#elseif section = "form">
			<p>${msg("configureCredentialsText")}</p>
			<form id="configCredentials" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
          <#list credentialOptions as key, value>
						<div class="${properties.kcFormGroupClass!}">
							<div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
								<button
									class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
									name="requiredActionName" type="submit" value="${value}">
                    ${msg("requiredAction." +  value)}
								</button>
							</div>
						</div>
          </#list>
			</form>
    </#if>
</@layout.registrationLayout>
