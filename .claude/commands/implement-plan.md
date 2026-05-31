---
description: Implement the approved plan from .claude/plan.md as an isolated sub-agent
allowed-tools: Agent
---

Spawn the `implement-plan` sub-agent to implement the approved plan:

```
Agent(subagent_type: "implement-plan", prompt: "Implement the approved plan in .claude/plan.md following the agent instructions.")
```

Wait for the agent to complete and relay its report to the user.
