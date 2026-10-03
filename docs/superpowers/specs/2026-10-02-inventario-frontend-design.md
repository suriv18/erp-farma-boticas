# Diseño: frontend de inventario (posiciones, ajustes y lotes)

**Estado:** implementado y verificado en navegador contra el backend real (ingreso con lote nuevo, ajuste de salida con error de stock insuficiente, filtros, bloqueo y desbloqueo de lote).

**Hallazgos de la verificación manual (sin resolver, fuera de este slice):** `.env.development` apunta `DEV_API_TARGET` al contenedor `:8085` desactualizado; la tabla y el detalle muestran el UUID del SKU y no hay columna de almacén; los selects de almacén incluyen almacenes inactivos y de establecimientos clausurados o suspendidos; la búsqueda de SKU dispara una petición por tecla sin debounce; un lote `BLOQUEADO` muestra disponible igual al físico; `POST /catalogo/skus` con una unidad desconocida responde 500 en lugar de 400.

**Contexto:** `features/inventario/` hoy es un fixture (`InventoryPage.tsx` con filas estáticas). El backend de `inventario` ya está commiteado y verificado (`check` completo en verde, tests de integración y concurrencia). Este slice reemplaza el fixture por una integración real contra `packages/api-client`, siguiendo el patrón de `features/organizacion/` y `features/catalogo/`.

## Alcance

Incluye:
- Listado paginado de posiciones de inventario con filtros.
- Registro de ingresos con lote nuevo y de ajustes manuales (`AJUSTE_INGRESO`, `AJUSTE_SALIDA`) sobre un lote existente.
- Detalle de lote con bloqueo y desbloqueo.

No incluye:
- Listado de lotes ni historial de movimientos (el backend no los expone).
- Movimientos `INGRESO_COMPRA` (los genera `compras`).
- Reservas, transferencias, política offline (ADR-008, sigue Propuesto).
- Handlers de MSW para los endpoints de inventario (igual que `organizacion`).

## API del backend consumida

Base `/api/v1/inventario` (el cliente antepone `/api/v1`):

| Operación | Endpoint | Permiso |
|---|---|---|
| Listar posiciones | `GET /posiciones?establecimientoId&almacenId&skuId&page&size` | `inventario.posiciones.consultar` |
| Consultar lote | `GET /lotes/{loteId}` | `inventario.lotes.consultar` |
| Bloquear lote | `POST /lotes/{loteId}/bloqueos` body `{ motivo }` | `inventario.lotes.bloquear` |
| Desbloquear lote | `DELETE /lotes/{loteId}/bloqueos` | `inventario.lotes.bloquear` |
| Registrar movimiento | `POST /movimientos` body `{ almacenId, skuId, loteId?, numeroLote?, fechaVencimiento?, tipo, cantidad, motivo }` | `inventario.movimientos.registrar` |

`POST /movimientos` envía el encabezado `Idempotency-Key`. La clave es estable por payload (mismo payload, misma clave, de modo que un reintento no duplica el movimiento; payload editado, clave nueva) y la provee `useClaveIdempotencia`.

El tenant se resuelve en el backend desde el claim `tid`; el frontend no envía `tenantId`.

Tipos de respuesta relevantes: `PosicionResponse` (id, establecimientoId, almacenId, skuId, loteId, numeroLote, fechaVencimiento, estadoLote, estadoInventario, cantidadFisica, cantidadReservada, cantidadDisponible, vendible, version, ultimoMovimientoAt), `LoteResponse` (id, skuId, numeroLote, fechaVencimiento, estado, motivoEstado, bloqueadoAt, vendible), `MovimientoResponse`.

Estados de lote: `HABILITADO`, `CUARENTENA`, `BLOQUEADO`, `INMOVILIZADO_RECALL`, `VENCIDO`, `BAJA_DESTRUIDO`. Solo `HABILITADO`, `CUARENTENA` y `BLOQUEADO` admiten ingreso; `admiteBloqueo` es verdadero solo para `HABILITADO` y `CUARENTENA`; el desbloqueo solo aplica a `BLOQUEADO` y falla si el lote está vencido.

