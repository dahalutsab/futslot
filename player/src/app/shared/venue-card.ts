import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, MapPin, Star } from 'lucide-angular';

import { VenueSummary } from '../core/api.types';
import { rupees } from './format';
import { digits, l } from '../i18n/i18n';

/** A venue in a list: photo, where it is, rating and the lowest price. */
@Component({
  selector: 'fs-venue-card',
  imports: [RouterLink, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let v = venue();
    <a [routerLink]="['/venues', v.slug, 'slots']" class="group block overflow-hidden rounded-xl border border-border bg-surface hover:border-subtle">
      <div class="aspect-[16/9] bg-surface-2">
        @if (v.coverPhotoUrl) {
          <img [src]="v.coverPhotoUrl" alt="" loading="lazy" class="size-full object-cover" />
        } @else {
          <div class="grid size-full place-items-center text-3xl font-bold text-subtle" aria-hidden="true">{{ v.name.charAt(0) }}</div>
        }
      </div>
      <div class="p-4">
        <div class="flex items-start gap-3">
          <h3 class="min-w-0 flex-1 font-semibold leading-snug group-hover:underline">{{ v.name }}</h3>
          @if (v.reviewCount > 0) {
            <span class="tabular flex shrink-0 items-center gap-1 text-sm font-medium" [attr.aria-label]="l().ratingFrom(v.averageRating, l().reviews(v.reviewCount))">
              <lucide-icon [img]="icons.Star" [size]="14" class="fill-current text-held-line" />{{ digits(v.averageRating.toFixed(1)) }}
              <span class="font-normal text-subtle">({{ digits(v.reviewCount) }})</span>
            </span>
          }
        </div>
        <p class="mt-1 flex items-center gap-1 text-sm text-muted">
          <lucide-icon [img]="icons.MapPin" [size]="14" />{{ v.area ? v.area + ', ' : '' }}{{ v.city }}
        </p>
        <p class="mt-3 text-sm">
          @if (v.fromPrice !== null) {
            <span class="text-muted">{{ l().fromPrice }}</span><span class="tabular font-semibold">{{ rupees(v.fromPrice) }}</span>
            <span class="text-muted"> · </span>
          }
          <span class="text-muted">{{ l().courts(v.courtCount) }}</span>
        </p>
      </div>
    </a>
  `,
})
export class VenueCard {
  protected readonly l = l;

  readonly venue = input.required<VenueSummary>();

  protected readonly icons = { MapPin, Star };

  protected readonly rupees = rupees;

  protected readonly digits = digits;
}
