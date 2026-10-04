# Frontend, parte 2: base compartida y caja (turno) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar lista la base compartida del frontend (idempotencia en `shared`, precio de referencia en el SKU, puesto de trabajo recordado, selector de terminal, formato de moneda) y entregar la pantalla `/caja` para abrir y cerrar el turno de una terminal contra el backend real.

**Architecture:** `features/caja` sigue el patrón de `features/inventario`: `api/` (funciones con `ApiClient` + `queryOptions`), `schemas/` (zod), `lib/` (lógica pura), `components/` y `pages/`. Lo que necesitan varias features se publica por `index.ts` de su dueño (`organizacion`: terminales y puesto de trabajo; `caja`: turno actual) o va a `shared/lib` si es técnico (idempotencia, preferencia local, formato).

**Tech Stack:** React 19.2, React Router 8 (rutas lazy), TanStack Query, react-hook-form + zod, Tailwind + `@boticas/ui-web`, Vitest + Testing Library + MSW (`server` de `src/test/mocks`), Playwright con `page.route`.

Spec: `docs/superpowers/specs/2026-10-03-frontend-caja-pos-ventas-design.md`. Plan previo (backend): `2026-10-03-frontend-parte-1-precio-sku.md` (debe estar integrado antes de verificar contra el backend real; los tests de este plan no lo requieren).

## Global Constraints

- Todo archivo **nuevo** (código y tests) debe alcanzar 100% de líneas, ramas, funciones y sentencias en Vitest (`coverage.thresholds` global, con exclusión solo del `coverage-baseline.txt` congelado: nunca agregar archivos a esa lista; los que se reemplazan y están en ella se quitan, ver Task 6).
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan entre sí por su `index.ts`.
- Estados de carga, vacío y error visibles en cada pantalla; formularios con react-hook-form + zod, mensajes en español.
- Textos de UI exactos usados por los tests y el e2e (no cambiar): ver cada tarea.
- Backend (contratos ya implementados): `GET /api/v1/ventas/turnos/actual?terminalId=` (404 `VEN_TURNO_NO_ENCONTRADO` si no hay turno abierto), `GET /api/v1/ventas/turnos/{id}`, `POST /api/v1/ventas/turnos` `{terminalId, fondoInicial}` → 201, `POST /api/v1/ventas/turnos/{id}/cierre` `{totalDeclarado, observacion?}` → 200. `TurnoResponse`: `id, terminalId, establecimientoId, cajeroId, aperturaAt, fondoInicial, estado ('ABIERTO'|'EN_ARQUEO'|'CERRADO'|'ANULADO'), cierreAt, totalVentasSistema, totalSistema, totalDeclarado, diferencia, observacionCierre` (los de cierre son `null` mientras está abierto). Errores: 409 `VEN_TURNO_YA_ABIERTO`, 409 `VEN_TERMINAL_NO_OPERABLE`, 404 `VEN_TERMINAL_NO_ENCONTRADA`, 409 `VEN_TURNO_NO_ABIERTO`.
- Dinero: `number` en JSON; se muestra con `formatoMoneda` (PEN, `es-PE`). Montos de formulario: texto con hasta 2 decimales (`/^\d{1,9}(\.\d{1,2})?$/`).
- Comandos desde `frontend/` (en PowerShell si `pnpm.ps1` está bloqueado usar `pnpm.cmd`): un archivo `pnpm --filter @boticas/erp-web test -- <ruta>`; todo `pnpm check`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan falla por una diferencia menor de nombres o textos reales del repo (p. ej. un `label` distinto), ajustar el test al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Mover la idempotencia a `shared/lib` y agregar utilidades compartidas

**Files:**
- Move (con `git mv`): `SRC/features/inventario/lib/idempotencia.ts` → `SRC/shared/lib/idempotencia.ts`; `idempotencia.test.ts`; `use-clave-idempotencia.ts`; `use-clave-idempotencia.test.tsx` (los cuatro).
- Modify: `SRC/features/inventario/components/AjusteDialog.tsx` (import)
- Modify: `SRC/shared/lib/format.ts`, `SRC/shared/lib/format.test.ts`
- Create: `SRC/shared/lib/use-preferencia-local.ts`, `SRC/shared/lib/use-preferencia-local.test.tsx`

**Interfaces:**
- Produces: `nuevaClaveIdempotencia(): string` y `useClaveIdempotencia(): (payload: unknown) => string` en `shared/lib`; `formatoMoneda(valor: number): string`; `formatoFechaHora(instante: string | null): string`; `usePreferenciaLocal<T>(clave: string, inicial: T): readonly [T, (valor: T) => void]`.

- [ ] **Step 1: Mover los cuatro archivos**

```bash
cd frontend/apps/erp-web/src
git mv features/inventario/lib/idempotencia.ts shared/lib/idempotencia.ts
git mv features/inventario/lib/idempotencia.test.ts shared/lib/idempotencia.test.ts
git mv features/inventario/lib/use-clave-idempotencia.ts shared/lib/use-clave-idempotencia.ts
git mv features/inventario/lib/use-clave-idempotencia.test.tsx shared/lib/use-clave-idempotencia.test.tsx
```
En `AjusteDialog.tsx` cambiar `import { useClaveIdempotencia } from '../lib/use-clave-idempotencia';` por `import { useClaveIdempotencia } from '../../../shared/lib/use-clave-idempotencia';`. Los imports relativos internos de los archivos movidos (`./idempotencia`) siguen válidos.

- [ ] **Step 2: Escribir los tests que fallan**

Agregar a `SRC/shared/lib/format.test.ts` (conservar los tests existentes):

```ts
import { formatoFechaHora, formatoMoneda } from './format';

describe('formatoMoneda', () => {
  it('formatea en soles con dos decimales', () => {
    expect(formatoMoneda(1234.5).replace(/\s/g, ' ')).toBe('S/ 1,234.50');
    expect(formatoMoneda(0).replace(/\s/g, ' ')).toBe('S/ 0.00');
  });
});

describe('formatoFechaHora', () => {
  it('devuelve un guion cuando no hay instante', () => {
    expect(formatoFechaHora(null)).toBe('—');
  });

  it('formatea el instante con la configuración regional peruana', () => {
    expect(formatoFechaHora('2026-10-03T15:30:00Z')).toBe(
      new Date('2026-10-03T15:30:00Z').toLocaleString('es-PE')
    );
  });
});
```
(si el archivo ya importa `valueOrDash, yesNo` de `./format`, unir los imports.)

