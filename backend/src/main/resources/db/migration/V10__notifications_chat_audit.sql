-- In-app notification inbox; push/SMS/email delivery status is tracked per row
CREATE TABLE notifications (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       text        NOT NULL,
    title      text        NOT NULL,
    body       text        NOT NULL,
    data       jsonb       NOT NULL DEFAULT '{}',
    channel    text        NOT NULL DEFAULT 'PUSH' CHECK (channel IN ('IN_APP', 'PUSH', 'SMS', 'EMAIL')),
    status     text        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at    timestamptz,
    read_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX idx_notifications_pending ON notifications (created_at) WHERE status = 'PENDING';

-- Player <-> venue chat, one thread per pair
CREATE TABLE conversations (
    id              uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id        uuid        NOT NULL REFERENCES venues (id),
    player_id       uuid        NOT NULL REFERENCES users (id),
    last_message_at timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (venue_id, player_id)
);
CREATE INDEX idx_conversations_player ON conversations (player_id, last_message_at DESC);
CREATE INDEX idx_conversations_venue ON conversations (venue_id, last_message_at DESC);

CREATE TABLE messages (
    id              uuid PRIMARY KEY DEFAULT uuidv7(),
    conversation_id uuid        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       uuid        NOT NULL REFERENCES users (id),
    booking_id      uuid REFERENCES bookings (id),
    body            text        NOT NULL CHECK (length(body) BETWEEN 1 AND 4000),
    read_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

-- Who changed what: bookings, payments, venue settings, admin actions
CREATE TABLE audit_logs (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    actor_id    uuid REFERENCES users (id),
    venue_id    uuid REFERENCES venues (id),
    action      text        NOT NULL,
    entity_type text        NOT NULL,
    entity_id   uuid,
    changes     jsonb,
    ip_address  inet,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_venue ON audit_logs (venue_id, created_at DESC);
