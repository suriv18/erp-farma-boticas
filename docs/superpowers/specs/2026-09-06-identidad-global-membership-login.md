# Diseño: Identidad global + Membership por tenant, login sin tenantId visible

**Fecha:** 2026-09-06
**Estado:** Aprobado, pendiente de plan de implementación

## Contexto y motivación

El plan `docs/superpowers/plans/2026-09-05-conectar-login-frontend-backend.md` (Task 5) definía un login con un campo `tenantId` (UUID) visible al usuario, junto a email y password. El cliente indicó explícitamente: *"cuando haces el login solo deberia estar el username y su password, internamente en el backend se controla en la logica al cliente"*.

Se investigaron patrones reales de login multi-tenant B2B (Salesforce, Auth0, Okta, WorkOS, Clerk) y se confirmó que el patrón dominante es: el usuario solo ingresa su identificador (email/usuario) y password; el backend resuelve a qué organización pertenece antes de validar credenciales. El prototipo de referencia de UX del propio proyecto (`app-botica/src/views/Login.tsx`, no versionado en git, generado como mockup de producto) confirma la misma intención: un único campo "Usuario, correo o código" + password, sin campo de tenant visible.

La investigación (WorkOS, Clerk, Auth0) también estableció que el patrón de datos correcto para esto en un esquema PostgreSQL multi-tenant de tabla compartida es **separar identidad de membership**, no forzar una constraint `UNIQUE(email)` sobre la tabla `usuario` actual (que ya mezcla `tenant_id` con datos de persona). Razón: si en el futuro una misma persona necesita pertenecer a más de un tenant (ej. empleado que trabaja en varias farmacias del grupo), el modelo separado solo requiere insertar una fila nueva en `membership`; el modelo combinado requeriría una migración destructiva de constraint.

Durante el análisis del código existente se descubrió que `CrearUsuarioHandler`/`Usuario.register` exigen hoy una identidad externa SSO/OIDC obligatoria (`identityProvider`, `identitySubject` con `@NotBlank`) para poder crear cualquier usuario — el login local con password se provisiona en un paso aparte (`provisionCredential`). El cliente indicó que el sistema debe **trabajar sin proveedor externo**: la creación de usuario deja de exigir ni de crear automáticamente una identidad SSO. El mecanismo de `identidad_externa` (vincular SSO a una identidad ya creada) se conserva como capacidad administrativa independiente, ya no obligatoria ni acoplada a la creación del usuario.

## Alcance

### Dentro de alcance

1. Separar el actual agregado/tabla `usuario` (que mezcla identidad de persona y relación con el tenant) en dos conceptos: **Identidad** (persona, email/username únicos globalmente) y **Membership** (relación identidad↔tenant: rol, estado, credenciales).
2. Cambiar el endpoint `POST /auth/login` para que no requiera `tenantId` en el request; el backend resuelve el tenant a partir del email/username. `POST /auth/password/forgot` pierde `tenantId` como consecuencia mecánica obligatoria (comparte `findAccountByLogin`), sin cambios de UX/producto en ese endpoint.
3. Actualizar `LocalAuthService`, `LocalAuthStorePort`, `LocalAuthJdbcAdapter`, `LoginRequest` (backend) en consecuencia.
4. Actualizar `CrearUsuarioHandler`, `IamWritePort`, `IamJpaWriteAdapter`, `IamWriteMapper`, dominio `Usuario`/`UsuarioId` para reflejar la separación.
5. Actualizar `SecurityControlJdbcAdapter` (8 queries que referencian `sch_seguridad.usuario`: `updateUserStatus`, `findExternalIdentities`, `createExternalIdentity`, `deleteExternalIdentity`, `findAssignments`, `revokeAssignment`, `findEffectivePermissions`, `findSessions`) e `IamJdbcReadRepository` (1 referencia) para usar `membership`/`identidad` según corresponda.
6. Nueva migración Flyway en `bootstrap-app/src/main/resources/db/migration/` (numeración V021+) que crea `identidad` y `membership`, migra `credencial_local`, `usuario_rol_ambito`, `sesion_usuario`, `identidad_externa` a referenciar `membership_id`/`identidad_id` según corresponda, y elimina la tabla `usuario` original.
7. Frontend: quitar el campo `tenantId` de `LoginForm.tsx`, `login.schema.ts`, `auth.api.ts`, `AuthSessionProvider.tsx`, y sus tests (`LoginPage.test.tsx`, `AuthSessionProvider.test.tsx`, handler MSW de login).
8. Arreglar en el proceso el bug ya detectado: el fixture UUID de prueba `11111111-1111-1111-1111-111111111111` no pasa la validación estricta de `z.uuid()` de Zod 4 (exige variante RFC 4122); al quitar el campo `tenantId` del formulario este problema desaparece por completo (el campo deja de existir en el schema del frontend).

