<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("deviceConfirmEmailBodyHtml", name, code, link, ttlMinutes))?no_esc}
</@layout.emailLayout>
