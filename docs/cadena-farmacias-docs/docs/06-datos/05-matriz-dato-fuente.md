# DAT-FAR-005 — Matriz Dato → Fuente

| Dato/Concepto | Origen | Clasificación |
|---|---|---|
| Registro sanitario | DIGEMID / regulación | NORM |
| Fabricante / titular / ATC / presentación / origen / tipo de liberación | consulta/formularios DIGEMID | NORM/REG |
| Principio activo + cantidad/unidad | formularios/estándares DIGEMID | NORM/REG |
| Categoría / marca / dimensiones / peso | operación comercial / V1 aportado | DOM/MKT |
| Venta fraccionada / factor de fracción | operación retail; requiere reglas por producto | DOM/CFG |
| Stock mínimo/máximo | política de reposición | ERP/CFG |
| Cliente | POS/CRM; datos personales sujetos a privacidad | FUNC/NORM-SEC |
| Consentimiento de marketing separado | Ley 29733 / D.S. 016-2024-JUS | NORM/SEC |
| Menú de navegación | UX/configuración; V2 aportado | TEC/UX |
| Permiso/rol/ámbito | seguridad/RBAC | SEC/DOM |
| Condición de venta | DIGEMID | NORM |
| Forma farmacéutica / vía / unidad | estándares DIGEMID | NORM/STD sectorial |
| Clasificación controlada | D.S. 023-2001-SA / DIGEMID | NORM |
| Establecimiento autorizado | Ley/Reglamento/DIGEMID | NORM |
| Director Técnico / asignación profesional | Reglamento vigente | NORM |
| Producto prohibido en farmacia/botica | RM 734-2025/MINSA | NORM |
| SKU comercial | operación retail | DOM/MKT |
| Posición de inventario | diseño de inventario multi-local | DOM |
| Movimiento append-only | trazabilidad | DOM/TEC |
| FEFO | política de salida | MKT/CFG |
| Precio/promoción | negocio retail | FUNC/DOM |
| Venta | proceso POS | FUNC/DOM |
| CPE / nota de crédito | SUNAT | FISCAL |
| `message_id` / idempotency key | sincronización | TEC/STD |
| Store Edge projection | arquitectura retail offline | TEC/MKT |
| Reporte de farmacovigilancia sin venta | DIGEMID/NotiMED | NORM/DOM |
| Caso recall por lote | DIGEMID retiros | NORM/DOM |
| Datos personales | Ley 29733 / DS 016-2024-JUS | NORM |

## Regla

La matriz deberá acompañar el futuro diccionario físico para impedir que un campo técnico sea presentado como requisito normativo.
