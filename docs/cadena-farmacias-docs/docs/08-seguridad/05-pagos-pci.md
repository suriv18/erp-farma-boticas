# SEC-FAR-005 — Pagos y Alcance PCI

PCI SSC mantiene PCI DSS v4.0.1 como versión publicada del estándar. [REF-49]

## Objetivo arquitectónico

**Reducir el alcance PCI** evitando almacenar o procesar datos de tarjeta cuando un terminal/adquirente/tokenización pueda hacerlo.

## Preferido

```text
POS
  ↓
Payment Port
  ↓
Terminal / adquirente certificado
  ↓
resultado/token/referencia
```

El dominio Retail conserva:

- importe;
- medio de pago normalizado;
- estado;
- referencia de operación;
- token no sensible cuando el proveedor lo permita.

No conserva por defecto:

- PAN completo;
- CVV/CVC;
- PIN;
- track data;
- datos prohibidos por estándares de pago.

## Decisión pendiente

El alcance PCI real solo puede determinarse cuando se seleccione el flujo de pagos, terminal, adquirente y e-commerce.
