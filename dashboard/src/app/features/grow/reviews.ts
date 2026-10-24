import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { afterNextRender, ChangeDetectionStrategy, Component, computed, effect, inject, Injector, signal, untracked } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Page, RatingSummary, Review } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { ago, dayLabel } from '../../shared/format';

/**
 * What players say after playing. Replies are public and signed as the venue; a kind, specific reply to a
 * low rating does more for the next player than the rating itself.
 */
@Component({
  selector: 'fs-reviews',
  imports: [DecimalPipe, LucideAngularModule, NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-3xl lg:text-4xl">Reviews</h1>
      <p class="mt-1 text-muted">Players can review a game they played. Your replies are public and signed as {{ venueName() }}.</p>

      @if (summary(); as s) {
        <section class="mt-5 grid gap-6 rounded-md border border-border bg-surface p-4 sm:grid-cols-[auto_1fr] lg:p-6" aria-label="Rating">
          <div>
            <p class="tabular text-4xl font-semibold">{{ s.reviewCount ? (s.averageRating ?? 0).toFixed(1) : '–' }}</p>
            <ng-container [ngTemplateOutlet]="stars" [ngTemplateOutletContext]="{ rating: s.averageRating ?? 0, size: 'size-5' }" />
            <p class="mt-1 text-sm text-muted">{{ s.reviewCount }} {{ s.reviewCount === 1 ? 'review' : 'reviews' }}</p>
          </div>
          <ul class="space-y-1.5 self-center" aria-label="Reviews by stars">
            @for (row of bars(); track row.stars) {
              <li class="grid grid-cols-[3rem_1fr_2rem] items-center gap-3 text-sm">
                <span class="text-muted">{{ row.stars }} star</span>
                <span class="h-2 overflow-hidden rounded-full bg-surface-2">
                  <span class="block h-full rounded-full bg-viz" [style.width.%]="row.share"></span>
                </span>
                <span class="tabular text-right">{{ row.count }}</span>
              </li>
            }
          </ul>
        </section>
      }

      <div class="mt-5 flex rounded-md bg-surface-2 p-1 sm:w-fit" role="radiogroup" aria-label="Show">
        @for (option of filters; track option.label) {
          <button type="button" role="radio" [attr.aria-checked]="unanswered() === option.value" (click)="unanswered.set(option.value)"
            class="h-9 flex-1 whitespace-nowrap rounded-md px-3 text-sm font-medium text-muted" [class.bg-surface]="unanswered() === option.value" [class.text-text]="unanswered() === option.value">
            {{ option.label }}
          </button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-4 rounded-md border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
      } @else if (reviews(); as list) {
        @if (list.length === 0) {
          <p class="mt-4 rounded-md border border-dashed border-border bg-surface px-6 py-8 text-center text-muted">
            {{ unanswered() ? 'You have replied to every review.' : 'No reviews yet. Players are asked for one after they play.' }}
          </p>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (review of list; track review.id) {
              <li class="rounded-md border border-border bg-surface p-4">
                <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                  <ng-container [ngTemplateOutlet]="stars" [ngTemplateOutletContext]="{ rating: review.rating, size: 'size-4' }" />
                  <span class="font-medium">{{ review.reviewerName }}</span>
                  <span class="text-sm text-subtle">played {{ fmt.dayLabel(review.playedOn) }} · {{ fmt.ago(review.createdAt) }}</span>
                  @if (review.status !== 'PUBLISHED') {
                    <span class="rounded-full bg-surface-2 px-2 py-0.5 text-xs font-medium text-muted">{{ review.status === 'HIDDEN' ? 'Hidden by FutSlot' : 'Removed by FutSlot' }}</span>
                  }
                </div>
                @if (review.comment) {
                  <p class="mt-2 whitespace-pre-line">{{ review.comment }}</p>
                } @else {
                  <p class="mt-2 text-sm text-subtle">Rating only, no comment.</p>
                }

                @if (editingId() === review.id) {
                  <form class="mt-3" (submit)="$event.preventDefault(); saveReply(review)">
                    <label [for]="'reply-' + review.id" class="sr-only">Reply to {{ review.reviewerName }}</label>
                    <textarea [id]="'reply-' + review.id" rows="3" maxlength="1000" [value]="draft()" (input)="draft.set($any($event.target).value)"
                      placeholder="Thank them, and say what you'll do about anything they raised."
                      class="w-full rounded-md border border-border bg-surface px-3 py-2 text-base"></textarea>
                    @if (replyError(); as message) {
                      <p class="mt-1 text-sm text-danger" role="alert">{{ message }}</p>
                    }
                    <div class="mt-2 flex gap-2">
                      <button type="submit" [disabled]="busy()" class="flex h-10 items-center gap-2 rounded-md bg-ink px-4 text-sm font-medium text-ink-fg disabled:opacity-60">
                        @if (busy()) {
                          <lucide-icon [img]="icons.LoaderCircle" [size]="16" class="animate-spin" />
                        }
                        Post reply
                      </button>
                      <button type="button" (click)="editingId.set(null)" class="h-10 rounded-md border border-border px-3 text-sm font-medium">Cancel</button>
                    </div>
                  </form>
                } @else if (review.ownerReply) {
                  <div class="mt-3 border-l-2 border-border pl-3">
                    <p class="text-sm font-medium">{{ venueName() }} replied <span class="font-normal text-subtle">{{ review.ownerRepliedAt ? fmt.ago(review.ownerRepliedAt) : '' }}</span></p>
                    <p class="mt-0.5 whitespace-pre-line text-muted">{{ review.ownerReply }}</p>
                    @if (review.status !== 'REMOVED') {
                      <div class="mt-1 flex gap-1">
                        <button type="button" (click)="startReply(review)" class="h-9 rounded-md px-2 text-sm font-medium hover:bg-surface-2">Edit</button>
                        @if (confirmDelete() === review.id) {
                          <button type="button" (click)="deleteReply(review)" [disabled]="busy()" class="h-9 rounded-md px-2 text-sm font-medium text-danger hover:bg-danger-bg">Delete reply?</button>
                          <button type="button" (click)="confirmDelete.set(null)" class="h-9 rounded-md px-2 text-sm font-medium hover:bg-surface-2">Keep</button>
                        } @else {
                          <button type="button" (click)="confirmDelete.set(review.id)" class="h-9 rounded-md px-2 text-sm font-medium text-muted hover:bg-surface-2">Delete</button>
                        }
                      </div>
                    }
                  </div>
                } @else if (review.status !== 'REMOVED') {
                  <button type="button" (click)="startReply(review)" class="mt-3 h-10 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">Reply</button>
                }
              </li>
            }
          </ul>
          @if (hasMore()) {
            <button type="button" (click)="more()" class="mt-3 h-11 w-full rounded-md border border-border font-medium hover:bg-surface-2">Show more</button>
          }
        }
      } @else {
        <div class="mt-4 h-40 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>

    <ng-template #stars let-rating="rating" let-size="size">
      <span class="flex gap-0.5" role="img" [attr.aria-label]="(rating | number: '1.0-1') + ' out of 5 stars'">
        @for (n of [1, 2, 3, 4, 5]; track n) {
          <svg viewBox="0 0 24 24" [class]="size" aria-hidden="true">
            <defs>
              <linearGradient [id]="'star-' + n + '-' + size + '-' + rating">
                <stop [attr.offset]="fill(rating, n)" stop-color="currentColor" />
                <stop [attr.offset]="fill(rating, n)" stop-color="currentColor" stop-opacity="0.2" />
              </linearGradient>
            </defs>
            <path d="M12 2.5l2.9 6.1 6.6.8-4.9 4.6 1.3 6.6L12 17.3l-5.9 3.3 1.3-6.6-4.9-4.6 6.6-.8z"
              [attr.fill]="'url(#star-' + n + '-' + size + '-' + rating + ')'" />
          </svg>
        }
      </span>
    </ng-template>
  `,
})
export class Reviews {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  private readonly injector = inject(Injector);

  protected readonly icons = { LoaderCircle };

  protected readonly fmt = { ago, dayLabel };

  protected readonly filters: { value: boolean; label: string }[] = [
    { value: false, label: 'All reviews' },
    { value: true, label: 'Needs a reply' },
  ];

  protected readonly summary = signal<RatingSummary | null>(null);

  protected readonly reviews = signal<Review[] | null>(null);

  protected readonly hasMore = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly unanswered = signal(false);

  protected readonly editingId = signal<string | null>(null);

  protected readonly draft = signal('');

  protected readonly busy = signal(false);

  protected readonly replyError = signal<string | null>(null);

  protected readonly confirmDelete = signal<string | null>(null);

  protected readonly venueName = computed(() => this.venues.current()?.venue.name ?? 'your venue');

  protected readonly bars = computed(() => {
    const stars = this.summary()?.stars ?? {};
    const max = Math.max(1, ...Object.values(stars));
    return [5, 4, 3, 2, 1].map((n) => ({ stars: n, count: stars[n] ?? 0, share: ((stars[n] ?? 0) / max) * 100 }));
  });

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  private page = 0;

  constructor() {
    effect(() => {
      const id = this.venueId();
      const unanswered = this.unanswered();
      if (id) {
        untracked(() => void this.load(id, unanswered, true));
      }
    });
  }

  /** How much of star {@code n} is filled for a rating, as a gradient stop. */
  protected fill(rating: number, n: number): string {
    return `${Math.round(Math.min(1, Math.max(0, rating - (n - 1))) * 100)}%`;
  }

  protected startReply(review: Review): void {
    this.replyError.set(null);
    this.confirmDelete.set(null);
    this.draft.set(review.ownerReply ?? '');
    this.editingId.set(review.id);
    afterNextRender(() => document.getElementById(`reply-${review.id}`)?.focus(), { injector: this.injector });
  }

  protected async saveReply(review: Review): Promise<void> {
    const id = this.venueId();
    if (!id || this.busy()) {
      return;
    }
    if (!this.draft().trim()) {
      this.replyError.set('Write a reply first.');
      return;
    }
    this.busy.set(true);
    this.replyError.set(null);
    try {
      const saved = await firstValueFrom(this.http.put<Review>(`/api/v1/owner/venues/${id}/reviews/${review.id}/reply`, { text: this.draft().trim() }));
      this.replace(saved);
      this.editingId.set(null);
    }
    catch (error) {
      this.replyError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async deleteReply(review: Review): Promise<void> {
    const id = this.venueId();
    if (!id) {
      return;
    }
    this.busy.set(true);
    try {
      this.replace(await firstValueFrom(this.http.delete<Review>(`/api/v1/owner/venues/${id}/reviews/${review.id}/reply`)));
      this.confirmDelete.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected more(): void {
    const id = this.venueId();
    if (id) {
      void this.load(id, this.unanswered(), false);
    }
  }

  private replace(saved: Review): void {
    this.reviews.update((list) => (list ?? []).map((r) => (r.id === saved.id ? saved : r)));
  }

  private async load(id: string, unanswered: boolean, reset: boolean): Promise<void> {
    if (reset) {
      this.page = 0;
      this.reviews.set(null);
    }
    try {
      const slug = this.venues.current()!.venue.slug;
      const [page, summary] = await Promise.all([
        firstValueFrom(this.http.get<Page<Review>>(`/api/v1/owner/venues/${id}/reviews`, { params: { unanswered, page: this.page, size: 20 } })),
        reset ? firstValueFrom(this.http.get<RatingSummary>(`/api/v1/venues/${slug}/reviews/summary`)) : Promise.resolve(this.summary()),
      ]);
      this.summary.set(summary);
      this.reviews.update((list) => (reset ? page.content : [...(list ?? []), ...page.content]));
      this.hasMore.set(page.page + 1 < page.totalPages);
      this.page = page.page + 1;
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
