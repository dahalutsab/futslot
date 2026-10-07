package com.futslot.slot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.venue.Court;
import com.futslot.venue.CourtRepository;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

@Service
public class CourtBlockService {

	private final VenueAccess access;

	private final CourtRepository courts;

	private final CourtBlockRepository blocks;

	private final BookedRanges bookedRanges;

	private final Clock clock;

	CourtBlockService(VenueAccess access, CourtRepository courts, CourtBlockRepository blocks,
			BookedRanges bookedRanges, Clock clock) {
		this.access = access;
		this.courts = courts;
		this.blocks = blocks;
		this.bookedRanges = bookedRanges;
		this.clock = clock;
	}

	/**
	 * Blocks may not overlap other blocks (enforced by the database too) or live bookings. The court row
	 * is locked so a booking can't slip in between the check and the insert.
	 */
	@Transactional
	CourtBlockResponse create(UUID ownerId, UUID venueId, UUID courtId, CourtBlockRequest request) {
		Court court = lockedCourt(ownerId, venueId, courtId);
		Instant start = request.startsAt().toInstant();
		Instant end = request.endsAt().toInstant();
		if (!end.isAfter(start)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BLOCK", "End must be after start");
		}
		if (!this.blocks.findOverlapping(List.of(court.getId()), start, end).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "BLOCK_OVERLAP", "This time is already blocked");
		}
		if (!this.bookedRanges.byCourt(List.of(court.getId()), start, end, this.clock.instant()).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_HAS_BOOKINGS",
					"There are bookings in this time. Cancel or move them first");
		}
		CourtBlock block = this.blocks.save(new CourtBlock(court.getId(), start, end, request.reason(),
				request.note(), ownerId));
		return CourtBlockResponse.from(block);
	}

	/**
	 * For other modules (tournament matches): blocks court time without a permission check, which the
	 * caller has done. Same overlap rules as {@link #create}.
	 */
	@Transactional
	public UUID reserve(UUID courtId, Instant start, Instant end, BlockReason reason, String note, UUID by) {
		this.courts.lockById(courtId);
		if (!this.blocks.findOverlapping(List.of(courtId), start, end).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "BLOCK_OVERLAP", "The court is blocked at that time");
		}
		if (!this.bookedRanges.byCourt(List.of(courtId), start, end, this.clock.instant()).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_HAS_BOOKINGS",
					"The court is booked at that time. Pick another time or move the booking");
		}
		return this.blocks.save(new CourtBlock(courtId, start, end, reason, note, by)).getId();
	}

	/** Frees time taken by {@link #reserve}; already-deleted blocks are fine. */
	@Transactional
	public void release(UUID blockId) {
		this.blocks.deleteById(blockId);
	}

	@Transactional(readOnly = true)
	List<CourtBlockResponse> list(UUID ownerId, UUID venueId, UUID courtId, Instant from, Instant to) {
		ownedCourt(ownerId, venueId, courtId);
		return this.blocks.findOverlapping(List.of(courtId), from, to).stream().map(CourtBlockResponse::from).toList();
	}

	@Transactional
	void delete(UUID ownerId, UUID venueId, UUID courtId, UUID blockId) {
		ownedCourt(ownerId, venueId, courtId);
		CourtBlock block = this.blocks.findByIdAndCourtId(blockId, courtId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BLOCK_NOT_FOUND", "Block not found"));
		this.blocks.delete(block);
	}

	private Court ownedCourt(UUID ownerId, UUID venueId, UUID courtId) {
		this.access.require(ownerId, venueId, VenuePermission.MANAGE_BLOCKS);
		return this.courts.findByIdAndVenueId(courtId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
	}

	private Court lockedCourt(UUID ownerId, UUID venueId, UUID courtId) {
		ownedCourt(ownerId, venueId, courtId);
		return this.courts.lockById(courtId);
	}

}
