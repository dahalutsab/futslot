import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { Bell, CalendarDays, CircleUser, Compass, House, Icon, IconData, LogIn, MessageSquare, Trophy } from '../shared/icon';
import { filter, map } from 'rxjs';

import { Badges } from '../core/badges';
import { Session } from '../core/session';
import { digits, l, Strings } from '../i18n/i18n';
import { LanguageToggle } from '../shared/language-toggle';
import { Logo } from '../shared/logo';

interface NavItem {
  path: string;
  label: (words: Strings) => string;
  icon: IconData;
  /** Only for signed-in players. */
  private: boolean;
  /** Shown in the phone's bottom bar (the rest are under Me). */
  onPhoneBar: boolean;
  /** Shown in the desktop top bar (Me is the player's name there instead). */
  onDesktop: boolean;
  badge?: 'messages';
  /** Other paths that belong to this item, so it shows as current there too. */
  also?: string[];
}

const NAVIGATION: NavItem[] = [
  { path: '/home', label: (w) => w.tabHome, icon: House, private: true, onPhoneBar: true, onDesktop: true },
  { path: '/explore', label: (w) => w.tabExplore, icon: Compass, private: false, onPhoneBar: true, onDesktop: true },
  { path: '/bookings', label: (w) => w.tabBookings, icon: CalendarDays, private: true, onPhoneBar: true, onDesktop: true },
  { path: '/matches', label: (w) => w.tabPlay, icon: Trophy, private: true, onPhoneBar: true, onDesktop: true, also: ['/tournaments', '/teams'] },
  { path: '/messages', label: (w) => w.tabMessages, icon: MessageSquare, private: true, onPhoneBar: false, onDesktop: true, badge: 'messages' },
  { path: '/me', label: (w) => w.tabMe, icon: CircleUser, private: true, onPhoneBar: true, onDesktop: false },
];

/** The frame around every screen: a top bar, and a bottom bar on phones. Browsing works signed out. */
@Component({
  selector: 'fs-shell',
  imports: [RouterOutlet, RouterLink, Icon, LanguageToggle, Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a href="#main" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-surface focus:px-3 focus:py-2">{{ l().skipToContent }}</a>

    <!-- The masthead: the name in the serif on paper, sections beside it, no rule (the pages draw their own) -->
    <header class="sticky top-0 z-20 bg-bg">
      <div class="mx-auto flex h-16 max-w-[1280px] items-center gap-2 px-5 lg:h-[76px] lg:px-[72px]">
        <a routerLink="/" class="mr-auto flex min-h-11 items-center lg:mr-7" [attr.aria-label]="l().futSlotHome">
          <fs-logo class="text-[21px] lg:text-2xl" />
        </a>
        <nav class="hidden flex-1 items-center gap-2 lg:flex" [attr.aria-label]="l().mainNav">
          @for (item of desktopItems(); track item.path) {
            <a [routerLink]="item.path" [attr.aria-current]="current(item) ? 'page' : null"
              class="flex items-center gap-1.5 px-3 py-2 text-[15px] hover:text-accent-700"
              [class]="current(item) ? 'text-text' : 'text-text/70'">
              <fs-icon [img]="item.icon" [size]="16" />{{ item.label(l()) }}
              @if (item.badge && badges.messages()) {
                <span class="tabular rounded-[1.5px] bg-accent-100 px-[7px] py-px text-[11px] tracking-[0.02em] text-accent-800"
                  [attr.aria-label]="l().unreadCount(badges.messages())">{{ digits(badges.messages()) }}</span>
              }
            </a>
          }
        </nav>
        <div class="flex items-center gap-4">
          @if (!session.signedIn()) {
            <fs-language-toggle />
            <a routerLink="/login" class="hidden min-h-10 items-center rounded-md border border-divider px-[18px] text-sm font-semibold hover:bg-text/7 active:bg-text/14 lg:flex">
              {{ l().signIn }}
            </a>
          } @else {
            <a routerLink="/notifications" class="relative grid size-11 place-items-center rounded-md text-accent hover:bg-accent/10"
              [attr.aria-label]="badges.notifications() ? l().notificationsUnread(badges.notifications()) : l().notifications">
              <fs-icon [img]="icons.Bell" [size]="22" />
              @if (badges.notifications()) {
                <span class="tabular absolute right-1.5 top-1.5 min-w-4 rounded-[1.5px] bg-accent-2 px-1 text-center text-[11px] font-semibold leading-4 text-bg" aria-hidden="true">{{ digits(badges.notifications() > 9 ? '9+' : badges.notifications()) }}</span>
              }
            </a>
            <a routerLink="/me" class="hidden max-w-48 truncate text-[15px] hover:text-accent-700 lg:inline">{{ session.user()?.fullName }}</a>
          }
        </div>
      </div>
    </header>

    <main id="main" tabindex="-1" class="min-w-0 pb-24 lg:pb-16">
      <router-outlet />
    </main>

    <!-- On phones, the sections along the bottom: icon and label, the current one in full ink, a hairline above -->
    <nav class="fixed inset-x-0 bottom-0 z-20 bg-bg pb-[env(safe-area-inset-bottom)] shadow-[0_-1px_0_var(--color-divider)] lg:hidden" [attr.aria-label]="l().mainNav">
      <ul class="grid" [style.grid-template-columns]="'repeat(' + phoneItems().length + ', minmax(0, 1fr))'">
        @for (item of phoneItems(); track item.path) {
          <li>
            <a [routerLink]="item.path" [attr.aria-current]="current(item) ? 'page' : null"
              class="relative flex h-[60px] flex-col items-center justify-center gap-0.5 text-xs" [class]="current(item) ? 'text-text' : 'text-text/60'">
              <fs-icon [img]="item.icon" [size]="24" />{{ item.label(l()) }}
              @if (item.badge && badges.messages()) {
                <span class="tabular absolute left-1/2 top-1.5 ml-2 min-w-4 rounded-[1.5px] bg-accent-100 px-1 text-center text-[11px] leading-4 text-accent-800"
                  [attr.aria-label]="l().unreadCount(badges.messages())">{{ digits(badges.messages()) }}</span>
              }
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
})
export class Shell {
  protected readonly l = l;

  protected readonly session = inject(Session);

  protected readonly badges = inject(Badges);

  protected readonly icons = { Bell, LogIn };

  protected readonly digits = digits;

  private readonly router = inject(Router);

  private readonly url = toSignal(this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url });

  /** Whether the page shown belongs to {@code item}. */
  protected current(item: NavItem): boolean {
    const path = this.url().split(/[?#]/)[0];
    const under = (base: string) => path === base || path.startsWith(base + '/');
    return item.path === '/explore' ? under('/explore') || path.startsWith('/venues') : under(item.path) || (item.also ?? []).some(under);
  }

  private readonly available = computed(() => NAVIGATION.filter((item) => !item.private || this.session.signedIn()));

  protected readonly desktopItems = computed(() => this.available().filter((item) => item.onDesktop));

  protected readonly phoneItems = computed(() => {
    const items = this.available().filter((item) => item.onPhoneBar);
    return this.session.signedIn() ? items
      : [...items, { path: '/login', label: (w: Strings) => w.signIn, icon: LogIn, private: false, onPhoneBar: true, onDesktop: false }];
  });
}
