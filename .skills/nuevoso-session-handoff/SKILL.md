---
name: nuevoso-session-handoff
description: >
  Preserva y restaura contexto operativo del proyecto nuevoSO / Sol OS Runtime v0
  entre sesiones de cualquier agente de desarrollo de software.

  USAR ESTA SKILL cuando:
  - El usuario pida explícitamente guardar contexto, cerrar sesión, hacer handoff,
    resetear contexto o continuar nuevoSO / Sol OS Runtime desde una sesión limpia.
  - Un agente detecte degradación de eficiencia por contexto largo, múltiples archivos,
    muchas iteraciones o una sesión prolongada: en ese caso RECOMENDAR el handoff y
    esperar confirmación explícita antes de generarlo.
  - Un nuevo agente comience a trabajar en el repositorio y necesite restaurar el
    estado real del proyecto antes de proponer cambios.
  - Se cambie de agente, modelo, herramienta o entorno de ejecución.

  ESTA SKILL ES AGNÓSTICA AL AGENTE.
  Debe funcionar con cualquier agente capaz de leer y escribir archivos del repositorio.

  NO ejecutar automáticamente solo porque la conversación sea larga.
  NO commitear ni hacer push sin autorización explícita del usuario.
---

# nuevoSO Session Handoff

Skill agnóstica para preservar continuidad entre agentes que escriben código en:

```text
nuevoSO
Sol Launcher Lab
Sol OS Runtime v0
```

La memoria debe permitir que otro agente —o el mismo agente después de limpiar contexto—
retome el trabajo sin depender de la conversación anterior.

No asumir una herramienta concreta.
No depender de comandos exclusivos de Claude Code, Codex, OpenCode ni otro proveedor.

---

## Principio central

El contexto operativo debe vivir en archivos del repositorio, no únicamente en la memoria
temporal del agente.

Usar dos niveles:

```text
SESSION_MEMORY.md
→ memoria canónica y acumulativa del proyecto

docs/session-handoffs/
→ snapshots autocontenidos por rama, feature o versión
```

`SESSION_MEMORY.md` resume el estado vigente.

Cada handoff crea además un snapshot inmutable asociado a la rama activa:

```text
docs/session-handoffs/{branch-slug}/
└── nuevoso_{branch-slug}_session-{NNN}_YYYY-MM-DD_HH-MM.md
```

Ejemplos:

```text
docs/session-handoffs/feat-sol-runtime-v0-security-foundation/
└── nuevoso_feat-sol-runtime-v0-security-foundation_session-001_2026-06-04_10-30.md

docs/session-handoffs/fix-task-runtime-000a-security-hardening/
└── nuevoso_fix-task-runtime-000a-security-hardening_session-001_2026-06-04_18-10.md

docs/session-handoffs/release-sol-runtime-v0/
└── nuevoso_release-sol-runtime-v0_session-003_2026-06-10_09-45.md
```

No incluir secretos, credenciales ni datos personales reales.

---

# Compatibilidad con agentes

Esta skill debe ser utilizable por cualquier agente de código con acceso al repositorio.

Ejemplos:

```text
Claude Code
Codex
OpenCode
Cursor
Windsurf
Cline
Aider
Copilot coding agents
agentes internos
agentes futuros
```

La lógica central siempre es la misma:

```text
leer memoria
→ verificar repositorio real
→ trabajar
→ generar snapshot
→ actualizar memoria canónica
→ entregar prompt de continuación
→ reiniciar manualmente el contexto
```

Cuando una herramienta no permita ejecutar comandos, crear sesiones o guardar archivos
automáticamente, el agente debe informar la limitación y entregar instrucciones manuales.

---

# Convención de rama obligatoria

Antes de crear un snapshot, leer la rama activa:

```bash
git branch --show-current
```

La rama debe estar asociada a un feature, fix, task, release o versión.

Convenciones preferidas:

```text
feat/<feature-name>
fix/<task-or-bug-name>
task/<task-id>-<short-name>
release/<version-or-milestone>
chore/<maintenance-name>
```

Ejemplos válidos:

```text
feat/sol-runtime-v0-security-foundation
fix/task-runtime-000a-security-hardening
task/task-runtime-001-consent-lifecycle
release/sol-runtime-v0
```

Si la rama activa es genérica o no expresa el objetivo, por ejemplo:

```text
main
master
develop
test
temp
```

no generar el snapshot todavía.