Crear `SRC/shared/lib/use-preferencia-local.test.tsx`:

```tsx
import { act, renderHook } from '@testing-library/react';
import { usePreferenciaLocal } from './use-preferencia-local';

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('usePreferenciaLocal', () => {
  it('usa el valor inicial cuando no hay nada guardado', () => {
    const { result } = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));

    expect(result.current[0]).toEqual({ a: 1 });
  });

  it('guarda y recupera el valor entre montajes', () => {
    const primera = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));
    act(() => primera.result.current[1]({ a: 2 }));
    primera.unmount();

    const segunda = renderHook(() => usePreferenciaLocal('clave', { a: 1 }));

    expect(segunda.result.current[0]).toEqual({ a: 2 });
  });

  it('ignora un valor guardado que no es JSON válido', () => {
    localStorage.setItem('clave', '{roto');

    const { result } = renderHook(() => usePreferenciaLocal('clave', 'inicial'));

    expect(result.current[0]).toBe('inicial');
  });

  it('sigue funcionando en memoria cuando el almacenamiento falla', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const { result } = renderHook(() => usePreferenciaLocal('clave', 'inicial'));

    act(() => result.current[1]('nuevo'));

    expect(result.current[0]).toBe('nuevo');
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/shared/lib`
Expected: FAIL (`formatoMoneda`, `formatoFechaHora` y `use-preferencia-local` no existen).

- [ ] **Step 4: Implementar**

Agregar a `SRC/shared/lib/format.ts`:

```ts
const MONEDA = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

export function formatoMoneda(valor: number): string {
  return MONEDA.format(valor);
}

export function formatoFechaHora(instante: string | null): string {
  return instante === null ? '—' : new Date(instante).toLocaleString('es-PE');
}
```
Si `es-PE` produce `S/ 1,234.50` con espacio normal o no separable, el test lo normaliza con `replace(/\s/g, ' ')`.

Crear `SRC/shared/lib/use-preferencia-local.ts`:

```ts
import { useState } from 'react';

function leer<T>(clave: string, inicial: T): T {
  try {
    const guardado = localStorage.getItem(clave);
    return guardado === null ? inicial : (JSON.parse(guardado) as T);
  } catch {
    return inicial;
  }
}

export function usePreferenciaLocal<T>(clave: string, inicial: T): readonly [T, (valor: T) => void] {
  const [valor, setValor] = useState<T>(() => leer(clave, inicial));

  return [
    valor,
    (nuevo) => {
      setValor(nuevo);
      try {
        localStorage.setItem(clave, JSON.stringify(nuevo));
      } catch {
        return;
      }
    }
  ] as const;
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/shared/lib src/features/inventario`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend
git commit -m "refactor(frontend): mover la idempotencia a shared y agregar moneda y preferencia local

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Precio de referencia y datos de venta en los tipos y el formulario de SKU

**Files:**
- Modify: `SRC/features/catalogo/api/skus.types.ts`, `SRC/features/catalogo/schemas/sku.schema.ts`, `SRC/features/catalogo/lib/sku-form.ts`, `SRC/features/catalogo/components/SkuForm.tsx`, `SRC/features/catalogo/pages/SkuDetailPage.tsx`
- Modify (tests y fixtures que construyen estos tipos): `SRC/features/catalogo/lib/sku-form.test.ts`, `SRC/features/catalogo/schemas/sku.schema.test.ts`, `SRC/features/catalogo/pages/SkuDetailPage.test.tsx`, `SRC/features/catalogo/components/SkuForm.test.tsx`, `SRC/test/inventario-fixtures.ts`

**Interfaces:**
- Produces: `SkuResumen` con `unidadVentaCodigo: string | null`, `permiteVentaFraccion: boolean`, `precioVentaReferencia: number | null` (agregados al final); `Sku.precioVentaReferencia: number | null` y `SkuPayload.precioVentaReferencia?: number | undefined` (después de `stockMaximoDefault`); campo de formulario `precioVentaReferencia`.

- [ ] **Step 1: Escribir los tests que fallan**

En `sku.schema.test.ts` agregar (con el helper `mensajes` ya usado en ese archivo):

```ts
  it('valida el precio de venta de referencia opcional con hasta 4 decimales', () => {
    expect(mensajes({ precioVentaReferencia: '' })).toEqual([]);
    expect(mensajes({ precioVentaReferencia: '12.5' })).toEqual([]);
    expect(mensajes({ precioVentaReferencia: '-1' })).toEqual([
      'El precio de venta de referencia debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ]);
    expect(mensajes({ precioVentaReferencia: '1.00001' })).toEqual([
      'El precio de venta de referencia debe ser un número mayor o igual a cero con hasta 4 decimales.'
    ]);
  });
```
En `sku-form.test.ts` agregar a las fixtures `precioVentaReferencia: null` junto a `stockMaximoDefault: null` y estos casos:

```ts
  it('envía el precio de referencia numérico o lo omite si está vacío', () => {
    expect(toSkuPayload({ ...SKU_FORM_VACIO, precioVentaReferencia: '12.5' }).precioVentaReferencia).toBe(12.5);
    expect(toSkuPayload(SKU_FORM_VACIO).precioVentaReferencia).toBeUndefined();
  });

  it('carga el precio de referencia en el formulario', () => {
    expect(toSkuFormValues({ ...skuBase, precioVentaReferencia: 12.5 }).precioVentaReferencia).toBe('12.5');
    expect(toSkuFormValues({ ...skuBase, precioVentaReferencia: null }).precioVentaReferencia).toBe('');
  });
```
(usar los nombres reales de las constantes/fixtures de ese test: el valor vacío por defecto y el SKU base ya presentes.) En `SkuDetailPage.test.tsx` agregar la aserción de que se muestra la fila `Precio de venta de referencia` con `S/ 12.50` para un SKU con `precioVentaReferencia: 12.5` y `—` cuando es `null`.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/catalogo`
Expected: FAIL (campos inexistentes; errores de tipos en fixtures).

- [ ] **Step 3: Implementar**

`skus.types.ts`: en `SkuResumen` agregar al final `unidadVentaCodigo: string | null; permiteVentaFraccion: boolean; precioVentaReferencia: number | null;`. En `Sku` agregar `precioVentaReferencia: number | null;` después de `stockMaximoDefault`. En `SkuPayload` agregar `precioVentaReferencia?: number | undefined;` después de `stockMaximoDefault`.

`sku.schema.ts`: después de `stockMaximoDefault` agregar

```ts
    precioVentaReferencia: decimal({
      etiqueta: 'El precio de venta de referencia',
      enteros: 10,
      decimales: 4,
      requerido: false,
      permiteCero: true
    }),
