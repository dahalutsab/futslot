-- How players pay the venue directly (e.g. "eSewa/Khalti 98XXXXXXXX, name Dhuku Futsal"). Shown with
-- the booking so players can pay the advance and submit the transaction id for staff to verify.
ALTER TABLE venues ADD COLUMN payment_instructions text;
