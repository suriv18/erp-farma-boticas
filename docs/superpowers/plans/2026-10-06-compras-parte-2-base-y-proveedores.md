# Compras, parte 2: base y proveedores (frontend) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el placeholder de `/compras` por un hub y entregar la gestión completa de proveedores (listado con filtros en la URL, alta, edición y cambio de estado) integrada contra `/api/v1/compras/proveedores`, dejando la base compartida (`api/invalidate`, traducción de errores `COM_*`, fixtures) que usarán las órdenes y las recepciones.

**Architecture:** `features/compras` sigue la estructura de `inventario` y `ventas` (`api/`, `lib/`, `schemas/`, `components/`, `pages/`, `routes.tsx`, `index.ts`). Los helpers que hoy viven privados en `organizacion` y que compras necesita (`CambiarEstadoDialog`, campos de esquema y utilidades de formulario) se mueven a `shared/` sin cambiar su comportamiento. Los formularios usan react-hook-form + zod con límites espejo del backend.

**Tech Stack:** React 19.2, React Router 8, TanStack Query, react-hook-form + zod, `@boticas/ui-web`, Vitest + Testing Library + MSW, Playwright.

Spec: `docs/superpowers/specs/2026-10-06-frontend-compras-design.md`. No depende de la parte 1 (backend de recepciones).

## Global Constraints

- Todo archivo **nuevo** (código y tests) alcanza 100% de líneas, ramas, funciones y sentencias; los archivos movidos conservan su cobertura; quitar `apps/erp-web/src/features/compras/routes.tsx` de `frontend/coverage-baseline.txt` (Task 5) y no agregar archivos a ese archivo.
- Sin comentarios en el código; sin duplicación; `forwardRef` y React Router 8 bloqueados por ESLint; las features solo se importan por su `index.ts`.
- Reglas espejo del backend para proveedor: `tipoDocumento` hasta 2 caracteres (vacío equivale a `6`, RUC); si el tipo es vacío o `6`, el documento debe cumplir `^(10|20)\d{9}$`; `numeroDocumento` de 1 a 15 caracteres; `razonSocial` de 1 a 300; `nombreComercial` hasta 300; `direccion` hasta 500; `ubigeo` de 6 dígitos o vacío; `telefono`/`contactoTelefono` hasta 40; `email`/`contactoEmail` hasta 254; `contactoNombre` hasta 180; `condicionPagoDefault` hasta 80; `diasCreditoDefault` entero `>= 0`; `monedaDefault` 3 letras mayúsculas; `calificacion` hasta 30. Valores por defecto del formulario: tipo `6`, condición `CONTADO`, días `0`, moneda `PEN`, calificación `CONFIABLE`, distribuidor marcado.
- Estados de proveedor: `ACTIVO`, `SUSPENDIDO`, `BLOQUEADO` (solo `ACTIVO` admite compras).
- Textos exactos: ver cada tarea (los usan los tests y el e2e).
- Comandos desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado): `pnpm exec vitest run <ruta>`, `pnpm typecheck`, `pnpm lint`, `pnpm check`, `pnpm e2e compras.spec.ts`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan difiere por nombres reales del repo (labels, fixtures), ajustar al comportamiento real sin reducir lo que verifica y anotarlo en el reporte.

Rutas abreviadas: `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Mover helpers de `organizacion` a `shared`

**Files:**
- Move: `SRC/features/organizacion/components/CambiarEstadoDialog.tsx` → `SRC/shared/components/CambiarEstadoDialog.tsx` (y su `.test.tsx`)
- Move: `SRC/features/organizacion/components/estilo-aviso.ts` → `SRC/shared/components/estilo-aviso.ts`
- Create: `SRC/shared/schemas/campos.ts`, `SRC/shared/lib/form-values.ts`
- Modify: `SRC/features/organizacion/components/Aviso.tsx`, `SRC/features/organizacion/pages/EmpresaDetailPage.tsx`, `SRC/features/organizacion/pages/EstablecimientoDetailPage.tsx`, `SRC/features/organizacion/schemas/campos.ts`, `SRC/features/organizacion/lib/form-values.ts`

**Interfaces:**
- Produces: `shared/components/CambiarEstadoDialog` (mismas props y comportamiento: `title`, `estados`, `current`, `consecuencias?`, `isSubmitting`, `error`, `onSubmit(estado)`, `onClose`); `shared/schemas/campos` exporta `textoOpcional(max, etiqueta)`, `correoOpcional`, `telefonoOpcional`, `ubigeoOpcional`; `shared/lib/form-values` exporta `emptyToUndefined(value: string): string | undefined` y `orEmpty(value: string | null): string`.

- [ ] **Step 1: Mover el diálogo y su estilo con git**

Run (desde `frontend/`):
```bash
git mv apps/erp-web/src/features/organizacion/components/CambiarEstadoDialog.tsx apps/erp-web/src/shared/components/CambiarEstadoDialog.tsx
git mv apps/erp-web/src/features/organizacion/components/CambiarEstadoDialog.test.tsx apps/erp-web/src/shared/components/CambiarEstadoDialog.test.tsx
git mv apps/erp-web/src/features/organizacion/components/estilo-aviso.ts apps/erp-web/src/shared/components/estilo-aviso.ts
```
El diálogo importa `./FormError`, `./estilo-aviso` y `./FormFields` (`CheckboxField`): los tres existen en `shared/components`, así que no requiere cambios de código.

- [ ] **Step 2: Actualizar los imports en organizacion**

En `SRC/features/organizacion/components/Aviso.tsx` cambiar `import { ESTILO_AVISO } from './estilo-aviso';` por:
```ts
import { ESTILO_AVISO } from '../../../shared/components/estilo-aviso';
```
En `SRC/features/organizacion/pages/EmpresaDetailPage.tsx` y `SRC/features/organizacion/pages/EstablecimientoDetailPage.tsx` cambiar `import { CambiarEstadoDialog } from '../components/CambiarEstadoDialog';` por:
```ts
import { CambiarEstadoDialog } from '../../../shared/components/CambiarEstadoDialog';
```

- [ ] **Step 3: Extraer los campos de esquema reutilizables**

Crear `SRC/shared/schemas/campos.ts`:

```ts
import { z } from 'zod';

