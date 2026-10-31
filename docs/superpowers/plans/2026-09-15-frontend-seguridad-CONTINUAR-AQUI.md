# Continuar: integración frontend del módulo seguridad

Última actualización: 2026-09-15, sesión en curso (Claude Code).

## Cómo reanudar

1. Abrir una sesión de Claude Code en `C:\ambiente-dev\PROYECTOS-2026\erp-botica`.
2. Decir algo como: **"continuemos con el plan de seguridad frontend, revisa el ledger de progreso y sigue desde ahí"**.
3. Claude debe leer `.superpowers/sdd/progress.md` (ledger de progreso) y `git log --oneline` en la rama `feature/frontend-seguridad-integracion` para confirmar qué está realmente commiteado antes de continuar — **no confiar solo en este documento**, confirmar contra el estado real de git.

## Qué es esto

Plan de implementación (`docs/superpowers/plans/2026-09-15-frontend-seguridad-integracion.md`, 9 tareas, ~5300 líneas) que conecta el frontend (`frontend/`, pnpm workspace, `apps/erp-web`) con el backend `service-botica`/`security` ya verificado en esta misma sesión. Se está ejecutando con el flujo `superpowers:subagent-driven-development`: un subagente implementador por tarea, seguido de un subagente revisor (spec compliance + calidad de código), con checkpoints entre cada una.

Diseño previo (spec aprobada): `docs/superpowers/specs/2026-09-15-frontend-seguridad-integracion-design.md`.

## Rama de trabajo

`feature/frontend-seguridad-integracion` — creada desde `feature/catalogo-controllers-rest` (que ya está pusheada a `origin` con el trabajo de backend de `security`/`catalogo` completo). Esta rama nueva **no se ha pusheado todavía**.

## Estado de las 9 tareas

| # | Tarea | Estado | Commit(s) |
|---|---|---|---|
| 1 | Persistir `tenantId`/`userId` en la sesión | ✅ Completa, revisada, aprobada | `7241656` |
| 2 | Componentes UI compartidos (DataTable, Modal, FormField, EstadoBadge, ConfirmActionDialog) | ✅ Completa, revisada, aprobada (+ fix post-review) | `dd0700f`, fix en `2abea93` |
| 3 | Pantalla de Permisos (solo lectura) | ✅ Completa, revisada, aprobada | `01cb115` |
| 4 | Pantalla de Roles (alta, estado, reemplazo de permisos) | ✅ Completa, revisada, aprobada | `dd75fa4` |
| 5 | Pantalla de Usuarios (listado, búsqueda, alta) | 🔄 **En curso** — implementador despachado, esperando resultado al cerrar la sesión | — |
| 6 | Detalle de Usuario (roles, identidades externas, credencial local, estado) | ⬜ Pendiente | — |
| 7 | Pantallas de Sesiones y Dispositivos | ⬜ Pendiente | — |
| 8 | Infraestructura Playwright (desktop/tablet/mobile) + specs E2E | ⬜ Pendiente | — |
| 9 | Verificación final (`pnpm check` + Playwright completo) | ⬜ Pendiente | — |

**Ledger canónico** (fuente de verdad de qué está commiteado, más detalle que esta tabla): `.superpowers/sdd/progress.md`.

## Cómo continuar exactamente donde quedó

1. Verificar si Task 5 ya terminó y qué commit dejó:
   ```bash
   cd /c/ambiente-dev/PROYECTOS-2026/erp-botica
   git log --oneline -3
   cat .superpowers/sdd/task-5-report.md   # si existe, leer el resultado del implementador
   ```
2. **Si Task 5 ya está commiteada pero no revisada**: generar el paquete de revisión y despachar el revisor de tarea (mismo patrón que las 4 anteriores — ver la sección "Patrón usado por tarea" abajo).
3. **Si Task 5 no llegó a completarse** (el implementador no reportó, o quedó BLOCKED/NEEDS_CONTEXT): revisar `.superpowers/sdd/task-5-report.md` si existe, o simplemente re-despachar el implementador de Task 5 desde cero (el brief ya está en `.superpowers/sdd/task-5-brief.md`, no hace falta regenerarlo salvo que el plan haya cambiado).
4. Una vez Task 5 aprobada, seguir con Task 6, 7, 8, 9 en el mismo orden y patrón.

## Patrón usado por tarea (repetir para las tareas 6-9)

```bash
# 1. Extraer el brief de la tarea N
SDD_SCRIPTS="/c/Users/Mijael/.claude/plugins/cache/claude-plugins-official/superpowers/6.0.3/skills/subagent-driven-development/scripts"
"$SDD_SCRIPTS/task-brief" docs/superpowers/plans/2026-09-15-frontend-seguridad-integracion.md N

# 2. Registrar el commit base actual (antes de dispatchar)
git rev-parse HEAD

# 3. Dispatchar un subagente implementador (Agent tool, subagent_type: general-purpose)
#    - Modelo: sonnet para tareas con coordinación multi-archivo o creación de páginas;
#      haiku solo si la tarea es transcripción mecánica pura (ej. Task 2).
#    - Prompt: seguir la plantilla de
#      C:\Users\Mijael\.claude\plugins\cache\claude-plugins-official\superpowers\6.0.3\skills\subagent-driven-development\implementer-prompt.md
#    - Pasarle: ruta del brief, contexto de qué tareas anteriores ya están commiteadas
#      y qué interfaces/tipos exponen (ver "Interfaces" en el plan), los 4 gotchas de
#      toolchain ya descubiertos (ver abajo), y la ruta del report file a escribir.

# 4. Cuando el implementador reporte DONE (o DONE_WITH_CONCERNS -- leer el report primero):
"$SDD_SCRIPTS/review-package" <BASE_SHA_del_paso_2> HEAD
#    (imprime la ruta del .diff generado)

# 5. Dispatchar un subagente revisor (Agent tool, subagent_type: general-purpose, modelo sonnet)
#    - Prompt: seguir la plantilla de
#      C:\Users\Mijael\.claude\plugins\cache\claude-plugins-official\superpowers\6.0.3\skills\subagent-driven-development\task-reviewer-prompt.md
#    - Pasarle: ruta del brief, ruta del report del implementador, ruta del .diff,
#      y los Global Constraints relevantes a esa tarea (copiados del plan).

# 6. Si el revisor aprueba (Task quality: Approved) -> marcar completa en el ledger:
#    editar .superpowers/sdd/progress.md agregando una línea "Task N: complete (...)"

# 7. Si el revisor encuentra Critical/Important -> dispatchar un subagente de fix con
#    TODOS los hallazgos juntos (no uno por hallazgo), luego re-generar el review-package
#    y re-dispatchar el revisor hasta que apruebe.
```

