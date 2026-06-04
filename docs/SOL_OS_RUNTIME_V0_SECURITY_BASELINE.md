# Sol OS Runtime v0 Security Baseline

## Current Architecture

Sol is an Android-native launcher lab built with Kotlin, Jetpack Compose, Room, DataStore,
Retrofit, and a Gemini provider behind an `AiProvider` abstraction. The chat UI builds a
system prompt from local memory, sends selected conversation context to the configured
provider, receives model text and tool calls, and dispatches those tool calls locally.

Runtime v0 introduces a deterministic governance layer between model-generated tool calls
and concrete Android executors:

```text
ToolCall -> ActionRequest -> PolicyEngine -> PolicyDecision -> ActionAuditEvent -> Executor
```

Executors are only called for `ALLOW` decisions.

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
  `scroll_screen`, and flashlight toggles.
- `R2_SENSITIVE`: actions that persist data, expose user context, contact external
  services, or enter sensitive device flows.
- `R3_DESTRUCTIVE_OR_EXTERNAL`: generic third-party interaction such as `tap_element`
  and `type_text`.
- `R4_BLOCKED`: unknown or unsupported tools.

Runtime v0 allows `R0` and `R1`. `R2` and `R3` require explicit confirmation and do not
execute yet because the consent lifecycle is deferred to `TASK-RUNTIME-001`. `R4` is denied.

## Local-First Privacy Boundary

Room memory, chat history, settings, and action audit events are local-first. Android
backup is disabled for the current laboratory runtime, and backup rules also exclude
database, shared preference, and file domains as a defense-in-depth default.

Persistence is local-first. Remote inference may still receive selected context until an
on-device model is available. In particular, the configured remote model can receive the
system prompt, selected recent chat history, selected local memory context, tool
declarations, and safe tool results. Runtime v0 blocks sensitive tool results such as
screen reads from executing silently.

## Audit Trail

The audit table records action ID, timestamp, tool name, risk level, policy decision,
sanitized argument summary, execution result category, and optional failure reason. It does
not store raw typed text, remembered facts, phone targets, URLs, API keys, raw prompts, or
full screen contents.

## Accessibility Fallback

The accessibility service can read screens, tap, type, scroll, and press back. This is an
experimental fallback for unintegrated apps, not a general permission to control the
phone. Runtime v0 classifies generic taps and text input as `R3_DESTRUCTIVE_OR_EXTERNAL`
and blocks them until explicit confirmation exists.

## Known Limitations

- There is no consent UI yet, so `REQUIRE_CONFIRMATION` decisions are safe refusals.
- Gemini API keys are still stored in DataStore; encrypted credential storage remains a
  required follow-up.
- Remote inference is still enabled when the user configures a cloud provider.
- Room migration is explicit for v1 to v2, but a full instrumented migration test remains
  a future hardening step.
- The accessibility service remains available for future governed flows.

## Explicitly Deferred Work

- `TASK-RUNTIME-001`: pending consent lifecycle and confirmation UI.
- Encrypted API key storage with Android Keystore-backed credentials.
- Full provider router for cloud and on-device inference.
- Spotify, YouTube, media playback, app removal, app archiving, usage statistics, AOSP,
  device-owner features, privileged package management, purchases, submissions, and
  financial operations.

## Next Recommended Task

Implement `TASK-RUNTIME-001`: a user-visible confirmation lifecycle that can present
sanitized action details, capture explicit consent, execute only the approved action, and
write the final audit result locally.
