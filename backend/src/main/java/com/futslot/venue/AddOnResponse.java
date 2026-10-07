package com.futslot.venue;

import java.math.BigDecimal;
import java.util.UUID;

public record AddOnResponse(UUID id, String name, AddOnCategory category, BigDecimal price, boolean active) {

	static AddOnResponse from(AddOnItem item) {
		return new AddOnResponse(item.getId(), item.getName(), item.getCategory(), item.getPrice(), item.isActive());
	}

}
