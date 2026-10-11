import { NgTemplateOutlet } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CircleAlert, Clock, Copy, ExternalLink, LucideAngularModule, MapPin, Plus, Send, Trash2 } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Amenity, CancellationTier, VenueDetail, VenuePolicy } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { HoursRow, hoursProblem, hoursRows, nextTier, overnight, parseLocation, policySummary, tiersProblem, toDayHours } from './venue-model';

type Section = 'details' | 'hours' | 'policy' | 'payment' | 'amenities';

interface DetailsForm {
  name: string;
  description: string;
  phone: string;
  email: string;
  addressLine: string;
  area: string;
  city: string;
  location: string;
}

function detailsOf(venue: VenueDetail): DetailsForm {
  return {
    name: venue.name,
    description: venue.description ?? '',
    phone: venue.phone ?? '',
    email: venue.email ?? '',
    addressLine: venue.addressLine,
    area: venue.area ?? '',
    city: venue.city,
    location: venue.latitude != null && venue.longitude != null ? `${venue.latitude}, ${venue.longitude}` : '',
  };
}

/**
 * What players see about the venue and the rules they book under: details and map location, opening hours,
 * booking and refund rules, how to pay, and amenities. Each card saves on its own. A new venue is submitted
 * for approval from here.
 */
@Component({
  selector: 'fs-venue-profile',
  imports: [LucideAngularModule, NgTemplateOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './venue-profile.html',
})
export class VenueProfile {
  private readonly http = inject(HttpClient);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { CircleAlert, Clock, Copy, ExternalLink, MapPin, Plus, Send, Trash2 };

  /** The server takes at most this many cancellation fees. */
  protected readonly maxTiers = 6;

  protected readonly overnight = overnight;

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly loadError = signal<string | null>(null);

  protected readonly allAmenities = signal<Amenity[]>([]);

  protected readonly details = signal<DetailsForm | null>(null);

  protected readonly rows = signal<HoursRow[]>([]);

  protected readonly policy = signal<VenuePolicy | null>(null);

  protected readonly instructions = signal('');

  protected readonly amenityCodes = signal<string[]>([]);

  protected readonly saving = signal<Section | 'submit' | null>(null);

  protected readonly saved = signal<Section | null>(null);

  protected readonly errors = signal<Partial<Record<Section | 'submit', string>>>({});

  protected readonly dirty = computed<Record<Section, boolean>>(() => {
    const venue = this.venue();
    if (!venue) {
      return { details: false, hours: false, policy: false, payment: false, amenities: false };
    }
    return {
      details: JSON.stringify(this.details()) !== JSON.stringify(detailsOf(venue)),
      // A new venue shows suggested hours that aren't saved yet
      hours: venue.operatingHours.length < 7 || JSON.stringify(this.rows()) !== JSON.stringify(hoursRows(venue.operatingHours)),
      policy: JSON.stringify(this.policy()) !== JSON.stringify(venue.policy),
      payment: this.instructions() !== (venue.paymentInstructions ?? ''),
      amenities: [...this.amenityCodes()].sort().join() !== venue.amenities.map((a) => a.code).sort().join(),
    };
  });

  protected readonly location = computed(() => {
    const text = this.details()?.location.trim() ?? '';
    return text ? parseLocation(text) : null;
  });

  protected readonly summary = computed(() => {
    const policy = this.policy();
    return policy ? policySummary(policy) : [];
  });

  constructor() {
    // Only a change of venue reloads; refreshing the venue list after a save must not
    const venueId = computed(() => this.venues.current()?.venue.id ?? null);
    effect(() => {
      const id = venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected isDirty(section: Section): boolean {
    return this.dirty()[section];
  }

  protected errorOf(section: Section | 'submit'): string | undefined {
    return this.errors()[section];
  }

  protected setDetail(field: keyof DetailsForm, value: string): void {
    this.details.update((form) => (form ? { ...form, [field]: value } : form));
    this.touched('details');
  }

  protected setRow(index: number, change: Partial<HoursRow>): void {
    this.rows.update((rows) => rows.map((row, i) => (i === index ? { ...row, ...change } : row)));
    this.touched('hours');
  }

  /** Gives every day the first day's hours. */
  protected copyFirstDay(): void {
    const [first] = this.rows();
    this.rows.update((rows) => rows.map((row) => ({ ...row, open: first.open, from: first.from, to: first.to })));
    this.touched('hours');
  }

  protected setPolicy(field: keyof VenuePolicy, value: number): void {
    this.policy.update((policy) => (policy ? { ...policy, [field]: value } : policy));
    this.touched('policy');
  }

  protected setTier(index: number, change: Partial<CancellationTier>): void {
    this.setTiers((tiers) => tiers.map((tier, i) => (i === index ? { ...tier, ...change } : tier)));
  }

  protected addTier(): void {
    this.setTiers((tiers) => [...tiers, nextTier(tiers)]);
  }

  protected removeTier(index: number): void {
    this.setTiers((tiers) => tiers.filter((_, i) => i !== index));
  }

  private setTiers(change: (tiers: CancellationTier[]) => CancellationTier[]): void {
    this.policy.update((policy) => (policy ? { ...policy, cancellationTiers: change(policy.cancellationTiers) } : policy));
    this.touched('policy');
  }

  protected setInstructions(value: string): void {
    this.instructions.set(value);
    this.touched('payment');
  }

  protected toggleAmenity(code: string): void {
    this.amenityCodes.update((codes) => (codes.includes(code) ? codes.filter((c) => c !== code) : [...codes, code]));
    this.touched('amenities');
  }

  protected has(code: string): boolean {
    return this.amenityCodes().includes(code);
  }

  protected async save(section: Section): Promise<void> {
    const venue = this.venue();
    if (!venue || this.saving()) {
      return;
    }
    const request = this.request(section, venue.id);
    if (typeof request === 'string') {
      this.fail(section, request);
      return;
    }
    this.saving.set(section);
    this.fail(section, null);
    try {
      const updated = await firstValueFrom(this.http.put<VenueDetail>(request.url, request.body));
      this.venue.set(updated);
      this.reset(section, updated);
      this.saved.set(section);
      if (section === 'details') {
        void this.venues.load();
      }
    }
    catch (error) {
      this.fail(section, problemOf(error).message);
    }
    finally {
      this.saving.set(null);
    }
  }

  protected async submit(): Promise<void> {
    const venue = this.venue();
    if (!venue || this.saving()) {
      return;
    }
    this.saving.set('submit');
    this.fail('submit', null);
    try {
      this.venue.set(await firstValueFrom(this.http.post<VenueDetail>(`/api/v1/owner/venues/${venue.id}/submit`, null)));
      void this.venues.load();
    }
    catch (error) {
      this.fail('submit', problemOf(error).message);
    }
    finally {
      this.saving.set(null);
    }
  }

  protected retry(): void {
    const current = this.venues.current();
    if (current) {
      void this.load(current.venue.id);
    }
  }

  private request(section: Section, id: string): { url: string; body: unknown } | string {
    const base = `/api/v1/owner/venues/${id}`;
    switch (section) {
      case 'details': {
        const form = this.details()!;
        if (!form.name.trim() || !form.addressLine.trim() || !form.city.trim()) {
          return 'Name, address and city are needed.';
        }
        const location = this.location();
        if (form.location.trim() && !location) {
          return 'Paste the map location as coordinates, like 27.7172, 85.3240, or as a Google Maps link.';
        }
        const blankToNull = (value: string) => value.trim() || null;
        return {
          url: base,
          body: {
            name: form.name.trim(),
            description: blankToNull(form.description),
            phone: blankToNull(form.phone),
            email: blankToNull(form.email),
            addressLine: form.addressLine.trim(),
            area: blankToNull(form.area),
            city: form.city.trim(),
            latitude: location?.latitude ?? null,
            longitude: location?.longitude ?? null,
          },
        };
      }
      case 'hours':
        return hoursProblem(this.rows()) ?? { url: `${base}/operating-hours`, body: toDayHours(this.rows()) };
      case 'policy':
        return tiersProblem(this.policy()?.cancellationTiers ?? []) ?? { url: `${base}/policy`, body: this.policy() };
      case 'payment':
        return { url: `${base}/payment-instructions`, body: { instructions: this.instructions().trim() || null } };
      case 'amenities':
        return { url: `${base}/amenities`, body: { codes: this.amenityCodes() } };
    }
  }

  private async load(id: string): Promise<void> {
    this.loadError.set(null);
    try {
      const [venue, amenities] = await Promise.all([
        firstValueFrom(this.http.get<VenueDetail>(`/api/v1/owner/venues/${id}`)),
        firstValueFrom(this.http.get<Amenity[]>('/api/v1/amenities')),
      ]);
      this.venue.set(venue);
      this.allAmenities.set(amenities);
      for (const section of ['details', 'hours', 'policy', 'payment', 'amenities'] as const) {
        this.reset(section, venue);
      }
      this.errors.set({});
      this.saved.set(null);
    }
    catch (error) {
      this.loadError.set(problemOf(error).message);
    }
  }

  /** Puts one card back to what the server has. */
  private reset(section: Section, venue: VenueDetail): void {
    switch (section) {
      case 'details':
        this.details.set(detailsOf(venue));
        break;
      case 'hours':
        this.rows.set(hoursRows(venue.operatingHours));
        break;
      case 'policy':
        this.policy.set({ ...venue.policy });
        break;
      case 'payment':
        this.instructions.set(venue.paymentInstructions ?? '');
        break;
      case 'amenities':
        this.amenityCodes.set(venue.amenities.map((a) => a.code));
        break;
    }
  }

  private touched(section: Section): void {
    if (this.saved() === section) {
      this.saved.set(null);
    }
  }

  private fail(section: Section | 'submit', message: string | null): void {
    this.errors.update((errors) => ({ ...errors, [section]: message ?? undefined }));
  }
}
