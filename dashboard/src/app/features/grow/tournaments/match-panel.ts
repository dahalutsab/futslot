import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom, Observable } from 'rxjs';

import { problemOf } from '../../../core/api-error';
import { TournamentDetail, TournamentMatch } from '../../../core/api.types';
import { dayLabel, localClock } from '../../../shared/format';
import { Sheet } from '../../../shared/sheet';
import { decided, isKnockout } from './tournament-model';

/**
 * One fixture: book a court and time for it (which blocks the court for players), and enter the score or a
 * walkover once it's played.
 */
@Component({
  selector: 'fs-match-panel',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let m = match();
    <fs-sheet [title]="(m.home?.name ?? 'To be decided') + ' v ' + (m.away?.name ?? 'To be decided')" [subtitle]="subtitle()" (closed)="closed.emit()">
      <section aria-labelledby="when-title">
        <h3 id="when-title" class="font-medium">When and where</h3>
        <form class="mt-2 space-y-3" (submit)="$event.preventDefault(); schedule()">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="m-date" class="mb-1 block text-sm font-medium">Day</label>
              <input id="m-date" type="date" [min]="tournament().startsOn" [max]="tournament().endsOn" [value]="date()" (input)="date.set($any($event.target).value)"
                class="h-11 w-full rounded-md border border-border bg-surface px-2 text-base" />
            </div>
            <div>
              <label for="m-time" class="mb-1 block text-sm font-medium">Kick-off</label>
              <input id="m-time" type="time" step="900" [value]="time()" (input)="time.set($any($event.target).value)"
                class="tabular h-11 w-full rounded-md border border-border bg-surface px-2 text-base" />
            </div>
          </div>
          <div>
            <label for="m-court" class="mb-1 block text-sm font-medium">Court</label>
            <select id="m-court" (change)="courtId.set($any($event.target).value)" class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base">
              @for (court of courts(); track court.id) {
                <option [value]="court.id" [selected]="courtId() === court.id">{{ court.name }}</option>
              }
            </select>
          </div>
          <p class="text-sm text-subtle">The court is blocked for {{ tournament().matchMinutes }} minutes so players can't book it.</p>
          <div class="flex flex-wrap gap-2">
            <button type="submit" [disabled]="busy()" class="h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2 disabled:opacity-60">
              {{ m.scheduledAt ? 'Move match' : 'Book court' }}
            </button>
            @if (m.scheduledAt && !decided(m)) {
              <button type="button" (click)="unschedule()" [disabled]="busy()" class="h-11 rounded-md px-3 font-medium text-muted hover:bg-surface-2">Free the court</button>
            }
          </div>
        </form>
      </section>

      @if (m.home && m.away) {
        <section class="mt-6 border-t border-border pt-4" aria-labelledby="result-title">
          <h3 id="result-title" class="font-medium">{{ decided(m) ? 'Result' : 'Enter the result' }}</h3>
          <form class="mt-2" (submit)="$event.preventDefault(); saveResult()">
            <div class="grid grid-cols-[1fr_auto_1fr] items-end gap-3">
              <div>
                <label for="m-home" class="mb-1 block truncate text-sm font-medium">{{ m.home.name }}</label>
                <input id="m-home" type="number" inputmode="numeric" min="0" max="99" [value]="home()" (input)="home.set($any($event.target).value)"
                  class="tabular h-14 w-full rounded-md border border-border bg-surface text-center text-2xl font-semibold" />
              </div>
              <span class="pb-3 text-muted">–</span>
              <div>
                <label for="m-away" class="mb-1 block truncate text-sm font-medium">{{ m.away.name }}</label>
                <input id="m-away" type="number" inputmode="numeric" min="0" max="99" [value]="away()" (input)="away.set($any($event.target).value)"
                  class="tabular h-14 w-full rounded-md border border-border bg-surface text-center text-2xl font-semibold" />
              </div>
            </div>
            @if (needsPenalties()) {
              <p class="mt-3 text-sm text-muted">Level in a knockout: who won on penalties?</p>
              <div class="mt-1 grid grid-cols-[1fr_auto_1fr] items-center gap-3">
                <input type="number" inputmode="numeric" min="0" max="99" [value]="homePens()" (input)="homePens.set($any($event.target).value)" [attr.aria-label]="m.home.name + ' penalties'"
                  class="tabular h-11 w-full rounded-md border border-border bg-surface text-center text-lg" />
                <span class="text-muted">pens</span>
                <input type="number" inputmode="numeric" min="0" max="99" [value]="awayPens()" (input)="awayPens.set($any($event.target).value)" [attr.aria-label]="m.away.name + ' penalties'"
                  class="tabular h-11 w-full rounded-md border border-border bg-surface text-center text-lg" />
              </div>
            }
            <button type="submit" [disabled]="busy()" class="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
              @if (busy()) {
                <lucide-icon [img]="spinner" [size]="18" class="animate-spin" />
              }
              {{ decided(m) ? 'Correct the result' : 'Save result' }}
            </button>
          </form>
          @if (!decided(m)) {
            <div class="mt-4">
              <p class="text-sm text-muted">Didn't turn up? Give the other team the match (3–0):</p>
              <div class="mt-2 flex flex-wrap gap-2">
                <button type="button" (click)="walkover(m.home.registrationId)" [disabled]="busy()" class="h-10 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">{{ m.home.name }} wins</button>
                <button type="button" (click)="walkover(m.away.registrationId)" [disabled]="busy()" class="h-10 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">{{ m.away.name }} wins</button>
              </div>
            </div>
          }
        </section>
      } @else {
        <p class="mt-6 border-t border-border pt-4 text-sm text-muted">The teams are known once the earlier matches are played.</p>
      }

      @if (error(); as message) {
        <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }
    </fs-sheet>
  `,
})
export class MatchPanel implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  readonly tournament = input.required<TournamentDetail>();

  readonly match = input.required<TournamentMatch>();

  readonly courts = input.required<{ id: string; name: string }[]>();

  readonly done = output<TournamentDetail>();

  readonly closed = output<void>();

  protected readonly decided = decided;

  protected readonly spinner = LoaderCircle;

  protected readonly date = signal('');

  protected readonly time = signal('17:00');

  protected readonly courtId = signal('');

  protected readonly home = signal('');

  protected readonly away = signal('');

  protected readonly homePens = signal('');

  protected readonly awayPens = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly needsPenalties = computed(() => isKnockout(this.match().stage) && this.home() !== '' && this.home() === this.away());

  protected readonly subtitle = computed(() => {
    const m = this.match();
    const when = m.scheduledAt ? `${dayLabel(m.scheduledAt.slice(0, 10))} · ${localClock(m.scheduledAt)}${m.court ? ' · ' + m.court.name : ''}` : 'Not booked yet';
    return when;
  });

  private base = '';

  ngOnInit(): void {
    const m = this.match();
    this.base = `/api/v1/owner/venues/${this.venueId()}/tournaments/${this.tournament().id}/matches/${m.id}`;
    this.date.set(m.scheduledAt?.slice(0, 10) ?? this.tournament().startsOn);
    this.time.set(m.scheduledAt?.slice(11, 16) ?? '17:00');
    this.courtId.set(m.court?.id ?? this.courts()[0]?.id ?? '');
    this.home.set(m.homeScore?.toString() ?? '');
    this.away.set(m.awayScore?.toString() ?? '');
    this.homePens.set(m.homePenalties?.toString() ?? '');
    this.awayPens.set(m.awayPenalties?.toString() ?? '');
  }

  protected schedule(): Promise<void> {
    if (!this.date() || !this.time() || !this.courtId()) {
      this.error.set('Pick a day, a kick-off time and a court.');
      return Promise.resolve();
    }
    return this.send(() => this.http.put<TournamentDetail>(`${this.base}/schedule`,
      { courtId: this.courtId(), startsAt: `${this.date()}T${this.time()}:00+05:45` }));
  }

  protected unschedule(): Promise<void> {
    return this.send(() => this.http.delete<TournamentDetail>(`${this.base}/schedule`));
  }

  protected saveResult(): Promise<void> {
    if (this.home() === '' || this.away() === '') {
      this.error.set('Enter both scores.');
      return Promise.resolve();
    }
    const pens = this.needsPenalties();
    if (pens && (this.homePens() === '' || this.awayPens() === '' || this.homePens() === this.awayPens())) {
      this.error.set('A knockout match needs a winner: enter the penalty score.');
      return Promise.resolve();
    }
    return this.send(() => this.http.post<TournamentDetail>(`${this.base}/result`, {
      homeScore: Number(this.home()), awayScore: Number(this.away()),
      homePenalties: pens ? Number(this.homePens()) : null, awayPenalties: pens ? Number(this.awayPens()) : null,
    }));
  }

  protected walkover(winnerRegistrationId: string): Promise<void> {
    return this.send(() => this.http.post<TournamentDetail>(`${this.base}/walkover`, { winnerRegistrationId }));
  }

  private async send(request: () => Observable<TournamentDetail>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      this.done.emit(await firstValueFrom(request()));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }
}
