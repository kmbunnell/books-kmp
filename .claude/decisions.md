# Decision Log

Day-to-day decisions made during development — captures both spec evolution and code choices made along the way.

---

## 2026-05-21 — lookup-book rate limiting: 200/hour + 500/day per user, with 500k cache row cap
**Context:** SHELVD-124 — abuse protection added after recognising the cache table and Google Books quota could be exploited by authenticated users running scripts.
**Decision:** Two-layer protection: (1) per-user rate limits enforced via `check_and_increment_rate_limit` RPC — 200 requests/hour and 500 requests/day, both checked atomically on every request; (2) `upsert_book_metadata_cache` silently skips the insert when the table exceeds 500,000 rows (Google Books result still returned to caller). Rate limit check fails open on DB error to avoid locking out legitimate users. `user_lookup_rate_limits` rows accumulate over time but are never re-read after their window passes — a pg_cron cleanup of rows older than 30 days can be added when needed.
**Rationale:** Limits are sized for real scanning sessions (a user can scan ~30 books in a few minutes; 200/hour gives comfortable headroom). Both hourly and daily windows are enforced because hourly alone allows sustained 4,800/day abuse, and daily alone doesn't catch burst scripting. Rate limits apply to all requests — including cache hits — because the goal is DB protection (preventing authenticated users from filling the cache table via scripts), not purely Google Books quota protection. The counter is capped at `p_limit + 1` so rejected requests don't inflate it beyond a meaningful value.

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
