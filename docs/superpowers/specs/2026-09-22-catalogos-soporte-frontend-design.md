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

Implementar los 6 catálogos anteriores en el frontend: Rubro Comercial como slice independiente que sigue exactamente el patrón ya validado de Marca (`api/*.api.ts+.types.ts+.test.ts`, `schemas/*.schema.ts`, `components/*Form.tsx`, `pages/*Page.tsx(+.test.tsx)`), y los otros 5 como configuraciones livianas sobre un módulo genérico compartido (`support-catalog/`) que concentra la lógica repetida de fetch, formulario y tabla. Todos con ruta lazy en `routes.tsx`, card en `CatalogPage`, y handlers MSW para modo mock.

## Decisiones de diseño

### 1. Rubro Comercial calca el patrón de Marca; los otros 5 comparten un módulo genérico

`CLAUDE.md` exige evitar código duplicado y extraer abstracciones reutilizables cuando hay patrones repetidos, y exige 100% de cobertura de tests en código nuevo — dos slices casi idénticos multiplicados por 5 (API, formulario y página) violarían esa regla sin aportar nada, porque los 5 catálogos "puros de soporte" (Condición de Venta, Forma Farmacéutica, Vía de Administración, Unidad de Medida, Clasificación Controlada) comparten exactamente la misma forma de contrato HTTP: `codigo: string` como identificador, `GET` de lista sin paginar con único filtro `estado`, `GET /{codigo}`, `POST`, `PUT /{codigo}`, `PATCH /{codigo}/estado` con body `{ status }`.

Por eso:
- **Rubro Comercial** se implementa como slice independiente calcando `marcas.api.ts`/`MarcaForm.tsx`/`MarcasPage.tsx` archivo por archivo — su contrato (tenant-scoped, `id: UUID`, paginado server-side) es genuinamente distinto al de los otros 5, así que no comparte el módulo genérico.
- **Los otros 5** comparten un módulo genérico bajo `features/catalogo/support-catalog/`:
  - `support-catalog.api.ts` — factory `createSupportCatalogApi<TItem, TRequest>(resource: string)` que devuelve las funciones `fetchList`, `fetchOne`, `create`, `update`, `changeStatus`, `listQuery` contra `/catalogo/{resource}`.
  - `SupportCatalogForm.tsx` — formulario genérico dirigido por una lista de definición de campos (`FieldDef[]`), con validación delegada al `zodResolver` del schema que cada catálogo define.
  - `SupportCatalogPage.tsx` — página genérica (tabla + búsqueda/paginado en cliente + filtro de estado + modales crear/editar) parametrizada por columnas, campos de formulario, título y la instancia de API.
  - Cada uno de los 5 catálogos reales se reduce a un archivo de configuración (`<slug>.config.ts`: tipos `Item`/`Request`, schema zod, columnas de tabla, `FieldDef[]`, instancia de la API vía la factory) más su entrada de ruta — sin repetir lógica de fetch, formulario o tabla.

Cada unidad genérica es testeable de forma aislada (tests de la factory de API con un `resource` de ejemplo, tests del formulario genérico con un `FieldDef[]` de ejemplo, tests de la página genérica con una instancia de API mockeada), y cada configuración de catálogo real se prueba con un test de integración liviano que verifica que el catálogo se renderiza y llama a los endpoints correctos con MSW — sin duplicar la lógica ya cubierta en los tests del módulo genérico.

### 2. Identificador y forma de request según tipo de catálogo

**Rubro Comercial** (tenant-scoped, sigue el patrón exacto de Marca):
- Identificador: `id: UUID`. Requiere `tenantId` en query params (GET) y en el body (POST/PUT/PATCH estado).
- Ruta backend: `/api/v1/catalogo/rubros-comerciales`.
- Campos: `codigo`, `nombre`, `descripcion` (nullable), `esFarmaceutico: boolean`, `orden: number`, `estado`.

