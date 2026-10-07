package com.futslot.split;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;
import com.futslot.user.User;

import jakarta.persistence.Column;
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

/** Someone playing in a split booking: an app user, or a guest known by name (and maybe phone). */
@Entity
@Table(name = "booking_participants")
class SplitPlayer {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID bookingId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", updatable = false)
	private User user;

	private String name;

	private String phone;

	@Enumerated(EnumType.STRING)
	private Side side;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal shareAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ParticipantStatus status = ParticipantStatus.ACCEPTED;

	private Instant settledAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected SplitPlayer() {
	}

	SplitPlayer(UUID bookingId, User user, String name, String phone, Side side) {
		this.bookingId = bookingId;
		this.user = user;
		this.name = (user == null) ? name : null;
		this.phone = (user == null) ? phone : null;
		this.side = side;
	}

	void setSide(Side side) {
		this.side = side;
	}

	/** The result is in: this is what they owe the booker. Nothing owed counts as settled. */
	void owe(BigDecimal share, Instant at) {
		this.shareAmount = share;
		if (share.signum() == 0) {
			settle(at);
		}
		else {
			unsettle();
		}
	}

	void settle(Instant at) {
		this.status = ParticipantStatus.PAID;
		this.paidAmount = this.shareAmount;
		this.settledAt = at;
	}

	void unsettle() {
		this.status = ParticipantStatus.ACCEPTED;
		this.paidAmount = BigDecimal.ZERO;
		this.settledAt = null;
	}

	void reset() {
		this.shareAmount = BigDecimal.ZERO;
		unsettle();
	}

	boolean isSettled() {
		return this.status == ParticipantStatus.PAID;
	}

	boolean is(UUID userId) {
		return this.user != null && this.user.getId().equals(userId);
	}

	String displayName() {
		return (this.user != null) ? this.user.getFullName() : this.name;
	}

	String contactPhone() {
		return (this.user != null) ? this.user.getPhone() : this.phone;
	}

	UUID getId() {
		return this.id;
	}

	UUID getBookingId() {
		return this.bookingId;
	}

	User getUser() {
		return this.user;
	}

	String getPhone() {
		return this.phone;
	}

	Side getSide() {
		return this.side;
	}

	BigDecimal getShareAmount() {
		return this.shareAmount;
	}

	Instant getSettledAt() {
		return this.settledAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
