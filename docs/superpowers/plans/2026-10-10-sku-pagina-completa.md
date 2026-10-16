# SKU en página completa (crear y editar) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar los modales de crear y editar SKU por dos páginas completas (`/catalogo/skus/nuevo` y `/catalogo/skus/:skuId/editar`) con un formulario en tarjetas y campos condicionales.

**Architecture:** `SkuForm` se rediseña (tarjetas por sección, barra de acciones fija, campos que dependen de `tipoSku` y `permiteVentaFraccion`). La definición de campos y la normalización de valores ocultos viven en funciones puras (`lib/sku-secciones.ts`). Dos páginas finas (`NuevoSkuPage`, `EditarSkuPage`) comparten un contenedor (`SkuPaginaFormulario`); listado y detalle pasan a enlazar a ellas.

**Tech Stack:** React 19, React Router 8, TanStack Query, react-hook-form + zod, Vitest + Testing Library + MSW, pnpm workspace (`frontend/`).

**Spec:** `docs/superpowers/specs/2026-10-10-sku-pagina-completa-design.md`

## Global Constraints

- Todo archivo **nuevo** del frontend debe alcanzar 100% de líneas y ramas (gate de Vitest). Los archivos existentes que no están en `frontend/coverage-baseline.txt` (`SkuForm.tsx`, `SkusPage.tsx`, `SkuDetailPage.tsx`) también están bajo el gate: no dejar ramas sin probar.
- Sin comentarios explicativos en el código; sin código duplicado; funciones lambda donde sea idiomático.
- Los componentes de feature no importan archivos internos de otra feature; no se tocan otras features.
- Etiquetas, mensajes de validación y valores iniciales del formulario no cambian (`skuSchema`, `toSkuPayload`, `toSkuFormValues`, `SKU_VACIO` se reutilizan sin modificar).
- Fuera de alcance: diálogo de estado, códigos de barra, backend, aviso de cambios sin guardar, e2e de Playwright.
- **Nunca** usar `git add -A`, `git add .` ni `git commit -a`: el árbol de trabajo del repositorio puede tener cambios ajenos sin commitear. Añadir siempre rutas explícitas.
- En Windows las rutas largas rompen `git worktree` y `node_modules`: usar un worktree en una ruta corta.
- Los comandos de prueba se ejecutan desde `frontend/` con `pnpm.cmd exec vitest run <ruta>` (no `pnpm test`, que corre todo con cobertura).

---

### Task 0: Preparar el worktree

**Files:** ninguno (entorno).

- [ ] **Step 1: Crear el worktree desde `origin/developer`**

```bash
cd C:/ambiente-dev/PROYECTOS-2026/erp-botica
git fetch origin
git worktree add C:/w/sku -b feature/sku-pagina-completa origin/developer
```

Expected: `HEAD is now at <hash>` sin errores de "Filename too long".

- [ ] **Step 2: Traer la spec y el plan al nuevo worktree**

La spec y el plan están en commits de la rama `feature/frontend-seguridad-integracion` que solo tocan esos dos archivos. Aplicarlos en orden cronológico:

```bash
cd C:/w/sku
git cherry-pick $(git -C C:/ambiente-dev/PROYECTOS-2026/erp-botica log --reverse --format=%h feature/frontend-seguridad-integracion -- docs/superpowers/specs/2026-10-10-sku-pagina-completa-design.md docs/superpowers/plans/2026-10-10-sku-pagina-completa.md)
```

Expected: `git log --oneline -3` muestra los commits de documentación y `ls docs/superpowers/specs/2026-10-10-sku-pagina-completa-design.md docs/superpowers/plans/2026-10-10-sku-pagina-completa.md` lista ambos archivos.

- [ ] **Step 3: Instalar dependencias**

```bash
cd C:/w/sku/frontend
corepack enable
pnpm.cmd install --frozen-lockfile
```

Expected: `Done` sin errores.

- [ ] **Step 4: Verificar el punto de partida**

```bash
cd C:/w/sku/frontend
pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo
```

Expected: todas las pruebas de `catalogo` pasan (línea base verde antes de tocar nada).

---

### Task 1: Funciones puras de secciones y visibilidad

**Files:**
- Create: `frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.ts`
- Create: `frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.test.ts`

**Interfaces:**
- Produces:
  - `type SkuSeccion = { titulo: string; campos: CampoFormulario<SkuFormValues>[] }`
  - `type SkuOpciones = { productos: readonly Opcion[]; categorias: readonly Opcion[]; marcas: readonly Opcion[]; unidades: readonly Opcion[] }`
  - `type SkuVisibilidad = Pick<SkuFormValues, 'tipoSku' | 'permiteVentaFraccion'>`
  - `skuSecciones(opciones: SkuOpciones, visibilidad: SkuVisibilidad): SkuSeccion[]`
  - `aplicarVisibilidad(values: SkuFormValues): SkuFormValues`

- [ ] **Step 1: Escribir las pruebas**

`frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.test.ts`:

