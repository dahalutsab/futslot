import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AddOnItem, Availability, Booking, BookingPayments, BookingRequest, Deposit, DepositWithdrawal, Page, Payment,
  PaymentClaim, PaymentMethod, Quote, RatingSummary, Review, User, VenueDetail, VenueSummary,
} from './api.types';

/** Every API call the player app makes. Calls under /venues work signed out too. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // Venues

  venues(filter: { q?: string; city?: string; page?: number }): Promise<Page<VenueSummary>> {
    const params: Record<string, string> = { size: '20', page: String(filter.page ?? 0) };
    if (filter.q) {
      params['q'] = filter.q;
    }
    if (filter.city) {
      params['city'] = filter.city;
    }
    return this.get<Page<VenueSummary>>('/api/v1/venues', params);
  }

  featured(city?: string): Promise<VenueSummary[]> {
    return this.get<VenueSummary[]>('/api/v1/featured-venues', city ? { placement: 'HOME_CAROUSEL', city } : { placement: 'HOME_CAROUSEL' });
  }

  venue(slug: string): Promise<VenueDetail> {
    return this.get<VenueDetail>(`/api/v1/venues/${slug}`);
  }

  availability(slug: string, date: string): Promise<Availability> {
    return this.get<Availability>(`/api/v1/venues/${slug}/availability`, { date });
  }

  addOns(slug: string): Promise<AddOnItem[]> {
    return this.get<AddOnItem[]>(`/api/v1/venues/${slug}/add-ons`);
  }

  reviews(slug: string, page = 0): Promise<Page<Review>> {
    return this.get<Page<Review>>(`/api/v1/venues/${slug}/reviews`, { page: String(page), size: '10' });
  }

  ratingSummary(slug: string): Promise<RatingSummary> {
    return this.get<RatingSummary>(`/api/v1/venues/${slug}/reviews/summary`);
  }

  // Bookings

  quote(request: BookingRequest): Promise<Quote> {
    return this.post<Quote>('/api/v1/bookings/quote', request);
  }

  book(request: BookingRequest): Promise<Booking> {
    return this.post<Booking>('/api/v1/bookings', request);
  }

  bookings(scope: 'upcoming' | 'past', page = 0): Promise<Booking[]> {
    return this.get<Booking[]>('/api/v1/bookings', { scope, page: String(page), size: '20' });
  }

  booking(id: string): Promise<Booking> {
    return this.get<Booking>(`/api/v1/bookings/${id}`);
  }

  cancel(id: string, reason: string): Promise<Booking> {
    return this.post<Booking>(`/api/v1/bookings/${id}/cancel`, { reason: reason || undefined });
  }

  bookingPayments(id: string): Promise<BookingPayments> {
    return this.get<BookingPayments>(`/api/v1/bookings/${id}/payments`);
  }

  payBooking(id: string, claim: PaymentClaim): Promise<Payment> {
    return this.post<Payment>(`/api/v1/bookings/${id}/payments`, claim);
  }

  // Deposit

  deposit(): Promise<Deposit> {
    return this.get<Deposit>('/api/v1/me/deposit');
  }

  topUp(claim: PaymentClaim): Promise<Payment> {
    return this.post<Payment>('/api/v1/me/deposit/top-ups', claim);
  }

  withdraw(method: PaymentMethod, sendTo: string): Promise<DepositWithdrawal> {
    return this.post<DepositWithdrawal>('/api/v1/me/deposit/withdrawals', { method, sendTo });
  }

  keepDeposit(withdrawalId: string): Promise<DepositWithdrawal> {
    return this.post<DepositWithdrawal>(`/api/v1/me/deposit/withdrawals/${withdrawalId}/cancel`, {});
  }

  // Account

  updateMe(change: { fullName?: string; email?: string; preferredLanguage?: 'en' | 'ne' }): Promise<User> {
    return firstValueFrom(this.http.patch<User>('/api/v1/me', change));
  }

  private get<T>(url: string, params?: Record<string, string>): Promise<T> {
    return firstValueFrom(this.http.get<T>(url, { params }));
  }

  private post<T>(url: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(url, body));
  }
}
