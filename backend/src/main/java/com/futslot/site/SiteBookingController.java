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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.futslot.auth.PageSignIn;
import com.futslot.booking.BookingResponse;
import com.futslot.booking.BookingService;
import com.futslot.booking.BookingSource;
import com.futslot.booking.BookingStatus;
import com.futslot.booking.PlayerBookingRequest;
import com.futslot.booking.QuoteResponse;
import com.futslot.common.ApiException;
import com.futslot.common.TextFormat;
import com.futslot.common.Wording;
import com.futslot.deposit.DepositService;
import com.futslot.deposit.DepositViews;
import com.futslot.payment.PaymentMethod;
import com.futslot.payment.PlayerPayments;
import com.futslot.slot.AvailabilityResponse;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.Slot;
import com.futslot.slot.SlotStatus;
import com.futslot.venue.VenueResponse;
import com.futslot.venue.VenueService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Booking on a venue's website: pick a free slot, sign in with a texted code, book, and (if the venue asks
 * for an advance) say how it was paid. A venue may also require the player's FutSlot booking deposit, which can be
 * topped up here. Forms post back to the site itself; each step checks the slot again.
 */
@Controller
class SiteBookingController {

	private static final String SITE = "/sites/{subdomain}";

	/** The same pages in Nepali. */
	private static final String NEPALI = SITE + SiteLanguage.NEPALI;

	private final VenueSiteRepository sites;

	private final SitePageService pages;

	private final VenueService venues;

	private final AvailabilityService availability;

	private final BookingService bookings;

	private final PageSignIn signIn;

	private final PlayerPayments payments;

	private final SiteSession session;

	private final DepositService deposits;

	private final SiteLanguage language;

	/** Where players send deposit top-ups: FutSlot's own wallet or account. */
	private final String futslotPaymentInstructions;

	/** The same in Nepali, or empty. */
	private final String futslotPaymentInstructionsNepali;

	private final Clock clock;

	SiteBookingController(VenueSiteRepository sites, SitePageService pages, VenueService venues,
			AvailabilityService availability, BookingService bookings, PageSignIn signIn, PlayerPayments payments,
			SiteSession session, DepositService deposits, SiteLanguage language,
			@Value("${futslot.subscriptions.payment-instructions:Contact FutSlot for payment details}") String futslotPaymentInstructions,
			@Value("${futslot.subscriptions.payment-instructions-ne:}") String futslotPaymentInstructionsNepali, Clock clock) {
		this.sites = sites;
		this.pages = pages;
		this.venues = venues;
		this.availability = availability;
		this.bookings = bookings;
		this.signIn = signIn;
		this.payments = payments;
		this.session = session;
		this.deposits = deposits;
		this.language = language;
		this.futslotPaymentInstructions = futslotPaymentInstructions;
		this.futslotPaymentInstructionsNepali = futslotPaymentInstructionsNepali;
		this.clock = clock;
	}

	@GetMapping({ SITE + "/book", NEPALI + "/book" })
	String book(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		Optional<UUID> player = this.session.player(request);
		return render(model, context, player.isPresent() ? "confirm" : "phone", court, start, null, false, null, null,
				player.orElse(null));
	}

	@PostMapping({ SITE + "/book/password", NEPALI + "/book/password" })
	String password(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			@RequestParam String phone, @RequestParam String password, HttpServletRequest request,
			HttpServletResponse response, Model model) {
		Context context = context(subdomain, request);
		try {
			this.session.signIn(this.signIn.withPassword(phone, password), request, response);
			return "redirect:" + context.base() + "/book?court=" + encode(court) + "&start=" + encode(start);
		}
		catch (ApiException ex) {
			String error = "PASSWORD_NOT_SET".equals(ex.getCode()) ? context.text().get("book.passwordNotSet") : context.text().error(ex);
			return render(model, context, "phone", court, start, phone, false, error, null, null);
		}
	}

	@PostMapping({ SITE + "/book/code", NEPALI + "/book/code" })
	String sendCode(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			@RequestParam String phone, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		try {
			this.signIn.sendCode(phone);
			return render(model, context, "code", court, start, phone, false, null, context.text().get("book.codeSent", phone.strip()), null);
		}
		catch (ApiException ex) {
			if ("OTP_COOLDOWN".equals(ex.getCode())) {
				return render(model, context, "code", court, start, phone, false, null, context.text().get("book.codeSentMoment"), null);
			}
			return render(model, context, "phone", court, start, phone, false, context.text().error(ex), null, null);
		}
	}

