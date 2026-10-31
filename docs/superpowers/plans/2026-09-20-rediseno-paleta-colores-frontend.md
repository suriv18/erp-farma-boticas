# Rediseño de Paleta de Colores y Dark Mode — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar la paleta de color ad-hoc del frontend (`teal-*`/`slate-*`/`emerald-*`/`rose-*`/`amber-*`/`blue-*` de Tailwind, hardcodeados por archivo) por design tokens semánticos (`primary`/`secondary`/`success`/`neutral`/`warning`/`danger`) basados en Primary `#2563EB`, Secondary `#06B6D4`, Tertiary(→success) `#10B981`, Neutral `#0F172A`, con soporte de dark mode (preferencia del SO + toggle manual persistido), aplicados a las 3 primitivas compartidas de `packages/ui-web` y a todas las pantallas existentes de `apps/erp-web`.

**Architecture:** Tailwind CSS 4 define los tokens en `@theme` dentro de `apps/erp-web/src/styles.css` (escalas 50→950 por color, más overrides bajo `[data-theme="dark"]` para superficie/texto/borde). El modo se controla con un atributo `data-theme` en `<html>`, fijado antes del primer paint por un script inline en `index.html`, y modificable en runtime por un hook `useTheme` + componente `ThemeToggle` nuevos en `packages/ui-web`. Las 3 primitivas de `ui-web` (`Button`, `Card`, `Badge`) y 3 componentes hoy duplicados entre `features/catalogo` y `features/seguridad` (`Modal`, `DataTable`, `EstadoBadge`) se migran/unifican en `ui-web` usando los tokens. El resto de las features reemplaza sus clases de color hardcodeadas por las clases generadas por los tokens, sin tocar estructura JSX ni lógica.

**Tech Stack:** React 19.2, Tailwind CSS 4.3 (`@theme`, CSS-first config), Vite 8, Vitest + Testing Library, pnpm workspaces (`packages/ui-web`, `apps/erp-web`).

## Global Constraints

- Colores base exactos: Primary `#2563EB`, Secondary `#06B6D4`, Success (ex-Tertiary) `#10B981`, Neutral `#0F172A`. Warning/Danger usan las escalas estándar de Tailwind (`amber`/`red`), no colores derivados de la paleta de marca.
- Dark mode: por defecto sigue `prefers-color-scheme`; el usuario puede forzar claro/oscuro con un toggle, persistido en `localStorage` bajo la clave `theme` (valores `'light' | 'dark'`; ausente = seguir SO).
- No se cambia estructura JSX, layout, copy, ni lógica de negocio de ninguna pantalla — solo clases de color/superficie y lo estrictamente necesario para contraste de texto sobre la nueva paleta.
- `pnpm check` (lint + typecheck + test + build) debe pasar en verde al final de cada tarea que toque código de `apps/erp-web` o `packages/ui-web`.
- Alcance de migración: `packages/ui-web`, y en `apps/erp-web`: `auth`, `dashboard`, `seguridad`, `catalogo`, `organizacion`, `shared/layout/AppShell.tsx`, `shared/pages/ModulePlaceholderPage.tsx` (cubre compras/ventas/pos/caja/clientes/inventario, que renderizan `ModulePlaceholderPage` sin JSX propio).
- En Windows PowerShell, si `pnpm.ps1` está bloqueado por policy, usar `pnpm.cmd`.

---

## File Structure

**Nuevos archivos:**
- `packages/ui-web/src/theme/useTheme.ts` — hook de estado del tema (lee/escribe `localStorage`, expone `theme`/`setTheme`).
- `packages/ui-web/src/theme/ThemeToggle.tsx` — botón de 2 estados (claro/oscuro) que usa `useTheme`.
- `packages/ui-web/src/theme/ThemeToggle.test.tsx` — test del toggle.
- `packages/ui-web/src/modal/Modal.tsx` — versión unificada de `Modal`.
- `packages/ui-web/src/modal/Modal.test.tsx` — migrado desde `features/seguridad/components/Modal.test.tsx`.
- `packages/ui-web/src/data-table/DataTable.tsx` — versión unificada de `DataTable`.
- `packages/ui-web/src/data-table/DataTable.test.tsx` — fusión de ambos tests existentes.
- `packages/ui-web/src/estado-badge/EstadoBadge.tsx` — versión unificada de `EstadoBadge`.
- `packages/ui-web/src/estado-badge/EstadoBadge.test.tsx` — migrado desde `features/seguridad/components/EstadoBadge.test.tsx`, con aserciones de clase actualizadas a los nuevos tokens.

**Modificados:**
- `apps/erp-web/src/styles.css` — tokens `@theme` nuevos + bloque `[data-theme="dark"]`.
- `apps/erp-web/index.html` — script inline anti-flash de tema.
- `packages/ui-web/src/index.ts` — exporta `useTheme`, `ThemeToggle`, `Modal`, `DataTable`, `EstadoBadge`.
- `packages/ui-web/src/button/Button.tsx`, `card/Card.tsx`, `badge/Badge.tsx` — tokens nuevos.
- `apps/erp-web/src/shared/layout/AppShell.tsx` — tokens nuevos + `ThemeToggle` en el header.
- `apps/erp-web/src/shared/pages/ModulePlaceholderPage.tsx` — tokens nuevos.
- `apps/erp-web/src/features/auth/pages/LoginPage.tsx`, `features/auth/components/LoginForm.tsx` — tokens nuevos.
- `apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx` — tokens nuevos.
- `apps/erp-web/src/features/organizacion/pages/OrganizationPage.tsx` — tokens nuevos.
- `apps/erp-web/src/features/catalogo/pages/{CategoriasPage,MarcasPage,CatalogPage}.tsx`, `features/catalogo/components/{CategoriaForm,MarcaForm,FormField,Pagination}.tsx` — tokens nuevos + imports de `Modal`/`DataTable`/`EstadoBadge` apuntando a `@boticas/ui-web`.
- `apps/erp-web/src/features/seguridad/pages/*.tsx`, `features/seguridad/components/{FormField,RolForm,PermisosChecklist,UsuarioForm,IdentidadExternaForm,CredencialLocalForm,ConfirmActionDialog,AsignarRolDialog}.tsx` — tokens nuevos + imports de `Modal`/`DataTable`/`EstadoBadge` apuntando a `@boticas/ui-web`.

**Eliminados:**
- `apps/erp-web/src/features/catalogo/components/{Modal,DataTable,EstadoBadge}.tsx`
- `apps/erp-web/src/features/seguridad/components/{Modal,DataTable,EstadoBadge}.tsx` + sus 3 `.test.tsx`
- `apps/erp-web/src/features/catalogo/components/DataTable.test.tsx`

---

## Mapeo de reemplazo de clases Tailwind (usar en todas las tareas de migración)

| Clase vieja | Clase nueva |
|---|---|
| `teal-50`…`teal-950` | `primary-50`…`primary-950` |
| `slate-50`…`slate-950` | `neutral-50`…`neutral-950` |
| `blue-50`…`blue-950` (uso informativo) | `secondary-50`…`secondary-950` |
| `emerald-50`…`emerald-950` (uso de éxito/positivo) | `success-50`…`success-950` |
| `amber-50`…`amber-950` (uso de advertencia) | `warning-50`…`warning-950` |
| `rose-50`…`rose-950` (uso de error/destructivo) | `danger-50`…`danger-950` |

No hay reasignación de peldaño (50 sigue siendo 50, 700 sigue siendo 700): solo cambia el nombre de familia. Esto mantiene el contraste relativo ya validado visualmente en el scaffold actual.

---

### Task 1: Design tokens en `@theme` + dark mode base

**Files:**
- Modify: `apps/erp-web/src/styles.css`
- Modify: `apps/erp-web/index.html`

**Interfaces:**
- Produces: clases utilitarias Tailwind `bg-primary-{50..950}`, `text-primary-{50..950}`, `border-primary-{50..950}` (ídem para `secondary`, `success`, `neutral`, `warning`, `danger`); atributo `data-theme="light"|"dark"` en `<html>`.

- [ ] **Step 1: Leer el `index.html` actual**

Run: `cat apps/erp-web/index.html` (o abrir con Read) para confirmar dónde insertar el script inline antes de `<div id="root">`.

- [ ] **Step 2: Reemplazar los tokens `@theme` en `styles.css`**

Reemplazar el contenido completo de `apps/erp-web/src/styles.css`:

