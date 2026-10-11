-- Venues charge a cancellation fee that grows as kickoff gets closer, instead of one cutoff. A tier applies
-- when a player cancels less than within_hours before the start; the fee is a share of the booking price,
-- never more than what the player paid up front. Cancelling 24 hours or more ahead is always free.
CREATE TABLE venue_cancellation_tiers (
    venue_id     uuid NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    within_hours int  NOT NULL CHECK (within_hours BETWEEN 1 AND 24),
    fee_percent  int  NOT NULL CHECK (fee_percent BETWEEN 1 AND 100),
    PRIMARY KEY (venue_id, within_hours)
);

-- The old rule refunded late_cancel_refund_pct of what was paid, so the venue kept the rest of it
INSERT INTO venue_cancellation_tiers (venue_id, within_hours, fee_percent)
SELECT id, least(free_cancel_hours, 24), 100 - late_cancel_refund_pct
FROM venues
WHERE free_cancel_hours > 0 AND late_cancel_refund_pct < 100;

-- The fee share the player's own cancellation fell into, so late cancellations can be counted per player
ALTER TABLE bookings ADD COLUMN cancellation_fee_percent int NOT NULL DEFAULT 0
    CHECK (cancellation_fee_percent BETWEEN 0 AND 100);

-- Past cancellations, judged by the venue's current policy since earlier versions weren't kept
UPDATE bookings b
SET cancellation_fee_percent = t.fee_percent
FROM venue_customers c, venue_cancellation_tiers t
WHERE b.status = 'CANCELLED' AND c.id = b.customer_id AND b.cancelled_by = c.user_id
  AND t.venue_id = b.venue_id AND b.cancelled_at > b.starts_at - make_interval(hours => t.within_hours);

ALTER TABLE venues DROP COLUMN free_cancel_hours, DROP COLUMN late_cancel_refund_pct;
