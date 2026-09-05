# UX-FAR-004 — UX del POS y Caja

## Objetivos

- mínima cantidad de pasos para una venta normal;
- nunca ocultar restricciones sanitarias/fiscales;
- funcionar con teclado/scanner/touch según hardware;
- estados online/offline visibles sin alarmismo;
- prevenir doble confirmación.

## Layout conceptual

```text
┌─────────────────────────────────────────────────────┐
│ Local | Caja | Cajero | ONLINE/OFFLINE | Hora      │
├───────────────────────────────┬─────────────────────┤
│ Buscar / escanear             │ Resumen            │
│                               │ Subtotal            │
│ Líneas de venta               │ Descuentos          │
│ SKU | cant | precio | estado  │ Total               │
│                               │                     │
│ Alertas de receta/stock       │ [Cobrar]            │
├───────────────────────────────┴─────────────────────┤
│ Acciones: suspender | cliente | devolución | ayuda │
└─────────────────────────────────────────────────────┘
```

## Reglas UX

1. `Cobrar` se deshabilita si existe bloqueo no resoluble.
2. Restricción por receta lleva a flujo de dispensación, no a un modal que el cajero pueda “aceptar”.
3. El doble clic no genera doble venta; UI + API usan idempotencia.
4. Si la venta está confirmada localmente pero sincronización pendiente, mostrar estado sin pedir al cajero repetirla.
5. No mostrar mensajes como `SQLSTATE 23505` al usuario.