```css
@import 'tailwindcss';
@import '@boticas/ui-web/styles.css';
@source '../../../packages/ui-web/src';

@theme {
  --font-sans:
    Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

  --color-primary-50: #eff6ff;
  --color-primary-100: #dbeafe;
  --color-primary-200: #bfdbfe;
  --color-primary-300: #93c5fd;
  --color-primary-400: #60a5fa;
  --color-primary-500: #3b82f6;
  --color-primary-600: #2563eb;
  --color-primary-700: #1d4ed8;
  --color-primary-800: #1e40af;
  --color-primary-900: #1e3a8a;
  --color-primary-950: #172554;

  --color-secondary-50: #ecfeff;
  --color-secondary-100: #cffafe;
  --color-secondary-200: #a5f3fc;
  --color-secondary-300: #67e8f9;
  --color-secondary-400: #22d3ee;
  --color-secondary-500: #06b6d4;
  --color-secondary-600: #0891b2;
  --color-secondary-700: #0e7490;
  --color-secondary-800: #155e75;
  --color-secondary-900: #164e63;
  --color-secondary-950: #083344;

  --color-success-50: #ecfdf5;
  --color-success-100: #d1fae5;
  --color-success-200: #a7f3d0;
  --color-success-300: #6ee7b7;
  --color-success-400: #34d399;
  --color-success-500: #10b981;
  --color-success-600: #059669;
  --color-success-700: #047857;
  --color-success-800: #065f46;
  --color-success-900: #064e3b;
  --color-success-950: #022c22;

  --color-neutral-50: #f8fafc;
  --color-neutral-100: #f1f5f9;
  --color-neutral-200: #e2e8f0;
  --color-neutral-300: #cbd5e1;
  --color-neutral-400: #94a3b8;
  --color-neutral-500: #64748b;
  --color-neutral-600: #475569;
  --color-neutral-700: #334155;
  --color-neutral-800: #1e293b;
  --color-neutral-900: #0f172a;
  --color-neutral-950: #020617;

  --color-warning-50: #fffbeb;
  --color-warning-100: #fef3c7;
  --color-warning-200: #fde68a;
  --color-warning-300: #fcd34d;
  --color-warning-400: #fbbf24;
  --color-warning-500: #f59e0b;
  --color-warning-600: #d97706;
  --color-warning-700: #b45309;
  --color-warning-800: #92400e;
  --color-warning-900: #78350f;
  --color-warning-950: #451a03;

  --color-danger-50: #fef2f2;
  --color-danger-100: #fee2e2;
  --color-danger-200: #fecaca;
  --color-danger-300: #fca5a5;
  --color-danger-400: #f87171;
  --color-danger-500: #ef4444;
  --color-danger-600: #dc2626;
  --color-danger-700: #b91c1c;
  --color-danger-800: #991b1b;
  --color-danger-900: #7f1d1d;
  --color-danger-950: #450a0a;
}

@layer base {
  * {
    box-sizing: border-box;
  }

  html {
    min-width: 320px;
    background: var(--color-neutral-50);
  }

  html[data-theme='dark'] {
    background: var(--color-neutral-950);
    color-scheme: dark;
  }

  body {
    margin: 0;
    min-width: 320px;
    min-height: 100vh;
    font-family: var(--font-sans);
    text-rendering: optimizeLegibility;
    -webkit-font-smoothing: antialiased;
  }

  button,
  input {
    font: inherit;
  }
}
```

- [ ] **Step 3: Insertar script anti-flash en `index.html`**

Editar `apps/erp-web/index.html`: insertar este `<script>` como el primer hijo de `<head>` (antes de cualquier `<link>` de estilos), para fijar `data-theme` antes del primer paint:

```html
<script>
  (function () {
    try {
      var stored = localStorage.getItem('theme');
      var theme =
        stored === 'light' || stored === 'dark'
          ? stored
          : window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
      document.documentElement.setAttribute('data-theme', theme);
    } catch (e) {
      document.documentElement.setAttribute('data-theme', 'light');
    }
  })();
</script>
```

- [ ] **Step 4: Verificar que el build de Tailwind reconoce los tokens**

Run: `pnpm --filter @boticas/erp-web build`
Expected: build exitoso, sin errores de Tailwind sobre clases desconocidas (en esta etapa nada las usa todavía, así que solo debe compilar limpio).

- [ ] **Step 5: Commit**

```bash
git add apps/erp-web/src/styles.css apps/erp-web/index.html
git commit -m "feat(frontend): definir design tokens de color y base de dark mode"
```

---

### Task 2: Hook `useTheme` + componente `ThemeToggle` en `ui-web`

**Files:**
- Create: `packages/ui-web/src/theme/useTheme.ts`
- Create: `packages/ui-web/src/theme/ThemeToggle.tsx`
- Create: `packages/ui-web/src/theme/ThemeToggle.test.tsx`
- Modify: `packages/ui-web/src/index.ts`

**Interfaces:**
- Consumes: `cn` desde `../lib/cn` (`packages/ui-web/src/lib/cn.ts:1`, firma `cn(...classes: Array<string | false | null | undefined>): string`).
- Produces: `useTheme(): { theme: 'light' | 'dark'; setTheme: (theme: 'light' | 'dark') => void }`; `ThemeToggle(props: ComponentPropsWithRef<'button'>): JSX.Element`.

- [ ] **Step 1: Escribir el test de `ThemeToggle` (falla primero)**

Crear `packages/ui-web/src/theme/ThemeToggle.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeToggle } from './ThemeToggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.setAttribute('data-theme', 'light');
  });

  it('inicia mostrando el tema actual del documento', () => {
    document.documentElement.setAttribute('data-theme', 'dark');
    render(<ThemeToggle />);
    expect(screen.getByRole('button', { name: /modo claro/i })).toBeInTheDocument();
  });

  it('alterna a oscuro, actualiza el atributo del documento y persiste en localStorage', async () => {
    const user = userEvent.setup();
    render(<ThemeToggle />);

    await user.click(screen.getByRole('button', { name: /modo oscuro/i }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(screen.getByRole('button', { name: /modo claro/i })).toBeInTheDocument();
  });

  it('alterna de vuelta a claro en un segundo clic', async () => {
    const user = userEvent.setup();
    render(<ThemeToggle />);

    await user.click(screen.getByRole('button', { name: /modo oscuro/i }));
    await user.click(screen.getByRole('button', { name: /modo claro/i }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('theme')).toBe('light');
  });
});
```

También añadir `vi` al import de test-setup si el proyecto no lo expone global: revisar `packages/ui-web/src/test-setup.ts` — si `vi`/`describe`/`it`/`expect` ya son globals (Vitest `globals: true` en config), quitar el import explícito de `vi` del bloque anterior y usar los globals, igual que hace `Button.test.tsx`. Confirmar leyendo `packages/ui-web/src/button/Button.test.tsx:1` (no importa `describe`/`it`/`expect` de vitest) antes de escribir este archivo, y replicar el mismo estilo (solo imports de `@testing-library/*`).

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm --filter @boticas/ui-web test -- src/theme/ThemeToggle.test.tsx`
Expected: FAIL — `Cannot find module './ThemeToggle'`.

- [ ] **Step 3: Implementar `useTheme`**

Crear `packages/ui-web/src/theme/useTheme.ts`:

```ts
import { useCallback, useState } from 'react';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

function currentDocumentTheme(): Theme {
  const attr = document.documentElement.getAttribute('data-theme');
  return attr === 'dark' ? 'dark' : 'light';
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(currentDocumentTheme);

  const setTheme = useCallback((next: Theme) => {
    document.documentElement.setAttribute('data-theme', next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // almacenamiento no disponible (modo privado, etc.) — el tema sigue aplicado en el DOM
    }
    setThemeState(next);
  }, []);

  return { theme, setTheme };
}
```

- [ ] **Step 4: Implementar `ThemeToggle`**

Crear `packages/ui-web/src/theme/ThemeToggle.tsx`:

```tsx
import type { ComponentPropsWithRef } from 'react';
import { Moon, Sun } from 'lucide-react';
import { cn } from '../lib/cn';
import { useTheme } from './useTheme';

export type ThemeToggleProps = Omit<ComponentPropsWithRef<'button'>, 'onClick' | 'children'>;

