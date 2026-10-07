import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Phone, Search, Smartphone, X } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { CustomerDetail, CustomerSummary, Page } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { ago, dayLabel, localClock, phone, rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';
import { StatusChip } from '../../shared/status-chip';
import { stateOf } from '../schedule/schedule-model';

type Sort = 'RECENT' | 'FREQUENT' | 'NAME';

const PAGE_SIZE = 30;

/**
 * Everyone who has booked with the venue, walk-ins included. Opening one shows how often they come, how
 * reliable they are, and the venue's private notes about them.
 */
@Component({
  selector: 'fs-customers',
  imports: [FormsModule, LucideAngularModule, Sheet, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customers.html',
})
export class Customers {
  private readonly http = inject(HttpClient);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { Phone, Search, Smartphone, X };

  protected readonly fmt = { ago, dayLabel, localClock, phone, rupees };

  protected readonly stateOf = stateOf;

  protected readonly query = signal(this.route.snapshot.queryParamMap.get('q') ?? '');

  protected readonly sort = signal<Sort>((this.route.snapshot.queryParamMap.get('sort') as Sort) ?? 'RECENT');

  protected readonly customers = signal<CustomerSummary[] | null>(null);

  protected readonly total = signal(0);

  protected readonly hasMore = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly detail = signal<CustomerDetail | null>(null);

  protected readonly detailError = signal<string | null>(null);

  protected readonly name = signal('');

  protected readonly notes = signal('');

  protected readonly saving = signal(false);

  protected readonly saved = signal(false);

  protected readonly sorts: { value: Sort; label: string }[] = [
    { value: 'RECENT', label: 'Recently booked' },
    { value: 'FREQUENT', label: 'Most bookings' },
    { value: 'NAME', label: 'Name' },
  ];

  private page = 0;

  private timer: ReturnType<typeof setTimeout> | null = null;

  private sequence = 0;

  constructor() {
    effect(() => {
      if (this.venues.current()) {
        this.sort();
        void this.load(true);
      }
    });
    inject(DestroyRef).onDestroy(() => this.timer && clearTimeout(this.timer));
  }

  protected typed(value: string): void {
    this.query.set(value);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => void this.load(true), 300);
  }

  protected setSort(value: string): void {
    this.sort.set(value as Sort);
  }

  protected more(): void {
    void this.load(false);
  }

  protected async open(customer: CustomerSummary): Promise<void> {
    const venue = this.venues.current();
    if (!venue) {
      return;
    }
    this.detail.set(null);
    this.detailError.set(null);
    this.saved.set(false);
    try {
      const detail = await firstValueFrom(this.http.get<CustomerDetail>(
        `/api/v1/owner/venues/${venue.venue.id}/customers/${customer.id}`));
      this.detail.set(detail);
      this.name.set(detail.name);
      this.notes.set(detail.notes ?? '');
    }
    catch (error) {
      this.detailError.set(problemOf(error).message);
    }
  }

  protected close(): void {
    this.detail.set(null);
    this.detailError.set(null);
  }

  protected async save(): Promise<void> {
    const venue = this.venues.current();
    const detail = this.detail();
    if (!venue || !detail || this.saving()) {
      return;
    }
    if (this.name().trim().length < 1) {
      this.detailError.set('Enter a name.');
      return;
    }
    this.saving.set(true);
    this.detailError.set(null);
    try {
      const updated = await firstValueFrom(this.http.patch<CustomerDetail>(
        `/api/v1/owner/venues/${venue.venue.id}/customers/${detail.id}`, { name: this.name().trim(), notes: this.notes() }));
      this.detail.set(updated);
      this.saved.set(true);
      this.customers.update((list) => (list ?? []).map((c) => (c.id === updated.id ? { ...c, name: updated.name } : c)));
    }
    catch (error) {
      this.detailError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  private async load(reset: boolean): Promise<void> {
    const venue = this.venues.current();
    if (!venue) {
      return;
    }
    if (reset) {
      this.page = 0;
      void this.router.navigate([], { queryParams: { q: this.query().trim() || null, sort: this.sort() }, replaceUrl: true });
    }
    const sequence = ++this.sequence;
    try {
      const params: Record<string, string | number> = { sort: this.sort(), page: this.page, size: PAGE_SIZE };
      if (this.query().trim()) {
        params['q'] = this.query().trim();
      }
      const result = await firstValueFrom(this.http.get<Page<CustomerSummary>>(
        `/api/v1/owner/venues/${venue.venue.id}/customers`, { params }));
      if (sequence !== this.sequence) {
        return;
      }
      this.customers.set(reset ? result.content : [...(this.customers() ?? []), ...result.content]);
      this.total.set(result.totalElements);
      this.hasMore.set(result.page + 1 < result.totalPages);
      this.page = result.page + 1;
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
