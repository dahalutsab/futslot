import { AppNotification } from '../core/api.types';

/** Where tapping a notification goes, from the ids it carries; null when there's nothing to open. */
export function notificationLink(notification: AppNotification): { path: string[]; query?: Record<string, string> } | null {
  const data = notification.data ?? {};
  if (notification.type.startsWith('DEPOSIT_')) {
    return { path: ['/deposit'] };
  }
  if (data['conversationId']) {
    return { path: ['/messages'], query: { c: data['conversationId'] } };
  }
  if (data['bookingId']) {
    return { path: ['/bookings', data['bookingId']] };
  }
  if (data['tournamentSlug']) {
    return { path: ['/tournaments', data['tournamentSlug']] };
  }
  if (data['tournamentRegistrationId']) {
    return { path: ['/tournaments'], query: { mine: '1' } };
  }
  if (data['matchPostId']) {
    return { path: ['/matches', data['matchPostId']] };
  }
  if (data['teamSlug']) {
    return { path: ['/teams', data['teamSlug']] };
  }
  if (data['membershipId']) {
    return { path: ['/memberships'] };
  }
  if (data['reviewId']) {
    return { path: ['/reviews'] };
  }
  if (data['recurringBookingId']) {
    return { path: ['/bookings'] };
  }
  if (data['venueSlug']) {
    return { path: ['/venues', data['venueSlug']] };
  }
  return null;
}
