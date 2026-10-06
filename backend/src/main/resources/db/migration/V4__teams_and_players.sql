CREATE TABLE player_profiles (
    user_id        uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    position       text CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD', 'ANY')),
    skill_level    text CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'PRO')),
    preferred_foot text CHECK (preferred_foot IN ('LEFT', 'RIGHT', 'BOTH')),
    city           text,
    bio            text,
    games_played   int         NOT NULL DEFAULT 0,
    is_public      boolean     NOT NULL DEFAULT true,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id          uuid PRIMARY KEY DEFAULT uuidv7(),
    name        text        NOT NULL,
    slug        text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    logo_url    text,
    captain_id  uuid        NOT NULL REFERENCES users (id),
    city        text,
    skill_level text CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'PRO')),
    is_public   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_teams_captain ON teams (captain_id);

CREATE TABLE team_members (
    team_id   uuid        NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id   uuid        NOT NULL REFERENCES users (id),
    role      text        NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('CAPTAIN', 'VICE_CAPTAIN', 'MEMBER')),
    status    text        NOT NULL DEFAULT 'INVITED' CHECK (status IN ('INVITED', 'ACTIVE', 'LEFT', 'REMOVED')),
    joined_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (team_id, user_id)
);
CREATE INDEX idx_team_members_user ON team_members (user_id);
