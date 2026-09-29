# Rediseño visual del login (Figma "FarmaVita") — Plan de implementación

> **Para agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** Reemplazar el diseño visual azul/amarillo actual de `LoginPage.tsx`/`LoginForm.tsx` por el diseño verde-teal "FarmaVita" del prototipo de Figma (frame `01 · Login`), sin tocar la integración real con el backend de autenticación.

**Architecture:** Cambios de JSX/clases Tailwind dentro de los dos componentes existentes, más 3 elementos nuevos puramente visuales sin estado de formulario (`select` de Sucursal, botones QR/PIN de caja). El flujo de datos (`useForm` → `loginSchema` → `onAuthenticate` → `useAuthSession.authenticate`) no cambia.

**Tech Stack:** React 19, react-hook-form + zod, Tailwind CSS 4, Vitest + Testing Library + MSW (tests existentes).

## Global Constraints

- Paleta: únicamente tokens `success-*` ya definidos en `frontend/apps/erp-web/src/styles.css:35-45` (`50`→`#ecfdf5` … `950`→`#022c22`). No introducir hex nuevos, no tocar `primary`/`warning` globales.
- No modificar `useAuthSession`, `auth.api.ts`, `login.schema.ts`. El payload enviado por `onAuthenticate` sigue siendo exactamente `{ email, password, remember }`.
- No modificar `packages/ui-web/src/button/Button.tsx` — su variante `primary` sigue en azul para el resto de la app; el botón de login se ajusta con `className` override.
- El campo "Sucursal" y los botones "Código QR"/"PIN de caja" son puramente visuales: no se registran en `useForm`, no aparecen en el payload de submit, no tienen `onClick` que navegue o llame a una API.
- Tipografía: se mantiene la fuente del sistema ya usada en el frontend (Tailwind default) — el prototipo de Figma usa Plus Jakarta Sans, pero cambiar la fuente global del frontend no está en el alcance del spec; solo cambia la paleta y el layout de contenido.
- Todo archivo nuevo con menos de 100% de cobertura no se considera terminado (regla del repo en `CLAUDE.md`); los archivos modificados (`LoginPage.tsx`, `LoginForm.tsx`) no son nuevos, pero cualquier rama de código nueva que se agregue debe quedar cubierta por los tests existentes o ajustados en este plan.

---

## Task 1: Actualizar `LoginPage.tsx` — panel izquierdo de branding

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`
- Test: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx` (sin cambios en esta task — el panel izquierdo no tiene aserciones de test hoy)

**Interfaces:**
- Consume: ninguna interfaz nueva — sigue usando `useAuthSession`, `useLocation`, `useNavigate` exactamente igual.
- Produce: el JSX del panel izquierdo (`<section>` con `lg:flex`) queda con la nueva estructura visual; no expone ninguna prop ni función nueva a `LoginForm`.

- [ ] **Paso 1: Reemplazar el fondo y gradientes del panel izquierdo**

En `LoginPage.tsx`, reemplazar:
```tsx
<section className="bg-primary-950 relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14">
  <div
    className="pointer-events-none absolute inset-0 opacity-80"
    style={{
      backgroundImage:
        'radial-gradient(circle at 12% 18%, rgba(45, 212, 191, 0.22), transparent 28%), radial-gradient(circle at 82% 78%, rgba(250, 204, 21, 0.18), transparent 27%)'
    }}
  />
```
por:
```tsx
<section className="bg-success-950 relative hidden min-h-screen overflow-hidden p-10 text-white lg:flex lg:flex-col xl:p-14">
  <div
    className="pointer-events-none absolute inset-0 opacity-80"
    style={{
      backgroundImage:
        'radial-gradient(circle at 12% 18%, rgba(16, 185, 129, 0.22), transparent 28%), radial-gradient(circle at 82% 78%, rgba(255, 255, 255, 0.12), transparent 27%)'
    }}
  />
```

- [ ] **Paso 2: Reemplazar el bloque de marca (logo + badge de seguridad) quitando el acento amarillo**

