import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';
import { SkillLevel, Team, TeamRole } from '../../core/api.types';
import { ROLE, SKILL } from '../../shared/labels';
import { PlayNav } from '../../shared/play-nav';

type Member = Team['members'][number];

/**
 * One team: its squad, and for members their role. Captains and vice-captains invite players by phone and remove
 * them; only the captain changes roles (handing the captaincy over makes them vice-captain).
 */
@Component({
  selector: 'fs-team',
  imports: [FormsModule, RouterLink, PlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      <a routerLink="/teams" class="mt-4 inline-block text-sm text-muted hover:text-text">← Teams</a>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }
      @if (team(); as t) {
        <div class="mt-2 flex items-center gap-4">
          <span class="grid size-14 shrink-0 place-items-center rounded-xl bg-brand text-2xl font-bold text-white" aria-hidden="true">{{ t.name.charAt(0) }}</span>
          <div class="min-w-0 flex-1">
            <h1 class="text-2xl font-semibold tracking-tight">{{ t.name }}</h1>
            <p class="text-muted">{{ details() }}</p>
          </div>
        </div>

        @if (t.myStatus === 'INVITED') {
          <section class="mt-5 flex flex-wrap items-center gap-3 rounded-xl border border-held-line bg-surface p-4">
            <p class="min-w-0 flex-1">You're invited to join {{ t.name }}.</p>
            <button type="button" (click)="answer(true)" class="h-11 rounded-lg bg-ink px-4 font-medium text-ink-fg">Join</button>
            <button type="button" (click)="answer(false)" class="h-11 rounded-lg px-3 font-medium text-muted hover:bg-surface-2">Decline</button>
          </section>
        }

        <section class="mt-6" aria-labelledby="squad-title">
          <h2 id="squad-title" class="font-semibold">Squad</h2>
          <ul class="mt-2 divide-y divide-border rounded-xl border border-border bg-surface">
            @for (m of t.members; track m.userId) {
              <li class="flex flex-wrap items-center gap-2 px-4 py-3">
                <span class="min-w-0 flex-1">
                  <span class="block">{{ m.name }}@if (m.userId === me()) {<span class="text-muted"> (you)</span>}</span>
                  <span class="block text-sm text-muted">{{ m.status === 'INVITED' ? 'Invited' : role[m.role] }}</span>
                </span>
                @if (isCaptain() && m.userId !== me() && m.status === 'ACTIVE') {
                  <label class="sr-only" [for]="'role-' + m.userId">Role of {{ m.name }}</label>
                  <select [id]="'role-' + m.userId" (change)="changeRole(m, $any($event.target).value)" class="h-11 rounded-lg border border-border bg-surface px-2 text-sm">
                    @for (r of roles; track r.value) {
                      <option [value]="r.value" [selected]="m.role === r.value">{{ r.value === 'CAPTAIN' ? 'Make captain' : r.label }}</option>
                    }
                  </select>
                }
                @if (leads() && m.userId !== me() && m.role !== 'CAPTAIN') {
                  <button type="button" (click)="remove(m)" class="h-11 rounded-lg px-3 text-sm font-medium text-muted hover:bg-surface-2 hover:text-danger">
                    {{ m.status === 'INVITED' ? 'Cancel invite' : 'Remove' }}
                  </button>
                }
              </li>
            }
          </ul>
        </section>

        @if (leads()) {
          <section class="mt-5 rounded-xl border border-border bg-surface p-4" aria-labelledby="invite-title">
            <h2 id="invite-title" class="font-semibold">Invite a player</h2>
            <p class="text-sm text-muted">By the phone number they use FutSlot with. They're told and can join.</p>
            <form class="mt-3 flex gap-2" (ngSubmit)="invite()">
              <label for="invite-phone" class="sr-only">Phone</label>
              <input id="invite-phone" name="phone" type="tel" inputmode="numeric" placeholder="98XXXXXXXX" class="tabular h-11 min-w-0 flex-1 rounded-lg border border-border bg-surface px-3 text-base"
                [ngModel]="phone()" (ngModelChange)="phone.set($event)" />
              <button type="submit" [disabled]="busy()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">Invite</button>
            </form>
          </section>
        }

        @if (t.myStatus === 'ACTIVE' && !isCaptain()) {
          <button type="button" (click)="leave()" class="mt-6 h-11 rounded-lg px-4 font-medium text-danger hover:bg-danger-bg">Leave {{ t.name }}</button>
        }
        @if (isCaptain() && t.memberCount > 1) {
          <p class="mt-6 text-sm text-subtle">To leave, make someone else captain first.</p>
        }
      } @else if (!error()) {
        <div class="mt-6 h-48 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class TeamPage {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly session = inject(Session);

  readonly slug = input.required<string>();

  protected readonly role = ROLE;

  protected readonly roles = (Object.keys(ROLE) as TeamRole[]).map((value) => ({ value, label: ROLE[value] }));

  protected readonly team = signal<Team | null>(null);

  protected readonly phone = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly me = computed(() => this.session.user()?.id ?? null);

  protected readonly isCaptain = computed(() => this.team()?.myStatus === 'ACTIVE' && this.team()?.myRole === 'CAPTAIN');

  protected readonly leads = computed(() => this.team()?.myStatus === 'ACTIVE' && (this.team()?.myRole === 'CAPTAIN' || this.team()?.myRole === 'VICE_CAPTAIN'));

  protected readonly details = computed(() => {
    const t = this.team();
    if (!t) {
      return '';
    }
    const skill: SkillLevel | null = t.skillLevel;
    return [t.city, skill ? SKILL[skill] : null, `${t.memberCount} ${t.memberCount === 1 ? 'player' : 'players'}`].filter(Boolean).join(' · ');
  });

  constructor() {
    effect(() => {
      const slug = this.slug();
      untracked(() => void this.load(slug));
    });
  }

  protected invite(): Promise<void> {
    const phone = this.phone().replace(/\D/g, '');
    if (phone.length < 10) {
      this.error.set('Enter their 10-digit mobile number.');
      return Promise.resolve();
    }
    return this.act(async (t) => {
      this.team.set(await this.api.invite(t.id, phone.slice(-10)));
      this.phone.set('');
    });
  }

  protected answer(accept: boolean): Promise<void> {
    return this.act(async (t) => {
      await this.api.answerInvite(t.id, accept);
      if (accept) {
        await this.load(t.slug);
      } else {
        await this.router.navigate(['/teams']);
      }
    });
  }

  protected remove(member: Member): Promise<void> {
    return this.act(async (t) => this.team.set(await this.api.removeMember(t.id, member.userId)));
  }

  protected changeRole(member: Member, role: TeamRole): Promise<void> {
    return this.act(async (t) => this.team.set(await this.api.changeRole(t.id, member.userId, role)));
  }

  protected leave(): Promise<void> {
    return this.act(async (t) => {
      await this.api.leaveTeam(t.id);
      await this.router.navigate(['/teams']);
    });
  }

  private async act(action: (team: Team) => Promise<void>): Promise<void> {
    const team = this.team();
    if (!team || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await action(team);
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
      this.team.set(await this.api.team(slug));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
