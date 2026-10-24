import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Plus, Search } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { SkillLevel, Team, TeamSummary } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { roleLabels, skillLabels } from '../../shared/labels';
import { PlayNav } from '../../shared/play-nav';

/** The player's teams and invitations, starting a team, and finding public teams. */
@Component({
  selector: 'fs-teams',
  imports: [FormsModule, RouterLink, LucideAngularModule, PlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }

      @if (invites().length) {
        <section class="mt-6" aria-labelledby="invites-title">
          <h2 id="invites-title" class="font-semibold">{{ l().invitations }}</h2>
          <ul class="mt-2 space-y-2">
            @for (team of invites(); track team.id) {
              <li class="flex flex-wrap items-center gap-3 rounded-md border border-held-line bg-surface p-4">
                <a [routerLink]="['/teams', team.slug]" class="min-w-0 flex-1 font-medium hover:underline">{{ team.name }}</a>
                <button type="button" (click)="answer(team, true)" class="h-11 rounded-md bg-ink px-4 font-medium text-ink-fg">{{ l().join }}</button>
                <button type="button" (click)="answer(team, false)" class="h-11 rounded-md px-3 font-medium text-muted hover:bg-surface-2">{{ l().decline }}</button>
              </li>
            }
          </ul>
        </section>
      }

      <section class="mt-6" aria-labelledby="mine-title">
        <div class="flex items-center gap-3">
          <h2 id="mine-title" class="flex-1 font-semibold">{{ l().yourTeams }}</h2>
          <button type="button" (click)="creating.set(!creating())" class="flex h-11 items-center gap-1.5 rounded-md border border-border px-3 text-sm font-medium hover:bg-surface-2">
            <lucide-icon [img]="icons.Plus" [size]="16" /> {{ l().newTeam }}
          </button>
        </div>
        @if (creating()) {
          <form class="mt-3 space-y-3 rounded-md border border-border bg-surface p-4" (ngSubmit)="create()">
            <div class="grid gap-3 sm:grid-cols-2">
              <div>
                <label for="team-name" class="mb-1.5 block font-medium">{{ l().name }}</label>
                <input id="team-name" name="name" type="text" maxlength="60" required class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base"
                  [ngModel]="name()" (ngModelChange)="name.set($event)" />
              </div>
              <div>
                <label for="team-city" class="mb-1.5 block font-medium">{{ l().city }}</label>
                <input id="team-city" name="city" type="text" maxlength="60" class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base"
                  [ngModel]="city()" (ngModelChange)="city.set($event)" />
              </div>
            </div>
            <fieldset>
              <legend class="mb-1.5 font-medium">{{ l().level }}</legend>
              <div class="flex flex-wrap gap-2">
                @for (s of skills(); track s.value) {
                  <button type="button" (click)="skill.set(skill() === s.value ? null : s.value)" [attr.aria-pressed]="skill() === s.value"
                    class="h-10 rounded-full border px-4 text-sm font-medium" [class]="skill() === s.value ? 'border-ink bg-ink text-ink-fg' : 'border-border text-muted'">{{ s.label }}</button>
                }
              </div>
            </fieldset>
            <button type="submit" [disabled]="busy()" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ l().createTeam }}</button>
            <p class="text-sm text-subtle">{{ l().youllBeCaptain }}</p>
          </form>
        }
        @if (teams(); as list) {
          @if (active().length === 0 && !creating()) {
            <p class="mt-2 text-muted">{{ l().notInTeam }}</p>
          }
          <ul class="mt-3 space-y-2">
            @for (team of active(); track team.id) {
              <li>
                <a [routerLink]="['/teams', team.slug]" class="flex items-center gap-3 rounded-md border border-border bg-surface p-4 hover:border-subtle">
                  <span class="grid size-10 shrink-0 place-items-center rounded-md bg-brand font-bold text-white" aria-hidden="true">{{ team.name.charAt(0) }}</span>
                  <span class="min-w-0 flex-1">
                    <span class="block font-medium">{{ team.name }}</span>
                    <span class="block text-sm text-muted">{{ l().players(team.memberCount) }}{{ team.myRole ? ' · ' + role()[team.myRole] : '' }}</span>
                  </span>
                </a>
              </li>
            }
          </ul>
        }
      </section>

      <section class="mt-8" aria-labelledby="find-title">
        <h2 id="find-title" class="font-semibold">{{ l().findATeam }}</h2>
        <form class="mt-2 flex gap-2" role="search" (ngSubmit)="search()">
          <label for="team-q" class="sr-only">{{ l().teamName }}</label>
          <div class="flex h-11 min-w-0 flex-1 items-center gap-2 rounded-md border border-border bg-surface px-3">
            <lucide-icon [img]="icons.Search" [size]="16" class="text-subtle" />
            <input id="team-q" name="q" type="search" [placeholder]="l().teamName" class="h-full min-w-0 flex-1 bg-transparent text-base outline-none" [ngModel]="q()" (ngModelChange)="q.set($event)" />
          </div>
          <button type="submit" class="h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">{{ l().search }}</button>
        </form>
        @if (found(); as list) {
          @if (list.length === 0) {
            <p class="mt-3 text-muted">{{ l().noPublicTeams }}</p>
          }
          <ul class="mt-3 grid gap-2 sm:grid-cols-2">
            @for (team of list; track team.id) {
              <li>
                <a [routerLink]="['/teams', team.slug]" class="block rounded-md border border-border bg-surface p-4 hover:border-subtle">
                  <span class="block font-medium">{{ team.name }}</span>
                  <span class="block text-sm text-muted">{{ [team.city, team.skillLevel ? skillLabel()[team.skillLevel] : null, l().players(team.memberCount)].filter(isText).join(' · ') }}</span>
                </a>
              </li>
            }
          </ul>
        }
      </section>
    </div>
  `,
})
export class Teams {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly icons = { Plus, Search };

  protected readonly role = roleLabels;

  protected readonly skillLabel = skillLabels;

  protected readonly skills = computed(() => Object.entries(skillLabels()).map(([value, label]) => ({ value: value as SkillLevel, label })));

  protected readonly isText = (value: string | null): value is string => !!value;

  protected readonly teams = signal<Team[] | null>(null);

  protected readonly active = computed(() => (this.teams() ?? []).filter((t) => t.myStatus === 'ACTIVE'));

  protected readonly invites = computed(() => (this.teams() ?? []).filter((t) => t.myStatus === 'INVITED'));

  protected readonly found = signal<TeamSummary[] | null>(null);

  protected readonly q = signal('');

  protected readonly creating = signal(false);

  protected readonly name = signal('');

  protected readonly city = signal('');

  protected readonly skill = signal<SkillLevel | null>(null);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
    void this.search();
  }

  protected async create(): Promise<void> {
    if (this.name().trim().length < 2) {
      this.error.set(l().giveTeamName);
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const team = await this.api.createTeam({ name: this.name().trim(), city: this.city().trim() || undefined, skillLevel: this.skill() ?? undefined, isPublic: true });
      await this.router.navigate(['/teams', team.slug]);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async answer(team: Team, accept: boolean): Promise<void> {
    try {
      await this.api.answerInvite(team.id, accept);
      await this.load();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  protected async search(): Promise<void> {
    try {
      this.found.set((await this.api.teams({ q: this.q().trim() || undefined })).content);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async load(): Promise<void> {
    try {
      this.teams.set(await this.api.myTeams());
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
