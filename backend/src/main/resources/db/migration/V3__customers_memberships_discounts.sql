-- A venue's own customer book. Walk-in and phone customers often have no app account,
-- so user_id is optional and linked later when the phone number signs up.
CREATE TABLE venue_customers (
    id              uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id        uuid        NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    user_id         uuid REFERENCES users (id),
    name            text        NOT NULL,
    phone           text        NOT NULL,
    notes           text,
    total_bookings  int         NOT NULL DEFAULT 0,
    last_booking_at timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (venue_id, phone)
);
CREATE INDEX idx_venue_customers_user ON venue_customers (user_id);

CREATE TABLE membership_plans (
    id               uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id         uuid           NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    name             text           NOT NULL,
    description      text,
    type             text           NOT NULL CHECK (type IN ('GAME_PACK', 'MONTHLY_PASS')),
    games_included   int CHECK (games_included > 0),
    discount_percent int CHECK (discount_percent BETWEEN 0 AND 100),
    validity_days    int            NOT NULL CHECK (validity_days > 0),
    price            numeric(12, 2) NOT NULL CHECK (price >= 0),
    active           boolean        NOT NULL DEFAULT true,
    created_at       timestamptz    NOT NULL DEFAULT now(),
    updated_at       timestamptz    NOT NULL DEFAULT now(),
    CHECK (type <> 'GAME_PACK' OR games_included IS NOT NULL)
);
CREATE INDEX idx_membership_plans_venue ON membership_plans (venue_id) WHERE active;

CREATE TABLE memberships (
    id              uuid PRIMARY KEY DEFAULT uuidv7(),
    plan_id         uuid        NOT NULL REFERENCES membership_plans (id),
    venue_id        uuid        NOT NULL REFERENCES venues (id),
    customer_id     uuid        NOT NULL REFERENCES venue_customers (id),
    starts_on       date        NOT NULL,
    expires_on      date        NOT NULL,
    games_remaining int CHECK (games_remaining >= 0),
    status          text        NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CHECK (expires_on >= starts_on)
);
CREATE INDEX idx_memberships_customer ON memberships (customer_id, status);
CREATE INDEX idx_memberships_venue ON memberships (venue_id, status);

-- venue_id NULL = platform-wide code issued by FutSlot. Codes are stored upper-case.
CREATE TABLE discount_codes (
    id                  uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id            uuid REFERENCES venues (id) ON DELETE CASCADE,
    code                text           NOT NULL CHECK (code = upper(code) AND length(code) BETWEEN 3 AND 32),
    description         text,
    type                text           NOT NULL CHECK (type IN ('PERCENT', 'FIXED')),
    value               numeric(12, 2) NOT NULL CHECK (value > 0),
    max_discount_amount numeric(12, 2) CHECK (max_discount_amount > 0),
    min_booking_amount  numeric(12, 2) CHECK (min_booking_amount >= 0),
    valid_from          timestamptz,
    valid_to            timestamptz,
    usage_limit         int CHECK (usage_limit > 0),
    per_user_limit      int CHECK (per_user_limit > 0),
    used_count          int            NOT NULL DEFAULT 0,
    active              boolean        NOT NULL DEFAULT true,
    created_by          uuid           NOT NULL REFERENCES users (id),
    created_at          timestamptz    NOT NULL DEFAULT now(),
    updated_at          timestamptz    NOT NULL DEFAULT now(),
    CHECK (type <> 'PERCENT' OR value <= 100),
    UNIQUE NULLS NOT DISTINCT (venue_id, code)
);
