package com.futslot.venue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;
import com.futslot.common.PhoneNumbers;
import com.futslot.common.Slugs;
import com.futslot.user.Role;
import com.futslot.user.UserService;

/** Venue setup by owners, and the public read side. Owners can only see and change their own venues. */
@Service
public class VenueService {

	private final VenueRepository venues;

	private final CourtRepository courts;

	private final PricingRuleRepository pricingRules;

	private final AmenityRepository amenities;

	private final UserService userService;

	private final VenueAccess access;

	private final VenueMapper mapper;

	VenueService(VenueRepository venues, CourtRepository courts, PricingRuleRepository pricingRules,
			AmenityRepository amenities, UserService userService, VenueAccess access, VenueMapper mapper) {
		this.venues = venues;
		this.courts = courts;
		this.pricingRules = pricingRules;
		this.amenities = amenities;
		this.userService = userService;
		this.access = access;
		this.mapper = mapper;
	}

	@Transactional
	public VenueResponse create(UUID ownerId, VenueDetails details) {
		String slug = Slugs.unique(details.name(), "venue", this.venues::existsBySlug);
		Venue venue = this.venues.save(new Venue(ownerId, slug, clean(details)));
		// Takes effect in the owner's next access token; owner endpoints check ownership, not the role
		this.userService.grantRole(ownerId, Role.OWNER);
		return this.mapper.toResponse(venue, true);
	}

	/** Venues the user owns or works at. */
	@Transactional(readOnly = true)
	public List<MyVenueResponse> listManaged(UUID userId) {
		return this.access.memberships(userId)
			.stream()
			.map(membership -> new MyVenueResponse(this.mapper.toSummary(membership.venue()),
					(membership.staffRole() != null) ? membership.staffRole().name() : "OWNER", membership.permissions()))
			.toList();
	}

	@Transactional(readOnly = true)
	public VenueResponse getManaged(UUID userId, UUID venueId) {
		return this.mapper.toResponse(this.access.require(userId, venueId, null), true);
	}

	@Transactional
	public VenueResponse updateDetails(UUID ownerId, UUID venueId, VenueDetails details) {
		Venue venue = owned(ownerId, venueId);
		venue.apply(clean(details));
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public VenueResponse updatePolicy(UUID ownerId, UUID venueId, VenuePolicy policy) {
		Venue venue = owned(ownerId, venueId);
		venue.applyPolicy(policy);
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public VenueResponse updatePaymentInstructions(UUID ownerId, UUID venueId, String instructions) {
		Venue venue = owned(ownerId, venueId);
		venue.setPaymentInstructions(instructions);
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public VenueResponse replaceOperatingHours(UUID ownerId, UUID venueId, List<DayHours> days) {
		Venue venue = owned(ownerId, venueId);
		Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
		List<OperatingHours> hours = new ArrayList<>();
		for (DayHours day : days) {
			if (!seen.add(day.day())) {
				throw badRequest("INVALID_HOURS", day.day() + " is listed more than once");
			}
			if (day.isClosed()) {
				hours.add(new OperatingHours(day.day(), orMidnight(day.openTime()), orMidnight(day.closeTime()), true));
				continue;
			}
			if (day.openTime() == null || day.closeTime() == null || day.openTime().equals(day.closeTime())) {
				throw badRequest("INVALID_HOURS", day.day() + " needs different open and close times");
			}
			hours.add(new OperatingHours(day.day(), day.openTime(), day.closeTime(), false));
		}
		if (seen.size() != 7) {
			throw badRequest("INVALID_HOURS", "Provide hours for all 7 days (mark days off as closed)");
		}
		venue.replaceOperatingHours(hours);
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public VenueResponse replaceAmenities(UUID ownerId, UUID venueId, Set<String> codes) {
		Venue venue = owned(ownerId, venueId);
		List<String> unknown = codes.stream().filter(code -> !this.amenities.existsById(code)).sorted().toList();
		if (!unknown.isEmpty()) {
			throw badRequest("UNKNOWN_AMENITY", "Unknown amenities: " + String.join(", ", unknown));
		}
		venue.replaceAmenities(codes);
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public VenueResponse submitForApproval(UUID ownerId, UUID venueId) {
		Venue venue = owned(ownerId, venueId);
		if (venue.getStatus() != VenueStatus.DRAFT && venue.getStatus() != VenueStatus.REJECTED) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_VENUE_STATUS",
					"Only draft or rejected venues can be submitted (current: " + venue.getStatus() + ")");
		}
		List<String> missing = new ArrayList<>();
		if (venue.getOperatingHours().size() != 7) {
			missing.add("opening hours");
		}
		List<Court> activeCourts = venue.getCourts().stream().filter(Court::isActive).toList();
		if (activeCourts.isEmpty()) {
			missing.add("at least one active court");
		}
		activeCourts.stream()
			.filter(court -> court.getPricingRules().stream().noneMatch(PricingRule::isActive))
			.forEach(court -> missing.add("pricing for " + court.getName()));
		if (!missing.isEmpty()) {
			throw badRequest("VENUE_INCOMPLETE", "Before submitting, add: " + String.join(", ", missing));
		}
		venue.submitForApproval();
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	public CourtResponse addCourt(UUID ownerId, UUID venueId, CourtRequest request) {
		Venue venue = owned(ownerId, venueId);
		if (this.courts.existsByVenueIdAndNameIgnoreCase(venueId, request.name().strip())) {
			throw duplicateCourt(request.name());
		}
		Court court = this.courts.save(new Court(venue, request));
		venue.getCourts().add(court);
		return CourtResponse.from(court, true);
	}

	@Transactional
	public CourtResponse updateCourt(UUID ownerId, UUID venueId, UUID courtId, CourtRequest request) {
		Court court = ownedCourt(ownerId, venueId, courtId);
		if (this.courts.existsByVenueIdAndNameIgnoreCaseAndIdNot(venueId, request.name().strip(), courtId)) {
			throw duplicateCourt(request.name());
		}
		court.apply(request);
		return CourtResponse.from(court, true);
	}

	@Transactional
	public PricingRuleResponse addPricingRule(UUID ownerId, UUID venueId, UUID courtId, PricingRuleRequest request) {
		Court court = ownedCourt(ownerId, venueId, courtId);
		validate(request);
		PricingRule rule = this.pricingRules.save(new PricingRule(court, request));
		return PricingRuleResponse.from(rule);
	}

	@Transactional
	public PricingRuleResponse updatePricingRule(UUID ownerId, UUID venueId, UUID courtId, UUID ruleId,
			PricingRuleRequest request) {
		ownedCourt(ownerId, venueId, courtId);
		validate(request);
		PricingRule rule = rule(courtId, ruleId);
		rule.apply(request);
		return PricingRuleResponse.from(rule);
	}

	@Transactional
	public void deletePricingRule(UUID ownerId, UUID venueId, UUID courtId, UUID ruleId) {
		ownedCourt(ownerId, venueId, courtId);
		this.pricingRules.delete(rule(courtId, ruleId));
	}

	@Transactional(readOnly = true)
	public PageResponse<VenueSummaryResponse> searchActive(String city, String query, Pageable pageable) {
		Specification<Venue> spec = (root, q, cb) -> cb.equal(root.get("status"), VenueStatus.ACTIVE);
		if (StringUtils.hasText(city)) {
			spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("city")), city.strip().toLowerCase()));
		}
		if (StringUtils.hasText(query)) {
			String pattern = "%" + query.strip().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
			spec = spec.and((root, q, cb) -> cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'),
					cb.like(cb.lower(root.get("area")), pattern, '\\')));
		}
		Page<Venue> page = this.venues.findAll(spec, pageable);
		return PageResponse.of(page, this.mapper::toSummary);
	}

