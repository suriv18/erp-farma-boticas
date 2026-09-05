# SEC-FAR-006 — Auditoría y Trazabilidad

## Auditoría funcional ≠ log técnico

### Auditoría

- quién;
- qué acción;
- sobre qué recurso;
- establecimiento/tenant;
- resultado;
- cuándo;
- correlation id;
- razón/aprobación cuando aplique.

### Observabilidad

- exceptions;
- traces;
- metrics;
- latencias;
- health;
- resource usage.

## Eventos auditables prioritarios

- login/logout/denegaciones;
- cambio de roles/permisos;
- apertura/cierre/arqueo de caja;
- override de precio/descuento;
- venta/devolución/anulación;
- acceso/validación de receta;
- dispensación y actor profesional;
- controlados;
- bloqueo/liberación de lote;
- ajuste de inventario;
- recall;
- CPE/reintento/anulación;
- posting ERP;
- exportación de datos.

## Contenido prohibido

No registrar en logs/auditoría:

- contraseñas;
- tokens completos;
- secretos;
- CVV/PAN completo;
- receta completa si basta una referencia;
- payload personal completo sin necesidad.
