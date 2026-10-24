import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';

import { PaymentClaim, PaymentMethod } from '../core/api.types';
import { methodLabel, rupees } from './format';
import { l } from '../i18n/i18n';

/** The methods a player can report having paid with (cash is taken at the counter, not reported). */
export const CLAIMABLE: PaymentMethod[] = ['ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'];

/**
 * "I paid": where to send the money, then the method and the transaction ID from the receipt. The amount is
 * fixed unless {@link minimum} is set, when the player may pay more (up to the total).
 */
@Component({
  selector: 'fs-pay-form',
  imports: [FormsModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (instructions()) {
      <div class="rounded-md bg-surface-2 px-3 py-2">
        @let least = minimum();
        <p class="text-sm text-muted">{{ least !== null ? l().sendAtLeastTo(rupees(least)) : l().sendAmountTo(rupees(amount())) }}</p>
        <p class="mt-0.5 whitespace-pre-line font-medium">{{ instructions() }}</p>
      </div>
    }
    <form class="mt-3 space-y-3" (ngSubmit)="submit()">
      <fieldset>
        <legend class="mb-1.5 font-medium">{{ l().paidWith }}</legend>
        <div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
          @for (m of methods(); track m) {
            <label class="flex h-11 cursor-pointer items-center justify-center rounded-md border text-sm font-medium has-[:checked]:border-ink has-[:checked]:bg-surface-2 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-ring"
              [class]="method() === m ? '' : 'border-border text-muted'">
              <input type="radio" class="sr-only" [name]="id() + '-method'" [value]="m" [checked]="method() === m" (change)="method.set(m)" />
              {{ methodLabel(m) }}
            </label>
          }
        </div>
      </fieldset>
      @if (minimum() !== null) {
        <div>
          <label [for]="id() + '-amount'" class="mb-1.5 block font-medium">{{ l().amountYouSent }}</label>
          <input [id]="id() + '-amount'" name="amount" type="number" inputmode="decimal" [min]="minimum()" [max]="amount()"
            class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base"
            [ngModel]="paid()" (ngModelChange)="paid.set(+$event)" required />
        </div>
      }
      <div>
        <label [for]="id() + '-txn'" class="mb-1.5 block font-medium">{{ l().transactionId }}</label>
        <input [id]="id() + '-txn'" name="transactionId" type="text" maxlength="100" autocomplete="off" [placeholder]="l().fromYourReceipt"
          class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base"
          [ngModel]="transactionId()" (ngModelChange)="transactionId.set($event)" required />
      </div>
      @if (problem(); as message) {
        <p class="text-danger" role="alert">{{ message }}</p>
      }
      <button type="submit" [disabled]="busy()"
        class="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
        @if (busy()) {
          <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" /> {{ l().sending }}
        } @else {
          {{ label() ?? l().sendForChecking }}
        }
      </button>
    </form>
  `,
})
export class PayForm {
  protected readonly l = l;

  /** Keeps field ids unique when two forms share a page. */
  readonly id = input('pay');

  /** What to pay: exact, or the most when {@link minimum} is set. */
  readonly amount = input.required<number>();

  readonly minimum = input<number | null>(null);

  readonly instructions = input<string | null>(null);

  readonly methods = input<PaymentMethod[]>(CLAIMABLE);

  /** The button's words; "Send for checking" if not given. */
  readonly label = input<string | null>(null);

  readonly busy = input(false);

  readonly error = input<string | null>(null);

  readonly paidWith = output<PaymentClaim>();

  protected readonly icons = { LoaderCircle };

  protected readonly rupees = rupees;

  protected readonly methodLabel = methodLabel;

  protected readonly method = signal<PaymentMethod>('ESEWA');

  protected readonly transactionId = signal('');

  protected readonly paid = signal(0);

  private readonly local = signal<string | null>(null);

  protected readonly problem = signal<string | null>(null);

  constructor() {
    effect(() => this.paid.set(this.minimum() ?? this.amount()));
    effect(() => this.problem.set(this.error() ?? this.local()));
  }

  protected submit(): void {
    if (this.busy()) {
      return;
    }
    const transactionId = this.transactionId().trim();
    const minimum = this.minimum();
    if (transactionId.length < 4) {
      this.local.set(l().enterTransactionId);
      return;
    }
    if (minimum !== null && (!(this.paid() >= minimum) || this.paid() > this.amount())) {
      this.local.set(l().enterBetween(rupees(minimum), rupees(this.amount())));
      return;
    }
    this.local.set(null);
    this.paidWith.emit({ method: this.method(), transactionId, amount: minimum !== null ? this.paid() : this.amount() });
  }
}
