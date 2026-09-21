# Diseño: Desplegable de perfil en el header

Fecha: 2026-09-20
Estado: propuesto

## Contexto

El botón de perfil en `AppShell.tsx` (esquina superior derecha del header, junto al `ThemeToggle` y el ícono de notificaciones) es hoy un `<button>` estático sin ninguna acción: muestra iniciales, nombre y rol **hardcodeados** ("MR", "María Rojas", "Administradora") y no está conectado a `useAuthSession()`. El usuario pidió agregarle un desplegable con: Mi Perfil, Configuraciones, Cerrar sesión, entre otros.

Investigación previa (relevante para el alcance):

- `useAuthSession()` (`features/auth/model/AuthSessionProvider.tsx`) expone `signOut()` real — llama al backend (`logout`) y limpia la sesión local (refresh token) — pero **no expone nombre ni correo del usuario**, solo `userId` (un ID crudo), `tenantId`, `accessToken`.
- No existe ningún endpoint backend (`service-botica/modules/security`) para "mi propio perfil" (`/me`, `/perfil`) ni un `GET /usuarios/{id}` genérico. El único listado de usuarios (`UsuarioController`) requiere el permiso admin `seguridad.usuarios.consultar`, que un usuario no-admin normalmente no tiene.
- Por lo tanto, el nombre/avatar/rol que se muestran en el header y en el desplegable **siguen siendo los valores hardcodeados actuales** — este trabajo no agrega la capacidad de traer el perfil real; eso queda fuera de alcance (requeriría un endpoint backend nuevo).
- El proyecto no tiene ninguna librería headless de menús/popover instalada. El `Modal` existente (`packages/ui-web/src/modal/Modal.tsx`) está hecho a mano, sin manejo de foco/Escape.

## Alcance

1. Nuevo componente `DropdownMenu` en `packages/ui-web`, construido sobre el paquete unificado `radix-ui` (específicamente su primitivo `DropdownMenu`), reemplazando el `<button>` estático del perfil en `AppShell.tsx`.
2. Contenido del menú (agrupado por secciones, con encabezado de marca):
   - **Encabezado**: fondo `bg-primary-600`, avatar circular con iniciales, nombre, rol (mismos datos hardcodeados de hoy — sin cambio de fuente de datos).
   - **Grupo "Cuenta"**: Mi Perfil, Seguridad de la cuenta.
   - **Grupo "Preferencias"**: Configuraciones, Cambiar sucursal.
   - Separador.
   - Ayuda y soporte.
   - Separador.
   - Cerrar sesión (tono `danger`, acción real).
3. Nueva feature `features/perfil/` con 4 rutas placeholder (mismo patrón que `compras`/`ventas`/etc., reutilizando `ModulePlaceholderPage`):
   - `/perfil` → "Mi Perfil"
   - `/perfil/seguridad` → "Seguridad de la cuenta"
   - `/perfil/configuraciones` → "Configuraciones"
   - `/perfil/sucursal` → "Cambiar sucursal"
   - `/perfil/ayuda` → "Ayuda y soporte"
4. "Cerrar sesión" ejecuta `useAuthSession().signOut()` y navega a `/login` al completar.

Fuera de alcance: endpoint backend de perfil propio, contenido real de esas 5 páginas (siguen siendo placeholders), mover el `ThemeToggle` dentro del menú (se mantiene como ícono aparte en el header, tal como está hoy — no se tocó en las preguntas de diseño), selector funcional de sucursal/tenant (la ruta es solo un placeholder de navegación).

## Componente `DropdownMenu` (packages/ui-web)

- Dependencia nueva: `radix-ui` (paquete unificado, ya que a futuro se prevé usar otros primitivos como Tooltip/Popover) en `packages/ui-web/package.json`.
- Radix maneja: apertura/cierre, foco por teclado (flechas, Home/End), cierre con Escape y al hacer click afuera, posicionamiento (evita que el menú se salga de la ventana), roles ARIA (`menu`, `menuitem`).
- La API expuesta desde `ui-web` es un conjunto pequeño de componentes de más alto nivel (no se re-exporta Radix crudo), siguiendo el estilo del resto de `ui-web`:
  - `DropdownMenu` (root + trigger + content, orquestado internamente)
  - `DropdownMenuItem` (ítem clickeable, con `icon`, `label`, y `to` para navegación via `react-router` `Link`, o `onSelect` para una acción como cerrar sesión)
  - `DropdownMenuLabel` (etiqueta de grupo, ej. "Cuenta")
  - `DropdownMenuSeparator`
- Estilos: superficie `bg-white dark:bg-neutral-900`, sombra, bordes redondeados, consistente con `Card`/`Modal`. Ítem hover `hover:bg-neutral-50 dark:hover:bg-neutral-800`. Ítem de tono `danger` (Cerrar sesión) con `text-danger-700 dark:text-danger-400` y hover `hover:bg-danger-50 dark:hover:bg-danger-900/20`.

## Cambios en `AppShell.tsx`

El bloque actual:

```tsx
<button className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800">
  <div className="...">MR</div>
  <div className="hidden sm:block">...</div>
  <ChevronDown className="..." />
</button>
```

se reemplaza por el nuevo `DropdownMenu` con el mismo trigger visual (mismas clases/estructura del botón), y como contenido el menú agrupado descrito arriba. El `ThemeToggle` permanece sin cambios, como ícono independiente junto al de notificaciones.

## Feature `features/perfil/`

Estructura mínima, siguiendo el patrón exacto de `features/compras/`:

```
features/perfil/
  routes.tsx        # 5 rutas, cada una lazy-loaded, reutilizando ModulePlaceholderPage
  index.ts           # export { profileRoutes } from './routes'
  pages/
    MyProfilePage.tsx
    AccountSecurityPage.tsx
    SettingsPage.tsx
    SwitchBranchPage.tsx
    HelpPage.tsx
```

Cada página es un wrapper de una línea sobre `ModulePlaceholderPage` (mismo patrón que `PurchasesPage.tsx`), con su propio `title`/`description`. Se agrega `profileRoutes` a `erpFeatureRoutes` en `app/feature-routes.ts`.

## Verificación

- `pnpm check` (lint + typecheck + test + build) en verde.
- Tests del nuevo `DropdownMenu` en `ui-web`: abre con click, cierra con Escape y con click afuera, navega correctamente en los ítems con `to`, ejecuta `onSelect` en "Cerrar sesión".
- Revisión visual manual (`pnpm dev`): el menú abre/cierra correctamente, se ve bien en claro y oscuro, "Cerrar sesión" efectivamente cierra sesión y redirige a `/login`.

## Riesgos / decisiones abiertas para el plan de implementación

- Confirmar en el plan si `pnpm-lock.yaml` requiere `pnpm install` explícito al agregar `radix-ui` (mismo patrón usado al agregar `lucide-react`/`@testing-library/user-event` en tareas previas).
- El ancho exacto del menú, espaciado y breakpoint en que el nombre/rol se ocultan (ya existe `hidden sm:block` en el trigger actual) se mantienen sin cambios; el plan de implementación fija los valores exactos de Tailwind para el contenido del menú en sí.
