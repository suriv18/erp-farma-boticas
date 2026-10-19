# Rediseño de paleta de marca (teal) + extracción de Input — Plan de implementación

> **Para agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** Cambiar `primary` (azul→teal) y `success` (emerald→lime) en `frontend/apps/erp-web/src/styles.css`, eliminar `secondary` migrando sus 2 usos a `neutral`, corregir las fugas de color hardcodeadas de `LoginPage.tsx`, y extraer un componente `Input` compartido en `packages/ui-web` para eliminar la duplicación de markup en 9 formularios — siguiendo `docs/superpowers/specs/2026-09-28-rediseno-paleta-marca-teal-design.md`.

**Architecture:** Cambio de valores de token en un único archivo CSS (`styles.css`) que se propaga automáticamente a toda la app porque ningún componente hardcodea hex. Un componente `Input` nuevo en `packages/ui-web` (mismo patrón que `Button`/`FormField`: props tipadas + `ref` + Tailwind). Reemplazo mecánico del `<input>` crudo por `<Input>` en 9 archivos de formulario, sin tocar su lógica de `react-hook-form`.

**Tech Stack:** React 19, Tailwind CSS 4 (`@theme`), react-hook-form + zod, Vitest + Testing Library.

## Global Constraints

- Valores exactos de la nueva escala `primary` (teal) y `success` (lime): ver Task 1, tomados de la escala estándar Tailwind CSS v4 — no inventar valores.
- `secondary` se elimina completamente de `@theme`; sus 2 usos (`LoginForm.tsx`, `DashboardPage.tsx`) migran a `neutral`.
- No se crea ningún componente `Select` — los 8 `<select>` repetidos en `AsignacionRolForm`, `IdentidadExternaForm`, `RolForm`, `UsuarioForm` quedan sin tocar (fuera de alcance, decisión explícita).
- No se migra ningún `<textarea>` a un componente compartido — solo `<input>` de texto/número/password/email/datetime-local.
- `LoginForm.tsx` (campos de correo/contraseña con ícono+toggle) NO se migra a `Input` en este plan — su patrón (ícono absoluto + botón de mostrar/ocultar superpuesto) es más complejo que lo que `Input` cubre; se deja tal cual, solo se corrige el residual de `secondary`→`neutral` en su banner de ayuda.
- Todo archivo nuevo (`Input.tsx` y su test) debe alcanzar 100% de cobertura de líneas y ramas (regla de `CLAUDE.md`).
- No modificar el mecanismo de dark mode (`useTheme`, `ThemeToggle`, `data-theme`) — solo actualizar los valores hex de los tokens que ya tienen su contraparte `dark:`.

---

## Task 1: Actualizar la paleta en `styles.css`

**Files:**
- Modify: `frontend/apps/erp-web/src/styles.css`

**Interfaces:**
- Consume: ninguna.
- Produce: los tokens CSS `--color-primary-*` (11 variables, ahora teal) y `--color-success-*` (11 variables, ahora lime) que consumen todos los componentes de `ui-web` y todas las features vía clases Tailwind `primary-*`/`success-*`. El bloque `--color-secondary-*` deja de existir.

- [ ] **Paso 1: Reemplazar el bloque `--color-primary-*` (líneas 11-21 del archivo)**

Reemplazar:
```css
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
```
por:
```css
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
```

- [ ] **Paso 2: Eliminar el bloque `--color-secondary-*` completo (líneas 23-33 del archivo original)**

Eliminar:
```css

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
```

- [ ] **Paso 3: Reemplazar el bloque `--color-success-*` (antes líneas 35-45)**

Reemplazar:
```css
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
```
por:
```css
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

- [ ] **Paso 4: Verificar que el archivo compila — correr el build**

Run: `npx vite build` desde `frontend/apps/erp-web/`
Expected: build exitoso (exit 0). Un `@theme` con CSS inválido fallaría aquí.

- [ ] **Paso 5: Commit**

```bash
git add frontend/apps/erp-web/src/styles.css
git commit -m "feat(design-system): migrar primary a teal, success a lime, retirar secondary"
```

---

## Task 2: Migrar los 2 usos de `secondary` a `neutral`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx`

