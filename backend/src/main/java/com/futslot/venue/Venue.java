package com.futslot.venue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "venues")
public class Venue {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID ownerId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String slug;

	private String description;

	private String phone;

	@Column(columnDefinition = "citext")
	private String email;

	@Column(nullable = false)
	private String addressLine;

	private String area;

	@Column(nullable = false)
	private String city;

	@Column(precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(nullable = false)
	private String timezone = "Asia/Kathmandu";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VenueStatus status = VenueStatus.DRAFT;

	private String rejectionReason;

	private UUID approvedBy;

	private Instant approvedAt;

	@Column(nullable = false)
	private int advancePercent;

	@Column(nullable = false)
	private int bookingHoldMinutes = 15;

	@Column(nullable = false)
	private int maxAdvanceBookingDays = 30;

	private Integer strikeLimit;

	private Integer strikeAdvancePercent;

	private String paymentInstructions;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal averageRating = BigDecimal.ZERO;

	@Column(nullable = false)
	private int reviewCount;

	@ElementCollection
	@CollectionTable(name = "operating_hours", joinColumns = @JoinColumn(name = "venue_id"))
	@OrderBy("dayOfWeek")
	private List<OperatingHours> operatingHours = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "venue_amenities", joinColumns = @JoinColumn(name = "venue_id"))
	@Column(name = "amenity_code", nullable = false)
	private Set<String> amenityCodes = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "venue_cancellation_tiers", joinColumns = @JoinColumn(name = "venue_id"))
	@OrderBy("withinHours desc")
	private List<CancellationTier> cancellationTiers = new ArrayList<>();

	@OneToMany(mappedBy = "venue")
	@OrderBy("sortOrder, name")
	private List<Court> courts = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Venue() {
	}

	Venue(UUID ownerId, String slug, VenueDetails details) {
		this.ownerId = ownerId;
		this.slug = slug;
		apply(details);
	}

	void apply(VenueDetails details) {
		this.name = details.name();
		this.description = details.description();
		this.phone = details.phone();
		this.email = details.email();
		this.addressLine = details.addressLine();
		this.area = details.area();
		this.city = details.city();
		this.latitude = details.latitude();
		this.longitude = details.longitude();
	}

	void applyPolicy(VenuePolicy policy) {
		this.advancePercent = policy.advancePercent();
		this.bookingHoldMinutes = policy.bookingHoldMinutes();
		this.cancellationTiers.clear();
		this.cancellationTiers.addAll(policy.cancellationTiers());
		this.maxAdvanceBookingDays = policy.maxAdvanceBookingDays();
		this.strikeLimit = policy.strikeLimit();
		this.strikeAdvancePercent = policy.strikeAdvancePercent();
	}

	public VenuePolicy policy() {
		return new VenuePolicy(this.advancePercent, this.bookingHoldMinutes, this.cancellationTiers,
				this.maxAdvanceBookingDays, this.strikeLimit, this.strikeAdvancePercent);
	}

	void setPaymentInstructions(String instructions) {
		this.paymentInstructions = (instructions != null && !instructions.isBlank()) ? instructions.strip() : null;
	}

	void submitForApproval() {
		this.status = VenueStatus.PENDING_APPROVAL;
		this.rejectionReason = null;
	}

	void approve(UUID adminId, Instant at) {
		this.status = VenueStatus.ACTIVE;
		this.approvedBy = adminId;
		this.approvedAt = at;
		this.rejectionReason = null;
	}

	void reject(String reason) {
		this.status = VenueStatus.REJECTED;
		this.rejectionReason = reason;
	}

	void suspend(String reason) {
		this.status = VenueStatus.SUSPENDED;
		this.rejectionReason = reason;
	}

	void replaceOperatingHours(List<OperatingHours> hours) {
		this.operatingHours.clear();
		this.operatingHours.addAll(hours);
	}

	void replaceAmenities(Set<String> codes) {
		this.amenityCodes.clear();
		this.amenityCodes.addAll(codes);
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getOwnerId() {
		return this.ownerId;
	}

	public String getName() {
		return this.name;
	}

	public String getSlug() {
		return this.slug;
	}

	public String getDescription() {
		return this.description;
	}

	public String getPhone() {
		return this.phone;
	}

	public String getEmail() {
		return this.email;
	}

	public String getAddressLine() {
		return this.addressLine;
	}

	public String getArea() {
		return this.area;
	}

	public String getCity() {
		return this.city;
	}

	public BigDecimal getLatitude() {
		return this.latitude;
	}

	public BigDecimal getLongitude() {
		return this.longitude;
	}

	public String getTimezone() {
		return this.timezone;
	}

	public VenueStatus getStatus() {
		return this.status;
	}

	public String getRejectionReason() {
		return this.rejectionReason;
	}

	public String getPaymentInstructions() {
		return this.paymentInstructions;
	}

	public BigDecimal getAverageRating() {
		return this.averageRating;
	}

	public int getReviewCount() {
		return this.reviewCount;
	}

	public List<OperatingHours> getOperatingHours() {
		return this.operatingHours;
	}

	public Set<String> getAmenityCodes() {
		return this.amenityCodes;
	}

	public List<Court> getCourts() {
		return this.courts;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
