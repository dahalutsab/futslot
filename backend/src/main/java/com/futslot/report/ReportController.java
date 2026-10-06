package com.futslot.report;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** {@code from} and {@code to} are local dates, both included; by default the last 30 days. */
@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/reports")
@Validated
class ReportController {

	private final ReportService reportService;

	private final BookingExport bookingExport;

	ReportController(ReportService reportService, BookingExport bookingExport) {
		this.reportService = reportService;
		this.bookingExport = bookingExport;
	}

	@GetMapping("/summary")
	VenueReport summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return this.reportService.summary(CurrentUser.id(jwt), venueId, from, to);
	}

	@GetMapping("/daily")
	List<DailyRow> daily(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return this.reportService.daily(CurrentUser.id(jwt), venueId, from, to);
	}

	@GetMapping("/peak-hours")
	List<PeakHourCell> peakHours(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return this.reportService.peakHours(CurrentUser.id(jwt), venueId, from, to);
	}

	@GetMapping("/top-customers")
	List<TopCustomerRow> topCustomers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
		return this.reportService.topCustomers(CurrentUser.id(jwt), venueId, from, to, limit);
	}

	@GetMapping(value = "/bookings.csv", produces = "text/csv")
	ResponseEntity<byte[]> exportBookings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		BookingExport.Csv csv = this.bookingExport.bookings(CurrentUser.id(jwt), venueId, from, to);
		return ResponseEntity.ok()
			.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(csv.filename()).build().toString())
			.body(csv.content().getBytes(StandardCharsets.UTF_8));
	}

}
