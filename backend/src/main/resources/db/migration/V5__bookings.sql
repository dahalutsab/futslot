-- A weekly standing slot ("Team X every Friday 19:00"). The application materialises
-- individual bookings from it a few weeks ahead, each pointing back via recurring_booking_id.
CREATE TABLE recurring_bookings (
    id           uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id     uuid           NOT NULL REFERENCES venues (id),
    court_id     uuid           NOT NULL REFERENCES courts (id),
    customer_id  uuid           NOT NULL REFERENCES venue_customers (id),
    team_id      uuid REFERENCES teams (id),
    day_of_week  smallint       NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time   time           NOT NULL,
    end_time     time           NOT NULL,
    starts_on    date           NOT NULL,
    ends_on      date,
    price        numeric(12, 2) NOT NULL CHECK (price >= 0),
    status       text           NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    generated_until date,
    created_by   uuid           NOT NULL REFERENCES users (id),
    created_at   timestamptz    NOT NULL DEFAULT now(),
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    CHECK (end_time > start_time),
    CHECK (ends_on IS NULL OR ends_on >= starts_on)
);
CREATE INDEX idx_recurring_bookings_venue ON recurring_bookings (venue_id, status);

-- Every booking belongs to a venue customer (walk-in or app user). booked_by_user_id is set
-- when a player booked through the app; created_by is whoever entered it (player, owner or staff).
CREATE TABLE bookings (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    reference            text           NOT NULL UNIQUE,
    venue_id             uuid           NOT NULL REFERENCES venues (id),
    court_id             uuid           NOT NULL REFERENCES courts (id),
    customer_id          uuid           NOT NULL REFERENCES venue_customers (id),
    booked_by_user_id    uuid REFERENCES users (id),
    team_id              uuid REFERENCES teams (id),
    recurring_booking_id uuid REFERENCES recurring_bookings (id),
    membership_id        uuid REFERENCES memberships (id),
    discount_code_id     uuid REFERENCES discount_codes (id),
    source               text           NOT NULL CHECK (source IN ('APP', 'WALK_IN', 'PHONE', 'RECURRING', 'TOURNAMENT', 'ADMIN')),
    status               text           NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'EXPIRED')),
    starts_at            timestamptz    NOT NULL,
    ends_at              timestamptz    NOT NULL,
    -- Money: total = price + add_ons - discount. paid_amount tracks what has actually been collected.
    price_amount         numeric(12, 2) NOT NULL CHECK (price_amount >= 0),
    add_ons_amount       numeric(12, 2) NOT NULL DEFAULT 0 CHECK (add_ons_amount >= 0),
    discount_amount      numeric(12, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    total_amount         numeric(12, 2) NOT NULL CHECK (total_amount >= 0),
    advance_required     numeric(12, 2) NOT NULL DEFAULT 0 CHECK (advance_required >= 0),
    paid_amount          numeric(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    payment_status       text           NOT NULL DEFAULT 'UNPAID'
        CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    commission_rate      numeric(5, 2)  NOT NULL DEFAULT 0 CHECK (commission_rate BETWEEN 0 AND 100),
    commission_amount    numeric(12, 2) NOT NULL DEFAULT 0 CHECK (commission_amount >= 0),
    hold_expires_at      timestamptz,
    notes                text,
    checked_in_at        timestamptz,
    cancelled_at         timestamptz,
    cancelled_by         uuid REFERENCES users (id),
    cancellation_reason  text,
    created_by           uuid REFERENCES users (id),
    version              bigint         NOT NULL DEFAULT 0,
    created_at           timestamptz    NOT NULL DEFAULT now(),
    updated_at           timestamptz    NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    CHECK (total_amount = price_amount + add_ons_amount - discount_amount),
    CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
    -- The double-booking guarantee: no two live bookings on the same court may overlap.
    -- Half-open ranges [start, end) let back-to-back slots (18:00-19:00, 19:00-20:00) coexist.
    CONSTRAINT bookings_no_overlap EXCLUDE USING gist (
        court_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status NOT IN ('CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_bookings_venue_starts ON bookings (venue_id, starts_at);
CREATE INDEX idx_bookings_court_starts ON bookings (court_id, starts_at);
CREATE INDEX idx_bookings_customer ON bookings (customer_id, starts_at DESC);
CREATE INDEX idx_bookings_booked_by ON bookings (booked_by_user_id, starts_at DESC);
CREATE INDEX idx_bookings_recurring ON bookings (recurring_booking_id);
CREATE INDEX idx_bookings_pending_hold ON bookings (hold_expires_at) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE booking_add_ons (
    booking_id      uuid           NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    add_on_item_id  uuid           NOT NULL REFERENCES add_on_items (id),
    quantity        int            NOT NULL CHECK (quantity > 0),
    unit_price      numeric(12, 2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (booking_id, add_on_item_id)
);

-- Split payment: teammates invited to share a booking's cost
CREATE TABLE booking_participants (
    id           uuid PRIMARY KEY DEFAULT uuidv7(),
    booking_id   uuid           NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    user_id      uuid           NOT NULL REFERENCES users (id),
    share_amount numeric(12, 2) NOT NULL CHECK (share_amount >= 0),
    paid_amount  numeric(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    status       text           NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACCEPTED', 'DECLINED', 'PAID')),
    created_at   timestamptz    NOT NULL DEFAULT now(),
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    UNIQUE (booking_id, user_id)
);
CREATE INDEX idx_booking_participants_user ON booking_participants (user_id);

CREATE TABLE discount_redemptions (
    id               uuid PRIMARY KEY DEFAULT uuidv7(),
    discount_code_id uuid           NOT NULL REFERENCES discount_codes (id),
    booking_id       uuid           NOT NULL UNIQUE REFERENCES bookings (id),
    user_id          uuid REFERENCES users (id),
    amount           numeric(12, 2) NOT NULL CHECK (amount >= 0),
    redeemed_at      timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_discount_redemptions_code_user ON discount_redemptions (discount_code_id, user_id);

-- Players waiting for a fully booked slot; notified in order if it frees up
CREATE TABLE waitlist_entries (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    court_id    uuid        NOT NULL REFERENCES courts (id) ON DELETE CASCADE,
    user_id     uuid        NOT NULL REFERENCES users (id),
    starts_at   timestamptz NOT NULL,
    ends_at     timestamptz NOT NULL,
    status      text        NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'NOTIFIED', 'BOOKED', 'EXPIRED', 'CANCELLED')),
    notified_at timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    UNIQUE (court_id, user_id, starts_at)
);
CREATE INDEX idx_waitlist_slot ON waitlist_entries (court_id, starts_at, created_at) WHERE status = 'WAITING';
