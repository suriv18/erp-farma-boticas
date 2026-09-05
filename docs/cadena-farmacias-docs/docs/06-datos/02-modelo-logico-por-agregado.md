# DAT-FAR-002 — Modelo Lógico por Agregado

**Versión:** 0.1  
**Estado:** lógico; no implica tablas 1:1.

## 1. BC-ORG

### `EmpresaOperadora`

Datos lógicos candidatos:

- identidad de empresa;
- identificadores fiscales/corporativos aplicables;
- razón social/nombre comercial;
- vigencia y estado.

### `EstablecimientoFarmaceutico`

- empresa operadora;
- tipo/categoría;
- identidad y dirección;
- autorización sanitaria y período de vigencia;
- estado operativo.

### `AsignacionProfesional`

- establecimiento;
- profesional/persona;
- función profesional;
- período de asignación;
- estado/evidencia.

## 2. BC-CAT

### `ProductoRegulado`

- registro sanitario;
- denominación;
- principio(s) activo(s)/composición;
- concentración;
- forma farmacéutica;
- vía;
- unidad;
- condición de venta;
- clasificación ATC cuando corresponda;
- clasificación controlada cuando corresponda;
- titular/fabricante;
- vigencia regulatoria;
- fuente/versionado.

### `SKUComercial`

- producto regulado;
- código interno;
- presentación comercial;
- códigos de barra y vigencia;
- estado comercial.

## 3. BC-PRC

`Proveedor`, `SolicitudCompra`, `OrdenCompra` y `RecepcionCompra` se mantienen como agregados distintos. La recepción no es la factura del proveedor y puede registrar diferencias contra la orden.

## 4. BC-INV

### `Lote`

- SKU/producto;
- número de lote;
- vencimiento;
- origen/fabricante cuando aplique;
- estado operativo/sanitario.

### `PosicionInventario`

Clave lógica candidata:

```text
Establecimiento
+ Almacén
+ Ubicación
+ SKU
+ Lote
+ Estado de inventario
```

Datos:

- cantidad física;
- cantidad reservada;
- cantidad disponible derivada o calculable;
- versión/concurrencia.

### `MovimientoInventario`

Ledger append-only candidato:

- operación;
- cantidad;
- scope origen/destino;
- documento causal;
- actor;
- fecha de negocio;
- correlation/message id.

### `TransferenciaInventario`

Mantiene solicitado, despachado y recibido como hechos distintos; las diferencias no sobrescriben el despacho.

## 5. BC-PRI

`ListaPrecio` y `Promocion` contienen vigencia y ámbito. La venta conserva un `PriceDecisionSnapshot` para reproducir el precio cobrado.

## 6. BC-RET

### `TurnoCaja`

- establecimiento/terminal/cajero;
- apertura;
- fondo inicial;
- movimientos de caja;
- totales por medio de pago;
- arqueo;
- cierre/diferencia.

### `Venta`

- tienda/terminal/turno;
- fecha/hora;
- líneas;
- precio resuelto;
- descuentos/promociones;
- pagos;
- referencias de dispensación cuando apliquen;
- estado y correlación offline/central.

## 7. BC-DSP

### `Prescripcion`

- paciente cuando sea requerido/obtenido legítimamente;
- prescriptor/datos mínimos;
- fecha;
- productos/indicaciones;
- validez/uso según regla aplicable;
- evidencia/documento.

### `Dispensacion`

- prescripción;
- establecimiento;
- profesional competente;
- productos/cantidades autorizados/entregados;
- fecha;
- resultado/observación.

## 8. BC-CTL

Se separan `RecetaControlada`, `RegistroMovimientoControlado` y `BalanceControlado` para no imponer una única regla a todos los productos controlados.

## 9. BC-FIS

### `ComprobanteElectronico`

- venta/documento origen;
- tipo fiscal;
- serie/número según mecanismo;
- fecha emisión;
- payload fiscal interno/versionado;
- estado de envío;
- respuesta/código externo traducido;
- evidencia.

### `NotaCreditoElectronica`

Referencia comprobante previo y motivo fiscal; no altera por sí misma el stock.

## 10. BC-RCL

`CasoRecall` mantiene alcance por producto/RS/lote, establecimientos afectados, acciones e inventario conciliado.

## 11. BC-FVG

`ReporteSeguridad` admite `VentaId` opcional. Debe poder registrarse aunque el producto se haya adquirido fuera de la cadena.

## 12. BC-FIN

`PostingRetail` utiliza clave idempotente, origen y estado. El plan contable y entidades contables completas dependen de si el ERP es propio o externo.

## 13. BC-IAM / BC-AUD

Se documentan en [Seguridad](../08-seguridad/README.md). No se diseñarán credenciales dentro de tablas del dominio Retail.
