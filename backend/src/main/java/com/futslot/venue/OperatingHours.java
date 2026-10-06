package com.futslot.venue;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.futslot.common.DayOfWeekConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

/** Opening hours for one weekday. A close time at or before the open time means open past midnight. */
@Embeddable
public record OperatingHours(@Convert(converter = DayOfWeekConverter.class) @Column(nullable = false) DayOfWeek dayOfWeek,
		@Column(nullable = false) LocalTime openTime, @Column(nullable = false) LocalTime closeTime,
		@Column(nullable = false) boolean isClosed) {

}
