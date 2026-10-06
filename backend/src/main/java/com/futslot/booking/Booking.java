package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.venue.Court;
import com.futslot.venue.Venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@CreationTimestamp
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

	/** Holds the slot until {@code expiresAt}; confirmed once the advance is paid. */
	void holdForPayment(BigDecimal advance, Instant expiresAt) {
		this.status = BookingStatus.PENDING_PAYMENT;
		this.advanceRequired = advance;
		this.holdExpiresAt = expiresAt;
	}

	boolean isLive() {
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

	public Court getCourt() {
		return this.court;
	}

	public VenueCustomer getCustomer() {
		return this.customer;
	}

	public UUID getBookedByUserId() {
		return this.bookedByUserId;
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

}
