-- Players keep a fixed Rs 1000 booking deposit with FutSlot. Venues that require it only take app and website
-- bookings from players whose deposit is full. A late cancellation or no-show fee the advance doesn't cover comes
-- out of the deposit, and FutSlot pays those fees on to the venue once a month. FutSlot keeps none of it.
ALTER TABLE venues ADD COLUMN deposit_required boolean NOT NULL DEFAULT false;

-- Booked while the venue required the deposit, so fees on it may come from the deposit
ALTER TABLE bookings ADD COLUMN deposit_backed boolean NOT NULL DEFAULT false;
CREATE INDEX idx_bookings_deposit_live ON bookings (customer_id)
    WHERE deposit_backed AND status IN ('PENDING_PAYMENT', 'CONFIRMED');

-- Top-ups are payments to FutSlot with no booking, invoice or other target
ALTER TABLE payments
    DROP CONSTRAINT payments_purpose_check,
    ADD CONSTRAINT payments_purpose_check CHECK (purpose IN ('BOOKING', 'SPLIT_SHARE', 'TOURNAMENT_ENTRY',
        'MEMBERSHIP', 'SUBSCRIPTION', 'FEATURED_LISTING', 'DEPOSIT')),
    DROP CONSTRAINT payments_check,
    ADD CONSTRAINT payments_target_check CHECK (num_nonnulls(booking_id, tournament_registration_id, membership_id,
        subscription_invoice_id, featured_listing_id) = CASE WHEN purpose = 'DEPOSIT' THEN 0 ELSE 1 END),
    DROP CONSTRAINT payments_check1,
    ADD CONSTRAINT payments_purpose_target_check CHECK (CASE purpose
        WHEN 'BOOKING' THEN booking_id IS NOT NULL AND booking_participant_id IS NULL
        WHEN 'SPLIT_SHARE' THEN booking_id IS NOT NULL AND booking_participant_id IS NOT NULL
        WHEN 'TOURNAMENT_ENTRY' THEN tournament_registration_id IS NOT NULL
        WHEN 'MEMBERSHIP' THEN membership_id IS NOT NULL
        WHEN 'SUBSCRIPTION' THEN subscription_invoice_id IS NOT NULL
        WHEN 'FEATURED_LISTING' THEN featured_listing_id IS NOT NULL
        WHEN 'DEPOSIT' THEN payer_user_id IS NOT NULL AND booking_participant_id IS NULL
    END);

CREATE TABLE deposit_wallets (
    user_id    uuid PRIMARY KEY REFERENCES users (id),
    balance    numeric(12, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at timestamptz    NOT NULL DEFAULT now(),
    updated_at timestamptz    NOT NULL DEFAULT now()
);

-- A player taking their whole deposit back. FutSlot sends it to send_to once due_at (3 days on) has passed.
CREATE TABLE deposit_withdrawals (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id    uuid           NOT NULL REFERENCES users (id),
    amount     numeric(12, 2) NOT NULL CHECK (amount > 0),
    method     text           NOT NULL CHECK (method IN ('ESEWA', 'KHALTI', 'BANK_TRANSFER')),
    send_to    text           NOT NULL,
    status     text           NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED', 'PAID', 'CANCELLED')),
    due_at     timestamptz    NOT NULL,
    reference  text,
    paid_by    uuid REFERENCES users (id),
    paid_at    timestamptz,
    created_at timestamptz    NOT NULL DEFAULT now(),
    updated_at timestamptz    NOT NULL DEFAULT now(),
    CHECK (status <> 'PAID' OR paid_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_deposit_withdrawals_open ON deposit_withdrawals (user_id) WHERE status = 'REQUESTED';
CREATE INDEX idx_deposit_withdrawals_due ON deposit_withdrawals (due_at) WHERE status = 'REQUESTED';

-- Who FutSlot paid a payout to, and that it was paid by hand
ALTER TABLE payouts ADD COLUMN paid_by uuid REFERENCES users (id);

-- Every change to a deposit: money in is positive, money out negative
CREATE TABLE deposit_entries (
    id            uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id       uuid           NOT NULL REFERENCES users (id),
    type          text           NOT NULL CHECK (type IN ('TOP_UP', 'FEE', 'WITHDRAWAL', 'WITHDRAWAL_CANCELLED')),
    amount        numeric(12, 2) NOT NULL,
    balance_after numeric(12, 2) NOT NULL CHECK (balance_after >= 0),
    payment_id    uuid REFERENCES payments (id),
    booking_id    uuid REFERENCES bookings (id),
    venue_id      uuid REFERENCES venues (id),
    withdrawal_id uuid REFERENCES deposit_withdrawals (id),
    payout_id     uuid REFERENCES payouts (id),
    created_at    timestamptz    NOT NULL DEFAULT now(),
    CHECK (CASE type
        WHEN 'TOP_UP' THEN payment_id IS NOT NULL AND amount > 0
        WHEN 'FEE' THEN booking_id IS NOT NULL AND venue_id IS NOT NULL AND amount < 0
        WHEN 'WITHDRAWAL' THEN withdrawal_id IS NOT NULL AND amount < 0
        WHEN 'WITHDRAWAL_CANCELLED' THEN withdrawal_id IS NOT NULL AND amount > 0
    END),
    CHECK (payout_id IS NULL OR type = 'FEE')
);
CREATE INDEX idx_deposit_entries_user ON deposit_entries (user_id, created_at DESC);
CREATE UNIQUE INDEX uq_deposit_entries_fee ON deposit_entries (booking_id) WHERE type = 'FEE';
CREATE INDEX idx_deposit_entries_unpaid_fees ON deposit_entries (venue_id, created_at)
    WHERE type = 'FEE' AND payout_id IS NULL;
