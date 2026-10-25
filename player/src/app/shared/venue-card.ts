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
    <a [routerLink]="['/venues', v.slug, 'slots']" class="group block">
      <div class="aspect-[3/2] overflow-hidden rounded-md bg-surface-2">
        @if (v.coverPhotoUrl) {
          <img [src]="v.coverPhotoUrl" alt="" loading="lazy" class="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]" />
        } @else {
          <!-- No photo yet: the court from above, mown in stripes, as on FutSlot's public pages -->
          <svg class="size-full bg-turf" viewBox="0 0 300 200" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
            @for (x of stripes; track x) {
              <rect [attr.x]="x" y="0" width="30" height="200" fill="#fff" fill-opacity=".05" />
            }
            <g fill="none" stroke="#fff" stroke-opacity=".45" stroke-width="2">
              <rect x="14" y="14" width="272" height="172" rx="2" />
              <path d="M150 14v172M14 64h28a36 36 0 0 1 0 72H14M286 64h-28a36 36 0 0 0 0 72h28" />
              <circle cx="150" cy="100" r="30" />
            </g>
          </svg>
        }
      </div>
      <div class="pt-3">
        <div class="flex items-start gap-3">
          <h3 class="min-w-0 flex-1 font-display text-[28px] font-semibold leading-none decoration-2 underline-offset-4 group-hover:underline">{{ v.name }}</h3>
          @if (v.reviewCount > 0) {
            <span class="tabular flex shrink-0 items-center gap-1 pt-1 text-sm font-medium" [attr.aria-label]="l().ratingFrom(v.averageRating, l().reviews(v.reviewCount))">
              <lucide-icon [img]="icons.Star" [size]="14" class="fill-current text-held-line" />{{ digits(v.averageRating.toFixed(1)) }}
              <span class="font-normal text-subtle">({{ digits(v.reviewCount) }})</span>
            </span>
          }
        </div>
        <p class="mt-1 flex items-center gap-1 text-muted">
          <lucide-icon [img]="icons.MapPin" [size]="15" />{{ v.area ? v.area + ', ' : '' }}{{ v.city }}
        </p>
        <p class="mt-1 flex flex-wrap items-baseline gap-x-3 text-[15px] text-muted">
          @if (v.fromPrice !== null) {
            <span class="tabular font-display text-[22px] font-semibold text-text">{{ l().fromPrice }}{{ rupees(v.fromPrice) }}</span>
          }
          <span>{{ l().courts(v.courtCount) }}</span>
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

  /** Where the lighter mown stripes start, every other 30 of the drawn court's 300 */
  protected readonly stripes = [0, 60, 120, 180, 240];
}
