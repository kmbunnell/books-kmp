// Integration tests for the `evict-cache` Edge Function.
//
// Prerequisites (run before `deno test`):
//   1. `supabase start`                        (local stack on :54321/:54322)
//   2. `supabase functions serve evict-cache`  (function on :54321/functions/v1)
//
// Run:
//   deno test --allow-net --allow-env supabase/functions/evict-cache/index.test.ts

import { assertEquals } from "jsr:@std/assert@1";
import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";

const FUNCTION_URL = "http://localhost:54321/functions/v1/evict-cache";
const SUPABASE_URL = "http://localhost:54321";
// Supabase CLI 2.x uses ES256 keys; use a pre-generated ES256 JWT for auth.
const SERVICE_ROLE_JWT = Deno.env.get("SUPABASE_SERVICE_ROLE_JWT")!;
if (!SERVICE_ROLE_JWT) throw new Error("SUPABASE_SERVICE_ROLE_JWT must be set");
const ANON_JWT = Deno.env.get("SUPABASE_ANON_JWT")!;
if (!ANON_JWT) throw new Error("SUPABASE_ANON_JWT must be set");

function admin(): SupabaseClient {
  return createClient(SUPABASE_URL, SERVICE_ROLE_JWT, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

async function post(opts: { token?: string | null } = {}): Promise<Response> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (opts.token !== null) {
    headers["Authorization"] = `Bearer ${opts.token ?? SERVICE_ROLE_JWT}`;
  }
  return await fetch(FUNCTION_URL, { method: "POST", headers, body: "{}" });
}

async function clearTestRows(): Promise<void> {
  await admin().from("book_metadata_cache").delete().like("isbn", "TEST-%");
}

async function seedCacheRow(
  isbn: string,
  lastFetchedAt: string,
  lookupCount: number,
): Promise<void> {
  const { error } = await admin().from("book_metadata_cache").insert({
    isbn,
    title: `Test Book ${isbn}`,
    authors: ["Test Author"],
    cover_url: null,
    lookup_count: lookupCount,
    last_fetched_at: lastFetchedAt,
  });
  if (error) throw new Error(`Failed to seed row ${isbn}: ${error.message}`);
}

function daysAgo(days: number): string {
  return new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString();
}

// --- Method guard ------------------------------------------------------------

Deno.test("GET → 405", async () => {
  const res = await fetch(FUNCTION_URL, {
    method: "GET",
    headers: { Authorization: `Bearer ${SERVICE_ROLE_JWT}` },
  });
  assertEquals(res.status, 405);
  await res.body?.cancel();
});

// --- Auth --------------------------------------------------------------------

// These two tests exercise gateway-level rejection (verify_jwt = true):
// missing or malformed JWTs are rejected by the gateway with 401 before the
// function handler runs.
Deno.test("no Authorization header → 401", async () => {
  const res = await post({ token: null });
  assertEquals(res.status, 401);
  await res.body?.cancel();
});

Deno.test("garbage bearer token → 401", async () => {
  const res = await post({ token: "not-a-real-jwt" });
  assertEquals(res.status, 401);
  await res.body?.cancel();
});

// This test exercises function-level role enforcement: a valid JWT that passed
// gateway validation is rejected because its role claim is not "service_role".
Deno.test("valid JWT with anon role → 403", async () => {
  const res = await post({ token: ANON_JWT });
  assertEquals(res.status, 403);
  await res.body?.cancel();
});

// --- Eviction logic ----------------------------------------------------------

Deno.test("stale low-popularity rows are deleted; count returned", async () => {
  await clearTestRows();

  // Two rows that should be evicted: old enough, lookup_count < 3
  await seedCacheRow("TEST-STALE-1", daysAgo(91), 0);
  await seedCacheRow("TEST-STALE-2", daysAgo(100), 2);

  const res = await post();
  assertEquals(res.status, 200);
  const body = await res.json();
  assertEquals(body.deleted, 2);

  const { data } = await admin()
    .from("book_metadata_cache")
    .select("isbn")
    .in("isbn", ["TEST-STALE-1", "TEST-STALE-2"]);
  assertEquals(data?.length, 0);

  await clearTestRows();
});

Deno.test("stale popular rows (lookup_count >= 3) are preserved", async () => {
  await clearTestRows();

  await seedCacheRow("TEST-POPULAR", daysAgo(95), 3);

  const res = await post();
  assertEquals(res.status, 200);
  await res.body?.cancel();

  const { data } = await admin()
    .from("book_metadata_cache")
    .select("isbn")
    .eq("isbn", "TEST-POPULAR");
  assertEquals(data?.length, 1);

  await clearTestRows();
});

Deno.test("fresh rows (< 90 days old) are preserved regardless of lookup_count", async () => {
  await clearTestRows();

  await seedCacheRow("TEST-FRESH", daysAgo(10), 0);

  const res = await post();
  assertEquals(res.status, 200);
  await res.body?.cancel();

  const { data } = await admin()
    .from("book_metadata_cache")
    .select("isbn")
    .eq("isbn", "TEST-FRESH");
  assertEquals(data?.length, 1);

  await clearTestRows();
});

Deno.test("no stale rows → deleted: 0", async () => {
  await clearTestRows();

  const res = await post();
  assertEquals(res.status, 200);
  const body = await res.json();
  assertEquals(body.deleted, 0);
});
