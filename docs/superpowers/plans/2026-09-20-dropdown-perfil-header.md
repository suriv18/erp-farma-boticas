# Desplegable de Perfil en el Header Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el botón de perfil estático del header (`AppShell.tsx`) por un menú desplegable accesible (teclado, foco, cierre al click afuera) construido con `radix-ui`, con contenido agrupado por secciones (Cuenta / Preferencias), 5 rutas placeholder nuevas bajo `features/perfil/`, y un ítem "Cerrar sesión" funcional conectado a `useAuthSession().signOut()`.

**Architecture:** Un componente `DropdownMenu` nuevo en `packages/ui-web` envuelve el primitivo `DropdownMenu` de `radix-ui`, exponiendo una API de más alto nivel (`DropdownMenu`, `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator`) estilizada con los tokens de color existentes. `AppShell.tsx` consume ese componente reemplazando el `<button>` actual, manteniendo el mismo trigger visual. Una nueva feature `features/perfil/` (mismo patrón que `features/compras/`) aporta 5 páginas placeholder que reutilizan `ModulePlaceholderPage`, registradas en `app/feature-routes.ts`.

**Tech Stack:** React 19.2.8, `radix-ui` 1.6.7 (paquete unificado, primitivo `DropdownMenu`), React Router 8 (`Link`, rutas lazy), Tailwind CSS 4, Vitest + Testing Library (unitario/integración), Playwright 1.63.0 (E2E, nuevo en este plan), pnpm workspace.

## Global Constraints

- El nombre/avatar/rol del usuario en el trigger y en el encabezado del menú siguen siendo los valores hardcodeados actuales ("MR" / "María Rojas" / "Administradora") — no se agrega ninguna llamada a backend para obtener el perfil real; eso está explícitamente fuera de alcance.
- Las 5 rutas nuevas (`/perfil`, `/perfil/seguridad`, `/perfil/configuraciones`, `/perfil/sucursal`, `/perfil/ayuda`) son placeholders puros vía `ModulePlaceholderPage` — sin lógica ni datos propios.
- "Cerrar sesión" es la única acción real: llama a `useAuthSession().signOut()`. No se llama `navigate('/login')` manualmente — `RequireAuthentication` (`features/auth/components/RequireAuthentication.tsx:12-14`) ya redirige a `/login` automáticamente en cuanto `status` pasa a `'unauthenticated'`.
- El `ThemeToggle` permanece como ícono independiente en el header, sin moverse dentro del menú.
- No se cambia la estructura de `AppShell.tsx` fuera del bloque del botón de perfil.
- Nueva dependencia: `radix-ui@^1.6.7` en `packages/ui-web/package.json` (`dependencies`, es runtime, no dev — mismo criterio que `lucide-react`).
- Nueva dependencia de desarrollo: `@playwright/test@^1.63.0` en la raíz del workspace (`frontend/package.json`) — el proyecto no tenía ninguna infraestructura E2E antes de este plan; este plan la introduce y establece el patrón para specs futuros.
- Playwright corre contra `apps/erp-web` en modo mock (`VITE_API_MODE=mock`, activa MSW — ver `apps/erp-web/src/main.tsx:7-8` y `apps/erp-web/src/test/mocks/handlers.ts`), sin depender de un backend Java real levantado. Login E2E usa las credenciales mock ya definidas en `handlers.ts:4-19` (`password: 'Boticas2026!'`).
- 3 proyectos de Playwright (desktop, tablet, móvil) usando los dispositivos predefinidos de Playwright (`Desktop Chrome`, `iPad (gen 7)`, `iPhone 14`) — no viewports inventados a mano.
- `pnpm check` (lint + typecheck + test + build) debe pasar en verde al final de cada tarea que toque código. `pnpm e2e` (Playwright) debe pasar en verde al final de las tareas que tocan el flujo del dropdown.
- En Windows PowerShell, si `pnpm.ps1` está bloqueado por policy, usar `pnpm.cmd`.

---

## File Structure

**Nuevos archivos:**
- `packages/ui-web/src/dropdown-menu/DropdownMenu.tsx` — componentes `DropdownMenu`, `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator`.
- `packages/ui-web/src/dropdown-menu/DropdownMenu.test.tsx` — tests de apertura/cierre/navegación/acción.
- `apps/erp-web/src/features/perfil/routes.tsx` — 5 rutas lazy.
- `apps/erp-web/src/features/perfil/index.ts` — `export { profileRoutes } from './routes'`.
- `apps/erp-web/src/features/perfil/pages/MyProfilePage.tsx`
- `apps/erp-web/src/features/perfil/pages/AccountSecurityPage.tsx`
- `apps/erp-web/src/features/perfil/pages/SettingsPage.tsx`
- `apps/erp-web/src/features/perfil/pages/SwitchBranchPage.tsx`
- `apps/erp-web/src/features/perfil/pages/HelpPage.tsx`
- `frontend/playwright.config.ts` — configuración de Playwright, 3 proyectos (desktop/tablet/móvil).
- `frontend/e2e/support/login.ts` — helper compartido para autenticarse en modo mock antes de cada spec E2E.
- `frontend/e2e/profile-dropdown.spec.ts` — specs E2E del dropdown de perfil (abrir/cerrar, teclado, click afuera, navegación, cerrar sesión), corridos automáticamente en los 3 proyectos.

