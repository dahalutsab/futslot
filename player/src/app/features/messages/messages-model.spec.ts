import { ChatMessage } from '../../core/api.types';
import { byDay, continues, merge } from './messages-model';

const ZONE = 'Asia/Kathmandu';

function message(id: string, createdAt: string, fromYou = false, senderName = 'Sita Rai'): ChatMessage {
  return { id, senderName, fromYou, bookingId: null, body: id, readAt: null, createdAt };
}

describe('messages model', () => {
  it('merges pages oldest first without duplicates', () => {
    const known = [message('b', '2026-10-07T10:00:00Z'), message('c', '2026-10-07T11:00:00Z')];
    const merged = merge(known, [message('c', '2026-10-07T11:00:00Z'), message('a', '2026-10-07T09:00:00Z')]);
    expect(merged.map((m) => m.id)).toEqual(['a', 'b', 'c']);
  });

  it('groups by the venue day, not the UTC day', () => {
    // 20:00 UTC on the 6th is 01:45 on the 7th in Kathmandu
    const groups = byDay([message('a', '2026-10-05T12:00:00Z'), message('b', '2026-10-06T20:00:00Z'),
      message('c', '2026-10-07T03:00:00Z')], ZONE, new Date('2026-10-07T06:00:00Z'));
    expect(groups.map((g) => [g.label, g.messages.length])).toEqual([['Mon 5 Oct', 1], ['Today', 2]]);
  });

  it('labels yesterday', () => {
    const groups = byDay([message('a', '2026-10-06T06:00:00Z')], ZONE, new Date('2026-10-07T06:00:00Z'));
    expect(groups[0].label).toBe('Yesterday');
  });

  it('runs a burst together only for the same sender within five minutes', () => {
    const first = message('a', '2026-10-07T10:00:00Z', true, 'Utsab');
    expect(continues(first, message('b', '2026-10-07T10:04:00Z', true, 'Utsab'))).toBe(true);
    expect(continues(first, message('b', '2026-10-07T10:06:00Z', true, 'Utsab'))).toBe(false);
    expect(continues(first, message('b', '2026-10-07T10:01:00Z', true, 'Hari'))).toBe(false);
    expect(continues(undefined, first)).toBe(false);
  });
});
