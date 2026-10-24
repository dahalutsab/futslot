import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { LoaderCircle, LucideAngularModule, Plus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { AddOnItem } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';

type Category = AddOnItem['category'];

const CATEGORIES: { value: Category; label: string }[] = [
  { value: 'EQUIPMENT_RENTAL', label: 'Rent out' },
  { value: 'FOOD_DRINK', label: 'Food & drinks' },
  { value: 'OTHER', label: 'Other' },
];

/** What venues commonly sell; the owner sets the price. */
const SUGGESTIONS: { name: string; category: Category }[] = [
  { name: 'Bibs (set)', category: 'EQUIPMENT_RENTAL' },
  { name: 'Ball', category: 'EQUIPMENT_RENTAL' },
  { name: 'Futsal shoes', category: 'EQUIPMENT_RENTAL' },
  { name: 'Water bottle', category: 'FOOD_DRINK' },
  { name: 'Energy drink', category: 'FOOD_DRINK' },
];

interface Editing {
  item: AddOnItem | null;
  name: string;
  category: Category;
  price: string;
  active: boolean;
}

/**
 * Things the venue sells or rents with a booking, like bibs and water. Staff add them to a booking from the
 * schedule and they're added to what the player owes.
 */
@Component({
  selector: 'fs-extras',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-4 lg:px-8 lg:py-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-3xl lg:text-4xl">Extras</h1>
          <p class="mt-1 text-muted">Things you sell or rent with a booking. Add them to a booking from the schedule.</p>
        </div>
        @if (items()) {
          <button type="button" (click)="add()" class="flex h-11 items-center gap-2 rounded-md bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
            <lucide-icon [img]="icons.Plus" [size]="18" />Add extra
          </button>
        }
      </div>

      @if (error(); as message) {
        <div class="mt-6 rounded-md border border-border bg-surface px-6 py-8 text-center">
          <p class="text-muted">{{ message }}</p>
          <button type="button" (click)="retry()" class="mt-3 h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">Try again</button>
        </div>
      } @else if (items(); as list) {
        @if (list.length === 0) {
          <div class="mt-5 rounded-md border border-dashed border-border bg-surface px-6 py-8 text-center">
            <p class="font-medium">Nothing for sale yet</p>
            <p class="mt-1 text-muted">Start with what players usually ask for:</p>
            <div class="mt-4 flex flex-wrap justify-center gap-2">
              @for (suggestion of suggestions; track suggestion.name) {
                <button type="button" (click)="add(suggestion)" class="flex h-11 items-center gap-1.5 rounded-full border border-border px-4 font-medium hover:bg-surface-2">
                  <lucide-icon [img]="icons.Plus" [size]="16" />{{ suggestion.name }}
                </button>
              }
            </div>
          </div>
        }
        @for (group of groups(); track group.value) {
          <section class="mt-5" [attr.aria-labelledby]="'extras-' + group.value">
            <h2 [id]="'extras-' + group.value" class="mb-2 text-sm font-medium text-subtle">{{ group.label }}</h2>
            <ul class="divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
              @for (item of group.items; track item.id) {
                <li class="flex items-center gap-3 pr-3">
                  <button type="button" (click)="edit(item)" class="flex min-h-14 min-w-0 flex-1 items-center gap-3 px-4 text-left hover:bg-surface-2">
                    <span class="min-w-0 flex-1 truncate font-medium" [class.text-muted]="!item.active">{{ item.name }}</span>
                    <span class="tabular" [class.text-muted]="!item.active">{{ fmt.rupees(item.price) }}</span>
                  </button>
                  <button type="button" role="switch" [attr.aria-checked]="item.active" [attr.aria-label]="'Selling ' + item.name"
                    (click)="toggle(item)" [disabled]="busyId() === item.id" class="flex h-11 items-center gap-2 text-sm disabled:opacity-60">
                    <span class="relative block h-6 w-10 shrink-0 rounded-full transition-colors" [class]="item.active ? 'bg-ink' : 'bg-subtle'">
                      <span class="absolute left-0 top-0.5 block size-5 rounded-full bg-surface shadow transition-transform" [class]="item.active ? 'translate-x-[18px]' : 'translate-x-0.5'"></span>
                    </span>
                    <span class="hidden w-16 text-left text-muted sm:block">{{ item.active ? 'On sale' : 'Off' }}</span>
                  </button>
                </li>
              }
            </ul>
          </section>
        }
        @if (rowError(); as message) {
          <p class="mt-3 text-sm text-danger" role="alert">{{ message }}</p>
        }
      } @else {
        <div class="mt-6 h-64 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>

    @if (editing(); as e) {
      <fs-sheet [title]="e.item ? 'Edit ' + e.item.name : 'Add an extra'" (closed)="editing.set(null)">
        <form id="extra-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
          <div>
            <label for="extra-name" class="mb-1.5 block font-medium">Name</label>
            <input id="extra-name" type="text" maxlength="60" autocomplete="off" [value]="e.name" (input)="patch({ name: $any($event.target).value })"
              placeholder="e.g. Bibs (set)" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
          </div>
          <fieldset>
            <legend class="mb-1.5 font-medium">Kind</legend>
            <div class="flex gap-2">
              @for (option of categories; track option.value) {
                <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-md border px-2 text-center font-medium"
                  [class]="e.category === option.value ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                  <input type="radio" name="category" class="sr-only" [checked]="e.category === option.value" (change)="patch({ category: option.value })" />
                  {{ option.label }}
                </label>
              }
            </div>
          </fieldset>
          <div>
            <label for="extra-price" class="mb-1.5 block font-medium">Price</label>
            <div class="relative w-40">
              <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted">Rs</span>
              <input id="extra-price" type="number" inputmode="numeric" min="0" step="5" [value]="e.price" (input)="patch({ price: $any($event.target).value })"
                class="tabular h-12 w-full rounded-md border border-border bg-surface pl-10 pr-3 text-base" />
            </div>
            <p class="mt-1.5 text-sm text-subtle">For one, e.g. one bottle. Staff choose how many.</p>
          </div>
          @if (e.item) {
            <label class="flex cursor-pointer items-start gap-3 rounded-md border border-border p-3">
              <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="e.active" (change)="patch({ active: !e.active })" />
              <span>
                <span class="block font-medium">On sale</span>
                <span class="text-sm text-subtle">Untick when you run out. Bookings that already have it keep it.</span>
              </span>
            </label>
          }
        </form>
        @if (formError(); as message) {
          <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
          <button type="submit" form="extra-form" [disabled]="saving()"
            class="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
            @if (saving()) {
              <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
            }
            {{ e.item ? 'Save' : 'Add extra' }}
          </button>
        </div>
      </fs-sheet>
    }
  `,
})
export class Extras {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { LoaderCircle, Plus };

  protected readonly fmt = { rupees };

  protected readonly categories = CATEGORIES;

  protected readonly suggestions = SUGGESTIONS;

  protected readonly items = signal<AddOnItem[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly rowError = signal<string | null>(null);

  protected readonly busyId = signal<string | null>(null);

  protected readonly editing = signal<Editing | null>(null);

  protected readonly saving = signal(false);

  protected readonly formError = signal<string | null>(null);

  protected readonly groups = computed(() => {
    const items = this.items() ?? [];
    return CATEGORIES.map((category) => ({
      ...category,
      items: items.filter((item) => item.category === category.value)
        .sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name)),
    })).filter((group) => group.items.length > 0);
  });

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected add(suggestion?: { name: string; category: Category }): void {
    this.formError.set(null);
    this.editing.set({ item: null, name: suggestion?.name ?? '', category: suggestion?.category ?? 'EQUIPMENT_RENTAL', price: '', active: true });
  }

  protected edit(item: AddOnItem): void {
    this.formError.set(null);
    this.editing.set({ item, name: item.name, category: item.category, price: String(item.price), active: item.active });
  }

  protected patch(change: Partial<Editing>): void {
    this.editing.update((e) => (e ? { ...e, ...change } : e));
  }

  protected async save(): Promise<void> {
    const e = this.editing();
    const id = this.venueId();
    if (!e || !id || this.saving()) {
      return;
    }
    const price = Number(e.price);
    if (!e.name.trim()) {
      this.formError.set('Give it a name staff and players will recognise.');
      return;
    }
    if (e.price === '' || !Number.isFinite(price) || price < 0) {
      this.formError.set('Enter the price for one, e.g. Rs 50.');
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const body = { name: e.name.trim(), category: e.category, price, active: e.active };
    try {
      const saved = e.item
        ? await firstValueFrom(this.http.put<AddOnItem>(`/api/v1/owner/venues/${id}/add-ons/${e.item.id}`, body))
        : await firstValueFrom(this.http.post<AddOnItem>(`/api/v1/owner/venues/${id}/add-ons`, body));
      this.items.update((list) => [...(list ?? []).filter((item) => item.id !== saved.id), saved]);
      this.editing.set(null);
    }
    catch (error) {
      this.formError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  protected async toggle(item: AddOnItem): Promise<void> {
    const id = this.venueId();
    if (!id || this.busyId()) {
      return;
    }
    this.busyId.set(item.id);
    this.rowError.set(null);
    try {
      const saved = await firstValueFrom(this.http.put<AddOnItem>(`/api/v1/owner/venues/${id}/add-ons/${item.id}`,
        { name: item.name, category: item.category, price: item.price, active: !item.active }));
      this.items.update((list) => (list ?? []).map((i) => (i.id === saved.id ? saved : i)));
    }
    catch (error) {
      this.rowError.set(`Couldn't change ${item.name}: ${problemOf(error).message}`);
    }
    finally {
      this.busyId.set(null);
    }
  }

  protected retry(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id);
    }
  }

  private async load(id: string): Promise<void> {
    try {
      this.items.set(await firstValueFrom(this.http.get<AddOnItem[]>(`/api/v1/owner/venues/${id}/add-ons`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
