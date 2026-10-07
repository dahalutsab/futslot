-- A scheduled tournament match blocks its court for match_minutes, so the slot can't be booked meanwhile.
ALTER TABLE tournaments
    ADD COLUMN match_minutes int NOT NULL DEFAULT 60 CHECK (match_minutes BETWEEN 10 AND 240),
    -- GROUP_KNOCKOUT only: how many groups; the top two of each go through to the knockout
    ADD COLUMN group_count   int CHECK (group_count IN (2, 4, 8));

ALTER TABLE tournament_matches
    ADD COLUMN court_block_id uuid REFERENCES court_blocks (id) ON DELETE SET NULL;
