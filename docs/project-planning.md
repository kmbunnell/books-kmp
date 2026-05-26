# Books-KMP — Project Brief (Planning Context)

This file is the source of truth for planning sessions, Jira ticket generation, and project decisions. It is not a coding guide — see `AGENTS.md` in the app repo for architecture and coding conventions.

---

## 1. Project Context

**Books-KMP** is a cross-platform mobile book library app built with Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP), backed by Supabase. Users scan ISBNs or search by title to pull metadata, then organize their library with a flexible tagging system.

**Platforms:** Android (minSdk 24, targetSdk 36) + iOS (iosArm64, iosSimulatorArm64)
**Jira project key:** `SHELVD`

### Current App State

The following features are **already implemented** — do not write tickets for these:

- Supabase email/password auth + session management
- Add books: ISBN barcode scan, ISBN lookup, title search, manual entry
- Duplicate detection (ISBN + normalized title)
- Book detail with tag assignment (toggle, optimistic cache update)
- Tag management: create custom tags, see book counts, delete
- Default tags seeded on signup (Read, Hardback, Paperback, ARC, Special Edition, Signed) — immutable via DB trigger
- Library: search by title/author, filter by tag (multi-select), sort by title or author
- Delete book
- Google Books API client (`GoogleBooksApiClient`) — replaces Open Library
- `lookup-book` Supabase Edge Function — cache-first ISBN/title lookup; calls Google Books on cache miss
- `book_metadata_cache` table — ISBN-keyed metadata cache with per-user rate limiting (200/hr, 500/day) and 500k-row capacity guard
- `evict-cache` Supabase Edge Function + weekly pg_cron job — evicts rows older than 90 days with `lookup_count < 3`

### Module Layout

| Module | Role |
|--------|------|
| `composeApp` | All UI — screens, ViewModels, navigation, expect/actual composables |
| `shared` | Domain models, repository interfaces, use cases, data implementations, DI modules |
| `shared-testing` | Shared test fakes and utilities |

### Key Library Versions (as of project brief creation)

Kotlin 2.3.0 · Compose Multiplatform 1.10.0 · Supabase-kt 3.1.4 · Ktor 3.1.3 · Koin 4.1.0 · Coil 3.4.0 · KotlinX Serialization 1.8.1 · KotlinX Coroutines 1.10.2 · CameraX 1.4.1 · ML Kit Barcode 17.3.0 · Androidx Navigation 2.9.2 · AGP 8.11.2

---

## 2. Epic Sequence

Ordered by execution. Dependencies noted — do not resequence without checking them.

| # | Epic | Category | Depends On |
|---|------|----------|------------|
| 1 | ~~Data layer migration (Open Library → Google Books + cache)~~ ✓ Done | Infrastructure | — |
| 2 | Social auth (Apple + Google Sign-In) | Feature | — |
| 3 | Staging environment & dev infrastructure | Infrastructure | — |
| 4 | Error handling audit & consistency | UX | Epic 2 |
| 5 | Add book flow redesign | UX | Epic 4 |
| 6 | UI polish & dark mode | UX | — |
| 7 | Monetization infrastructure (RevenueCat, IAP, paywall) | Monetization | Epic 3 |
| 8 | Onboarding carousel | UX | Epic 7 |
| 9 | Observability (Firebase Crashlytics) | Infrastructure | — |
| 10 | App store readiness | Store | All prior epics |
| 11 | Bookshelf AI — bulk photo import | Post-launch / Premium | Epic 1 |

### Key dependency rationale

- Epics 1 and 2 can run in parallel — no shared dependency
- Epic 3 must complete before Epic 7 (RevenueCat sandbox requires staging) and before Epic 10
- Epic 4 must complete before Epic 5 — new screens must conform to established error patterns from day one
- Epic 7 must complete before Epic 8 — onboarding paywall slide must reflect real feature gates
- Epic 6 has no hard dependencies but should complete before Epic 10 (store screenshots)

---

## 3. Jira Scripting Conventions

### acli Flags

- Issue type: `--type "Epic"` for epics, `--type "Task"` for stories/tasks
- Parent link: `--parent "$EPIC_KEY"` on all stories and tasks; omit on epics
- Labels: comma-separated, no spaces — e.g. `--label "infrastructure,auth,kmp"`
- Project: `--project "SHELVD"` on every call

