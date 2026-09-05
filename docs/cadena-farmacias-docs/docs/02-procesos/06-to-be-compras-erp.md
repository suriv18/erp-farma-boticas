# BPM-FAR-006 — TO-BE Compras, Proveedores y ERP (Procure-to-Pay)

## 1. Objetivo

Definir el flujo de abastecimiento corporativo desde la necesidad de compra hasta la obligación de pago y su contabilización, sin mezclar la decisión sanitaria de recepción con el proceso financiero.

## 2. Clasificación de la fuente

- **NORM**: requisitos sanitarios aplicables al establecimiento, producto y documentación.
- **ERP/MKT**: patrón Procure-to-Pay observado en ERP actuales.
- **DOM**: decisión de dominio propuesta para la cadena.
- **POR_VALIDAR**: política que debe confirmar la empresa.

## 3. Flujo objetivo

```text
Necesidad de abastecimiento
        ↓
Propuesta de reposición / solicitud de compra
        ↓
Validar producto, proveedor y establecimiento
        ↓
Aprobación según política corporativa
        ↓
Orden de compra
        ↓
Proveedor despacha
        ↓
Recepción física y sanitaria
        │
        ├── cantidades
        ├── lote
        ├── vencimiento
        ├── registro/identificación del producto
        ├── estado de conservación
        └── condiciones especiales cuando apliquen
        ↓
Recepción aceptada / parcial / observada / rechazada
        ↓
Factura del proveedor
        ↓
Conciliación OC ↔ recepción ↔ factura
        ↓
Cuenta por pagar
        ↓
Pago
        ↓
Contabilización / costos / impuestos
```

## 4. Frontera ERP vs. dominio farmacéutico

El ERP será responsable de:

- proveedor y condiciones comerciales;
- solicitud y orden de compra;
- aprobaciones;
- factura de proveedor;
- cuentas por pagar;
- pagos;
- costo e integración contable.

El dominio farmacéutico/logístico será responsable de decidir si lo recibido puede incorporarse al inventario disponible:

```text
ERP dice: "se recibieron 20 unidades"
             ≠
Farmacéutico/WMS dice: "20 unidades vendibles"
```

Una recepción puede generar inventario en estado `CUARENTENA`, `OBSERVADO` o equivalente hasta que se resuelva la conformidad aplicable.

## 5. Matching financiero

Como patrón ERP se documenta el **three-way match**:

```text
Orden de Compra
      +
Recepción
      +
Factura proveedor
      ↓
Validación para pago
```

No se declara como obligación normativa peruana; es una capacidad ERP recomendada para reducir pagos incorrectos y discrepancias.

## 6. Compras de productos fiscalizados

Los productos sujetos al D.S. N.° 023-2001-SA no deben pasar por un flujo genérico sin controles adicionales. El reglamento atribuye al químico farmacéutico responsable obligaciones sobre adquisición, almacenamiento, custodia, dispensación y control; para determinadas adquisiciones de estupefacientes contempla el Formulario Oficial de Pedido y autorización correspondiente.

Por ello el modelo deberá soportar una política:

```text
Producto normal
    → compra estándar

Producto fiscalizado
    → compra estándar
      + validación regulatoria
      + actor QF
      + documentación específica
      + trazabilidad reforzada
```

## 7. Reglas candidatas derivadas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-COM-001 | No crear una OC con proveedor inactivo/no habilitado para la organización. | DOM |
| RC-COM-002 | La recepción debe mantener referencia a OC y proveedor cuando proviene de compra. | DOM |
| RC-COM-003 | Todo producto trazable por lote debe registrar lote y vencimiento al momento de recepción cuando corresponda. | NORM/DOM |
| RC-COM-004 | Una discrepancia de cantidad/precio no debe ocultarse; debe quedar registrada y resolverse. | ERP/DOM |
| RC-COM-005 | Los productos fiscalizados aplican un flujo reforzado de compra/control. | NORM |
| RC-COM-006 | Pago de factura puede condicionarse a políticas de conciliación configurables. | ERP/POR_VALIDAR |

## 8. Fuentes

- DIGEMID — Establecimientos Farmacéuticos: https://www.digemid.minsa.gob.pe/webDigemid/establecimientos/
- D.S. N.° 023-2001-SA: https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf
- SAP — Procurement workflow / invoice processing: https://help.sap.com/docs/buying-invoicing/shopping-guide-for-business-purchases/procurement-overview
- Microsoft Dynamics 365 — Vendor invoices: https://learn.microsoft.com/en-us/dynamics365/finance/accounts-payable/vendor-invoices-overview
- Odoo 19 — 3-way matching: https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/purchase/manage_deals/control_bills.html