### Fuera de alcance (explícito)

- **Flujo administrativo de gestión de identidades SSO/OIDC ya existente** (`SecurityControlPort.findExternalIdentities`/`createExternalIdentity`/`deleteExternalIdentity`, expuesto en endpoints propios bajo `/api/v1/usuarios/{userId}/identidades-externas` si existen): no se rediseña. Solo se ajusta su FK (`usuario_id` → `identidad_id`, ver "Modelo de datos") para seguir la migración de la tabla que referencia. Sigue disponible como mecanismo independiente para quien quiera vincular una identidad SSO después de creado el usuario.
- **Reusar una identidad existente para agregarle un membership en otro tenant** vía `CrearUsuarioHandler`: no soportado en este cambio. Si un admin intenta crear un usuario con un email que ya existe como identidad (en cualquier tenant), la operación falla con conflicto (`DUPLICATE_EMAIL`), igual que hoy. "Invitar una identidad existente a mi organización" queda como trabajo futuro.
- **Selector de organización post-login** (para el caso de una identidad con membership en varios tenants): no se implementa. El modelo de datos lo permite a futuro (relación N:1 hoy, ampliable a N:N) pero el login solo soporta hoy el caso de una identidad con un único membership activo.
- **Migración de datos productivos**: no aplica. El proyecto está en fase temprana (`CLAUDE.md`: mayoría de módulos son scaffolds); no existen usuarios reales en ningún entorno que deban preservarse. La migración de esquema puede recrear las tablas sin lógica de deduplicación de emails en producción.
- **UX de `POST /auth/password/forgot`**: no se rediseña su experiencia de usuario ni se le agrega consumidor real en el frontend (el link "¿Olvidaste tu contraseña?" de `LoginForm.tsx` sigue mostrando solo un mensaje informativo, sin llamar al backend). Sin embargo, como `LocalAuthStorePort.findAccountByLogin` cambia de firma a `(String login)` sin tenantId (ver sección "Login"), `LocalAuthService.requestPasswordReset` y `SolicitarRecuperacionPasswordRequest`/`PasswordResetRequest` DEBEN actualizarse en el mismo cambio para no compilar contra un método que ya no existe: pierden el campo `tenantId`, igual que el login. Esto es una consecuencia mecánica obligatoria del cambio de firma, no una ampliación de alcance de producto.

## Diseño

### 1. Modelo de datos

Nueva migración `bootstrap-app/src/main/resources/db/migration/V021__separar_identidad_membership.sql` (la numeración exacta se confirma al momento de implementar, siguiendo la última migración existente en ese directorio). Esta migración reemplaza lo que definió `V013__seguridad_iam.sql` (que permanece intacta en `docs/cadena-farmacias-docs/database/migrations/` como registro histórico del diseño original, sin editarse).

