# nuevoSO

Sol Launcher Lab is an Android-native AI launcher prototype. It is now beginning the
transition toward Sol OS Runtime v0: a security-governed runtime where the model can
propose actions, but deterministic local policy decides whether those actions may run.

Persistence is local-first. Remote inference may still receive selected context until an
on-device model is available.

## Runtime v0 Security Baseline

The current runtime routes model tool calls through:

```text
Model proposal
-> ActionRequest
-> PolicyEngine
-> PolicyDecision
-> durable pre-execution audit event
-> Executor only when explicitly allowed and audit persistence succeeded
-> append-only outcome audit event
```

Unknown tools fail closed. Sensitive actions require explicit local confirmation through
the merged `TASK-RUNTIME-001` consent lifecycle before execution. Action audit storage is
Room v3 and append-only: each action can have multiple lifecycle events keyed by
`eventId`, with `actionId` indexed for review.

The current active hardening objective is Android Keystore-backed encrypted API key
storage. Provider/model settings remain non-sensitive DataStore preferences; raw API keys
must not be stored in DataStore, logs, audit records, UI state, prompts, tests, or errors.

See [docs/SOL_OS_RUNTIME_V0_SECURITY_BASELINE.md](docs/SOL_OS_RUNTIME_V0_SECURITY_BASELINE.md)
for the threat model, policy levels, audit behavior, backup decision, and deferred work.
