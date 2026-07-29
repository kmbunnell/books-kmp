---
description: Build a full TDD-ready implementation plan from a freeform description (no ticket required) and write it to plan.md
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

You are acting as a senior KMP/CMP engineer who knows this codebase's Clean Architecture + MVI conventions cold. Use that judgment when placing changes in the right layer, naming things consistently with the rest of the app, and flagging best-practice conflicts — don't just mechanically fill in section headers.

## Context

`$ARGUMENTS` is a freeform description of what to plan — no ticket required. If empty, stop and ask the user to describe what they want planned.

## Step 1 — Check for existing plan

Read `.claude/plan.md`. If non-empty, show its title/summary line and use `AskUserQuestion`: "Existing plan for [TITLE]. Overwrite with the new plan?" Stop if no.

## Step 2 — Scan the codebase

Use Glob and Grep for targeted lookups. Spawn an Explore agent only if the request touches 3+ layers and two targeted searches fail to locate the relevant code.

## Step 3 — Build the plan

1. **Summary** — One sentence: what this plan does and why.
2. **Requirements** — Distilled from the freeform description. Flag ambiguous items **[NEEDS CLARIFICATION]**. Flag any spot where a current KMP/Android/iOS best practice would conflict with an AGENTS.md rule **[BEST-PRACTICE CONFLICT]**.
3. **Files to modify/create** — Exact paths with one-line rationale, grouped by layer: domain → data → presentation.
4. **Tests to write first** — Test files and key cases per implementation step. For every ViewModel async operation include: (a) loading shown while in-flight, (b) loading cleared on success, (c) loading cleared on failure, (d) re-entry guard (second intent while loading does nothing).
5. **Implementation steps** — Ordered list following TDD sequence: test → implement → refactor per step.
6. **Risks and regressions** — Edge cases or breakage risk. "None identified" if clean.

## Step 4 — Present and approve

Present the plan. Address any **[NEEDS CLARIFICATION]** or **[BEST-PRACTICE CONFLICT]** items first.

Use `AskUserQuestion`:

> Approve this plan?
> - **Yes** — writes to `.claude/plan.md`. Run `/clear` then `/implement-plan` when ready.
> - **Request changes** — describe what to revise.
> - **Cancel** — stop without writing.

If changes are requested, revise and re-present. Repeat until approved or cancelled.

## Step 5 — Write the plan

```bash
git branch --show-current
```

```
# Implementation Plan: <Summary>

**Source:** Freeform request
**Branch:** <current branch>
**Status:** Approved

<plan content>
```

Confirm: "Plan written. Run `/implement-plan` when ready."

**Do not write any production or test code.**
