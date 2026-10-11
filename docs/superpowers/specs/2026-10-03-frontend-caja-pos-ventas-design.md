# Frontend de caja, POS e historial de ventas

Fecha: 2026-10-03
Contexto: el backend de turno de caja y venta en efectivo esta completo (`/api/v1/ventas/turnos`, `/api/v1/ventas/ventas`). `features/caja`, `features/pos` y `features/ventas` son placeholders (`ModulePlaceholderPage`). La anulacion de venta existe en backend pero su UI queda fuera de este slice.

## Alcance

Dentro: pantallas de Caja (turno), POS (venta en efectivo) y Ventas (historial y detalle); cambio minimo de backend para un precio de referencia en el SKU.
Fuera: UI de anulacion y devolucion, CPE y comprobantes fiscales, promociones, clientes, otros medios de pago, modo offline, motor de precios (RF-POS-005).

## Backend: precio de referencia en `catalogo`

- Migracion `V037`: `sch_catalogo.sku_comercial.precio_venta_referencia NUMERIC(18,4)` nullable con `CHECK (precio_venta_referencia IS NULL OR precio_venta_referencia >= 0)`.
- El campo se agrega al dominio, request y response del CRUD de SKU y al resumen del listado, con tests (cobertura 100% en archivos nuevos). Es un dato de referencia: `ventas` sigue recibiendo el precio enviado por el POS.

## Frontend: features (cada una publica solo por `index.ts`)

### caja
- API: query del turno actual por terminal (`GET /ventas/turnos/actual?terminalId=`), query por id, mutaciones abrir (`POST /ventas/turnos`) y cerrar (`POST /ventas/turnos/{id}/cierre`).
- Pagina `/caja`: sin turno abierto, formulario de apertura (terminal y fondo inicial); con turno abierto, resumen (fondo, apertura, cajero, total del sistema) y cierre con total declarado y observacion mostrando la diferencia; tras cerrar se muestra el resultado (total sistema, declarado, diferencia).
- Otras features consumen la query de turno actual solo por `index.ts`.

### ventas
- API: registrar (`POST /ventas/ventas` con `Idempotency-Key`), obtener y listar (`GET /ventas/ventas`, filtros `establecimientoId`, `desde`, `hasta`, paginacion). Tipos de venta, linea, lote consumido, pago y pagina.
- Pagina `/ventas`: historial paginado con filtros de fecha y local; detalle `/ventas/:ventaId` con lineas, lotes consumidos y pago.
- Componente de comprobante interno imprimible (no fiscal), reutilizado por el POS.

### pos
- Pagina `/pos`: seleccion de terminal y almacen de trabajo, recordada en el navegador (localStorage con respaldo si falla).
- Busqueda y escaneo de productos por texto o codigo de barras con la query de SKU existente; cada resultado muestra precio de referencia y stock disponible del almacen (posiciones de inventario).
- Carrito: cantidad, precio editable y total en vivo; si el SKU no trae precio, el cajero lo digita. Reglas espejo del backend: cantidad > 0 con hasta 4 decimales, fraccion solo si el SKU permite fraccion, precio >= 0 con hasta 4 decimales, maximo 100 lineas.
- Cobro en efectivo: monto recibido y vuelto en vivo; confirmar envia la venta con `Idempotency-Key` estable por contenido del carrito (mismo patron que ajustes de inventario) y muestra el comprobante interno con opcion de imprimir.
- Sin turno abierto en la terminal, el cobro se bloquea y se enlaza a Caja.

## Reuso y arquitectura

- La clave de idempotencia y su hook hoy viven en `features/inventario/lib`; pasan a `shared/lib` porque ahora los usan dos features (inventario y pos), sin duplicar.
- Terminales, almacenes y estructura corporativa se consumen desde `features/organizacion`; el selector de SKU y `skusQuery` desde `features/catalogo`; posiciones desde `features/inventario`; todo solo por `index.ts`.
- `app/feature-routes.ts` y su test componen las rutas nuevas; rutas lazy como el resto.
- Sin comentarios, sin duplicacion, `forwardRef` y React Router 8 bloqueados por ESLint; Tailwind y componentes de `packages/ui-web`.

## Errores y estados

- Los `ProblemDetail` del backend se traducen: `INV_STOCK_INSUFICIENTE` (linea sin stock), `VEN_TURNO_NO_ABIERTO` (enlace a Caja), `VEN_IDEMPOTENCY_CONFLICT`, `VEN_MONTO_RECIBIDO_INSUFICIENTE`, `VEN_TERMINAL_NO_OPERABLE`, `VEN_ALMACEN_NO_OPERABLE`; resto con mensaje generico y el codigo.
- Estados de carga, vacio y error en cada pagina; formularios con react-hook-form y zod como el resto.

## Pruebas

- Vitest + Testing Library con umbral de 100% de lineas y ramas por archivo nuevo: API, logica pura de carrito y totales, componentes y paginas.
- E2E de Playwright con la API simulada (`page.route`): abrir caja, vender (con vuelto) y cerrar, en desktop, tablet y movil.
- Verificacion manual en navegador contra el backend real (alta de turno, venta con FEFO, cierre con diferencia).
- `pnpm check` (lint, typecheck, test, build) en verde antes de dar por terminado.

## Planes de implementacion (por entregable)

1. Backend: precio de referencia en `catalogo` (V037).
2. Frontend base compartida y `caja` (mover idempotencia a `shared`, API y paginas de turno).
3. Frontend `ventas`: API, historial, detalle y comprobante imprimible.
4. Frontend `pos`: busqueda, carrito, cobro y comprobante.

## Supuestos POR_VALIDAR

Precio de referencia como dato del SKU; comprobante interno no fiscal como cierre de la venta; terminal y almacen de trabajo recordados por navegador.
