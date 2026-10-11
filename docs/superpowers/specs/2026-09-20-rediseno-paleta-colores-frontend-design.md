# Diseño: Rediseño de paleta de colores y design tokens del frontend

Fecha: 2026-09-20
Estado: propuesto

## Contexto

El frontend (`frontend/`) es hoy un scaffold navegable sin sistema de diseño consolidado (ver `CLAUDE.md` raíz del repo). Estado actual encontrado al revisar el código:

- `apps/erp-web/src/styles.css` define tokens `@theme` (`--color-brand-50/700/950`, teal `#0f766e`) que **no se usan en ningún componente** — están huérfanos.
- `packages/ui-web/src/{button,card,badge}` (las únicas 3 primitivas compartidas) usan clases Tailwind hardcodeadas: `teal-700`, `slate-*`, `emerald/amber/rose-*`.
- Cada feature repite su propio `Modal`, `DataTable`, `EstadoBadge` con estilos casi idénticos en vez de reusar `ui-web`: confirmado en `features/catalogo/components/{Modal,DataTable,EstadoBadge}.tsx` y `features/seguridad/components/{Modal,DataTable,EstadoBadge}.tsx`.
- No existe mecanismo de dark mode.

El usuario pidió reajustar la UI/UX con una nueva paleta: **Primary `#2563EB`, Secondary `#06B6D4`, Tertiary `#10B981`, Neutral `#0F172A`**.

## Alcance

Rediseño completo en esta iteración:

1. Nuevos design tokens semánticos en Tailwind 4 (`@theme`), con soporte claro/oscuro.
2. Mecanismo de dark mode: preferencia del SO por defecto + toggle manual persistido.
3. Actualización de `packages/ui-web` (`Button`, `Card`, `Badge`) a los nuevos tokens.
4. Unificación de `Modal`, `DataTable`, `EstadoBadge` duplicados entre `catalogo` y `seguridad` en `ui-web`, migrados a los nuevos tokens.
5. Migración de clases hardcodeadas a tokens en todas las pantallas existentes: `auth` (login), `dashboard`, `seguridad` (usuarios/roles/permisos), `catalogo` (categorías/marcas), y los placeholders de `compras`, `ventas`, `pos`, `caja`, `clientes`, `inventario`, `organizacion`.

Fuera de alcance: cambios de layout/estructura de información, nueva iconografía, animaciones/transiciones nuevas, rediseño de la navegación (sidebar/header) más allá de aplicar los tokens y ubicar el `ThemeToggle`.

## Mapeo semántico de colores

| Token | Base | Uso |
|---|---|---|
| `primary` | `#2563EB` | Acciones principales (botones primary, foco, marca, nav activa) |
| `secondary` | `#06B6D4` | Acentos informativos, elementos interactivos secundarios, gráficos |
| `success` | `#10B981` (era "Tertiary") | Únicamente estados positivos/OK (badges, confirmaciones) — reservado, no decorativo |
| `neutral` | `#0F172A` | Texto, bordes, fondos — reemplaza `slate-*` hardcodeado |
| `warning` | Amber estándar Tailwind (`amber-500/600`) | Advertencias |
| `danger` | Red estándar Tailwind (`red-500/600`) | Errores, acciones destructivas |

Cada token se define como escala 50→950 (generada a partir del valor base dado, ubicado en el peldaño 500 o 600 según corresponda al uso típico de Tailwind), no como un solo valor plano, para permitir variantes hover/active/disabled/fondos-suaves sin salirse de la paleta.

## Dark mode

- Activación: `<html data-theme="light|dark">`. Al cargar, un script inline (antes de pintar, para evitar flash) lee `localStorage.theme`; si no existe, usa `window.matchMedia('(prefers-color-scheme: dark)')`.
- El usuario puede forzar el modo con un `ThemeToggle` (nuevo componente en `ui-web`, ubicado en el header/nav del layout del ERP), que escribe la elección en `localStorage.theme` y actualiza el atributo `data-theme`.
- Los tokens críticos (fondo de página, fondo de superficie/card, texto primario/secundario, bordes) tienen valores distintos bajo `[data-theme="dark"]`, definidos junto a los tokens claros en `@theme` / bloque `@layer base`.
- Los tokens de marca (`primary`/`secondary`/`success`/`warning`/`danger`) mantienen su hue en ambos modos, ajustando solo el peldaño de la escala usado (más claro/saturado sobre fondo oscuro) donde el contraste WCAG AA lo requiera.

