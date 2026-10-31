# Frontend de compras: proveedores, ordenes de compra y recepciones

Fecha: 2026-10-06
Contexto: el backend de compras esta implementado (`/api/v1/compras/proveedores`, `/api/v1/compras/ordenes`, `/api/v1/compras/recepciones`) con permisos sembrados en `V032` (`compras.proveedores.consultar|gestionar`, `compras.ordenes.consultar|crear|aprobar|anular`, `compras.recepciones.consultar|registrar`). `features/compras` es un placeholder (`PurchasesPage` con `ModulePlaceholderPage`). La recepcion ya ingresa stock a inventario (lote, posicion y movimiento) por el backend.

## Alcance

Dentro: proveedores (listar, alta, edicion, cambio de estado), ordenes de compra (listar, detalle, crear, aprobar, emitir, anular), recepciones (registrar contra una orden y ver las recepciones de cada orden) y el endpoint de backend que lista recepciones por orden.
Fuera: edicion de una orden ya creada (el backend no la ofrece: se anula y se vuelve a crear), devolucion a proveedor, facturas y cuentas por pagar (`sch_finanzas`), busqueda incremental de proveedor, UI de transferencias entre almacenes.

## Contrato del backend que el frontend respeta

- Proveedor: `POST` y `PUT /proveedores`, `PATCH /proveedores/{id}/estado` (ACTIVO, SUSPENDIDO, BLOQUEADO; solo ACTIVO admite compras), `GET /proveedores?estado&texto&page&size`, `GET /proveedores/{id}`. Campos: documento, razon social, nombre comercial, direccion, ubigeo, contacto, condicion y dias de credito, moneda por defecto, banderas laboratorio/importador/distribuidor, calificacion.
- Orden: `POST /ordenes` (nace en BORRADOR), `GET /ordenes?proveedorId&estado&page&size` (resumen con razon social del proveedor), `GET /ordenes/{id}` (lineas con `cantidadRecibida` y `cantidadPendiente`), `POST /ordenes/{id}/aprobacion`, `/emision`, `/anulacion` (motivo obligatorio). Estados: BORRADOR, EN_APROBACION, APROBADA, EMITIDA, PARCIALMENTE_RECIBIDA, RECIBIDA, CANCELADA, CERRADA. Aprobable: BORRADOR o EN_APROBACION. Emitible: APROBADA. Anulable: antes de recibir y sin recepciones. Recepcionable: EMITIDA o PARCIALMENTE_RECIBIDA.
- Lineas de orden: `descuento` e `impuesto` son montos absolutos; `totalLinea = precio x cantidad - descuento + impuesto`; tolerancias de exceso y defecto en porcentaje.
- Recepcion: `POST /recepciones` con `Idempotency-Key` opcional, `GET /recepciones/{id}`. Cada item referencia `numeroLineaOrden`, `numeroLote`, `fechaVencimiento` obligatoria, `cantidadRecibida`, `cantidadRechazada` con motivo y `costoUnitario`.
- Errores `COM_*`: `COM_PROVEEDOR_INVALIDO`, `COM_ORDEN_INVALIDA`, `COM_ORDEN_ESTADO_INVALIDO`, `COM_ORDEN_MOTIVO_INVALIDO`, `COM_RECEPCION_INVALIDA`, `COM_RECEPCION_ORDEN_NO_RECEPCIONABLE`, `COM_RECEPCION_LINEA_NO_ENCONTRADA`, `COM_RECEPCION_EXCEDE_PENDIENTE`.

## Backend: listado de recepciones por orden

- `GET /api/v1/compras/recepciones?ordenCompraId=<uuid>&page&size`: `ordenCompraId` es obligatorio; responde `PaginaResponse<RecepcionResponse>` con lineas y lote, ordenado por fecha de recepcion descendente. Permiso `compras.recepciones.consultar`.
- Capas: query y handler en `application`, metodo en `ComprasReadPort`, implementacion JDBC en el adaptador de lectura, endpoint en `RecepcionController`. Sin migracion nueva.
- Pruebas: unitarias del handler y test de integracion (`ComprasApiIntegrationTest`) que registra dos recepciones sobre una orden y verifica el listado, el orden, la paginacion, el parametro obligatorio y el permiso. Cobertura 100% en archivos nuevos.

## Frontend: `features/compras` (publica solo por `index.ts`)

Estructura interna: `api/` (tipos y queries por recurso, `invalidate.ts`), `lib/` (logica pura: IGV, calculo de totales de linea y de orden, traduccion de errores, tonos de estado), `schemas/` (zod), `components/` y `pages/`.

### Navegacion (patron de Seguridad: hub y subpantallas)
- `/compras`: hub con accesos a Ordenes de compra y Proveedores.
- `/compras/proveedores`, `/compras/proveedores/nuevo`, `/compras/proveedores/:proveedorId`.
- `/compras/ordenes`, `/compras/ordenes/nueva`, `/compras/ordenes/:ordenId`, `/compras/ordenes/:ordenId/recepcion`.
- Rutas con `lazy` y `compras/routes.test.ts` como en caja y ventas.

### Proveedores
- Listado con filtros de estado y texto en la URL, paginacion y `EstadoBadge`.
- Alta y edicion en una pagina con react-hook-form y zod que espeja los limites del backend (longitudes, `diasCreditoDefault >= 0`, moneda de 3 letras); el detalle permite cambiar el estado con dialogo de confirmacion (mismo patron de `organizacion`).