**Modificados:**
- `packages/ui-web/package.json` — agrega `radix-ui` a `dependencies`.
- `packages/ui-web/src/index.ts` — exporta los 4 símbolos de `DropdownMenu`.
- `apps/erp-web/src/shared/layout/AppShell.tsx` — reemplaza el `<button>` de perfil por `DropdownMenu`.
- `apps/erp-web/src/app/feature-routes.ts` — agrega `profileRoutes` a `erpFeatureRoutes`.
- `apps/erp-web/src/app/feature-routes.test.ts` — actualiza la lista de paths esperados.
- `frontend/package.json` — agrega `@playwright/test` a `devDependencies` y el script `e2e`.
- `frontend/.gitignore` (o `.gitignore` raíz, verificar cuál aplica) — agrega `/test-results/` y `/playwright-report/`.

---

### Task 1: Agregar dependencia `radix-ui` a `ui-web`

**Files:**
- Modify: `packages/ui-web/package.json`

**Interfaces:**
- Produces: paquete `radix-ui` instalado y disponible para import (`import { DropdownMenu } from 'radix-ui'`) en `packages/ui-web`.

- [ ] **Step 1: Agregar la dependencia**

En `packages/ui-web/package.json`, agregar `"radix-ui": "^1.6.7"` al objeto `"dependencies"` (que hoy solo tiene `lucide-react`):

```json
"dependencies": {
  "lucide-react": "^1.30.0",
  "radix-ui": "^1.6.7"
},
```

- [ ] **Step 2: Instalar**

Run: `pnpm install` (desde `frontend/`)
Expected: `pnpm-lock.yaml` se actualiza con la entrada de `radix-ui` y sus dependencias transitivas; exit code 0.

- [ ] **Step 3: Verificar que el import funciona**

Run: `pnpm --filter @boticas/ui-web typecheck`
Expected: sin errores (el paquete aún no se usa en ningún archivo `.tsx`, así que esto solo confirma que la instalación fue correcta y no rompió nada).

- [ ] **Step 4: Commit**

```bash
git add packages/ui-web/package.json pnpm-lock.yaml
git commit -m "feat(ui-web): agregar radix-ui como dependencia para DropdownMenu"
```

---

### Task 2: Componente `DropdownMenu` en `ui-web` (TDD)

**Files:**
- Create: `packages/ui-web/src/dropdown-menu/DropdownMenu.tsx`
- Create: `packages/ui-web/src/dropdown-menu/DropdownMenu.test.tsx`
- Modify: `packages/ui-web/src/index.ts`

**Interfaces:**
- Consumes: `radix-ui`'s `DropdownMenu` primitive (`Root`, `Trigger`, `Portal`, `Content`, `Item`, `Label`, `Separator`); `cn` desde `../lib/cn`.
- Produces:
  - `DropdownMenu({ trigger, children }: { trigger: ReactNode; children: ReactNode })` — componente raíz que envuelve trigger + contenido.
  - `DropdownMenuItem({ icon, children, to, tone, onSelect }: { icon?: LucideIcon; children: ReactNode; to?: string; tone?: 'default' | 'danger'; onSelect?: () => void })` — si `to` está presente, renderiza como `Link` de `react-router` dentro del `Item` de Radix; si no, ejecuta `onSelect` al seleccionarse.
  - `DropdownMenuLabel({ children }: { children: ReactNode })`.
  - `DropdownMenuSeparator()`.
  - Estos 4 símbolos son consumidos por Task 4 (`AppShell.tsx`).

- [ ] **Step 1: Escribir el test (falla primero)**

