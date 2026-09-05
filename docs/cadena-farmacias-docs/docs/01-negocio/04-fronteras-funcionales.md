# DOC-FAR-004 — Fronteras ERP, Retail, WMS y Farmacéutico

## 1. Objetivo

Evitar el error de modelar toda la plataforma como un único ERP genérico.

## 2. ERP Corporativo

El ERP responde principalmente a:

- ¿qué compramos y a quién?
- ¿qué debemos pagar?
- ¿cuánto dinero tenemos y dónde?
- ¿cómo se contabilizan las operaciones?
- ¿cuál es el costo y resultado por empresa/centro?

No debería decidir:

- si una receta es válida;
- si un producto requiere receta;
- qué QF puede realizar una actividad;
- qué lote puede dispensarse por regla sanitaria;
- si una venta de producto fiscalizado puede realizarse.

## 3. Retail / POS

El POS responde principalmente a:

- ¿qué está comprando el cliente?
- ¿a qué precio?
- ¿qué promoción aplica?
- ¿qué medio de pago usa?
- ¿qué comprobante corresponde?
- ¿qué caja/turno realiza la operación?

El POS debe solicitar autorizaciones/reglas del dominio farmacéutico cuando corresponda.

## 4. Supply Chain / WMS

Responde a:

- ¿dónde está físicamente el stock?
- ¿de qué lote?
- ¿cuándo vence?
- ¿qué cantidad está disponible/reservada/bloqueada/en tránsito?
- ¿qué debe reponerse?
- ¿qué se despachó y qué se recibió?

No debe asumir que `stock > 0` significa `vendible = true`.

Ejemplo:

```text
Stock físico: 100
├── disponible: 60
├── reservado: 10
├── bloqueado: 20
└── vencido/no vendible: 10
```

## 5. Dominio Farmacéutico

Responde a:

- ¿qué es el producto desde el punto de vista sanitario?
- ¿cuál es su registro y condición de venta?
- ¿requiere receta?
- ¿qué receta aplica?
- ¿quién está autorizado para dispensar/intervenir?
- ¿es producto controlado?
- ¿hay alerta/retiro/bloqueo?
- ¿qué orientación/registros son necesarios?

## 6. Relación entre venta y dispensación

No son exactamente lo mismo.

```text
Receta / necesidad del usuario
          ↓
Validación farmacéutica
          ↓
Dispensación / expendio autorizado
          ↓
Producto(s) listos para cobro
          ↓
POS / Venta
          ↓
Pago + comprobante
          ↓
Salida de inventario
          ↓
Contabilización / ERP
```

Un único flujo de usuario puede atravesar varios dominios sin convertirlos en un solo agregado o módulo.

## 7. Relación compra → inventario → ERP

```text
Planificación / reposición
        ↓
Solicitud / Orden de compra
        ↓
Proveedor
        ↓
Recepción
        ↓
Validación de producto/lote/vencimiento
        ↓
Inventario disponible o cuarentena
        ↓
Cuenta por pagar
        ↓
Tesorería
        ↓
Contabilidad
```

## 8. Relación POS → contabilidad

No se recomienda que cada pantalla POS genere directamente asientos contables.

```text
Venta POS
   ↓
Transacción retail cerrada
   ↓
Evento / integración contable
   ↓
Resumen / documento contabilizable
   ↓
ERP financiero
```

La granularidad final se definirá con Contabilidad y SUNAT.

## 9. Frontera Producto Sanitario vs SKU Comercial

```text
Producto farmacéutico
│
├── principio activo
├── concentración
├── forma farmacéutica
├── vía
├── registro sanitario
├── condición de venta
└── clasificación regulatoria
        │
        ▼
Presentación / SKU comercial
│
├── código de barras
├── paquete
├── unidad de venta
├── marca
├── precio
├── costo
└── promociones
```

Un cambio de precio no debe alterar la identidad/regla sanitaria del medicamento.
