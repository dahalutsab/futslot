-- Settlement to venues for money FutSlot collected online, minus commission and refunds
CREATE TABLE payouts (
    id                uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id          uuid           NOT NULL REFERENCES venues (id),
    period_start      date           NOT NULL,
    period_end        date           NOT NULL,
    gross_amount      numeric(12, 2) NOT NULL CHECK (gross_amount >= 0),
    commission_amount numeric(12, 2) NOT NULL DEFAULT 0 CHECK (commission_amount >= 0),
    refund_amount     numeric(12, 2) NOT NULL DEFAULT 0 CHECK (refund_amount >= 0),
    net_amount        numeric(12, 2) NOT NULL,
    status            text           NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED')),
    method            text CHECK (method IN ('BANK_TRANSFER', 'ESEWA', 'KHALTI')),
    transfer_reference text,
    paid_at           timestamptz,
    created_at        timestamptz    NOT NULL DEFAULT now(),
    updated_at        timestamptz    NOT NULL DEFAULT now(),
    CHECK (period_end >= period_start),
    CHECK (net_amount = gross_amount - commission_amount - refund_amount),
    UNIQUE (venue_id, period_start, period_end)
);

-- One row per payment attempt. Exactly one target (what is being paid for) is set, matching purpose.
-- Cash and manual entries are recorded by owners/staff (recorded_by); online ones come from a gateway.
CREATE TABLE payments (
    id                         uuid PRIMARY KEY DEFAULT uuidv7(),
    purpose                    text           NOT NULL
        CHECK (purpose IN ('BOOKING', 'SPLIT_SHARE', 'TOURNAMENT_ENTRY', 'MEMBERSHIP', 'SUBSCRIPTION', 'FEATURED_LISTING')),
    booking_id                 uuid REFERENCES bookings (id),
    booking_participant_id     uuid REFERENCES booking_participants (id),
    tournament_registration_id uuid REFERENCES tournament_registrations (id),
    membership_id              uuid REFERENCES memberships (id),
    subscription_invoice_id    uuid REFERENCES subscription_invoices (id),
    featured_listing_id        uuid REFERENCES featured_listings (id),
    venue_id                   uuid REFERENCES venues (id),
    payer_user_id              uuid REFERENCES users (id),
    amount                     numeric(12, 2) NOT NULL CHECK (amount > 0),
    currency                   char(3)        NOT NULL DEFAULT 'NPR',
    method                     text           NOT NULL
        CHECK (method IN ('CASH', 'ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER', 'CARD', 'MEMBERSHIP_CREDIT')),
    status                     text           NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN ('INITIATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    gateway_transaction_id     text,
    gateway_response           jsonb,
    failure_reason             text,
    recorded_by                uuid REFERENCES users (id),
    payout_id                  uuid REFERENCES payouts (id),
    paid_at                    timestamptz,
    created_at                 timestamptz    NOT NULL DEFAULT now(),
    updated_at                 timestamptz    NOT NULL DEFAULT now(),
    CHECK (num_nonnulls(booking_id, tournament_registration_id, membership_id, subscription_invoice_id, featured_listing_id) = 1),
    CHECK (CASE purpose
        WHEN 'BOOKING' THEN booking_id IS NOT NULL AND booking_participant_id IS NULL
        WHEN 'SPLIT_SHARE' THEN booking_id IS NOT NULL AND booking_participant_id IS NOT NULL
        WHEN 'TOURNAMENT_ENTRY' THEN tournament_registration_id IS NOT NULL
        WHEN 'MEMBERSHIP' THEN membership_id IS NOT NULL
        WHEN 'SUBSCRIPTION' THEN subscription_invoice_id IS NOT NULL
        WHEN 'FEATURED_LISTING' THEN featured_listing_id IS NOT NULL
    END),
    CHECK (status <> 'SUCCEEDED' OR paid_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_payments_gateway_txn ON payments (method, gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL;
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_venue_paid ON payments (venue_id, paid_at);
CREATE INDEX idx_payments_payer ON payments (payer_user_id, created_at DESC);
CREATE INDEX idx_payments_unsettled ON payments (venue_id) WHERE payout_id IS NULL AND status = 'SUCCEEDED';

-- Raw gateway callbacks / verification responses, kept for reconciliation and debugging
CREATE TABLE payment_events (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    payment_id  uuid        NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    event_type  text        NOT NULL,
    payload     jsonb       NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_events_payment ON payment_events (payment_id, received_at);

CREATE TABLE refunds (
    id                uuid PRIMARY KEY DEFAULT uuidv7(),
    payment_id        uuid           NOT NULL REFERENCES payments (id),
    amount            numeric(12, 2) NOT NULL CHECK (amount > 0),
    reason            text           NOT NULL,
    status            text           NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'REJECTED')),
    gateway_refund_id text,
    requested_by      uuid REFERENCES users (id),
    processed_by      uuid REFERENCES users (id),
    processed_at      timestamptz,
    created_at        timestamptz    NOT NULL DEFAULT now(),
    updated_at        timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_refunds_payment ON refunds (payment_id);
