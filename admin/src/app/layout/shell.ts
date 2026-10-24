import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  ArrowDownToLine, ArrowUpFromLine, Building2, CreditCard, LayoutDashboard, LogOut, LucideAngularModule, LucideIconData,
  Monitor, Moon, ScrollText, Star, Sun, Wallet,
} from 'lucide-angular';

import { Queue, QueueCounts } from '../core/queue';
import { Session } from '../core/session';
import { Theme, ThemeChoice } from '../core/theme';
import { Logo } from '../shared/logo';

interface NavItem {
  path: string;
  label: string;
  icon: LucideIconData;
  /** The queue count shown next to it. */
  count?: keyof QueueCounts;
}

const NAVIGATION: NavItem[] = [
  { path: '/', label: 'Overview', icon: LayoutDashboard },
  { path: '/venues', label: 'Venues', icon: Building2, count: 'venues' },
  { path: '/payments', label: 'Payments to check', icon: Wallet, count: 'payments' },
  { path: '/withdrawals', label: 'Deposits to send', icon: ArrowUpFromLine, count: 'withdrawals' },
  { path: '/payouts', label: 'Venue payouts', icon: ArrowDownToLine, count: 'payouts' },
  { path: '/reviews', label: 'Reviews', icon: Star },
  { path: '/billing', label: 'Plans & billing', icon: CreditCard },
  { path: '/audit', label: 'Audit log', icon: ScrollText },
];

/** A sidebar on wide screens; on phones, a top bar with the sections in a row under it. */
@Component({
  selector: 'fs-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule, Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a href="#main" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-surface focus:px-3 focus:py-2">Skip to content</a>
    <div class="min-h-dvh lg:grid lg:grid-cols-[240px_1fr]">
      <aside class="sticky top-0 hidden h-dvh flex-col border-r border-border bg-surface lg:flex">
        <div class="flex h-14 items-center gap-2 px-5">
          <fs-logo name="FutSlot admin" />
        </div>
        <nav class="flex-1 overflow-y-auto px-3 pb-4" aria-label="Main">
          <ul>
            @for (item of items; track item.path) {
              <li>
                <a [routerLink]="item.path" routerLinkActive="bg-surface-2 text-text font-medium" [routerLinkActiveOptions]="{ exact: item.path === '/' }"
                  ariaCurrentWhenActive="page" class="flex h-9 items-center gap-3 rounded-md px-2 text-muted hover:bg-surface-2 hover:text-text">
                  <lucide-icon [img]="item.icon" [size]="18" [strokeWidth]="1.75" />
                  <span class="flex-1">{{ item.label }}</span>
                  @if (item.count && queue.counts()[item.count]; as n) {
                    <span class="tabular rounded-full bg-verify-bg px-2 text-xs font-medium text-verify-fg" [attr.aria-label]="n + ' waiting'">{{ n }}</span>
                  }
                </a>
              </li>
            }
          </ul>
        </nav>
        <div class="border-t border-border p-3">
          <div class="mb-3 flex rounded-md bg-surface-2 p-1" role="radiogroup" aria-label="Theme">
            @for (option of themes; track option.value) {
              <button type="button" role="radio" [attr.aria-checked]="theme.choice() === option.value" (click)="theme.set(option.value)"
                class="flex h-8 flex-1 items-center justify-center gap-1.5 rounded-md text-xs text-muted"
                [class.bg-surface]="theme.choice() === option.value" [class.text-text]="theme.choice() === option.value">
                <lucide-icon [img]="option.icon" [size]="14" />{{ option.label }}
              </button>
            }
          </div>
          <div class="flex items-center gap-2 px-1">
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">{{ session.user()?.fullName }}</p>
              <p class="tabular truncate text-xs text-subtle">{{ session.user()?.phone }}</p>
            </div>
            <button type="button" (click)="session.signOut()" aria-label="Sign out"
              class="grid size-11 place-items-center rounded-md text-muted hover:bg-surface-2 hover:text-text">
              <lucide-icon [img]="icons.LogOut" [size]="18" />
            </button>
          </div>
        </div>
      </aside>

      <header class="sticky top-0 z-20 border-b border-border bg-surface lg:hidden">
        <div class="flex h-14 items-center gap-2 px-4">
          <fs-logo name="FutSlot admin" class="flex-1" />
          <button type="button" (click)="session.signOut()" aria-label="Sign out" class="grid size-11 place-items-center rounded-md text-muted">
            <lucide-icon [img]="icons.LogOut" [size]="18" />
          </button>
        </div>
        <nav class="flex gap-1 overflow-x-auto px-3 pb-2 [scrollbar-width:none]" aria-label="Main">
          @for (item of items; track item.path) {
            <a [routerLink]="item.path" routerLinkActive="bg-surface-2 text-text" [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              ariaCurrentWhenActive="page" class="flex h-9 shrink-0 items-center gap-1.5 rounded-md px-3 text-sm font-medium text-muted">
              {{ item.label }}
              @if (item.count && queue.counts()[item.count]; as n) {
                <span class="tabular rounded-full bg-verify-bg px-1.5 text-xs text-verify-fg">{{ n }}</span>
              }
            </a>
          }
        </nav>
      </header>

      <main id="main" tabindex="-1" class="min-w-0">
        <router-outlet />
      </main>
    </div>
  `,
})
export class Shell {
  protected readonly session = inject(Session);

  protected readonly theme = inject(Theme);

  protected readonly queue = inject(Queue);

  protected readonly items = NAVIGATION;

  protected readonly icons = { LogOut };

  protected readonly themes: { value: ThemeChoice; label: string; icon: LucideIconData }[] = [
    { value: 'light', label: 'Light', icon: Sun },
    { value: 'dark', label: 'Dark', icon: Moon },
    { value: 'system', label: 'Auto', icon: Monitor },
  ];
}
