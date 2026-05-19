---
description: Build an implementation plan for the current feature ticket and write it to plan.md
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

## Context

`$ARGUMENTS` may contain a ticket key. If empty, derive it from the current branch:

```bash
git branch --show-current
```

Extract key from `feature/SHELVD-XX` pattern. If not on a feature branch, stop: "Cannot determine ticket key. Pass the key as an argument or check out the feature branch first."

## Step 1 — Read the ticket

```bash
acli jira workitem view <KEY> --fields "*all"
```

Parse: summary, description, acceptance criteria.

## Step 2 — Check blockers

```bash
acli jira workitem search --jql "issue in linkedIssues(<KEY>, 'blocks') AND status != 'Done'" --limit 10
```

List any open blockers in the plan.

## Step 3 — Check for existing plan

Read `.claude/plan.md`. If non-empty and for a different ticket, use `AskUserQuestion`: "Existing plan for [TICKET]. Overwrite with [KEY]?" Stop if no.

## Step 4 — Scan the codebase

Use Glob and Grep for targeted lookups. Spawn an Explore agent only if the ticket touches 3+ layers and two targeted searches fail to locate the relevant code.

## Step 5 — Build the plan

1. **Summary** — One sentence: what this ticket does and why.
2. **Requirements** — Acceptance criteria from the ticket. Flag ambiguous items **[NEEDS CLARIFICATION]**.
3. **Files to modify/create** — Exact paths with one-line rationale, grouped by layer: domain → data → presentation.
4. **Tests to write first** — Test files and key cases per implementation step. For every ViewModel async operation include: (a) loading shown while in-flight, (b) loading cleared on success, (c) loading cleared on failure, (d) re-entry guard (second intent while loading does nothing).
5. **Implementation steps** — Ordered list following TDD sequence: test → implement → refactor per step.
6. **Risks and regressions** — Edge cases or breakage risk. "None identified" if clean.

## Step 6 — Present and approve

Present the plan. Address any **[NEEDS CLARIFICATION]** items first.

Use `AskUserQuestion`:

> Approve this plan?
> - **Yes** — writes to `.claude/plan.md`. Run `/clear` then `/implement-plan` when ready.
> - **Request changes** — describe what to revise.
> - **Cancel** — stop without writing.

If changes are requested, revise and re-present. Repeat until approved or cancelled.

## Step 7 — Write the plan

```
# Implementation Plan for <KEY>: <Summary>

**Ticket:** <KEY>
**Branch:** feature/<KEY>
**Status:** Approved

<plan content>
```

Confirm: "Plan written. Run `/implement-plan` when ready."

**Do not write any production or test code.**
