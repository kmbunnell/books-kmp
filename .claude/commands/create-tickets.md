---
description: Generate and create Jira tasks for a SHELVD epic
allowed-tools: Bash, Read, AskUserQuestion
---

## Context

`$ARGUMENTS` must be an epic key (e.g. `SHELVD-42`). If empty, stop: "Pass an epic key — e.g. `/create-tickets SHELVD-42`."

## Step 1 — Read planning context

Read `docs/project-planning.md`. Note:
- Which epics are already marked done — do not generate tickets for completed work
- The "Current App State" section — features already implemented must not become ticket scope
- Any planning decisions relevant to this epic

## Step 2 — Fetch the epic

```bash
acli jira workitem view $ARGUMENTS --fields "*all"
```

If not found or not an Epic type, stop: "Epic not found."

Parse: summary, description, acceptance criteria, any child issues already attached.

```bash
acli jira workitem search --jql "project = SHELVD AND parentEpic = $ARGUMENTS" --limit 50
```

List any existing child tickets — do not duplicate them.

## Step 3 — Draft the ticket set

Based on the epic scope, draft a set of tasks. Each task must:
- Be independently implementable and testable (one logical unit of work)
- Follow the ordering and dependency rules from `docs/project-planning.md`
- Note explicit blockers where a task must complete before another in the same epic

For each proposed task, draft:
- **Summary** — short, imperative ("Add X", "Implement Y", "Delete Z")
- **User story** — "As a mobile developer, I need to..." (engineering perspective, not end-user)
- **Context** — why this task exists, relevant background
- **Steps** — numbered implementation steps
- **Acceptance Criteria** — bulleted, testable outcomes
- **Labels** — comma-separated, no spaces (derive from epic category: e.g. `feature,auth,kmp`)
- **Depends on** — other tasks in this set that must complete first (if any)

**Heredoc single-quote rule:** Scan all description text for single quotes (`'`) — possessives, contractions, quoted terms. Rephrase to eliminate them before generating any script. After generating, validate with `bash -n`.

## Step 4 — Present and confirm

Present the full ticket set as a numbered list with summary, AC, and dependencies for each.

Use `AskUserQuestion`:
> Ready to create these N tickets under epic $ARGUMENTS?
> - **Yes** — generate and run the acli script
> - **Revise** — describe changes
> - **Cancel** — stop without creating

If revisions requested, update the draft and re-present. Repeat until approved or cancelled.

## Step 5 — Generate and run the acli script

For each ticket, generate:

```bash
DESCRIPTION=$(cat <<'EOF'
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
  --summary "Summary here" \
  --description "$DESCRIPTION" \
  --label "label1,label2" \
  --parent "$EPIC_KEY"
```

Validate syntax before running:
```bash
bash -n script.sh && echo "Syntax OK"
```

Run the script. Capture and report each created ticket key.

## Output

List the created ticket keys and summaries. Confirm: "N tickets created under $ARGUMENTS."
