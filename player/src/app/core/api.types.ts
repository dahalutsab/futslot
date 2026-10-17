/** Shapes returned by the FutSlot API (backend: docs/api/openapi.yaml), as players see them. */

export type Role = 'PLAYER' | 'OWNER' | 'STAFF' | 'ADMIN';

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

// Venues

export interface VenueSummary {
  id: string;
  slug: string;
  name: string;
  area: string | null;
  city: string;
  latitude: number | null;
  longitude: number | null;
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

/** Cancelling less than {@link withinHours} before the start costs {@link feePercent} of the price. */
export interface CancellationTier {
  withinHours: number;
  feePercent: number;
}

export interface VenuePolicy {
  advancePercent: number;
  bookingHoldMinutes: number;
  /** Furthest from the start first; empty means cancelling is always free. */
  cancellationTiers: CancellationTier[];
  maxAdvanceBookingDays: number;
  /** Late cancellations and no-shows in {@link STRIKE_WINDOW_DAYS} that raise the advance; null when off. */
  strikeLimit: number | null;
  strikeAdvancePercent: number | null;
  /** Players need a full {@link DEPOSIT_AMOUNT} FutSlot deposit to book here. */
  depositRequired: boolean;
}

/** How far back late cancellations and no-shows count as strikes. */
export const STRIKE_WINDOW_DAYS = 90;

/** The booking deposit players keep with FutSlot to book venues that require it. */
export const DEPOSIT_AMOUNT = 1000;

export interface Amenity {
  code: string;
  name: string;
  icon: string;
}

export type CourtFormat = 'FIVE_A_SIDE' | 'SIX_A_SIDE' | 'SEVEN_A_SIDE';
export type CourtSurface = 'ARTIFICIAL_TURF' | 'HARDWOOD' | 'CONCRETE' | 'RUBBER';

export interface Court {
  id: string;
  name: string;
  format: CourtFormat;
  surface: CourtSurface;
  indoor: boolean;
  slotDurationMinutes: number;
  status: 'ACTIVE' | 'INACTIVE';
  sortOrder: number;
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
  policy: VenuePolicy;
  /** Where to pay the venue's advance: its own eSewa, Khalti or bank account. */
  paymentInstructions: string | null;
  averageRating: number;
  reviewCount: number;
  fromPrice: number | null;
  operatingHours: DayHours[];
  amenities: Amenity[];
  courts: Court[];
  photos: VenuePhoto[];
}

export interface AddOnItem {
  id: string;
  name: string;
  category: 'EQUIPMENT_RENTAL' | 'FOOD_DRINK' | 'OTHER';
  price: number;
  active: boolean;
}

// Slots and bookings

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

export interface AddOnLine {
  itemId: string;
  quantity: number;
}

/** One slot, by its start from the availability response. A membership or a discount code, not both. */
export interface BookingRequest {
  courtId: string;
  startsAt: string;
  notes?: string;
  membershipId?: string;
  discountCode?: string;
  addOns?: AddOnLine[];
}

/**
 * {@code total = price + addOns - discount}; {@code advanceRequired} is what must be paid to confirm.
 * {@code strikeAdvance}: higher than usual because of recent late cancellations and no-shows.
 * {@code depositToAdd}: what the player must add to their FutSlot deposit before booking (0 when nothing).
 */
export interface Quote {
  price: number;
  addOns: number;
  discount: number;
  total: number;
  advanceRequired: number;
  strikeAdvance: boolean;
  depositRequired: boolean;
  depositToAdd: number;
  membershipId: string | null;
  discountCode: string | null;
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
  /** The venue's fee share when the player cancelled late (or, if {@link depositBacked}, didn't show up). */
  cancellationFeePercent: number;
  /** Booked at a venue that requires the deposit, so fees the advance doesn't cover come out of it. */
  depositBacked: boolean;
  createdAt: string;
}

// Payments

export type PaymentMethod = 'CASH' | 'ESEWA' | 'KHALTI' | 'FONEPAY' | 'BANK_TRANSFER';

export type TransactionStatus = 'INITIATED' | 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'REFUNDED' | 'PARTIALLY_REFUNDED';

export interface Payment {
  id: string;
  purpose: 'BOOKING' | 'SPLIT_SHARE' | 'MEMBERSHIP' | 'TOURNAMENT_ENTRY' | 'DEPOSIT';
  bookingId: string | null;
  bookingReference: string | null;
  membershipId: string | null;
  membershipPlanName: string | null;
  tournamentRegistrationId: string | null;
  tournamentName: string | null;
  customerName: string | null;
  amount: number;
  method: PaymentMethod;
  status: TransactionStatus;
  transactionId: string | null;
  failureReason: string | null;
  paidAt: string | null;
  createdAt: string;
}

/** "I paid Rs X on eSewa, transaction Y". */
export interface PaymentClaim {
  method: PaymentMethod;
  transactionId: string;
  amount: number;
}

export type RefundStatus = 'REQUESTED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'REJECTED';

export interface Refund {
  id: string;
  bookingId: string;
  bookingReference: string;
  amount: number;
  originalMethod: PaymentMethod;
  reason: string;
  status: RefundStatus;
  reference: string | null;
  createdAt: string;
  processedAt: string | null;
}

/** The money side of one booking, and where to pay the venue. */
export interface BookingPayments {
  totalAmount: number;
  advanceRequired: number;
  paidAmount: number;
  amountDue: number;
  paymentStatus: PaymentStatus;
  paymentInstructions: string | null;
  payments: Payment[];
  refunds: Refund[];
}

// The FutSlot booking deposit

export type DepositEntryType = 'TOP_UP' | 'FEE' | 'WITHDRAWAL' | 'WITHDRAWAL_CANCELLED';

/** Positive money in, negative money out. {@code bookingReference} and {@code venueName} are set for fees. */
export interface DepositEntry {
  id: string;
  type: DepositEntryType;
  amount: number;
  balanceAfter: number;
  bookingReference: string | null;
  venueName: string | null;
  createdAt: string;
}

/** The whole balance on its way back; FutSlot sends it once {@code dueAt} has passed. */
export interface DepositWithdrawal {
  id: string;
  amount: number;
  method: PaymentMethod;
  sendTo: string;
  status: 'REQUESTED' | 'PAID' | 'CANCELLED';
  dueAt: string;
  reference: string | null;
  paidAt: string | null;
  createdAt: string;
}

/**
 * {@code toAdd} is what fills it (0 when full). {@code topUpBeingChecked}: a top-up was sent that FutSlot
 * hasn't checked yet; {@code paymentInstructions} says where to send one.
 */
export interface Deposit {
  required: number;
  balance: number;
  toAdd: number;
  topUpBeingChecked: boolean;
  paymentInstructions: string | null;
  withdrawal: DepositWithdrawal | null;
  history: DepositEntry[];
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

/** What a player sends to review a booking; a star rating alone is a review. */
export interface ReviewWrite {
  rating: number;
  comment?: string;
}

// Notifications

/** {@code data} holds ids to open what it's about: bookingId, conversationId, tournamentSlug... */
export interface AppNotification {
  id: string;
  type: string;
  title: string;
  body: string;
  data: Record<string, string>;
  read: boolean;
  createdAt: string;
}

// Messages with venues

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

/** {@code fromYou}: sent by the player; venue replies are signed with the venue's name. */
export interface ChatMessage {
  id: string;
  senderName: string;
  fromYou: boolean;
  bookingId: string | null;
  body: string;
  readAt: string | null;
  createdAt: string;
}

// Waitlist

export type WaitlistStatus = 'WAITING' | 'NOTIFIED' | 'BOOKED' | 'EXPIRED' | 'CANCELLED';

/** A booked slot the player wants if it frees up; NOTIFIED means it did and they were told. */
export interface WaitlistEntry {
  id: string;
  venueId: string;
  venueName: string;
  venueSlug: string;
  courtId: string;
  courtName: string;
  startsAt: string;
  endsAt: string;
  status: WaitlistStatus;
  notifiedAt: string | null;
  createdAt: string;
}

// Memberships

export type MembershipType = 'GAME_PACK' | 'MONTHLY_PASS';

export type MembershipStatus = 'PENDING_PAYMENT' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

/** A game pack ({@link gamesIncluded} games) or a pass ({@link discountPercent} off every game). */
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

// Splitting a booking's cost

export type SplitRule = 'EQUAL' | 'LOSERS_PAY_ALL' | 'LOSERS_PAY_PERCENT' | 'LOSERS_PAY_COURT';

export type Side = 'A' | 'B';

export type MatchResult = 'A_WON' | 'B_WON' | 'DRAW';

export interface SplitPerson {
  userId: string;
  name: string;
  phone: string | null;
}

/** {@code share} is set once the result is in; {@code phone} only for the booker. */
export interface SplitPlayer {
  id: string;
  name: string;
  side: Side;
  booker: boolean;
  you: boolean;
  appUser: boolean;
  phone: string | null;
  share: number | null;
  settled: boolean;
  settledAt: string | null;
}

/** What each player on a side pays for one result, roughly (shares differ by at most a rupee). */
export interface SplitOutcome {
  result: MatchResult;
  sideATotal: number;
  sideAEach: number;
  sideBTotal: number;
  sideBEach: number;
}

export interface Split {
  bookingId: string;
  reference: string;
  venueName: string;
  courtName: string;
  startsAt: string;
  bookedBy: SplitPerson;
  rule: SplitRule;
  losersPercent: number | null;
  courtAmount: number;
  extrasAmount: number;
  extrasNote: string | null;
  total: number;
  sideAName: string;
  sideBName: string;
  players: SplitPlayer[];
  result: MatchResult | null;
  resultRecordedAt: string | null;
  outcomes: SplitOutcome[];
  collected: number;
  outstanding: number;
  youAreBooker: boolean;
}

export interface SplitRules {
  rule: SplitRule;
  losersPercent?: number;
  extrasAmount?: number;
  extrasNote?: string;
  sideAName?: string;
  sideBName?: string;
  bookerSide?: Side;
}

/** Money the player owes whoever booked a game they played in. */
export interface OwedShare {
  bookingId: string;
  venueName: string;
  startsAt: string;
  side: string;
  result: MatchResult | null;
  amount: number;
  payTo: SplitPerson;
}

// Players and teams

export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'PRO';

export type Position = 'GOALKEEPER' | 'DEFENDER' | 'MIDFIELDER' | 'FORWARD' | 'ANY';

export type PreferredFoot = 'LEFT' | 'RIGHT' | 'BOTH';

/** A player as others see them (short name); profile fields are null when not filled in or kept private. */
export interface PlayerCard {
  userId: string;
  name: string;
  avatarUrl: string | null;
  position: Position | null;
  skillLevel: SkillLevel | null;
  preferredFoot: PreferredFoot | null;
  city: string | null;
  bio: string | null;
  gamesPlayed: number;
  /** Only on the player's own card: whether other players see the profile. */
  isPublic: boolean | null;
}

export interface PlayerProfileRequest {
  position?: Position;
  skillLevel?: SkillLevel;
  preferredFoot?: PreferredFoot;
  city?: string;
  bio?: string;
  isPublic?: boolean;
}

export type TeamRole = 'CAPTAIN' | 'VICE_CAPTAIN' | 'MEMBER';

export type MemberStatus = 'INVITED' | 'ACTIVE' | 'LEFT' | 'REMOVED';

export interface TeamSummary {
  id: string;
  name: string;
  slug: string;
  logoUrl: string | null;
  city: string | null;
  skillLevel: SkillLevel | null;
  memberCount: number;
}

/** Members see full names and their own role; leaders also see pending invites. */
export interface Team {
  id: string;
  name: string;
  slug: string;
  logoUrl: string | null;
  city: string | null;
  skillLevel: SkillLevel | null;
  isPublic: boolean;
  captainId: string;
  memberCount: number;
  members: { userId: string; name: string; avatarUrl: string | null; role: TeamRole; status: MemberStatus }[];
  myRole: TeamRole | null;
  myStatus: MemberStatus | null;
}

export interface TeamRequest {
  name: string;
  city?: string;
  skillLevel?: SkillLevel;
  isPublic?: boolean;
}

// Matchmaking

export type MatchPostType = 'NEED_PLAYERS' | 'FIND_OPPONENT';

export type MatchPostStatus = 'OPEN' | 'FILLED' | 'CANCELLED' | 'EXPIRED';

export type JoinRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'WITHDRAWN';

export interface TeamRef {
  id: string;
  name: string;
  slug: string;
}

/**
 * Phones are shared once a request is accepted: the poster sees accepted players' numbers, accepted players see
 * the poster's ({@code contactPhone}). {@code requests} is only filled in for the poster.
 */
export interface MatchPost {
  id: string;
  type: MatchPostType;
  status: MatchPostStatus;
  postedBy: { userId: string; name: string };
  team: TeamRef | null;
  venue: { id: string; name: string; slug: string } | null;
  city: string | null;
  startsAt: string;
  playersNeeded: number | null;
  placesTaken: number;
  skillLevel: SkillLevel | null;
  costPerPlayer: number | null;
  note: string | null;
  mine: boolean;
  myRequest: { id: string; status: JoinRequestStatus; team: TeamRef | null } | null;
  contactPhone: string | null;
  requests: { id: string; player: PlayerCard; team: TeamRef | null; message: string | null; status: JoinRequestStatus; phone: string | null; createdAt: string }[];
  createdAt: string;
}

/** Where and when: a booking of the player's, or a venue and time, or a city and time. */
export interface MatchPostRequest {
  type: MatchPostType;
  teamId?: string;
  bookingId?: string;
  venueId?: string;
  city?: string;
  startsAt?: string;
  playersNeeded?: number;
  skillLevel?: SkillLevel;
  costPerPlayer?: number;
  note?: string;
}

// Tournaments

export type TournamentFormat = 'KNOCKOUT' | 'LEAGUE' | 'GROUP_KNOCKOUT';

export type TournamentStatus = 'DRAFT' | 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export type Stage = 'GROUP' | 'LEAGUE' | 'ROUND_OF_32' | 'ROUND_OF_16' | 'QUARTER_FINAL' | 'SEMI_FINAL' | 'THIRD_PLACE' | 'FINAL';

export interface EntryTeam {
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

export type EntryStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'WAITLISTED' | 'WITHDRAWN' | 'REJECTED';

export interface TournamentEntry {
  id: string;
  team: EntryTeam;
  status: EntryStatus;
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
  home: EntryTeam | null;
  away: EntryTeam | null;
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
  team: EntryTeam;
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
  champion: EntryTeam | null;
}

export interface MyTournamentEntry {
  tournament: TournamentSummary;
  entry: TournamentEntry;
}
