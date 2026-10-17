import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Bell, CalendarDays, CircleUser, Compass, LogIn, LucideAngularModule, LucideIconData, MessageSquare } from 'lucide-angular';

import { Badges } from '../core/badges';
import { Session } from '../core/session';

interface NavItem {
  path: string;
  label: string;
  icon: LucideIconData;
  /** Only for signed-in players. */
  private: boolean;
  /** Shown in the phone's bottom bar (the rest are under Me). */
  onPhoneBar: boolean;
  badge?: 'messages';
}

const NAVIGATION: NavItem[] = [
  { path: '/', label: 'Explore', icon: Compass, private: false, onPhoneBar: true },
  { path: '/bookings', label: 'Bookings', icon: CalendarDays, private: true, onPhoneBar: true },
  { path: '/messages', label: 'Messages', icon: MessageSquare, private: true, onPhoneBar: true, badge: 'messages' },
  { path: '/me', label: 'Me', icon: CircleUser, private: true, onPhoneBar: true },
];

/** The frame around every screen: a top bar, and a bottom bar on phones. Browsing works signed out. */
@Component({
  selector: 'fs-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a href="#main" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-surface focus:px-3 focus:py-2">Skip to content</a>

    <header class="sticky top-0 z-20 border-b border-border bg-surface/95 backdrop-blur">
      <div class="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4 lg:px-8">
        <a routerLink="/" class="flex items-center gap-2" aria-label="FutSlot home">
          <span class="grid size-7 place-items-center rounded-md bg-brand text-sm font-bold text-white" aria-hidden="true">F</span>
          <span class="font-semibold tracking-tight">FutSlot</span>
        </a>
        <nav class="hidden flex-1 items-center gap-1 lg:flex" aria-label="Main">
          @for (item of desktopItems(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="bg-surface-2 text-text" [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              ariaCurrentWhenActive="page" class="flex h-9 items-center gap-2 rounded-md px-3 font-medium text-muted hover:bg-surface-2 hover:text-text">
              <lucide-icon [img]="item.icon" [size]="17" [strokeWidth]="1.75" />{{ item.label }}
              @if (item.badge && badges.messages()) {
                <span class="tabular rounded-full bg-ink px-1.5 text-xs text-ink-fg" [attr.aria-label]="badges.messages() + ' unread'">{{ badges.messages() }}</span>
              }
            </a>
          }
        </nav>
        <div class="ml-auto flex items-center gap-2">
          @if (!session.signedIn()) {
            <a routerLink="/login" class="flex h-10 items-center gap-2 rounded-lg bg-ink px-4 font-medium text-ink-fg">
              <lucide-icon [img]="icons.LogIn" [size]="17" /> Sign in
            </a>
          } @else {
            <span class="hidden truncate text-sm text-muted lg:inline">{{ session.user()?.fullName }}</span>
            <a routerLink="/notifications" class="relative -mr-2 grid size-11 place-items-center rounded-md text-muted hover:bg-surface-2 hover:text-text"
              [attr.aria-label]="badges.notifications() ? 'Notifications, ' + badges.notifications() + ' unread' : 'Notifications'">
              <lucide-icon [img]="icons.Bell" [size]="20" [strokeWidth]="1.75" />
              @if (badges.notifications()) {
                <span class="tabular absolute right-1 top-1 min-w-4 rounded-full bg-danger px-1 text-center text-[11px] font-semibold leading-4 text-white" aria-hidden="true">{{ badges.notifications() > 9 ? '9+' : badges.notifications() }}</span>
              }
            </a>
          }
        </div>
      </div>
    </header>

    <main id="main" tabindex="-1" class="min-w-0 pb-20 lg:pb-8">
      <router-outlet />
    </main>

    <nav class="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden" aria-label="Main">
      <ul class="grid" [style.grid-template-columns]="'repeat(' + phoneItems().length + ', minmax(0, 1fr))'">
        @for (item of phoneItems(); track item.path) {
          <li>
            <a [routerLink]="item.path" routerLinkActive="text-text" [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              ariaCurrentWhenActive="page" class="relative flex h-16 flex-col items-center justify-center gap-1 text-xs font-medium text-muted">
              <lucide-icon [img]="item.icon" [size]="22" [strokeWidth]="1.75" />{{ item.label }}
              @if (item.badge && badges.messages()) {
                <span class="tabular absolute left-1/2 top-2 ml-2 min-w-4 rounded-full bg-ink px-1 text-center text-[11px] font-semibold leading-4 text-ink-fg"
                  [attr.aria-label]="badges.messages() + ' unread'">{{ badges.messages() }}</span>
              }
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
})
export class Shell {
  protected readonly session = inject(Session);

  protected readonly badges = inject(Badges);

  protected readonly icons = { Bell, LogIn };

  protected readonly desktopItems = computed(() => NAVIGATION.filter((item) => !item.private || this.session.signedIn()));

  protected readonly phoneItems = computed(() => {
    const items = this.desktopItems().filter((item) => item.onPhoneBar);
    return this.session.signedIn() ? items : [...items, { path: '/login', label: 'Sign in', icon: LogIn, private: false, onPhoneBar: true }];
  });
}
