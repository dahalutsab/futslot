import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Star } from 'lucide-angular';

import { ReviewWrite } from '../core/api.types';

const WORDS = ['', 'Poor', 'Not great', 'Okay', 'Good', 'Great'];

/** Stars (required) and a comment (optional). */
@Component({
  selector: 'fs-review-form',
  imports: [FormsModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="space-y-3" (ngSubmit)="submit()">
      <fieldset>
        <legend class="sr-only">Rating</legend>
        <div class="flex items-center gap-1" role="radiogroup" aria-label="Rating">
          @for (n of [1, 2, 3, 4, 5]; track n) {
            <button type="button" role="radio" [attr.aria-checked]="rating() === n" [attr.aria-label]="n + ' out of 5: ' + words[n]"
              (click)="rating.set(n)" class="grid size-11 place-items-center rounded-md hover:bg-surface-2">
              <lucide-icon [img]="star" [size]="26" [class]="n <= rating() ? 'fill-current text-held-line' : 'text-subtle'" />
            </button>
          }
          <span class="ml-2 text-sm text-muted">{{ words[rating()] }}</span>
        </div>
      </fieldset>
      <div>
        <label [for]="id() + '-comment'" class="mb-1.5 block font-medium">What was it like? <span class="font-normal text-subtle">(optional)</span></label>
        <textarea [id]="id() + '-comment'" name="comment" rows="3" maxlength="1000" placeholder="The turf, the lights, the staff…"
          class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-base" [ngModel]="comment()" (ngModelChange)="comment.set($event)"></textarea>
      </div>
      @if (problem(); as message) {
        <p class="text-danger" role="alert">{{ message }}</p>
      }
      <div class="flex gap-2">
        <button type="submit" [disabled]="busy()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ busy() ? 'Saving…' : label() }}</button>
        <ng-content />
      </div>
    </form>
  `,
})
export class ReviewForm {
  readonly id = input('review');

  readonly initial = input<ReviewWrite | null>(null);

  readonly label = input('Post review');

  readonly busy = input(false);

  readonly error = input<string | null>(null);

  readonly saved = output<ReviewWrite>();

  protected readonly star = Star;

  protected readonly words = WORDS;

  protected readonly rating = signal(0);

  protected readonly comment = signal('');

  protected readonly problem = signal<string | null>(null);

  constructor() {
    effect(() => {
      const initial = this.initial();
      this.rating.set(initial?.rating ?? 0);
      this.comment.set(initial?.comment ?? '');
    });
    effect(() => this.problem.set(this.error()));
  }

  protected submit(): void {
    if (this.busy()) {
      return;
    }
    if (this.rating() === 0) {
      this.problem.set('Pick how many stars.');
      return;
    }
    this.problem.set(null);
    this.saved.emit({ rating: this.rating(), comment: this.comment().trim() || undefined });
  }
}
