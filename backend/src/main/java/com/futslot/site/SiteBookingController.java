package com.futslot.site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.futslot.auth.CodeSignIn;
import com.futslot.booking.BookingResponse;
import com.futslot.booking.BookingService;
import com.futslot.booking.BookingSource;
import com.futslot.booking.BookingStatus;
import com.futslot.booking.PlayerBookingRequest;
import com.futslot.booking.QuoteResponse;
import com.futslot.common.ApiException;
import com.futslot.payment.PaymentMethod;
import com.futslot.payment.PlayerPayments;
import com.futslot.slot.AvailabilityResponse;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.Slot;
import com.futslot.slot.SlotStatus;
import com.futslot.venue.VenueResponse;
import com.futslot.venue.VenueService;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Booking on a venue's website: pick a free slot, sign in with a texted code, book, and (if the venue asks
 * for an advance) say how it was paid. Forms post back to the site itself; each step checks the slot again.
 */
@Controller
class SiteBookingController {

	private static final List<BookPage.Method> METHODS = List.of(new BookPage.Method("ESEWA", "eSewa"),
			new BookPage.Method("KHALTI", "Khalti"), new BookPage.Method("FONEPAY", "Fonepay"),
			new BookPage.Method("BANK_TRANSFER", "Bank transfer"));

	private final VenueSiteRepository sites;

	private final SitePageService pages;

	private final VenueService venues;

	private final AvailabilityService availability;

	private final BookingService bookings;

	private final CodeSignIn signIn;

	private final PlayerPayments payments;

	private final SiteSession session;

	private final Clock clock;

	SiteBookingController(VenueSiteRepository sites, SitePageService pages, VenueService venues,
			AvailabilityService availability, BookingService bookings, CodeSignIn signIn, PlayerPayments payments,
			SiteSession session, Clock clock) {
		this.sites = sites;
		this.pages = pages;
		this.venues = venues;
		this.availability = availability;
		this.bookings = bookings;
		this.signIn = signIn;
		this.payments = payments;
		this.session = session;
		this.clock = clock;
	}

	@GetMapping("/sites/{subdomain}/book")
	String book(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		Optional<UUID> player = this.session.player(request);
		return render(model, context, player.isPresent() ? "confirm" : "phone", court, start, null, false, null, null,
				player.orElse(null));
	}

	@PostMapping("/sites/{subdomain}/book/code")
	String sendCode(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			@RequestParam String phone, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		try {
			this.signIn.sendCode(phone);
			return render(model, context, "code", court, start, phone, false, null, "We've texted a 6-digit code to " + phone.strip() + ".", null);
		}
		catch (ApiException ex) {
			if ("OTP_COOLDOWN".equals(ex.getCode())) {
				return render(model, context, "code", court, start, phone, false, null, "We sent you a code a moment ago. Enter it below.", null);
			}
			return render(model, context, "phone", court, start, phone, false, ex.getMessage(), null, null);
		}
	}

	@PostMapping("/sites/{subdomain}/book/verify")
	String verify(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			@RequestParam String phone, @RequestParam String code, @RequestParam(required = false) String name,
			HttpServletRequest request, HttpServletResponse response, Model model) {
		Context context = context(subdomain, request);
		try {
			UUID player = this.signIn.verify(phone, code.strip(), StringUtils.hasText(name) ? name.strip() : null);
			this.session.signIn(player, request, response);
			return "redirect:" + context.base() + "/book?court=" + encode(court) + "&start=" + encode(start);
		}
		catch (ApiException ex) {
			boolean needsName = "FULL_NAME_REQUIRED".equals(ex.getCode());
			return needsName
					? render(model, context, "code", court, start, phone, true, null, "Welcome! What's your name? The venue sees it on your booking.", null)
					: render(model, context, "code", court, start, phone, false, ex.getMessage(), null, null);
		}
	}

	@PostMapping("/sites/{subdomain}/book/confirm")
	String confirm(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		Optional<UUID> player = this.session.player(request);
		if (player.isEmpty()) {
			return render(model, context, "phone", court, start, null, false, "Sign in to book.", null, null);
		}
		Optional<Found> found = find(context, court, start);
		if (found.isEmpty()) {
			return render(model, context, "gone", court, start, null, false, null, null, player.get());
		}
		try {
			BookingResponse booking = this.bookings.bookAsPlayer(player.get(),
					new PlayerBookingRequest(found.get().courtId(), found.get().slot().startsAt(), null, null, null, null),
					BookingSource.WEBSITE);
			return "redirect:" + context.base() + "/bookings/" + booking.id();
		}
		catch (ApiException ex) {
			return render(model, context, "confirm", court, start, null, false, ex.getMessage(), null, player.get());
		}
	}

