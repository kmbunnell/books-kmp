-- Explicit table grants required by Supabase's upcoming PostgREST/Data API change.
--
-- New Supabase projects no longer expose public-schema tables to the API by
-- default; existing projects must add explicit GRANTs before Oct 30 2026 when
-- the same policy applies to them.
--
-- RLS policies are unchanged — GRANTs let PostgREST see the table; RLS still
-- controls which rows each authenticated user can actually read or write.
-- service_role bypasses both, so Edge Function RPCs are unaffected.

-- Schema usage (required for PostgREST to resolve table names)
GRANT USAGE ON SCHEMA public TO anon, authenticated;

-- books: full CRUD for signed-in users; RLS restricts each user to their own rows
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.books TO authenticated;

-- tags: full CRUD for signed-in users; RLS restricts each user to their own rows
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.tags TO authenticated;

-- book_tags: no UPDATE (PK-only table); RLS joins through books/tags for ownership
GRANT SELECT, INSERT, DELETE ON TABLE public.book_tags TO authenticated;

-- book_metadata_cache: read-only for authenticated users; writes go through
-- SECURITY DEFINER RPCs called by the lookup-book Edge Function (service_role)
GRANT SELECT ON TABLE public.book_metadata_cache TO authenticated;