```

`sku-form.ts`: en el formulario vacío `precioVentaReferencia: '',` después de `stockMaximoDefault`; en `toSkuPayload` `precioVentaReferencia: numeroOpcional(values.precioVentaReferencia),`; en `toSkuFormValues` `precioVentaReferencia: numeroDeFormulario(sku.precioVentaReferencia),`, siempre después de la línea de `stockMaximoDefault`.

`SkuForm.tsx`: en el grupo `Control y stock`, después del campo `stockMaximoDefault` agregar `{ name: 'precioVentaReferencia', label: 'Precio de venta de referencia', tipo: 'decimal' },`.

`SkuDetailPage.tsx`: después de la fila `Stock máximo por defecto` agregar `['Precio de venta de referencia', sku.precioVentaReferencia === null ? '—' : formatoMoneda(sku.precioVentaReferencia)],` importando `formatoMoneda` de `../../../shared/lib/format` (adaptar a la forma real de la lista de filas del archivo).

`SRC/test/inventario-fixtures.ts`: `sampleSku` agrega `unidadVentaCodigo: 'UND', permiteVentaFraccion: false, precioVentaReferencia: 12.5`. Corregir todos los demás sitios que compilen con error de tipos (`pnpm typecheck`), agregando los campos nuevos con `null`/`false`.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/catalogo src/features/inventario && pnpm typecheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(catalogo): precio de venta de referencia en el SKU del frontend

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Organización publica terminales, puesto de trabajo y selector de terminal

**Files:**
- Create: `SRC/features/organizacion/lib/use-puesto-trabajo.ts`, `SRC/features/organizacion/lib/use-puesto-trabajo.test.tsx`
- Create: `SRC/features/organizacion/components/TerminalSelector.tsx`, `SRC/features/organizacion/components/TerminalSelector.test.tsx`
- Modify: `SRC/features/organizacion/index.ts`

**Interfaces:**
- Consumes: `terminalesQuery`, `Terminal` (ya existen en `features/organizacion/api`); `corporateStructureQuery`; `usePreferenciaLocal` (Task 1); `SelectField` de `shared/components/FormFields`.
- Produces: `PuestoTrabajo = { establecimientoId: string; terminalId: string; almacenId: string }`; `PUESTO_VACIO`; `usePuestoTrabajo(): { puesto: PuestoTrabajo; setPuesto: (puesto: PuestoTrabajo) => void }`; componente `TerminalSelector({ establecimientoId, terminalId, onChange })` con `onChange(establecimientoId: string, terminalId: string)`; el índice exporta `terminalesQuery`, `Terminal`, `usePuestoTrabajo`, `PuestoTrabajo`, `TerminalSelector`.

- [ ] **Step 1: Escribir los tests que fallan**

`use-puesto-trabajo.test.tsx`:

```tsx
import { act, renderHook } from '@testing-library/react';
import { PUESTO_VACIO, usePuestoTrabajo } from './use-puesto-trabajo';

afterEach(() => localStorage.clear());

describe('usePuestoTrabajo', () => {
  it('empieza vacío y recuerda el puesto elegido', () => {
    const primera = renderHook(() => usePuestoTrabajo());
    expect(primera.result.current.puesto).toEqual(PUESTO_VACIO);

    act(() =>
      primera.result.current.setPuesto({ establecimientoId: 'est-1', terminalId: 'term-1', almacenId: 'alm-1' })
    );
    primera.unmount();

    const segunda = renderHook(() => usePuestoTrabajo());
    expect(segunda.result.current.puesto).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1',
      almacenId: 'alm-1'
    });
  });
});
```

`TerminalSelector.test.tsx` (usar `renderRoute`, `server`, `pagina`, `sampleEstructura` y `sampleTerminal` de `test/organizacion-fixtures`; si el fixture de terminal tiene otro nombre, usar el real):

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { sampleEstructura } from '../../../test/inventario-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalSelector } from './TerminalSelector';

beforeEach(() => {
  server.use(
    http.get('*/api/v1/estructura-corporativa', () => HttpResponse.json(sampleEstructura)),
    http.get('*/api/v1/organizacion/terminales-pos', ({ request }) => {
      const establecimiento = new URL(request.url).searchParams.get('establecimientoId');
      return HttpResponse.json(
        pagina(
          establecimiento === 'est-1'
            ? [
                { ...sampleTerminal, id: 'term-1', nombre: 'Caja 1', estado: 'ACTIVO' },
                { ...sampleTerminal, id: 'term-2', nombre: 'Caja 2', estado: 'BLOQUEADO' }
              ]
            : []
        )
      );
    })
  );
});

function renderSelector(establecimientoId: string, terminalId: string, onChange = vi.fn()) {
  const Selector = () => (
    <TerminalSelector establecimientoId={establecimientoId} terminalId={terminalId} onChange={onChange} />
  );
  return { onChange, ...renderRoute('/x', Selector, '/x') };
}

describe('TerminalSelector', () => {
  it('lista los establecimientos y, al elegir uno, notifica el cambio sin terminal', async () => {
    const { onChange, user } = renderSelector('', '');

    await user.selectOptions(await screen.findByLabelText('Establecimiento'), 'est-1');

    expect(onChange).toHaveBeenCalledWith('est-1', '');
  });

  it('muestra solo las terminales activas del establecimiento y notifica la elegida', async () => {
    const { onChange, user } = renderSelector('est-1', '');

    expect(await screen.findByRole('option', { name: 'Caja 1' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Caja 2' })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-1');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });

  it('deshabilita el selector de terminal mientras no hay establecimiento', async () => {
    renderSelector('', '');

    expect(await screen.findByLabelText('Terminal')).toBeDisabled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion/lib/use-puesto src/features/organizacion/components/TerminalSelector`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`use-puesto-trabajo.ts`:

```ts
import { usePreferenciaLocal } from '../../../shared/lib/use-preferencia-local';

export type PuestoTrabajo = {
  establecimientoId: string;
  terminalId: string;
  almacenId: string;
};

export const PUESTO_VACIO: PuestoTrabajo = { establecimientoId: '', terminalId: '', almacenId: '' };

export function usePuestoTrabajo() {
  const [puesto, setPuesto] = usePreferenciaLocal<PuestoTrabajo>('erp.puesto-trabajo', PUESTO_VACIO);
  return { puesto, setPuesto };
}
```

`TerminalSelector.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { SelectField } from '../../../shared/components/FormFields';
import { corporateStructureQuery } from '../api/organization.api';
import { terminalesQuery } from '../api/terminales.api';

type TerminalSelectorProps = {
  establecimientoId: string;
  terminalId: string;
  onChange: (establecimientoId: string, terminalId: string) => void;
};

export function TerminalSelector({ establecimientoId, terminalId, onChange }: TerminalSelectorProps) {
  const { data: estructura } = useQuery(corporateStructureQuery);
  const { data: terminales } = useQuery({
    ...terminalesQuery({ establecimientoId, size: 100 }),
    enabled: establecimientoId !== ''
  });
  const establecimientos = (estructura?.companies ?? []).flatMap(({ establishments }) => establishments);
  const activas = (terminales?.items ?? []).filter(({ estado }) => estado === 'ACTIVO');

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <SelectField
        id="puesto-establecimiento"
        label="Establecimiento"
        value={establecimientoId}
        onChange={(event) => onChange(event.target.value, '')}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="puesto-terminal"
        label="Terminal"
        value={terminalId}
        disabled={establecimientoId === ''}
        onChange={(event) => onChange(establecimientoId, event.target.value)}
      >
        <option value="">Selecciona una terminal</option>
        {activas.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
          </option>
        ))}
      </SelectField>
    </div>
  );
}
```
`index.ts` de organización agregar:

```ts
export { terminalesQuery } from './api/terminales.api';
export type { Terminal } from './api/terminales.types';
export { usePuestoTrabajo, PUESTO_VACIO, type PuestoTrabajo } from './lib/use-puesto-trabajo';
export { TerminalSelector } from './components/TerminalSelector';
```
Si `Pagina`/`terminalesQuery` ya aceptan `size` solo mediante `ParametrosLista`, el parámetro `size: 100` es válido.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion && pnpm typecheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(organizacion): publicar terminales, puesto de trabajo y selector de terminal

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: API de caja (turnos), tipos, fixtures y utilidad de diferencia

**Files:**
- Create: `SRC/features/caja/api/caja.types.ts`, `SRC/features/caja/api/turnos.api.ts`, `SRC/features/caja/api/turnos.api.test.ts`, `SRC/features/caja/api/invalidate.ts`, `SRC/features/caja/api/invalidate.test.ts`
- Create: `SRC/features/caja/lib/arqueo.ts`, `SRC/features/caja/lib/arqueo.test.ts`
- Create: `SRC/test/ventas-fixtures.ts`

**Interfaces:**
- Produces: tipos `Turno`, `AbrirTurnoPayload`, `CerrarTurnoPayload`; `fetchTurnoActual(client, terminalId): Promise<Turno | null>` (404 → `null`); `turnoActualQuery(terminalId)` con clave `['caja', 'turno-actual', terminalId]`; `abrirTurno(client, payload)`; `cerrarTurno(client, turnoId, payload)`; `invalidateCaja(queryClient)` (clave `['caja']`); `diferenciaArqueo(totalSistema: number, totalDeclarado: number): number` (redondeo a 2 decimales); fixtures `sampleTurno`, `sampleTurnoCerrado`.

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/test/ventas-fixtures.ts` (se ampliará en los planes 3 y 4):

```ts
import type { Turno } from '../features/caja/api/caja.types';

export const sampleTurno: Turno = {
  id: 'turno-1',
  terminalId: 'term-1',
  establecimientoId: 'est-1',
  cajeroId: 'user-1',
  aperturaAt: '2026-10-03T13:00:00Z',
  fondoInicial: 100,
  estado: 'ABIERTO',
  cierreAt: null,
  totalVentasSistema: null,
  totalSistema: null,
  totalDeclarado: null,
  diferencia: null,
  observacionCierre: null
};

export const sampleTurnoCerrado: Turno = {
  ...sampleTurno,
  estado: 'CERRADO',
  cierreAt: '2026-10-03T21:00:00Z',
  totalVentasSistema: 250,
  totalSistema: 350,
  totalDeclarado: 348.5,
  diferencia: -1.5,
  observacionCierre: 'Faltante de monedas'
};
```

`arqueo.test.ts`:

```ts
import { diferenciaArqueo } from './arqueo';

describe('diferenciaArqueo', () => {
  it('es declarado menos sistema redondeado a dos decimales', () => {
    expect(diferenciaArqueo(350, 348.5)).toBe(-1.5);
    expect(diferenciaArqueo(100, 100)).toBe(0);
    expect(diferenciaArqueo(0.1, 0.30000000000000004)).toBe(0.2);
  });
});
```

`turnos.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleTurno, sampleTurnoCerrado } from '../../../test/ventas-fixtures';
import { abrirTurno, cerrarTurno, fetchTurnoActual, turnoActualQuery } from './turnos.api';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

