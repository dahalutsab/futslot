import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MatchPost, Team } from '../../core/api.types';
import { phone } from '../../shared/format';
import { PlayNav } from '../../shared/play-nav';
import { PlayerCardView } from '../../shared/player-card';
import { MatchSummary } from './match-summary';

type Request = MatchPost['requests'][number];

/**
 * One game post. Others ask to join (as a player, or for an opponent post as a team they lead); the poster
 * accepts or declines, and numbers are shared once someone's accepted.
 */
@Component({
  selector: 'fs-match',
  imports: [FormsModule, RouterLink, PlayNav, PlayerCardView, MatchSummary],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      <a routerLink="/matches" class="mt-4 inline-block text-sm text-muted hover:text-text">← Games</a>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }
      @if (post(); as p) {
        <section class="mt-2 rounded-xl border border-border bg-surface p-5">
          <fs-match-summary [post]="p" />
          @if (p.note) {
            <p class="mt-3 whitespace-pre-line">{{ p.note }}</p>
          }
          @if (p.venue) {
            <a [routerLink]="['/venues', p.venue.slug]" class="mt-3 inline-flex min-h-11 items-center text-sm font-medium underline-offset-2 hover:underline">See {{ p.venue.name }}</a>
          }
        </section>

        @if (p.mine) {
          <!-- The poster: requests to answer -->
          <section class="mt-4" aria-labelledby="requests-title">
            <h2 id="requests-title" class="font-semibold">Asking to join</h2>
            @if (p.requests.length === 0) {
              <p class="mt-1 text-muted">No one yet. You're told when someone asks.</p>
            }
            <ul class="mt-2 space-y-2">
              @for (r of p.requests; track r.id) {
                <li class="rounded-xl border border-border bg-surface p-4">
                  <fs-player-card [player]="r.player" />
                  @if (r.team) {
                    <p class="mt-2 text-sm">For <a [routerLink]="['/teams', r.team.slug]" class="font-medium hover:underline">{{ r.team.name }}</a></p>
                  }
                  @if (r.message) {
                    <p class="mt-2 rounded-lg bg-surface-2 px-3 py-2 text-sm">{{ r.message }}</p>
                  }
                  @if (r.status === 'PENDING' && p.status === 'OPEN') {
                    <div class="mt-3 flex gap-2">
                      <button type="button" (click)="answer(r, true)" [disabled]="busy()" class="h-11 rounded-lg bg-ink px-4 font-medium text-ink-fg disabled:opacity-60">Accept</button>
                      <button type="button" (click)="answer(r, false)" [disabled]="busy()" class="h-11 rounded-lg px-3 font-medium text-muted hover:bg-surface-2">Decline</button>
                    </div>
                  } @else {
                    <p class="mt-2 text-sm" [class]="r.status === 'ACCEPTED' ? 'text-confirmed-fg' : 'text-muted'">
                      {{ r.status === 'ACCEPTED' ? 'Accepted' : r.status === 'DECLINED' ? 'Declined' : r.status === 'WITHDRAWN' ? 'Withdrew' : 'Waiting' }}
                      @if (r.phone) { · <a [href]="'tel:' + r.phone" class="tabular font-medium text-text">{{ fmt.phone(r.phone) }}</a> }
                    </p>
                  }
                </li>
              }
            </ul>
          </section>
          @if (p.status === 'OPEN') {
            <button type="button" (click)="cancel()" [disabled]="busy()" class="mt-5 h-11 rounded-lg px-4 font-medium text-danger hover:bg-danger-bg">Call the game off</button>
          }
        } @else if (p.myRequest && p.myRequest.status !== 'WITHDRAWN') {
          <!-- Someone who asked -->
          <section class="mt-4 rounded-xl border p-5" [class]="p.myRequest.status === 'ACCEPTED' ? 'border-confirmed-line bg-confirmed-bg text-confirmed-fg' : 'border-border bg-surface'">
            @if (p.myRequest.status === 'ACCEPTED') {
              <p class="font-semibold">You're in!</p>
              @if (p.contactPhone) {
                <p class="mt-1">Call or message {{ p.postedBy.name }} on <a [href]="'tel:' + p.contactPhone" class="tabular font-medium underline">{{ fmt.phone(p.contactPhone) }}</a>.</p>
              }
            } @else if (p.myRequest.status === 'DECLINED') {
              <p>{{ p.postedBy.name }} picked someone else this time.</p>
            } @else {
              <p>You asked to join. {{ p.postedBy.name }} decides, and you're told.</p>
              <button type="button" (click)="withdraw()" [disabled]="busy()" class="mt-3 h-11 rounded-lg border border-border px-4 font-medium hover:bg-surface-2">Withdraw</button>
            }
          </section>
        } @else if (p.status === 'OPEN') {
          <!-- Everyone else: ask to join -->
          <section class="mt-4 rounded-xl border border-border bg-surface p-5" aria-labelledby="ask-title">
            <h2 id="ask-title" class="font-semibold">{{ p.type === 'FIND_OPPONENT' ? 'Play them with your team' : 'Ask to join' }}</h2>
            @if (p.type === 'FIND_OPPONENT' && myTeams().length === 0) {
              <p class="mt-1 text-muted">You need to captain a team to take this on. <a routerLink="/teams" class="font-medium text-text underline">Start a team</a></p>
            } @else {
              <form class="mt-3 space-y-3" (ngSubmit)="ask()">
                @if (p.type === 'FIND_OPPONENT') {
                  <div>
                    <label for="ask-team" class="mb-1.5 block font-medium">Your team</label>
                    <select id="ask-team" name="team" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="teamId()" (ngModelChange)="teamId.set($event)">
                      @for (t of myTeams(); track t.id) {
                        <option [value]="t.id">{{ t.name }}</option>
                      }
                    </select>
                  </div>
                }
                <div>
                  <label for="ask-message" class="mb-1.5 block font-medium">Message <span class="font-normal text-subtle">(optional)</span></label>
                  <input id="ask-message" name="message" type="text" maxlength="300" placeholder="e.g. Can play in goal" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base"
                    [ngModel]="message()" (ngModelChange)="message.set($event)" />
                </div>
                <button type="submit" [disabled]="busy()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">Ask to join</button>
                <p class="text-sm text-subtle">They see your player profile. <a routerLink="/me/profile" class="font-medium text-text underline-offset-2 hover:underline">Edit it</a></p>
              </form>
            }
          </section>
        }
      } @else if (!error()) {
        <div class="mt-6 h-48 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class MatchPage {
  private readonly api = inject(Api);

  readonly id = input.required<string>();

  protected readonly fmt = { phone };

  protected readonly post = signal<MatchPost | null>(null);

  /** Teams the player leads, to answer an opponent post. */
  protected readonly myTeams = signal<Team[]>([]);

  protected readonly teamId = signal<string | null>(null);

  protected readonly message = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly opponentPost = computed(() => this.post()?.type === 'FIND_OPPONENT');

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
    effect(() => {
      if (this.opponentPost()) {
        untracked(() => this.api.myTeams().then((teams) => {
          const led = teams.filter((t) => t.myStatus === 'ACTIVE' && (t.myRole === 'CAPTAIN' || t.myRole === 'VICE_CAPTAIN'));
          this.myTeams.set(led);
          this.teamId.set(led[0]?.id ?? null);
        }, () => undefined));
      }
    });
  }

  protected ask(): Promise<void> {
    return this.act((p) => this.api.askToJoin(p.id, this.message().trim(), this.opponentPost() ? this.teamId() ?? undefined : undefined));
  }

  protected withdraw(): Promise<void> {
    return this.act((p) => this.api.withdrawRequest(p.id));
  }

  protected answer(request: Request, accept: boolean): Promise<void> {
    return this.act((p) => this.api.answerRequest(p.id, request.id, accept));
  }

  protected cancel(): Promise<void> {
    return this.act((p) => this.api.cancelMatchPost(p.id));
  }

  private async act(action: (post: MatchPost) => Promise<MatchPost>): Promise<void> {
    const post = this.post();
    if (!post || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      this.post.set(await action(post));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(id: string): Promise<void> {
    try {
      this.post.set(await this.api.matchPost(id));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
