-- Splitting a booking's cost between the players, including "baazi" where the result decides who pays.
-- FutSlot only works out the shares: the booker pays the venue and collects from the others.

-- How the cost is shared. Court cost is the booking total; extras (water, drinks) are added on top.
CREATE TABLE booking_splits (
    booking_id         uuid PRIMARY KEY REFERENCES bookings (id) ON DELETE CASCADE,
    rule               text           NOT NULL
        CHECK (rule IN ('EQUAL', 'LOSERS_PAY_ALL', 'LOSERS_PAY_PERCENT', 'LOSERS_PAY_COURT')),
    losers_percent     int CHECK (losers_percent BETWEEN 50 AND 100),
    extras_amount      numeric(12, 2) NOT NULL DEFAULT 0 CHECK (extras_amount >= 0),
    extras_note        text,
    side_a_name        text           NOT NULL DEFAULT 'Team A',
    side_b_name        text           NOT NULL DEFAULT 'Team B',
    result             text CHECK (result IN ('A_WON', 'B_WON', 'DRAW')),
    result_recorded_at timestamptz,
    created_at         timestamptz    NOT NULL DEFAULT now(),
    updated_at         timestamptz    NOT NULL DEFAULT now(),
    CHECK (rule <> 'LOSERS_PAY_PERCENT' OR losers_percent IS NOT NULL),
    CHECK ((result IS NULL) = (result_recorded_at IS NULL))
);

-- Players can be app users or guests known only by name (most friends won't have the app).
ALTER TABLE booking_participants ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE booking_participants
    ADD COLUMN name       text,
    ADD COLUMN phone      text,
    ADD COLUMN side       text CHECK (side IN ('A', 'B')),
    ADD COLUMN settled_at timestamptz,
    ADD CONSTRAINT booking_participants_identified CHECK (user_id IS NOT NULL OR name IS NOT NULL);