Reemplazar:
```tsx
<div className="relative flex items-center gap-3">
  <div className="bg-warning-300 text-primary-950 shadow-primary-950/30 grid size-11 place-items-center rounded-2xl shadow-lg">
    <Pill className="size-5" aria-hidden="true" />
  </div>
  <div>
    <p className="text-lg font-black tracking-tight">ERP Boticas</p>
    <p className="text-primary-100/65 text-xs font-medium">
      Gestión farmacéutica inteligente
    </p>
  </div>
</div>

<div className="relative my-auto max-w-xl py-16">
  <div className="border-primary-300/20 text-primary-50 inline-flex items-center gap-2 rounded-full border bg-white/10 px-3 py-1.5 text-xs font-semibold backdrop-blur">
    <ShieldCheck className="text-warning-300 size-3.5" aria-hidden="true" />
    Operación segura y centralizada
  </div>
  <h1 className="mt-6 max-w-lg text-4xl leading-[1.08] font-black tracking-[-0.035em] text-balance xl:text-5xl">
    Todo lo que tu botica necesita, en un solo lugar.
  </h1>
  <p className="text-primary-50/70 mt-5 max-w-lg text-base leading-7 xl:text-lg">
    Controla sucursales, productos, stock y ventas con información confiable para decidir
    mejor cada día.
  </p>
```
por:
```tsx
<div className="relative flex items-center gap-3">
  <div className="bg-white text-success-800 shadow-success-950/30 grid size-11 place-items-center rounded-2xl shadow-lg">
    <Pill className="size-5" aria-hidden="true" />
  </div>
  <div>
    <p className="text-lg font-black tracking-tight">FarmaVita</p>
    <p className="text-success-100/65 text-xs font-medium">
      Sistema de gestión de boticas
    </p>
  </div>
</div>

<div className="relative my-auto max-w-xl py-16">
  <div className="border-white/20 text-success-50 inline-flex items-center gap-2 rounded-full border bg-white/10 px-3 py-1.5 text-xs font-semibold backdrop-blur">
    <ShieldCheck className="text-white size-3.5" aria-hidden="true" />
    Plataforma para boticas y farmacias
  </div>
  <h1 className="mt-6 max-w-lg text-4xl leading-[1.08] font-black tracking-[-0.035em] text-balance xl:text-5xl">
    Gestiona tu botica con precisión y confianza.
  </h1>
  <p className="text-success-50/70 mt-5 max-w-lg text-base leading-7 xl:text-lg">
    Controla inventario, lotes y vencimientos, ventas y caja de todas tus sucursales desde un
    solo lugar.
  </p>
```

- [ ] **Paso 3: Reemplazar la lista de beneficios por los 3 bloques de feature con ícono, quitando el check amarillo**

Reemplazar el array y el `<ul>`:
```tsx
const operationalBenefits = [
  'Inventario y lotes en tiempo real',
  'Ventas y caja con trazabilidad',
  'Permisos por empresa y sucursal'
];
```
por:
```tsx
const operationalFeatures = [
  {
    icon: Boxes,
    title: 'Inventario inteligente',
    description: 'Control de lotes, stock mínimo y alertas de vencimiento.'
  },
  {
    icon: Store,
    title: 'Ventas y comprobantes',
    description: 'Boletas y facturas electrónicas en segundos.'
  },
  {
    icon: Warehouse,
    title: 'Reportes en tiempo real',
    description: 'Indicadores por sucursal, turno y vendedor.'
  }
];
```
Y reemplazar:
```tsx
<ul className="mt-8 space-y-3.5" aria-label="Beneficios de la plataforma">
  {operationalBenefits.map((benefit) => (
    <li
      key={benefit}
      className="text-primary-50 flex items-center gap-3 text-sm font-medium"
    >
      <span className="bg-warning-300 text-primary-950 grid size-6 place-items-center rounded-full">
        <Check className="size-3.5 stroke-[3]" aria-hidden="true" />
      </span>
      {benefit}
    </li>
  ))}
</ul>
```
por:
```tsx
<ul className="mt-8 space-y-4" aria-label="Funcionalidades de la plataforma">
  {operationalFeatures.map(({ icon: Icon, title, description }) => (
    <li key={title} className="flex items-start gap-3.5">
      <span className="bg-white/15 text-white grid size-10 shrink-0 place-items-center rounded-xl">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <div>
        <p className="text-white text-sm font-bold">{title}</p>
        <p className="text-success-50/70 mt-0.5 text-sm leading-6">{description}</p>
      </div>
    </li>
  ))}
</ul>
```

