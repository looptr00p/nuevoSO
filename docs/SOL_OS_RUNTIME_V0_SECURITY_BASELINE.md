# Sol OS Runtime v0 Security Baseline

## Current Architecture

Sol is an Android-native launcher lab built with Kotlin, Jetpack Compose, Room, DataStore,
Retrofit, and a Gemini provider behind an `AiProvider` abstraction. The chat UI builds a
system prompt from local memory, sends selected conversation context to the configured
provider, receives model text and tool calls, and dispatches those tool calls locally.

Runtime v0 introduces a deterministic governance layer between model-generated tool calls
and concrete Android executors:

```text
ToolCall
-> ActionRequest
-> PolicyEngine
-> durable pre-execution audit event, or explicit local confirmation for R2/R3
-> Executor only if ALLOW and audit persistence succeeded
-> append-only outcome audit event
```

Executors are only called for `ALLOW` decisions after the mandatory pre-execution audit
event is durably recorded. `REQUIRE_CONFIRMATION` decisions pause locally until the user
approves or rejects the sanitized action prompt. Approved actions still require a durable
pre-execution audit event before execution. If any mandatory audit write fails, the action
does not run.

## Threat Model

The language model is not trusted as a security authority. It can misunderstand user
intent, be influenced by remote content, or propose actions that affect third-party apps.
The local runtime must therefore fail closed for unknown actions, prevent silent sensitive
operations, and avoid storing raw private payloads in audit logs.

High-risk surfaces in this lab include accessibility reads, generic taps, text input,
installed-app inventory, web search queries, calls, app installation, settings changes,
and local memory writes.

## Policy Levels

- `R0_READ_ONLY`: local read-only operations that do not disclose sensitive data externally.
- `R1_REVERSIBLE`: bounded reversible actions such as `open_app`, `press_back`,
  `scroll_screen`, and explicit `flashlight=on/off` operations.
- `R2_SENSITIVE`: actions that persist data, expose user context, contact external
  services, or enter sensitive device flows.
- `R3_DESTRUCTIVE_OR_EXTERNAL`: generic third-party interaction such as `tap_element`
  and `type_text`.
- `R4_BLOCKED`: unknown or unsupported tools.

Runtime v0 allows `R0` and `R1`. `R2` and `R3` require explicit local user confirmation
through the consent lifecycle introduced in `TASK-RUNTIME-001`. `R4` is denied.

## Local-First Privacy Boundary

Room memory, chat history, settings, and action audit events are local-first. Android
backup is disabled for the current laboratory runtime. Backup rules also explicitly
exclude root, file, database, shared preference, external, and device-protected storage
domains for cloud backup and device transfer where the Android backup XML supports them.

Persistence is local-first. Remote inference may still receive selected context until an
on-device model is available. In particular, the configured remote model can receive the
system prompt, selected recent chat history, selected local memory context, tool
declarations, and safe tool results. Approval tokens are local-only app state and are never
sent to the model transcript. Runtime v0 blocks sensitive tool results such as screen reads
from executing silently.

## Audit Trail

The Room database is version 3. The audit table is append-only and keyed by `eventId`,
with `actionId` indexed so multiple lifecycle events can describe one action. Events
record timestamp, tool name, risk level, policy decision, lifecycle stage, sanitized
summary, execution result category, and a controlled `SafeFailureCode`.

Lifecycle stages include pre-execution and terminal states such as `EXECUTION_STARTED`,
`CONFIRMATION_REQUIRED`, `CONFIRMATION_GRANTED`, `CONFIRMATION_REJECTED`,
`CONFIRMATION_EXPIRED`, `DENIED`, `EXECUTION_SUCCEEDED`, `EXECUTION_FAILED`, and
`LEGACY_RECORDED` for migrated v2 audit rows. Room supports the explicit migration path
`v1 -> v2 -> v3`; v2 audit rows are represented as legacy lifecycle events and legacy
summaries are redacted during migration to avoid preserving historical raw payload leaks.

The sanitizer is allowlist-based by tool. Unknown argument values are redacted by default.
Search queries, alarm labels, call targets, remembered facts, tap descriptions, and typed
text persist only length metadata. Unknown tools persist tool name, normalized argument
key names, and value lengths, never raw values. Raw exception messages are not persisted
or returned to the model; controlled failure codes are used instead.

If final audit persistence fails after an executor has already run, the dispatcher does
not retry the executor. It returns a safe uncertainty result stating that the action may
have completed but audit finalization failed. This is a residual limitation for Android
side effects that cannot always be rolled back after execution.

## Consent Lifecycle

`TASK-RUNTIME-001` adds a local in-memory approval store for `REQUIRE_CONFIRMATION`
decisions. Each pending approval receives a UUID v4 token, action ID, deterministic
SHA-256 hash of the sanitized arguments, risk level, issue timestamp, and expiry timestamp.
The default expiry window is 120 seconds.

Tokens are single-use. Unknown, replayed, expired, action-mismatched, or argument-hash
mismatched tokens fail closed and never reach executors. Approval is only consumed through
the app's user-driven confirmation surface; the model-facing dispatch path still returns a
safe refusal and does not expose tokens.

The confirmation UI shows only sanitized tool details: tool name, risk level, and sanitized
summary. It does not show raw arguments, typed text, screen contents, phone targets, URLs,
credentials, or the approval token.

## Accessibility Fallback

The accessibility service can read screens, tap, type, scroll, and press back. This is an
experimental fallback for unintegrated apps, not a general permission to control the
phone. Runtime v0 classifies generic taps and text input as `R3_DESTRUCTIVE_OR_EXTERNAL`
and allows them only after explicit local confirmation.

## Known Limitations

- Gemini API keys are still stored in DataStore; encrypted credential storage remains a
  required follow-up.
- Remote inference is still enabled when the user configures a cloud provider.
- Approval requests are in-memory only; they intentionally do not survive process death.
- Real migration verification exists as an instrumented `MigrationTestHelper` test and
  compiles with `assembleAndroidTest`; connected execution still requires an available
  emulator or device.
- The accessibility service remains available for future governed flows.

## Explicitly Deferred Work

- Encrypted API key storage with Android Keystore-backed credentials.
- Full provider router for cloud and on-device inference.
- Spotify, YouTube, media playback, app removal, app archiving, usage statistics, AOSP,
  device-owner features, privileged package management, purchases, submissions, and
  financial operations.

## Next Recommended Task

Human review of `TASK-RUNTIME-001`, followed by encrypted API key storage with
Android Keystore-backed credentials.
