package com.futslot.review;

/** Only PUBLISHED reviews are shown to players and counted in the venue's rating. */
public enum ReviewStatus {

	PUBLISHED,

	/** Hidden by an admin pending a decision; the author and venue still see it. */
	HIDDEN,

	/** Taken down by an admin; can no longer be edited or replied to. */
	REMOVED

}
