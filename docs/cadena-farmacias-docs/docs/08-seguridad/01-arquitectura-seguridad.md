# SEC-FAR-001 — Arquitectura de Seguridad

## 1. Principios

- deny by default;
- least privilege;
- separation of duties;
- zero trust entre componentes donde sea práctico;
- minimización de datos;
- defensa en profundidad;
- secretos fuera del código/BD funcional;
- autenticación fuerte para operaciones sensibles;
- auditoría independiente de logs técnicos.

## 2. Capas

```text
Usuario / dispositivo
      ↓
Autenticación
      ↓
Sesión / dispositivo confiable
      ↓
RBAC + ámbito
      ↓
Regla contextual de dominio
      ↓
Autorización de objeto/propiedad
      ↓
Operación
      ↓
Auditoría
```

OWASP API Security Top 10 2023 mantiene autorización a nivel de objeto/función entre los riesgos principales; por ello conocer un `id` nunca concede acceso por sí solo. [REF-48]

## 3. Autenticación

La tecnología queda pendiente de ADR. Candidatos:

- OIDC/OAuth2 con proveedor de identidad;
- JWT de acceso corto + sesión/refresh revocable;
- autenticación corporativa federada para backoffice;
- credenciales/dispositivo gestionado en Store Edge.

No se copiará automáticamente la decisión JWT del proyecto de Salud Ocupacional sin validar operación retail/offline.

## 4. Seguridad de APIs

- validación de issuer/audience/algoritmo si se adopta JWT;
- rate limiting de flujos sensibles;
- autorización por objeto y propiedad;
- límites de payload;
- protección SSRF en integraciones/webhooks;
- inventario de APIs y versiones;
- validación estricta de APIs de terceros.

## 5. Baseline

OWASP ASVS 5.0.0 se utilizará como catálogo de verificación; el nivel objetivo deberá definirse según análisis de riesgo. [REF-47]
