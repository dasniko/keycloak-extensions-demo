<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("magicLinkTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <#-- display of magic link message will be handled through global message handling in template.ftl -->
        </div>
    </#if>
</@layout.registrationLayout>
