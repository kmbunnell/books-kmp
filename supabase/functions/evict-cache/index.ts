// evict-cache Edge Function
//
// Deletes stale, low-popularity rows from `book_metadata_cache`:
//   last_fetched_at < now() - INTERVAL '90 days' AND lookup_count < 3
//
// Returns: { "deleted": N } where N is the number of rows removed.
//
// Intended to be invoked weekly via a pg_cron job (see migration
// 20260523000000_evict_cache_cron.sql). Requires the service role key
// (auto-injected as SUPABASE_SERVICE_ROLE_KEY in the Edge Runtime) to
// bypass RLS for the DELETE.

import { createClient } from "jsr:@supabase/supabase-js@2";

const NINETY_DAYS_MS = 90 * 24 * 60 * 60 * 1000;
const MIN_LOOKUP_COUNT = 3;

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const url = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceRoleKey) {
    return jsonResponse({ error: "Service misconfigured" }, 500);
  }

  const callerKey = (req.headers.get("Authorization") ?? "")
    .replace(/^Bearer\s+/i, "")
    .trim();
  if (callerKey !== serviceRoleKey) {
    return jsonResponse({ error: "Forbidden" }, 403);
  }

  const admin = createClient(url, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const cutoff = new Date(Date.now() - NINETY_DAYS_MS).toISOString();

  const { count, error } = await admin
    .from("book_metadata_cache")
    .delete({ count: "exact" })
    .lt("last_fetched_at", cutoff)
    .lt("lookup_count", MIN_LOOKUP_COUNT);

  if (error) {
    console.error("Eviction failed:", error.message, error.code);
    return jsonResponse({ error: "Eviction failed" }, 500);
  }

  return jsonResponse({ deleted: count ?? 0 }, 200);
});
