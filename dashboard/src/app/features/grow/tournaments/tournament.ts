import { NgTemplateOutlet } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArrowLeft, LucideAngularModule, Trophy } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../../core/api-error';
import { TournamentDetail, TournamentEntry, TournamentMatch, VenueDetail } from '../../../core/api.types';
import { VenueContext } from '../../../core/venue-context';
import { dayLabel, localClock, methodLabel, rupees } from '../../../shared/format';
import { MatchPanel } from './match-panel';
import { TournamentForm } from './tournament-form';
import { decided, formatLabel, groupsFinished, score, sections, STATUS_LABELS } from './tournament-model';

type Tab = 'overview' | 'teams' | 'fixtures' | 'table';

const ENTRY_LABELS: Record<TournamentEntry['status'], string> = {
  PENDING_PAYMENT: 'Waiting for entry fee',
  CONFIRMED: 'Confirmed',
  WAITLISTED: 'Waiting list',
  WITHDRAWN: 'Withdrew',
  REJECTED: 'Removed',
};

/**
 * One tournament from sign-ups to the final: the next step to take, the teams and their fees, fixtures to
 * book courts for and score, and the table.
 */
@Component({
  selector: 'fs-tournament',
  imports: [LucideAngularModule, MatchPanel, NgTemplateOutlet, RouterLink, TournamentForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-4 lg:px-8 lg:py-6">
      <a routerLink="/grow/tournaments" class="-ml-2 inline-flex h-10 items-center gap-1.5 rounded-md px-2 text-sm font-medium text-muted hover:bg-surface-2 hover:text-text">
        <lucide-icon [img]="icons.ArrowLeft" [size]="16" />Tournaments
      </a>

      @if (error(); as message) {
        <p class="mt-4 rounded-md border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
      } @else if (t(); as t) {
        <div class="mt-1 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 class="text-3xl lg:text-4xl">{{ t.name }}</h1>
            <p class="mt-1 text-muted">{{ format(t.format) }} · {{ fmt.dayLabel(t.startsOn) }}{{ t.endsOn !== t.startsOn ? ' – ' + fmt.dayLabel(t.endsOn) : '' }} · {{ statusLabels[t.status] }}</p>
          </div>
          @if (editable()) {
            <button type="button" (click)="editing.set(true)" class="h-10 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">Edit details</button>
          }
        </div>

        <div class="mt-4 flex gap-1 overflow-x-auto overflow-y-hidden border-b border-border [scrollbar-width:none]" role="tablist">
          @for (option of tabs(); track option.value) {
            <button type="button" role="tab" [attr.aria-selected]="tab() === option.value" (click)="tab.set(option.value)"
              class="-mb-px h-11 shrink-0 border-b-2 px-3 font-medium" [class]="tab() === option.value ? 'border-ink text-text' : 'border-transparent text-muted hover:text-text'">
              {{ option.label }}
            </button>
          }
        </div>

        @if (actionError(); as message) {
          <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }

        <div class="mt-5" role="tabpanel">
          @switch (tab()) {
            @case ('overview') {
              @if (t.champion) {
                <section class="flex items-center gap-3 rounded-md border border-border bg-surface p-4">
                  <lucide-icon [img]="icons.Trophy" [size]="28" class="shrink-0 text-muted" />
                  <div><p class="text-sm text-muted">Champions</p><p class="text-lg font-semibold">{{ t.champion.name }}</p></div>
                </section>
              }
              <!-- The next step -->
              @switch (t.status) {
                @case ('DRAFT') {
                  <ng-container [ngTemplateOutlet]="step" [ngTemplateOutletContext]="{ title: 'Open registration', text: 'Teams can then find it in the app and sign up' + (t.entryFee > 0 ? ', paying the ' + fmt.rupees(t.entryFee) + ' entry fee to you.' : '.'), action: 'open', label: 'Open registration' }" />
                }
                @case ('REGISTRATION_OPEN') {
                  <ng-container [ngTemplateOutlet]="step" [ngTemplateOutletContext]="{ title: t.teamsIn + ' of ' + t.maxTeams + ' teams in' + (t.waitlisted ? ', ' + t.waitlisted + ' waiting' : ''), text: 'Close registration when you’re ready to make the draw.', action: 'close', label: 'Close registration' }" />
                }
                @case ('REGISTRATION_CLOSED') {
                  <section class="rounded-md border border-border bg-surface p-4">
                    <h2 class="font-semibold">Make the draw</h2>
                    <p class="mt-1 text-muted">{{ confirmedCount() }} confirmed {{ confirmedCount() === 1 ? 'team goes' : 'teams go' }} into the draw.
                      @if (pendingCount()) { {{ pendingCount() }} still waiting for their entry fee {{ pendingCount() === 1 ? 'is' : 'are' }} left out unless you record the payment first. }
                    </p>
                    <label class="mt-3 flex cursor-pointer items-center gap-3">
                      <input type="checkbox" class="size-4 accent-current" [checked]="shuffle()" (change)="shuffle.set(!shuffle())" />
                      <span>Draw at random <span class="text-subtle">(otherwise seeded in the order they signed up)</span></span>
                    </label>
                    <button type="button" (click)="act('draw')" [disabled]="busy()" class="mt-3 h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">Make the draw</button>
                  </section>
                }
                @case ('IN_PROGRESS') {
                  @if (t.format === 'GROUP_KNOCKOUT' && groupsDone()) {
                    <ng-container [ngTemplateOutlet]="step" [ngTemplateOutletContext]="{ title: 'The groups are done', text: 'Draw the knockout: the top two of each group go through.', action: 'knockout', label: 'Draw the knockout' }" />
                  } @else {
                    <section class="rounded-md border border-border bg-surface p-4">
                      <h2 class="font-semibold">{{ playedCount() }} of {{ matchCount() }} matches played</h2>
                      <p class="mt-1 text-muted">Book a court for each match and enter results under Fixtures.{{ unbookedCount() ? ' ' + unbookedCount() + ' still need a court.' : '' }}</p>
                      <button type="button" (click)="tab.set('fixtures')" class="mt-3 h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">Go to fixtures</button>
                    </section>
                  }
                }
              }

              <dl class="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div class="rounded-md bg-surface px-3 py-2"><dt class="text-xs text-muted">Teams</dt><dd class="tabular font-medium">{{ t.teamsIn }} of {{ t.maxTeams }}</dd></div>
                <div class="rounded-md bg-surface px-3 py-2"><dt class="text-xs text-muted">Entry fee</dt><dd class="tabular font-medium">{{ t.entryFee > 0 ? fmt.rupees(t.entryFee) : 'Free' }}</dd></div>
                <div class="rounded-md bg-surface px-3 py-2"><dt class="text-xs text-muted">Players</dt><dd class="tabular font-medium">{{ t.teamSize }}-a-side, squads up to {{ t.maxSquadSize }}</dd></div>
                <div class="rounded-md bg-surface px-3 py-2"><dt class="text-xs text-muted">Matches</dt><dd class="tabular font-medium">{{ t.matchMinutes }} min</dd></div>
              </dl>
              @if (t.prizeDetails) {
                <p class="mt-4"><span class="text-muted">Prizes:</span> {{ t.prizeDetails }}</p>
              }
              @if (t.description) {
                <p class="mt-2 whitespace-pre-line">{{ t.description }}</p>
              }
              @if (t.rules) {
                <details class="mt-4 rounded-md border border-border"><summary class="flex h-11 cursor-pointer items-center px-3 font-medium">Rules</summary>
                  <p class="whitespace-pre-line border-t border-border p-3 text-muted">{{ t.rules }}</p></details>
              }

              @if (t.status !== 'COMPLETED' && t.status !== 'CANCELLED') {
                <div class="mt-8 border-t border-border pt-4">
                  @if (confirmCancel()) {
                    <p class="font-medium">Cancel {{ t.name }}?</p>
                    <p class="text-sm text-muted">Teams are told, booked courts are freed and paid entry fees are owed back, which you refund under Payments.</p>
                    <div class="mt-3 flex gap-2">
                      <button type="button" (click)="act('cancel')" [disabled]="busy()" class="h-11 rounded-md bg-danger px-4 font-medium text-surface disabled:opacity-60">Cancel tournament</button>
                      <button type="button" (click)="confirmCancel.set(false)" class="h-11 rounded-md border border-border px-4 font-medium">Keep it</button>
                    </div>
                  } @else {
                    <button type="button" (click)="confirmCancel.set(true)" class="h-11 rounded-md px-1 font-medium text-danger hover:underline">Cancel tournament</button>
                  }
                </div>
              }
            }

            @case ('teams') {
              @if (t.registrations.length === 0) {
                <p class="rounded-md border border-dashed border-border bg-surface px-6 py-8 text-center text-muted">
                  {{ t.status === 'DRAFT' ? 'Open registration and teams can sign up in the app.' : 'No teams yet.' }}
                </p>
              } @else {
                <ul class="divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
                  @for (entry of entries(); track entry.id) {
                    <li class="px-4 py-3">
                      <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                        <span class="font-medium" [class.text-muted]="entry.status !== 'CONFIRMED' && entry.status !== 'PENDING_PAYMENT'">{{ entry.team.name }}</span>
                        @if (entry.groupName) {
                          <span class="text-sm text-subtle">Group {{ entry.groupName }}</span>
                        }
                        <span class="ml-auto text-sm" [class]="entry.status === 'PENDING_PAYMENT' ? 'text-text' : 'text-muted'">{{ entryLabel(entry) }}</span>
                      </div>
                      <p class="mt-0.5 text-sm text-muted">{{ squadLine(entry) }}</p>
                      @if (entry.status === 'PENDING_PAYMENT') {
                        @if (recording() === entry.id) {
                          <form class="mt-2 flex flex-wrap items-end gap-2" (submit)="$event.preventDefault(); recordFee(entry)">
                            <select (change)="feeMethod.set($any($event.target).value)" aria-label="Paid with" class="h-10 rounded-md border border-border bg-surface px-2 text-base">
                              @for (method of methods; track method) {
                                <option [value]="method" [selected]="feeMethod() === method">{{ fmt.methodLabel(method) }}</option>
                              }
                            </select>
                            @if (feeMethod() !== 'CASH') {
                              <input type="text" maxlength="100" placeholder="Transaction ID" [value]="feeTxn()" (input)="feeTxn.set($any($event.target).value)" aria-label="Transaction ID"
                                class="tabular h-10 w-44 rounded-md border border-border bg-surface px-2 text-base" />
                            }
                            <button type="submit" [disabled]="busy()" class="h-10 rounded-md bg-ink px-3 text-sm font-medium text-ink-fg disabled:opacity-60">Paid {{ fmt.rupees(t.entryFee) }}</button>
                            <button type="button" (click)="recording.set(null)" class="h-10 rounded-md px-2 text-sm font-medium hover:bg-surface-2">Cancel</button>
                          </form>
                        } @else {
                          <div class="mt-2 flex gap-2">
                            <button type="button" (click)="startRecording(entry)" class="h-9 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">Record entry fee</button>
                            @if (!hasFixtures()) {
                              <button type="button" (click)="reject(entry)" [disabled]="busy()" class="h-9 rounded-md px-2 text-sm font-medium text-muted hover:bg-surface-2">Remove team</button>
                            }
                          </div>
                        }
                      } @else if (entry.status === 'CONFIRMED' && !hasFixtures()) {
                        <button type="button" (click)="reject(entry)" [disabled]="busy()" class="mt-1 h-9 rounded-md px-1 text-sm font-medium text-muted hover:underline">Remove team</button>
                      }
                    </li>
                  }
                </ul>
                @if (t.entryFee > 0) {
                  <p class="mt-2 text-sm text-subtle">Fees paid by wallet in the app show up under Payments for you to check.</p>
                }
              }
            }

            @case ('fixtures') {
              @if (fixtureSections().length === 0) {
                <p class="rounded-md border border-dashed border-border bg-surface px-6 py-8 text-center text-muted">Fixtures appear after the draw.</p>
              } @else {
                @for (section of fixtureSections(); track section.key) {
                  <h2 class="mb-2 mt-5 text-sm font-medium text-subtle first:mt-0">{{ section.title }}</h2>
                  <ul class="divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
                    @for (m of section.matches; track m.id) {
                      <li>
                        <button type="button" (click)="openMatch(m)" class="flex w-full flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-left hover:bg-surface-2">
                          <span class="min-w-0 flex-1">
                            <span class="block font-medium"><span [class.text-muted]="!m.home">{{ m.home?.name ?? 'To be decided' }}</span><span class="font-normal text-muted">{{ ' v ' }}</span><span [class.text-muted]="!m.away">{{ m.away?.name ?? 'To be decided' }}</span></span>
                            <span class="block text-sm" [class]="m.scheduledAt ? 'text-muted' : 'text-text'">
                              {{ m.scheduledAt ? fmt.dayLabel(m.scheduledAt.slice(0, 10)) + ', ' + fmt.localClock(m.scheduledAt) + (m.court ? ' · ' + m.court.name : '') : (decided(m) ? '' : 'Needs a court') }}
                            </span>
                          </span>
                          <span class="tabular text-right font-semibold">{{ m.status === 'WALKOVER' ? 'Walkover' : (score(m) ?? '') }}</span>
                        </button>
                      </li>
                    }
                  </ul>
                }
              }
            }

            @case ('table') {
              @for (table of t.tables; track table.groupName ?? 'league') {
                @if (table.groupName) {
                  <h2 class="mb-2 mt-5 text-sm font-medium text-subtle first:mt-0">Group {{ table.groupName }}</h2>
                }
                <div class="overflow-x-auto rounded-md border border-border bg-surface">
                  <table class="w-full text-sm">
                    <thead><tr class="text-left text-muted">
                      <th scope="col" class="px-4 py-2 font-medium">Team</th>
                      @for (head of ['P', 'W', 'D', 'L', 'GD', 'Pts']; track head) {
                        <th scope="col" class="px-2 py-2 text-right font-medium">{{ head }}</th>
                      }
                    </tr></thead>
                    <tbody class="divide-y divide-border">
                      @for (row of table.rows; track row.team.registrationId; let i = $index) {
                        <tr>
                          <td class="px-4 py-2"><span class="tabular mr-2 text-subtle">{{ i + 1 }}</span>{{ row.team.name }}</td>
                          <td class="tabular px-2 py-2 text-right">{{ row.played }}</td>
                          <td class="tabular px-2 py-2 text-right">{{ row.won }}</td>
                          <td class="tabular px-2 py-2 text-right">{{ row.drawn }}</td>
                          <td class="tabular px-2 py-2 text-right">{{ row.lost }}</td>
                          <td class="tabular px-2 py-2 text-right">{{ row.goalDifference > 0 ? '+' : '' }}{{ row.goalDifference }}</td>
                          <td class="tabular px-2 py-2 text-right font-semibold">{{ row.points }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            }
          }
        </div>

        <ng-template #step let-title="title" let-text="text" let-action="action" let-label="label">
          <section class="rounded-md border border-border bg-surface p-4">
            <h2 class="font-semibold">{{ title }}</h2>
            <p class="mt-1 text-muted">{{ text }}</p>
            <button type="button" (click)="act(action)" [disabled]="busy()" class="mt-3 h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ label }}</button>
          </section>
        </ng-template>
      } @else {
        <div class="mt-4 h-64 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>

    @if (editing() && t(); as current) {
      <fs-tournament-form [venueId]="venueId()!" [tournament]="current" (saved)="t.set($event); editing.set(false)" (closed)="editing.set(false)" />
    }
    @if (openMatchId() && t(); as current) {
      @if (selectedMatch(); as m) {
        <fs-match-panel [venueId]="venueId()!" [tournament]="current" [match]="m" [courts]="courts()"
          (done)="t.set($event); openMatchId.set(null)" (closed)="openMatchId.set(null)" />
      }
    }
  `,
})
export class Tournament {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  /** From the route. */
  readonly id = input.required<string>();

  protected readonly icons = { ArrowLeft, Trophy };

  protected readonly fmt = { dayLabel, localClock, methodLabel, rupees };

  protected readonly statusLabels = STATUS_LABELS;

  protected readonly format = formatLabel;

  protected readonly decided = decided;

  protected readonly score = score;

  protected readonly methods = ['CASH', 'ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'];

  protected readonly t = signal<TournamentDetail | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly actionError = signal<string | null>(null);

  protected readonly tab = signal<Tab>('overview');

  protected readonly busy = signal(false);

  protected readonly shuffle = signal(true);

  protected readonly confirmCancel = signal(false);

  protected readonly editing = signal(false);

  protected readonly recording = signal<string | null>(null);

  protected readonly feeMethod = signal('CASH');

  protected readonly feeTxn = signal('');

  protected readonly openMatchId = signal<string | null>(null);

  protected readonly courts = signal<{ id: string; name: string }[]>([]);

  protected readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  protected readonly selectedMatch = computed(() => this.t()?.matches.find((m) => m.id === this.openMatchId()) ?? null);

  protected readonly fixtureSections = computed(() => sections(this.t()?.matches ?? []));

  protected readonly hasFixtures = computed(() => (this.t()?.matches.length ?? 0) > 0);

  protected readonly matchCount = computed(() => (this.t()?.matches ?? []).filter((m) => !m.bye).length);

  protected readonly playedCount = computed(() => (this.t()?.matches ?? []).filter((m) => !m.bye && decided(m)).length);

  protected readonly unbookedCount = computed(() => (this.t()?.matches ?? []).filter((m) => !m.bye && !decided(m) && !m.scheduledAt).length);

  protected readonly groupsDone = computed(() => groupsFinished(this.t()?.matches ?? []));

  protected readonly confirmedCount = computed(() => (this.t()?.registrations ?? []).filter((r) => r.status === 'CONFIRMED').length);

  protected readonly pendingCount = computed(() => (this.t()?.registrations ?? []).filter((r) => r.status === 'PENDING_PAYMENT').length);

  /** Details can change until fixtures are drawn. */
  protected readonly editable = computed(() => {
    const t = this.t();
    return !!t && ['DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED'].includes(t.status) && t.matches.length === 0;
  });

  protected readonly entries = computed(() => {
    const order = ['PENDING_PAYMENT', 'CONFIRMED', 'WAITLISTED', 'WITHDRAWN', 'REJECTED'];
    return [...(this.t()?.registrations ?? [])].sort((a, b) => order.indexOf(a.status) - order.indexOf(b.status)
      || (a.groupName ?? '').localeCompare(b.groupName ?? '') || a.registeredAt.localeCompare(b.registeredAt));
  });

  protected readonly tabs = computed(() => {
    const t = this.t();
    const list: { value: Tab; label: string }[] = [
      { value: 'overview', label: 'Overview' },
      { value: 'teams', label: `Teams${t ? ` (${t.registrations.filter((r) => r.status === 'CONFIRMED' || r.status === 'PENDING_PAYMENT').length})` : ''}` },
      { value: 'fixtures', label: 'Fixtures' },
    ];
    if (t?.tables.length) {
      list.push({ value: 'table', label: 'Table' });
    }
    return list;
  });

  constructor() {
    effect(() => {
      const venueId = this.venueId();
      const id = this.id();
      if (venueId) {
        untracked(() => void this.load(venueId, id));
      }
    });
  }

  protected entryLabel(entry: TournamentEntry): string {
    return ENTRY_LABELS[entry.status];
  }

  protected squadLine(entry: TournamentEntry): string {
    const names = entry.squad.map((p) => (p.jerseyNumber !== null ? `${p.name} (${p.jerseyNumber})` : p.name));
    return names.length ? `${names.length} players: ${names.join(', ')}` : 'No squad';
  }

  protected openMatch(match: TournamentMatch): void {
    this.openMatchId.set(match.id);
  }

  protected startRecording(entry: TournamentEntry): void {
    this.feeMethod.set('CASH');
    this.feeTxn.set('');
    this.recording.set(entry.id);
  }

  protected async act(action: 'open' | 'close' | 'draw' | 'knockout' | 'cancel'): Promise<void> {
    const body = action === 'draw' ? { shuffle: this.shuffle() } : null;
    await this.run(() => firstValueFrom(this.http.post<TournamentDetail>(`${this.base()}/${action}`, body)));
    if (action === 'draw' || action === 'knockout') {
      this.tab.set('fixtures');
    }
    this.confirmCancel.set(false);
  }

  protected reject(entry: TournamentEntry): Promise<void> {
    return this.run(() => firstValueFrom(this.http.post<TournamentDetail>(`${this.base()}/registrations/${entry.id}/reject`, null)));
  }

  protected async recordFee(entry: TournamentEntry): Promise<void> {
    if (this.feeMethod() !== 'CASH' && !this.feeTxn().trim()) {
      this.actionError.set('Enter the transaction ID from their payment.');
      return;
    }
    const venueId = this.venueId();
    await this.run(async () => {
      await firstValueFrom(this.http.post(`/api/v1/owner/venues/${venueId}/tournament-entries/${entry.id}/payments`,
        { method: this.feeMethod(), transactionId: this.feeMethod() === 'CASH' ? null : this.feeTxn().trim() }));
      return firstValueFrom(this.http.get<TournamentDetail>(this.base()));
    });
    this.recording.set(null);
  }

  private base(): string {
    return `/api/v1/owner/venues/${this.venueId()}/tournaments/${this.id()}`;
  }

  private async run(request: () => Promise<TournamentDetail>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    try {
      this.t.set(await request());
    }
    catch (error) {
      this.actionError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(venueId: string, id: string): Promise<void> {
    try {
      const [tournament, venue] = await Promise.all([
        firstValueFrom(this.http.get<TournamentDetail>(`/api/v1/owner/venues/${venueId}/tournaments/${id}`)),
        firstValueFrom(this.http.get<VenueDetail>(`/api/v1/owner/venues/${venueId}`)),
      ]);
      this.t.set(tournament);
      this.courts.set(venue.courts.filter((c) => c.status === 'ACTIVE').map((c) => ({ id: c.id, name: c.name })));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
