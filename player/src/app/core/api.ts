import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AddOnItem, AppNotification, Availability, Booking, BookingPayments, BookingRequest, ChatMessage, ConversationSummary,
  Deposit, DepositWithdrawal, MatchPost, MatchPostRequest, MatchPostType, MatchResult, Membership, MembershipPlan,
  MyTournamentEntry, OwedShare, Page, Payment, PaymentClaim, PaymentMethod, PlayerCard, PlayerProfileRequest, Quote,
  RatingSummary, Review, ReviewWrite, Side, SkillLevel, Split, SplitRules, Team, TeamRequest, TeamRole, TeamSummary,
  TournamentDetail, TournamentSummary, User, VenueDetail, VenueSummary, WaitlistEntry,
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

  // Memberships

  membershipPlans(slug: string): Promise<MembershipPlan[]> {
    return this.get<MembershipPlan[]>(`/api/v1/venues/${slug}/membership-plans`);
  }

  memberships(): Promise<Membership[]> {
    return this.get<Membership[]>('/api/v1/me/memberships');
  }

  /** Payment details are left out for a free plan. */
  buyMembership(planId: string, claim: PaymentClaim | null): Promise<Membership> {
    return this.post<Membership>('/api/v1/me/memberships', { planId, ...claim });
  }

  // Splitting a booking's cost

  /** Rejects with SPLIT_NOT_FOUND until the booker sets one up. */
  split(bookingId: string): Promise<Split> {
    return this.get<Split>(`/api/v1/bookings/${bookingId}/split`);
  }

  setUpSplit(bookingId: string, rules: SplitRules): Promise<Split> {
    return firstValueFrom(this.http.put<Split>(`/api/v1/bookings/${bookingId}/split`, rules));
  }

  /** An app user by phone, or a guest by name. */
  addSplitPlayer(bookingId: string, player: { name?: string; phone?: string; side: Side }): Promise<Split> {
    return this.post<Split>(`/api/v1/bookings/${bookingId}/split/players`, player);
  }

  moveSplitPlayer(bookingId: string, playerId: string, side: Side): Promise<Split> {
    return firstValueFrom(this.http.put<Split>(`/api/v1/bookings/${bookingId}/split/players/${playerId}`, { side }));
  }

  removeSplitPlayer(bookingId: string, playerId: string): Promise<Split> {
    return firstValueFrom(this.http.delete<Split>(`/api/v1/bookings/${bookingId}/split/players/${playerId}`));
  }

  splitResult(bookingId: string, result: MatchResult | null): Promise<Split> {
    return this.post<Split>(`/api/v1/bookings/${bookingId}/split/result`, { result });
  }

  undoSplitResult(bookingId: string): Promise<Split> {
    return firstValueFrom(this.http.delete<Split>(`/api/v1/bookings/${bookingId}/split/result`));
  }

  splitPaid(bookingId: string, playerId: string, paid: boolean): Promise<Split> {
    const url = `/api/v1/bookings/${bookingId}/split/players/${playerId}/paid`;
    return firstValueFrom(paid ? this.http.post<Split>(url, null) : this.http.delete<Split>(url));
  }

  splitDues(): Promise<OwedShare[]> {
    return this.get<OwedShare[]>('/api/v1/me/split-dues');
  }

  // Players and teams

  myProfile(): Promise<PlayerCard> {
    return this.get<PlayerCard>('/api/v1/me/player-profile');
  }

  saveProfile(profile: PlayerProfileRequest): Promise<PlayerCard> {
    return firstValueFrom(this.http.put<PlayerCard>('/api/v1/me/player-profile', profile));
  }

  player(userId: string): Promise<PlayerCard> {
    return this.get<PlayerCard>(`/api/v1/players/${userId}`);
  }

  teams(filter: { q?: string; city?: string; page?: number }): Promise<Page<TeamSummary>> {
    const params: Record<string, string> = { page: String(filter.page ?? 0), size: '20' };
    if (filter.q) {
      params['q'] = filter.q;
    }
    if (filter.city) {
      params['city'] = filter.city;
    }
    return this.get<Page<TeamSummary>>('/api/v1/teams', params);
  }

  team(slug: string): Promise<Team> {
    return this.get<Team>(`/api/v1/teams/${slug}`);
  }

  /** Teams the player is in or invited to. */
  myTeams(): Promise<Team[]> {
    return this.get<Team[]>('/api/v1/me/teams');
  }

  createTeam(team: TeamRequest): Promise<Team> {
    return this.post<Team>('/api/v1/teams', team);
  }

  updateTeam(teamId: string, team: TeamRequest): Promise<Team> {
    return firstValueFrom(this.http.put<Team>(`/api/v1/teams/${teamId}`, team));
  }

  invite(teamId: string, phone: string): Promise<Team> {
    return this.post<Team>(`/api/v1/teams/${teamId}/invitations`, { phone });
  }

  answerInvite(teamId: string, accept: boolean): Promise<Team | null> {
    return this.post<Team | null>(`/api/v1/teams/${teamId}/invitation/${accept ? 'accept' : 'decline'}`, {});
  }

  leaveTeam(teamId: string): Promise<void> {
    return this.post<void>(`/api/v1/teams/${teamId}/leave`, {});
  }

  removeMember(teamId: string, userId: string): Promise<Team> {
    return firstValueFrom(this.http.delete<Team>(`/api/v1/teams/${teamId}/members/${userId}`));
  }

  changeRole(teamId: string, userId: string, role: TeamRole): Promise<Team> {
    return firstValueFrom(this.http.put<Team>(`/api/v1/teams/${teamId}/members/${userId}/role`, { role }));
  }

  // Matchmaking

  /** Open posts, soonest first. {@code date} is a Nepal date. */
  matchPosts(filter: { city?: string; type?: MatchPostType; date?: string; skillLevel?: SkillLevel; page?: number }): Promise<Page<MatchPost>> {
    const params: Record<string, string> = { page: String(filter.page ?? 0), size: '20' };
    for (const key of ['city', 'type', 'date', 'skillLevel'] as const) {
      if (filter[key]) {
        params[key] = filter[key]!;
      }
    }
    return this.get<Page<MatchPost>>('/api/v1/match-posts', params);
  }

  matchPost(id: string): Promise<MatchPost> {
    return this.get<MatchPost>(`/api/v1/match-posts/${id}`);
  }

  createMatchPost(post: MatchPostRequest): Promise<MatchPost> {
    return this.post<MatchPost>('/api/v1/match-posts', post);
  }

  cancelMatchPost(id: string): Promise<MatchPost> {
    return this.post<MatchPost>(`/api/v1/match-posts/${id}/cancel`, {});
  }

  /** {@code teamId}: the team asking, to answer a FIND_OPPONENT post. */
  askToJoin(id: string, message: string, teamId?: string): Promise<MatchPost> {
    return this.post<MatchPost>(`/api/v1/match-posts/${id}/requests`, { message: message || undefined, teamId });
  }

  withdrawRequest(id: string): Promise<MatchPost> {
    return this.post<MatchPost>(`/api/v1/match-posts/${id}/requests/withdraw`, {});
  }

  answerRequest(id: string, requestId: string, accept: boolean): Promise<MatchPost> {
    return this.post<MatchPost>(`/api/v1/match-posts/${id}/requests/${requestId}/${accept ? 'accept' : 'decline'}`, {});
  }

  myMatchPosts(): Promise<Page<MatchPost>> {
    return this.get<Page<MatchPost>>('/api/v1/me/match-posts', { size: '50' });
  }

  myMatchRequests(): Promise<Page<MatchPost>> {
    return this.get<Page<MatchPost>>('/api/v1/me/match-requests', { size: '50' });
  }

  // Tournaments

  tournaments(upcoming = true, city?: string): Promise<TournamentSummary[]> {
    return this.get<TournamentSummary[]>('/api/v1/tournaments', city ? { upcoming: String(upcoming), city } : { upcoming: String(upcoming) });
  }

  tournament(slug: string): Promise<TournamentDetail> {
    return this.get<TournamentDetail>(`/api/v1/tournaments/${slug}`);
  }

  register(tournamentId: string, teamId: string, squad: { userId: string; jerseyNumber?: number }[]): Promise<TournamentDetail> {
    return this.post<TournamentDetail>(`/api/v1/tournaments/${tournamentId}/registrations`, { teamId, squad });
  }

  withdrawEntry(tournamentId: string, registrationId: string): Promise<TournamentDetail> {
    return this.post<TournamentDetail>(`/api/v1/tournaments/${tournamentId}/registrations/${registrationId}/withdraw`, {});
  }

  myEntries(): Promise<MyTournamentEntry[]> {
    return this.get<MyTournamentEntry[]>('/api/v1/me/tournament-entries');
  }

  payEntry(registrationId: string, claim: PaymentClaim): Promise<Payment> {
    return this.post<Payment>(`/api/v1/tournament-entries/${registrationId}/payments`, claim);
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
