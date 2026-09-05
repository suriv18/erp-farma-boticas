# ADR-010 — Navegación dinámica desacoplada de RBAC

**Estado:** Aceptado  
**Fecha:** 2026-09-02

## Contexto

El ERP requiere menús distintos según aplicación, rol y ámbito, pero ocultar opciones del frontend no debe convertirse en mecanismo de autorización.

## Decisión

- Mantener catálogo de módulos/permisos en `sch_seguridad`.
- Mantener metadatos de navegación en `sch_app`.
- Relacionar menú con permisos únicamente para visibilidad.
- Resolver `/api/v1/me/navigation` desde la identidad autenticada y contexto vigente.
- Autorizar cada endpoint independientemente del menú.

## Consecuencias

Positivas:
- menú configurable sin acoplarlo a roles;
- mismo RBAC sirve a ERP/POS/e-commerce/mobile;
- menor riesgo de confundir UX con seguridad.

Costos:
- requiere invalidación de caché por cambio de ámbito/permisos;
- frontend mantiene catálogo seguro de rutas/iconos.
