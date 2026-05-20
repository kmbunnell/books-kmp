# Decision Log

Day-to-day decisions made during development — captures both spec evolution and code choices made along the way.

---

<!-- Format:
## YYYY-MM-DD — Short title
**Context:** What situation or feature prompted this.
**Decision:** What we ended up doing.
**Rationale:** Why this over the original plan or alternatives.
-->

## 2026-05-20 — book_metadata_cache.lookup_count defaults to 1, not 0
**Context:** SHELVD-123 — creating the `book_metadata_cache` table. The ticket specifies `lookup_count INTEGER NOT NULL DEFAULT 1`.
**Decision:** First insert sets `lookup_count = 1`, representing the lookup that caused the cache miss and triggered the insert.
**Rationale:** Matches the ticket spec. The eviction cron (future story) must not interpret `lookup_count = 1` as "never looked up" — it means "looked up exactly once." Eviction logic should treat 1 as a valid baseline, not a sentinel for stale entries.
