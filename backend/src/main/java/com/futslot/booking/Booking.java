package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;
import com.futslot.venue.AddOnItem;
import com.futslot.venue.Court;
import com.futslot.venue.Venue;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One reservation of a court for a time range. Overlaps with other live bookings on the same court are
 * rejected by the database (bookings_no_overlap); the service checks first to give a friendly error.
 */
@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, unique = true, updatable = false)
	private String reference;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "court_id")
	private Court court;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", updatable = false)
	private VenueCustomer customer;

	@Column(updatable = false)
	private UUID bookedByUserId;

	@Column(updatable = false)
	private UUID recurringBookingId;

	@Column(updatable = false)
	private UUID membershipId;

	@Column(updatable = false)
	private UUID discountCodeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private BookingSource source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal priceAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal addOnsAmount = BigDecimal.ZERO;

	@ElementCollection
	@CollectionTable(name = "booking_add_ons", joinColumns = @JoinColumn(name = "booking_id"))
	private List<BookingAddOn> addOns = new ArrayList<>();

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal advanceRequired = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

	private Instant holdExpiresAt;

	private String notes;

	private Instant cancelledAt;

	private UUID cancelledBy;

	private String cancellationReason;

	@Column(updatable = false)
	private UUID createdBy;

	@Version
	private long version;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Booking() {
	}

	Booking(String reference, Venue venue, Court court, VenueCustomer customer, UUID bookedByUserId,
			BookingSource source, Instant startsAt, Instant endsAt, BigDecimal price, UUID createdBy, String notes) {
		this.reference = reference;
		this.venue = venue;
		this.court = court;
		this.customer = customer;
		this.bookedByUserId = bookedByUserId;
		this.source = source;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.priceAmount = price;
		this.totalAmount = price;
		this.createdBy = createdBy;
		this.notes = notes;
		this.status = BookingStatus.CONFIRMED;
	}

	static Booking occurrence(String reference, RecurringBooking series, Instant startsAt, Instant endsAt,
			UUID createdBy) {
		Booking booking = new Booking(reference, series.getVenue(), series.getCourt(), series.getCustomer(), null,
				BookingSource.RECURRING, startsAt, endsAt, series.getPrice(), createdBy, null);
		booking.recurringBookingId = series.getId();
		return booking;
	}

	/**
	 * A membership or discount code takes {@code discount} off the price. Call before
	 * {@link #holdForPayment}, since the advance is a share of what's left to pay.
	 */
	void applyDiscount(BigDecimal discount, UUID membershipId, UUID discountCodeId) {
		this.discountAmount = discount;
		this.membershipId = membershipId;
		this.discountCodeId = discountCodeId;
		recalculate();
	}

	/**
	 * Replaces the extras (the court discount stays on the court price). What's paid stays paid; the
	 * payment status follows the new total.
	 */
	void setAddOns(Map<AddOnItem, Integer> items) {
		Map<UUID, BigDecimal> charged = new HashMap<>();
		this.addOns.forEach(line -> charged.put(line.getItem().getId(), line.getUnitPrice()));
		this.addOns.clear();
		items.forEach((item, quantity) -> {
			BookingAddOn line = new BookingAddOn(item, quantity);
			// Keep the price already charged for an item that was on the booking before
			BigDecimal earlier = charged.get(item.getId());
			this.addOns.add((earlier != null) ? line.atPrice(earlier) : line);
		});
		this.addOnsAmount = this.addOns.stream().map(BookingAddOn::total).reduce(BigDecimal.ZERO, BigDecimal::add);
		recalculate();
	}

	private void recalculate() {
		this.totalAmount = this.priceAmount.add(this.addOnsAmount).subtract(this.discountAmount);
		if (this.paymentStatus == PaymentStatus.UNPAID || this.paymentStatus == PaymentStatus.PARTIALLY_PAID
				|| this.paymentStatus == PaymentStatus.PAID) {
			this.paymentStatus = (this.paidAmount.compareTo(this.totalAmount) >= 0) ? PaymentStatus.PAID
					: (this.paidAmount.signum() > 0) ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.UNPAID;
		}
	}

	/** Holds the slot until {@code expiresAt}; confirmed once the advance is paid. */
	void holdForPayment(BigDecimal advance, Instant expiresAt) {
		this.status = BookingStatus.PENDING_PAYMENT;
		this.advanceRequired = advance;
		this.holdExpiresAt = expiresAt;
	}

	/**
	 * Money the venue received for this booking. Paying at least the advance confirms a held booking.
	 */
	public void recordPayment(BigDecimal amount) {
		this.paidAmount = this.paidAmount.add(amount);
		this.paymentStatus = (this.paidAmount.compareTo(this.totalAmount) >= 0) ? PaymentStatus.PAID
				: PaymentStatus.PARTIALLY_PAID;
		if (this.status == BookingStatus.PENDING_PAYMENT && this.paidAmount.compareTo(this.advanceRequired) >= 0) {
			this.status = BookingStatus.CONFIRMED;
			this.holdExpiresAt = null;
		}
	}

	/** Money the venue paid back. */
	public void recordRefund(BigDecimal amount) {
		this.paidAmount = this.paidAmount.subtract(amount).max(BigDecimal.ZERO);
		this.paymentStatus = (this.paidAmount.signum() == 0) ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
	}

	/** While a submitted payment is being verified the slot stays held with no deadline. */
	public void pauseHold() {
		this.holdExpiresAt = null;
	}

	public void resumeHold(Instant until) {
		if (this.status == BookingStatus.PENDING_PAYMENT) {
			this.holdExpiresAt = until;
		}
	}

	public BigDecimal amountDue() {
		return this.totalAmount.subtract(this.paidAmount).max(BigDecimal.ZERO);
	}

	public boolean isLive() {
		return this.status == BookingStatus.PENDING_PAYMENT || this.status == BookingStatus.CONFIRMED;
	}

	void cancel(UUID by, String reason, Instant at) {
		this.status = BookingStatus.CANCELLED;
		this.cancelledBy = by;
		this.cancellationReason = reason;
		this.cancelledAt = at;
	}

	void moveTo(Court court, Instant startsAt, Instant endsAt) {
		this.court = court;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	void markNoShow() {
		this.status = BookingStatus.NO_SHOW;
	}

	void markCompleted() {
		this.status = BookingStatus.COMPLETED;
	}

	public UUID getId() {
		return this.id;
	}

	public String getReference() {
		return this.reference;
	}

	public Venue getVenue() {
		return this.venue;
	}

	public UUID getMembershipId() {
		return this.membershipId;
	}

	public UUID getDiscountCodeId() {
		return this.discountCodeId;
	}

	public BigDecimal getDiscountAmount() {
		return this.discountAmount;
	}

	public BigDecimal getAddOnsAmount() {
		return this.addOnsAmount;
	}

	public List<BookingAddOn> getAddOns() {
		return this.addOns;
	}

	public Court getCourt() {
		return this.court;
	}

	public VenueCustomer getCustomer() {
		return this.customer;
	}

	public UUID getBookedByUserId() {
		return this.bookedByUserId;
	}

	public UUID getRecurringBookingId() {
		return this.recurringBookingId;
	}

	public BookingSource getSource() {
		return this.source;
	}

	public BookingStatus getStatus() {
		return this.status;
	}

	public Instant getStartsAt() {
		return this.startsAt;
	}

	public Instant getEndsAt() {
		return this.endsAt;
	}

	public BigDecimal getPriceAmount() {
		return this.priceAmount;
	}

	public BigDecimal getTotalAmount() {
		return this.totalAmount;
	}

	public BigDecimal getAdvanceRequired() {
		return this.advanceRequired;
	}

	public BigDecimal getPaidAmount() {
		return this.paidAmount;
	}

	public PaymentStatus getPaymentStatus() {
		return this.paymentStatus;
	}

	public Instant getHoldExpiresAt() {
		return this.holdExpiresAt;
	}

	public String getNotes() {
		return this.notes;
	}

	public Instant getCancelledAt() {
		return this.cancelledAt;
	}

	public String getCancellationReason() {
		return this.cancellationReason;
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
