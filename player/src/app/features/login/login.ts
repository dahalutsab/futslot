import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ArrowRight, Eye, EyeOff, LoaderCircle, LucideAngularModule, ShieldCheck } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { CodePurpose } from '../../core/api.types';
import { Session } from '../../core/session';
import { l } from '../../i18n/i18n';
import { GoogleButton } from '../../shared/google-button';
import { LanguageToggle } from '../../shared/language-toggle';

/**
 * signIn: phone and password. The others text a code first: signUp makes an account, reset sets a new password (or
 * the first one, for an account from before passwords), google proves the number on a first Google sign-in.
 */
type Mode = 'signIn' | 'signUp' | 'reset' | 'google';

const PURPOSE: Record<Exclude<Mode, 'signIn'>, CodePurpose> = { signUp: 'SIGN_UP', reset: 'RESET_PASSWORD', google: 'LINK_GOOGLE' };

const MIN_PASSWORD = 8;

/** Sign in with a phone number and password, or with Google; create an account; or reset a forgotten password. */
@Component({
  selector: 'fs-login',
  imports: [FormsModule, LucideAngularModule, RouterLink, GoogleButton, LanguageToggle],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
})
export class Login implements OnDestroy {
  protected readonly l = l;

  private readonly session = inject(Session);

  private readonly router = inject(Router);

  private readonly route = inject(ActivatedRoute);

  protected readonly icons = { ArrowRight, Eye, EyeOff, LoaderCircle, ShieldCheck };

  protected readonly minPassword = MIN_PASSWORD;

  protected readonly mode = signal<Mode>('signIn');

  /** For the modes that text a code: whether it's been sent. */
  protected readonly codeSent = signal(false);

  protected readonly phone = signal('');

  protected readonly password = signal('');

  protected readonly showPassword = signal(false);

  protected readonly code = signal('');

  protected readonly fullName = signal('');

  /** Google's sign-in needs a name typed in only when Google didn't give one. */
  protected readonly needsName = signal(false);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly notice = signal<string | null>(null);

  protected readonly resendIn = signal(0);

  protected readonly googleClientId = signal<string | null>(null);

  protected readonly expired = this.route.snapshot.queryParamMap.has('expired');

  protected readonly phoneValid = computed(() => /^\d{10}$/.test(this.digits(this.phone())));

  protected readonly title = computed(() => ({
    signIn: l().signInTitle,
    signUp: l().signUpTitle,
    reset: l().resetTitle,
    google: l().googleTitle,
  })[this.mode()]);

  protected readonly subtitle = computed(() => ({
    signIn: l().signInSubtitle,
    signUp: l().signUpSubtitle,
    reset: l().resetSubtitle,
    google: l().googleSubtitle,
  })[this.mode()]);

  /** The ID token from Google, kept while the player proves their number. */
  private googleToken: string | null = null;

  private timer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.session.options().then((options) => this.googleClientId.set(options.googleWebClientId), () => undefined);
  }

  switchTo(mode: Mode, notice: string | null = null): void {
    this.mode.set(mode);
    this.codeSent.set(false);
    this.code.set('');
    this.password.set('');
    this.needsName.set(false);
    this.error.set(null);
    this.notice.set(notice);
    queueMicrotask(() => document.getElementById(this.phoneValid() && mode === 'signIn' ? 'password' : 'phone')?.focus());
  }

  // Buttons stay enabled and check on submit: a disabled submit button makes the browser ignore Enter, which
  // fast typing or autofill can hit before the button re-enables.
  async signIn(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.phoneValid()) {
      this.error.set(l().enterMobile);
      return;
    }
    if (!this.password()) {
      this.error.set(l().enterPassword);
      return;
    }
    await this.run(() => this.session.signIn(this.digits(this.phone()), this.password()), (code) => {
      if (code === 'PASSWORD_NOT_SET') {
        this.switchTo('reset', l().passwordNotSet);
        return true;
      }
      return false;
    });
  }

  async sendCode(): Promise<void> {
    const mode = this.mode();
    if (this.busy() || mode === 'signIn') {
      return;
    }
    if (!this.phoneValid()) {
      this.error.set(l().enterMobile);
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const sent = await this.session.requestCode(this.digits(this.phone()), PURPOSE[mode]);
      this.showCodeStep(sent.resendAfterSeconds);
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'OTP_COOLDOWN') {
        // A code went out under a minute ago (a double tap, or a reload): let them use it
        this.notice.set(l().codeSentMoment);
        this.showCodeStep(60);
      }
      else if (problem.code === 'ACCOUNT_EXISTS') {
        this.switchTo('signIn', l().accountExists);
      }
      else if (problem.code === 'ACCOUNT_NOT_FOUND') {
        this.switchTo('signUp', l().noAccountYet);
      }
      else {
        this.error.set(problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  /** The code step: create the account, set the new password, or link Google. */
  async finish(): Promise<void> {
    const mode = this.mode();
    if (this.busy() || mode === 'signIn') {
      return;
    }
    const problem = this.check(mode);
    if (problem) {
      this.error.set(problem);
      return;
    }
    const phone = this.digits(this.phone());
    const name = this.fullName().trim();
    await this.run(() => {
      switch (mode) {
        case 'signUp':
          return this.session.signUp(phone, this.code(), name, this.password());
        case 'reset':
          return this.session.resetPassword(phone, this.code(), this.password());
        case 'google':
          return this.session.linkGoogle(this.googleToken!, phone, this.code(), name);
      }
    }, (code) => {
      if (code === 'FULL_NAME_REQUIRED') {
        this.needsName.set(true);
        queueMicrotask(() => document.getElementById('full-name')?.focus());
        return true;
      }
      return false;
    });
  }

  async google(idToken: string): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.googleToken = idToken;
    await this.run(() => this.session.google(idToken), (code) => {
      if (code === 'GOOGLE_PHONE_NEEDED') {
        this.switchTo('google');
        return true;
      }
      return false;
    });
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

  private check(mode: Exclude<Mode, 'signIn'>): string | null {
    if (!/^\d{6}$/.test(this.code())) {
      return l().enterCode;
    }
    if ((mode === 'signUp' || this.needsName()) && this.fullName().trim().length < 2) {
      return l().enterName;
    }
    if (mode !== 'google' && [...this.password()].length < MIN_PASSWORD) {
      return l().passwordTooShort(MIN_PASSWORD);
    }
    return null;
  }

  /** Signs in with {@code action}, then goes where the player was headed. {@code handled} deals with known codes. */
  private async run(action: () => Promise<unknown>, handled: (code: string | null) => boolean): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
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
