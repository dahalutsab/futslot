package com.futslot.review;

import java.math.BigDecimal;
import java.util.Map;

/** {@code stars} maps each rating 1-5 to how many published reviews gave it (0 when none). */
public record RatingSummaryResponse(BigDecimal averageRating, int reviewCount, Map<Integer, Long> stars) {

}