**`sch_seguridad.identidad`** (nueva, reemplaza el rol de "persona" de `usuario`):
- `id BIGINT GENERATED ALWAYS AS IDENTITY`
- `uuid_publico UUID NOT NULL DEFAULT uuidv7()`
- `email CITEXT NOT NULL`
- `username CITEXT` (nullable)
- `tipo_documento VARCHAR(20)`
- `numero_documento VARCHAR(30)`
- `nombres VARCHAR(150)`
- `apellidos VARCHAR(180)`
- `telefono VARCHAR(40)`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMPTZ`
- `PRIMARY KEY (id)`
- `UNIQUE (uuid_publico)`
- `UNIQUE INDEX` global sobre `email` (sin scope de tenant)
- `UNIQUE INDEX` global sobre `username WHERE username IS NOT NULL`
- `UNIQUE INDEX` global sobre `(tipo_documento, numero_documento) WHERE numero_documento IS NOT NULL`

**`sch_seguridad.membership`** (nueva, reemplaza el rol "por-tenant" de `usuario`):
- `id BIGINT GENERATED ALWAYS AS IDENTITY`
- `uuid_publico UUID NOT NULL DEFAULT uuidv7()`
- `tenant_id BIGINT NOT NULL`
- `identidad_id BIGINT NOT NULL`
- `nombre_mostrar VARCHAR(250)`
- `estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'` (mismo check constraint que hoy: `ACTIVO/INACTIVO/BLOQUEADO/SUSPENDIDO`)
- `bloqueado_hasta TIMESTAMPTZ`
- `mfa_requerido BOOLEAN NOT NULL DEFAULT FALSE`
- `requiere_cambio_credencial BOOLEAN NOT NULL DEFAULT FALSE`
- `ultimo_login_at TIMESTAMPTZ`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `updated_at TIMESTAMPTZ`
- `PRIMARY KEY (id)`
- `UNIQUE (uuid_publico)`
- `UNIQUE (tenant_id, id)` (se mantiene, la necesitan las FK compuestas de `usuario_rol_ambito`/`sesion_usuario` si se conserva ese patrón — ver más abajo)
- `UNIQUE (tenant_id, identidad_id)` (una identidad tiene a lo sumo un membership por tenant)
- `FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id)`
- `FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id)`

**Tablas existentes que cambian su FK:**
- `credencial_local`: su columna `usuario_id` pasa a `membership_id`, referenciando `membership(id)`. La contraseña es específica de la relación con esa organización (coherente con que el canal POS/dispositivo confiable ya es por-tenant).
- `usuario_rol_ambito`: su FK compuesta `fk_seg_ura_usuario FOREIGN KEY (tenant_id, usuario_id) REFERENCES sch_seguridad.usuario(tenant_id, id)` pasa a `FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id)`, manteniendo `tenant_id` como columna propia (denormalizada desde `membership.tenant_id`) para no romper el índice `ix_seg_ura_usuario(tenant_id, usuario_id, estado)` ni el índice único `uk_seg_ura_activa`, que se renombran a usar `membership_id` en vez de `usuario_id`.
- `sesion_usuario`: mismo tratamiento — `fk_seg_sesion_usuario` pasa a `(tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id)`, `ix_seg_sesion_usuario` se ajusta igual.
- `identidad_externa`: su columna `usuario_id` pasa a `identidad_id`, referenciando `sch_seguridad.identidad(id)` en vez de `sch_seguridad.usuario(id)`. Un login SSO identifica a la persona, no su relación con un tenant específico. Solo cambia la FK y las 3 queries de `SecurityControlJdbcAdapter` que la usan (`findExternalIdentities`, `createExternalIdentity`, `deleteExternalIdentity`); no se agrega ni modifica lógica de negocio de SSO.

**Tabla eliminada:** `sch_seguridad.usuario` (reemplazada por `identidad` + `membership`). Como no hay datos productivos, la migración puede hacer `DROP TABLE` (en cascada tras recrear las FK de las tablas dependientes) sin lógica de backfill/deduplicación.

### 2. Dominio (módulo `security`)

