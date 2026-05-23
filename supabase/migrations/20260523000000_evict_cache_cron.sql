-- Weekly eviction of stale, low-popularity rows from book_metadata_cache.
--
-- Prerequisites (one-time Dashboard steps; not applied by this migration):
--   1. Supabase Dashboard → Vault → New Secret
--        name:  service_role_key
--        value: <your project's service role key>
--   2. Supabase Dashboard → Vault → New Secret
--        name:  project_url
--        value: https://<your-project-ref>.supabase.co
--   3. Supabase Dashboard → Database → Extensions: ensure pg_cron and pg_net
--      are enabled.
--
-- Both the project URL and service role key are read from Vault at run time
-- and are never written to git.
--
-- Local dev note: cron.schedule will register the job, but net.http_post
-- cannot reach the local Edge Function from inside Postgres. Verify the
-- function by direct curl invocation; verify the cron registration via
--   SELECT * FROM cron.job WHERE jobname = 'evict-stale-book-cache';

CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;
CREATE EXTENSION IF NOT EXISTS supabase_vault;

SELECT cron.unschedule('evict-stale-book-cache')
WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'evict-stale-book-cache');

SELECT cron.schedule(
  'evict-stale-book-cache',
  '0 2 * * 0', -- UTC Sunday, low-traffic window
  $$ SELECT net.http_post(
       url     := (
         SELECT decrypted_secret FROM vault.decrypted_secrets
         WHERE name = 'project_url'
       ) || '/functions/v1/evict-cache',
       headers := jsonb_build_object(
         'Authorization', 'Bearer ' || (
           SELECT decrypted_secret FROM vault.decrypted_secrets
           WHERE name = 'service_role_key'
         ),
         'Content-Type', 'application/json'
       ),
       body    := '{}'::jsonb
     ) $$
);
