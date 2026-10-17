import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Review, ReviewStatus } from '../../core/api.types';
import { ago, dayLabel } from '../../shared/format';
import { REVIEW_STATUS } from '../../shared/labels';

/**
 * Keeping reviews fair. Hide one while checking it, remove one that breaks the rules, or publish it again. A venue's
 * rating counts published reviews only.
 */
@Component({
  selector: 'fs-reviews',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-2xl font-semibold tracking-tight">Reviews</h1>
      <div class="mt-4 flex gap-1 rounded-lg bg-surface-2 p-1" role="tablist" aria-label="Review status">
        @for (s of statuses; track s) {
          <button type="button" role="tab" [attr.aria-selected]="status() === s" (click)="status.set(s)"
            class="h-10 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="status() === s" [class.text-text]="status() === s">{{ label[s] }}</button>
        }
      </div>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }
      @if (reviews(); as list) {
        @if (list.length === 0) {
          <p class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center text-muted">No {{ label[status()].toLowerCase() }} reviews.</p>
        }
        <ul class="mt-4 space-y-3">
          @for (r of list; track r.id) {
            <li class="rounded-xl border border-border bg-surface p-4">
              <div class="flex flex-wrap items-center gap-x-3">
                <a [routerLink]="['/venues', r.venueId]" class="min-w-0 flex-1 font-medium hover:underline">{{ r.venueName }}</a>
                <span class="text-held-line" [attr.aria-label]="r.rating + ' out of 5'">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</span>
              </div>
              @if (r.comment) {
                <p class="mt-2 whitespace-pre-line">{{ r.comment }}</p>
              }
              <p class="mt-2 text-sm text-muted">{{ r.reviewerName }} · played {{ fmt.dayLabel(r.playedOn) }} · {{ fmt.ago(r.createdAt) }}</p>
              @if (r.ownerReply) {
                <p class="mt-2 rounded-lg bg-surface-2 px-3 py-2 text-sm"><span class="font-medium">Venue replied:</span> {{ r.ownerReply }}</p>
              }
              <div class="mt-3 flex flex-wrap gap-2">
                @for (s of statuses; track s) {
                  @if (s !== r.status) {
                    <button type="button" (click)="moderate(r, s)" [disabled]="busy() === r.id"
                      class="h-10 rounded-lg border px-4 text-sm font-medium disabled:opacity-60"
                      [class]="s === 'REMOVED' ? 'border-border text-danger hover:bg-danger-bg' : 'border-border hover:bg-surface-2'">{{ verb[s] }}</button>
                  }
                }
              </div>
            </li>
          }
        </ul>
      } @else if (!error()) {
        <div class="mt-6 h-40 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Reviews {
  private readonly api = inject(Api);

  protected readonly fmt = { ago, dayLabel };

  protected readonly statuses: ReviewStatus[] = ['PUBLISHED', 'HIDDEN', 'REMOVED'];

  protected readonly label = REVIEW_STATUS;

  protected readonly verb: Record<ReviewStatus, string> = { PUBLISHED: 'Publish', HIDDEN: 'Hide', REMOVED: 'Remove' };

  protected readonly status = signal<ReviewStatus>('PUBLISHED');

  protected readonly reviews = signal<Review[] | null>(null);

  protected readonly busy = signal<string | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const status = this.status();
      untracked(() => void this.load(status));
    });
  }

  protected async moderate(review: Review, status: ReviewStatus): Promise<void> {
    this.busy.set(review.id);
    this.error.set(null);
    try {
      await this.api.moderate(review.id, status);
      this.reviews.update((list) => list?.filter((r) => r.id !== review.id) ?? null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(null);
    }
  }

  private async load(status: ReviewStatus): Promise<void> {
    this.reviews.set(null);
    try {
      this.reviews.set((await this.api.reviews(status)).content);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
