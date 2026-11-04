import { LitElement, html, css } from "lit";
import { ContextProvider } from "@lit/context";
import {
  emptyI18n,
  loadI18n,
  messagesContext,
} from "../i18n.js";
import "../nav.js";

/**
 * Account console page chrome: sidebar + main content slot.
 *
 * Loads theme messages and provides them via Lit context (`messagesContext`)
 * so descendants can `consumeMessages(this)` and call `.t(key)`.
 *
 *   <account-shell>
 *     <account-personal-info></account-personal-info>
 *   </account-shell>
 *
 * Extra nav links: project into the sidebar via slot="link" on the shell
 * (forwarded), or set `.links` / call `addLink()` on the inner sidebar.
 *
 * Style via `--account-shell-*` / shared tokens, or ::part(layout|nav|main).
 */
export class AccountShell extends LitElement {
  static properties = {
    /** @type {import('../i18n.js').AccountI18n} */
    i18n: { state: true },
    status: { state: true },
  };

  static styles = css`
    :host {
      display: block;
      font-family: var(--account-font, system-ui, sans-serif);
      line-height: 1.5;
      padding: var(--account-shell-padding, 2rem);
    }

    .layout {
      display: grid;
      gap: var(--account-shell-gap, 2rem);
      grid-template-columns: minmax(12rem, var(--account-shell-nav-width, 16rem))
        minmax(0, 1fr);
      align-items: start;
    }

    @media (max-width: 40rem) {
      .layout {
        grid-template-columns: 1fr;
      }
    }

    .main {
      min-width: 0;
    }
  `;

  /** @type {ContextProvider<import('../i18n.js').AccountI18n>} */
  #messagesProvider;

  constructor() {
    super();
    this.i18n = emptyI18n();
    this.status = "loading";
    this.#messagesProvider = new ContextProvider(this, {
      context: messagesContext,
      initialValue: this.i18n,
    });
  }

  connectedCallback() {
    super.connectedCallback();
    this.#loadMessages();
  }

  async #loadMessages() {
    this.status = "loading";
    try {
      this.i18n = await loadI18n();
      this.#messagesProvider.setValue(this.i18n);
    } catch (error) {
      console.warn("Failed to load account messages", error);
      this.i18n = emptyI18n();
      this.#messagesProvider.setValue(this.i18n);
    } finally {
      this.status = "ready";
    }
  }

  render() {
    if (this.status === "loading") {
      return html`<p part="message">Loading…</p>`;
    }

    return html`
      <div class="layout" part="layout">
        <account-sidebar part="nav">
          <slot name="link" slot="link"></slot>
        </account-sidebar>
        <div class="main" part="main">
          <slot></slot>
        </div>
      </div>
    `;
  }
}

customElements.define("account-shell", AccountShell);
