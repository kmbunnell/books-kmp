---
description: Fetch a SHELVD Jira ticket, assign it, transition to In Progress, and create the feature branch
allowed-tools: Bash, AskUserQuestion
---

## Context

`$ARGUMENTS` may contain a specific ticket key (e.g. `SHELVD-42`). If empty, find the next unassigned To Do ticket.

## Step 1 — Verify acli

```bash
acli jira workitem search --jql "project = SHELVD" --limit 1
```

If this fails, stop: "Cannot connect to Jira. Check that `acli` is installed and authenticated."

## Step 2 — Fetch ticket

**If `$ARGUMENTS` is non-empty:**
```bash
acli jira workitem search --jql "project = SHELVD AND NOT issuetype = Epic AND key = $ARGUMENTS" --limit 1
```
If no results: stop — "Ticket key not found."

**If `$ARGUMENTS` is empty:**
```bash
acli jira workitem search --jql "project = SHELVD AND status = 'To Do' AND NOT issuetype = Epic AND assignee is EMPTY ORDER BY priority ASC, created ASC" --limit 1
```
If no results: stop — "No free tickets in To Do."

## Step 3 — Assign and transition

Extract the key (e.g. `SHELVD-12`), then:

```bash
acli jira workitem edit --key <KEY> --assignee "@me" --yes
acli jira workitem transition --key <KEY> --status "In Progress" --yes
```

## Step 4 — Create or check out branch

```bash
git branch --list feature/<KEY>
```

If the branch exists: `git checkout feature/<KEY>` — report "Branch already exists — checked out."

If not, check current branch:
```bash
git branch --show-current
```
- On `main` or `develop`: `git checkout -b feature/<KEY>`
- On another feature branch: warn the user, then create anyway.

## Output

Report the ticket key, summary line, and branch name. These are inputs to `/plan-build`.
