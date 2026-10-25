import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BadgeCheck, ChevronRight, Coins, Heart, Hourglass, Icon, IconData, LogOut, MessageSquare, Shirt, Star, Wallet } from '../../shared/icon';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';
import { currentLanguage, l, Language } from '../../i18n/i18n';
import { LanguageChoice } from '../../i18n/language-choice';
import { phone } from '../../shared/format';
import { LANGUAGE_NAMES } from '../../shared/language-toggle';

interface Link {
  path: string;
  label: string;
  detail: string;
  icon: IconData;
}

/** The player's account: their details, everything else they keep with FutSlot, language and signing out. */
@Component({
  selector: 'fs-me',
  imports: [FormsModule, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-xl px-4 py-6 lg:py-10">
      <h1 class="text-4xl">{{ session.user()?.fullName }}</h1>
      <p class="tabular text-muted">{{ fmt.phone(session.user()?.phone) }}</p>

      <ul class="mt-5 divide-y divide-border rounded-md border border-border bg-surface">
        @for (link of links(); track link.path) {
          <li>
            <a [routerLink]="link.path" class="flex min-h-14 items-center gap-3 px-4 py-3 hover:bg-surface-2">
              <fs-icon [img]="link.icon" [size]="20" [strokeWidth]="1.75" class="text-muted" />
              <span class="min-w-0 flex-1"><span class="block font-medium">{{ link.label }}</span><span class="block text-sm text-muted">{{ link.detail }}</span></span>
              <fs-icon [img]="icons.ChevronRight" [size]="18" class="text-subtle" />
            </a>
          </li>
        }
      </ul>

      <section class="mt-6 rounded-md border border-border bg-surface p-5" aria-labelledby="details-title">
        <h2 id="details-title" class="font-semibold">{{ l().yourDetails }}</h2>
        <form class="mt-3 space-y-3" (ngSubmit)="save()">
          <div>
            <label for="full-name" class="mb-1.5 block font-medium">{{ l().name }}</label>
            <input id="full-name" name="fullName" type="text" maxlength="100" autocomplete="name"
              class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" [ngModel]="fullName()" (ngModelChange)="fullName.set($event)" />
            <p class="mt-1 text-sm text-subtle">{{ l().venuesSeeThis }}</p>
          </div>
          <div>
            <label for="email" class="mb-1.5 block font-medium">{{ l().email }} <span class="font-normal text-subtle">{{ l().optional }}</span></label>
            <input id="email" name="email" type="email" maxlength="254" autocomplete="email"
              class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" [ngModel]="email()" (ngModelChange)="email.set($event)" />
          </div>
          @if (message(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="saving()" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">{{ saving() ? l().saving : l().save }}</button>
        </form>
      </section>

      <section class="mt-6 rounded-md border border-border bg-surface p-5" aria-labelledby="password-title">
        <h2 id="password-title" class="font-semibold">{{ hasPassword() ? l().password : l().setAPassword }}</h2>
        @if (!hasPassword()) {
          <p class="mt-1 text-sm text-muted">{{ l().googleOnlyNote }}</p>
        }
        <form class="mt-3 space-y-3" (ngSubmit)="changePassword()">
          <input type="text" name="username" autocomplete="username" [value]="session.user()?.phone ?? ''" class="hidden" aria-hidden="true" tabindex="-1" />
          @if (hasPassword()) {
            <div>
              <label for="current-password" class="mb-1.5 block font-medium">{{ l().currentPassword }}</label>
              <input id="current-password" name="currentPassword" type="password" autocomplete="current-password"
                class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" [ngModel]="currentPassword()" (ngModelChange)="currentPassword.set($event)" />
            </div>
          }
          <div>
            <label for="new-password" class="mb-1.5 block font-medium">{{ l().newPassword }}</label>
            <input id="new-password" name="newPassword" type="password" autocomplete="new-password" aria-describedby="new-password-help"
              class="h-11 w-full rounded-md border border-border bg-surface px-3 text-base" [ngModel]="newPassword()" (ngModelChange)="newPassword.set($event)" />
            <p id="new-password-help" class="mt-1 text-sm text-subtle">{{ l().passwordStayHere }}</p>
          </div>
          @if (passwordMessage(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="changing()" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">
            {{ changing() ? l().saving : hasPassword() ? l().changePassword : l().setPassword }}
          </button>
        </form>
      </section>

      <section class="mt-6 rounded-md border border-border bg-surface p-5" aria-labelledby="language-title">
        <h2 id="language-title" class="font-semibold">{{ l().language }}</h2>
        <p class="mt-1 text-sm text-muted">{{ l().languageHelp }}</p>
        <div class="mt-3 flex rounded-md bg-surface-2 p-1" role="radiogroup" aria-labelledby="language-title">
          @for (option of languageOptions; track option) {
            <button type="button" role="radio" [attr.aria-checked]="language() === option" (click)="languages.set(option)" [attr.lang]="option"
              class="h-9 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="language() === option" [class.text-text]="language() === option">{{ languageNames[option] }}</button>
          }
        </div>
      </section>


      <button type="button" (click)="session.signOut()" class="mt-6 flex h-12 w-full items-center justify-center gap-2 rounded-md font-medium text-danger hover:bg-danger-bg">
        <fs-icon [img]="icons.LogOut" [size]="18" /> {{ l().signOut }}
      </button>
    </div>
  `,
})
export class Me {
  protected readonly l = l;

  protected readonly session = inject(Session);


  private readonly api = inject(Api);

  protected readonly icons = { ChevronRight, LogOut };

  protected readonly fmt = { phone };

  protected readonly links = computed<Link[]>(() => [
    { path: '/me/profile', label: l().meProfile, detail: l().meProfileDetail, icon: Shirt },
    { path: '/messages', label: l().tabMessages, detail: l().meMessagesDetail, icon: MessageSquare },
    { path: '/deposit', label: l().meDeposit, detail: l().meDepositDetail, icon: Wallet },
    { path: '/memberships', label: l().meMemberships, detail: l().meMembershipsDetail, icon: BadgeCheck },
    { path: '/splits', label: l().meDues, detail: l().meDuesDetail, icon: Coins },
    { path: '/favourites', label: l().meFavourites, detail: l().meFavouritesDetail, icon: Heart },
    { path: '/reviews', label: l().meReviews, detail: l().meReviewsDetail, icon: Star },
    { path: '/waitlist', label: l().meWaitlist, detail: l().meWaitlistDetail, icon: Hourglass },
  ]);

  protected readonly languages = inject(LanguageChoice);

  protected readonly language = currentLanguage;

  protected readonly languageOptions: Language[] = ['en', 'ne'];

  protected readonly languageNames = LANGUAGE_NAMES;


  protected readonly fullName = signal(this.session.user()?.fullName ?? '');

  protected readonly email = signal(this.session.user()?.email ?? '');

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
      this.message.set({ ok: false, text: l().enterName });
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    try {
      const user = await this.api.updateMe({ fullName, email: this.email().trim() || undefined });
      this.session.updateUser(user);
      this.message.set({ ok: true, text: l().saved });
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
      this.passwordMessage.set({ ok: false, text: l().passwordTooShort(8) });
      return;
    }
    if (this.hasPassword() && !this.currentPassword()) {
      this.passwordMessage.set({ ok: false, text: l().enterCurrentPassword });
      return;
    }
    this.changing.set(true);
    this.passwordMessage.set(null);
    try {
      await this.session.changePassword(this.hasPassword() ? this.currentPassword() : null, this.newPassword());
      this.currentPassword.set('');
      this.newPassword.set('');
      this.passwordMessage.set({ ok: true, text: l().passwordSaved });
    }
    catch (error) {
      this.passwordMessage.set({ ok: false, text: problemOf(error).message });
    }
    finally {
      this.changing.set(false);
    }
  }
}