export function ThemeToggle({ className, ref, ...props }: ThemeToggleProps) {
  const { theme, setTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      ref={ref}
      type="button"
      aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
      onClick={() => setTheme(isDark ? 'light' : 'dark')}
      className={cn(
        'grid size-10 place-items-center rounded-xl text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-700 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100',
        className
      )}
      {...props}
    >
      {isDark ? <Sun className="size-4.5" aria-hidden="true" /> : <Moon className="size-4.5" aria-hidden="true" />}
    </button>
  );
}
```

Nota: el test busca el botón por `name: /modo claro/i` cuando el tema actual es oscuro (el label anuncia la acción "cambiar a modo claro"), y `/modo oscuro/i` cuando el tema es claro — coincide con `aria-label` de arriba.

`lucide-react` ya es dependencia del workspace (usado en `Modal`/`AppShell`); si `packages/ui-web/package.json` no lo tiene como dependencia directa, añadirlo:

Run: `cat packages/ui-web/package.json` para verificar. Si falta `lucide-react`, ejecutar:

Run: `pnpm --filter @boticas/ui-web add lucide-react`

- [ ] **Step 5: Exportar desde `index.ts`**

Editar `packages/ui-web/src/index.ts`:

```ts
export { Badge, type BadgeProps } from './badge/Badge';
export { Button, type ButtonProps } from './button/Button';
export { Card } from './card/Card';
export { cn } from './lib/cn';
export { ThemeToggle, type ThemeToggleProps } from './theme/ThemeToggle';
export { useTheme, type Theme } from './theme/useTheme';
```

- [ ] **Step 6: Ejecutar el test para confirmar que pasa**

Run: `pnpm --filter @boticas/ui-web test -- src/theme/ThemeToggle.test.tsx`
Expected: PASS — 3 tests.

- [ ] **Step 7: Commit**

```bash
git add packages/ui-web/src/theme packages/ui-web/src/index.ts packages/ui-web/package.json
git commit -m "feat(ui-web): agregar useTheme y ThemeToggle para dark mode manual"
```

---

### Task 3: Migrar `Button`, `Card`, `Badge` a los nuevos tokens (con soporte dark)

**Files:**
- Modify: `packages/ui-web/src/button/Button.tsx`
- Modify: `packages/ui-web/src/card/Card.tsx`
- Modify: `packages/ui-web/src/badge/Badge.tsx`
- Test (ya existentes, no requieren nuevos casos): `packages/ui-web/src/button/Button.test.tsx`

**Interfaces:**
- Consumes: tokens `primary-*`, `neutral-*`, `success-*`, `warning-*`, `danger-*` de Task 1.
- Produces: `Button` con variantes `'primary' | 'secondary' | 'ghost'` (sin cambio de firma pública); `Card` sin props nuevas; `Badge` con tonos `'neutral' | 'success' | 'warning' | 'danger'` (sin cambio de firma pública).

- [ ] **Step 1: Migrar `Button.tsx`**

Reemplazar el objeto `variants` en `packages/ui-web/src/button/Button.tsx:12-18`:

```tsx
const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-primary-600 text-white shadow-sm hover:bg-primary-700 focus-visible:outline-primary-600 disabled:bg-neutral-300 dark:disabled:bg-neutral-700',
  secondary:
    'border border-neutral-200 bg-white text-neutral-700 shadow-sm hover:border-neutral-300 hover:bg-neutral-50 focus-visible:outline-neutral-500 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800',
  ghost:
    'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900 focus-visible:outline-neutral-500 dark:text-neutral-300 dark:hover:bg-neutral-800 dark:hover:text-white'
};
```

- [ ] **Step 2: Migrar `Card.tsx`**

Reemplazar la línea de `className` en `packages/ui-web/src/card/Card.tsx:7`:

```tsx
      className={cn(
        'rounded-2xl border border-neutral-200/80 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900',
        className
      )}
```

- [ ] **Step 3: Migrar `Badge.tsx`**

Reemplazar el objeto `tones` en `packages/ui-web/src/badge/Badge.tsx:6-11`:

```tsx
const tones: Record<BadgeTone, string> = {
  neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-200',
  success: 'bg-success-50 text-success-700 dark:bg-success-900/40 dark:text-success-300',
  warning: 'bg-warning-50 text-warning-800 dark:bg-warning-900/40 dark:text-warning-300',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-900/40 dark:text-danger-300'
};
```

- [ ] **Step 4: Ejecutar los tests de `ui-web`**

Run: `pnpm --filter @boticas/ui-web test`
Expected: PASS — `Button.test.tsx` y `ThemeToggle.test.tsx` en verde (no aseveran clases de color, siguen pasando).

- [ ] **Step 5: Commit**

```bash
git add packages/ui-web/src/button/Button.tsx packages/ui-web/src/card/Card.tsx packages/ui-web/src/badge/Badge.tsx
git commit -m "feat(ui-web): migrar Button, Card y Badge a los nuevos design tokens"
```

---

### Task 4: Unificar `Modal` en `ui-web`

**Files:**
- Create: `packages/ui-web/src/modal/Modal.tsx`
- Create: `packages/ui-web/src/modal/Modal.test.tsx`
- Modify: `packages/ui-web/src/index.ts`
- Modify: `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`, `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- Modify: `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`, `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`, `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`, `apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.tsx`, `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`
- Delete: `apps/erp-web/src/features/catalogo/components/Modal.tsx`, `apps/erp-web/src/features/seguridad/components/Modal.tsx`, `apps/erp-web/src/features/seguridad/components/Modal.test.tsx`

**Interfaces:**
- Produces: `Modal({ open, onClose, title, children }: ModalProps)` donde `ModalProps = PropsWithChildren<{ open: boolean; onClose: () => void; title: string }>` — idéntica firma a las dos versiones actuales, así que ningún call site cambia su forma de invocación, solo el import.

- [ ] **Step 1: Copiar el test existente al nuevo módulo**

Crear `packages/ui-web/src/modal/Modal.test.tsx` con el contenido exacto de `apps/erp-web/src/features/seguridad/components/Modal.test.tsx` (mostrado arriba en la exploración), cambiando únicamente la línea de import:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

