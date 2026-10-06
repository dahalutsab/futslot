package com.futslot.common;

import java.time.DayOfWeek;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores {@link DayOfWeek} as its ISO number (1 = Monday ... 7 = Sunday) in a smallint column. */
@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Short> {

	@Override
	public Short convertToDatabaseColumn(DayOfWeek day) {
		return (day != null) ? (short) day.getValue() : null;
	}

	@Override
	public DayOfWeek convertToEntityAttribute(Short value) {
		return (value != null) ? DayOfWeek.of(value) : null;
	}

}