**Interfaces:**
- Consume: ninguna interfaz nueva.
- Produce: `StatCardProps.tone` en `DashboardPage.tsx` cambia su unión de tipos de `'primary' | 'secondary' | 'warning' | 'danger'` a `'primary' | 'neutral' | 'warning' | 'danger'`; el único caller (`<StatCard tone="secondary">`) se actualiza a `tone="neutral"` en el mismo paso para no dejar el tipo roto.

- [ ] **Paso 1: En `LoginForm.tsx`, reemplazar el banner de ayuda de recuperación de contraseña**

Reemplazar:
```tsx
      {recoveryVisible ? (
        <div
          id="recovery-help"
          role="status"
          className="border-secondary-100 bg-secondary-50 text-secondary-900 dark:border-secondary-800 dark:bg-secondary-900/30 dark:text-secondary-200 flex gap-3 rounded-xl border p-3.5 text-xs leading-5"
        >
          <CircleHelp
            className="text-secondary-600 dark:text-secondary-400 mt-0.5 size-4 shrink-0"
            aria-hidden="true"
          />
          Solicita el restablecimiento al administrador de tu organización. El enlace se enviará
          únicamente a tu correo corporativo.
        </div>
      ) : null}
```
por:
```tsx
      {recoveryVisible ? (
        <div
          id="recovery-help"
          role="status"
          className="border-neutral-200 bg-neutral-50 text-neutral-700 dark:border-neutral-700 dark:bg-neutral-800/40 dark:text-neutral-300 flex gap-3 rounded-xl border p-3.5 text-xs leading-5"
        >
          <CircleHelp
            className="text-neutral-500 dark:text-neutral-400 mt-0.5 size-4 shrink-0"
            aria-hidden="true"
          />
          Solicita el restablecimiento al administrador de tu organización. El enlace se enviará
          únicamente a tu correo corporativo.
        </div>
      ) : null}
```

- [ ] **Paso 2: En `DashboardPage.tsx`, actualizar el tipo `StatCardProps.tone` y `toneClasses`**

Reemplazar:
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
por:
```tsx
type StatCardProps = {
  label: string;
  value: string;
  detail: string;
  icon: LucideIcon;
  tone: 'primary' | 'neutral' | 'warning' | 'danger';
};

const toneClasses = {
  primary: 'bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300',
  neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-900/40 dark:text-warning-300',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-900/40 dark:text-danger-300'
};
```

- [ ] **Paso 3: En el mismo archivo, actualizar el caller `<StatCard tone="secondary">` (stat card "Unidades en stock")**

Reemplazar:
```tsx
        <StatCard
          label="Unidades en stock"
          value={isPending ? '—' : String(data?.stockUnits ?? 0)}
          detail="En todos los almacenes"
          icon={Boxes}
          tone="secondary"
        />
```
por:
```tsx
        <StatCard
          label="Unidades en stock"
          value={isPending ? '—' : String(data?.stockUnits ?? 0)}
          detail="En todos los almacenes"
          icon={Boxes}
          tone="neutral"
        />
```

- [ ] **Paso 4: En el mismo archivo, reemplazar el bloque de "Comprobantes pendientes"**

Reemplazar:
```tsx
            <div className="bg-secondary-50 dark:bg-secondary-900/30 flex gap-3 rounded-xl p-4">
              <ReceiptText className="text-secondary-700 dark:text-secondary-400 mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-secondary-950 dark:text-secondary-100 text-sm font-semibold">
                  Comprobantes pendientes
                </p>
                <p className="text-secondary-800 dark:text-secondary-300 mt-1 text-xs leading-5">
                  3 documentos esperan confirmación.
                </p>
              </div>
            </div>
```
por:
```tsx
            <div className="bg-neutral-100 dark:bg-neutral-800/60 flex gap-3 rounded-xl p-4">
              <ReceiptText className="text-neutral-600 dark:text-neutral-400 mt-0.5 size-5 shrink-0" />
              <div>
                <p className="text-neutral-900 dark:text-neutral-100 text-sm font-semibold">
                  Comprobantes pendientes
                </p>
                <p className="text-neutral-700 dark:text-neutral-300 mt-1 text-xs leading-5">
                  3 documentos esperan confirmación.
                </p>
              </div>
            </div>
```

- [ ] **Paso 5: Verificar con typecheck y tests**

