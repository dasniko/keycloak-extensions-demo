/**
 * Helpers for Keycloak user-profile attribute metadata.
 */

export const ROOT_FIELDS = new Set([
  "username",
  "email",
  "firstName",
  "lastName",
]);

/**
 * @param {string | undefined} annotated
 * @returns {string}
 */
export function resolveInputType(annotated) {
  if (typeof annotated !== "string" || !annotated) {
    return "text";
  }
  if (
    annotated === "textarea" ||
    annotated === "select" ||
    annotated === "multiselect"
  ) {
    return annotated;
  }
  if (annotated.startsWith("html5-")) {
    return annotated.slice("html5-".length);
  }
  if (annotated.startsWith("html")) {
    return annotated.substring("html".length + 2);
  }
  return annotated;
}

/**
 * @param {string} raw
 * @returns {string}
 */
export function displayName(raw) {
  const match = /^\$\{(.+)\}$/.exec(raw);
  if (!match) {
    return raw;
  }
  return match[1]
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/^./, (c) => c.toUpperCase());
}

/**
 * Map a user-profile attribute to primitive `<account-input>` props.
 *
 * @param {any} attribute
 * @returns {{
 *   name: string,
 *   type: string,
 *   required: boolean,
 *   readOnly: boolean,
 *   options: string[],
 *   placeholder: string | undefined,
 *   label: string,
 * }}
 */
export function attributeToInputProps(attribute) {
  const options = attribute?.validators?.options?.options;
  return {
    name: attribute.name,
    type: resolveInputType(attribute?.annotations?.inputType),
    required: Boolean(attribute?.required),
    readOnly: Boolean(attribute?.readOnly),
    options: Array.isArray(options) ? options.map(String) : [],
    placeholder:
      typeof attribute?.annotations?.inputTypePlaceholder === "string"
        ? attribute.annotations.inputTypePlaceholder
        : undefined,
    label: displayName(attribute.displayName || attribute.name),
  };
}
