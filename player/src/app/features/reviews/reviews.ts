import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Booking, Review } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { dayLabel, localClock } from '../../shared/format';

/** Games waiting for a review, and the reviews the player wrote (each edited from its booking). */
@Component({
  selector: 'fs-reviews',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">{{ l().meReviews }}</h1>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }

      @if (toReview().length) {
        <section class="mt-6" aria-labelledby="pending-title">
          <h2 id="pending-title" class="font-semibold">{{ l().howWasYourGame }}</h2>
          <p class="text-sm text-muted">{{ l().reviewWindow }}</p>
          <ul class="mt-3 space-y-2">
            @for (booking of toReview(); track booking.id) {
              <li>
                <a [routerLink]="['/bookings', booking.id]" fragment="review" class="flex items-center gap-3 rounded-xl border border-border bg-surface p-4 hover:border-subtle">
                  <span class="min-w-0 flex-1">
                    <span class="block font-medium">{{ booking.venue.name }}</span>
                    <span class="tabular block text-sm text-muted">{{ fmt.dayLabel(booking.startsAt.slice(0, 10)) }} · {{ fmt.localClock(booking.startsAt) }}</span>
                  </span>
                  <span class="text-sm font-medium">{{ l().review }}</span>
                </a>
              </li>
            }
          </ul>
        </section>
      }

      <section class="mt-8" aria-labelledby="mine-title">
        <h2 id="mine-title" class="font-semibold">{{ l().yourReviews }}</h2>
        @if (mine(); as list) {
          @if (list.length === 0) {
            <p class="mt-2 text-muted">{{ l().noReviewsWritten }}</p>
          } @else {
            <ul class="mt-3 space-y-3">
              @for (review of list; track review.id) {
                <li class="rounded-xl border border-border bg-surface p-4">
                  <div class="flex items-center gap-3">
                    <p class="min-w-0 flex-1 font-medium">{{ review.venueName }}</p>
                    <span class="text-held-line" [attr.aria-label]="l().starsOutOfFive(review.rating)">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</span>
                  </div>
                  @if (review.comment) {
                    <p class="mt-2 whitespace-pre-line">{{ review.comment }}</p>
                  }
                  @if (review.status !== 'PUBLISHED') {
                    <p class="mt-2 text-sm text-noshow-fg">{{ review.status === 'HIDDEN' ? l().reviewHidden : l().reviewRemoved }}</p>
                  }
                  @if (review.ownerReply) {
                    <div class="mt-3 rounded-lg bg-surface-2 px-3 py-2 text-sm">
                      <p class="font-medium">{{ l().venueReplied(review.venueName) }}</p>
                      <p class="mt-0.5 whitespace-pre-line text-muted">{{ review.ownerReply }}</p>
                    </div>
                  }
                  <p class="mt-2 flex items-center gap-3 text-sm text-subtle">
                    {{ l().playedOn(fmt.dayLabel(review.playedOn)) }}
                    @if (review.bookingId) {
                      <a [routerLink]="['/bookings', review.bookingId]" fragment="review" class="flex min-h-11 items-center font-medium text-text underline-offset-2 hover:underline">{{ l().edit }}</a>
                    }
                  </p>
                </li>
              }
            </ul>
          }
        } @else {
          <div class="mt-3 h-32 animate-pulse rounded-xl bg-surface" [attr.aria-label]="l().loading"></div>
        }
      </section>
    </div>
  `,
})
export class Reviews {
  protected readonly l = l;

  private readonly api = inject(Api);

  protected readonly fmt = { dayLabel, localClock };

  protected readonly toReview = signal<Booking[]>([]);

  protected readonly mine = signal<Review[] | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    Promise.all([this.api.toReview(), this.api.myReviews()]).then(([pending, mine]) => {
      this.toReview.set(pending);
      this.mine.set(mine.content);
    }, (error) => this.error.set(problemOf(error).message));
  }
}