Primero:
1. informar al usuario;
2. proponer una rama asociada al trabajo actual;
3. esperar confirmación;
4. crear o cambiar a la rama aprobada;
5. recién entonces generar el handoff.

---

# Normalización de nombre de rama

Convertir la rama activa en `{branch-slug}`.

Reglas:

```text
- pasar a minúsculas
- reemplazar "/" por "-"
- reemplazar espacios y "_" por "-"
- eliminar caracteres no alfanuméricos salvo "-"
- colapsar guiones repetidos
- quitar guiones al inicio y al final
- limitar a 80 caracteres
```

Ejemplo:

```text
fix/TASK-RUNTIME-000A_security-hardening
→ fix-task-runtime-000a-security-hardening
```

---

# Modo A — Restaurar contexto al comenzar una sesión

Usar este modo cuando el usuario diga:

```text
continúa con nuevoSO
retoma Sol OS Runtime
sigue con el launcher
carga la memoria
abre una nueva sesión con el handoff
```

## Paso A1 — Leer instrucciones permanentes

Leer primero, si existen:

```text
AGENTS.md
SESSION_MEMORY.md
```

Después leer la rama activa:

```bash
git branch --show-current
```

Normalizarla como `{branch-slug}` y localizar el snapshot más reciente de esa rama:

```bash
find "docs/session-handoffs/{branch-slug}" \
  -type f \
  -name "nuevoso_{branch-slug}_session-*.md" \
  2>/dev/null | sort | tail -n 1
```

Si no existe un snapshot para la rama actual, buscar el más reciente global como respaldo:

```bash
find docs/session-handoffs \
  -type f \
  -name 'nuevoso_*_session-*.md' \
  2>/dev/null | sort | tail -n 1
```

Leer primero el snapshot de la rama actual.
Usar el snapshot global solo como referencia histórica.

Orden de autoridad:

```text
1. AGENTS.md
2. estado real de git y del repositorio
3. SESSION_MEMORY.md
4. snapshot más reciente de la rama activa
5. snapshot global más reciente, solo como referencia
6. conversación actual
```

Si hay contradicciones, verificar con hechos y documentarlas.

## Paso A2 — Verificar estado real

Ejecutar:

```bash
git status --short
git branch --show-current
git log --oneline --decorate -n 8
```

Cuando sea útil, revisar:

```bash
git diff --stat
git diff --check
```

No asumir que la memoria refleja el último estado.

## Paso A3 — Verificar validación disponible

Preferir:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleAndroidTest
```

Cuando exista emulador:

```bash
./gradlew connectedDebugAndroidTest
```

Si el entorno no permite ejecutar Android SDK o emulador, informar la limitación y revisar
CI remoto cuando esté disponible.

No afirmar que algo compila o pasa pruebas sin evidencia.

## Paso A4 — Resumir antes de actuar

Antes de modificar código, entregar un resumen breve:

```text
- objetivo actual
- rama actual
- branch-slug
- último commit
- cambios sin commit
- snapshot más reciente de la rama
- validaciones conocidas
- próximo paso recomendado
- riesgos abiertos
```

Si el usuario ya pidió una tarea concreta, continuar sin abrir nuevos objetivos.

---

# Modo B — Recomendar handoff

Recomendar handoff, pero no ejecutarlo automáticamente, cuando exista alguna señal:

```text
- contexto estimado cercano a 70–80%
- muchas lecturas o ediciones de archivos grandes
- sesión prolongada
- respuestas más cortas o pérdida de detalles previos
- múltiples tareas mezcladas
- cambio de agente, modelo, herramienta o entorno
- usuario quiere resetear desde cero sin perder continuidad
```

Mensaje recomendado:

> El contexto de esta sesión ya está bastante cargado. Conviene generar un
> **nuevoSO session handoff** asociado a la rama activa para continuar en limpio
> sin perder decisiones, cambios ni próximos pasos. ¿Lo preparo?

Esperar confirmación explícita.

---

# Modo C — Generar handoff

Ejecutar directamente solo cuando:

```text
- el usuario lo pida explícitamente
- el usuario confirme después de una recomendación
```

## Paso C1 — Inspeccionar el estado antes de escribir memoria

Ejecutar:

```bash
git status --short
git branch --show-current
git log --oneline --decorate -n 8
git diff --stat
git diff --check
```

Si corresponde, ejecutar validaciones:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleAndroidTest
```

Con emulador disponible:

```bash
./gradlew connectedDebugAndroidTest
```

Registrar resultados exactos. No ocultar fallos.

## Paso C2 — Validar rama y construir branch slug

