# Diseño: Rediseño de la paleta de marca del frontend (verde-teal) + extracción de `Input`

Fecha: 2026-09-28
Estado: propuesto

## Contexto

Tras rediseñar el login siguiendo el prototipo de Figma "FarmaVita" (verde-teal, ver `docs/superpowers/specs/2026-09-28-login-rediseno-figma-design.md`), el usuario confirmó que prefiere ese color sobre el azul actual y pidió extender el cambio a **toda la identidad de marca del frontend**, no solo el login.

Se auditó el uso real de color en todo `frontend/` antes de diseñar este cambio (hallazgos completos en el historial de la sesión, resumen aquí):

- El sistema actual (`frontend/apps/erp-web/src/styles.css`) define 6 escalas Tailwind 4 (`@theme`) completas 50→950: `primary` (azul), `secondary` (cyan), `success` (emerald/verde), `neutral` (slate), `warning` (amber), `danger` (red).
- Un spec previo (`docs/superpowers/specs/2026-09-20-rediseno-paleta-colores-frontend-design.md`) que proponía una paleta similar **está obsoleto** — describe un estado del código (tokens huérfanos, componentes duplicados, sin dark mode) que ya no existe; se trata como referencia histórica, no como fuente de verdad.
- `packages/ui-web` (12 componentes: `Button`, `Card`, `Badge`, `EstadoBadge`, `Modal`, `DataTable`, `Pagination`, `PageHeader`, `ListFilters`, `IconButton`, `DropdownMenu`, `FormField`, más `ThemeToggle`/`useTheme`) está 100% basado en tokens, sin hardcodes, y es la base sobre la que se propaga cualquier cambio de paleta.
- Dark mode ya funciona (`data-theme` + `useTheme` + `ThemeToggle`), con cobertura `dark:` amplia; no se modifica en este spec.
- **Fugas de color fuera del sistema de tokens**, ambas en `LoginPage.tsx` (introducidas en el rediseño del login de esta misma sesión): `bg-[#f4f7f5]` (hex arbitrario) y un `linear-gradient` con hex crudos (`#0d9488`, `#065f46`, `#022c22`) de los cuales solo el último coincide exactamente con un escalón real de `success`.
- `success` ya no es exclusivo del login: `DashboardPage.tsx` también lo usa semánticamente (tarjeta "Clientes activos", badges de estado).
- `secondary` (cyan) tiene uso mínimo: solo 2 lugares (`LoginForm.tsx` banner de ayuda, `DashboardPage.tsx` 2 stat cards informativas).
- Duplicación de markup (no de color): el mismo bloque de clases de `<input>` (`focus:border-primary-600 focus:ring-primary-100...`) está copiado literalmente en 9+ archivos de formularios de `seguridad` y `catalogo`, en vez de usar un componente compartido. `FormField` (en `ui-web`) ya existe y envuelve label+error+hint, pero no el elemento `<input>` en sí.

## Alcance

### 1. Nueva paleta en `frontend/apps/erp-web/src/styles.css`

| Token | Antes | Después | Fuente |
|---|---|---|---|
| `primary` | azul (`#2563eb` en 600) | verde-teal, escala `teal` estándar de Tailwind | valores exactos abajo |
| `success` | emerald (`#059669` en 600) | verde-lima, escala `lime` estándar de Tailwind | valores exactos abajo |
| `secondary` | cyan (`#0891b2` en 600) | **eliminado** del `@theme` | — |
| `neutral`, `warning`, `danger` | sin cambios | sin cambios | — |

Valores exactos a usar (escala estándar Tailwind CSS v4, no inventados):

```
--color-primary-50: #f0fdfa;
--color-primary-100: #ccfbf1;
--color-primary-200: #99f6e4;
--color-primary-300: #5eead4;
--color-primary-400: #2dd4bf;
--color-primary-500: #14b8a6;
--color-primary-600: #0d9488;
--color-primary-700: #0f766e;
--color-primary-800: #115e59;
--color-primary-900: #134e4a;
--color-primary-950: #042f2e;

--color-success-50: #f7fee7;
--color-success-100: #ecfccb;
--color-success-200: #d9f99d;
--color-success-300: #bef264;
--color-success-400: #a3e635;
--color-success-500: #84cc16;
--color-success-600: #65a30d;
--color-success-700: #4d7c0f;
--color-success-800: #3f6212;
--color-success-900: #365314;
--color-success-950: #1a2e05;
```

El bloque `--color-secondary-*` (11 líneas) se elimina de `@theme`.

### 2. Migración de los 2 usos de `secondary` → `neutral`

- `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`: el banner de ayuda de "¿Olvidaste tu contraseña?" (`border-secondary-100 bg-secondary-50 text-secondary-900 ...` y el ícono `CircleHelp` con `text-secondary-600`) pasa a `neutral` (ej. `border-neutral-200 bg-neutral-50 text-neutral-700`, icono `text-neutral-500`, con sus pares `dark:`).
- `frontend/apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx`: las 2 stat cards que usan `secondary-*` ("Unidades en stock", "Comprobantes pendientes") migran a `neutral`.

### 3. Corrección de fugas de color en `LoginPage.tsx`

