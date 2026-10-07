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

export type BookingSource = 'APP' | 'WEBSITE' | 'WALK_IN' | 'PHONE' | 'RECURRING' | 'TOURNAMENT' | 'ADMIN';

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

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** {@code hasApp}: linked to a FutSlot account, so they get notifications. */
export interface CustomerSummary {
  id: string;
  name: string;
  phone: string;
  hasApp: boolean;
  totalBookings: number;
  lastBookingAt: string | null;
}

export interface CustomerDetail {
  id: string;
  name: string;
  phone: string;
  hasApp: boolean;
  notes: string | null;
  customerSince: string;
  lastBookingAt: string | null;
  stats: { total: number; upcoming: number; completed: number; cancelled: number; noShows: number; completedValue: number };
  recentBookings: Booking[];
}

// Reports

export interface VenueReport {
  from: string;
  to: string;
  bookings: {
    total: number; played: number; pendingPayment: number; confirmed: number; completed: number; noShow: number;
    cancelled: number; expired: number; bySource: Record<string, number>; cancellationRate: number | null;
    noShowRate: number | null;
  };
  money: {
    bookedValue: number; discounts: number; outstanding: number; collected: number; refunded: number; net: number;
    collectedByMethod: Record<string, number>; membershipSales: number; refundsOwed: number;
  };
  occupancy: { openHours: number; bookedHours: number; rate: number | null };
  courts: { courtId: string; name: string; active: boolean; bookings: number; bookedHours: number; bookedValue: number;
    occupancyRate: number | null }[];
}

export interface DailyRow {
  date: string;
  bookings: number;
  bookedHours: number;
  bookedValue: number;
  cancelled: number;
  collected: number;
}

export interface PeakHourCell {
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  hour: number;
  bookings: number;
}

export interface TopCustomer {
  customerId: string;
  name: string;
  phone: string;
  bookings: number;
  spent: number;
  lastPlayedAt: string;
}

export interface ConversationSummary {
  id: string;
  venueId: string;
  venueName: string;
  venueSlug: string;
  playerId: string;
  playerName: string;
  lastMessage: string | null;
  lastMessageAt: string | null;
  unread: number;
}

/** {@code fromYou}: sent by anyone at the venue. */
export interface ChatMessage {
  id: string;
  senderName: string;
  fromYou: boolean;
  bookingId: string | null;
  body: string;
  readAt: string | null;
  createdAt: string;
}

// Venue settings

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** Times are "HH:mm:ss"; a close at or before the open means open past midnight. */
export interface DayHours {
  day: Weekday;
  openTime: string | null;
  closeTime: string | null;
  closed: boolean;
}

export interface VenuePolicy {
  advancePercent: number;
  bookingHoldMinutes: number;
  freeCancelHours: number;
  lateCancelRefundPct: number;
  maxAdvanceBookingDays: number;
}

export interface Amenity {
  code: string;
  name: string;
  icon: string;
}

export type CourtFormat = 'FIVE_A_SIDE' | 'SIX_A_SIDE' | 'SEVEN_A_SIDE';
export type CourtSurface = 'ARTIFICIAL_TURF' | 'HARDWOOD' | 'CONCRETE' | 'RUBBER';
export type PriceLabel = 'STANDARD' | 'PEAK' | 'OFF_PEAK' | 'WEEKEND' | 'HOLIDAY';

export interface PricingRule {
  id: string;
  label: PriceLabel;
  dayOfWeek: Weekday | null;
  startTime: string;
  endTime: string;
  price: number;
  priority: number;
  validFrom: string | null;
  validTo: string | null;
  active: boolean;
}

export interface Court {
  id: string;
  name: string;
  format: CourtFormat;
  surface: CourtSurface;
  indoor: boolean;
  slotDurationMinutes: number;
  status: 'ACTIVE' | 'INACTIVE';
  sortOrder: number;
  pricingRules: PricingRule[];
}

export interface VenuePhoto {
  id: string;
  url: string;
  caption: string | null;
  sortOrder: number;
  cover: boolean;
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
  amenities: Amenity[];
  courts: Court[];
  photos: VenuePhoto[];
}

// Staff

export type StaffRole = 'MANAGER' | 'RECEPTIONIST';

