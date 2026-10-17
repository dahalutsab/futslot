import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Bell, LucideAngularModule } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { AppNotification } from '../../core/api.types';
import { Badges } from '../../core/badges';
import { ago } from '../../shared/format';
import { notificationLink } from '../../shared/notification-link';

/** Everything FutSlot told the player: newest first, unread marked, each opening what it's about. */
@Component({
  selector: 'fs-notifications',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <div class="flex items-center gap-3">
        <h1 class="flex-1 text-2xl font-semibold tracking-tight">Notifications</h1>
        @if (hasUnread()) {
          <button type="button" (click)="readAll()" class="h-11 rounded-lg px-3 text-sm font-medium text-muted hover:bg-surface-2 hover:text-text">Mark all read</button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-6 rounded-xl border border-border bg-surface px-6 py-10 text-center text-muted">{{ message }}</p>
      } @else if (items(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center">
            <lucide-icon [img]="icons.Bell" [size]="28" class="mx-auto text-subtle" />
            <p class="mt-3 font-medium">Nothing yet</p>
            <p class="mt-1 text-muted">Booking confirmations, reminders and replies from venues show up here.</p>
          </div>
        } @else {
          <ul class="mt-4 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
            @for (n of list; track n.id) {
              <li>
                <button type="button" (click)="open(n)" class="flex w-full gap-3 px-4 py-3 text-left hover:bg-surface-2">
                  <span class="mt-2 size-2 shrink-0 rounded-full" [class]="n.read ? 'bg-transparent' : 'bg-ring'" [attr.aria-label]="n.read ? null : 'Unread'"></span>
                  <span class="min-w-0 flex-1">
                    <span class="flex items-baseline gap-2">
                      <span class="min-w-0 flex-1" [class]="n.read ? 'font-medium' : 'font-semibold'">{{ n.title }}</span>
                      <span class="shrink-0 text-xs text-subtle">{{ ago(n.createdAt) }}</span>
                    </span>
                    <span class="mt-0.5 block text-sm text-muted">{{ n.body }}</span>
                  </span>
                </button>
              </li>
            }
          </ul>
          @if (hasMore()) {
            <div class="mt-6 text-center">
              <button type="button" (click)="load(page() + 1)" class="h-11 rounded-lg border border-border bg-surface px-6 font-medium hover:bg-surface-2">Show older</button>
            </div>
          }
        }
      } @else {
        <div class="mt-6 h-48 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Notifications {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly badges = inject(Badges);

  protected readonly icons = { Bell };

  protected readonly ago = ago;

  protected readonly items = signal<AppNotification[] | null>(null);

  protected readonly page = signal(0);

  protected readonly hasMore = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly hasUnread = () => (this.items() ?? []).some((n) => !n.read);

  constructor() {
    void this.load(0);
  }

  protected async open(notification: AppNotification): Promise<void> {
    if (!notification.read) {
      this.items.update((list) => list?.map((n) => (n.id === notification.id ? { ...n, read: true } : n)) ?? null);
      void this.api.readNotification(notification.id).then(() => this.badges.refresh(), () => undefined);
    }
    const link = notificationLink(notification);
    if (link) {
      await this.router.navigate(link.path, { queryParams: link.query });
    }
  }

  protected async readAll(): Promise<void> {
    try {
      await this.api.readAllNotifications();
      this.items.update((list) => list?.map((n) => ({ ...n, read: true })) ?? null);
      void this.badges.refresh();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  protected async load(page: number): Promise<void> {
    try {
      const result = await this.api.notifications(page);
      this.items.set(page === 0 ? result.content : [...(this.items() ?? []), ...result.content]);
      this.page.set(page);
      this.hasMore.set(page + 1 < result.totalPages);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