**Los otros 5 (condiciones de venta, formas farmacéuticas, vías de administración, unidades de medida, clasificaciones controladas)** — catálogos globales de soporte:
- Identificador: `codigo: string`. Las rutas usan `{codigo}` en el path, nunca `tenantId`.
- El backend no pagina (`GET /...?estado=` devuelve el array completo, sin `page`/`size`/`totalElements`) ni admite búsqueda por texto server-side.
- Los `*.api.ts` de estos 5 devuelven `Promise<CatalogoItem[]>` directamente desde `fetchX`, no `PaginaResponse<T>`.

### 3. Paginación y búsqueda en cliente, centralizadas en `SupportCatalogPage`

Para mantener la misma experiencia visual que Categorías/Marcas (tabla + input de búsqueda + `Pagination`), `SupportCatalogPage` (el componente genérico) implementa una sola vez, para los 5 catálogos que lo usan:
1. Trae la lista completa una sola vez vía `listQuery` (TanStack Query, `queryKey: ['catalogo', resource, estado]`, sin `page`/`size`/`q` en la key).
2. Filtra en cliente por texto libre (`codigo` o el campo de denominación que cada config declare como buscable, case-insensitive) usando `useMemo` sobre `data`.
3. Pagina en cliente cortando el array filtrado (`slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)`) y construye un objeto `{ items, page, size, totalElements }` compatible con el componente `Pagination` existente (sin cambios a `Pagination.tsx`).
4. El único filtro real contra el backend es `estado` (Activo/Inactivo/Todos), ya que el propio endpoint lo soporta vía query param.

Esta lógica se implementa y se prueba **una sola vez** en `SupportCatalogPage.test.tsx`; las páginas concretas de cada catálogo no repiten estos casos de prueba.

Rubro Comercial, al ser tenant-scoped con paginación server-side real (igual que Marca), no usa `SupportCatalogPage` — usa `fetchRubrosComerciales` con `page`/`size`/`q` enviados al backend, igual que `fetchMarcas`, en su propia `RubrosComercialesPage.tsx`.

### 4. Formularios: `SupportCatalogForm` genérico + `FieldDef[]` por catálogo

`SupportCatalogForm` reemplaza a los 5 formularios repetidos: recibe `fields: FieldDef[]`, un `schema` de zod, `defaultValues`, `onSubmit` y `submitLabel`, y renderiza cada campo según su `type` (`'text' | 'textarea' | 'number' | 'checkbox'`) usando `FormField`/`register` de la misma forma que `MarcaForm` — incluyendo `<input type="checkbox">` para los booleanos (patrón ya usado en `UsuarioForm.tsx:99-104`). El componente en sí no conoce nada específico de ningún catálogo.

```ts
export type FieldDef = {
  name: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'checkbox';
};
```

Campos por catálogo (usados para construir tanto el `FieldDef[]` como el schema zod de cada config):

| Catálogo | Campos del formulario |
|---|---|
| Rubro Comercial (slice propio, no usa `SupportCatalogForm`) | `codigo`, `nombre`, `descripcion?`, `esFarmaceutico` (checkbox), `orden` (number) |
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

## Archivos nuevos — módulo genérico (una sola vez)

```
features/catalogo/support-catalog/support-catalog.api.ts
features/catalogo/support-catalog/support-catalog.api.test.ts
features/catalogo/support-catalog/SupportCatalogForm.tsx
features/catalogo/support-catalog/SupportCatalogForm.test.tsx
features/catalogo/support-catalog/SupportCatalogPage.tsx
features/catalogo/support-catalog/SupportCatalogPage.test.tsx
features/catalogo/support-catalog/index.ts
```

## Archivos nuevos — Rubro Comercial (slice propio, calca Marca)

```
features/catalogo/api/rubros-comerciales.api.ts
features/catalogo/api/rubros-comerciales.types.ts
features/catalogo/api/rubros-comerciales.api.test.ts
features/catalogo/schemas/rubro-comercial.schema.ts
features/catalogo/components/RubroComercialForm.tsx
features/catalogo/pages/RubrosComercialesPage.tsx
features/catalogo/pages/RubrosComercialesPage.test.tsx
```