### Ordenes de compra
- Listado con filtros de proveedor y estado en la URL, paginacion, total formateado por moneda y enlace al detalle.
- Crear orden: proveedor (lista de proveedores ACTIVOS, hasta 100), establecimiento destino (`useEstablecimientos`), fecha de entrega estimada, moneda, condicion de pago, dias de credito, observacion y un editor de lineas. Cada linea: busqueda de SKU con `skusQuery` del catalogo, cantidad, precio unitario, descuento, impuesto y tolerancias opcionales; la unidad de la linea es la de venta del SKU. Maximo 200 lineas. Los totales (subtotal, descuento, impuesto, total) se recalculan en vivo con redondeo a dos decimales.
- IGV sugerido: al agregar o cambiar una linea cuyo SKU es afecto a IGV, el impuesto se propone como 18% de (precio x cantidad - descuento) y el usuario puede corregirlo; si lo edita a mano, deja de recalcularse para esa linea. La tasa vive en `lib/igv.ts` y la pantalla rotula el valor como sugerido. Es una decision POR_VALIDAR (ver Supuestos).
- Detalle: estado, proveedor, destino, totales, lineas con recibido y pendiente, y las recepciones de la orden (consulta por `ordenCompraId`). Acciones segun estado: Aprobar, Emitir, Anular (dialogo con motivo obligatorio) y Registrar recepcion. Cada accion invalida las consultas de ordenes y muestra el error del backend en el dialogo.

### Recepcion
- Pagina por orden recepcionable. Cabecera: almacen (de los almacenes del establecimiento destino), documento del proveedor (tipo, serie, numero), guias de remision, temperatura y humedad, observacion. Un bloque por linea con pendiente: numero de lote, fecha de fabricacion opcional, fecha de vencimiento, cantidad recibida, cantidad rechazada con motivo, costo unitario (por defecto el precio de la orden). Solo se envian las lineas con cantidad recibida mayor que cero.
- Validacion espejo: cantidad recibida positiva, rechazada no negativa y menor o igual a la recibida, motivo obligatorio si hay rechazo, vencimiento obligatorio y posterior a la fabricacion.
- `Idempotency-Key` estable por contenido con `useClaveIdempotencia`; se reinicia con `reiniciar()` tras registrar con exito para no reutilizar la clave en la siguiente recepcion de contenido identico.
- Tras registrar, se invalidan ordenes, recepciones e `inventario` (con `invalidateInventario` publicado por su `index.ts`) y se vuelve al detalle de la orden.

## Reuso y arquitectura

- Consume otras features solo por `index.ts`: `catalogo` (`skusQuery`), `organizacion` (`useEstablecimientos`), `inventario` (`invalidateInventario`). `shared/` aporta formularios, `formatoMoneda`, `describeApiError`, `useClaveIdempotencia`, paginacion y filtros en la URL.
- Sin duplicar: los helpers de lista paginada y de dialogo de confirmacion ya existentes se reutilizan; si compras necesita uno nuevo de uso comun se extrae a `shared/`, no a un paquete.
- `ModulePlaceholderPage` deja de usarse en compras; `compras/index.ts` exporta `purchasesRoutes`.

## Errores y estados

- Cada pantalla distingue cargando, error y vacio con los mensajes del repo. Los `COM_*` se traducen a mensajes de usuario en `lib/errores-compras.ts`; un codigo desconocido muestra el detalle y el codigo.
- 403 sin permiso se muestra como error de la API en la pantalla y en los dialogos (no se oculta navegacion por permiso, igual que el resto del frontend).

## Pruebas

- Logica pura (`lib/`) con 100% de cobertura de lineas y ramas; componentes y paginas con Vitest, Testing Library y MSW; cobertura 100% en todo archivo nuevo y sin agregar archivos a `coverage-baseline.txt`.
- E2E Playwright simulado con `page.route` (desktop, tablet, movil): crear proveedor, crear orden, aprobar, emitir y registrar una recepcion parcial; sin desborde horizontal.
- Verificacion manual contra el backend real al cerrar el ultimo plan: crear proveedor y orden, aprobar, emitir, recibir con lote y vencimiento, comprobar que el stock sube en Inventario y que la orden pasa a PARCIALMENTE_RECIBIDA o RECIBIDA.

## Planes de implementacion (por entregable)

1. Backend: listado de recepciones por orden.
2. Base y proveedores: tipos, API, `lib/` base, hub, listado, alta, edicion y estado.
3. Ordenes de compra: listado, detalle y acciones (aprobar, emitir, anular).
4. Crear orden: editor de lineas, SKU, IGV sugerido y totales.
5. Recepcion: formulario, idempotencia, recepciones en el detalle, e2e y verificacion manual.

## Supuestos POR_VALIDAR

- Tasa de IGV 18% sugerida en lineas afectas: regla de negocio sin validar en el proyecto (ventas mantiene el IGV en 0); queda aislada en `lib/igv.ts` y editable por linea.
- Sin edicion de ordenes: el flujo corrector es anular y recrear, porque el backend no expone `PUT` de orden.
- La unidad de la linea es la unidad de venta del SKU; si compras exige una unidad de compra distinta, se agrega un selector de unidades cuando el catalogo de unidades tenga UI.
- Lista de proveedores activos limitada a 100 en el selector de la orden.
