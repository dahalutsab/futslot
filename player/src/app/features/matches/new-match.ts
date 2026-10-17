import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Booking, MatchPostRequest, MatchPostType, SkillLevel, Team, VenueSummary } from '../../core/api.types';
import { dayLabel, localClock } from '../../shared/format';
import { SKILL } from '../../shared/labels';
import { PlayNav } from '../../shared/play-nav';

/** Nepal's offset, for a time picked without one. */
const NEPAL_OFFSET = '+05:45';

/**
 * Posting a game: short of players for one of your bookings, or your team looking for an opponent. Where and
 * when come from the booking, or when nothing's booked yet, from a venue (or just a city) and a time.
 */
@Component({
  selector: 'fs-new-match',
  imports: [FormsModule, RouterLink, PlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      <a routerLink="/matches" class="mt-4 inline-block text-sm text-muted hover:text-text">← Games</a>
      <h1 class="mt-2 text-2xl font-semibold tracking-tight">Post a game</h1>

      <form class="mt-5 space-y-5" (ngSubmit)="post()">
        <fieldset>
          <legend class="mb-2 font-semibold">What do you need?</legend>
          <div class="grid grid-cols-2 gap-2">
            @for (t of types; track t.value) {
              <button type="button" (click)="type.set(t.value)" [attr.aria-pressed]="type() === t.value"
                class="rounded-lg border p-3 text-left" [class]="type() === t.value ? 'border-ink bg-surface' : 'border-border bg-surface text-muted'">
                <span class="block font-medium">{{ t.label }}</span><span class="block text-sm">{{ t.detail }}</span>
              </button>
            }
          </div>
        </fieldset>

        @if (type() === 'FIND_OPPONENT') {
          @if (teams().length === 0) {
            <p class="rounded-lg bg-held-bg px-3 py-2 text-held-fg">You need to captain a team to find an opponent. <a routerLink="/teams" class="font-medium underline">Start a team</a></p>
          } @else {
            <div>
              <label for="team" class="mb-1.5 block font-medium">Your team</label>
              <select id="team" name="team" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="teamId()" (ngModelChange)="teamId.set($event)">
                @for (t of teams(); track t.id) {
                  <option [value]="t.id">{{ t.name }}</option>
                }
              </select>
            </div>
          }
        }

        <fieldset>
          <legend class="mb-2 font-semibold">Where and when</legend>
          @if (bookings().length) {
            <div class="space-y-2">
              @for (b of bookings(); track b.id) {
                <label class="flex cursor-pointer items-center gap-3 rounded-lg border border-border bg-surface p-3 has-[:checked]:border-ink">
                  <input type="radio" name="where" [checked]="bookingId() === b.id" (change)="bookingId.set(b.id)" />
                  <span><span class="block font-medium">{{ b.venue.name }}</span><span class="tabular block text-sm text-muted">{{ fmt.dayLabel(b.startsAt.slice(0, 10)) }} · {{ fmt.localClock(b.startsAt) }} · {{ b.court.name }}</span></span>
                </label>
              }
              <label class="flex cursor-pointer items-center gap-3 rounded-lg border border-border bg-surface p-3 has-[:checked]:border-ink">
                <input type="radio" name="where" [checked]="bookingId() === null" (change)="bookingId.set(null)" />
                <span class="font-medium">Not booked yet</span>
              </label>
            </div>
          }
          @if (bookingId() === null) {
            <div class="mt-3">
              <label for="venue" class="mb-1.5 block font-medium">Venue <span class="font-normal text-subtle">(optional)</span></label>
              <select id="venue" name="venue" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="venueId()" (ngModelChange)="venueId.set($event || null)">
                <option value="">Not decided: just a city</option>
                @for (v of venues(); track v.id) {
                  <option [value]="v.id">{{ v.name }}, {{ v.city }}</option>
                }
              </select>
            </div>
            <div class="mt-3 grid gap-3 sm:grid-cols-2">
              @if (!venueId()) {
                <div>
                  <label for="city" class="mb-1.5 block font-medium">City</label>
                  <input id="city" name="city" type="text" maxlength="60" placeholder="Kathmandu" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
                    [ngModel]="city()" (ngModelChange)="city.set($event)" />
                </div>
              }
              <div>
                <label for="when" class="mb-1.5 block font-medium">When</label>
                <input id="when" name="when" type="datetime-local" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
                  [ngModel]="when()" (ngModelChange)="when.set($event)" />
              </div>
            </div>
          }
        </fieldset>

        @if (type() === 'NEED_PLAYERS') {
          <div class="grid gap-3 sm:grid-cols-2">
            <div>
              <label for="needed" class="mb-1.5 block font-medium">Players needed</label>
              <input id="needed" name="needed" type="number" inputmode="numeric" min="1" max="20" class="tabular h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
                [ngModel]="needed()" (ngModelChange)="needed.set(+$event)" />
            </div>
            <div>
              <label for="cost" class="mb-1.5 block font-medium">Each pays <span class="font-normal text-subtle">(Rs, optional)</span></label>
              <input id="cost" name="cost" type="number" inputmode="decimal" min="0" class="tabular h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
                [ngModel]="cost()" (ngModelChange)="cost.set($event === '' || $event === null ? null : +$event)" />
            </div>
          </div>
        }

        <fieldset>
          <legend class="mb-1.5 font-medium">Level <span class="font-normal text-subtle">(optional)</span></legend>
          <div class="flex flex-wrap gap-2">
            @for (s of skills; track s.value) {
              <button type="button" (click)="skill.set(skill() === s.value ? null : s.value)" [attr.aria-pressed]="skill() === s.value"
                class="h-10 rounded-full border px-4 text-sm font-medium" [class]="skill() === s.value ? 'border-ink bg-ink text-ink-fg' : 'border-border text-muted'">{{ s.label }}</button>
            }
          </div>
        </fieldset>

        <div>
          <label for="note" class="mb-1.5 block font-medium">Note <span class="font-normal text-subtle">(optional)</span></label>
          <textarea id="note" name="note" rows="2" maxlength="500" placeholder="e.g. Friendly game, need a keeper" class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-base"
            [ngModel]="note()" (ngModelChange)="note.set($event)"></textarea>
        </div>

        @if (error(); as message) {
          <p class="text-danger" role="alert">{{ message }}</p>
        }
        <button type="submit" [disabled]="busy() || !canPost()" class="h-12 w-full rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">Post it</button>
      </form>
    </div>
  `,
})
export class NewMatch {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly fmt = { dayLabel, localClock };

  protected readonly types: { value: MatchPostType; label: string; detail: string }[] = [
    { value: 'NEED_PLAYERS', label: 'Players', detail: "We're short for a game" },
    { value: 'FIND_OPPONENT', label: 'An opponent', detail: 'Our team wants a match' },
  ];

  protected readonly skills = (Object.keys(SKILL) as SkillLevel[]).map((value) => ({ value, label: SKILL[value] }));

  protected readonly type = signal<MatchPostType>('NEED_PLAYERS');

  protected readonly bookings = signal<Booking[]>([]);

  protected readonly teams = signal<Team[]>([]);

  protected readonly bookingId = signal<string | null>(null);

  protected readonly teamId = signal<string | null>(null);

  protected readonly city = signal('');

  protected readonly venues = signal<VenueSummary[]>([]);

  protected readonly venueId = signal<string | null>(null);

  protected readonly when = signal('');

  protected readonly needed = signal(2);

  protected readonly cost = signal<number | null>(null);

  protected readonly skill = signal<SkillLevel | null>(null);

  protected readonly note = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly canPost = computed(() => this.type() === 'NEED_PLAYERS' || this.teams().length > 0);

  constructor() {
    Promise.all([this.api.bookings('upcoming'), this.api.myTeams()]).then(([bookings, teams]) => {
      const live = bookings.filter((b) => b.status === 'CONFIRMED' || b.status === 'PENDING_PAYMENT');
      this.bookings.set(live);
      this.bookingId.set(live[0]?.id ?? null);
      const led = teams.filter((t) => t.myStatus === 'ACTIVE' && (t.myRole === 'CAPTAIN' || t.myRole === 'VICE_CAPTAIN'));
      this.teams.set(led);
      this.teamId.set(led[0]?.id ?? null);
    }, (error) => this.error.set(problemOf(error).message));
    this.api.venues({ size: 50 }).then((page) => this.venues.set(page.content), () => undefined);
  }

  protected async post(): Promise<void> {
    const bookingId = this.bookingId();
    const venueId = bookingId ? null : this.venueId();
    if (!bookingId && (!this.when() || (!venueId && !this.city().trim()))) {
      this.error.set('Say where (a venue or a city) and when, or pick one of your bookings.');
      return;
    }
    const request: MatchPostRequest = {
      type: this.type(),
      teamId: this.type() === 'FIND_OPPONENT' ? this.teamId() ?? undefined : undefined,
      bookingId: bookingId ?? undefined,
      venueId: venueId ?? undefined,
      city: bookingId || venueId ? undefined : this.city().trim(),
      startsAt: bookingId ? undefined : `${this.when()}:00${NEPAL_OFFSET}`,
      playersNeeded: this.type() === 'NEED_PLAYERS' ? this.needed() : undefined,
      costPerPlayer: this.type() === 'NEED_PLAYERS' && this.cost() ? this.cost()! : undefined,
      skillLevel: this.skill() ?? undefined,
      note: this.note().trim() || undefined,
    };
    this.busy.set(true);
    this.error.set(null);
    try {
      const post = await this.api.createMatchPost(request);
      await this.router.navigate(['/matches', post.id]);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }
}