## Estructura de la feature

```
features/inventario/
  api/        posiciones.api.ts, lotes.api.ts, movimientos.api.ts (+ .types.ts, + tests)
  components/ tabla de posiciones, diálogo de ajuste, diálogo de bloqueo, acciones de fila
  lib/        payloads, resaltado de vencimiento (vencimiento.ts), puedeBloquear, almacenes por establecimiento,
              hooks useMutacionInventario, useClaveIdempotencia y usePosicionesFiltros
  schemas/    ajuste.schema.ts, bloqueo.schema.ts
  pages/      InventoryPage.tsx (posiciones), LoteDetailPage.tsx
  routes.tsx, index.ts
```

Reglas de la arquitectura frontend: la feature publica solo su `index.ts`; no importa internos de otra feature. Los filtros de posiciones viven en la URL mediante `usePosicionesFiltros`, que arma los parámetros desde una referencia (`ref`) porque `Pagination` dispara `onSizeChange` y luego `onPageChange` en el mismo tick. `useMutacionInventario` centraliza las mutaciones (invalidación de queries) y `useClaveIdempotencia` la clave del movimiento. Las utilidades compartidas `shared/lib/query-string.ts` y `pagina.types.ts` sirven a inventario y a otras features.

Las piezas técnicas reutilizables entre features (`buildQuery`, `describe-api-error`) deben tomarse de `shared/` si ya están allí; si hoy viven dentro de `organizacion`, se mueven a `shared/` en lugar de duplicarlas (prohibido el código duplicado).

Cada función de `api/` sigue el estilo de `almacenes.api.ts`: función `fetchX(client, params)` más `xQuery(params)` con `queryOptions` y `queryKey` estables (`['inventario', 'posiciones', 'lista', ...]`), y mutaciones que reciben el `ApiClient`.

## Datos de apoyo para filtros

- **Establecimiento y almacén:** `corporateStructureQuery` exportada por el `index.ts` de `organizacion`. Trae empresa → establecimientos → almacenes (id, nombre) en una llamada. El select de almacén depende del establecimiento elegido y se reinicia al cambiarlo.
- **SKU:** `skusQuery` exportada por el `index.ts` de `catalogo`, como búsqueda con `q`.

### Limitación conocida del backend

`PosicionResponse` trae `skuId` pero no nombre ni código del producto, y no existe un endpoint para resolver SKUs por lote de ids. Decisión para este slice: el nombre del producto se muestra solo cuando hay un SKU filtrado; en la tabla general se muestra el código corto del SKU (prefijo del id) junto al número de lote. La corrección de fondo es que el backend agregue `skuCodigo`/`skuNombre` a la posición en el read adapter; queda como cambio posterior, fuera de este slice.

## Pantallas

### `/inventario` — posiciones
- `PageHeader` con botón **Registrar ingreso** (ingreso con lote nuevo).
- `ListFilters`: establecimiento, almacén, búsqueda de SKU. Filtros y página se reflejan en la URL.
- `DataTable` paginado: SKU y lote, vencimiento, físico, reservado, disponible, badge de estado del lote y marca de no vendible, columna de acciones (ojo → detalle del lote; ajuste rápido con almacén, SKU y lote precargados) reutilizando el patrón de `AccionesFila`.
- Vencimiento próximo resaltado visualmente. El umbral es de 90 días y es **POR_VALIDAR** (no es regla de negocio confirmada); vive como constante única `UMBRAL_VENCIMIENTO_PROXIMO_DIAS` en `lib/vencimiento.ts`.
- Estados de carga, vacío y mensaje de error, igual que el resto de listados (`DataTable` no ofrece reintento).

