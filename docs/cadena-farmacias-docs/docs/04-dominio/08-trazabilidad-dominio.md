# DOM-FAR-008 — Trazabilidad RF/CU/CA → Dominio

**Versión:** 0.1

## 1. Propósito

Conectar la especificación funcional con el modelo de dominio para que ningún agregado exista únicamente “porque parece buena idea”.

## 2. Trazabilidad principal

| Proceso / necesidad | RF relevantes | CU | Agregado(s) principal(es) | Servicio/política |
|---|---|---|---|---|
| Habilitar local y responsables | `RF-ORG-*` | `CU-ORG-001` | `EstablecimientoFarmaceutico`, `AsignacionProfesional` | `EvaluarCompetenciaDispensador` |
| Maestro de producto | `RF-CAT-*` | `CU-CAT-001` | `ProductoRegulado`, `SKUComercial` | `ResolverEstadoRegulatorioProducto` |
| Compra | `RF-COM-*` | `CU-COM-001` | `SolicitudCompra`, `OrdenCompra` | políticas de aprobación |
| Recepción | `RF-COM-009..013`, `RF-INV-001..005` | `CU-COM-002` | `RecepcionCompra`, `Lote`, `PosicionInventario` | `ConciliarOcRecepcionFactura` |
| Inventario/lotes | `RF-INV-*` | `CU-INV-001` | `Lote`, `PosicionInventario`, `ReservaInventario`, `ConteoInventario` | `SeleccionarLotesParaSalida`, `EvaluarVendibilidad` |
| Bloqueo/inmovilización | `RF-INV-010..012` | `CU-INV-002` | `Lote`, `PosicionInventario` | `EvaluarVendibilidad` |
| Transferencia | `RF-TRF-*` | `CU-TRF-001` | `TransferenciaInventario`, `ReservaInventario` | política de abastecimiento |
| Precio/promoción | `RF-PRE-*` | `CU-PRE-001` | `ListaPrecio`, `Promocion` | `ResolverPrecioVenta` |
| Venta POS | `RF-POS-*` | `CU-POS-001` | `TurnoCaja`, `Venta` | `EvaluarVendibilidad`, `ResolverPrecioVenta` |
| CPE | `RF-POS-009..`, `RF-INT-*` | `CU-POS-002` | `ComprobanteElectronico`, `NotaCreditoElectronica` | ACL SUNAT/PSE |
| Cierre caja | `RF-POS-*` | `CU-POS-003` | `TurnoCaja` | conciliación de caja |
| Prescripción | `RF-DSP-*` | `CU-DSP-001` | `Prescripcion` | `ValidarPrescripcionPolicy` |
| Dispensación | `RF-DSP-*` | `CU-DSP-002` | `Dispensacion` | `EvaluarCompetenciaDispensador` |
| Controlados | `RF-CTL-*` | `CU-CTL-001/002` | `RecetaControlada`, `RegistroMovimientoControlado`, `BalanceControlado` | `ValidarRecetaControladaPolicy` |
| Devolución | `RF-DEV-*` | `CU-DEV-001` | `DevolucionComercial`, `NotaCreditoElectronica` | `ResolverDisposicionDevolucion` |
| Recall | `RF-RCL-*` | `CU-RCL-001` | `CasoRecall` | `ResolverImpactoRecall` |
| Farmacovigilancia | `RF-FVG-*` | `CU-FVG-001` | `ReporteSeguridad` | política privacidad/notificación |
| Posting ERP | `RF-ERP-*` | `CU-ERP-001` | `PostingRetail`, `CuentaPorPagar` | `ConstruirPostingRetail` |
| Observatorio | `RF-OBS-*` | `CU-OBS-001` | `ReporteMensualPrecios` | `PrepararReporteObservatorio` |
| Autorización | `RF-SEC-*` | `CU-SEC-001` | futuro `BC-IAM` | políticas RBAC/ámbito/competencia |
| Auditoría | `RF-AUD-*` | `CU-AUD-001` | futuro `BC-AUD` | política append-only |

## 3. Ejemplo de trazabilidad completa — venta bajo receta

```text
BPM-FAR-004
  ↓
RN-DSP-* / RN-SEC-005
  ↓
RF-DSP-001 / RF-DSP-016 / RF-POS-002
  ↓
CU-DSP-001 → CU-DSP-002 → CU-POS-001
  ↓
CA-RF-DSP-* / CA-RF-POS-*
  ↓
Prescripcion
  ↓
ValidarPrescripcionPolicy
  ↓
Dispensacion
  ↓
DispensacionAutorizada
  ↓
Venta
  ↓
ComprobanteElectronico
  ↓
TST-DSP-* / TST-POS-*
```

## 4. Ejemplo — recall

```text
Alerta / Retiro DIGEMID
    ↓
RN-RCL-001..005
    ↓
RF-RCL-*
    ↓
CU-RCL-001
    ↓
CasoRecall
    ↓
ResolverImpactoRecall
    ↓
BloquearLoteCommand
    ↓
LoteBloqueado
    ↓
Inventario no vendible
    ↓
pruebas POS + inventario + conciliación
```

## 5. Ejemplo — devolución

```text
Venta original
   ↓
CU-DEV-001
   ↓
DevolucionComercial
   ├── reembolso
   ├── NotaCredito (BC-FIS)
   └── recepción física (BC-INV)
                     ↓
          ResolverDisposicionDevolucion
                     ↓
     NO reingreso automático a vendible
```

## 6. Regla de evolución

Cuando se agregue una nueva tabla/API en fases posteriores deberá existir, según aplique:

```text
Fuente/Proceso
 → RN
 → RF
 → CU
 → CA
 → concepto/agregado
 → contrato API
 → persistencia
 → prueba
```

Si no se puede justificar el concepto por esta cadena, debe revisarse antes de implementarse.
