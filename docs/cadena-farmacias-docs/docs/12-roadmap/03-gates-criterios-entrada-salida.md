# ROAD-FAR-003 — Gates y Criterios de Entrada/Salida

## Gate G0 — Evidencia de negocio

Para iniciar diseño detallado de una capacidad:

- proceso TO-BE documentado;
- actores identificados;
- RF/RN asociadas;
- fuente o clasificación `DOM/FUNC/MKT/TEC/POR_VALIDAR`.

## Gate G1 — Dominio

Antes de persistencia/API:

- agregado/owner definido;
- invariantes;
- estados;
- comandos/eventos relevantes;
- fronteras con otros BC.

## Gate G2 — Arquitectura

Antes de implementación:

- ADR aplicables;
- requisitos de seguridad;
- consistencia/transacción;
- dependencia externa;
- impacto Store Edge.

## Gate G3 — Contrato

Antes del controller/adapter externo:

- API/event contract;
- errores;
- idempotencia;
- autorización;
- criterios de aceptación;
- compatibilidad/versionado.

## Gate G4 — Ready for Release

- tests verdes;
- quality gates;
- seguridad revisada;
- observabilidad;
- migración validada;
- runbook cuando aplique;
- release notes;
- evidencia de trazabilidad.

## Gate G5 — Operacional

- health/metrics;
- alertas accionables;
- backup/restore cuando aplique;
- ownership operativo;
- dashboard/runbook;
- rollback/forward-fix definido.
