package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.payment.PaymentMethod;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueRepository;

/**
 * Paying venues the deposit fees FutSlot collected for them. After each month ends, the month's fees become one
 * payout per venue; an admin sends the money to the venue and marks it paid.
 */
@Service
class PayoutService {

	private static final ZoneId NEPAL = ZoneId.of("Asia/Kathmandu");

	private static final Set<PaymentMethod> SEND_METHODS = EnumSet.of(PaymentMethod.ESEWA, PaymentMethod.KHALTI,
			PaymentMethod.BANK_TRANSFER);

	private final DepositEntryRepository entries;

	private final PayoutRepository payouts;

	private final VenueRepository venues;

	private final VenueAccess access;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	PayoutService(DepositEntryRepository entries, PayoutRepository payouts, VenueRepository venues, VenueAccess access,
			ApplicationEventPublisher events, Clock clock) {
		this.entries = entries;
		this.payouts = payouts;
		this.venues = venues;
		this.access = access;
		this.events = events;
		this.clock = clock;
	}

	/**
	 * Groups every fee from months that have ended, and isn't paid out yet, into one payout per venue and month.
	 * Safe to run any number of times.
	 */
	@Transactional
	int closeEndedMonths() {
		Instant monthStart = YearMonth.now(this.clock.withZone(NEPAL)).atDay(1).atStartOfDay(NEPAL).toInstant();
		Map<UUID, Map<YearMonth, List<DepositEntry>>> due = this.entries.findUnpaidFeesBefore(monthStart)
			.stream()
			.collect(Collectors.groupingBy(DepositEntry::getVenueId,
					Collectors.groupingBy(e -> YearMonth.from(e.getCreatedAt().atZone(NEPAL)), TreeMap::new,
							Collectors.toList())));
		int created = 0;
		for (Map.Entry<UUID, Map<YearMonth, List<DepositEntry>>> venue : due.entrySet()) {
			for (Map.Entry<YearMonth, List<DepositEntry>> month : venue.getValue().entrySet()) {
				BigDecimal total = month.getValue()
					.stream()
					.map(e -> e.getAmount().negate())
					.reduce(BigDecimal.ZERO, BigDecimal::add);
				LocalDate start = month.getKey().atDay(1);
				LocalDate end = month.getKey().atEndOfMonth();
				Payout existing = this.payouts.findByVenueIdAndPeriodStartAndPeriodEnd(venue.getKey(), start, end)
					.orElse(null);
				Payout payout;
				if (existing == null) {
					payout = this.payouts.save(new Payout(venue.getKey(), start, end, total));
					created++;
				}
				else if (existing.getStatus() == PayoutStatus.PENDING) {
					// A fee whose booking change committed just after the month was closed
					payout = existing;
					payout.add(total);
				}
				else {
					LocalDate last = month.getValue().getLast().getCreatedAt().atZone(NEPAL).toLocalDate();
					payout = this.payouts.save(new Payout(venue.getKey(), last, last, total));
					created++;
				}
				month.getValue().forEach(e -> e.paidOutIn(payout));
			}
		}
		return created;
	}

	@Transactional(readOnly = true)
	List<DepositViews.Payout> pending() {
		return this.payouts.findByStatusOrderByPeriodStartAscVenueId(PayoutStatus.PENDING)
			.stream()
			.map(this::view)
			.toList();
	}

	@Transactional
	DepositViews.Payout markPaid(UUID adminId, UUID payoutId, DepositRequests.MarkPayoutPaid request) {
		if (!SEND_METHODS.contains(request.method())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_METHOD", "Use eSewa, Khalti or bank transfer");
		}
		Payout payout = this.payouts.findById(payoutId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYOUT_NOT_FOUND", "Payout not found"));
		if (payout.getStatus() != PayoutStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_PAYOUT_STATUS", "This payout is " + payout.getStatus());
		}
		payout.paid(adminId, request.method(), request.reference().strip(), this.clock.instant());
		Venue venue = this.venues.findById(payout.getVenueId()).orElseThrow();
		this.events.publishEvent(new DepositEvent(DepositEvent.Type.PAYOUT_SENT, venue.getOwnerId(),
				payout.getNetAmount(), null, venue.getName(), "deposit fees in "
						+ YearMonth.from(payout.getPeriodStart()).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))));
		return view(payout, venue);
	}

	/** What FutSlot holds for the venue from this month (and any month not yet closed), and recent payouts. */
	@Transactional(readOnly = true)
	DepositViews.VenueFees forVenue(UUID userId, UUID venueId) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		List<DepositViews.Fee> fees = this.entries.findUnpaidFees(venueId).stream().map(DepositViews.Fee::from).toList();
		BigDecimal unpaid = fees.stream().map(DepositViews.Fee::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<DepositViews.Payout> history = new ArrayList<>();
		this.payouts.findTop12ByVenueIdOrderByPeriodStartDesc(venueId).forEach(p -> history.add(view(p, venue)));
		return new DepositViews.VenueFees(unpaid, fees, history);
	}

	private DepositViews.Payout view(Payout payout) {
		return view(payout, this.venues.findById(payout.getVenueId()).orElseThrow());
	}

	private static DepositViews.Payout view(Payout payout, Venue venue) {
		return new DepositViews.Payout(payout.getId(), venue.getId(), venue.getName(), payout.getPeriodStart(),
				payout.getPeriodEnd(), payout.getNetAmount(), payout.getStatus(), payout.getMethod(),
				payout.getTransferReference(), payout.getPaidAt(), venue.getPaymentInstructions());
	}

}
