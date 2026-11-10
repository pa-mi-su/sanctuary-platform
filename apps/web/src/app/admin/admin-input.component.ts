import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';

@Component({
  selector: 'admin-input',
  standalone: true,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => AdminInputComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => AdminInputComponent), multi: true },
  ],
  template: ` <label [for]="inputId"
      >{{ label }}
      @if (required) {
        <span aria-hidden="true">*</span>
      }
    </label>
    <div class="input-wrap">
      <input
        #field
        [id]="inputId"
        [type]="type === 'password' && visible ? 'text' : type"
        [value]="value"
        [disabled]="disabled"
        [readOnly]="readonly"
        [required]="required"
        [attr.autocomplete]="autocomplete"
        [attr.inputmode]="inputmode || null"
        [attr.maxlength]="maxlength"
        [attr.minlength]="minlength || null"
        [attr.placeholder]="placeholder || null"
        [attr.aria-describedby]="description || null"
        (input)="update($event)"
        (blur)="touched()"
      />
      <div class="actions">
        @if (value && !readonly && !disabled) {
          <button
            type="button"
            class="clear"
            [attr.aria-label]="'Clear ' + label"
            (click)="clear()"
          >
            ×
          </button>
        }
        @if (type === 'password') {
          <button
            type="button"
            [disabled]="disabled"
            [attr.aria-label]="(visible ? 'Hide ' : 'Show ') + label"
            [attr.aria-pressed]="visible"
            (click)="visible = !visible"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
              <circle cx="12" cy="12" r="3" />
              @if (visible) {
                <path d="m3 3 18 18" />
              }
            </svg>
          </button>
        }
      </div>
    </div>`,
  styles: `
    :host {
      display: block;
    }
    label {
      display: block;
      font-size: 13px;
      font-weight: 650;
      margin-bottom: 8px;
      color: var(--admin-label, #d9edf2);
    }
    label span {
      color: #f0cd7b;
    }
    .input-wrap {
      position: relative;
    }
    input {
      box-sizing: border-box;
      width: 100%;
      padding: 13px 88px 13px 14px;
      border: 1px solid var(--admin-border, #416373);
      border-radius: 10px;
      background: var(--admin-input, #12384b);
      color: var(--admin-text, #edf6f7);
      font: inherit;
      min-height: 48px;
    }
    input:read-only {
      opacity: 0.65;
    }
    input:focus-visible,
    button:focus-visible {
      outline: 2px solid #e7c575;
      outline-offset: 3px;
    }
    .actions {
      position: absolute;
      right: 8px;
      top: 50%;
      transform: translateY(-50%);
      display: flex;
      align-items: center;
      gap: 2px;
    }
    button {
      display: grid;
      place-items: center;
      background: transparent;
      border: 0;
      border-radius: 7px;
      color: var(--admin-muted, #b2cbd5);
      padding: 7px;
      cursor: pointer;
      min-width: 32px;
      min-height: 32px;
    }
    button:hover {
      background: #ffffff12;
      color: #fff;
    }
    .clear {
      font-size: 23px;
      line-height: 1;
    }
    svg {
      width: 20px;
      height: 20px;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.7;
    }
  `,
})
export class AdminInputComponent implements ControlValueAccessor, Validator {
  @Input() label = '';
  @Input() inputId = '';
  @Input() type = 'text';
  @Input() autocomplete = 'off';
  @Input() inputmode = '';
  @Input() placeholder = '';
  @Input() description = '';
  @Input() required = false;
  @Input() readonly = false;
  @Input() minlength = 0;
  @Input() maxlength = 100000;
  @Output() changed = new EventEmitter<string>();
  @ViewChild('field') field?: ElementRef<HTMLInputElement>;
  value = '';
  visible = false;
  disabled = false;
  private onChange: (value: string) => void = () => {};
  touched = () => {};
  writeValue(value: unknown) {
    this.value = value == null ? '' : String(value);
  }
  registerOnChange(fn: (value: string) => void) {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(disabled: boolean) {
    this.disabled = disabled;
  }
  update(event: Event) {
    this.set((event.target as HTMLInputElement).value);
  }
  private set(value: string) {
    this.value = value;
    this.onChange(value);
    this.changed.emit(value);
  }
  clear() {
    this.set('');
    this.touched();
    this.field?.nativeElement.focus();
  }
  validate(control: AbstractControl): ValidationErrors | null {
    if (this.required && !control.value) return { required: true };
    if (this.type === 'email' && control.value && Validators.email(control)) return { email: true };
    if (control.value && String(control.value).length < this.minlength) return { minlength: true };
    if (control.value && String(control.value).length > this.maxlength) return { maxlength: true };
    return null;
  }
}