```ts
import { SKU_VACIO } from './sku-form';
import { aplicarVisibilidad, skuSecciones, type SkuOpciones } from './sku-secciones';
import type { SkuFormValues } from '../schemas/sku.schema';

const opciones: SkuOpciones = {
  productos: [{ value: 'pr-1', label: 'Paracetamol 500 mg' }],
  categorias: [{ value: 'c-1', label: 'Analgésicos' }],
  marcas: [{ value: 'm-1', label: 'Bayer' }],
  unidades: [{ value: 'UND', label: 'UND — Unidad' }]
};

const nombres = (tipoSku: SkuFormValues['tipoSku'], permiteVentaFraccion: boolean) =>
  skuSecciones(opciones, { tipoSku, permiteVentaFraccion }).flatMap((seccion) =>
    seccion.campos.map((campo) => campo.name)
  );

describe('skuSecciones', () => {
  it('mantiene las cuatro secciones en orden', () => {
    const titulos = skuSecciones(opciones, { tipoSku: 'REGULADO', permiteVentaFraccion: false }).map(
      (seccion) => seccion.titulo
    );

    expect(titulos).toEqual([
      'Identificación',
      'Unidades y medidas',
      'Venta por fracción',
      'Control y stock'
    ]);
  });

  it('muestra el producto regulado solo para SKU regulados', () => {
    expect(nombres('REGULADO', false)).toContain('productoReguladoId');
    expect(nombres('NO_REGULADO', false)).not.toContain('productoReguladoId');
  });

  it('muestra factor y unidad de fracción solo si se permite la venta por fracción', () => {
    expect(nombres('REGULADO', true)).toEqual(
      expect.arrayContaining(['factorFraccion', 'unidadFraccionCodigo'])
    );
    expect(nombres('REGULADO', false)).not.toEqual(
      expect.arrayContaining(['factorFraccion'])
    );
    expect(nombres('REGULADO', false)).not.toContain('unidadFraccionCodigo');
  });

  it('siempre incluye el checkbox que controla la fracción y los 25 campos cuando todo aplica', () => {
    expect(nombres('REGULADO', true)).toContain('permiteVentaFraccion');
    expect(nombres('REGULADO', true)).toHaveLength(25);
    expect(nombres('NO_REGULADO', false)).toHaveLength(22);
  });

  it('entrega las opciones a los campos de selección', () => {
    const campos = skuSecciones(opciones, { tipoSku: 'REGULADO', permiteVentaFraccion: true }).flatMap(
      (seccion) => seccion.campos
    );
    const opcionesDe = (nombre: string) => {
      const campo = campos.find((item) => item.name === nombre);
      return campo !== undefined && campo.tipo === 'seleccion' ? campo.opciones : undefined;
    };

    expect(opcionesDe('productoReguladoId')).toEqual(opciones.productos);
    expect(opcionesDe('categoriaId')).toEqual(opciones.categorias);
    expect(opcionesDe('marcaId')).toEqual(opciones.marcas);
    expect(opcionesDe('unidadVentaCodigo')).toEqual(opciones.unidades);
    expect(opcionesDe('unidadContenidoCodigo')).toEqual(opciones.unidades);
    expect(opcionesDe('unidadFraccionCodigo')).toEqual(opciones.unidades);
    expect(opcionesDe('tipoSku')).toEqual([
      { value: 'REGULADO', label: 'Regulado' },
      { value: 'NO_REGULADO', label: 'No regulado' }
    ]);
  });
});

describe('aplicarVisibilidad', () => {
  const lleno: SkuFormValues = {
    ...SKU_VACIO,
    productoReguladoId: 'pr-1',
    factorFraccion: '2',
    unidadFraccionCodigo: 'UND'
  };

  it('conserva los valores cuando todos los campos aplican', () => {
    const values = { ...lleno, tipoSku: 'REGULADO' as const, permiteVentaFraccion: true };

    expect(aplicarVisibilidad(values)).toEqual(values);
  });

  it('vacía el producto regulado de un SKU no regulado', () => {
    const resultado = aplicarVisibilidad({ ...lleno, tipoSku: 'NO_REGULADO', permiteVentaFraccion: true });

    expect(resultado.productoReguladoId).toBe('');
    expect(resultado.factorFraccion).toBe('2');
  });

  it('vacía factor y unidad de fracción cuando no se permite la venta por fracción', () => {
    const resultado = aplicarVisibilidad({ ...lleno, tipoSku: 'REGULADO', permiteVentaFraccion: false });

    expect(resultado.factorFraccion).toBe('');
    expect(resultado.unidadFraccionCodigo).toBe('');
    expect(resultado.productoReguladoId).toBe('pr-1');
  });

  it('no modifica el objeto original', () => {
    const values = { ...lleno, tipoSku: 'NO_REGULADO' as const, permiteVentaFraccion: false };

    aplicarVisibilidad(values);

    expect(values.productoReguladoId).toBe('pr-1');
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/lib/sku-secciones.test.ts`
Expected: FAIL, "Failed to resolve import './sku-secciones'".

- [ ] **Step 3: Implementar**

`frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.ts`:

```ts
import { TIPOS_SKU } from '../api/skus.types';
import type { CampoFormulario, Opcion } from '../components/CamposFormulario';
import type { SkuFormValues } from '../schemas/sku.schema';

export type SkuSeccion = { titulo: string; campos: CampoFormulario<SkuFormValues>[] };

export type SkuOpciones = {
  productos: readonly Opcion[];
  categorias: readonly Opcion[];
  marcas: readonly Opcion[];
  unidades: readonly Opcion[];
};

export type SkuVisibilidad = Pick<SkuFormValues, 'tipoSku' | 'permiteVentaFraccion'>;

const ETIQUETAS_TIPO_SKU = { REGULADO: 'Regulado', NO_REGULADO: 'No regulado' } as const;
const OPCIONES_TIPO_SKU = TIPOS_SKU.map((tipo) => ({
  value: tipo,
  label: ETIQUETAS_TIPO_SKU[tipo]
}));

export function skuSecciones(opciones: SkuOpciones, visibilidad: SkuVisibilidad): SkuSeccion[] {
  const productoRegulado: CampoFormulario<SkuFormValues> = {
    name: 'productoReguladoId',
    label: 'Producto regulado',
    tipo: 'seleccion',
    opciones: opciones.productos
  };
  const camposFraccion: CampoFormulario<SkuFormValues>[] = [
    { name: 'factorFraccion', label: 'Factor de fracción', tipo: 'decimal' },
    {
      name: 'unidadFraccionCodigo',
      label: 'Unidad de fracción',
      tipo: 'seleccion',
      opciones: opciones.unidades
    }
  ];

  return [
    {
      titulo: 'Identificación',
      campos: [
        { name: 'tipoSku', label: 'Tipo de SKU', tipo: 'seleccion', opciones: OPCIONES_TIPO_SKU },
        ...(visibilidad.tipoSku === 'REGULADO' ? [productoRegulado] : []),
        { name: 'codigoInterno', label: 'Código interno', tipo: 'texto' },
        { name: 'descripcionComercial', label: 'Descripción comercial', tipo: 'texto' },
        { name: 'nombreCorto', label: 'Nombre corto', tipo: 'texto' },
        { name: 'presentacionComercial', label: 'Presentación comercial', tipo: 'texto' },
        { name: 'categoriaId', label: 'Categoría', tipo: 'seleccion', opciones: opciones.categorias },
        { name: 'marcaId', label: 'Marca', tipo: 'seleccion', opciones: opciones.marcas }
      ]
    },
    {
      titulo: 'Unidades y medidas',
      campos: [
        {
          name: 'unidadVentaCodigo',
          label: 'Unidad de venta',
          tipo: 'seleccion',
          opciones: opciones.unidades
        },
        { name: 'contenido', label: 'Contenido', tipo: 'decimal' },
        {
          name: 'unidadContenidoCodigo',
          label: 'Unidad de contenido',
          tipo: 'seleccion',
          opciones: opciones.unidades
        },
        { name: 'pesoGramos', label: 'Peso (g)', tipo: 'decimal' },
        { name: 'altoCm', label: 'Alto (cm)', tipo: 'decimal' },
        { name: 'anchoCm', label: 'Ancho (cm)', tipo: 'decimal' },
        { name: 'largoCm', label: 'Largo (cm)', tipo: 'decimal' }
      ]
    },
    {
      titulo: 'Venta por fracción',
      campos: [
        { name: 'permiteVentaFraccion', label: 'Permite venta por fracción', tipo: 'checkbox' },
        ...(visibilidad.permiteVentaFraccion ? camposFraccion : [])
      ]
    },
    {
      titulo: 'Control y stock',
      campos: [
        { name: 'requiereLote', label: 'Requiere lote', tipo: 'checkbox' },
        { name: 'requiereVencimiento', label: 'Requiere fecha de vencimiento', tipo: 'checkbox' },
        { name: 'afectoIgv', label: 'Afecto a IGV', tipo: 'checkbox' },
        { name: 'stockMinimoDefault', label: 'Stock mínimo por defecto', tipo: 'decimal' },
        { name: 'stockMaximoDefault', label: 'Stock máximo por defecto', tipo: 'decimal' },
        { name: 'precioVentaReferencia', label: 'Precio de venta de referencia', tipo: 'decimal' },
        { name: 'imagenUri', label: 'URI de imagen', tipo: 'texto' }
      ]
    }
  ];
}

export function aplicarVisibilidad(values: SkuFormValues): SkuFormValues {
  const regulado = values.tipoSku === 'REGULADO';
  const fraccion = values.permiteVentaFraccion;

  return {
    ...values,
    productoReguladoId: regulado ? values.productoReguladoId : '',
    factorFraccion: fraccion ? values.factorFraccion : '',
    unidadFraccionCodigo: fraccion ? values.unidadFraccionCodigo : ''
  };
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/lib/sku-secciones.test.ts`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.ts frontend/apps/erp-web/src/features/catalogo/lib/sku-secciones.test.ts
git commit -m "feat(catalogo): secciones del formulario de SKU y descarte de campos ocultos

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Rediseñar `SkuForm`

