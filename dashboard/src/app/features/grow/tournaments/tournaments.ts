import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Plus, Trophy } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../../core/api-error';
import { TournamentDetail, TournamentSummary } from '../../../core/api.types';
import { VenueContext } from '../../../core/venue-context';
import { dayLabel, rupees } from '../../../shared/format';
import { TournamentForm } from './tournament-form';
import { formatLabel, STATUS_LABELS } from './tournament-model';

/** The venue's tournaments, newest first, and starting a new one. */
@Component({
  selector: 'fs-tournaments',
  imports: [LucideAngularModule, RouterLink, TournamentForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-4 lg:px-8 lg:py-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-xl font-semibold tracking-tight lg:text-2xl">Tournaments</h1>
          <p class="mt-1 text-muted">Teams sign up and pay in the app. You make the draw, book the courts and enter results.</p>
        </div>
        @if (list()) {
          <button type="button" (click)="creating.set(true)" class="flex h-11 items-center gap-2 rounded-lg bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
            <lucide-icon [img]="icons.Plus" [size]="18" />New tournament
          </button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-5 rounded-xl border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
      } @else if (list(); as tournaments) {
        @if (tournaments.length === 0) {
          <div class="mt-5 rounded-xl border border-dashed border-border bg-surface px-6 py-10 text-center">
            <lucide-icon [img]="icons.Trophy" [size]="28" class="mx-auto text-subtle" />
            <p class="mt-3 font-medium">No tournaments yet</p>
            <p class="mt-1 text-muted">A weekend knockout fills quiet hours and brings new teams to your venue.</p>
          </div>
        } @else {
          <ul class="mt-5 grid gap-3 sm:grid-cols-2">
            @for (t of tournaments; track t.id) {
              <li>
                <a [routerLink]="t.id" class="flex h-full flex-col rounded-xl border border-border bg-surface p-4 hover:bg-surface-2">
                  <span class="flex items-start justify-between gap-3">
                    <span class="font-semibold">{{ t.name }}</span>
                    <span class="shrink-0 rounded-full bg-surface-2 px-2 py-0.5 text-xs font-medium text-muted">{{ statusLabels[t.status] }}</span>
                  </span>
                  <span class="mt-1 text-sm text-muted">{{ format(t.format) }} · {{ fmt.dayLabel(t.startsOn) }}{{ t.endsOn !== t.startsOn ? ' – ' + fmt.dayLabel(t.endsOn) : '' }}</span>
                  <span class="mt-3 flex items-baseline justify-between text-sm">
                    <span class="tabular">{{ t.teamsIn }} of {{ t.maxTeams }} teams</span>
                    <span class="tabular text-muted">{{ t.entryFee > 0 ? fmt.rupees(t.entryFee) + ' entry' : 'Free entry' }}</span>
                  </span>
                </a>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-5 h-40 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>

    @if (creating() && venueId(); as id) {
      <fs-tournament-form [venueId]="id" (saved)="created($event)" (closed)="creating.set(false)" />
    }
  `,
})
export class Tournaments {
  private readonly http = inject(HttpClient);

  private readonly router = inject(Router);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { Plus, Trophy };

  protected readonly fmt = { dayLabel, rupees };

  protected readonly statusLabels = STATUS_LABELS;

  protected readonly format = formatLabel;

  protected readonly list = signal<TournamentSummary[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly creating = signal(false);

  protected readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected created(tournament: TournamentDetail): void {
    this.creating.set(false);
    void this.router.navigate(['/grow/tournaments', tournament.id]);
  }

  private async load(id: string): Promise<void> {
    try {
      this.list.set(await firstValueFrom(this.http.get<TournamentSummary[]>(`/api/v1/owner/venues/${id}/tournaments`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
