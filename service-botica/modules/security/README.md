# Módulo Security

**Estado:** administración IAM y autenticación local JWT implementadas sobre el esquema canónico
`sch_seguridad`.

## Modelo de datos

- `V013__seguridad_iam.sql` continúa siendo la fuente canónica para usuarios, roles, permisos,
  ámbitos, sesiones y dispositivos.
- `V018__seed_security_administration_permissions.sql` registra las capacidades administrativas.
- `V020__local_authentication_jwt.sql` extiende el esquema con `credencial_local`, `token_refresh`
  y `token_recuperacion_password`.
- Las contraseñas se almacenan mediante un hash adaptativo de Spring Security. Los refresh tokens y
  tokens de recuperación son valores opacos aleatorios; la base de datos conserva únicamente su
  digest SHA-256.

## Autenticación local

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Público |
| `POST` | `/api/v1/auth/refresh` | Público, rota el refresh token |
| `POST` | `/api/v1/auth/logout` | JWT válido |
| `POST` | `/api/v1/auth/password/forgot` | Público, respuesta no enumerable |
| `POST` | `/api/v1/auth/password/reset` | Público, token de un solo uso |
| `POST` | `/api/v1/auth/password/change` | JWT válido |
| `POST` | `/api/v1/usuarios/{userId}/credencial-local` | `seguridad.credenciales.gestionar` |

El access token es un JWT HS256 de corta duración. Cada petición valida firma, issuer, audience,
expiración y el estado actual de la sesión en `sch_seguridad.sesion_usuario`. Las autoridades se
resuelven nuevamente desde roles y permisos activos en la base de datos; solo asignaciones de ámbito
`GLOBAL` se convierten en authorities genéricas. Los ámbitos organizacionales requieren un evaluador
de autorización específico y no se elevan implícitamente a permisos globales.

El login bloquea la credencial tras intentos fallidos, exige un dispositivo `CONFIABLE` para el canal
POS y deniega cuentas marcadas `mfa_requerido` mientras no exista un segundo factor implementado.
Cambiar, recuperar o reemplazar una contraseña revoca las sesiones vigentes. La reutilización de un
refresh token rotado también revoca su sesión.

La solicitud de recuperación publica `PasswordResetRequested`; el consumidor de notificaciones debe
enviar el token por el canal configurado. El token nunca se devuelve en HTTP ni debe registrarse en
logs.

## Configuración obligatoria

Producción debe definir `BOTICA_JWT_SECRET` con al menos 32 bytes aleatorios codificados en Base64.
La aplicación falla al iniciar si la variable está ausente, no es Base64 o tiene menos de 256 bits.
También pueden ajustarse:

- `BOTICA_JWT_ISSUER` y `BOTICA_JWT_AUDIENCE`;
- `BOTICA_ACCESS_TOKEN_TTL` (máximo una hora);
- `BOTICA_REFRESH_TOKEN_TTL` (máximo 90 días);
- `BOTICA_PASSWORD_RESET_TTL` (máximo una hora);
- `BOTICA_MAX_FAILED_ATTEMPTS` y `BOTICA_LOCK_DURATION`.

## Administración IAM

El módulo expone control de usuarios, identidades externas, roles, permisos, asignaciones con ámbito,
módulos, sesiones y dispositivos. Todos los endpoints administrativos usan `@PreAuthorize` y los
códigos `seguridad.*` registrados en el catálogo canónico.

## Límites arquitectónicos

La implementación mantiene Ports & Adapters: los casos de uso dependen de puertos de aplicación;
JWT, hashing, HTTP, eventos y JDBC quedan en infraestructura. Los controllers no acceden directamente
a repositorios ni entidades.

## Pruebas automatizadas

- pruebas unitarias de `LocalAuthService` para login, bloqueo, POS, refresh, replay, logout,
  recuperación, cambio y aprovisionamiento de contraseñas;
- pruebas de límites de `LocalAuthPolicy` y del secreto HS256 mínimo de 256 bits;
- pruebas criptográficas de PBKDF2, claims JWT, expiración, tokens opacos y digest SHA-256;
- pruebas de sesión revocada y resolución actualizada de authorities desde la base de datos;
- pruebas HTTP E2E y de empaquetado de las migraciones V013, V018 y V020.

Verificación recomendada:

```text
./gradlew :modules:security:check
./gradlew :bootstrap-app:test
./gradlew :testing:architecture-tests:test
```