- [ ] **Paso 4: Reemplazar el grid de métricas (Sucursales/Unidades/Disponibilidad) por las 2 tarjetas del prototipo (Ventas de hoy / Por vencer)**

Reemplazar:
```tsx
<div className="mt-10 grid max-w-lg grid-cols-3 gap-3">
  <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
    <Store className="text-warning-300 size-5" aria-hidden="true" />
    <p className="mt-3 text-xl font-bold">12</p>
    <p className="text-primary-50/60 mt-0.5 text-[11px]">Sucursales</p>
  </div>
  <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
    <Warehouse className="text-warning-300 size-5" aria-hidden="true" />
    <p className="mt-3 text-xl font-bold">8.4k</p>
    <p className="text-primary-50/60 mt-0.5 text-[11px]">Unidades</p>
  </div>
  <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
    <Boxes className="text-warning-300 size-5" aria-hidden="true" />
    <p className="mt-3 text-xl font-bold">99.9%</p>
    <p className="text-primary-50/60 mt-0.5 text-[11px]">Disponibilidad</p>
  </div>
</div>
```
por:
```tsx
<div className="mt-10 grid max-w-lg grid-cols-2 gap-3">
  <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
    <p className="text-success-50/70 text-xs font-semibold">Ventas de hoy</p>
    <p className="mt-2 text-xl font-black">S/ 8,452.30</p>
    <p className="text-success-200 mt-1 text-[11px] font-bold">▲ 12.4%</p>
  </div>
  <div className="rounded-2xl border border-white/10 bg-white/8 p-4 backdrop-blur-sm">
    <p className="text-success-50/70 text-xs font-semibold">Por vencer (30 días)</p>
    <p className="mt-2 text-xl font-black">7 lotes</p>
    <p className="text-success-200 mt-1 text-[11px] font-bold">Amoxicilina 500mg</p>
  </div>
</div>
```

- [ ] **Paso 5: Reemplazar el footer del panel izquierdo**

Reemplazar:
```tsx
<div className="text-primary-50/50 relative flex items-center justify-between gap-5 border-t border-white/10 pt-6 text-xs">
  <span>© 2026 ERP Boticas</span>
  <span className="flex items-center gap-1.5">
    <Building2 className="size-3.5" aria-hidden="true" />
    Botica Central · Lima
  </span>
</div>
```
por:
```tsx
<div className="text-success-50/50 relative flex items-center justify-between gap-5 border-t border-white/10 pt-6 text-xs">
  <span>Datos cifrados de extremo a extremo · Respaldo diario automático</span>
  <span className="flex items-center gap-1.5">
    <Building2 className="size-3.5" aria-hidden="true" />
    © 2026 FarmaVita
  </span>
</div>
```

- [ ] **Paso 6: Ajustar los imports de íconos** (quitar `Check` si ya no se usa en este archivo, mantener `Boxes`, `Building2`, `Pill`, `ShieldCheck`, `Store`, `Warehouse`)

Verificar con lectura del archivo tras los pasos 1-5 cuáles íconos siguen usados y limpiar el import `lucide-react` en consecuencia.

- [ ] **Paso 7: Correr los tests existentes para confirmar que el panel izquierdo (sin aserciones directas) no rompe nada**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: los 6 tests existentes siguen en verde (ninguno asevera contenido del panel izquierdo).

- [ ] **Paso 8: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx
git commit -m "feat(auth): rediseñar panel de branding del login con paleta success"
```

---

## Task 2: Actualizar el header móvil y el título/subtítulo del panel derecho en `LoginPage.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`
- Test: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx:83` (el texto `'Ingresa a tu cuenta'` cambia — este paso ACTUALIZA esa aserción)

