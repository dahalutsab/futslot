import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule, Plus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { MembershipPlan, MembershipType } from '../../core/api.types';
import { rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';

interface Editing {
  plan: MembershipPlan | null;
  type: MembershipType;
  name: string;
  description: string;
  games: string;
  percent: string;
  days: string;
  price: string;
  active: boolean;
}

/** What a plan gives, in a line. */
export function planSummary(plan: Pick<MembershipPlan, 'type' | 'gamesIncluded' | 'discountPercent' | 'validityDays'>): string {
  const days = `valid ${plan.validityDays} days`;
  if (plan.type === 'GAME_PACK') {
    return `${plan.gamesIncluded} games, ${days}`;
  }
  const cap = plan.gamesIncluded ? ` for up to ${plan.gamesIncluded} games` : ' on every booking';
  return `${plan.discountPercent}% off${cap}, ${days}`;
}

/**
 * The plans players can buy: game packs (games paid up front) and passes (a discount while valid). Players
 * buy them in the app or at the counter.
 */
@Component({
  selector: 'fs-membership-plans',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <p class="text-muted">Regulars pay up front and get a better price. Players see active plans in the app.</p>
      <button type="button" (click)="add()" class="flex h-11 items-center gap-2 rounded-lg bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
        <lucide-icon [img]="icons.Plus" [size]="18" />New plan
      </button>
    </div>
    @if (error(); as message) {
      <p class="mt-4 rounded-xl border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
    } @else if (plans(); as list) {
      @if (list.length === 0) {
        <div class="mt-4 rounded-xl border border-dashed border-border bg-surface px-6 py-8 text-center">
          <p class="font-medium">No plans yet</p>
          <p class="mt-1 text-muted">A 10-game pack is a good start: regulars pay once and you fill the court.</p>
          <button type="button" (click)="add('GAME_PACK')" class="mt-4 h-11 rounded-lg border border-border px-4 font-medium hover:bg-surface-2">Create a 10-game pack</button>
        </div>
      } @else {
        <ul class="mt-4 grid gap-3 sm:grid-cols-2">
          @for (plan of list; track plan.id) {
            <li>
              <button type="button" (click)="edit(plan)" class="flex h-full w-full flex-col rounded-xl border border-border bg-surface p-4 text-left hover:bg-surface-2" [class.opacity-60]="!plan.active">
                <span class="flex w-full items-baseline justify-between gap-3">
                  <span class="font-semibold">{{ plan.name }}</span>
                  <span class="tabular font-semibold">{{ fmt.rupees(plan.price) }}</span>
                </span>
                <span class="mt-1 text-sm text-muted">{{ summary(plan) }}</span>
                @if (perGame(plan); as each) {
                  <span class="mt-1 text-sm text-subtle">{{ each }} a game</span>
                }
                @if (!plan.active) {
                  <span class="mt-2 text-sm text-muted">Not on sale</span>
                }
              </button>
            </li>
          }
        </ul>
      }
    } @else {
      <div class="mt-4 h-32 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
    }

    @if (editing(); as e) {
      <fs-sheet [title]="e.plan ? 'Edit ' + e.plan.name : 'New plan'" (closed)="editing.set(null)">
        <form id="plan-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
          <fieldset [disabled]="!!e.plan">
            <legend class="mb-1.5 font-medium">Kind</legend>
            <div class="space-y-2">
              @for (kind of kinds; track kind.value) {
                <label class="flex cursor-pointer items-start gap-3 rounded-lg border p-3" [class]="e.type === kind.value ? 'border-ink bg-surface-2' : 'border-border'">
                  <input type="radio" name="kind" class="mt-1 size-4 accent-current" [checked]="e.type === kind.value" (change)="patch({ type: kind.value })" />
                  <span><span class="block font-medium">{{ kind.label }}</span><span class="text-sm text-muted">{{ kind.detail }}</span></span>
                </label>
              }
            </div>
            @if (e.plan) {
              <p class="mt-1.5 text-sm text-subtle">The kind can't change once players may have bought it.</p>
            }
          </fieldset>
          <div class="grid grid-cols-2 gap-3">
            @if (e.type === 'GAME_PACK') {
              <div>
                <label for="plan-games" class="mb-1.5 block font-medium">Games</label>
                <input id="plan-games" type="number" inputmode="numeric" min="1" max="500" [value]="e.games" (input)="patch({ games: $any($event.target).value })"
                  class="tabular h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
              </div>
            } @else {
              <div>
                <label for="plan-percent" class="mb-1.5 block font-medium">Discount</label>
                <div class="relative">
                  <input id="plan-percent" type="number" inputmode="numeric" min="1" max="100" [value]="e.percent" (input)="patch({ percent: $any($event.target).value })"
                    class="tabular h-12 w-full rounded-lg border border-border bg-surface pl-3 pr-8 text-base" />
                  <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted">%</span>
                </div>
              </div>
            }
            <div>
              <label for="plan-days" class="mb-1.5 block font-medium">Valid for</label>
              <div class="relative">
                <input id="plan-days" type="number" inputmode="numeric" min="1" max="366" [value]="e.days" (input)="patch({ days: $any($event.target).value })"
                  class="tabular h-12 w-full rounded-lg border border-border bg-surface pl-3 pr-14 text-base" />
                <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted">days</span>
              </div>
            </div>
          </div>
          @if (e.type === 'MONTHLY_PASS') {
            <div>
              <label for="plan-cap" class="mb-1.5 block font-medium">Limit to <span class="font-normal text-subtle">(optional)</span></label>
              <div class="relative w-40">
                <input id="plan-cap" type="number" inputmode="numeric" min="1" max="500" [value]="e.games" (input)="patch({ games: $any($event.target).value })"
                  class="tabular h-12 w-full rounded-lg border border-border bg-surface pl-3 pr-16 text-base" />
                <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted">games</span>
              </div>
            </div>
          }
          <div>
            <label for="plan-price" class="mb-1.5 block font-medium">Price</label>
            <div class="relative w-40">
              <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted">Rs</span>
              <input id="plan-price" type="number" inputmode="numeric" min="0" step="100" [value]="e.price" (input)="patch({ price: $any($event.target).value })"
                class="tabular h-12 w-full rounded-lg border border-border bg-surface pl-10 pr-3 text-base" />
            </div>
            @if (draftPerGame(); as each) {
              <p class="mt-1.5 text-sm text-muted">{{ each }} a game.</p>
            }
          </div>
          <div>
            <label for="plan-name" class="mb-1.5 block font-medium">Name</label>
            <input id="plan-name" type="text" maxlength="100" [value]="e.name" [placeholder]="suggestedName()" (input)="patch({ name: $any($event.target).value })"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
          </div>
          <div>
            <label for="plan-description" class="mb-1.5 block font-medium">Details for players <span class="font-normal text-subtle">(optional)</span></label>
            <textarea id="plan-description" rows="2" maxlength="500" [value]="e.description" (input)="patch({ description: $any($event.target).value })"
              placeholder="e.g. Any court, any time. Book in the app as usual." class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-base"></textarea>
          </div>
          @if (e.plan) {
            <label class="flex cursor-pointer items-start gap-3 rounded-lg border border-border p-3">
              <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="e.active" (change)="patch({ active: !e.active })" />
              <span><span class="block font-medium">On sale</span><span class="text-sm text-subtle">Members who already bought it keep it either way.</span></span>
            </label>
          }
        </form>
        @if (formError(); as message) {
          <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
          <button type="submit" form="plan-form" [disabled]="saving()" class="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">
            @if (saving()) {
              <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
            }
            {{ e.plan ? 'Save' : 'Create plan' }}
          </button>
        </div>
      </fs-sheet>
    }
  `,
})
export class MembershipPlans implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  protected readonly icons = { LoaderCircle, Plus };

  protected readonly fmt = { rupees };

  protected readonly summary = planSummary;

  protected readonly kinds: { value: MembershipType; label: string; detail: string }[] = [
    { value: 'GAME_PACK', label: 'Game pack', detail: 'A number of games paid up front. Each booking uses one.' },
    { value: 'MONTHLY_PASS', label: 'Pass', detail: 'A discount on bookings while it lasts, e.g. 15% off for a month.' },
  ];

  protected readonly plans = signal<MembershipPlan[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly editing = signal<Editing | null>(null);

  protected readonly saving = signal(false);

  protected readonly formError = signal<string | null>(null);

  protected readonly suggestedName = computed(() => {
    const e = this.editing();
    if (!e) {
      return '';
    }
    return e.type === 'GAME_PACK' ? `${e.games || 10}-game pack` : `${e.percent || 15}% pass`;
  });

  protected readonly draftPerGame = computed(() => {
    const e = this.editing();
    return e && e.type === 'GAME_PACK' && Number(e.games) > 0 && e.price !== '' ? rupees(Math.round(Number(e.price) / Number(e.games))) : null;
  });

  ngOnInit(): void {
    void this.load();
  }

  protected perGame(plan: MembershipPlan): string | null {
    return plan.type === 'GAME_PACK' && plan.gamesIncluded ? rupees(Math.round(plan.price / plan.gamesIncluded)) : null;
  }

  protected add(type: MembershipType = 'GAME_PACK'): void {
    this.formError.set(null);
    this.editing.set({ plan: null, type, name: '', description: '', games: type === 'GAME_PACK' ? '10' : '', percent: '15',
      days: type === 'GAME_PACK' ? '90' : '30', price: '', active: true });
  }

  protected edit(plan: MembershipPlan): void {
    this.formError.set(null);
    this.editing.set({ plan, type: plan.type, name: plan.name, description: plan.description ?? '', games: plan.gamesIncluded?.toString() ?? '',
      percent: plan.discountPercent?.toString() ?? '', days: String(plan.validityDays), price: String(plan.price), active: plan.active });
  }

  protected patch(change: Partial<Editing>): void {
    this.editing.update((e) => (e ? { ...e, ...change } : e));
  }

  protected async save(): Promise<void> {
    const e = this.editing();
    if (!e || this.saving()) {
      return;
    }
    const games = e.games.trim() ? Number(e.games) : null;
    if (e.type === 'GAME_PACK' && !(games && games >= 1)) {
      this.formError.set('Say how many games the pack includes.');
      return;
    }
    if (e.type === 'MONTHLY_PASS' && !(Number(e.percent) >= 1 && Number(e.percent) <= 100)) {
      this.formError.set('Set a discount between 1% and 100%.');
      return;
    }
    if (!(Number(e.days) >= 1) || e.price === '' || Number(e.price) < 0) {
      this.formError.set('Set how long it lasts and its price.');
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const body = {
      name: e.name.trim() || this.suggestedName(),
      description: e.description.trim() || null,
      type: e.type,
      gamesIncluded: games,
      discountPercent: e.type === 'MONTHLY_PASS' ? Number(e.percent) : null,
      validityDays: Number(e.days),
      price: Number(e.price),
      active: e.active,
    };
    const base = `/api/v1/owner/venues/${this.venueId()}/membership-plans`;
    try {
      const saved = e.plan
        ? await firstValueFrom(this.http.put<MembershipPlan>(`${base}/${e.plan.id}`, body))
        : await firstValueFrom(this.http.post<MembershipPlan>(base, body));
      this.plans.update((list) => [...(list ?? []).filter((p) => p.id !== saved.id), saved].sort((a, b) => a.price - b.price));
      this.editing.set(null);
    }
    catch (error) {
      this.formError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  private async load(): Promise<void> {
    try {
      const plans = await firstValueFrom(this.http.get<MembershipPlan[]>(`/api/v1/owner/venues/${this.venueId()}/membership-plans`));
      this.plans.set([...plans].sort((a, b) => Number(b.active) - Number(a.active) || a.price - b.price));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
