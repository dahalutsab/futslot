package com.futslot.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1")
@Validated
class BookingController {

	private final BookingService bookingService;

	BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	// Player

	@PostMapping("/bookings")
	@ResponseStatus(HttpStatus.CREATED)
	BookingResponse book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlayerBookingRequest request) {
		return this.bookingService.bookAsPlayer(CurrentUser.id(jwt), request);
	}

	@GetMapping("/bookings")
	List<BookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "upcoming") @Pattern(regexp = "upcoming|past") String scope,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.bookingService.listForPlayer(CurrentUser.id(jwt), "upcoming".equals(scope), page, size);
	}

	@GetMapping("/bookings/{bookingId}")
	BookingResponse myBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return this.bookingService.getForPlayer(CurrentUser.id(jwt), bookingId);
	}

	@PostMapping("/bookings/{bookingId}/cancel")
	BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody(required = false) CancelRequest request) {
		return this.bookingService.cancelAsPlayer(CurrentUser.id(jwt), bookingId, reason(request));
	}

	// Owner

	@PostMapping("/owner/venues/{venueId}/bookings")
	@ResponseStatus(HttpStatus.CREATED)
	BookingResponse bookForCustomer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody OwnerBookingRequest request) {
		return this.bookingService.bookAsOwner(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping("/owner/venues/{venueId}/bookings")
	List<BookingResponse> venueBookings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return this.bookingService.listForVenueDay(CurrentUser.id(jwt), venueId, date);
	}

	@GetMapping("/owner/venues/{venueId}/bookings/{bookingId}")
	BookingResponse venueBooking(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID bookingId) {
		return this.bookingService.getForOwner(CurrentUser.id(jwt), venueId, bookingId);
	}

	@PostMapping("/owner/venues/{venueId}/bookings/{bookingId}/reschedule")
	BookingResponse reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID bookingId, @Valid @RequestBody RescheduleRequest request) {
		return this.bookingService.reschedule(CurrentUser.id(jwt), venueId, bookingId, request);
	}

	@PostMapping("/owner/venues/{venueId}/bookings/{bookingId}/cancel")
	BookingResponse cancelForCustomer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID bookingId, @Valid @RequestBody(required = false) CancelRequest request) {
		return this.bookingService.cancelAsOwner(CurrentUser.id(jwt), venueId, bookingId, reason(request));
	}

	@PostMapping("/owner/venues/{venueId}/bookings/{bookingId}/no-show")
	BookingResponse noShow(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID bookingId) {
		return this.bookingService.markNoShow(CurrentUser.id(jwt), venueId, bookingId);
	}

	@PostMapping("/owner/venues/{venueId}/bookings/{bookingId}/complete")
	BookingResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID bookingId) {
		return this.bookingService.markCompleted(CurrentUser.id(jwt), venueId, bookingId);
	}

	private static String reason(CancelRequest request) {
		return (request != null) ? request.reason() : null;
	}

}
