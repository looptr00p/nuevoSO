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
-> Audit event
-> Executor only when explicitly allowed
```

Unknown tools fail closed. Sensitive actions return a safe blocked result until a future
confirmation UI is implemented.

See [docs/SOL_OS_RUNTIME_V0_SECURITY_BASELINE.md](docs/SOL_OS_RUNTIME_V0_SECURITY_BASELINE.md)
for the threat model, policy levels, audit behavior, backup decision, and deferred work.