describe('turnos.api', () => {
  it('fetchTurnoActual consulta por terminal y devuelve el turno', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(sampleTurno);
      })
    );

    await expect(fetchTurnoActual(client, 'term-1')).resolves.toEqual(sampleTurno);
    expect(recibido?.searchParams.get('terminalId')).toBe('term-1');
  });

  it('fetchTurnoActual devuelve null cuando la terminal no tiene turno abierto', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', () =>
        HttpResponse.json({ title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' }, { status: 404 })
      )
    );

    await expect(fetchTurnoActual(client, 'term-1')).resolves.toBeNull();
  });

  it('fetchTurnoActual propaga los demás errores', async () => {
    server.use(
      http.get('http://localhost/api/v1/ventas/turnos/actual', () =>
        HttpResponse.json({ title: 'Error' }, { status: 500 })
      )
    );

    await expect(fetchTurnoActual(client, 'term-1')).rejects.toBeInstanceOf(ApiError);
  });

  it('abrirTurno envía terminal y fondo inicial', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/ventas/turnos', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleTurno, { status: 201 });
      })
    );

    await expect(abrirTurno(client, { terminalId: 'term-1', fondoInicial: 100 })).resolves.toEqual(sampleTurno);
    expect(body).toEqual({ terminalId: 'term-1', fondoInicial: 100 });
  });

  it('cerrarTurno envía el total declarado y la observación', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/ventas/turnos/turno-1/cierre', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleTurnoCerrado);
      })
    );

    await expect(
      cerrarTurno(client, 'turno-1', { totalDeclarado: 348.5, observacion: 'Faltante de monedas' })
    ).resolves.toEqual(sampleTurnoCerrado);
    expect(body).toEqual({ totalDeclarado: 348.5, observacion: 'Faltante de monedas' });
  });

  it('turnoActualQuery arma la clave y consulta con el cliente de la aplicación', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(sampleTurno);

    const query = turnoActualQuery('term-1');
    const result = await new QueryClient().fetchQuery(query);

    expect(query.queryKey).toEqual(['caja', 'turno-actual', 'term-1']);
    expect(get).toHaveBeenCalledWith('/ventas/turnos/actual?terminalId=term-1');
    expect(result).toEqual(sampleTurno);
  });
});
```

`invalidate.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { invalidateCaja } from './invalidate';

