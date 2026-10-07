import { ChatMessage } from '../../core/api.types';
import { addDays, today } from '../../shared/format';

export interface DayGroup {
  day: string;
  label: string;
  messages: ChatMessage[];
}

/** Adds {@code incoming} (any order, possibly overlapping) to {@code known}, oldest first, without duplicates. */
export function merge(known: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byId = new Map(known.map((message) => [message.id, message]));
  for (const message of incoming) {
    byId.set(message.id, message);
  }
  return [...byId.values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id));
}

/** Oldest-first messages split by the venue's calendar day, labelled "Today", "Yesterday" or "Thu 9 Oct". */
export function byDay(messages: ChatMessage[], timeZone: string, now = new Date()): DayGroup[] {
  const todayKey = today(timeZone, now);
  const groups: DayGroup[] = [];
  for (const message of messages) {
    const day = today(timeZone, new Date(message.createdAt));
    let group = groups.at(-1);
    if (!group || group.day !== day) {
      group = { day, label: dayName(day, todayKey), messages: [] };
      groups.push(group);
    }
    group.messages.push(message);
  }
  return groups;
}

function dayName(day: string, todayKey: string): string {
  if (day === todayKey) {
    return 'Today';
  }
  if (day === addDays(todayKey, -1)) {
    return 'Yesterday';
  }
  const [y, m, d] = day.split('-').map(Number);
  return new Intl.DateTimeFormat('en-GB', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' })
    .format(new Date(Date.UTC(y, m - 1, d)));
}

/** Whether two neighbouring messages read as one burst: same side and sender, within five minutes. */
export function continues(previous: ChatMessage | undefined, message: ChatMessage): boolean {
  return !!previous && previous.fromYou === message.fromYou && previous.senderName === message.senderName
    && new Date(message.createdAt).getTime() - new Date(previous.createdAt).getTime() < 5 * 60_000;
}
