import { LitElement, html, css, nothing } from "lit";

/**
 * Primitive form control (form-associated).
 *
 *   <account-input name="email" type="email" value="a@b.c" required></account-input>
 *   <account-input name="bio" type="textarea"></account-input>
 *   <account-input name="locale" type="select" .options=${["en","nl"]}></account-input>
 *
 * Style via shared tokens / `--account-input-*` or ::part(input).
 */
export class AccountInput extends LitElement {
  static formAssociated = true;

  static properties = {
    name: { type: String, reflect: true },
    type: { type: String, reflect: true },
    value: { type: String },
    /** @type {string[]} */
    values: { attribute: false },
    /** @type {string[]} */
    options: { attribute: false },
    required: { type: Boolean, reflect: true },
    disabled: { type: Boolean, reflect: true },
    readOnly: { type: Boolean, reflect: true, attribute: "readonly" },
    placeholder: { type: String },
  };

  static styles = css`
    :host {
      --account-input-border: var(--account-border-color, currentColor);
      --account-input-radius: var(--account-radius, 0.35rem);
      --account-input-padding: 0.45rem 0.6rem;

      display: block;
    }

    input,
    select,
    textarea {
      display: block;
      width: 100%;
      box-sizing: border-box;
      font: inherit;
      padding: var(--account-input-padding);
      border: 1px solid var(--account-input-border);
      border-radius: var(--account-input-radius);
      background: transparent;
      color: inherit;
    }

    textarea {
      min-height: 5rem;
      resize: vertical;
    }

    input:disabled,
    select:disabled,
    textarea:disabled {
      opacity: 0.65;
    }
  `;

  /** @type {ElementInternals} */
  #internals;

  constructor() {
    super();
    this.#internals = this.attachInternals();
    this.type = "text";
    this.value = "";
    this.values = [];
    this.options = [];
    this.required = false;
    this.disabled = false;
    this.readOnly = false;
    this.placeholder = "";
  }

  createRenderRoot() {
    return this.attachShadow({ mode: "open", delegatesFocus: true });
  }

  /** @returns {string[]} */
  get #selectedValues() {
    if (this.values?.length) {
      return this.values.map(String);
    }
    return this.value ? [this.value] : [];
  }

  #syncFormValue() {
    const name = this.name;
    if (!name) {
      this.#internals.setFormValue(null);
      return;
    }
    const selected = this.#selectedValues.filter((v) => v !== "");
    if (selected.length > 1) {
      const data = new FormData();
      for (const entry of selected) {
        data.append(name, entry);
      }
      this.#internals.setFormValue(data);
      return;
    }
    this.#internals.setFormValue(selected[0] ?? "");
  }

  /**
   * @param {Event} event
   */
  #onInput(event) {
    const target = event.target;
    if (target instanceof HTMLSelectElement && target.multiple) {
      this.values = [...target.selectedOptions].map((option) => option.value);
      this.value = this.values[0] ?? "";
    } else if (
      target instanceof HTMLInputElement ||
      target instanceof HTMLSelectElement ||
      target instanceof HTMLTextAreaElement
    ) {
      this.value = target.value;
      this.values = target.value ? [target.value] : [];
    }
    this.#syncFormValue();
  }

  updated() {
    this.#syncFormValue();
  }

  render() {
    const name = this.name ?? "";
    const inputType = this.type || "text";
    const options = Array.isArray(this.options) ? this.options : [];
    const blocked = this.disabled || this.readOnly;
    const value = this.#selectedValues[0] ?? "";
    const values = new Set(this.#selectedValues);

    if (inputType === "textarea") {
      return html`
        <textarea
          part="input"
          id=${name}
          name=${name}
          ?required=${this.required}
          ?disabled=${blocked}
          .value=${value}
          @input=${this.#onInput}
        ></textarea>
      `;
    }

    if (
      inputType === "select" ||
      inputType === "multiselect" ||
      options.length
    ) {
      const multiple = inputType === "multiselect";
      return html`
        <select
          part="input"
          id=${name}
          name=${name}
          ?required=${this.required}
          ?disabled=${blocked}
          ?multiple=${multiple}
          @change=${this.#onInput}
        >
          ${!this.required && !multiple
            ? html`<option value=""></option>`
            : nothing}
          ${options.map(
            (option) => html`
              <option value=${option} ?selected=${values.has(option)}>
                ${option}
              </option>
            `,
          )}
        </select>
      `;
    }

    return html`
      <input
        part="input"
        id=${name}
        name=${name}
        type=${inputType}
        .value=${value}
        ?required=${this.required}
        ?disabled=${blocked}
        placeholder=${this.placeholder || nothing}
        @input=${this.#onInput}
      />
    `;
  }
}

customElements.define("account-input", AccountInput);
