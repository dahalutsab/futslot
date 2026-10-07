package com.futslot.booking;

import java.util.List;

import com.futslot.venue.AddOnLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The extras on a booking, replacing what was there; an empty list removes them all. */
public record AddOnsRequest(@NotNull @Size(max = 10) List<@Valid AddOnLine> addOns) {

}
