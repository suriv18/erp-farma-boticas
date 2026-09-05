# DOM-FAR-009 — Matriz de Origen de Conceptos de Dominio

**Versión:** 0.1  
**Objetivo:** distinguir evidencia normativa/externa de decisiones internas.

## 1. Leyenda

- `NORM`: fuente oficial sanitaria.
- `FISCAL`: fuente SUNAT/tributaria.
- `FUNC`: derivado de BPM/RF/CU.
- `MKT`: benchmarking de sistemas existentes.
- `DDD`: patrón de modelado, no obligación del negocio.
- `DOM`: decisión de dominio de la solución.
- `TEC`: decisión técnica futura.

## 2. Matriz

| Concepto | Origen | Fuente/razón | Nota |
|---|---|---|---|
| Establecimiento farmacéutico | `NORM` | DIGEMID / D.S. 014-2011-SA | Requiere autorización para actividades aplicables. |
| Director Técnico/QF | `NORM` | Reglamento y modificaciones vigentes | Responsabilidad profesional. |
| Producto regulado | `NORM/DOM` | DIGEMID RS/estándares | Nombre del aggregate es de diseño; atributos son regulatorios. |
| Registro sanitario | `NORM` | DIGEMID | Campo/concepto oficial. |
| Condición de venta | `NORM` | DIGEMID estándar | No inventar catálogo. |
| Forma farmacéutica | `NORM` | DIGEMID estándar | Usar codificación/fuente vigente. |
| Clasificación controlada | `NORM` | DIGEMID / D.S. 023-2001-SA | Reglas por lista. |
| SKU comercial | `FUNC/MKT/DOM` | Retail | No es entidad normativa. |
| Código de barras | `FUNC/MKT` | Retail | Puede haber más de uno por SKU. |
| Lote | `NORM/FUNC` | BPA / trazabilidad | Identidad necesaria según producto. |
| Fecha de vencimiento | `NORM` | BPA/DIGEMID | Producto vencido no comercializable. |
| Posición de inventario | `DOM` | Multi-almacén/multi-local | Forma de representar cantidad por scope. |
| Movimiento de inventario | `FUNC/DOM/AUD` | Kardex/trazabilidad | Append-only recomendado. |
| Reserva de inventario | `FUNC/MKT/DOM` | Concurrencia POS/transferencias | Diseño operacional. |
| FEFO | `MKT/CFG` | Benchmark + usos específicos | No universalizar. |
| Prescripción | `NORM` | Buenas prácticas / reglamento | Datos/reglas según fuente. |
| Dispensación | `NORM` | RM 013-2009/MINSA | Proceso sanitario. |
| Venta POS | `FUNC/MKT` | Retail | Proceso comercial. |
| Turno de caja | `FUNC/MKT` | Retail | Diseño operativo. |
| CPE | `FISCAL` | SUNAT | Separado de venta. |
| Nota de crédito | `FISCAL` | SUNAT | Relacionada a comprobante previo. |
| Devolución comercial | `FUNC/DOM` | BPM-FAR-008 | No equivale a disposición sanitaria. |
| Caso Recall | `NORM/DOM` | DIGEMID retiros | Aggregate interno modela el proceso. |
| Lote afectado por recall | `NORM` | DIGEMID retiro mercado | Criterio publicado. |
| Reporte farmacovigilancia | `NORM/DOM` | DIGEMID NotiMED | Modelo interno conserva seguimiento. |
| Venta opcional en FVG | `NORM/DOM` | NotiMED admite reporte independiente | No exigir ticket. |
| Posting ERP | `ERP/DOM` | BPM/benchmark | Término de integración financiera. |
| Three-way match | `ERP/MKT/CFG` | Benchmark ERP | No obligación sanitaria. |
| Bounded Context | `DDD` | DDD | No es módulo normativo. |
| Aggregate Root | `DDD` | DDD | No equivale a tabla. |
| Domain Event | `DDD/TEC` | DDD | Transporte se define después. |
| UUID/PK/índices | `TEC` | arquitectura/datos | No definidos en esta fase. |

## 3. Fuentes que obligan a versionar datos externos

Se recomienda conservar fuente/fecha/versión para:

- condición de venta;
- estado/vigencia de registro sanitario;
- clasificación controlada;
- catálogo de estándares DIGEMID;
- alertas/retiros;
- reglas/estado CPE externo;
- reporte de precios.

Esto es una decisión `DOM/AUD` para poder demostrar qué información utilizó el sistema en una operación histórica.

## 4. Fuentes con alcance limitado

La RM 132-2015/MINSA de Buenas Prácticas de Almacenamiento se refiere expresamente a laboratorios, droguerías, almacenes especializados y almacenes aduaneros. Sus conceptos ayudan a modelar trazabilidad/recepción en los componentes logísticos donde aplique, pero **no se debe afirmar automáticamente que cada requisito de ese manual aplica a toda botica/farmacia minorista** sin verificar el alcance normativo concreto. [REF-31]