Leer:

```bash
BRANCH="$(git branch --show-current)"
```

Si `BRANCH` está vacío o es una rama genérica como `main`, `master`, `develop`, `test` o `temp`,
detenerse y solicitar una rama asociada a feature, fix, task, release o versión.

Normalizar:

```bash
BRANCH_SLUG="$(
  printf '%s' "$BRANCH" \
  | tr '[:upper:]' '[:lower:]' \
  | sed 's#[/_[:space:]]#-#g' \
  | sed 's/[^a-z0-9-]//g' \
  | sed 's/-\{2,\}/-/g' \
  | sed 's/^-//' \
  | sed 's/-$//' \
  | cut -c1-80
)"
```

Crear carpeta específica de rama:

```bash
mkdir -p "docs/session-handoffs/$BRANCH_SLUG"
```

## Paso C3 — Determinar número de sesión por rama

Contar snapshots previos solo dentro de la rama actual:

```bash
find "docs/session-handoffs/$BRANCH_SLUG" \
  -type f \
  -name "nuevoso_${BRANCH_SLUG}_session-*.md" \
  | wc -l
```

El nuevo número es:

```text
cantidad existente para esa rama + 1
```

Usar padding de tres dígitos:

```text
001
002
003
```

Cada rama comienza su propia secuencia en `001`.

## Paso C4 — Generar snapshot autocontenido

Nombre:

```text
docs/session-handoffs/{branch-slug}/
nuevoso_{branch-slug}_session-{NNN}_YYYY-MM-DD_HH-MM.md
```

Usar hora local del entorno.

El snapshot debe incluir obligatoriamente:

```text
1. Identidad del proyecto
2. Rama activa, branch-slug y objetivo asociado
3. Objetivo de la sesión
4. Estado real de git
5. Trabajo completado
6. Trabajo en progreso
7. Cambios sin commit
8. Decisiones tomadas y razones
9. Riesgos abiertos
10. Problemas encontrados y soluciones
11. Archivos creados y modificados
12. Validaciones ejecutadas y resultados exactos
13. Limitaciones del entorno
14. Próximos pasos ordenados
15. Paso inmediato para el siguiente agente
16. Prompt de continuación listo para pegar
```

Usar la plantilla en:

```text
references/memory-template.md
```

## Paso C5 — Actualizar `SESSION_MEMORY.md`

Actualizar la memoria canónica del proyecto:

```text
SESSION_MEMORY.md
```

Como mínimo:

```text
- estado actual
- rama actual
- branch-slug
- feature, fix, task o versión asociada
- último commit conocido
- cambios sin commit
- validaciones exactas
- backlog actualizado
- riesgos abiertos
- entrada nueva en bitácora
- referencia al snapshot recién creado
```

No reemplazar información histórica útil por un resumen pobre.

No escribir afirmaciones no verificadas.

## Paso C6 — Aplicar reglas de seguridad

Nunca persistir dentro del snapshot ni de `SESSION_MEMORY.md`:

```text
- API keys
- tokens
- contraseñas
- credenciales
- rutas locales privadas innecesarias
- datos personales reales
- contenido completo de pantallas privadas
- dumps de Room
- logs con secretos
- prompts con payloads privados
```

Usar datos sintéticos y descripciones sanitizadas.

## Paso C7 — No commitear automáticamente

Después de generar o actualizar memoria:

```bash
git status --short
git diff --stat
git diff --check
```

Informar al usuario qué archivos quedaron modificados.

No hacer:

```text
git commit
git push
git reset
git clean
```

salvo autorización explícita del usuario.

---

# Modo D — Reiniciar contexto y continuar

La skill debe preparar el reinicio, pero no asumir que todas las herramientas pueden abrir una
sesión nueva ni ejecutar comandos de control de contexto.

## Flujo universal

1. Generar snapshot asociado a la rama activa.
2. Actualizar `SESSION_MEMORY.md`.
3. Mostrar el bloque `## Prompt de continuación`.
4. Indicar al usuario abrir una sesión limpia, thread nuevo o contexto nuevo con su herramienta.
5. Pegar el prompt de continuación como primer mensaje.
6. Pedir al nuevo agente verificar repositorio antes de editar.

## Adaptación según capacidades

Si la herramienta ofrece un comando para limpiar contexto:

```text
- informar el comando disponible
- no ejecutarlo sin confirmación si puede interrumpir trabajo
```

Si la herramienta no ofrece limpieza automática:

```text
- indicar abrir una nueva sesión manualmente
```

