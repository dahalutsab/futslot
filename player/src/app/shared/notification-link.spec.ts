import { AppNotification } from '../core/api.types';
import { notificationLink } from './notification-link';

function notification(type: string, data: Record<string, string>): AppNotification {
  return { id: 'n', type, title: 't', body: 'b', data, read: false, createdAt: '2026-10-17T00:00:00Z' };
}

describe('notification links', () => {
  it('opens what the notification is about', () => {
    expect(notificationLink(notification('BOOKING_CONFIRMED', { bookingId: 'b1', venueId: 'v' }))).toEqual({ path: ['/bookings', 'b1'] });
    expect(notificationLink(notification('CHAT_MESSAGE', { conversationId: 'c1', venueId: 'v' })))
      .toEqual({ path: ['/messages'], query: { c: 'c1' } });
    expect(notificationLink(notification('WAITLIST_SLOT_FREE', { venueId: 'v', venueSlug: 'dhuku' }))).toEqual({ path: ['/venues', 'dhuku'] });
    expect(notificationLink(notification('TEAM_INVITE', { teamId: 't', teamSlug: 'reds' }))).toEqual({ path: ['/teams', 'reds'] });
  });

  it('sends deposit news to the deposit, whatever it carries', () => {
    expect(notificationLink(notification('DEPOSIT_FEE_TAKEN', {}))).toEqual({ path: ['/deposit'] });
  });

  it('has nowhere to go without ids', () => {
    expect(notificationLink(notification('VENUE_APPROVED', {}))).toBeNull();
  });
});
