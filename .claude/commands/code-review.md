---
description: Code review as a senior Android engineer with KMP/CMP expertise
allowed-tools: Bash
---

Review the current diff as a pull request reviewer. First, determine which platforms are touched:

```bash
git diff HEAD --name-only; git diff --staged --name-only
```

Based on the changed files, adopt the appropriate senior engineer persona(s):

- Files under `iosApp/` or `iosMain/` → **senior iOS engineer** (Swift, Objective-C, SwiftUI, AVFoundation, UIKit, Xcode project conventions, App Store requirements)
- Files under `androidApp/`, `androidMain/`, `androidUnitTest/`, or `androidInstrumentedTest/` → **senior Android engineer** (Jetpack, Android SDK, Gradle, Play Store requirements)
- Files under `commonMain/` or `commonTest/` → **senior KMP engineer** (shared Kotlin, expect/actual, Kotlin stdlib, coroutines, platform-agnostic design)
- Files touching multiple platforms → review each platform section from the relevant persona, clearly labeling which hat you're wearing

All reviews share these cross-cutting concerns regardless of platform: Clean Architecture, MVI, Koin, TDD, and KMP correctness.

## Optional: Ticket Context

`$ARGUMENTS` may contain a Jira ticket key (e.g. `SHELVD-42`). If a key was provided, fetch the ticket now:

```bash
acli jira workitem view $ARGUMENTS --fields "summary,description,acceptance criteria"
```

If no key was provided, skip this section entirely.

## Review the diff
 
```bash
git diff HEAD
git diff --staged
```

## Evaluate against

**Architecture**
- [ ] Layer boundaries respected: no DTOs in presentation, no framework annotations in domain models
- [ ] Repository interfaces in domain, implementations in data
- [ ] Use cases: single `operator fun invoke`, return `Result<T, XxxError>` — not raw strings, not custom `XxxResult` sealed interfaces
- [ ] Error types are feature-specific sealed interfaces in their own files

**MVI**
- [ ] ViewModel exposes `StateFlow<XxxUiState>`, accepts `sealed interface XxxIntent` via `onIntent()`, emits via `SharedFlow/Channel<XxxEffect>`
- [ ] No business logic in ViewModels — delegate to use cases only
- [ ] String resolution only in Compose via `stringResource()` — never in ViewModel or use cases

**Error handling**
- [ ] No `catch (e: Exception)` in `suspend` functions without rethrowing `CancellationException` first
- [ ] No raw `Channel` bridging Android SDK callbacks — use `suspendCancellableCoroutine`

**Platform / CMP**
- [ ] No platform imports (`android.*`, `UIKit`, etc.) in `commonMain`
- [ ] `expect`/`actual` only for platform UI, not business logic
- [ ] Camera/resource composables use `DisposableEffect` for cleanup
- [ ] One-shot callbacks guarded by `AtomicBoolean.compareAndSet`

**DI**
- [ ] All dependencies wired through Koin modules — no manual construction outside DI

**Strings**
- [ ] No user-visible strings hardcoded in Kotlin — all in `commonMain/composeResources/values/strings.xml`

**Tests**
- [ ] Every public use case `invoke` and repository function with logic has a test
- [ ] Hand-rolled fakes — no mocking libraries
- [ ] `Turbine` used for Flow/StateFlow assertions

**Kotlin quality**
- [ ] No `!!`, sealed classes for finite states, explicit return types on public APIs
- [ ] No duplicated logic or repeated Compose components
- [ ] No hardcoded keys or URLs

## Report format

**Summary** — one paragraph on overall quality and merge-readiness.

**Acceptance Criteria** *(only if a ticket key was provided)* — list each AC item and whether the diff satisfies it (✅ met / ❌ not met / ⚠️ partial). Flag unmet or partial items as Critical below.

**Critical** — must fix before merge (file:line, problem, fix).

**Suggestions** — non-blocking improvements.

**Looks Good** — reinforce good patterns worth calling out.

If there's nothing to review, say so.
