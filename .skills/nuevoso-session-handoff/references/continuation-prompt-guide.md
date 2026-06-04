# Continuation Prompt Guide

A continuation prompt for nuevoSO must be short enough to paste into a fresh agent session,
but specific enough to avoid rediscovery work.

This guide is agent-agnostic.

Always include:

1. Project identity: `nuevoSO / Sol OS Runtime v0`.
2. Required files to read:
   - `AGENTS.md`
   - `SESSION_MEMORY.md`
   - latest snapshot for the active branch under `docs/session-handoffs/{branch-slug}/`
3. The expected Git branch.
4. The feature, fix, task, release, or version associated with that branch.
5. Commands to verify actual repository state.
6. The bounded immediate objective.
7. A reminder not to start new objectives.
8. A reminder not to commit or push without explicit permission.
9. Security-first rules when the task touches actions, memory, backup, migrations, credentials,
   accessibility, remote providers, or user data.
10. Environment limitations when the next agent cannot execute Git, Gradle, Android SDK, CI,
    or emulator checks directly.

Branch naming rules:

```text
feat/<feature-name>
fix/<task-or-bug-name>
task/<task-id>-<short-name>
release/<version-or-milestone>
chore/<maintenance-name>
```

Snapshot naming rules:

```text
docs/session-handoffs/{branch-slug}/
nuevoso_{branch-slug}_session-{NNN}_YYYY-MM-DD_HH-MM.md
```

Do not mention tool-specific reset commands unless the active agent actually supports them.
Do not paste secrets, raw logs, local databases, or personal data into the continuation prompt.