describe('invalidateCaja', () => {
  it('invalida todas las consultas de caja', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateCaja(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['caja'] });
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/caja`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`caja.types.ts`:

```ts
export const ESTADOS_TURNO = ['ABIERTO', 'EN_ARQUEO', 'CERRADO', 'ANULADO'] as const;

export type EstadoTurno = (typeof ESTADOS_TURNO)[number];

export type Turno = {
  id: string;
  terminalId: string;
  establecimientoId: string;
  cajeroId: string;
  aperturaAt: string;
  fondoInicial: number;
  estado: EstadoTurno;
  cierreAt: string | null;
  totalVentasSistema: number | null;
  totalSistema: number | null;
  totalDeclarado: number | null;
  diferencia: number | null;
  observacionCierre: string | null;
};

export type AbrirTurnoPayload = {
  terminalId: string;
  fondoInicial: number;
};

export type CerrarTurnoPayload = {
  totalDeclarado: number;
  observacion?: string | undefined;
};
```

`turnos.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import { ApiError, type ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import { withQuery } from '../../../shared/lib/query-string';
import type { AbrirTurnoPayload, CerrarTurnoPayload, Turno } from './caja.types';

export async function fetchTurnoActual(client: ApiClient, terminalId: string): Promise<Turno | null> {
  try {
    return await client.get<Turno>(withQuery('/ventas/turnos/actual', { terminalId }));
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export function turnoActualQuery(terminalId: string) {
  return queryOptions({
    queryKey: ['caja', 'turno-actual', terminalId],
    queryFn: () => fetchTurnoActual(apiClient, terminalId)
  });
}

export function abrirTurno(client: ApiClient, payload: AbrirTurnoPayload): Promise<Turno> {
  return client.post<Turno, AbrirTurnoPayload>('/ventas/turnos', payload);
}

export function cerrarTurno(
  client: ApiClient,
  turnoId: string,
  payload: CerrarTurnoPayload
): Promise<Turno> {
  return client.post<Turno, CerrarTurnoPayload>(`/ventas/turnos/${turnoId}/cierre`, payload);
}
```

`invalidate.ts`:

```ts
import type { QueryClient } from '@tanstack/react-query';

export function invalidateCaja(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['caja'] });
}
```

`arqueo.ts`:

```ts
export const diferenciaArqueo = (totalSistema: number, totalDeclarado: number): number =>
  Math.round((totalDeclarado - totalSistema) * 100) / 100;
```
Nota: si `client.get` del `ApiClient` reportara el estado en otra propiedad que `status`, usar la real (ver `packages/api-client/src/api-error.ts`; `describeApiError` ya usa `error.status`).

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/caja && pnpm typecheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(caja): api de turnos, tipos y arqueo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Pantalla de caja (apertura, resumen y cierre)

**Files:**
- Create: `SRC/features/caja/schemas/turno.schema.ts`, `SRC/features/caja/schemas/turno.schema.test.ts`
- Create: `SRC/features/caja/components/AbrirTurnoForm.tsx` y `.test.tsx`, `ResumenTurno.tsx` y `.test.tsx`, `CerrarTurnoForm.tsx` y `.test.tsx`, `ResultadoCierre.tsx` y `.test.tsx`
- Create: `SRC/features/caja/lib/use-mutacion-caja.ts` y `.test.tsx`
- Modify (reemplazar contenido): `SRC/features/caja/pages/CashRegisterPage.tsx`; Create: `SRC/features/caja/pages/CashRegisterPage.test.tsx`
- Modify: `SRC/features/caja/index.ts`

**Interfaces:**
- Consumes: Task 3 (`TerminalSelector`, `usePuestoTrabajo`), Task 4 (API, tipos, `diferenciaArqueo`, `invalidateCaja`), `formatoMoneda`, `formatoFechaHora` (Task 1), `describeApiError`, `FormError`, `TextField`, `DatoItem`.
- Produces: `abrirTurnoSchema`/`cerrarTurnoSchema` con valores `{ fondoInicial: string }` y `{ totalDeclarado: string; observacion: string }`; `useMutacionCaja(mutationFn, onSuccess?)` análogo a `useMutacionInventario` pero invalida `['caja']`; `index.ts` exporta `cashRegisterRoutes`, `turnoActualQuery`, `invalidateCaja` y el tipo `Turno`.
- Textos de UI exactos: títulos `Caja`; botones `Abrir turno`, `Cerrar turno`; etiquetas `Fondo inicial`, `Total declarado`, `Observación`; mensajes `No hay un turno abierto en esta terminal.`, `Selecciona una terminal para ver su turno.`, `No se pudo cargar el turno.`; resultado de cierre con encabezado `Turno cerrado` y filas `Total del sistema`, `Total declarado`, `Diferencia`.

- [ ] **Step 1: Escribir los tests que fallan**

`turno.schema.test.ts`:

```ts
import { abrirTurnoSchema, cerrarTurnoSchema } from './turno.schema';

const mensajes = (resultado: { success: boolean; error?: { issues: { message: string }[] } }) =>
  resultado.success ? [] : (resultado.error?.issues.map(({ message }) => message) ?? []);

describe('abrirTurnoSchema', () => {
  it.each(['0', '100', '100.5', '100.50'])('acepta el fondo %s', (fondoInicial) => {
    expect(abrirTurnoSchema.safeParse({ fondoInicial }).success).toBe(true);
  });

  it.each(['', '-1', '1.234', 'abc'])('rechaza el fondo "%s"', (fondoInicial) => {
    expect(mensajes(abrirTurnoSchema.safeParse({ fondoInicial }))).toEqual([
      'El fondo inicial debe ser un monto mayor o igual a cero con hasta 2 decimales.'
    ]);
  });
});

describe('cerrarTurnoSchema', () => {
  it('acepta un total declarado válido con o sin observación', () => {
    expect(cerrarTurnoSchema.safeParse({ totalDeclarado: '348.50', observacion: '' }).success).toBe(true);
    expect(cerrarTurnoSchema.safeParse({ totalDeclarado: '0', observacion: 'Todo cuadra' }).success).toBe(true);
  });

  it('rechaza un total declarado inválido', () => {
    expect(mensajes(cerrarTurnoSchema.safeParse({ totalDeclarado: '', observacion: '' }))).toEqual([
      'El total declarado debe ser un monto mayor o igual a cero con hasta 2 decimales.'
    ]);
  });

  it('rechaza una observación de más de 1000 caracteres', () => {
    expect(
      mensajes(cerrarTurnoSchema.safeParse({ totalDeclarado: '1', observacion: 'x'.repeat(1001) }))
    ).toEqual(['La observación no debe exceder 1000 caracteres.']);
  });
});
```

Tests de componentes y página (casos obligatorios; escribirlos siguiendo el estilo de `AjusteDialog.test.tsx`/`InventoryPage.test.tsx`, con `renderRoute`, `server` de MSW, `sampleEstructura`, `sampleTerminal`, `sampleTurno`, `sampleTurnoCerrado`):

- `AbrirTurnoForm.test.tsx`: (a) envía `{ fondoInicial: '150.50' }` como valores al `onSubmit` al pulsar `Abrir turno`; (b) con fondo vacío muestra `El fondo inicial debe ser un monto mayor o igual a cero con hasta 2 decimales.` y no llama a `onSubmit`; (c) muestra `error` en un `role="alert"` y deshabilita el botón cuando `isSubmitting`.
- `ResumenTurno.test.tsx`: muestra fondo (`S/ 100.00`), apertura formateada, cajero (`user-1`) y estado `ABIERTO`; muestra el total del sistema `—` mientras el turno está abierto.
- `CerrarTurnoForm.test.tsx`: (a) al escribir `Total declarado` se muestra la diferencia en vivo `Diferencia: -S/ 1.50` (formato `formatoMoneda` con signo) para `totalSistemaEstimado=350` y `348.5`; con total declarado vacío no muestra diferencia; (b) `onSubmit` recibe `{ totalDeclarado: '348.5', observacion: 'Faltante' }`; (c) validación y `error`/`isSubmitting` como el formulario de apertura. Props: `{ totalEstimado: number; isSubmitting; error; onSubmit }` donde `totalEstimado = fondoInicial` (el sistema real se informa tras cerrar; la diferencia en vivo se calcula contra `totalEstimado` y se rotula `Diferencia estimada`). **Resolver el rótulo exacto como `Diferencia estimada: <monto>` en el test y en el componente.**
- `ResultadoCierre.test.tsx`: con `sampleTurnoCerrado` muestra `Turno cerrado`, `Total del sistema` `S/ 350.00`, `Total declarado` `S/ 348.50`, `Diferencia` `-S/ 1.50` y la observación; con `diferencia` `0` muestra `S/ 0.00`.
- `use-mutacion-caja.test.tsx`: al resolver llama `onSuccess`, invalida `['caja']`; ante error expone `mensajeError` con `describeApiError`; `cerrar` no actúa mientras `isPending` (si el hook expone `cerrar`; si no se necesita, no exponerlo).
- `CashRegisterPage.test.tsx`: (a) sin terminal elegida muestra `Selecciona una terminal para ver su turno.` y el selector; (b) con puesto guardado en `localStorage` (`erp.puesto-trabajo`) y `GET turnos/actual` 404 → `No hay un turno abierto en esta terminal.` y formulario `Abrir turno`; al enviar `150.5` hace `POST /ventas/turnos` con `{ terminalId: 'term-1', fondoInicial: 150.5 }` y vuelve a consultar mostrando el resumen del turno abierto; (c) con turno abierto muestra el resumen y `Cerrar turno`; al enviar `348.5` con observación hace `POST /ventas/turnos/turno-1/cierre` con `{ totalDeclarado: 348.5, observacion: 'Faltante' }` y muestra `Turno cerrado` con la diferencia; (d) error del backend en la apertura (409 `VEN_TURNO_YA_ABIERTO`) se muestra en un `role="alert"`; (e) error 500 de la consulta → `No se pudo cargar el turno.`.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/caja`
Expected: FAIL.

- [ ] **Step 3: Implementar**

`turno.schema.ts`:

```ts
import { z } from 'zod';

const OBSERVACION_MAX = 1000;
export const PATRON_MONTO = /^\d{1,9}(\.\d{1,2})?$/;

const monto = (etiqueta: string) =>
  z
    .string()
    .refine(
      (valor) => PATRON_MONTO.test(valor),
      `${etiqueta} debe ser un monto mayor o igual a cero con hasta 2 decimales.`
    );

export const abrirTurnoSchema = z.object({ fondoInicial: monto('El fondo inicial') });

export const cerrarTurnoSchema = z.object({
  totalDeclarado: monto('El total declarado'),
  observacion: z.string().max(OBSERVACION_MAX, `La observación no debe exceder ${OBSERVACION_MAX} caracteres.`)
});

export type AbrirTurnoFormValues = z.infer<typeof abrirTurnoSchema>;
export type CerrarTurnoFormValues = z.infer<typeof cerrarTurnoSchema>;
```
`use-mutacion-caja.ts`:

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { invalidateCaja } from '../api/invalidate';

export function useMutacionCaja<TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>
) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn,
    onSuccess: () => invalidateCaja(queryClient)
  });

  return {
    mutate: mutation.mutate,
    data: mutation.data,
    isPending: mutation.isPending,
    mensajeError: mutation.isError ? describeApiError(mutation.error) : null
  };
}
```
(ajustar `use-mutacion-caja.test.tsx` a esta superficie: `mutate`, `data`, `isPending`, `mensajeError`.)

`AbrirTurnoForm.tsx` (patrón de `AjusteForm`): `useForm<AbrirTurnoFormValues>` con `defaultValues: { fondoInicial: '' }`, `zodResolver(abrirTurnoSchema)`, `mode: 'onTouched'`; `TextField id="turno-fondo" label="Fondo inicial" inputMode="decimal"`; `FormError` con `error`; `Button type="submit" disabled={isSubmitting}` con texto `Abrir turno`. Props `{ isSubmitting: boolean; error: string | null; onSubmit: (values: AbrirTurnoFormValues) => void }`.

`CerrarTurnoForm.tsx`: igual con `cerrarTurnoSchema`, campos `Total declarado` (`id="turno-declarado"`) y `Observación` (`id="turno-observacion"`), `useWatch` de `totalDeclarado`; si coincide con `PATRON_MONTO` muestra `Diferencia estimada: {formatoMoneda(diferenciaArqueo(totalEstimado, Number(valor)))}`; botón `Cerrar turno`. Props `{ totalEstimado: number; isSubmitting; error; onSubmit }`. Importa `PATRON_MONTO` de `turno.schema.ts` (no duplicarlo).

`ResumenTurno.tsx`: `Card` con `dl` de `DatoItem` (`Estado`, `Fondo inicial` con `formatoMoneda`, `Apertura` con `formatoFechaHora`, `Cajero` con `cajeroId`, `Total del sistema` con `—` cuando `totalSistema` es `null`, si no `formatoMoneda`).

`ResultadoCierre.tsx`: `Card` con encabezado `Turno cerrado` y `DatoItem` `Total del sistema`, `Total declarado`, `Diferencia`, `Observación` (con `valueOrDash`), usando `formatoMoneda` (diferencia negativa como `-S/ 1.50`; verificar con `Intl` real y ajustar el test si el signo se coloca distinto, manteniendo la verificación del valor).

`CashRegisterPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { TerminalSelector, usePuestoTrabajo } from '../../organizacion';
import { abrirTurno, cerrarTurno, turnoActualQuery } from '../api/turnos.api';
import { AbrirTurnoForm } from '../components/AbrirTurnoForm';
import { CerrarTurnoForm } from '../components/CerrarTurnoForm';
import { ResultadoCierre } from '../components/ResultadoCierre';
import { ResumenTurno } from '../components/ResumenTurno';
import { useMutacionCaja } from '../lib/use-mutacion-caja';
import type { CerrarTurnoFormValues } from '../schemas/turno.schema';

