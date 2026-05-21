# Decision Log

Day-to-day decisions made during development — captures both spec evolution and code choices made along the way.

---

<!-- Format:
## YYYY-MM-DD — Short title
**Context:** What situation or feature prompted this.
**Decision:** What we ended up doing.
**Rationale:** Why this over the original plan or alternatives.
-->

## 2026-05-21 — lookup-book upsert/increment via SECURITY DEFINER RPCs
**Context:** SHELVD-124 — the Edge Function needs atomic upsert with `lookup_count = lookup_count + 1` on conflict, plus a fire-and-forget increment on cache hit. PostgREST's upsert path can't express `lookup_count = lookup_count + 1` directly.
**Decision:** Added `supabase/migrations/20260521132104_book_metadata_cache_rpcs.sql` with two `SECURITY DEFINER` SQL functions: `increment_book_metadata_cache_lookup(p_isbn)` and `upsert_book_metadata_cache(...)`. The Edge Function calls these via `admin.rpc(...)`.
**Rationale:** Keeps the increment atomic (no read-modify-write race) and the upsert correct under concurrent inserts. Migration is small and tightly scoped to this function's needs; alternative supabase-js `.upsert()` would reset `lookup_count` to 1 on conflict, violating ticket requirement #6.

## 2026-05-21 — lookup-book test runs as integration tests against running stack
**Context:** SHELVD-124 — Deno is not installed standalone in dev environments; Supabase CLI bundles its own Deno for `functions serve`. The plan calls tests via `fetch("http://localhost:54321/functions/v1/lookup-book", ...)`.
**Decision:** `index.test.ts` is a real integration test: it requires `supabase start` + `supabase functions serve lookup-book` running locally, and uses the well-known local anon/service-role keys (overridable via env). Tests that require hitting the real Google Books API were deleted — no external API calls in automated tests.
**Rationale:** Pure unit tests would mock `supabase-js` heavily and not verify the SQL RPC contract that the function depends on.

## 2026-05-20 — book_metadata_cache.lookup_count defaults to 1, not 0
**Context:** SHELVD-123 — creating the `book_metadata_cache` table. The ticket specifies `lookup_count INTEGER NOT NULL DEFAULT 1`.
**Decision:** First insert sets `lookup_count = 1`, representing the lookup that caused the cache miss and triggered the insert.
**Rationale:** Matches the ticket spec. The eviction cron (future story) must not interpret `lookup_count = 1` as "never looked up" — it means "looked up exactly once." Eviction logic should treat 1 as a valid baseline, not a sentinel for stale entries.