**Interfaces:**
- Consume: ninguna interfaz nueva.
- Produce: el `<h2>` del panel derecho pasa a mostrar el texto `"Bienvenido de nuevo 👋"`, usado por el test de Task 2 Paso 2.

- [ ] **Paso 1: Actualizar el test que depende del texto anterior (antes de tocar el componente, para verlo fallar por el motivo correcto)**

En `LoginPage.test.tsx:83`, reemplazar:
```tsx
    expect(await screen.findByRole('heading', { name: 'Ingresa a tu cuenta' })).toBeInTheDocument();
```
por:
```tsx
    expect(
      await screen.findByRole('heading', { name: 'Bienvenido de nuevo 👋' })
    ).toBeInTheDocument();
```

- [ ] **Paso 2: Ejecutar el test y confirmar que falla por el heading nuevo aún no existente**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx -t "redirige al login"`
Expected: FAIL — no encuentra un heading con nombre "Bienvenido de nuevo 👋" (el componente todavía dice "Ingresa a tu cuenta").

- [ ] **Paso 3: Actualizar el header móvil (quitar acento `warning`) y el bloque de icono/título/subtítulo del panel derecho**

Reemplazar:
```tsx
<div className="flex items-center gap-3 lg:hidden">
  <div className="bg-primary-900 text-warning-300 grid size-10 place-items-center rounded-xl">
    <Pill className="size-4.5" aria-hidden="true" />
  </div>
  <div>
    <p className="font-black tracking-tight text-neutral-950 dark:text-white">
      ERP Boticas
    </p>
    <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
      Gestión farmacéutica
    </p>
  </div>
</div>

<div className="my-auto w-full max-w-md self-center py-10">
  <div className="bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300 mb-7 inline-flex size-12 items-center justify-center rounded-2xl lg:hidden">
    <ShieldCheck className="size-5" aria-hidden="true" />
  </div>
  <p className="text-primary-700 dark:text-primary-400 text-sm font-bold">
    Bienvenido de nuevo
  </p>
  <h2 className="mt-2 text-3xl font-black tracking-[-0.03em] text-neutral-950 sm:text-4xl dark:text-white">
    Ingresa a tu cuenta
  </h2>
  <p className="mt-3 max-w-sm text-sm leading-6 text-neutral-500 dark:text-neutral-400">
    Utiliza las credenciales asignadas por el administrador de tu organización.
  </p>

  <LoginForm onAuthenticate={handleAuthentication} submitError={submitError} />
</div>
```
por:
```tsx
<div className="flex items-center gap-3 lg:hidden">
  <div className="bg-success-900 text-white grid size-10 place-items-center rounded-xl">
    <Pill className="size-4.5" aria-hidden="true" />
  </div>
  <div>
    <p className="font-black tracking-tight text-neutral-950 dark:text-white">
      FarmaVita
    </p>
    <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
      Sistema de gestión de boticas
    </p>
  </div>
</div>

<div className="my-auto w-full max-w-md self-center py-10">
  <div className="bg-success-100 text-success-800 dark:bg-success-900/40 dark:text-success-300 mb-7 inline-flex size-12 items-center justify-center rounded-2xl lg:hidden">
    <ShieldCheck className="size-5" aria-hidden="true" />
  </div>
  <h2 className="text-3xl font-black tracking-[-0.03em] text-neutral-950 sm:text-4xl dark:text-white">
    Bienvenido de nuevo 👋
  </h2>
  <p className="mt-3 max-w-sm text-sm leading-6 text-neutral-500 dark:text-neutral-400">
    Ingresa tus credenciales para acceder al panel de tu botica.
  </p>

  <LoginForm onAuthenticate={handleAuthentication} submitError={submitError} />
</div>
```

- [ ] **Paso 4: Ejecutar el test y confirmar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde.

- [ ] **Paso 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx
git commit -m "feat(auth): actualizar titulo y header movil del login al nuevo diseño"
```

---

## Task 3: Actualizar el footer de `LoginPage.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`

