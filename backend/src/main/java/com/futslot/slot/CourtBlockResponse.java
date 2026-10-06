package com.futslot.slot;

import java.time.Instant;
import java.util.UUID;

public record CourtBlockResponse(UUID id, UUID courtId, Instant startsAt, Instant endsAt, BlockReason reason,
		String note) {

	static CourtBlockResponse from(CourtBlock block) {
		return new CourtBlockResponse(block.getId(), block.getCourtId(), block.getStartsAt(), block.getEndsAt(),
				block.getReason(), block.getNote());
	}

}