### `/inventario/lotes/:loteId` — detalle de lote
- Muestra número de lote, SKU, vencimiento, estado, motivo y fecha del bloqueo, y si es vendible.
- **Bloquear:** visible solo si el estado es `HABILITADO` o `CUARENTENA` (`puedeBloquear(estado)`, espejo de `Lote.bloquear`). Diálogo con motivo obligatorio (máx. 1000 caracteres).
- **Desbloquear:** visible solo si el estado es `BLOQUEADO` (`puedeDesbloquear(estado)`, espejo de `Lote.desbloquear`). Diálogo de confirmación, mismo patrón que `CambiarEstadoDialog`. El backend lo rechaza si el lote está vencido (`LOTE_NO_HABILITABLE`); ese error se muestra en el diálogo, sin duplicar la regla en el cliente.
- En `INMOVILIZADO_RECALL`, `VENCIDO` y `BAJA_DESTRUIDO` no se muestra ninguna de las dos acciones.

### Diálogo de movimiento
Un mismo diálogo con dos modos, validado con react-hook-form + zod; cantidad positiva y motivo obligatorio (máx. 1000).
- **Desde una fila de la tabla:** lote, almacén y SKU vienen precargados. Se elige el tipo (Ingreso o Salida, con etiquetas en español; `AJUSTE_INGRESO` | `AJUSTE_SALIDA`), la cantidad y el motivo.
- **Desde la cabecera, botón Registrar ingreso:** registra un ingreso con un lote NUEVO. Campos: almacén, SKU (con búsqueda), número de lote, vencimiento, cantidad y motivo; el tipo queda fijo en `AJUSTE_INGRESO`.
- El backend no expone un listado de lotes, por lo que seleccionar un lote existente solo es posible desde una fila.
- Al guardar invalida las queries de posiciones y del lote afectado. Los errores del backend (stock insuficiente, lote que no admite ingreso) se muestran dentro del diálogo con `describe-api-error`.

## Permisos
Los botones de escritura corresponden a `inventario.lotes.bloquear` e `inventario.movimientos.registrar`. Si el frontend no cuenta con un mecanismo establecido para condicionar la UI por permiso, este slice **no lo introduce**: los botones quedan visibles y el backend responde 403, que se muestra como error. Se verifica al inicio de la implementación si ya existe ese mecanismo.

## Manejo de errores
Todo error de API se traduce con `describe-api-error` (ProblemDetail RFC 9457). Sin `null` silenciosos: las queries muestran un mensaje de error, igual que el resto de listados; las mutaciones muestran el mensaje en el diálogo sin cerrarlo.

## Estrategia de pruebas
Gate de la repo: cada archivo nuevo debe tener 100% de líneas y ramas (umbrales por archivo de Vitest).

- **`api/*.test.ts`:** URL, método y cuerpo con un `ApiClient` falso; parámetros opcionales ausentes no viajan en la query; `queryKey` estables.
- **Schemas:** cantidad positiva, motivo no vacío y ≤1000, lote existente o número de lote con vencimiento; bloqueo exige motivo.
- **`lib/`:** tablas de casos que cubren todos los valores de `EstadoLote` (`puedeBloquear`, `puedeDesbloquear`), resaltado de vencimiento y payloads.
- **Componentes y páginas (Testing Library):** carga, vacío, error con reintento, paginación, filtros que actualizan la URL, validaciones de diálogos, envío correcto, invalidación de queries, error del backend en el diálogo, acciones Bloquear/Desbloquear según estado (incluido el caso sin ninguna acción).
- **Rutas:** `routes.test.ts` y `feature-routes.test.ts` actualizados.
- **E2E Playwright** (desktop/tablet/móvil, Vite en modo `http`, API simulada con `page.route`): filtrar posiciones, registrar un ingreso con lote nuevo y un ajuste, bloquear un lote desde el detalle.
- **Verificación real:** prueba manual en navegador contra el backend local (login real, ajuste de ingreso, bloqueo), como se hizo con `organizacion`.
- **Cierre:** `pnpm check` (lint, typecheck, test, build) en verde.

## Fuera de alcance / pendientes
- Campos `skuCodigo`/`skuNombre` en `PosicionResponse` (backend).
- Listado de lotes e historial de movimientos (requieren endpoints nuevos).
- Umbral de "vencimiento próximo" por política de negocio (POR_VALIDAR).
- Condicionado de UI por permisos, si no existe mecanismo previo.
