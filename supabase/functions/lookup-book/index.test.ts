// Integration tests for the `lookup-book` Edge Function.
//
// Prerequisites (run before `deno test`):
//   1. `supabase start`                       (local stack on :54321/:54322)
//   2. `supabase functions serve lookup-book` (function on :54321/functions/v1)
//
// Run:
//   deno test --allow-net --allow-env supabase/functions/lookup-book/index.test.ts

import { assertEquals } from "jsr:@std/assert@1";
import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { extractIsbn, parseRequest } from "./_lib.ts";

const FUNCTION_URL = "http://localhost:54321/functions/v1/lookup-book";
const SUPABASE_URL = "http://localhost:54321";
// In Supabase CLI 2.x, GoTrue uses EC (ES256) keys and rejects the legacy HS256 JWT-based
// ANON_KEY/SERVICE_ROLE_KEY for auth API calls. Use PUBLISHABLE_KEY for the anon client
// and a pre-generated ES256 service-role JWT for admin operations.
const PUBLISHABLE_KEY = Deno.env.get("SUPABASE_PUBLISHABLE_KEY")!;
if (!PUBLISHABLE_KEY) throw new Error("SUPABASE_PUBLISHABLE_KEY must be set");
const SERVICE_ROLE_JWT = Deno.env.get("SUPABASE_SERVICE_ROLE_JWT")!;
if (!SERVICE_ROLE_JWT) throw new Error("SUPABASE_SERVICE_ROLE_JWT must be set");

function setupSupabaseAdmin(): SupabaseClient {
  return createClient(SUPABASE_URL, SERVICE_ROLE_JWT, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

// Shared test user — created once, deleted in teardown to avoid accumulating
// rows in auth.users across repeated test runs.
let sharedToken: string;
let sharedUserId: string;

Deno.test("setup: create shared test user", async () => {
  const email = `test-lookup-book@example.com`;
  const password = "Password123!";
  const admin = setupSupabaseAdmin();

  // Delete any leftover from a previous interrupted run before creating fresh.
  const { data: existing } = await admin.auth.admin.listUsers();
  const prior = existing?.users?.find((u) => u.email === email);
  if (prior) await admin.auth.admin.deleteUser(prior.id);

  const { data, error } = await admin.auth.admin.createUser({
    email,
    password,
    email_confirm: true,
  });
  if (error || !data.user) throw new Error(`Failed to create test user: ${error?.message}`);
  sharedUserId = data.user.id;

  // Sign in to get a session token.
  const anon = createClient(SUPABASE_URL, PUBLISHABLE_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: session, error: signInErr } = await anon.auth.signInWithPassword({ email, password });
  if (signInErr || !session.session) throw new Error(`Failed to sign in: ${signInErr?.message}`);
  sharedToken = session.session.access_token;
});

async function clearCacheRow(isbn: string): Promise<void> {
  const admin = setupSupabaseAdmin();
  await admin.from("book_metadata_cache").delete().eq("isbn", isbn);
}

async function post(
  body: unknown,
  opts: { token?: string | null } = {},
): Promise<Response> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  // null = omit Authorization header entirely; undefined = use sharedToken; string = use that value
  if (opts.token !== null) {
    headers["Authorization"] = `Bearer ${opts.token ?? sharedToken}`;
  }
  return await fetch(FUNCTION_URL, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

// --- Unit: extractIsbn -------------------------------------------------------

Deno.test("extractIsbn: returns ISBN_13 when present", () => {
  const identifiers = [{ type: "ISBN_13", identifier: "9781234567890" }];
  assertEquals(extractIsbn(identifiers), "9781234567890");
});

Deno.test("extractIsbn: falls back to ISBN_10 when no ISBN_13", () => {
  const identifiers = [{ type: "ISBN_10", identifier: "1234567890" }];
  assertEquals(extractIsbn(identifiers), "1234567890");
});

Deno.test("extractIsbn: returns null when no identifiers", () => {
  assertEquals(extractIsbn(undefined), null);
});

Deno.test("extractIsbn: prefers ISBN_13 over ISBN_10 when both present", () => {
  const identifiers = [
    { type: "ISBN_10", identifier: "1234567890" },
    { type: "ISBN_13", identifier: "9781234567890" },
  ];
  assertEquals(extractIsbn(identifiers), "9781234567890");
});

// --- Unit: parseRequest ------------------------------------------------------

Deno.test("parseRequest: invalid JSON → error", () => {
  const result = parseRequest("not json");
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Invalid JSON body");
});

Deno.test("parseRequest: non-object body → error", () => {
  const result = parseRequest('"just a string"');
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Body must be a JSON object");
});

Deno.test("parseRequest: missing type field → error", () => {
  const result = parseRequest(JSON.stringify({}));
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Missing or invalid 'type' (expected 'isbn' or 'title')");
});

Deno.test("parseRequest: isbn type without isbn field → error", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn" }));
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Missing or invalid 'isbn'");
});

Deno.test("parseRequest: isbn type with empty isbn → error", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn", isbn: "   " }));
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Missing or invalid 'isbn'");
});

Deno.test("parseRequest: isbn type with invalid format → error", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn", isbn: "12345" }));
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Invalid ISBN format — expected 10 or 13 digits");
});

