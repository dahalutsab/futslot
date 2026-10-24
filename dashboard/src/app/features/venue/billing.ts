import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { Check, Clock, LucideAngularModule, Sparkles } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { BillingCycle, Invoice, Plan, Subscription } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { methodLabel, rupees } from '../../shared/format';

const ZONE = 'Asia/Kathmandu';

const WALLETS = ['ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'];

/** "7 Oct 2026" for an instant or a date. */
function longDate(value: string): string {
  const date = value.length === 10 ? new Date(`${value}T00:00:00Z`) : new Date(value);
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: value.length === 10 ? 'UTC' : ZONE })
    .format(date);
}

/** "MANUAL_BOOKINGS" reads as "Manual bookings". */
function featureLabel(code: string): string {
  const words = code.toLowerCase().replaceAll('_', ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/**
 * The venue's FutSlot plan, what it costs, and paying for it. FutSlot holds no card details: owners pay
 * FutSlot's wallet or bank account and send the transaction ID, which FutSlot checks. Owner only.
 */
@Component({
  selector: 'fs-billing',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-3xl lg:text-4xl">Plan & billing</h1>
      <p class="mt-1 text-muted">Your FutSlot plan for {{ venueName() }}.</p>

      @if (error(); as message) {
        <div class="mt-6 rounded-md border border-border bg-surface px-6 py-8 text-center">
          <p class="text-muted">{{ message }}</p>
          <button type="button" (click)="retry()" class="mt-3 h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">Try again</button>
        </div>
      } @else if (subscription(); as s) {
        @if (!s.limitsEnforced) {
          <p class="mt-5 flex gap-3 rounded-md border border-border bg-surface px-4 py-3">
            <lucide-icon [img]="icons.Sparkles" [size]="20" class="mt-0.5 shrink-0 text-muted" />
            <span>While FutSlot is launching, every venue gets everything with no limits, whatever the plan.</span>
          </p>
        }

        <!-- Current plan -->
        <section class="mt-5 rounded-md border border-border bg-surface p-4 lg:p-6" aria-labelledby="current-title">
          <p class="text-sm text-muted">Current plan</p>
          <h2 id="current-title" class="mt-0.5 text-2xl font-semibold">{{ s.plan.name }}</h2>
          <p class="mt-1 text-muted">{{ statusLine(s) }}</p>
          <dl class="mt-4 grid grid-cols-2 gap-3 sm:max-w-sm">
            <div class="rounded-md bg-surface-2 px-3 py-2">
              <dt class="text-xs text-muted">Courts</dt>
              <dd class="tabular font-semibold">{{ s.courts }}{{ s.plan.maxCourts != null ? ' of ' + s.plan.maxCourts : '' }}</dd>
            </div>
            <div class="rounded-md bg-surface-2 px-3 py-2">
              <dt class="text-xs text-muted">Staff</dt>
              <dd class="tabular font-semibold">{{ s.staff }}{{ s.plan.maxStaff != null ? ' of ' + s.plan.maxStaff : '' }}</dd>
            </div>
          </dl>
        </section>

        <!-- What's owed -->
        @if (s.openInvoice; as invoice) {
          <section class="mt-5 rounded-md border p-4 lg:p-6" [class]="invoice.status === 'OVERDUE' ? 'border-danger/40 bg-danger-bg' : 'border-border bg-surface'"
            aria-labelledby="invoice-title">
            <div class="flex flex-wrap items-baseline justify-between gap-2">
              <h2 id="invoice-title" class="text-lg font-semibold">{{ invoice.status === 'OVERDUE' ? 'Overdue' : 'To pay' }}: {{ fmt.rupees(invoice.amount) }}</h2>
              <span class="tabular text-sm text-muted">{{ invoice.invoiceNumber }} · due {{ fmt.longDate(invoice.dueOn) }}</span>
            </div>
            <p class="mt-1 text-sm text-muted">{{ invoice.planName }}, {{ fmt.longDate(invoice.periodStart) }} to {{ fmt.longDate(invoice.periodEnd) }}</p>
            @if (invoice.paymentPending || claimed()) {
              <p class="mt-4 flex items-center gap-2 rounded-md bg-surface-2 px-3 py-2" role="status">
                <lucide-icon [img]="icons.Clock" [size]="18" class="shrink-0 text-muted" />Payment sent. FutSlot will confirm it soon and let you know.
              </p>
            } @else {
              <div class="mt-4 rounded-md bg-surface-2 px-3 py-2">
                <p class="text-sm font-medium text-muted">Pay FutSlot</p>
                <p class="mt-0.5 whitespace-pre-line">{{ s.paymentInstructions || 'Contact FutSlot for payment details.' }}</p>
              </div>
              <form class="mt-4 grid gap-3 sm:grid-cols-[auto_1fr_auto] sm:items-end" (submit)="$event.preventDefault(); claim(invoice)">
                <div>
                  <label for="claim-method" class="mb-1.5 block text-sm font-medium">Paid with</label>
                  <select id="claim-method" (change)="method.set($any($event.target).value)" class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base">
                    @for (wallet of wallets; track wallet) {
                      <option [value]="wallet" [selected]="method() === wallet">{{ fmt.methodLabel(wallet) }}</option>
                    }
                  </select>
                </div>
                <div>
                  <label for="claim-id" class="mb-1.5 block text-sm font-medium">Transaction ID</label>
                  <input id="claim-id" type="text" maxlength="100" autocomplete="off" [value]="transactionId()" (input)="transactionId.set($any($event.target).value)"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
                <button type="submit" [disabled]="busy()" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">I've paid {{ fmt.rupees(invoice.amount) }}</button>
              </form>
              @if (claimError(); as message) {
                <p class="mt-2 text-sm text-danger" role="alert">{{ message }}</p>
              }
            }
          </section>
        }

        <!-- Plans -->
        <section class="mt-8" aria-labelledby="plans-title">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <h2 id="plans-title" class="text-lg font-semibold">Plans</h2>
            @if (offersYearly()) {
              <div class="flex rounded-md bg-surface-2 p-1" role="radiogroup" aria-label="Billing">
                @for (option of cycles; track option.value) {
                  <button type="button" role="radio" [attr.aria-checked]="cycle() === option.value" (click)="cycle.set(option.value)"
                    class="h-9 rounded-md px-3 text-sm font-medium text-muted" [class.bg-surface]="cycle() === option.value" [class.text-text]="cycle() === option.value">
                    {{ option.label }}
                  </button>
                }
              </div>
            }
          </div>
          <ul class="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (plan of plans(); track plan.id) {
              @let current = isCurrent(s, plan);
              <li class="flex flex-col rounded-md border bg-surface p-4" [class]="current ? 'border-ink' : 'border-border'">
                <h3 class="font-semibold">{{ plan.name }}</h3>
                <p class="mt-1">
                  @if (priceOf(plan) === null) {
                    <span class="text-muted">Monthly only</span>
                  } @else if (priceOf(plan) === 0) {
                    <span class="text-2xl font-semibold">Free</span>
                  } @else {
                    <span class="tabular text-2xl font-semibold">{{ fmt.rupees(priceOf(plan)) }}</span>
                    <span class="text-muted"> / {{ cycle() === 'YEARLY' ? 'year' : 'month' }}</span>
                  }
                </p>
                @if (plan.description) {
                  <p class="mt-2 text-sm text-muted">{{ plan.description }}</p>
                }
                <ul class="mt-3 flex-1 space-y-1.5 text-sm">
                  <li class="flex gap-2"><lucide-icon [img]="icons.Check" [size]="16" class="mt-0.5 shrink-0 text-muted" />{{ limits(plan) }}</li>
                  @for (feature of plan.features; track feature) {
                    <li class="flex gap-2"><lucide-icon [img]="icons.Check" [size]="16" class="mt-0.5 shrink-0 text-muted" />{{ fmt.featureLabel(feature) }}</li>
                  }
                </ul>
                @if (current && (s.nextPlan || s.cancelAtPeriodEnd)) {
                  <button type="button" (click)="choose(plan)" [disabled]="busy()" class="mt-4 h-11 rounded-md border border-border font-medium hover:bg-surface-2 disabled:opacity-60">
                    Stay on {{ plan.name }}
                  </button>
                } @else if (current) {
                  <p class="mt-4 flex h-11 items-center justify-center rounded-md bg-surface-2 font-medium text-muted">Current plan</p>
                } @else if (isNext(s, plan)) {
                  <p class="mt-4 flex h-11 items-center justify-center rounded-md bg-surface-2 text-sm font-medium text-muted">Starts {{ s.currentPeriodEnd ? fmt.longDate(s.currentPeriodEnd) : 'at renewal' }}</p>
                } @else if (confirming() === plan.code) {
                  <div class="mt-4 rounded-md bg-surface-2 p-3">
                    <p class="text-sm">{{ consequence(s, plan) }}</p>
                    <div class="mt-3 flex gap-2">
                      <button type="button" (click)="choose(plan)" [disabled]="busy() || priceOf(plan) === null" class="h-10 flex-1 rounded-md bg-ink px-3 text-sm font-medium text-ink-fg disabled:opacity-60">Confirm</button>
                      <button type="button" (click)="confirming.set(null)" class="h-10 rounded-md border border-border px-3 text-sm font-medium">Back</button>
                    </div>
                  </div>
                } @else {
                  <button type="button" (click)="confirming.set(plan.code)" [disabled]="priceOf(plan) === null"
                    class="mt-4 h-11 rounded-md border border-border font-medium hover:bg-surface-2 disabled:opacity-50">
                    Switch to {{ plan.name }}
                  </button>
                }
              </li>
            }
          </ul>
          @if (chooseError(); as message) {
            <p class="mt-3 text-sm text-danger" role="alert">{{ message }}</p>
          }
        </section>

        <!-- History -->
        @if (invoices().length > 0) {
          <section class="mt-8" aria-labelledby="invoices-title">
            <h2 id="invoices-title" class="text-lg font-semibold">Invoices</h2>
            <ul class="mt-3 divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
              @for (invoice of invoices(); track invoice.id) {
                <li class="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3">
                  <span class="tabular whitespace-nowrap font-medium">{{ invoice.invoiceNumber }}</span>
                  <span class="flex-1 text-sm text-muted">{{ invoice.planName }} · {{ fmt.longDate(invoice.periodStart) }} to {{ fmt.longDate(invoice.periodEnd) }}</span>
                  <span class="tabular font-medium">{{ fmt.rupees(invoice.amount) }}</span>
                  <span class="w-40 text-right text-sm" [class]="invoice.status === 'OVERDUE' ? 'text-danger' : 'text-muted'">{{ invoiceState(invoice) }}</span>
                </li>
              }
            </ul>
          </section>
        }
      } @else {
        <div class="mt-6 h-64 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Billing {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { Check, Clock, Sparkles };

  protected readonly fmt = { featureLabel, longDate, methodLabel, rupees };

  protected readonly wallets = WALLETS;

  protected readonly cycles: { value: BillingCycle; label: string }[] = [
    { value: 'MONTHLY', label: 'Monthly' },
    { value: 'YEARLY', label: 'Yearly' },
  ];

  protected readonly subscription = signal<Subscription | null>(null);

  protected readonly plans = signal<Plan[]>([]);

  protected readonly invoices = signal<Invoice[]>([]);

  protected readonly error = signal<string | null>(null);

  protected readonly cycle = signal<BillingCycle>('MONTHLY');

  protected readonly confirming = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly chooseError = signal<string | null>(null);

  protected readonly method = signal('ESEWA');

  protected readonly transactionId = signal('');

  protected readonly claimError = signal<string | null>(null);

  /** Set right after sending a payment, before the next load says so. */
  protected readonly claimed = signal(false);

  protected readonly offersYearly = computed(() => this.plans().some((plan) => plan.yearlyPrice !== null));

  protected readonly venueName = computed(() => this.venues.current()?.venue.name ?? 'your venue');

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected priceOf(plan: Plan): number | null {
    return this.cycle() === 'YEARLY' && plan.monthlyPrice > 0 ? plan.yearlyPrice : plan.monthlyPrice;
  }

  protected limits(plan: Plan): string {
    const courts = plan.maxCourts == null ? 'Any number of courts' : `${plan.maxCourts} ${plan.maxCourts === 1 ? 'court' : 'courts'}`;
    const staff = plan.maxStaff == null ? 'any number of staff' : plan.maxStaff === 0 ? 'no staff accounts' : `${plan.maxStaff} staff`;
    return `${courts}, ${staff}`;
  }

  protected isCurrent(s: Subscription, plan: Plan): boolean {
    return s.plan.code === plan.code && (plan.monthlyPrice === 0 || (s.billingCycle ?? 'MONTHLY') === this.cycle());
  }

  /** The plan the venue moves to when the current period ends. */
  protected isNext(s: Subscription, plan: Plan): boolean {
    return s.nextPlan ? s.nextPlan.code === plan.code && (s.nextBillingCycle ?? 'MONTHLY') === this.cycle()
      : s.cancelAtPeriodEnd && plan.monthlyPrice === 0;
  }

  protected statusLine(s: Subscription): string {
    const end = s.currentPeriodEnd ? longDate(s.currentPeriodEnd) : null;
    switch (s.status) {
      case null:
      case 'CANCELLED':
      case 'EXPIRED':
        return 'No payment needed.';
      case 'PAST_DUE':
        return s.openInvoice ? 'Waiting for your payment.' : 'Waiting for payment.';
      default:
        if (s.cancelAtPeriodEnd && end) {
          return `Paid until ${end}, then back to Free.`;
        }
        if (s.nextPlan && end) {
          return `Paid until ${end}, then ${s.nextPlan.name} (${(s.nextBillingCycle ?? 'MONTHLY').toLowerCase()}).`;
        }
        return end ? `${this.cycleWord(s.billingCycle)} plan, renews on ${end}.` : '';
    }
  }

  /** What choosing a plan will do, before the owner confirms. */
  protected consequence(s: Subscription, plan: Plan): string {
    const end = s.currentPeriodEnd ? longDate(s.currentPeriodEnd) : 'the end of this period';
    const paidUp = s.status === 'ACTIVE';
    if (plan.monthlyPrice === 0) {
      return paidUp ? `You keep ${s.plan.name} until ${end}, then move to Free. Nothing more to pay.` : 'You move to Free now. Nothing to pay.';
    }
    const price = `${rupees(this.priceOf(plan))} a ${this.cycle() === 'YEARLY' ? 'year' : 'month'}`;
    return paidUp ? `${plan.name} (${price}) starts when your current period ends on ${end}.`
      : `You get an invoice for ${plan.name} (${price}). It starts once FutSlot confirms your payment.`;
  }

  protected invoiceState(invoice: Invoice): string {
    switch (invoice.status) {
      case 'PAID':
        return invoice.paidAt ? `Paid ${longDate(invoice.paidAt)}` : 'Paid';
      case 'VOID':
        return 'Cancelled';
      case 'OVERDUE':
        return invoice.paymentPending ? 'Payment being checked' : 'Overdue';
      default:
        return invoice.paymentPending ? 'Payment being checked' : `Due ${longDate(invoice.dueOn)}`;
    }
  }

  protected async choose(plan: Plan): Promise<void> {
    const id = this.venueId();
    if (!id || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.chooseError.set(null);
    try {
      this.subscription.set(await firstValueFrom(this.http.put<Subscription>(`/api/v1/owner/venues/${id}/subscription`,
        { planCode: plan.code, billingCycle: this.cycle() })));
      this.confirming.set(null);
      this.claimed.set(false);
      this.invoices.set(await firstValueFrom(this.http.get<Invoice[]>(`/api/v1/owner/venues/${id}/invoices`)));
    }
    catch (error) {
      this.chooseError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async claim(invoice: Invoice): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (this.transactionId().trim().length < 4) {
      this.claimError.set('Enter the transaction ID from your payment receipt.');
      return;
    }
    this.busy.set(true);
    this.claimError.set(null);
    try {
      await firstValueFrom(this.http.post(`/api/v1/invoices/${invoice.id}/payments`,
        { method: this.method(), transactionId: this.transactionId().trim(), amount: invoice.amount }));
      this.claimed.set(true);
      this.transactionId.set('');
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'PAYMENT_PENDING') {
        this.claimed.set(true);
      }
      else {
        this.claimError.set(problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  protected retry(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id);
    }
  }

  private cycleWord(cycle: BillingCycle | null): string {
    return cycle === 'YEARLY' ? 'Yearly' : 'Monthly';
  }

  private async load(id: string): Promise<void> {
    try {
      const [subscription, plans, invoices] = await Promise.all([
        firstValueFrom(this.http.get<Subscription>(`/api/v1/owner/venues/${id}/subscription`)),
        firstValueFrom(this.http.get<Plan[]>('/api/v1/subscription-plans')),
        firstValueFrom(this.http.get<Invoice[]>(`/api/v1/owner/venues/${id}/invoices`)),
      ]);
      this.subscription.set(subscription);
      this.plans.set([...plans].sort((a, b) => a.monthlyPrice - b.monthlyPrice));
      this.invoices.set(invoices);
      this.cycle.set(subscription.billingCycle ?? 'MONTHLY');
      this.claimed.set(false);
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
