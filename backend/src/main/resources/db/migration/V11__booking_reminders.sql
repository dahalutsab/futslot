-- Set when the "your game starts soon" reminder goes out, so it is sent exactly once
ALTER TABLE bookings ADD COLUMN reminder_sent_at timestamptz;

CREATE INDEX idx_bookings_reminder_due ON bookings (starts_at)
    WHERE status = 'CONFIRMED' AND reminder_sent_at IS NULL;
