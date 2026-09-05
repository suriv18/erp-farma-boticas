# DOC-FAR-005 — Matriz Fuente → Capacidad

## 1. Propósito

Evitar que una funcionalidad del mercado se confunda con una obligación legal o que una decisión técnica se presente como regla sanitaria.

| Fuente | Tipo | Concepto sustentado | Capacidades derivables |
|---|---|---|---|
| Ley 29459 | NORM | productos farmacéuticos/dispositivos/productos sanitarios y condiciones básicas | producto regulado, estado sanitario, trazabilidad, prohibiciones |
| D.S. 014-2011-SA + modificatorias | NORM | establecimientos farmacéuticos, funcionamiento, responsabilidades | establecimiento, autorización, Director Técnico, personal, dispensación |
| D.S. 015-2025-SA | NORM | actualización art. 43; personal técnico y responsabilidad del DT | perfiles profesionales, restricciones de funciones, capacitación/supervisión |
| D.S. 016-2011-SA | NORM | registro/control/vigilancia sanitaria y condición de venta | registro sanitario, condición de venta, atributos regulatorios |
| D.S. 023-2001-SA | NORM | estupefacientes/psicotrópicos sujetos a fiscalización | producto controlado, receta/documento especial, registros de control |
| DS 105-MINSA/2020/DIGEMID | NORM | proceso de dispensación | receta, validación, análisis, selección, registro, entrega/orientación |
| DIGEMID Estándares PF 2026 | NORM/OFICIAL | catálogos: forma, vía, condición de venta, ATC, controlados, etc. | MDM farmacéutico |
| DIGEMID Registro Sanitario | OFICIAL | consulta de producto y condición | integración/verificación maestro |
| Observatorio de Precios | NORM/OFICIAL | reporte de precios por farmacias/boticas | pricing regulatorio/reportes |
| SUNAT CPE | NORM/OFICIAL | comprobantes electrónicos | facturación/comprobantes POS |
| LS Central Pharmacy | MKT | farmacia + POS + inventario + recetas + omnicanal | benchmark funcional |
| Dynamics 365 Commerce | MKT | POS, offline, pricing, inventario, pedidos | continuidad POS/retail |
| Oracle Retail | MKT | merchandising, purchasing, replenishment, transferencias, sales audit | núcleo retail central |
| SAP Retail | MKT | inventario por local/ubicación, movimientos, valoración | integración logística-finanzas |
| Odoo 19 Inventory | MKT | lotes, vencimiento y FEFO | política de picking/rotación |

## 2. Ejemplos de clasificación correcta

### `requiere_receta`

- Fuente: registro/condición de venta oficial.
- Tipo: `NORM`.

### `FEFO`

- Fuente: práctica de supply chain y sistemas actuales.
- Tipo: `MKT/DOM`.
- No se declara como obligación sanitaria universal hasta localizar una norma específica aplicable al proceso/categoría.

### `POS offline`

- Fuente: soluciones retail actuales.
- Tipo: `MKT/POR_VALIDAR`.
- Solo se convertirá en `FUNC` si el negocio exige continuidad ante pérdida de internet.

### `uuid_publico`

- Fuente: decisión técnica futura.
- Tipo: `TEC`.

### `Microservicio de inventario`

- Fuente: ninguna todavía.
- Tipo: **no decidido**.
