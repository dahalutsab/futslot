import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Payment, Refund, RefundStatus } from '../../core/api.types';

@Injectable({ providedIn: 'root' })
export class PaymentsApi {
  private readonly http = inject(HttpClient);

  pending(venueId: string): Promise<Payment[]> {
    return firstValueFrom(this.http.get<Payment[]>(`/api/v1/owner/venues/${venueId}/payments/pending`));
  }

  approve(venueId: string, paymentId: string): Promise<Payment> {
    return firstValueFrom(this.http.post<Payment>(`/api/v1/owner/venues/${venueId}/payments/${paymentId}/approve`, {}));
  }

  reject(venueId: string, paymentId: string, reason: string): Promise<Payment> {
    return firstValueFrom(this.http.post<Payment>(`/api/v1/owner/venues/${venueId}/payments/${paymentId}/reject`,
      { reason }));
  }

  refunds(venueId: string, status: RefundStatus): Promise<Refund[]> {
    return firstValueFrom(this.http.get<Refund[]>(`/api/v1/owner/venues/${venueId}/refunds`, { params: { status } }));
  }

  refunded(venueId: string, refundId: string, reference: string): Promise<Refund> {
    return firstValueFrom(this.http.post<Refund>(`/api/v1/owner/venues/${venueId}/refunds/${refundId}/complete`,
      { reference: reference || undefined }));
  }
}
