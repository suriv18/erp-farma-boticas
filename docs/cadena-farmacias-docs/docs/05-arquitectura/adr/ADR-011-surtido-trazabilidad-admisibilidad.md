# ADR-011 — Surtido, trazabilidad y admisibilidad por establecimiento

**Estado:** decidido para el diseño documental solicitado; implementación y validación operativa no ejecutadas.
**Fecha:** 2026-09-15. **Origen:** DOM/TEC. **Alcance:** Core Central y límite inicial de operación offline del surtido diverso.

## Contexto

El diagnóstico [DAT-FAR-009](../../06-datos/09-adecuacion-surtido-multiproducto-peru.md) identifica capacidades del catálogo que el inventario todavía no soporta. La revisión normativa citada allí delimita categorías admisibles; este ADR fija decisiones de software, no autorizaciones para artículos concretos.

## Decisión

1. Separar rubro comercial, régimen sanitario, admisibilidad y trazabilidad. Ninguno se infiere del nombre ni del indicador es_farmaceutico.
2. Usar expediente sanitario y versiones inmutables como fuente canónica por tenant. Un expediente puede respaldar varios SKU de la misma presentación autorizada o sus variantes verificadas. La NSO del SKU pasa a ser una proyección de lectura, sin edición independiente.
3. Resolver admisibilidad con políticas versionadas para SKU, establecimiento, canal y fecha. Ausencia de revisión significa PENDIENTE; un bloqueo explícito prevalece sobre una autorización comercial. La revisión requiere un permiso específico y evidencia.
4. Hacer independientes los requisitos de lote, vencimiento y serie, con la invariante exige_vencimiento implica exige_lote. Una unidad serializada puede pertenecer a un lote; las cantidades serializadas son enteras y cada serie representa una unidad.
5. Una combinación comercial de atributos identifica un SKU. Normalizar y validar variantes bajo una familia comercial; impedir duplicados, sin usar un hash como única garantía.
6. Conservar versiones de política/evidencia en la operación. Cambios de política no reescriben ventas ni habilitan retroactivamente un artículo.
7. Para el primer lanzamiento del nuevo surtido, confirmación exclusivamente online. Ante pérdida de conexión no se confirma su venta; puede guardarse un borrador local. No se activa un plazo de caché por intuición. La continuidad offline de módulos existentes conserva su ADR y no se declara resuelta aquí.
8. Aplicar cambios mediante expansión, carga revisada, validación y activación por tenant. No reescribir V001–V017 aplicadas ni inventar lotes, autorizaciones o vencimientos para adaptar datos.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Clasificar cosméticos/vitaminas como NO_REGULADO por familia comercial | Confunde clasificación comercial y régimen sanitario |
| Lote genérico y fecha 2099 para todo artículo retail | Produce trazabilidad falsa y distorsiona FEFO |
| Una NSO editable por cada SKU | Duplica la misma autorización y permite discrepancias |
| Sobrescribir JSON de la regla vigente | Impide reconstruir decisiones y controlar concurrencia |
| Vender con caché sin vigencia definida | No permite sostener la evaluación de admisibilidad al confirmar |

## Consecuencias

Se mantiene el núcleo de SKU, venta e inventario. Se añaden entidades sanitarias, políticas, variantes y series, además de cambios de nulabilidad y FKs. Crece el trabajo de revisión y la necesidad de migración gradual. El bloqueo online inicial limita disponibilidad del nuevo surtido durante desconexiones y evita prometer continuidad todavía no diseñada.

Especificaciones vinculantes para este alcance: [diccionario y reglas](../../06-datos/10-diseno-detallado-surtido.md), [contrato API](../../07-api/08-contrato-surtido.md), [migración](../../11-devops/10-migracion-surtido.md), [pruebas](../../10-pruebas/03-plan-validacion-surtido.md).