- `bg-[#f4f7f5]` (fondo de `<main>`) → `bg-neutral-50` (token real más cercano al hex usado).
- El `linear-gradient` del panel izquierdo (hoy `#0d9488 0%, #065f46 55%, #022c22 100%`) se reescribe usando los valores CSS variable reales de la nueva escala `primary`: `var(--color-primary-600) 0%, var(--color-primary-800) 55%, var(--color-primary-950) 100%`. Esto además simplifica el mantenimiento: si la paleta cambia en el futuro, el gradiente se actualiza solo.
- El botón mostrar/ocultar contraseña en `LoginForm.tsx` tiene un residual `focus-visible:outline-primary-600` que hoy desentona (todo el resto del formulario usa `success-*` para foco). Con `primary` ahora también verde (teal) y `success` verde (lime), ambos son "verdes" pero de matices distintos — se decide en el plan de implementación si se dejan así (ambos son válidos visualmente) o se unifica a `success` por consistencia interna del archivo.

### 4. Nuevo componente `Input` en `packages/ui-web`

- Cubre el caso de `<input>` de texto simple con las clases de foco/borde ya usadas hoy (tokenizadas a `primary`), para usarse dentro de `FormField` (que ya existe y no se modifica en su contrato).
- Soporta una prop opcional de ícono a la izquierda (para los casos con `Mail`/`LockKeyhole`/etc. de `lucide-react`), sin acoplarse a un ícono específico.
- No se acopla a react-hook-form: recibe props estándar de `<input>` (incluyendo `ref` para que `register(...)` de react-hook-form siga funcionando vía forwardRef o el patrón de props-with-ref que ya usa `Button` en el repo).
- Se usa dentro de `FormField` en los formularios que hoy duplican el bloque de clases: `MarcaForm`, `CategoriaForm`, `RubroComercialForm`, `SupportCatalogForm`, `AsignacionRolForm`, `CredencialLocalForm`, `IdentidadExternaForm`, `RolForm`, `UsuarioForm`. El `<textarea>` de `MarcaForm` (y cualquier otro campo de texto largo) queda fuera de este spec — solo se migra el elemento `<input>`, no `<textarea>`.
- **`<select>` explícitamente fuera de alcance**: 4 de los 9 archivos (`AsignacionRolForm` con 5 selects, `IdentidadExternaForm` con 1, `RolForm` con 1, `UsuarioForm` con 1 — 8 en total) repiten la misma clase larga también en elementos `<select>`, no solo `<input>`. Se decidió no crear un componente `Select` en este trabajo — los `<select>` mantienen su clase repetida tal cual, sin extraer, como una mejora futura separada.
- `LoginForm.tsx` (campos de correo/contraseña con ícono + toggle de mostrar/ocultar) se evalúa en el plan de implementación si migra a `Input` o se deja como está — su patrón es más específico (ícono + botón de toggle superpuesto) y fue recién rediseñado; no es obligatorio migrarlo para cumplir este spec, pero se prefiere si no complica el componente.

### Fuera de alcance

- Cambiar `warning`, `danger` o `neutral`.
- Modificar el mecanismo de dark mode (`useTheme`, `ThemeToggle`, `data-theme`) — solo se actualizan los valores hex que ya tienen su contraparte `dark:` existente.
- Migrar `<textarea>` u otros controles de formulario a un componente compartido — solo `<input>`.
- Cualquier otro componente de `ui-web` más allá de `Input`.
- Actualizar el spec obsoleto `2026-09-20-rediseno-paleta-colores-frontend-design.md` — queda como está, solo se documenta aquí que no representa el estado actual.
- **`LoginPage.tsx` no se modifica en absoluto** (decisión explícita del usuario durante la implementación: "el login ya estuvo bien, no lo cambies"). Los hex hardcodeados del degradado (`#0d9488`, `#065f46`, `#022c22`) y del fondo (`#f4f7f5`) se dejan tal cual, aunque ya coinciden visualmente con el nuevo `primary`/`neutral` — la limpieza de esa duplicación queda pendiente para cuando el usuario lo pida.

## Verificación

- Actualizar `packages/ui-web/src/button/Button.test.tsx` y `packages/ui-web/src/estado-badge/EstadoBadge.test.tsx` (y cualquier otro test que asevere nombres de clase `primary-*`/`success-*`/`secondary-*` literales) para reflejar los nuevos valores/ausencia de `secondary`.
- Nuevo test para el componente `Input` (cobertura 100% por ser archivo nuevo, según la regla de `CLAUDE.md`).
- `pnpm check` (lint + typecheck + test + build) sin errores nuevos.
- Revisión visual manual con `pnpm dev`: login, dashboard, `AppShell`/sidebar, una pantalla de `seguridad` (con formulario) y una de `catalogo` (con formulario), en modo claro y oscuro.
- Confirmar visualmente que `primary` (teal) y `success` (lime) son distinguibles entre sí en los lugares donde ambos podrían aparecer juntos (ej. un botón primary junto a un badge de estado "Activo").

## Riesgos / decisiones abiertas para el plan de implementación

- Si migrar o no `LoginForm.tsx` al nuevo componente `Input` (ver sección 4) — decisión de implementación, no bloqueante para el resto del spec.
- Si el residual `focus-visible:outline-primary-600` del botón de mostrar/ocultar contraseña se deja o se cambia a `success` — cosmético, no afecta la paleta global.
- El componente `Input` nuevo requiere decidir su firma exacta de props (`icon?: LucideIcon`, manejo de `aria-invalid`, etc.) — se resuelve en el plan con el detalle de código completo, tomando como referencia el patrón ya usado en `MarcaForm.tsx` y los formularios de `seguridad`.