export function CashRegisterPage() {
  const { puesto, setPuesto } = usePuestoTrabajo();
  const { data: turno, isPending, isError } = useQuery({
    ...turnoActualQuery(puesto.terminalId),
    enabled: puesto.terminalId !== ''
  });
  const apertura = useMutacionCaja((valores: { fondoInicial: string }) =>
    abrirTurno(apiClient, { terminalId: puesto.terminalId, fondoInicial: Number(valores.fondoInicial) })
  );
  const cierre = useMutacionCaja((valores: CerrarTurnoFormValues) =>
    cerrarTurno(apiClient, turno?.id ?? '', {
      totalDeclarado: Number(valores.totalDeclarado),
      observacion: valores.observacion.trim() === '' ? undefined : valores.observacion.trim()
    })
  );
  ...
}
```
Completar el cuerpo así: `PageHeader title="Caja" context="Operaciones / Caja" description="Apertura y cierre del turno de la terminal."`; `Card` con `TerminalSelector` cuyo `onChange` hace `setPuesto({ ...puesto, establecimientoId, terminalId })` (el `almacenId` se conserva solo si el establecimiento no cambia: `almacenId: establecimientoId === puesto.establecimientoId ? puesto.almacenId : ''`). Ramas de render, en este orden: sin terminal → texto `Selecciona una terminal para ver su turno.`; `isPending` → `Cargando…`; `isError` → `No se pudo cargar el turno.`; `cierre.data` (resultado reciente) → `ResultadoCierre` con `cierre.data`; `turno === null` → texto `No hay un turno abierto en esta terminal.` + `AbrirTurnoForm` (con `apertura.mensajeError`/`isPending`); si no, `ResumenTurno` + `CerrarTurnoForm` con `totalEstimado={turno.fondoInicial}`. Al cerrar con éxito, `ResultadoCierre` permanece visible aunque la consulta de turno actual vuelva a `null` (por eso `cierre.data` se evalúa primero); al cambiar de terminal debe desaparecer: usar `key={puesto.terminalId}` en el contenedor del estado de la mutación o descartar `cierre.data` si `cierre.data.terminalId !== puesto.terminalId`.

`index.ts`:

```ts
export { cashRegisterRoutes } from './routes';
export { turnoActualQuery } from './api/turnos.api';
export { invalidateCaja } from './api/invalidate';
export type { Turno } from './api/caja.types';
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/caja && pnpm typecheck && pnpm lint`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(caja): pantalla de apertura, resumen y cierre de turno

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Rutas de caja con test, baseline de cobertura y e2e

**Files:**
- Create: `SRC/features/caja/routes.test.ts`
- Modify: `frontend/coverage-baseline.txt` (quitar la línea `apps/erp-web/src/features/caja/routes.tsx`)
- Create: `E2E/support/caja-api.ts`, `E2E/caja.spec.ts`

**Interfaces:**
- Consumes: `cashRegisterRoutes`, `mockAuthApi`/`login`/`json` de `E2E/support/login.ts`, `sampleEstructura` y `sampleTurno*` de las fixtures.
- Produces: `abrirCajaEn(page, path)` (mock de la API de caja + login + `goto`), usado también por los planes 3 y 4 (el mock se amplía allí).

- [ ] **Step 1: Escribir los tests**

`SRC/features/caja/routes.test.ts`:

```ts
import { cashRegisterRoutes } from './routes';