Crear `packages/ui-web/src/dropdown-menu/DropdownMenu.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { LogOut, User } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from './DropdownMenu';

function renderMenu(onSelectLogout: () => void) {
  return render(
    <MemoryRouter>
      <DropdownMenu trigger={<button>Abrir menú</button>}>
        <DropdownMenuLabel>Cuenta</DropdownMenuLabel>
        <DropdownMenuItem icon={User} to="/perfil">
          Mi Perfil
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem icon={LogOut} tone="danger" onSelect={onSelectLogout}>
          Cerrar sesión
        </DropdownMenuItem>
      </DropdownMenu>
    </MemoryRouter>
  );
}

describe('DropdownMenu', () => {
  it('no muestra el contenido antes de abrir', () => {
    renderMenu(() => {});
    expect(screen.queryByText('Mi Perfil')).not.toBeInTheDocument();
  });

  it('muestra el contenido al hacer clic en el trigger', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));

    expect(screen.getByText('Cuenta')).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /Mi Perfil/ })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /Cerrar sesión/ })).toBeInTheDocument();
  });

  it('cierra el menú con la tecla Escape', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));
    expect(screen.getByText('Cuenta')).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.queryByText('Cuenta')).not.toBeInTheDocument();
  });

  it('el item con "to" renderiza un enlace de navegación', async () => {
    const user = userEvent.setup();
    renderMenu(() => {});

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));

    const link = screen.getByRole('menuitem', { name: /Mi Perfil/ });
    expect(link.querySelector('a')).toHaveAttribute('href', '/perfil');
  });

  it('el item sin "to" ejecuta onSelect al seleccionarlo', async () => {
    const user = userEvent.setup();
    const onSelectLogout = vi.fn();
    renderMenu(onSelectLogout);

    await user.click(screen.getByRole('button', { name: 'Abrir menú' }));
    await user.click(screen.getByRole('menuitem', { name: /Cerrar sesión/ }));

    expect(onSelectLogout).toHaveBeenCalledOnce();
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm vitest run --project ui-web -- src/dropdown-menu/DropdownMenu.test.tsx` (desde `frontend/`)
Expected: FAIL — `Cannot find module './DropdownMenu'`.

- [ ] **Step 3: Implementar `DropdownMenu.tsx`**

Crear `packages/ui-web/src/dropdown-menu/DropdownMenu.tsx`:

```tsx
import type { ReactNode } from 'react';
import { DropdownMenu as RadixDropdownMenu } from 'radix-ui';
import { Link } from 'react-router';
import type { LucideIcon } from 'lucide-react';
import { cn } from '../lib/cn';

export type DropdownMenuProps = {
  trigger: ReactNode;
  children: ReactNode;
};

export function DropdownMenu({ trigger, children }: DropdownMenuProps) {
  return (
    <RadixDropdownMenu.Root>
      <RadixDropdownMenu.Trigger asChild>{trigger}</RadixDropdownMenu.Trigger>
      <RadixDropdownMenu.Portal>
        <RadixDropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 w-64 overflow-hidden rounded-2xl border border-neutral-200 bg-white py-1.5 shadow-xl dark:border-neutral-800 dark:bg-neutral-900"
        >
          {children}
        </RadixDropdownMenu.Content>
      </RadixDropdownMenu.Portal>
    </RadixDropdownMenu.Root>
  );
}

export type DropdownMenuItemProps = {
  icon?: LucideIcon;
  children: ReactNode;
  to?: string;
  tone?: 'default' | 'danger';
  onSelect?: () => void;
};

export function DropdownMenuItem({
  icon: Icon,
  children,
  to,
  tone = 'default',
  onSelect
}: DropdownMenuItemProps) {
  const itemClassName = cn(
    'flex cursor-pointer items-center gap-2.5 px-4 py-2.5 text-sm font-medium outline-none transition-colors',
    tone === 'danger'
      ? 'text-danger-700 hover:bg-danger-50 dark:text-danger-400 dark:hover:bg-danger-900/20'
      : 'text-neutral-700 hover:bg-neutral-50 dark:text-neutral-200 dark:hover:bg-neutral-800'
  );

  if (to) {
    return (
      <RadixDropdownMenu.Item asChild>
        <Link to={to} className={itemClassName}>
          {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
          {children}
        </Link>
      </RadixDropdownMenu.Item>
    );
  }

  return (
    <RadixDropdownMenu.Item className={itemClassName} onSelect={onSelect}>
      {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
      {children}
    </RadixDropdownMenu.Item>
  );
}

export function DropdownMenuLabel({ children }: { children: ReactNode }) {
  return (
    <RadixDropdownMenu.Label className="px-4 pt-2 pb-1 text-[11px] font-bold tracking-[0.08em] text-neutral-400 uppercase dark:text-neutral-500">
      {children}
    </RadixDropdownMenu.Label>
  );
}

export function DropdownMenuSeparator() {
  return (
    <RadixDropdownMenu.Separator className="my-1.5 h-px bg-neutral-100 dark:bg-neutral-800" />
  );
}
```

- [ ] **Step 4: Exportar desde `index.ts`**

Editar `packages/ui-web/src/index.ts`, agregar tras la línea de `DataTable`:

```ts
export {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  type DropdownMenuProps,
  type DropdownMenuItemProps
} from './dropdown-menu/DropdownMenu';
```