describe('Modal', () => {
  it('no renderiza nada cuando open es false', () => {
    render(
      <Modal open={false} onClose={() => {}} title="Título">
        <p>Contenido</p>
      </Modal>
    );
    expect(screen.queryByText('Contenido')).not.toBeInTheDocument();
  });

  it('renderiza el título y el contenido cuando open es true', () => {
    render(
      <Modal open onClose={() => {}} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    expect(screen.getByRole('heading', { name: 'Nuevo usuario' })).toBeInTheDocument();
    expect(screen.getByText('Formulario')).toBeInTheDocument();
  });

  it('llama a onClose al hacer clic en cerrar', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Nuevo usuario">
        <p>Formulario</p>
      </Modal>
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm --filter @boticas/ui-web test -- src/modal/Modal.test.tsx`
Expected: FAIL — `Cannot find module './Modal'`.

- [ ] **Step 3: Implementar `Modal.tsx` con tokens nuevos**

Crear `packages/ui-web/src/modal/Modal.tsx`:

```tsx
import type { PropsWithChildren } from 'react';
import { X } from 'lucide-react';

export type ModalProps = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title: string;
}>;

export function Modal({ open, onClose, title, children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-950/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl dark:bg-neutral-900"
      >
        <div className="flex items-center justify-between">
          <h2 id="modal-title" className="text-lg font-bold text-neutral-950 dark:text-white">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="grid size-8 place-items-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
          >
            <X className="size-4.5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Exportar desde `index.ts`**

Añadir a `packages/ui-web/src/index.ts`:

```ts
export { Modal, type ModalProps } from './modal/Modal';
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm --filter @boticas/ui-web test -- src/modal/Modal.test.tsx`
Expected: PASS — 3 tests.

- [ ] **Step 6: Actualizar los 7 call sites y borrar los duplicados**

En cada uno de estos archivos, reemplazar el import `import { Modal } from '../components/Modal';` (o `'./Modal'` en `seguridad/components/ConfirmActionDialog.tsx` y `seguridad/components/AsignarRolDialog.tsx`) por `import { Modal } from '@boticas/ui-web';` — o, si el archivo ya importa otras cosas de `@boticas/ui-web`, añadir `Modal` a esa lista de nombres importados en vez de una línea de import separada:

- `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`
- `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`
- `apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.tsx`
- `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`

Luego borrar:

Run: `rm apps/erp-web/src/features/catalogo/components/Modal.tsx apps/erp-web/src/features/seguridad/components/Modal.tsx apps/erp-web/src/features/seguridad/components/Modal.test.tsx`

- [ ] **Step 7: Verificar typecheck y tests del app**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores (ningún import roto).

Run: `pnpm --filter @boticas/erp-web test`
Expected: PASS — todos los tests existentes de `seguridad`/`catalogo` que rendericen un Modal (`ConfirmActionDialog.test.tsx`, `AsignarRolDialog.test.tsx`, `UsersPage.test.tsx`, etc.) siguen en verde, ya que la API pública no cambió.

- [ ] **Step 8: Commit**

```bash
git add packages/ui-web/src/modal packages/ui-web/src/index.ts apps/erp-web/src/features/catalogo apps/erp-web/src/features/seguridad
git commit -m "refactor(frontend): unificar Modal duplicado de catalogo y seguridad en ui-web"
```

---

### Task 5: Unificar `DataTable` en `ui-web`

**Files:**
- Create: `packages/ui-web/src/data-table/DataTable.tsx`
- Create: `packages/ui-web/src/data-table/DataTable.test.tsx`
- Modify: `packages/ui-web/src/index.ts`
- Modify: `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`, `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- Modify: `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`, `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`, `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`, `apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`
- Delete: `apps/erp-web/src/features/catalogo/components/DataTable.tsx`, `apps/erp-web/src/features/catalogo/components/DataTable.test.tsx`, `apps/erp-web/src/features/seguridad/components/DataTable.tsx`, `apps/erp-web/src/features/seguridad/components/DataTable.test.tsx`

**Interfaces:**
- Consumes: `Card` desde `../card/Card` (`packages/ui-web/src/card/Card.tsx`, ya migrado en Task 3).
- Produces: `DataTable<T>({ columns, rows, rowKey, emptyMessage, isLoading?, isError?, errorMessage?, startIndex? })` donde `DataTableColumn<T> = { header: string; cell: (row: T, index: number) => ReactNode }`. `startIndex` es opcional (default `0`); llamadas que hoy definen `cell: (row) => ...` sin segundo parámetro (patrón de `seguridad`) siguen siendo válidas en TypeScript porque un callback con menos parámetros es asignable a un tipo de función con más.

La diferencia real entre las dos versiones actuales es solo esa: `catalogo/DataTable.tsx` pasa `startIndex + index` a `cell` y expone `startIndex`; `seguridad/DataTable.tsx` no. La versión unificada usa la superficie de `catalogo` (superset) sin remover nada que `seguridad` necesite.

- [ ] **Step 1: Escribir el test unificado (fusión de ambos, falla primero)**

Crear `packages/ui-web/src/data-table/DataTable.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { DataTable } from './DataTable';

type Row = { id: string; nombre: string };

const rows: Row[] = [
  { id: 'a', nombre: 'Alfa' },
  { id: 'b', nombre: 'Beta' }
];

describe('DataTable', () => {
  it('renderiza una fila por cada elemento', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('Alfa')).toBeInTheDocument();
    expect(screen.getByText('Beta')).toBeInTheDocument();
  });

  it('muestra el mensaje vacio cuando no hay filas', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('Sin filas.')).toBeInTheDocument();
  });

  it('muestra un indicador de carga', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        isLoading
      />
    );
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra un mensaje de error', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'Nombre', cell: (row) => row.nombre }]}
        rows={[]}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        isError
        errorMessage="No se pudo cargar la información."
      />
    );
    expect(screen.getByText('No se pudo cargar la información.')).toBeInTheDocument();
  });

  it('expone el indice de fila a cell para armar una columna N°', () => {
    render(
      <DataTable<Row>
        columns={[
          { header: 'N°', cell: (_row, index) => index + 1 },
          { header: 'Nombre', cell: (row) => row.nombre }
        ]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('aplica startIndex para numerar correctamente en paginas siguientes', () => {
    render(
      <DataTable<Row>
        columns={[{ header: 'N°', cell: (_row, index) => index + 1 }]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
        startIndex={20}
      />
    );
    expect(screen.getByText('21')).toBeInTheDocument();
    expect(screen.getByText('22')).toBeInTheDocument();
  });

  it('permite una columna de Acciones con contenido interactivo por fila', () => {
    render(
      <DataTable<Row>
        columns={[
          { header: 'Nombre', cell: (row) => row.nombre },
          { header: 'Acciones', cell: (row) => <button type="button">Editar {row.nombre}</button> }
        ]}
        rows={rows}
        rowKey={(row) => row.id}
        emptyMessage="Sin filas."
      />
    );
    expect(screen.getByRole('button', { name: 'Editar Alfa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar Beta' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm --filter @boticas/ui-web test -- src/data-table/DataTable.test.tsx`
Expected: FAIL — `Cannot find module './DataTable'`.

- [ ] **Step 3: Implementar `DataTable.tsx` con tokens nuevos**

Crear `packages/ui-web/src/data-table/DataTable.tsx`:

```tsx
import type { ReactNode } from 'react';
import { Card } from '../card/Card';

export type DataTableColumn<T> = {
  header: string;
  cell: (row: T, index: number) => ReactNode;
};

export type DataTableProps<T> = {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  emptyMessage: string;
  isLoading?: boolean;
  isError?: boolean;
  errorMessage?: string;
  startIndex?: number;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage,
  isLoading = false,
  isError = false,
  errorMessage = 'No se pudo cargar la información.',
  startIndex = 0
}: DataTableProps<T>) {
  return (
    <Card className="overflow-hidden">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-neutral-100 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-800/60">
          <tr>
            {columns.map((column) => (
              <th
                key={column.header}
                className="px-4 py-3 font-semibold text-neutral-600 dark:text-neutral-300"
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
          {isLoading ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-6 text-center text-neutral-500 dark:text-neutral-400"
              >
                Cargando…
              </td>
            </tr>
          ) : null}
          {!isLoading && isError ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-6 text-center text-danger-700 dark:text-danger-400"
              >
                {errorMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError && rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-6 text-center text-neutral-500 dark:text-neutral-400"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : null}
          {!isLoading && !isError
            ? rows.map((row, index) => (
                <tr key={rowKey(row)} className="hover:bg-neutral-50 dark:hover:bg-neutral-800/40">
                  {columns.map((column) => (
                    <td
                      key={column.header}
                      className="px-4 py-3 text-neutral-700 dark:text-neutral-200"
                    >
                      {column.cell(row, startIndex + index)}
                    </td>
                  ))}
                </tr>
              ))
            : null}
        </tbody>
      </table>
    </Card>
  );
}
```

- [ ] **Step 4: Exportar desde `index.ts`**

Añadir a `packages/ui-web/src/index.ts`:

```ts
export { DataTable, type DataTableColumn, type DataTableProps } from './data-table/DataTable';
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm --filter @boticas/ui-web test -- src/data-table/DataTable.test.tsx`
Expected: PASS — 7 tests.

- [ ] **Step 6: Actualizar los 6 call sites y borrar los duplicados**

En cada uno de estos archivos, reemplazar el import de `DataTable` desde `'../components/DataTable'` por `'@boticas/ui-web'` (o añadir `DataTable` a un import existente de `@boticas/ui-web`):

- `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`
- `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`

En `seguridad/pages/PermissionsPage.tsx`, si sus `cell` callbacks se definían como `(row) => ...` sin segundo parámetro, no requieren cambio: siguen siendo compatibles con el tipo unificado.

Luego borrar:

Run: `rm apps/erp-web/src/features/catalogo/components/DataTable.tsx apps/erp-web/src/features/catalogo/components/DataTable.test.tsx apps/erp-web/src/features/seguridad/components/DataTable.tsx apps/erp-web/src/features/seguridad/components/DataTable.test.tsx`

- [ ] **Step 7: Verificar typecheck y tests del app**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

Run: `pnpm --filter @boticas/erp-web test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add packages/ui-web/src/data-table packages/ui-web/src/index.ts apps/erp-web/src/features/catalogo apps/erp-web/src/features/seguridad
git commit -m "refactor(frontend): unificar DataTable duplicado de catalogo y seguridad en ui-web"
```

---

### Task 6: Unificar `EstadoBadge` en `ui-web`

**Files:**
- Create: `packages/ui-web/src/estado-badge/EstadoBadge.tsx`
- Create: `packages/ui-web/src/estado-badge/EstadoBadge.test.tsx`
- Modify: `packages/ui-web/src/index.ts`
- Modify: `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`, `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- Modify: `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`, `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`, `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`, `apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx`, `apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`
- Delete: `apps/erp-web/src/features/catalogo/components/EstadoBadge.tsx`, `apps/erp-web/src/features/seguridad/components/EstadoBadge.tsx`, `apps/erp-web/src/features/seguridad/components/EstadoBadge.test.tsx`

**Interfaces:**
- Consumes: `Badge` desde `../badge/Badge` (`packages/ui-web/src/badge/Badge.tsx`, ya migrado en Task 3), con tonos `'neutral' | 'success' | 'warning' | 'danger'`.
- Produces: `EstadoBadge({ status }: { status: string })`. Usa el set de estados de `seguridad` (superset de `catalogo`): `SUCCESS = {ACTIVO, ACTIVE, CONFIABLE, TRUSTED}`, `DANGER = {INACTIVO, INACTIVE, REVOCADO, REVOKED, BLOQUEADO, BLOCKED}`, `WARNING = {PENDIENTE, PENDING, SUSPENDIDO, SUSPENDED}`, resto → `neutral`. `catalogo` solo usaba `ACTIVO`/`INACTIVO`, ambos cubiertos.

- [ ] **Step 1: Escribir el test (adaptado del de `seguridad`, con las clases nuevas, falla primero)**

Crear `packages/ui-web/src/estado-badge/EstadoBadge.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { EstadoBadge } from './EstadoBadge';

describe('EstadoBadge', () => {
  it('muestra tono success para estados activos', () => {
    render(<EstadoBadge status="ACTIVO" />);
    const badge = screen.getByText('ACTIVO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-success-50', 'text-success-700');
  });

  it('muestra tono danger para estados bloqueados o revocados', () => {
    render(<EstadoBadge status="BLOQUEADO" />);
    const badge = screen.getByText('BLOQUEADO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-danger-50', 'text-danger-700');
  });

  it('muestra tono warning para estados pendientes', () => {
    render(<EstadoBadge status="PENDIENTE" />);
    const badge = screen.getByText('PENDIENTE');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-warning-50', 'text-warning-800');
  });

  it('muestra tono neutral para estados no reconocidos', () => {
    render(<EstadoBadge status="DESCONOCIDO" />);
    const badge = screen.getByText('DESCONOCIDO');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-neutral-100', 'text-neutral-700');
  });
});
```

- [ ] **Step 2: Ejecutar el test para confirmar que falla**

Run: `pnpm --filter @boticas/ui-web test -- src/estado-badge/EstadoBadge.test.tsx`
Expected: FAIL — `Cannot find module './EstadoBadge'`.

- [ ] **Step 3: Implementar `EstadoBadge.tsx`**

Crear `packages/ui-web/src/estado-badge/EstadoBadge.tsx`:

```tsx
import { Badge } from '../badge/Badge';

const SUCCESS_STATUSES = new Set(['ACTIVO', 'ACTIVE', 'CONFIABLE', 'TRUSTED']);
const DANGER_STATUSES = new Set(['INACTIVO', 'INACTIVE', 'REVOCADO', 'REVOKED', 'BLOQUEADO', 'BLOCKED']);
const WARNING_STATUSES = new Set(['PENDIENTE', 'PENDING', 'SUSPENDIDO', 'SUSPENDED']);

function toneFor(status: string): 'success' | 'danger' | 'warning' | 'neutral' {
  const normalized = status.toUpperCase();
  if (SUCCESS_STATUSES.has(normalized)) return 'success';
  if (DANGER_STATUSES.has(normalized)) return 'danger';
  if (WARNING_STATUSES.has(normalized)) return 'warning';
  return 'neutral';
}

export function EstadoBadge({ status }: { status: string }) {
  return <Badge tone={toneFor(status)}>{status}</Badge>;
}
```

- [ ] **Step 4: Exportar desde `index.ts`**

Añadir a `packages/ui-web/src/index.ts`:

```ts
export { EstadoBadge } from './estado-badge/EstadoBadge';
```

- [ ] **Step 5: Ejecutar el test para confirmar que pasa**

Run: `pnpm --filter @boticas/ui-web test -- src/estado-badge/EstadoBadge.test.tsx`
Expected: PASS — 4 tests.

- [ ] **Step 6: Actualizar los 7 call sites y borrar los duplicados**

En cada uno de estos archivos, reemplazar el import de `EstadoBadge` desde `'../components/EstadoBadge'` por `'@boticas/ui-web'` (o añadir `EstadoBadge` a un import existente):

- `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`
- `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UsersPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/UserDetailPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/RolesPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx`
- `apps/erp-web/src/features/seguridad/pages/PermissionsPage.tsx`

Luego borrar:

Run: `rm apps/erp-web/src/features/catalogo/components/EstadoBadge.tsx apps/erp-web/src/features/seguridad/components/EstadoBadge.tsx apps/erp-web/src/features/seguridad/components/EstadoBadge.test.tsx`

- [ ] **Step 7: Verificar typecheck y tests del app**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

Run: `pnpm --filter @boticas/erp-web test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add packages/ui-web/src/estado-badge packages/ui-web/src/index.ts apps/erp-web/src/features/catalogo apps/erp-web/src/features/seguridad
git commit -m "refactor(frontend): unificar EstadoBadge duplicado de catalogo y seguridad en ui-web"
```

---

### Task 7: Migrar `AppShell` (nav/header) y agregar `ThemeToggle` visible

**Files:**
- Modify: `apps/erp-web/src/shared/layout/AppShell.tsx`

**Interfaces:**
- Consumes: `ThemeToggle` desde `@boticas/ui-web` (Task 2).

- [ ] **Step 1: Aplicar el mapeo de clases y agregar `ThemeToggle`**

En `apps/erp-web/src/shared/layout/AppShell.tsx`:

1. Añadir `ThemeToggle` al import existente de `@boticas/ui-web` (línea 21: `import { Button, cn } from '@boticas/ui-web';` → `import { Button, cn, ThemeToggle } from '@boticas/ui-web';`).
2. Reemplazar cada clase según el mapeo de la sección "Mapeo de reemplazo de clases": `bg-teal-950`→`bg-primary-950`, `text-teal-800`→`text-primary-800`, `text-teal-100/70`→`text-primary-100/70`, `border-white/10`→sin cambio, `text-teal-100/50`→`text-primary-100/50`, `bg-white text-teal-900`→`bg-white text-primary-900`, `text-teal-50/75`→`text-primary-50/75`, `bg-slate-50`→`bg-neutral-50`, `text-slate-900`→`text-neutral-900`, `bg-slate-950/50`→`bg-neutral-950/50`, `text-teal-50`→`text-primary-50`, `border-slate-200/80`→`border-neutral-200/80`, `bg-white/90`→sin cambio, `text-slate-400`→`text-neutral-400`, `border-slate-200`→`border-neutral-200`, `bg-slate-50`→`bg-neutral-50`, `focus:border-teal-600`→`focus:border-primary-600`, `focus:ring-teal-100`→`focus:ring-primary-100`, `bg-rose-500`→`bg-danger-500`, `ring-white`→sin cambio, `hover:bg-slate-50`→`hover:bg-neutral-50`, `bg-teal-100 text-teal-800`→`bg-primary-100 text-primary-800`, `text-slate-800`→`text-neutral-800`, `text-slate-500`→`text-neutral-500`, `text-slate-400`→`text-neutral-400`.
3. Agregar variantes `dark:` a los contenedores de fondo/superficie principales: `min-h-screen bg-neutral-50 text-neutral-900` → `min-h-screen bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100`; el `<header>` (`border-neutral-200/80 bg-white/90 backdrop-blur-xl`) → añadir `dark:border-neutral-800 dark:bg-neutral-900/90`.
4. Ubicar `<ThemeToggle />` dentro del contenedor `<div className="ml-auto flex items-center gap-2">` (línea 140), como primer hijo, antes del botón de notificaciones.

Archivo completo resultante:

```tsx
import {
  Bell,
  Boxes,
  Building2,
  ChevronDown,
  LayoutDashboard,
  Menu,
  MonitorSmartphone,
  PackageSearch,
  ReceiptText,
  Search,
  ShieldCheck,
  ShoppingCart,
  Users,
  WalletCards,
  X,
  type LucideIcon
} from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet } from 'react-router';
import { Button, cn, ThemeToggle } from '@boticas/ui-web';

type NavigationItem = {
  label: string;
  to: string;
  icon: LucideIcon;
};

const navigation: NavigationItem[] = [
  { label: 'Resumen', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Catálogo', to: '/catalogo', icon: PackageSearch },
  { label: 'Inventario', to: '/inventario', icon: Boxes },
  { label: 'Compras', to: '/compras', icon: ShoppingCart },
  { label: 'Ventas', to: '/ventas', icon: ReceiptText },
  { label: 'Punto de venta', to: '/pos', icon: MonitorSmartphone },
  { label: 'Caja', to: '/caja', icon: WalletCards },
  { label: 'Clientes', to: '/clientes', icon: Users },
  { label: 'Seguridad', to: '/seguridad', icon: ShieldCheck },
  { label: 'Organización', to: '/organizacion', icon: Building2 }
];

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <>
      <div className="flex h-20 items-center gap-3 border-b border-white/10 px-6">
        <div className="grid size-10 place-items-center rounded-xl bg-white text-primary-800 shadow-lg shadow-primary-950/20">
          <span className="text-lg font-black">B+</span>
        </div>
        <div>
          <p className="font-bold tracking-tight text-white">ERP Boticas</p>
          <p className="text-xs text-primary-100/70">Gestión farmacéutica</p>
        </div>
      </div>

      <nav aria-label="Navegación principal" className="flex-1 space-y-1 p-4">
        <p className="mb-3 px-3 text-[11px] font-bold tracking-[0.18em] text-primary-100/50 uppercase">
          Operaciones
        </p>
        {navigation.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-white text-primary-900 shadow-sm'
                    : 'text-primary-50/75 hover:bg-white/10 hover:text-white'
                )
              }
            >
              <Icon className="size-4.5" aria-hidden="true" />
              {item.label}
            </NavLink>
          );
        })}
      </nav>

      <div className="m-4 rounded-2xl border border-white/10 bg-white/5 p-4">
        <p className="text-xs font-semibold text-white">Sucursal activa</p>
        <p className="mt-1 text-sm text-primary-50/70">Botica Central · Lima</p>
      </div>
    </>
  );
}

export function AppShell() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col bg-primary-950 lg:flex">
        <SidebarContent />
      </aside>

      {mobileMenuOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-neutral-950/50 backdrop-blur-sm"
            aria-label="Cerrar menú"
            onClick={() => setMobileMenuOpen(false)}
          />
          <aside className="relative flex h-full w-72 flex-col bg-primary-950 shadow-2xl">
            <button
              className="absolute top-5 right-4 rounded-lg p-2 text-primary-50 hover:bg-white/10"
              aria-label="Cerrar menú"
              onClick={() => setMobileMenuOpen(false)}
            >
              <X className="size-5" />
            </button>
            <SidebarContent onNavigate={() => setMobileMenuOpen(false)} />
          </aside>
        </div>
      ) : null}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-20 items-center gap-4 border-b border-neutral-200/80 bg-white/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8 dark:border-neutral-800 dark:bg-neutral-900/90">
          <Button
            variant="ghost"
            size="sm"
            className="px-2 lg:hidden"
            aria-label="Abrir menú"
            onClick={() => setMobileMenuOpen(true)}
          >
            <Menu className="size-5" />
          </Button>

          <div className="relative hidden max-w-md flex-1 md:block">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-neutral-400" />
            <input
              type="search"
              placeholder="Buscar productos, clientes o ventas..."
              className="h-11 w-full rounded-xl border border-neutral-200 bg-neutral-50 pr-4 pl-10 text-sm outline-none placeholder:text-neutral-400 focus:border-primary-600 focus:ring-3 focus:ring-primary-100 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100 dark:placeholder:text-neutral-500"
            />
          </div>

          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <Button variant="ghost" size="sm" className="relative px-2" aria-label="Notificaciones">
              <Bell className="size-5" />
              <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-danger-500 ring-2 ring-white dark:ring-neutral-900" />
            </Button>
            <button className="flex items-center gap-3 rounded-xl p-1.5 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800">
              <div className="grid size-9 place-items-center rounded-xl bg-primary-100 text-sm font-bold text-primary-800 dark:bg-primary-900/50 dark:text-primary-200">
                MR
              </div>
              <div className="hidden sm:block">
                <p className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">María Rojas</p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400">Administradora</p>
              </div>
              <ChevronDown className="hidden size-4 text-neutral-400 sm:block" />
            </button>
          </div>
        </header>

        <main className="p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/shared/layout/AppShell.tsx
git commit -m "feat(frontend): migrar AppShell a los nuevos tokens y agregar ThemeToggle"
```

---

### Task 8: Migrar `ModulePlaceholderPage` (cubre compras/ventas/pos/caja/clientes/inventario)

**Files:**
- Modify: `apps/erp-web/src/shared/pages/ModulePlaceholderPage.tsx`

- [ ] **Step 1: Aplicar el mapeo de clases**

Reemplazar el contenido de `apps/erp-web/src/shared/pages/ModulePlaceholderPage.tsx`:

```tsx
import { ArrowLeft, Construction } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function ModulePlaceholderPage({
  description,
  title
}: {
  description: string;
  title: string;
}) {
  return (
    <div className="mx-auto max-w-7xl">
      <p className="text-sm font-semibold text-primary-700 dark:text-primary-400">Módulo ERP</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
        {title}
      </h1>
      <Card className="mt-7 grid min-h-80 place-items-center p-8 text-center">
        <div className="max-w-md">
          <div className="mx-auto grid size-14 place-items-center rounded-2xl bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
            <Construction className="size-6" />
          </div>
          <h2 className="mt-5 text-xl font-bold text-neutral-900 dark:text-neutral-50">
            Módulo preparado
          </h2>
          <p className="mt-2 text-sm leading-6 text-neutral-500 dark:text-neutral-400">
            {description}
          </p>
          <Link
            to="/dashboard"
            className="mt-6 inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white px-4 text-sm font-semibold text-neutral-700 shadow-sm transition-colors hover:border-neutral-300 hover:bg-neutral-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-500 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
          >
            <ArrowLeft className="size-4" />
            Volver al resumen
          </Link>
        </div>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/shared/pages/ModulePlaceholderPage.tsx
git commit -m "feat(frontend): migrar ModulePlaceholderPage a los nuevos tokens"
```

---

### Task 9: Migrar `auth` (LoginPage + LoginForm)

**Files:**
- Modify: `apps/erp-web/src/features/auth/pages/LoginPage.tsx`
- Modify: `apps/erp-web/src/features/auth/components/LoginForm.tsx`

- [ ] **Step 1: Migrar `LoginPage.tsx`**

Aplicar el mapeo de clases sobre el archivo mostrado en la exploración (`apps/erp-web/src/features/auth/pages/LoginPage.tsx`). Cambios línea por línea:

- Línea 43: `bg-[#f4f7f5]` → sin cambio (color de fondo decorativo fuera de la paleta semántica; se mantiene, no es un token con significado de estado).
- Línea 44: `bg-teal-950` → `bg-primary-950`.
- Línea 56: `bg-amber-300 text-teal-950 shadow-lg shadow-teal-950/30` → `bg-warning-300 text-primary-950 shadow-lg shadow-primary-950/30`.
- Línea 61: `text-teal-100/65` → `text-primary-100/65`.
- Línea 66: `border-teal-300/20`→`border-primary-300/20`; `text-teal-50`→`text-primary-50`.
- Línea 67: `text-amber-300`→`text-warning-300`.
- Línea 73: `text-teal-50/70`→`text-primary-50/70`.
- Línea 82: `text-teal-50`→`text-primary-50`.
- Línea 84: `bg-amber-300 text-teal-950`→`bg-warning-300 text-primary-950`.
- Línea 94, 99, 104: `text-amber-300`→`text-warning-300`.
- Línea 96, 101, 106: `text-teal-50/60`→`text-primary-50/60`.
- Línea 111: `text-teal-50/50`→`text-primary-50/50`.
- Línea 122: `bg-teal-900 text-amber-300`→`bg-primary-900 text-warning-300`.
- Línea 126: `text-slate-950`→`text-neutral-950 dark:text-white`.
- Línea 127: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 132: `bg-teal-100 text-teal-800`→`bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300`.
- Línea 135: `text-teal-700`→`text-primary-700 dark:text-primary-400`.
- Línea 136: `text-slate-950`→`text-neutral-950 dark:text-white`.
- Línea 139: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 146: `border-slate-200/70`→`border-neutral-200/70 dark:border-neutral-800`.
- Línea 146: `text-slate-400`→`text-neutral-400 dark:text-neutral-500`.
- Línea 43 (el `<main>`) y línea 120 (el `<section>` derecho): añadir `dark:bg-neutral-950` al `<main>` para que el panel derecho (blanco por defecto) tenga fondo oscuro coherente.

El bloque derecho (`<section>` de línea 120) no tiene `bg-white` explícito hoy (hereda el fondo de `<main>`); al añadir `dark:bg-neutral-950` en `<main>`, ambas mitades quedan oscuras salvo el panel izquierdo que ya es `bg-primary-950` en ambos modos (mantiene su identidad de marca).

- [ ] **Step 2: Migrar `LoginForm.tsx`**

Aplicar el mapeo de clases sobre `apps/erp-web/src/features/auth/components/LoginForm.tsx`:

- Línea 46: `border-rose-200 bg-rose-50 text-rose-700`→`border-danger-200 bg-danger-50 text-danger-700 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300`.
- Línea 52: `text-slate-700`→`text-neutral-700 dark:text-neutral-200`.
- Línea 57: `text-slate-400`→`text-neutral-400`.
- Línea 68: `border-slate-200 bg-white text-slate-900`→`border-neutral-200 bg-white text-neutral-900 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100`; `placeholder:text-slate-400`→`placeholder:text-neutral-400 dark:placeholder:text-neutral-500`; `hover:border-slate-300`→`hover:border-neutral-300 dark:hover:border-neutral-600`; `focus:border-teal-600`→`focus:border-primary-600`; `focus:ring-teal-100`→`focus:ring-primary-100 dark:focus:ring-primary-900/40`; `aria-invalid:border-rose-400`→`aria-invalid:border-danger-400`; `aria-invalid:focus:ring-rose-100`→`aria-invalid:focus:ring-danger-100`.
- Línea 73: `text-rose-600`→`text-danger-600 dark:text-danger-400`.
- Línea 81: `text-slate-700`→`text-neutral-700 dark:text-neutral-200`.
- Línea 86: `text-teal-700`→`text-primary-700 dark:text-primary-400`; `hover:text-teal-900`→`hover:text-primary-900 dark:hover:text-primary-200`; `focus-visible:outline-teal-600`→`focus-visible:outline-primary-600`.
- Línea 96: `text-slate-400`→`text-neutral-400`.
- Línea 106: mismo patrón que línea 68.
- Línea 112: `text-slate-400`→`text-neutral-400 dark:text-neutral-500`; `hover:bg-slate-100`→`hover:bg-neutral-100 dark:hover:bg-neutral-800`; `hover:text-slate-700`→`hover:text-neutral-700 dark:hover:text-neutral-100`; `focus-visible:outline-teal-600`→`focus-visible:outline-primary-600`.
- Línea 119: `text-rose-600`→`text-danger-600 dark:text-danger-400`.
- Línea 129: `border-blue-100 bg-blue-50 text-blue-900`→`border-secondary-100 bg-secondary-50 text-secondary-900 dark:border-secondary-800 dark:bg-secondary-900/30 dark:text-secondary-200`.
- Línea 131: `text-blue-600`→`text-secondary-600 dark:text-secondary-400`.
- Línea 137: `text-slate-600`→`text-neutral-600 dark:text-neutral-300`.
- Línea 140: `border-slate-300 text-teal-700`→`border-neutral-300 text-primary-700 dark:border-neutral-600`; `focus:ring-teal-600`→`focus:ring-primary-600`.
- Línea 157: `bg-slate-50 text-slate-500`→`bg-neutral-50 text-neutral-500 dark:bg-neutral-800/50 dark:text-neutral-400`.
- Línea 158: `text-teal-700`→`text-primary-700 dark:text-primary-400`.
- Línea 165: `text-slate-400`→`text-neutral-400`.
- Línea 166: `text-emerald-600`→`text-success-600 dark:text-success-400`.

- [ ] **Step 3: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`

Run: `pnpm --filter @boticas/erp-web test -- src/features/auth`
Expected: PASS — `LoginPage.test.tsx` y `AuthSessionProvider.test.tsx` no aseveran clases CSS de color (verificar al editar; si alguno lo hace, actualizar la aserción al nuevo nombre de clase siguiendo el mismo mapeo).

- [ ] **Step 4: Commit**

```bash
git add apps/erp-web/src/features/auth
git commit -m "feat(frontend): migrar pantallas de auth a los nuevos tokens de color"
```

---

### Task 10: Migrar `dashboard` (DashboardPage)

**Files:**
- Modify: `apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx`

- [ ] **Step 1: Migrar el mapa de tonos y las clases hardcodeadas**

En `apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx`:

Reemplazar `toneClasses` (líneas 24-29):

```tsx
type StatCardProps = {
  label: string;
  value: string;
  detail: string;
  icon: LucideIcon;
  tone: 'primary' | 'secondary' | 'warning' | 'danger';
};

const toneClasses = {
  primary: 'bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300',
  secondary: 'bg-secondary-50 text-secondary-700 dark:bg-secondary-900/40 dark:text-secondary-300',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-900/40 dark:text-warning-300',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-900/40 dark:text-danger-300'
};
```

Y sus 4 usos en las `StatCard` (`tone="teal"`→`tone="primary"`, `tone="blue"`→`tone="secondary"`, `tone="amber"`→`tone="warning"`, `tone="rose"`→`tone="danger"`).

Aplicar el resto del mapeo:

- `text-slate-500`→`text-neutral-500 dark:text-neutral-400` (label de `StatCard`, línea 36 y las demás ocurrencias de `text-slate-500`).
- `text-slate-950`→`text-neutral-950 dark:text-white` (valor de `StatCard`, línea 37, y título h1 línea 67).
- `text-teal-700`→`text-primary-700 dark:text-primary-400` (línea 66, fecha).
- `border-rose-200 bg-rose-50 text-rose-800`→`border-danger-200 bg-danger-50 text-danger-800 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300` (línea 81, card de error).
- `border-slate-100 bg-slate-50`→`border-neutral-100 bg-neutral-800/60 dark:border-neutral-800` en el `<thead>` (línea 137).
- `text-slate-500 uppercase`→`text-neutral-500 dark:text-neutral-400 uppercase` (línea 137).
- `divide-slate-100`→`divide-neutral-100 dark:divide-neutral-800` (línea 145).
- `hover:bg-slate-50/70`→`hover:bg-neutral-50/70 dark:hover:bg-neutral-800/40` (línea 147).
- `text-slate-800`→`text-neutral-800 dark:text-neutral-100` (líneas 148, 150).
- `text-slate-600`→`text-neutral-600 dark:text-neutral-300` (línea 149).
- `text-teal-700`→`text-primary-700 dark:text-primary-400` (línea 169, ícono `PackageCheck`).
- Bloque "Reposición de inventario" (líneas 172-179): `bg-amber-50`→`bg-warning-50 dark:bg-warning-900/30`; `text-amber-700`→`text-warning-700 dark:text-warning-400`; `text-amber-950`→`text-warning-950 dark:text-warning-100`; `text-amber-800`→`text-warning-800 dark:text-warning-300`.
- Bloque "Comprobantes pendientes" (líneas 181-188): `bg-blue-50`→`bg-secondary-50 dark:bg-secondary-900/30`; `text-blue-700`→`text-secondary-700 dark:text-secondary-400`; `text-blue-950`→`text-secondary-950 dark:text-secondary-100`; `text-blue-800`→`text-secondary-800 dark:text-secondary-300`.
- Bloque "Clientes activos" (líneas 190-197): `bg-emerald-50`→`bg-success-50 dark:bg-success-900/30`; `text-emerald-700`→`text-success-700 dark:text-success-400`; `text-emerald-950`→`text-success-950 dark:text-success-100`; `text-emerald-800`→`text-success-800 dark:text-success-300`.
- Título "Ventas recientes" / "Atención requerida" (`text-slate-900`, líneas 128, 166) → `text-neutral-900 dark:text-neutral-50`.
- Subtítulos (`text-slate-500`, líneas 129, 167) → `text-neutral-500 dark:text-neutral-400`.

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`

Run: `pnpm --filter @boticas/erp-web test -- src/features/dashboard`
Expected: PASS — revisar `DashboardPage.test.tsx`; si asevera texto de tono/clase, actualizar al nuevo nombre.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/features/dashboard
git commit -m "feat(frontend): migrar DashboardPage a los nuevos tokens de color"
```

---

### Task 11: Migrar `organizacion` (OrganizationPage)

**Files:**
- Modify: `apps/erp-web/src/features/organizacion/pages/OrganizationPage.tsx`

- [ ] **Step 1: Aplicar el mapeo de clases**

En `apps/erp-web/src/features/organizacion/pages/OrganizationPage.tsx`:

- Línea 41: `text-teal-700`→`text-primary-700 dark:text-primary-400`.
- Línea 42: `text-slate-950`→`text-neutral-950 dark:text-white`.
- Línea 43: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 49: `border-rose-200 bg-rose-50 text-rose-800`→`border-danger-200 bg-danger-50 text-danger-800 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300`.
- Línea 67: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 68: `text-slate-950`→`text-neutral-950 dark:text-white`.
- Línea 70: `bg-teal-50 text-teal-700`→`bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300`.
- Línea 79: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 86: `border-slate-100`→`border-neutral-100 dark:border-neutral-800`.
- Línea 88: `text-slate-950`→`text-neutral-950 dark:text-white`.
- Línea 91: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Línea 96: `divide-slate-100`→`divide-neutral-100 dark:divide-neutral-800`.
- Línea 101: `text-teal-700`→`text-primary-700 dark:text-primary-400`.
- Línea 104: `text-slate-900`→`text-neutral-900 dark:text-neutral-50`.
- Línea 105: `text-slate-500`→`text-neutral-500 dark:text-neutral-400`.
- Líneas 113, 122: `bg-slate-50`→`bg-neutral-50 dark:bg-neutral-800/50`.
- Líneas 114, 123: `text-slate-600`→`text-neutral-600 dark:text-neutral-300`.
- Líneas 117, 126: `text-slate-700`→`text-neutral-700 dark:text-neutral-200`.

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion`
Expected: PASS — revisar `OrganizationPage.test.tsx` por aserciones de clase.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/features/organizacion
git commit -m "feat(frontend): migrar OrganizationPage a los nuevos tokens de color"
```

---

### Task 12: Migrar el resto de `catalogo` (formularios, paginación, `CatalogPage`)

**Files:**
- Modify: `apps/erp-web/src/features/catalogo/pages/CatalogPage.tsx`
- Modify: `apps/erp-web/src/features/catalogo/pages/CategoriasPage.tsx`
- Modify: `apps/erp-web/src/features/catalogo/pages/MarcasPage.tsx`
- Modify: `apps/erp-web/src/features/catalogo/components/CategoriaForm.tsx`
- Modify: `apps/erp-web/src/features/catalogo/components/MarcaForm.tsx`
- Modify: `apps/erp-web/src/features/catalogo/components/FormField.tsx`
- Modify: `apps/erp-web/src/features/catalogo/components/Pagination.tsx`

- [ ] **Step 1: Leer cada archivo y aplicar el mapeo de clases**

Para cada archivo de esta lista: leerlo con la herramienta de lectura, y aplicar mecánicamente el mapeo de la tabla "Mapeo de reemplazo de clases" (sección superior de este plan) sobre toda ocurrencia de `teal-`, `slate-`, `blue-` (uso informativo), `emerald-` (uso de éxito), `amber-` (uso de advertencia), `rose-` (uso de error). Para superficies de fondo/texto/borde que hoy no tienen variante oscura (todas, porque el scaffold no tenía dark mode), añadir el par `dark:` correspondiente siguiendo el mismo patrón usado en las Tasks 7-11:
  - `bg-white`→ mantener + `dark:bg-neutral-900` si es una superficie tipo Card/formulario.
  - `text-neutral-900/950` (post-mapeo)→ añadir `dark:text-white` o `dark:text-neutral-50`.
  - `text-neutral-500/600/700` (post-mapeo)→ añadir `dark:text-neutral-400/300/200` respectivamente (un peldaño más claro en dark).
  - `border-neutral-200/100` (post-mapeo)→ añadir `dark:border-neutral-700/800`.
  - `bg-neutral-50` (post-mapeo)→ añadir `dark:bg-neutral-800/50`.

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`

Run: `pnpm --filter @boticas/erp-web test -- src/features/catalogo`
Expected: PASS — si algún test (`CatalogPage.test.tsx`, `CategoriasPage.test.tsx`, `MarcasPage.test.tsx`, `Pagination.test.tsx`) asevera una clase de color específica, actualizarla al nuevo nombre de token antes de continuar.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/features/catalogo
git commit -m "feat(frontend): migrar formularios y paginas de catalogo a los nuevos tokens"
```

---

### Task 13: Migrar el resto de `seguridad` (formularios, checklist, diálogos)

**Files:**
- Modify: `apps/erp-web/src/features/seguridad/pages/SecurityPage.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/FormField.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/RolForm.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/PermisosChecklist.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/UsuarioForm.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/ConfirmActionDialog.tsx`
- Modify: `apps/erp-web/src/features/seguridad/components/AsignarRolDialog.tsx`

- [ ] **Step 1: Leer cada archivo y aplicar el mismo mapeo mecánico que en Task 12**

Mismo procedimiento: leer cada archivo, aplicar la tabla de mapeo de clases y las reglas de variante `dark:` descritas en Task 12 Step 1. Prestar atención particular a `ConfirmActionDialog.tsx` (probablemente usa `rose-*`→`danger-*` para su variante destructiva, dado que confirma acciones como desactivar/revocar) y a `PermisosChecklist.tsx` (estados de check/activo probablemente usan `teal-*`/`emerald-*`).

- [ ] **Step 2: Verificar**

Run: `pnpm --filter @boticas/erp-web typecheck`

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad`
Expected: PASS — actualizar cualquier aserción de clase de color en los `.test.tsx` de esta feature (`ConfirmActionDialog.test.tsx`, `AsignarRolDialog.test.tsx`, `RolForm.test.tsx`, `UsuarioForm.test.tsx`, `IdentidadExternaForm.test.tsx`, `CredencialLocalForm.test.tsx`, `PermisosChecklist.test.tsx`, `SecurityPage.test.tsx`, `UsersPage.test.tsx`, `UserDetailPage.test.tsx`, `RolesPage.test.tsx`, `PermissionsPage.test.tsx`) al nuevo nombre de token si asevera colores directamente.

- [ ] **Step 3: Commit**

```bash
git add apps/erp-web/src/features/seguridad
git commit -m "feat(frontend): migrar formularios y dialogos de seguridad a los nuevos tokens"
```

---

### Task 14: Verificación final end-to-end

**Files:** ninguno (solo verificación).

- [ ] **Step 1: Ejecutar el pipeline completo**

Run: `pnpm check`
Expected: lint + typecheck + test + build en verde para todo el workspace.

- [ ] **Step 2: Verificación visual manual en navegador**

Run: `pnpm dev` (en background o en otra terminal).

Con el servidor arriba, visitar en el navegador y confirmar visualmente contraste y coherencia de color en modo claro y oscuro (alternando con el `ThemeToggle` del header, y también probando `prefers-color-scheme` del SO):

- `/login`
- `/dashboard`
- `/seguridad/usuarios` y el detalle de un usuario
- `/catalogo/categorias`
- Un placeholder, p. ej. `/compras`

Expected: sin texto ilegible por bajo contraste, sin residuos visuales de `teal`/`slate`/`emerald`/`rose`/`amber`/`blue` fuera del mapeo definido, toggle de tema funcionando y persistiendo tras recargar la página.

- [ ] **Step 3: Búsqueda final de residuos de la paleta vieja**

Run: `grep -rn "teal-\|slate-\|emerald-\|rose-" apps/erp-web/src packages/ui-web/src --include="*.tsx" --include="*.css"`
Expected: sin resultados fuera de comentarios o de `bg-[#f4f7f5]` (decorativo, fuera de paleta semántica, dejado tal cual en Task 9). Si aparecen residuos, migrarlos con el mismo mapeo antes de cerrar la tarea.

- [ ] **Step 4: Commit final si hubo ajustes de la verificación**

Solo si el Step 3 encontró y corrigió residuos:

```bash
git add -A
git commit -m "fix(frontend): eliminar residuos de la paleta de color anterior"
```

---

## Self-Review

**Spec coverage:**
- Tokens semánticos con escalas 50-950 → Task 1.
- Dark mode (SO + toggle manual persistido) → Tasks 1-2, aplicado transversalmente en 7-13.
- `ui-web` (Button/Card/Badge) → Task 3.
- Unificación de Modal/DataTable/EstadoBadge → Tasks 4-6.
- Migración de auth/dashboard/seguridad/catalogo/placeholders/AppShell → Tasks 7-13.
- Verificación (`pnpm check`, visual, contraste) → Task 14.

**Placeholder scan:** sin "TBD"/"TODO"; las tareas 12-13 usan una regla de mapeo mecánico explícita en vez de repetir el mismo mapeo línea por línea para ~15 archivos adicionales sin bifurcaciones reales de comportamiento — la regla en sí es completa y verificable (Task 14 Step 3 la audita con `grep`).

**Type consistency:** `DataTableColumn<T>.cell` firma `(row: T, index: number) => ReactNode` es consistente entre Task 5 (definición) y su uso en Task 6/12/13 (call sites sin cambios de firma). `ModalProps`, `BadgeProps`, `ButtonProps` no cambian de forma pública en ninguna tarea.

**Scope check:** cada tarea produce un estado verificable (`typecheck`/`test` en verde) de forma independiente; Task 14 es la única que depende de que todas las anteriores estén completas.