### Script Format

```bash
DESCRIPTION_S1=$(cat <<'EOF'
As a mobile developer, I need to ...

h3. Context
...

h3. Steps
# Step one
# Step two

h3. Acceptance Criteria
* Criterion one
* Criterion two
EOF
)

acli jira workitem create \
  --project "SHELVD" \
  --type "Task" \
  --summary "Short summary here" \
  --description "$DESCRIPTION_S1" \
  --label "label1,label2" \
  --parent "$EPIC_KEY"
```

### Heredoc Single-Quote Bug

**Problem:** Using `<<'EOF'` prevents variable expansion but causes a bash syntax error if a single quote `'` appears anywhere inside the heredoc body. The shell interprets the first `'` as closing the delimiter.

**Common triggers in KMP descriptions:**
- Possessives and contractions: `user's`, `don't`, `it's`
- Quoted terms: `'staging'`

**Fix — preferred:** Rephrase to eliminate the single quote entirely.
```bash
# Instead of: respect the user's existing library
# Write:       respect the existing user library
```

**Fix — fallback:** Switch to unquoted `<<EOF` and escape any `$` variables with a backslash.
```bash
DESCRIPTION=$(cat <<EOF
This uses \$SOME_VAR and the user data safely.
EOF
)
```

**Standing rule:** Before generating any acli script, scan all description text for single quotes and rephrase. After generating, always validate:
```bash
bash -n your_script.sh && echo "Syntax OK"
```

### Ticket Writing Standards

- User story line: "As a mobile developer, I need to..." (not end-user stories — these are engineering tasks)
- Blocker relationships: call out explicitly in the story text when a task must complete before others in the same epic
- Description sections: Context · Steps · Acceptance Criteria (all three, every ticket)
- Steps use numbered list (`#`); ACs use bullet list (`*`) — Jira wiki markup

---

## 4. Ticket-Writing Cheat Sheet

These are the architectural rules from `AGENTS.md` that most directly affect Steps and Acceptance Criteria. For full detail, see `AGENTS.md`.

- **Result<T, E> everywhere:** Every `suspend` repo/use-case/gateway function returns `Result<T, XxxError>` — including those that would naturally return `Boolean`, `Unit`, or `T?`. No raw exceptions from callers. No `try/catch` wrapping a Result-returning call — pattern-match instead.
- **Error types:** Feature-specific sealed interfaces (e.g. `AddBookError`, `SignInError`). Each defined in its own file in the domain layer. ViewModel-level errors co-locate with the ViewModel file.
- **Use cases only for real orchestration:** If a ViewModel is doing a simple read/write against one repository, it calls the repository directly — no use case wrapper needed.
- **No localized strings in ViewModels:** Errors are typed `XxxError` values. `stringResource()` resolution happens exclusively in Compose screens.
- **Hand-rolled fakes:** No mocking libraries (KMP compatibility). Fakes live in `shared-testing`.
- **DTOs separate from domain models:** No `@Serializable` or column names leaking into domain models. Map explicitly.
- **Koin for all DI:** Never manually construct dependencies outside a Koin module.
- **TDD:** Failing test before implementation. Skip tests only for pure data classes and interfaces with no logic.

---

## 5. Planning Decisions

Running log of notable decisions made during planning sessions. Add an entry whenever a spec evolves, an approach is chosen over an alternative, or a trade-off is accepted.

### Format

```
### YYYY-MM-DD — Short title
**Decision:** What was decided.
**Rationale:** Why.
**Alternatives considered:** What was ruled out and why (if relevant).
```

---

### 2026-05-20 — project_brief.md established as planning source of truth
**Decision:** Created `project_brief.md` as the planning-context counterpart to `AGENTS.md`. `jira_scripting_notes.md` retired; its content folded into Section 3 of this file.
**Rationale:** Single file for planning sessions reduces context overhead. Scripting conventions and project state belong together. `AGENTS.md` stays as the coding contract for the app repo and is not duplicated here beyond a short cheat sheet.
**Alternatives considered:** Keeping `jira_scripting_notes.md` as a separate file — rejected to avoid two files covering overlapping Jira conventions.