describe('cashRegisterRoutes', () => {
  it('declara la ruta de caja', () => {
    expect(cashRegisterRoutes.map(({ path }) => path)).toEqual(['caja']);
  });

  it.each(cashRegisterRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
```

`E2E/support/caja-api.ts`:

```ts
import type { Page } from '@playwright/test';
import { sampleEstructura } from '../../apps/erp-web/src/test/inventario-fixtures';
import { sampleTurno, sampleTurnoCerrado } from '../../apps/erp-web/src/test/ventas-fixtures';
import type { Turno } from '../../apps/erp-web/src/features/caja/api/caja.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

const terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'T01',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export async function mockCajaApi(page: Page) {
  let turno: Turno | null = null;

  await page.route('**/api/v1/estructura-corporativa', (route) => json(route, 200, sampleEstructura));
  await page.route(/\/api\/v1\/organizacion\/terminales-pos(\?|$)/, (route) =>
    json(route, 200, pagina([terminal]))
  );
  await page.route('**/api/v1/ventas/turnos/**', async (route) => {
    const request = route.request();
    const body = request.postData() ? (JSON.parse(request.postData() ?? '{}') as Record<string, unknown>) : {};
    if (request.url().includes('/cierre')) {
      turno = {
        ...sampleTurnoCerrado,
        totalDeclarado: Number(body['totalDeclarado']),
        diferencia: Number(body['totalDeclarado']) - sampleTurnoCerrado.totalSistema!,
        observacionCierre: (body['observacion'] as string | undefined) ?? null
      };
      const cerrado = turno;
      turno = null;
      return json(route, 200, cerrado);
    }
    return turno
      ? json(route, 200, turno)
      : json(route, 404, { title: 'No encontrado', code: 'VEN_TURNO_NO_ENCONTRADO' });
  });
  await page.route(/\/api\/v1\/ventas\/turnos$/, async (route) => {
    const body = JSON.parse(route.request().postData() ?? '{}') as Record<string, unknown>;
    turno = { ...sampleTurno, fondoInicial: Number(body['fondoInicial']) };
    return json(route, 201, turno);
  });
}

export async function abrirCajaEn(page: Page, path: string) {
  await mockCajaApi(page);
  await login(page);
  await page.goto(path);
}
```
Playwright evalúa los `route` en orden inverso de registro; el patrón `**/ventas/turnos/**` no coincide con `/ventas/turnos` (sin barra final), por lo que `POST /ventas/turnos` lo atiende el último registrado. Verificarlo al ejecutar y reordenar si hace falta.

`E2E/caja.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { abrirCajaEn } from './support/caja-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Caja', () => {
  test('abre y cierra el turno de una terminal', async ({ page }) => {
    await abrirCajaEn(page, '/caja');

    await expect(page.getByRole('heading', { name: 'Caja', exact: true })).toBeVisible();
    await page.getByLabel('Establecimiento').selectOption('est-1');
    await page.getByLabel('Terminal').selectOption('term-1');
    await expect(page.getByText('No hay un turno abierto en esta terminal.')).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByLabel('Fondo inicial').fill('100');
    await page.getByRole('button', { name: 'Abrir turno' }).click();
    await expect(page.getByRole('button', { name: 'Cerrar turno' })).toBeVisible();

    await page.getByLabel('Total declarado').fill('348.5');
    await page.getByLabel('Observación').fill('Faltante de monedas');
    await page.getByRole('button', { name: 'Cerrar turno' }).click();

    await expect(page.getByText('Turno cerrado')).toBeVisible();
    await expect(page.getByText('Faltante de monedas')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar**

Run: `pnpm --filter @boticas/erp-web test -- src/features/caja/routes` → PASS; luego `pnpm test:e2e -- caja` (usar el script de e2e de `frontend/package.json`; si el nombre difiere, ver `scripts` y usar el que ejecuta Playwright) → PASS en desktop, tablet y móvil.

- [ ] **Step 3: Quitar la línea de baseline y verificar todo**

Eliminar `apps/erp-web/src/features/caja/routes.tsx` de `frontend/coverage-baseline.txt`. Run: `pnpm check`
Expected: lint, typecheck, tests con cobertura 100% en los archivos nuevos y build en verde.

- [ ] **Step 4: Commit**

```bash
git add frontend
git commit -m "test(caja): rutas con lazy loading, baseline y e2e de apertura y cierre

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (secciones caja y base compartida):** idempotencia a `shared` (Task 1); precio de referencia en el SKU del frontend (Task 2, necesario para que el cajero lo vea y edite); query del turno actual publicada por `index.ts` (Tasks 4–5); página de caja con apertura, resumen, cierre con diferencia y resultado final (Task 5); estados de carga/vacío/error (Task 5); e2e en desktop/tablet/móvil con `page.route` (Task 6); `pnpm check` en verde (Task 6). La selección de terminal recordada en el navegador (que el spec asigna al POS) se resuelve aquí en `usePuestoTrabajo` por ser compartida con caja; el POS solo añade el almacén.

**Escaneo de placeholders:** el Task 5 describe los tests de componentes y de la página como casos obligatorios con valores concretos en lugar de código completo; el implementador debe escribirlos siguiendo `AjusteDialog.test.tsx`. No quedan "TBD"; las dos notas de verificación (formato `-S/ 1.50` y orden de rutas de Playwright) indican qué comprobar y cómo ajustar sin bajar la cobertura.

**Consistencia de tipos:** `Turno`, `AbrirTurnoPayload`, `CerrarTurnoPayload`, `turnoActualQuery`, `invalidateCaja`, `PuestoTrabajo { establecimientoId, terminalId, almacenId }` y `TerminalSelector` se usan con los mismos nombres y firmas en los planes 3 y 4; `formatoMoneda` y `formatoFechaHora` de `shared/lib/format` son la fuente única de formato monetario y de fecha.
