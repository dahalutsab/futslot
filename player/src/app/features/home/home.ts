import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { Booking, Deposit, MatchPost, OwedShare, Split, VenueDetail, VenueSummary, WaitlistEntry } from '../../core/api.types';
import { Session } from '../../core/session';
import { digits, l } from '../../i18n/i18n';
import { bookingChip, cancelOutcome } from '../../shared/booking-model';
import { dayLabel, localClock, longDate, methodLabel, rupees, today, weekdayShort } from '../../shared/format';
import { Coins, Hourglass, Icon, IconData, MapPin, ShareNetwork, Star, Wallet } from '../../shared/icon';
import { skillLabels } from '../../shared/labels';
import { StatusChip } from '../../shared/status-chip';
import { VenueCard } from '../../shared/venue-card';

const VALLEY = 'Asia/Kathmandu';

/** Words in venue names that say what it is, not which one: left out of the headline's "at Dhuku." */
const GENERIC = new Set(['futsal', 'hub', 'arena', 'recreation', 'recreational', 'centre', 'center', 'sports', 'ground', 'club', '&', 'and']);

/** Something waiting on the player: magenta when it's theirs to do, cyan while it's in hand, plain when optional. */
interface Need {
  icon: IconData;
  ink: 'act' | 'progress' | 'optional';
  title: string;
  detail: string;
  amount: string;
  link: string;
}

/** A tournament game coming up for one of the player's teams, as one sentence. */
interface Fixture {
  kicker: string;
  team: string;
  other: string;
  when: string;
  venue: string;
  slug: string;
}

/**
 * The signed-in player's home (design_handoff_broadsheet, screens 1b and 1c), set as a front page: the next game
 * is the lead story with its money spelled out, then what needs them, venues to book, this week's games, open games
 * nearby and their tournament. Everything comes from the API; a part that fails to load is left out.
 */