- Nuevo agregado `Identidad` (`domain/model/Identidad.java`): encapsula email, username, nombres, apellidos, documento, teléfono, con las mismas reglas de normalización/validación que hoy tiene `Usuario.register` para esos campos (lowercase de email/username, trim de espacios, límites de longitud). Sin campos de identidad externa/SSO.
- El agregado `Usuario` se convierte conceptualmente en `Membership`: mantiene tenantId, una referencia a `identidadId`, displayName, estado, mfaRequired, credentialChangeRequired. Se evalúa en el plan de implementación si renombrar la clase y `UsuarioId` a `Membership`/`MembershipId`, o mantener los nombres actuales para minimizar el churn de referencias — decisión técnica de bajo impacto a tomar durante la implementación, no bloquea el diseño.
- `CrearUsuarioCommand` y `CrearUsuarioRequest` (HTTP) pierden los campos `identityProvider`, `identityIssuer`, `identitySubject`, `emailClaim` — dejan de exigirse. Quedan: `tenantId`, `documentType`, `documentNumber`, `firstNames`, `lastNames`, `username`, `email`, `phone`, `displayName`, `credentialChangeRequired`, `mfaRequired`.
- `CrearUsuarioHandler` pasa a orquestar, en una sola operación transaccional: (a) crear la `Identidad` (falla con `DUPLICATE_EMAIL`/`DUPLICATE_USERNAME`/`DUPLICATE_DOCUMENT` si el email/username/documento ya existen globalmente), (b) crear el `Membership` para el tenant indicado. Ya **no** crea automáticamente una fila en `identidad_externa` — ese mecanismo queda como capacidad administrativa separada e independiente (ver "Fuera de alcance"). No se soporta reusar una identidad existente para un tenant distinto (ver "Fuera de alcance").
- `IamWritePort.save(Usuario user)` deja de depender de `IdentidadExternaJpaRepository`/`existsByProviderAndSubject`. Se confirmó que `DUPLICATE_IDENTITY` (en el enum `SaveUsuarioOutcome`) solo se produce y se consume dentro de `IamWritePort`/`CrearUsuarioHandler`/`IamJpaWriteAdapter` — ningún otro código lo referencia — así que se elimina del enum junto con el código que lo producía.
- `IamWritePort.userBelongsToTenant(UUID userId, UUID tenantId)` no cambia de firma: `userId` sigue siendo el identificador público de membership, que ya es por-tenant.

### 3. Login (`LocalAuthService`, `LoginRequest`, frontend)

- `LoginRequest` (backend, `api/dto/request/LoginRequest.java`): quita el campo `tenantId`. Queda `(String login, String password, String channel, UUID deviceId)`.
- `LocalAuthUseCase.LoginCommand`: mismo cambio, quita `tenantId`.
- `LocalAuthStorePort.findAccountByLogin(UUID tenantId, String login)` cambia a `findAccountByLogin(String login)`. La implementación en `LocalAuthJdbcAdapter` hace join `identidad` → `membership` (con `estado`/`bloqueado_hasta` de membership) → `credencial_local` (vía `membership_id`), filtrando por `LOWER(identidad.email) = LOWER(:login) OR LOWER(identidad.username) = LOWER(:login)`, sin filtro de tenant.
- Si el email/username no existe como identidad, o existe pero no tiene ningún membership, se trata igual que hoy: credenciales inválidas genéricas (no se filtra por qué falló, para no exponer existencia de cuentas).
- Si una identidad tuviera más de un membership activo (no debería ocurrir dado que `CrearUsuarioHandler` no permite crear un segundo membership hoy, pero el modelo de datos no lo impide a nivel de BD si se inserta manualmente), `findAccountByLogin` toma el primero según el orden natural de la consulta (sin `ORDER BY` explícito adicional) — no es una decisión de producto relevante mientras no exista una vía en el sistema para crear esa situación; no se agrega manejo especial ni error dedicado para este caso.
- El resto de la lógica de `LocalAuthService.login` (verificación de estado ACTIVO/BLOQUEADO, `bloqueado_hasta`, `mfaRequired`, chequeo de canal POS/dispositivo confiable, emisión de tokens, registro de éxito/fallo) no cambia conceptualmente, solo opera sobre los datos ya unidos desde `membership` en vez de `usuario`.
- El JWT emitido sigue llevando `tenantId` y `userId` como claims — sin cambios en `AuthTokenPort`, `SessionAwareJwtDecoder`, `LocalJwtAuthenticationConverter` ni en la resolución de permisos efectivos (`findEffectivePermissions`), porque `userId` sigue siendo, de cara a esas piezas, el identificador de membership (que ya es por-tenant).

