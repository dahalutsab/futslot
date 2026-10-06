package com.futslot.slot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.futslot.venue.CourtFormat;

public record AvailabilityResponse(UUID venueId, LocalDate date, String timezone, List<CourtSlots> courts) {

	public record CourtSlots(UUID courtId, String name, CourtFormat format, int slotDurationMinutes, List<Slot> slots) {

	}

}
