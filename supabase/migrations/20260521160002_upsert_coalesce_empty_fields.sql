-- Replaces upsert_book_metadata_cache with two improvements:
-- (1) silently skips the insert when the cache table exceeds 500,000 rows;
-- (2) empty/null values from Google Books never overwrite previously cached
--     good data — existing title, authors, and cover_url are preserved.

CREATE OR REPLACE FUNCTION upsert_book_metadata_cache(
    p_isbn      TEXT,
    p_title     TEXT,
    p_authors   TEXT[],
    p_cover_url TEXT
)
RETURNS VOID
LANGUAGE SQL
SECURITY DEFINER
SET search_path = public
AS $$
    INSERT INTO book_metadata_cache (isbn, title, authors, cover_url, last_fetched_at, lookup_count)
    SELECT p_isbn, p_title, p_authors, p_cover_url, now(), 1
    WHERE (SELECT reltuples FROM pg_class WHERE relname = 'book_metadata_cache') < 500000
    ON CONFLICT (isbn) DO UPDATE
        SET title           = CASE WHEN EXCLUDED.title <> '' THEN EXCLUDED.title ELSE book_metadata_cache.title END,
            authors         = CASE WHEN array_length(EXCLUDED.authors, 1) > 0 THEN EXCLUDED.authors ELSE book_metadata_cache.authors END,
            cover_url       = COALESCE(EXCLUDED.cover_url, book_metadata_cache.cover_url),
            last_fetched_at = now(),
            lookup_count    = book_metadata_cache.lookup_count + 1;
$$;
