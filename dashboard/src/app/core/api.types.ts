/** Shapes returned by the FutSlot API (backend: docs/api/openapi.yaml). */

export type Role = 'PLAYER' | 'OWNER' | 'STAFF' | 'ADMIN';

export type VenuePermission =
  | 'MANAGE_VENUE'
  | 'MANAGE_BOOKINGS'
  | 'MANAGE_BLOCKS'
  | 'VIEW_CUSTOMERS'
  | 'VIEW_REPORTS'
  | 'MANAGE_STAFF';

export interface User {
  id: string;
  fullName: string;
  phone: string;
  email: string | null;
  avatarUrl: string | null;
  preferredLanguage: string;
  roles: Role[];
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  refreshToken: string;
  user: User;
}

export interface OtpSent {
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

export type VenueStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';

export interface VenueSummary {
  id: string;
  slug: string;
  name: string;
  area: string | null;
  city: string;
  status: VenueStatus;
  averageRating: number;
  reviewCount: number;
  fromPrice: number | null;
  courtCount: number;
  amenities: string[];
  coverPhotoUrl: string | null;
}

/** A venue the signed-in user runs or works at; {@code access} is OWNER, MANAGER or RECEPTIONIST. */
export interface MyVenue {
  venue: VenueSummary;
  access: 'OWNER' | 'MANAGER' | 'RECEPTIONIST';
  permissions: VenuePermission[];
}

/** RFC 7807 problem body; {@code code} is stable and machine-readable. */
export interface ApiProblem {
  status: number;
  detail: string;
  code?: string;
}

// Schedule

export type SlotStatus = 'AVAILABLE' | 'BOOKED' | 'BLOCKED' | 'PAST';

export interface Slot {
  /** Venue-local, with offset, e.g. 2026-10-09T18:00:00+05:45. */
  startsAt: string;
  endsAt: string;
  price: number;
  label: string | null;
  status: SlotStatus;
}

export interface CourtSlots {
  courtId: string;
  name: string;
  format: string;
  slotDurationMinutes: number;
  slots: Slot[];
}

export interface Availability {
  venueId: string;
  date: string;
  timezone: string;
  courts: CourtSlots[];
}

export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW' | 'EXPIRED';

export type BookingSource = 'APP' | 'WALK_IN' | 'PHONE' | 'RECURRING' | 'TOURNAMENT' | 'ADMIN';

export type PaymentStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export interface Booking {
  id: string;
  reference: string;
  venue: { id: string; name: string; slug: string };
  court: { id: string; name: string };
  customer: { id: string; name: string; phone: string };
  source: BookingSource;
  status: BookingStatus;
  startsAt: string;
  endsAt: string;
  priceAmount: number;
  addOnsAmount: number;
  addOns: { itemId: string; name: string; quantity: number; unitPrice: number }[];
  discountAmount: number;
  membershipId: string | null;
  discountCodeId: string | null;
  totalAmount: number;
  advanceRequired: number;
  paidAmount: number;
  paymentStatus: PaymentStatus;
  holdExpiresAt: string | null;
  notes: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdAt: string;
}

export type BlockReason = 'MAINTENANCE' | 'PRIVATE_EVENT' | 'HOLIDAY' | 'TOURNAMENT' | 'OTHER';

export interface CourtBlock {
  id: string;
  courtId: string;
  /** Instants (UTC). */
  startsAt: string;
  endsAt: string;
  reason: BlockReason;
  note: string | null;
}

export type PaymentMethod = 'CASH' | 'ESEWA' | 'KHALTI' | 'FONEPAY' | 'BANK_TRANSFER';

export interface Payment {
  id: string;
  purpose: 'BOOKING' | 'MEMBERSHIP' | 'TOURNAMENT_ENTRY' | 'SUBSCRIPTION' | 'FEATURED_LISTING';
  bookingId: string | null;
  bookingReference: string | null;
  membershipId: string | null;
  membershipPlanName: string | null;
  tournamentRegistrationId: string | null;
  tournamentName: string | null;
  customerName: string | null;
  amount: number;
  method: PaymentMethod;
  status: string;
  transactionId: string | null;
  failureReason: string | null;
  paidAt: string | null;
  createdAt: string;
}

export type RefundStatus = 'REQUESTED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'REJECTED';

/** Money owed back to a player, and how they originally paid (so it can go back the same way). */
export interface Refund {
  id: string;
  bookingId: string;
  bookingReference: string;
  customerName: string;
  customerPhone: string;
  amount: number;
  originalMethod: PaymentMethod;
  reason: string;
  status: RefundStatus;
  reference: string | null;
  createdAt: string;
  processedAt: string | null;
}

export interface AddOnItem {
  id: string;
  name: string;
  category: 'EQUIPMENT_RENTAL' | 'FOOD_DRINK' | 'OTHER';
  price: number;
  active: boolean;
}
