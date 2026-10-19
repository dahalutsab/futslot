import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ArrowRight, Eye, EyeOff, LoaderCircle, LucideAngularModule, ShieldCheck } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { User } from '../../core/api.types';
import { Session } from '../../core/session';

/** signIn: phone and password. reset: a texted code, then a new password (or the first one, for an older account). */
type Mode = 'signIn' | 'reset';

const MIN_PASSWORD = 8;

const NOT_ADMIN = "This number isn't a FutSlot admin account.";

/** Sign in with a phone number and password, or reset a forgotten one. Only FutSlot admin accounts get in. */
@Component({
  selector: 'fs-login',
  imports: [FormsModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
})
export class Login implements OnDestroy {
  private readonly session = inject(Session);

  private readonly router = inject(Router);

  private readonly route = inject(ActivatedRoute);

  protected readonly icons = { ArrowRight, Eye, EyeOff, LoaderCircle, ShieldCheck };

  protected readonly minPassword = MIN_PASSWORD;

  protected readonly mode = signal<Mode>('signIn');

  protected readonly codeSent = signal(false);

  protected readonly phone = signal('');

  protected readonly password = signal('');

  protected readonly showPassword = signal(false);

  protected readonly code = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly notice = signal<string | null>(null);

  protected readonly resendIn = signal(0);

  protected readonly expired = this.route.snapshot.queryParamMap.has('expired');

  protected readonly phoneValid = computed(() => /^\d{10}$/.test(this.digits(this.phone())));

  private timer: ReturnType<typeof setInterval> | null = null;

  switchTo(mode: Mode, notice: string | null = null): void {
    this.mode.set(mode);
    this.codeSent.set(false);
    this.code.set('');
    this.password.set('');
    this.error.set(null);
    this.notice.set(notice);
  }

  // Buttons stay enabled and check on submit: a disabled submit button makes the browser ignore Enter, which
  // fast typing or autofill can hit before the button re-enables.
  async signIn(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.phoneValid()) {
      this.error.set('Enter your 10-digit mobile number, like 98XXXXXXXX.');
      return;
    }
    if (!this.password()) {
      this.error.set('Enter your password.');
      return;
    }
    await this.run(() => this.session.signIn(this.digits(this.phone()), this.password()), (code) => {
      if (code === 'PASSWORD_NOT_SET') {
        this.switchTo('reset', "Your account doesn't have a password yet. We'll text you a code to set one.");
        return true;
      }
      return false;
    });
  }

  async sendCode(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.phoneValid()) {
      this.error.set('Enter your 10-digit mobile number, like 98XXXXXXXX.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const sent = await this.session.requestCode(this.digits(this.phone()), 'RESET_PASSWORD');
      this.showCodeStep(sent.resendAfterSeconds);
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'OTP_COOLDOWN') {
        // A code went out under a minute ago (a double tap, or a reload): let them use it
        this.notice.set('We sent you a code a moment ago. Enter it below.');
        this.showCodeStep(60);
      }
      else {
        // A number new to FutSlot can't be an admin
        this.error.set(problem.code === 'ACCOUNT_NOT_FOUND' ? NOT_ADMIN : problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  async reset(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!/^\d{6}$/.test(this.code())) {
      this.error.set('Enter the 6-digit code we sent.');
      return;
    }
    if ([...this.password()].length < MIN_PASSWORD) {
      this.error.set(`Use at least ${MIN_PASSWORD} characters for your password.`);
      return;
    }
    await this.run(() => this.session.resetPassword(this.digits(this.phone()), this.code(), this.password()), () => false);
  }

  changeNumber(): void {
    this.codeSent.set(false);
    this.code.set('');
    this.notice.set(null);
    this.error.set(null);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  /** Signs in with {@code action}, admins only, then goes where they were headed. */
  private async run(action: () => Promise<User>, handled: (code: string | null) => boolean): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      const user = await action();
      if (!user.roles.includes('ADMIN')) {
        await this.session.signOut();
        this.switchTo('signIn');
        this.error.set(NOT_ADMIN);
        return;
      }
      const next = this.route.snapshot.queryParamMap.get('next');
      await this.router.navigateByUrl(next && next.startsWith('/') && !next.startsWith('//') ? next : '/');
    }
    catch (error) {
      const problem = problemOf(error);
      if (!handled(problem.code)) {
        this.error.set(problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  private showCodeStep(resendAfter: number): void {
    this.codeSent.set(true);
    this.startCountdown(resendAfter);
    queueMicrotask(() => document.getElementById('code')?.focus());
  }

  private startCountdown(seconds: number): void {
    this.resendIn.set(seconds);
    if (this.timer) {
      clearInterval(this.timer);
    }
    this.timer = setInterval(() => {
      this.resendIn.update((left) => Math.max(0, left - 1));
      if (this.resendIn() === 0 && this.timer) {
        clearInterval(this.timer);
      }
    }, 1000);
  }

  /** Accepts "98-1234 5678", "+977 9812345678" and so on; the API wants the 10 local digits. */
  private digits(input: string): string {
    const digits = input.replace(/\D/g, '');
    return digits.startsWith('977') && digits.length === 13 ? digits.slice(3) : digits;
  }
}
