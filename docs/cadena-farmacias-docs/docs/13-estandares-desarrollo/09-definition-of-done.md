# STD-FAR-009 — Definition of Done

Una historia/capacidad no está terminada solo porque compile.

## DoD mínima

- RF/CU/CA identificados;
- regla/fuente revisada cuando aplique;
- diseño de dominio coherente;
- implementación respeta arquitectura;
- unit tests;
- integration tests cuando corresponda;
- pruebas de autorización;
- contrato OpenAPI/evento actualizado;
- errores Problem Details definidos;
- idempotencia/concurrencia analizada;
- migración validada si existe;
- logs/metrics/traces necesarios;
- sin secretos ni findings bloqueantes;
- documentación actualizada;
- code review aprobado;
- criterios de aceptación verificados.

## Adicional Store Edge

Si afecta tienda/offline:

- compatibilidad de protocolo;
- replay/deduplicación probada;
- pérdida de conexión probada;
- backlog/reintento probado;
- upgrade de Store Edge evaluado.

## Adicional regulatorio/fiscal

- fuente identificada;
- pruebas negativas;
- auditoría;
- evidencia del resultado;
- no hardcodear regla temporal si debe parametrizarse/versionarse.
