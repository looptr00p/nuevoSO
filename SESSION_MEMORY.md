# nuevoSO — Memoria de sesión (continuidad entre sesiones)

> Este archivo es la **fuente de verdad** para retomar el proyecto en cualquier sesión
> nueva de Claude Code (web o local). Está versionado en el repo a propósito: los
> contenedores son efímeros, así que todo lo que importa vive aquí (y en git).
>
> **Cómo usarlo:** invoca la skill `/nuevoso-continue` o di "continúa con nuevoSO".
> Mantén este archivo actualizado al final de cada sesión (sección *Bitácora*).

---

## 1. Qué es nuevoSO

Un **launcher Android nativo (APK, Kotlin/Compose)** que convierte un teléfono Android
existente en un "SO 100% IA": la IA es la cara del sistema — lanza apps, ejecuta acciones
y **aprende del usuario** mediante una capa de memoria local.

**Decisiones de arquitectura ya tomadas (no re-litigar sin pedirlo al usuario):**

- **APK nativo, NO PWA.** Solo una app nativa con `intent-filter category HOME` puede ser
  el launcher por defecto. Un ROM/AOSP no es viable (no compila en CI gratis, riesgo de
  brick). El launcher = front-end real del "SO IA", sin riesgo.
- **Se compila en GitHub Actions, NO en el contenedor.** El Android SDK está bloqueado aquí
  (`dl.google.com` → 403). Los runners `ubuntu-latest` traen el SDK preinstalado y producen
  `app-debug.apk` como artifact descargable.
- **Motor IA: Gemini (capa gratuita)** por defecto, detrás de una **capa de proveedor
  intercambiable** (`AiProvider`). Requisito explícito del usuario: no atarse a ningún
  modelo; poder enchufar Claude, DeepSeek local o futuros modelos sin reescribir la app.
- **"Aprende de mí" = memoria LOCAL (Room).** Ningún modelo se reentrena. Se guardan
  `UserFact` + historial de chat en el teléfono y se inyectan al prompt. 100% on-device.
- **Futuro (v2): modo híbrido conmutable** nube ↔ modelo on-device (Gemma/DeepSeek vía
  MediaPipe/MLC). La interfaz `AiProvider` ya lo deja listo; falta el runtime local.

---

## 2. Estado actual (al 2026-06-04)

- ✅ **Compila en CI.** Último CI verde registrado: commit `6075541`, run `26862773391` (~17.65 MB).
  Localmente Gradle compila en esta máquina con acceso a `~/.gradle`; no hay emulator/device activo
  en `adb devices` durante esta sesión.
- ✅ Repo en GitHub: **https://github.com/looptr00p/nuevoSO** (rama `main`).
- ✅ App completa v1 (chat home + cajón de apps + ajustes + agente) con Runtime v0 gobernado.
- ✅ `TASK-RUNTIME-001` cerrado, revisado por humano y mergeado en `main` como `3d447bd`
  (`feat: add governed consent lifecycle and basic actions`).
- 🚧 Objetivo activo de seguridad: almacenamiento cifrado de API keys con Android
  Keystore-backed storage y migración legacy desde DataStore.
- ⚠️ **Commits salen `verified: false`** (firma del entorno rota, error 400 del servidor de
  firmas). Es cosmético, no afecta código ni APK. Workaround: `git -c commit.gpgsign=false`.
- 📥 **Descarga del APK:** Actions → run verde → sección *Artifacts* (al final) → `app-debug`
  → descomprimir el zip. Requiere estar logueado en GitHub.

---

## 3. Stack y versiones (pineadas en `gradle/libs.versions.toml`)

