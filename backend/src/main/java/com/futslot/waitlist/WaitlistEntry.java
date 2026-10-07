package com.futslot.waitlist;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.venue.Court;

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

/** A player waiting for a booked slot to free up. */
@Entity
@Table(name = "waitlist_entries")
class WaitlistEntry {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "court_id", updatable = false)
	private Court court;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, updatable = false)
	private Instant startsAt;

	@Column(nullable = false, updatable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WaitlistStatus status = WaitlistStatus.WAITING;

	private Instant notifiedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected WaitlistEntry() {
	}

	WaitlistEntry(Court court, UUID userId, Instant startsAt, Instant endsAt) {
		this.court = court;
		this.userId = userId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	void rejoin() {
		this.status = WaitlistStatus.WAITING;
		this.notifiedAt = null;
	}

	void notified(Instant at) {
		this.status = WaitlistStatus.NOTIFIED;
		this.notifiedAt = at;
	}

	void setStatus(WaitlistStatus status) {
		this.status = status;
	}

	/** Still waiting or notified, and the slot hasn't started. */
	boolean isOpenAt(Instant now) {
		return (this.status == WaitlistStatus.WAITING || this.status == WaitlistStatus.NOTIFIED)
				&& now.isBefore(this.startsAt);
	}

	WaitlistStatus statusAt(Instant now) {
		return ((this.status == WaitlistStatus.WAITING || this.status == WaitlistStatus.NOTIFIED)
				&& !now.isBefore(this.startsAt)) ? WaitlistStatus.EXPIRED : this.status;
	}

	UUID getId() {
		return this.id;
	}

	Court getCourt() {
		return this.court;
	}

	UUID getUserId() {
		return this.userId;
	}

	Instant getStartsAt() {
		return this.startsAt;
	}

	Instant getEndsAt() {
		return this.endsAt;
	}

	WaitlistStatus getStatus() {
		return this.status;
	}

	Instant getNotifiedAt() {
		return this.notifiedAt;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

}