export function textoOpcional(max: number, etiqueta: string) {
  return z.string().max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export const correoOpcional = z
  .string()
  .max(320, 'El correo no debe exceder 320 caracteres.')
  .refine((value) => value === '' || z.email().safeParse(value).success, 'El correo no es válido.');

export const ubigeoOpcional = z.string().regex(/^(\d{6})?$/, 'El ubigeo debe tener 6 dígitos.');

const TELEFONO_CARACTERES = /^[\d\s+\-()]*$/;

function tieneCantidadDigitosValida(valor: string) {
  const digitos = valor.replace(/\D/g, '').length;
  return digitos >= 6 && digitos <= 15;
}

export const telefonoOpcional = z
  .string()
  .max(40, 'El teléfono no debe exceder 40 caracteres.')
  .refine(
    (valor) =>
      valor === '' || (TELEFONO_CARACTERES.test(valor) && tieneCantidadDigitosValida(valor)),
    'El teléfono debe tener entre 6 y 15 dígitos y solo admite números, espacios, +, - y paréntesis.'
  );
```

Reemplazar `SRC/features/organizacion/schemas/campos.ts` por (conserva lo que no se movió y reexporta lo movido para no tocar los demás imports):

```ts
import { z } from 'zod';

export {
  correoOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from '../../../shared/schemas/campos';

export function nombreRequerido(max: number) {
  return z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(max, `El nombre no debe exceder ${max} caracteres.`);
}

export const codigoRequerido = z
  .string()
  .min(1, 'El código es obligatorio.')
  .max(40, 'El código no debe exceder 40 caracteres.');

export const zonaHorariaRequerida = z
  .string()
  .min(1, 'La zona horaria es obligatoria.')
  .max(80, 'La zona horaria no debe exceder 80 caracteres.');

export function numeroOpcional(etiqueta: string, limite?: number) {
  return z
    .string()
    .refine(
      (value) =>
        value === '' ||
        (Number.isFinite(Number(value)) &&
          (limite === undefined || Math.abs(Number(value)) <= limite)),
      limite === undefined
        ? `${etiqueta} debe ser un número.`
        : `${etiqueta} debe estar entre -${limite} y ${limite}.`
    );
}

export const sitioWebOpcional = z
  .string()
  .max(300, 'El sitio web no debe exceder 300 caracteres.')
  .refine(
    (valor) => valor === '' || (z.url().safeParse(valor).success && /^https?:\/\//i.test(valor)),
    'El sitio web debe ser una URL que empiece con http:// o https://.'
  );
```

- [ ] **Step 4: Extraer las utilidades de formulario**

Crear `SRC/shared/lib/form-values.ts`:

```ts
export function emptyToUndefined(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

export function orEmpty(value: string | null): string {
  return value ?? '';
}
```

Reemplazar `SRC/features/organizacion/lib/form-values.ts` por:

```ts
export { emptyToUndefined, orEmpty } from '../../../shared/lib/form-values';

export function toNumberOrUndefined(value: string): number | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : Number(trimmed);
}

export function numberOrEmpty(value: number | null): string {
  return value === null ? '' : String(value);
}

export function aMayusculasSinEspacios(value: string): string {
  return value.replace(/\s/g, '').toUpperCase();
}
```

- [ ] **Step 5: Verificar que nada cambió de comportamiento**

Run: `pnpm exec vitest run apps/erp-web/src/features/organizacion apps/erp-web/src/shared && pnpm typecheck && pnpm lint`
Expected: PASS con los mismos tests de antes (los tests de `campos` y `form-values` de organizacion siguen ejecutando los helpers movidos a través del reexport, por lo que `shared/schemas/campos.ts` y `shared/lib/form-values.ts` quedan cubiertos al 100%).

- [ ] **Step 6: Commit**

```bash
git add frontend
git commit -m "refactor(frontend): mover dialogo de estado y helpers de formulario a shared

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Tipos, API, invalidación, errores `COM_*` y fixtures

**Files:**
- Create: `SRC/features/compras/api/proveedores.types.ts`, `SRC/features/compras/api/proveedores.api.ts`, `SRC/features/compras/api/proveedores.api.test.ts`
- Create: `SRC/features/compras/api/invalidate.ts`, `SRC/features/compras/api/invalidate.test.ts`
- Create: `SRC/features/compras/lib/errores-compras.ts`, `SRC/features/compras/lib/errores-compras.test.ts`
- Create: `SRC/test/compras-fixtures.ts`

**Interfaces:**
- Produces:

```ts
export const ESTADOS_PROVEEDOR = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;
export type EstadoProveedor = (typeof ESTADOS_PROVEEDOR)[number];
export type Proveedor = {
  id: string; tipoDocumento: string | null; numeroDocumento: string; razonSocial: string;
  nombreComercial: string | null; direccion: string | null; ubigeo: string | null;
  telefono: string | null; email: string | null; contactoNombre: string | null;
  contactoTelefono: string | null; contactoEmail: string | null;
  condicionPagoDefault: string | null; diasCreditoDefault: number; monedaDefault: string | null;
  esLaboratorio: boolean; esImportador: boolean; esDistribuidor: boolean;
  calificacion: string | null; estado: EstadoProveedor;
};
export type ProveedorPayload = {
  tipoDocumento?: string | undefined; numeroDocumento: string; razonSocial: string;
  nombreComercial?: string | undefined; direccion?: string | undefined; ubigeo?: string | undefined;
  telefono?: string | undefined; email?: string | undefined; contactoNombre?: string | undefined;
  contactoTelefono?: string | undefined; contactoEmail?: string | undefined;
  condicionPagoDefault?: string | undefined; diasCreditoDefault: number; monedaDefault: string;
  esLaboratorio: boolean; esImportador: boolean; esDistribuidor: boolean;
  calificacion?: string | undefined;
};
export type FetchProveedoresParams = {
  estado?: string | undefined; texto?: string | undefined;
  page?: number | undefined; size?: number | undefined;
};
export function fetchProveedores(client: ApiClient, params: FetchProveedoresParams): Promise<PaginaResponse<Proveedor>>;
export function proveedoresQuery(params: FetchProveedoresParams);   // key ['compras','proveedores','lista', estado, texto, page, size]
export function fetchProveedor(client: ApiClient, proveedorId: string): Promise<Proveedor>;
export function proveedorQuery(proveedorId: string);                 // key ['compras','proveedores','detalle', proveedorId]
export function crearProveedor(client: ApiClient, payload: ProveedorPayload): Promise<Proveedor>;
export function actualizarProveedor(client: ApiClient, proveedorId: string, payload: ProveedorPayload): Promise<Proveedor>;
export function cambiarEstadoProveedor(client: ApiClient, proveedorId: string, estado: EstadoProveedor): Promise<Proveedor>;
export function invalidateCompras(queryClient: QueryClient): Promise<void>;   // invalida ['compras']
export function describeErrorCompras(error: unknown): string;
export const sampleProveedor: Proveedor;   // test/compras-fixtures.ts
```

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/test/compras-fixtures.ts`:

```ts
import type { Proveedor } from '../features/compras/api/proveedores.types';

export const sampleProveedor: Proveedor = {
  id: 'prov-1',
  tipoDocumento: '6',
  numeroDocumento: '20100070970',
  razonSocial: 'Laboratorios Perú SAC',
  nombreComercial: 'LabPerú',
  direccion: null,
  ubigeo: null,
  telefono: null,
  email: null,
  contactoNombre: null,
  contactoTelefono: null,
  contactoEmail: null,
  condicionPagoDefault: 'CREDITO 30',
  diasCreditoDefault: 30,
  monedaDefault: 'PEN',
  esLaboratorio: true,
  esImportador: false,
  esDistribuidor: true,
  calificacion: 'CONFIABLE',
  estado: 'ACTIVO'
};
```

`SRC/features/compras/api/proveedores.api.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { createApiClient } from '@boticas/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { apiClient } from '../../../app/api';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import {
  actualizarProveedor,
  cambiarEstadoProveedor,
  crearProveedor,
  fetchProveedor,
  fetchProveedores,
  proveedorQuery,
  proveedoresQuery
} from './proveedores.api';
import type { ProveedorPayload } from './proveedores.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
});
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const payload: ProveedorPayload = {
  numeroDocumento: '20100070970',
  razonSocial: 'Laboratorios Perú SAC',
  diasCreditoDefault: 30,
  monedaDefault: 'PEN',
  esLaboratorio: true,
  esImportador: false,
  esDistribuidor: true
};

describe('proveedores.api', () => {
  it('fetchProveedores envía estado, texto, página y tamaño', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([sampleProveedor]));
      })
    );

    const result = await fetchProveedores(client, {
      estado: 'ACTIVO',
      texto: 'lab',
      page: 2,
      size: 50
    });

    expect(recibido?.searchParams.get('estado')).toBe('ACTIVO');
    expect(recibido?.searchParams.get('texto')).toBe('lab');
    expect(recibido?.searchParams.get('page')).toBe('2');
    expect(recibido?.searchParams.get('size')).toBe('50');
    expect(result.items).toEqual([sampleProveedor]);
  });

  it('fetchProveedores omite los filtros ausentes y usa la paginación por defecto', async () => {
    let recibido: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores', ({ request }) => {
        recibido = new URL(request.url);
        return HttpResponse.json(pagina([]));
      })
    );

    await fetchProveedores(client, {});

    expect(recibido?.search).toBe('?page=0&size=20');
  });

  it('fetchProveedor obtiene el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/compras/proveedores/prov-1', () =>
        HttpResponse.json(sampleProveedor)
      )
    );

    await expect(fetchProveedor(client, 'prov-1')).resolves.toEqual(sampleProveedor);
  });

  it('crearProveedor hace POST con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.post('http://localhost/api/v1/compras/proveedores', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor, { status: 201 });
      })
    );

    await expect(crearProveedor(client, payload)).resolves.toEqual(sampleProveedor);
    expect(body).toEqual(payload);
  });

  it('actualizarProveedor hace PUT con el cuerpo', async () => {
    let body: unknown;
    server.use(
      http.put('http://localhost/api/v1/compras/proveedores/prov-1', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor);
      })
    );

    await expect(actualizarProveedor(client, 'prov-1', payload)).resolves.toEqual(sampleProveedor);
    expect(body).toEqual(payload);
  });

  it('cambiarEstadoProveedor hace PATCH con el estado', async () => {
    let body: unknown;
    server.use(
      http.patch('http://localhost/api/v1/compras/proveedores/prov-1/estado', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleProveedor, estado: 'SUSPENDIDO' });
      })
    );

    const result = await cambiarEstadoProveedor(client, 'prov-1', 'SUSPENDIDO');

    expect(result.estado).toBe('SUSPENDIDO');
    expect(body).toEqual({ estado: 'SUSPENDIDO' });
  });

  it('proveedoresQuery arma la clave con valores por defecto y con filtros', () => {
    expect(proveedoresQuery({}).queryKey).toEqual(['compras', 'proveedores', 'lista', '', '', 0, 20]);
    expect(proveedoresQuery({ estado: 'ACTIVO', texto: 'lab', page: 2, size: 10 }).queryKey).toEqual([
      'compras',
      'proveedores',
      'lista',
      'ACTIVO',
      'lab',
      2,
      10
    ]);
  });

  it('proveedoresQuery y proveedorQuery consultan con el cliente de la aplicación', async () => {
    const get = vi
      .spyOn(apiClient, 'get')
      .mockResolvedValueOnce(pagina([sampleProveedor]))
      .mockResolvedValueOnce(sampleProveedor);
    const queryClient = new QueryClient();

    await queryClient.fetchQuery(proveedoresQuery({ estado: 'ACTIVO' }));
    const detalle = proveedorQuery('prov-1');
    await queryClient.fetchQuery(detalle);

    expect(get).toHaveBeenNthCalledWith(1, '/compras/proveedores?estado=ACTIVO&page=0&size=20');
    expect(get).toHaveBeenNthCalledWith(2, '/compras/proveedores/prov-1');
    expect(detalle.queryKey).toEqual(['compras', 'proveedores', 'detalle', 'prov-1']);
  });
});
```

`SRC/features/compras/api/invalidate.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { invalidateCompras } from './invalidate';

describe('invalidateCompras', () => {
  it('invalida todas las consultas de compras', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateCompras(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });
});
```

`SRC/features/compras/lib/errores-compras.test.ts`:

```ts
import { ApiError } from '@boticas/api-client';
import { describeErrorCompras } from './errores-compras';

const error = (code: string | undefined, status = 409, detail = 'detalle del backend') =>
  new ApiError('falló', status, { code, detail, status });