## Componentes afectados en `packages/ui-web`

- `Button`: variantes `primary`/`secondary`/`ghost` migradas a tokens; se evalúa si conviene una variante `danger` explícita dado que ahora hay acciones destructivas (revocar, desactivar) en varias pantallas de `seguridad`.
- `Card`: fondo/borde a tokens `neutral`, con variante dark.
- `Badge`: tonos `neutral/success/warning/danger` a tokens (el `success` ahora es consistente con el token semántico global, no un verde ad-hoc).
- `Modal` (nuevo en `ui-web`, absorbe `catalogo/components/Modal.tsx` y `seguridad/components/Modal.tsx`): misma API pública que las dos versiones actuales lo permitan sin romper llamadas existentes; se ajustan los imports en ambas features.
- `DataTable` (nuevo en `ui-web`, absorbe ambas versiones): idem.
- `EstadoBadge` (nuevo en `ui-web`, absorbe ambas versiones): idem — probablemente se apoya en `Badge` con tonos ya migrados.
- `ThemeToggle` (nuevo): botón/switch de 2 o 3 estados (claro/oscuro, o claro/oscuro/sistema) con ícono, usa `localStorage` + `data-theme`.

Antes de unificar `Modal`/`DataTable`/`EstadoBadge`, se comparan ambas implementaciones (`catalogo` vs `seguridad`) para confirmar que su API (props) es compatible o casi compatible; si hay divergencias reales de comportamiento (no solo de estilo), la versión unificada soporta ambos casos vía props opcionales, sin perder funcionalidad de ninguna de las dos.

## Migración de features

Reemplazo mecánico de clases de color hardcodeadas (`teal-*`, `slate-*`, `blue-*`, `emerald/amber/rose-*` sueltos) por las clases generadas por los nuevos tokens (`bg-primary-600`, `text-neutral-700`, etc.) en cada archivo `.tsx` de:

- `features/auth/*` (LoginPage, LoginForm)
- `features/dashboard/*`
- `features/seguridad/*` (todas las páginas y componentes, incluyendo los que se migran a `ui-web`)
- `features/catalogo/*` (ídem)
- `features/compras`, `ventas`, `pos`, `caja`, `clientes`, `inventario`, `organizacion` (placeholders — se aplican los tokens a lo poco que ya tienen renderizado, sin agregar funcionalidad)

No se reescribe estructura JSX ni lógica de estas pantallas — solo clases de color/superficie. Cambios de spacing/tipografía no entran salvo que sean necesarios para legibilidad con la nueva paleta (p. ej. contraste de texto sobre un fondo de color).

## Verificación

- `pnpm check` (lint + typecheck + test + build) debe pasar.
- `pnpm test` para los tests existentes de componentes migrados (`Button.test.tsx`, `EstadoBadge.test.tsx`), ajustados si aseveran clases CSS específicas que cambian de nombre.
- Revisión visual manual con `pnpm dev`: login, dashboard, una pantalla de listado y una de detalle en `seguridad`, una pantalla de `catalogo`, en modo claro y oscuro, y toggle funcionando.
- Contraste de texto/fondo verificado a ojo contra WCAG AA para las combinaciones más usadas (texto sobre `primary`, texto sobre `neutral` oscuro).

## Riesgos / decisiones abiertas para el plan de implementación

- Definir la escala 50→950 exacta de cada color (herramienta/método de generación) es una decisión técnica que se resuelve en el plan de implementación, no en este spec.
- Si `Modal`/`DataTable`/`EstadoBadge` de `catalogo` y `seguridad` divergen más de lo esperado en props/comportamiento, el plan debe decidir si se unifican con una API superset o si se documenta por qué alguno queda sin unificar.
