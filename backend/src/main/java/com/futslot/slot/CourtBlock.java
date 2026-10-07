package com.futslot.slot;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Time a court cannot be booked (maintenance, private event, holiday...). */
@Entity
@Table(name = "court_blocks")
public class CourtBlock {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID courtId;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BlockReason reason;

	private String note;

	@Column(nullable = false, updatable = false)
	private UUID createdBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected CourtBlock() {
	}

	CourtBlock(UUID courtId, Instant startsAt, Instant endsAt, BlockReason reason, String note, UUID createdBy) {
		this.courtId = courtId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.reason = reason;
		this.note = note;
		this.createdBy = createdBy;
	}

	TimeRange range() {
		return new TimeRange(this.startsAt, this.endsAt);
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getCourtId() {
		return this.courtId;
	}

	public Instant getStartsAt() {
		return this.startsAt;
	}

	public Instant getEndsAt() {
		return this.endsAt;
	}

	public BlockReason getReason() {
		return this.reason;
	}

	public String getNote() {
		return this.note;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
