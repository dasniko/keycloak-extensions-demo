# Keycloak Custom Validators

Custom `Validator` implementations for Keycloak's user-profile attribute validation (the `validations` list on a realm's user profile attributes), usable from the declarative user profile configuration without any custom form code.

---

## Phone Number Validator (`phone-number`)

`PhoneNumberValidator` validates that an attribute value is a semantically correct phone number, using Google's [libphonenumber](https://github.com/google/libphonenumber) (`PhoneNumberUtil`) — not just a format/regex check, but actual per-region validity (correct number of digits, valid area/prefix, etc. for the target country).

A blank value is not treated as an error here — pair it with a `length`/`required` validator if the attribute is mandatory.

**Configuration:**

| Property | Description | Default |
|---|---|---|
| Region | ISO 3166-1 alpha-2 country code the number is validated against (e.g. `DE`, `US`, `GB`, `FR`, `AT`, `CH`). | `DE` |

On failure, adds a `error-invalid-phone-number` validation error (see `messages_en.properties` / `messages_de.properties`).

---

## Unique Value Validator (`unique-value`)

`UniqueValueValidator` ensures the attribute's value is unique across **all** users in the realm — e.g. to enforce a unique custom attribute the way Keycloak already enforces unique usernames/emails.

**How it works:** searches all users for the given attribute value (`searchForUserByUserAttributeStream`) and fails validation if any match is found *other than the user currently being validated* (resolved from the `UserModel.class` context attribute, so editing a user's own unchanged value doesn't trigger a false positive).

No configuration. Uses the generic `ValidationError.MESSAGE_INVALID_VALUE` message key rather than a validator-specific one.
