-- A venue's own website: which template, where it lives and how it looks. One per venue.
CREATE TABLE venue_sites (
    venue_id      uuid PRIMARY KEY REFERENCES venues (id) ON DELETE CASCADE,
    template      text        NOT NULL CHECK (template IN ('TURF', 'FLOODLIGHT')),
    -- dhuku in dhuku.futslot.com
    subdomain     text        NOT NULL UNIQUE CHECK (subdomain ~ '^[a-z0-9]([a-z0-9-]{1,38}[a-z0-9])$'),
    -- e.g. dhukufutsal.com; lower case, no www
    custom_domain text UNIQUE,
    accent_color  text CHECK (accent_color ~ '^#[0-9a-f]{6}$'),
    headline      text CHECK (char_length(headline) <= 120),
    published     boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);
