# BPM-FAR-004 — TO-BE Venta, Dispensación y POS

## 1. Objetivo

Separar claramente la decisión farmacéutica de la transacción comercial y del cobro.

## 2. Producto de venta libre / retail

```text
Producto escaneado/buscado
        ↓
Identificar SKU y producto regulado
        ↓
Condición de venta
        ↓
¿requiere control farmacéutico/receta?
        │
       No
        ↓
Validar stock vendible
        ↓
Seleccionar lote conforme política
        ↓
Precio / promoción
        ↓
Pago
        ↓
Comprobante
        ↓
Movimiento de inventario
        ↓
Auditoría venta
```

## 3. Producto bajo receta

```text
Producto / receta
        ↓
Recepción de prescripción
        ↓
Validar legibilidad/formato/vigencia/datos
        ↓
Validar prescriptor/paciente/producto según reglas
        ↓
Análisis e interpretación por actor competente
        ↓
Selección / preparación
        ↓
Validar stock y lote
        ↓
Registrar dispensación
        ↓
Orientación/información al usuario
        ↓
Liberar ítem para cobro
        ↓
POS / Pago / Comprobante
        ↓
Salida de inventario
```

## 4. Fuente normativa

La Directiva Sanitaria N.° 105-MINSA/2020/DIGEMID establece que el proceso de dispensación incluye:

- recepción y validación de la prescripción;
- análisis e interpretación;
- preparación y selección de productos;
- registros;
- entrega e información para el paciente.

Asimismo, la dispensación debe respetar la condición de venta del registro sanitario y acepta recetas físicas, en imagen digital y electrónicas que cumplan los requisitos aplicables.

## 5. Diseño funcional

Por lo anterior, no se modelará:

```text
VentaDetalle
    receta_id nullable
```

como única representación del proceso.

Conceptualmente habrá una relación más rica:

```text
Prescripción
    ↓
ProcesoDispensación
    ↓
LíneaDispensada
    ↓
LíneaVentaPOS
```

La estructura definitiva se resolverá durante el modelo de dominio.
