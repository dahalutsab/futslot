-- Bookings made on a venue's own website, so owners can see what the site brings in
ALTER TABLE bookings DROP CONSTRAINT bookings_source_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_source_check
    CHECK (source IN ('APP', 'WEBSITE', 'WALK_IN', 'PHONE', 'RECURRING', 'TOURNAMENT', 'ADMIN'));