**Frontend (`frontend/apps/erp-web/src/features/auth/`):**
- `schemas/login.schema.ts`: se quita el campo `tenantId` de `loginSchema` y de `LoginCredentials`.
- `components/LoginForm.tsx`: se quita el campo "Identificador de organización" agregado en un intento previo de esta tarea; el formulario queda con email + password (+ checkbox "recordar"), manteniendo el manejo de `submitError` ya diseñado.
- `api/auth.api.ts` y `api/auth-tokens.types.ts` (o donde esté tipado `LoginRequest` del cliente): se quita `tenantId` del body enviado a `POST /auth/login`.
- `model/AuthSessionProvider.tsx`: `authenticate` deja de enviar `tenantId` en el request de login.
- Tests a actualizar: `LoginPage.test.tsx` (quita el llenado del campo tenant), `AuthSessionProvider.test.tsx` (su fixture de `authenticate` ya no necesita `tenantId` en las credenciales), y el handler MSW de login en `test/mocks/handlers.ts` (ya no necesita validar/ignorar un `tenantId` en el body, aunque tampoco lo prohibía).

## Testing

- **Backend:** actualizar/crear tests de `LocalAuthService` (unit) y `IamApiIntegrationTest` (integración, ya referenciado en `CLAUDE.md` como caso de referencia para RBAC) cubriendo: login exitoso sin tenantId, login con email que no existe, login con username que no existe, creación de usuario con email duplicado entre tenants distintos (debe fallar), creación de dos usuarios con emails distintos en tenants distintos (debe funcionar), verificación de que los permisos efectivos y el RBAC por ámbito siguen resolviendo correctamente tras el cambio de FK a `membership_id` (reutilizar el escenario de `grantsEffectiveAuthoritiesFromEstablishmentScopedRoleAssignment` mencionado en `CLAUDE.md`). El test existente `IamApiIntegrationTest` (método que hoy hace `POST /api/v1/usuarios` con `identityProvider`/`identityIssuer`/`identitySubject` en el body, alrededor de la línea 326) debe actualizarse para dejar de enviar esos campos, ya eliminados de `CrearUsuarioRequest`; el test de validación HTTP (`returnsProblemDetailsForInvalidHttpInput`, que hoy envía un body con `identityIssuer`/`identitySubject` vacíos para forzar un 400) debe reescribirse para forzar la validación con los campos que sí siguen siendo obligatorios en el nuevo `CrearUsuarioRequest` (por ejemplo, `tenantId` ausente).
- **Frontend:** los tests ya escritos en esta sesión (`LoginPage.test.tsx`, `AuthSessionProvider.test.tsx`) se ajustan para no usar `tenantId`; deben quedar en verde junto con el resto de la suite (`pnpm test`), typecheck y build.

## Riesgos y consideraciones

- Este cambio toca el módulo `security`, el más maduro y probado del backend — requiere correr `./gradlew.bat check` completo (build + tests + ArchUnit + Spring Modulith verify) antes de considerarlo terminado, no solo los tests nuevos.
- Al eliminar la tabla `usuario` y renombrar FKs, cualquier código o test existente que referencie `sch_seguridad.usuario` directamente (por nombre de tabla, no solo vía JPA/JDBC ports) debe localizarse y actualizarse; se hará una búsqueda exhaustiva en el plan de implementación antes de escribir la migración.
- El diseño de la migración con `DROP TABLE`/recreación asume que ningún otro módulo del backend (fuera de `security`) referencia `sch_seguridad.usuario` — se debe confirmar esto explícitamente al implementar (búsqueda cross-módulo), dado que Spring Modulith podría no detectar una referencia directa a nivel de esquema SQL si algún otro módulo la usara sin pasar por la API pública de `security`.
