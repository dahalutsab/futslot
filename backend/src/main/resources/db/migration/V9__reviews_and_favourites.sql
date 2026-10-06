-- One review per completed booking. venues.average_rating / review_count are updated by the application.
CREATE TABLE reviews (
    id               uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id         uuid        NOT NULL REFERENCES venues (id),
    user_id          uuid        NOT NULL REFERENCES users (id),
    booking_id       uuid        NOT NULL UNIQUE REFERENCES bookings (id),
    rating           smallint    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          text,
    owner_reply      text,
    owner_replied_at timestamptz,
    status           text        NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED')),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_venue ON reviews (venue_id, created_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_reviews_user ON reviews (user_id);

CREATE TABLE favourite_venues (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    venue_id   uuid        NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, venue_id)
);
CREATE INDEX idx_favourite_venues_venue ON favourite_venues (venue_id);
