import {
  BadgePercent,
  CalendarDays,
  ChartColumn,
  ClipboardList,
  CreditCard,
  Image,
  LayoutGrid,
  LucideIconData,
  Megaphone,
  MessageSquare,
  Package,
  Star,
  Store,
  Trophy,
  UserCog,
  Users,
  Wallet,
} from 'lucide-angular';

import { VenuePermission } from '../core/api.types';
import { VenueContext } from '../core/venue-context';

export interface NavItem {
  path: string;
  label: string;
  icon: LucideIconData;
  /** null: anyone at the venue; OWNER: the owner only; a list: any one of them. */
  needs: VenuePermission | VenuePermission[] | 'OWNER' | null;
  /** Shown in the phone bottom bar (at most 4, plus More). */
  onPhoneBar?: boolean;
  badge?: 'payments' | 'messages';
  /** What the screen is for, in a line; shown when searching for it. */
  about: string;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

/**
 * Grouped so no group has more than 7 entries (Miller's law); the first group is what staff use all day.
 */
export const NAVIGATION: NavGroup[] = [
  {
    label: 'Daily',
    items: [
      { path: 'schedule', label: 'Schedule', icon: CalendarDays, needs: null, onPhoneBar: true,
        about: "Today's courts at a glance: bookings, holds, blocks and tournament matches." },
      { path: 'bookings', label: 'Bookings', icon: ClipboardList, needs: 'MANAGE_BOOKINGS', onPhoneBar: true,
        about: 'Find, move, cancel and take payment for bookings.' },
      { path: 'payments', label: 'Payments', icon: Wallet, needs: 'MANAGE_BOOKINGS', onPhoneBar: true, badge: 'payments',
        about: 'Wallet payments to verify, and refunds to pay back.' },
      { path: 'customers', label: 'Customers', icon: Users, needs: 'VIEW_CUSTOMERS',
        about: 'Everyone who has booked with you, with their history and notes.' },
      { path: 'messages', label: 'Messages', icon: MessageSquare, needs: 'MANAGE_BOOKINGS', onPhoneBar: true,
        badge: 'messages', about: 'Questions from players.' },
      { path: 'reports', label: 'Reports', icon: ChartColumn, needs: 'VIEW_REPORTS',
        about: 'Bookings, money and occupancy over time, and a CSV export.' },
    ],
  },
  {
    label: 'Venue',
    items: [
      { path: 'venue/profile', label: 'Profile & hours', icon: Store, needs: 'MANAGE_VENUE',
        about: 'Name, address, opening hours, policies and payment instructions.' },
      { path: 'venue/courts', label: 'Courts & pricing', icon: LayoutGrid, needs: 'MANAGE_VENUE',
        about: 'Your courts and their prices by time of day.' },
      { path: 'venue/add-ons', label: 'Extras', icon: Package, needs: 'MANAGE_VENUE',
        about: 'Bibs, balls, water and anything else you sell with a booking.' },
      { path: 'venue/photos', label: 'Photos', icon: Image, needs: 'MANAGE_VENUE',
        about: 'Photos players see, and the cover shown in listings.' },
      { path: 'venue/staff', label: 'Staff', icon: UserCog, needs: 'MANAGE_STAFF',
        about: 'Who can use this dashboard, and what they can do.' },
      { path: 'venue/billing', label: 'Plan & billing', icon: CreditCard, needs: 'OWNER',
        about: 'Your FutSlot plan and invoices.' },
    ],
  },
  {
    label: 'Grow',
    items: [
      { path: 'grow/memberships', label: 'Memberships & codes', icon: BadgePercent, needs: ['MANAGE_BOOKINGS', 'MANAGE_VENUE'],
        about: 'Game packs, monthly passes and discount codes.' },
      { path: 'grow/tournaments', label: 'Tournaments', icon: Trophy, needs: 'MANAGE_VENUE',
        about: 'Run knockouts and leagues: entries, fixtures and results.' },
      { path: 'grow/reviews', label: 'Reviews', icon: Star, needs: 'MANAGE_VENUE',
        about: 'What players say, and your replies.' },
      { path: 'grow/featured', label: 'Featured listing', icon: Megaphone, needs: 'OWNER',
        about: 'Be shown first to players in the app.' },
    ],
  },
];

export const ALL_ITEMS: NavItem[] = NAVIGATION.flatMap((group) => group.items);

/** Whether the person may see this item at the venue they're looking at. */
export function canSee(item: NavItem, venues: VenueContext): boolean {
  if (item.needs === null) {
    return true;
  }
  if (item.needs === 'OWNER') {
    return venues.isOwner();
  }
  return Array.isArray(item.needs) ? item.needs.some((p) => venues.can(p)) : venues.can(item.needs);
}
