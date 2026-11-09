-- Not applied. The challenge database stays as delivered.
-- Recommended only if the orders table grows enough to measure a difference.
-- Apply manually, and document it, before using it against Neon.

CREATE INDEX IF NOT EXISTS orders_user_status_instrument_idx
    ON orders (userid, status, instrumentid);

CREATE INDEX IF NOT EXISTS marketdata_instrument_date_idx
    ON marketdata (instrumentid, date);