Si el agente no tiene acceso de escritura al repositorio:

```text
- generar el snapshot como archivo descargable
- entregar el prompt de continuación
- indicar copiar el archivo al repositorio manualmente
```

Si el agente no puede ejecutar Git:

```text
- solicitar al usuario ejecutar los comandos
- registrar que el estado no fue verificado directamente
```

No inventar capacidades de la herramienta.

---

# Prompt de continuación obligatorio

Cada snapshot debe terminar con un bloque:

```markdown
## Prompt de continuación

Actúa como agente de implementación para nuevoSO / Sol OS Runtime v0.

Antes de proponer cambios:
1. Lee `AGENTS.md`.
2. Lee `SESSION_MEMORY.md`.
3. Verifica la rama activa con:
   `git branch --show-current`
4. Lee el snapshot más reciente de la rama:
   `docs/session-handoffs/{branch-slug}/nuevoso_{branch-slug}_session-{NNN}_YYYY-MM-DD_HH-MM.md`
5. Ejecuta:
   - `git status --short`
   - `git branch --show-current`
   - `git log --oneline --decorate -n 8`
6. Resume el estado real, riesgos abiertos y próximo paso inmediato.
7. No modifiques código hasta confirmar que la memoria coincide con el repositorio.
8. Respeta las reglas de seguridad y los límites de alcance definidos en `AGENTS.md`.

Rama esperada:
`{BRANCH}`

Feature, fix, task o versión asociada:
`{WORKSTREAM}`

Objetivo inmediato:
`{OBJETIVO_INMEDIATO}`

No abras objetivos nuevos sin autorización.
No hagas commit ni push sin autorización explícita.
```

Completar `{branch-slug}`, `{NNN}`, fecha, hora, `{BRANCH}`, `{WORKSTREAM}` y
`{OBJETIVO_INMEDIATO}` con el estado real.

---

# Reglas específicas de Sol OS Runtime v0

Mantener siempre estas reglas:

```text
- El modelo propone acciones; la política local decide.
- Seguridad por delante de conveniencia.
- Unknown tools fallan cerrado.
- No ejecutar acciones sensibles silenciosamente.
- No agregar bypass flags.
- No almacenar payloads privados en bruto.
- No almacenar mensajes de excepción sin sanitizar.
- No usar fallbackToDestructiveMigration().
- Mantener backup desactivado salvo diseño explícito de recuperación cifrada.
- Accesibilidad es fallback experimental gobernado, no autorización general.
- No afirmar que todos los datos permanecen en el dispositivo mientras exista inferencia remota.
- No afirmar que una validación pasó si no se ejecutó realmente.
```

---

# Relación con `nuevoso-continue`

Esta skill reemplaza el flujo anterior de `nuevoso-continue`.

`nuevoso-continue` solo restauraba contexto desde `SESSION_MEMORY.md`.

`nuevoso-session-handoff` agrega:

```text
- restauración
- recomendación preventiva
- snapshot autocontenido por rama
- vínculo explícito con feature, fix, task o versión
- actualización de memoria canónica
- prompt de continuación
- soporte agnóstico para cualquier agente
- reglas explícitas de seguridad
- reinicio manual controlado
```

---

# Criterios de calidad del handoff

El snapshot debe ser:

```text
Autocontenido
→ el siguiente agente puede retomar sin leer la conversación anterior

Trazable
→ el nombre del archivo identifica la rama y el workstream

Accionable
→ el siguiente paso es concreto

Fiel
→ no inventa estado ni validaciones

Seguro
→ no almacena secretos ni datos privados

Compacto
→ prioriza información operativa

Verificable
→ separa hechos, supuestos y limitaciones

Agnóstico
→ sirve para cualquier agente de desarrollo
```

---

# Archivos esperados

```text
SESSION_MEMORY.md

docs/session-handoffs/
└── {branch-slug}/
    └── nuevoso_{branch-slug}_session-{NNN}_YYYY-MM-DD_HH-MM.md
```

La skill puede conservarse en cualquier ubicación que el agente utilizado pueda cargar.

Ejemplos:

```text
.skills/nuevoso-session-handoff/SKILL.md
.claude/skills/nuevoso-session-handoff/SKILL.md
.codex/skills/nuevoso-session-handoff/SKILL.md
.opencode/skills/nuevoso-session-handoff/SKILL.md
```

No asumir una ruta única.
La ruta canónica recomendada dentro del repositorio es:

```text
.skills/nuevoso-session-handoff/
```
