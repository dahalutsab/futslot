import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AddOnItem, AppNotification, Availability, Booking, BookingPayments, BookingRequest, ChatMessage, ConversationSummary,
  Deposit, DepositWithdrawal, Page, Payment, PaymentClaim, PaymentMethod, Quote, RatingSummary, Review, ReviewWrite, User,
  VenueDetail, VenueSummary, WaitlistEntry,
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

  // Favourites

  favourites(): Promise<VenueSummary[]> {
    return this.get<VenueSummary[]>('/api/v1/me/favourites');
  }

  favourite(venueId: string, on: boolean): Promise<void> {
    const url = `/api/v1/me/favourites/${venueId}`;
    return firstValueFrom(on ? this.http.put<void>(url, null) : this.http.delete<void>(url));
  }

  // Reviews

  /** Rejects with REVIEW_NOT_FOUND when the booking has no review yet. */
  bookingReview(bookingId: string): Promise<Review> {
    return this.get<Review>(`/api/v1/bookings/${bookingId}/review`);
  }

  writeReview(bookingId: string, review: ReviewWrite, update: boolean): Promise<Review> {
    const url = `/api/v1/bookings/${bookingId}/review`;
    return firstValueFrom(update ? this.http.put<Review>(url, review) : this.http.post<Review>(url, review));
  }

  deleteReview(bookingId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/v1/bookings/${bookingId}/review`));
  }

  myReviews(page = 0): Promise<Page<Review>> {
    return this.get<Page<Review>>('/api/v1/me/reviews', { page: String(page), size: '20' });
  }

  /** Played bookings still waiting for a review. */
  toReview(): Promise<Booking[]> {
    return this.get<Booking[]>('/api/v1/me/reviews/pending');
  }

  // Waitlist

  joinWaitlist(courtId: string, startsAt: string): Promise<WaitlistEntry> {
    return this.post<WaitlistEntry>('/api/v1/waitlist', { courtId, startsAt });
  }

  waitlist(): Promise<WaitlistEntry[]> {
    return this.get<WaitlistEntry[]>('/api/v1/me/waitlist');
  }

  leaveWaitlist(entryId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/v1/me/waitlist/${entryId}`));
  }

  // Notifications

  notifications(page = 0): Promise<Page<AppNotification>> {
    return this.get<Page<AppNotification>>('/api/v1/notifications', { page: String(page), size: '20' });
  }

  unreadNotifications(): Promise<number> {
    return this.get<{ unread: number }>('/api/v1/notifications/unread-count').then((r) => r.unread);
  }

  readNotification(id: string): Promise<AppNotification> {
    return this.post<AppNotification>(`/api/v1/notifications/${id}/read`, {});
  }

  readAllNotifications(): Promise<void> {
    return this.post<void>('/api/v1/notifications/read-all', {});
  }

  // Messages

  conversations(): Promise<ConversationSummary[]> {
    return this.get<ConversationSummary[]>('/api/v1/me/conversations');
  }

  /** Newest first, 50 at a time; {@code before} pages back through older ones. */
  messages(conversationId: string, before?: string): Promise<ChatMessage[]> {
    return this.get<ChatMessage[]>(`/api/v1/me/conversations/${conversationId}/messages`, before ? { before } : undefined);
  }

  reply(conversationId: string, body: string): Promise<ChatMessage> {
    return this.post<ChatMessage>(`/api/v1/me/conversations/${conversationId}/messages`, { body });
  }

  /** Starts (or continues) the conversation with a venue. */
  messageVenue(venueId: string, body: string, bookingId?: string): Promise<ChatMessage> {
    return this.post<ChatMessage>(`/api/v1/venues/${venueId}/messages`, { body, bookingId });
  }

  readConversation(conversationId: string): Promise<void> {
    return this.post<void>(`/api/v1/me/conversations/${conversationId}/read`, {});
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
