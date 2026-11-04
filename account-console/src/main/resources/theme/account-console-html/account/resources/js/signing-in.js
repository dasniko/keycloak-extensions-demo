import { LitElement, html, css, nothing } from "lit";
import { getCredentials, getKeycloak } from "./api.js";
import { consumeMessages } from "./i18n.js";

/**
 * @param {number} epochMs
 * @param {string} locale
 */
function formatDate(epochMs, locale) {
  return new Date(epochMs).toLocaleString(locale || "en", {
    dateStyle: "long",
    timeStyle: "short",
  });
}

/**
 * Signing-in page: credential types grouped by category (mirrors account-ui SigningIn).
 *
 * Labels come from the shell-provided messages context.
 * Create / update / delete redirect through keycloak-js `login({ action })`.
 *
 * Overridable slots: `title`, `description`.
 * Events (bubbles, composed): `load`, cancelable `action`, `error`.
 *
 * Style via shared tokens / `--account-signing-*` or
 * ::part(title|description|category|type|list|item|actions|message|empty).
 */
export class SigningIn extends LitElement {
  static properties = {
    /** @type {any[] | undefined} */
    credentials: { state: true },
    status: { state: true },
    message: { state: true },
  };

  static styles = css`
    :host {
      --account-signing-gap: var(--account-gap, 1rem);
      --account-signing-max-width: 40rem;
      --account-signing-label-color: var(--account-label-color, currentColor);
      --account-signing-muted-color: var(--account-muted-color, currentColor);
      --account-signing-border: var(--account-border-color, currentColor);
      --account-signing-radius: var(--account-radius, 0.35rem);
      --account-signing-error-color: var(--account-error-color, #b42318);
      --account-signing-button-padding: var(
        --account-button-padding,
        0.5rem 1rem
      );
      --account-signing-button-bg: var(
        --account-button-bg,
        light-dark(#1b1b1b, #f0f0f0)
      );
      --account-signing-button-fg: var(
        --account-button-fg,
        light-dark(#fff, #1b1b1b)
      );

      display: block;
      max-width: var(--account-signing-max-width);
    }

    h1 {
      margin: 0 0 0.35rem;
      font-size: 1.5rem;
    }

    h2 {
      margin: 1.5rem 0 0.75rem;
      font-size: 1.25rem;
    }

    h3 {
      margin: 0 0 0.25rem;
      font-size: 1.05rem;
    }

    .description {
      margin: 0 0 var(--account-signing-gap);
      color: var(--account-signing-label-color);
    }

    .type-header {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: flex-start;
      justify-content: space-between;
      margin: 1rem 0 0.75rem;
    }

    .help {
      margin: 0;
      color: var(--account-signing-muted-color);
      font-size: 0.9rem;
    }

    .list {
      list-style: none;
      margin: 0 0 1.25rem;
      padding: 0;
      border: 1px solid var(--account-signing-border);
      border-radius: var(--account-signing-radius);
    }

    .item {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem 1.25rem;
      align-items: center;
      justify-content: space-between;
      padding: 0.85rem 1rem;
      border-top: 1px solid var(--account-signing-border);
    }

    .item:first-child {
      border-top: none;
    }

    .item-main {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      min-width: 0;
      flex: 1 1 12rem;
    }

    .item-label {
      font-weight: 600;
    }

    .item-meta {
      color: var(--account-signing-muted-color);
      font-size: 0.85rem;
    }

    .item-info {
      flex: 1 1 100%;
      margin: 0;
      font-size: 0.9rem;
      color: var(--account-signing-muted-color);
    }

    .item-info p {
      margin: 0.25rem 0 0;
    }

    .item-info dl {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 0.15rem 0.75rem;
      margin: 0.35rem 0 0;
    }

    .item-info dt {
      font-weight: 600;
    }

    .item-info dd {
      margin: 0;
    }

    .warning {
      color: var(--account-signing-error-color);
    }

    .empty {
      margin: 0;
      padding: 0.85rem 1rem;
      color: var(--account-signing-muted-color);
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      align-items: center;
    }

    button {
      font: inherit;
      cursor: pointer;
      border: 1px solid var(--account-signing-border);
      border-radius: var(--account-signing-radius);
      padding: var(--account-signing-button-padding);
      background: transparent;
      color: inherit;
    }

    button.primary {
      border-color: transparent;
      background: var(--account-signing-button-bg);
      color: var(--account-signing-button-fg);
    }

    button.danger {
      border-color: var(--account-signing-error-color);
      color: var(--account-signing-error-color);
    }

    button.link {
      border: none;
      padding: 0;
      background: none;
      color: inherit;
      text-decoration: underline;
      text-underline-offset: 0.15em;
    }

    .message {
      margin: 0;
      font-size: 0.9rem;
    }

    .message[data-tone="error"] {
      color: var(--account-signing-error-color);
    }
  `;

