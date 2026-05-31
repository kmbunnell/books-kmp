---
description: Review changed code as a senior KMP/CMP engineer in an isolated sub-agent
allowed-tools: Agent
---

Spawn the `kmp-reviewer` sub-agent to review the current diff:

```
Agent(subagent_type: "kmp-reviewer", prompt: "Review the current diff against all checklists in the agent instructions and produce the full report.")
```

Wait for the agent to complete and relay its report to the user.
