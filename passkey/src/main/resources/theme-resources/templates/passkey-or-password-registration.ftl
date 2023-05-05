<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
    <#elseif section = "form">
			<form id="setupAuth" action="${url.loginAction}" method="post">
				<input type="hidden" id="setupType" name="setupType"/>
			</form>
			<div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
				<input tabindex="4"
							 class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
							 name="login" id="kc-login" type="submit" value="${msg("setupPassword")}" onclick="setupPassword()"/>
			</div>

			<div style="border-bottom: 1px solid;  text-align: center;  height: 10px;  margin-bottom: 10px;">
				<span style="background: #fff; padding: 0 5px;">Or</span>
			</div>

			<div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
				<input tabindex="4"
							 class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
							 name="login" id="kc-login" type="submit" value="${msg("setupPasskey")}" onclick="setupPasskey()"/>
			</div>
    </#if>
	<script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
	<script type="text/javascript">
		// Check if WebAuthn is supported by this browser
		// If not redirect to password setup
		if (!window.PublicKeyCredential) {
			$("#setupType").val("password");
			$("#setupAuth").submit();
		}

		if (document.getElementById("kc-select-try-another-way-form") != null) {
			document.getElementById("kc-select-try-another-way-form").style.display = "none";
		}

		function setupPasskey() {
			$("#setupType").val("passkey");
			$("#setupAuth").submit();
		}

		function setupPassword() {
			$("#setupType").val("password");
			$("#setupAuth").submit();
		}
	</script>

</@layout.registrationLayout>
