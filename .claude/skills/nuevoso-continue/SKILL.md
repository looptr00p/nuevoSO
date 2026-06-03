---
name: nuevoso-continue
description: Resume work on the nuevoSO project (AI-first native Android launcher APK). Use this at the start of any new session when the user wants to continue building, fixing, or extending nuevoSO, or says things like "continúa con nuevoSO", "retoma el launcher", "sigue con el SO IA". Loads the persisted session memory so context is not lost between ephemeral sessions.
---

# Continuar nuevoSO

Esta skill restaura el contexto del proyecto **nuevoSO** (launcher Android IA nativo) para
continuar el trabajo en una sesión nueva sin perder las decisiones ya tomadas.

## Paso 1 — Cargar la memoria de sesión (obligatorio)

Lee **`SESSION_MEMORY.md`** en la raíz del repo. Es la fuente de verdad: arquitectura,
estado actual, stack/versiones, mapa del código, gotchas resueltos, operativa de git y el
backlog de próximos pasos. NO empieces a trabajar sin haberlo leído.

Si el archivo no existe (repo recién clonado en otra ruta), búscalo:
`find . -name SESSION_MEMORY.md`.

## Paso 2 — Verificar el estado real

Antes de proponer cambios, confirma dónde está el proyecto:

- `git log --oneline -5` — últimos commits.
- `git status` — cambios sin commitear.
- Si hace falta saber si compila: el APK **no se puede compilar en el contenedor** (SDK
  bloqueado). La verificación es **CI verde en GitHub Actions**. Para revisar el último run:
  `gh run list --limit 3` (o la API de Actions con el token disponible).

## Paso 3 — Continuar el trabajo

1. Revisa la sección **"Próximos pasos sugeridos"** de `SESSION_MEMORY.md`.
2. Pregunta al usuario qué quiere abordar (o retoma lo que pidió explícitamente).
3. Respeta los **gotchas críticos** de la memoria — son errores ya resueltos, no los repitas:
   - `gradle-wrapper.jar` commiteado; `ExposedDropdownMenu` sin import; `FLAG_ACTIVITY_NEW_TASK`
     al lanzar apps; API key nunca en el repo; etc.
4. Para git en este entorno: commitea con `git -c commit.gpgsign=false` (la firma está rota;
   los commits salen Unverified, es esperado). Push con `git push -u origin main` y reintentos
   con backoff solo si falla por red. **No crees PRs** salvo que el usuario lo pida.

## Paso 4 — Cerrar la sesión

Al terminar trabajo significativo, **actualiza `SESSION_MEMORY.md`**:
- Añade una entrada con fecha a la sección **Bitácora**.
- Actualiza **"Estado actual"** (último commit/run/artifact) y marca/añade ítems del backlog.
- Commitea la memoria junto con los cambios para que la próxima sesión la herede.

## Principios

- La memoria vive en el repo a propósito: los contenedores son efímeros, git es la persistencia.
- No re-litigues decisiones de arquitectura ya cerradas (ver sección 1 de la memoria) sin
  consultarlo con el usuario.
- Verifica con hechos (CI verde, artifact generado), no asumas que algo compila.
