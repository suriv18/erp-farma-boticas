# DOM-FAR-003 — Agregados, Entidades y Value Objects

**Versión:** 0.1  
**Estado:** Candidatos tácticos DDD; no son todavía tablas físicas.

## 1. Criterio

Un agregado representa un límite de consistencia. Las referencias entre agregados se realizan preferentemente mediante IDs tipados y no mediante grafos de objetos gigantes. [REF-36]

## 2. BC-ORG — Organización y Cumplimiento

### 2.1. `EmpresaOperadora` — Aggregate Root

Responsabilidades:

- identidad de la empresa/operador de cadena;
- vigencia corporativa dentro del sistema;
- asociación con establecimientos.

No contiene inventario, ventas ni personal clínico completo.

### 2.2. `EstablecimientoFarmaceutico` — Aggregate Root

Atributos/conceptos:

- `EstablecimientoId`;
- categoría/tipo;
- identidad y dirección;
- autorización sanitaria y vigencia;
- estado operativo;
- empresa operadora.

Invariantes candidatas:

- un establecimiento inactivo/suspendido no inicia nuevas operaciones reguladas;
- la autorización sanitaria se conserva históricamente y no se sobrescribe destructivamente.

DIGEMID señala que los establecimientos que realizan actividades farmacéuticas requieren autorización sanitaria previa. [REF-02]

### 2.3. `AsignacionProfesional` — Aggregate Root candidato

Representa período de responsabilidad/asignación de:

- Director Técnico;
- QF asistente;
- personal técnico u otros perfiles autorizados.

Se separa de `EstablecimientoFarmaceutico` para evitar un agregado creciente con todo el historial de personal.

## 3. BC-CAT — Catálogo Farmacéutico Regulatorio

### 3.1. `ProductoRegulado` — Aggregate Root

Identidad conceptual regulatoria.

Value Objects/atributos candidatos:

- `RegistroSanitario`;
- `DenominacionProducto`;
- `PrincipioActivo` / composición estructurada;
- `Concentracion`;
- `FormaFarmaceutica`;
- `ViaAdministracion`;
- `CondicionVenta`;
- `ClasificacionATC` cuando aplique;
- `ClasificacionControlada` cuando aplique;
- titular/fabricante;
- período de vigencia regulatoria;
- fuente/versión de evidencia.

DIGEMID mantiene estándares explícitos para forma farmacéutica, vía, unidad, condición de venta, ATC y productos/sustancias controladas. [REF-06]

### 3.2. `SKUComercial` — Aggregate Root

Representa la unidad/presentación comercial que usa retail.

Conceptos:

- `SkuId`;
- `ProductoReguladoId`;
- código interno;
- descripción comercial;
- presentación;
- códigos de barra con vigencia;
- estado comercial.

Invariante:

> un SKU no puede cambiar silenciosamente de `ProductoReguladoId` después de tener movimientos históricos.

### 3.3. `ProductoRegulatorioSnapshot` — Value Object

Se congela en operaciones donde importa la regla vigente:

```text
registro sanitario
condición de venta
clasificación controlada
forma/concentración relevante
versión/fuente
fecha de referencia
```

Evita que una venta histórica cambie de interpretación porque el catálogo actualizó su condición después.

## 4. BC-PRC — Compras y Abastecimiento

### 4.1. `Proveedor` — Aggregate Root

Datos comerciales/fiscales y habilitación aplicable. No se asumirá un conjunto sanitario universal para todo proveedor hasta validar tipo de suministro.

### 4.2. `SolicitudCompra` — Aggregate Root

Entidades hijas:

- `LineaSolicitudCompra`.

Comportamientos:

- agregar/quitar línea;
- enviar aprobación;
- aprobar/rechazar;
- cancelar antes de compromiso permitido.

### 4.3. `OrdenCompra` — Aggregate Root

Entidades:

- `LineaOrdenCompra`;
- historial/versiones autorizadas.

Value Objects:

- `Money`;
- `CantidadCompra`;
- `CondicionComercial`;
- `PeriodoEntrega`.

### 4.4. `RecepcionCompra` — Aggregate Root

Representa el acto físico/operativo de recibir mercadería, independiente de la factura del proveedor.

Entidades:

- `LineaRecepcion`;
- `DiferenciaRecepcion`.

Puede originar registro de lotes y movimientos de entrada en `BC-INV`.

## 5. BC-INV — Inventario y Trazabilidad

### 5.1. `Lote` — Aggregate Root

Conceptos:

- `LoteId`;
- `SkuId` / `ProductoReguladoId`;
- `NumeroLote`;
- `FechaVencimiento`;
- fabricante/origen cuando corresponda;
- condición operativa/sanitaria;
- evidencias.

