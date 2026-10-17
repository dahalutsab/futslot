import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { FeaturedListing, Invoice, InvoiceStatus, PaymentMethod, Plan, PlanRequest } from '../../core/api.types';
import { dayLabel, methodLabel, rupees, today } from '../../shared/format';
import { FEATURED_STATUS, INVOICE_STATUS, PLACEMENT } from '../../shared/labels';

type Tab = 'plans' | 'invoices' | 'featured';

const METHODS: PaymentMethod[] = ['BANK_TRANSFER', 'ESEWA', 'KHALTI', 'FONEPAY', 'CASH'];

interface PlanForm {
  code: string;
  name: string;
  description: string;
  monthlyPrice: number | null;
  yearlyPrice: number | null;
  maxCourts: number | null;
  maxStaff: number | null;
  features: string;
  active: boolean;
}

const BLANK: PlanForm = { code: '', name: '', description: '', monthlyPrice: null, yearlyPrice: null, maxCourts: null, maxStaff: null, features: '', active: true };

/**
 * What venues pay FutSlot: the subscription plans on sale, invoices (record a payment taken another way), and
 * featured listings. Payments venues report themselves are checked under Payments to check.
 */
@Component({
  selector: 'fs-billing',
  imports: [FormsModule, NgTemplateOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './billing.html',
})
export class Billing {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly fmt = { rupees, dayLabel, methodLabel, localDay: (instant: string) => today('Asia/Kathmandu', new Date(instant)) };

  protected readonly invoiceStatus = INVOICE_STATUS;

  protected readonly featuredStatus = FEATURED_STATUS;

  protected readonly placement = PLACEMENT;

  protected readonly methods = METHODS;

  protected readonly tabs: { value: Tab; label: string }[] = [
    { value: 'plans', label: 'Plans' },
    { value: 'invoices', label: 'Invoices' },
    { value: 'featured', label: 'Featured listings' },
  ];

  protected readonly tab = signal<Tab>((inject(ActivatedRoute).snapshot.queryParamMap.get('tab') as Tab) ?? 'plans');

  protected readonly plans = signal<Plan[] | null>(null);

  /** The plan being edited: its id, or 'new'. */
  protected readonly editing = signal<string | null>(null);

  protected readonly form = signal<PlanForm>(BLANK);

  protected readonly paid = signal(false);

  protected readonly invoices = signal<Invoice[] | null>(null);

  protected readonly featured = signal<FeaturedListing[] | null>(null);

  /** The invoice or listing whose payment is being recorded. */
  protected readonly recording = signal<string | null>(null);

  protected readonly method = signal<PaymentMethod>('BANK_TRANSFER');

  protected readonly transactionId = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const tab = this.tab();
      const paid = this.paid();
      untracked(() => void this.load(tab, paid));
    });
  }

  protected show(tab: Tab): void {
    this.tab.set(tab);
    this.recording.set(null);
    void this.router.navigate([], { queryParams: { tab }, replaceUrl: true });
  }

  protected edit(plan: Plan | null): void {
    this.error.set(null);
    this.editing.set(plan?.id ?? 'new');
    this.form.set(plan ? {
      code: plan.code, name: plan.name, description: plan.description ?? '', monthlyPrice: plan.monthlyPrice, yearlyPrice: plan.yearlyPrice,
      maxCourts: plan.maxCourts, maxStaff: plan.maxStaff, features: plan.features.join('\n'), active: plan.active,
    } : BLANK);
  }

  protected set<K extends keyof PlanForm>(key: K, value: PlanForm[K]): void {
    this.form.update((f) => ({ ...f, [key]: value }));
  }

  protected async savePlan(): Promise<void> {
    const f = this.form();
    const id = this.editing();
    if (!f.name.trim() || f.monthlyPrice === null || f.monthlyPrice < 0) {
      this.error.set('Give the plan a name and a monthly price.');
      return;
    }
    const plan: PlanRequest = {
      code: id === 'new' ? f.code.trim().toUpperCase() : undefined,
      name: f.name.trim(),
      description: f.description.trim() || undefined,
      monthlyPrice: f.monthlyPrice,
      yearlyPrice: f.yearlyPrice ?? undefined,
      maxCourts: f.maxCourts ?? undefined,
      maxStaff: f.maxStaff ?? undefined,
      features: f.features.split('\n').map((line) => line.trim()).filter(Boolean),
      active: f.active,
    };
    await this.act(async () => {
      await this.api.savePlan(plan, id === 'new' ? undefined : id ?? undefined);
      this.editing.set(null);
      this.plans.set(await this.api.plans());
    });
  }

  protected record(id: string): void {
    this.recording.set(id);
    this.method.set('BANK_TRANSFER');
    this.transactionId.set('');
    this.error.set(null);
  }

  protected async recordPayment(kind: 'invoice' | 'featured', id: string): Promise<void> {
    await this.act(async () => {
      if (kind === 'invoice') {
        await this.api.recordInvoicePayment(id, this.method(), this.transactionId().trim());
      } else {
        await this.api.recordFeaturedPayment(id, this.method(), this.transactionId().trim());
      }
      this.recording.set(null);
      await this.load(this.tab(), this.paid());
    });
  }

  private async act(action: () => Promise<void>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(tab: Tab, paid: boolean): Promise<void> {
    try {
      if (tab === 'plans') {
        this.plans.set(await this.api.plans());
      } else if (tab === 'invoices') {
        this.invoices.set(null);
        const statuses: InvoiceStatus[] = paid ? ['PAID'] : ['DUE', 'OVERDUE'];
        this.invoices.set(await this.api.invoices(statuses));
      } else {
        this.featured.set(await this.api.featured(['PENDING_PAYMENT', 'SCHEDULED', 'ACTIVE']));
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