Run (desde `frontend/apps/erp-web/`): `npx tsc -b --pretty false`
Expected: sin errores (confirma que no quedó ningún `tone="secondary"` huérfano).

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/auth apps/erp-web/src/features/dashboard`
Expected: todos los tests PASS.

- [ ] **Paso 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx frontend/apps/erp-web/src/features/dashboard/pages/DashboardPage.tsx
git commit -m "feat(design-system): migrar usos de secondary a neutral"
```

---

## Task 3: Corregir las fugas de color hardcodeadas en `LoginPage.tsx`

> **Estado: OMITIDA por decisión explícita del usuario.** El usuario indicó "el login ya estuvo bien, no lo cambies" — aunque el resultado visual de esta task sería idéntico (los hex hardcodeados ya coinciden con los nuevos valores de `primary`, solo se limpiaría cómo está escrito el código), se decidió no tocar `LoginPage.tsx` en absoluto. El archivo queda con sus valores hex hardcodeados (`#0d9488`, `#065f46`, `#022c22`, `#f4f7f5`) tal como estaban antes de este plan. Esto no afecta el resto del rediseño de paleta: el token `primary` ya migró a teal (Task 1) y esos hex del login ya son visualmente equivalentes al nuevo token, así que no hay discrepancia visual, solo una inconsistencia de "cómo está escrito" que se acepta conscientemente.

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`

**Interfaces:**
- Consume: ninguna interfaz nueva.
- Produce: ninguna interfaz nueva — solo valores de `className`/`style`.

- [ ] **Paso 1: Reemplazar el fondo hardcodeado de `<main>`**

Reemplazar:
```tsx
    <main className="min-h-screen bg-[#f4f7f5] lg:grid lg:grid-cols-[minmax(0,1.08fr)_minmax(480px,0.92fr)] dark:bg-neutral-950">
```
por:
```tsx
    <main className="min-h-screen bg-neutral-50 lg:grid lg:grid-cols-[minmax(0,1.08fr)_minmax(480px,0.92fr)] dark:bg-neutral-950">
```

- [ ] **Paso 2: Reemplazar el gradiente hardcodeado del panel izquierdo por variables CSS reales de `primary`**

Reemplazar:
```tsx
      <section
        className="relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14"
        style={{ backgroundImage: 'linear-gradient(160deg, #0d9488 0%, #065f46 55%, #022c22 100%)' }}
      >
```
por:
```tsx
      <section
        className="relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14"
        style={{
          backgroundImage:
            'linear-gradient(160deg, var(--color-primary-600) 0%, var(--color-primary-800) 55%, var(--color-primary-950) 100%)'
        }}
      >
```

- [ ] **Paso 3: Verificar con tests y build**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/auth/pages/LoginPage.test.tsx`
Expected: 5/5 PASS (ningún test asevera el fondo/gradiente, son cambios puramente visuales).

Run (desde `frontend/apps/erp-web/`): `npx vite build`
Expected: build exitoso.

- [ ] **Paso 4: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx
git commit -m "fix(auth): usar tokens de color reales en vez de hex sueltos en el login"
```

---

## Task 4: Crear el componente `Input` en `packages/ui-web`

**Files:**
- Create: `frontend/packages/ui-web/src/input/Input.tsx`
- Create: `frontend/packages/ui-web/src/input/Input.test.tsx`
- Modify: `frontend/packages/ui-web/src/index.ts`

**Interfaces:**
- Consume: `cn` de `../lib/cn` (mismo patrón que `Button`).
- Produce: `export function Input(props: InputProps)` y `export type InputProps`, consumidos por Task 5 en los 9 formularios. Firma:
```ts
type InputProps = ComponentPropsWithRef<'input'> & {
  icon?: LucideIcon;
};
```
`Input` reenvía `ref` (para que `register('campo')` de react-hook-form siga funcionando), acepta cualquier prop estándar de `<input>` (`id`, `type`, `min`, `placeholder`, `aria-invalid`, `aria-describedby`, etc.) vía spread, y añade una clase `aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100` para que el estado de error de zod/react-hook-form (que ya setea `aria-invalid`) se refleje visualmente sin que cada formulario lo repita.

- [ ] **Paso 1: Escribir el test (falla primero)**

Crear `frontend/packages/ui-web/src/input/Input.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Mail } from 'lucide-react';
import { Input } from './Input';

