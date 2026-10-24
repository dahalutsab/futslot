import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MembershipPlan, PaymentClaim, VenueDetail } from '../../core/api.types';
import { Session } from '../../core/session';
import { l } from '../../i18n/i18n';
import { rupees } from '../../shared/format';
import { planTerms } from '../../shared/labels';
import { PayForm } from '../../shared/pay-form';

/** A venue's game packs and passes, bought by paying the venue and reporting it. */
@Component({
  selector: 'fs-venue-plans',
  imports: [PayForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (plans().length) {
      <section class="mt-10" aria-labelledby="plans-title">
        <h2 id="plans-title" class="text-lg font-semibold">{{ l().memberships }}</h2>
        <p class="text-muted">{{ l().membershipsPitch }}</p>
        <ul class="mt-3 grid gap-3 sm:grid-cols-2">
          @for (plan of plans(); track plan.id) {
            <li class="rounded-md border border-border bg-surface p-4" [class.sm:col-span-2]="buying() === plan.id">
              <div class="flex items-start gap-3">
                <div class="min-w-0 flex-1">
                  <p class="font-semibold">{{ plan.name }}</p>
                  <p class="text-sm text-muted">{{ terms(plan, plan.validityDays) }}</p>
                </div>
                <p class="tabular font-semibold">{{ plan.price > 0 ? rupees(plan.price) : l().free }}</p>
              </div>
              @if (plan.description) {
                <p class="mt-2 text-sm text-muted">{{ plan.description }}</p>
              }
              @if (buying() === plan.id) {
                <div class="mt-4">
                  @if (plan.price > 0) {
                    <fs-pay-form [id]="'plan-' + plan.id" [amount]="plan.price" [instructions]="venue().paymentInstructions"
                      [busy]="busy()" [error]="error()" [label]="l().iPaid" (paidWith)="buy(plan, $event)" />
                    <p class="mt-2 text-sm text-subtle">{{ l().membershipChecks(venue().name) }}</p>
                  } @else {
                    @if (error(); as message) {
                      <p class="mb-2 text-danger" role="alert">{{ message }}</p>
                    }
                    <button type="button" (click)="buy(plan, null)" [disabled]="busy()" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ l().getIt }}</button>
                  }
                  <button type="button" (click)="buying.set(null)" class="mt-2 h-11 px-1 text-sm font-medium text-muted hover:text-text">{{ l().notNow }}</button>
                </div>
              } @else {
                <button type="button" (click)="start(plan)" class="mt-3 h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">{{ l().buy }}</button>
              }
            </li>
          }
        </ul>
      </section>
    }
  `,
})
export class VenuePlans {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly session = inject(Session);

  readonly venue = input.required<VenueDetail>();

  protected readonly rupees = rupees;

  protected readonly terms = planTerms;

  protected readonly plans = signal<MembershipPlan[]>([]);

  protected readonly buying = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const slug = this.venue().slug;
      untracked(() => this.api.membershipPlans(slug).then((plans) => this.plans.set(plans.filter((p) => p.active)), () => undefined));
    });
  }

  protected start(plan: MembershipPlan): void {
    if (!this.session.signedIn()) {
      void this.router.navigate(['/login'], { queryParams: { next: this.router.url } });
      return;
    }
    this.error.set(null);
    this.buying.set(plan.id);
  }

  protected async buy(plan: MembershipPlan, claim: PaymentClaim | null): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.buyMembership(plan.id, claim);
      await this.router.navigate(['/memberships']);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }
}
