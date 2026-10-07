import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ArrowRight, LoaderCircle, LucideAngularModule, ShieldCheck } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';

type Step = 'phone' | 'code';

/** Sign in with a phone number and the 6-digit code sent to it. New numbers are asked for a name. */
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

  protected readonly icons = { ArrowRight, LoaderCircle, ShieldCheck };

  protected readonly step = signal<Step>('phone');

  protected readonly phone = signal('');

  protected readonly code = signal('');

  protected readonly fullName = signal('');

  protected readonly needsName = signal(false);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly resendIn = signal(0);

  protected readonly expired = this.route.snapshot.queryParamMap.has('expired');

  protected readonly phoneValid = computed(() => /^\d{10}$/.test(this.digits(this.phone())));

  protected readonly codeValid = computed(() => /^\d{6}$/.test(this.code()) && (!this.needsName() || this.fullName().trim().length >= 2));

  private timer: ReturnType<typeof setInterval> | null = null;

  // Buttons stay enabled and check on submit: a disabled submit button makes the browser ignore Enter, which
  // fast typing or autofill can hit before the button re-enables.
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
      const sent = await this.session.requestCode(this.digits(this.phone()));
      this.step.set('code');
      this.startCountdown(sent.resendAfterSeconds);
      queueMicrotask(() => document.getElementById('code')?.focus());
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  async verify(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.codeValid()) {
      this.error.set(this.needsName() && this.fullName().trim().length < 2 ? 'Enter your name.'
        : 'Enter the 6-digit code we sent.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.session.verify(this.digits(this.phone()), this.code(), this.fullName().trim());
      const next = this.route.snapshot.queryParamMap.get('next');
      await this.router.navigateByUrl(next && next.startsWith('/') && !next.startsWith('//') ? next : '/');
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'FULL_NAME_REQUIRED') {
        this.needsName.set(true);
        this.error.set(null);
        queueMicrotask(() => document.getElementById('full-name')?.focus());
      }
      else {
        this.error.set(problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  changeNumber(): void {
    this.step.set('phone');
    this.code.set('');
    this.needsName.set(false);
    this.error.set(null);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
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
