import { A11yModule } from '@angular/cdk/a11y';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CalendarDays, LucideAngularModule, LucideIconData, Search, User } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { Booking, CustomerSummary, Page } from '../core/api.types';
import { VenueContext } from '../core/venue-context';
import { dayLabel, localClock, phone } from '../shared/format';
import { ALL_ITEMS, canSee } from './navigation';

interface Result {
  id: string;
  group: 'Bookings' | 'Customers' | 'Go to';
  icon: LucideIconData;
  title: string;
  detail: string;
  go: () => void;
}

/**
 * Find anything from anywhere (Ctrl/⌘ K): a booking by reference, name or phone, a customer, or a screen.
 * Arrow keys move, Enter opens, Escape closes.
 */
@Component({
  selector: 'fs-command-palette',
  imports: [A11yModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closed.emit()' },
  template: `
    <div class="fixed inset-0 z-40 bg-black/50" (click)="closed.emit()" aria-hidden="true"></div>
    <div role="dialog" aria-modal="true" aria-label="Search" cdkTrapFocus cdkTrapFocusAutoCapture
      class="fixed inset-x-3 top-3 z-50 mx-auto flex max-h-[80dvh] max-w-xl flex-col overflow-hidden rounded-xl border border-border bg-surface shadow-2xl sm:top-[12vh]">
      <div class="flex items-center gap-3 border-b border-border px-4">
        <lucide-icon [img]="icons.Search" [size]="20" class="shrink-0 text-subtle" />
        <input #input type="text" role="combobox" aria-expanded="true" aria-controls="palette-results" aria-autocomplete="list"
          [attr.aria-activedescendant]="results()[active()] ? 'palette-' + active() : null" cdkFocusInitial
          [value]="query()" (input)="typed($any($event.target).value)" (keydown)="keydown($event)" autocomplete="off" spellcheck="false"
          placeholder="Find a booking, customer or screen" aria-label="Search"
          class="h-14 min-w-0 flex-1 bg-transparent text-base outline-none" />
        <kbd class="hidden rounded border border-border px-1.5 py-0.5 text-xs text-subtle sm:block">Esc</kbd>
      </div>
      <ul id="palette-results" role="listbox" aria-label="Results" class="min-h-0 flex-1 overflow-y-auto p-2">
        @for (result of results(); track result.group + result.id; let i = $index) {
          @if (i === 0 || results()[i - 1].group !== result.group) {
            <li role="presentation" class="px-2 pb-1 pt-2 text-xs font-medium uppercase tracking-wide text-subtle">{{ result.group }}</li>
          }
          <li [id]="'palette-' + i" role="option" [attr.aria-selected]="i === active()" (click)="choose(result)" (mousemove)="active.set(i)"
            class="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2" [class.bg-surface-2]="i === active()">
            <lucide-icon [img]="result.icon" [size]="18" class="shrink-0 text-muted" />
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium">{{ result.title }}</span>
              <span class="block truncate text-sm text-muted">{{ result.detail }}</span>
            </span>
          </li>
        }
        @if (results().length === 0) {
          <li role="presentation" class="px-3 py-6 text-center text-muted">
            {{ searching() ? 'Searching…' : 'Nothing matches "' + query().trim() + '".' }}
          </li>
        }
      </ul>
      <p class="hidden border-t border-border px-4 py-2 text-xs text-subtle sm:block">
        Search by booking reference (FS-…), name or phone number. ↑ ↓ to move, Enter to open.
      </p>
    </div>
  `,
})
export class CommandPalette {
  private readonly http = inject(HttpClient);

  private readonly router = inject(Router);

  private readonly venues = inject(VenueContext);

  readonly closed = output<void>();

  protected readonly icons = { Search };

  protected readonly query = signal('');

  protected readonly active = signal(0);

  protected readonly searching = signal(false);

  private readonly bookings = signal<Booking[]>([]);

  private readonly customers = signal<CustomerSummary[]>([]);

  protected readonly results = computed<Result[]>(() => {
    const term = this.query().trim().toLowerCase();
    const pages: Result[] = ALL_ITEMS.filter((item) => canSee(item, this.venues))
      .filter((item) => !term || item.label.toLowerCase().includes(term) || item.about.toLowerCase().includes(term))
      .slice(0, term ? 4 : 8)
      .map((item) => ({ id: item.path, group: 'Go to', icon: item.icon, title: item.label, detail: item.about,
        go: () => void this.router.navigate(['/' + item.path]) }));
    const bookings: Result[] = this.bookings().slice(0, 6).map((b) => ({
      id: b.id, group: 'Bookings', icon: CalendarDays, title: `${b.customer.name} · ${b.reference}`,
      detail: `${dayLabel(b.startsAt.slice(0, 10))}, ${localClock(b.startsAt)} · ${b.court.name}`,
      go: () => void this.router.navigate(['/bookings'], { queryParams: { q: b.reference, open: b.id } }),
    }));
    const customers: Result[] = this.customers().slice(0, 4).map((c) => ({
      id: c.id, group: 'Customers', icon: User, title: c.name,
      detail: `${phone(c.phone)} · ${c.totalBookings} ${c.totalBookings === 1 ? 'booking' : 'bookings'}`,
      go: () => void this.router.navigate(['/customers'], { queryParams: { q: c.name, open: c.id } }),
    }));
    // Searching for someone puts records first; with nothing typed, it's a quick way to any screen
    return term ? [...bookings, ...customers, ...pages] : pages;
  });

  private timer: ReturnType<typeof setTimeout> | null = null;

  private sequence = 0;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.timer && clearTimeout(this.timer));
  }

  protected typed(value: string): void {
    this.query.set(value);
    this.active.set(0);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    const term = value.trim();
    if (term.length < 2) {
      this.bookings.set([]);
      this.customers.set([]);
      this.searching.set(false);
      return;
    }
    this.searching.set(true);
    this.timer = setTimeout(() => void this.search(term), 200);
  }

  protected keydown(event: KeyboardEvent): void {
    const count = this.results().length;
    if (event.key === 'ArrowDown' && count) {
      event.preventDefault();
      this.active.update((i) => (i + 1) % count);
    }
    else if (event.key === 'ArrowUp' && count) {
      event.preventDefault();
      this.active.update((i) => (i - 1 + count) % count);
    }
    else if (event.key === 'Enter') {
      event.preventDefault();
      const result = this.results()[this.active()];
      if (result) {
        this.choose(result);
      }
    }
  }

  protected choose(result: Result): void {
    result.go();
    this.closed.emit();
  }

  private async search(term: string): Promise<void> {
    const venue = this.venues.current();
    if (!venue) {
      return;
    }
    const sequence = ++this.sequence;
    const base = `/api/v1/owner/venues/${venue.venue.id}`;
    const [bookings, customers] = await Promise.all([
      this.venues.can('MANAGE_BOOKINGS')
        ? firstValueFrom(this.http.get<Booking[]>(`${base}/bookings/search`, { params: { q: term } })).catch(() => [])
        : Promise.resolve([]),
      this.venues.can('VIEW_CUSTOMERS')
        ? firstValueFrom(this.http.get<Page<CustomerSummary>>(`${base}/customers`, { params: { q: term, size: 4 } }))
          .then((page) => page.content).catch(() => [])
        : Promise.resolve([]),
    ]);
    if (sequence === this.sequence) {
      this.bookings.set(bookings);
      this.customers.set(customers);
      this.searching.set(false);
      this.active.set(0);
    }
  }
}