export interface StaffMember {
  id: string;
  userId: string;
  fullName: string;
  phone: string;
  role: StaffRole;
  permissions: VenuePermission[];
  active: boolean;
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

export interface Invoice {
  id: string;
  invoiceNumber: string;
  venueId: string;
  planCode: string | null;
  planName: string | null;
  billingCycle: BillingCycle | null;
  amount: number;
  periodStart: string;
  periodEnd: string;
  dueOn: string;
  status: 'DUE' | 'PAID' | 'OVERDUE' | 'VOID';
  paidAt: string | null;
  /** Sent by the owner, not yet checked by FutSlot. */
  paymentPending: boolean;
}

/** Without a subscription the venue is on Free and the subscription fields are null. */
export interface Subscription {
  venueId: string;
  plan: Plan;
  status: 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELLED' | 'EXPIRED' | null;
  billingCycle: BillingCycle | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  nextPlan: Plan | null;
  nextBillingCycle: BillingCycle | null;
  openInvoice: Invoice | null;
  limitsEnforced: boolean;
  courts: number;
  staff: number;
  paymentInstructions: string | null;
}

// Memberships and discount codes

export type MembershipType = 'GAME_PACK' | 'MONTHLY_PASS';

export type MembershipStatus = 'PENDING_PAYMENT' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export interface MembershipPlan {
  id: string;
  venueId: string;
  name: string;
  description: string | null;
  type: MembershipType;
  gamesIncluded: number | null;
  discountPercent: number | null;
  validityDays: number;
  price: number;
  active: boolean;
}

export interface Membership {
  id: string;
  venue: { id: string; name: string; slug: string };
  plan: { id: string; name: string; type: MembershipType; gamesIncluded: number | null; discountPercent: number | null };
  customer: { id: string; name: string; phone: string };
  startsOn: string;
  expiresOn: string;
  gamesRemaining: number | null;
  status: MembershipStatus;
  createdAt: string;
}

export interface DiscountCode {
  id: string;
  code: string;
  description: string | null;
  type: 'PERCENT' | 'FIXED';
  value: number;
  maxDiscountAmount: number | null;
  minBookingAmount: number | null;
  validFrom: string | null;
  validTo: string | null;
  usageLimit: number | null;
  perUserLimit: number | null;
  usedCount: number;
  active: boolean;
}

// Reviews

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
  status: 'PUBLISHED' | 'HIDDEN' | 'REMOVED';
  createdAt: string;
}

/** {@code stars} maps each rating 1–5 to how many published reviews gave it. */
export interface RatingSummary {
  averageRating: number | null;
  reviewCount: number;
  stars: Record<string, number>;
}

// Featured listings

export type FeaturedPlacement = 'HOME_CAROUSEL' | 'SEARCH_TOP' | 'CITY_TOP';

export interface FeaturedListing {
  id: string;
  venueId: string;
  venueName: string;
  placement: FeaturedPlacement;
  city: string | null;
  startsAt: string;
  endsAt: string;
  price: number;
  status: 'PENDING_PAYMENT' | 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'CANCELLED';
  paymentInstructions: string | null;
  paymentPending: boolean;
}

export interface FeaturedQuote {
  placement: FeaturedPlacement;
  startsOn: string;
  days: number;
  pricePerDay: number;
  price: number;
  available: boolean;
}

// Tournaments

export type TournamentFormat = 'KNOCKOUT' | 'LEAGUE' | 'GROUP_KNOCKOUT';

export type TournamentStatus = 'DRAFT' | 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export type Stage = 'GROUP' | 'LEAGUE' | 'ROUND_OF_32' | 'ROUND_OF_16' | 'QUARTER_FINAL' | 'SEMI_FINAL' | 'THIRD_PLACE' | 'FINAL';

export interface TeamRef {
  registrationId: string;
  teamId: string;
  name: string;
  slug: string;
}

export interface TournamentSummary {
  id: string;
  slug: string;
  name: string;
  venue: { id: string; name: string; slug: string; city: string };
  format: TournamentFormat;
  status: TournamentStatus;
  startsOn: string;
  endsOn: string;
  entryFee: number;
  teamsIn: number;
  maxTeams: number;
  bannerUrl: string | null;
}

export interface TournamentEntry {
  id: string;
  team: TeamRef;
  status: 'PENDING_PAYMENT' | 'CONFIRMED' | 'WAITLISTED' | 'WITHDRAWN' | 'REJECTED';
  groupName: string | null;
  seed: number | null;
  squad: { userId: string; name: string; jerseyNumber: number | null }[];
  registeredAt: string;
}

export interface TournamentMatch {
  id: string;
  stage: Stage;
  groupName: string | null;
  round: number;
  matchNumber: number;
  home: TeamRef | null;
  away: TeamRef | null;
  court: { id: string; name: string } | null;
  /** Venue-local with offset. */
  scheduledAt: string | null;
  homeScore: number | null;
  awayScore: number | null;
  homePenalties: number | null;
  awayPenalties: number | null;
  winnerRegistrationId: string | null;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'WALKOVER' | 'POSTPONED' | 'CANCELLED';
  bye: boolean;
}

export interface Standing {
  team: TeamRef;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface TournamentDetail {
  id: string;
  slug: string;
  name: string;
  venue: { id: string; name: string; slug: string; city: string };
  description: string | null;
  rules: string | null;
  bannerUrl: string | null;
  format: TournamentFormat;
  status: TournamentStatus;
  teamSize: number;
  maxSquadSize: number;
  maxTeams: number;
  entryFee: number;
  prizeDetails: string | null;
  pointsForWin: number;
  pointsForDraw: number;
  registrationOpensAt: string | null;
  registrationClosesAt: string | null;
  startsOn: string;
  endsOn: string;
  matchMinutes: number;
  groupCount: number | null;
  acceptingTeams: boolean;
  teamsIn: number;
  waitlisted: number;
  registrations: TournamentEntry[];
  matches: TournamentMatch[];
  tables: { groupName: string | null; rows: Standing[] }[];
  champion: TeamRef | null;
}

// The venue's website

export type SiteTemplate = 'TURF' | 'FLOODLIGHT';

/** Before the owner saves, {@code saved} is false and the fields are suggestions. */
export interface SiteSettings {
  venueId: string;
  saved: boolean;
  template: SiteTemplate;
  subdomain: string;
  customDomain: string | null;
  accentColor: string | null;
  headline: string | null;
  published: boolean;
  url: string;
  previewPath: string;
  /** The venue is approved, so the site can go live. */
  live: boolean;
  dnsTarget: string;
}