**Files:**
- Modify (reemplazo completo): `frontend/apps/erp-web/src/features/catalogo/components/SkuForm.tsx`
- Modify (reemplazo completo): `frontend/apps/erp-web/src/features/catalogo/components/SkuForm.test.tsx`

**Interfaces:**
- Consumes: `skuSecciones`, `aplicarVisibilidad` (Task 1); `SKU_VACIO`; `skuSchema`; `CamposFormulario`; hooks `useOpciones*`.
- Produces: `SkuFormProps = { defaultValues?: SkuFormValues; onSubmit: (values: SkuFormValues) => void; onCancel: () => void; submitLabel: string; isSubmitting?: boolean; error?: string | null }` y `SkuForm`.

- [ ] **Step 1: Reemplazar las pruebas**

`frontend/apps/erp-web/src/features/catalogo/components/SkuForm.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { SKU_VACIO } from '../lib/sku-form';
import { SkuForm, type SkuFormProps } from './SkuForm';

function renderForm(props: Partial<SkuFormProps> = {}) {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({
        items: [
          {
            id: 'pr-1',
            denominacion: 'Paracetamol 500 mg',
            condicionVentaCodigo: 'VL',
            estadoRegulatorio: 'VIGENTE'
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  return {
    onSubmit,
    onCancel,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <SkuForm onSubmit={onSubmit} onCancel={onCancel} submitLabel="Guardar" {...props} />
      </QueryClientProvider>
    )
  };
}

const completo = {
  ...SKU_VACIO,
  codigoInterno: 'SKU-3',
  descripcionComercial: 'Alcohol',
  unidadVentaCodigo: 'UND'
};

describe('SkuForm', () => {
  it('agrupa los campos en las cuatro secciones', () => {
    renderForm();

    for (const titulo of [
      'Identificación',
      'Unidades y medidas',
      'Venta por fracción',
      'Control y stock'
    ]) {
      expect(screen.getByRole('group', { name: titulo })).toBeInTheDocument();
    }
  });

  it('valida los campos obligatorios antes de enviar', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByText('El código interno debe tener al menos 2 caracteres.')
    ).toBeInTheDocument();
    expect(screen.getByText('Selecciona la unidad de venta.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('carga las opciones de producto, categoria, marca y unidades y envia los valores', async () => {
    const { onSubmit, user } = renderForm();

    expect(await screen.findByRole('option', { name: 'Paracetamol 500 mg' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Bayer' })).toBeInTheDocument();
    expect(await screen.findByRole('option', { name: 'Analgésicos' })).toBeInTheDocument();
    expect((await screen.findAllByRole('option', { name: 'UND — Unidad' })).length).toBeGreaterThan(
      0
    );

    await user.selectOptions(screen.getByLabelText('Producto regulado'), 'pr-1');
    await user.type(screen.getByLabelText('Código interno'), 'SKU-1');
    await user.type(screen.getByLabelText('Descripción comercial'), 'Paracetamol 500 mg x 100');
    await user.selectOptions(screen.getByLabelText('Unidad de venta'), 'UND');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...SKU_VACIO,
      productoReguladoId: 'pr-1',
      codigoInterno: 'SKU-1',
      descripcionComercial: 'Paracetamol 500 mg x 100',
      unidadVentaCodigo: 'UND'
    });
  });

  it('exige el producto regulado para un SKU regulado y lo oculta como no regulado', async () => {
    const { onSubmit, user } = renderForm({ defaultValues: completo });

    await user.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(
      await screen.findByText('Un SKU regulado requiere un producto regulado asociado.')
    ).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Tipo de SKU'), 'NO_REGULADO');
    expect(screen.queryByLabelText('Producto regulado')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('muestra factor y unidad de fracción solo si se permite la venta por fracción', async () => {
    const { user } = renderForm();

    expect(screen.queryByLabelText('Factor de fracción')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Unidad de fracción')).not.toBeInTheDocument();

    await user.click(screen.getByLabelText('Permite venta por fracción'));
    expect(screen.getByLabelText('Factor de fracción')).toBeInTheDocument();
    expect(screen.getByLabelText('Unidad de fracción')).toBeInTheDocument();

    await user.click(screen.getByLabelText('Permite venta por fracción'));
    expect(screen.queryByLabelText('Factor de fracción')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Unidad de fracción')).not.toBeInTheDocument();
  });

  it('descarta al enviar los valores de los campos ocultos', async () => {
    const { onSubmit, user } = renderForm({
      defaultValues: {
        ...completo,
        tipoSku: 'NO_REGULADO',
        productoReguladoId: 'pr-1',
        permiteVentaFraccion: false,
        factorFraccion: '2',
        unidadFraccionCodigo: 'UND'
      }
    });

    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0]?.[0]).toMatchObject({
      productoReguladoId: '',
      factorFraccion: '',
      unidadFraccionCodigo: ''
    });
  });

  it('exige factor y unidad cuando se permite la venta por fracción', async () => {
    const { onSubmit, user } = renderForm({
      defaultValues: { ...completo, tipoSku: 'NO_REGULADO' }
    });

    await user.click(screen.getByLabelText('Permite venta por fracción'));
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByText(
        'El factor de fracción es obligatorio cuando se permite venta por fracción.'
      )
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('cancela sin enviar', async () => {
    const { onCancel, onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('muestra el error del servidor y bloquea el envio', () => {
    renderForm({ error: 'Ya existe un SKU con ese código interno.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un SKU con ese código interno.');
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/components/SkuForm.test.tsx`
Expected: FAIL (no existe el rol `group` por sección ni el botón "Cancelar"; el factor de fracción se muestra siempre).