(mantener el orden alfabético existente en el archivo: entre `DataTable` y `EstadoBadge`).

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm vitest run --project ui-web -- src/dropdown-menu/DropdownMenu.test.tsx`
Expected: PASS — 5 tests.

- [ ] **Step 6: Ejecutar toda la suite de `ui-web` para confirmar que nada se rompió**

Run: `pnpm vitest run --project ui-web`
Expected: PASS — todos los tests existentes más los 5 nuevos.

- [ ] **Step 7: Commit**

```bash
git add packages/ui-web/src/dropdown-menu packages/ui-web/src/index.ts
git commit -m "feat(ui-web): agregar componente DropdownMenu sobre radix-ui"
```

---

### Task 3: Feature `features/perfil/` con 5 páginas placeholder

**Files:**
- Create: `apps/erp-web/src/features/perfil/pages/MyProfilePage.tsx`
- Create: `apps/erp-web/src/features/perfil/pages/AccountSecurityPage.tsx`
- Create: `apps/erp-web/src/features/perfil/pages/SettingsPage.tsx`
- Create: `apps/erp-web/src/features/perfil/pages/SwitchBranchPage.tsx`
- Create: `apps/erp-web/src/features/perfil/pages/HelpPage.tsx`
- Create: `apps/erp-web/src/features/perfil/routes.tsx`
- Create: `apps/erp-web/src/features/perfil/index.ts`

**Interfaces:**
- Consumes: `ModulePlaceholderPage` desde `../../../shared/pages/ModulePlaceholderPage` (firma `{ title: string; description: string }`, sin cambios).
- Produces: `profileRoutes` — un array `RouteObject[]` con 5 rutas lazy, paths `perfil`, `perfil/seguridad`, `perfil/configuraciones`, `perfil/sucursal`, `perfil/ayuda`. Consumido por Task 5 (`feature-routes.ts`).

- [ ] **Step 1: Crear las 5 páginas placeholder**

Crear `apps/erp-web/src/features/perfil/pages/MyProfilePage.tsx`:

```tsx
import { ModulePlaceholderPage } from '../../../shared/pages/ModulePlaceholderPage';

export function MyProfilePage() {
  return (
    <ModulePlaceholderPage
      title="Mi Perfil"
      description="Datos personales y preferencias de tu cuenta."
    />
  );
}
```

Crear `apps/erp-web/src/features/perfil/pages/AccountSecurityPage.tsx`:

```tsx
import { ModulePlaceholderPage } from '../../../shared/pages/ModulePlaceholderPage';

export function AccountSecurityPage() {
  return (
    <ModulePlaceholderPage
      title="Seguridad de la cuenta"
      description="Contraseña, dispositivos y sesiones activas de tu cuenta."
    />
  );
}
```

Crear `apps/erp-web/src/features/perfil/pages/SettingsPage.tsx`:

```tsx
import { ModulePlaceholderPage } from '../../../shared/pages/ModulePlaceholderPage';

export function SettingsPage() {
  return (
    <ModulePlaceholderPage
      title="Configuraciones"
      description="Preferencias generales de la plataforma."
    />
  );
}
```

Crear `apps/erp-web/src/features/perfil/pages/SwitchBranchPage.tsx`:

```tsx
import { ModulePlaceholderPage } from '../../../shared/pages/ModulePlaceholderPage';

export function SwitchBranchPage() {
  return (
    <ModulePlaceholderPage
      title="Cambiar sucursal"
      description="Selecciona la empresa o sucursal con la que quieres trabajar."
    />
  );
}
```

Crear `apps/erp-web/src/features/perfil/pages/HelpPage.tsx`:

```tsx
import { ModulePlaceholderPage } from '../../../shared/pages/ModulePlaceholderPage';

export function HelpPage() {
  return (
    <ModulePlaceholderPage
      title="Ayuda y soporte"
      description="Documentación y canales de contacto con soporte interno."
    />
  );
}
```

- [ ] **Step 2: Crear `routes.tsx`**

Crear `apps/erp-web/src/features/perfil/routes.tsx`:

```tsx
import type { RouteObject } from 'react-router';

export const profileRoutes = [
  {
    path: 'perfil',
    lazy: async () => {
      const { MyProfilePage } = await import('./pages/MyProfilePage');
      return { Component: MyProfilePage };
    }
  },
  {
    path: 'perfil/seguridad',
    lazy: async () => {
      const { AccountSecurityPage } = await import('./pages/AccountSecurityPage');
      return { Component: AccountSecurityPage };
    }
  },
  {
    path: 'perfil/configuraciones',
    lazy: async () => {
      const { SettingsPage } = await import('./pages/SettingsPage');
      return { Component: SettingsPage };
    }
  },
  {
    path: 'perfil/sucursal',
    lazy: async () => {
      const { SwitchBranchPage } = await import('./pages/SwitchBranchPage');
      return { Component: SwitchBranchPage };
    }
  },
  {
    path: 'perfil/ayuda',
    lazy: async () => {
      const { HelpPage } = await import('./pages/HelpPage');
      return { Component: HelpPage };
    }
  }
] satisfies RouteObject[];
```

- [ ] **Step 3: Crear `index.ts`**

Crear `apps/erp-web/src/features/perfil/index.ts`:

```ts
export { profileRoutes } from './routes';
```

- [ ] **Step 4: Verificar typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck` (desde `frontend/`)
Expected: sin errores (estos archivos aún no están importados por nadie, así que solo se valida su propia sintaxis/tipos).

