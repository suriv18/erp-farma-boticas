# BPM-FAR-013 — Matriz Proceso → Regla → Fuente

## 1. Objetivo

Consolidar las reglas candidatas surgidas de los procesos TO-BE antes de convertirlas en requisitos y reglas formales `RF-*` / `RN-*`.

## 2. Clasificación

| Etiqueta | Significado |
|---|---|
| `NORM` | Derivada de norma/fuente oficial aplicable. |
| `FISCAL` | Derivada de SUNAT/comprobantes/tributación. |
| `DOM` | Decisión propuesta del dominio. |
| `ERP` | Práctica de ERP/retail contrastada con mercado. |
| `SEC` | Seguridad, autorización o confidencialidad. |
| `AUD` | Trazabilidad/auditoría. |
| `POR_VALIDAR` | Requiere política empresarial o validación especializada. |

## 3. Matriz inicial

| Proceso | Regla candidata | Fuente principal | Tipo |
|---|---|---|---|
| Compra | Producto fiscalizado activa controles específicos. | D.S. 023-2001-SA | NORM |
| Compra | Factura puede conciliarse con OC y recepción. | ERP actuales | ERP/DOM |
| Recepción | Lote/vencimiento deben preservarse para productos trazables. | Farmacéutico + inventario | NORM/DOM |
| POS | Boleta/factura tienen flujo tributario independiente del pago. | SUNAT | FISCAL |
| POS | Toda venta identifica local, terminal, turno y operador. | Retail | DOM/AUD |
| Devolución | Nota de crédito puede sustentar devolución total/parcial. | SUNAT | FISCAL |
| Devolución | Producto físico devuelto no vuelve automáticamente a vendible. | BPA + seguridad sanitaria | NORM/DOM |
| Controlados | Tipo de receta depende de lista/clasificación. | D.S. 023-2001-SA | NORM |
| Controlados | Receta especial atendida debe ser retenida/archivada cuando aplica. | D.S. 023-2001-SA | NORM |
| Controlados | Balance trimestral para listas/sujetos previstos por norma. | D.S. 023-2001-SA | NORM |
| Recall | Bloqueo se realiza a nivel de lote afectado. | Alertas DIGEMID | NORM/DOM |
| Recall | Stock inmovilizado se concilia por establecimiento. | Recall | DOM/AUD |
| Farmacovigilancia | Reporte puede existir sin venta asociada. | DIGEMID | NORM/DOM |
| Farmacovigilancia | Datos del paciente/reportante requieren confidencialidad. | Marco sanitario/privacidad | NORM/SEC |
| Cierre | Posting ERP debe ser idempotente. | Arquitectura/ERP | TEC/DOM |
| Cierre | Diferencia de caja queda registrada. | Retail | DOM/AUD |
| Cierre | Estado de venta y estado contable son independientes. | ERP/DDD | DOM |

## 4. Regla de gobierno documental

Nada de esta matriz pasa automáticamente a `RN` normativa. Durante la siguiente fase cada regla deberá:

1. identificar sujeto obligado;
2. verificar vigencia de fuente;
3. precisar alcance (farmacia, botica, droguería, almacén, cadena, QF, etc.);
4. documentar excepción;
5. definir criterio de aceptación;
6. clasificar qué parte es técnica/configurable.

## 5. Resultado esperado

Esta matriz será la entrada para:

```text
03-requerimientos/
├── 01-srs.md
├── 02-requerimientos-funcionales.md
├── 03-requerimientos-no-funcionales.md
├── 04-reglas-negocio.md
├── 05-casos-uso.md
└── 08-criterios-aceptacion.md
```
