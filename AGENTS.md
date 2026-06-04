# Sol OS Runtime v0 — Codex Working Agreement

## Project identity

This repository contains **Sol Launcher Lab** and the emerging **Sol OS Runtime v0**.

Sol is an AI-native Android launcher evolving toward a security-governed operating-system runtime.

The language model may propose actions.
The language model must never be the security authority.

## Current objective

Complete:

```text
TASK-RUNTIME-002
Implement Android Keystore-backed encrypted API key storage
```

`TASK-RUNTIME-001` was reviewed by a human and merged in commit `3d447bd`.
Do not begin the next runtime objective until the credential storage task has been
reviewed and approved by a human.

## Security principles

- Security correctness has priority over preserving demo behavior.
- Fail closed for unknown actions, malformed inputs, missing permissions, policy ambiguity, and mandatory audit failures.
- Do not add bypass flags.
- Do not weaken deterministic policies for convenience.
- Do not persist raw private payloads.
- Do not persist raw exception messages.
- Do not expose API keys, tokens, credentials, typed text, screen contents, phone targets, URLs with sensitive parameters, user facts, or full app inventories.
- Do not retry executors automatically after uncertain Android side effects.
- Treat documentation as part of the security boundary.
- Prefer bounded, declarative actions over ambiguous commands.
- Assume remote models are untrusted components, not security authorities.

## Local-first data policy

- Store user data locally whenever possible.
- Remote inference may receive only the minimum selected context required for the task.
- Never commit secrets.
- Never add real personal data to fixtures or tests.
- Use synthetic data in tests.
- Keep Android backup disabled unless an explicitly approved encrypted recovery design exists.
- Exclude sensitive storage from cloud backup and device-to-device transfer as defense in depth.
- Preserve local memory across schema upgrades through explicit Room migrations.

## Action-governance rules

Every model-generated tool call must follow a deterministic local path:

```text
ToolCall
→ parse and normalize
→ sanitize
→ ActionRequest
→ PolicyEngine
→ durable pre-execution audit event
→ Executor only when explicitly allowed
→ append-only outcome audit event
```

Mandatory rules:

- Unknown tools must fail closed.
- Sensitive actions must never execute silently.
- Destructive or externally consequential actions must require explicit user confirmation.
- Generic accessibility taps and text entry are restricted fallbacks.
- Mandatory pre-execution audit persistence must succeed before execution.
- Final audit persistence failure must never trigger an automatic executor retry.
- Sanitization must fail closed: unknown argument values are redacted by default.
- Persist controlled failure codes instead of raw exception messages.
- Audit events must be append-only and support multiple lifecycle events for one action ID.

## Android conventions

- Use Kotlin identifiers in English.
- Prefer idiomatic Kotlin and small, reviewable changes.
- Use explicit Room migrations.
- Never add `fallbackToDestructiveMigration()`.
- Enable and maintain Room schema export when migrations are introduced.
- Add real migration verification with `MigrationTestHelper`.
- Prefer declarative actions over in-memory toggles.
- Flashlight control must require explicit `on` or `off` values.
- Accessibility is an experimental governed fallback, not a general authorization mechanism.
- Keep remote-provider integration behind abstractions suitable for future local or hybrid inference.

## Testing expectations

For security-sensitive changes, add focused tests for:

- unknown tools denied by default
- sensitive tools requiring confirmation
- blocked actions never reaching executors
- pre-execution audit failure preventing execution
- outcome audit failure not retrying an executor
- append-only audit lifecycle
- sanitizer redacting unknown arguments by default
- safe failure-code persistence
- Room migration preservation
- Android backup exclusion configuration
- explicit flashlight semantics

Do not claim that a test passed unless it actually ran and passed.

## Required workflow

Before editing:

```bash
git status --short
git branch --show-current
git log --oneline --decorate -n 8
```

Before handoff:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleAndroidTest
git status --short
git diff --stat
git diff --check
```

When an emulator is available:

```bash
./gradlew connectedDebugAndroidTest
```

If a command fails:

- report the exact command
- report the exact reason
- distinguish baseline failures from introduced failures
- distinguish environment limitations from code failures
- do not conceal or minimize failures

## Git rules

- Do not use destructive Git commands.
- Do not overwrite unrelated local changes.
- Do not commit unless explicitly requested.
- Do not push unless explicitly requested.
- Do not rewrite history.
- Stop after completing the bounded task.
- Provide a handoff for human review before starting the next objective.

## Scope boundaries for TASK-RUNTIME-001

`TASK-RUNTIME-001` is closed and merged. The active task is bounded to secure
credential storage and legacy API key migration.

Do not implement:

- Spotify integration
- YouTube integration
- media controls
- app removal
- app archiving
- usage analytics
- inference router
- local model execution
- Sol Algebraico UI
- AOSP integration
- device-owner features
- privileged installation
- purchases
- form submission
- financial operations
- autonomous messaging
- unrelated UI work

## Required final handoff

At the end of the task, report:

1. Baseline state before edits.
2. Confirmed risks.
3. Architecture changes made.
4. Token and consent lifecycle design.
5. Expiry and replay protection design.
6. Confirmation UI design.
7. Audit integration design.
8. Database migration design.
9. Files created.
10. Files modified.
11. Tests added.
12. Exact validation commands executed.
13. Exact results.
14. Remaining risks.
15. Environment limitations.
16. Deferred work.
17. Recommended next task.
18. `git diff --stat`.
19. Confirmation that no commit and no push were performed.
