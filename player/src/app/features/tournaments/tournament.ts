import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Trophy, Wallet } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Payment, PaymentClaim, Stage, Team, TournamentDetail, TournamentEntry, TournamentMatch } from '../../core/api.types';
import { Session } from '../../core/session';
import { dayLabel, localClock, rupees } from '../../shared/format';
import { ENTRY_STATUS, FORMAT, STAGE, TOURNAMENT_STATUS } from '../../shared/labels';
import { PayForm } from '../../shared/pay-form';
import { PlayNav } from '../../shared/play-nav';

/**
 * One tournament: what it is, entering a team (captains pick the squad), paying the entry fee to the venue,
 * and once it's on, the fixtures and tables.
 */
@Component({
  selector: 'fs-tournament',
  imports: [FormsModule, RouterLink, LucideAngularModule, PayForm, PlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament.html',
})
export class TournamentPage {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly session = inject(Session);

  readonly slug = input.required<string>();

  protected readonly icons = { Trophy, Wallet };

  protected readonly fmt = { dayLabel, localClock, rupees };

  protected readonly squadName = (p: TournamentEntry['squad'][number]) => (p.jerseyNumber !== null ? `${p.name} (${p.jerseyNumber})` : p.name);

  protected readonly status = TOURNAMENT_STATUS;

  protected readonly format = FORMAT;

  protected readonly stage = STAGE;

  protected readonly entryStatus = ENTRY_STATUS;

  protected readonly tournament = signal<TournamentDetail | null>(null);

  /** The venue's own wallet or account, for the entry fee. */
  protected readonly paymentInstructions = signal<string | null>(null);

  /** Entries of the player's teams in this tournament, whatever their state. */
  protected readonly mine = signal<TournamentEntry[]>([]);

  /** Fee payments reported for those entries, by entry id. */
  protected readonly payments = signal<Record<string, Payment[]>>({});

  /** Teams the player leads that could enter. */
  protected readonly teams = signal<Team[]>([]);

  protected readonly entering = signal(false);

  protected readonly teamId = signal<string | null>(null);

  /** Squad picks: jersey number (or empty) by player. */
  protected readonly squad = signal<Record<string, string>>({});

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly team = computed(() => this.teams().find((t) => t.id === this.teamId()) ?? null);

  protected readonly picked = computed(() => Object.keys(this.squad()).length);

  /** Fixtures by stage (and group), in order. */
  protected readonly rounds = computed(() => {
    const groups = new Map<string, TournamentMatch[]>();
    for (const match of this.tournament()?.matches ?? []) {
      if (match.bye) {
        continue;
      }
      const key = match.stage + (match.groupName ? ` ${match.groupName}` : '');
      groups.set(key, [...(groups.get(key) ?? []), match]);
    }
    return [...groups.entries()].map(([key, matches]) => ({
      key,
      label: STAGE[matches[0].stage as Stage] + (matches[0].groupName ? ` · Group ${matches[0].groupName}` : ''),
      matches,
    }));
  });

  constructor() {
    effect(() => {
      const slug = this.slug();
      untracked(() => void this.load(slug));
    });
  }

  protected startEntry(): void {
    if (!this.session.signedIn()) {
      void this.router.navigate(['/login'], { queryParams: { next: this.router.url } });
      return;
    }
    this.entering.set(true);
    this.pickTeam(this.teams()[0]?.id ?? null);
  }

  protected pickTeam(teamId: string | null): void {
    this.teamId.set(teamId);
    this.squad.set({});
  }

  protected toggle(userId: string): void {
    this.squad.update((squad) => {
      const next = { ...squad };
      if (userId in next) {
        delete next[userId];
      } else {
        next[userId] = '';
      }
      return next;
    });
  }

  protected jersey(userId: string, value: string): void {
    this.squad.update((squad) => ({ ...squad, [userId]: value }));
  }

  protected async enter(): Promise<void> {
    const t = this.tournament();
    const teamId = this.teamId();
    if (!t || !teamId || this.busy()) {
      return;
    }
    if (this.picked() < t.teamSize) {
      this.error.set(`Pick at least ${t.teamSize} players.`);
      return;
    }
    const squad = Object.entries(this.squad()).map(([userId, number]) => ({ userId, jerseyNumber: number === '' ? undefined : +number }));
    await this.act(async () => {
      this.tournament.set(await this.api.register(t.id, teamId, squad));
      this.entering.set(false);
      await this.loadMine(t.id);
    });
  }

  protected pay(entry: TournamentEntry, claim: PaymentClaim): Promise<void> {
    return this.act(async () => {
      await this.api.payEntry(entry.id, claim);
      await this.load(this.slug());
    });
  }

  protected withdraw(entry: TournamentEntry): Promise<void> {
    const t = this.tournament();
    if (!t) {
      return Promise.resolve();
    }
    return this.act(async () => {
      this.tournament.set(await this.api.withdrawEntry(t.id, entry.id));
      await this.loadMine(t.id);
    });
  }

  /** An entry fee payment the venue hasn't checked yet. */
  protected checking(entry: TournamentEntry): boolean {
    return this.payments()[entry.id]?.some((p) => p.status === 'PENDING') ?? false;
  }

  /** Why the venue turned down the last payment, when it did. */
  protected turnedDown(entry: TournamentEntry): string | null {
    const last = this.payments()[entry.id]?.at(-1);
    return last?.status === 'FAILED' ? last.failureReason ?? "The venue couldn't find this payment." : null;
  }

  protected score(match: TournamentMatch): string {
    if (match.homeScore === null || match.awayScore === null) {
      return match.scheduledAt ? localClock(match.scheduledAt) : 'vs';
    }
    const penalties = match.homePenalties !== null ? ` (${match.homePenalties}–${match.awayPenalties} pens)` : '';
    return `${match.homeScore}–${match.awayScore}${penalties}`;
  }

  private async act(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(slug: string): Promise<void> {
    try {
      const t = await this.api.tournament(slug);
      this.tournament.set(t);
      void this.api.venue(t.venue.slug).then((v) => this.paymentInstructions.set(v.paymentInstructions), () => undefined);
      if (this.session.signedIn()) {
        await this.loadMine(t.id);
        const teams = await this.api.myTeams();
        this.teams.set(teams.filter((team) => team.myStatus === 'ACTIVE' && (team.myRole === 'CAPTAIN' || team.myRole === 'VICE_CAPTAIN')));
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async loadMine(tournamentId: string): Promise<void> {
    const entries = await this.api.myEntries();
    const mine = entries.filter((e) => e.tournament.id === tournamentId && e.entry.status !== 'WITHDRAWN').map((e) => e.entry);
    const due = mine.filter((entry) => entry.status === 'PENDING_PAYMENT');
    const payments = await Promise.all(due.map((entry) => this.api.entryPayments(entry.id)));
    this.payments.set(Object.fromEntries(due.map((entry, i) => [entry.id, payments[i]])));
    this.mine.set(mine);
  }
}