	@PostMapping({ SITE + "/book/verify", NEPALI + "/book/verify" })
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
					? render(model, context, "code", court, start, phone, true, null, context.text().get("book.welcome"), null)
					: render(model, context, "code", court, start, phone, false, context.text().error(ex), null, null);
		}
	}

	@PostMapping({ SITE + "/book/confirm", NEPALI + "/book/confirm" })
	String confirm(@PathVariable String subdomain, @RequestParam String court, @RequestParam String start,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		Optional<UUID> player = this.session.player(request);
		if (player.isEmpty()) {
			return render(model, context, "phone", court, start, null, false, context.text().get("book.signInToBook"), null, null);
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
			return render(model, context, "confirm", court, start, null, false, context.text().error(ex), null, player.get());
		}
	}

	@GetMapping({ SITE + "/bookings/{bookingId}", NEPALI + "/bookings/{bookingId}" })
	String booking(@PathVariable String subdomain, @PathVariable UUID bookingId, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		BookingResponse booking = playersBooking(player, bookingId, context);
		model.addAttribute("book", new BookPage(context.site(), "booking", null, null, false, null, null, true,
				view(booking, context, this.payments.awaitingCheck(bookingId)), List.of()));
		return "sites/book";
	}

	/** The player's bookings at this venue, coming up first, so they can get back to one. */
	@GetMapping({ SITE + "/bookings", NEPALI + "/bookings" })
	String bookings(@PathVariable String subdomain, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		List<BookPage.Booking> list = new java.util.ArrayList<>();
		for (boolean upcoming : List.of(true, false)) {
			this.bookings.listForPlayer(player, upcoming, 0, 20)
				.stream()
				.filter(b -> b.venue().id().equals(context.venue().id()))
				.forEach(b -> list.add(view(b, context, this.payments.awaitingCheck(b.id()))));
		}
		model.addAttribute("book", new BookPage(context.site(), "list", null, null, false, null, null, true, null, list));
		return "sites/book";
	}

	@PostMapping({ SITE + "/bookings/{bookingId}/pay", NEPALI + "/bookings/{bookingId}/pay" })
	String pay(@PathVariable String subdomain, @PathVariable UUID bookingId, @RequestParam String method,
			@RequestParam String transactionId, @RequestParam BigDecimal amount, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		BookingResponse booking = playersBooking(player, bookingId, context);
		try {
			this.payments.claimForBooking(player, bookingId, PaymentMethod.valueOf(method), transactionId.strip(), amount);
			return "redirect:" + context.base() + "/bookings/" + bookingId + "?sent=1";
		}
		catch (ApiException | IllegalArgumentException ex) {
			String message = (ex instanceof ApiException api) ? context.text().error(api) : context.text().get("book.chooseHowPaid");
			model.addAttribute("book", new BookPage(context.site(), "booking", null, null, false, message, null, true,
					view(booking, context, this.payments.awaitingCheck(bookingId)), List.of()));
			return "sites/book";
		}
	}

	/** The player's FutSlot deposit, to top up before booking a venue that requires it. */
	@GetMapping({ SITE + "/deposit", NEPALI + "/deposit" })
	String deposit(@PathVariable String subdomain, @RequestParam(required = false) String court,
			@RequestParam(required = false) String start, @RequestParam(required = false) String sent,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		return renderDeposit(model, context, player, court, start, null,
				(sent != null) ? context.text().get("book.topUpSentNotice") : null);
	}

	@PostMapping({ SITE + "/deposit", NEPALI + "/deposit" })
	String topUp(@PathVariable String subdomain, @RequestParam String method, @RequestParam String transactionId,
			@RequestParam BigDecimal amount, @RequestParam(required = false) String court,
			@RequestParam(required = false) String start, HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		try {
			this.payments.claimDepositTopUp(player, PaymentMethod.valueOf(method), transactionId.strip(), amount);
			String back = StringUtils.hasText(court) && StringUtils.hasText(start)
					? "&court=" + encode(court) + "&start=" + encode(start) : "";
			return "redirect:" + context.base() + "/deposit?sent=1" + back;
		}
		catch (ApiException | IllegalArgumentException ex) {
			String message = (ex instanceof ApiException api) ? context.text().error(api) : context.text().get("book.chooseHowPaid");
			return renderDeposit(model, context, player, court, start, message, null);
		}
	}

	/** Taking the deposit back: FutSlot sends it a few days later. */
	@PostMapping({ SITE + "/deposit/withdraw", NEPALI + "/deposit/withdraw" })
	String withdraw(@PathVariable String subdomain, @RequestParam String method, @RequestParam String sendTo,
			HttpServletRequest request, Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		try {
			if (!StringUtils.hasText(sendTo) || sendTo.strip().length() > 200) {
				throw new IllegalArgumentException();
			}
			this.deposits.requestWithdrawal(player, PaymentMethod.valueOf(method), sendTo);
			return "redirect:" + context.base() + "/deposit";
		}
		catch (ApiException | IllegalArgumentException ex) {
			String message = (ex instanceof ApiException api) ? context.text().error(api) : context.text().get("book.chooseWhereToSend");
			return renderDeposit(model, context, player, null, null, message, null);
		}
	}

	@PostMapping({ SITE + "/deposit/withdrawals/{withdrawalId}/cancel", NEPALI + "/deposit/withdrawals/{withdrawalId}/cancel" })
	String keepDeposit(@PathVariable String subdomain, @PathVariable UUID withdrawalId, HttpServletRequest request,
			Model model) {
		Context context = context(subdomain, request);
		UUID player = this.session.player(request).orElse(null);
		if (player == null) {
			return "redirect:" + SiteLanguage.home(context.base());
		}
		try {
			this.deposits.cancelWithdrawal(player, withdrawalId);
			return "redirect:" + context.base() + "/deposit";
		}
		catch (ApiException ex) {
			return renderDeposit(model, context, player, null, null, context.text().error(ex), null);
		}
	}

	private String renderDeposit(Model model, Context context, UUID player, String court, String start, String error,
			String notice) {
		TextFormat format = context.text().format();
		List<BookPage.Method> sendMethods = sendMethods(context.text());
		DepositViews.Deposit mine = this.deposits.forPlayer(player);
		BigDecimal toAdd = mine.toAdd();
		DepositViews.Withdrawal open = mine.withdrawal();
		BookPage.Withdrawal withdrawal = (open != null) ? new BookPage.Withdrawal(open.id().toString(),
				format.rupees(open.amount()), label(sendMethods, open.method().name()), open.sendTo(),
				format.day(open.dueAt().atZone(ZoneId.of(context.venue().timezone())).toLocalDate())) : null;
		BookPage.Deposit deposit = new BookPage.Deposit(format.rupees(DepositService.AMOUNT),
				format.rupees(mine.balance()), (toAdd.signum() > 0) ? format.rupees(toAdd) : null,
				toAdd.toPlainString(), this.payments.depositTopUpBeingChecked(player),
				(context.text().nepali() && StringUtils.hasText(this.futslotPaymentInstructionsNepali))
						? this.futslotPaymentInstructionsNepali : this.futslotPaymentInstructions,
				methods(context.text()), withdrawal, open == null && mine.balance().signum() > 0, sendMethods,
				StringUtils.hasText(court) ? court : null, StringUtils.hasText(start) ? start : null);
		model.addAttribute("book", new BookPage(context.site(), "deposit", null, null, false, error, notice, true, null,
				List.of(), deposit));
		return "sites/book";
	}

	@PostMapping({ SITE + "/sign-out", NEPALI + "/sign-out" })
	String signOut(@PathVariable String subdomain, HttpServletRequest request, HttpServletResponse response) {
		Context context = context(subdomain, request);
		this.session.signOut(request, response);
		return "redirect:" + SiteLanguage.home(context.base());
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
		TextFormat format = context.text().format();
		BigDecimal total = found.slot().price();
		BigDecimal advance = total.multiply(BigDecimal.valueOf(venue.policy().advancePercent()))
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING);
		boolean strikeAdvance = false;
		String depositToAdd = null;
		if (player != null) {
			try {
				QuoteResponse quote = this.bookings.quote(player,
						new PlayerBookingRequest(found.courtId(), found.slot().startsAt(), null, null, null, null));
				total = quote.total();
				advance = quote.advanceRequired();
				strikeAdvance = quote.strikeAdvance();
				depositToAdd = (quote.depositToAdd().signum() > 0) ? format.rupees(quote.depositToAdd()) : null;
			}
			catch (ApiException ex) {
				// Shown again when they confirm
			}
		}
		LocalDate day = found.slot().startsAt().atZoneSameInstant(ZoneId.of(venue.timezone())).toLocalDate();
		String time = format.clock(found.slot().startsAt().toLocalTime()) + " – " + format.clock(found.slot().endsAt().toLocalTime());
		return new BookPage.Slot(court, start, found.courtName(), format.day(day), time, format.rupees(total),
				advance.signum() > 0 ? format.rupees(advance) : null, format.number(venue.policy().bookingHoldMinutes()),
				strikeAdvance, venue.policy().depositRequired(), depositToAdd);
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

	private BookPage.Booking view(BookingResponse booking, Context context, boolean checking) {
		VenueResponse venue = context.venue();
		Wording text = context.text();
		TextFormat format = text.format();
		ZoneId zone = ZoneId.of(venue.timezone());
		LocalDate day = booking.startsAt().atZoneSameInstant(zone).toLocalDate();
		String time = format.clock(booking.startsAt().toLocalTime()) + " – " + format.clock(booking.endsAt().toLocalTime());
		BigDecimal due = booking.advanceRequired().subtract(booking.paidAmount()).max(BigDecimal.ZERO);
		boolean waiting = booking.status() == BookingStatus.PENDING_PAYMENT && !checking && due.signum() > 0
				&& booking.holdExpiresAt() != null && booking.holdExpiresAt().isAfter(this.clock.instant());
		String holdUntil = (booking.holdExpiresAt() != null)
				? format.clock(booking.holdExpiresAt().atZone(zone).toLocalTime()) : null;
		String tone = switch (booking.status()) {
			case PENDING_PAYMENT -> "pending";
			case CONFIRMED -> "good";
			case COMPLETED -> "neutral";
			case CANCELLED, EXPIRED, NO_SHOW -> "bad";
		};
		String status = (booking.status() == BookingStatus.PENDING_PAYMENT)
				? text.get(checking ? "book.status.checking" : "book.status.waiting") : text.get("book.status." + booking.status().name());
		return new BookPage.Booking(booking.id().toString(), booking.reference(), status, tone, booking.court().name(),
				format.day(day), time, format.rupees(booking.totalAmount()), format.rupees(booking.paidAmount()),
				waiting ? format.rupees(due) : null, due.toPlainString(), holdUntil, checking,
				StringUtils.hasText(venue.paymentInstructions()) ? venue.paymentInstructions() : text.get("book.askVenue", venue.name()),
				methods(text));
	}

	/**
	 * The site being used, the page's language, and what its links start with: "" or "/ne" on its own address,
	 * /sites/{subdomain} or /sites/{subdomain}/ne otherwise.
	 */
	private Context context(String subdomain, HttpServletRequest request) {
		VenueSite site = this.sites.findBySubdomain(subdomain.toLowerCase(Locale.ROOT))
			.filter(VenueSite::isPublished)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		String slug = this.pages.slugOf(site.getVenueId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		VenueResponse venue = this.venues.getActiveBySlug(slug);
		Wording text = this.language.of(request);
		String base = SiteLanguage.base(request, site.getSubdomain(), text.nepali());
		String color = (site.getAccentColor() != null) ? site.getAccentColor() : SitePageService.defaultAccent(site.getTemplate());
		boolean dark = site.getTemplate() == SiteTemplate.FLOODLIGHT;
		BookPage.Site view = new BookPage.Site(venue.name(), site.getTemplate().name().toLowerCase(Locale.ROOT),
				dark ? "https://fonts.googleapis.com/css2?family=Bebas+Neue&family=Inter:wght@400;500;600;700&family=Noto+Sans+Devanagari:wght@400;600&display=swap"
						: "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Noto+Sans+Devanagari:wght@400;600;700&display=swap",
				color, SiteFormat.inkOn(color), SiteFormat.readableOn(color, dark ? "#131715" : "#ffffff"), base, text.code(),
				SiteLanguage.base(request, site.getSubdomain(), !text.nepali()), text.format().rupees(DepositService.AMOUNT));
		return new Context(view, venue, slug, base, text);
	}

	private static List<BookPage.Method> methods(Wording text) {
		return List.of(new BookPage.Method("ESEWA", "eSewa"), new BookPage.Method("KHALTI", "Khalti"),
				new BookPage.Method("FONEPAY", "Fonepay"), new BookPage.Method("BANK_TRANSFER", text.get("method.bankTransfer")));
	}

	/** Where FutSlot can send a deposit back. */
	private static List<BookPage.Method> sendMethods(Wording text) {
		return List.of(new BookPage.Method("ESEWA", "eSewa"), new BookPage.Method("KHALTI", "Khalti"),
				new BookPage.Method("BANK_TRANSFER", text.get("method.bankAccount")));
	}

	private static String label(List<BookPage.Method> methods, String value) {
		return methods.stream().filter(m -> m.value().equals(value)).map(BookPage.Method::label).findFirst().orElse(value);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private record Context(BookPage.Site site, VenueResponse venue, String slug, String base, Wording text) {
	}

	private record Found(UUID courtId, String courtName, Slot slot) {
	}

}
