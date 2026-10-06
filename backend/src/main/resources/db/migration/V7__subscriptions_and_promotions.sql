-- FutSlot's own revenue: venue subscription plans and paid featured listings.
-- Plans are managed by platform admins; only the FREE plan is seeded.
CREATE TABLE subscription_plans (
    id                 uuid PRIMARY KEY DEFAULT uuidv7(),
    code               text           NOT NULL UNIQUE CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    name               text           NOT NULL,
    description        text,
    monthly_price      numeric(12, 2) NOT NULL CHECK (monthly_price >= 0),
    yearly_price       numeric(12, 2) CHECK (yearly_price >= 0),
    commission_percent numeric(5, 2)  NOT NULL DEFAULT 0 CHECK (commission_percent BETWEEN 0 AND 100),
    max_courts         int CHECK (max_courts > 0),
    max_staff          int CHECK (max_staff >= 0),
    features           text[]         NOT NULL DEFAULT '{}',
    active             boolean        NOT NULL DEFAULT true,
    sort_order         int            NOT NULL DEFAULT 0,
    created_at         timestamptz    NOT NULL DEFAULT now(),
    updated_at         timestamptz    NOT NULL DEFAULT now()
);
INSERT INTO subscription_plans (code, name, description, monthly_price, max_courts, max_staff, features)
VALUES ('FREE', 'Free', 'Calendar, manual bookings and online listing for one court', 0, 1, 0,
        ARRAY['CALENDAR', 'MANUAL_BOOKINGS', 'ONLINE_LISTING']);

CREATE TABLE venue_subscriptions (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id             uuid        NOT NULL REFERENCES venues (id),
    plan_id              uuid        NOT NULL REFERENCES subscription_plans (id),
    billing_cycle        text        NOT NULL DEFAULT 'MONTHLY' CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    status               text        NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),
    trial_ends_at        timestamptz,
    current_period_start timestamptz NOT NULL,
    current_period_end   timestamptz NOT NULL,
    cancel_at_period_end boolean     NOT NULL DEFAULT false,
    cancelled_at         timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CHECK (current_period_end > current_period_start)
);
-- At most one live subscription per venue
CREATE UNIQUE INDEX uq_venue_subscriptions_live ON venue_subscriptions (venue_id)
    WHERE status IN ('TRIALING', 'ACTIVE', 'PAST_DUE');

CREATE TABLE subscription_invoices (
    id              uuid PRIMARY KEY DEFAULT uuidv7(),
    subscription_id uuid           NOT NULL REFERENCES venue_subscriptions (id),
    invoice_number  text           NOT NULL UNIQUE,
    amount          numeric(12, 2) NOT NULL CHECK (amount >= 0),
    period_start    timestamptz    NOT NULL,
    period_end      timestamptz    NOT NULL,
    due_on          date           NOT NULL,
    status          text           NOT NULL DEFAULT 'DUE' CHECK (status IN ('DUE', 'PAID', 'OVERDUE', 'VOID')),
    paid_at         timestamptz,
    created_at      timestamptz    NOT NULL DEFAULT now(),
    updated_at      timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscription_invoices_subscription ON subscription_invoices (subscription_id, period_start DESC);

CREATE TABLE featured_listings (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id   uuid           NOT NULL REFERENCES venues (id),
    placement  text           NOT NULL CHECK (placement IN ('HOME_CAROUSEL', 'SEARCH_TOP', 'CITY_TOP')),
    city       text,
    starts_at  timestamptz    NOT NULL,
    ends_at    timestamptz    NOT NULL,
    price      numeric(12, 2) NOT NULL CHECK (price >= 0),
    status     text           NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status IN ('PENDING_PAYMENT', 'SCHEDULED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    created_at timestamptz    NOT NULL DEFAULT now(),
    updated_at timestamptz    NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    CHECK (placement <> 'CITY_TOP' OR city IS NOT NULL)
);
CREATE INDEX idx_featured_listings_live ON featured_listings (placement, city, starts_at, ends_at)
    WHERE status IN ('SCHEDULED', 'ACTIVE');
