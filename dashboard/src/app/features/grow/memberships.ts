import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LoaderCircle, LucideAngularModule, Plus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Membership, MembershipPlan, MembershipStatus, Page } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { dayLabel, methodLabel, phone, rupees, today } from '../../shared/format';
import { Sheet } from '../../shared/sheet';
import { DiscountCodes } from './discount-codes';
import { MembershipPlans, planSummary } from './membership-plans';

type Tab = 'members' | 'plans' | 'codes';

/** "Mon 4 Jan", with the year when it isn't this year: "Mon 4 Jan 2027". */
function withYear(day: string): string {
  return day.slice(0, 4) === today('Asia/Kathmandu').slice(0, 4) ? dayLabel(day) : `${dayLabel(day)} ${day.slice(0, 4)}`;
}

interface Selling {
  planId: string;
  phone: string;
  name: string;
  method: string;
  transactionId: string;
}

/**
 * Memberships people have bought, the plans on sale, and discount codes. Counter staff can sell a
 * membership; plans and codes need venue settings access.
 */
@Component({
  selector: 'fs-memberships',
  imports: [DiscountCodes, LucideAngularModule, MembershipPlans, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-xl font-semibold tracking-tight lg:text-2xl">Memberships & codes</h1>
      <div class="mt-4 flex gap-1 border-b border-border" role="tablist">
        @for (option of tabs(); track option.value) {
          <button type="button" role="tab" [attr.aria-selected]="tab() === option.value" (click)="go(option.value)"
            class="-mb-px h-11 border-b-2 px-3 font-medium" [class]="tab() === option.value ? 'border-ink text-text' : 'border-transparent text-muted hover:text-text'">
            {{ option.label }}
          </button>
        }
      </div>

      <div class="mt-5" role="tabpanel">
        @if (venueId(); as id) {
          @switch (tab()) {
            @case ('plans') {
              <fs-membership-plans [venueId]="id" />
            }
            @case ('codes') {
              <fs-discount-codes [venueId]="id" />
            }
            @default {
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="flex rounded-lg bg-surface-2 p-1" role="radiogroup" aria-label="Show">
                  @for (option of filters; track option.label) {
                    <button type="button" role="radio" [attr.aria-checked]="status() === option.value" (click)="status.set(option.value)"
                      class="h-9 rounded-md px-3 text-sm font-medium text-muted" [class.bg-surface]="status() === option.value" [class.text-text]="status() === option.value">
                      {{ option.label }}
                    </button>
                  }
                </div>
                @if (canSell()) {
                  <button type="button" (click)="startSelling()" class="flex h-11 items-center gap-2 rounded-lg bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
                    <lucide-icon [img]="icons.Plus" [size]="18" />Sell membership
                  </button>
                }
              </div>
              @if (error(); as message) {
                <p class="mt-4 rounded-xl border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
              } @else if (members(); as list) {
                @if (list.length === 0) {
                  <p class="mt-4 rounded-xl border border-dashed border-border bg-surface px-6 py-8 text-center text-muted">
                    {{ status() === 'ACTIVE' ? 'No active members. Sell one at the counter, or players can buy plans in the app.' : 'Nothing here.' }}
                  </p>
                } @else {
                  <ul class="mt-4 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
                    @for (m of list; track m.id) {
                      <li>
                        <button type="button" (click)="open(m)" class="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-surface-2">
                          <span class="min-w-0 flex-1">
                            <span class="block truncate font-medium">{{ m.customer.name }}</span>
                            <span class="block truncate text-sm text-muted">{{ m.plan.name }} · <span class="tabular">{{ fmt.phone(m.customer.phone) }}</span></span>
                          </span>
                          <span class="shrink-0 text-right text-sm">
                            @if (m.gamesRemaining !== null && m.status === 'ACTIVE') {
                              <span class="tabular block font-medium">{{ m.gamesRemaining }} {{ m.gamesRemaining === 1 ? 'game' : 'games' }} left</span>
                            }
                            <span class="block text-muted">{{ statusText(m) }}</span>
                          </span>
                        </button>
                      </li>
                    }
                  </ul>
                  @if (hasMore()) {
                    <button type="button" (click)="more()" class="mt-3 h-11 w-full rounded-lg border border-border font-medium hover:bg-surface-2">Show more</button>
                  }
                }
              } @else {
                <div class="mt-4 h-32 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
              }
            }
          }
        }
      </div>
    </div>

    @if (selling(); as s) {
      <fs-sheet title="Sell a membership" (closed)="selling.set(null)">
        @if (plans().length === 0) {
          <p class="text-muted">There are no plans on sale yet. Create one under Plans first.</p>
        } @else {
          <form id="sell-form" class="space-y-4" (submit)="$event.preventDefault(); sell()">
            <fieldset>
              <legend class="mb-1.5 font-medium">Plan</legend>
              <div class="space-y-2">
                @for (plan of plans(); track plan.id) {
                  <label class="flex cursor-pointer items-start gap-3 rounded-lg border p-3" [class]="s.planId === plan.id ? 'border-ink bg-surface-2' : 'border-border'">
                    <input type="radio" name="plan" class="mt-1 size-4 accent-current" [checked]="s.planId === plan.id" (change)="patchSale({ planId: plan.id })" />
                    <span class="flex-1"><span class="block font-medium">{{ plan.name }}</span><span class="text-sm text-muted">{{ summary(plan) }}</span></span>
                    <span class="tabular font-medium">{{ fmt.rupees(plan.price) }}</span>
                  </label>
                }
              </div>
            </fieldset>
            <div>
              <label for="sell-phone" class="mb-1.5 block font-medium">Phone</label>
              <input id="sell-phone" type="tel" inputmode="numeric" autocomplete="off" placeholder="98XXXXXXXX" [value]="s.phone" (input)="patchSale({ phone: $any($event.target).value })"
                class="tabular h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
              <p class="mt-1.5 text-sm text-subtle">Their bookings with this number use the membership.</p>
            </div>
            <div>
              <label for="sell-name" class="mb-1.5 block font-medium">Name</label>
              <input id="sell-name" type="text" maxlength="100" autocomplete="off" [value]="s.name" (input)="patchSale({ name: $any($event.target).value })"
                class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label for="sell-method" class="mb-1.5 block font-medium">Paid with</label>
                <select id="sell-method" (change)="patchSale({ method: $any($event.target).value })" class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base">
                  @for (method of methods; track method) {
                    <option [value]="method" [selected]="s.method === method">{{ fmt.methodLabel(method) }}</option>
                  }
                </select>
              </div>
              @if (s.method !== 'CASH') {
                <div>
                  <label for="sell-txn" class="mb-1.5 block font-medium">Transaction ID</label>
                  <input id="sell-txn" type="text" maxlength="100" autocomplete="off" [value]="s.transactionId" (input)="patchSale({ transactionId: $any($event.target).value })"
                    class="tabular h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
                </div>
              }
            </div>
          </form>
        }
        @if (sellError(); as message) {
          <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        @if (plans().length > 0) {
          <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            <button type="submit" form="sell-form" [disabled]="busy()" class="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">
              @if (busy()) {
                <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
              }
              Sell for {{ fmt.rupees(salePrice()) }}
            </button>
          </div>
        }
      </fs-sheet>
    }

    @if (selected(); as m) {
      <fs-sheet [title]="m.customer.name" [subtitle]="m.plan.name" (closed)="selected.set(null)">
        <dl class="grid grid-cols-2 gap-3">
          <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Status</dt><dd class="font-medium">{{ statusText(m) }}</dd></div>
          <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Phone</dt><dd class="tabular font-medium">{{ fmt.phone(m.customer.phone) }}</dd></div>
          <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Started</dt><dd class="tabular font-medium">{{ fmt.withYear(m.startsOn) }}</dd></div>
          <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Ends</dt><dd class="tabular font-medium">{{ fmt.withYear(m.expiresOn) }}</dd></div>
          @if (m.gamesRemaining !== null) {
            <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Games left</dt><dd class="tabular font-medium">{{ m.gamesRemaining }}</dd></div>
          }
          @if (m.plan.discountPercent) {
            <div class="rounded-lg bg-surface-2 px-3 py-2"><dt class="text-xs text-muted">Discount</dt><dd class="tabular font-medium">{{ m.plan.discountPercent }}% off</dd></div>
          }
        </dl>
        @if (m.status === 'PENDING_PAYMENT') {
          <p class="mt-4 rounded-lg bg-surface-2 px-3 py-2 text-sm">Bought in the app and waiting for you to check the payment under Payments.</p>
        }
        @if (canSell() && (m.status === 'ACTIVE' || m.status === 'PENDING_PAYMENT')) {
          <div class="mt-6 border-t border-border pt-4">
            @if (confirmCancel()) {
              <p class="font-medium">Cancel this membership?</p>
              <p class="text-sm text-muted">Remaining games or discount stop now. Any refund is up to you, outside FutSlot.</p>
              <div class="mt-3 flex gap-2">
                <button type="button" (click)="cancel(m)" [disabled]="busy()" class="h-11 rounded-lg bg-danger px-4 font-medium text-surface disabled:opacity-60">Cancel membership</button>
                <button type="button" (click)="confirmCancel.set(false)" class="h-11 rounded-lg border border-border px-4 font-medium">Keep it</button>
              </div>
            } @else {
              <button type="button" (click)="confirmCancel.set(true)" class="h-11 rounded-lg px-1 font-medium text-danger hover:underline">Cancel membership</button>
            }
          </div>
        }
        @if (sellError(); as message) {
          <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
      </fs-sheet>
    }
  `,
})
export class Memberships {
  private readonly http = inject(HttpClient);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { LoaderCircle, Plus };

  protected readonly fmt = { methodLabel, phone, rupees, withYear };

  protected readonly summary = planSummary;

  protected readonly methods = ['CASH', 'ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'];

  protected readonly filters: { value: MembershipStatus | null; label: string }[] = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'PENDING_PAYMENT', label: 'Waiting for payment' },
    { value: null, label: 'All' },
  ];

  protected readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  protected readonly canSell = computed(() => this.venues.can('MANAGE_BOOKINGS'));

  protected readonly tabs = computed(() => [
    ...(this.canSell() ? [{ value: 'members' as Tab, label: 'Members' }] : []),
    ...(this.venues.can('MANAGE_VENUE') ? [{ value: 'plans' as Tab, label: 'Plans' }, { value: 'codes' as Tab, label: 'Discount codes' }] : []),
  ]);

  protected readonly tab = signal<Tab>((this.route.snapshot.queryParamMap.get('tab') as Tab) ?? 'members');

  protected readonly status = signal<MembershipStatus | null>('ACTIVE');

  protected readonly members = signal<Membership[] | null>(null);

  protected readonly hasMore = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly plans = signal<MembershipPlan[]>([]);

  protected readonly selling = signal<Selling | null>(null);

  protected readonly selected = signal<Membership | null>(null);

  protected readonly busy = signal(false);

  protected readonly sellError = signal<string | null>(null);

  protected readonly confirmCancel = signal(false);

  protected readonly salePrice = computed(() => this.plans().find((p) => p.id === this.selling()?.planId)?.price ?? 0);

  private page = 0;

  constructor() {
    effect(() => {
      const id = this.venueId();
      const status = this.status();
      if (id && this.tab() === 'members' && this.canSell()) {
        untracked(() => void this.load(id, status, true));
      }
    });
    effect(() => {
      // Land on the first tab this person can see
      const tabs = this.tabs();
      if (tabs.length && !tabs.some((t) => t.value === this.tab())) {
        untracked(() => this.tab.set(tabs[0].value));
      }
    });
  }

  protected go(tab: Tab): void {
    this.tab.set(tab);
    void this.router.navigate([], { queryParams: { tab: tab === 'members' ? null : tab }, replaceUrl: true });
  }

  protected statusText(m: Membership): string {
    switch (m.status) {
      case 'ACTIVE':
        return `Until ${withYear(m.expiresOn)}`;
      case 'PENDING_PAYMENT':
        return 'Payment to check';
      case 'EXPIRED':
        return m.gamesRemaining === 0 ? 'Used up' : `Ended ${withYear(m.expiresOn)}`;
      case 'CANCELLED':
        return 'Cancelled';
    }
  }

  protected more(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id, this.status(), false);
    }
  }

  protected async startSelling(): Promise<void> {
    const id = this.venueId();
    if (!id) {
      return;
    }
    this.sellError.set(null);
    try {
      const plans = (await firstValueFrom(this.http.get<MembershipPlan[]>(`/api/v1/owner/venues/${id}/membership-plans`))).filter((p) => p.active);
      this.plans.set(plans.sort((a, b) => a.price - b.price));
    }
    catch {
      // The API returns plans only to people who manage the venue; staff who can sell still see the
      // ones on sale through the public list
      const slug = this.venues.current()?.venue.slug;
      this.plans.set(slug ? await firstValueFrom(this.http.get<MembershipPlan[]>(`/api/v1/venues/${slug}/membership-plans`)) : []);
    }
    this.selling.set({ planId: this.plans()[0]?.id ?? '', phone: '', name: '', method: 'CASH', transactionId: '' });
  }

  protected patchSale(change: Partial<Selling>): void {
    this.selling.update((s) => (s ? { ...s, ...change } : s));
  }

  protected async sell(): Promise<void> {
    const s = this.selling();
    const id = this.venueId();
    if (!s || !id || this.busy()) {
      return;
    }
    if (s.phone.replace(/\D/g, '').length < 10 || !s.name.trim()) {
      this.sellError.set('Enter their 10-digit mobile number and name.');
      return;
    }
    if (s.method !== 'CASH' && !s.transactionId.trim()) {
      this.sellError.set('Enter the transaction ID from their payment.');
      return;
    }
    this.busy.set(true);
    this.sellError.set(null);
    try {
      await firstValueFrom(this.http.post(`/api/v1/owner/venues/${id}/memberships`, {
        planId: s.planId, customerName: s.name.trim(), customerPhone: s.phone.trim(), method: s.method,
        transactionId: s.method === 'CASH' ? null : s.transactionId.trim(),
      }));
      this.selling.set(null);
      this.status.set('ACTIVE');
      await this.load(id, 'ACTIVE', true);
    }
    catch (error) {
      this.sellError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected open(m: Membership): void {
    this.sellError.set(null);
    this.confirmCancel.set(false);
    this.selected.set(m);
  }

  protected async cancel(m: Membership): Promise<void> {
    const id = this.venueId();
    if (!id) {
      return;
    }
    this.busy.set(true);
    try {
      const updated = await firstValueFrom(this.http.post<Membership>(`/api/v1/owner/venues/${id}/memberships/${m.id}/cancel`, null));
      this.members.update((list) => (list ?? []).map((x) => (x.id === updated.id ? updated : x)));
      this.selected.set(null);
    }
    catch (error) {
      this.sellError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(id: string, status: MembershipStatus | null, reset: boolean): Promise<void> {
    if (reset) {
      this.page = 0;
      this.members.set(null);
    }
    try {
      const params: Record<string, string | number> = { page: this.page, size: 30 };
      if (status) {
        params['status'] = status;
      }
      const result = await firstValueFrom(this.http.get<Page<Membership>>(`/api/v1/owner/venues/${id}/memberships`, { params }));
      this.members.update((list) => (reset ? result.content : [...(list ?? []), ...result.content]));
      this.hasMore.set(result.page + 1 < result.totalPages);
      this.page = result.page + 1;
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
