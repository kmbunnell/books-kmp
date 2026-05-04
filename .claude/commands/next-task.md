---
description: Pick up the next SHELVD Jira task and create an implementation plan
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

## Context

`$ARGUMENTS` may contain a specific ticket key (e.g. `SHELVD-42`).

This skill runs two phases in sequence. Read and follow each phase's steps file in full before moving to the next.

## Phase 1 — Jira pickup

Read `.claude/commands/jira-pickup.md` and follow all its steps, passing `$ARGUMENTS` as the ticket key argument.

## Phase 2 — Plan build

Read `.claude/commands/plan-build.md` and follow all its steps, using the ticket key from Phase 1.

**Do not write any production or test code.**
