import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Plus } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MatchPost, MatchPostType } from '../../core/api.types';
import { addDays, dayLabel, today } from '../../shared/format';
import { PlayNav } from '../../shared/play-nav';
import { MatchSummary } from './match-summary';

type Tab = 'open' | 'mine';

const NEPAL = 'Asia/Kathmandu';

/** Pickup games: teams short of players, and teams looking for an opponent. Plus the player's own posts and requests. */
@Component({
  selector: 'fs-matches',
  imports: [RouterLink, LucideAngularModule, PlayNav, MatchSummary],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      <div class="mt-6 flex items-center gap-3">
        <h1 class="flex-1 text-2xl font-semibold tracking-tight">Games</h1>
        <a routerLink="/matches/new" class="flex h-11 items-center gap-1.5 rounded-lg bg-ink px-4 font-medium text-ink-fg">
          <lucide-icon [img]="icons.Plus" [size]="18" /> Post a game
        </a>
      </div>
      <p class="mt-1 text-muted">Short of players, or looking for a team to play? Post it, or join one.</p>

      <div class="mt-4 flex gap-1 rounded-lg bg-surface-2 p-1" role="tablist" aria-label="Games">
        <button type="button" role="tab" [attr.aria-selected]="tab() === 'open'" (click)="show('open')" class="h-10 flex-1 rounded-md font-medium text-muted"
          [class.bg-surface]="tab() === 'open'" [class.text-text]="tab() === 'open'">Open games</button>
        <button type="button" role="tab" [attr.aria-selected]="tab() === 'mine'" (click)="show('mine')" class="h-10 flex-1 rounded-md font-medium text-muted"
          [class.bg-surface]="tab() === 'mine'" [class.text-text]="tab() === 'mine'">Yours</button>
      </div>

      @if (tab() === 'open') {
        <div class="mt-3 flex flex-wrap gap-2" role="group" aria-label="Kind of game">
          @for (t of types; track t.label) {
            <button type="button" (click)="type.set(t.value)" [attr.aria-pressed]="type() === t.value" class="h-9 rounded-full border px-4 text-sm font-medium"
              [class]="type() === t.value ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">{{ t.label }}</button>
          }
        </div>
        <div class="-mx-4 mt-2 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]" role="group" aria-label="Day">
          <button type="button" (click)="day.set(null)" [attr.aria-pressed]="day() === null" class="h-9 shrink-0 rounded-full border px-4 text-sm font-medium"
            [class]="day() === null ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">Any day</button>
          @for (d of days; track d; let i = $index) {
            <button type="button" (click)="day.set(d)" [attr.aria-pressed]="day() === d" class="h-9 shrink-0 rounded-full border px-4 text-sm font-medium"
              [class]="day() === d ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">{{ i === 0 ? 'Today' : i === 1 ? 'Tomorrow' : dayLabel(d) }}</button>
          }
        </div>
      }

      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (posts(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">{{ tab() === 'open' ? 'No open games' : "You haven't posted or joined a game" }}</p>
            <p class="mt-1 text-muted">Post one, and players nearby can ask to join.</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (post of list; track post.id) {
              <li>
                <a [routerLink]="['/matches', post.id]" class="block rounded-xl border border-border bg-surface p-4 hover:border-subtle">
                  <fs-match-summary [post]="post" />
                  @if (post.mine) {
                    <p class="mt-2 text-sm font-medium">Your post{{ pending(post) ? ' · ' + pending(post) + ' asking to join' : '' }}</p>
                  } @else if (post.myRequest) {
                    <p class="mt-2 text-sm font-medium" [class.text-confirmed-fg]="post.myRequest.status === 'ACCEPTED'">
                      {{ post.myRequest.status === 'PENDING' ? 'You asked to join' : post.myRequest.status === 'ACCEPTED' ? "You're in" : post.myRequest.status === 'DECLINED' ? 'Not this time' : 'You withdrew' }}
                    </p>
                  }
                </a>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 space-y-3" aria-label="Loading">
          @for (i of [1, 2]; track i) {
            <div class="h-32 animate-pulse rounded-xl bg-surface"></div>
          }
        </div>
      }
    </div>
  `,
})
export class Matches {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly icons = { Plus };

  protected readonly dayLabel = dayLabel;

  protected readonly types: { value: MatchPostType | null; label: string }[] = [
    { value: null, label: 'All' },
    { value: 'NEED_PLAYERS', label: 'Players wanted' },
    { value: 'FIND_OPPONENT', label: 'Opponent wanted' },
  ];

  protected readonly days = Array.from({ length: 7 }, (_, i) => addDays(today(NEPAL), i));

  protected readonly tab = signal<Tab>(inject(ActivatedRoute).snapshot.queryParamMap.get('tab') === 'mine' ? 'mine' : 'open');

  protected readonly type = signal<MatchPostType | null>(null);

  protected readonly day = signal<string | null>(null);

  protected readonly posts = signal<MatchPost[] | null>(null);

  protected readonly error = signal<string | null>(null);

  private readonly filter = computed(() => ({ tab: this.tab(), type: this.type(), day: this.day() }));

  constructor() {
    effect(() => {
      const filter = this.filter();
      untracked(() => void this.load(filter));
    });
  }

  protected show(tab: Tab): void {
    this.tab.set(tab);
    void this.router.navigate([], { queryParams: { tab: tab === 'mine' ? 'mine' : null }, replaceUrl: true });
  }

  protected pending(post: MatchPost): number {
    return post.requests.filter((r) => r.status === 'PENDING').length;
  }

  private async load(filter: { tab: Tab; type: MatchPostType | null; day: string | null }): Promise<void> {
    this.posts.set(null);
    this.error.set(null);
    try {
      if (filter.tab === 'mine') {
        const [posts, requests] = await Promise.all([this.api.myMatchPosts(), this.api.myMatchRequests()]);
        this.posts.set([...posts.content, ...requests.content].sort((a, b) => a.startsAt.localeCompare(b.startsAt)));
      } else {
        this.posts.set((await this.api.matchPosts({ type: filter.type ?? undefined, date: filter.day ?? undefined })).content);
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
