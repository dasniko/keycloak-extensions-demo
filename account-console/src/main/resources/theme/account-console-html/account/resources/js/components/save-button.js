import { LitElement, html, css } from "lit";

/**
 * Submit button with a built-in saving state.
 *
 *   <account-save-button></account-save-button>
 *   <account-save-button saving></account-save-button>
 *   <account-save-button label="Update" saving-label="Updating…"></account-save-button>
 *
 * Style via shared tokens / `--account-save-*` or ::part(button).
 */
export class SaveButton extends LitElement {
  static properties = {
    saving: { type: Boolean, reflect: true },
    disabled: { type: Boolean, reflect: true },
    label: { type: String },
    savingLabel: { type: String, attribute: "saving-label" },
  };

  static styles = css`
    :host {
      --account-save-radius: var(--account-radius, 0.35rem);
      --account-save-padding: var(--account-button-padding, 0.5rem 1rem);
      --account-save-bg: var(--account-button-bg, light-dark(#1b1b1b, #f0f0f0));
      --account-save-fg: var(--account-button-fg, light-dark(#fff, #1b1b1b));

      display: inline-block;
    }

    button {
      font: inherit;
      cursor: pointer;
      border: none;
      border-radius: var(--account-save-radius);
      padding: var(--account-save-padding);
      background: var(--account-save-bg);
      color: var(--account-save-fg);
    }

    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `;

  constructor() {
    super();
    this.saving = false;
    this.disabled = false;
    this.label = "Save";
    this.savingLabel = "Saving…";
  }

  /**
   * Shadow-DOM buttons are not form-associated; submit the nearest form instead.
   */
  #onClick() {
    if (this.disabled || this.saving) {
      return;
    }
    this.closest("form")?.requestSubmit();
  }

  render() {
    return html`
      <button
        part="button"
        type="button"
        ?disabled=${this.disabled || this.saving}
        @click=${this.#onClick}
      >
        ${this.saving ? this.savingLabel : this.label}
      </button>
    `;
  }
}

customElements.define("account-save-button", SaveButton);
