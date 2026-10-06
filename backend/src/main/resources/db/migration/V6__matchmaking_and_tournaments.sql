-- "Need 2 players for tonight" / "Looking for an opponent on Saturday"
CREATE TABLE match_posts (
    id             uuid PRIMARY KEY DEFAULT uuidv7(),
    type           text        NOT NULL CHECK (type IN ('NEED_PLAYERS', 'FIND_OPPONENT')),
    created_by     uuid        NOT NULL REFERENCES users (id),
    team_id        uuid REFERENCES teams (id),
    booking_id     uuid REFERENCES bookings (id),
    venue_id       uuid REFERENCES venues (id),
    city           text        NOT NULL,
    starts_at      timestamptz NOT NULL,
    players_needed int CHECK (players_needed > 0),
    skill_level    text CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'PRO')),
    cost_per_player numeric(12, 2) CHECK (cost_per_player >= 0),
    note           text,
    status         text        NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FILLED', 'CANCELLED', 'EXPIRED')),
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CHECK (type <> 'NEED_PLAYERS' OR players_needed IS NOT NULL),
    CHECK (type <> 'FIND_OPPONENT' OR team_id IS NOT NULL)
);
CREATE INDEX idx_match_posts_open ON match_posts (city, starts_at) WHERE status = 'OPEN';

CREATE TABLE match_post_responses (
    id         uuid PRIMARY KEY DEFAULT uuidv7(),
    post_id    uuid        NOT NULL REFERENCES match_posts (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES users (id),
    team_id    uuid REFERENCES teams (id),
    message    text,
    status     text        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (post_id, user_id)
);

CREATE TABLE tournaments (
    id                     uuid PRIMARY KEY DEFAULT uuidv7(),
    venue_id               uuid           NOT NULL REFERENCES venues (id),
    organizer_id           uuid           NOT NULL REFERENCES users (id),
    name                   text           NOT NULL,
    slug                   text           NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    description            text,
    rules                  text,
    banner_url             text,
    format                 text           NOT NULL CHECK (format IN ('KNOCKOUT', 'LEAGUE', 'GROUP_KNOCKOUT')),
    team_size              int            NOT NULL DEFAULT 5 CHECK (team_size BETWEEN 3 AND 11),
    max_squad_size         int            NOT NULL DEFAULT 8 CHECK (max_squad_size >= team_size),
    max_teams              int            NOT NULL CHECK (max_teams >= 2),
    entry_fee              numeric(12, 2) NOT NULL DEFAULT 0 CHECK (entry_fee >= 0),
    prize_details          text,
    points_for_win         int            NOT NULL DEFAULT 3,
    points_for_draw        int            NOT NULL DEFAULT 1,
    registration_opens_at  timestamptz,
    registration_closes_at timestamptz,
    starts_on              date           NOT NULL,
    ends_on                date           NOT NULL,
    status                 text           NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at             timestamptz    NOT NULL DEFAULT now(),
    updated_at             timestamptz    NOT NULL DEFAULT now(),
    CHECK (ends_on >= starts_on)
);
CREATE INDEX idx_tournaments_venue ON tournaments (venue_id, starts_on);
CREATE INDEX idx_tournaments_status ON tournaments (status, starts_on);

CREATE TABLE tournament_registrations (
    id            uuid PRIMARY KEY DEFAULT uuidv7(),
    tournament_id uuid        NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    team_id       uuid        NOT NULL REFERENCES teams (id),
    registered_by uuid        NOT NULL REFERENCES users (id),
    group_name    text,
    seed          int,
    status        text        NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'WAITLISTED', 'WITHDRAWN', 'REJECTED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tournament_id, team_id)
);

-- Squad list a team submits for a tournament
CREATE TABLE tournament_squad_members (
    registration_id uuid NOT NULL REFERENCES tournament_registrations (id) ON DELETE CASCADE,
    user_id         uuid NOT NULL REFERENCES users (id),
    jersey_number   int CHECK (jersey_number BETWEEN 0 AND 99),
    PRIMARY KEY (registration_id, user_id)
);

-- Fixtures. Knockout winners advance via next_match_id. Standings are computed from completed matches.
CREATE TABLE tournament_matches (
    id                     uuid PRIMARY KEY DEFAULT uuidv7(),
    tournament_id          uuid        NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    stage                  text        NOT NULL CHECK (stage IN ('GROUP', 'LEAGUE', 'ROUND_OF_32', 'ROUND_OF_16', 'QUARTER_FINAL', 'SEMI_FINAL', 'THIRD_PLACE', 'FINAL')),
    group_name             text,
    round                  int         NOT NULL DEFAULT 1,
    match_number           int         NOT NULL,
    home_registration_id   uuid REFERENCES tournament_registrations (id),
    away_registration_id   uuid REFERENCES tournament_registrations (id),
    court_id               uuid REFERENCES courts (id),
    booking_id             uuid UNIQUE REFERENCES bookings (id),
    scheduled_at           timestamptz,
    home_score             int CHECK (home_score >= 0),
    away_score             int CHECK (away_score >= 0),
    home_penalties         int CHECK (home_penalties >= 0),
    away_penalties         int CHECK (away_penalties >= 0),
    winner_registration_id uuid REFERENCES tournament_registrations (id),
    next_match_id          uuid REFERENCES tournament_matches (id),
    status                 text        NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'WALKOVER', 'POSTPONED', 'CANCELLED')),
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tournament_id, match_number),
    CHECK (home_registration_id IS NULL OR home_registration_id <> away_registration_id)
);
CREATE INDEX idx_tournament_matches_tournament ON tournament_matches (tournament_id, stage, round);