## Archivos nuevos — configuración por catálogo de soporte (× 5)

```
features/catalogo/support-catalog/configs/<slug>.config.ts
features/catalogo/support-catalog/configs/<slug>.config.test.ts
```

Cada `<slug>.config.ts` exporta: el tipo `Item`, el tipo `Request`, el `schema` zod, el arreglo `fields: FieldDef[]`, las `columns` de tabla, y la instancia de API creada con `createSupportCatalogApi<Item, Request>('<resource>')`. Su `.config.test.ts` es un test de integración liviano: monta `SupportCatalogPage` con esa config y un servidor MSW con los endpoints reales del catálogo (`/catalogo/<resource>`), y verifica listar/crear/editar/cambiar-estado end-to-end — sin repetir los casos de borde ya cubiertos por `SupportCatalogPage.test.tsx` (paginación en cliente, búsqueda en cliente, filtro de estado), que se prueban una sola vez con una config de ejemplo.

## Archivos modificados

```
features/catalogo/routes.tsx            — +6 rutas lazy (rubros-comerciales + los 5 de soporte)
features/catalogo/pages/CatalogPage.tsx — +6 cards
test/mocks/handlers.ts                  — +handlers de los 6 nuevos +handlers faltantes de categorías/marcas
app/feature-routes.test.ts              — actualizar lista de paths esperados
```

## Testing

- Unitario: `support-catalog.api.test.ts` cubre `fetchList`/`fetchOne`/`create`/`update`/`changeStatus` de la factory contra el `ApiClient` (mockeado), con un `resource` de ejemplo — cubre a los 5 catálogos que la consumen.
- Unitario: `SupportCatalogForm.test.tsx` cubre render de cada `type` de `FieldDef` (text/textarea/number/checkbox), validación zod y submit, con un `fields`/`schema` de ejemplo.
- Unitario: `SupportCatalogPage.test.tsx` cubre render de tabla, filtro de estado, búsqueda en cliente, paginación en cliente y flujo crear/editar/cambiar estado con MSW, con una config de ejemplo.
- Unitario: cada `<slug>.config.test.ts` es un test de integración liviano (ver arriba) — no repite casos de la página genérica, solo confirma que la config real (schema, fields, resource) funciona end-to-end contra los endpoints reales de ese catálogo.
- Unitario: `rubros-comerciales.api.test.ts` y `RubrosComercialesPage.test.tsx` calcan exactamente `marcas.api.test.ts`/`MarcasPage.test.tsx`.
- `pnpm check` (lint + typecheck + test + build) en verde al final. Cobertura 100% en todos los archivos nuevos (`CLAUDE.md` § Cobertura de tests).
- Verificación manual en navegador en modo mock: navegar a cada una de las 6 páginas nuevas desde `CatalogPage`, crear/editar/activar-desactivar un registro de cada catálogo.

## Self-Review

- **Placeholder scan:** sin TBD/TODO.
- **Consistencia interna:** la distinción tenant-scoped (Rubro Comercial, slice propio) vs. global-por-código (los otros 5, módulo genérico `support-catalog/`) se aplica consistentemente en las secciones 1, 2, 3 y 4, y en la lista de archivos.
- **Scope check:** acotado a 6 catálogos, dejando explícitamente fuera Principios Activos/Productos Regulados-SKU y tipo_documento_identidad — apto para un solo plan de implementación (una tarea por el módulo genérico, una tarea por Rubro Comercial, una tarea por cada una de las 5 configs, siguiendo `superpowers:writing-plans`).
- **Ambigüedad:** el punto de mocks para Categorías/Marcas (gap pre-existente) se declaró explícitamente en alcance para no dejarlo ambiguo. La regla de "no duplicar código" de `CLAUDE.md` motivó el cambio de diseño de 6 slices independientes a 1 módulo genérico + 5 configs + 1 slice propio para Rubro Comercial.
