---
description: Pick up the next SHELVD Jira task and create an implementation plan
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

## Context

`$ARGUMENTS` may contain a specific ticket key (e.g. `SHELVD-42`).

**Before doing anything else, verify `acli` is available and authenticated:**

```bash
acli jira workitem search --jql "project = SHELVD" --limit 1
```

If this fails, stop immediately and report: "Cannot connect to Jira. Check that `acli` is installed and authenticated."

---

**If a key was provided** (i.e. `$ARGUMENTS` is non-empty), look it up:

```bash
acli jira workitem search --jql "project = SHELVD AND NOT issuetype = Epic AND key = $ARGUMENTS" --limit 1
```

If the lookup returned no results, stop immediately and report:
"Ticket key not found"

**If no key was provided** (i.e. `$ARGUMENTS` is empty), find the next unassigned ticket:

```bash
acli jira workitem search --jql "project = SHELVD AND status = 'To Do' AND NOT issuetype = Epic AND assignee is EMPTY ORDER BY priority ASC, created ASC" --limit 1
```

If the command above returned no issues, stop immediately and report:
"No free tickets in todo — all To Do items are already assigned."

## Workflow

Run the following steps in order. Stop and report if any step fails.

### Step 1 — Assign, move to In Progress, and create branch

Extract the issue key (e.g. `SHELVD-12`) from the issue found above, then run:

```bash
acli jira workitem edit --key <KEY> --assignee "@me" --yes
acli jira workitem transition --key <KEY> --status "In Progress" --yes
```

Then check if the feature branch already exists:

```bash
git branch --list feature/<KEY>
```

**If the branch already exists**, check it out:
```bash
git checkout feature/<KEY>
```
Report: "Branch `feature/<KEY>` already exists — checked out."

**If the branch does not exist**, check the current branch:
```bash
git branch --show-current
```

If the current branch is `main` or `develop`, create and checkout a feature branch:
```bash
git checkout -b feature/<KEY>
```

If already on a feature branch (not `main` or `develop`), warn the user:
> "You are currently on branch `<current-branch>`, not main or develop. Creating `feature/<KEY>` from here. If this is unintentional, stop and switch to main first."
Then create the branch anyway:
```bash
git checkout -b feature/<KEY>
```

### Step 2 — Read the full issue

```bash
acli jira workitem view <KEY> --fields "*all"
```

Parse the summary, description, and acceptance criteria. These are your requirements.

### Step 3 — Check for blockers

```bash
acli jira workitem search --jql "issue in linkedIssues(<KEY>, 'blocks') AND status != 'Done'" --limit 10
```

If any blocking tickets are not Done, list them in the plan as unresolved blockers and warn the user.

### Step 4 — Check for existing plan

Read `.claude/plan.md`. If it is not empty and already contains a plan:
1. Report which ticket the existing plan is for (read the title line).
2. Ask the user: "There is an existing plan for [TICKET]. Overwrite it with a new plan for [CURRENT-KEY]?"
3. If the user says no, stop.
4. If yes, proceed — the file will be overwritten in Step 6.

### Step 5 — Build the plan

**Keep planning focused and concise. Minimize token usage — no boilerplate, no restating obvious context.**

Scan the codebase to understand what exists and what needs to change. Default to Glob, Grep, and Read for targeted lookups. Only use an Explore agent if the ticket touches 3+ layers or you cannot locate the relevant code after 2 targeted searches.

Cross-reference the ticket requirements against:
- `CLAUDE.md` — architecture rules, MVI pattern, TDD requirements, code conventions
- `docs/overview.md` — only for tech rationale, schema notes, or planned features

**Pitfall scan:** Before writing the plan, check each implementation step for CLAUDE.md violations and make the correct pattern explicit in that step rather than leaving it implicit. Pay special attention to: `isLoading = true` before every async call and `false` on every exit path (success + each failure branch); `CancellationException` rethrown before any broad `catch (e: Exception)` in a `suspend` function.

Build a plan with these sections:

1. **Summary** — One sentence: what this ticket does and why.
2. **Requirements** — Bullet list of acceptance criteria from the ticket. Flag any that are ambiguous or incomplete as **[NEEDS CLARIFICATION]**.
3. **Files to modify/create** — Exact file paths with a one-line rationale for each. Group by layer (domain, data, presentation).
4. **Tests to write first (TDD)** — List the test files and key test cases that will be written before production code. Include both unit tests and Compose UI tests where applicable. For each ViewModel async operation, the test list must include: (a) loading indicator shown while in-flight, (b) loading cleared on success, (c) loading cleared on failure, (d) a second intent while loading does not trigger a duplicate call.
5. **Implementation steps** — Ordered list of what to build, in the sequence that satisfies TDD (test → implement → refactor per step).
6. **Risks and regressions** — Anything that could break existing functionality, edge cases to watch for, or architectural concerns. If none, say "None identified."

### Step 6 — Present and get approval

Present the plan to the user. If you flagged any **[NEEDS CLARIFICATION]** items, ask about those first.

After presenting, remind the user:

> The approved plan will be written to `.claude/plan.md`. When you're ready to implement, run `/clear` then `/implement-plan`.

Use `AskUserQuestion` to ask the user if they approve the plan, want changes, or need to clarify requirements.

### Step 7 — Write the approved plan

Once the user approves, write the plan to `.claude/plan.md` with this format:

```markdown
# Implementation Plan for <KEY>: <Summary>

**Ticket:** <KEY>
**Branch:** feature/<KEY>
**Status:** Approved

<the approved plan content from Step 5>
```

Confirm to the user that the plan has been written and remind them to run `/clear` then `/implement-plan` when ready.

**Do not write any production code or test code. This skill is planning only.**
