package com.futslot.deposit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingEventType;
import com.futslot.booking.BookingLookup;
import com.futslot.common.ApiException;
import com.futslot.payment.PaymentMethod;
import com.futslot.user.User;
import com.futslot.user.UserService;

/**
 * Players' booking deposits. FutSlot holds a fixed {@link #AMOUNT} per player; venues that require it only take
 * app and website bookings from players whose deposit is full. When a player cancels such a booking late or
 * doesn't show up, the part of the venue's fee their advance doesn't cover comes out of the deposit and is paid
 * to the venue at the end of the month. Players can take the deposit back once none of their bookings rely on
 * it; FutSlot sends it {@link #WITHDRAWAL_WAIT} later.
 */
@Service
public class DepositService {

	public static final BigDecimal AMOUNT = new BigDecimal("1000.00");

	static final Duration WITHDRAWAL_WAIT = Duration.ofDays(3);

	private static final Set<PaymentMethod> SEND_METHODS = EnumSet.of(PaymentMethod.ESEWA, PaymentMethod.KHALTI,
			PaymentMethod.BANK_TRANSFER);

	private final DepositWalletRepository wallets;

	private final DepositEntryRepository entries;

	private final WithdrawalRepository withdrawals;

	private final BookingLookup bookings;

	private final UserService users;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	DepositService(DepositWalletRepository wallets, DepositEntryRepository entries, WithdrawalRepository withdrawals,
			BookingLookup bookings, UserService users, ApplicationEventPublisher events, Clock clock) {
		this.wallets = wallets;
		this.entries = entries;
		this.withdrawals = withdrawals;
		this.bookings = bookings;
		this.users = users;
		this.events = events;
		this.clock = clock;
	}

	// Booking

	/**
	 * Before booking a venue that requires the deposit: refuses unless it's full. Locks the wallet until commit,
	 * so a withdrawal can't slip in between this check and the booking being saved.
	 */
	@Transactional
	public void requireFull(UUID userId) {
		BigDecimal balance = this.wallets.lock(userId).map(DepositWallet::getBalance).orElse(BigDecimal.ZERO);
		if (balance.compareTo(AMOUNT) < 0) {
			throw new ApiException(HttpStatus.CONFLICT, "DEPOSIT_REQUIRED",
					"This venue needs your Rs 1,000 FutSlot booking deposit. Add Rs %,d to your deposit to book"
						.formatted(AMOUNT.subtract(balance).setScale(0, RoundingMode.CEILING).intValueExact()));
		}
	}

	/** What the player must add to fill their deposit; 0 when it's full. */
	@Transactional(readOnly = true)
	public BigDecimal toAdd(UUID userId) {
		return AMOUNT.subtract(balanceOf(userId)).max(BigDecimal.ZERO);
	}

	/**
	 * Late cancellations and no-shows of deposit-backed bookings: the fee (a share of the booking price) is taken
	 * from what the player paid first; the deposit covers the rest, as far as it goes. Runs inside the booking's
	 * transaction.
	 */
	@EventListener
	void onBookingEnded(BookingEvent event) {
		boolean noShow = event.type() == BookingEventType.NO_SHOW;
		boolean byPlayer = event.type() == BookingEventType.CANCELLED && event.actorUserId() != null
				&& event.actorUserId().equals(event.playerUserId());
		if (!noShow && !byPlayer) {
			return;
		}
		Booking booking = this.bookings.byId(event.bookingId());
		if (!booking.isDepositBacked() || booking.getCancellationFeePercent() == 0) {
			return;
		}
		BigDecimal fee = booking.getTotalAmount()
			.multiply(BigDecimal.valueOf(booking.getCancellationFeePercent()))
			.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal uncovered = fee.subtract(fee.min(booking.getPaidAmount()));
		if (uncovered.signum() <= 0) {
			return;
		}
		DepositWallet wallet = this.wallets.lock(booking.getBookedByUserId()).orElse(null);
		BigDecimal taken = (wallet != null) ? uncovered.min(wallet.getBalance()) : BigDecimal.ZERO;
		if (taken.signum() <= 0) {
			return;
		}
		this.entries.save(DepositEntry.fee(wallet, taken, booking));
		String what = noShow ? "not showing up to booking " + booking.getReference()
				: "cancelling booking " + booking.getReference() + " late (" + booking.getCancellationFeePercent()
						+ "% fee)";
		this.events.publishEvent(new DepositEvent(DepositEvent.Type.FEE_TAKEN, wallet.getUserId(), taken,
				wallet.getBalance(), booking.getVenue().getName(), what));
	}

	// Top-ups, reported and checked like other payments to FutSlot

