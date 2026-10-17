import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LoaderCircle, LucideAngularModule, ShieldCheck } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Deposit, DEPOSIT_AMOUNT, DepositEntry, PaymentClaim, PaymentMethod } from '../../core/api.types';
import { ago, dayLabel, methodLabel, rupees, today } from '../../shared/format';
import { PayForm } from '../../shared/pay-form';

/** Where FutSlot can send a deposit back. */
const SEND_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'ESEWA', label: 'eSewa' },
  { value: 'KHALTI', label: 'Khalti' },
  { value: 'BANK_TRANSFER', label: 'Bank account' },
];

/**
 * The player's FutSlot booking deposit: how much is in it, topping it up, taking it back, and every change.
 * Some venues only take bookings from players whose deposit is full.
 */
@Component({
  selector: 'fs-deposit',
  imports: [FormsModule, LucideAngularModule, PayForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './deposit.html',
})
export class DepositPage {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  /** Where the player was booking, to go back once the deposit is full. */
  readonly back = input<string | undefined>();

  protected readonly icons = { LoaderCircle, ShieldCheck };

  protected readonly fmt = { rupees, methodLabel, ago, dayLabel, localDay: (instant: string) => today('Asia/Kathmandu', new Date(instant)) };

  protected readonly depositAmount = DEPOSIT_AMOUNT;

  protected readonly sendMethods = SEND_METHODS;

  protected readonly deposit = signal<Deposit | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly topUpError = signal<string | null>(null);

  protected readonly toppingUp = signal(false);

  protected readonly withdrawOpen = signal(false);

  protected readonly sendMethod = signal<PaymentMethod>('ESEWA');

  protected readonly sendTo = signal('');

  protected readonly withdrawing = signal(false);

  protected readonly withdrawError = signal<string | null>(null);

  protected readonly full = computed(() => this.deposit()?.toAdd === 0);

  protected readonly backLink = computed(() => {
    const back = this.back();
    return back && back.startsWith('/') && !back.startsWith('//') ? back : null;
  });

  constructor() {
    void this.load();
  }

  protected goBack(link: string): void {
    void this.router.navigateByUrl(link);
  }

  protected entryLabel(entry: DepositEntry): string {
    switch (entry.type) {
      case 'TOP_UP':
        return 'Topped up';
      case 'FEE':
        return `Fee to ${entry.venueName} · ${entry.bookingReference}`;
      case 'WITHDRAWAL':
        return 'Taken back';
      case 'WITHDRAWAL_CANCELLED':
        return 'Kept after all';
    }
  }

  protected async topUp(claim: PaymentClaim): Promise<void> {
    if (this.toppingUp()) {
      return;
    }
    this.toppingUp.set(true);
    this.topUpError.set(null);
    try {
      await this.api.topUp(claim);
      await this.load();
    }
    catch (error) {
      this.topUpError.set(problemOf(error).message);
    }
    finally {
      this.toppingUp.set(false);
    }
  }

  protected async withdraw(): Promise<void> {
    if (this.withdrawing()) {
      return;
    }
    const sendTo = this.sendTo().trim();
    if (sendTo.length < 5) {
      this.withdrawError.set('Enter the number or account to send it to.');
      return;
    }
    this.withdrawing.set(true);
    this.withdrawError.set(null);
    try {
      await this.api.withdraw(this.sendMethod(), sendTo);
      this.withdrawOpen.set(false);
      await this.load();
    }
    catch (error) {
      this.withdrawError.set(problemOf(error).message);
    }
    finally {
      this.withdrawing.set(false);
    }
  }

  protected async keep(): Promise<void> {
    const withdrawal = this.deposit()?.withdrawal;
    if (!withdrawal || this.withdrawing()) {
      return;
    }
    this.withdrawing.set(true);
    try {
      await this.api.keepDeposit(withdrawal.id);
      await this.load();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.withdrawing.set(false);
    }
  }

  protected async load(): Promise<void> {
    try {
      this.deposit.set(await this.api.deposit());
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
