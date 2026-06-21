# Decision Log

Non-obvious decisions — rejected alternatives, surprising constraints, gotchas for future work. One or two sentences per entry; skip anything obvious from the code or ticket.

<!-- Format: ## YYYY-MM-DD — Title (TICKET) — one sentence on what and why. -->

---

## 2026-06-21 — Groq chosen over Gemini for AI recommendations; title lookup instead of ISBN (SHELVD-171)
Groq's free tier needs no billing info, making it zero-friction to run. Result quality is lower than a paid model but acceptable for the feature. Recommended books are looked up by title (not ISBN) because Groq hallucinates ISBNs — the numbers it returns are plausible-looking but invalid, so ISBN lookup silently fails or surfaces wrong books. Title lookup through the existing Google Books path is the only reliable resolution strategy.

## 2026-06-19 — ConfirmationDialog used for advisory pre-flight gate, not destructive action (SHELVD-170)
`ConfirmationDialog` is intentionally used to warn the user when fewer than 6 books match their filters before requesting recommendations. The alternative (inline advisory on the recommendations screen) forces a round-trip: navigate → see poor results → navigate back → re-filter → retry. Pre-flight is the right UX; the component is structurally correct even though the tier docs describe it as "destructive only."

## 2026-06-17 — BookRecommendation is in-memory only, no genres field (SHELVD-165)
Gemini recommendation responses are never persisted; adding a recommended book to the library goes through the existing `AddBookUseCase` ISBN-lookup flow. The model carries `reason` and `description` but no `genres` field — other epic tickets assuming genre/description DB columns will need updating.

## 2026-06-12 — EntitlementRepository defaults to false when init fetch fails (SHELVD-159)
On network error or a missing profile row at startup, `isPremium` stays `false` (deny premium) — a conservative paywall stance. Known limitation: an existing premium user sees the free tier until `setPremiumStatus` succeeds or the app restarts with a successful fetch.

## 2026-05-28 — `supportingText` kept for auth form validation; not replaced with `InlineErrorText` (SHELVD-137)
`OutlinedTextField`'s `supportingText` slot reserves space and prevents layout shifts; standalone `InlineErrorText` below the field would cause content to jump. `InlineErrorText` is for error messages outside Material's text field component.

## 2026-05-28 — Splash screen has no error UI by design (SHELVD-137)
Session restore failure sets `isLoading = false, isAuthenticated = false`, which triggers `onNotAuthenticated()` navigation to Sign In immediately — a snackbar on a transient screen would never be visible.

## 2026-05-23 — Password policy tightened to 8 chars + letters_digits (SHELVD-127)
Improves security posture; existing accounts are not force-rotated — the stricter policy applies only to new sign-ups and password changes going forward.

## 2026-05-22 — GoogleBooksApiClient takes a `() -> String?` session lambda, not a SessionProvider interface (SHELVD-125)
Passing the access-token accessor as a lambda keeps the client testable with a plain `{ "jwt" }`/`{ null }` stub and avoids adding a one-method `SessionProvider` interface solely for that purpose.

## 2026-05-21 — Rate limits count all requests, not just Google Books calls (SHELVD-124)
Goal is DB protection against authenticated users flooding the cache table, not just quota protection — so cache hits count. Counter capped at `p_limit + 1` so rejected requests don't inflate it past a meaningful value.

## 2026-05-21 — Atomic cache upsert via SECURITY DEFINER RPC instead of supabase-js upsert (SHELVD-124)
`supabase-js .upsert()` resets `lookup_count` to 1 on conflict; a SQL function is the only way to express `lookup_count = lookup_count + 1` atomically.

## 2026-05-21 — lookup-book tests are integration tests, not unit tests (SHELVD-124)
Mocking `supabase-js` wouldn't verify the SQL RPC contract; real tests require `supabase start` + `supabase functions serve lookup-book`.

## 2026-05-20 — book_metadata_cache.lookup_count defaults to 1, not 0 (SHELVD-123)
Represents the miss that triggered the insert. Future eviction logic must not treat 1 as "never looked up."
