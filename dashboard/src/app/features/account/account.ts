import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';

/** The signed-in person's own account: changing their password, or setting one if they sign in with Google. */
@Component({
  selector: 'fs-account',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-xl font-semibold tracking-tight lg:text-2xl">Your account</h1>
      <p class="tabular mt-1 text-muted">{{ session.user()?.fullName }} · {{ session.user()?.phone }}</p>

      <section class="mt-6 rounded-xl border border-border bg-surface p-5" aria-labelledby="password-title">
        <h2 id="password-title" class="font-semibold">{{ hasPassword() ? 'Password' : 'Set a password' }}</h2>
        @if (!hasPassword()) {
          <p class="mt-1 text-sm text-muted">You sign in with Google. A password lets you sign in with your number too.</p>
        }
        <form class="mt-3 space-y-3" (ngSubmit)="save()">
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
          @if (message(); as m) {
            <p [class]="m.ok ? 'text-confirmed-fg' : 'text-danger'" role="status">{{ m.text }}</p>
          }
          <button type="submit" [disabled]="saving()" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">
            {{ saving() ? 'Saving…' : hasPassword() ? 'Change password' : 'Set password' }}
          </button>
        </form>
      </section>
    </div>
  `,
})
export class Account {
  protected readonly session = inject(Session);

  /** Sessions saved before accounts said whether they have a password count as having one. */
  protected readonly hasPassword = computed(() => this.session.user()?.passwordSet !== false);

  protected readonly currentPassword = signal('');

  protected readonly newPassword = signal('');

  protected readonly saving = signal(false);

  protected readonly message = signal<{ ok: boolean; text: string } | null>(null);

  protected async save(): Promise<void> {
    if ([...this.newPassword()].length < 8) {
      this.message.set({ ok: false, text: 'Use at least 8 characters.' });
      return;
    }
    if (this.hasPassword() && !this.currentPassword()) {
      this.message.set({ ok: false, text: 'Enter your current password.' });
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    try {
      await this.session.changePassword(this.hasPassword() ? this.currentPassword() : null, this.newPassword());
      this.currentPassword.set('');
      this.newPassword.set('');
      this.message.set({ ok: true, text: 'Password saved. Other devices are signed out.' });
    }
    catch (error) {
      this.message.set({ ok: false, text: problemOf(error).message });
    }
    finally {
      this.saving.set(false);
    }
  }
}