	/** What a top-up must be: exactly enough to fill the deposit. */
	@Transactional(readOnly = true)
	public BigDecimal topUpDue(UUID userId) {
		if (this.withdrawals.findByUserIdAndStatus(userId, WithdrawalStatus.REQUESTED).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "WITHDRAWAL_PENDING",
					"You're taking your deposit back. Cancel that first to top up");
		}
		BigDecimal due = toAdd(userId);
		if (due.signum() == 0) {
			throw new ApiException(HttpStatus.CONFLICT, "DEPOSIT_FULL", "Your deposit is already full");
		}
		return due;
	}

	/** An admin found the money the player sent. */
	@Transactional
	public void toppedUp(UUID userId, UUID paymentId, BigDecimal amount) {
		this.wallets.createIfMissing(userId);
		DepositWallet wallet = this.wallets.lock(userId).orElseThrow();
		this.entries.save(DepositEntry.topUp(wallet, amount, paymentId));
		this.events.publishEvent(new DepositEvent(DepositEvent.Type.TOPPED_UP, userId, amount, wallet.getBalance(),
				null, null));
	}

	// Players

	@Transactional(readOnly = true)
	DepositViews.Deposit forPlayer(UUID userId) {
		BigDecimal balance = balanceOf(userId);
		DepositViews.Withdrawal open = this.withdrawals.findByUserIdAndStatus(userId, WithdrawalStatus.REQUESTED)
			.map(w -> DepositViews.Withdrawal.from(w, null, null))
			.orElse(null);
		List<DepositViews.Entry> history = this.entries.findByUserIdOrderByCreatedAtDesc(userId, Limit.of(50))
			.stream()
			.map(DepositViews.Entry::from)
			.toList();
		return new DepositViews.Deposit(AMOUNT, balance, AMOUNT.subtract(balance).max(BigDecimal.ZERO), false, null,
				open, history);
	}

	/** Takes the whole balance out now; FutSlot sends it once the wait is over. */
	@Transactional
	DepositViews.Withdrawal requestWithdrawal(UUID userId, DepositRequests.Withdraw request) {
		if (!SEND_METHODS.contains(request.method())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_METHOD", "Choose eSewa, Khalti or bank transfer");
		}
		DepositWallet wallet = this.wallets.lock(userId).orElse(null);
		if (this.withdrawals.findByUserIdAndStatus(userId, WithdrawalStatus.REQUESTED).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "WITHDRAWAL_PENDING", "You've already asked for your deposit back");
		}
		if (wallet == null || wallet.getBalance().signum() == 0) {
			throw new ApiException(HttpStatus.CONFLICT, "NOTHING_TO_WITHDRAW", "Your deposit is empty");
		}
		// Checked with the wallet locked, so no deposit booking can be made meanwhile
		if (this.bookings.hasLiveDepositBookings(userId)) {
			throw new ApiException(HttpStatus.CONFLICT, "DEPOSIT_IN_USE",
					"Your deposit covers bookings you haven't played yet. You can take it back once they're played or cancelled");
		}
		Withdrawal withdrawal = this.withdrawals.save(new Withdrawal(userId, wallet.getBalance(), request.method(),
				request.sendTo().strip(), this.clock.instant().plus(WITHDRAWAL_WAIT)));
		this.entries.save(DepositEntry.withdrawal(wallet, withdrawal));
		return DepositViews.Withdrawal.from(withdrawal, null, null);
	}

	@Transactional
	DepositViews.Withdrawal cancelWithdrawal(UUID userId, UUID withdrawalId) {
		DepositWallet wallet = this.wallets.lock(userId).orElseThrow(DepositService::withdrawalNotFound);
		Withdrawal withdrawal = this.withdrawals.findByIdAndUserId(withdrawalId, userId)
			.orElseThrow(DepositService::withdrawalNotFound);
		if (withdrawal.getStatus() != WithdrawalStatus.REQUESTED) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_WITHDRAWAL_STATUS",
					"This withdrawal is " + withdrawal.getStatus());
		}
		withdrawal.cancel();
		this.entries.save(DepositEntry.withdrawalCancelled(wallet, withdrawal));
		return DepositViews.Withdrawal.from(withdrawal, null, null);
	}

	// Admins

	@Transactional(readOnly = true)
	List<DepositViews.Withdrawal> withdrawals(WithdrawalStatus status) {
		return this.withdrawals.findByStatusOrderByDueAt(status).stream().map(this::forAdmin).toList();
	}

	/** FutSlot sent the money; only once the wait is over, in case a fee was still to come. */
	@Transactional
	DepositViews.Withdrawal markWithdrawalPaid(UUID adminId, UUID withdrawalId, String reference) {
		Withdrawal withdrawal = this.withdrawals.findById(withdrawalId).orElseThrow(DepositService::withdrawalNotFound);
		if (withdrawal.getStatus() != WithdrawalStatus.REQUESTED) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_WITHDRAWAL_STATUS",
					"This withdrawal is " + withdrawal.getStatus());
		}
		Instant now = this.clock.instant();
		if (now.isBefore(withdrawal.getDueAt())) {
			throw new ApiException(HttpStatus.CONFLICT, "WITHDRAWAL_NOT_DUE", "This withdrawal can't be sent before "
					+ withdrawal.getDueAt());
		}
		withdrawal.paid(adminId, reference.strip(), now);
		this.events.publishEvent(new DepositEvent(DepositEvent.Type.WITHDRAWAL_PAID, withdrawal.getUserId(),
				withdrawal.getAmount(), balanceOf(withdrawal.getUserId()), null, withdrawal.getSendTo()));
		return forAdmin(withdrawal);
	}

	private DepositViews.Withdrawal forAdmin(Withdrawal withdrawal) {
		User player = this.users.get(withdrawal.getUserId());
		return DepositViews.Withdrawal.from(withdrawal, player.getFullName(), player.getPhone());
	}

	private BigDecimal balanceOf(UUID userId) {
		return this.wallets.findById(userId).map(DepositWallet::getBalance).orElse(BigDecimal.ZERO);
	}

	private static ApiException withdrawalNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "Withdrawal not found");
	}

}
