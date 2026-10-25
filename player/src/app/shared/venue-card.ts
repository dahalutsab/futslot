import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { VenueSummary } from '../core/api.types';
import { rupees } from './format';
import { digits, l } from '../i18n/i18n';

/** A venue in a list, as on the front page: its photo screened like newsprint, where it is, its name and lowest price. */
@Component({
  selector: 'fs-venue-card',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let v = venue();
    <a [routerLink]="['/venues', v.slug, 'slots']" class="group flex flex-col gap-1.5 text-text hover:text-text">
      <div class="halftone mb-3.5 aspect-[3/2]">
        @if (v.coverPhotoUrl) {
          <img [src]="v.coverPhotoUrl" alt="" loading="lazy" class="size-full object-cover" />
        } @else {
          <!-- No photo yet: the court from above, its surface one of three, as in the venues' own uploads -->
          <svg class="size-full" viewBox="0 0 300 200" preserveAspectRatio="xMidYMid slice" aria-hidden="true" [style.background]="surface()">
            <g fill="none" stroke="#fff" stroke-opacity=".9" stroke-width="1.5">
              <rect x="15" y="4" width="270" height="192" />
              <path d="M150 4v192" />
              <circle cx="150" cy="100" r="36" />
            </g>
          </svg>
        }
      </div>
      <span class="kicker">{{ v.area || v.city }} · {{ l().courts(v.courtCount) }}</span>
      <h3 class="text-2xl leading-7 tracking-[-0.01em] group-hover:text-accent-700">{{ v.name }}</h3>
      <p class="mt-1.5 flex flex-wrap gap-x-4 text-[15px]">
        @if (v.fromPrice !== null) {
          <span class="tabular">{{ l().fromPrice }}{{ rupees(v.fromPrice) }}</span>
        }
        @if (v.reviewCount > 0) {
          <span class="tabular text-text/70" [attr.aria-label]="l().ratingFrom(v.averageRating, l().reviews(v.reviewCount))">★ {{ digits(v.averageRating.toFixed(1)) }} ({{ digits(v.reviewCount) }})</span>
        }
      </p>
    </a>
  `,
})
export class VenueCard {
  protected readonly l = l;

  readonly venue = input.required<VenueSummary>();

  protected readonly rupees = rupees;

  protected readonly digits = digits;

  /** Turf green, a hard court's blue or a clay brown, by the name's length: the same as on FutSlot's public pages */
  protected readonly surface = computed(() => ['#2c6a44', '#122e54', '#6b5640'][this.venue().name.length % 3]);
}
