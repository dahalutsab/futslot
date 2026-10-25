import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Icon, Plus } from '../../shared/icon';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { MatchPost, MatchPostType } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { addDays, dayLabel, today } from '../../shared/format';
import { PlayNav } from '../../shared/play-nav';
import { MatchSummary } from './match-summary';

type Tab = 'open' | 'mine';

const NEPAL = 'Asia/Kathmandu';

/** Pickup games: teams short of players, and teams looking for an opponent. Plus the player's own posts and requests. */
@Component({
  selector: 'fs-matches',
  imports: [RouterLink, Icon, PlayNav, MatchSummary],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:py-10">
      <fs-play-nav />
      <div class="mt-6 flex items-center gap-3">
        <h1 class="flex-1 text-4xl">{{ l().playGames }}</h1>
        <a routerLink="/matches/new" class="flex h-11 items-center gap-1.5 rounded-md bg-ink px-4 font-medium text-ink-fg">
          <fs-icon [img]="icons.Plus" [size]="18" /> {{ l().postAGame }}
        </a>
      </div>
      <p class="mt-1 text-muted">{{ l().gamesIntro }}</p>

      <div class="mt-4 flex gap-1 rounded-md bg-surface-2 p-1" role="tablist" [attr.aria-label]="l().playGames">
        <button type="button" role="tab" [attr.aria-selected]="tab() === 'open'" (click)="show('open')" class="h-10 flex-1 rounded-md font-medium text-muted"
          [class.bg-surface]="tab() === 'open'" [class.text-text]="tab() === 'open'">{{ l().openGames }}</button>
        <button type="button" role="tab" [attr.aria-selected]="tab() === 'mine'" (click)="show('mine')" class="h-10 flex-1 rounded-md font-medium text-muted"
          [class.bg-surface]="tab() === 'mine'" [class.text-text]="tab() === 'mine'">{{ l().yours }}</button>
      </div>

      @if (tab() === 'open') {
        <div class="mt-3 flex flex-wrap gap-2" role="group" [attr.aria-label]="l().kindOfGame">
          @for (t of types(); track t.label) {
            <button type="button" (click)="type.set(t.value)" [attr.aria-pressed]="type() === t.value" class="h-9 rounded-full border px-4 text-sm font-medium"
              [class]="type() === t.value ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">{{ t.label }}</button>
          }
        </div>
        <div class="-mx-4 mt-2 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]" role="group" [attr.aria-label]="l().day">
          <button type="button" (click)="day.set(null)" [attr.aria-pressed]="day() === null" class="h-9 shrink-0 rounded-full border px-4 text-sm font-medium"
            [class]="day() === null ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">{{ l().anyDay }}</button>
          @for (d of days; track d; let i = $index) {
            <button type="button" (click)="day.set(d)" [attr.aria-pressed]="day() === d" class="h-9 shrink-0 rounded-full border px-4 text-sm font-medium"
              [class]="day() === d ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted'">{{ i === 0 ? l().today : i === 1 ? l().tomorrow : dayLabel(d) }}</button>
          }
        </div>
      }

      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (posts(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">{{ tab() === 'open' ? l().noOpenGames : l().noPostedGames }}</p>
            <p class="mt-1 text-muted">{{ l().noGamesBody }}</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (post of list; track post.id) {
              <li>
                <a [routerLink]="['/matches', post.id]" class="block rounded-md border border-border bg-surface p-4 hover:border-subtle">
                  <fs-match-summary [post]="post" />
                  @if (post.mine) {
                    <p class="mt-2 text-sm font-medium">{{ pending(post) ? l().yourPostAsking(pending(post)) : l().yourPost }}</p>
                  } @else if (post.myRequest) {
                    <p class="mt-2 text-sm font-medium" [class.text-confirmed-fg]="post.myRequest.status === 'ACCEPTED'">
                      {{ post.myRequest.status === 'PENDING' ? l().youAskedToJoin : post.myRequest.status === 'ACCEPTED' ? l().youreIn : post.myRequest.status === 'DECLINED' ? l().notThisTime : l().youWithdrew }}
                    </p>
                  }
                </a>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 space-y-3" [attr.aria-label]="l().loading">
          @for (i of [1, 2]; track i) {
            <div class="h-32 animate-pulse rounded-md bg-surface"></div>
          }
        </div>
      }
    </div>
  `,
})
export class Matches {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly icons = { Plus };

  protected readonly dayLabel = dayLabel;

  protected readonly types = computed<{ value: MatchPostType | null; label: string }[]>(() => [
    { value: null, label: l().all },
    { value: 'NEED_PLAYERS', label: l().playersWanted },
    { value: 'FIND_OPPONENT', label: l().opponentWanted },
  ]);

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
