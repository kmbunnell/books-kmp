---
description: Pick up the next SHELVD Jira task and begin implementation
allowed-tools: Bash(acli:*)
---

## Context

`$ARGUMENTS` may contain a specific ticket key (e.g. `SHELVD-42`).

**If a key was provided**, look it up:

- Ticket lookup: !`acli jira workitem search --jql "project = SHELVD AND status = 'To Do' AND NOT issuetype = Epic AND key = $ARGUMENTS" --limit 1`

If the lookup returned no results, stop immediately and report:
"Ticket key not found in todo"

**If no key was provided**, find the next unassigned ticket:

- Next To Do issue: !`acli jira workitem search --jql "project = SHELVD AND status = 'To Do' AND NOT issuetype = Epic AND assignee is EMPTY ORDER BY created ASC" --limit 1`

If the command above returned no issues, stop immediately and report:
"No free tickets in todo — all To Do items are already assigned."

## Workflow

Run the following steps in order. Stop and report if any step fails.

### Step 1 — Assign and move to In Progress

Extract the issue key (e.g. `SHELVD-12`) from the issue found above, then run:

```bash
acli jira workitem edit --key <KEY> --assignee "@me" --yes
acli jira workitem transition --key <KEY> --status "In Progress" --yes
```

### Step 2 — Read the full issue

```bash
acli jira workitem view <KEY> --fields "*all"
```

Parse the summary, description, and acceptance criteria. These are your requirements.

### Step 3 — Implement

1. Enter plan mode.
2. Scan the project structure to understand what files are relevant.
3. Identify which files need to be created or modified.
4. Write an implementation plan and present it to the user.
5. Wait for the user to confirm before writing any code.
6. Follow TDD: write failing tests first, then implement, then refactor.
