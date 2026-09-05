# ADR-001: Autenticación JWT (no OAuth2 en MVP)

- Estado: Sustituido / decisión reabierta
- Fecha: 2026-07-15
- Decisores: Equipo ERP Boticas

> Este ADR se conserva como antecedente. La documentación canónica de seguridad determinó que IAM
> debe validarse para la operación retail/offline antes de escoger JWT propio, OIDC/IdP o un modelo
> híbrido. Consulte
> [`GOV-FAR-001`](../cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md) y
> [`SEC-FAR-001`](../cadena-farmacias-docs/docs/08-seguridad/01-arquitectura-seguridad.md).

## Contexto

Sprint 0 exige cerrar el mecanismo de autenticación del backend antes de Sprint 1 (security/RBAC). Las opciones evaluadas fueron JWT propio con Spring Security y OAuth2/OIDC (Authorization Server o proveedor externo).

## Decisión

Usar **Spring Security + JWT** (stateless) para el MVP.

OAuth2/OIDC queda como evolución opcional post-release si se requiere SSO corporativo o IdP externo.

## Consecuencias

- Sprint 1 implementa login, emisión/validación de JWT y RBAC.
- No se introduce Authorization Server ni dependencia de IdP externo en el MVP.
- La sesión en frontend se modela con Zustand + token en almacenamiento seguro del cliente.
- Si más adelante se adopta OAuth2, el contrato de permisos (roles/módulos) se mantiene; solo cambia el emisor del token.
