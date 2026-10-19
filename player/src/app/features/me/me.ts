import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BadgeCheck, ChevronRight, Coins, Heart, Hourglass, LogOut, LucideAngularModule, LucideIconData, Monitor, Moon, Shirt, Star, Sun, Wallet } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';
import { Theme, ThemeChoice } from '../../core/theme';
import { phone } from '../../shared/format';

interface Link {
  path: string;
  label: string;
  detail: string;
  icon: LucideIconData;
}

/** The player's account: their details, everything else they keep with FutSlot, the theme and signing out. */
@Component({
  selector: 'fs-me',
  imports: [FormsModule, RouterLink, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-xl px-4 py-6 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">{{ session.user()?.fullName }}</h1>
      <p class="tabular text-muted">{{ fmt.phone(session.user()?.phone) }}</p>

      <ul class="mt-5 divide-y divide-border rounded-xl border border-border bg-surface">
        @for (link of links; track link.path) {
          <li>
            <a [routerLink]="link.path" class="flex min-h-14 items-center gap-3 px-4 py-3 hover:bg-surface-2">
              <lucide-icon [img]="link.icon" [size]="20" [strokeWidth]="1.75" class="text-muted" />
              <span class="min-w-0 flex-1"><span class="block font-medium">{{ link.label }}</span><span class="block text-sm text-muted">{{ link.detail }}</span></span>
              <lucide-icon [img]="icons.ChevronRight" [size]="18" class="text-subtle" />
            </a>
          </li>
        }
      </ul>

      <section class="mt-6 rounded-xl border border-border bg-surface p-5" aria-labelledby="details-title">
        <h2 id="details-title" class="font-semibold">Your details</h2>
        <form class="mt-3 space-y-3" (ngSubmit)="save()">
          <div>
            <label for="full-name" class="mb-1.5 block font-medium">Name</label>
            <input id="full-name" name="fullName" type="text" maxlength="100" autocomplete="name"
              class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="fullName()" (ngModelChange)="fullName.set($event)" />
            <p class="mt-1 text-sm text-subtle">Venues see this on your bookings.</p>
          </div>
          <div>
            <label for="email" class="mb-1.5 block font-medium">Email <span class="font-normal text-subtle">(optional)</span></label>
            <input id="email" name="email" type="email" maxlength="254" autocomplete="email"
              class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="email()" (ngModelChange)="email.set($event)" />
          </div>
          <fieldset>
            <legend class="mb-1.5 font-medium">Texts and notifications in</legend>
            <div class="flex rounded-lg bg-surface-2 p-1" role="radiogroup">
              @for (option of languages; track option.value) {
                <button type="button" role="radio" [attr.aria-checked]="language() === option.value" (click)="language.set(option.value)"
                  class="h-9 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="language() === option.value" [class.text-text]="language() === option.value">{{ option.label }}</button>
              }
            </div>
          </fieldset>
          @if (message(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="saving()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ saving() ? 'Saving…' : 'Save' }}</button>
        </form>
      </section>

      <section class="mt-6 rounded-xl border border-border bg-surface p-5" aria-labelledby="password-title">
        <h2 id="password-title" class="font-semibold">{{ hasPassword() ? 'Password' : 'Set a password' }}</h2>
        @if (!hasPassword()) {
          <p class="mt-1 text-sm text-muted">You sign in with Google. A password lets you sign in with your number too.</p>
        }
        <form class="mt-3 space-y-3" (ngSubmit)="changePassword()">
          <input type="text" name="username" autocomplete="username" [value]="session.user()?.phone ?? ''" class="hidden" aria-hidden="true" tabindex="-1" />
          @if (hasPassword()) {
            <div>
              <label for="current-password" class="mb-1.5 block font-medium">Current password</label>
              <input id="current-password" name="currentPassword" type="password" autocomplete="current-password"
                class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="currentPassword()" (ngModelChange)="currentPassword.set($event)" />
            </div>
          }
          <div>
            <label for="new-password" class="mb-1.5 block font-medium">New password</label>
            <input id="new-password" name="newPassword" type="password" autocomplete="new-password" aria-describedby="new-password-help"
              class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" [ngModel]="newPassword()" (ngModelChange)="newPassword.set($event)" />
            <p id="new-password-help" class="mt-1 text-sm text-subtle">At least 8 characters. You stay signed in here; other devices are signed out.</p>
          </div>
          @if (passwordMessage(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="changing()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">
            {{ changing() ? 'Saving…' : hasPassword() ? 'Change password' : 'Set password' }}
          </button>
        </form>
      </section>

      <section class="mt-6 rounded-xl border border-border bg-surface p-5" aria-labelledby="theme-title">
        <h2 id="theme-title" class="font-semibold">Appearance</h2>
        <div class="mt-3 flex rounded-lg bg-surface-2 p-1" role="radiogroup" aria-labelledby="theme-title">
          @for (option of themes; track option.value) {
            <button type="button" role="radio" [attr.aria-checked]="theme.choice() === option.value" (click)="theme.set(option.value)"
              class="flex h-9 flex-1 items-center justify-center gap-1.5 rounded-md text-sm font-medium text-muted"
              [class.bg-surface]="theme.choice() === option.value" [class.text-text]="theme.choice() === option.value">
              <lucide-icon [img]="option.icon" [size]="15" />{{ option.label }}
            </button>
          }
        </div>
      </section>

      <button type="button" (click)="session.signOut()" class="mt-6 flex h-12 w-full items-center justify-center gap-2 rounded-lg font-medium text-danger hover:bg-danger-bg">
        <lucide-icon [img]="icons.LogOut" [size]="18" /> Sign out
      </button>
    </div>
  `,
})
export class Me {
  protected readonly session = inject(Session);

  protected readonly theme = inject(Theme);

  private readonly api = inject(Api);

  protected readonly icons = { ChevronRight, LogOut };

  protected readonly fmt = { phone };

  protected readonly links: Link[] = [
    { path: '/me/profile', label: 'Player profile', detail: 'Your position and level, for games and teams', icon: Shirt },
    { path: '/deposit', label: 'Booking deposit', detail: 'Top up or take it back', icon: Wallet },
    { path: '/memberships', label: 'Memberships', detail: 'Game packs and passes', icon: BadgeCheck },
    { path: '/splits', label: 'What you owe', detail: 'Your share of games friends booked', icon: Coins },
    { path: '/favourites', label: 'Saved venues', detail: 'Venues you like, to book again', icon: Heart },
    { path: '/reviews', label: 'Reviews', detail: 'Rate the venues you played at', icon: Star },
    { path: '/waitlist', label: 'Waitlist', detail: 'Booked slots you want if they free up', icon: Hourglass },
  ];

  protected readonly languages: { value: 'en' | 'ne'; label: string }[] = [
    { value: 'en', label: 'English' },
    { value: 'ne', label: 'नेपाली' },
  ];

  protected readonly themes: { value: ThemeChoice; label: string; icon: LucideIconData }[] = [
    { value: 'light', label: 'Light', icon: Sun },
    { value: 'dark', label: 'Dark', icon: Moon },
    { value: 'system', label: 'Auto', icon: Monitor },
  ];

  protected readonly fullName = signal(this.session.user()?.fullName ?? '');

  protected readonly email = signal(this.session.user()?.email ?? '');

  protected readonly language = signal<'en' | 'ne'>(this.session.user()?.preferredLanguage === 'ne' ? 'ne' : 'en');

  protected readonly saving = signal(false);

  protected readonly message = signal<{ ok: boolean; text: string } | null>(null);

  /** Sessions saved before accounts said whether they have a password count as having one. */
  protected readonly hasPassword = computed(() => this.session.user()?.passwordSet !== false);

  protected readonly currentPassword = signal('');

  protected readonly newPassword = signal('');

  protected readonly changing = signal(false);

  protected readonly passwordMessage = signal<{ ok: boolean; text: string } | null>(null);

  protected async save(): Promise<void> {
    const fullName = this.fullName().trim();
    if (fullName.length < 2) {
      this.message.set({ ok: false, text: 'Enter your name.' });
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    try {
      const user = await this.api.updateMe({ fullName, email: this.email().trim() || undefined, preferredLanguage: this.language() });
      this.session.updateUser(user);
      this.message.set({ ok: true, text: 'Saved.' });
    }
    catch (error) {
      this.message.set({ ok: false, text: problemOf(error).message });
    }
    finally {
      this.saving.set(false);
    }
  }

  protected async changePassword(): Promise<void> {
    if ([...this.newPassword()].length < 8) {
      this.passwordMessage.set({ ok: false, text: 'Use at least 8 characters.' });
      return;
    }
    if (this.hasPassword() && !this.currentPassword()) {
      this.passwordMessage.set({ ok: false, text: 'Enter your current password.' });
      return;
    }
    this.changing.set(true);
    this.passwordMessage.set(null);
    try {
      await this.session.changePassword(this.hasPassword() ? this.currentPassword() : null, this.newPassword());
      this.currentPassword.set('');
      this.newPassword.set('');
      this.passwordMessage.set({ ok: true, text: 'Password saved. Other devices are signed out.' });
    }
    catch (error) {
      this.passwordMessage.set({ ok: false, text: problemOf(error).message });
    }
    finally {
      this.changing.set(false);
    }
  }
}