describe('describeErrorCompras', () => {
  it.each([
    ['COM_PROVEEDOR_DUPLICADO', 'Ya existe un proveedor con ese documento.'],
    [
      'COM_PROVEEDOR_NO_OPERABLE',
      'El proveedor debe estar activo para emitir órdenes de compra.'
    ],
    [
      'COM_ORDEN_ESTADO_INVALIDO',
      'La orden no admite esta acción en su estado actual. Actualiza la pantalla.'
    ],
    [
      'COM_RECEPCION_ORDEN_NO_RECEPCIONABLE',
      'La orden no está en un estado que permita recibir mercadería.'
    ],
    [
      'COM_RECEPCION_EXCEDE_PENDIENTE',
      'La cantidad recibida excede lo pendiente de la orden más su tolerancia.'
    ],
    ['COM_RECEPCION_LINEA_NO_ENCONTRADA', 'Una línea de la recepción no corresponde a la orden.'],
    [
      'COM_IDEMPOTENCY_CONFLICT',
      'Esta recepción ya se envió con datos distintos. Revisa el formulario y vuelve a registrar.'
    ],
    ['COM_ALMACEN_NO_OPERABLE', 'El almacén debe estar activo y controlar lotes.'],
    [
      'COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO',
      'El almacén no pertenece al establecimiento destino de la orden.'
    ],
    [
      'COM_MODIFICACION_CONCURRENTE',
      'Otro usuario modificó este registro. Actualiza la pantalla e inténtalo de nuevo.'
    ],
    ['COM_SKU_NO_OPERABLE', 'Uno de los productos no está activo comercialmente.'],
    ['COM_ESTABLECIMIENTO_NO_OPERABLE', 'El establecimiento destino debe estar activo.']
  ])('traduce %s', (code, mensaje) => {
    expect(describeErrorCompras(error(code))).toBe(mensaje);
  });

  it('para otro código del backend muestra el detalle y el código', () => {
    expect(describeErrorCompras(error('COM_ORDEN_INVALIDA', 400))).toBe(
      'detalle del backend (COM_ORDEN_INVALIDA)'
    );
  });

  it('sin código usa el mensaje genérico de la API', () => {
    expect(describeErrorCompras(error(undefined, 500))).toBe('detalle del backend');
    expect(describeErrorCompras(error(undefined, 403))).toBe('No tienes permiso para esta acción.');
    expect(describeErrorCompras(new Error('x'))).toBe(
      'No se pudo completar la operación. Inténtalo de nuevo.'
    );
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/api/proveedores.types.ts`:

```ts
export const ESTADOS_PROVEEDOR = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

export type EstadoProveedor = (typeof ESTADOS_PROVEEDOR)[number];

export type Proveedor = {
  id: string;
  tipoDocumento: string | null;
  numeroDocumento: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccion: string | null;
  ubigeo: string | null;
  telefono: string | null;
  email: string | null;
  contactoNombre: string | null;
  contactoTelefono: string | null;
  contactoEmail: string | null;
  condicionPagoDefault: string | null;
  diasCreditoDefault: number;
  monedaDefault: string | null;
  esLaboratorio: boolean;
  esImportador: boolean;
  esDistribuidor: boolean;
  calificacion: string | null;
  estado: EstadoProveedor;
};

export type ProveedorPayload = {
  tipoDocumento?: string | undefined;
  numeroDocumento: string;
  razonSocial: string;
  nombreComercial?: string | undefined;
  direccion?: string | undefined;
  ubigeo?: string | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  contactoNombre?: string | undefined;
  contactoTelefono?: string | undefined;
  contactoEmail?: string | undefined;
  condicionPagoDefault?: string | undefined;
  diasCreditoDefault: number;
  monedaDefault: string;
  esLaboratorio: boolean;
  esImportador: boolean;
  esDistribuidor: boolean;
  calificacion?: string | undefined;
};
```

`SRC/features/compras/api/proveedores.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from '../../../shared/lib/pagina.types';
import { withQuery } from '../../../shared/lib/query-string';
import type { EstadoProveedor, Proveedor, ProveedorPayload } from './proveedores.types';

export type FetchProveedoresParams = {
  estado?: string | undefined;
  texto?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchProveedores(
  client: ApiClient,
  params: FetchProveedoresParams
): Promise<PaginaResponse<Proveedor>> {
  return client.get<PaginaResponse<Proveedor>>(
    withQuery('/compras/proveedores', {
      estado: params.estado,
      texto: params.texto,
      page: params.page ?? 0,
      size: params.size ?? 20
    })
  );
}

export function proveedoresQuery(params: FetchProveedoresParams) {
  return queryOptions({
    queryKey: [
      'compras',
      'proveedores',
      'lista',
      params.estado ?? '',
      params.texto ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchProveedores(apiClient, params)
  });
}

export function fetchProveedor(client: ApiClient, proveedorId: string): Promise<Proveedor> {
  return client.get<Proveedor>(`/compras/proveedores/${proveedorId}`);
}

export function proveedorQuery(proveedorId: string) {
  return queryOptions({
    queryKey: ['compras', 'proveedores', 'detalle', proveedorId],
    queryFn: () => fetchProveedor(apiClient, proveedorId)
  });
}

export function crearProveedor(client: ApiClient, payload: ProveedorPayload): Promise<Proveedor> {
  return client.post<Proveedor, ProveedorPayload>('/compras/proveedores', payload);
}

export function actualizarProveedor(
  client: ApiClient,
  proveedorId: string,
  payload: ProveedorPayload
): Promise<Proveedor> {
  return client.put<Proveedor, ProveedorPayload>(`/compras/proveedores/${proveedorId}`, payload);
}

export function cambiarEstadoProveedor(
  client: ApiClient,
  proveedorId: string,
  estado: EstadoProveedor
): Promise<Proveedor> {
  return client.patch<Proveedor, { estado: EstadoProveedor }>(
    `/compras/proveedores/${proveedorId}/estado`,
    { estado }
  );
}
```

`SRC/features/compras/api/invalidate.ts`:

```ts
import type { QueryClient } from '@tanstack/react-query';

export function invalidateCompras(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: ['compras'] });
}
```

`SRC/features/compras/lib/errores-compras.ts`:

```ts
import { ApiError } from '@boticas/api-client';
import { describeApiError } from '../../../shared/lib/describe-api-error';

const MENSAJES: Record<string, string> = {
  COM_PROVEEDOR_DUPLICADO: 'Ya existe un proveedor con ese documento.',
  COM_PROVEEDOR_NO_OPERABLE: 'El proveedor debe estar activo para emitir órdenes de compra.',
  COM_ORDEN_ESTADO_INVALIDO:
    'La orden no admite esta acción en su estado actual. Actualiza la pantalla.',
  COM_RECEPCION_ORDEN_NO_RECEPCIONABLE:
    'La orden no está en un estado que permita recibir mercadería.',
  COM_RECEPCION_EXCEDE_PENDIENTE:
    'La cantidad recibida excede lo pendiente de la orden más su tolerancia.',
  COM_RECEPCION_LINEA_NO_ENCONTRADA: 'Una línea de la recepción no corresponde a la orden.',
  COM_IDEMPOTENCY_CONFLICT:
    'Esta recepción ya se envió con datos distintos. Revisa el formulario y vuelve a registrar.',
  COM_ALMACEN_NO_OPERABLE: 'El almacén debe estar activo y controlar lotes.',
  COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO:
    'El almacén no pertenece al establecimiento destino de la orden.',
  COM_MODIFICACION_CONCURRENTE:
    'Otro usuario modificó este registro. Actualiza la pantalla e inténtalo de nuevo.',
  COM_SKU_NO_OPERABLE: 'Uno de los productos no está activo comercialmente.',
  COM_ESTABLECIMIENTO_NO_OPERABLE: 'El establecimiento destino debe estar activo.'
};

export function describeErrorCompras(error: unknown): string {
  const code = error instanceof ApiError ? error.problem?.code : undefined;
  if (code === undefined) return describeApiError(error);
  return MENSAJES[code] ?? `${describeApiError(error)} (${code})`;
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos (`proveedores.types.ts` queda cubierto cuando lo importan las pantallas en la Task 5; hasta entonces el gate de cobertura global se verifica al final).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): tipos, api de proveedores, invalidacion y errores del modulo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Esquema, valores y payload del formulario de proveedor

**Files:**
- Create: `SRC/features/compras/schemas/proveedor.schema.ts`, `SRC/features/compras/schemas/proveedor.schema.test.ts`
- Create: `SRC/features/compras/lib/proveedor-form.ts`, `SRC/features/compras/lib/proveedor-form.test.ts`
- Create: `SRC/features/compras/lib/proveedor-roles.ts`, `SRC/features/compras/lib/proveedor-roles.test.ts`

**Interfaces:**
- Consumes: `Proveedor`, `ProveedorPayload` (Task 2); `textoOpcional`, `correoOpcional`, `telefonoOpcional`, `ubigeoOpcional` (`shared/schemas/campos`); `emptyToUndefined`, `orEmpty` (`shared/lib/form-values`).
- Produces:

```ts
export const proveedorSchema: ZodObject;
export type ProveedorFormValues = {
  tipoDocumento: string; numeroDocumento: string; razonSocial: string; nombreComercial: string;
  direccion: string; ubigeo: string; telefono: string; email: string; contactoNombre: string;
  contactoTelefono: string; contactoEmail: string; condicionPagoDefault: string;
  diasCreditoDefault: string; monedaDefault: string; calificacion: string;
  esLaboratorio: boolean; esImportador: boolean; esDistribuidor: boolean;
};
export const PROVEEDOR_FORM_VACIO: ProveedorFormValues;
export function proveedorAFormulario(proveedor: Proveedor): ProveedorFormValues;
export function toProveedorPayload(values: ProveedorFormValues): ProveedorPayload;
export function rolesProveedor(proveedor: Pick<Proveedor, 'esLaboratorio' | 'esImportador' | 'esDistribuidor'>): string[];
```

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/features/compras/schemas/proveedor.schema.test.ts`:

```ts
import { PROVEEDOR_FORM_VACIO } from '../lib/proveedor-form';
import { proveedorSchema } from './proveedor.schema';

const valido = { ...PROVEEDOR_FORM_VACIO, numeroDocumento: '20100070970', razonSocial: 'Lab SAC' };

const mensajeDe = (campo: string, valores: object) => {
  const resultado = proveedorSchema.safeParse(valores);
  return resultado.success
    ? undefined
    : resultado.error.issues.find((issue) => issue.path[0] === campo)?.message;
};

describe('proveedorSchema', () => {
  it('acepta un proveedor con los valores por defecto y un RUC válido', () => {
    expect(proveedorSchema.safeParse(valido).success).toBe(true);
  });

  it.each(['6', ''])('con tipo "%s" exige un RUC de 11 dígitos que empiece con 10 o 20', (tipo) => {
    const mensaje = 'El RUC debe tener 11 dígitos y empezar con 10 o 20.';

    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '30100070970' })).toBe(mensaje);
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '2010007097' })).toBe(mensaje);
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: tipo, numeroDocumento: '10100070970' })).toBeUndefined();
  });

  it('con otro tipo de documento no exige formato de RUC', () => {
    expect(
      proveedorSchema.safeParse({ ...valido, tipoDocumento: '1', numeroDocumento: '12345678' }).success
    ).toBe(true);
  });

  it('valida los límites de documento, razón social y textos', () => {
    expect(mensajeDe('tipoDocumento', { ...valido, tipoDocumento: '123' })).toBe('El tipo de documento admite hasta 2 caracteres.');
    expect(mensajeDe('numeroDocumento', { ...valido, numeroDocumento: '' })).toBe('El número de documento es obligatorio.');
    expect(mensajeDe('numeroDocumento', { ...valido, tipoDocumento: '1', numeroDocumento: '1'.repeat(16) })).toBe('El número de documento admite hasta 15 caracteres.');
    expect(mensajeDe('razonSocial', { ...valido, razonSocial: '   ' })).toBe('La razón social es obligatoria.');
    expect(mensajeDe('razonSocial', { ...valido, razonSocial: 'a'.repeat(301) })).toBe('La razón social no debe exceder 300 caracteres.');
    expect(mensajeDe('nombreComercial', { ...valido, nombreComercial: 'a'.repeat(301) })).toBe('El nombre comercial no debe exceder 300 caracteres.');
    expect(mensajeDe('direccion', { ...valido, direccion: 'a'.repeat(501) })).toBe('La dirección no debe exceder 500 caracteres.');
    expect(mensajeDe('contactoNombre', { ...valido, contactoNombre: 'a'.repeat(181) })).toBe('El contacto no debe exceder 180 caracteres.');
    expect(mensajeDe('condicionPagoDefault', { ...valido, condicionPagoDefault: 'a'.repeat(81) })).toBe('La condición de pago no debe exceder 80 caracteres.');
    expect(mensajeDe('calificacion', { ...valido, calificacion: 'a'.repeat(31) })).toBe('La calificación no debe exceder 30 caracteres.');
  });

  it('valida ubigeo, teléfonos y correos', () => {
    expect(mensajeDe('ubigeo', { ...valido, ubigeo: '1501' })).toBe('El ubigeo debe tener 6 dígitos.');
    expect(mensajeDe('ubigeo', { ...valido, ubigeo: '150101' })).toBeUndefined();
    expect(mensajeDe('telefono', { ...valido, telefono: 'abc' })).toContain('El teléfono');
    expect(mensajeDe('contactoTelefono', { ...valido, contactoTelefono: '999 888 777' })).toBeUndefined();
    expect(mensajeDe('email', { ...valido, email: 'sin-arroba' })).toBe('El correo no es válido.');
    expect(mensajeDe('email', { ...valido, email: `${'a'.repeat(250)}@x.pe` })).toBe('El correo no debe exceder 254 caracteres.');
    expect(mensajeDe('contactoEmail', { ...valido, contactoEmail: 'ventas@lab.pe' })).toBeUndefined();
    expect(mensajeDe('contactoEmail', { ...valido, contactoEmail: `${'a'.repeat(250)}@x.pe` })).toBe('El correo no debe exceder 254 caracteres.');
  });

  it('valida los días de crédito y la moneda', () => {
    const mensajeDias = 'Los días de crédito deben ser un entero mayor o igual a 0.';

    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '-1' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '1.5' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '' })).toBe(mensajeDias);
    expect(mensajeDe('diasCreditoDefault', { ...valido, diasCreditoDefault: '45' })).toBeUndefined();
    expect(mensajeDe('monedaDefault', { ...valido, monedaDefault: 'pen' })).toBe('La moneda debe ser un código de 3 letras mayúsculas.');
    expect(mensajeDe('monedaDefault', { ...valido, monedaDefault: 'USD' })).toBeUndefined();
  });
});
```

`SRC/features/compras/lib/proveedor-form.test.ts`:

```ts
import { sampleProveedor } from '../../../test/compras-fixtures';
import { PROVEEDOR_FORM_VACIO, proveedorAFormulario, toProveedorPayload } from './proveedor-form';

