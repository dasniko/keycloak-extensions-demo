# Keycloak Friendly Captcha Registration Form Action


## Authentication Flow


## JavaScript Widget Library


## Templates

Replace in your custom theme in the templates `register.ftl` and `register-user-profile.ftl` the sections surrounded with `<#if recaptchaRequired??>...</#if>` with the following snippet:

```injectedfreemarker
<#if friendlyCaptchaRequired??>
<div class="form-group">
  <div class="${properties.kcInputWrapperClass!}">
    <div class="frc-captcha" data-sitekey="${friendlyCaptchaSiteKey}" data-lang="${friendlyCaptchaLanguage!}" data-start="auto" data-puzzle-endpoint="${friendlyCaptchaDataPuzzleEndpoint!}"></div>
  </div>
</div>
</#if>
```

## Messages

This extension makes use of currently these message labels:

```properties
friendlyCaptchaNotConfigured=Friendly Captcha is required, but not configured
friendlyCaptchaFailed=Invalid Friendly Captcha
```

You can overwrite and localize them in you custom theme message bundles.
