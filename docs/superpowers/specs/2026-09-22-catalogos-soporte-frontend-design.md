# Integración frontend de los catálogos de soporte — Design

## Contexto

El backend (`service-botica/modules/catalogo/`) expone 10 de los ~11 catálogos maestros de `sch_catalogo` como API REST CRUD completa bajo `/api/v1/catalogo/*` (CQRS + `Result` + `@PreAuthorize`). El frontend (`frontend/apps/erp-web/src/features/catalogo/`) ya integra 2 de esos 10 contra el backend real: **Categorías** y **Marcas**, con un patrón maduro (`api/`, `components/`, `pages/`, `schemas/`) calcado del patrón usado en `features/seguridad/`.

Faltan por integrar 8 catálogos. De esos, 6 son **catálogos de soporte**: entidades globales, identificadas por `codigo: string` (no `UUID`), sin `tenantId`, sin paginación ni búsqueda server-side en su endpoint de listado:

- Rubros comerciales — **excepción**: este es tenant-scoped (usa `id: UUID`, requiere `tenantId`), igual que Categoría/Marca, no como los otros 5. Se incluye en este plan por ser sencillo y quedar completo el bloque de "catálogos simples".
- Condiciones de venta
- Formas farmacéuticas
- Vías de administración
- Unidades de medida
- Clasificaciones controladas

Quedan fuera de este plan (más complejos, con relaciones N:M): Principios Activos y Productos Regulados/SKU.

## Objetivo

Implementar, para cada uno de los 6 catálogos anteriores, un slice frontend independiente que siga exactamente el patrón ya validado de Categoría/Marca: `api/*.api.ts+.types.ts+.test.ts`, `schemas/*.schema.ts`, `components/*Form.tsx(+.test.tsx)`, `pages/*Page.tsx(+.test.tsx)`, ruta lazy en `routes.tsx`, card en `CatalogPage`, y handlers MSW para modo mock.

## Decisiones de diseño

### 1. Un slice independiente por catálogo (no una abstracción compartida)

A pesar de que los 5 catálogos "puros de soporte" comparten forma casi idéntica, se implementan como 6 slices independientes (uno por catálogo), calcando el patrón de `MarcasPage`/`MarcaForm`/`marcas.api.ts` archivo por archivo. Se prioriza consistencia con el código existente y simplicidad de lectura por sobre una abstracción genérica prematura — cada pieza se puede entender y modificar sin conocer una capa de indirección adicional.

### 2. Identificador y forma de request según tipo de catálogo

**Rubro Comercial** (tenant-scoped, sigue el patrón exacto de Marca):
- Identificador: `id: UUID`. Requiere `tenantId` en query params (GET) y en el body (POST/PUT/PATCH estado).
- Ruta backend: `/api/v1/catalogo/rubros-comerciales`.
- Campos: `codigo`, `nombre`, `descripcion` (nullable), `esFarmaceutico: boolean`, `orden: number`, `estado`.

**Los otros 5 (condiciones de venta, formas farmacéuticas, vías de administración, unidades de medida, clasificaciones controladas)** — catálogos globales de soporte:
- Identificador: `codigo: string`. Las rutas usan `{codigo}` en el path, nunca `tenantId`.
- El backend no pagina (`GET /...?estado=` devuelve el array completo, sin `page`/`size`/`totalElements`) ni admite búsqueda por texto server-side.
- Los `*.api.ts` de estos 5 devuelven `Promise<CatalogoItem[]>` directamente desde `fetchX`, no `PaginaResponse<T>`.

### 3. Paginación y búsqueda en cliente para los 5 catálogos sin paginación server-side

Para mantener la misma experiencia visual que Categorías/Marcas (tabla + input de búsqueda + `Pagination`), cada página de estos 5 catálogos:
1. Trae la lista completa una sola vez vía `xQuery` (TanStack Query, `queryKey: ['catalogo', '<slug>', estado]`, sin `page`/`size`/`q` en la key).
2. Filtra en cliente por texto libre (`codigo` o `denominacion`, case-insensitive) usando `useMemo` sobre `data`.
3. Pagina en cliente cortando el array filtrado (`slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)`) y construye un objeto `{ items, page, size, totalElements }` compatible con el componente `Pagination` existente (sin cambios a `Pagination.tsx`).
4. El único filtro real contra el backend es `estado` (Activo/Inactivo/Todos), ya que el propio endpoint lo soporta vía query param.

Rubro Comercial, al ser tenant-scoped con paginación server-side real (igual que Marca), no aplica este punto — usa `fetchRubrosComerciales` con `page`/`size`/`q` enviados al backend, igual que `fetchMarcas`.

### 4. Formularios: campos por catálogo

Todos siguen el patrón de `MarcaForm` (react-hook-form + `zodResolver` + `FormField` de `@boticas/ui-web`), agregando `<input type="checkbox">` (patrón ya usado en `UsuarioForm.tsx:99-104`) para los campos booleanos:

| Catálogo | Campos del formulario |
|---|---|
| Rubro Comercial | `codigo`, `nombre`, `descripcion?`, `esFarmaceutico` (checkbox), `orden` (number) |
| Condición de Venta | `codigo`, `denominacion`, `requiereReceta` (checkbox), `requiereRetencion` (checkbox), `fuente`, `versionFuente` |
| Forma Farmacéutica | `codigo`, `denominacion`, `fuente` |
| Vía de Administración | `codigo`, `denominacion`, `fuente` |
| Unidad de Medida | `codigo`, `denominacion`, `simbolo`, `permiteDecimal` (checkbox), `fuente` |
| Clasificación Controlada | `codigo`, `denominacion`, `normaFuente`, `requiereRecetaEspecial` (checkbox), `retieneReceta` (checkbox), `vigenciaRecetaDias?` (number opcional) |

