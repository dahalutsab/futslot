import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ChevronRight, Heart, Hourglass, LogOut, LucideAngularModule, LucideIconData, Monitor, Moon, Star, Sun, Wallet } from 'lucide-angular';

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
    { path: '/deposit', label: 'Booking deposit', detail: 'Top up or take it back', icon: Wallet },
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
}
