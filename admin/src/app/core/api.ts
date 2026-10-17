import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AuditEntry, FeaturedListing, FeaturedStatus, Invoice, InvoiceStatus, Page, Payment, PaymentMethod, Payout, Plan,
  PlanRequest, PlatformReport, Review, ReviewStatus, VenueDetail, VenueStatus, VenueSummary, Withdrawal,
} from './api.types';

/** Every API call the admin app makes; all of them need an admin account. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // Venues

  venues(status: VenueStatus, page = 0): Promise<Page<VenueSummary>> {
    return this.get<Page<VenueSummary>>('/api/v1/admin/venues', { status, page: String(page), size: '50' });
  }

  venue(id: string): Promise<VenueDetail> {
    return this.get<VenueDetail>(`/api/v1/admin/venues/${id}`);
  }

  approve(id: string): Promise<VenueDetail> {
    return this.post<VenueDetail>(`/api/v1/admin/venues/${id}/approve`, {});
  }

  /** {@code action}: reject a venue waiting for approval, or suspend a live one. */
  refuse(id: string, action: 'reject' | 'suspend', reason: string): Promise<VenueDetail> {
    return this.post<VenueDetail>(`/api/v1/admin/venues/${id}/${action}`, { reason });
  }

  reinstate(id: string): Promise<VenueDetail> {
    return this.post<VenueDetail>(`/api/v1/admin/venues/${id}/reinstate`, {});
  }

  // Payments to FutSlot: deposit top-ups, invoices, featured listings

  pendingPayments(): Promise<Payment[]> {
    return this.get<Payment[]>('/api/v1/admin/payments/pending');
  }

  approvePayment(id: string): Promise<Payment> {
    return this.post<Payment>(`/api/v1/admin/payments/${id}/approve`, {});
  }

  rejectPayment(id: string, reason: string): Promise<Payment> {
    return this.post<Payment>(`/api/v1/admin/payments/${id}/reject`, { reason });
  }

  // Deposits

  withdrawals(status: Withdrawal['status']): Promise<Withdrawal[]> {
    return this.get<Withdrawal[]>('/api/v1/admin/deposit-withdrawals', { status });
  }

  withdrawalPaid(id: string, reference: string): Promise<Withdrawal> {
    return this.post<Withdrawal>(`/api/v1/admin/deposit-withdrawals/${id}/paid`, { reference });
  }

  /** Payouts not sent yet. */
  payouts(): Promise<Payout[]> {
    return this.get<Payout[]>('/api/v1/admin/payouts');
  }

  payoutPaid(id: string, method: PaymentMethod, reference: string): Promise<Payout> {
    return this.post<Payout>(`/api/v1/admin/payouts/${id}/paid`, { method, reference });
  }

  // Reviews

  reviews(status: ReviewStatus, page = 0): Promise<Page<Review>> {
    return this.get<Page<Review>>('/api/v1/admin/reviews', { status, page: String(page), size: '50' });
  }

  moderate(id: string, status: ReviewStatus): Promise<Review> {
    return this.post<Review>(`/api/v1/admin/reviews/${id}/status`, { status });
  }

  // FutSlot billing

  plans(): Promise<Plan[]> {
    return this.get<Plan[]>('/api/v1/admin/subscription-plans');
  }

  savePlan(plan: PlanRequest, id?: string): Promise<Plan> {
    return id ? firstValueFrom(this.http.put<Plan>(`/api/v1/admin/subscription-plans/${id}`, plan))
      : this.post<Plan>('/api/v1/admin/subscription-plans', plan);
  }

  invoices(statuses: InvoiceStatus[]): Promise<Invoice[]> {
    return this.get<Invoice[]>('/api/v1/admin/invoices', { status: statuses.join(',') });
  }

  /** Money taken another way (cash, a transfer FutSlot found itself): the invoice is paid in full. */
  recordInvoicePayment(id: string, method: PaymentMethod, transactionId: string): Promise<Payment> {
    return this.post<Payment>(`/api/v1/admin/invoices/${id}/payments`, { method, transactionId: transactionId || undefined });
  }

  featured(statuses: FeaturedStatus[]): Promise<FeaturedListing[]> {
    return this.get<FeaturedListing[]>('/api/v1/admin/featured-listings', { status: statuses.join(',') });
  }

  recordFeaturedPayment(id: string, method: PaymentMethod, transactionId: string): Promise<Payment> {
    return this.post<Payment>(`/api/v1/admin/featured-listings/${id}/payments`, { method, transactionId: transactionId || undefined });
  }

  // Reports and the audit log

  report(from?: string, to?: string): Promise<PlatformReport> {
    const params: Record<string, string> = {};
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    return this.get<PlatformReport>('/api/v1/admin/reports/summary', params);
  }

  audit(venueId?: string, page = 0): Promise<Page<AuditEntry>> {
    const params: Record<string, string> = { page: String(page), size: '50' };
    if (venueId) {
      params['venueId'] = venueId;
    }
    return this.get<Page<AuditEntry>>('/api/v1/admin/audit-log', params);
  }

  private get<T>(url: string, params?: Record<string, string>): Promise<T> {
    return firstValueFrom(this.http.get<T>(url, { params }));
  }

  private post<T>(url: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(url, body));
  }
}