describe('proveedor-form', () => {
  it('el formulario vacío trae los valores por defecto del backend', () => {
    expect(PROVEEDOR_FORM_VACIO).toEqual({
      tipoDocumento: '6',
      numeroDocumento: '',
      razonSocial: '',
      nombreComercial: '',
      direccion: '',
      ubigeo: '',
      telefono: '',
      email: '',
      contactoNombre: '',
      contactoTelefono: '',
      contactoEmail: '',
      condicionPagoDefault: 'CONTADO',
      diasCreditoDefault: '0',
      monedaDefault: 'PEN',
      calificacion: 'CONFIABLE',
      esLaboratorio: false,
      esImportador: false,
      esDistribuidor: true
    });
  });

  it('proveedorAFormulario convierte nulos en vacíos y días en texto', () => {
    expect(proveedorAFormulario(sampleProveedor)).toEqual({
      ...PROVEEDOR_FORM_VACIO,
      numeroDocumento: '20100070970',
      razonSocial: 'Laboratorios Perú SAC',
      nombreComercial: 'LabPerú',
      condicionPagoDefault: 'CREDITO 30',
      diasCreditoDefault: '30',
      esLaboratorio: true,
      esDistribuidor: true
    });
  });

  it('proveedorAFormulario usa vacío cuando el proveedor no trae tipo, condición, moneda ni calificación', () => {
    const formulario = proveedorAFormulario({
      ...sampleProveedor,
      tipoDocumento: null,
      condicionPagoDefault: null,
      monedaDefault: null,
      calificacion: null
    });

    expect(formulario.tipoDocumento).toBe('');
    expect(formulario.condicionPagoDefault).toBe('');
    expect(formulario.monedaDefault).toBe('');
    expect(formulario.calificacion).toBe('');
  });

  it('toProveedorPayload recorta, omite los textos vacíos y convierte los días a número', () => {
    const payload = toProveedorPayload({
      ...PROVEEDOR_FORM_VACIO,
      numeroDocumento: ' 20100070970 ',
      razonSocial: '  Lab SAC ',
      email: ' ventas@lab.pe ',
      diasCreditoDefault: '45',
      esLaboratorio: true
    });

    expect(payload).toEqual({
      tipoDocumento: '6',
      numeroDocumento: '20100070970',
      razonSocial: 'Lab SAC',
      nombreComercial: undefined,
      direccion: undefined,
      ubigeo: undefined,
      telefono: undefined,
      email: 'ventas@lab.pe',
      contactoNombre: undefined,
      contactoTelefono: undefined,
      contactoEmail: undefined,
      condicionPagoDefault: 'CONTADO',
      diasCreditoDefault: 45,
      monedaDefault: 'PEN',
      esLaboratorio: true,
      esImportador: false,
      esDistribuidor: true,
      calificacion: 'CONFIABLE'
    });
  });
});
```

`SRC/features/compras/lib/proveedor-roles.test.ts`:

```ts
import { rolesProveedor } from './proveedor-roles';