- [ ] **Step 5: Commit**

```bash
git add apps/erp-web/src/features/perfil
git commit -m "feat(frontend): agregar paginas placeholder de la feature perfil"
```

---

### Task 4: Registrar `profileRoutes` en el router y actualizar el test de rutas

**Files:**
- Modify: `apps/erp-web/src/app/feature-routes.ts`
- Modify: `apps/erp-web/src/app/feature-routes.test.ts`

**Interfaces:**
- Consumes: `profileRoutes` desde `../features/perfil` (Task 3).

- [ ] **Step 1: Registrar las rutas**

Editar `apps/erp-web/src/app/feature-routes.ts`:

```ts
import type { RouteObject } from 'react-router';
import { authRoutes } from '../features/auth';
import { cashRegisterRoutes } from '../features/caja';
import { catalogRoutes } from '../features/catalogo';
import { customerRoutes } from '../features/clientes';
import { purchasesRoutes } from '../features/compras';
import { dashboardRoutes } from '../features/dashboard';
import { inventoryRoutes } from '../features/inventario';
import { organizationRoutes } from '../features/organizacion';
import { posRoutes } from '../features/pos';
import { profileRoutes } from '../features/perfil';
import { securityRoutes } from '../features/seguridad';
import { salesRoutes } from '../features/ventas';

export const publicFeatureRoutes = [...authRoutes] satisfies RouteObject[];

export const erpFeatureRoutes = [
  ...dashboardRoutes,
  ...catalogRoutes,
  ...inventoryRoutes,
  ...purchasesRoutes,
  ...salesRoutes,
  ...posRoutes,
  ...cashRegisterRoutes,
  ...customerRoutes,
  ...securityRoutes,
  ...organizationRoutes,
  ...profileRoutes
] satisfies RouteObject[];
```

- [ ] **Step 2: Actualizar el test existente**

En `apps/erp-web/src/app/feature-routes.test.ts`, el array esperado de paths (línea 8-26) debe incluir los 5 paths nuevos al final, en el mismo orden en que `profileRoutes` se agregó a `erpFeatureRoutes` (al final):

```ts
    expect(paths).toEqual([
      'dashboard',
      'catalogo',
      'catalogo/marcas',
      'catalogo/categorias',
      'inventario',
      'compras',
      'ventas',
      'pos',
      'caja',
      'clientes',
      'seguridad',
      'seguridad/permisos',
      'seguridad/usuarios',
      'seguridad/usuarios/:userId',
      'seguridad/roles',
      'seguridad/roles/:roleId',
      'organizacion',
      'perfil',
      'perfil/seguridad',
      'perfil/configuraciones',
      'perfil/sucursal',
      'perfil/ayuda'
    ]);
```

- [ ] **Step 3: Ejecutar el test**

Run: `pnpm vitest run --project erp-web -- src/app/feature-routes.test.ts` (desde `frontend/`)
Expected: PASS — 2 tests (incluye la aserción de que no hay paths duplicados y que todas las rutas usan `lazy`).

- [ ] **Step 4: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 5: Commit**

```bash
git add apps/erp-web/src/app/feature-routes.ts apps/erp-web/src/app/feature-routes.test.ts
git commit -m "feat(frontend): registrar rutas de perfil en el router"
```

---

### Task 5: Integrar `DropdownMenu` en `AppShell.tsx`

**Files:**
- Modify: `apps/erp-web/src/shared/layout/AppShell.tsx`

**Interfaces:**
- Consumes: `DropdownMenu`, `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator` desde `@boticas/ui-web` (Task 2); `useAuthSession` desde `../../features/auth` (ya expuesto, ver `features/auth/index.ts:4`).

- [ ] **Step 1: Leer el archivo actual para confirmar el bloque exacto a reemplazar**

El bloque actual (líneas ~146-157 de `AppShell.tsx`, dentro de `<div className="ml-auto flex items-center gap-2">`):

```tsx
            <button className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800">
              <div className="bg-primary-100 text-primary-800 dark:bg-primary-900/50 dark:text-primary-200 grid size-9 place-items-center rounded-xl text-sm font-bold">
                MR
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">
                  María Rojas
                </p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400">Administradora</p>
              </div>
              <ChevronDown className="hidden size-4 text-neutral-400 sm:block" />
            </button>
```

- [ ] **Step 2: Reemplazar imports**

Agregar al import de `lucide-react` (junto a los íconos ya usados): `HelpCircle`, `LogOut`, `Settings`, `Shield`, `Store`, `User`.

Agregar al import de `@boticas/ui-web`: `DropdownMenu`, `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator`.

Agregar un nuevo import: `import { useAuthSession } from '../../features/auth';`.

- [ ] **Step 3: Reemplazar el bloque del botón de perfil**

Sustituir el `<button>` de los Steps 1 por:

