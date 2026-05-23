// lookup-book Edge Function
//
// Single server-side entry point for book metadata lookups.
//
// Request body:
//   { "type": "isbn",  "isbn":  "9780000000000" }
//   { "type": "title", "query": "the great gatsby" }
//
// Behavior:
// - ISBN: cache-first against `book_metadata_cache`, then Google Books on miss.
//   On cache hit:  await `lookup_count` += 1, then return cached data.
//   On cache miss: return Google Books data immediately; upsert into cache via
//                  EdgeRuntime.waitUntil so it completes after the response.
// - Title: Google Books passthrough; returns up to 10 results. No cache write.
//
// All requests require a valid Supabase JWT in the `Authorization: Bearer ...` header.

import { createClient, type SupabaseClient } from "jsr:@supabase/supabase-js@2";

// Deno's EdgeRuntime global is provided by the Supabase Edge Runtime; declare it
// here so TypeScript doesn't complain when type-checking locally.
declare const EdgeRuntime:
  | { waitUntil: (p: Promise<unknown>) => void }
  | undefined;

if (!Deno.env.get("GOOGLE_BOOKS_API_KEY")) {
  console.warn("GOOGLE_BOOKS_API_KEY not set — using unauthenticated Google Books quota (100 req/day per IP)");
}

interface BookMetadata {
  isbn: string | null;
  title: string;
  authors: string[];
  cover_url: string | null;
}

interface ParsedIsbnRequest {
  type: "isbn";
  isbn: string;
}

interface ParsedTitleRequest {
  type: "title";
  query: string;
}

type ParsedRequest = ParsedIsbnRequest | ParsedTitleRequest;

interface ParseSuccess {
  ok: true;
  value: ParsedRequest;
}

interface ParseFailure {
  ok: false;
  error: string;
}

type ParseResult = ParseSuccess | ParseFailure;

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function parseRequest(bodyText: string): ParseResult {
  let body: unknown;
  try {
    body = JSON.parse(bodyText);
  } catch (_e) {
    return { ok: false, error: "Invalid JSON body" };
  }

  if (typeof body !== "object" || body === null) {
    return { ok: false, error: "Body must be a JSON object" };
  }

  const obj = body as Record<string, unknown>;
  const type = obj["type"];

  if (type === "isbn") {
    const isbn = obj["isbn"];
    if (typeof isbn !== "string" || isbn.trim() === "") {
      return { ok: false, error: "Missing or invalid 'isbn'" };
    }
    const trimmed = isbn.trim();
    if (!/^\d{9}[\dX]$|^\d{13}$/.test(trimmed)) {
      return { ok: false, error: "Invalid ISBN format — expected 10 or 13 digits" };
    }
    return { ok: true, value: { type: "isbn", isbn: trimmed } };
  }

  if (type === "title") {
    const query = obj["query"];
    if (typeof query !== "string" || query.trim() === "") {
      return { ok: false, error: "Missing or invalid 'query'" };
    }
    return { ok: true, value: { type: "title", query: query.trim() } };
  }

  return { ok: false, error: "Missing or invalid 'type' (expected 'isbn' or 'title')" };
}

type AuthResult =
  | { ok: true; client: SupabaseClient; userId: string }
  | { ok: false; status: 401 | 500 };

async function requireAuth(req: Request): Promise<AuthResult> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
    return { ok: false, status: 401 };
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!supabaseUrl || !supabaseAnonKey) {
    console.error("requireAuth: SUPABASE_URL or SUPABASE_ANON_KEY not set");
    return { ok: false, status: 500 };
  }

  const client = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await client.auth.getUser();
  if (error || !data?.user) {
    return { ok: false, status: 401 };
  }
  return { ok: true, client, userId: data.user.id };
}

async function checkRateLimit(userId: string): Promise<boolean> {
  const admin = serviceRoleClient();
  const [hourly, daily] = await Promise.all([
    admin.rpc("check_and_increment_rate_limit", { p_user_id: userId, p_limit: 200, p_window: "hour" }),
    admin.rpc("check_and_increment_rate_limit", { p_user_id: userId, p_limit: 500, p_window: "day" }),
  ]);
  if (hourly.error) console.error("Rate limit hourly check failed:", hourly.error.message);
  if (daily.error) console.error("Rate limit daily check failed:", daily.error.message);
  // Fail open on RPC error — don't block legitimate users on a DB hiccup
  if (hourly.error || daily.error) return true;
  return hourly.data !== false && daily.data !== false;
}