**Interfaces:**
- Consume/Produce: ninguna interfaz nueva — solo texto/clases del `<footer>`.

- [ ] **Paso 1: Reemplazar el footer inferior del panel derecho**

Reemplazar:
```tsx
<footer className="flex flex-col items-center justify-between gap-2 border-t border-neutral-200/70 pt-5 text-[11px] text-neutral-400 sm:flex-row dark:border-neutral-800 dark:text-neutral-500">
  <span>Privacidad y tratamiento de datos</span>
  <span>Versión 1.0.0</span>
</footer>
```
por:
```tsx
<footer className="flex flex-col items-center justify-between gap-2 border-t border-neutral-200/70 pt-5 text-[11px] text-neutral-400 sm:flex-row dark:border-neutral-800 dark:text-neutral-500">
  <span>© 2026 FarmaVita · Términos · Privacidad</span>
  <span>v2.4.0</span>
</footer>
```

- [ ] **Paso 2: Correr los tests**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS.

- [ ] **Paso 3: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx
git commit -m "feat(auth): actualizar footer del login al nuevo diseño"
```

---

## Task 4: Actualizar `LoginForm.tsx` — título de campos, focus rings y checkbox a paleta `success`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`
- Test: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx:39` (el label `'Correo corporativo'` cambia — este paso ACTUALIZA esa aserción)

**Interfaces:**
- Consume: `LoginFormProps { onAuthenticate, submitError }` — sin cambios de firma.
- Produce: el `<label htmlFor="email">` pasa a mostrar `"Usuario o correo electrónico"`, consumido por `fillValidCredentials` en el test.

- [ ] **Paso 1: Actualizar el test que depende del label anterior**

En `LoginPage.test.tsx`, reemplazar todas las ocurrencias de:
```tsx
screen.getByLabelText('Correo corporativo')
```
por:
```tsx
screen.getByLabelText('Usuario o correo electrónico')
```
(aparece en `fillValidCredentials` línea 39, y en el test de mensaje de error línea 71 — ambas se actualizan).

- [ ] **Paso 2: Ejecutar los tests y confirmar que fallan por el label aún no cambiado**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: FAIL — `getByLabelText('Usuario o correo electrónico')` no encuentra ningún elemento.

- [ ] **Paso 3: Actualizar el label del campo de correo y los focus rings a `success`**

Reemplazar:
```tsx
<label
  htmlFor="email"
  className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
>
  Correo corporativo
</label>
<div className="relative mt-2">
  <Mail
    className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
    aria-hidden="true"
  />
  <input
    id="email"
    type="email"
    autoComplete="username"
    autoFocus
    placeholder="nombre@boticas.pe"
    aria-describedby={errors.email ? 'email-error' : undefined}
    aria-invalid={Boolean(errors.email)}
    className="focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
    {...register('email')}
  />
</div>
```
por:
```tsx
<label
  htmlFor="email"
  className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
>
  Usuario o correo electrónico
</label>
<div className="relative mt-2">
  <Mail
    className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
    aria-hidden="true"
  />
  <input
    id="email"
    type="email"
    autoComplete="username"
    autoFocus
    placeholder="ej. jperez@farmavita.pe"
    aria-describedby={errors.email ? 'email-error' : undefined}
    aria-invalid={Boolean(errors.email)}
    className="focus:border-success-600 focus:ring-success-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-success-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
    {...register('email')}
  />