- [ ] **Step 3: Reemplazar el componente**

`frontend/apps/erp-web/src/features/catalogo/components/SkuForm.tsx`:

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useWatch } from 'react-hook-form';
import { Button, Card } from '@boticas/ui-web';
import { FormError } from '../../../shared/components/FormError';
import { SKU_VACIO } from '../lib/sku-form';
import { aplicarVisibilidad, skuSecciones } from '../lib/sku-secciones';
import {
  useOpcionesCategorias,
  useOpcionesMarcas,
  useOpcionesProductosRegulados,
  useOpcionesSoporte
} from '../lib/use-opciones';
import { skuSchema, type SkuFormValues } from '../schemas/sku.schema';
import { unidadesMedidaApi } from '../support-catalog/configs/unidades-medida.config';
import { CamposFormulario } from './CamposFormulario';

const validarSku = zodResolver(skuSchema);

export type SkuFormProps = {
  defaultValues?: SkuFormValues;
  onSubmit: (values: SkuFormValues) => void;
  onCancel: () => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function SkuForm({
  defaultValues = SKU_VACIO,
  onSubmit,
  onCancel,
  submitLabel,
  isSubmitting = false,
  error = null
}: SkuFormProps) {
  const productos = useOpcionesProductosRegulados();
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();
  const unidades = useOpcionesSoporte(unidadesMedidaApi);

  const {
    control,
    formState: { errors },
    handleSubmit,
    register
  } = useForm<SkuFormValues>({
    defaultValues,
    mode: 'onTouched',
    resolver: (values, context, options) =>
      validarSku(aplicarVisibilidad(values), context, options)
  });

  const tipoSku = useWatch({ control, name: 'tipoSku' });
  const permiteVentaFraccion = useWatch({ control, name: 'permiteVentaFraccion' });
  const secciones = skuSecciones(
    { productos, categorias, marcas, unidades },
    { tipoSku, permiteVentaFraccion }
  );

  return (
    <form
      className="space-y-6"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      {error ? <FormError message={error} /> : null}
      {secciones.map((seccion) => (
        <Card key={seccion.titulo} className="p-5">
          <fieldset>
            <legend className="text-base font-bold text-neutral-900 dark:text-neutral-50">
              {seccion.titulo}
            </legend>
            <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <CamposFormulario
                prefijo="sku"
                campos={seccion.campos}
                register={register}
                errors={errors}
              />
            </div>
          </fieldset>
        </Card>
      ))}
      <div className="sticky bottom-0 z-10 flex justify-end gap-3 border-t border-neutral-200 bg-white/90 py-3 backdrop-blur dark:border-neutral-800 dark:bg-neutral-950/90">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/components/SkuForm.test.tsx`
Expected: PASS (9 tests).

Si `exige factor y unidad...` falla porque el mensaje exacto difiere, leer el texto real en `schemas/sku.schema.ts` (`'El factor de fracción es obligatorio cuando se permite venta por fracción.'`) y ajustar solo la prueba.

- [ ] **Step 5: Verificar tipos y lint del archivo**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec eslint apps/erp-web/src/features/catalogo/components/SkuForm.tsx apps/erp-web/src/features/catalogo/lib/sku-secciones.ts`
Expected: sin errores. (`SkusPage.tsx` y `SkuDetailPage.tsx` aún no compilan por la prop `onCancel` requerida: se arregla en las tareas 5 y 6; no ejecutar `typecheck` completo todavía.)

- [ ] **Step 6: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/components/SkuForm.tsx frontend/apps/erp-web/src/features/catalogo/components/SkuForm.test.tsx
git commit -m "feat(catalogo): formulario de SKU en tarjetas con campos condicionales

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Rutas y página de creación

**Files:**
- Create: `frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.ts`
- Create: `frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.test.ts`
- Create: `frontend/apps/erp-web/src/features/catalogo/components/SkuPaginaFormulario.tsx`
- Create: `frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.tsx`
- Create: `frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.test.tsx`

**Interfaces:**
- Consumes: `SkuForm`, `SkuFormProps` (Task 2); `crearSku(client, payload)`; `toSkuPayload`; `describeApiError`.
- Produces:
  - `RUTA_SKUS = '/catalogo/skus'`, `RUTA_NUEVO_SKU = '/catalogo/skus/nuevo'`, `rutaEditarSku(skuId: string): string`
  - `SkuPaginaFormulario({ title, description, ...Omit<SkuFormProps, 'onCancel'> })`
  - `NuevoSkuPage()`

- [ ] **Step 1: Escribir las pruebas**

`frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.test.ts`:

```ts
import { RUTA_NUEVO_SKU, RUTA_SKUS, rutaEditarSku } from './sku-rutas';

describe('sku-rutas', () => {
  it('construye las rutas de SKU', () => {
    expect(RUTA_SKUS).toBe('/catalogo/skus');
    expect(RUTA_NUEVO_SKU).toBe('/catalogo/skus/nuevo');
    expect(rutaEditarSku('sku-1')).toBe('/catalogo/skus/sku-1/editar');
  });
});
```

`frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { NuevoSkuPage } from './NuevoSkuPage';

function renderPage() {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidar = vi.spyOn(queryClient, 'invalidateQueries');
  const router = createMemoryRouter(
    [
      { path: '/catalogo/skus/nuevo', Component: NuevoSkuPage },
      { path: '/catalogo/skus', Component: () => <h1>Listado de SKU</h1> }
    ],
    { initialEntries: ['/catalogo/skus/nuevo'] }
  );
  return {
    invalidar,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

async function completarFormulario(user: ReturnType<typeof userEvent.setup>) {
  await screen.findAllByRole('option', { name: 'UND — Unidad' });
  await user.selectOptions(screen.getByLabelText('Tipo de SKU'), 'NO_REGULADO');
  await user.type(screen.getByLabelText('Código interno'), 'SKU-001');
  await user.type(screen.getByLabelText('Descripción comercial'), 'Alcohol 70% 250 ml');
  await user.selectOptions(screen.getByLabelText('Unidad de venta'), 'UND');
}

describe('NuevoSkuPage', () => {
  it('muestra el encabezado con el enlace al listado', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Nuevo SKU' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Catálogo / SKU' })).toHaveAttribute(
      'href',
      '/catalogo/skus'
    );
  });

  it('crea el SKU sin enviar tenant, refresca el listado y vuelve a él', async () => {
    let body: Record<string, unknown> = {};
    server.use(
      http.post('*/api/v1/catalogo/skus', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 'sku-1' }, { status: 201 });
      })
    );

    const { invalidar, user } = renderPage();
    await completarFormulario(user);
    await user.click(screen.getByRole('button', { name: 'Crear SKU' }));

    expect(await screen.findByRole('heading', { name: 'Listado de SKU' })).toBeInTheDocument();
    expect(body).not.toHaveProperty('tenantId');
    expect(body).toMatchObject({
      tipoSku: 'NO_REGULADO',
      codigoInterno: 'SKU-001',
      unidadVentaCodigo: 'UND',
      stockMinimoDefault: 0
    });
    expect(invalidar).toHaveBeenCalledWith({ queryKey: ['catalogo', 'skus'] });
  });

  it('muestra el error del servidor y conserva el formulario', async () => {
    server.use(
      http.post('*/api/v1/catalogo/skus', () =>
        HttpResponse.json(
          { title: 'Conflicto', detail: 'Ya existe un SKU con ese código interno.' },
          { status: 409 }
        )
      )
    );

    const { user } = renderPage();
    await completarFormulario(user);
    await user.click(screen.getByRole('button', { name: 'Crear SKU' }));

    expect(await screen.findByText('Ya existe un SKU con ese código interno.')).toBeInTheDocument();
    expect(screen.getByLabelText('Código interno')).toHaveValue('SKU-001');
    expect(screen.queryByRole('heading', { name: 'Listado de SKU' })).not.toBeInTheDocument();
  });

  it('cancela y vuelve al listado', async () => {
    const { user } = renderPage();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByRole('heading', { name: 'Listado de SKU' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.test.tsx apps/erp-web/src/features/catalogo/lib/sku-rutas.test.ts`
Expected: FAIL, "Failed to resolve import" de `./NuevoSkuPage` y `./sku-rutas`.

- [ ] **Step 3: Implementar**

`frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.ts`:

```ts
export const RUTA_SKUS = '/catalogo/skus';
export const RUTA_NUEVO_SKU = `${RUTA_SKUS}/nuevo`;
export const rutaEditarSku = (skuId: string): string => `${RUTA_SKUS}/${skuId}/editar`;
```

`frontend/apps/erp-web/src/features/catalogo/components/SkuPaginaFormulario.tsx`:

```tsx
import { Link, useNavigate } from 'react-router';
import { PageHeader } from '@boticas/ui-web';
import { RUTA_SKUS } from '../lib/sku-rutas';
import { SkuForm, type SkuFormProps } from './SkuForm';

export type SkuPaginaFormularioProps = Omit<SkuFormProps, 'onCancel'> & {
  title: string;
  description: string;
};

export function SkuPaginaFormulario({ title, description, ...formProps }: SkuPaginaFormularioProps) {
  const navigate = useNavigate();

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={title}
        context={<Link to={RUTA_SKUS}>Catálogo / SKU</Link>}
        description={description}
      />
      <div className="mt-6">
        <SkuForm {...formProps} onCancel={() => void navigate(RUTA_SKUS)} />
      </div>
    </div>
  );
}
```

`frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.tsx`:

```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../app/api';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { crearSku } from '../api/skus.api';
import { SkuPaginaFormulario } from '../components/SkuPaginaFormulario';
import { toSkuPayload } from '../lib/sku-form';
import { RUTA_SKUS } from '../lib/sku-rutas';
import type { SkuFormValues } from '../schemas/sku.schema';

export function NuevoSkuPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const createMutation = useMutation({
    mutationFn: (values: SkuFormValues) => crearSku(apiClient, toSkuPayload(values)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });
      void navigate(RUTA_SKUS);
    }
  });

  return (
    <SkuPaginaFormulario
      title="Nuevo SKU"
      description="Registra un SKU comercial con sus unidades, fracción y control de stock."
      onSubmit={(values) => createMutation.mutate(values)}
      submitLabel="Crear SKU"
      isSubmitting={createMutation.isPending}
      error={createMutation.error ? describeApiError(createMutation.error) : null}
    />
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.test.tsx apps/erp-web/src/features/catalogo/lib/sku-rutas.test.ts`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.ts frontend/apps/erp-web/src/features/catalogo/lib/sku-rutas.test.ts frontend/apps/erp-web/src/features/catalogo/components/SkuPaginaFormulario.tsx frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.tsx frontend/apps/erp-web/src/features/catalogo/pages/NuevoSkuPage.test.tsx
git commit -m "feat(catalogo): pagina completa para crear un SKU

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Página de edición

**Files:**
- Create: `frontend/apps/erp-web/src/features/catalogo/pages/sku-api.fixture.ts`
- Create: `frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.tsx`
- Create: `frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.test.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx` (usar la fixture compartida; la limpieza de los tests del modal se hace en Task 6)

**Interfaces:**
- Consumes: `SkuPaginaFormulario` (Task 3), `skuQuery(skuId)`, `actualizarSku(client, skuId, payload)`, `toSkuFormValues`, `toSkuPayload`, `useRouteParam`.
- Produces: `EditarSkuPage()`, y `skuApi(overrides?: Record<string, unknown>)` en `pages/sku-api.fixture.ts` (solo para pruebas).

- [ ] **Step 1: Extraer la fixture compartida**

Crear `frontend/apps/erp-web/src/features/catalogo/pages/sku-api.fixture.ts` con el contenido exacto de la función `skuApi` que hoy está en `SkuDetailPage.test.tsx`:

```ts
export function skuApi(overrides: Record<string, unknown> = {}) {
  return {
    id: 'sku-1',
    tenantId: 'tenant-1',
    productoReguladoId: 'pr-1',
    categoriaId: 'categoria-1',
    marcaId: 'marca-desconocida',
    tipoSku: 'REGULADO',
    codigoInterno: 'SKU-001',
    descripcionComercial: 'Paracetamol 500 mg x 100',
    nombreCorto: null,
    presentacionComercial: null,
    unidadVentaCodigo: 'UND',
    contenido: 100,
    unidadContenidoCodigo: null,
    pesoGramos: null,
    altoCm: null,
    anchoCm: null,
    largoCm: null,
    permiteVentaFraccion: false,
    factorFraccion: null,
    unidadFraccionCodigo: null,
    requiereLote: true,
    requiereVencimiento: true,
    afectoIgv: true,
    stockMinimoDefault: 5,
    stockMaximoDefault: null,
    precioVentaReferencia: null,
    imagenUri: null,
    codigosBarra: [],
    estado: 'ACTIVO',
    createdBy: 'admin',
    createdAt: '2026-01-01T00:00:00Z',
    updatedBy: null,
    updatedAt: null,
    ...overrides
  };
}
```

En `SkuDetailPage.test.tsx`: borrar la función local `skuApi` (desde `function skuApi(` hasta su `}` de cierre) y añadir al bloque de imports `import { skuApi } from './sku-api.fixture';`.

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx`
Expected: PASS (las pruebas actuales del detalle siguen verdes).

- [ ] **Step 2: Escribir las pruebas de la página**

`frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { server } from '../../../test/mocks/server';
import { EditarSkuPage } from './EditarSkuPage';
import { skuApi } from './sku-api.fixture';

function renderPage() {
  server.use(
    http.get('*/api/v1/catalogo/productos-regulados', () =>
      HttpResponse.json({
        items: [
          {
            id: 'pr-1',
            denominacion: 'Paracetamol 500 mg',
            condicionVentaCodigo: 'VL',
            estadoRegulatorio: 'VIGENTE'
          }
        ],
        page: 0,
        size: 100,
        totalElements: 1
      })
    )
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidar = vi.spyOn(queryClient, 'invalidateQueries');
  const router = createMemoryRouter(
    [
      { path: '/catalogo/skus/:skuId/editar', Component: EditarSkuPage },
      { path: '/catalogo/skus', Component: () => <h1>Listado de SKU</h1> }
    ],
    { initialEntries: ['/catalogo/skus/sku-1/editar'] }
  );
  return {
    invalidar,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    )
  };
}

describe('EditarSkuPage', () => {
  it('muestra el estado de carga y luego el formulario con los datos del SKU', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi()))
    );

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('SKU-001')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Editar SKU' })).toBeInTheDocument();
    expect(screen.getByDisplayValue('Paracetamol 500 mg x 100')).toBeInTheDocument();
  });

  it('muestra el error cuando el SKU no existe', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => new HttpResponse(null, { status: 404 }))
    );

    renderPage();

    expect(
      await screen.findByText('El recurso no existe o no pertenece a tu organización.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument();
  });

  it('guarda los cambios sin enviar tenant, refresca el listado y vuelve a él', async () => {
    let body: Record<string, unknown> = {};
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi())),
      http.put('*/api/v1/catalogo/skus/sku-1', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(skuApi({ descripcionComercial: body.descripcionComercial }));
      })
    );

    const { invalidar, user } = renderPage();
    const campo = await screen.findByLabelText('Descripción comercial');
    await user.clear(campo);
    await user.type(campo, 'Paracetamol 500 mg x 50');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('heading', { name: 'Listado de SKU' })).toBeInTheDocument();
    expect(body).not.toHaveProperty('tenantId');
    expect(body).toMatchObject({
      codigoInterno: 'SKU-001',
      descripcionComercial: 'Paracetamol 500 mg x 50',
      stockMinimoDefault: 5
    });
    expect(invalidar).toHaveBeenCalledWith({ queryKey: ['catalogo', 'skus'] });
  });

  it('muestra el error del servidor al guardar y conserva el formulario', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi())),
      http.put('*/api/v1/catalogo/skus/sku-1', () => new HttpResponse(null, { status: 403 }))
    );

    const { user } = renderPage();
    await screen.findByDisplayValue('SKU-001');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('No tienes permiso para esta acción.')).toBeInTheDocument();
    expect(screen.getByDisplayValue('SKU-001')).toBeInTheDocument();
  });

  it('cancela y vuelve al listado', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi()))
    );

    const { user } = renderPage();
    await screen.findByDisplayValue('SKU-001');
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByRole('heading', { name: 'Listado de SKU' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/EditarSkuPage.test.tsx`
Expected: FAIL, "Failed to resolve import './EditarSkuPage'".

- [ ] **Step 4: Implementar**

`frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.tsx`:

```tsx
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { apiClient } from '../../../app/api';
import { FormError } from '../../../shared/components/FormError';
import { describeApiError } from '../../../shared/lib/describe-api-error';
import { useRouteParam } from '../../../shared/lib/use-route-param';
import { actualizarSku, skuQuery } from '../api/skus.api';
import { SkuPaginaFormulario } from '../components/SkuPaginaFormulario';
import { toSkuFormValues, toSkuPayload } from '../lib/sku-form';
import { RUTA_SKUS } from '../lib/sku-rutas';
import type { SkuFormValues } from '../schemas/sku.schema';

export function EditarSkuPage() {
  const skuId = useRouteParam('skuId');
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const result = useQuery(skuQuery(skuId));

  const updateMutation = useMutation({
    mutationFn: (values: SkuFormValues) => actualizarSku(apiClient, skuId, toSkuPayload(values)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['catalogo', 'skus'] });
      void navigate(RUTA_SKUS);
    }
  });

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  return (
    <SkuPaginaFormulario
      title="Editar SKU"
      description={`Modifica los datos del SKU ${result.data.codigoInterno}.`}
      defaultValues={toSkuFormValues(result.data)}
      onSubmit={(values) => updateMutation.mutate(values)}
      submitLabel="Guardar cambios"
      isSubmitting={updateMutation.isPending}
      error={updateMutation.error ? describeApiError(updateMutation.error) : null}
    />
  );
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/EditarSkuPage.test.tsx`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/pages/sku-api.fixture.ts frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.tsx frontend/apps/erp-web/src/features/catalogo/pages/EditarSkuPage.test.tsx frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx
git commit -m "feat(catalogo): pagina completa para editar un SKU

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Registrar las rutas

**Files:**
- Modify: `frontend/apps/erp-web/src/features/catalogo/routes.tsx` (insertar antes del bloque `catalogo/skus/:skuId`)
- Create: `frontend/apps/erp-web/src/features/catalogo/routes.test.ts`

**Interfaces:**
- Consumes: `NuevoSkuPage`, `EditarSkuPage` (Tasks 3 y 4).
- Produces: rutas `catalogo/skus/nuevo` y `catalogo/skus/:skuId/editar`.

- [ ] **Step 1: Escribir la prueba**

`frontend/apps/erp-web/src/features/catalogo/routes.test.ts`:

```ts
import { catalogRoutes } from './routes';

const paths = catalogRoutes.map(({ path }) => path);

describe('catalogRoutes de SKU', () => {
  it('registra nuevo antes del detalle para que no se interprete como id', () => {
    expect(paths).toContain('catalogo/skus/nuevo');
    expect(paths).toContain('catalogo/skus/:skuId/editar');
    expect(paths.indexOf('catalogo/skus/nuevo')).toBeLessThan(
      paths.indexOf('catalogo/skus/:skuId')
    );
  });

  it.each(['catalogo/skus/nuevo', 'catalogo/skus/:skuId/editar'])(
    'carga la página de %s',
    async (path) => {
      const route = catalogRoutes.find((item) => item.path === path) as {
        lazy: () => Promise<{ Component: unknown }>;
      };

      const { Component } = await route.lazy();

      expect(Component).toBeTypeOf('function');
    }
  );
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/routes.test.ts`
Expected: FAIL (las rutas no existen: `toContain` falla).

- [ ] **Step 3: Añadir las rutas**

En `routes.tsx`, justo antes del objeto con `path: 'catalogo/skus/:skuId'`, insertar:

```tsx
  {
    path: 'catalogo/skus/nuevo',
    lazy: async () => {
      const { NuevoSkuPage } = await import('./pages/NuevoSkuPage');
      return { Component: NuevoSkuPage };
    }
  },
  {
    path: 'catalogo/skus/:skuId/editar',
    lazy: async () => {
      const { EditarSkuPage } = await import('./pages/EditarSkuPage');
      return { Component: EditarSkuPage };
    }
  },
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/routes.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/routes.tsx frontend/apps/erp-web/src/features/catalogo/routes.test.ts
git commit -m "feat(catalogo): rutas de crear y editar SKU

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Listado y detalle enlazan a las páginas (se eliminan los modales)

**Files:**
- Modify (reemplazo completo): `frontend/apps/erp-web/src/features/catalogo/pages/SkusPage.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/pages/SkusPage.test.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.tsx`
- Modify: `frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx`

**Interfaces:**
- Consumes: `RUTA_NUEVO_SKU`, `rutaEditarSku` (Task 3); `buttonClassName` de `@boticas/ui-web`.

- [ ] **Step 1: Actualizar las pruebas del listado**

En `SkusPage.test.tsx`:

1. Borrar los dos bloques `it` completos: `'crea un SKU sin enviar tenant y refresca el listado'` y `'muestra el error del servidor al crear y permite cerrar el dialogo'`.
2. Añadir dentro del `describe('SkusPage', ...)`:

```tsx
  it('ofrece un enlace a la página de nuevo SKU', async () => {
    server.use(http.get('*/api/v1/catalogo/skus', () => HttpResponse.json(pagina([]))));

    renderPage();

    expect(await screen.findByText('No se encontraron SKU.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nuevo SKU' })).toHaveAttribute(
      'href',
      '/catalogo/skus/nuevo'
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
```

- [ ] **Step 2: Actualizar las pruebas del detalle**

En `SkuDetailPage.test.tsx`:

1. Borrar los dos bloques `it`: `'edita el SKU sin enviar tenant y refresca los datos'` y `'muestra el error del servidor al editar y permite cerrar el dialogo'`.
2. Añadir dentro del `describe`:

```tsx
  it('ofrece un enlace a la página de edición', async () => {
    server.use(
      http.get('*/api/v1/catalogo/skus/sku-1', () => HttpResponse.json(skuApi()))
    );

    renderPage();

    expect(await screen.findByRole('link', { name: 'Editar' })).toHaveAttribute(
      'href',
      '/catalogo/skus/sku-1/editar'
    );
  });
```

Quitar los imports que queden sin uso en ambos archivos (`waitFor` solo si ya no se usa; `SkuForm` no se importa en las pruebas).

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo/pages/SkusPage.test.tsx apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx`
Expected: FAIL en las dos pruebas nuevas (`Nuevo SKU` y `Editar` siguen siendo botones, no enlaces).

- [ ] **Step 4: Reemplazar `SkusPage.tsx`**

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { DataTable, EstadoBadge, ListFilters, PageHeader, buttonClassName } from '@boticas/ui-web';
import { skusQuery } from '../api/skus.api';
import { ESTADOS_SKU, TIPOS_SKU, type SkuResumen } from '../api/skus.types';
import { EnlaceDetalle } from '../components/EnlaceDetalle';
import { FiltroSelect } from '../components/FiltroSelect';
import { RUTA_NUEVO_SKU } from '../lib/sku-rutas';
import { opcionesDeValores, useOpcionesCategorias, useOpcionesMarcas } from '../lib/use-opciones';

export function SkusPage() {
  const [search, setSearch] = useState('');
  const [tipo, setTipo] = useState('');
  const [estado, setEstado] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [marcaId, setMarcaId] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const categorias = useOpcionesCategorias();
  const marcas = useOpcionesMarcas();

  const { data, isPending, isError } = useQuery(
    skusQuery({
      q: search || undefined,
      categoriaId: categoriaId || undefined,
      marcaId: marcaId || undefined,
      tipoSku: tipo || undefined,
      estado: estado || undefined,
      page,
      size
    })
  );

  const filtrar = (actualizar: (value: string) => void) => (value: string) => {
    actualizar(value);
    setPage(0);
  };

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="SKU comerciales"
        context="Catálogo / SKU"
        description="Administra los SKU comerciales, sus códigos de barras y su estado."
        actions={
          <Link to={RUTA_NUEVO_SKU} className={buttonClassName()}>
            Nuevo SKU
          </Link>
        }
      />

      <ListFilters
        label="Buscar SKU"
        placeholder="Código interno o descripción"
        value={search}
        onValueChange={filtrar(setSearch)}
      >
        <FiltroSelect
          id="filtro-tipo-sku"
          label="Tipo de SKU"
          value={tipo}
          onValueChange={filtrar(setTipo)}
          opciones={opcionesDeValores(TIPOS_SKU)}
          opcionVacia="Todos"
        />
        <FiltroSelect
          id="filtro-estado-sku"
          label="Estado"
          value={estado}
          onValueChange={filtrar(setEstado)}
          opciones={opcionesDeValores(ESTADOS_SKU)}
          opcionVacia="Todos"
        />
        <FiltroSelect
          id="filtro-categoria-sku"
          label="Categoría"
          value={categoriaId}
          onValueChange={filtrar(setCategoriaId)}
          opciones={categorias}
          opcionVacia="Todas"
        />
        <FiltroSelect
          id="filtro-marca-sku"
          label="Marca"
          value={marcaId}
          onValueChange={filtrar(setMarcaId)}
          opciones={marcas}
          opcionVacia="Todas"
        />
      </ListFilters>

      <div className="mt-6">
        <DataTable<SkuResumen>
          columns={[
            { header: 'N°', cell: (_row, index) => index + 1 },
            { header: 'Código interno', cell: (row) => row.codigoInterno },
            { header: 'Descripción comercial', cell: (row) => row.descripcionComercial },
            { header: 'Tipo', cell: (row) => row.tipoSku },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <EnlaceDetalle nombre={row.codigoInterno} href={`/catalogo/skus/${row.id}`} />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="No se encontraron SKU."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de SKU."
          startIndex={page * size}
          pagination={{
            page,
            size,
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

- [ ] **Step 5: Editar `SkuDetailPage.tsx`**

Aplicar estos cambios exactos:

1. Imports: reemplazar
```tsx
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
```
por
```tsx
import { Button, Card, EstadoBadge, PageHeader, buttonClassName } from '@boticas/ui-web';
```
2. Quitar los imports `actualizarSku` (dejar `import { cambiarEstadoSku, skuQuery } from '../api/skus.api';`), `import { SkuForm } from '../components/SkuForm';`, `import { toSkuFormValues, toSkuPayload } from '../lib/sku-form';` y `import type { SkuFormValues } from '../schemas/sku.schema';`. Añadir `import { rutaEditarSku } from '../lib/sku-rutas';`.
3. Quitar `const [editOpen, setEditOpen] = useState(false);` y el bloque completo `const updateMutation = useMutation({ ... });`.
4. En `actions`, reemplazar el primer `<Button variant="secondary" onClick={...}>Editar</Button>` por:
```tsx
            <Link to={rutaEditarSku(sku.id)} className={buttonClassName('secondary')}>
              Editar
            </Link>
```
(el botón "Cambiar estado" no cambia).
5. Quitar el bloque `<Modal open={editOpen} ...>...</Modal>` completo.

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd C:/w/sku/frontend && pnpm.cmd exec vitest run apps/erp-web/src/features/catalogo`
Expected: PASS en toda la feature catálogo.

- [ ] **Step 7: Tipos y lint**

Run: `cd C:/w/sku/frontend && pnpm.cmd typecheck`
Expected: 3 proyectos `Done`, sin errores.

Run: `cd C:/w/sku/frontend && pnpm.cmd exec eslint apps/erp-web/src/features/catalogo`
Expected: 0 errores (las advertencias existentes de `watch` en seguridad no aplican a esta carpeta).

- [ ] **Step 8: Commit**

```bash
cd C:/w/sku
git add frontend/apps/erp-web/src/features/catalogo/pages/SkusPage.tsx frontend/apps/erp-web/src/features/catalogo/pages/SkusPage.test.tsx frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.tsx frontend/apps/erp-web/src/features/catalogo/pages/SkuDetailPage.test.tsx
git commit -m "feat(catalogo): listado y detalle de SKU enlazan a las paginas de crear y editar

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Verificación final

**Files:** ninguno.

- [ ] **Step 1: `pnpm check` completo**

Run: `cd C:/w/sku/frontend && pnpm.cmd check`
Expected: `exit 0`: lint con 0 errores, tipos OK, todas las pruebas pasan, cobertura sin violaciones de umbral, build correcto. Tarda unos 9 minutos; ejecutar en segundo plano con la salida a un archivo y revisar el final del log. Si falla la cobertura, el log nombra el archivo y las líneas o ramas sin cubrir: añadir la prueba que falta, no excluir el archivo.

- [ ] **Step 2: Verificación en navegador contra el backend real**

Levantar el backend (`cd service-botica && .\gradlew.bat :bootstrap-app:bootRun`, requiere PostgreSQL) y el frontend (`cd frontend && pnpm.cmd dev`, puerto 3000). Con la sesión iniciada comprobar:

1. En `/catalogo/skus`, "Nuevo SKU" navega a `/catalogo/skus/nuevo` y no abre ningún diálogo.
2. Tipo REGULADO: aparece "Producto regulado". Cambiar a NO_REGULADO: desaparece.
3. "Permite venta por fracción" desmarcado: no hay factor ni unidad de fracción; al marcarlo aparecen.
4. La barra Cancelar/Guardar se mantiene visible al desplazarse y no tapa el último campo.
5. Crear un SKU no regulado sin fracción: vuelve al listado y el SKU aparece.
6. Crear un SKU regulado con fracción (factor y unidad): se guarda correctamente.
7. Desde el detalle, "Editar" abre `/catalogo/skus/:id/editar` con los datos cargados; cambiar la descripción, guardar y comprobar el cambio en el listado.
8. Provocar un error (código interno duplicado): el mensaje aparece arriba y el formulario conserva lo escrito.
9. Vista en móvil (ancho estrecho): una columna, sin scroll horizontal.

Anotar cualquier diferencia; corregirla con una prueba que la cubra antes de continuar.

- [ ] **Step 3: Revisar el alcance del cambio**

Run: `cd C:/w/sku && git status --short && git log --oneline origin/developer..HEAD`
Expected: árbol limpio y solo los commits de este plan (más los dos de documentación traídos en la Task 0). Ningún archivo fuera de `features/catalogo/` ni de `docs/superpowers/`.

- [ ] **Step 4: Subir la rama y abrir el PR (solo si se pide)**

No hacer push ni abrir PR sin autorización explícita. Cuando se pida: `git push -u origin feature/sku-pagina-completa` y `gh pr create --base developer`.