@Component({
  selector: 'fs-home',
  imports: [RouterLink, Icon, StatusChip, VenueCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="frame">
      <!-- The head rule and dateline -->
      <div class="rule-thick-thin lg:mt-3"></div>
      <p class="kicker flex justify-between gap-x-7 gap-y-3 border-b border-text py-2.5 max-lg:!text-[12px] lg:py-3.5">
        <span class="lg:hidden">{{ shortDate() }}</span>
        <span class="hidden lg:inline">{{ longDate() }}</span>
        <span class="hidden lg:inline">{{ greeting() }}</span>
        <span class="hidden lg:inline">{{ l().gamesThisWeek(week().length) }}</span>
        @if (deposit(); as d) {
          <a routerLink="/deposit" class="tabular hover:text-accent-700">{{ l().depositBalance(rupees(d.balance)) }}</a>
        }
      </p>

      <section class="grid gap-12 pt-6 pb-10 lg:grid-cols-[minmax(0,7fr)_minmax(0,4fr)] lg:gap-[72px] lg:pt-14 lg:pb-16">
        <!-- The lead: the next game -->
        <div>
          @if (bookings() === null) {
            <div class="h-72 animate-pulse bg-surface-2" [attr.aria-label]="l().loading"></div>
          } @else if (next(); as b) {
            <p class="kicker mb-2.5 flex items-center gap-2 max-lg:!text-[12px] lg:mb-[18px] lg:gap-2.5">
              {{ l().yourNextGame }} <fs-status-chip class="normal-case tracking-normal" [value]="chip(b)" />
            </p>
            <h1 class="set-in ml-[-0.03em] text-[40px] leading-[44px] tracking-[-0.02em] lg:ml-[-0.035em] lg:text-[72px] lg:leading-[76px] lg:tracking-[-0.025em]">
              {{ when(b) }}<span class="font-normal italic">{{ l().atVenue(shortName(b.venue.name)) }}</span>
            </h1>
            <p class="set-in mt-2 text-[15px] leading-[23px] text-text/78 lg:hidden" style="--set: 1">{{ placeShort(b) }}</p>
            <p class="set-in mt-4 hidden text-[19px] leading-[30px] text-text/80 lg:block" style="--set: 1">{{ place(b) }}</p>

            <!-- Phone: the countdown beside one line of money -->
            <div class="mt-[22px] flex items-end gap-[18px] lg:hidden">
              <span class="cmyk-num font-display text-[72px] font-semibold">
                <span class="paper">{{ countdown().figure }}</span>
                <span class="plate plate-c" aria-hidden="true">{{ countdown().figure }}</span>
                <span class="plate plate-m" aria-hidden="true">{{ countdown().figure }}</span>
                <span class="plate plate-y" aria-hidden="true">{{ countdown().figure }}</span>
              </span>
              <p class="mb-1 text-sm leading-[21px] text-text/75">
                {{ l().leftToPay(rupees(left(b))) }}
                @if (split(); as s) { {{ l().splitPaidShort(settled(s), s.players.length) }} }
              </p>
            </div>

            <!-- Desktop: the countdown, then the money as an index -->
            <div class="mt-11 hidden grid-cols-[auto_minmax(0,1fr)] items-start gap-14 lg:grid">
              <div>
                <span class="cmyk-num block font-display text-[132px] font-semibold">
                  <span class="paper">{{ countdown().figure }}</span>
                  <span class="plate plate-c" aria-hidden="true">{{ countdown().figure }}</span>
                  <span class="plate plate-m" aria-hidden="true">{{ countdown().figure }}</span>
                  <span class="plate plate-y" aria-hidden="true">{{ countdown().figure }}</span>
                </span>
                <p class="mt-4 text-[15px] text-text/70">{{ countdown().label }}</p>
              </div>
              <div class="tabular text-base leading-7">
                <p class="flex gap-2.5"><span>{{ peak() ? l().moneyCourtPeak : l().moneyCourt }}</span><span class="leader"></span><span>{{ rupees(b.priceAmount) }}</span></p>
                @if (b.addOnsAmount > 0) {
                  <p class="flex gap-2.5"><span>{{ extras(b) }}</span><span class="leader"></span><span>{{ rupees(b.addOnsAmount) }}</span></p>
                }
                @if (b.discountAmount > 0) {
                  <p class="flex gap-2.5 text-accent-700"><span>{{ l().moneyDiscount }}</span><span class="leader !border-current"></span><span>− {{ rupees(b.discountAmount) }}</span></p>
                }
                @if (b.paidAmount > 0) {
                  <p class="flex gap-2.5 text-accent-700"><span>{{ paidWith() ? l().moneyPaidWith(paidWith()!) : l().moneyPaid }}</span><span class="leader !border-current"></span><span>− {{ rupees(b.paidAmount) }}</span></p>
                }
                <p class="mt-1.5 flex gap-2.5 border-t border-text pt-1.5 font-semibold">
                  <span>{{ l().moneyPayAtVenue }}</span><span class="flex-1"></span><span>{{ rupees(left(b)) }}</span>
                </p>
                <p class="mt-3.5 text-[15px] leading-6 text-text/75">
                  @if (split(); as s) {
                    <strong class="font-semibold text-text">{{ l().splitPaid(settled(s), s.players.length) }}</strong>
                  }
                  {{ cancelLine(b) }}
                </p>
              </div>
            </div>

            <div class="mt-5 flex gap-2 lg:mt-9 lg:gap-2.5">
              @if (b.status === 'PENDING_PAYMENT' && !checking().has(b.id)) {
                <a [routerLink]="['/bookings', b.id]" class="btn-primary flex-1 lg:flex-none">
                  <fs-icon [img]="icons.Wallet" [size]="18" />{{ l().payTheAdvance }}
                </a>
              } @else if (owing(); as n) {
                <button type="button" (click)="remind(b)" class="btn-primary flex-1 lg:flex-none">
                  <fs-icon [img]="icons.ShareNetwork" [size]="18" class="max-lg:hidden!" />
                  <span class="lg:hidden">{{ l().remindShort(n) }}</span><span class="hidden lg:inline">{{ l().remindWhoOwe(n) }}</span>
                </button>
              } @else {
                <a [routerLink]="['/bookings', b.id, 'split']" class="btn-primary flex-1 lg:flex-none">
                  <fs-icon [img]="icons.ShareNetwork" [size]="18" class="max-lg:hidden!" />{{ l().splitTheCost }}
                </a>
              }
              @if (directions(); as url) {
                <a [href]="url" target="_blank" rel="noopener" class="btn-secondary">
                  <fs-icon [img]="icons.MapPin" [size]="18" />{{ l().directions }}
                </a>
              }
              <a [routerLink]="['/bookings', b.id]" class="btn-ghost max-lg:hidden!">{{ l().bookingDetails }}</a>
            </div>
            @if (copied()) {
              <p class="mt-3 text-sm text-accent-700" role="status">{{ l().reminderCopied }}</p>
            }
          } @else {
            <p class="kicker mb-[18px]">{{ l().yourNextGame }}</p>
            <h1 class="ml-[-0.03em] text-[40px] leading-[44px] tracking-[-0.02em] lg:ml-[-0.035em] lg:text-[72px] lg:leading-[76px] lg:tracking-[-0.025em]">
              {{ l().nothingBooked }}<span class="font-normal italic">{{ l().findOneTonight }}</span>
            </h1>
            <p class="mt-4 max-w-[50ch] text-[17px] leading-7 text-text/80 lg:text-[19px] lg:leading-[30px]">{{ l().nothingBookedBody }}</p>
            <a routerLink="/explore" class="btn-primary mt-8">{{ l().findACourt }}</a>
          }
        </div>

        <!-- What's waiting on the player, most urgent first -->
        <aside aria-labelledby="needs-title">
          <h2 id="needs-title" class="text-[22px] leading-7 lg:text-[28px] lg:leading-8">{{ l().needsYou }}</h2>
          <p class="mt-1 mb-4 hidden text-[15px] text-text/70 lg:mt-2 lg:block">{{ l().needsCount(needs().length) }}</p>
          @if (needs().length === 0) {
            <p class="border-t border-divider py-4 text-[15px] text-text/70 lg:hidden">{{ l().needsCount(0) }}</p>
          }
          <ul>
            @for (need of needs(); track need.link + need.title; let i = $index) {
              <li [class.max-lg:hidden]="i >= 3">
                <a [routerLink]="need.link" class="group/need flex min-h-11 gap-3 border-t border-divider py-3 text-text hover:text-accent-700 lg:gap-3.5 lg:py-4">
                  <fs-icon [img]="need.icon" [size]="22" class="mt-0.5 lg:mt-[3px]"
                    [class]="need.ink === 'act' ? 'text-accent-2-700' : need.ink === 'progress' ? 'text-accent-700' : 'text-text/60'" />
                  <span class="min-w-0 flex-1 transition-transform duration-200 group-hover/need:translate-x-1">
                    <span class="block text-[15px] leading-[22px] font-semibold lg:text-base lg:leading-6">{{ need.title }}</span>
                    <span class="block text-[13px] leading-5 text-text/70 lg:text-sm lg:leading-[22px]">{{ need.detail }}</span>
                  </span>
                  <span class="tabular text-[15px] leading-[22px] lg:text-base lg:leading-6">{{ need.amount }}</span>
                </a>
              </li>
            }
          </ul>
        </aside>
      </section>

      <!-- Venues to book, so a game is a tap away from home too: featured ones first -->
      @if (venues().length) {
        <section class="pt-2 pb-12 lg:pt-6 lg:pb-16" aria-labelledby="venues-title">
          <div class="mb-6 flex items-baseline justify-between gap-4 lg:mb-7">
            <h2 id="venues-title" class="kicker">{{ l().homeVenues }}</h2>
            <a routerLink="/explore" class="text-[15px] text-accent-700 hover:text-accent-800">{{ l().allVenuesLink }}</a>
          </div>
          <ul class="grid gap-x-12 gap-y-12 sm:grid-cols-2 lg:grid-cols-3 lg:gap-y-14">
            @for (venue of venues(); track venue.id; let i = $index) {
              <li [class.max-sm:hidden]="i >= 3"><fs-venue-card [venue]="venue" /></li>
            }
          </ul>
        </section>
      }

      <!-- Second row: this week, and games looking for players -->
      <section class="hidden gap-[72px] pt-6 pb-12 lg:grid lg:grid-cols-[minmax(0,7fr)_minmax(0,4fr)]">
        <div>
          <div class="mb-3 flex items-baseline justify-between">
            <h2 class="text-[28px] leading-8">{{ l().thisWeek }}</h2>
            <a routerLink="/bookings" class="text-[15px] text-accent-700 hover:text-accent-800">{{ l().allBookingsLink }}</a>
          </div>
          @if (week().length) {
            <table class="w-full border-collapse text-[15px]">
              <thead>
                <tr class="kicker !text-[11px]">
                  <th class="border-b border-divider p-2.5 text-left font-normal">{{ l().colDay }}</th>
                  <th class="border-b border-divider p-2.5 text-left font-normal">{{ l().colVenue }}</th>
                  <th class="border-b border-divider p-2.5 text-left font-normal">{{ l().colTime }}</th>
                  <th class="border-b border-divider p-2.5 text-left font-normal">{{ l().colStatus }}</th>
                  <th class="border-b border-divider p-2.5 text-right font-normal">{{ l().colTotal }}</th>
                </tr>
              </thead>
              <tbody>
                @for (b of week(); track b.id) {
                  <tr class="hover:bg-text/4">
                    <td class="border-b border-text/8 p-2.5 font-semibold">{{ dayShort(b) }}</td>
                    <td class="border-b border-text/8 p-2.5"><a [routerLink]="['/bookings', b.id]" class="hover:text-accent-700">{{ b.venue.name }}</a></td>
                    <td class="tabular border-b border-text/8 p-2.5">{{ time(b) }}</td>
                    <td class="border-b border-text/8 p-2.5"><fs-status-chip [value]="chip(b)" /></td>
                    <td class="tabular border-b border-text/8 p-2.5 text-right">{{ rupees(b.totalAmount) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="border-t border-divider py-4 text-[15px] text-text/70">{{ l().nothingElseThisWeek }}</p>
          }
        </div>
        <div>
          <h2 class="mb-3 text-[28px] leading-8">{{ l().gamesLookingForPlayers }}</h2>
          @for (game of openGames(); track game.id) {
            <div class="flex items-center gap-4 border-t border-divider py-3.5">
              <span class="min-w-0 flex-1">
                <span class="block text-base leading-6 font-semibold">{{ game.venue?.name ?? game.city }}, {{ gameWhen(game) }}</span>
                <span class="block text-sm leading-[22px] text-text/70">{{ gameDetail(game) }}</span>
              </span>
              <a [routerLink]="['/matches', game.id]" class="btn-secondary">{{ l().join }}</a>
            </div>
          } @empty {
            <p class="border-t border-divider py-4 text-[15px] text-text/70">{{ l().noGamesLooking }}</p>
          }
        </div>
      </section>

      @if (fixture(); as f) {
        <section class="hidden pt-8 pb-4 lg:block">
          <p class="kicker mb-3.5">{{ f.kicker }}</p>
          <p class="max-w-[30ch] font-display text-[32px] leading-10 tracking-[-0.01em]">
            {{ l().playsAgainst(f.team, f.other) }}<span class="italic">{{ l().onDayAt(f.when, f.venue) }}</span>
          </p>
          <a [routerLink]="['/tournaments', f.slug]" class="mt-3.5 inline-block text-[15px] text-accent-700 hover:text-accent-800">{{ l().fixturesAndTable }}</a>
        </section>
      }
    </div>
  `,
  styles: `
    .btn-primary,
    .btn-secondary,
    .btn-ghost {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      min-height: 44px;
      padding-inline: 20px;
      border: 1px solid transparent;
      border-radius: 2px;
      font-size: 15px;
      font-weight: 600;
      line-height: 1.2;
    }
    .btn-primary {
      background: var(--color-accent);
      color: var(--color-bg);
    }
    .btn-primary:hover {
      background: var(--color-accent-600);
    }
    .btn-primary:active {
      background: var(--color-accent-700);
    }
    .btn-secondary {
      border-color: var(--color-divider);
    }
    .btn-secondary:hover {
      background: rgb(32 30 29 / 0.07);
    }
    .btn-ghost {
      padding-inline: 14px;
      color: var(--color-accent-700);
    }
    .btn-ghost:hover {
      background: rgb(0 136 176 / 0.1);
    }
  `,
})
export class Home {
  protected readonly l = l;

  protected readonly rupees = rupees;

  protected readonly icons = { MapPin, ShareNetwork, Wallet };

  private readonly api = inject(Api);

  private readonly session = inject(Session);

  /** Upcoming bookings, soonest first; null while loading. */
  protected readonly bookings = signal<Booking[] | null>(null);

  /** Bookings with a payment waiting to be checked. */
  protected readonly checking = signal<Set<string>>(new Set());

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly split = signal<Split | null>(null);

  protected readonly deposit = signal<Deposit | null>(null);

  private readonly dues = signal<OwedShare[]>([]);

  private readonly waitlist = signal<WaitlistEntry[]>([]);

  private readonly unreviewed = signal<Booking[]>([]);

  protected readonly openGames = signal<MatchPost[]>([]);

  /** Six venues to book: featured ones first, then the rest as the API orders them. */
  protected readonly venues = signal<VenueSummary[]>([]);

  protected readonly fixture = signal<Fixture | null>(null);

  protected readonly copied = signal(false);

  /** Whether the next game is at the court's peak price. */
  protected readonly peak = signal(false);

  /** How the advance for the next game was paid: "eSewa". */
  protected readonly paidWith = signal<string | null>(null);

  /** Ticks every half minute, for the countdown. */
  private readonly now = signal(new Date());

  constructor() {
    const timer = setInterval(() => this.now.set(new Date()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
    void this.load();
  }

  protected readonly next = computed(() =>
    this.bookings()?.find((b) => (b.status === 'CONFIRMED' || b.status === 'PENDING_PAYMENT') && new Date(b.endsAt) > this.now()) ?? null);

  /** The next seven days' bookings, the lead one included. */
  protected readonly week = computed(() => {
    const until = this.now().getTime() + 7 * 86_400_000;
    return (this.bookings() ?? []).filter((b) => new Date(b.startsAt).getTime() < until);
  });

  protected readonly greeting = computed(() => {
    const name = this.session.user()?.fullName.split(' ')[0] ?? '';
    const hour = Number(new Intl.DateTimeFormat('en-GB', { hour: 'numeric', hourCycle: 'h23', timeZone: VALLEY }).format(this.now()));
    return hour < 12 ? l().greetMorning(name) : hour < 17 ? l().greetAfternoon(name) : l().greetEvening(name);
  });

  protected readonly longDate = computed(() => longDate(today(VALLEY, this.now())));

  protected readonly shortDate = computed(() => dayLabel(today(VALLEY, this.now())));

  /** "2:14" and "hours to kick-off"; a day or more away, "3" and "days to kick-off". */
  protected readonly countdown = computed(() => {
    const b = this.next();
    const minutes = b ? Math.max(0, Math.round((new Date(b.startsAt).getTime() - this.now().getTime()) / 60_000)) : 0;
    if (minutes >= 24 * 60) {
      const days = Math.floor(minutes / (24 * 60));
      return { figure: digits(days), label: l().daysToKickOff(days) };
    }
    return { figure: digits(`${Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, '0')}`), label: l().hoursToKickOff };
  });

  /** Split players who haven't settled, when the player booked it and so collects. */
  protected readonly owing = computed(() => {
    const s = this.split();
    const unpaid = s?.youAreBooker ? s.players.filter((p) => !p.settled && !p.booker).length : 0;
    return unpaid > 0 ? unpaid : null;
  });

  protected readonly directions = computed(() => {
    const v = this.venue();
    return v?.latitude != null && v.longitude != null
      ? `https://www.google.com/maps/dir/?api=1&destination=${v.latitude},${v.longitude}` : null;
  });

  /** Most urgent first: what's theirs to pay, then what's being checked or waited on, then reviews. */
  protected readonly needs = computed<Need[]>(() => {
    const held = (this.bookings() ?? []).filter((b) => b.status === 'PENDING_PAYMENT');
    return [
      ...held.filter((b) => !this.checking().has(b.id)).map((b) => ({
        icon: Wallet, ink: 'act' as const, title: l().needPayAdvance,
        detail: l().needPayAdvanceDetail(b.venue.name, this.shortWhen(b.startsAt), b.holdExpiresAt ? localClock(b.holdExpiresAt) : ''),
        amount: rupees(Math.max(0, b.advanceRequired - b.paidAmount)), link: `/bookings/${b.id}`,
      })),
      ...this.dues().map((d) => ({
        icon: Coins, ink: 'act' as const, title: l().needPayShare,
        detail: l().needPayShareDetail(d.venueName, this.shortWhen(d.startsAt), d.payTo.name.split(' ')[0]),
        amount: rupees(d.amount), link: `/bookings/${d.bookingId}/split`,
      })),
      ...held.filter((b) => this.checking().has(b.id)).map((b) => ({
        icon: Wallet, ink: 'progress' as const, title: l().needChecking,
        detail: `${b.venue.name}, ${this.shortWhen(b.startsAt)}`, amount: rupees(b.advanceRequired), link: `/bookings/${b.id}`,
      })),
      ...this.waitlist().map((w) => ({
        icon: Hourglass, ink: w.status === 'NOTIFIED' ? 'act' as const : 'progress' as const,
        title: w.status === 'NOTIFIED' ? l().needWaitlistFree : l().needWaitlist,
        detail: `${w.venueName}, ${this.shortWhen(w.startsAt)}`, amount: '', link: '/waitlist',
      })),
      ...this.unreviewed().map((b) => ({
        icon: Star, ink: 'optional' as const, title: l().needReview(b.venue.name),
        detail: l().needReviewDetail(dayLabel(b.startsAt.slice(0, 10))), amount: '', link: '/reviews',
      })),
    ];
  });

  protected chip(b: Booking) {
    return bookingChip(b, this.checking().has(b.id));
  }

  /** "Tonight, 6 PM", "Today, 7 AM", "Tomorrow, 6 PM", "Sat 1 Nov, 6 PM". */
  protected when(b: Booking): string {
    const day = b.startsAt.slice(0, 10);
    const hour = Number(b.startsAt.slice(11, 13));
    const today_ = today(VALLEY, this.now());
    const word = day === today_ ? (hour >= 17 ? l().whenTonight : l().today) : day === this.tomorrow() ? l().tomorrow : dayLabel(day);
    return `${word}, ${localClock(b.startsAt)}`;
  }

  /** "Dhuku" for "Dhuku Futsal Hub": the name without the words every venue has, as people say it. */
  protected shortName(name: string): string {
    const short = name.split(/\s+/).filter((word) => !GENERIC.has(word.toLowerCase())).join(' ');
    return short || name;
  }

  /** "Dhuku Futsal Hub, Sital Marg, Maharajgunj · Court A · 6 PM – 7 PM". */
  protected place(b: Booking): string {
    return [[b.venue.name, this.street()].filter(Boolean).join(', '), b.court.name, this.time(b)].join(' · ');
  }

  /** On phones: "Court A · Sital Marg, Maharajgunj". */
  protected placeShort(b: Booking): string {
    return [b.court.name, this.street()].filter(Boolean).join(' · ');
  }

  private street(): string {
    const v = this.venue();
    return v ? [v.addressLine, v.area].filter(Boolean).join(', ') : '';
  }

  protected time(b: Booking): string {
    return `${localClock(b.startsAt)} – ${localClock(b.endsAt)}`;
  }

  protected dayShort(b: Booking): string {
    return dayLabel(b.startsAt.slice(0, 10));
  }

  protected extras(b: Booking): string {
    return b.addOns.length ? b.addOns.map((a) => a.name).join(', ') : l().moneyExtras;
  }

  /** What's left for the venue: the total less what's been paid. */
  protected left(b: Booking): number {
    return Math.max(0, b.totalAmount - b.paidAmount);
  }

  protected settled(s: Split): number {
    return s.players.filter((p) => p.settled || p.booker).length;
  }

  /** The exact fee for cancelling now, and when it stopped being free. */
  protected cancelLine(b: Booking): string {
    const v = this.venue();
    if (!v) {
      return '';
    }
    const outcome = cancelOutcome(b, v.policy, this.now());
    const freeHours = Math.max(0, ...v.policy.cancellationTiers.map((t) => t.withinHours));
    const freeUntil = freeHours > 0 ? new Date(new Date(b.startsAt).getTime() - freeHours * 3_600_000).toISOString() : null;
    const untilLabel = freeUntil ? this.relative(this.inValley(freeUntil)) : null;
    return outcome.fee > 0 ? l().cancelNowCosts(rupees(outcome.fee), untilLabel) : l().cancelFree(untilLabel);
  }

  protected gameWhen(game: MatchPost): string {
    return this.shortWhen(game.startsAt);
  }

  protected gameDetail(game: MatchPost): string {
    const spots = game.playersNeeded !== null ? Math.max(0, game.playersNeeded - game.placesTaken) : null;
    return [
      game.skillLevel ? skillLabels()[game.skillLevel] : null,
      spots !== null ? l().spotsLeft(spots) : null,
      game.costPerPlayer !== null ? l().eachCost(rupees(game.costPerPlayer)) : null,
    ].filter(Boolean).join(' · ');
  }

  /** Shares the reminder, or copies it where sharing isn't there. */
  protected async remind(b: Booking): Promise<void> {
    const text = l().remindText(b.venue.name, this.when(b));
    const url = `${location.origin}/bookings/${b.id}/split`;
    try {
      if (navigator.share) {
        await navigator.share({ text, url });
        return;
      }
      await navigator.clipboard.writeText(`${text} ${url}`);
      this.copied.set(true);
    } catch {
      // Dismissed: nothing to do
    }
  }

  private tomorrow(): string {
    return today(VALLEY, new Date(this.now().getTime() + 86_400_000));
  }

  /** "18:00 yesterday", "18:00 today", "18:00 tomorrow", else "Sun 18:00". */
  private relative(iso: string): string {
    const day = iso.slice(0, 10);
    const today_ = today(VALLEY, this.now());
    const yesterday = today(VALLEY, new Date(this.now().getTime() - 86_400_000));
    const word = day === today_ ? l().wordToday : day === yesterday ? l().wordYesterday : day === this.tomorrow() ? l().wordTomorrow : null;
    return word ? l().timeOnDay(localClock(iso), word) : this.shortWhen(iso);
  }

  /** "Sun, 7 AM": a day and time for a list row (venue-local times carry their offset). */
  private shortWhen(iso: string): string {
    return `${weekdayShort(iso.slice(0, 10))} ${localClock(iso)}`;
  }

  /** An instant as the valley's local time with its offset, the way the API writes them. */
  private inValley(instant: string): string {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: VALLEY, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    }).formatToParts(new Date(instant));
    const get = (type: string) => parts.find((p) => p.type === type)?.value ?? '00';
    return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}:00+05:45`;
  }

  private async load(): Promise<void> {
    try {
      const upcoming = (await this.api.bookings('upcoming')).sort((a, b) => a.startsAt.localeCompare(b.startsAt));
      const held = upcoming.filter((b) => b.status === 'PENDING_PAYMENT');
      const checks = await Promise.all(held.map((b) => this.api.bookingPayments(b.id).catch(() => null)));
      this.checking.set(new Set(held.filter((_, i) => checks[i]?.payments.some((p) => p.status === 'PENDING')).map((b) => b.id)));
      this.bookings.set(upcoming);
    } catch {
      this.bookings.set([]);
    }
    const b = this.next();
    if (b) {
      this.api.venue(b.venue.slug).then((v) => this.venue.set(v), () => undefined);
      this.api.split(b.id).then((s) => this.split.set(s), () => undefined);
      this.api.availability(b.venue.slug, b.startsAt.slice(0, 10)).then((a) => {
        const slot = a.courts.find((c) => c.courtId === b.court.id)?.slots.find((x) => x.startsAt === b.startsAt);
        this.peak.set(slot?.label === 'PEAK');
      }, () => undefined);
      if (b.paidAmount > 0) {
        this.api.bookingPayments(b.id).then((p) => {
          const paid = p.payments.find((x) => x.status === 'SUCCEEDED');
          this.paidWith.set(paid ? methodLabel(paid.method) : null);
        }, () => undefined);
      }
    }
    this.api.deposit().then((d) => this.deposit.set(d), () => undefined);
    this.api.splitDues().then((d) => this.dues.set(d), () => undefined);
    this.api.waitlist().then((w) => this.waitlist.set(w.filter((e) => e.status === 'WAITING' || e.status === 'NOTIFIED')), () => undefined);
    this.api.toReview().then((r) => this.unreviewed.set(r.slice(0, 2)), () => undefined);
    this.api.matchPosts({ type: 'NEED_PLAYERS' }).then(
      (page) => this.openGames.set(page.content.filter((p) => !p.mine && p.status === 'OPEN').slice(0, 3)), () => undefined);
    void this.loadFixture();
    void this.loadVenues();
  }

  private async loadVenues(): Promise<void> {
    const [featured, page] = await Promise.all([this.api.featured().catch(() => []), this.api.venues({ size: 6 }).catch(() => null)]);
    const rest = (page?.content ?? []).filter((v) => !featured.some((f) => f.id === v.id));
    this.venues.set([...featured, ...rest].slice(0, 6));
  }

  /** The next scheduled game for one of the player's teams, in a tournament they're in. */
  private async loadFixture(): Promise<void> {
    try {
      const entries = await this.api.myEntries();
      for (const { tournament, entry } of entries) {
        if (tournament.status === 'COMPLETED' || tournament.status === 'CANCELLED') {
          continue;
        }
        const detail = await this.api.tournament(tournament.slug);
        const game = detail.matches
          .filter((m) => m.status === 'SCHEDULED' && m.scheduledAt && (m.home?.registrationId === entry.id || m.away?.registrationId === entry.id))
          .sort((a, b) => (a.scheduledAt ?? '').localeCompare(b.scheduledAt ?? ''))[0];
        if (game?.scheduledAt && game.home && game.away) {
          const ours = game.home.registrationId === entry.id ? game.home : game.away;
          const theirs = ours === game.home ? game.away : game.home;
          this.fixture.set({
            kicker: [tournament.name, entry.groupName].filter(Boolean).join(' · '),
            team: ours.name, other: theirs.name,
            when: `${dayLabel(game.scheduledAt.slice(0, 10))}, ${localClock(game.scheduledAt)}`,
            venue: tournament.venue.name, slug: tournament.slug,
          });
          return;
        }
      }
    } catch {
      // No tournament line
    }
  }
}
