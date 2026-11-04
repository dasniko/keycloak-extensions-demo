import { LitElement, html, css, nothing } from "lit";
import { consumeMessages } from "./i18n.js";

/**
 * @typedef {{
 *   label?: string,
 *   labelKey?: string,
 *   href?: string,
 *   children?: NavLink[],
 * }} NavLink
 */

/** Default account console menu (mirrors Keycloak content.json / message keys). */
export const DEFAULT_LINKS = Object.freeze([
  { labelKey: "personalInfo", href: "../account" },
  {
    labelKey: "accountSecurity",
    children: [
      { labelKey: "signingIn", href: "signing-in.html" },
      { labelKey: "deviceActivity", href: "device-activity.html" },
      { labelKey: "linkedAccounts", href: "linked-accounts.html" },
    ],
  },
  { labelKey: "applications", href: "applications.html" },
  { labelKey: "groups", href: "groups.html" },
  { labelKey: "organizations", href: "organizations.html" },
  { labelKey: "resources", href: "resources.html" },
  { labelKey: "verifiableCredentialsTitle", href: "verifiable-credentials.html" },
]);

/**
 * Account console sidebar.
 *
 * Links — use defaults, replace via the `links` property/attribute, call
 * `addLink()`, or project extra items into the `link` slot:
 *
 *   <account-sidebar></account-sidebar>
 *   <account-sidebar .links=${[...DEFAULT_LINKS, { label: "Help", href: "help.html" }]}></account-sidebar>
 *   <account-sidebar links='[{"label":"Help","href":"help.html"}]'></account-sidebar>
 *   <account-sidebar>
 *     <a slot="link" href="help.html">Help</a>
 *   </account-sidebar>
 *
 * Style — override CSS custom properties on the host, or target parts:
 *
 *   account-sidebar {
 *     --account-nav-link-color: #444;
 *     --account-nav-link-active-color: #000;
 *   }
 *   account-sidebar::part(link-active) { font-weight: 700; }
 */
export class NavBar extends LitElement {
  static properties = {
    /** @type {NavLink[] | undefined} */
    links: { type: Array },
  };

  #i18n = consumeMessages(this);

  /** @returns {(key: string, params?: Record<string, string | number>) => string} */
  get #t() {
    return this.#i18n.value?.t ?? ((key) => key);
  }

  /**
   * @param {NavLink} item
   */
  #labelOf(item) {
    if (item.labelKey) {
      return this.#t(item.labelKey);
    }
    return item.label ?? "";
  }

  static styles = css`
    :host {
      --account-nav-bg: transparent;
      --account-nav-fg: inherit;
      --account-nav-link-color: color-mix(
        in srgb,
        currentColor 70%,
        transparent
      );
      --account-nav-link-hover-color: currentColor;
      --account-nav-link-active-color: currentColor;
      --account-nav-link-active-weight: 600;
      --account-nav-gap: 0.25rem;
      --account-nav-indent: 1rem;
      --account-nav-padding: 0;
      --account-nav-font-size: inherit;
      --account-nav-group-label-color: color-mix(
        in srgb,
        currentColor 55%,
        transparent
      );
      --account-nav-group-label-weight: 600;
      --account-nav-group-label-size: 0.75em;
      --account-nav-group-label-transform: uppercase;
      --account-nav-group-label-tracking: 0.04em;

      display: block;
      color: var(--account-nav-fg);
      font-size: var(--account-nav-font-size);
    }

    nav {
      background: var(--account-nav-bg);
      padding: var(--account-nav-padding);
    }

    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: var(--account-nav-gap);
    }

    ul ul {
      padding-inline-start: var(--account-nav-indent);
      margin-block: var(--account-nav-gap);
    }

    .group-label {
      display: block;
      margin: 0.75rem 0 0.25rem;
      color: var(--account-nav-group-label-color);
      font-size: var(--account-nav-group-label-size);
      font-weight: var(--account-nav-group-label-weight);
      letter-spacing: var(--account-nav-group-label-tracking);
      text-transform: var(--account-nav-group-label-transform);
    }

    a {
      color: var(--account-nav-link-color);
      text-decoration: none;
    }

    a:hover {
      color: var(--account-nav-link-hover-color);
    }

    a[aria-current="page"] {
      color: var(--account-nav-link-active-color);
      font-weight: var(--account-nav-link-active-weight);
    }

    [part="extra"] {
      display: flex;
      flex-direction: column;
      gap: var(--account-nav-gap);
      margin-block-start: var(--account-nav-gap);
    }

    ::slotted(a) {
      color: var(--account-nav-link-color);
      text-decoration: none;
    }

    ::slotted(a:hover) {
      color: var(--account-nav-link-hover-color);
    }
  `;

  /**
   * Replace or seed the link list, then append one entry.
   * @param {NavLink} link
   */
  addLink(link) {
    this.links = [...(this.links ?? structuredClone(DEFAULT_LINKS)), link];
  }

  /** @returns {NavLink[]} */
  get resolvedLinks() {
    return this.links ?? DEFAULT_LINKS;
  }

  /**
   * @param {string | undefined} href
   * @returns {boolean}
   */
  #isActive(href) {
    if (!href) {
      return false;
    }
    const path = location.pathname.replace(/\/+$/, "");
    if (href.startsWith("..")) {
      return path.endsWith("account");
    }
    return path.endsWith("/" + href.replace(/^\.\//, ""));
  }

  /**
   * @param {NavLink[]} items
   */
  #renderItems(items) {
    return html`
      <ul part="list">
        ${items.map((item) => this.#renderItem(item))}
      </ul>
      <div part="extra">
        <slot name="link"></slot>
      </div>
    `;
  }

  /**
   * @param {NavLink} item
   */
  #renderItem(item) {
    const label = this.#labelOf(item);

    if (item.children?.length) {
      return html`
        <li part="group">
          <span class="group-label" part="group-label">${label}</span>
          <ul part="list">
            ${item.children.map((child) => this.#renderItem(child))}
          </ul>
        </li>
      `;
    }

    if (!item.href) {
      return nothing;
    }

    const active = this.#isActive(item.href);
    return html`
      <li part="item">
        <a
          part=${active ? "link link-active" : "link"}
          href=${item.href}
          aria-current=${active ? "page" : nothing}
          >${label}</a
        >
      </li>
    `;
  }

  render() {
    return html`<nav part="nav">${this.#renderItems(this.resolvedLinks)}</nav>`;
  }
}

customElements.define("account-sidebar", NavBar);