  #i18n = consumeMessages(this);

  constructor() {
    super();
    this.status = "loading";
    this.message = "";
  }

  /** @returns {(key: string, params?: Record<string, string | number>) => string} */
  get #t() {
    return this.#i18n.value?.t ?? ((key) => key);
  }

  get #locale() {
    return this.#i18n.value?.locale || "en";
  }

  /**
   * @param {{ key?: string, parameters?: string[] } | undefined} message
   */
  #msgFromApi(message) {
    if (!message?.key) {
      return "";
    }
    /** @type {Record<string, string>} */
    const params = {};
    (message.parameters ?? []).forEach((value, index) => {
      params[String(index)] = value;
    });
    return this.#t(message.key, params);
  }

  connectedCallback() {
    super.connectedCallback();
    this.#load();
  }

  /**
   * @param {string} type
   * @param {unknown} [detail]
   * @param {{ cancelable?: boolean }} [options]
   */
  #emit(type, detail, options = {}) {
    return this.dispatchEvent(
      new CustomEvent(type, {
        detail,
        bubbles: true,
        composed: true,
        cancelable: Boolean(options.cancelable),
      }),
    );
  }

  async #load() {
    this.status = "loading";
    this.message = "";
    try {
      this.credentials = await getCredentials();
      this.status = "ready";
      this.#emit("load", this.credentials);
    } catch (error) {
      this.status = "error";
      this.message = error instanceof Error ? error.message : String(error);
      this.#emit("error", { phase: "load", error, message: this.message });
    }
  }

  /**
   * @param {string} action
   * @param {{ kind: string, type?: string, credentialId?: string }} meta
   */
  async #runAction(action, meta) {
    if (
      !this.#emit("action", { action, ...meta }, { cancelable: true })
    ) {
      return;
    }
    try {
      const keycloak = await getKeycloak();
      await keycloak.login({ action });
    } catch (error) {
      this.message = error instanceof Error ? error.message : String(error);
      this.#emit("error", { phase: "action", error, message: this.message });
    }
  }

  /** @returns {string[]} */
  #categories() {
    return [...new Set((this.credentials ?? []).map((c) => c.category))];
  }

  /**
   * @param {string} category
   */
  #containersFor(category) {
    return (this.credentials ?? []).filter((c) => c.category === category);
  }

  /**
   * @param {any} meta
   */
  #credentialLabel(meta) {
    const credential = meta?.credential ?? {};
    if (credential.userLabel) {
      return this.#t(credential.userLabel) || credential.userLabel;
    }
    return this.#t(credential.type) || credential.type || "";
  }

  /**
   * @param {any} meta
   */
  #authenticatorProvider(meta) {
    return meta?.infoProperties?.find(
      (p) => p.key === "webauthn-authenticator-provider",
    )?.parameters?.[0];
  }

  /**
   * @param {any} meta
   */
  #infoProperties(meta) {
    return (meta?.infoProperties ?? []).filter(
      (p) => p.key !== "webauthn-authenticator-provider",
    );
  }

  /**
   * @param {any} meta
   */
  #renderCredentialInfo(meta) {
    const infoMessage = this.#msgFromApi(meta.infoMessage);
    const properties = this.#infoProperties(meta);
    const warningTitle = this.#msgFromApi(meta.warningMessageTitle);
    const warningDescription = this.#msgFromApi(meta.warningMessageDescription);
    if (
      !infoMessage &&
      !properties.length &&
      !(warningTitle && warningDescription)
    ) {
      return nothing;
    }

    return html`
      <div class="item-info" part="info">
        ${infoMessage ? html`<p>${infoMessage}</p>` : nothing}
        ${properties.length
          ? html`
              <dl>
                ${properties.map(
                  (prop) => html`
                    <dt>${this.#t(prop.key)}</dt>
                    <dd>${prop.parameters?.[0] ?? ""}</dd>
                  `,
                )}
              </dl>
            `
          : nothing}
        ${warningTitle && warningDescription
          ? html`
              <p class="warning">${warningTitle}</p>
              <p class="warning">${warningDescription}</p>
            `
          : nothing}
      </div>
    `;
  }

  /**
   * @param {any} container
   * @param {any} meta
   */
  #renderCredential(container, meta) {
    const credential = meta.credential ?? {};
    const provider = this.#authenticatorProvider(meta);
    const created =
      credential.createdDate != null
        ? this.#t("credentialCreatedAt", {
            date: formatDate(credential.createdDate, this.#locale),
          })
        : "";

    return html`
      <li class="item" part="item" data-credential-id=${credential.id ?? ""}>
        <div class="item-main">
          <span class="item-label">${this.#credentialLabel(meta)}</span>
          ${provider
            ? html`<span class="item-meta">${provider}</span>`
            : nothing}
          ${created ? html`<span class="item-meta">${created}</span>` : nothing}
        </div>
        ${this.#renderCredentialInfo(meta)}
        <div class="actions" part="actions">
          ${container.removeable && credential.id
            ? html`
                <button
                  class="danger"
                  type="button"
                  data-testrole="remove"
                  @click=${() =>
                    this.#runAction(`delete_credential:${credential.id}`, {
                      kind: "delete",
                      type: container.type,
                      credentialId: credential.id,
                    })}
                >
                  ${this.#t("delete")}
                </button>
              `
            : nothing}
          ${container.updateAction
            ? html`
                <button
                  class="primary"
                  type="button"
                  data-testrole="update"
                  @click=${() =>
                    this.#runAction(container.updateAction, {
                      kind: "update",
                      type: container.type,
                      credentialId: credential.id,
                    })}
                >
                  ${this.#t("update")}
                </button>
              `
            : nothing}
        </div>
      </li>
    `;
  }

  /**
   * @param {any} container
   */
  #renderContainer(container) {
    const displayName = this.#t(container.displayName);
    const helptext = this.#t(container.helptext);
    const metas = container.userCredentialMetadatas ?? [];
    const typeLabel =
      this.#t(`${container.type}-display-name`) || displayName;

    return html`
      <section part="type" data-type=${container.type}>
        <div class="type-header">
          <div>
            <h3 data-testid="${container.type}/title">${displayName}</h3>
            ${helptext
              ? html`<p class="help" data-testid="${container.type}/help-text">
                  ${helptext}
                </p>`
              : nothing}
          </div>
          ${container.createAction
            ? html`
                <button
                  class="link"
                  type="button"
                  data-testid="${container.type}/create"
                  @click=${() =>
                    this.#runAction(container.createAction, {
                      kind: "create",
                      type: container.type,
                    })}
                >
                  ${this.#t("setUpNew", { name: typeLabel })}
                </button>
              `
            : nothing}
        </div>
        <ul
          class="list"
          part="list"
          data-testid="${container.type}/credential-list"
        >
          ${metas.length === 0
            ? html`<li
                class="empty"
                part="empty"
                data-testid="${container.type}/not-set-up"
              >
                ${this.#t("notSetUp", { name: displayName })}
              </li>`
            : metas.map((meta) => this.#renderCredential(container, meta))}
        </ul>
      </section>
    `;
  }

  render() {
    if (this.status === "loading") {
      return html`<p part="message" class="message">Loading credentials…</p>`;
    }

    if (this.status === "error" && !this.credentials) {
      return html`
        <p part="message" class="message" data-tone="error">${this.message}</p>
      `;
    }

    return html`
      <slot name="title">
        <h1 part="title">${this.#t("signingIn")}</h1>
      </slot>
      <slot name="description">
        <p part="description" class="description">
          ${this.#t("signingInDescription")}
        </p>
      </slot>
      ${this.#categories().map(
        (category) => html`
          <section part="category" data-category=${category}>
            <h2 id="${category}-categ-title">${this.#t(category)}</h2>
            ${this.#containersFor(category).map((container) =>
              this.#renderContainer(container),
            )}
          </section>
        `,
      )}
      ${this.message
        ? html`<p part="message" class="message" data-tone="error">
            ${this.message}
          </p>`
        : nothing}
    `;
  }
}

customElements.define("account-signing-in", SigningIn);