describe('Input', () => {
  it('renderiza un input de texto con las clases base', () => {
    render(<Input aria-label="Nombre" />);
    const input = screen.getByLabelText('Nombre');
    expect(input).toBeInTheDocument();
    expect(input.className).toContain('focus:border-primary-600');
    expect(input.className).toContain('rounded-xl');
  });

  it('acepta ref para integrarse con react-hook-form', () => {
    const ref = createRef<HTMLInputElement>();
    render(<Input aria-label="Correo" ref={ref} />);
    expect(ref.current).toBe(screen.getByLabelText('Correo'));
  });

  it('renderiza el icono cuando se provee y ajusta el padding izquierdo', () => {
    render(<Input aria-label="Correo" icon={Mail} />);
    const input = screen.getByLabelText('Correo');
    expect(input.className).toContain('pl-11');
    expect(document.querySelector('svg')).toBeInTheDocument();
  });

  it('no aplica padding izquierdo extra ni renderiza icono cuando no se provee', () => {
    render(<Input aria-label="Sin icono" />);
    const input = screen.getByLabelText('Sin icono');
    expect(input.className).not.toContain('pl-11');
    expect(document.querySelector('svg')).not.toBeInTheDocument();
  });

  it('reenvia props estandar como type, placeholder y aria-invalid', () => {
    render(<Input aria-label="Password" type="password" placeholder="••••" aria-invalid />);
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');
    expect(input).toHaveAttribute('placeholder', '••••');
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('combina className adicional sin perder las clases base', () => {
    render(<Input aria-label="Custom" className="my-custom-class" />);
    const input = screen.getByLabelText('Custom');
    expect(input.className).toContain('my-custom-class');
    expect(input.className).toContain('rounded-xl');
  });
});
```

- [ ] **Paso 2: Correr el test y verificar que falla (el archivo `Input.tsx` no existe aún)**

Run (desde `frontend/`): `npx vitest run packages/ui-web/src/input/Input.test.tsx`
Expected: FAIL — no se puede resolver el módulo `./Input`.

- [ ] **Paso 3: Crear `Input.tsx`**

```tsx
import type { ComponentPropsWithRef } from 'react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '../lib/cn';

export type InputProps = ComponentPropsWithRef<'input'> & {
  icon?: LucideIcon;
};

export function Input({ className, icon: Icon, ref, ...props }: InputProps) {
  if (!Icon) {
    return (
      <input
        ref={ref}
        className={cn(
          'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none transition placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600',
          className
        )}
        {...props}
      />
    );
  }

  return (
    <div className="relative">
      <Icon
        className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
        aria-hidden="true"
      />
      <input
        ref={ref}
        className={cn(
          'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm outline-none transition placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600',
          className
        )}
        {...props}
      />
    </div>
  );
}
```

- [ ] **Paso 4: Correr el test y verificar que pasa**

Run (desde `frontend/`): `npx vitest run packages/ui-web/src/input/Input.test.tsx`
Expected: PASS — 6/6 tests en verde.

- [ ] **Paso 5: Exportar `Input` desde `packages/ui-web/src/index.ts`**

El archivo no sigue un orden alfabético estricto (ver el export de `cn` intercalado entre componentes). Insertar la línea entre el export de `IconButton` y el de `Modal`:
```ts
export { Input, type InputProps } from './input/Input';
```

- [ ] **Paso 6: Correr cobertura del paquete para confirmar 100% en el archivo nuevo**

Run (desde `frontend/`): `npx vitest run packages/ui-web/src/input --coverage`
Expected: `Input.tsx` con 100% statements/branches/functions/lines.

- [ ] **Paso 7: Commit**

```bash
git add frontend/packages/ui-web/src/input/Input.tsx frontend/packages/ui-web/src/input/Input.test.tsx frontend/packages/ui-web/src/index.ts
git commit -m "feat(ui-web): agregar componente Input compartido"
```

---

## Task 5: Migrar `MarcaForm`, `CategoriaForm`, `RubroComercialForm` a `Input`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/catalogo/components/MarcaForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/components/CategoriaForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/components/RubroComercialForm.tsx`

**Interfaces:**
- Consume: `Input` de `@boticas/ui-web` (Task 4).
- Produce: ninguna interfaz nueva — el comportamiento de cada formulario (validación, submit) no cambia.

- [ ] **Paso 1: En `MarcaForm.tsx`, importar `Input` y reemplazar los 2 `<input>` de texto (Código, Nombre) — el `<textarea>` de Descripción NO se toca**

Cambiar el import:
```tsx
import { Button, FormField } from '@boticas/ui-web';
```
por:
```tsx
import { Button, FormField, Input } from '@boticas/ui-web';
```

Reemplazar:
```tsx
      <FormField label="Código" htmlFor="marca-codigo" error={errors.codigo?.message}>
        <input
          id="marca-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="marca-nombre" error={errors.nombre?.message}>
        <input
          id="marca-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Código" htmlFor="marca-codigo" error={errors.codigo?.message}>
        <Input id="marca-codigo" {...register('codigo')} />
      </FormField>

      <FormField label="Nombre" htmlFor="marca-nombre" error={errors.nombre?.message}>
        <Input id="marca-nombre" {...register('nombre')} />
      </FormField>
```

- [ ] **Paso 2: En `CategoriaForm.tsx`, aplicar el mismo reemplazo a Código, Nombre, Nivel y Orden (el `<textarea>` de Descripción NO se toca)**

Cambiar el import igual que en el paso 1.

Reemplazar:
```tsx
      <FormField label="Código" htmlFor="categoria-codigo" error={errors.codigo?.message}>
        <input
          id="categoria-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="categoria-nombre" error={errors.nombre?.message}>
        <input
          id="categoria-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Código" htmlFor="categoria-codigo" error={errors.codigo?.message}>
        <Input id="categoria-codigo" {...register('codigo')} />
      </FormField>

      <FormField label="Nombre" htmlFor="categoria-nombre" error={errors.nombre?.message}>
        <Input id="categoria-nombre" {...register('nombre')} />
      </FormField>
```

Reemplazar:
```tsx
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nivel" htmlFor="categoria-nivel" error={errors.nivel?.message}>
          <input
            id="categoria-nivel"
            type="number"
            min={1}
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('nivel', { valueAsNumber: true })}
          />
        </FormField>
        <FormField label="Orden" htmlFor="categoria-orden" error={errors.orden?.message}>
          <input
            id="categoria-orden"
            type="number"
            min={0}
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('orden', { valueAsNumber: true })}
          />
        </FormField>
      </div>
```
por:
```tsx
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Nivel" htmlFor="categoria-nivel" error={errors.nivel?.message}>
          <Input
            id="categoria-nivel"
            type="number"
            min={1}
            {...register('nivel', { valueAsNumber: true })}
          />
        </FormField>
        <FormField label="Orden" htmlFor="categoria-orden" error={errors.orden?.message}>
          <Input
            id="categoria-orden"
            type="number"
            min={0}
            {...register('orden', { valueAsNumber: true })}
          />
        </FormField>
      </div>
```

- [ ] **Paso 3: En `RubroComercialForm.tsx`, aplicar el mismo reemplazo a Código, Nombre y Orden (el `<textarea>` de Descripción y el `<input type="checkbox">` de "Es farmacéutico" NO se tocan)**

Cambiar el import igual que en el paso 1.

Reemplazar:
```tsx
      <FormField label="Código" htmlFor="rubro-codigo" error={errors.codigo?.message}>
        <input
          id="rubro-codigo"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('codigo')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rubro-nombre" error={errors.nombre?.message}>
        <input
          id="rubro-nombre"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('nombre')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Código" htmlFor="rubro-codigo" error={errors.codigo?.message}>
        <Input id="rubro-codigo" {...register('codigo')} />
      </FormField>

      <FormField label="Nombre" htmlFor="rubro-nombre" error={errors.nombre?.message}>
        <Input id="rubro-nombre" {...register('nombre')} />
      </FormField>
```

Reemplazar:
```tsx
      <FormField label="Orden" htmlFor="rubro-orden" error={errors.orden?.message}>
        <input
          id="rubro-orden"
          type="number"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('orden', { valueAsNumber: true })}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Orden" htmlFor="rubro-orden" error={errors.orden?.message}>
        <Input id="rubro-orden" type="number" {...register('orden', { valueAsNumber: true })} />
      </FormField>
```

- [ ] **Paso 4: Correr los tests de los 3 formularios**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/catalogo`
Expected: todos los tests existentes de `catalogo` PASS (los tests interactúan por `label`/`role`, no por clase CSS, así que no deberían requerir cambios).

- [ ] **Paso 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/catalogo/components/MarcaForm.tsx frontend/apps/erp-web/src/features/catalogo/components/CategoriaForm.tsx frontend/apps/erp-web/src/features/catalogo/components/RubroComercialForm.tsx
git commit -m "refactor(catalogo): usar Input compartido en MarcaForm, CategoriaForm y RubroComercialForm"
```

---

## Task 6: Migrar `SupportCatalogForm` a `Input`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.tsx`

**Interfaces:**
- Consume: `Input` de `@boticas/ui-web`.
- Produce: ninguna interfaz nueva — este archivo alimenta 7 de las 8 pantallas de catálogo de soporte, así que el cambio se propaga a todas ellas sin tocarlas individualmente.

- [ ] **Paso 1: Importar `Input` y eliminar la constante `inputClassName` ya no usada por el caso de texto/número (se mantiene solo si el checkbox la necesitara, pero el checkbox tiene su propia clase — verificar y eliminar si queda huérfana)**

Cambiar:
```tsx
import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form';
import { Button, FormField } from '@boticas/ui-web';
import type { FieldDef } from './support-catalog.types';

const inputClassName =
  'focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100';
```
por:
```tsx
import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form';
import { Button, FormField, Input } from '@boticas/ui-web';
import type { FieldDef } from './support-catalog.types';
```

- [ ] **Paso 2: Reemplazar el bloque de renderizado del caso texto/número (el `<textarea>` del caso `field.type === 'textarea'` NO se toca)**

Reemplazar:
```tsx
        return (
          <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
            <input
              id={htmlId}
              type={field.type === 'number' ? 'number' : 'text'}
              className={inputClassName}
              {...register(fieldName, field.type === 'number' ? { setValueAs: toOptionalNumber } : {})}
            />
          </FormField>
        );
```
por:
```tsx
        return (
          <FormField key={field.name} label={field.label} htmlFor={htmlId} error={fieldError}>
            <Input
              id={htmlId}
              type={field.type === 'number' ? 'number' : 'text'}
              {...register(fieldName, field.type === 'number' ? { setValueAs: toOptionalNumber } : {})}
            />
          </FormField>
        );
```

- [ ] **Paso 3: Correr los tests de `support-catalog` y de las 7 páginas que lo consumen**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/catalogo`
Expected: todos PASS.

- [ ] **Paso 4: Commit**

```bash
git add frontend/apps/erp-web/src/features/catalogo/support-catalog/SupportCatalogForm.tsx
git commit -m "refactor(catalogo): usar Input compartido en SupportCatalogForm"
```

---

## Task 7: Migrar `CredencialLocalForm` a `Input`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx`

**Interfaces:**
- Consume: `Input` de `@boticas/ui-web`.
- Produce: ninguna interfaz nueva.

- [ ] **Paso 1: Importar `Input` y reemplazar los 2 campos de contraseña (el checkbox NO se toca)**

Cambiar el import:
```tsx
import { Button, FormField } from '@boticas/ui-web';
```
por:
```tsx
import { Button, FormField, Input } from '@boticas/ui-web';
```

Reemplazar:
```tsx
      <FormField label="Contraseña" htmlFor="credencial-password" error={errors.password?.message}>
        <input
          id="credencial-password"
          type="password"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('password')}
        />
      </FormField>

      <FormField
        label="Confirmar contraseña"
        htmlFor="credencial-confirm"
        error={errors.confirmPassword?.message}
      >
        <input
          id="credencial-confirm"
          type="password"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('confirmPassword')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Contraseña" htmlFor="credencial-password" error={errors.password?.message}>
        <Input id="credencial-password" type="password" {...register('password')} />
      </FormField>

      <FormField
        label="Confirmar contraseña"
        htmlFor="credencial-confirm"
        error={errors.confirmPassword?.message}
      >
        <Input id="credencial-confirm" type="password" {...register('confirmPassword')} />
      </FormField>
```

- [ ] **Paso 2: Correr los tests de `seguridad`**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/seguridad`
Expected: todos PASS.

- [ ] **Paso 3: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/components/CredencialLocalForm.tsx
git commit -m "refactor(seguridad): usar Input compartido en CredencialLocalForm"
```

---

## Task 8: Migrar `IdentidadExternaForm`, `RolForm`, `UsuarioForm` a `Input` (solo los `<input>`, no los `<select>`)

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/RolForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/UsuarioForm.tsx`

**Interfaces:**
- Consume: `Input` de `@boticas/ui-web`.
- Produce: ninguna interfaz nueva. Los `<select>` de estos 3 archivos quedan sin tocar (fuera de alcance, ver Global Constraints).

- [ ] **Paso 1: En `IdentidadExternaForm.tsx`, importar `Input` y reemplazar los 4 `<input>` de texto (Nombre del proveedor, Identificador, Emisor, Correo asociado) — el `<select>` de Proveedor NO se toca**

Cambiar el import:
```tsx
import { Button, FormField } from '@boticas/ui-web';
```
por:
```tsx
import { Button, FormField, Input } from '@boticas/ui-web';
```

Reemplazar:
```tsx
          <input
            id="identidad-provider-custom"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('providerCustom')}
          />
```
por:
```tsx
          <Input id="identidad-provider-custom" {...register('providerCustom')} />
```

Reemplazar:
```tsx
        <input
          id="identidad-subject"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('subject')}
        />