`FechaVencimiento` es dato; `vencido` es condición derivada.

### 5.2. `PosicionInventario` — Aggregate Root candidato

Clave de negocio conceptual:

```text
Establecimiento + Almacén + Ubicación + SKU + Lote + EstadoInventario
```

Responsabilidades:

- cantidad física/lógica en ese scope;
- reservar;
- liberar reserva;
- consumir cantidad disponible;
- trasladar entre estados/ubicaciones mediante movimientos.

Value Objects:

- `InventoryScope`;
- `Quantity`;
- `InventoryState`.

### 5.3. `MovimientoInventario` — Entidad/ledger append-only

Cada cambio de cantidad debe dejar un movimiento con:

- tipo;
- cantidad;
- origen/destino;
- documento/causa;
- fecha;
- actor/correlación.

La estrategia de persistencia (ledger + balance, solo ledger, etc.) queda abierta.

### 5.4. `ReservaInventario` — Aggregate Root candidato

Evita doble asignación de stock para:

- venta;
- pedido;
- transferencia.

No se asume todavía cómo funcionará en modo offline.

### 5.5. `TransferenciaInventario` — Aggregate Root

Entidades:

- `LineaTransferencia`;
- `DespachoTransferencia`;
- `RecepcionTransferencia`.

Puede tener diferencias entre despachado y recibido sin editar destructivamente el despacho.

### 5.6. `ConteoInventario` — Aggregate Root

Cubre conteos cíclicos/generales y diferencias antes de generar ajustes.

## 6. BC-PRI — Precios y Promociones

### 6.1. `ListaPrecio` — Aggregate Root

- ámbito (cadena/empresa/local/canal);
- período de vigencia;
- líneas de precio;
- estado publicación.

### 6.2. `Promocion` — Aggregate Root

- condiciones;
- beneficio;
- alcance;
- período;
- prioridad/compatibilidad configurada.

No se incluirán automáticamente reglas de promociones de medicamentos bajo receta sin revisar restricciones legales/comerciales aplicables.

### 6.3. `CotizacionVenta` — Value Object/resultado de servicio

Contiene el precio resuelto y evidencia de las reglas utilizadas para una venta concreta.

## 7. BC-RET — Retail/POS y Caja

### 7.1. `TurnoCaja` — Aggregate Root

Responsabilidades:

- abrir con cajero/terminal;
- registrar entradas/salidas autorizadas;
- totalizar medios de pago;
- iniciar arqueo;
- cerrar;
- registrar diferencias.

### 7.2. `Venta` — Aggregate Root

Entidades:

- `LineaVenta`;
- `PagoVenta`;
- referencias a reservas/lotes asignados;
- referencia a `DispensacionId` cuando corresponda.

Value Objects:

- `Money`;
- `TaxBreakdown`;
- `PriceDecisionSnapshot`;
- `CustomerReference` opcional;
- `SaleChannel`.

Invariantes candidatas:

- la confirmación no consume más stock del reservado/confirmado;
- una línea que requiere dispensación debe estar respaldada por una decisión válida del contexto farmacéutico;
- el estado de CPE no forma parte del estado interno de la venta.

### 7.3. `DevolucionComercial` — Aggregate Root

Referencia una venta original sin reescribirla.

Entidades:

- líneas devueltas;
- decisión comercial de reembolso;
- referencia a nota de crédito cuando corresponda.

No decide por sí misma si el producto retorna a stock vendible. Esa decisión pertenece al flujo de inventario/calidad.

## 8. BC-DSP — Prescripción y Dispensación

### 8.1. `Prescripcion` — Aggregate Root

Conceptos candidatos:

- identificador/documento;
- prescriptor;
- paciente;
- fecha;
- líneas prescritas;
- evidencia documental;
- resultado de validación.

No se fijará un plazo universal de vigencia para toda receta.

### 8.2. `Dispensacion` — Aggregate Root

Entidades:

- `LineaDispensacion`.

Responsabilidades:

- evaluar prescripción aplicable;
- verificar competencia del dispensador;
- registrar producto/cantidad seleccionada;
- registrar entrega/información;
- conservar evidencia y vínculo con la venta.

El Manual de Buenas Prácticas de Dispensación distingue etapas de recepción/validación, análisis, preparación/selección, registros y entrega/información. [REF-25]

### 8.3. Value Objects

- `ProfesionalFarmaceuticoRef`;
- `PrescriptorRef`;
- `PacienteRef`;
- `PrescriptionValidityDecision`;
- `DispensingDecision`.

