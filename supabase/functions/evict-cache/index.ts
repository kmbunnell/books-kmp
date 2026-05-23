// evict-cache Edge Function
//
// Deletes stale, low-popularity rows from `book_metadata_cache`.
// Eviction thresholds are configurable via env vars:
//   EVICTION_WINDOW_DAYS  (default: 90) — rows older than this are candidates
//   EVICTION_MIN_LOOKUPS  (default: 3)  — rows with fewer lookups than this are candidates
//
// Returns: { "deleted": N } where N is the number of rows removed.
//
// Intended to be invoked weekly via a pg_cron job (see migration
// 20260523000000_evict_cache_cron.sql). Requires a service-role JWT;
// verify_jwt = true in config.toml validates the JWT at the gateway and
// this handler checks the role claim to enforce service_role-only access.

import { createClient } from "jsr:@supabase/supabase-js@2";

const EVICTION_WINDOW_DAYS =
  parseInt(Deno.env.get("EVICTION_WINDOW_DAYS") ?? "90", 10) || 90;
const EVICTION_MIN_LOOKUPS =
  parseInt(Deno.env.get("EVICTION_MIN_LOOKUPS") ?? "3", 10) || 3;

// SECURITY: This function decodes the JWT payload without verifying the signature.
// Signature verification is delegated to the Supabase gateway (verify_jwt = true in
// config.toml). Do NOT set verify_jwt = false for this function — doing so would allow
// a forged JWT to bypass the service_role check below.
function getJwtRole(authHeader: string): string | null {
  try {
    const token = authHeader.replace(/^Bearer\s+/i, "").trim();
    const payload = token.split(".")[1];
    const decoded = JSON.parse(
      atob(payload.replace(/-/g, "+").replace(/_/g, "/")),
    );
    return decoded.role ?? null;
  } catch {
    return null;
  }
}

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

  const authHeader = req.headers.get("Authorization") ?? "";
  if (getJwtRole(authHeader) !== "service_role") {
    return jsonResponse({ error: "Forbidden" }, 403);
  }

  const admin = createClient(url, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const cutoffMs = EVICTION_WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const cutoff = new Date(Date.now() - cutoffMs).toISOString();

  const { count, error } = await admin
    .from("book_metadata_cache")
    .delete({ count: "exact" })
    .lt("last_fetched_at", cutoff)
    .lt("lookup_count", EVICTION_MIN_LOOKUPS);

  if (error) {
    console.error("Eviction failed:", error.message, error.code);
    return jsonResponse({ error: "Eviction failed" }, 500);
  }

  return jsonResponse({ deleted: count ?? 0 }, 200);
});