```tsx
            <DropdownMenu
              trigger={
                <button
                  aria-label="Menú de perfil de María Rojas"
                  className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800"
                >
                  <div className="bg-primary-100 text-primary-800 dark:bg-primary-900/50 dark:text-primary-200 grid size-9 place-items-center rounded-xl text-sm font-bold">
                    MR
                  </div>
                  <div className="hidden sm:block">
                    <p className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">
                      María Rojas
                    </p>
                    <p className="text-xs text-neutral-500 dark:text-neutral-400">
                      Administradora
                    </p>
                  </div>
                  <ChevronDown className="hidden size-4 text-neutral-400 sm:block" />
                </button>
              }
            >
              <div className="bg-primary-600 flex items-center gap-3 px-4 py-3.5 text-white">
                <div className="grid size-9 place-items-center rounded-full bg-white text-sm font-bold text-primary-700">
                  MR
                </div>
                <div>
                  <p className="text-sm font-semibold">María Rojas</p>
                  <p className="text-primary-100 text-xs">Administradora</p>
                </div>
              </div>

              <DropdownMenuLabel>Cuenta</DropdownMenuLabel>
              <DropdownMenuItem icon={User} to="/perfil">
                Mi Perfil
              </DropdownMenuItem>
              <DropdownMenuItem icon={Shield} to="/perfil/seguridad">
                Seguridad
              </DropdownMenuItem>

              <DropdownMenuLabel>Preferencias</DropdownMenuLabel>
              <DropdownMenuItem icon={Settings} to="/perfil/configuraciones">
                Configuraciones
              </DropdownMenuItem>
              <DropdownMenuItem icon={Store} to="/perfil/sucursal">
                Cambiar sucursal
              </DropdownMenuItem>

              <DropdownMenuSeparator />
              <DropdownMenuItem icon={HelpCircle} to="/perfil/ayuda">
                Ayuda y soporte
              </DropdownMenuItem>

              <DropdownMenuSeparator />
              <DropdownMenuItem icon={LogOut} tone="danger" onSelect={() => void signOut()}>
                Cerrar sesión
              </DropdownMenuItem>
            </DropdownMenu>
```

