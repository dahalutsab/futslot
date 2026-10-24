import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule, MapPin, Trophy } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MyTournamentEntry, TournamentSummary } from '../../core/api.types';
import { Session } from '../../core/session';
import { l } from '../../i18n/i18n';
import { dayLabel, rupees } from '../../shared/format';
import { entryStatusLabels, formatLabels, tournamentStatusLabels } from '../../shared/labels';
import { PlayNav } from '../../shared/play-nav';

type Tab = 'upcoming' | 'past' | 'mine';

/** Tournaments venues run: coming up, finished, and the ones the player's teams entered. */
@Component({
  selector: 'fs-tournaments',
  imports: [RouterLink, LucideAngularModule, PlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:py-10">
      @if (session.signedIn()) {
        <fs-play-nav />
      }
      <h1 class="mt-6 text-2xl font-semibold tracking-tight">{{ l().tournaments }}</h1>
      <div class="mt-4 flex gap-1 rounded-md bg-surface-2 p-1" role="tablist" [attr.aria-label]="l().tournaments">
        @for (t of tabs(); track t.value) {
          <button type="button" role="tab" [attr.aria-selected]="tab() === t.value" (click)="show(t.value)" class="h-10 flex-1 rounded-md font-medium text-muted"
            [class.bg-surface]="tab() === t.value" [class.text-text]="tab() === t.value">{{ t.label }}</button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (tab() === 'mine') {
        @if (entries(); as list) {
          @if (list.length === 0) {
            <p class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center text-muted">{{ l().teamsNotEntered }}</p>
          }
          <ul class="mt-4 space-y-3">
            @for (e of list; track e.entry.id) {
              <li>
                <a [routerLink]="['/tournaments', e.tournament.slug]" class="block rounded-md border border-border bg-surface p-4 hover:border-subtle">
                  <p class="font-semibold">{{ e.tournament.name }}</p>
                  <p class="text-sm text-muted">{{ e.entry.team.name }} · {{ fmt.dayLabel(e.tournament.startsOn) }}</p>
                  <p class="mt-2 text-sm font-medium" [class]="e.entry.status === 'PENDING_PAYMENT' ? 'text-held-fg' : e.entry.status === 'CONFIRMED' ? 'text-confirmed-fg' : 'text-muted'">{{ entryStatus()[e.entry.status] }}</p>
                </a>
              </li>
            }
          </ul>
        }
      } @else if (list(); as items) {
        @if (items.length === 0) {
          <p class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center text-muted">{{ tab() === 'upcoming' ? l().noTournamentsComingUp + '. ' + l().checkBackSoon : l().noFinishedTournaments }}</p>
        }
        <ul class="mt-4 grid gap-4 sm:grid-cols-2">
          @for (t of items; track t.id) {
            <li>
              <a [routerLink]="['/tournaments', t.slug]" class="block overflow-hidden rounded-md border border-border bg-surface hover:border-subtle">
                <div class="aspect-[16/7] bg-tournament-bg">
                  @if (t.bannerUrl) {
                    <img [src]="t.bannerUrl" alt="" loading="lazy" class="size-full object-cover" />
                  } @else {
                    <div class="grid size-full place-items-center text-tournament-fg"><lucide-icon [img]="icons.Trophy" [size]="32" /></div>
                  }
                </div>
                <div class="p-4">
                  <p class="text-xs font-medium text-tournament-fg">{{ status()[t.status] }}</p>
                  <p class="mt-1 font-semibold">{{ t.name }}</p>
                  <p class="mt-1 flex items-center gap-1 text-sm text-muted"><lucide-icon [img]="icons.MapPin" [size]="14" />{{ t.venue.name }}, {{ t.venue.city }}</p>
                  <p class="tabular mt-2 text-sm">{{ fmt.dayLabel(t.startsOn) }}{{ t.endsOn !== t.startsOn ? ' – ' + fmt.dayLabel(t.endsOn) : '' }} · {{ format()[t.format] }}</p>
                  <p class="tabular text-sm text-muted">{{ t.entryFee > 0 ? l().perTeam(fmt.rupees(t.entryFee)) : l().freeToEnter }} · {{ l().teamsOfMax(t.teamsIn, t.maxTeams) }}</p>
                </div>
              </a>
            </li>
          }
        </ul>
      } @else {
        <div class="mt-6 h-48 animate-pulse rounded-md bg-surface" [attr.aria-label]="l().loading"></div>
      }
    </div>
  `,
})
export class Tournaments {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly session = inject(Session);

  protected readonly icons = { MapPin, Trophy };

  protected readonly fmt = { dayLabel, rupees };

  protected readonly status = tournamentStatusLabels;

  protected readonly format = formatLabels;

  protected readonly entryStatus = entryStatusLabels;

  protected readonly tab = signal<Tab>(inject(ActivatedRoute).snapshot.queryParamMap.has('mine') ? 'mine' : 'upcoming');

  protected readonly tabs = () => [
    { value: 'upcoming' as Tab, label: l().comingUp },
    { value: 'past' as Tab, label: l().finished },
    ...(this.session.signedIn() ? [{ value: 'mine' as Tab, label: l().yourTeamsTab }] : []),
  ];

  protected readonly list = signal<TournamentSummary[] | null>(null);

  protected readonly entries = signal<MyTournamentEntry[] | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const tab = this.tab();
      untracked(() => void this.load(tab));
    });
  }

  protected show(tab: Tab): void {
    this.tab.set(tab);
    void this.router.navigate([], { queryParams: { mine: tab === 'mine' ? 1 : null }, replaceUrl: true });
  }

  private async load(tab: Tab): Promise<void> {
    this.error.set(null);
    try {
      if (tab === 'mine') {
        this.entries.set(await this.api.myEntries());
      } else {
        this.list.set(null);
        this.list.set(await this.api.tournaments(tab === 'upcoming'));
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