	@GetMapping("/sites/{subdomain}/bookings/{bookingId}")
	String booking(@PathVariable String subdomain, @PathVariable UUID bookingId, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + context.base() + "/";
		}
		BookingResponse booking = playersBooking(player, bookingId, context);
		model.addAttribute("book", new BookPage(context.site(), "booking", null, null, false, null, null, true,
				view(booking, context.venue(), this.payments.awaitingCheck(bookingId)), List.of()));
		return "sites/book";
	}

	/** The player's bookings at this venue, coming up first, so they can get back to one. */
	@GetMapping("/sites/{subdomain}/bookings")
	String bookings(@PathVariable String subdomain, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + context.base() + "/";
		}
		List<BookPage.Booking> list = new java.util.ArrayList<>();
		for (boolean upcoming : List.of(true, false)) {
			this.bookings.listForPlayer(player, upcoming, 0, 20)
				.stream()
				.filter(b -> b.venue().id().equals(context.venue().id()))
				.forEach(b -> list.add(view(b, context.venue(), this.payments.awaitingCheck(b.id()))));
		}
		model.addAttribute("book", new BookPage(context.site(), "list", null, null, false, null, null, true, null, list));
		return "sites/book";
	}

	@PostMapping("/sites/{subdomain}/bookings/{bookingId}/pay")
	String pay(@PathVariable String subdomain, @PathVariable UUID bookingId, @RequestParam String method,
			@RequestParam String transactionId, @RequestParam BigDecimal amount, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + context.base() + "/";
		}
		BookingResponse booking = playersBooking(player, bookingId, context);
		try {
			this.payments.claimForBooking(player, bookingId, PaymentMethod.valueOf(method), transactionId.strip(), amount);
			return "redirect:" + context.base() + "/bookings/" + bookingId + "?sent=1";
		}
		catch (ApiException | IllegalArgumentException ex) {
			String message = (ex instanceof ApiException api) ? api.getMessage() : "Choose how you paid.";
			model.addAttribute("book", new BookPage(context.site(), "booking", null, null, false, message, null, true,
					view(booking, context.venue(), this.payments.awaitingCheck(bookingId)), List.of()));
			return "sites/book";
		}
	}

	@PostMapping("/sites/{subdomain}/sign-out")
	String signOut(@PathVariable String subdomain, HttpServletRequest request, HttpServletResponse response) {
		Context context = context(subdomain, request);
		this.session.signOut(request, response);
		return "redirect:" + context.base() + "/";
	}

	private String render(Model model, Context context, String step, String court, String start, String phone,
			boolean needsName, String error, String notice, UUID player) {
		Optional<Found> found = find(context, court, start);
		String actual = (found.isEmpty() && !"gone".equals(step)) ? "gone" : step;
		BookPage.Slot slot = found.map(f -> slot(f, context, court, start, player)).orElse(null);
		model.addAttribute("book", new BookPage(context.site(), actual, slot, phone, needsName, error, notice, player != null, null, List.of()));
		return "sites/book";
	}

	/** The slot as the player will pay for it: with their membership or price, once we know who they are. */
	private BookPage.Slot slot(Found found, Context context, String court, String start, UUID player) {
		VenueResponse venue = context.venue();
		BigDecimal total = found.slot().price();
		BigDecimal advance = total.multiply(BigDecimal.valueOf(venue.policy().advancePercent()))
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING);
		if (player != null) {
			try {
				QuoteResponse quote = this.bookings.quote(player,
						new PlayerBookingRequest(found.courtId(), found.slot().startsAt(), null, null, null, null));
				total = quote.total();
				advance = quote.advanceRequired();
			}
			catch (ApiException ex) {
				// Shown again when they confirm
			}
		}
		LocalDate day = found.slot().startsAt().atZoneSameInstant(ZoneId.of(venue.timezone())).toLocalDate();
		String time = SiteFormat.clock(found.slot().startsAt().toLocalTime()) + " – " + SiteFormat.clock(found.slot().endsAt().toLocalTime());
		return new BookPage.Slot(court, start, found.courtName(), SiteFormat.day(day), time, SiteFormat.rupees(total),
				advance.signum() > 0 ? SiteFormat.rupees(advance) : null, String.valueOf(venue.policy().bookingHoldMinutes()));
	}

	/** The free slot on this venue's court starting at {@code start}, if it's still free. */
	private Optional<Found> find(Context context, String courtText, String startText) {
		UUID courtId;
		OffsetDateTime start;
		try {
			courtId = UUID.fromString(courtText);
			start = OffsetDateTime.parse(startText);
		}
		catch (IllegalArgumentException | DateTimeParseException ex) {
			return Optional.empty();
		}
		LocalDate day = start.atZoneSameInstant(ZoneId.of(context.venue().timezone())).toLocalDate();
		// A slot after midnight belongs to the night before
		for (LocalDate operatingDay : List.of(day, day.minusDays(1))) {
			AvailabilityResponse open;
			try {
				open = this.availability.forPlayers(context.slug(), operatingDay);
			}
			catch (ApiException ex) {
				continue;
			}
			for (AvailabilityResponse.CourtSlots court : open.courts()) {
				if (!court.courtId().equals(courtId)) {
					continue;
				}
				for (Slot slot : court.slots()) {
					if (slot.startsAt().toInstant().equals(start.toInstant())) {
						return slot.status() == SlotStatus.AVAILABLE ? Optional.of(new Found(courtId, court.name(), slot)) : Optional.empty();
					}
				}
			}
		}
		return Optional.empty();
	}

	private BookingResponse playersBooking(UUID player, UUID bookingId, Context context) {
		try {
			BookingResponse booking = this.bookings.getForPlayer(player, bookingId);
			if (!booking.venue().id().equals(context.venue().id())) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return booking;
		}
		catch (ApiException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	private BookPage.Booking view(BookingResponse booking, VenueResponse venue, boolean checking) {
		ZoneId zone = ZoneId.of(venue.timezone());
		LocalDate day = booking.startsAt().atZoneSameInstant(zone).toLocalDate();
		String time = SiteFormat.clock(booking.startsAt().toLocalTime()) + " – " + SiteFormat.clock(booking.endsAt().toLocalTime());
		BigDecimal due = booking.advanceRequired().subtract(booking.paidAmount()).max(BigDecimal.ZERO);
		boolean waiting = booking.status() == BookingStatus.PENDING_PAYMENT && !checking && due.signum() > 0
				&& booking.holdExpiresAt() != null && booking.holdExpiresAt().isAfter(this.clock.instant());
		String holdUntil = (booking.holdExpiresAt() != null)
				? SiteFormat.clock(booking.holdExpiresAt().atZone(zone).toLocalTime()) : null;
		String[] status = switch (booking.status()) {
			case PENDING_PAYMENT -> checking ? new String[] { "Payment sent, being checked", "pending" }
					: new String[] { "Waiting for your advance", "pending" };
			case CONFIRMED -> new String[] { "Confirmed", "good" };
			case COMPLETED -> new String[] { "Played", "neutral" };
			case CANCELLED -> new String[] { "Cancelled", "bad" };
			case EXPIRED -> new String[] { "Not paid in time, so the slot was released", "bad" };
			case NO_SHOW -> new String[] { "Marked as not played", "bad" };
		};
		return new BookPage.Booking(booking.id().toString(), booking.reference(), status[0], status[1], booking.court().name(),
				SiteFormat.day(day), time, SiteFormat.rupees(booking.totalAmount()), SiteFormat.rupees(booking.paidAmount()),
				waiting ? SiteFormat.rupees(due) : null, due.toPlainString(), holdUntil, checking,
				StringUtils.hasText(venue.paymentInstructions()) ? venue.paymentInstructions() : "Ask " + venue.name() + " for their eSewa or Khalti number.",
				METHODS);
	}

	/** The site being used, and what its links start with: "" on its own address, /sites/{subdomain} otherwise. */
	private Context context(String subdomain, HttpServletRequest request) {
		VenueSite site = this.sites.findBySubdomain(subdomain.toLowerCase(Locale.ROOT))
			.filter(VenueSite::isPublished)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		String slug = this.pages.slugOf(site.getVenueId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		VenueResponse venue = this.venues.getActiveBySlug(slug);
		String base = (request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) ? "" : "/sites/" + site.getSubdomain();
		String color = (site.getAccentColor() != null) ? site.getAccentColor() : SitePageService.defaultAccent(site.getTemplate());
		boolean dark = site.getTemplate() == SiteTemplate.FLOODLIGHT;
		BookPage.Site view = new BookPage.Site(venue.name(), site.getTemplate().name().toLowerCase(Locale.ROOT),
				dark ? "https://fonts.googleapis.com/css2?family=Bebas+Neue&family=Inter:wght@400;500;600;700&display=swap"
						: "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap",
				color, SiteFormat.inkOn(color), SiteFormat.readableOn(color, dark ? "#131715" : "#ffffff"), base);
		return new Context(view, venue, slug, base);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private record Context(BookPage.Site site, VenueResponse venue, String slug, String base) {
	}

	private record Found(UUID courtId, String courtName, Slot slot) {
	}

}
