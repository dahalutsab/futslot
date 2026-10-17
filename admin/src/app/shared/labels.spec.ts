import { Payment } from '../core/api.types';
import { actionLabel, paymentFor } from './labels';

function payment(purpose: Payment['purpose'], invoiceNumber: string | null = null): Payment {
  return { id: 'p', purpose, subscriptionInvoiceId: null, invoiceNumber, featuredListingId: null, customerName: 'X', amount: 1000,
    method: 'ESEWA', status: 'PENDING', transactionId: 'T', failureReason: null, paidAt: null, createdAt: '2026-10-17T00:00:00Z' };
}

describe('labels', () => {
  it('says what a payment to FutSlot is for', () => {
    expect(paymentFor(payment('DEPOSIT'))).toBe('Booking deposit top-up');
    expect(paymentFor(payment('SUBSCRIPTION', 'INV-2026-0042'))).toBe('Invoice INV-2026-0042');
    expect(paymentFor(payment('FEATURED_LISTING'))).toBe('Featured listing');
  });

  it('turns audit actions into words', () => {
    expect(actionLabel('BOOKING_NO_SHOW')).toBe('Booking no show');
  });
});