```
por:
```tsx
        <Input id="identidad-subject" {...register('subject')} />
```

Reemplazar:
```tsx
        <input
          id="identidad-issuer"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('issuer')}
        />
```
por:
```tsx
        <Input id="identidad-issuer" {...register('issuer')} />
```

Reemplazar:
```tsx
        <input
          id="identidad-email"
          type="email"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('emailClaim')}
        />
```
por:
```tsx
        <Input id="identidad-email" type="email" {...register('emailClaim')} />
```

- [ ] **Paso 2: En `RolForm.tsx`, importar `Input` y reemplazar los 2 `<input>` de texto (Código, Nombre) — el `<textarea>` de Descripción, el `<select>` de Tipo de rol y el checkbox NO se tocan**

Cambiar el import igual que en el paso 1.

Reemplazar:
```tsx
      <FormField label="Código" htmlFor="rol-code" error={errors.code?.message}>
        <input
          id="rol-code"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('code')}
        />
      </FormField>

      <FormField label="Nombre" htmlFor="rol-name" error={errors.name?.message}>
        <input
          id="rol-name"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('name')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Código" htmlFor="rol-code" error={errors.code?.message}>
        <Input id="rol-code" {...register('code')} />
      </FormField>

      <FormField label="Nombre" htmlFor="rol-name" error={errors.name?.message}>
        <Input id="rol-name" {...register('name')} />
      </FormField>