Nota importante sobre el `aria-label` del trigger: el bloque `<div className="hidden sm:block">` que envuelve "María Rojas"/"Administradora" se oculta por debajo del breakpoint `sm` (640px) — el proyecto Playwright `mobile` (Task 6, iPhone 14, ~390px de ancho) cae por debajo de ese breakpoint. Sin un `aria-label` explícito en el `<button>`, el accessible name del trigger estaría vacío en viewport móvil (solo quedaría el texto oculto por `hidden`, que los lectores de pantalla y Playwright's accessibility tree no cuentan). Por eso el trigger lleva `aria-label="Menú de perfil de María Rojas"` fijo, independiente del breakpoint — esto es lo que hace que los specs de Task 7 (`page.getByRole('button', { name: /María Rojas/ })`) funcionen igual en los 3 proyectos.
```

- [ ] **Step 4: Obtener `signOut` dentro de `AppShell`**

Dentro de la función `export function AppShell()`, junto a la línea existente `const [mobileMenuOpen, setMobileMenuOpen] = useState(false);`, agregar:

```tsx
  const { signOut } = useAuthSession();
```

- [ ] **Step 5: Verificar typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 6: Verificar build**

Run: `pnpm --filter @boticas/erp-web build`
Expected: build exitoso.

- [ ] **Step 7: Ejecutar toda la suite de `erp-web`**

Run: `pnpm vitest run --project erp-web` (desde `frontend/`)
Expected: PASS — todos los tests existentes (ningún test cubre hoy `AppShell.tsx` directamente, así que no se esperan regresiones ahí; el test de `feature-routes.test.ts` de Task 4 ya cubre las rutas).

- [ ] **Step 8: Commit**

```bash
git add apps/erp-web/src/shared/layout/AppShell.tsx
git commit -m "feat(frontend): reemplazar boton de perfil por DropdownMenu con Cerrar sesion funcional"
```

---

### Task 6: Instalar y configurar Playwright (desktop/tablet/móvil)

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/.gitignore`
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/support/login.ts`

**Interfaces:**
- Produces: script `pnpm e2e` (raíz del workspace) que corre todos los specs bajo `frontend/e2e/**/*.spec.ts` en 3 proyectos (`desktop`, `tablet`, `mobile`); helper `login(page: Page): Promise<void>` en `e2e/support/login.ts`, consumido por Task 7.

- [ ] **Step 1: Instalar Playwright**

Run (desde `frontend/`): `pnpm add -D -w @playwright/test@^1.63.0`

Expected: `@playwright/test` agregado a `devDependencies` de `frontend/package.json`; `pnpm-lock.yaml` actualizado.

- [ ] **Step 2: Instalar los navegadores de Playwright**

Run: `pnpm exec playwright install --with-deps chromium`

Expected: descarga el binario de Chromium que usan los 3 proyectos (desktop/tablet/móvil emulan viewports sobre el mismo motor Chromium, no requieren WebKit/Firefox para este plan).

- [ ] **Step 3: Agregar el script `e2e`**

En `frontend/package.json`, agregar al objeto `"scripts"` (junto a `"test"`):

```json
"e2e": "playwright test",
```

- [ ] **Step 4: Crear `playwright.config.ts`**

Crear `frontend/playwright.config.ts`:

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry'
  },
  webServer: {
    command: 'pnpm --filter @boticas/erp-web dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    env: {
      VITE_API_MODE: 'mock'
    }
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'tablet',
      use: { ...devices['iPad (gen 7)'] }
    },
    {
      name: 'mobile',
      use: { ...devices['iPhone 14'] }
    }
  ]
});
```

- [ ] **Step 5: Crear el helper de login compartido**

Crear `frontend/e2e/support/login.ts`:

```ts
import type { Page } from '@playwright/test';

export async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Correo corporativo').fill('maria.rojas@boticas.pe');
  await page.getByLabel('Contraseña').fill('Boticas2026!');
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click();
  await page.waitForURL('**/dashboard');
}
```

Nota: las credenciales están fijadas por el mock handler (`apps/erp-web/src/test/mocks/handlers.ts:4-19`), que solo valida la contraseña (`'Boticas2026!'`) — el valor de correo es arbitrario mientras sea un string no vacío que pase la validación del formulario (`login.schema.ts`), ya que el mock no verifica el campo `login`.

- [ ] **Step 6: Actualizar `.gitignore`**

En `frontend/.gitignore`, agregar tras la línea `.vite/`:

```
/test-results/
/playwright-report/
```

- [ ] **Step 7: Verificar que Playwright arranca (smoke test manual)**

Crear temporalmente un spec vacío no es necesario — en su lugar, verificar que el comando reconoce la config sin errores:

Run: `pnpm exec playwright test --list`
Expected: exit 0, imprime "No tests found" (o similar) ya que `e2e/` aún no tiene ningún `.spec.ts` — confirma que `playwright.config.ts` es válido y los 3 proyectos se registran.

- [ ] **Step 8: Commit**

```bash
git add package.json pnpm-lock.yaml playwright.config.ts .gitignore e2e/support/login.ts
git commit -m "feat(frontend): instalar y configurar Playwright con proyectos desktop/tablet/movil"
```

---

### Task 7: Specs E2E del dropdown de perfil

**Files:**
- Create: `frontend/e2e/profile-dropdown.spec.ts`

**Interfaces:**
- Consumes: `login` desde `./support/login` (Task 6); el `DropdownMenu` renderizado en `AppShell.tsx` (Task 5) — este spec solo puede escribirse y pasar después de que Task 5 esté completa.

- [ ] **Step 1: Escribir el spec**

Crear `frontend/e2e/profile-dropdown.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { login } from './support/login';

test.describe('Dropdown de perfil', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('abre al hacer clic en el trigger y muestra los grupos', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Seguridad' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Configuraciones' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Cambiar sucursal' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Ayuda y soporte' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Cerrar sesión' })).toBeVisible();
  });

  test('cierra con la tecla Escape', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    await page.keyboard.press('Escape');

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeHidden();
  });

  test('cierra al hacer clic afuera', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    await page.mouse.click(10, 10);

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeHidden();
  });

  test('navega a la pagina placeholder al hacer clic en Mi Perfil', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await page.getByRole('menuitem', { name: 'Mi Perfil' }).click();

    await expect(page).toHaveURL(/\/perfil$/);
    await expect(page.getByRole('heading', { name: 'Mi Perfil' })).toBeVisible();
  });

  test('cierra sesion y redirige a login', async ({ page }) => {
    await page.getByRole('button', { name: /María Rojas/ }).click();
    await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click();

    await expect(page).toHaveURL(/\/login$/);
  });

  test('navegable por teclado: Tab hasta el trigger, Enter abre, flecha mueve el foco', async ({
    page
  }) => {
    const trigger = page.getByRole('button', { name: /María Rojas/ });
    await trigger.focus();
    await page.keyboard.press('Enter');

    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeVisible();

    await page.keyboard.press('ArrowDown');
    await expect(page.getByRole('menuitem', { name: 'Mi Perfil' })).toBeFocused();
  });
});
```

- [ ] **Step 2: Ejecutar los specs en los 3 proyectos**

Run (desde `frontend/`): `pnpm e2e`
Expected: PASS — 6 tests × 3 proyectos (desktop/tablet/mobile) = 18 ejecuciones, todas en verde. El `webServer` de `playwright.config.ts` levanta `pnpm --filter @boticas/erp-web dev` automáticamente en modo mock antes de correr los specs.

Si algún test falla por timing (el menú de Radix anima su apertura), agregar `await expect(...).toBeVisible()` con el timeout default de Playwright (5s) es suficiente — no se requiere `page.waitForTimeout`.

- [ ] **Step 3: Revisar el reporte HTML si algo falló**

Run: `pnpm exec playwright show-report`
Expected: (solo si el Step 2 falló) abre el reporte interactivo con capturas/traza del fallo para diagnosticar.

- [ ] **Step 4: Commit**

```bash
git add e2e/profile-dropdown.spec.ts
git commit -m "test(frontend): agregar specs E2E del dropdown de perfil (desktop/tablet/movil)"
```

---

### Task 8: Verificación final end-to-end

**Files:** ninguno (solo verificación).

- [ ] **Step 1: Ejecutar el pipeline completo**

Run: `pnpm check` (desde `frontend/`)
Expected: lint + typecheck + test + build en verde para todo el workspace (incluye los tests unitarios de `DropdownMenu` de Task 2 y el test de rutas actualizado de Task 4).

- [ ] **Step 2: Ejecutar la suite E2E completa una vez más**

Run: `pnpm e2e` (desde `frontend/`)
Expected: los 6 specs de Task 7 en verde en los 3 proyectos (desktop/tablet/mobile) — 18 ejecuciones en total, confirmando que nada de las tareas posteriores rompió el flujo.

- [ ] **Step 3: Verificación visual manual en navegador**

Run: `pnpm dev`.

Con el servidor arriba:
1. Iniciar sesión (o usar una sesión ya activa) y llegar al dashboard.
2. Hacer clic en el botón de perfil (esquina superior derecha del header) — el menú debe abrir con el encabezado morado/azul (`bg-primary-600`), los grupos "Cuenta" y "Preferencias", y "Cerrar sesión" en rojo al final.
3. Confirmar que se ve correctamente en modo claro y oscuro (usando el `ThemeToggle`).
4. Navegar con el teclado (Tab hasta el trigger, Enter para abrir, flechas para moverse entre ítems, Escape para cerrar) — confirmar que funciona.
5. Hacer clic afuera del menú abierto — debe cerrarse.
6. Hacer clic en "Mi Perfil" — debe navegar a `/perfil` y mostrar la página placeholder "Mi Perfil".
7. Volver, abrir el menú de nuevo, hacer clic en "Cerrar sesión" — debe cerrar la sesión y redirigir a `/login`.

Expected: todos los pasos anteriores funcionan como se describe, sin errores en la consola del navegador.

- [ ] **Step 4: Commit final si hubo ajustes de la verificación**

Solo si el Step 2 o el Step 3 encontraron y corrigieron algo:

```bash
git add -A
git commit -m "fix(frontend): ajustes tras verificacion visual del dropdown de perfil"
```

---

## Self-Review

**Spec coverage:**
- Componente `DropdownMenu` sobre `radix-ui` → Tasks 1-2.
- Contenido agrupado (Cuenta/Preferencias + Ayuda + Cerrar sesión) → Task 5.
- 5 rutas placeholder → Task 3-4.
- Cerrar sesión funcional → Task 5 (usa `signOut()` real, sin `navigate` manual — documentado en Global Constraints por qué).
- Pruebas unitarias del `DropdownMenu` (apertura, cierre con Escape, navegación con `to`, acción con `onSelect`) → Task 2, con TDD (RED confirmado antes de implementar).
- Pruebas E2E multi-viewport (desktop/tablet/móvil) del flujo completo del dropdown (abrir, grupos visibles, Escape, click afuera, navegación, cerrar sesión, navegación por teclado) → Tasks 6-7, corridas dos veces (Task 7 al escribirlas, Task 8 como confirmación final).
- Verificación (`pnpm check` = lint + typecheck + test unitario + build, más `pnpm e2e`, más manual en navegador con teclado/click-afuera/dark mode) → Task 8.

**Placeholder scan:** sin "TBD"/"TODO"; todo el código de las tareas está completo, no hay pasos descritos sin código mostrado.

**Type consistency:** `DropdownMenuItemProps` (`to`, `tone`, `onSelect`, `icon`) definido en Task 2 se usa consistentemente en Task 5 con los mismos nombres de prop. `ModulePlaceholderPage`'s `{ title, description }` (ya existente, sin cambios) se usa igual en las 5 páginas de Task 3. El `aria-label` del trigger fijado en Task 5 es el mismo string (`/María Rojas/` como substring) que los locators de los specs E2E de Task 7 — sin esa fijación, los specs del proyecto `mobile` habrían fallado por el `hidden sm:block` del breakpoint (detectado y corregido durante la escritura del plan, no dejado para el implementador).

**Scope check:** cada tarea produce un estado verificable de forma independiente. Dependencias reales: Task 2 depende de Task 1 (paquete instalado); Task 4 depende de Task 3 (rutas exportadas); Task 5 depende de Tasks 2 y 4 (componente + rutas); Task 7 depende de Task 5 (el flujo debe existir en la UI antes de poder escribir specs E2E contra él) y de Task 6 (config de Playwright); Task 8 depende de todas las anteriores.
