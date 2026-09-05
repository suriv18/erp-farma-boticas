# Fase 7B — API e Integraciones

**Estado:** estándar y contratos candidatos; aún no existe OpenAPI completo por recurso.

## Documentos

- [API-FAR-001 — Estándar REST](01-estandar-rest.md)
- [API-FAR-002 — Recursos y endpoints candidatos](02-recursos-endpoints-candidatos.md)
- [API-FAR-003 — Problem Details](03-problem-details.md)
- [API-FAR-004 — Idempotencia, concurrencia y sincronización](04-idempotencia-concurrencia-sync.md)
- [API-FAR-005 — OpenAPI contract-first](05-openapi-contract-first.md)
- [API-FAR-006 — Contratos externos](06-integraciones-externas.md)
- [API-FAR-007 — Navegación dinámica](07-contrato-navegacion-dinamica.md)

## Principios

- HTTP según RFC 9110. [REF-43]
- errores con RFC 9457 cuando aplique. [REF-44]
- OpenAPI como contrato independiente del lenguaje; la última publicación es 3.2.0, pero la versión efectiva del proyecto dependerá del toolchain. [REF-45]
- Richardson Level 2 como objetivo base; hypermedia solo cuando aporte valor.
- no exponer entidades de persistencia directamente.