Deno.test("parseRequest: valid ISBN-13 → success", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn", isbn: "9780743273565" }));
  assertEquals(result.ok, true);
  if (result.ok) {
    assertEquals(result.value.type, "isbn");
    if (result.value.type === "isbn") assertEquals(result.value.isbn, "9780743273565");
  }
});

Deno.test("parseRequest: valid ISBN-10 ending in X → success", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn", isbn: "047301450X" }));
  assertEquals(result.ok, true);
  if (result.ok) {
    assertEquals(result.value.type, "isbn");
    if (result.value.type === "isbn") assertEquals(result.value.isbn, "047301450X");
  }
});

Deno.test("parseRequest: isbn with surrounding whitespace → trimmed", () => {
  const result = parseRequest(JSON.stringify({ type: "isbn", isbn: "  9780743273565  " }));
  assertEquals(result.ok, true);
  if (result.ok && result.value.type === "isbn") assertEquals(result.value.isbn, "9780743273565");
});

Deno.test("parseRequest: title type without query → error", () => {
  const result = parseRequest(JSON.stringify({ type: "title" }));
  assertEquals(result.ok, false);
  if (!result.ok) assertEquals(result.error, "Missing or invalid 'query'");
});

Deno.test("parseRequest: valid title query → success", () => {
  const result = parseRequest(JSON.stringify({ type: "title", query: "the great gatsby" }));
  assertEquals(result.ok, true);
  if (result.ok) {
    assertEquals(result.value.type, "title");
    if (result.value.type === "title") assertEquals(result.value.query, "the great gatsby");
  }
});

Deno.test("parseRequest: title query with surrounding whitespace → trimmed", () => {
  const result = parseRequest(JSON.stringify({ type: "title", query: "  dune  " }));
  assertEquals(result.ok, true);
  if (result.ok && result.value.type === "title") assertEquals(result.value.query, "dune");
});

// --- Step 1: request parsing -------------------------------------------------

Deno.test("missing type field → 400", async () => {
  const res = await post({});
  assertEquals(res.status, 400);
  await res.body?.cancel();
});

Deno.test("type: isbn without isbn → 400", async () => {
  const res = await post({ type: "isbn" });
  assertEquals(res.status, 400);
  await res.body?.cancel();
});

Deno.test("type: title without query → 400", async () => {
  const res = await post({ type: "title" });
  assertEquals(res.status, 400);
  await res.body?.cancel();
});

// --- Step 2: JWT auth --------------------------------------------------------

Deno.test("no Authorization header → 401", async () => {
  const res = await post({ type: "isbn", isbn: "9780000000000" }, { token: null });
  assertEquals(res.status, 401);
  await res.body?.cancel();
});

