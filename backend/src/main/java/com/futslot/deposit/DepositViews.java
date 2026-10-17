package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingStatus;
import com.futslot.payment.PaymentMethod;

/** API shapes for booking deposits. */
public final class DepositViews {

	private DepositViews() {
	}

	/**
	 * A player's deposit. {@code toAdd} is what they must send to fill it (0 when full).
	 * {@code topUpBeingChecked}: they've sent a top-up FutSlot hasn't checked yet. {@code paymentInstructions}
	 * says where to send it. {@code withdrawal} is the one waiting to be sent, or null.
	 */
	public record Deposit(BigDecimal required, BigDecimal balance, BigDecimal toAdd, boolean topUpBeingChecked,
			String paymentInstructions, Withdrawal withdrawal, List<Entry> history) {

		Deposit withTopUp(boolean beingChecked, String instructions) {
			return new Deposit(this.required, this.balance, this.toAdd, beingChecked, instructions, this.withdrawal,
					this.history);
		}

	}

	/** {@code bookingReference} and {@code venueName} are set for fees. */
	public record Entry(UUID id, DepositEntryType type, BigDecimal amount, BigDecimal balanceAfter,
			String bookingReference, String venueName, Instant createdAt) {

		static Entry from(DepositEntry entry) {
			Booking booking = entry.getBooking();
			return new Entry(entry.getId(), entry.getType(), entry.getAmount(), entry.getBalanceAfter(),
					(booking != null) ? booking.getReference() : null,
					(booking != null) ? booking.getVenue().getName() : null, entry.getCreatedAt());
		}

	}

	/** {@code playerName} and {@code playerPhone} are for admins, who send the money. */
	public record Withdrawal(UUID id, BigDecimal amount, PaymentMethod method, String sendTo,
			WithdrawalStatus status, Instant dueAt, String reference, Instant paidAt, Instant createdAt,
			String playerName, String playerPhone) {

		static Withdrawal from(com.futslot.deposit.Withdrawal withdrawal, String playerName, String playerPhone) {
			return new Withdrawal(withdrawal.getId(), withdrawal.getAmount(), withdrawal.getMethod(),
					withdrawal.getSendTo(), withdrawal.getStatus(), withdrawal.getDueAt(), withdrawal.getReference(),
					withdrawal.getPaidAt(), withdrawal.getCreatedAt(), playerName, playerPhone);
		}

	}

	/**
	 * A month of deposit fees owed to a venue. {@code venueName} and {@code paymentInstructions} (the venue's
	 * own wallet or account) are for admins sending it.
	 */
	public record Payout(UUID id, UUID venueId, String venueName, LocalDate periodStart, LocalDate periodEnd,
			BigDecimal amount, PayoutStatus status, PaymentMethod method, String reference, Instant paidAt,
			String paymentInstructions) {

	}

	/**
	 * Deposit fees for a venue: {@code unpaid} is what FutSlot holds for it and will pay out after the month
	 * ends; {@code fees} lists those charges.
	 */
	public record VenueFees(BigDecimal unpaid, List<Fee> fees, List<Payout> payouts) {

	}

	/**
	 * {@code noShow}: charged for a no-show rather than a late cancellation. {@code amount} is what came out of
	 * the deposit, on top of any advance the venue kept.
	 */
	public record Fee(UUID bookingId, String bookingReference, String customerName, LocalDate playedOn,
			boolean noShow, int feePercent, BigDecimal amount, Instant chargedAt) {

		static Fee from(DepositEntry entry) {
			Booking booking = entry.getBooking();
			LocalDate day = booking.getStartsAt().atZone(ZoneId.of(booking.getVenue().getTimezone())).toLocalDate();
			return new Fee(booking.getId(), booking.getReference(), booking.getCustomer().getName(), day,
					booking.getStatus() == BookingStatus.NO_SHOW, booking.getCancellationFeePercent(),
					entry.getAmount().negate(), entry.getCreatedAt());
		}

	}

}
