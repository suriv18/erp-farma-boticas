# DAT-FAR-008 — Consolidación del DDL previo y scripts V1/V2 aportados

**Versión:** 0.1  
**Estado:** aplicado al baseline PostgreSQL 18 v0.3.

## 1. Objetivo

Unificar en una sola línea base el modelo construido durante el análisis de Cadena de Farmacias y los scripts de referencia aportados posteriormente, sin duplicar entidades ni degradar las fronteras DDD.

## 2. Regla de precedencia

1. Norma/fuente oficial y reglas de negocio documentadas.
2. RF/CU/CA y modelo de dominio actual.
3. Arquitectura aprobada: multi-tenant, Core Central + Store Edge opcional, Outbox/Inbox.
4. Campos/capacidades útiles de los scripts aportados.
5. Si una capacidad del V1 no tiene alcance/RF suficiente, se difiere.

## 3. Elementos adoptados del V1

| Área | Elementos rescatados/adaptados |
|---|---|
| Organización | dirección fiscal, contacto, geolocalización, online, datos operativos de almacén/terminal |
| Catálogo | categoría, marca, dimensiones/peso, fraccionamiento, stock mínimo/máximo, código de barras principal |
| Regulatorio | fabricante/titular, forma, condición de venta, principios activos; ampliados con campos DIGEMID |
| Proveedores | contactos, condiciones de pago, días de crédito, roles laboratorio/importador/distribuidor |
| Compras | fechas de entrega, totales, descuentos/impuestos, recepción documental y cantidades aceptadas/cuarentena/rechazadas |
| Inventario | costo promedio, stock disponible generado, stock anterior/posterior, conteos, transferencias enriquecidas |
| Retail | cliente, medio de pago, número de operación, vendedor, tipos/canales de venta, datos de pago sin información PCI sensible |
| Finanzas | cuenta bancaria, CxP, pagos CxP y CxC |
| Plataforma | sesiones de aplicación, notificaciones, feature flags y versiones |

## 4. Elementos adoptados del V2

- `modulo_sistema` separado del menú.
- `permiso` por recurso/acción.
- árbol `menu_navegacion`.
- relación menú ↔ permisos.
- prevención de ciclos y padres inválidos.
- endpoint de navegación derivado de la sesión, sin `usuarioId` aportado por el cliente.
- menú como **visibilidad**, nunca como autorización del backend.

Implementación física: `V016__navegacion_dinamica_rbac.sql`.

## 5. Elementos del V1 deliberadamente no copiados tal cual

| Elemento | Motivo |
|---|---|
| `BIGSERIAL` + `gen_random_uuid()` | baseline objetivo PostgreSQL 18 usa Identity + UUIDv7 |
| `requiere_receta` duplicado entre producto comercial y condición de venta | evita fuentes de verdad contradictorias |
| una sola FK `empresa_id`/`sucursal_id` sin contexto | se mantienen FKs contextuales multi-tenant |
| `venta_detalle.lote_id` único | una línea puede consumir varios lotes; se mantiene `venta_linea_lote` |
| devolución → stock automático | contradice regla de evaluación/disposición sanitaria |
| `request_payload` / `response_payload` crudos en logs | riesgo de secretos/PII; se guardan hashes/metadata sanitizada |
| password local fijo en `usuario` | autenticación concreta sigue sujeta a ADR |
| sincronización genérica por conflictos JSON | Store Edge usa Outbox/Inbox/idempotencia; conflictos se diseñan por aggregate/policy |

## 6. Capacidades diferidas

No se incorporan todavía al baseline físico completo:

- RRHH/planillas;
- CRM/CMR avanzado y fidelización completa;
- carrito/e-commerce/delivery completo;
- libro mayor/asientos contables completos;
- pasarela de pagos con payloads propios de proveedor.

Estas capacidades existen en el mapa/roadmap, pero deben entrar mediante su RF/CU/ADR correspondiente antes de fijar tablas definitivas.

## 7. Campos regulatorios enriquecidos

`producto_regulado` incluye ahora, entre otros: registro sanitario, denominación, concentración, presentación, forma farmacéutica, vía, condición de venta, ATC, titular, fabricante, importador, origen, tipo de liberación y clasificación controlada. Los ingredientes activos admiten cantidad/unidad.

Estos atributos se justifican por los campos de consulta/formularios de DIGEMID y no por el V1 por sí solo.
