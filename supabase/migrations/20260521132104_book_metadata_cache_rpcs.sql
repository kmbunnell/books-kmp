-- RPCs supporting the `lookup-book` Edge Function.
--
-- Edge Function callers (running as service_role) need atomic upsert with
-- `lookup_count = lookup_count + 1` on conflict — something the PostgREST
-- upsert path can't express. These two SECURITY DEFINER functions provide
-- that atomicity.

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

-- service_role bypasses RLS and has full schema privileges by default; explicit
-- grants below make the call-path discoverable to other roles if needed.
GRANT EXECUTE ON FUNCTION increment_book_metadata_cache_lookup(TEXT) TO service_role;
GRANT EXECUTE ON FUNCTION upsert_book_metadata_cache(TEXT, TEXT, TEXT[], TEXT) TO service_role;
