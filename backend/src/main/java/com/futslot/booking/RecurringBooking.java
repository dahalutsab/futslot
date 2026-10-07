package com.futslot.booking;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.DayOfWeekConverter;
import com.futslot.common.Timestamps;
import com.futslot.venue.Court;
import com.futslot.venue.Venue;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/**
 * A weekly standing slot. Real {@link Booking}s are created from it a few weeks ahead
 * ({@code generatedUntil}), so the overlap constraint protects them like any other booking.
 */
@Entity
@Table(name = "recurring_bookings")
public class RecurringBooking {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "court_id", updatable = false)
	private Court court;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", updatable = false)
	private VenueCustomer customer;

	@Convert(converter = DayOfWeekConverter.class)
	@Column(nullable = false, updatable = false)
	private DayOfWeek dayOfWeek;

	@Column(nullable = false, updatable = false)
	private LocalTime startTime;

	@Column(nullable = false, updatable = false)
	private LocalTime endTime;

	@Column(nullable = false, updatable = false)
	private LocalDate startsOn;

	private LocalDate endsOn;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecurringStatus status = RecurringStatus.ACTIVE;

	/** Last date occurrences exist for; null before the first generation. */
	private LocalDate generatedUntil;

	@Column(nullable = false, updatable = false)
	private UUID createdBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected RecurringBooking() {
	}

	RecurringBooking(Venue venue, Court court, VenueCustomer customer, DayOfWeek dayOfWeek, LocalTime startTime,
			LocalTime endTime, LocalDate startsOn, LocalDate endsOn, BigDecimal price, UUID createdBy) {
		this.venue = venue;
		this.court = court;
		this.customer = customer;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
		this.price = price;
		this.createdBy = createdBy;
	}

	void generatedUntil(LocalDate date) {
		this.generatedUntil = date;
	}

	void pause() {
		this.status = RecurringStatus.PAUSED;
	}

	/** Generation restarts from today, filling the weeks freed while paused. */
	void resume() {
		this.status = RecurringStatus.ACTIVE;
		this.generatedUntil = null;
	}

	void cancel() {
		this.status = RecurringStatus.CANCELLED;
	}

	public UUID getId() {
		return this.id;
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

	public DayOfWeek getDayOfWeek() {
		return this.dayOfWeek;
	}

	public LocalTime getStartTime() {
		return this.startTime;
	}

	public LocalTime getEndTime() {
		return this.endTime;
	}

	public LocalDate getStartsOn() {
		return this.startsOn;
	}

	public LocalDate getEndsOn() {
		return this.endsOn;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public RecurringStatus getStatus() {
		return this.status;
	}

	public LocalDate getGeneratedUntil() {
		return this.generatedUntil;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
