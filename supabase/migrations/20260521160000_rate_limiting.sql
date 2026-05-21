-- Per-user rate limiting for the `lookup-book` Edge Function.
--
-- check_and_increment_rate_limit atomically increments the request count for
-- the given time window and returns TRUE if the request is within the limit.
-- The Edge Function calls this twice per request: once for the hourly limit
-- (200/hour) and once for the daily limit (500/day). Either returning FALSE
-- causes a 429 response.
--
-- Old rows accumulate but are never read again once their window passes.
-- A cleanup job (pg_cron) can prune rows older than 30 days when needed.

CREATE TABLE IF NOT EXISTS user_lookup_rate_limits (
    user_id       UUID         NOT NULL,
    window_start  TIMESTAMPTZ  NOT NULL,
    request_count INTEGER      NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, window_start)
);

-- No policies needed — service_role bypasses RLS and is the only caller.
-- This blocks direct PostgREST access from anon/authenticated clients,
-- preventing users from deleting their own rate limit rows.
ALTER TABLE user_lookup_rate_limits ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION check_and_increment_rate_limit(
    p_user_id UUID,
    p_limit   INTEGER,
    p_window  TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_allowed BOOLEAN;
BEGIN
    IF p_window NOT IN ('hour', 'day') THEN
        RAISE EXCEPTION 'Invalid window value: %. Must be ''hour'' or ''day''.', p_window;
    END IF;

    INSERT INTO user_lookup_rate_limits (user_id, window_start, request_count)
    VALUES (p_user_id, date_trunc(p_window, now()), 1)
    ON CONFLICT (user_id, window_start) DO UPDATE
        SET request_count = LEAST(user_lookup_rate_limits.request_count + 1, p_limit + 1)
    RETURNING request_count <= p_limit INTO v_allowed;

    RETURN v_allowed;
END;
$$;

GRANT EXECUTE ON FUNCTION check_and_increment_rate_limit(UUID, INTEGER, TEXT) TO service_role;
