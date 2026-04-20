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

- **Architecture**: layer separation respected, no DTOs in presentation, no framework annotations in domain models
- **TDD**: tests written alongside production code, every public use case/repo function has a test, descriptive test names
- **KMP correctness**: no platform imports in `commonMain`, `expect`/`actual` only when necessary
- **Kotlin quality**: no `!!`, sealed classes for finite states, explicit return types on public APIs
- **DRY**: no duplicated logic or repeated Compose components
- **Secrets**: no hardcoded keys or URLs

## Report format

**Summary** — one paragraph on overall quality and merge-readiness.

**Acceptance Criteria** *(only if a ticket key was provided)* — list each AC item and whether the diff satisfies it (✅ met / ❌ not met / ⚠️ partial). Flag unmet or partial items as Critical below.

**Critical** — must fix before merge (file:line, problem, fix).

**Suggestions** — non-blocking improvements.

**Looks Good** — reinforce good patterns worth calling out.

If there's nothing to review, say so.
