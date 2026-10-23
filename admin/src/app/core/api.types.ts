/** Shapes returned by the FutSlot API (backend: docs/api/openapi.yaml), as FutSlot admins see them. */

export type Role = 'PLAYER' | 'OWNER' | 'STAFF' | 'ADMIN';

export interface User {
  id: string;
  fullName: string;
  phone: string;
  email: string | null;
  avatarUrl: string | null;
  preferredLanguage: string;
  roles: Role[];
  /** False for accounts made before passwords, or with Google, until one is set. */
  passwordSet: boolean;
  googleLinked: boolean;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  refreshToken: string;
  user: User;
}

/** Why a code is texted: proving the number for a new account or a Google link, or resetting a password. */
export type CodePurpose = 'SIGN_UP' | 'RESET_PASSWORD' | 'LINK_GOOGLE';

export interface OtpSent {
  expiresInSeconds: number;
  resendAfterSeconds: number;
  /** The code itself, only in a demo without SMS. */
  code: string | null;
}

/** RFC 7807 problem body; {@code code} is stable and machine-readable. */
export interface ApiProblem {
  status: number;
  detail: string;
  code?: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type PaymentMethod = 'CASH' | 'ESEWA' | 'KHALTI' | 'FONEPAY' | 'BANK_TRANSFER';

// Venues

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

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** Times are "HH:mm:ss"; a close at or before the open means open past midnight. */
export interface DayHours {
  day: Weekday;
  openTime: string | null;
  closeTime: string | null;
  closed: boolean;
}

export interface CancellationTier {
  withinHours: number;
  feePercent: number;
}

export interface VenuePolicy {
  advancePercent: number;
  bookingHoldMinutes: number;
  cancellationTiers: CancellationTier[];
  maxAdvanceBookingDays: number;
  strikeLimit: number | null;
  strikeAdvancePercent: number | null;
  depositRequired: boolean;
}

export interface PricingRule {
  id: string;
  label: 'STANDARD' | 'PEAK' | 'OFF_PEAK' | 'WEEKEND' | 'HOLIDAY';
  dayOfWeek: Weekday | null;
  startTime: string;
  endTime: string;
  price: number;
  priority: number;
  active: boolean;
}

export interface Court {
  id: string;
  name: string;
  format: 'FIVE_A_SIDE' | 'SIX_A_SIDE' | 'SEVEN_A_SIDE';
  surface: 'ARTIFICIAL_TURF' | 'HARDWOOD' | 'CONCRETE' | 'RUBBER';
  indoor: boolean;
  slotDurationMinutes: number;
  status: 'ACTIVE' | 'INACTIVE';
  sortOrder: number;
  pricingRules: PricingRule[];
}

export interface VenueDetail {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  phone: string | null;
  email: string | null;
  addressLine: string;
  area: string | null;
  city: string;
  latitude: number | null;
  longitude: number | null;
  timezone: string;
  status: VenueStatus;
  rejectionReason: string | null;
  policy: VenuePolicy;
  paymentInstructions: string | null;
  averageRating: number;
  reviewCount: number;
  fromPrice: number | null;
  operatingHours: DayHours[];
  amenities: { code: string; name: string; icon: string }[];
  courts: Court[];
  photos: { id: string; url: string; caption: string | null; sortOrder: number; cover: boolean }[];
}

// Payments to FutSlot

export type PlatformPurpose = 'SUBSCRIPTION' | 'FEATURED_LISTING' | 'DEPOSIT';

/** A payment a venue or player reported sending FutSlot. {@code customerName} is the venue, or the player for a deposit. */
export interface Payment {
  id: string;
  purpose: PlatformPurpose;
  subscriptionInvoiceId: string | null;
  invoiceNumber: string | null;
  featuredListingId: string | null;
  customerName: string | null;
  amount: number;
  method: PaymentMethod;
  status: string;
  transactionId: string | null;
  failureReason: string | null;
  paidAt: string | null;
  createdAt: string;
}

// Deposits

/** A player's deposit going back; {@code dueAt} is when FutSlot may send it. */
export interface Withdrawal {
  id: string;
  amount: number;
  method: PaymentMethod;
  sendTo: string;
  status: 'REQUESTED' | 'PAID' | 'CANCELLED';
  dueAt: string;
  reference: string | null;
  paidAt: string | null;
  createdAt: string;
  playerName: string | null;
  playerPhone: string | null;
}

/** A month of deposit fees owed to a venue; {@code paymentInstructions} is the venue's wallet or account. */
export interface Payout {
  id: string;
  venueId: string;
  venueName: string;
  periodStart: string;
  periodEnd: string;
  amount: number;
  status: 'PENDING' | 'PROCESSING' | 'PAID' | 'FAILED';
  method: PaymentMethod | null;
  reference: string | null;
  paidAt: string | null;
  paymentInstructions: string | null;
}

// Reviews

export type ReviewStatus = 'PUBLISHED' | 'HIDDEN' | 'REMOVED';

export interface Review {
  id: string;
  venueId: string;
  venueName: string;
  bookingId: string | null;
  playedOn: string;
  rating: number;
  comment: string | null;
  reviewerName: string;
  reviewerAvatarUrl: string | null;
  ownerReply: string | null;
  ownerRepliedAt: string | null;
  status: ReviewStatus;
  createdAt: string;
}

// FutSlot billing

export type BillingCycle = 'MONTHLY' | 'YEARLY';

export interface Plan {
  id: string;
  code: string;
  name: string;
  description: string | null;
  monthlyPrice: number;
  yearlyPrice: number | null;
  maxCourts: number | null;
  maxStaff: number | null;
  features: string[];
  active: boolean;
}

/** {@code code} is set once; an empty {@code yearlyPrice} means monthly only; empty limits mean unlimited. */
export interface PlanRequest {
  code?: string;
  name: string;
  description?: string;
  monthlyPrice: number;
  yearlyPrice?: number;
  maxCourts?: number;
  maxStaff?: number;
  features?: string[];
  active?: boolean;
}

export type InvoiceStatus = 'DUE' | 'PAID' | 'OVERDUE' | 'VOID';

/** {@code paymentPending}: the venue sent a payment FutSlot hasn't checked yet. */
export interface Invoice {
  id: string;
  invoiceNumber: string;
  venueId: string;
  venueName: string | null;
  planCode: string | null;
  planName: string | null;
  billingCycle: BillingCycle | null;
  amount: number;
  periodStart: string;
  periodEnd: string;
  dueOn: string;
  status: InvoiceStatus;
  paidAt: string | null;
  paymentPending: boolean;
}

export type FeaturedPlacement = 'HOME_CAROUSEL' | 'SEARCH_TOP' | 'CITY_TOP';

export type FeaturedStatus = 'PENDING_PAYMENT' | 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'CANCELLED';

export interface FeaturedListing {
  id: string;
  venueId: string;
  venueName: string;
  placement: FeaturedPlacement;
  city: string | null;
  startsAt: string;
  endsAt: string;
  price: number;
  status: FeaturedStatus;
  paymentInstructions: string | null;
  paymentPending: boolean;
}

// Reports and the audit log

/** {@code venuesByStatus} is the current count, not the period's. */
export interface PlatformReport {
  from: string;
  to: string;
  venuesByStatus: Partial<Record<VenueStatus, number>>;
  newVenues: number;
  newUsers: number;
  bookings: number;
  playedBookings: number;
  bookedValue: number;
  venuesWithBookings: number;
  topVenues: { venueId: string; name: string; city: string; bookings: number; bookedValue: number }[];
}

export interface AuditEntry {
  id: string;
  actorId: string | null;
  actorName: string | null;
  venueId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  changes: Record<string, string>;
  ipAddress: string | null;
  createdAt: string;
}
