package com.futslot.discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingEventType;
import com.futslot.common.ApiException;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

/**
 * Venue promo codes for app bookings. A code counts as used while its booking is live; if the booking is
 * cancelled or expires the use is given back.
 */
@Service
public class DiscountCodeService {

	private final DiscountCodeRepository codes;

	private final DiscountRedemptionRepository redemptions;

	private final VenueAccess access;

	DiscountCodeService(DiscountCodeRepository codes, DiscountRedemptionRepository redemptions, VenueAccess access) {
		this.codes = codes;
		this.redemptions = redemptions;
		this.access = access;
	}

	@Transactional
	DiscountCodeResponse create(UUID userId, UUID venueId, DiscountCodeRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		validate(request);
		if (this.codes.existsByVenueIdAndCode(venueId, DiscountCode.normalize(request.code()))) {
			throw new ApiException(HttpStatus.CONFLICT, "CODE_TAKEN", "You already have a code " + request.code().strip());
		}
		return DiscountCodeResponse.from(this.codes.save(new DiscountCode(venueId, request, userId)));
	}

	/** Everything but the code and its type can change. */
	@Transactional
	DiscountCodeResponse update(UUID userId, UUID venueId, UUID codeId, DiscountCodeRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		validate(request);
		DiscountCode code = this.codes.findByIdAndVenueId(codeId, venueId).orElseThrow(DiscountCodeService::notFound);
		if (!code.getCode().equals(DiscountCode.normalize(request.code())) || code.getType() != request.type()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_IMMUTABLE",
					"A code's text and type can't change. Create a new code instead");
		}
		code.apply(request);
		return DiscountCodeResponse.from(code);
	}

	@Transactional(readOnly = true)
	List<DiscountCodeResponse> list(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		return this.codes.findByVenueIdOrderByCreatedAtDesc(venueId).stream().map(DiscountCodeResponse::from).toList();
	}

	/**
	 * The venue's code if {@code userId} may use it on a booking of {@code price} now. With {@code lock}
	 * the code row is locked so usage limits hold under concurrent bookings.
	 */
	@Transactional
	public DiscountCode usable(UUID venueId, String text, UUID userId, BigDecimal price, Instant now, boolean lock) {
		String normalized = DiscountCode.normalize(text);
		DiscountCode code = (lock ? this.codes.lockByVenueIdAndCode(venueId, normalized)
				: this.codes.findByVenueIdAndCode(venueId, normalized))
			.filter(DiscountCode::isActive)
			.orElseThrow(() -> invalid("INVALID_DISCOUNT_CODE", "That code isn't valid here"));
		if ((code.getValidFrom() != null && now.isBefore(code.getValidFrom()))
				|| (code.getValidTo() != null && now.isAfter(code.getValidTo()))) {
			throw invalid("DISCOUNT_NOT_VALID_NOW", "That code isn't valid right now");
		}
		if (code.getMinBookingAmount() != null && price.compareTo(code.getMinBookingAmount()) < 0) {
			throw invalid("DISCOUNT_MIN_AMOUNT",
					"That code needs a booking of at least Rs " + code.getMinBookingAmount().toPlainString());
		}
		if (code.getUsageLimit() != null && code.getUsedCount() >= code.getUsageLimit()) {
			throw invalid("DISCOUNT_USED_UP", "That code has been fully used");
		}
		if (code.getPerUserLimit() != null
				&& this.redemptions.countByDiscountCodeIdAndUserId(code.getId(), userId) >= code.getPerUserLimit()) {
			throw invalid("DISCOUNT_ALREADY_USED", "You've already used that code");
		}
		return code;
	}

	public static BigDecimal discountFor(DiscountCode code, BigDecimal price) {
		return code.discountOn(price);
	}

	/** Call with a code from {@link #usable} with {@code lock}, once the booking is saved. */
	@Transactional
	public void redeem(DiscountCode code, UUID bookingId, UUID userId, BigDecimal amount) {
		this.redemptions.save(new DiscountRedemption(code.getId(), bookingId, userId, amount));
		code.redeemed();
	}

	@EventListener
	void onBookingEnded(BookingEvent event) {
		if (event.type() != BookingEventType.CANCELLED && event.type() != BookingEventType.EXPIRED) {
			return;
		}
		this.redemptions.findByBookingId(event.bookingId()).ifPresent(redemption -> {
			this.codes.lockById(redemption.getDiscountCodeId()).released();
			this.redemptions.delete(redemption);
		});
	}

	private static void validate(DiscountCodeRequest request) {
		if (request.type() == DiscountType.PERCENT && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
			throw invalid("INVALID_DISCOUNT", "A percentage can't be over 100");
		}
		if (request.validFrom() != null && request.validTo() != null && !request.validTo().isAfter(request.validFrom())) {
			throw invalid("INVALID_DISCOUNT", "validTo must be after validFrom");
		}
	}

	private static ApiException invalid(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "DISCOUNT_CODE_NOT_FOUND", "Discount code not found");
	}

}
