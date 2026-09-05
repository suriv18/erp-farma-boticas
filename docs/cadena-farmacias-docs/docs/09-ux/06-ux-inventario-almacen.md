# UX-FAR-006 — UX de Inventario y Almacén

## Principios

- lote y vencimiento siempre visibles cuando importan;
- capturas optimizadas para scanner;
- cantidades solicitada/despachada/recibida no se sobrescriben;
- diferencias requieren motivo y evidencia;
- bloqueo/recall debe tener señal inequívoca.

## Vista de stock

Filtros esenciales:

- establecimiento;
- almacén/ubicación;
- SKU;
- lote;
- vencimiento;
- estado;
- disponibilidad.

## Recepción

```text
OC
 ↓
Escanear SKU
 ↓
Capturar lote + vencimiento + cantidad
 ↓
Comparar OC
 ↓
Registrar diferencia
 ↓
Aceptar recepción
```

## Transferencias

Mostrar simultáneamente:

- solicitado;
- preparado;
- despachado;
- en tránsito;
- recibido;
- diferencia.