describe('rolesProveedor', () => {
  it('lista los roles marcados en orden', () => {
    expect(
      rolesProveedor({ esLaboratorio: true, esImportador: true, esDistribuidor: true })
    ).toEqual(['Laboratorio', 'Importador', 'Distribuidor']);
    expect(
      rolesProveedor({ esLaboratorio: false, esImportador: true, esDistribuidor: false })
    ).toEqual(['Importador']);
  });

  it('devuelve una lista vacía cuando no hay roles', () => {
    expect(
      rolesProveedor({ esLaboratorio: false, esImportador: false, esDistribuidor: false })
    ).toEqual([]);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/schemas apps/erp-web/src/features/compras/lib`
Expected: FAIL (módulos inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/schemas/proveedor.schema.ts`:

```ts
import { z } from 'zod';
import {
  correoOpcional,
  telefonoOpcional,
  textoOpcional,
  ubigeoOpcional
} from '../../../shared/schemas/campos';

const TIPO_RUC = '6';
const RUC_PROVEEDOR = /^(10|20)\d{9}$/;

const correoHasta254 = correoOpcional.refine(
  (value) => value.length <= 254,
  'El correo no debe exceder 254 caracteres.'
);

export const proveedorSchema = z
  .object({
    tipoDocumento: z.string().max(2, 'El tipo de documento admite hasta 2 caracteres.'),
    numeroDocumento: z
      .string()
      .min(1, 'El número de documento es obligatorio.')
      .max(15, 'El número de documento admite hasta 15 caracteres.'),
    razonSocial: z
      .string()
      .trim()
      .min(1, 'La razón social es obligatoria.')
      .max(300, 'La razón social no debe exceder 300 caracteres.'),
    nombreComercial: textoOpcional(300, 'El nombre comercial'),
    direccion: textoOpcional(500, 'La dirección'),
    ubigeo: ubigeoOpcional,
    telefono: telefonoOpcional,
    email: correoHasta254,
    contactoNombre: textoOpcional(180, 'El contacto'),
    contactoTelefono: telefonoOpcional,
    contactoEmail: correoHasta254,
    condicionPagoDefault: textoOpcional(80, 'La condición de pago'),
    diasCreditoDefault: z
      .string()
      .regex(/^\d{1,4}$/, 'Los días de crédito deben ser un entero mayor o igual a 0.'),
    monedaDefault: z
      .string()
      .regex(/^[A-Z]{3}$/, 'La moneda debe ser un código de 3 letras mayúsculas.'),
    calificacion: textoOpcional(30, 'La calificación'),
    esLaboratorio: z.boolean(),
    esImportador: z.boolean(),
    esDistribuidor: z.boolean()
  })
  .superRefine((valores, contexto) => {
    const esRuc = valores.tipoDocumento === '' || valores.tipoDocumento === TIPO_RUC;
    if (esRuc && !RUC_PROVEEDOR.test(valores.numeroDocumento)) {
      contexto.addIssue({
        code: 'custom',
        path: ['numeroDocumento'],
        message: 'El RUC debe tener 11 dígitos y empezar con 10 o 20.'
      });
    }
  });

export type ProveedorFormValues = z.infer<typeof proveedorSchema>;
```

`SRC/features/compras/lib/proveedor-form.ts`:

```ts
import { emptyToUndefined, orEmpty } from '../../../shared/lib/form-values';
import type { Proveedor, ProveedorPayload } from '../api/proveedores.types';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export const PROVEEDOR_FORM_VACIO: ProveedorFormValues = {
  tipoDocumento: '6',
  numeroDocumento: '',
  razonSocial: '',
  nombreComercial: '',
  direccion: '',
  ubigeo: '',
  telefono: '',
  email: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  condicionPagoDefault: 'CONTADO',
  diasCreditoDefault: '0',
  monedaDefault: 'PEN',
  calificacion: 'CONFIABLE',
  esLaboratorio: false,
  esImportador: false,
  esDistribuidor: true
};

export const proveedorAFormulario = (proveedor: Proveedor): ProveedorFormValues => ({
  tipoDocumento: orEmpty(proveedor.tipoDocumento),
  numeroDocumento: proveedor.numeroDocumento,
  razonSocial: proveedor.razonSocial,
  nombreComercial: orEmpty(proveedor.nombreComercial),
  direccion: orEmpty(proveedor.direccion),
  ubigeo: orEmpty(proveedor.ubigeo),
  telefono: orEmpty(proveedor.telefono),
  email: orEmpty(proveedor.email),
  contactoNombre: orEmpty(proveedor.contactoNombre),
  contactoTelefono: orEmpty(proveedor.contactoTelefono),
  contactoEmail: orEmpty(proveedor.contactoEmail),
  condicionPagoDefault: orEmpty(proveedor.condicionPagoDefault),
  diasCreditoDefault: String(proveedor.diasCreditoDefault),
  monedaDefault: orEmpty(proveedor.monedaDefault),
  calificacion: orEmpty(proveedor.calificacion),
  esLaboratorio: proveedor.esLaboratorio,
  esImportador: proveedor.esImportador,
  esDistribuidor: proveedor.esDistribuidor
});

export const toProveedorPayload = (values: ProveedorFormValues): ProveedorPayload => ({
  tipoDocumento: emptyToUndefined(values.tipoDocumento),
  numeroDocumento: values.numeroDocumento.trim(),
  razonSocial: values.razonSocial.trim(),
  nombreComercial: emptyToUndefined(values.nombreComercial),
  direccion: emptyToUndefined(values.direccion),
  ubigeo: emptyToUndefined(values.ubigeo),
  telefono: emptyToUndefined(values.telefono),
  email: emptyToUndefined(values.email),
  contactoNombre: emptyToUndefined(values.contactoNombre),
  contactoTelefono: emptyToUndefined(values.contactoTelefono),
  contactoEmail: emptyToUndefined(values.contactoEmail),
  condicionPagoDefault: emptyToUndefined(values.condicionPagoDefault),
  diasCreditoDefault: Number(values.diasCreditoDefault),
  monedaDefault: values.monedaDefault,
  esLaboratorio: values.esLaboratorio,
  esImportador: values.esImportador,
  esDistribuidor: values.esDistribuidor,
  calificacion: emptyToUndefined(values.calificacion)
});
```

`SRC/features/compras/lib/proveedor-roles.ts`:

```ts
import type { Proveedor } from '../api/proveedores.types';

export const rolesProveedor = (
  proveedor: Pick<Proveedor, 'esLaboratorio' | 'esImportador' | 'esDistribuidor'>
): string[] =>
  [
    proveedor.esLaboratorio ? 'Laboratorio' : null,
    proveedor.esImportador ? 'Importador' : null,
    proveedor.esDistribuidor ? 'Distribuidor' : null
  ].filter((rol): rol is string => rol !== null);
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras && pnpm typecheck && pnpm lint`
Expected: PASS con 100% en los archivos nuevos. Si un mensaje de teléfono difiere del texto de `telefonoOpcional`, ajustar solo ese `toContain` al texto real.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): esquema, valores y payload del formulario de proveedor

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Formulario, tabla y filtros de proveedores

**Files:**
- Create: `SRC/features/compras/components/ProveedorForm.tsx` y `.test.tsx`
- Create: `SRC/features/compras/components/ProveedoresTable.tsx` y `.test.tsx`
- Create: `SRC/features/compras/lib/use-proveedores-filtros.ts` (se cubre desde `ProveedoresPage.test.tsx` en la Task 5)

**Interfaces:**
- Consumes: `proveedorSchema`, `ProveedorFormValues`, `PROVEEDOR_FORM_VACIO`, `rolesProveedor`, `Proveedor`, `ESTADOS_PROVEEDOR`.
- Produces:
  - `ProveedorForm({ defaultValues?, submitLabel, isSubmitting?, error?, onSubmit(values), onCancel? })`.
  - `ProveedoresTable({ rows, isLoading, isError, pagination })`.
  - `useProveedoresFiltros(): { filtros: { estado: string; texto: string; page: number; size: number }, setEstado(valor), setTexto(valor), setPage(page: number), setSize(size: number) }`.
- Textos exactos: etiquetas del formulario `Tipo de documento`, `Número de documento`, `Razón social`, `Nombre comercial`, `Dirección`, `Ubigeo`, `Teléfono`, `Correo`, `Contacto`, `Teléfono del contacto`, `Correo del contacto`, `Condición de pago`, `Días de crédito`, `Moneda`, `Calificación`, casillas `Laboratorio`, `Importador`, `Distribuidor`; tabla con columnas `Documento`, `Razón social`, `Tipo`, `Condición de pago`, `Estado`, `Acciones`, vacío `No hay proveedores con los filtros indicados.`, error `No se pudo cargar el listado de proveedores.`, enlace por fila con nombre accesible `Ver detalle de {razonSocial}` hacia `/compras/proveedores/{id}`.

- [ ] **Step 1: Escribir los tests que fallan**

`ProveedorForm.test.tsx`:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { proveedorAFormulario } from '../lib/proveedor-form';
import { ProveedorForm } from './ProveedorForm';

function renderForm(overrides: Partial<Parameters<typeof ProveedorForm>[0]> = {}) {
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  render(<ProveedorForm submitLabel="Crear proveedor" onSubmit={onSubmit} {...overrides} />);
  return { onSubmit, onCancel, user: userEvent.setup() };
}

describe('ProveedorForm', () => {
  it('arranca con los valores por defecto del backend', () => {
    renderForm();

    expect(screen.getByLabelText('Tipo de documento')).toHaveValue('6');
    expect(screen.getByLabelText('Condición de pago')).toHaveValue('CONTADO');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('0');
    expect(screen.getByLabelText('Moneda')).toHaveValue('PEN');
    expect(screen.getByLabelText('Calificación')).toHaveValue('CONFIABLE');
    expect(screen.getByLabelText('Distribuidor')).toBeChecked();
    expect(screen.getByLabelText('Laboratorio')).not.toBeChecked();
  });

  it('muestra los errores de validación y no envía un formulario inválido', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    expect(await screen.findByText('El número de documento es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('La razón social es obligatoria.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('envía los valores del formulario válido', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Número de documento'), '20100070970');
    await user.type(screen.getByLabelText('Razón social'), 'Lab SAC');
    await user.click(screen.getByLabelText('Laboratorio'));
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0]?.[0]).toMatchObject({
      numeroDocumento: '20100070970',
      razonSocial: 'Lab SAC',
      esLaboratorio: true,
      esDistribuidor: true,
      diasCreditoDefault: '0'
    });
  });

  it('precarga los valores recibidos', () => {
    renderForm({ defaultValues: proveedorAFormulario(sampleProveedor), submitLabel: 'Guardar cambios' });

    expect(screen.getByLabelText('Razón social')).toHaveValue('Laboratorios Perú SAC');
    expect(screen.getByLabelText('Días de crédito')).toHaveValue('30');
    expect(screen.getByLabelText('Laboratorio')).toBeChecked();
  });

  it('deshabilita el envío mientras guarda y muestra el error recibido', () => {
    renderForm({ isSubmitting: true, error: 'Ya existe un proveedor con ese documento.' });

    expect(screen.getByRole('button', { name: 'Crear proveedor' })).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un proveedor con ese documento.');
  });

  it('cancela solo cuando se entrega onCancel', async () => {
    const onCancel = vi.fn();
    const { user } = renderForm({ onCancel });

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('no muestra Cancelar sin onCancel', () => {
    renderForm();

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
  });
});
```

`ProveedoresTable.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedoresTable } from './ProveedoresTable';

const pagination = {
  page: 0,
  size: 20,
  totalElements: 1,
  onPageChange: vi.fn(),
  onSizeChange: vi.fn()
};

function renderTable(overrides: Partial<Parameters<typeof ProveedoresTable>[0]> = {}) {
  const Pantalla = () => (
    <ProveedoresTable
      rows={[sampleProveedor]}
      isLoading={false}
      isError={false}
      pagination={pagination}
      {...overrides}
    />
  );
  return renderRoute('/compras/proveedores', Pantalla, '/compras/proveedores');
}

describe('ProveedoresTable', () => {
  it('muestra documento, razón social, roles, condición, estado y enlace al detalle', () => {
    renderTable();

    expect(screen.getByText('20100070970')).toBeInTheDocument();
    expect(screen.getByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByText('Laboratorio')).toBeInTheDocument();
    expect(screen.getByText('Distribuidor')).toBeInTheDocument();
    expect(screen.getByText('CREDITO 30')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Ver detalle de Laboratorios Perú SAC' })
    ).toHaveAttribute('href', '/compras/proveedores/prov-1');
  });

  it('muestra guiones cuando no hay roles ni condición de pago', () => {
    renderTable({
      rows: [
        {
          ...sampleProveedor,
          esLaboratorio: false,
          esDistribuidor: false,
          condicionPagoDefault: null
        }
      ]
    });

    expect(screen.getAllByText('—')).toHaveLength(2);
  });

  it('muestra el vacío, la carga y el error', () => {
    renderTable({ rows: [] });
    expect(screen.getByText('No hay proveedores con los filtros indicados.')).toBeInTheDocument();
  });

  it('muestra el estado de carga', () => {
    renderTable({ rows: [], isLoading: true });
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('muestra el error de carga', () => {
    renderTable({ rows: [], isError: true });
    expect(screen.getByText('No se pudo cargar el listado de proveedores.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components`
Expected: FAIL (componentes inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/components/ProveedorForm.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { CheckboxField, TextField } from '../../../shared/components/FormFields';
import { PROVEEDOR_FORM_VACIO } from '../lib/proveedor-form';
import { proveedorSchema, type ProveedorFormValues } from '../schemas/proveedor.schema';

type CampoTexto = Exclude<
  keyof ProveedorFormValues,
  'esLaboratorio' | 'esImportador' | 'esDistribuidor'
>;

const CAMPOS: ReadonlyArray<{ name: CampoTexto; id: string; label: string }> = [
  { name: 'tipoDocumento', id: 'proveedor-tipo-documento', label: 'Tipo de documento' },
  { name: 'numeroDocumento', id: 'proveedor-numero-documento', label: 'Número de documento' },
  { name: 'razonSocial', id: 'proveedor-razon-social', label: 'Razón social' },
  { name: 'nombreComercial', id: 'proveedor-nombre-comercial', label: 'Nombre comercial' },
  { name: 'direccion', id: 'proveedor-direccion', label: 'Dirección' },
  { name: 'ubigeo', id: 'proveedor-ubigeo', label: 'Ubigeo' },
  { name: 'telefono', id: 'proveedor-telefono', label: 'Teléfono' },
  { name: 'email', id: 'proveedor-email', label: 'Correo' },
  { name: 'contactoNombre', id: 'proveedor-contacto-nombre', label: 'Contacto' },
  { name: 'contactoTelefono', id: 'proveedor-contacto-telefono', label: 'Teléfono del contacto' },
  { name: 'contactoEmail', id: 'proveedor-contacto-email', label: 'Correo del contacto' },
  { name: 'condicionPagoDefault', id: 'proveedor-condicion-pago', label: 'Condición de pago' },
  { name: 'diasCreditoDefault', id: 'proveedor-dias-credito', label: 'Días de crédito' },
  { name: 'monedaDefault', id: 'proveedor-moneda', label: 'Moneda' },
  { name: 'calificacion', id: 'proveedor-calificacion', label: 'Calificación' }
];

export type ProveedorFormProps = {
  defaultValues?: ProveedorFormValues | undefined;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
  onSubmit: (values: ProveedorFormValues) => void;
  onCancel?: (() => void) | undefined;
};

export function ProveedorForm({
  defaultValues,
  submitLabel,
  isSubmitting = false,
  error = null,
  onSubmit,
  onCancel
}: ProveedorFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<ProveedorFormValues>({
    defaultValues: defaultValues ?? PROVEEDOR_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(proveedorSchema)
  });

  return (
    <form
      className="space-y-6"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {CAMPOS.map(({ name, id, label }) => (
          <TextField
            key={id}
            id={id}
            label={label}
            error={errors[name]?.message}
            {...register(name)}
          />
        ))}
      </div>
      <div className="flex flex-wrap gap-6">
        <CheckboxField id="proveedor-laboratorio" label="Laboratorio" {...register('esLaboratorio')} />
        <CheckboxField id="proveedor-importador" label="Importador" {...register('esImportador')} />
        <CheckboxField id="proveedor-distribuidor" label="Distribuidor" {...register('esDistribuidor')} />
      </div>
      {error ? <FormError message={error} /> : null}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
        {onCancel ? (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancelar
          </Button>
        ) : null}
      </div>
    </form>
  );
}
```

`SRC/features/compras/components/ProveedoresTable.tsx`:

```tsx
import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import {
  Badge,
  DataTable,
  EstadoBadge,
  iconButtonClassName,
  type PaginationProps
} from '@boticas/ui-web';
import { valueOrDash } from '../../../shared/lib/format';
import type { Proveedor } from '../api/proveedores.types';
import { rolesProveedor } from '../lib/proveedor-roles';

type ProveedoresTableProps = {
  rows: Proveedor[];
  isLoading: boolean;
  isError: boolean;
  pagination: PaginationProps;
};

export function ProveedoresTable({ rows, isLoading, isError, pagination }: ProveedoresTableProps) {
  return (
    <DataTable<Proveedor>
      columns={[
        { header: 'Documento', cell: (row) => row.numeroDocumento },
        { header: 'Razón social', cell: (row) => row.razonSocial },
        {
          header: 'Tipo',
          cell: (row) => {
            const roles = rolesProveedor(row);
            return roles.length === 0 ? (
              '—'
            ) : (
              <div className="flex flex-wrap gap-1">
                {roles.map((rol) => (
                  <Badge key={rol}>{rol}</Badge>
                ))}
              </div>
            );
          }
        },
        { header: 'Condición de pago', cell: (row) => valueOrDash(row.condicionPagoDefault) },
        { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
        {
          header: 'Acciones',
          cell: (row) => (
            <Link
              to={`/compras/proveedores/${row.id}`}
              aria-label={`Ver detalle de ${row.razonSocial}`}
              title={`Ver detalle de ${row.razonSocial}`}
              className={iconButtonClassName()}
            >
              <Eye className="size-4.5" aria-hidden="true" />
            </Link>
          )
        }
      ]}
      rows={rows}
      rowKey={(row) => row.id}
      emptyMessage="No hay proveedores con los filtros indicados."
      isLoading={isLoading}
      isError={isError}
      errorMessage="No se pudo cargar el listado de proveedores."
      pagination={pagination}
    />
  );
}
```

`SRC/features/compras/lib/use-proveedores-filtros.ts`:

```ts
import { enteroEnRango, useParametrosUrl } from '../../../shared/lib/use-filtros-url';

export type FiltrosProveedores = {
  estado: string;
  texto: string;
  page: number;
  size: number;
};

const PAGE_POR_DEFECTO = 0;
const SIZE_POR_DEFECTO = 20;
const SIZE_MAXIMO = 100;

export function useProveedoresFiltros() {
  const { params, actualizar } = useParametrosUrl();

  const filtros: FiltrosProveedores = {
    estado: params.get('estado') ?? '',
    texto: params.get('texto') ?? '',
    page: enteroEnRango(params.get('page'), 0, Number.MAX_SAFE_INTEGER, PAGE_POR_DEFECTO),
    size: enteroEnRango(params.get('size'), 1, SIZE_MAXIMO, SIZE_POR_DEFECTO)
  };

  return {
    filtros,
    setEstado: (estado: string) => actualizar({ estado, page: '' }),
    setTexto: (texto: string) => actualizar({ texto, page: '' }),
    setPage: (page: number) => actualizar({ page: String(page) }),
    setSize: (size: number) => actualizar({ size: String(size), page: '' })
  };
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/components && pnpm typecheck && pnpm lint`
Expected: PASS. Si el input del `ProveedorForm` no respeta `register` a través de `TextField`, revisar que `CAMPOS` use `{...register(name)}` después de `error` (igual que `CerrarTurnoForm`).

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): formulario, tabla y filtros de proveedores

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Pantallas, rutas, baseline y e2e de proveedores

**Files:**
- Modify (reemplazar): `SRC/features/compras/pages/PurchasesPage.tsx`; Create: `PurchasesPage.test.tsx`
- Create: `SRC/features/compras/pages/ProveedoresPage.tsx` y `.test.tsx`, `NuevoProveedorPage.tsx` y `.test.tsx`, `ProveedorDetailPage.tsx` y `.test.tsx`
- Modify: `SRC/features/compras/routes.tsx`; Create: `SRC/features/compras/routes.test.ts`
- Modify: `SRC/app/feature-routes.test.ts`, `frontend/coverage-baseline.txt`
- Create: `E2E/support/compras-api.ts`, `E2E/compras.spec.ts`

**Interfaces:**
- Consumes: todo lo anterior; `useRouteParam` (`shared/lib/use-route-param`), `CambiarEstadoDialog` (`shared/components`), `apiClient`.
- Produces: rutas `compras`, `compras/proveedores`, `compras/proveedores/nuevo`, `compras/proveedores/:proveedorId`.
- Textos exactos: hub con título `Compras`, descripción `Proveedores, órdenes y recepción de mercadería.`, tarjeta `Proveedores` (enlace a `/compras/proveedores`); `ProveedoresPage`: título `Proveedores`, contexto `Compras / Proveedores`, enlace `Nuevo proveedor` a `/compras/proveedores/nuevo`, filtro de texto `Buscar proveedor` (placeholder `Documento, razón social o nombre comercial`), selector `Estado` con `Todos`; `NuevoProveedorPage`: título `Nuevo proveedor`, botón `Crear proveedor`; `ProveedorDetailPage`: botón `Cambiar estado`, botón `Guardar cambios`, aviso `Proveedor actualizado.` (`role="status"`), diálogo `Cambiar estado de {razonSocial}` con botón `Guardar estado`.

- [ ] **Step 1: Escribir los tests que fallan**

`PurchasesPage.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { renderRoute } from '../../../test/render-route';
import { PurchasesPage } from './PurchasesPage';

describe('PurchasesPage', () => {
  it('muestra el hub con el acceso a proveedores', () => {
    renderRoute('/compras', PurchasesPage, '/compras');

    expect(screen.getByRole('heading', { name: 'Compras' })).toBeInTheDocument();
    expect(screen.getByText('Proveedores, órdenes y recepción de mercadería.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Proveedores/ })).toHaveAttribute(
      'href',
      '/compras/proveedores'
    );
  });
});
```

`ProveedoresPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { pagina } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedoresPage } from './ProveedoresPage';

const proveedoresUrl = '*/api/v1/compras/proveedores';

function renderPage(entrada = '/compras/proveedores') {
  return renderRoute('/compras/proveedores', ProveedoresPage, entrada);
}

function capturar() {
  const captura = { params: new URLSearchParams() };
  server.use(
    http.get(proveedoresUrl, ({ request }) => {
      captura.params = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([sampleProveedor], { totalElements: 120 }));
    })
  );
  return captura;
}

describe('ProveedoresPage', () => {
  it('lista los proveedores con enlace al detalle y al alta', async () => {
    server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([sampleProveedor]))));

    renderPage();

    expect(await screen.findByText('Laboratorios Perú SAC')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Proveedores' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nuevo proveedor' })).toHaveAttribute(
      'href',
      '/compras/proveedores/nuevo'
    );
    expect(
      screen.getByRole('link', { name: 'Ver detalle de Laboratorios Perú SAC' })
    ).toHaveAttribute('href', '/compras/proveedores/prov-1');
  });

  it('muestra el estado de carga, el vacío y el error', async () => {
    server.use(http.get(proveedoresUrl, () => HttpResponse.json(pagina([]))));
    const primera = renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(
      await screen.findByText('No hay proveedores con los filtros indicados.')
    ).toBeInTheDocument();
    primera.unmount();

    server.use(http.get(proveedoresUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderPage();

    expect(
      await screen.findByText('No se pudo cargar el listado de proveedores.')
    ).toBeInTheDocument();
  });

  it('filtra por estado y texto y refleja los filtros en la URL', async () => {
    const captura = capturar();
    const { user, router } = renderPage();
    await screen.findByText('Laboratorios Perú SAC');

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await waitFor(() => expect(captura.params.get('estado')).toBe('SUSPENDIDO'));
    await user.type(screen.getByLabelText('Buscar proveedor'), 'lab');
    await waitFor(() => expect(captura.params.get('texto')).toBe('lab'));

    expect(router.state.location.search).toBe('?estado=SUSPENDIDO&texto=lab');
  });

  it('lee los filtros iniciales de la URL', async () => {
    const captura = capturar();

    renderPage('/compras/proveedores?estado=ACTIVO&texto=lab&page=2&size=50');

    await waitFor(() => expect(captura.params.get('estado')).toBe('ACTIVO'));
    expect(captura.params.get('texto')).toBe('lab');
    expect(captura.params.get('page')).toBe('2');
    expect(captura.params.get('size')).toBe('50');
    expect(screen.getByLabelText('Buscar proveedor')).toHaveValue('lab');
  });

  it('pagina y cambia el tamaño de página', async () => {
    const captura = capturar();
    const { user } = renderPage();
    await screen.findByText('Laboratorios Perú SAC');

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(captura.params.get('page')).toBe('1'));
    await user.selectOptions(screen.getByLabelText('Filas por página'), '50');
    await waitFor(() => expect(captura.params.get('size')).toBe('50'));
    expect(captura.params.get('page')).toBe('0');
  });
});
```

`NuevoProveedorPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { NuevoProveedorPage } from './NuevoProveedorPage';

const proveedoresUrl = '*/api/v1/compras/proveedores';

function renderPage() {
  return renderRoute('*', NuevoProveedorPage, '/compras/proveedores/nuevo');
}

async function completar(user: ReturnType<typeof renderPage>['user']) {
  await user.type(screen.getByLabelText('Número de documento'), '20100070970');
  await user.type(screen.getByLabelText('Razón social'), 'Laboratorios Perú SAC');
}

describe('NuevoProveedorPage', () => {
  it('crea el proveedor, invalida las consultas y abre su detalle', async () => {
    let body: unknown;
    server.use(
      http.post(proveedoresUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor, { status: 201 });
      })
    );
    const { user, router, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    await completar(user);
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/compras/proveedores/prov-1'));
    expect(body).toMatchObject({
      tipoDocumento: '6',
      numeroDocumento: '20100070970',
      razonSocial: 'Laboratorios Perú SAC',
      diasCreditoDefault: 0,
      monedaDefault: 'PEN',
      esDistribuidor: true
    });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error traducido del backend y no navega', async () => {
    server.use(
      http.post(proveedoresUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_DUPLICADO', detail: 'duplicado' },
          { status: 409 }
        )
      )
    );
    const { user, router } = renderPage();

    await completar(user);
    await user.click(screen.getByRole('button', { name: 'Crear proveedor' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un proveedor con ese documento.'
    );
    expect(router.state.location.pathname).toBe('/compras/proveedores/nuevo');
  });

  it('cancela y vuelve al listado', async () => {
    const { user, router } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(router.state.location.pathname).toBe('/compras/proveedores');
  });
});
```

`ProveedorDetailPage.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { sampleProveedor } from '../../../test/compras-fixtures';
import { renderRoute } from '../../../test/render-route';
import { ProveedorDetailPage } from './ProveedorDetailPage';

const detalleUrl = '*/api/v1/compras/proveedores/prov-1';
const estadoUrl = '*/api/v1/compras/proveedores/prov-1/estado';

function renderPage() {
  return renderRoute(
    '/compras/proveedores/:proveedorId',
    ProveedorDetailPage,
    '/compras/proveedores/prov-1'
  );
}

describe('ProveedorDetailPage', () => {
  it('muestra el proveedor con su estado y el formulario precargado', async () => {
    server.use(http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)));

    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' })
    ).toBeInTheDocument();
    expect(screen.getByText('Documento 20100070970')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByLabelText('Razón social')).toHaveValue('Laboratorios Perú SAC');
    expect(screen.getByLabelText('Laboratorio')).toBeChecked();
  });

  it('muestra la carga y el error de consulta', async () => {
    server.use(
      http.get(detalleUrl, () =>
        HttpResponse.json({ title: 'No encontrado' }, { status: 404 })
      )
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('guarda los cambios, invalida las consultas y avisa', async () => {
    let body: unknown;
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.put(detalleUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(sampleProveedor);
      })
    );
    const { user, queryClient } = renderPage();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.clear(screen.getByLabelText('Nombre comercial'));
    await user.type(screen.getByLabelText('Nombre comercial'), 'LabPerú 2');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Proveedor actualizado.');
    expect(body).toMatchObject({ nombreComercial: 'LabPerú 2', numeroDocumento: '20100070970' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['compras'] });
  });

  it('muestra el error al guardar', async () => {
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.put(detalleUrl, () =>
        HttpResponse.json(
          { title: 'Conflicto', code: 'COM_PROVEEDOR_DUPLICADO', detail: 'duplicado' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un proveedor con ese documento.'
    );
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('cambia el estado desde el diálogo y lo cierra', async () => {
    let body: unknown;
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.patch(estadoUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleProveedor, estado: 'SUSPENDIDO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(body).toEqual({ estado: 'SUSPENDIDO' }));
    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { name: 'Cambiar estado de Laboratorios Perú SAC' })
      ).not.toBeInTheDocument()
    );
  });

  it('muestra el error del cambio de estado y permite cancelar el diálogo', async () => {
    server.use(
      http.get(detalleUrl, () => HttpResponse.json(sampleProveedor)),
      http.patch(estadoUrl, () => HttpResponse.json({ title: 'Sin permiso' }, { status: 403 }))
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Laboratorios Perú SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(
      screen.queryByRole('heading', { name: 'Cambiar estado de Laboratorios Perú SAC' })
    ).not.toBeInTheDocument();
  });
});
```

`routes.test.ts`:

```ts
import { purchasesRoutes } from './routes';

describe('purchasesRoutes', () => {
  it('declara las rutas del módulo de compras', () => {
    expect(purchasesRoutes.map(({ path }) => path)).toEqual([
      'compras',
      'compras/proveedores',
      'compras/proveedores/nuevo',
      'compras/proveedores/:proveedorId'
    ]);
  });

  it.each(purchasesRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
```

En `SRC/app/feature-routes.test.ts` reemplazar la línea `'compras',` de la lista esperada por:

```ts
      'compras',
      'compras/proveedores',
      'compras/proveedores/nuevo',
      'compras/proveedores/:proveedorId',
```

`E2E/support/compras-api.ts`:

```ts
import type { Page } from '@playwright/test';
import { sampleProveedor } from '../../apps/erp-web/src/test/compras-fixtures';
import type { Proveedor } from '../../apps/erp-web/src/features/compras/api/proveedores.types';
import { json, login } from './login';

const pagina = <T>(items: T[]) => ({ items, page: 0, size: 20, totalElements: items.length });

export async function mockComprasApi(page: Page) {
  let proveedores: Proveedor[] = [sampleProveedor];

  await page.route(/\/api\/v1\/compras\/proveedores(\?|$)/, async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      const body = JSON.parse(request.postData() ?? '{}') as Partial<Proveedor>;
      const nuevo = {
        ...sampleProveedor,
        ...body,
        id: `prov-${proveedores.length + 1}`,
        estado: 'ACTIVO'
      } as Proveedor;
      proveedores = [...proveedores, nuevo];
      return json(route, 201, nuevo);
    }
    return json(route, 200, pagina(proveedores));
  });

  await page.route(/\/api\/v1\/compras\/proveedores\/[^/?]+(\/estado)?(\?|$)/, async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const cambioEstado = pathname.endsWith('/estado');
    const id = pathname.split('/').at(cambioEstado ? -2 : -1);
    const actual = proveedores.find((proveedor) => proveedor.id === id);
    if (!actual) return json(route, 404, { title: 'No encontrado' });
    if (request.method() === 'GET') return json(route, 200, actual);
    const body = JSON.parse(request.postData() ?? '{}') as Partial<Proveedor>;
    const actualizado = { ...actual, ...body } as Proveedor;
    proveedores = proveedores.map((proveedor) => (proveedor.id === id ? actualizado : proveedor));
    return json(route, 200, actualizado);
  });
}

export async function abrirComprasEn(page: Page, path: string) {
  await mockComprasApi(page);
  await login(page);
  await page.goto(path);
}
```

`E2E/compras.spec.ts`:

```ts
import { expect, test } from '@playwright/test';
import { abrirComprasEn } from './support/compras-api';
import { expectNoHorizontalOverflow } from './support/layout';

test.describe('Compras', () => {
  test('crea un proveedor, lo edita y cambia su estado', async ({ page }) => {
    await abrirComprasEn(page, '/compras');

    await expect(page.getByRole('heading', { name: 'Compras', exact: true })).toBeVisible();
    await page.getByRole('link', { name: /Proveedores/ }).click();
    await expect(page.getByText('Laboratorios Perú SAC')).toBeVisible();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('link', { name: 'Nuevo proveedor' }).click();
    await page.getByLabel('Número de documento').fill('20512345678');
    await page.getByLabel('Razón social').fill('Droguería Andina SAC');
    await page.getByRole('button', { name: 'Crear proveedor' }).click();

    await expect(page.getByRole('heading', { name: 'Droguería Andina SAC' })).toBeVisible();
    await page.getByLabel('Nombre comercial').fill('Andina');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Proveedor actualizado.')).toBeVisible();

    await page.getByRole('button', { name: 'Cambiar estado' }).click();
    await page.getByLabel('Estado').selectOption('SUSPENDIDO');
    await page.getByRole('button', { name: 'Guardar estado' }).click();
    await expect(page.getByText('SUSPENDIDO')).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras/pages apps/erp-web/src/features/compras/routes.test.ts apps/erp-web/src/app`
Expected: FAIL (páginas y rutas inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/compras/pages/PurchasesPage.tsx` (reemplaza el placeholder):

```tsx
import { Truck } from 'lucide-react';
import { Link } from 'react-router';
import { Card, PageHeader } from '@boticas/ui-web';

const sections = [
  {
    to: '/compras/proveedores',
    icon: Truck,
    title: 'Proveedores',
    description: 'Administra los laboratorios, importadores y distribuidores.'
  }
];

export function PurchasesPage() {
  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Compras"
        context="Operaciones / Compras"
        description="Proveedores, órdenes y recepción de mercadería."
      />
      <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {sections.map(({ to, icon: Icon, title, description }) => (
          <Link key={to} to={to} className="block focus-visible:outline-none">
            <Card className="hover:border-primary-300 hover:bg-primary-50/40 dark:hover:border-primary-700 dark:hover:bg-primary-900/20 h-full p-6 transition-colors">
              <div className="bg-primary-50 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300 grid size-11 place-items-center rounded-xl">
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <h2 className="mt-4 text-lg font-semibold text-neutral-950 dark:text-white">
                {title}
              </h2>
              <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
```

`SRC/features/compras/pages/ProveedoresPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { ListFilters, PageHeader, buttonClassName } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { proveedoresQuery } from '../api/proveedores.api';
import { ESTADOS_PROVEEDOR } from '../api/proveedores.types';
import { ProveedoresTable } from '../components/ProveedoresTable';
import { useProveedoresFiltros } from '../lib/use-proveedores-filtros';

export function ProveedoresPage() {
  const { filtros, setEstado, setTexto, setPage, setSize } = useProveedoresFiltros();
  const { data, isPending, isError } = useQuery(
    proveedoresQuery({
      estado: filtros.estado === '' ? undefined : filtros.estado,
      texto: filtros.texto === '' ? undefined : filtros.texto,
      page: filtros.page,
      size: filtros.size
    })
  );

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Proveedores"
        context="Compras / Proveedores"
        description="Administra los proveedores de la cadena."
        actions={
          <Link to="/compras/proveedores/nuevo" className={buttonClassName()}>
            Nuevo proveedor
          </Link>
        }
      />
      <ListFilters
        label="Buscar proveedor"
        placeholder="Documento, razón social o nombre comercial"
        value={filtros.texto}
        onValueChange={setTexto}
      >
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-estado"
            label="Estado"
            value={filtros.estado}
            onChange={(event) => setEstado(event.target.value)}
          >
            <option value="">Todos</option>
            {ESTADOS_PROVEEDOR.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </SelectField>
        </div>
      </ListFilters>
      <div className="mt-6">
        <ProveedoresTable
          rows={data?.items ?? []}
          isLoading={isPending}
          isError={isError}
          pagination={{
            page: filtros.page,
            size: filtros.size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>
    </div>
  );
}
```

`SRC/features/compras/pages/NuevoProveedorPage.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router';
import { Card, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { crearProveedor } from '../api/proveedores.api';
import { invalidateCompras } from '../api/invalidate';
import { ProveedorForm } from '../components/ProveedorForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { toProveedorPayload } from '../lib/proveedor-form';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export function NuevoProveedorPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const crear = useMutation({
    mutationFn: (values: ProveedorFormValues) =>
      crearProveedor(apiClient, toProveedorPayload(values)),
    onSuccess: (proveedor) => {
      void invalidateCompras(queryClient);
      void navigate(`/compras/proveedores/${proveedor.id}`);
    }
  });

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title="Nuevo proveedor"
        context={<Link to="/compras/proveedores">Compras / Proveedores</Link>}
        description="Registra un proveedor para poder comprarle."
      />
      <Card className="mt-6 p-6">
        <ProveedorForm
          submitLabel="Crear proveedor"
          isSubmitting={crear.isPending}
          error={crear.isError ? describeErrorCompras(crear.error) : null}
          onSubmit={(values) => crear.mutate(values)}
          onCancel={() => void navigate('/compras/proveedores')}
        />
      </Card>
    </div>
  );
}
```

`SRC/features/compras/pages/ProveedorDetailPage.tsx`:

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { CambiarEstadoDialog } from '../../../shared/components/CambiarEstadoDialog';
import { FormError } from '../../../shared/components/FormError';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { invalidateCompras } from '../api/invalidate';
import {
  actualizarProveedor,
  cambiarEstadoProveedor,
  proveedorQuery
} from '../api/proveedores.api';
import { ESTADOS_PROVEEDOR, type EstadoProveedor } from '../api/proveedores.types';
import { ProveedorForm } from '../components/ProveedorForm';
import { describeErrorCompras } from '../lib/errores-compras';
import { proveedorAFormulario, toProveedorPayload } from '../lib/proveedor-form';
import type { ProveedorFormValues } from '../schemas/proveedor.schema';

export function ProveedorDetailPage() {
  const proveedorId = useRouteParam('proveedorId');
  const queryClient = useQueryClient();
  const [estadoAbierto, setEstadoAbierto] = useState(false);
  const result = useQuery(proveedorQuery(proveedorId));

  const actualizar = useMutation({
    mutationFn: (values: ProveedorFormValues) =>
      actualizarProveedor(apiClient, proveedorId, toProveedorPayload(values)),
    onSuccess: () => invalidateCompras(queryClient)
  });
  const cambiarEstado = useMutation({
    mutationFn: (estado: EstadoProveedor) =>
      cambiarEstadoProveedor(apiClient, proveedorId, estado),
    onSuccess: () => {
      setEstadoAbierto(false);
      return invalidateCompras(queryClient);
    }
  });

  const cerrarEstado = () => {
    setEstadoAbierto(false);
    cambiarEstado.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeErrorCompras(result.error)} />;

  const proveedor = result.data;

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title={proveedor.razonSocial}
        context={<Link to="/compras/proveedores">Compras / Proveedores</Link>}
        description={`Documento ${proveedor.numeroDocumento}`}
        actions={
          <>
            <EstadoBadge status={proveedor.estado} />
            <Button variant="secondary" onClick={() => setEstadoAbierto(true)}>
              Cambiar estado
            </Button>
          </>
        }
      />
      <Card className="mt-6 space-y-4 p-6">
        <ProveedorForm
          defaultValues={proveedorAFormulario(proveedor)}
          submitLabel="Guardar cambios"
          isSubmitting={actualizar.isPending}
          error={actualizar.isError ? describeErrorCompras(actualizar.error) : null}
          onSubmit={(values) => actualizar.mutate(values)}
        />
        {actualizar.isSuccess ? (
          <p role="status" className="text-sm font-medium text-success-700 dark:text-success-400">
            Proveedor actualizado.
          </p>
        ) : null}
      </Card>
      {estadoAbierto ? (
        <CambiarEstadoDialog
          title={`Cambiar estado de ${proveedor.razonSocial}`}
          estados={ESTADOS_PROVEEDOR}
          current={proveedor.estado}
          isSubmitting={cambiarEstado.isPending}
          error={cambiarEstado.isError ? describeErrorCompras(cambiarEstado.error) : null}
          onSubmit={(estado) => cambiarEstado.mutate(estado)}
          onClose={cerrarEstado}
        />
      ) : null}
    </div>
  );
}
```

`SRC/features/compras/routes.tsx` (reemplazar):

```tsx
import type { RouteObject } from 'react-router';

export const purchasesRoutes = [
  {
    path: 'compras',
    lazy: async () => {
      const { PurchasesPage } = await import('./pages/PurchasesPage');
      return { Component: PurchasesPage };
    }
  },
  {
    path: 'compras/proveedores',
    lazy: async () => {
      const { ProveedoresPage } = await import('./pages/ProveedoresPage');
      return { Component: ProveedoresPage };
    }
  },
  {
    path: 'compras/proveedores/nuevo',
    lazy: async () => {
      const { NuevoProveedorPage } = await import('./pages/NuevoProveedorPage');
      return { Component: NuevoProveedorPage };
    }
  },
  {
    path: 'compras/proveedores/:proveedorId',
    lazy: async () => {
      const { ProveedorDetailPage } = await import('./pages/ProveedorDetailPage');
      return { Component: ProveedorDetailPage };
    }
  }
] satisfies RouteObject[];
```

`SRC/features/compras/index.ts` queda igual (`export { purchasesRoutes } from './routes';`).

- [ ] **Step 4: Ejecutar, quitar baseline y verificación completa**

Run: `pnpm exec vitest run apps/erp-web/src/features/compras apps/erp-web/src/app apps/erp-web/src/shared apps/erp-web/src/features/organizacion` → PASS.
Quitar la línea `apps/erp-web/src/features/compras/routes.tsx` de `frontend/coverage-baseline.txt`.
Run: `pnpm format && pnpm e2e compras.spec.ts` → PASS en desktop, tablet y móvil; `pnpm check` → verde (lint, typecheck, tests con umbral 100% por archivo y build).
Si `PurchasesPage` o `ProveedoresPage` quedaran con ramas sin cubrir, agregar el caso al test correspondiente sin reducir lo que verifica.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(compras): hub y gestion de proveedores con e2e

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (secciones hub, proveedores y base):** hub `/compras` y rutas lazy con `routes.test.ts` (Task 5); listado con filtros de estado y texto en la URL, paginación y `EstadoBadge` (Task 4 y 5); alta y edición en página con react-hook-form y zod espejo del backend, cambio de estado con diálogo de confirmación (Task 3, 4 y 5); base compartida `invalidateCompras`, traducción de errores `COM_*` y fixtures (Task 2); reutilización sin duplicar mediante el movimiento de `CambiarEstadoDialog` y los helpers a `shared` (Task 1); cobertura 100% en archivos nuevos, baseline reducido y e2e en tres viewports (Task 5).

**Escaneo de placeholders:** todo paso de código incluye el código. Los únicos ajustes dependientes del repo (mensaje de teléfono, regla de `register` en `TextField`, ramas residuales de cobertura) están acotados a un caso concreto. No hay "TBD".

**Consistencia de tipos:** `Proveedor`, `ProveedorPayload`, `EstadoProveedor`, `ESTADOS_PROVEEDOR`, `FetchProveedoresParams`, `proveedoresQuery`, `proveedorQuery`, `crearProveedor`, `actualizarProveedor`, `cambiarEstadoProveedor`, `invalidateCompras`, `describeErrorCompras`, `ProveedorFormValues`, `PROVEEDOR_FORM_VACIO`, `proveedorAFormulario`, `toProveedorPayload`, `rolesProveedor`, `useProveedoresFiltros` y `sampleProveedor` se usan con las mismas firmas en las cinco tareas. Las partes 3 a 5 consumen `invalidateCompras`, `describeErrorCompras`, `sampleProveedor`, `Proveedor`, `proveedoresQuery` y `ESTADOS_PROVEEDOR` tal como quedan definidos aquí.
