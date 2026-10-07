-- Venue customers who later signed up with the same phone number: link them so their bookings show up in
-- their account. New accounts are linked as they're created.
UPDATE venue_customers c
SET user_id = u.id
FROM users u
WHERE c.user_id IS NULL
  AND c.phone = u.phone;
