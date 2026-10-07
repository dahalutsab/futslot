package com.futslot.split;

/**
 * Stored in booking_participants.status. Splits use ACCEPTED (in the game, share not yet settled) and PAID
 * (settled with the booker); INVITED and DECLINED are left for a future invite flow.
 */
enum ParticipantStatus {

	INVITED, ACCEPTED, DECLINED, PAID

}