	@Transactional(readOnly = true)
	public VenueResponse getActiveBySlug(String slug) {
		return this.venues.findBySlugAndStatus(slug, VenueStatus.ACTIVE)
			.map(venue -> this.mapper.toResponse(venue, false))
			.orElseThrow(VenueService::venueNotFound);
	}

	@Transactional(readOnly = true)
	public List<AmenityResponse> listAmenities() {
		return this.amenities.findAll().stream().map(AmenityResponse::from).toList();
	}

	/** Venue settings, courts and pricing need MANAGE_VENUE (owners and managers). */
	private Venue owned(UUID userId, UUID venueId) {
		return this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
	}

	private Court ownedCourt(UUID ownerId, UUID venueId, UUID courtId) {
		owned(ownerId, venueId);
		return this.courts.findByIdAndVenueId(courtId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
	}

	private PricingRule rule(UUID courtId, UUID ruleId) {
		return this.pricingRules.findByIdAndCourtId(ruleId, courtId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRICING_RULE_NOT_FOUND", "Pricing rule not found"));
	}

	private static void validate(PricingRuleRequest request) {
		if (!request.endTime().isAfter(request.startTime())) {
			throw badRequest("INVALID_PRICING_RULE", "End time must be after start time");
		}
		if (request.validFrom() != null && request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
			throw badRequest("INVALID_PRICING_RULE", "validTo must be on or after validFrom");
		}
	}

	private static VenueDetails clean(VenueDetails details) {
		String phone = StringUtils.hasText(details.phone()) ? PhoneNumbers.normalize(details.phone()) : null;
		String email = StringUtils.hasText(details.email()) ? details.email().strip() : null;
		return new VenueDetails(details.name().strip(), details.description(), phone, email,
				details.addressLine().strip(), StringUtils.hasText(details.area()) ? details.area().strip() : null,
				details.city().strip(), details.latitude(), details.longitude());
	}

	private static LocalTime orMidnight(LocalTime time) {
		return (time != null) ? time : LocalTime.MIDNIGHT;
	}

	static ApiException venueNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found");
	}

	private static ApiException duplicateCourt(String name) {
		return new ApiException(HttpStatus.CONFLICT, "COURT_NAME_TAKEN", "A court named '" + name.strip() + "' already exists");
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
