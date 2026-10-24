import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../../core/api-error';
import { TournamentDetail, TournamentFormat } from '../../../core/api.types';
import { addDays, today } from '../../../shared/format';
import { Sheet } from '../../../shared/sheet';
import { FORMATS } from './tournament-model';

interface Form {
  name: string;
  format: TournamentFormat;
  startsOn: string;
  endsOn: string;
  maxTeams: string;
  groupCount: string;
  teamSize: string;
  maxSquadSize: string;
  entryFee: string;
  prizeDetails: string;
  matchMinutes: string;
  pointsForWin: string;
  pointsForDraw: string;
  closesOn: string;
  description: string;
  rules: string;
}

/** Creates a tournament or changes one that hasn't started. */
@Component({
  selector: 'fs-tournament-form',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-sheet [title]="tournament() ? 'Edit ' + tournament()!.name : 'New tournament'" (closed)="closed.emit()">
      @let f = form();
      <form id="tournament-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
        <div>
          <label for="t-name" class="mb-1.5 block font-medium">Name</label>
          <input id="t-name" type="text" maxlength="100" [value]="f.name" (input)="patch({ name: $any($event.target).value })"
            placeholder="e.g. Dashain Cup 2083" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
        </div>
        <fieldset [disabled]="locked()">
          <legend class="mb-1.5 font-medium">Format</legend>
          <div class="space-y-2">
            @for (option of formats; track option.value) {
              <label class="flex cursor-pointer items-start gap-3 rounded-md border p-3" [class]="f.format === option.value ? 'border-ink bg-surface-2' : 'border-border'">
                <input type="radio" name="format" class="mt-1 size-4 accent-current" [checked]="f.format === option.value" (change)="patch({ format: option.value })" />
                <span><span class="block font-medium">{{ option.label }}</span><span class="text-sm text-muted">{{ option.detail }}</span></span>
              </label>
            }
          </div>
          @if (locked()) {
            <p class="mt-1.5 text-sm text-subtle">Teams have signed up, so the format and team size stay as they are.</p>
          }
        </fieldset>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label for="t-start" class="mb-1.5 block font-medium">Starts</label>
            <input id="t-start" type="date" [value]="f.startsOn" (input)="patch({ startsOn: $any($event.target).value })" class="h-12 w-full rounded-md border border-border bg-surface px-2 text-base" />
          </div>
          <div>
            <label for="t-end" class="mb-1.5 block font-medium">Ends</label>
            <input id="t-end" type="date" [value]="f.endsOn" (input)="patch({ endsOn: $any($event.target).value })" class="h-12 w-full rounded-md border border-border bg-surface px-2 text-base" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label for="t-teams" class="mb-1.5 block font-medium">Teams</label>
            <input id="t-teams" type="number" inputmode="numeric" min="2" max="32" [value]="f.maxTeams" (input)="patch({ maxTeams: $any($event.target).value })"
              class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
          </div>
          @if (f.format === 'GROUP_KNOCKOUT') {
            <div>
              <label for="t-groups" class="mb-1.5 block font-medium">Groups</label>
              <select id="t-groups" [disabled]="locked()" (change)="patch({ groupCount: $any($event.target).value })" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base">
                @for (n of ['2', '4', '8']; track n) {
                  <option [value]="n" [selected]="f.groupCount === n">{{ n }} groups</option>
                }
              </select>
            </div>
          } @else {
            <div>
              <label for="t-size" class="mb-1.5 block font-medium">Players a side</label>
              <input id="t-size" type="number" inputmode="numeric" min="3" max="11" [disabled]="locked()" [value]="f.teamSize" (input)="patch({ teamSize: $any($event.target).value })"
                class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
            </div>
          }
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label for="t-fee" class="mb-1.5 block font-medium">Entry fee per team</label>
            <div class="relative">
              <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted">Rs</span>
              <input id="t-fee" type="number" inputmode="numeric" min="0" step="100" [value]="f.entryFee" (input)="patch({ entryFee: $any($event.target).value })"
                class="tabular h-12 w-full rounded-md border border-border bg-surface pl-10 pr-3 text-base" />
            </div>
          </div>
          <div>
            <label for="t-minutes" class="mb-1.5 block font-medium">Match length</label>
            <div class="relative">
              <input id="t-minutes" type="number" inputmode="numeric" min="10" max="240" [value]="f.matchMinutes" (input)="patch({ matchMinutes: $any($event.target).value })"
                class="tabular h-12 w-full rounded-md border border-border bg-surface pl-3 pr-12 text-base" />
              <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted">min</span>
            </div>
          </div>
        </div>
        <div>
          <label for="t-prize" class="mb-1.5 block font-medium">Prizes <span class="font-normal text-subtle">(optional)</span></label>
          <input id="t-prize" type="text" maxlength="500" [value]="f.prizeDetails" (input)="patch({ prizeDetails: $any($event.target).value })"
            placeholder="e.g. Rs 25,000 and a trophy for the winners" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
        </div>
        <div>
          <label for="t-description" class="mb-1.5 block font-medium">About <span class="font-normal text-subtle">(optional)</span></label>
          <textarea id="t-description" rows="2" maxlength="2000" [value]="f.description" (input)="patch({ description: $any($event.target).value })"
            class="w-full rounded-md border border-border bg-surface px-3 py-2 text-base"></textarea>
        </div>
        <details class="rounded-md border border-border">
          <summary class="flex h-11 cursor-pointer items-center px-3 font-medium">More options</summary>
          <div class="space-y-4 border-t border-border p-3">
            <div class="grid grid-cols-2 gap-3">
              @if (f.format === 'GROUP_KNOCKOUT') {
                <div>
                  <label for="t-size2" class="mb-1.5 block text-sm font-medium">Players a side</label>
                  <input id="t-size2" type="number" inputmode="numeric" min="3" max="11" [disabled]="locked()" [value]="f.teamSize" (input)="patch({ teamSize: $any($event.target).value })"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
              }
              <div>
                <label for="t-squad" class="mb-1.5 block text-sm font-medium">Squad up to</label>
                <input id="t-squad" type="number" inputmode="numeric" min="3" max="20" [value]="f.maxSquadSize" (input)="patch({ maxSquadSize: $any($event.target).value })"
                  class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
              </div>
              <div>
                <label for="t-closes" class="mb-1.5 block text-sm font-medium">Registration closes</label>
                <input id="t-closes" type="date" [value]="f.closesOn" (input)="patch({ closesOn: $any($event.target).value })"
                  class="h-11 w-full rounded-md border border-border bg-surface px-2 text-base" />
              </div>
            </div>
            @if (f.format !== 'KNOCKOUT') {
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label for="t-win" class="mb-1.5 block text-sm font-medium">Points for a win</label>
                  <input id="t-win" type="number" inputmode="numeric" min="0" max="10" [value]="f.pointsForWin" (input)="patch({ pointsForWin: $any($event.target).value })"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
                <div>
                  <label for="t-draw" class="mb-1.5 block text-sm font-medium">Points for a draw</label>
                  <input id="t-draw" type="number" inputmode="numeric" min="0" max="10" [value]="f.pointsForDraw" (input)="patch({ pointsForDraw: $any($event.target).value })"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
              </div>
            }
            <div>
              <label for="t-rules" class="mb-1.5 block text-sm font-medium">Rules</label>
              <textarea id="t-rules" rows="4" maxlength="4000" [value]="f.rules" (input)="patch({ rules: $any($event.target).value })"
                placeholder="e.g. Two halves of 20 minutes. Rolling substitutes. Yellow card: 2 minutes off."
                class="w-full rounded-md border border-border bg-surface px-3 py-2 text-base"></textarea>
            </div>
          </div>
        </details>
      </form>
      @if (error(); as message) {
        <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }
      <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
        <button type="submit" form="tournament-form" [disabled]="busy()" class="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
          @if (busy()) {
            <lucide-icon [img]="spinner" [size]="18" class="animate-spin" />
          }
          {{ tournament() ? 'Save' : 'Create as a draft' }}
        </button>
      </div>
    </fs-sheet>
  `,
})
export class TournamentForm implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  readonly tournament = input<TournamentDetail | null>(null);

  readonly saved = output<TournamentDetail>();

  readonly closed = output<void>();

  protected readonly formats = FORMATS;

  protected readonly spinner = LoaderCircle;

  protected readonly form = signal<Form>(this.blank());

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly locked = signal(false);

  ngOnInit(): void {
    const t = this.tournament();
    if (t) {
      this.locked.set(t.registrations.length > 0);
      this.form.set({
        name: t.name, format: t.format, startsOn: t.startsOn, endsOn: t.endsOn, maxTeams: String(t.maxTeams),
        groupCount: String(t.groupCount ?? 2), teamSize: String(t.teamSize), maxSquadSize: String(t.maxSquadSize),
        entryFee: String(t.entryFee), prizeDetails: t.prizeDetails ?? '', matchMinutes: String(t.matchMinutes),
        pointsForWin: String(t.pointsForWin), pointsForDraw: String(t.pointsForDraw),
        closesOn: t.registrationClosesAt ? new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kathmandu' }).format(new Date(t.registrationClosesAt)) : '',
        description: t.description ?? '', rules: t.rules ?? '',
      });
    }
  }

  protected patch(change: Partial<Form>): void {
    this.form.update((f) => ({ ...f, ...change }));
  }

  protected async save(): Promise<void> {
    if (this.busy()) {
      return;
    }
    const f = this.form();
    if (!f.name.trim()) {
      this.error.set('Give the tournament a name.');
      return;
    }
    if (!f.startsOn || !f.endsOn || f.endsOn < f.startsOn) {
      this.error.set('Set when it starts and ends; the end can’t be before the start.');
      return;
    }
    const number = (text: string) => (text.trim() === '' ? null : Number(text));
    const body = {
      name: f.name.trim(),
      description: f.description.trim() || null,
      rules: f.rules.trim() || null,
      bannerUrl: this.tournament()?.bannerUrl ?? null,
      format: f.format,
      teamSize: number(f.teamSize),
      maxSquadSize: number(f.maxSquadSize),
      maxTeams: number(f.maxTeams),
      entryFee: number(f.entryFee) ?? 0,
      prizeDetails: f.prizeDetails.trim() || null,
      pointsForWin: number(f.pointsForWin),
      pointsForDraw: number(f.pointsForDraw),
      registrationOpensAt: this.tournament()?.registrationOpensAt ?? null,
      // Registration closes at the end of the chosen day in Nepal
      registrationClosesAt: f.closesOn ? `${f.closesOn}T23:59:00+05:45` : null,
      startsOn: f.startsOn,
      endsOn: f.endsOn,
      matchMinutes: number(f.matchMinutes),
      groupCount: f.format === 'GROUP_KNOCKOUT' ? number(f.groupCount) : null,
    };
    this.busy.set(true);
    this.error.set(null);
    const base = `/api/v1/owner/venues/${this.venueId()}/tournaments`;
    try {
      const t = this.tournament();
      this.saved.emit(t
        ? await firstValueFrom(this.http.put<TournamentDetail>(`${base}/${t.id}`, body))
        : await firstValueFrom(this.http.post<TournamentDetail>(base, body)));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private blank(): Form {
    const start = addDays(today('Asia/Kathmandu'), 14);
    return {
      name: '', format: 'KNOCKOUT', startsOn: start, endsOn: addDays(start, 1), maxTeams: '8', groupCount: '2', teamSize: '5',
      maxSquadSize: '8', entryFee: '0', prizeDetails: '', matchMinutes: '40', pointsForWin: '3', pointsForDraw: '1', closesOn: '',
      description: '', rules: '',
    };
  }
}