## 9. BC-CTL — Productos Controlados

### 9.1. `RecetaControlada` — Aggregate Root candidato

Extiende semánticamente la receta con:

- lista/clasificación aplicable;
- tipo de receta especial;
- datos reglamentarios específicos;
- retención/archivo;
- estado de validación.

No se hereda técnicamente de `Prescripcion`; la integración se hace por referencia/contrato para evitar acoplamiento entre contextos.

### 9.2. `RegistroMovimientoControlado` — Aggregate/ledger

Movimiento append-only por establecimiento/producto/lote/cantidad y documento origen.

### 9.3. `BalanceControlado` — Aggregate Root

Consolida período, existencias iniciales, entradas, salidas, ajustes y existencias finales para los sujetos/listas donde corresponda. [REF-26]

## 10. BC-FIS — Fiscal/CPE

### 10.1. `ComprobanteElectronico` — Aggregate Root

Conceptos:

- tipo (boleta/factura según corresponda);
- serie/número;
- venta origen;
- adquirente;
- importes;
- documento electrónico generado;
- envío;
- respuesta/estado fiscal.

SUNAT diferencia boleta, factura y notas y sus efectos tributarios. [REF-13][REF-27]

### 10.2. `NotaCreditoElectronica` — Aggregate Root

Referencia comprobante previo. La nota no altera destructivamente el comprobante ni la venta origen.

## 11. BC-RCL — Seguridad de Producto y Recall

### 11.1. `CasoRecall` — Aggregate Root

Entidades:

- `ProductoLoteAfectado`;
- `AfectacionEstablecimiento`;
- `AccionRecall`;
- `ConciliacionRecall`.

Responsabilidades:

- registrar fuente/motivo/alcance;
- identificar lotes;
- ordenar bloqueos;
- registrar inmovilización por local;
- reconciliar cantidades;
- cerrar con evidencia.

DIGEMID publica retiro de mercado con producto, RS, lote, motivo, fecha y situación. [REF-30]

## 12. BC-FVG — Farmacovigilancia/Tecnovigilancia

### 12.1. `ReporteSeguridad` — Aggregate Root

Conceptos:

- producto sospechoso;
- paciente (datos mínimos necesarios);
- reportante;
- descripción/evento;
- temporalidad;
- gravedad/campos aplicables;
- venta/lote opcionales;
- seguimiento;
- envíos externos;
- versiones.

NotiMED permite reporte por profesionales y público; por ello `VentaId` es opcional. [REF-33]

## 13. BC-FIN — ERP Financiero

El modelo financiero se mantiene deliberadamente parcial hasta profundizar contabilidad.

### Agregados candidatos actuales

- `CuentaPorPagar` — nace de factura de proveedor aceptada, no de recepción física.
- `PostingRetail` — lote idempotente de hechos retail a contabilidad.
- `ReversaPosting` — corrección trazable.

No se define todavía:

- plan contable completo;
- reglas tributarias contables exhaustivas;
- activos fijos;
- nómina;
- consolidación legal avanzada.

## 14. BC-OBS — Reporte Regulatorio de Precios

### `ReporteMensualPrecios` — Aggregate Root

- período;
- establecimientos incluidos;
- líneas de precio;
- fuente/versión del precio;
- estado de preparación/envío;
- evidencia del envío cuando exista.

El mecanismo de transmisión exacto permanece `POR_VALIDAR`.

## 15. Value Objects transversales candidatos

| Value Object | Semántica |
|---|---|
| `Money` | importe + moneda |
| `Quantity` | valor + unidad |
| `DateRange` | desde/hasta |
| `RegistroSanitario` | número + estado/vigencia/evidencia aplicable |
| `CondicionVenta` | código/denominación de fuente regulatoria |
| `NumeroLote` | identificador de lote |
| `FechaVencimiento` | fecha de expiración |
| `Barcode` | código de barras + tipo/vigencia |
| `Ruc` | identificador tributario validado |
| `CpeReference` | tipo/serie/número/emisor |
| `InventoryScope` | establecimiento/almacén/ubicación |
| `ProfessionalReference` | identidad profesional sin duplicar su agregado |
| `SourceEvidence` | fuente, versión, fecha y referencia de dato externo |

## 16. Prohibiciones de diseño

- `Producto` como aggregate gigante con stock, precios, ventas, recetas y compras.
- navegación JPA directa entre todos los contextos.
- `Venta` modificando tablas internas de SUNAT/ERP.
- `Receta` almacenada como simple URL sin metadatos/estado.
- `stock` como único número global por producto.
- `lote` solo como texto libre dentro de la venta.
