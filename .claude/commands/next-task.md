---
description: Pick up the next SHELVD Jira task and create an implementation plan
allowed-tools: Bash, Read, Glob, Grep, Agent, AskUserQuestion
---

## Context

`$ARGUMENTS` may contain a specific ticket key (e.g. `SHELVD-42`).

## Phase 1 — Jira pickup

Run `/jira-pickup`, passing `$ARGUMENTS` as the ticket key. Wait for it to complete and capture the ticket key and branch name from its output.

## Phase 2 — Plan build

Run `/plan-build` with the ticket key from Phase 1. Wait for the plan to be approved before finishing.

**Do not write any production or test code.**
