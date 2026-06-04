# nuevoSO / Sol OS Runtime v0 — Session Handoff

## Metadata

- Session: `{NNN}`
- Generated at: `{YYYY-MM-DD HH:MM}`
- Agent/tool: `{AGENT_TOOL}`
- Repository: `looptr00p/nuevoSO`
- Branch: `{BRANCH}`
- Branch slug: `{BRANCH_SLUG}`
- Associated workstream: `{WORKSTREAM}`
- Latest known commit: `{COMMIT}`
- Snapshot path: `docs/session-handoffs/{BRANCH_SLUG}/nuevoso_{BRANCH_SLUG}_session-{NNN}_{YYYY-MM-DD}_{HH-MM}.md`

## 1. Project identity

Describe Sol Launcher Lab and the transition toward Sol OS Runtime v0.

## 2. Branch and workstream

- Active branch: `{BRANCH}`
- Branch slug: `{BRANCH_SLUG}`
- Associated feature, fix, task, release, or version: `{WORKSTREAM}`
- Why this branch exists: `{BRANCH_PURPOSE}`

## 3. Session objective

State the bounded objective for this session.

## 4. Verified repository state

```text
git status --short
{OUTPUT}

git branch --show-current
{OUTPUT}

git log --oneline --decorate -n 8
{OUTPUT}
```

## 5. Completed work

- `{ITEM}`

## 6. Work in progress

- `{ITEM}`

## 7. Uncommitted changes

```text
{FILES_AND_STATUS}
```

## 8. Decisions made

| Decision | Reason | Alternatives rejected |
|---|---|---|
| `{DECISION}` | `{REASON}` | `{ALTERNATIVES}` |

## 9. Open risks

| Risk | Severity | Current mitigation | Next action |
|---|---|---|---|
| `{RISK}` | `{SEVERITY}` | `{MITIGATION}` | `{NEXT_ACTION}` |

## 10. Problems and solutions

| Problem | Root cause | Resolution or workaround | Status |
|---|---|---|---|
| `{PROBLEM}` | `{CAUSE}` | `{RESOLUTION}` | `{STATUS}` |

## 11. Artifacts

### Created

- `{PATH}`

### Modified

- `{PATH}`

## 12. Validation evidence

| Command | Result | Notes |
|---|---|---|
| `./gradlew test` | `{RESULT}` | `{NOTES}` |
| `./gradlew lint` | `{RESULT}` | `{NOTES}` |
| `./gradlew assembleDebug` | `{RESULT}` | `{NOTES}` |
| `./gradlew assembleAndroidTest` | `{RESULT}` | `{NOTES}` |
| `./gradlew connectedDebugAndroidTest` | `{RESULT}` | `{NOTES}` |
| `git diff --check` | `{RESULT}` | `{NOTES}` |

Do not mark commands as passed unless they actually ran and passed.

## 13. Environment limitations

- `{LIMITATION}`

## 14. Ordered next steps

1. `{NEXT_STEP}`
2. `{NEXT_STEP}`
3. `{NEXT_STEP}`

## 15. Immediate next action

```text
{IMMEDIATE_NEXT_ACTION}
```

## 16. Prompt de continuación

Actúa como agente de implementación para nuevoSO / Sol OS Runtime v0.

Antes de proponer cambios:
1. Lee `AGENTS.md`.
2. Lee `SESSION_MEMORY.md`.
3. Verifica la rama activa con:
   `git branch --show-current`
4. Lee este snapshot:
   `docs/session-handoffs/{BRANCH_SLUG}/nuevoso_{BRANCH_SLUG}_session-{NNN}_{YYYY-MM-DD}_{HH-MM}.md`
5. Ejecuta:
   - `git status --short`
   - `git branch --show-current`
   - `git log --oneline --decorate -n 8`
6. Resume el estado real, riesgos abiertos y próximo paso inmediato.
7. No modifiques código hasta confirmar que la memoria coincide con el repositorio.
8. Respeta las reglas de seguridad y los límites de alcance definidos en `AGENTS.md`.

Rama esperada:

```text
{BRANCH}
```

Feature, fix, task o versión asociada:

```text
{WORKSTREAM}
```

Objetivo inmediato:

```text
{IMMEDIATE_NEXT_ACTION}
```

No abras objetivos nuevos sin autorización.
No hagas commit ni push sin autorización explícita.