</div>
```

- [ ] **Paso 4: Actualizar el focus ring del campo de contraseña**

Reemplazar (dentro del bloque de contraseña):
```tsx
className="focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-12 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
```
por:
```tsx
className="focus:border-success-600 focus:ring-success-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-success-900/40 h-12 w-full rounded-xl border border-neutral-200 bg-white pr-12 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none placeholder:text-neutral-400 hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:hover:border-neutral-600"
```

- [ ] **Paso 5: Actualizar el enlace "¿Olvidaste tu contraseña?"**

Reemplazar:
```tsx
className="text-primary-700 hover:text-primary-900 focus-visible:outline-primary-600 dark:text-primary-400 dark:hover:text-primary-200 text-xs font-semibold transition focus-visible:rounded focus-visible:outline-2 focus-visible:outline-offset-2"
```
por:
```tsx
className="text-success-700 hover:text-success-900 focus-visible:outline-success-600 dark:text-success-400 dark:hover:text-success-200 text-xs font-semibold transition focus-visible:rounded focus-visible:outline-2 focus-visible:outline-offset-2"
```

- [ ] **Paso 6: Actualizar el checkbox "Recordar mi correo en este equipo" (texto se mantiene igual, solo color)**

Reemplazar:
```tsx
className="text-primary-700 focus:ring-primary-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
```
por:
```tsx
className="text-success-700 focus:ring-success-600 size-4 rounded border-neutral-300 dark:border-neutral-600"
```

- [ ] **Paso 7: Actualizar el ícono `ShieldCheck` del aviso de seguridad inferior**

Reemplazar:
```tsx
<ShieldCheck
  className="text-primary-700 dark:text-primary-400 mt-0.5 size-4 shrink-0"
  aria-hidden="true"
/>
```
por:
```tsx
<ShieldCheck
  className="text-success-700 dark:text-success-400 mt-0.5 size-4 shrink-0"
  aria-hidden="true"
/>
```

- [ ] **Paso 8: Ejecutar los tests y confirmar que pasan**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde.

- [ ] **Paso 9: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx
git commit -m "feat(auth): migrar acentos de LoginForm a paleta success"
```

---

## Task 5: Actualizar el botón "Iniciar sesión" y el mensaje de recuperación en `LoginForm.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`
- Test: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx` (el nombre accesible del botón cambia de `'Iniciar Sesión'` a `'Iniciar sesión'` — este paso ACTUALIZA esas aserciones)

**Interfaces:**
- Consume: ninguna interfaz nueva.
- Produce: el botón de submit pasa a tener texto visible `"Iniciar sesión"` (minúscula en "sesión", como en el prototipo), consumido por los tests que buscan `getByRole('button', { name: ... })`.

- [ ] **Paso 1: Actualizar los tests que dependen del texto anterior del botón**

En `LoginPage.test.tsx`, reemplazar las 3 ocurrencias de:
```tsx
screen.getByRole('button', { name: 'Iniciar Sesión' })
```
por:
```tsx
screen.getByRole('button', { name: 'Iniciar sesión' })
```
(líneas 47, 63, 73).

- [ ] **Paso 2: Ejecutar los tests y confirmar que fallan por el nombre accesible aún no cambiado**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: FAIL — no se encuentra un botón con nombre accesible "Iniciar sesión".

- [ ] **Paso 3: Actualizar el texto y estilo del botón de submit**

Reemplazar:
```tsx
<Button type="submit" className="h-12 w-full text-[15px]" disabled={isSubmitting}>
  {isSubmitting ? (
    <>
      <LoaderCircle className="size-4.5 animate-spin" aria-hidden="true" />
      Verificando acceso
    </>
  ) : (
    <>Iniciar Sesión</>
  )}
</Button>
```
por:
```tsx
<Button
  type="submit"
  className="bg-success-600 hover:bg-success-700 focus-visible:outline-success-600 h-12 w-full text-[15px]"
  disabled={isSubmitting}
>
  {isSubmitting ? (
    <>
      <LoaderCircle className="size-4.5 animate-spin" aria-hidden="true" />
      Verificando acceso
    </>
  ) : (
    <>
      Iniciar sesión
      <ArrowRight className="size-4.5" aria-hidden="true" />
    </>
  )}
</Button>
```

- [ ] **Paso 4: Agregar el import de `ArrowRight` de `lucide-react`**

En el bloque de imports de `LoginForm.tsx`, agregar `ArrowRight` a la lista ya importada de `lucide-react`.

- [ ] **Paso 5: Ejecutar los tests y confirmar que pasan**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde.

- [ ] **Paso 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx
git commit -m "feat(auth): actualizar boton de envio del login al nuevo diseño"
```

---

## Task 6: Agregar el campo visual "Sucursal" (sin lógica de formulario) en `LoginForm.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`

