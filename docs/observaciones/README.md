# Observaciones de revisión — ERP Botica

Fecha: 10 de octubre de 2026.

La revisión abarca los 17 módulos declarados de `service-botica`, las 12 funcionalidades de frontend y el inventario de 116 tablas declaradas en 38 migraciones. Incluye un análisis específico de las 14 tablas del catálogo y comparación con fuentes oficiales del mercado.

## Documentos

| Documento | Contenido |
| --- | --- |
| [Revisión integral de módulos](2026-10-10-revision-integral-modulos.md) | Estado de cada módulo, frontend, infraestructura, OBS-007–019 y escenarios de aceptación. |
| [Producto, SKU y modelo de catálogo](2026-10-10-analisis-modelo-catalogo.md) | Decisión de diseño, revisión tabla por tabla, caja/blíster/unidad, OBS-020–023 y migración propuesta. |
| [Comparativa de mercado](2026-10-10-comparativa-mercado.md) | Inkafarma, Mifarma, Boticas Perú, Farmacia Universal y referencias ERP/POS; fuentes y brechas. |
| [Inventario de tablas](2026-10-10-inventario-tablas.md) | Las 116 declaraciones CREATE TABLE, agrupadas por esquema y enlazadas a su migración. |
| [Revisión inicial](2026-10-10-revision-erp-botica.md) | OBS-001–006: autorización, sesión/caché, contraseña, dispensación, integridad de lotes y documentación. |

## Conclusiones principales

- El núcleo implementado es parcial: seis módulos contienen implementación de negocio y once mantienen estructura. Este conteo no representa un porcentaje de avance.
- Conviene mantener producto regulado y SKU separados. Familia comercial opcional, presentaciones estructuradas y evidencia sanitaria versionada resuelven necesidades diferentes.
- Las prioridades son autorización por ámbito, sesión, dispensación/admisibilidad, costos, impuestos, unidades y consistencia de inventario.
- Hay diferencias entre tablas diseñadas, contratos expuestos y procesos ejecutables. No está acreditada equivalencia funcional con una cadena comercial.
- Los 23 identificadores de observaciones distinguen defectos, brechas de integración/capacidad y decisiones pendientes; cada informe precisa evidencia y alcance.

## Alcance de verificación

Se inspeccionaron código, SQL, documentación y pruebas existentes. La revisión inicial registra un `gradlew check` satisfactorio con reutilización de caché, además de comprobaciones frontend y sus límites. Esta ampliación no vuelve a presentar esos resultados como nuevas pruebas de integración.

No se verificó una base desplegada ni se completaron pruebas end-to-end con frontend, servicio y PostgreSQL reales. Los escenarios propuestos están pendientes de ejecución; no son pruebas aprobadas. La exploración de mercado utilizó páginas públicas y documentación oficial, sin transacciones ni acceso a sistemas internos.

Los cambios de esta revisión son documentales dentro de esta carpeta. Las recomendaciones no se aplicaron a código ni migraciones, y los diseños del ADR-011 se distinguen de la implementación actual.

