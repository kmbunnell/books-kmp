# Decision Log

Non-obvious decisions — rejected alternatives, surprising constraints, gotchas for future work. One or two sentences per entry; skip anything obvious from the code or ticket.

<!-- Format: ## YYYY-MM-DD — Title (TICKET) — one sentence on what and why. -->

---

## 2026-05-21 — Rate limits count all requests, not just Google Books calls (SHELVD-124)
Goal is DB protection against authenticated users flooding the cache table, not just quota protection — so cache hits count. Counter capped at `p_limit + 1` so rejected requests don't inflate it past a meaningful value.

## 2026-05-21 — Atomic cache upsert via SECURITY DEFINER RPC instead of supabase-js upsert (SHELVD-124)
`supabase-js .upsert()` resets `lookup_count` to 1 on conflict; a SQL function is the only way to express `lookup_count = lookup_count + 1` atomically.

## 2026-05-21 — lookup-book tests are integration tests, not unit tests (SHELVD-124)
Mocking `supabase-js` wouldn't verify the SQL RPC contract; real tests require `supabase start` + `supabase functions serve lookup-book`.

## 2026-05-20 — book_metadata_cache.lookup_count defaults to 1, not 0 (SHELVD-123)
Represents the miss that triggered the insert. Future eviction logic must not treat 1 as "never looked up."