**Interfaces:**
- Consume: ninguna interfaz nueva — el `<select>` NO se registra con `register(...)`, es un elemento no controlado por react-hook-form.
- Produce: ninguna interfaz nueva expuesta a `LoginPage` — el payload de `onAuthenticate` no incluye `sucursal`.

- [ ] **Paso 1: Insertar el bloque de campo "Sucursal" al inicio del `<form>`, antes del campo de correo**

Insertar, justo después de la apertura del `<form>` y el bloque `{submitError ? (...) : null}`:
```tsx
<div>
  <label
    htmlFor="sucursal"
    className="text-sm font-semibold text-neutral-700 dark:text-neutral-200"
  >
    Sucursal
  </label>
  <div className="relative mt-2">
    <Building2
      className="pointer-events-none absolute top-1/2 left-3.5 size-4.5 -translate-y-1/2 text-neutral-400"
      aria-hidden="true"
    />
    <select
      id="sucursal"
      defaultValue="botica-central-huamanga"
      className="focus:border-success-600 focus:ring-success-100 dark:focus:ring-success-900/40 h-12 w-full appearance-none rounded-xl border border-neutral-200 bg-white pr-4 pl-11 text-sm text-neutral-900 shadow-sm transition outline-none hover:border-neutral-300 focus:ring-4 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:hover:border-neutral-600"
    >
      <option value="botica-central-huamanga">Botica Central – Huamanga</option>
    </select>
  </div>
</div>
```

Nota: el `<select>` es puramente visual — una sola opción de ejemplo, `defaultValue` fijo, sin `register`, sin `onChange`. No se envía en `onAuthenticate`.

- [ ] **Paso 2: Agregar el import de `Building2` de `lucide-react`** en `LoginForm.tsx` (ya existe en `LoginPage.tsx`, aquí es una importación nueva).

- [ ] **Paso 3: Ejecutar los tests existentes para confirmar que el nuevo campo no interfiere con el submit**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde; en particular, el test "navega al dashboard cuando las credenciales son válidas" sigue pasando porque el payload real no incluye `sucursal`.

- [ ] **Paso 4: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx
git commit -m "feat(auth): agregar selector visual de sucursal sin logica de formulario"
```

---

## Task 7: Agregar separador "o continúa con" + botones "Código QR"/"PIN de caja" (decorativos) en `LoginForm.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`

**Interfaces:**
- Consume: ninguna interfaz nueva.
- Produce: ninguna interfaz nueva — ambos botones son `type="button"` sin `onClick`.

- [ ] **Paso 1: Insertar el separador y los 2 botones entre el `<Button type="submit">` y el bloque del aviso de seguridad**

Insertar después del cierre de `</Button>` y antes del `<div className="flex items-start gap-2.5 ...">` (aviso de seguridad):
```tsx
<div className="flex items-center gap-3">
  <div className="h-px flex-1 bg-neutral-200 dark:bg-neutral-700" />
  <span className="text-xs font-medium text-neutral-400 dark:text-neutral-500">
    o continúa con
  </span>
  <div className="h-px flex-1 bg-neutral-200 dark:bg-neutral-700" />
</div>

<div className="grid grid-cols-2 gap-3">
  <button
    type="button"
    className="flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white text-sm font-semibold text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
  >
    <QrCode className="size-4.5" aria-hidden="true" />
    Código QR
  </button>
  <button
    type="button"
    className="flex h-11 items-center justify-center gap-2 rounded-xl border border-neutral-200 bg-white text-sm font-semibold text-neutral-700 transition hover:border-neutral-300 hover:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-200 dark:hover:border-neutral-600 dark:hover:bg-neutral-800"
  >
    <KeyRound className="size-4.5" aria-hidden="true" />
    PIN de caja
  </button>
