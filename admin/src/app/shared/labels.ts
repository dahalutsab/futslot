import { FeaturedPlacement, FeaturedStatus, InvoiceStatus, Payment, ReviewStatus, VenueStatus } from '../core/api.types';

/** How each API value reads to an admin. */

export const VENUE_STATUS: Record<VenueStatus, string> = {
  PENDING_APPROVAL: 'Waiting for approval', ACTIVE: 'Live', SUSPENDED: 'Suspended', REJECTED: 'Not approved', DRAFT: 'Draft',
};

export const REVIEW_STATUS: Record<ReviewStatus, string> = { PUBLISHED: 'Published', HIDDEN: 'Hidden', REMOVED: 'Removed' };

export const INVOICE_STATUS: Record<InvoiceStatus, string> = { DUE: 'Due', OVERDUE: 'Overdue', PAID: 'Paid', VOID: 'Void' };

export const PLACEMENT: Record<FeaturedPlacement, string> = {
  HOME_CAROUSEL: 'Home carousel', SEARCH_TOP: 'Top of search', CITY_TOP: 'Top of city',
};

export const FEATURED_STATUS: Record<FeaturedStatus, string> = {
  PENDING_PAYMENT: 'Waiting for payment', SCHEDULED: 'Scheduled', ACTIVE: 'Live', ENDED: 'Ended', CANCELLED: 'Cancelled',
};

/** "Booking deposit", "Invoice INV-2026-0042", "Featured listing". */
export function paymentFor(payment: Payment): string {
  switch (payment.purpose) {
    case 'DEPOSIT':
      return 'Booking deposit top-up';
    case 'SUBSCRIPTION':
      return `Invoice ${payment.invoiceNumber ?? ''}`.trim();
    case 'FEATURED_LISTING':
      return 'Featured listing';
  }
}

/** "AUDIT_ACTION_NAME" → "Audit action name". */
export function actionLabel(action: string): string {
  const words = action.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