Deno.test("garbage bearer token → 401", async () => {
  const res = await post(
    { type: "isbn", isbn: "9780000000000" },
    { token: "not-a-real-jwt" },
  );
  assertEquals(res.status, 401);
  await res.body?.cancel();
});

// --- Step 3: ISBN cache hit --------------------------------------------------

Deno.test("ISBN present in cache → 200 cached data; lookup_count incremented", async () => {
  const admin = setupSupabaseAdmin();
  const isbn = "9999999999991";
  await clearCacheRow(isbn);

  const { error: insertErr } = await admin.from("book_metadata_cache").insert({
    isbn,
    title: "Cached Title",
    authors: ["Cached Author"],
    cover_url: "https://example.com/c.jpg",
    lookup_count: 1,
  });
  assertEquals(insertErr, null);

  const res = await post({ type: "isbn", isbn });
  assertEquals(res.status, 200);
  const body = await res.json();
  assertEquals(body.isbn, isbn);
  assertEquals(body.title, "Cached Title");
  assertEquals(body.authors, ["Cached Author"]);

  const { data: row } = await admin
    .from("book_metadata_cache")
    .select("lookup_count")
    .eq("isbn", isbn)
    .single();
  assertEquals(row?.lookup_count, 2);

  await clearCacheRow(isbn);
});

// --- Rate limiting -----------------------------------------------------------

async function seedRateLimitRow(
  userId: string,
  window: "hour" | "day",
  count: number,
): Promise<void> {
  const admin = setupSupabaseAdmin();
  const now = new Date();
  const windowStart = window === "hour"
    ? new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(), now.getUTCHours(), 0, 0, 0)).toISOString()
    : new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(), 0, 0, 0, 0)).toISOString();
  await admin.from("user_lookup_rate_limits").upsert({
    user_id: userId,
    window_start: windowStart,
    request_count: count,
  });
}

async function clearRateLimitRows(userId: string): Promise<void> {
  const admin = setupSupabaseAdmin();
  await admin.from("user_lookup_rate_limits").delete().eq("user_id", userId);
}

Deno.test("hourly limit reached (200) → 429", async () => {
  await clearRateLimitRows(sharedUserId);
  await seedRateLimitRow(sharedUserId, "hour", 200);
  const res = await post({ type: "isbn", isbn: "9780000000001" });
  assertEquals(res.status, 429);
  await res.body?.cancel();
  await clearRateLimitRows(sharedUserId);
});

Deno.test("daily limit reached (500) → 429", async () => {
  await clearRateLimitRows(sharedUserId);
  await seedRateLimitRow(sharedUserId, "day", 500);
  const res = await post({ type: "isbn", isbn: "9780000000001" });
  assertEquals(res.status, 429);
  await res.body?.cancel();
  await clearRateLimitRows(sharedUserId);
});

Deno.test("hourly at 199 and daily at 499 → not rate limited", async () => {
  await clearRateLimitRows(sharedUserId);
  await seedRateLimitRow(sharedUserId, "hour", 199);
  await seedRateLimitRow(sharedUserId, "day", 499);
  // Any valid request that doesn't need Google Books — use the cache-hit ISBN
  const admin = setupSupabaseAdmin();
  const isbn = "9999999999992";
  await admin.from("book_metadata_cache").insert({
    isbn,
    title: "Rate Limit Test Book",
    authors: ["Test Author"],
    cover_url: null,
    lookup_count: 1,
  });
  const res = await post({ type: "isbn", isbn });
  assertEquals(res.status, 200);
  await res.body?.cancel();
  await admin.from("book_metadata_cache").delete().eq("isbn", isbn);
  await clearRateLimitRows(sharedUserId);
});

// --- Teardown ----------------------------------------------------------------

Deno.test("teardown: delete shared test user", async () => {
  const admin = setupSupabaseAdmin();
  const { error } = await admin.auth.admin.deleteUser(sharedUserId);
  if (error) console.error("Failed to delete test user:", error.message);
});
