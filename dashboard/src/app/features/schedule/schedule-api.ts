import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  Availability,
  BlockReason,
  Booking,
  BookingSource,
  CourtBlock,
  Payment,
  PaymentMethod,
} from '../../core/api.types';
import { addDays } from '../../shared/format';

export interface WalkIn {
  courtId: string;
  startsAt: string;
  customerName: string;
  customerPhone: string;
  source: Extract<BookingSource, 'WALK_IN' | 'PHONE'>;
  notes?: string;
}

/** The calls the Schedule screen makes, for the selected venue. */
@Injectable({ providedIn: 'root' })
export class ScheduleApi {
  private readonly http = inject(HttpClient);

  availability(venueId: string, date: string): Promise<Availability> {
    return firstValueFrom(this.http.get<Availability>(`/api/v1/owner/venues/${venueId}/availability`, { params: { date } }));
  }

  bookings(venueId: string, date: string): Promise<Booking[]> {
    return firstValueFrom(this.http.get<Booking[]>(`/api/v1/owner/venues/${venueId}/bookings`, { params: { date } }));
  }

  /** Blocks around the day, generously (late slots run past midnight); the schedule keeps what's in view. */
  async blocks(venueId: string, courtIds: string[], date: string): Promise<CourtBlock[]> {
    const from = `${addDays(date, -1)}T12:00:00Z`;
    const to = `${addDays(date, 1)}T18:00:00Z`;
    const lists = await Promise.all(courtIds.map((courtId) => firstValueFrom(
      this.http.get<CourtBlock[]>(`/api/v1/owner/venues/${venueId}/courts/${courtId}/blocks`, { params: { from, to } }))));
    return lists.flat();
  }

  book(venueId: string, walkIn: WalkIn): Promise<Booking> {
    return firstValueFrom(this.http.post<Booking>(`/api/v1/owner/venues/${venueId}/bookings`, walkIn));
  }

  block(venueId: string, courtId: string, startsAt: string, endsAt: string, reason: BlockReason, note?: string)
      : Promise<CourtBlock> {
    return firstValueFrom(this.http.post<CourtBlock>(`/api/v1/owner/venues/${venueId}/courts/${courtId}/blocks`,
      { startsAt, endsAt, reason, note }));
  }

  unblock(venueId: string, courtId: string, blockId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/v1/owner/venues/${venueId}/courts/${courtId}/blocks/${blockId}`));
  }

  takePayment(venueId: string, bookingId: string, amount: number, method: PaymentMethod, transactionId?: string)
      : Promise<Payment> {
    return firstValueFrom(this.http.post<Payment>(`/api/v1/owner/venues/${venueId}/bookings/${bookingId}/payments`,
      { amount, method, transactionId: transactionId || undefined }));
  }

  pendingPayments(venueId: string): Promise<Payment[]> {
    return firstValueFrom(this.http.get<Payment[]>(`/api/v1/owner/venues/${venueId}/payments/pending`));
  }

  approve(venueId: string, paymentId: string): Promise<Payment> {
    return firstValueFrom(this.http.post<Payment>(`/api/v1/owner/venues/${venueId}/payments/${paymentId}/approve`, {}));
  }

  reject(venueId: string, paymentId: string, reason: string): Promise<Payment> {
    return firstValueFrom(this.http.post<Payment>(`/api/v1/owner/venues/${venueId}/payments/${paymentId}/reject`,
      { reason }));
  }

  played(venueId: string, bookingId: string): Promise<Booking> {
    return firstValueFrom(this.http.post<Booking>(`/api/v1/owner/venues/${venueId}/bookings/${bookingId}/complete`, {}));
  }

  noShow(venueId: string, bookingId: string): Promise<Booking> {
    return firstValueFrom(this.http.post<Booking>(`/api/v1/owner/venues/${venueId}/bookings/${bookingId}/no-show`, {}));
  }

  cancel(venueId: string, bookingId: string, reason: string): Promise<Booking> {
    return firstValueFrom(this.http.post<Booking>(`/api/v1/owner/venues/${venueId}/bookings/${bookingId}/cancel`,
      { reason: reason || undefined }));
  }
}
