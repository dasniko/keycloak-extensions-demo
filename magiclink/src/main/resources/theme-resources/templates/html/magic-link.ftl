<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("magicLinkEmailBodyHtml", name, link))?no_esc}
</@layout.emailLayout>