```

- [ ] **Paso 3: En `UsuarioForm.tsx`, importar `Input` y reemplazar los 6 `<input>` de texto (Número de documento, Nombres, Apellidos, Correo, Username, Teléfono) — el `<select>` de Tipo de documento y el checkbox NO se tocan**

Cambiar el import igual que en el paso 1.

Reemplazar:
```tsx
          <input
            id="usuario-document-number"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('documentNumber')}
          />
```
por:
```tsx
          <Input id="usuario-document-number" {...register('documentNumber')} />
```

Reemplazar:
```tsx
          <input
            id="usuario-first-names"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('firstNames')}
          />
```
por:
```tsx
          <Input id="usuario-first-names" {...register('firstNames')} />
```

Reemplazar:
```tsx
          <input
            id="usuario-last-names"
            className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
            {...register('lastNames')}
          />
```
por:
```tsx
          <Input id="usuario-last-names" {...register('lastNames')} />
```

Reemplazar:
```tsx
        <input
          id="usuario-email"
          type="email"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('email')}
        />
```
por:
```tsx
        <Input id="usuario-email" type="email" {...register('email')} />
```

Reemplazar:
```tsx
        <input
          id="usuario-username"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('username')}
        />
```
por:
```tsx
        <Input id="usuario-username" {...register('username')} />
