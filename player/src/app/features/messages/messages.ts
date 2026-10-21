import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ArrowLeft, CalendarDays, LucideAngularModule, MessageSquare, Search, SendHorizontal } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Booking, ChatMessage, ConversationSummary } from '../../core/api.types';
import { Badges } from '../../core/badges';
import { digits, l } from '../../i18n/i18n';
import { bookingChip } from '../../shared/booking-model';
import { ago, clockAt, dayLabel, localClock } from '../../shared/format';
import { StatusChip } from '../../shared/status-chip';
import { byDay, continues, merge } from './messages-model';

const ZONE = 'Asia/Kathmandu';

/** The API returns a page of this many; a full page means there may be older ones. */
const PAGE = 50;

const POLL_MS = 15_000;

/**
 * The player's conversations with venues, one thread per venue. Replies come signed with the venue's name. A
 * conversation starts from a venue's page or a booking ({@code ?venue=<id>&name=<venue>&booking=<id>}); if one
 * with that venue exists already, it carries on there.
 */
@Component({
  selector: 'fs-messages',
  imports: [LucideAngularModule, RouterLink, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './messages.html',
})
export class Messages {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly badges = inject(Badges);

  protected readonly icons = { ArrowLeft, CalendarDays, MessageSquare, Search, SendHorizontal };

  protected readonly fmt = { ago, dayLabel, localClock };

  protected readonly digits = digits;

  protected readonly chip = (booking: Booking) => bookingChip(booking);

  protected readonly conversations = signal<ConversationSummary[] | null>(null);

  protected readonly listError = signal<string | null>(null);

  protected readonly filter = signal('');

  private readonly params = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  /** Writing to a venue from its page or a booking: {@code id}, its {@code name} and the {@code booking} it's about. */
  protected readonly compose = computed(() => {
    const params = this.params();
    const id = params.get('venue');
    return id ? { id, name: params.get('name') ?? l().theVenue, booking: params.get('booking') } : null;
  });

  protected readonly selectedId = computed(() => {
    const compose = this.compose();
    if (compose) {
      return this.conversations()?.find((c) => c.venueId === compose.id)?.id ?? null;
    }
    return this.params().get('c');
  });

  /** A thread or a new message is open (on phones, the list is hidden then). */
  protected readonly open = computed(() => this.selectedId() !== null || this.compose() !== null);

  protected readonly selected = computed(() => this.conversations()?.find((c) => c.id === this.selectedId()) ?? null);

  protected readonly title = computed(() => this.selected()?.venueName ?? this.compose()?.name ?? '');

  protected readonly shown = computed(() => {
    const term = this.filter().trim().toLowerCase();
    const list = this.conversations() ?? [];
    return term ? list.filter((c) => c.venueName.toLowerCase().includes(term)) : list;
  });

  protected readonly messages = signal<ChatMessage[]>([]);

  protected readonly threadLoading = signal(false);

  protected readonly threadError = signal<string | null>(null);

  protected readonly hasEarlier = signal(false);

  protected readonly loadingEarlier = signal(false);

  protected readonly draft = signal('');

  protected readonly sending = signal(false);

  protected readonly sendError = signal<string | null>(null);

  /** Bookings that messages are about, fetched once each; null when it couldn't be loaded. */
  protected readonly bookings = signal<Record<string, Booking | null>>({});

  private readonly now = signal(new Date());

  protected readonly groups = computed(() => byDay(this.messages(), ZONE, this.now()));

  /** The player's latest message, if the venue has read it ("Seen"). */
  protected readonly seenId = computed(() => {
    const last = this.messages().filter((m) => m.fromYou).at(-1);
    return last?.readAt ? last.id : null;
  });

  protected readonly continues = continues;

  private readonly drafts = new Map<string, string>();

  private threadSequence = 0;

  constructor() {
    void this.loadList();
    effect(() => {
      const id = this.selectedId();
      untracked(() => void this.openThread(id));
    });
    const poll = () => {
      if (document.visibilityState === 'visible') {
        this.now.set(new Date());
        void this.loadList();
        void this.refreshThread();
      }
    };
    // Polls while the tab is in view, and catches up as soon as the player comes back to it
    const timer = setInterval(poll, POLL_MS);
    document.addEventListener('visibilitychange', poll);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', poll);
    });
  }

  protected select(conversation: ConversationSummary): void {
    void this.router.navigate([], { queryParams: { c: conversation.id } });
  }

  protected back(): void {
    void this.router.navigate([], { queryParams: {} });
  }

  protected time(message: ChatMessage): string {
    return clockAt(message.createdAt, ZONE);
  }

  protected typed(value: string): void {
    this.draft.set(value);
    this.sendError.set(null);
    const id = this.selectedId();
    if (id) {
      this.drafts.set(id, value);
    }
  }

  protected keydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      void this.send();
    }
  }

  protected async send(): Promise<void> {
    const compose = this.compose();
    const id = this.selectedId();
    const body = this.draft().trim();
    if ((!id && !compose) || this.sending()) {
      return;
    }
    if (!body) {
      this.sendError.set(l().typeMessageFirst);
      return;
    }
    this.sending.set(true);
    this.sendError.set(null);
    try {
      // From a venue or booking, the first message goes to the venue (tied to the booking); then it's a reply
      const sent = compose ? await this.api.messageVenue(compose.id, body, compose.booking ?? undefined)
        : await this.api.reply(id!, body);
      this.draft.set('');
      if (compose) {
        await this.loadList();
        const started = this.conversations()?.find((c) => c.venueId === compose.id);
        void this.router.navigate([], { queryParams: { c: started?.id ?? null }, replaceUrl: true });
        return;
      }
      this.messages.update((list) => merge(list, [sent]));
      this.drafts.delete(id!);
      this.conversations.update((list) => this.bump(list, id!, sent));
    }
    catch (error) {
      this.sendError.set(problemOf(error).message);
    }
    finally {
      this.sending.set(false);
    }
  }

  protected async earlier(): Promise<void> {
    const oldest = this.messages()[0];
    if (!oldest || this.loadingEarlier()) {
      return;
    }
    this.loadingEarlier.set(true);
    const sequence = this.threadSequence;
    try {
      const page = await this.fetch(this.selectedId()!, oldest.createdAt);
      if (sequence === this.threadSequence) {
        this.messages.update((list) => merge(list, page));
        this.hasEarlier.set(page.length === PAGE);
        this.loadBookings(page);
      }
    }
    catch (error) {
      this.threadError.set(problemOf(error).message);
    }
    finally {
      this.loadingEarlier.set(false);
    }
  }

  protected retryThread(): void {
    void this.openThread(this.selectedId());
  }

  protected retryList(): void {
    this.listError.set(null);
    void this.loadList();
  }

  private async loadList(): Promise<void> {
    try {
      const list = await this.api.conversations();
      // The open thread is read as messages arrive, so don't flash an unread count on it
      const open = this.selectedId();
      this.conversations.set(list.map((c) => (c.id === open && !this.threadError() ? { ...c, unread: 0 } : c)));
      this.listError.set(null);
    }
    catch (error) {
      if (!this.conversations()) {
        this.listError.set(problemOf(error).message);
      }
    }
  }

  private async openThread(id: string | null): Promise<void> {
    const sequence = ++this.threadSequence;
    this.messages.set([]);
    this.threadError.set(null);
    this.sendError.set(null);
    this.hasEarlier.set(false);
    this.draft.set(id ? this.drafts.get(id) ?? '' : '');
    if (!id) {
      this.threadLoading.set(false);
      return;
    }
    this.threadLoading.set(true);
    try {
      const page = await this.fetch(id);
      if (sequence !== this.threadSequence) {
        return;
      }
      this.messages.set(merge([], page));
      this.hasEarlier.set(page.length === PAGE);
      this.loadBookings(page);
      await this.markRead(id, page);
    }
    catch (error) {
      if (sequence === this.threadSequence) {
        this.threadError.set(problemOf(error).message);
      }
    }
    finally {
      if (sequence === this.threadSequence) {
        this.threadLoading.set(false);
      }
    }
  }

  /** Picks up new messages in the open thread. */
  private async refreshThread(): Promise<void> {
    const id = this.selectedId();
    if (!id || this.threadLoading() || this.threadError()) {
      return;
    }
    const sequence = this.threadSequence;
    try {
      const page = await this.fetch(id);
      if (sequence !== this.threadSequence) {
        return;
      }
      this.messages.update((list) => merge(list, page));
      this.loadBookings(page);
      await this.markRead(id, page);
    }
    catch {
      // The next poll tries again
    }
  }

  private fetch(id: string, before?: string): Promise<ChatMessage[]> {
    return this.api.messages(id, before);
  }

  private async markRead(id: string, page: ChatMessage[]): Promise<void> {
    const unread = page.some((m) => !m.fromYou && !m.readAt) || (this.conversations()?.find((c) => c.id === id)?.unread ?? 0) > 0;
    if (!unread) {
      return;
    }
    await this.api.readConversation(id);
    this.conversations.update((list) => list?.map((c) => (c.id === id ? { ...c, unread: 0 } : c)) ?? null);
    void this.badges.refresh();
  }

  private loadBookings(page: ChatMessage[]): void {
    const known = this.bookings();
    const wanted = [...new Set(page.map((m) => m.bookingId).filter((id): id is string => !!id && !(id in known)))];
    for (const bookingId of wanted) {
      this.bookings.update((all) => ({ ...all, [bookingId]: null }));
      this.api.booking(bookingId)
        .then((booking) => this.bookings.update((all) => ({ ...all, [bookingId]: booking })))
        .catch(() => undefined);
    }
  }

  /** Moves a conversation to the top with its newest message. */
  private bump(list: ConversationSummary[] | null, id: string, message: ChatMessage): ConversationSummary[] | null {
    const conversation = list?.find((c) => c.id === id);
    if (!list || !conversation) {
      return list;
    }
    const updated = { ...conversation, lastMessage: message.body, lastMessageAt: message.createdAt };
    return [updated, ...list.filter((c) => c.id !== id)];
  }
}