</div>
```

Nota: ambos `<button>` son `type="button"` (no `submit`) y sin `onClick` — no interfieren con el submit del formulario ni disparan ninguna acción.

- [ ] **Paso 2: Agregar los imports de `QrCode` y `KeyRound` de `lucide-react`**

- [ ] **Paso 3: Ejecutar los tests existentes para confirmar que no interfieren con el submit real**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde (en particular, `user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))` sigue apuntando al botón correcto porque `getByRole` con `name` exacto distingue "Iniciar sesión" de "Código QR"/"PIN de caja").

- [ ] **Paso 4: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx
git commit -m "feat(auth): agregar botones decorativos de codigo QR y PIN de caja"
```

---

## Task 8: Agregar topbar "Ayuda" / selector de idioma en `LoginPage.tsx`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx`

**Interfaces:**
- Consume/Produce: ninguna interfaz nueva.

- [ ] **Paso 1: Insertar la topbar decorativa al inicio de la `<section>` del panel derecho, antes del header móvil**

Insertar como primer hijo de la segunda `<section>` (la que hoy empieza con `<div className="flex items-center gap-3 lg:hidden">`):
```tsx
<div className="hidden items-center justify-end gap-5 pb-4 text-sm font-semibold text-neutral-500 lg:flex dark:text-neutral-400">
  <span className="flex items-center gap-1.5">
    <CircleHelp className="size-4" aria-hidden="true" />
    Ayuda
  </span>
  <span className="flex items-center gap-1.5">
    <Globe className="size-4" aria-hidden="true" />
    Español
  </span>
</div>
```

Nota: ambos `<span>` son decorativos (no `<button>`, no `onClick`) — visualmente indican la presencia de esas acciones en el prototipo sin implementar funcionalidad real.

- [ ] **Paso 2: Agregar los imports de `CircleHelp` y `Globe` de `lucide-react` en `LoginPage.tsx`**

- [ ] **Paso 3: Ejecutar los tests**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx`
Expected: PASS — los 6 tests en verde.

- [ ] **Paso 4: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.tsx
git commit -m "feat(auth): agregar topbar decorativa de ayuda e idioma"
```

---

## Task 9: Verificación final completa

**Files:** ninguno nuevo — solo comandos de verificación.

- [ ] **Paso 1: Lint**

Run: `pnpm --filter @boticas/erp-web lint`
Expected: sin errores nuevos.

- [ ] **Paso 2: Typecheck**

Run: `pnpm --filter @boticas/erp-web typecheck`
Expected: sin errores.

- [ ] **Paso 3: Suite completa de tests de auth**

Run: `pnpm --filter @boticas/erp-web test -- LoginPage.test.tsx AuthSessionProvider.test.tsx`
Expected: todos PASS.

- [ ] **Paso 4: Revisión visual manual**

```bash
pnpm --filter @boticas/erp-web dev
```
Abrir `http://localhost:3000/login`, comparar visualmente contra el screenshot de Figma del frame `01 · Login` (paleta verde-teal, layout de 2 paneles, textos exactos), en modo claro y oscuro (si el toggle de tema existe en el shell de la app; si no, verificar las clases `dark:` inspeccionando con DevTools).

- [ ] **Paso 5: Build completo**

Run: `pnpm --filter @boticas/erp-web build`
Expected: build exitoso sin errores de tipos ni de bundling.

---

## Self-Review

**Cobertura del spec:** panel izquierdo (Task 1), título/header móvil (Task 2), footer (Task 3), acentos de formulario/focus rings/checkbox (Task 4), botón de submit (Task 5), campo visual de sucursal (Task 6), botones decorativos QR/PIN (Task 7), topbar de ayuda/idioma (Task 8), verificación final (Task 9). Las pantallas de error/recuperar contraseña quedan explícitamente fuera de alcance como indica el spec.

**Placeholders:** cada paso de código incluye el JSX completo exacto a reemplazar y su reemplazo — ningún paso dice "estilizar similar a" sin mostrar el código.

**Consistencia de nombres:** `LoginFormProps { onAuthenticate, submitError }` no cambia en ningún task. Los textos de aserciones de test (`'Usuario o correo electrónico'`, `'Bienvenido de nuevo 👋'`, `'Iniciar sesión'`) se actualizan en el mismo task que cambia el componente correspondiente, nunca por separado.