```

Reemplazar:
```tsx
        <input
          id="usuario-phone"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('phone')}
        />
```
por:
```tsx
        <Input id="usuario-phone" {...register('phone')} />
```

- [ ] **Paso 4: Correr los tests de `seguridad`**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/seguridad`
Expected: todos PASS.

- [ ] **Paso 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/components/IdentidadExternaForm.tsx frontend/apps/erp-web/src/features/seguridad/components/RolForm.tsx frontend/apps/erp-web/src/features/seguridad/components/UsuarioForm.tsx
git commit -m "refactor(seguridad): usar Input compartido en IdentidadExternaForm, RolForm y UsuarioForm"
```

---

## Task 9: Migrar `AsignacionRolForm` a `Input` (solo los 2 `<input type="datetime-local">`)

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx`

**Interfaces:**
- Consume: `Input` de `@boticas/ui-web`.
- Produce: ninguna interfaz nueva. Los 5 `<select>` de este archivo quedan sin tocar.

- [ ] **Paso 1: Importar `Input` y reemplazar los 2 campos de fecha**

Cambiar el import:
```tsx
import { Button, FormField } from '@boticas/ui-web';
```
por:
```tsx
import { Button, FormField, Input } from '@boticas/ui-web';
```

Reemplazar:
```tsx
      <FormField label="Vigente desde (opcional)" htmlFor="asignacion-valid-from">
        <input
          id="asignacion-valid-from"
          type="datetime-local"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validFrom')}
        />
      </FormField>

      <FormField label="Vigente hasta (opcional)" htmlFor="asignacion-valid-until">
        <input
          id="asignacion-valid-until"
          type="datetime-local"
          className="focus:border-primary-600 focus:ring-primary-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100"
          {...register('validUntil')}
        />
      </FormField>
```
por:
```tsx
      <FormField label="Vigente desde (opcional)" htmlFor="asignacion-valid-from">
        <Input id="asignacion-valid-from" type="datetime-local" {...register('validFrom')} />
      </FormField>

      <FormField label="Vigente hasta (opcional)" htmlFor="asignacion-valid-until">
        <Input id="asignacion-valid-until" type="datetime-local" {...register('validUntil')} />
      </FormField>
```

