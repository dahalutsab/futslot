-- Signing in with a password (users.password_hash, bcrypt) instead of a texted code each time; a code is still
-- texted once to prove the phone at sign-up, and to reset a forgotten password. failed_sign_ins wrong passwords
-- in a row lock the account until sign_in_locked_until. google_subject links a Google account ("sub" claim).
ALTER TABLE users
    ADD COLUMN failed_sign_ins      int NOT NULL DEFAULT 0 CHECK (failed_sign_ins >= 0),
    ADD COLUMN sign_in_locked_until timestamptz,
    ADD COLUMN password_changed_at  timestamptz,
    ADD COLUMN google_subject       text UNIQUE;
