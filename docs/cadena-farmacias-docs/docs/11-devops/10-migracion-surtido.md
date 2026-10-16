# DEVOPS-FAR-010 — Plan de migración de surtido diverso

**Estado:** procedimiento documental definido; no ejecutado. **Fecha:** 2026-09-15.

## 1. Alcance y orden

El SQL original y V001–V017 describen el estado actual. [DAT-FAR-010](../06-datos/10-diseno-detallado-surtido.md) describe el destino. Asignar números de migración disponibles al implementar; V018 no se asume libre en todas las ramas o instalaciones. No modificar checksums de versiones aplicadas.

| Paso | Acción | Gate de salida |
|---|---|---|
| M0 Inventario | Identificar versión aplicada, tamaño, duplicados, faltantes, cruces SKU/lote, NSO contradictoria y dependencias | Informe por tenant, copia de seguridad y restore probado en entorno aislado |
| M1 Expansión | Crear expedientes, versiones, políticas, familia/variantes, series y relaciones de decisiones; mantener aplicación anterior compatible | DDL limpio en PostgreSQL 18 y reconstrucción V001–V017 + nuevas versiones |
| M2 Carga | Mapear SKU existentes con evidencia revisada; cargar familia y política; conservar IDs y cantidades | Conteos y sumas conciliados; casos ambiguos PENDIENTE, sin autorización automática |
| M3 Integridad | Añadir claves tenant/SKU/id y FKs contextuales; conciliar duplicados antes de índices | Ningún cruce, referencia huérfana o clave natural duplicada |
| M4 Opcionalidad | Adaptar nulabilidad de lote/fecha e índices; desplegar comandos y controles de persistencia | Políticas exigibles incluso con escritura directa del rol de aplicación; todos los flujos de prueba pasan |
| M5 Activación | Habilitar por tenant/local con RBAC, revisión sanitaria y confirmación online | Ensayo de venta/transferencia/devolución y rollback funcional; métricas y soporte preparados |
| M6 Contracción | Retirar escrituras antiguas de NSO y campos/proyecciones obsoletos cuando ya no haya consumidores | Evidencia de uso, compatibilidad y plan de retención de historia |

M3 precede a M4 para que flexibilizar NULL no deje una ventana sin integridad contextual. Índices grandes requieren una estrategia de creación y validación compatible con el motor y la herramienta elegidos; medir duración/bloqueos en ensayo, no prometer una ventana sin mediciones.

## 2. Reglas de conversión

- No eliminar ni fusionar lotes automáticamente por similitud de número. Duplicados activos pasan a conciliación con documentos y saldos.
- Mantener lote y vencimiento históricos; no convertirlos a NULL solo porque cambió la política comercial.
- Un SKU sin evidencia suficiente queda PENDIENTE. No mapear de oficio todas las vitaminas o cosméticos a NO_REGULADO.
- Las diferencias entre codigo_nso y producto_regulado generan un informe de revisión; no se resuelven tomando arbitrariamente el último valor.
- Conservar UUID públicos, documentos, importes, costo y cantidades antes/después. La carga de series exige identificar unidades reales y cuadrar con stock; no fabricar seriales.
- Una revisión de datos puede producir ajustes con autorización, nunca una alteración silenciosa del kardex.

## 3. Conciliación y controles

Comparar por tenant, SKU, local y estado: físico, reservado, disponible y valor. Comparar líneas de recepción, transferencias, ventas y devoluciones con sus asignaciones de lotes/series. Exigir delta cero salvo ajuste autorizado identificado; exportar evidencia de cada diferencia y su resolución.

Versionar y guardar el script de migración, su revisión, duración, filas afectadas y resultados de validación. Las migraciones y el ensayo usan copias aisladas, no la base productiva para probar comportamiento.

## 4. Reversión

Antes de M5: desactivar la función y volver a la aplicación compatible sin borrar las tablas añadidas. Después de admitir stock sin lote/fecha, restaurar NOT NULL a ciegas no es una reversión válida. Congelar nuevas operaciones del surtido afectado y aplicar forward-fix; una restauración requiere conciliación de todas las operaciones posteriores al backup y autorización operativa.

Probar la compatibilidad de lectores antiguos; si no pueden interpretar nuevos datos, no incluirlos en la ruta de rollback. La bandera de función reduce exposición, pero no sustituye controles de integridad ni revierte datos.

## 5. Responsables de ejecución

Equipo de datos: DDL y conciliación. Backend: comandos, transacciones y permisos. QA: [plan de pruebas](../10-pruebas/03-plan-validacion-surtido.md). Responsable sanitario autorizado: clasificación/evidencias por artículo. Operaciones: ventana y recuperación. Los nombres y la fecha del despliegue se asignan al plan operativo, no se inventan en la documentación.