- [ ] **Paso 2: Correr los tests de `seguridad`**

Run (desde `frontend/`): `npx vitest run apps/erp-web/src/features/seguridad`
Expected: todos PASS.

- [ ] **Paso 3: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/components/AsignacionRolForm.tsx
git commit -m "refactor(seguridad): usar Input compartido en AsignacionRolForm"
```

---

## Task 10: Verificación final completa

**Files:** ninguno nuevo — solo comandos de verificación.

- [ ] **Paso 1: Lint**

Run (desde `frontend/`): `npx eslint apps packages`
Expected: sin errores nuevos.

- [ ] **Paso 2: Typecheck**

Run (desde `frontend/apps/erp-web/`): `npx tsc -b --pretty false`
Expected: sin errores.

- [ ] **Paso 3: Suite completa de tests con cobertura**

Run (desde `frontend/`): `npx vitest run --coverage`
Expected: todos los tests PASS; `Input.tsx` nuevo con 100% de cobertura; ningún archivo existente baja de su cobertura previa por los cambios de este plan (los cambios son de estilo/JSX equivalente, no de lógica).

- [ ] **Paso 4: Build completo**

Run (desde `frontend/apps/erp-web/`): `npx vite build`
Expected: build exitoso.

- [ ] **Paso 5: Revisión visual manual**

```bash
pnpm --filter @boticas/erp-web dev
```
Abrir la app y verificar en modo claro y oscuro: login (panel izquierdo teal, formulario), dashboard (stat cards, incluida la que era "secondary" ahora en gris neutral), sidebar del `AppShell` (ahora teal en vez de azul), una pantalla de `seguridad` con formulario (ej. crear usuario) y una de `catalogo` (ej. marcas) verificando que los inputs migrados a `Input` se ven y funcionan igual que antes (foco, error, placeholder).

- [ ] **Paso 6: Commit final si la revisión visual detecta algún ajuste menor, o cierre sin commit si todo está correcto**

---

## Self-Review

**Cobertura del spec:** paleta (Task 1), migración de `secondary` (Task 2), fugas de color del login (Task 3), componente `Input` nuevo con TDD (Task 4), y su aplicación en los 9 formularios exactos listados en el spec, uno por uno o agrupados por similitud (Tasks 5-9), más verificación final (Task 10). La decisión de no crear `Select` y no migrar `LoginForm.tsx`/`<textarea>` está reflejada en Global Constraints y respetada en cada task.

**Placeholders:** cada paso de código muestra el bloque completo `antes`/`después` tomado literalmente de los archivos reales inspeccionados — ningún paso dice "aplicar el mismo patrón" sin mostrar el código exacto del archivo correspondiente.

**Consistencia de nombres:** `InputProps` y la firma `{ icon?: LucideIcon }` definidas en Task 4 se usan sin variación en las Tasks 5-9. El nombre de export `Input` desde `@boticas/ui-web` es el mismo en todos los imports agregados.