function serviceRoleClient(): SupabaseClient {
  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  return createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

interface GoogleBooksVolume {
  volumeInfo?: {
    title?: string;
    authors?: string[];
    imageLinks?: { thumbnail?: string };
  };
}

interface GoogleBooksResponse {
  totalItems?: number;
  items?: GoogleBooksVolume[];
}

function mapVolume(volume: GoogleBooksVolume, isbn: string | null): BookMetadata {
  const v = volume.volumeInfo ?? {};
  return {
    isbn,
    title: v.title ?? "",
    authors: Array.isArray(v.authors) ? v.authors : [],
    cover_url: v.imageLinks?.thumbnail?.replace(/^http:\/\//, "https://") ?? null,
  };
}

async function fetchGoogleBooks(url: string): Promise<GoogleBooksResponse | null> {
  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.error(`Google Books API error: ${res.status} ${res.statusText}`);
      return null;
    }
    return (await res.json()) as GoogleBooksResponse;
  } catch (e) {
    console.error("Google Books fetch failed:", e);
    return null;
  }
}

function googleBooksUrl(params: { q: string; maxResults?: number }): string {
  const key = Deno.env.get("GOOGLE_BOOKS_API_KEY") ?? "";
  const qs = new URLSearchParams();
  qs.set("q", params.q);
  if (params.maxResults !== undefined) {
    qs.set("maxResults", String(params.maxResults));
  }
  if (key) qs.set("key", key);
  return `https://www.googleapis.com/books/v1/volumes?${qs.toString()}`;
}

async function handleIsbn(isbn: string): Promise<Response> {
  const admin = serviceRoleClient();

  // Cache-first lookup.
  const { data: cached, error: cacheErr } = await admin
    .from("book_metadata_cache")
    .select("isbn,title,authors,cover_url")
    .eq("isbn", isbn)
    .maybeSingle();

  if (cacheErr) {
    return jsonResponse({ error: "Cache lookup failed" }, 500);
  }

  if (cached) {
    // Await the increment before returning — it's a single indexed UPDATE and
    // not on the critical path for response latency.
    const { error: incErr } = await admin
      .rpc("increment_book_metadata_cache_lookup", { p_isbn: isbn });
    if (incErr) console.error("lookup_count increment failed:", incErr.message);
    return jsonResponse(cached, 200);
  }

  // Cache miss — call Google Books.
  const gb = await fetchGoogleBooks(googleBooksUrl({ q: `isbn:${isbn}` }));
  if (gb === null) {
    return jsonResponse({ error: "Upstream book provider failed" }, 502);
  }
  if (!gb.totalItems || gb.totalItems === 0 || !gb.items || gb.items.length === 0) {
    return jsonResponse({ error: "Book not found" }, 404);
  }

  const mapped = mapVolume(gb.items[0], isbn);

  // Upsert into cache after returning the response. On conflict (concurrent
  // insert between check and write), lookup_count increments rather than resets.
  const upsertPromise = admin
    .rpc("upsert_book_metadata_cache", {
      p_isbn: isbn,
      p_title: mapped.title,
      p_authors: mapped.authors,
      p_cover_url: mapped.cover_url,
    })
    .then(({ error }) => {
      if (error) console.error("Cache upsert failed:", error.message);
    });

  if (typeof EdgeRuntime !== "undefined" && EdgeRuntime?.waitUntil) {
    EdgeRuntime.waitUntil(upsertPromise);
  } else {
    await upsertPromise;
  }

  return jsonResponse(mapped, 200);
}

async function handleTitle(query: string): Promise<Response> {
  const gb = await fetchGoogleBooks(
    googleBooksUrl({ q: `intitle:${query}`, maxResults: 10 }),
  );
  if (gb === null) {
    return jsonResponse({ error: "Upstream book provider failed" }, 502);
  }
  const items = Array.isArray(gb.items) ? gb.items : [];
  const results = items.map((v) => mapVolume(v, null));
  return jsonResponse(results, 200);
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const bodyText = await req.text();
  if (new TextEncoder().encode(bodyText).byteLength > 4096) {
    return jsonResponse({ error: "Payload too large" }, 413);
  }

  const parsed = parseRequest(bodyText);
  if (!parsed.ok) {
    return jsonResponse({ error: parsed.error }, 400);
  }

  const auth = await requireAuth(req);
  if (!auth.ok) {
    const msg = auth.status === 500 ? "Internal server error" : "Unauthorized";
    return jsonResponse({ error: msg }, auth.status);
  }

  const allowed = await checkRateLimit(auth.userId);
  if (!allowed) {
    return jsonResponse({ error: "Rate limit exceeded" }, 429);
  }

  if (parsed.value.type === "isbn") {
    return await handleIsbn(parsed.value.isbn);
  }
  return await handleTitle(parsed.value.query);
});
