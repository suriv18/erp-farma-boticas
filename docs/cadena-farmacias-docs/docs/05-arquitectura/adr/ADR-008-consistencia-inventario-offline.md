# ADR-008 — Consistencia de inventario offline

**Estado:** Propuesto.

## Problema
Cuando una tienda queda offline, el stock central y el stock local pueden divergir.

## Pendiente
Definir con datos reales de tiendas/cajas/e-commerce: safety stock, offline sellable quantity, conflictos, reservas omnicanal y tratamiento de sobreventa. No hardcodear una política antes de ese análisis.

## Límite de activación del nuevo surtido

ADR-011 fija confirmación exclusivamente online para el surtido nuevo durante su primera activación. Ese límite está decidido y no requiere una política de stock offline para empezar. El problema general de continuidad de tienda de este ADR mantiene su estado propuesto.
