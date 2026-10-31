# ROAD-FAR-007 — Cierre documental del surtido diverso

**Fecha:** 2026-09-15. **Estado:** documentación de este alcance completada; implementación no ejecutada.

## 1. Resultado y alcance

Se completan las decisiones que quedaron abiertas en el análisis de boticas/farmacias con medicamentos, vitaminas, cosméticos y otros artículos admisibles. La evidencia peruana permanece en DAT-FAR-009; las decisiones técnicas se separan de las autorizaciones concretas de cada artículo.

Este cierre no declara terminados ERP completo, CRM, e-commerce, infraestructura, retención legal, proveedor fiscal, autenticación ni todos los ADR operativos. No se elimina un pendiente real cambiándole la etiqueta.

## 2. Entregables y resolución

| Pendiente documental | Resolución | Documento |
|---|---|---|
| Fuente de NSO y otras evidencias | Expediente por tenant, versiones inmutables y proyección de lectura en SKU | [DAT-FAR-010](../06-datos/10-diseno-detallado-surtido.md) |
| Admisibilidad por local | Reglas con ámbito/vigencia, precedencia de bloqueo, revisión y snapshots | [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md) |
| Stock sin lote/caducidad | Opcionalidad real con políticas y claves contextuales | [DAT-FAR-010](../06-datos/10-diseno-detallado-surtido.md) |
| Variantes y series | Identidad canónica de variante y unidad serializada con asignaciones | [DAT-FAR-010](../06-datos/10-diseno-detallado-surtido.md) |
| Contratos | Operaciones, campos, permisos, idempotencia y errores | [API-FAR-008](../07-api/08-contrato-surtido.md) |
| Operación offline inicial | Confirmación online; borrador durante desconexión | [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md) |
| Cambio de base existente | Expansión, carga revisada, validación, activación y contracción | [DEVOPS-FAR-010](../11-devops/10-migracion-surtido.md) |
| Aceptación | RF/RN/CU/CA y 23 escenarios de ejecución | [REQ-FAR-MULTI](../03-requerimientos/09-surtido-multiproducto.md), [TST-MULTI](../10-pruebas/03-plan-validacion-surtido.md) |

## 3. Estado de ejecución

| Trabajo | Estado |
|---|---|
| Decisiones, diccionario, contratos y procedimiento | Documentados |
| Enlaces e identificadores documentales | Verificados al cierre |
| DDL de evolución y código | Pendientes de implementación |
| Migración/conciliación de una base real | Pendiente de ejecución |
| Pruebas PostgreSQL, concurrencia y E2E | Definidas, no ejecutadas |
| Habilitación sanitaria del surtido de cada local | Revisión operativa por SKU y evidencia |

La documentación puede guiar la implementación sin volver a escoger el modelo. Las pruebas, métricas, aprobaciones operativas y resultados reales se registrarán cuando existan; no se presume su aprobación por este cierre.
