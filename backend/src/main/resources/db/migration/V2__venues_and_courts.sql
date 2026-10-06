CREATE TABLE venues (
    id                       uuid PRIMARY KEY DEFAULT uuidv7(),
    owner_id                 uuid          NOT NULL REFERENCES users (id),
    name                     text          NOT NULL,
    slug                     text          NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    description              text,
    phone                    text,
    email                    citext,
    address_line             text          NOT NULL,
    area                     text,
    city                     text          NOT NULL,
    latitude                 numeric(9, 6) CHECK (latitude BETWEEN -90 AND 90),
    longitude                numeric(9, 6) CHECK (longitude BETWEEN -180 AND 180),
    timezone                 text          NOT NULL DEFAULT 'Asia/Kathmandu',
    status                   text          NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'REJECTED')),
    rejection_reason         text,
    approved_by              uuid REFERENCES users (id),
    approved_at              timestamptz,
    -- Booking and cancellation policy
    advance_percent          int           NOT NULL DEFAULT 0 CHECK (advance_percent BETWEEN 0 AND 100),
    booking_hold_minutes     int           NOT NULL DEFAULT 15 CHECK (booking_hold_minutes > 0),
    free_cancel_hours        int           NOT NULL DEFAULT 24 CHECK (free_cancel_hours >= 0),
    late_cancel_refund_pct   int           NOT NULL DEFAULT 0 CHECK (late_cancel_refund_pct BETWEEN 0 AND 100),
    max_advance_booking_days int           NOT NULL DEFAULT 30 CHECK (max_advance_booking_days > 0),
    -- Denormalised from reviews for fast listing
    average_rating           numeric(3, 2) NOT NULL DEFAULT 0,
    review_count             int           NOT NULL DEFAULT 0,
    created_at               timestamptz   NOT NULL DEFAULT now(),
    updated_at               timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_venues_owner ON venues (owner_id);
CREATE INDEX idx_venues_city_status ON venues (city, status);

CREATE TABLE venue_photos (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id   uuid        NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    url        text        NOT NULL,
    caption    text,
    sort_order int         NOT NULL DEFAULT 0,
    is_cover   boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_venue_photos_venue ON venue_photos (venue_id, sort_order);
CREATE UNIQUE INDEX uq_venue_photos_one_cover ON venue_photos (venue_id) WHERE is_cover;

CREATE TABLE amenities (
    code text PRIMARY KEY,
    name text NOT NULL,
    icon text
);
INSERT INTO amenities (code, name, icon) VALUES
    ('PARKING', 'Parking', 'local_parking'),
    ('CHANGING_ROOM', 'Changing room', 'checkroom'),
    ('SHOWER', 'Shower', 'shower'),
    ('DRINKING_WATER', 'Drinking water', 'water_drop'),
    ('CAFE', 'Cafe / canteen', 'local_cafe'),
    ('FLOODLIGHTS', 'Floodlights', 'light'),
    ('WIFI', 'Wi-Fi', 'wifi'),
    ('FIRST_AID', 'First aid', 'medical_services'),
    ('EQUIPMENT_RENTAL', 'Equipment rental', 'sports_soccer'),
    ('SPECTATOR_SEATING', 'Spectator seating', 'event_seat');

CREATE TABLE venue_amenities (
    venue_id     uuid NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    amenity_code text NOT NULL REFERENCES amenities (code),
    PRIMARY KEY (venue_id, amenity_code)
);

-- Owners invite staff; permissions limit what each staff member can do
CREATE TABLE venue_staff (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id    uuid        NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    user_id     uuid        NOT NULL REFERENCES users (id),
    role        text        NOT NULL CHECK (role IN ('MANAGER', 'RECEPTIONIST')),
    permissions text[]      NOT NULL DEFAULT '{}',
    active      boolean     NOT NULL DEFAULT true,
    invited_by  uuid        NOT NULL REFERENCES users (id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (venue_id, user_id)
);
CREATE INDEX idx_venue_staff_user ON venue_staff (user_id);

-- ISO day of week: 1 = Monday ... 7 = Sunday. close_time before open_time means open past midnight.
CREATE TABLE operating_hours (
    venue_id    uuid    NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time   time    NOT NULL,
    close_time  time    NOT NULL,
    is_closed   boolean NOT NULL DEFAULT false,
    PRIMARY KEY (venue_id, day_of_week)
);

CREATE TABLE courts (
    id                    uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id              uuid        NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    name                  text        NOT NULL,
    format                text        NOT NULL DEFAULT 'FIVE_A_SIDE'
        CHECK (format IN ('FIVE_A_SIDE', 'SIX_A_SIDE', 'SEVEN_A_SIDE')),
    surface               text        NOT NULL DEFAULT 'ARTIFICIAL_TURF'
        CHECK (surface IN ('ARTIFICIAL_TURF', 'HARDWOOD', 'CONCRETE', 'RUBBER')),
    is_indoor             boolean     NOT NULL DEFAULT false,
    slot_duration_minutes int         NOT NULL DEFAULT 60 CHECK (slot_duration_minutes BETWEEN 15 AND 240),
    status                text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    sort_order            int         NOT NULL DEFAULT 0,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),
    UNIQUE (venue_id, name)
);

-- Price for a court in a local time window. NULL day_of_week = every day.
-- When several rules match, the highest priority wins (lets owners layer peak/weekend/holiday prices).
CREATE TABLE pricing_rules (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    court_id    uuid           NOT NULL REFERENCES courts (id) ON DELETE CASCADE,
    label       text           NOT NULL DEFAULT 'STANDARD' CHECK (label IN ('STANDARD', 'PEAK', 'OFF_PEAK', 'WEEKEND', 'HOLIDAY')),
    day_of_week smallint CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  time           NOT NULL,
    end_time    time           NOT NULL,
    price       numeric(12, 2) NOT NULL CHECK (price >= 0),
    priority    int            NOT NULL DEFAULT 0,
    valid_from  date,
    valid_to    date,
    active      boolean        NOT NULL DEFAULT true,
    created_at  timestamptz    NOT NULL DEFAULT now(),
    updated_at  timestamptz    NOT NULL DEFAULT now(),
    CHECK (end_time > start_time),
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)
);
CREATE INDEX idx_pricing_rules_court ON pricing_rules (court_id) WHERE active;

-- Maintenance, private events, holidays: time the court cannot be booked
CREATE TABLE court_blocks (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    court_id   uuid        NOT NULL REFERENCES courts (id) ON DELETE CASCADE,
    starts_at  timestamptz NOT NULL,
    ends_at    timestamptz NOT NULL,
    reason     text        NOT NULL CHECK (reason IN ('MAINTENANCE', 'PRIVATE_EVENT', 'HOLIDAY', 'TOURNAMENT', 'OTHER')),
    note       text,
    created_by uuid        NOT NULL REFERENCES users (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    EXCLUDE USING gist (court_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
);

-- Rentable extras and canteen items sold with a booking
CREATE TABLE add_on_items (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id   uuid           NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    name       text           NOT NULL,
    category   text           NOT NULL CHECK (category IN ('EQUIPMENT_RENTAL', 'FOOD_DRINK', 'OTHER')),
    price      numeric(12, 2) NOT NULL CHECK (price >= 0),
    active     boolean        NOT NULL DEFAULT true,
    created_at timestamptz    NOT NULL DEFAULT now(),
    updated_at timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_add_on_items_venue ON add_on_items (venue_id) WHERE active;
