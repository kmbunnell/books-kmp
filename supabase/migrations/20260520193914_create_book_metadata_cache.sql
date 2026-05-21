CREATE TABLE IF NOT EXISTS book_metadata_cache (
    isbn             TEXT        PRIMARY KEY,
    title            TEXT        NOT NULL,
    authors          TEXT[],
    cover_url        TEXT,
    last_fetched_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    lookup_count     INTEGER     NOT NULL DEFAULT 1
);

ALTER TABLE book_metadata_cache ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can read book metadata cache"
    ON book_metadata_cache FOR SELECT
    TO authenticated
    USING (true);

CREATE INDEX IF NOT EXISTS idx_book_metadata_cache_last_fetched_at
    ON book_metadata_cache(last_fetched_at);

CREATE INDEX IF NOT EXISTS idx_book_metadata_cache_lookup_count
    ON book_metadata_cache(lookup_count);
