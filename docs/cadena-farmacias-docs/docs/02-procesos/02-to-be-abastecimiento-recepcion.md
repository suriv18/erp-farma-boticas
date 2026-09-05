# BPM-FAR-002 — TO-BE Abastecimiento, Compra y Recepción

## 1. Objetivo

Definir el flujo desde la necesidad de stock hasta su disponibilidad física y financiera.

## 2. Flujo propuesto

```text
Demanda / stock / política
        ↓
Necesidad de abastecimiento
        ↓
¿Compra o transferencia interna?
        │
        ├── Transferencia → proceso BPM-FAR-003
        │
        └── Compra
              ↓
        Solicitud de compra
              ↓
        Selección proveedor
              ↓
        Orden de compra
              ↓
        Despacho proveedor
              ↓
        Recepción física
              ↓
        Validación documental
              ↓
        Producto + lote + vencimiento
              ↓
        ¿conforme?
           ┌──┴───┐
           │      │
          Sí      No
           │      ↓
           │   Cuarentena/rechazo/diferencia
           ↓
   Stock en ubicación válida
           ↓
   Disponibilidad según estado
           ↓
   Conformidad para CxP / ERP
```

## 3. Controles de recepción a profundizar

- producto solicitado vs recibido;
- cantidad;
- presentación;
- lote;
- vencimiento;
- estado/condiciones del producto;
- registro sanitario/estado cuando corresponda;
- proveedor autorizado según reglas aplicables;
- temperatura/cadena de frío si aplica;
- documentos de traslado/compra;
- diferencias y motivo.

## 4. Principios

1. Recibir físicamente no significa que el stock quede automáticamente vendible.
2. El inventario debe distinguir al menos estados disponibles, bloqueados/cuarentena y no vendibles.
3. El lote recibido debe quedar ligado al origen de recepción.
4. La conformidad logística y la conformidad financiera son conceptos distintos.
5. Una factura de proveedor no debe crear stock por sí sola.
