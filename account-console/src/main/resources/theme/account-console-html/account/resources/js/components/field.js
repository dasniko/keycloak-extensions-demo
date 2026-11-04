import { LitElement, html, css, nothing } from "lit";

/**
 * Label + control wrapper for account forms.
 *
 *   <account-field for="email" label="Email" required>
 *     <account-input id="email" name="email" type="email"></account-input>
 *   </account-field>
 *
 * Style via shared tokens or ::part(field|label|control|help|error).
 */
export class AccountField extends LitElement {
  static properties = {
    label: { type: String },
    for: { type: String },
    required: { type: Boolean, reflect: true },
    help: { type: String },
    error: { type: String },
  };

  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    label {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .caption {
      color: var(--account-label-color, currentColor);
      font-size: 0.9rem;
    }

    .help {
      margin: 0;
      font-size: 0.85rem;
      color: var(--account-muted-color, currentColor);
    }

    .error {
      margin: 0;
      font-size: 0.85rem;
      color: var(--account-error-color, #b42318);
    }
  `;

  constructor() {
    super();
    this.label = "";
    this.for = "";
    this.required = false;
    this.help = "";
    this.error = "";
  }

  render() {
    // Wrap the slotted control in <label> so association works across the
    // shadow boundary (label[for] cannot see slotted light-DOM ids).
    return html`
      <div part="field">
        <label part="label">
          ${this.label
            ? html`<span class="caption"
                >${this.label}${this.required ? html` *` : nothing}</span
              >`
            : nothing}
          <span part="control"><slot></slot></span>
        </label>
        ${this.help
          ? html`<p part="help" class="help">${this.help}</p>`
          : nothing}
        ${this.error
          ? html`<p part="error" class="error">${this.error}</p>`
          : nothing}
      </div>
    `;
  }
}

customElements.define("account-field", AccountField);
