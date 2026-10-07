-- A plan change on a running subscription takes effect at renewal, so the current period stays as paid for.
ALTER TABLE venue_subscriptions
    ADD COLUMN next_plan_id       uuid REFERENCES subscription_plans (id),
    ADD COLUMN next_billing_cycle text CHECK (next_billing_cycle IN ('MONTHLY', 'YEARLY')),
    ADD CONSTRAINT venue_subscriptions_next_plan CHECK ((next_plan_id IS NULL) = (next_billing_cycle IS NULL));

-- What each invoice is for: the first period, or a renewal (possibly onto a different plan).
ALTER TABLE subscription_invoices
    ADD COLUMN plan_id       uuid REFERENCES subscription_plans (id),
    ADD COLUMN billing_cycle text CHECK (billing_cycle IN ('MONTHLY', 'YEARLY'));
CREATE UNIQUE INDEX uq_subscription_invoices_period ON subscription_invoices (subscription_id, period_start)
    WHERE status <> 'VOID';
