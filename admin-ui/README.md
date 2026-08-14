# Keycloak Admin UI Extensions

Demonstrates Keycloak's `UiPageProvider` / `UiTabProvider` SPIs — a way to extend the admin console with new pages and realm-settings tabs purely through provider config metadata, without writing any custom frontend code. The admin console renders a generic CRUD form from `getConfigProperties()`.

---

## Todo Admin Page (`UiPageProvider`)

`AdminUiPage` registers a new master-detail page (id `Todo`) in the admin console, backed by `ComponentModel` storage — admins can create, edit and delete "Todo" instances directly from the generated UI, no backing Java logic needed beyond the field definitions.

**Fields:**

| Property | Type | Description |
|---|---|---|
| Name | `STRING_TYPE` | Short name of the task. |
| Description | `TEXT_TYPE` | Description of what needs to be done. |
| Priority | `LIST_TYPE` | One of `critical`, `high priority`, `neutral`, `low priority`, `unknown`. |

Purely a demo of the extension point — items are stored as generic components but nothing in this repo reads them back out.

---

## Realm Logo Tab (`UiTabProvider`)

`ThemeUiTab` adds a config field to the realm settings' **Attributes** tab (`getPath()` = `/:realm/realm-settings/:tab?` with `tab=attributes`) for setting a realm logo.

| Property | Type | Description |
|---|---|---|
| Set a logo | `STRING_TYPE` | Shown on the account UI. |

**How it works:** `onCreate()` runs when the tab's component is saved, and copies the submitted `logo` value straight onto the realm as a `logo` realm attribute (`realm.setAttribute("logo", ...)`) — so a custom account/login theme can read `realm.getAttribute("logo")` to render it, without needing its own admin UI for the setting.
