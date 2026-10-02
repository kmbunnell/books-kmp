-- Explicit table grant for public.profiles (see 20260527000000_grant_table_access.sql).
--
-- SELECT/INSERT/UPDATE only (no DELETE). RLS policies still restrict each
-- authenticated user to their own row.
GRANT SELECT, INSERT, UPDATE ON TABLE public.profiles TO authenticated;