- Kotlin **2.0.20** (requiere plugin `org.jetbrains.kotlin.plugin.compose` — olvidarlo es
  la causa #1 de fallo "Compose Compiler unsupported").
- AGP **8.5.2**, Gradle **8.9** (wrapper, jar COMMITEADO), JDK **17** en CI, KSP `2.0.20-1.0.25`.
- compileSdk/targetSdk **34**, minSdk **26**.
- Compose BOM 2024.09.03, Navigation-Compose 2.8.2, lifecycle 2.8.6, activity-compose 1.9.2.
- Room 2.6.1 (KSP), DataStore-Preferences 1.1.1.
- Retrofit 2.11.0 + retrofit2-kotlinx-serialization-converter 1.0.0, OkHttp logging 4.12.0,
  kotlinx-serialization-json 1.7.3, coroutines 1.8.1.
- Solo Maven Central + Google Maven (los únicos repos que los runners alcanzan).

---

## 4. Mapa del código (`app/src/main/java/com/nuevoso/launcher/`)

```
App.kt                 # Application: arma dependencias a mano (sin Hilt en v1)
MainActivity.kt        # Activity HOME; NavHost (chat | drawer | settings)
data/
  settings/SettingsRepository.kt   # DataStore: apiKey, modelId, provider
  apps/AppRepository.kt            # PackageManager: listar/lanzar apps
  memory/                          # *** capa que "aprende de mí" (Room) ***
    MemoryDb.kt, MemoryDao.kt, MemoryEntities.kt (UserFact, ChatMessageEntity),
    MemoryRepository.kt            # buildMemoryContext(), saveMessage(), saveFact()
ai/
  AiProvider.kt        # *** interfaz intercambiable: chat(system, history, tools, toolResults) -> AiTurn
  AiModels.kt          # tipos neutrales: Msg, ToolCall, ToolResult, ToolSpec, ParamSpec, AiTurn
  ProviderFactory.kt   # elige provider según settings (gemini hoy)
  Prompts.kt           # fun buildSystemPrompt(memoryContext): String  (top-level, NO en object)
  gemini/GeminiProvider.kt, GeminiApi.kt, GeminiDtos.kt
agent/
  AgentLoop.kt         # bucle máx 15 rondas; pausa localmente si una tool requiere confirmación
  Tools.kt             # ALL_TOOLS: open_app, search_web, set_alarm, call, toggle_setting, remember_fact
  ActionDispatcher.kt  # ToolCall -> ActionRequest -> PolicyEngine -> audit/consent/executor
  security/            # PolicyEngine, sanitizer, audit models, approval token store
  executors/{OpenApp,SearchWeb,Dial,SetAlarm,ToggleSetting}Executor.kt
ui/
  chat/                # ConfirmationPanel muestra solo tool/risk/sanitizedSummary
  theme/{Theme,Color,Type}.kt
  chat/{ChatScreen,ChatViewModel,ChatUiState}.kt   # SUPERFICIE HOME
  drawer/{AppDrawerScreen,AppDrawerViewModel}.kt
  settings/{SettingsScreen,SettingsViewModel}.kt
```

**AndroidManifest.xml** — lo que lo hace launcher: `MainActivity` con DOS `intent-filter`
(`MAIN+LAUNCHER` y `MAIN+HOME+DEFAULT`), `launchMode="singleTask"`, `stateNotNeeded="true"`,
`exported="true"`. Permisos: `INTERNET`, `QUERY_ALL_PACKAGES` (+ bloque `<queries>`
MAIN/LAUNCHER), `SET_ALARM`.

---

## 5. Gotchas críticos (no repetir errores ya resueltos)

- `gradle/wrapper/gradle-wrapper.jar` **DEBE** estar commiteado (binario). `.gitignore`
  tiene `!gradle/wrapper/gradle-wrapper.jar`. `gradlew` con bit ejecutable.
- `ExposedDropdownMenu` **NO se importa**: es miembro del scope de `ExposedDropdownMenuBox { }`.
  Importarlo rompe la compilación (ya pasó en build 51cb0fb).
- `JsonPrimitive.content` es propiedad miembro — NO existe `import kotlinx.serialization.json.content`.
- Lanzar apps desde el launcher requiere `FLAG_ACTIVITY_NEW_TASK`.
- `QUERY_ALL_PACKAGES` + `<queries>` obligatorio en Android 11+ o el cajón sale vacío.
- API key solo on-device, **NUNCA** en el repo. Las claves deben vivir en almacenamiento
  cifrado respaldado por Android Keystore; DataStore solo conserva provider/model no sensibles.
- Wi-Fi/Bluetooth no se pueden conmutar por código en Android moderno: `toggle_setting`
  hace deep-link al panel. Solo la linterna (`CameraManager.setTorchMode`) es programable
  y ahora exige valor declarativo explícito `on` u `off`; no hay toggle por estado local.
- Si Gradle falla por permisos sobre `~/.gradle`, reintentar con permisos aprobados. Para releases,
  seguir verificando también vía CI verde.

---

## 6. Operativa de git en este entorno

- Remote HTTPS ya configurado. Push: `git push -u origin main`; reintentar con backoff
  (2s/4s/8s/16s) solo si falla por red.
- Firma rota → commitear con `git -c commit.gpgsign=false commit ...`. Los commits salen
  como Unverified; es esperado y cosmético.
- `gh` CLI: usar el que esté disponible; si no, API vía token. NO crear PRs salvo que el
  usuario lo pida explícitamente.
- Para ver por qué falló un build de CI (los logs crudos están bloqueados por host): el
  workflow ya emite `::error::` annotations legibles vía la API de check-runs annotations.

---

## 7. Próximos pasos sugeridos (backlog v1.x / v2)

Pendientes y mejoras candidatas (confirmar prioridad con el usuario antes de implementar):

- [ ] **Voz (es-ES)**: `SpeechRecognizerManager` + TTS (entrada/salida por voz en el chat).
- [x] **Emulador local** — `Pixel_3a_API_34` configurado en Mac, `local.properties` + `~/.zshrc` con `ANDROID_HOME`.
- [x] **Accessibility Service** (`NuevoSOAccessibilityService`) — `read_screen`, `tap_element`, `type_text`, `scroll_screen`, `press_back`. Activación desde Ajustes.
- [x] **Navegación web autónoma** — system prompt con flujo obligatorio search→read→tap. Límite de rondas 6→15.
- [x] **`list_apps`** — el agente puede listar apps instaladas.
- [x] **`install_app`** — abre Play Store, navega y toca Instalar internamente; retry 429 con backoff en GeminiProvider.
- [x] **Fixes**: botón ← en Settings y Drawer; crash cajón (`distinctBy packageName`); `GeminiSchema.type` sin default (fix 400).
- [x] **Streaming SSE** de respuestas de Gemini — hecho (2026-06-03). `streamGenerateContent?alt=sse`.
- [x] **EncryptedSharedPreferences / AndroidX Security** para la API key con migración legacy
  (implementado localmente; pendiente review humano/merge).
- [ ] **Proveedor Claude** (`ClaudeProvider : AiProvider`) para validar la abstracción.
- [ ] **Modo híbrido on-device** (Gemma/DeepSeek vía MediaPipe LLM Inference o MLC) — v2.
- [ ] **Widgets/gestos** y generación de mini-apps a medida.
- [ ] **Release firmado** (keystore) para distribución fuera de debug.
- [ ] Mejorar el cajón de apps (búsqueda, favoritos, orden por uso registrado en memoria).

---

## 8. Bitácora (añadir entrada al final de cada sesión)

- **2026-06-03** — v1 completa y compilando en CI (commit `2abbfcc`). APK descargable.
  Creada esta memoria de sesión + skill `nuevoso-continue` para continuidad.
- **2026-06-03** — Solidificado el agente + streaming SSE.
  - Agente: la conversación viaja como **transcript único** (`Msg` ahora lleva `toolCalls`/
    `toolResults`; roles `user`/`model`/`tool`). `AgentLoop` acumula el transcript y reenvía los
    `functionCall` del modelo antes de los `functionResponse` → multi-tool correcto en Gemini.
    Guarda de rondas + `ActionDispatcher` con try/catch por herramienta (un fallo ya no tumba el turno).
  - `AiProvider.chat`: se quitó `toolResults`, se añadió `onTextDelta(textoAcumulado)`.
  - Streaming: `GeminiApi.streamGenerateContent` (`@Streaming`, `alt=sse`); `GeminiProvider` parsea
    líneas `data:` y emite texto acumulado. UI: `ChatUiState.streamingText` + burbuja viva en `ChatScreen`.
  - ✅ CI verde: commit `3d57d77`, run `26862773391`, artifact `app-debug` (~17.65 MB).
- **2026-06-03** — Sesión de emulador + agente completo.
  - Emulador local configurado (Mac, `Pixel_3a_API_34`, `local.properties`, `~/.zshrc`).
  - `NuevoSOAccessibilityService`: 5 tools de control de pantalla (read/tap/type/scroll/back).
  - `install_app`: flujo autónomo Play Store → tap Instalar → espera → abre app. Retry 429 con backoff.
  - `list_apps`: lista apps instaladas vía `AppRepository`.
  - System prompt reescrito: flujo obligatorio de navegación web (search→read→tap hasta completar).
  - Fixes: botón ← en Settings/Drawer; crash cajón (`distinctBy`); `GeminiSchema.type`; `GeminiFunctionDeclaration.parameters` nullable.
- **2026-06-03** — `TASK-RUNTIME-000` preparado en rama local `feat/sol-runtime-v0-security-foundation` sin commit/push.
  - Añadido paquete `agent.security`: `ActionRequest`, `ActionPolicy`, `PolicyDecision`,
    `ActionAuditEvent`, sanitizer, registry y `PolicyEngine`.
  - `ActionDispatcher` ahora convierte `ToolCall -> ActionRequest -> PolicyDecision`, audita y
    solo llama ejecutores cuando la decisión es `ALLOW`; `R2/R3` devuelven bloqueo seguro hasta
    `TASK-RUNTIME-001`.
  - Room sube a v2 con tabla `action_audit_events` y migración explícita `MIGRATION_1_2`; se quitó
    `fallbackToDestructiveMigration()`.
  - Backup cloud/device transfer desactivado/excluido; Gemini ya no instala `HttpLoggingInterceptor`.
  - Prompt actualizado: el modelo propone acciones, no tiene control total ni puede saltarse política.
  - Docs: README ampliado y `docs/SOL_OS_RUNTIME_V0_SECURITY_BASELINE.md`.
  - Tests nuevos para política, sanitizer, dispatcher, migración y config. `test` y `assembleDebug`
    pasan; `lint` conserva el fallo baseline preexistente.
- **2026-06-04** — `TASK-RUNTIME-000A` implementado en rama `fix/task-runtime-000a-security-hardening` sin commit/push.
  - Auditoría Room v3 append-only: `eventId` como PK, `actionId` indexado, lifecycle stages,
    `SafeFailureCode`, y migración `MIGRATION_2_3` desde filas v2 legacy.
  - `ActionDispatcher`: auditoría durable antes del executor; si falla, no ejecuta. La
    finalización fallida posterior a side effects no reintenta executor y devuelve incertidumbre segura.
  - Sanitizer fail-closed por allowlist de herramienta; unknown args/tools persisten solo claves y longitudes.
  - `toggle_setting(setting="flashlight", value="on|off")` reemplaza el toggle ambiguo en memoria.
  - Backup XML excluye root/file/database/sharedpref/external y dominios device_* en cloud/transfer.
  - Tests: dispatcher durability, sanitizer unknown args, flashlight declarativo, SQL guardrails y
    `MemoryMigrationInstrumentedTest` con `MigrationTestHelper`. `connectedDebugAndroidTest` queda pendiente
    si no hay emulator/device en `adb devices`.
- **2026-06-04** — `TASK-RUNTIME-001` implementado en rama `task/runtime-001-consent-lifecycle` sin commit/push.
  - Consent lifecycle local: `ActionDispatcher.dispatchForAgent` emite `PendingConfirmation`
    para R2/R3; `dispatch()` sigue siendo model-facing y no expone tokens.
  - Tokens in-memory UUID v4 con `actionId`, hash SHA-256 determinista de argumentos sanitizados,
    `riskLevel`, `issuedAtMillis`, `expiresAtMillis` (120s), single-use, expiry y replay protection.
  - `AgentLoop` pausa y guarda continuación local; `ChatViewModel` reanuda solo tras Aprobar,
    Rechazar o timeout. El token nunca entra al transcript del modelo.
  - UI Compose de confirmación muestra solo tool, riesgo y resumen sanitizado.
  - Auditoría append-only añade `CONFIRMATION_GRANTED`, `CONFIRMATION_REJECTED`,
    `CONFIRMATION_EXPIRED`; aprobaciones válidas aún pasan por pre-execution audit antes del executor.
  - Tests nuevos: dispatcher approval/reject/expiry/replay/pre-audit, store binding/hash/unknown token,
    y agent loop token-boundary. `./gradlew test` ✅.
- **2026-06-04** — Cierre operativo de la rama `task/runtime-001-consent-lifecycle`.
  - Alarmas relativas: `set_alarm` acepta `delay_minutes` para casos como "en 3 minutos"; el prompt
    instruye al modelo a no pedir hora exacta cuando el retraso relativo está claro.
  - Permiso de alarmas corregido a `com.android.alarm.permission.SET_ALARM`; el intent de Clock dejó
    de fallar por `Permission Denial`.
  - Calendario: añadida tool gobernada `create_calendar_event` para abrir Google Calendar con evento
    prellenado desde día/fecha, hora inicio y hora término; sigue siendo R2 y requiere confirmación.
  - La auditoría de calendario redacciona título, ubicación y descripción como metadata de longitud.
  - Tests unitarios de sanitizer/policy/cálculo temporal pasan; app debug reinstalada en emulador.
- **2026-06-04** — Objetivo de almacenamiento seguro de credenciales implementado localmente sin commit/push.
  - `TASK-RUNTIME-001` documentado como cerrado/revisado/mergeado en `main` (`3d447bd`).
  - API keys salen de `AppSettings`/DataStore como fuente primaria; `SettingsRepository` expone solo
    `hasApiKey` y migra una key legacy desde DataStore solo si la escritura cifrada tiene éxito.
  - Añadido `CredentialRepository` con backend `AndroidKeystoreCredentialRepository`
    (`EncryptedSharedPreferences` + `MasterKey`) y failure codes controlados.
  - `ProviderFactory` lee la key desde la fuente segura y falla cerrado si falta la key, falla
    secure storage, o el provider configurado no está implementado.
  - Settings UI trata la API key como write-only: permite guardar/limpiar, no precarga la clave guardada.
  - Unit tests pasan; `assembleAndroidTest` compila. `connectedDebugAndroidTest` falló por
    `No compatible devices connected` tras timeout de propiedades en `emulator-5554`.
