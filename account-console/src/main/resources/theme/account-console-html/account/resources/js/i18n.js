import { createContext, ContextConsumer } from "@lit/context";
import { getEnvironment } from "./account-client.js";

/**
 * Shared account-console i18n.
 *
 * `account-shell` loads messages from Keycloak’s theme localization endpoint
 * (`GET /resources/{realm}/account/{locale}`) and provides them via Lit context.
 * Descendants call `consumeMessages(this)` and use `.t(key, params)`.
 *
 *   import { consumeMessages } from "./i18n.js";
 *
 *   class MyPage extends LitElement {
 *     #i18n = consumeMessages(this);
 *     render() {
 *       return html`<h1>${this.#t("personalInfo")}</h1>`;
 *     }
 *     get #t() {
 *       return this.#i18n.value?.t ?? ((key) => key);
 *     }
 *   }
 */

/**
 * @typedef {{
 *   locale: string,
 *   messages: Record<string, string>,
 *   ready: boolean,
 *   t: (key: string, params?: Record<string, string | number>) => string,
 * }} AccountI18n
 */

/** @type {import('@lit/context').Context<AccountI18n>} */
export const messagesContext = createContext(
  Symbol.for("account-console-messages"),
);

/**
 * @param {Record<string, string> | undefined} messages
 * @param {string} key
 * @param {Record<string, string | number>} [params]
 */
export function translate(messages, key, params = {}) {
  if (!key) {
    return "";
  }
  let text = messages?.[key] ?? key;
  // Account-ui Trans tags: <0>Created</0> {{date}}.
  text = text.replace(/<\/?\d+>/g, "");
  for (const [name, value] of Object.entries(params)) {
    const str = String(value);
    text = text.replaceAll(`{{${name}}}`, str);
    text = text.replaceAll(`{${name}}`, str);
  }
  return text;
}

/**
 * @param {Record<string, string>} messages
 * @param {string} locale
 * @returns {AccountI18n}
 */
export function createI18n(messages, locale) {
  return {
    locale,
    messages,
    ready: true,
    t: (key, params) => translate(messages, key, params),
  };
}

/** @returns {AccountI18n} */
export function emptyI18n() {
  return {
    locale: "en",
    messages: {},
    ready: false,
    t: (key, params) => translate({}, key, params),
  };
}

/**
 * Fetch theme messages for a realm/locale.
 * @param {string} serverBaseUrl
 * @param {string} realm
 * @param {string} locale
 * @returns {Promise<Record<string, string>>}
 */
export async function fetchMessages(serverBaseUrl, realm, locale) {
  const base = serverBaseUrl.replace(/\/+$/, "");
  const url = `${base}/resources/${encodeURIComponent(realm)}/account/${encodeURIComponent(locale)}`;
  const response = await fetch(url, { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`Failed to load messages (${response.status})`);
  }
  /** @type {{ key: string, value: string }[]} */
  const entries = await response.json();
  return Object.fromEntries(entries.map(({ key, value }) => [key, value]));
}

/**
 * Load i18n for the current account environment locale.
 * @returns {Promise<AccountI18n>}
 */
export async function loadI18n() {
  const env = await getEnvironment();
  const locale = env.locale || "en";
  const messages = await fetchMessages(env.serverBaseUrl, env.realm, locale);
  return createI18n(messages, locale);
}

/**
 * Subscribe a Lit host to the shell-provided messages context.
 * @param {import('lit').ReactiveControllerHost & HTMLElement} host
 */
export function consumeMessages(host) {
  return new ContextConsumer(host, {
    context: messagesContext,
    subscribe: true,
  });
}