## Gotchas de toolchain ya descubiertos (pasarlos proactivamente a cada implementador nuevo)

Estos 4 problemas aparecieron en tareas anteriores y probablemente reaparecerán en las tareas 6-9. Ya se documentaron en los dispatches de Task 4 y 5:

1. **Comando de test correcto**: `pnpm exec vitest run <path-relativo-a-frontend>` **desde `frontend/`** (la raíz del workspace pnpm) — NUNCA `pnpm --filter @boticas/erp-web exec vitest run <path>`, que ignora el `vitest.config.ts` raíz (con `globals: true` vía `projects`) y falla con `ReferenceError: beforeAll is not defined` incluso en código correcto. **Ya corregido en las 61 ocurrencias del archivo del plan** (`docs/superpowers/plans/2026-09-15-frontend-seguridad-integracion.md`), así que los briefs nuevos ya traen el comando correcto — no hace falta repetir esta corrección, solo tenerla presente si algo falla igual.
2. `tsconfig.base.json` tiene `exactOptionalPropertyTypes: true` — cualquier campo opcional que venga de zod `.optional()` (tipo `T | undefined`) debe tiparse explícitamente como `T | undefined` en el destino (props, payloads), no solo `T` con `?`.
3. `react-hook-form`'s `handleSubmit(callback)` invoca `callback(values, event)` — si el `onSubmit` de un formulario espera `(values: X) => void` y el test usa `toHaveBeenCalledWith(expect.objectContaining(...))`, hay que envolver como `handleSubmit((values) => onSubmit(values))`, no `handleSubmit(onSubmit)`, o la aserción falla por aridad exacta.
4. Cualquier página que renderice un `<Link>` de react-router necesita su test envuelto en `<MemoryRouter>` (o `createMemoryRouter`+`RouterProvider`), si no `render()` lanza una excepción sin contexto de router.
5. `@typescript-eslint/require-await` (vía `recommendedTypeChecked` en `eslint.config.js`) falla en handlers `async` sin ningún `await` adentro — no marcar handlers MSW como `async` si no usan `await`.

## Hallazgo importante ya corregido: `feature-routes.test.ts`

Task 4 descubrió que `frontend/apps/erp-web/src/app/feature-routes.test.ts` tenía una aserción desactualizada desde Task 3 (nunca se actualizó al agregar `seguridad/permisos`), lo cual solo se detecta corriendo `pnpm check` completo, no la suite acotada de `src/features/seguridad`. Ya está corregido (commit `dd75fa4`), pero es un recordatorio de que **cada implementador debe correr `pnpm check` completo antes de dar la tarea por terminada**, no solo la suite de la carpeta que tocó.

## Tipos canónicos ya establecidos (no redefinir en tareas futuras)

- `PaginaResponse<T>` — definido en `frontend/apps/erp-web/src/features/seguridad/api/roles.types.ts` (Task 4). Task 5 (Usuarios) ya lo importa desde ahí. Cualquier tarea futura que pagine debe importarlo del mismo lugar.
- `AuthSession` (con `tenantId`/`userId`) — `frontend/apps/erp-web/src/features/auth/model/auth-session.context.ts` (Task 1).
- Componentes de `features/seguridad/components/`: `DataTable`, `Modal`, `FormField`, `EstadoBadge`, `ConfirmActionDialog` (Task 2).

## Después de completar las 9 tareas

Según el flujo `subagent-driven-development`:
1. Dispatchar un revisor final de rama completa (`superpowers:requesting-code-review`'s `code-reviewer.md`), con el diff completo `merge-base main HEAD` (o contra la rama de origen real, `feature/catalogo-controllers-rest`, según corresponda).
2. Si aprueba, usar `superpowers:finishing-a-development-branch` para decidir merge/PR/limpieza.
3. Antes de cerrar, confirmar con el usuario si se hace push de `feature/frontend-seguridad-integracion` a `origin` (todavía no se ha hecho ningún push en esta rama).

## Notas de contexto adicionales

- El backend real (`service-botica`) debe estar corriendo (`docker compose up -d postgres` + `./gradlew.bat :bootstrap-app:bootRun`) para la Task 8 (Playwright E2E) — no es necesario para las Tasks 1-7, que usan MSW en los tests unitarios/integración.
- El módulo `app-botica/` es un prototipo visual aparte, **no** el frontend real — confirmado explícitamente por el usuario. No confundir.
- El módulo `catalogo` del frontend (pantallas CRUD equivalentes a las de seguridad) es la **siguiente spec**, aún no iniciada — se decidió hacer primero `seguridad` completo, y reutilizar la infraestructura Playwright (Task 8 de este plan) para `catalogo` después.
