---
description: Scope a freeform feature ask into an overview, change outline, and task breakdown
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

You are acting as a senior KMP/CMP engineer who knows this codebase's Clean Architecture + MVI conventions cold. Use that judgment when placing changes in the right layer, naming things consistently with the rest of the app, and flagging best-practice conflicts — don't just mechanically fill in section headers.

**Best-practice conflicts.** AGENTS.md states: "If a current KMP/Android/iOS platform best practice would conflict with a rule in this document, do not silently follow either side." Whenever the codebase scan, architecture outline, or task breakdown surfaces a spot where current platform best practice would push against an AGENTS.md rule, tag it **[BEST-PRACTICE CONFLICT]** inline and surface it for discussion — never resolve it silently in either direction. This applies across every step below, not just Step 5.

## Step 1 — Context

`$ARGUMENTS` is the freeform feature description. If empty, stop and ask the user to describe the feature.

## Step 2 — Check for existing feature plan

Read `.claude/feature-plan.md`. If it exists and covers a different feature, use `AskUserQuestion`: "Existing feature plan for [TITLE]. Overwrite with the new ask?" Stop if no.

## Step 3 — Scan the codebase

Use Glob and Grep for targeted lookups of existing related models, repositories, use cases, and screens. Spawn an Explore agent only if the feature touches 3+ layers and two targeted searches fail to locate the relevant code. Actively look for utilities and patterns to reuse rather than proposing new ones.

## Step 4 — Check past decisions

Grep `.claude/decisions.md` for entries relevant to this feature's area. Note any that constrain the approach.

## Step 5 — Surface ambiguities

Before drafting, identify missing requirements or open scope questions (platform scope, data model shape, interaction with existing features, edge cases). Batch them into `AskUserQuestion` — don't silently assume on scope-defining questions. Also check here for any **[BEST-PRACTICE CONFLICT]** per the standing rule above — a conflict is itself a reason to ask, not just a reason to flag in prose.

## Step 6 — Build the plan

1. **Overview** — 2–4 sentences: what the feature is, why, user-facing behavior.
2. **Requirements** — Bullets distilled from the ask + clarifying answers. Unresolved items flagged **[NEEDS CLARIFICATION]**; platform-vs-AGENTS.md tensions flagged **[BEST-PRACTICE CONFLICT]**.
3. **Architecture outline** — Grouped domain → data → presentation, following AGENTS.md's Clean Architecture/MVI rules (new/changed domain models, `XxxError` sealed interfaces, use cases only where real orchestration exists, repository interface/impl, ViewModel intents/state/effects, Compose screens, Koin wiring). Name concrete file paths where identifiable from the scan; otherwise describe by the app's existing naming convention. Tag **[BEST-PRACTICE CONFLICT]** on any layer/pattern choice where doing it "the AGENTS.md way" diverges from current platform guidance.
4. **Related past decisions** — Relevant `.claude/decisions.md` entries, so the breakdown doesn't contradict them.
5. **Task breakdown** — Ordered, actionable, roughly PR-sized tasks, each layer-tagged with a 1–2 sentence description and likely files. This is a scoping-level breakdown, not a full TDD step list (that's `/plan-build`/`/implement-plan`'s job once a task is picked up individually). Order by dependency, not just narrative flow: a task may not reference or consume an artifact (schema column, class, queue, config) that only a later-numbered task creates. Before finalizing the list, re-check each task against every other task for this — prerequisites (schema/infra checks, shared building blocks multiple tasks consume) belong as early as their own dependencies allow, even if that reads less narratively linear.
6. **Open questions / risks** — Anything deferred. "None identified" if clean.

## Step 7 — Present and confirm

Address any **[NEEDS CLARIFICATION]** or **[BEST-PRACTICE CONFLICT]** items first.

Use `AskUserQuestion`:

> Approve this feature plan?
> - **Yes** — writes to `.claude/feature-plan.md`.
> - **Request changes** — describe what to revise.
> - **Cancel** — stop without writing.

If changes are requested, revise and re-present. Repeat until approved or cancelled.

## Step 8 — Write the plan

```
# Feature Plan: <Title>

**Status:** Draft
**Created:** <date>

<plan content>
```

Confirm: "Feature plan written to `.claude/feature-plan.md`. Pick a task and scope it into a ticket, or hand a task to `/plan-build` once it has a ticket key."

## Step 9 — Optionally log a decision

If the scoping conversation surfaced a notable, non-obvious decision (per AGENTS.md's instruction to log such decisions), offer to append it to `.claude/decisions.md` in its existing format, after confirming with the user.

**Do not write any production or test code.**