`vigenteDesde`/`vigenteHasta` de Condición de Venta y campos de auditoría (`createdAt`, etc.) **no** se incluyen en el formulario de creación/edición — son de solo lectura o fuera de alcance de este plan (el backend los acepta opcionalmente pero no hay campo de UI para ellos, igual que Marca no expone campos de auditoría en su formulario).

### 5. Cambio de estado (activar/inactivar)

Igual que Marca: botón inline en la tabla que alterna `ACTIVO`/`INACTIVO` vía `PATCH /{id-o-codigo}/estado`, sin diálogo de confirmación (consistente con el patrón actual de Categoría/Marca, que tampoco confirma). No se introduce `ConfirmActionDialog` aquí para no divergir del patrón ya usado por los 2 catálogos existentes.

### 6. Mocks MSW

Se agregan handlers en `frontend/apps/erp-web/src/test/mocks/handlers.ts` para los 6 catálogos nuevos **y también para Categorías/Marcas**, que hoy no tienen ninguno (gap detectado, fuera del alcance original pero necesario para que `pnpm dev` en modo mock funcione consistentemente en todo `features/catalogo/`). Cada catálogo obtiene:
- `GET` de listado (todos los items o paginado según corresponda al tipo de catálogo).
- `GET` de detalle por id/código.
- `POST` crear.
- `PUT` actualizar.
- `PATCH /estado` cambiar estado.

Los datos fixture son representativos y mínimos (2-3 registros por catálogo), consistentes con los DTOs reales del backend.

### 7. Navegación

`CatalogPage.tsx` pasa de 2 a 8 cards en el mismo grid (`sections` array extendido), sin reestructurar la landing en secciones. Cada card usa un ícono de `lucide-react` distinto y navega a `/catalogo/<slug>`.

Slugs de ruta:
- `/catalogo/rubros-comerciales`
- `/catalogo/condiciones-venta`
- `/catalogo/formas-farmaceuticas`
- `/catalogo/vias-administracion`
- `/catalogo/unidades-medida`
- `/catalogo/clasificaciones-controladas`

### 8. Permisos

No se agrega lógica de permisos en frontend (Categoría/Marca tampoco la tienen hoy) — el backend rechaza con 403 si el usuario no tiene `catalogo.soporte.gestionar`/`.consultar` (o `catalogo.rubros-comerciales.*` para Rubro Comercial), y el `GlobalExceptionHandler`/`ApplicationErrorHttpMapper` ya mapea eso a un `ProblemDetail` que el `DataTable`/mutations muestran como error genérico, igual que cualquier otro fallo de red.

## Fuera de alcance

- Principios Activos y Productos Regulados/SKU (catálogos más complejos, con relaciones N:M — plan aparte).
- `tipo_documento_identidad` (sin implementar en el backend todavía).
- Tests de integración HTTP del backend (gap pre-existente, no se resuelve desde el frontend).
- Cualquier cambio a `Pagination.tsx`, `DataTable`, `FormField` o el `apiClient` — se reutilizan tal cual existen hoy.

## Archivos nuevos por catálogo (× 6)

```
features/catalogo/api/<slug>.api.ts
features/catalogo/api/<slug>.types.ts
features/catalogo/api/<slug>.api.test.ts
features/catalogo/schemas/<slug>.schema.ts
features/catalogo/components/<Catalogo>Form.tsx
features/catalogo/components/<Catalogo>Form.test.tsx
features/catalogo/pages/<Catalogo>Page.tsx
features/catalogo/pages/<Catalogo>Page.test.tsx
```

## Archivos modificados

```
features/catalogo/routes.tsx            — +6 rutas lazy
features/catalogo/pages/CatalogPage.tsx — +6 cards
test/mocks/handlers.ts                  — +handlers de los 6 nuevos +handlers faltantes de categorías/marcas
app/feature-routes.test.ts              — actualizar lista de paths esperados
```

## Testing

- Unitario: cada `*.api.test.ts` cubre `fetchX`/`crearX`/`actualizarX`/`cambiarEstadoX` contra el `ApiClient` (mockeado), igual que `marcas.api.test.ts`.
- Unitario: cada `*Form.test.tsx` cubre validación zod (campos requeridos, límites) y submit, igual que `MarcaForm` (si existe su test — verificar patrón exacto al implementar).
- Unitario: cada `*Page.test.tsx` cubre render de tabla, filtro de estado, búsqueda en cliente (para los 5 sin paginación server-side) y flujo crear/editar/cambiar estado con `MSW` interceptando.
- `pnpm check` (lint + typecheck + test + build) en verde al final.
- Verificación manual en navegador en modo mock: navegar a cada una de las 6 páginas nuevas desde `CatalogPage`, crear/editar/activar-desactivar un registro de cada catálogo.

## Self-Review

- **Placeholder scan:** sin TBD/TODO.
- **Consistencia interna:** la distinción tenant-scoped (Rubro Comercial) vs. global-por-código (los otros 5) se aplica consistentemente en las secciones 2, 3 y 4.
- **Scope check:** acotado a 6 catálogos, dejando explícitamente fuera Principios Activos/Productos Regulados-SKU y tipo_documento_identidad — apto para un solo plan de implementación (probablemente con una tarea por catálogo, siguiendo `superpowers:writing-plans`).
- **Ambigüedad:** el punto de mocks para Categorías/Marcas (gap pre-existente) se declaró explícitamente en alcance para no dejarlo ambiguo.
