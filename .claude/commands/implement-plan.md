---
description: Implement the approved plan from plan.md following TDD and CLAUDE.md guidelines
allowed-tools: Bash, Read, Write, Edit, Glob, Grep, Agent, AskUserQuestion
---

## Purpose

Read `.claude/plan.md` and implement every step, following TDD and the architecture rules in `CLAUDE.md`. Iterate until all new tests pass, verify no regressions, fix formatting, then report results.

## Token-efficiency rules (follow throughout)

- **Do not re-read files you have already read** in this session unless they were modified since.
- **Do not re-read `CLAUDE.md` or `docs/*.md`** — you already have those rules in your system context. Only re-read if the plan references a specific doc section you are unsure about.
- **Batch related edits** — if multiple changes go into the same file, make them in one Edit call, not several.
- **Minimize Gradle invocations** — each `./gradlew` call is expensive. Combine tasks where possible (e.g., `./gradlew :composeApp:testDebugUnitTest ktlintCheck`). Do not run the full build after every single file change.
- **Fail fast** — if a Gradle command fails, read only the last 100 lines of output to diagnose. Do not dump the entire log.
- **No exploratory searches** — the plan already lists files to modify/create. Go directly to those paths. Use Glob/Grep only if a path in the plan is ambiguous.
- **No commentary between steps** — do not narrate what you are about to do. Just do it. Report only errors, decisions, and the final summary.

## Step 0 — Load the plan and verify state

Read `.claude/plan.md`. If it is empty or missing, stop immediately:

> "No plan found. Run `/next-task` first to create an implementation plan."

If the plan's `**Status:**` field is `Implemented`, stop immediately:

> "This plan is already implemented. Run `/next-task` for a new ticket."

Parse the plan fields:
- **Ticket** — the Jira key
- **Branch** — the expected git branch (e.g. `feature/SHELVD-42`)
- **Status** — must be `Approved` to proceed

Verify the current git branch matches the plan's **Branch**:

```bash
git branch --show-current
```

If the current branch does NOT match the plan's branch, stop immediately:

> "You are on branch `<current>` but this plan targets `<plan-branch>`. Switch to `<plan-branch>` before implementing."

If the current branch is `main` or `develop`, stop immediately even if no branch is in the plan:

> "You are on `<branch>`. Do not implement directly on a protected branch. Create a feature branch first."

Parse the plan sections: **Summary**, **Files to modify/create**, **Tests to write first**, **Implementation steps**, and **Done checklist**.

## Guardrails — when to stop and check in

**Stop and ask the user with `AskUserQuestion` if any of these occur:**

- **Stuck on a test for 3 attempts** — do not silently move on. Report the failure and ask whether to skip, re-approach, or hand off.
- **Scope creep** — if implementing a step requires changes to files not listed in the plan, or introduces new classes/interfaces the plan did not anticipate, stop and describe what you think is needed. Let the user decide whether to expand scope or adjust.
- **Ambiguity in the plan** — if a step is unclear, references something that doesn't exist, or could be interpreted multiple ways, ask rather than guess.
- **Cascading failures** — if fixing one test breaks another, or the same root cause is failing multiple tests, stop after the second unexpected failure and report the pattern.
- **Architecture doubt** — if you are unsure whether an approach violates CLAUDE.md rules (layer boundaries, MVI pattern, DI wiring), ask before writing the code.
- **More than 2 consecutive Gradle failures** — something systemic is wrong. Report what you've tried and ask the user.

**General rule: when in doubt, check in. A 30-second pause is cheaper than 5 minutes of wrong-direction work.**

## Step 1 — TDD implementation loop

For each implementation step in the plan, follow this cycle:

1. **Write the failing test(s)** listed for that step.
2. **Run only the new test(s)** to confirm they fail (red):
   ```bash
   ./gradlew :composeApp:testDebugUnitTest --tests "full.qualified.TestClassName" 2>&1 | tail -100
   ```
   If the test does not compile or fails for the wrong reason, fix the test before proceeding.
3. **Write the minimum production code** to make the test(s) pass.
4. **Re-run the same test(s)** to confirm they pass (green).
5. **Refactor** if needed — re-run the test(s) to confirm they still pass.
6. Move to the next implementation step.

**If a test fails after writing production code:**
- Read the failure output (last 100 lines only).
- Fix the code. Do not rewrite the test to match broken code.
- Re-run. Repeat up to 3 attempts per test. If still failing after 3 attempts, **stop and check in with the user** (see Guardrails above).

## Step 2 — Run all new tests together

After all implementation steps are done, run all new test classes together in one Gradle invocation:

```bash
./gradlew :composeApp:testDebugUnitTest --tests "pkg.Test1" --tests "pkg.Test2" ... 2>&1 | tail -100
```

If any fail, attempt one fix per failing test. If that doesn't resolve it, stop and check in with the user before proceeding to regression checks.

## Step 3 — Regression check

Run the full test suite for affected modules:

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -100
```

If the project has a `shared` module with tests:

```bash
./gradlew :shared:testDebugUnitTest 2>&1 | tail -100
```

## Step 4 — Build affected modules

```bash
./gradlew :composeApp:assembleDebug 2>&1 | tail -100
```

Then verify iOS compilation compiles cleanly:

```bash
./gradlew :composeApp:compileKotlinIosArm64 2>&1 | tail -100
```

## Step 5 — Lint and format

*Assumes the project has the ktlint Gradle plugin configured. If `ktlintFormat` or `ktlintCheck` tasks are not found, skip this step and note it in the final report.*

```bash
./gradlew ktlintFormat 2>&1 | tail -100
```

Then verify:

```bash
./gradlew ktlintCheck 2>&1 | tail -100
```

If ktlintCheck still fails, read the output and fix remaining issues manually, then re-run ktlintCheck once more.

## Step 6 — Evaluate results

Collect all outcomes into three categories:

- **Passed** — tests pass, build succeeds, lint clean.
- **Regressions** — existing tests that now fail (list each: test class, test name, failure reason).
- **Stuck** — new tests that could not be made to pass after 3 attempts (list each with last error).

### If regressions or stuck items exist

Present the list to the user using `AskUserQuestion`:

> **Implementation complete with issues.**
>
> **Regressions (N):**
> - `TestClass#testName` — reason
>
> **Stuck (N):**
> - `TestClass#testName` — reason
>
> How would you like to proceed?
> 1. Fix regressions now (I'll attempt fixes)
> 2. Revert and re-plan
> 3. Continue — I'll handle these manually

Wait for the user's response and follow their direction.

### If everything passes

Run `/code-review` now. Do not mark the plan Implemented or report complete until the review shows no Critical items. Fix any Critical items found, re-run affected tests, then report:

> **Implementation complete.**
> - All new tests pass (N tests)
> - No regressions detected
> - Build succeeds (Android + iOS compilation)
> - Lint clean
> - Code review: no Critical items

## Step 7 — Update plan status

Edit `.claude/plan.md` and change `**Status:** Approved` to `**Status:** Implemented`.

**Do not create a git commit. The user will decide when to commit.**
