-- Drop unused columns from book_metadata_cache and slim down the upsert RPC
-- to match. The increment RPC is recreated in-place (same signature, adds
-- SET search_path hardening).

ALTER TABLE book_metadata_cache
    DROP COLUMN IF EXISTS publisher,
    DROP COLUMN IF EXISTS published_date,
    DROP COLUMN IF EXISTS page_count,
    DROP COLUMN IF EXISTS description,
    DROP COLUMN IF EXISTS metadata;

-- Drop old upsert function (signature changed — fewer params — so OR REPLACE
-- is not valid; must drop and recreate).
DROP FUNCTION IF EXISTS upsert_book_metadata_cache(TEXT, TEXT, TEXT[], TEXT, TEXT, TEXT, INTEGER, TEXT, JSONB);

CREATE OR REPLACE FUNCTION increment_book_metadata_cache_lookup(p_isbn TEXT)
RETURNS VOID
LANGUAGE SQL
SECURITY DEFINER
SET search_path = public
AS $$
    UPDATE book_metadata_cache
    SET lookup_count = lookup_count + 1
    WHERE isbn = p_isbn;
$$;

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
    VALUES (p_isbn, p_title, p_authors, p_cover_url, now(), 1)
    ON CONFLICT (isbn) DO UPDATE
        SET title           = EXCLUDED.title,
            authors         = EXCLUDED.authors,
            cover_url       = EXCLUDED.cover_url,
            last_fetched_at = now(),
            lookup_count    = book_metadata_cache.lookup_count + 1;
$$;

GRANT EXECUTE ON FUNCTION increment_book_metadata_cache_lookup(TEXT) TO service_role;
GRANT EXECUTE ON FUNCTION upsert_book_metadata_cache(TEXT, TEXT, TEXT[], TEXT) TO service_role;
