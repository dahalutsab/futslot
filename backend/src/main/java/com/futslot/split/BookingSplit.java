package com.futslot.split;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** The cost-sharing rule for one booking and, once played, the result that decides who pays. */
@Entity
@Table(name = "booking_splits")
class BookingSplit {

	@Id
	private UUID bookingId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SplitRule rule;

	private Integer losersPercent;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal extrasAmount = BigDecimal.ZERO;

	private String extrasNote;

	@Column(name = "side_a_name", nullable = false)
	private String sideAName;

	@Column(name = "side_b_name", nullable = false)
	private String sideBName;

	@Enumerated(EnumType.STRING)
	private MatchResult result;

	private Instant resultRecordedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected BookingSplit() {
	}

	BookingSplit(UUID bookingId, String sideAName, String sideBName) {
		this.bookingId = bookingId;
		this.sideAName = sideAName;
		this.sideBName = sideBName;
	}

	void apply(SplitRequests.Rules request) {
		this.rule = request.rule();
		this.losersPercent = (request.rule() == SplitRule.LOSERS_PAY_PERCENT) ? request.losersPercent() : null;
		this.extrasAmount = (request.extrasAmount() != null) ? request.extrasAmount() : BigDecimal.ZERO;
		this.extrasNote = (request.extrasNote() != null && !request.extrasNote().isBlank()) ? request.extrasNote().strip()
				: null;
		if (request.sideAName() != null && !request.sideAName().isBlank()) {
			this.sideAName = request.sideAName().strip();
		}
		if (request.sideBName() != null && !request.sideBName().isBlank()) {
			this.sideBName = request.sideBName().strip();
		}
	}

	void record(MatchResult result, Instant at) {
		this.result = result;
		this.resultRecordedAt = at;
	}

	void clearResult() {
		this.result = null;
		this.resultRecordedAt = null;
	}

	String sideName(Side side) {
		return (side == Side.A) ? this.sideAName : this.sideBName;
	}

	UUID getBookingId() {
		return this.bookingId;
	}

	SplitRule getRule() {
		return this.rule;
	}

	Integer getLosersPercent() {
		return this.losersPercent;
	}

	BigDecimal getExtrasAmount() {
		return this.extrasAmount;
	}

	String getExtrasNote() {
		return this.extrasNote;
	}

	String getSideAName() {
		return this.sideAName;
	}

	String getSideBName() {
		return this.sideBName;
	}

	MatchResult getResult() {
		return this.result;
	}

	Instant getResultRecordedAt() {
		return this.resultRecordedAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
