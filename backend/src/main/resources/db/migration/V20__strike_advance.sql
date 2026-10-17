-- Players who keep cancelling late or not showing up must pay more up front. Off unless both are set:
-- strike_limit late cancellations and no-shows in the last 90 days raise the advance to
-- strike_advance_percent (or the usual advance, if that is higher).
ALTER TABLE venues
    ADD COLUMN strike_limit           int CHECK (strike_limit BETWEEN 1 AND 10),
    ADD COLUMN strike_advance_percent int CHECK (strike_advance_percent BETWEEN 1 AND 100),
    ADD CONSTRAINT venues_strike_rule_check CHECK ((strike_limit IS NULL) = (strike_advance_percent IS NULL));

CREATE INDEX idx_bookings_customer_strikes ON bookings (customer_id, starts_at)
    WHERE status = 'NO_SHOW' OR (status = 'CANCELLED' AND cancellation_fee_percent > 0);
