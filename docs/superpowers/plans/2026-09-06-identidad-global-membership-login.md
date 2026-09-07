# Identidad global + Membership por tenant, login sin tenantId — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que el login del ERP solo pida email/username + password (sin tenantId visible), separando el modelo de datos de `usuario` en `identidad` (persona, email único global) y `membership` (relación identidad↔tenant), y eliminando la exigencia de una identidad SSO/OIDC obligatoria al crear un usuario.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS dentro del módulo `security` de `service-botica`. Se introduce el agregado `Identidad` (persona) junto al ya existente `Usuario` (que pasa a representar `Membership`, relación con un tenant). La persistencia de escritura sigue en JPA (`persistence/write`), la de lectura en JDBC (`persistence/read`). El login (`LocalAuthService`) resuelve el tenant a partir del email/username sin requerirlo como parámetro.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring JDBC (`JdbcClient`), PostgreSQL 18 + Flyway, JUnit 5, MockMvc; React 19, TypeScript, Zod, React Hook Form, Vitest, MSW.

## Global Constraints

- Backend: `cd service-botica && .\gradlew.bat check --warning-mode all` debe pasar (build + tests + ArchUnit + Spring Modulith verify) antes de dar por terminado el trabajo de backend.
- Frontend: `cd frontend && pnpm check` (lint + typecheck + test + build) debe pasar antes de dar por terminado el trabajo de frontend.
- No modificar `docs/cadena-farmacias-docs/database/migrations/V013__seguridad_iam.sql` (queda como registro histórico). Toda migración nueva va en `service-botica/bootstrap-app/src/main/resources/db/migration/`, numerada después de la última existente.
- El perfil de test del backend (`bootstrap-app/src/test/resources/application-test.yaml`) usa **Postgres real vía Testcontainers**, Flyway habilitado (`spring.flyway.enabled: true`), y `spring.jpa.hibernate.ddl-auto: validate` — Hibernate valida cada `@Entity` contra el esquema real migrado en CADA `@SpringBootTest`, no solo en los tests de integración explícitos. Esto significa que ninguna tarea puede dejar una entidad JPA desalineada del esquema entre commits: la migración SQL y las entidades JPA que dependen de ella deben aterrizar juntas en la misma tarea. (Nota: el CLAUDE.md del repo describe este perfil como H2 en memoria con `ddl-auto: create` — eso está desactualizado; la configuración real verificada en el archivo de recursos de test es la de este párrafo.)
- No se implementa flujo de negocio de SSO/OIDC nuevo; solo se ajusta la FK de `identidad_externa` para que compile y funcione tras la migración.
- No hay datos productivos que preservar: la migración puede recrear tablas sin lógica de deduplicación.
- Seguir el estilo de código existente: sin comentarios explicativos salvo invariantes no obvias, Result pattern para errores esperables, nombres en español para el dominio de negocio (igual que el código ya existente).

---

## Fase 1 — Dominio y persistencia

### Task 1: Agregado `Identidad`

**Files:**
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Identidad.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/IdentidadId.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/IdentidadTest.java`

**Interfaces:**
- Consumes: nada nuevo (usa tipos ya existentes: `Result`, `ErrorDetail`, `AggregateRoot`).
- Produces: `Identidad.register(IdentidadId id, String documentType, String documentNumber, String firstNames, String lastNames, String username, String email, String phone, Instant createdAt): Result<Identidad, ErrorDetail>`, `Identidad.restore(...)`, getters `id()`, `documentType()`, `documentNumber()`, `firstNames()`, `lastNames()`, `username()`, `email()`, `phone()`, `createdAt()`, `updatedAt()`. Usado por Task 3 (`CrearUsuarioHandler`) y Task 4 (persistencia).

Este task es puramente de dominio (`:modules:security:test`), sin dependencia de Spring Boot ni de la base de datos — verificable de forma aislada.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/IdentidadTest.java`:

```java
package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentidadTest {

    @Test
    void registersAndNormalizesAPersonProfile() {
        var result = Identidad.register(
                new IdentidadId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                null, null, "Ada", "Lovelace", "ada",
                " ADMIN@EXAMPLE.TEST ", null,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isSuccess());
        var identity = result.getOrElse(error -> null);
        assertEquals("admin@example.test", identity.email());
        assertEquals("ada", identity.username());
        assertEquals("Ada", identity.firstNames());
        assertEquals("Lovelace", identity.lastNames());
    }

    @Test
    void rejectsAnInvalidEmailWithoutThrowingAnExpectedException() {
        var result = Identidad.register(
                new IdentidadId(UUID.randomUUID()),
                null, null, "Ada", "Lovelace", "ada",
                "invalid-email", null,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isFailure());
        assertEquals("SEC_IDENTIDAD_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresIdAndCreatedAt() {
        var missingId = Identidad.register(
                null, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingId.isFailure());

        var missingCreatedAt = Identidad.register(
                new IdentidadId(UUID.randomUUID()),
                null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, null);
        assertTrue(missingCreatedAt.isFailure());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.domain.model.IdentidadTest"`
Expected: FAIL — `Identidad` e `IdentidadId` no existen todavía (error de compilación).

- [ ] **Step 3: Crear `IdentidadId`**

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/IdentidadId.java`:

```java
package com.softprimesolutions.security.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record IdentidadId(UUID value) {

    public IdentidadId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [ ] **Step 4: Crear `Identidad`**

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Identidad.java`:

```java
package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/** Persona identificable de forma unica en todo el sistema, independiente de su relacion con un tenant. */
public final class Identidad extends AggregateRoot {

    private static final int MAX_EMAIL_LENGTH = 254;

    private final IdentidadId id;
    private final String documentType;
    private final String documentNumber;
    private final String firstNames;
    private final String lastNames;
    private final String username;
    private final String email;
    private final String phone;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Identidad(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Identidad, ErrorDetail> register(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedDocumentType = normalizeNullable(documentType);
        normalizedDocumentType = normalizedDocumentType == null
                ? null : normalizedDocumentType.toUpperCase(Locale.ROOT);
        var normalizedDocumentNumber = normalizeNullable(documentNumber);
        if ((normalizedDocumentType == null) != (normalizedDocumentNumber == null)) {
            return invalid("document", "El tipo y número de documento deben informarse juntos.");
        }
        if (normalizedDocumentType != null && (!hasLength(normalizedDocumentType, 1, 20)
                || !hasLength(normalizedDocumentNumber, 1, 30))) {
            return invalid("document", "El documento no cumple las longitudes permitidas.");
        }

        var normalizedFirstNames = normalizeSpaces(firstNames);
        if (normalizedFirstNames != null && !hasLength(normalizedFirstNames, 1, 150)) {
            return invalid("firstNames", "Los nombres no deben exceder 150 caracteres.");
        }
        var normalizedLastNames = normalizeSpaces(lastNames);
        if (normalizedLastNames != null && !hasLength(normalizedLastNames, 1, 180)) {
            return invalid("lastNames", "Los apellidos no deben exceder 180 caracteres.");
        }

        var normalizedUsername = normalizeNullable(username);
        if (normalizedUsername != null && !hasLength(normalizedUsername, 1, 150)) {
            return invalid("username", "El username no debe exceder 150 caracteres.");
        }
        normalizedUsername = normalizedUsername == null ? null : normalizedUsername.toLowerCase(Locale.ROOT);

        var normalizedEmail = normalizeNullable(email);
        if (!isValidEmail(normalizedEmail)) {
            return invalid("email", "El correo electrónico no tiene un formato válido.");
        }
        normalizedEmail = normalizedEmail.toLowerCase(Locale.ROOT);

        var normalizedPhone = normalizeNullable(phone);
        if (normalizedPhone != null && normalizedPhone.length() > 40) {
            return invalid("phone", "El teléfono no debe exceder 40 caracteres.");
        }

        return Result.success(new Identidad(
                id, normalizedDocumentType, normalizedDocumentNumber, normalizedFirstNames,
                normalizedLastNames, normalizedUsername, normalizedEmail, normalizedPhone,
                createdAt, null));
    }

    public static Identidad restore(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt,
            Instant updatedAt) {
        return new Identidad(
                id, documentType, documentNumber, firstNames, lastNames, username, email, phone,
                createdAt, updatedAt);
    }

    private static Result<Identidad, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_IDENTIDAD_INVALIDA", message, Map.of("field", field)));
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        var normalized = normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static boolean hasLength(String value, int minimum, int maximum) {
        return value != null && value.length() >= minimum && value.length() <= maximum;
    }

    private static boolean isValidEmail(String value) {
        if (!hasLength(value, 3, MAX_EMAIL_LENGTH)) return false;
        var at = value.indexOf('@');
        return at > 0 && at == value.lastIndexOf('@') && at < value.length() - 1;
    }

    public IdentidadId id() { return id; }
    public String documentType() { return documentType; }
    public String documentNumber() { return documentNumber; }
    public String firstNames() { return firstNames; }
    public String lastNames() { return lastNames; }
    public String username() { return username; }
    public String email() { return email; }
    public String phone() { return phone; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.domain.model.IdentidadTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Identidad.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/IdentidadId.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/IdentidadTest.java
git commit -m "feat(security): agregar agregado de dominio Identidad"
```

---

### Task 2: Simplificar `Usuario` para representar Membership (sin identidad SSO)

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Usuario.java`
- Delete: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/ReferenciaIdentidad.java`
- Modify: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/UsuarioTest.java`

**Interfaces:**
- Consumes: `IdentidadId` (Task 1).
- Produces: `Usuario.register(UsuarioId id, TenantId tenantId, IdentidadId identidadId, String displayName, boolean credentialChangeRequired, boolean mfaRequired, Instant createdAt): Result<Usuario, ErrorDetail>`, `Usuario.restore(...)`, getter nuevo `identidadId()`. Se eliminan los getters `identity()`, `documentType()`, `documentNumber()`, `firstNames()`, `lastNames()`, `username()`, `email()`, `phone()` (esos datos ahora viven en `Identidad`). Usado por Task 3 (`CrearUsuarioHandler`), Task 4 (persistencia).

Este task es puramente de dominio, verificable de forma aislada, igual que la Task 1.

- [ ] **Step 1: Escribir el test que falla — reemplazar `UsuarioTest.java` completo**

Reemplazar `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/UsuarioTest.java`:

```java
package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void registersAMembershipForATenant() {
        var result = Usuario.register(
                new UsuarioId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                new IdentidadId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                "  Ada   Lovelace  ",
                false,
                false,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isSuccess());
        var user = result.getOrElse(error -> null);
        assertEquals("Ada Lovelace", user.displayName());
        assertEquals(EstadoUsuario.ACTIVO, user.status());
    }

    @Test
    void requiresTenantAndIdentidadId() {
        var missingTenant = Usuario.register(
                new UsuarioId(UUID.randomUUID()), null,
                new IdentidadId(UUID.randomUUID()), "Ada Lovelace", false, false,
                Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingTenant.isFailure());
        assertEquals("SEC_USUARIO_INVALIDO", missingTenant.fold(value -> null, error -> error.code()));

        var missingIdentidad = Usuario.register(
                new UsuarioId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                null, "Ada Lovelace", false, false,
                Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingIdentidad.isFailure());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.domain.model.UsuarioTest"`
Expected: FAIL — la firma actual de `Usuario.register` no coincide (error de compilación).

- [ ] **Step 3: Reescribir `Usuario.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Usuario.java`:

```java
package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;

/** Relacion (membership) entre una Identidad y un tenant: rol, estado y credenciales locales. */
public final class Usuario extends AggregateRoot {

    private final UsuarioId id;
    private final TenantId tenantId;
    private final IdentidadId identidadId;
    private final String displayName;
    private final boolean credentialChangeRequired;
    private final boolean mfaRequired;
    private final EstadoUsuario status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Usuario(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.identidadId = identidadId;
        this.displayName = displayName;
        this.credentialChangeRequired = credentialChangeRequired;
        this.mfaRequired = mfaRequired;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Usuario, ErrorDetail> register(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de membership es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (identidadId == null) return invalid("identidadId", "La identidad es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedDisplayName = normalizeSpaces(displayName);
        if (normalizedDisplayName != null && normalizedDisplayName.length() > 250) {
            return invalid("displayName", "El nombre para mostrar no debe exceder 250 caracteres.");
        }

        return Result.success(new Usuario(
                id, tenantId, identidadId, normalizedDisplayName, credentialChangeRequired,
                mfaRequired, EstadoUsuario.ACTIVO, createdAt, null));
    }

    public static Usuario restore(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        return new Usuario(
                id, tenantId, identidadId, displayName, credentialChangeRequired, mfaRequired,
                status, createdAt, updatedAt);
    }

    private static Result<Usuario, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_USUARIO_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public UsuarioId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public IdentidadId identidadId() { return identidadId; }
    public String displayName() { return displayName; }
    public boolean credentialChangeRequired() { return credentialChangeRequired; }
    public boolean mfaRequired() { return mfaRequired; }
    public EstadoUsuario status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Eliminar `ReferenciaIdentidad.java` (ya no se usa)**

Run: `rm service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/ReferenciaIdentidad.java`

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.domain.model.UsuarioTest"`
Expected: PASS. Nota: el módulo `security` no compilará completo todavía (otros archivos como `CrearUsuarioHandler`, `IamWriteMapper` referencian la API vieja de `Usuario`) — eso se corrige en la Task 4; no ejecutar `:modules:security:check` completo ni ningún `@SpringBootTest` de `bootstrap-app` hasta terminar la Task 4.

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Usuario.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/valueobject/ReferenciaIdentidad.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/UsuarioTest.java
git commit -m "refactor(security): convertir Usuario en el agregado Membership sin SSO obligatorio"
```

---

### Task 3: `CrearUsuarioCommand`/`CrearUsuarioHandler` sin SSO obligatorio (dominio y aplicación, sin persistencia real todavía)

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/command/CrearUsuarioCommand.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/result/UsuarioResult.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/mapper/IamApplicationMapper.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandler.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandlerTest.java`

**Interfaces:**
- Consumes: `Identidad` (Task 1), `Usuario` (Task 2).
- Produces: `CrearUsuarioCommand(UUID tenantId, String documentType, String documentNumber, String firstNames, String lastNames, String username, String email, String phone, String displayName, boolean credentialChangeRequired, boolean mfaRequired)`, `IamWritePort.save(Identidad identidad, Usuario user): SaveUsuarioOutcome` (nueva firma, sin `DUPLICATE_IDENTITY`). Usado por Task 4 (implementación real del puerto) y Task 7 (controller HTTP).

Esta tarea deja `IamWritePort` con la firma nueva pero SIN una implementación JPA real todavía (eso es la Task 4) — `:modules:security` seguirá sin compilar completo hasta la Task 4 (el módulo `security` no depende de Spring Boot para sus propios tests unitarios, así que esto es seguro; solo `bootstrap-app` con `@SpringBootTest` no debe ejecutarse hasta la Task 4). El test de esta tarea usa un fake del puerto, no la implementación real.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandlerTest.java`:

```java
package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearUsuarioHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void createsAUserWithoutRequiringAnExternalIdentityProvider() {
        var writePort = new FakeIamWritePort();
        var handler = new CrearUsuarioHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-06T10:00:00Z"));

        var result = handler.execute(new CrearUsuarioCommand(
                TENANT_ID, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, "Ada Lovelace", false, false));

        assertTrue(result.isSuccess());
        var created = result.getOrElse(error -> null);
        assertEquals("admin@example.test", created.email());
        assertEquals("Ada Lovelace", created.displayName());
    }

    @Test
    void failsWithConflictWhenEmailAlreadyExists() {
        var writePort = new FakeIamWritePort();
        writePort.saveOutcome = IamWritePort.SaveUsuarioOutcome.DUPLICATE_EMAIL;
        var handler = new CrearUsuarioHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-06T10:00:00Z"));

        var result = handler.execute(new CrearUsuarioCommand(
                TENANT_ID, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, "Ada Lovelace", false, false));

        assertTrue(result.isFailure());
        assertEquals("SEC_EMAIL_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class SequentialIds implements com.softprimesolutions.shared.application.port.IdentifierGenerator {
        private long sequence;

        @Override
        public UUID next() {
            return new UUID(0, ++sequence);
        }
    }

    private static final class FakeIamWritePort implements IamWritePort {
        private IamWritePort.SaveUsuarioOutcome saveOutcome = IamWritePort.SaveUsuarioOutcome.CREATED;

        @Override
        public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
            return saveOutcome;
        }

        @Override
        public SaveRolOutcome save(Rol role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Rol> findRole(UUID roleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean allPermissionsExist(Set<String> permissionCodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tenantExists(UUID tenantId) {
            return true;
        }

        @Override
        public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveAssignmentOutcome save(AsignacionRol assignment) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.application.usecase.command.CrearUsuarioHandlerTest"`
Expected: FAIL — error de compilación, `CrearUsuarioCommand`/`IamWritePort.save` todavía tienen la firma vieja con campos SSO y `Usuario` solo.

- [ ] **Step 3: Reescribir `CrearUsuarioCommand.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/command/CrearUsuarioCommand.java`:

```java
package com.softprimesolutions.security.application.dto.command;

import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearUsuarioCommand(
        UUID tenantId,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String phone,
        String displayName,
        boolean credentialChangeRequired,
        boolean mfaRequired) implements Command<UsuarioResult> {
}
```

- [ ] **Step 4: Reescribir `UsuarioResult.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/result/UsuarioResult.java`:

```java
package com.softprimesolutions.security.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResult(
        UUID id,
        UUID tenantId,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String displayName,
        String phone,
        boolean credentialChangeRequired,
        boolean mfaRequired,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 5: Actualizar `IamWritePort.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java`:

```java
package com.softprimesolutions.security.application.port.out;

import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IamWritePort {

    SaveUsuarioOutcome save(Identidad identidad, Usuario user);

    SaveRolOutcome save(Rol role);

    SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt);

    Optional<Rol> findRole(UUID roleId);

    boolean allPermissionsExist(Set<String> permissionCodes);

    boolean tenantExists(UUID tenantId);

    boolean userBelongsToTenant(UUID userId, UUID tenantId);

    boolean roleBelongsToTenant(UUID roleId, UUID tenantId);

    boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope);

    SaveAssignmentOutcome save(AsignacionRol assignment);

    enum SaveUsuarioOutcome {
        CREATED,
        TENANT_NOT_FOUND,
        DUPLICATE_USERNAME,
        DUPLICATE_EMAIL,
        DUPLICATE_DOCUMENT,
        DUPLICATE_CONSTRAINT
    }
    enum SaveRolOutcome { CREATED, UPDATED, TENANT_NOT_FOUND, DUPLICATE_CODE }
    enum SaveAssignmentOutcome { CREATED, DUPLICATE }
}
```

- [ ] **Step 6: Actualizar `CrearUsuarioHandler.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandler.java`:

```java
package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.CrearUsuarioUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class CrearUsuarioHandler implements CrearUsuarioUseCase {

    private final IamWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearUsuarioHandler(IamWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<UsuarioResult, ApplicationError> execute(CrearUsuarioCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var identidadId = new IdentidadId(identifierGenerator.next());
        var identidad = Identidad.register(
                identidadId, command.documentType(), command.documentNumber(), command.firstNames(),
                command.lastNames(), command.username(), command.email(), command.phone(), clock.now());
        return identidad.fold(
                value -> registerUsuario(value, command),
                this::validationFailure);
    }

    private Result<UsuarioResult, ApplicationError> registerUsuario(Identidad identidad, CrearUsuarioCommand command) {
        var user = Usuario.register(
                new UsuarioId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                identidad.id(),
                command.displayName(),
                command.credentialChangeRequired(),
                command.mfaRequired(),
                clock.now());
        return user.fold(usuario -> persist(identidad, usuario), this::validationFailure);
    }

    private Result<UsuarioResult, ApplicationError> persist(Identidad identidad, Usuario user) {
        return switch (writePort.save(identidad, user)) {
            case CREATED -> Result.success(IamApplicationMapper.toResult(identidad, user));
            case TENANT_NOT_FOUND -> Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
            case DUPLICATE_EMAIL -> Result.failure(new StandardApplicationError(
                    "SEC_EMAIL_DUPLICADO", "El correo electrónico ya está registrado.",
                    ErrorCategory.CONFLICT, Map.of("email", identidad.email())));
            case DUPLICATE_USERNAME -> Result.failure(new StandardApplicationError(
                    "SEC_USERNAME_DUPLICADO", "El username ya está registrado.", ErrorCategory.CONFLICT));
            case DUPLICATE_DOCUMENT -> Result.failure(new StandardApplicationError(
                    "SEC_DOCUMENTO_DUPLICADO", "El documento ya está registrado.", ErrorCategory.CONFLICT));
            case DUPLICATE_CONSTRAINT -> Result.failure(new StandardApplicationError(
                    "SEC_USUARIO_DUPLICADO",
                    "El correo, username o documento ya pertenecen a otra identidad.",
                    ErrorCategory.CONFLICT));
        };
    }

    private Result<UsuarioResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 7: Actualizar `IamApplicationMapper.java`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/mapper/IamApplicationMapper.java`, reemplazar el método `toResult(Usuario user)` (y su import de `Usuario` si hace falta agregar el de `Identidad`):

```java
    public static UsuarioResult toResult(Identidad identidad, Usuario user) {
        return new UsuarioResult(
                user.id().value(),
                user.tenantId().value(),
                identidad.documentType(),
                identidad.documentNumber(),
                identidad.firstNames(),
                identidad.lastNames(),
                identidad.username(),
                identidad.email(),
                user.displayName(),
                identidad.phone(),
                user.credentialChangeRequired(),
                user.mfaRequired(),
                user.status().name(),
                user.createdAt(),
                user.updatedAt());
    }
```

Agregar el import `com.softprimesolutions.security.domain.model.Identidad` al inicio del archivo.

- [ ] **Step 8: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.application.usecase.command.CrearUsuarioHandlerTest"`
Expected: PASS. El módulo `security` todavía no compila completo (`IamJpaWriteAdapter`/`IamWriteMapper` siguen referenciando la firma vieja de `IamWritePort.save` y `UsuarioJpaEntity`) — eso se corrige en la Task 4. No ejecutar `:modules:security:compileJava`/`check` completo ni ningún `@SpringBootTest` de `bootstrap-app` todavía.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/command/CrearUsuarioCommand.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/result/UsuarioResult.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/mapper/IamApplicationMapper.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandler.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/CrearUsuarioHandlerTest.java
git commit -m "feat(security): crear usuario sin exigir identidad SSO obligatoria"
```

---

### Task 4: Persistencia completa — migración Flyway, entidades JPA, `IamJpaWriteAdapter`, `IdentidadExternaJpaEntity`

**Esta es la tarea más grande del plan.** Fusiona lo que en el diseño original eran varias tareas separadas, porque este proyecto no tiene ningún test que arranque sin validar JPA contra el esquema real: `bootstrap-app/src/test/resources/application-test.yaml` usa Postgres real vía Testcontainers, Flyway habilitado, y `spring.jpa.hibernate.ddl-auto: validate` — Hibernate valida **todas** las `@Entity` del classpath contra el esquema migrado en **cada** `@SpringBootTest`, no solo en tests explícitos de esa entidad. Por eso la migración SQL y las entidades JPA que dependen de ella (incluida `IdentidadExternaJpaEntity`, que no es nueva pero cuya columna cambia de nombre) deben aterrizar juntas: cualquier corte intermedio deja el contexto de Spring sin poder arrancar.

**Files:**
- Create: `service-botica/bootstrap-app/src/main/resources/db/migration/V021__separar_identidad_membership.sql`
- Test: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadJpaEntity.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/MembershipJpaEntity.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/IdentidadJpaRepository.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/MembershipJpaRepository.java`
- Delete: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/UsuarioJpaEntity.java`
- Delete: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/UsuarioJpaRepository.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadExternaJpaEntity.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/mapper/IamWriteMapper.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/AsignacionRolJpaEntity.java`

**Interfaces:**
- Consumes: `Identidad`/`Usuario` (Tasks 1-2), `IamWritePort.save(Identidad, Usuario)` (Task 3).
- Produces: esquema `sch_seguridad.identidad`/`sch_seguridad.membership`, `IamJpaWriteAdapter` completo y compilable, `:bootstrap-app` capaz de arrancar `@SpringBootTest` de nuevo. Usado por todas las tareas siguientes (Fases 2-6).

- [ ] **Step 1: Verificar el número de migración siguiente disponible**

Run: `ls service-botica/bootstrap-app/src/main/resources/db/migration/`
Expected: la migración más alta hoy es `V020__local_authentication_jwt.sql`. Usar `V021` como siguiente número. Si al ejecutar este paso ya existe un `V021`, usar el siguiente número libre y ajustar el nombre de archivo en los pasos restantes de esta tarea.

- [ ] **Step 2: Escribir el test de integración que falla — verifica que las tablas nuevas existen y las antiguas FKs ya no**

Antes de escribir el test, leer `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java` completo para copiar EXACTAMENTE sus anotaciones de clase (`@SpringBootTest`, `@ActiveProfiles`, `@Import` de configuración de Testcontainers, etc.) — no asumir un nombre de perfil o configuración sin verificarlo contra ese archivo real.

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java` con esas mismas anotaciones de clase y:

```java
package com.softprimesolutions.security.db;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class MigrationV021Test {
    // Copiar aqui las anotaciones de clase exactas de IamApiIntegrationTest
    // (@SpringBootTest, @ActiveProfiles, @Import, etc.)

    @Autowired
    private DataSource dataSource;

    @Test
    void createsIdentidadAndMembershipTablesAndDropsUsuario() {
        var jdbc = JdbcClient.create(dataSource);

        var identidadExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'identidad'
                        """)
                .query(Long.class).single();
        assertThat(identidadExists).isEqualTo(1L);

        var membershipExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'membership'
                        """)
                .query(Long.class).single();
        assertThat(membershipExists).isEqualTo(1L);

        var usuarioExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'usuario'
                        """)
                .query(Long.class).single();
        assertThat(usuarioExists).isEqualTo(0L);

        var credencialLocalHasMembershipId = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'credencial_local'
                           AND column_name = 'membership_id'
                        """)
                .query(Long.class).single();
        assertThat(credencialLocalHasMembershipId).isEqualTo(1L);

        var identidadExternaHasIdentidadId = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'identidad_externa'
                           AND column_name = 'identidad_id'
                        """)
                .query(Long.class).single();
        assertThat(identidadExternaHasIdentidadId).isEqualTo(1L);

        var emailUniqueGlobal = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_indexes
                         WHERE schemaname = 'sch_seguridad' AND tablename = 'identidad'
                           AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%email%'
                        """)
                .query(Long.class).single();
        assertThat(emailUniqueGlobal).isEqualTo(1L);
    }
}
```

- [ ] **Step 3: Ejecutar y verificar que el test falla**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.db.MigrationV021Test"`
Expected: FAIL — la tabla `identidad`/`membership` no existen todavía, o `usuario` sigue existiendo. Confirmar que el Spring context SÍ arranca correctamente en este punto (falla solo la aserción, no el bootstrap) — si el context ya falla a arrancar aquí, algo previo (Tasks 1-3) rompió la compilación del módulo `security` de forma que afecta a `bootstrap-app`; detenerse y reportar BLOCKED en vez de continuar.

- [ ] **Step 4: Escribir la migración `V021__separar_identidad_membership.sql`**

Crear `service-botica/bootstrap-app/src/main/resources/db/migration/V021__separar_identidad_membership.sql`:

```sql
-- Separa la tabla usuario (persona + relacion con tenant) en identidad (persona, global)
-- y membership (relacion identidad-tenant: rol, estado, credenciales).
-- No hay datos productivos que preservar: se recrea el esquema sin backfill.

CREATE TABLE sch_seguridad.identidad (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    email CITEXT NOT NULL,
    username CITEXT,
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(30),
    nombres VARCHAR(150),
    apellidos VARCHAR(180),
    telefono VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_identidad PRIMARY KEY (id),
    CONSTRAINT uk_seg_identidad_uuid UNIQUE (uuid_publico)
);

CREATE UNIQUE INDEX uk_seg_identidad_email ON sch_seguridad.identidad(email);
CREATE UNIQUE INDEX uk_seg_identidad_username ON sch_seguridad.identidad(username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX uk_seg_identidad_documento ON sch_seguridad.identidad(tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL;

CREATE TABLE sch_seguridad.membership (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    tenant_id BIGINT NOT NULL,
    identidad_id BIGINT NOT NULL,
    nombre_mostrar VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    bloqueado_hasta TIMESTAMPTZ,
    mfa_requerido BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_cambio_credencial BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT pk_seg_membership PRIMARY KEY (id),
    CONSTRAINT uk_seg_membership_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_seg_membership_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_seg_membership_tenant_identidad UNIQUE (tenant_id, identidad_id),
    CONSTRAINT fk_seg_membership_tenant FOREIGN KEY (tenant_id) REFERENCES sch_farmacia.tenant(id),
    CONSTRAINT fk_seg_membership_identidad FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id),
    CONSTRAINT ck_seg_membership_estado CHECK (estado IN ('ACTIVO','INACTIVO','BLOQUEADO','SUSPENDIDO'))
);

ALTER TABLE sch_seguridad.credencial_local DROP CONSTRAINT IF EXISTS fk_seg_credencial_usuario;
ALTER TABLE sch_seguridad.credencial_local RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.credencial_local
    ADD CONSTRAINT fk_seg_credencial_membership FOREIGN KEY (membership_id) REFERENCES sch_seguridad.membership(id);

ALTER TABLE sch_seguridad.usuario_rol_ambito DROP CONSTRAINT fk_seg_ura_usuario;
ALTER TABLE sch_seguridad.usuario_rol_ambito RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.usuario_rol_ambito
    ADD CONSTRAINT fk_seg_ura_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP INDEX IF EXISTS sch_seguridad.ix_seg_ura_usuario;
CREATE INDEX ix_seg_ura_membership ON sch_seguridad.usuario_rol_ambito(tenant_id, membership_id, estado);

DROP INDEX IF EXISTS sch_seguridad.uk_seg_ura_activa;
CREATE UNIQUE INDEX uk_seg_ura_activa ON sch_seguridad.usuario_rol_ambito(
    tenant_id, membership_id, rol_id, tipo_ambito,
    COALESCE(empresa_id, 0), COALESCE(establecimiento_id, 0),
    COALESCE(almacen_id, 0), COALESCE(terminal_id, 0)
) WHERE estado = 'ACTIVO';

ALTER TABLE sch_seguridad.sesion_usuario DROP CONSTRAINT fk_seg_sesion_usuario;
ALTER TABLE sch_seguridad.sesion_usuario RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.sesion_usuario
    ADD CONSTRAINT fk_seg_sesion_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP INDEX IF EXISTS sch_seguridad.ix_seg_sesion_usuario;
CREATE INDEX ix_seg_sesion_membership ON sch_seguridad.sesion_usuario(tenant_id, membership_id, estado);

ALTER TABLE sch_seguridad.identidad_externa DROP CONSTRAINT fk_seg_identidad_usuario;
ALTER TABLE sch_seguridad.identidad_externa RENAME COLUMN usuario_id TO identidad_id;
ALTER TABLE sch_seguridad.identidad_externa
    ADD CONSTRAINT fk_seg_identidad_externa_identidad
        FOREIGN KEY (identidad_id) REFERENCES sch_seguridad.identidad(id);

-- token_refresh y token_recuperacion_password (V020) tambien referencian usuario directamente
-- y deben migrar igual que credencial_local/sesion_usuario para poder eliminar sch_seguridad.usuario
-- sin dejar objetos dependientes.
ALTER TABLE sch_seguridad.token_refresh DROP CONSTRAINT fk_seg_refresh_usuario;
ALTER TABLE sch_seguridad.token_refresh RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.token_refresh
    ADD CONSTRAINT fk_seg_refresh_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

ALTER TABLE sch_seguridad.token_recuperacion_password DROP CONSTRAINT fk_seg_recuperacion_usuario;
ALTER TABLE sch_seguridad.token_recuperacion_password RENAME COLUMN usuario_id TO membership_id;
ALTER TABLE sch_seguridad.token_recuperacion_password
    ADD CONSTRAINT fk_seg_recuperacion_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES sch_seguridad.membership(tenant_id, id);

DROP TABLE sch_seguridad.usuario;
```

Nota: si al ejecutar este paso `fk_seg_refresh_usuario`/`fk_seg_recuperacion_usuario` u otro nombre de constraint no coincide exactamente con el real (verificar contra `V020__local_authentication_jwt.sql`), usar el nombre real de esa migración en vez de este literal.

- [ ] **Step 5: Crear `IdentidadJpaEntity.java`**

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadJpaEntity.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identidad", schema = "sch_seguridad")
public class IdentidadJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(columnDefinition = "citext", nullable = false)
    private String email;

    @Column(columnDefinition = "citext")
    private String username;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @Column(length = 150)
    private String nombres;

    @Column(length = 180)
    private String apellidos;

    @Column(length = 40)
    private String telefono;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected IdentidadJpaEntity() {
    }

    public IdentidadJpaEntity(
            UUID uuidPublico, String email, String username, String tipoDocumento,
            String numeroDocumento, String nombres, String apellidos, String telefono,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.email = email;
        this.username = username;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getTelefono() { return telefono; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 6: Crear `MembershipJpaEntity.java`**

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/MembershipJpaEntity.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership", schema = "sch_seguridad")
public class MembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "identidad_id", nullable = false)
    private Long identidadId;

    @Column(name = "nombre_mostrar", length = 250)
    private String nombreMostrar;

    @Column(name = "requiere_cambio_credencial", nullable = false)
    private boolean requiereCambioCredencial;

    @Column(name = "mfa_requerido", nullable = false)
    private boolean mfaRequerido;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected MembershipJpaEntity() {
    }

    public MembershipJpaEntity(
            UUID uuidPublico, Long tenantId, Long identidadId, String nombreMostrar,
            boolean requiereCambioCredencial, boolean mfaRequerido, String estado,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.identidadId = identidadId;
        this.nombreMostrar = nombreMostrar;
        this.requiereCambioCredencial = requiereCambioCredencial;
        this.mfaRequerido = mfaRequerido;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getIdentidadId() { return identidadId; }
    public String getNombreMostrar() { return nombreMostrar; }
    public boolean isRequiereCambioCredencial() { return requiereCambioCredencial; }
    public boolean isMfaRequerido() { return mfaRequerido; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 7: Crear los repositorios JPA nuevos**

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/IdentidadJpaRepository.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.IdentidadJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadJpaRepository extends JpaRepository<IdentidadJpaEntity, Long> {
    Optional<IdentidadJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
```

Crear `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/MembershipJpaRepository.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.MembershipJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {
    Optional<MembershipJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndIdentidadId(Long tenantId, Long identidadId);
}
```

- [ ] **Step 8: Eliminar `UsuarioJpaEntity.java` y `UsuarioJpaRepository.java`**

Run: `rm service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/UsuarioJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/UsuarioJpaRepository.java`

- [ ] **Step 9: Actualizar `IdentidadExternaJpaEntity.java` — columna `usuario_id` pasa a `identidad_id`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadExternaJpaEntity.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "identidad_externa", schema = "sch_seguridad")
public class IdentidadExternaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identidad_id", nullable = false)
    private Long identidadId;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(length = 500)
    private String issuer;

    @Column(name = "email_claim", columnDefinition = "citext")
    private String emailClaim;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentidadExternaJpaEntity() {
    }

    public IdentidadExternaJpaEntity(
            Long identidadId, String provider, String subject, String issuer,
            String emailClaim, Instant createdAt) {
        this.identidadId = identidadId;
        this.provider = provider;
        this.subject = subject;
        this.issuer = issuer;
        this.emailClaim = emailClaim;
        this.createdAt = createdAt;
    }
}
```

Nota: este archivo ya no se usa desde `IamJpaWriteAdapter.save` (Step 12 de esta tarea deja de crear identidades externas automáticamente al registrar un usuario — ver Task 3, que ya quitó ese acoplamiento). Sigue existiendo para el CRUD administrativo de `SecurityControlPort`, que se actualiza en una tarea posterior de la Fase 5 del plan.

- [ ] **Step 10: Actualizar `AsignacionRolJpaEntity.java` — columna `usuario_id` pasa a `membership_id`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/AsignacionRolJpaEntity.java`, cambiar:

```java
    @Column(name = "membership_id", nullable = false)
    private Long usuarioId;
```

(Solo el valor del atributo `name` en la anotación `@Column` cambia de `"usuario_id"` a `"membership_id"`; el nombre del campo Java `usuarioId` y sus getters/uso interno no cambian en esta tarea, para no ampliar el alcance del refactor de nombres más allá de lo que exige la migración.)

- [ ] **Step 11: Actualizar `IamWriteMapper.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/mapper/IamWriteMapper.java` completo:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.mapper;

import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.EstadoRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.AsignacionRolJpaEntity;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.IdentidadJpaEntity;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.MembershipJpaEntity;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.RolJpaEntity;
import java.util.Set;
import java.util.UUID;

public final class IamWriteMapper {

    private IamWriteMapper() {
    }

    public static IdentidadJpaEntity toEntity(Identidad identidad) {
        return new IdentidadJpaEntity(
                identidad.id().value(), identidad.email(), identidad.username(), identidad.documentType(),
                identidad.documentNumber(), identidad.firstNames(), identidad.lastNames(),
                identidad.phone(), identidad.createdAt(), identidad.updatedAt());
    }

    public static MembershipJpaEntity toEntity(Usuario user, Long tenantId, Long identidadId) {
        return new MembershipJpaEntity(
                user.id().value(), tenantId, identidadId, user.displayName(),
                user.credentialChangeRequired(), user.mfaRequired(),
                user.status().name(), user.createdAt(), user.updatedAt());
    }

    public static RolJpaEntity toEntity(Rol role, Long tenantId) {
        return new RolJpaEntity(
                role.id().value(), tenantId, role.code(), role.name(), role.description(),
                role.roleType().name(), role.systemRole(), role.status().name(),
                role.createdAt(), role.updatedAt());
    }

    public static Rol toDomain(RolJpaEntity entity, UUID tenantUuid, Set<String> permissionCodes) {
        return Rol.restore(
                new RolId(entity.getUuidPublico()), new TenantId(tenantUuid),
                entity.getCodigo(), entity.getNombre(), entity.getDescripcion(),
                com.softprimesolutions.security.domain.model.TipoRol.valueOf(entity.getTipoRol()),
                entity.isEsSistema(), permissionCodes, EstadoRol.valueOf(entity.getEstado()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static AsignacionRolJpaEntity toEntity(
            AsignacionRol assignment,
            Long tenantId,
            Long usuarioId,
            Long rolId,
            Long empresaId,
            Long establecimientoId,
            Long almacenId,
            Long terminalId) {
        return new AsignacionRolJpaEntity(
                assignment.id(), tenantId, usuarioId, rolId, assignment.scope().type().name(),
                empresaId, establecimientoId, almacenId, terminalId,
                assignment.validFrom(), assignment.validUntil(), assignment.status().name(),
                assignment.createdBy(), assignment.createdAt());
    }
}
```

- [ ] **Step 12: Reescribir `IamJpaWriteAdapter.java` completo**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java` completo:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.adapter;

import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.infrastructure.persistence.write.mapper.IamWriteMapper;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.AsignacionRolJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.IdentidadJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.MembershipJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.PermisoJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.RolJpaRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IamJpaWriteAdapter implements IamWritePort {

    private final IdentidadJpaRepository identidadRepository;
    private final MembershipJpaRepository membershipRepository;
    private final RolJpaRepository roleRepository;
    private final PermisoJpaRepository permissionRepository;
    private final AsignacionRolJpaRepository assignmentRepository;
    private final JdbcClient jdbcClient;

    public IamJpaWriteAdapter(
            IdentidadJpaRepository identidadRepository,
            MembershipJpaRepository membershipRepository,
            RolJpaRepository roleRepository,
            PermisoJpaRepository permissionRepository,
            AsignacionRolJpaRepository assignmentRepository,
            JdbcClient jdbcClient) {
        this.identidadRepository = identidadRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
        var tenantId = findTenantId(user.tenantId().value());
        if (tenantId.isEmpty()) return SaveUsuarioOutcome.TENANT_NOT_FOUND;
        if (identidadRepository.existsByEmail(identidad.email())) return SaveUsuarioOutcome.DUPLICATE_EMAIL;
        if (identidad.username() != null && identidadRepository.existsByUsername(identidad.username())) {
            return SaveUsuarioOutcome.DUPLICATE_USERNAME;
        }
        if (identidad.documentNumber() != null && identidadRepository.existsByTipoDocumentoAndNumeroDocumento(
                identidad.documentType(), identidad.documentNumber())) {
            return SaveUsuarioOutcome.DUPLICATE_DOCUMENT;
        }
        try {
            var identidadEntity = identidadRepository.saveAndFlush(IamWriteMapper.toEntity(identidad));
            membershipRepository.saveAndFlush(
                    IamWriteMapper.toEntity(user, tenantId.get(), identidadEntity.getId()));
            return SaveUsuarioOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveUsuarioOutcome.DUPLICATE_CONSTRAINT;
        }
    }

    @Override
    @Transactional
    public SaveRolOutcome save(Rol role) {
        var tenantId = findTenantId(role.tenantId().value());
        if (tenantId.isEmpty()) return SaveRolOutcome.TENANT_NOT_FOUND;
        var existing = roleRepository.findByUuidPublico(role.id().value());
        if (existing.isEmpty() && roleRepository.existsByTenantIdAndCodigo(tenantId.get(), role.code())) {
            return SaveRolOutcome.DUPLICATE_CODE;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_seguridad.rol
                                   SET nombre = :name, descripcion = :description, tipo_rol = :roleType,
                                       es_sistema = :systemRole, estado = :status, updated_at = :updatedAt
                                 WHERE uuid_publico = :roleId
                                """)
                        .param("name", role.name())
                        .param("description", role.description())
                        .param("roleType", role.roleType().name())
                        .param("systemRole", role.systemRole())
                        .param("status", role.status().name())
                        .param("updatedAt", toOffsetDateTime(role.updatedAt()))
                        .param("roleId", role.id().value())
                        .update();
                return SaveRolOutcome.UPDATED;
            }
            roleRepository.saveAndFlush(IamWriteMapper.toEntity(role, tenantId.get()));
            return SaveRolOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveRolOutcome.DUPLICATE_CODE;
        }
    }

    @Override
    @Transactional
    public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
        var entity = roleRepository.findByUuidPublico(role.id().value());
        if (entity.isEmpty()) return SaveRolOutcome.DUPLICATE_CODE;

        jdbcClient.sql("DELETE FROM sch_seguridad.rol_permiso WHERE rol_id = :roleId")
                .param("roleId", entity.get().getId())
                .update();
        for (var permissionCode : role.permissionCodes()) {
            jdbcClient.sql("""
                            INSERT INTO sch_seguridad.rol_permiso
                                (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                            SELECT :tenantId, :roleId, p.id, 'ACTIVO', :grantedAt, :grantedBy
                              FROM sch_seguridad.permiso p
                             WHERE p.codigo = :permissionCode
                            """)
                    .param("tenantId", entity.get().getTenantId())
                    .param("roleId", entity.get().getId())
                    .param("grantedAt", toOffsetDateTime(grantedAt))
                    .param("grantedBy", grantedBy)
                    .param("permissionCode", permissionCode)
                    .update();
        }
        jdbcClient.sql("UPDATE sch_seguridad.rol SET updated_at = :updatedAt WHERE id = :roleId")
                .param("updatedAt", toOffsetDateTime(grantedAt))
                .param("roleId", entity.get().getId())
                .update();
        return SaveRolOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> findRole(UUID roleId) {
        return roleRepository.findByUuidPublico(roleId).flatMap(entity ->
                findTenantUuid(entity.getTenantId()).map(tenantUuid ->
                        IamWriteMapper.toDomain(entity, tenantUuid, findPermissionCodes(entity.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPermissionsExist(Set<String> permissionCodes) {
        return permissionCodes.isEmpty()
                || permissionRepository.countByCodigoIn(permissionCodes) == permissionCodes.size();
    }

    @Override
    public boolean tenantExists(UUID tenantId) {
        return findTenantId(tenantId).isPresent();
    }

    @Override
    public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
        return membershipRepository.findByUuidPublico(userId)
                .map(membership -> findTenantId(tenantId).filter(membership.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    @Override
    public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
        return roleRepository.findByUuidPublico(roleId)
                .map(role -> findTenantId(tenantId).filter(role.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    @Override
    public boolean scopeExists(
            UUID tenantId,
            com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional scope) {
        return resolveScope(tenantId, scope).isPresent();
    }

    @Override
    @Transactional
    public SaveAssignmentOutcome save(AsignacionRol assignment) {
        var tenantId = findTenantId(assignment.tenantId().value());
        var user = membershipRepository.findByUuidPublico(assignment.userId().value());
        var role = roleRepository.findByUuidPublico(assignment.roleId().value());
        var scope = resolveScope(assignment.tenantId().value(), assignment.scope());
        if (tenantId.isEmpty() || user.isEmpty() || role.isEmpty() || scope.isEmpty()) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
        var resolvedScope = scope.get();
        var duplicateCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                          FROM sch_seguridad.usuario_rol_ambito
                         WHERE tenant_id = :tenantId
                           AND membership_id = :userId
                           AND rol_id = :roleId
                           AND tipo_ambito = :scopeType
                           AND COALESCE(empresa_id, 0) = :companyId
                           AND COALESCE(establecimiento_id, 0) = :establishmentId
                           AND COALESCE(almacen_id, 0) = :warehouseId
                           AND COALESCE(terminal_id, 0) = :terminalId
                           AND estado = 'ACTIVO'
                        """)
                .param("tenantId", tenantId.get())
                .param("userId", user.get().getId())
                .param("roleId", role.get().getId())
                .param("scopeType", assignment.scope().type().name())
                .param("companyId", valueOrZero(resolvedScope.companyId()))
                .param("establishmentId", valueOrZero(resolvedScope.establishmentId()))
                .param("warehouseId", valueOrZero(resolvedScope.warehouseId()))
                .param("terminalId", valueOrZero(resolvedScope.terminalId()))
                .query(Long.class)
                .single();
        if (duplicateCount > 0) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
        try {
            assignmentRepository.saveAndFlush(IamWriteMapper.toEntity(
                    assignment, tenantId.get(), user.get().getId(), role.get().getId(),
                    resolvedScope.companyId(), resolvedScope.establishmentId(),
                    resolvedScope.warehouseId(), resolvedScope.terminalId()));
            return SaveAssignmentOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<UUID> findTenantUuid(Long tenantId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_farmacia.tenant WHERE id = :tenantId")
                .param("tenantId", tenantId)
                .query(UUID.class)
                .optional();
    }

    private Set<String> findPermissionCodes(Long roleId) {
        return jdbcClient.sql("""
                        SELECT p.codigo
                          FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE rp.rol_id = :roleId AND rp.estado = 'ACTIVO'
                         ORDER BY p.codigo
                        """)
                .param("roleId", roleId)
                .query(String.class)
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<ResolvedScope> resolveScope(
            UUID tenantUuid,
            com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional scope) {
        if (tenantUuid == null || scope == null || scope.type() == null) return Optional.empty();
        return switch (scope.type()) {
            case GLOBAL -> findTenantId(tenantUuid).map(ignored -> new ResolvedScope(null, null, null, null));
            case EMPRESA -> jdbcClient.sql("""
                            SELECT e.id
                              FROM sch_farmacia.empresa_operadora e
                              JOIN sch_farmacia.tenant t ON t.id = e.tenant_id
                             WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :companyUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .query(Long.class)
                    .optional()
                    .map(companyId -> new ResolvedScope(companyId, null, null, null));
            case ESTABLECIMIENTO -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id
                              FROM sch_farmacia.establecimiento_farmaceutico s
                              JOIN sch_farmacia.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"), null, null))
                    .optional();
            case ALMACEN -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id, a.id AS warehouse_id
                              FROM sch_farmacia.almacen a
                              JOIN sch_farmacia.establecimiento_farmaceutico s
                                ON s.id = a.establecimiento_id AND s.empresa_id = a.empresa_id AND s.tenant_id = a.tenant_id
                              JOIN sch_farmacia.empresa_operadora e ON e.id = a.empresa_id AND e.tenant_id = a.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                               AND a.uuid_publico = :warehouseUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .param("warehouseUuid", scope.warehouseId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"),
                            rs.getLong("warehouse_id"), null))
                    .optional();
            case TERMINAL -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id, p.id AS terminal_id
                              FROM sch_farmacia.terminal_pos p
                              JOIN sch_farmacia.establecimiento_farmaceutico s
                                ON s.id = p.establecimiento_id AND s.empresa_id = p.empresa_id AND s.tenant_id = p.tenant_id
                              JOIN sch_farmacia.empresa_operadora e ON e.id = p.empresa_id AND e.tenant_id = p.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = p.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                               AND p.uuid_publico = :terminalUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .param("terminalUuid", scope.terminalId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"),
                            null, rs.getLong("terminal_id")))
                    .optional();
        };
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record ResolvedScope(Long companyId, Long establishmentId, Long warehouseId, Long terminalId) {
    }
}
```

- [ ] **Step 13: Compilar el módulo `security` completo**

Run: `cd service-botica && .\gradlew.bat :modules:security:compileJava`
Expected: BUILD SUCCESSFUL. Si hay errores, revisar que `IdentidadExternaJpaRepository`/`IdentidadExternaJpaEntity` no queden referenciados en algún otro sitio con la firma vieja (por ejemplo el constructor de `IamJpaWriteAdapter` ya no recibe `IdentidadExternaJpaRepository` como parámetro — verificar que ningún test unitario existente instancie `IamJpaWriteAdapter` con la lista de argumentos vieja).

- [ ] **Step 14: Ejecutar el test de migración y verificar que pasa (GREEN)**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.db.MigrationV021Test"`
Expected: PASS — el Spring context arranca completo (JPA valida limpio contra el esquema migrado) y las aserciones de esquema pasan.

- [ ] **Step 15: Ejecutar el resto de tests de `:modules:security` y confirmar que no se rompió nada de las Tasks 1-3**

Run: `cd service-botica && .\gradlew.bat :modules:security:test`
Expected: BUILD SUCCESSFUL — todos los tests unitarios existentes y los de las Tasks 1-3 en verde.

- [ ] **Step 16: Ejecutar el pre-existente `IamApiIntegrationTest` y confirmar que sigue arrancando (aunque pueda fallar en aserciones de negocio que se corrigen en tareas posteriores de la Fase 5)**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.api.IamApiIntegrationTest"`
Expected: el Spring context debe arrancar sin `SchemaManagementException`. Es aceptable (y esperado) que algún test de este archivo falle por aserciones de negocio relacionadas con los campos SSO que ya no existen en `CrearUsuarioRequest` — eso se corrige explícitamente en una tarea posterior de la Fase 5 dedicada a actualizar este archivo de test. Lo que NO es aceptable en este punto es que el contexto de Spring falle a arrancar; si eso ocurre, hay un problema real en la migración o las entidades de esta tarea y debe resolverse aquí, no diferirse.

- [ ] **Step 17: Commit**

```bash
git add service-botica/bootstrap-app/src/main/resources/db/migration/V021__separar_identidad_membership.sql service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/MembershipJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/IdentidadJpaRepository.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/MembershipJpaRepository.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/UsuarioJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/UsuarioJpaRepository.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/IdentidadExternaJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/entity/AsignacionRolJpaEntity.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/mapper/IamWriteMapper.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java
git commit -m "feat(security): migrar esquema y persistencia a identidad global + membership por tenant"
```

---
## Fase 2 — Login sin tenantId

### Task 5: `LocalAuthStorePort.findAccountByLogin` sin tenantId

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/LocalAuthStorePort.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/LocalAuthUseCase.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/LocalAuthService.java`
- Modify: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/LocalAuthServiceTest.java`

**Interfaces:**
- Consumes: nada nuevo.
- Produces: `LocalAuthStorePort.findAccountByLogin(String login): Optional<LocalAccount>` (sin tenantId), `LocalAuthUseCase.LoginCommand` sin `tenantId`, `LocalAuthUseCase.PasswordResetRequest(String login)` sin `tenantId`. Usado por Task 6 (adapter JDBC), Task 7 (controller/DTOs HTTP).

- [ ] **Step 1: Actualizar el test que falla — `LocalAuthServiceTest.java`**

En `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/LocalAuthServiceTest.java`, reemplazar cada construcción de `new LoginCommand(TENANT_ID, ...)` quitando `TENANT_ID` como primer argumento. Por ejemplo, el primer test:

```java
    @Test
    void logsInAndPersistsARefreshBackedSession() {
        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, true, false));
        var longIp = "1".repeat(120);
        var longAgent = "agent".repeat(250);

        var result = service.login(new LoginCommand(
                " local.admin ", CURRENT_PASSWORD, " web ", longIp, longAgent, null));
```

Aplicar el mismo cambio (quitar `TENANT_ID` como primer argumento posicional de `LoginCommand`) en todos los usos dentro del archivo: `returnsTheSameGenericErrorForUnknownAndIncorrectCredentials`, `rejectsMissingCommandsAndRequiredValuesWithoutCallingPersistence` (incluyendo el caso `new LoginCommand(null, "user", ...)` que pasa a `new LoginCommand("user", ...)` con un solo `null` de menos — ese test ya no aplica tal como está, ver Step siguiente), `defaultsBlankChannelToWebAndNormalizesEmptyRequestMetadata`, `rejectsInvalidChannelAndFailsClosedForLockedInactiveOrMfaAccounts`, `requiresATrustedDeviceForPosAndHandlesSessionPersistenceFailure`, y el helper privado `loginWeb()`.

En `rejectsMissingCommandsAndRequiredValuesWithoutCallingPersistence`, el caso que verificaba `tenantId == null` pierde sentido (ya no existe ese campo); reemplazar esa línea:

```java
        assertFailureCode(service.login(null), "AUTH_REQUEST_INVALID");
        assertFailureCode(service.login(new LoginCommand(
                "", CURRENT_PASSWORD, "WEB", null, null, null)), "AUTH_REQUEST_INVALID");
```

(ahora valida que un `login` en blanco es inválido, cubriendo la misma rama `blank(command.login())` de `LocalAuthService.login`).

En `createsOneTimePasswordRecoveryMaterialWithoutRevealingUnknownAccounts`, reemplazar `new PasswordResetRequest(TENANT_ID, " local.admin ")` por `new PasswordResetRequest(" local.admin ")` (y el segundo uso con `"unknown"`).

En `FakeStore`, cambiar la firma sobreescrita:

```java
        @Override
        public Optional<LocalAccount> findAccountByLogin(String login) {
            return accountByLogin;
        }
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.application.usecase.command.LocalAuthServiceTest"`
Expected: FAIL — error de compilación, `LoginCommand`/`PasswordResetRequest`/`findAccountByLogin` todavía tienen la firma vieja con `tenantId`.

- [ ] **Step 3: Actualizar `LocalAuthStorePort.java`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/LocalAuthStorePort.java`, cambiar la firma:

```java
    Optional<LocalAccount> findAccountByLogin(String login);
```

- [ ] **Step 4: Actualizar `LocalAuthUseCase.java`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/LocalAuthUseCase.java`, cambiar los records:

```java
    record LoginCommand(
            String login, String password, String channel,
            String ipAddress, String userAgent, UUID deviceId) {
    }

    record PasswordResetRequest(String login) {
    }
```

- [ ] **Step 5: Actualizar `LocalAuthService.java` — métodos `login` y `requestPasswordReset`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/LocalAuthService.java`, reemplazar el inicio de `login`:

```java
    @Override
    public Result<TokenResult, ApplicationError> login(LoginCommand command) {
        if (command == null || blank(command.login()) || blank(command.password())) {
            return invalid("login", "Usuario y contrasena son obligatorios.");
        }
        var channel = normalizeChannel(command.channel());
        if (!CHANNELS.contains(channel)) return invalid("channel", "El canal de autenticacion no es valido.");

        var now = clock.now();
        var account = store.findAccountByLogin(command.login().trim());
```

Y reemplazar el inicio de `requestPasswordReset`:

```java
    @Override
    public Result<Unit, ApplicationError> requestPasswordReset(PasswordResetRequest command) {
        if (command == null || blank(command.login())) {
            return invalid("login", "Usuario es obligatorio.");
        }
        passwords.matches("dummy-password-that-is-never-valid", dummyPasswordHash);
        var account = store.findAccountByLogin(command.login().trim());
```

El resto de ambos métodos permanece igual (usan `account`, `value`, etc. de la misma forma).

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.application.usecase.command.LocalAuthServiceTest"`
Expected: PASS — los 15 tests existentes en verde con la nueva firma.

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/LocalAuthStorePort.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/LocalAuthUseCase.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/LocalAuthService.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/LocalAuthServiceTest.java
git commit -m "feat(security): login y recuperacion de password ya no requieren tenantId"
```

---

### Task 6: `LocalAuthJdbcAdapter` sobre `identidad`/`membership`

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/LocalAuthJdbcAdapter.java`

**Interfaces:**
- Consumes: esquema `identidad`/`membership` (Task 4).
- Produces: `LocalAuthJdbcAdapter` compilable y funcional sobre el nuevo esquema. Usado por Task 10 (test de integración).

- [ ] **Step 1: Reemplazar `findAccountByLogin`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/LocalAuthJdbcAdapter.java`:

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalAccount> findAccountByLogin(String login) {
        return jdbc.sql(accountSelect() + """
                         WHERE LOWER(CAST(i.username AS VARCHAR)) = LOWER(:login)
                            OR LOWER(CAST(i.email AS VARCHAR)) = LOWER(:login)
                        """)
                .param("login", login)
                .query((rs, row) -> account(rs)).optional();
    }
```

- [ ] **Step 2: Actualizar `accountSelect()` y `account(ResultSet)`**

Reemplazar `accountSelect()`:

```java
    private static String accountSelect() {
        return """
                SELECT t.uuid_publico AS tenant_uuid, m.uuid_publico AS user_uuid,
                       CAST(i.username AS VARCHAR) AS username, CAST(i.email AS VARCHAR) AS email,
                       m.nombre_mostrar, m.estado AS user_status, c.estado AS credential_status,
                       c.password_hash, c.intentos_fallidos, c.bloqueado_hasta,
                       (c.requiere_cambio OR m.requiere_cambio_credencial) AS requiere_cambio,
                       m.mfa_requerido
                  FROM sch_seguridad.identidad i
                  JOIN sch_seguridad.membership m ON m.identidad_id = i.id
                  JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                  JOIN sch_seguridad.credencial_local c ON c.membership_id = m.id
                """;
    }
```

`account(ResultSet)` no cambia (lee las mismas columnas por alias).

- [ ] **Step 3: Reemplazar el resto de queries que usan `sch_seguridad.usuario` — `findAccountByUser`**

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalAccount> findAccountByUser(UUID tenantId, UUID userId) {
        return jdbc.sql(accountSelect() + """
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> account(rs)).optional();
    }
```

- [ ] **Step 4: Actualizar `provisionCredential`, `recordLoginFailure`, `recordLoginSuccess`, `createSessionAndRefresh`, `findRefreshToken`, `rotateRefreshToken` (sin cambio de tablas), `revokeSession`, `createPasswordReset`, `resetPassword`, `changePassword`, `isSessionActive`, `findEffectivePermissions`, `insertRefresh`, `replacePasswordAndRevoke`, `revokeAllUserSessions`, `updateUserPasswordChangeRequirement`, `findUserInternalId`**

Reemplazar todo el cuerpo del archivo desde `provisionCredential` hasta el final de la clase (antes de `uuid`/`instant`/`toOffsetDateTime`/`UserReference`) por:

```java
    @Override
    @Transactional
    public ProvisionOutcome provisionCredential(
            UUID tenantId, UUID userId, String passwordHash, boolean requireChange, Instant at) {
        var membershipInternalId = findMembershipInternalId(tenantId, userId);
        if (membershipInternalId.isEmpty()) return ProvisionOutcome.USER_NOT_FOUND;
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local
                           SET password_hash = :passwordHash, intentos_fallidos = 0, bloqueado_hasta = NULL,
                               requiere_cambio = :requireChange, estado = 'ACTIVA',
                               password_changed_at = :at, updated_at = :at
                         WHERE membership_id = :membershipId
                        """)
                .param("passwordHash", passwordHash).param("requireChange", requireChange)
                .param("at", toOffsetDateTime(at)).param("membershipId", membershipInternalId.get()).update();
        if (updated == 1) {
            updateUserPasswordChangeRequirement(tenantId, userId, requireChange, at);
            revokeAllUserSessions(tenantId, userId, "CREDENCIAL_REEMPLAZADA", at);
            return ProvisionOutcome.UPDATED;
        }
        try {
            jdbc.sql("""
                            INSERT INTO sch_seguridad.credencial_local
                                (membership_id, password_hash, requiere_cambio, estado,
                                 password_changed_at, created_at)
                            VALUES (:membershipId, :passwordHash, :requireChange, 'ACTIVA', :at, :at)
                            """)
                    .param("membershipId", membershipInternalId.get()).param("passwordHash", passwordHash)
                    .param("requireChange", requireChange)
                    .param("at", toOffsetDateTime(at)).update();
            updateUserPasswordChangeRequirement(tenantId, userId, requireChange, at);
            return ProvisionOutcome.CREATED;
        } catch (DataIntegrityViolationException concurrentInsert) {
            return provisionCredential(tenantId, userId, passwordHash, requireChange, at);
        }
    }

    @Override
    @Transactional
    public void recordLoginFailure(
            UUID tenantId, UUID userId, int maximumAttempts, Instant lockedUntil, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET intentos_fallidos = c.intentos_fallidos + 1,
                               bloqueado_hasta = CASE
                                   WHEN c.intentos_fallidos + 1 >= :maximumAttempts THEN :lockedUntil
                                   ELSE c.bloqueado_hasta END,
                               updated_at = :at
                         WHERE c.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("maximumAttempts", maximumAttempts)
                .param("lockedUntil", toOffsetDateTime(lockedUntil))
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    @Override
    @Transactional
    public void recordLoginSuccess(UUID tenantId, UUID userId, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET intentos_fallidos = 0, bloqueado_hasta = NULL, updated_at = :at
                         WHERE c.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.membership m SET ultimo_login_at = :at, updated_at = :at
                         WHERE m.uuid_publico = :userId AND m.tenant_id = (
                               SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    @Override
    @Transactional
    public boolean createSessionAndRefresh(SessionData session, RefreshTokenData refresh) {
        var inserted = jdbc.sql("""
                        INSERT INTO sch_seguridad.sesion_usuario
                            (uuid_sesion, tenant_id, membership_id, provider, auth_method, canal,
                             ip_origen, user_agent, dispositivo_ref, login_at, ultimo_uso_at,
                             expira_at, estado)
                        SELECT :sessionId, t.id, m.id, 'local', 'PASSWORD', :channel,
                               :ipAddress, :userAgent, :deviceId, :loginAt, :loginAt,
                               :expiresAt, 'ACTIVA'
                          FROM sch_seguridad.membership m
                          JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND m.estado = 'ACTIVO'
                        """)
                .param("sessionId", session.sessionId()).param("channel", session.channel())
                .param("loginAt", toOffsetDateTime(session.loginAt()))
                .param("expiresAt", toOffsetDateTime(session.expiresAt()))
                .param("tenantId", session.tenantId()).param("userId", session.userId())
                .param("ipAddress", session.ipAddress(), Types.OTHER)
                .param("userAgent", session.userAgent(), Types.VARCHAR)
                .param("deviceId", session.deviceId(), Types.OTHER).update();
        if (inserted != 1) return false;
        return insertRefresh(refresh) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRefreshToken> findRefreshToken(String tokenHash) {
        return jdbc.sql("""
                        SELECT t.uuid_publico AS tenant_uuid, m.uuid_publico AS user_uuid,
                               r.sesion_uuid, r.familia_uuid, r.expira_at, r.usado_at, r.revocado_at
                          FROM sch_seguridad.token_refresh r
                          JOIN sch_farmacia.tenant t ON t.id = r.tenant_id
                          JOIN sch_seguridad.membership m ON m.id = r.membership_id AND m.tenant_id = r.tenant_id
                         WHERE r.token_hash = :tokenHash
                        """)
                .param("tokenHash", tokenHash)
                .query((rs, row) -> new StoredRefreshToken(
                        uuid(rs, "tenant_uuid"), uuid(rs, "user_uuid"), uuid(rs, "sesion_uuid"),
                        uuid(rs, "familia_uuid"), instant(rs, "expira_at"), instant(rs, "usado_at"),
                        instant(rs, "revocado_at"))).optional();
    }

    @Override
    @Transactional
    public boolean rotateRefreshToken(String currentHash, RefreshTokenData replacement, Instant at) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh
                           SET usado_at = :at, reemplazado_por = :replacementId
                         WHERE token_hash = :currentHash AND usado_at IS NULL AND revocado_at IS NULL
                           AND expira_at > :at
                        """)
                .param("at", toOffsetDateTime(at)).param("replacementId", replacement.id())
                .param("currentHash", currentHash).update();
        return updated == 1 && insertRefresh(replacement) == 1;
    }

    @Override
    @Transactional
    public boolean revokeSession(UUID tenantId, UUID userId, UUID sessionId, String reason, Instant at) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.sesion_usuario s
                           SET estado = 'REVOCADA', revocado_at = :at, motivo_revocacion = :reason
                         WHERE s.uuid_sesion = :sessionId AND s.estado = 'ACTIVA'
                           AND s.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at)).param("reason", reason).param("sessionId", sessionId)
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh SET revocado_at = :at
                         WHERE sesion_uuid = :sessionId AND revocado_at IS NULL
                        """)
                .param("at", toOffsetDateTime(at)).param("sessionId", sessionId).update();
        return updated == 1;
    }

    @Override
    @Transactional
    public void createPasswordReset(
            UUID tenantId, UUID userId, String tokenHash, Instant expiresAt, Instant at) {
        var membershipInternalId = findMembershipInternalId(tenantId, userId).orElseThrow();
        var tenantInternalId = findTenantInternalId(tenantId).orElseThrow();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_recuperacion_password
                           SET revocado_at = :at
                         WHERE tenant_id = :tenantId AND membership_id = :userId
                           AND consumido_at IS NULL AND revocado_at IS NULL
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantInternalId).param("userId", membershipInternalId).update();
        jdbc.sql("""
                        INSERT INTO sch_seguridad.token_recuperacion_password
                            (tenant_id, membership_id, token_hash, expira_at, created_at)
                        VALUES (:tenantId, :userId, :tokenHash, :expiresAt, :at)
                        """)
                .param("tenantId", tenantInternalId).param("userId", membershipInternalId)
                .param("tokenHash", tokenHash)
                .param("expiresAt", toOffsetDateTime(expiresAt))
                .param("at", toOffsetDateTime(at)).update();
    }

    @Override
    @Transactional
    public boolean resetPassword(String tokenHash, String newPasswordHash, Instant at) {
        var target = jdbc.sql("""
                        SELECT t.uuid_publico AS tenant_uuid, m.uuid_publico AS user_uuid
                          FROM sch_seguridad.token_recuperacion_password p
                          JOIN sch_farmacia.tenant t ON t.id = p.tenant_id
                          JOIN sch_seguridad.membership m ON m.id = p.membership_id AND m.tenant_id = p.tenant_id
                         WHERE p.token_hash = :tokenHash AND p.consumido_at IS NULL
                           AND p.revocado_at IS NULL AND p.expira_at > :at
                        """)
                .param("tokenHash", tokenHash).param("at", toOffsetDateTime(at))
                .query((rs, row) -> new UserReference(uuid(rs, "tenant_uuid"), uuid(rs, "user_uuid")))
                .optional();
        if (target.isEmpty()) return false;
        var consumed = jdbc.sql("""
                        UPDATE sch_seguridad.token_recuperacion_password SET consumido_at = :at
                         WHERE token_hash = :tokenHash AND consumido_at IS NULL
                           AND revocado_at IS NULL AND expira_at > :at
                        """)
                .param("at", toOffsetDateTime(at)).param("tokenHash", tokenHash).update();
        if (consumed != 1) return false;
        return replacePasswordAndRevoke(target.get(), newPasswordHash, at, "PASSWORD_RECUPERADA");
    }

    @Override
    @Transactional
    public boolean changePassword(UUID tenantId, UUID userId, String newPasswordHash, Instant at) {
        return replacePasswordAndRevoke(
                new UserReference(tenantId, userId), newPasswordHash, at, "PASSWORD_CAMBIADA");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSessionActive(UUID tenantId, UUID userId, UUID sessionId, Instant at) {
        return jdbc.sql("""
                        SELECT COUNT(*)
                          FROM sch_seguridad.sesion_usuario s
                          JOIN sch_seguridad.membership m ON m.id = s.membership_id AND m.tenant_id = s.tenant_id
                          JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND s.uuid_sesion = :sessionId AND s.estado = 'ACTIVA'
                           AND m.estado = 'ACTIVO' AND m.mfa_requerido = FALSE
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.credencial_local c
                                WHERE c.membership_id = m.id AND c.estado = 'ACTIVA'
                           )
                           AND (s.expira_at IS NULL OR s.expira_at > :at)
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("sessionId", sessionId).param("at", toOffsetDateTime(at))
                .query(Long.class).single() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTrustedDevice(UUID tenantId, UUID deviceId) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.dispositivo_tienda d
                          JOIN sch_farmacia.tenant t ON t.id = d.tenant_id
                         WHERE t.uuid_publico = :tenantId AND d.uuid_publico = :deviceId
                           AND d.estado = 'CONFIABLE'
                        """)
                .param("tenantId", tenantId).param("deviceId", deviceId).query(Long.class).single() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at) {
        return new LinkedHashSet<>(jdbc.sql("""
                        SELECT DISTINCT p.codigo
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol_permiso rp ON rp.rol_id = r.id AND rp.tenant_id = r.tenant_id
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                          JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
                           AND a.estado = 'ACTIVO' AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                           AND a.vigente_desde <= :at
                           AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= :at)
                         ORDER BY p.codigo
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("at", toOffsetDateTime(at))
                .query(String.class).list());
    }

    private int insertRefresh(RefreshTokenData value) {
        return jdbc.sql("""
                        INSERT INTO sch_seguridad.token_refresh
                            (uuid_publico, tenant_id, membership_id, sesion_uuid, familia_uuid,
                             token_hash, expira_at, created_at)
                        SELECT :id, t.id, m.id, :sessionId, :familyId, :tokenHash, :expiresAt, :createdAt
                          FROM sch_seguridad.membership m
                          JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                        """)
                .param("id", value.id()).param("sessionId", value.sessionId())
                .param("familyId", value.familyId()).param("tokenHash", value.tokenHash())
                .param("expiresAt", toOffsetDateTime(value.expiresAt()))
                .param("createdAt", toOffsetDateTime(value.createdAt()))
                .param("tenantId", value.tenantId()).param("userId", value.userId()).update();
    }

    private boolean replacePasswordAndRevoke(
            UserReference user, String newPasswordHash, Instant at, String reason) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET password_hash = :passwordHash, intentos_fallidos = 0,
                               bloqueado_hasta = NULL, requiere_cambio = FALSE, estado = 'ACTIVA',
                               password_changed_at = :at, updated_at = :at
                         WHERE c.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("passwordHash", newPasswordHash).param("at", toOffsetDateTime(at))
                .param("tenantId", user.tenantId()).param("userId", user.userId()).update();
        if (updated != 1) return false;
        updateUserPasswordChangeRequirement(user.tenantId(), user.userId(), false, at);
        revokeAllUserSessions(user.tenantId(), user.userId(), reason, at);
        return true;
    }

    private void revokeAllUserSessions(UUID tenantId, UUID userId, String reason, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.sesion_usuario s
                           SET estado = 'REVOCADA', revocado_at = :at, motivo_revocacion = :reason
                         WHERE s.estado = 'ACTIVA' AND s.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at)).param("reason", reason)
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh r SET revocado_at = :at
                         WHERE r.revocado_at IS NULL AND r.membership_id = (
                               SELECT m.id FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    private void updateUserPasswordChangeRequirement(
            UUID tenantId, UUID userId, boolean requireChange, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.membership m
                           SET requiere_cambio_credencial = :requireChange, updated_at = :at
                         WHERE m.uuid_publico = :userId AND m.tenant_id = (
                               SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId)
                        """)
                .param("requireChange", requireChange).param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    private Optional<Long> findMembershipInternalId(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT m.id FROM sch_seguridad.membership m
                          JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                        """).param("tenantId", tenantId).param("userId", userId)
                .query(Long.class).optional();
    }

    private Optional<Long> findTenantInternalId(UUID tenantId) {
        return jdbc.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId")
                .param("tenantId", tenantId).query(Long.class).optional();
    }
```

Nota: las columnas de `token_refresh` y `token_recuperacion_password` que antes se llamaban `usuario_id` se renombraron a `membership_id` en la migración de la Task 4 (igual que `credencial_local`, `usuario_rol_ambito`, `sesion_usuario`), porque esas dos tablas también tenían una FK directa a `sch_seguridad.usuario` que había que repuntar antes de poder eliminar esa tabla.

- [ ] **Step 2: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:security:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/LocalAuthJdbcAdapter.java
git commit -m "feat(security): LocalAuthJdbcAdapter opera sobre identidad y membership"
```

---

### Task 7: DTOs HTTP de login sin `tenantId` y controller

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/LoginRequest.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/SolicitarRecuperacionPasswordRequest.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/CrearUsuarioRequest.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/response/UsuarioResponse.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/LocalAuthController.java`

**Interfaces:**
- Consumes: `LoginCommand`/`PasswordResetRequest` (Task 5), `CrearUsuarioCommand`/`UsuarioResult` (Task 3).
- Produces: `POST /auth/login` y `POST /auth/password/forgot` sin `tenantId` en el body; `POST /api/v1/usuarios` sin campos SSO. Usado por Task 10 (test de integración) y por el frontend (Task 11+).

- [ ] **Step 1: Actualizar `LoginRequest.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/LoginRequest.java`:

```java
package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LoginRequest(
        @NotBlank @Size(max = 254) String login,
        @NotBlank @Size(max = 128) String password,
        @Size(max = 30) String channel,
        UUID deviceId) {
}
```

- [ ] **Step 2: Actualizar `SolicitarRecuperacionPasswordRequest.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/SolicitarRecuperacionPasswordRequest.java`:

```java
package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacionPasswordRequest(
        @NotBlank @Size(max = 254) String login) {
}
```

- [ ] **Step 3: Actualizar `CrearUsuarioRequest.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/CrearUsuarioRequest.java`:

```java
package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearUsuarioRequest(
        @NotNull UUID tenantId,
        @Size(max = 20) String documentType,
        @Size(max = 30) String documentNumber,
        @Size(max = 150) String firstNames,
        @Size(max = 180) String lastNames,
        @Size(max = 150) String username,
        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 250) String displayName,
        Boolean credentialChangeRequired,
        Boolean mfaRequired) {
}
```

- [ ] **Step 4: Actualizar `UsuarioResponse.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/response/UsuarioResponse.java`:

```java
package com.softprimesolutions.security.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        UUID tenantId,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String displayName,
        String phone,
        boolean credentialChangeRequired,
        boolean mfaRequired,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 5: Actualizar `IamApiMapper.java`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java`, reemplazar `toCommand(CrearUsuarioRequest)` y `toResponse(UsuarioResult)`:

```java
    public static CrearUsuarioCommand toCommand(CrearUsuarioRequest request) {
        return new CrearUsuarioCommand(
                request.tenantId(), request.documentType(), request.documentNumber(),
                request.firstNames(), request.lastNames(), request.username(), request.email(),
                request.phone(), request.displayName(), Boolean.TRUE.equals(request.credentialChangeRequired()),
                Boolean.TRUE.equals(request.mfaRequired()));
    }
```

```java
    public static UsuarioResponse toResponse(UsuarioResult result) {
        return new UsuarioResponse(
                result.id(), result.tenantId(), result.documentType(), result.documentNumber(),
                result.firstNames(), result.lastNames(), result.username(), result.email(),
                result.displayName(), result.phone(), result.credentialChangeRequired(),
                result.mfaRequired(), result.status(), result.createdAt(), result.updatedAt());
    }
```

- [ ] **Step 6: Actualizar `LocalAuthController.java`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/LocalAuthController.java`, reemplazar la construcción del command en `login`:

```java
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var command = new LoginCommand(
                request.login(), request.password(), request.channel(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), request.deviceId());
        return auth.login(command).fold(LocalAuthController::tokenResponse, IamControllerSupport::problem);
    }
```

Y en `requestPasswordReset`:

```java
    @PostMapping("/auth/password/forgot")
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody SolicitarRecuperacionPasswordRequest request) {
        return auth.requestPasswordReset(new PasswordResetRequest(request.login())).fold(
                ignored -> ResponseEntity.accepted().build(), IamControllerSupport::problem);
    }
```

- [ ] **Step 7: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:security:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/LoginRequest.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/SolicitarRecuperacionPasswordRequest.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/CrearUsuarioRequest.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/response/UsuarioResponse.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/LocalAuthController.java
git commit -m "feat(security): API HTTP de login y creacion de usuario sin tenantId/SSO"
```

---

## Fase 3 — Adapters de lectura y control administrativo

### Task 8: `SecurityControlJdbcAdapter` e `IamJdbcReadRepository` sobre `identidad`/`membership`

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/adapter/SecurityControlJdbcAdapter.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/repository/IamJdbcReadRepository.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/projection/UsuarioProjection.java`

**Interfaces:**
- Consumes: esquema `identidad`/`membership` (Task 4).
- Produces: `SecurityControlJdbcAdapter`/`IamJdbcReadRepository` compilables y funcionales. Usado por Task 10.

- [ ] **Step 1: Actualizar `SecurityControlJdbcAdapter.updateUserStatus`**

```java
    @Override
    @Transactional
    public boolean updateUserStatus(UUID tenantId, UUID userId, String status, Instant changedAt) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.membership m
                           SET estado = :status, updated_at = :changedAt
                          FROM sch_farmacia.tenant t
                         WHERE t.id = m.tenant_id
                           AND t.uuid_publico = :tenantId
                           AND m.uuid_publico = :userId
                        """)
                .param("status", status).param("changedAt", toOffsetDateTime(changedAt))
                .param("tenantId", tenantId).param("userId", userId).update() == 1;
    }
```

- [ ] **Step 2: Actualizar `findExternalIdentities`, `createExternalIdentity`, `deleteExternalIdentity`**

```java
    @Override
    @Transactional(readOnly = true)
    public List<ExternalIdentityView> findExternalIdentities(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT i.provider, i.subject, i.issuer, CAST(i.email_claim AS VARCHAR) AS email_claim,
                               i.ultimo_login_at, i.created_at
                          FROM sch_seguridad.identidad_externa i
                          JOIN sch_seguridad.membership m ON m.identidad_id = i.identidad_id
                          JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                      ORDER BY i.created_at, i.id
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> new ExternalIdentityView(
                        rs.getString("provider"), rs.getString("subject"), rs.getString("issuer"),
                        rs.getString("email_claim"), instant(rs, "ultimo_login_at"), instant(rs, "created_at")))
                .list();
    }

    @Override
    @Transactional
    public boolean createExternalIdentity(
            UUID tenantId, UUID userId, ExternalIdentityData data, Instant createdAt) {
        try {
            var query = jdbc.sql("""
                            INSERT INTO sch_seguridad.identidad_externa
                                (identidad_id, provider, subject, issuer, email_claim, created_at)
                            SELECT m.identidad_id, :provider, :subject, :issuer, :emailClaim, :createdAt
                              FROM sch_seguridad.membership m
                              JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                             WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                            """)
                    .param("provider", data.provider()).param("subject", data.subject())
                    .param("createdAt", toOffsetDateTime(createdAt)).param("tenantId", tenantId).param("userId", userId);
            query = data.issuer() == null
                    ? query.param("issuer", null, Types.VARCHAR) : query.param("issuer", data.issuer());
            query = data.emailClaim() == null
                    ? query.param("emailClaim", null, Types.VARCHAR) : query.param("emailClaim", data.emailClaim());
            return query.update() == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteExternalIdentity(UUID tenantId, UUID userId, String provider, String subject) {
        return jdbc.sql("""
                        DELETE FROM sch_seguridad.identidad_externa i
                         WHERE i.provider = :provider AND i.subject = :subject
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.membership m
                               JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE m.identidad_id = i.identidad_id AND m.uuid_publico = :userId
                                  AND t.uuid_publico = :tenantId
                           )
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.identidad_externa other
                                WHERE other.identidad_id = i.identidad_id AND other.id <> i.id
                           )
                        """)
                .param("provider", provider).param("subject", subject)
                .param("tenantId", tenantId).param("userId", userId).update() == 1;
    }
```

- [ ] **Step 3: Actualizar `findAssignments`, `revokeAssignment`, `findEffectivePermissions`, `findSessions`**

```java
    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> findAssignments(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT a.uuid_publico, r.uuid_publico AS rol_uuid, r.codigo AS rol_codigo,
                               r.nombre AS rol_nombre, a.tipo_ambito,
                               e.uuid_publico AS empresa_uuid, s.uuid_publico AS establecimiento_uuid,
                               w.uuid_publico AS almacen_uuid, p.uuid_publico AS terminal_uuid,
                               a.vigente_desde, a.vigente_hasta, a.estado, a.created_by, a.created_at
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                     LEFT JOIN sch_farmacia.empresa_operadora e ON e.id = a.empresa_id
                     LEFT JOIN sch_farmacia.establecimiento_farmaceutico s ON s.id = a.establecimiento_id
                     LEFT JOIN sch_farmacia.almacen w ON w.id = a.almacen_id
                     LEFT JOIN sch_farmacia.terminal_pos p ON p.id = a.terminal_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                      ORDER BY a.created_at DESC, a.id DESC
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> assignment(rs)).list();
    }

    @Override
    @Transactional
    public boolean revokeAssignment(UUID tenantId, UUID userId, UUID assignmentId) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.usuario_rol_ambito a
                           SET estado = 'REVOCADO'
                         WHERE a.uuid_publico = :assignmentId AND a.estado = 'ACTIVO'
                           AND EXISTS (
                               SELECT 1
                                 FROM sch_seguridad.membership m
                                 JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                                WHERE m.id = a.membership_id AND m.tenant_id = a.tenant_id
                                  AND t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           )
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("assignmentId", assignmentId).update() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at) {
        return new LinkedHashSet<>(jdbc.sql("""
                        SELECT DISTINCT p.codigo
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol_permiso rp ON rp.rol_id = r.id AND rp.tenant_id = r.tenant_id
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                          JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
                           AND a.estado = 'ACTIVO' AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                           AND a.vigente_desde <= :at
                           AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= :at)
                      ORDER BY p.codigo
                        """)
                .param("tenantId", tenantId).param("userId", userId).param("at", toOffsetDateTime(at))
                .query(String.class).list());
    }
```

```java
    @Override
    @Transactional(readOnly = true)
    public List<SessionView> findSessions(UUID tenantId, UUID userId) {
        var sql = """
                SELECT s.uuid_sesion, m.uuid_publico AS usuario_uuid, s.provider, s.auth_method,
                       s.canal, CAST(s.ip_origen AS VARCHAR) AS ip_origen, s.user_agent,
                       s.dispositivo_ref, s.login_at, s.ultimo_uso_at, s.expira_at,
                       s.logout_at, s.revocado_at, s.motivo_revocacion, s.estado
                  FROM sch_seguridad.sesion_usuario s
                  JOIN sch_seguridad.membership m ON m.id = s.membership_id AND m.tenant_id = s.tenant_id
                  JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
                 WHERE t.uuid_publico = :tenantId
                """ + (userId == null ? "" : " AND m.uuid_publico = :userId")
                + " ORDER BY s.login_at DESC, s.id DESC";
        var query = jdbc.sql(sql).param("tenantId", tenantId);
        if (userId != null) query = query.param("userId", userId);
        return query.query((rs, row) -> session(rs)).list();
    }
```

- [ ] **Step 4: Actualizar `IamJdbcReadRepository` — `USER_FILTER`, `findUsers`, `UsuarioProjection`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/repository/IamJdbcReadRepository.java`, reemplazar `USER_FILTER`:

```java
    private static final String USER_FILTER = """
            FROM sch_seguridad.membership m
            JOIN sch_seguridad.identidad i ON i.id = m.identidad_id
            JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
            WHERE t.uuid_publico = :tenantId
              AND (:search = ''
                OR LOWER(COALESCE(m.nombre_mostrar, '')) LIKE :pattern
                OR LOWER(COALESCE(i.email::text, '')) LIKE :pattern
                OR LOWER(COALESCE(i.username::text, '')) LIKE :pattern
                OR LOWER(COALESCE(i.numero_documento, '')) LIKE :pattern)
            """;
```

Reemplazar `findUsers`:

```java
    public List<UsuarioProjection> findUsers(UUID tenantId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT m.uuid_publico, t.uuid_publico AS tenant_uuid,
                               i.tipo_documento, i.numero_documento, i.nombres, i.apellidos,
                               i.username, i.email, m.nombre_mostrar, i.telefono,
                               m.requiere_cambio_credencial, m.mfa_requerido,
                               m.estado, m.created_at, m.updated_at
                        """ + USER_FILTER + " ORDER BY m.nombre_mostrar, m.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> new UsuarioProjection(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("tipo_documento"),
                        rs.getString("numero_documento"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("nombre_mostrar"),
                        rs.getString("telefono"),
                        rs.getBoolean("requiere_cambio_credencial"),
                        rs.getBoolean("mfa_requerido"),
                        rs.getString("estado"),
                        toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                        toInstant(rs.getObject("updated_at", OffsetDateTime.class))))
                .list();
    }
```

- [ ] **Step 5: Actualizar `UsuarioProjection.java`**

Reemplazar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/projection/UsuarioProjection.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record UsuarioProjection(
        UUID id,
        UUID tenantId,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String displayName,
        String phone,
        boolean credentialChangeRequired,
        boolean mfaRequired,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 6: Buscar y actualizar el mapeo de `UsuarioProjection` a `UsuarioResult`/query handler de listado**

Run: `grep -rln "UsuarioProjection" service-botica/modules/security/src/main/java`

Abrir cada archivo resultante (probablemente un `ListarUsuariosHandler` o similar en `application/usecase/query/`) y ajustar la construcción de `UsuarioResult`/DTO de página para que use los campos nuevos de `UsuarioProjection` (sin `identityProvider`/`identityIssuer`/`identitySubject`/`emailClaim`), replicando el mismo patrón de mapeo campo-por-campo ya usado en Task 3 Step 7 para `IamApplicationMapper.toResult`.

- [ ] **Step 7: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:security:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/
git commit -m "feat(security): adapters de lectura y control administrativo sobre identidad/membership"
```

---

### Task 9: `IamApiIntegrationTest` — actualizar body de creación de usuario y validación HTTP

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java`

**Interfaces:**
- Consumes: `CrearUsuarioRequest` (Task 7).

- [ ] **Step 1: Actualizar el body de creación de usuario (alrededor de la línea 326)**

En `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java`, reemplazar el bloque:

```java
        var userResponse = mockMvc.perform(post("/api/v1/usuarios")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"%s",
                                  "email":"admin@example.test",
                                  "displayName":"Ada Lovelace"
                                }
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String userId = JsonPath.read(userResponse, "$.id");
```

- [ ] **Step 2: Actualizar `returnsProblemDetailsForInvalidHttpInput` (línea 454-465)**

Reemplazar el body que fuerza el 400: como `CrearUsuarioRequest` ya no tiene `identityIssuer`/`identitySubject` obligatorios, forzar la validación con `tenantId` ausente (sigue siendo `@NotNull`):

```java
    @Test
    void returnsProblemDetailsForInvalidHttpInput() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }
```

- [ ] **Step 3: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.api.IamApiIntegrationTest"`
Expected: PASS. Si falla por otra razón relacionada con `login`/`auth`, revisar si algún otro método de esta clase (no listado en este plan porque no se identificó al momento de escribirlo) también construye un body de login con `tenantId` — buscar con `grep -n "auth/login\|tenantId" service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java` y ajustar cualquier body de login encontrado para quitar `tenantId`, igual que se hizo con el body de creación de usuario en el Step 1.

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java
git commit -m "test(security): actualizar IamApiIntegrationTest para creacion de usuario sin SSO"
```

---

### Task 10: Verificación completa del backend (`check`) y test de login real end-to-end

**Files:**
- Test: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/LocalAuthLoginWithoutTenantIdTest.java`

**Interfaces:**
- Consumes: todo lo construido en las Tasks 1-11.

- [ ] **Step 1: Escribir el test de integración que falla — login sin tenantId resuelve el tenant correctamente**

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/LocalAuthLoginWithoutTenantIdTest.java`, copiando la configuración de clase (`@SpringBootTest`, Testcontainers, `@AutoConfigureMockMvc` o equivalente) exactamente de `IamApiIntegrationTest.java`:

```java
package com.softprimesolutions.security.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

class LocalAuthLoginWithoutTenantIdTest {
    // Copiar las anotaciones de clase, campos @Autowired de infraestructura (DataSource/JdbcClient
    // para sembrar datos) y helpers de autenticacion (admin(), etc.) exactamente de IamApiIntegrationTest.

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAUserAndLogsInWithoutSendingTenantId() throws Exception {
        var userResponse = mockMvc.perform(post("/api/v1/usuarios")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                                new SimpleGrantedAuthority("seguridad.usuarios.gestionar")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"%s",
                                  "email":"sinssoyaml@example.test",
                                  "username":"sinssoyaml",
                                  "displayName":"Sin SSO"
                                }
                                """.formatted(SEED_TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userId = JsonPath.read(userResponse, "$.id");

        mockMvc.perform(post("/api/v1/usuarios/{userId}/credencial-local", userId)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                                new SimpleGrantedAuthority("seguridad.credenciales.gestionar")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","password":"SinSso2026!Valid","requireChange":false}
                                """.formatted(SEED_TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"sinssoyaml@example.test","password":"SinSso2026!Valid"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").exists())
                .andExpect(jsonPath("$.userId").exists());
    }
}
```

Este esqueleto usa un placeholder `SEED_TENANT_ID` y helpers que deben copiarse literalmente de `IamApiIntegrationTest` (constante de tenant sembrado en el `@BeforeAll`/`@Sql` de esa clase, y la forma exacta en que ese test siembra el tenant antes de sus casos) — revisar `IamApiIntegrationTest.java` completo (no solo el fragmento ya leído en este plan) para copiar la configuración de sembrado de tenant, ya que no se incluye aquí por no haber sido inspeccionada en su totalidad al escribir este plan.

- [ ] **Step 2: Ejecutar y verificar que falla o pasa, y ajustar el esqueleto según la configuración real de `IamApiIntegrationTest`**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.api.LocalAuthLoginWithoutTenantIdTest"`

Si falla por configuración de clase incompleta (falta `@SpringBootTest`, Testcontainers, etc.), completar copiando exactamente de `IamApiIntegrationTest`. Iterar hasta que el test compile y corra.

- [ ] **Step 3: Ajustar hasta que pase**

Expected final: PASS — login exitoso sin `tenantId` en el body, y el `AuthTokenResponse` sigue devolviendo `tenantId`/`userId` resueltos correctamente por el backend.

- [ ] **Step 4: Ejecutar la suite completa del backend**

Run: `cd service-botica && .\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL — todos los tests, ArchUnit y Spring Modulith `verify()` en verde.

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/LocalAuthLoginWithoutTenantIdTest.java
git commit -m "test(security): verificar login end-to-end sin tenantId visible"
```

---

## Fase 4 — Frontend

### Task 11: Quitar `tenantId` del schema, formulario y cliente de API

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts`
- Modify: `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`
- Modify: `frontend/apps/erp-web/src/features/auth/api/auth.api.ts`
- Modify: `frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts`
- Modify: `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`

**Interfaces:**
- Produces: `LoginCredentials` sin `tenantId`. Usado por Task 12 (tests).

- [ ] **Step 1: Actualizar `login.schema.ts`**

Reemplazar `frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts`:

```ts
import { z } from 'zod';

export const loginSchema = z.object({
  email: z.email('Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});

export type LoginCredentials = z.infer<typeof loginSchema>;
```

- [ ] **Step 2: Revertir `LoginForm.tsx` — quitar el campo de tenant**

En `frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx`, quitar el bloque completo del campo `tenantId` agregado anteriormente (el `<div>` con `htmlFor="tenantId"` insertado antes del bloque de `email`) y quitar `tenantId: ''` de `defaultValues`:

```tsx
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register
  } = useForm<LoginCredentials>({
    defaultValues: { email: '', password: '', remember: false },
    mode: 'onTouched',
    resolver: zodResolver(loginSchema)
  });
```

Dejar intacto el resto del formulario (el manejo de `submitError`, el campo de email, password, checkbox y footer no cambian).

- [ ] **Step 3: Actualizar `auth-tokens.types.ts` — quitar `tenantId` de `LoginRequest`**

Run: `grep -n "tenantId" frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts`

Quitar el campo `tenantId` del tipo `LoginRequest` en ese archivo (mantener `login`, `password`, `channel` y cualquier otro campo ya existente).

- [ ] **Step 4: Actualizar `auth.api.ts` — quitar `tenantId` del body enviado**

En `frontend/apps/erp-web/src/features/auth/api/auth.api.ts`, la función `login` ya construye el body a partir de `LoginRequest` — no requiere cambios de código si `LoginRequest` (Step 3) ya no tiene `tenantId`; verificar que no haya una referencia explícita a `request.tenantId` en este archivo y quitarla si existe.

- [ ] **Step 5: Actualizar `AuthSessionProvider.tsx` — `authenticate` deja de enviar `tenantId`**

En `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx`, reemplazar el cuerpo de `authenticate`:

```tsx
  const authenticate = useCallback(async (credentials: LoginCredentials) => {
    const response = await loginRequest(apiClient, {
      login: credentials.email,
      password: credentials.password
    });
    setAccessToken(response.accessToken);
    saveRefreshToken(response.refreshToken);
  }, []);
```

- [ ] **Step 6: Verificar tipos con typecheck**

Run: `cd frontend && pnpm typecheck`
Expected: puede fallar todavía por `LoginPage.test.tsx` y `AuthSessionProvider.test.tsx` (Task 12) usando `tenantId` en sus fixtures — eso se corrige en la siguiente tarea. Si falla por algo en los archivos de producción tocados en esta tarea, corregirlo antes de continuar.

- [ ] **Step 7: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/schemas/login.schema.ts frontend/apps/erp-web/src/features/auth/components/LoginForm.tsx frontend/apps/erp-web/src/features/auth/api/auth.api.ts frontend/apps/erp-web/src/features/auth/api/auth-tokens.types.ts frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.tsx
git commit -m "feat(auth): quitar tenantId del login en frontend"
```

---

### Task 12: Actualizar tests de frontend (`LoginPage`, `AuthSessionProvider`, handler MSW)

**Files:**
- Modify: `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx`
- Modify: `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx`
- Modify: `frontend/apps/erp-web/src/test/mocks/handlers.ts`

**Interfaces:**
- Consumes: `LoginCredentials` sin `tenantId` (Task 11).

- [ ] **Step 1: Reescribir `LoginPage.test.tsx` sin el campo tenant**

Reemplazar `frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { RequireAuthentication } from '../components/RequireAuthentication';
import { AuthSessionProvider } from '../model/AuthSessionProvider';
import { LoginPage } from './LoginPage';

function renderLogin(initialEntry = '/login') {
  const router = createMemoryRouter(
    [
      { path: '/login', Component: LoginPage },
      {
        Component: RequireAuthentication,
        children: [{ path: '/dashboard', Component: () => <h1>Resumen operativo</h1> }]
      }
    ],
    { initialEntries: [initialEntry] }
  );

  return {
    user: userEvent.setup(),
    ...render(
      <AuthSessionProvider>
        <RouterProvider router={router} />
      </AuthSessionProvider>
    )
  };
}

async function fillValidCredentials(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
  await user.type(screen.getByLabelText('Contraseña'), 'Boticas2026!');
}

describe('LoginPage', () => {
  it('muestra validaciones accesibles y permite visualizar la contraseña', async () => {
    const { user } = renderLogin();

    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByText('Ingresa un correo electrónico válido.')).toBeInTheDocument();
    expect(screen.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument();

    const password = screen.getByLabelText('Contraseña');
    expect(password).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(password).toHaveAttribute('type', 'text');
  });

  it('navega al dashboard cuando las credenciales son válidas', async () => {
    const { user } = renderLogin();

    await fillValidCredentials(user);
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(await screen.findByRole('heading', { name: 'Resumen operativo' })).toBeInTheDocument();
  });

  it('muestra un mensaje de error cuando el backend rechaza las credenciales', async () => {
    const { user } = renderLogin();

    await user.type(screen.getByLabelText('Correo corporativo'), 'admin@boticas.pe');
    await user.type(screen.getByLabelText('Contraseña'), 'ContrasenaIncorrecta1!');
    await user.click(screen.getByRole('button', { name: 'Iniciar Sesión' }));

    expect(
      await screen.findByText('Credenciales incorrectas o cuenta bloqueada.')
    ).toBeInTheDocument();
  });

  it('redirige al login cuando se intenta abrir una ruta privada sin sesión', async () => {
    renderLogin('/dashboard');

    expect(await screen.findByRole('heading', { name: 'Ingresa a tu cuenta' })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Actualizar `AuthSessionProvider.test.tsx` — fixture de `authenticate` sin `tenantId`**

En `frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx`, en el componente `Probe`, quitar `tenantId` del objeto pasado a `session.authenticate`:

```tsx
      <button onClick={() => session.authenticate({
        email: 'admin@boticas.pe',
        password: 'Boticas2026!',
        remember: false
      })}>
        login
      </button>
```

- [ ] **Step 3: Actualizar el handler MSW de login — ya no necesita el campo `tenantId` en la respuesta simulada (puede mantenerse, no rompe nada) pero se quita cualquier validación del body que dependa de `tenantId` si existiera**

Run: `grep -n "tenantId" frontend/apps/erp-web/src/test/mocks/handlers.ts`

Si el handler de `POST */api/v1/auth/login` valida `body.tenantId` en algún `if`, quitar esa condición (la respuesta simulada puede seguir incluyendo `tenantId` en el JSON de vuelta, ya que ese campo sigue existiendo en `AuthTokenResponse`/`AuthTokenResponse` del lado servidor — solo el request deja de llevarlo).

- [ ] **Step 4: Ejecutar la suite completa de frontend**

Run: `cd frontend && pnpm test`
Expected: PASS — todos los tests en verde, incluyendo `LoginPage.test.tsx` (4/4) y `AuthSessionProvider.test.tsx` (4/4).

- [ ] **Step 5: Typecheck y build completos**

Run: `cd frontend && pnpm typecheck`
Run: `cd frontend && pnpm build`
Expected: sin errores en ambos.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/auth/pages/LoginPage.test.tsx frontend/apps/erp-web/src/features/auth/model/AuthSessionProvider.test.tsx frontend/apps/erp-web/src/test/mocks/handlers.ts
git commit -m "test(auth): actualizar tests de login sin tenantId"
```

---

### Task 13: Verificación manual end-to-end contra el backend real

**Files:** ninguno (verificación manual).

- [ ] **Step 1: Levantar PostgreSQL y el backend**

Run: `cd service-botica && docker-compose up -d postgres` (o el comando equivalente ya documentado en el README del proyecto para levantar solo Postgres)
Run: `cd service-botica && .\gradlew.bat :bootstrap-app:bootRun`
Expected: la app arranca sin errores, las migraciones Flyway (incluida `V021`) se aplican correctamente contra Postgres real.

- [ ] **Step 2: Sembrar un tenant, identidad, membership y credencial de prueba**

Usar el mismo mecanismo de siembra ya usado en desarrollo (ver README raíz o script de seed existente) para crear un tenant y, contra `POST /api/v1/usuarios` + `POST /api/v1/usuarios/{userId}/credencial-local`, un usuario de prueba con email y password conocidos — replicando el mismo flujo del test de la Task 10 pero manualmente vía curl/Postman o el propio frontend.

- [ ] **Step 3: Levantar el frontend en modo HTTP (no mock)**

Run: `cd frontend && pnpm dev` con `VITE_API_MODE=http` en `.env.development.local` (o el mecanismo ya documentado para desactivar MSW).

- [ ] **Step 4: Probar el login real en el navegador**

Abrir `http://localhost:5173/login`, ingresar el email/password sembrados en el Step 2 (sin ningún campo de tenant visible en el formulario), confirmar que redirige al dashboard.

- [ ] **Step 5: Probar el logout y la restauración de sesión tras recargar**

Recargar la página tras el login y confirmar que la sesión se restaura (vía refresh token en `sessionStorage`); hacer logout y confirmar que redirige a `/login` y limpia el refresh token.

- [ ] **Step 6: Apagar el backend**

Run: `Ctrl+C` en la terminal del backend; `cd service-botica && docker-compose down` si se levantó Postgres vía compose.

- [ ] **Step 7: Commit final (solo si el Step 4-5 reveló necesidad de algún ajuste)**

Si no hubo ajustes, no se crea commit en esta tarea.

---

## Self-Review

**Cobertura de la spec:**
- Dominio separado en `Identidad`/`Usuario` (membership): Tasks 1, 2. ✓
- Creación de usuario sin SSO obligatorio (dominio/aplicación): Task 3. ✓
- Modelo de datos (identidad/membership, FKs, eliminación de `usuario`) + persistencia JPA completa: Task 4. ✓
- Login sin `tenantId` (backend completo: puerto, adapter, DTOs, controller): Tasks 5, 6, 7. ✓
- `SecurityControlJdbcAdapter`/`IamJdbcReadRepository` migrados: Task 8. ✓
- `identidad_externa` con FK a `identidad_id`: Task 4 (migración + entidad JPA) + Task 8 (queries de `SecurityControlJdbcAdapter`). ✓
- `POST /auth/password/forgot` sin `tenantId`: Task 5 (`PasswordResetRequest`) + Task 7 (`SolicitarRecuperacionPasswordRequest`). ✓
- Test de integración existente actualizado: Task 9. ✓
- Verificación completa backend (`check`): Task 10. ✓
- Frontend sin campo tenant, bug del UUID fixture resuelto por eliminación del campo: Tasks 11, 12. ✓
- Verificación manual end-to-end: Task 13. ✓
- Fuera de alcance (selector post-login, reusar identidad en otro tenant, migración de datos productivos): correctamente no incluidos como tareas. ✓

**Consistencia de nombres verificada:** `findAccountByLogin(String login)` (Task 5, puerto) se implementa igual en Task 6 (adapter). `IamWritePort.save(Identidad, Usuario)` (definido en Task 3, implementado en Task 4) coincide entre la interfaz y `IamJpaWriteAdapter`. `CrearUsuarioCommand` sin campos SSO se usa consistentemente en Task 3 (handler), Task 7 (mapper HTTP) y Task 9 (test de integración). `membership_id` como nombre de columna (incluyendo `token_refresh`/`token_recuperacion_password`, corregido tras el bloqueo real del implementador de la Task 4 original) se usa consistentemente en Task 4 (migración), Task 6 y Task 8.

**Riesgo identificado y mitigado:** las Tasks 10 y 9 dependen de la estructura exacta de `IamApiIntegrationTest` (helpers de siembra de tenant, configuración de Testcontainers) que no se leyó en su totalidad al escribir este plan — ambas tareas incluyen instrucciones explícitas de copiar esa configuración del archivo real en vez de asumirla, para no bloquear la ejecución con una firma inventada.

**Resecuenciación tras bloqueo real:** la primera versión de este plan separaba "Task 1: solo migración SQL" de "Tasks 4/6: entidades JPA e IamJpaWriteAdapter", asumiendo que ambas podían verificarse por separado. Un implementador real de la Task 1 quedó BLOCKED al descubrir que `bootstrap-app/src/test/resources/application-test.yaml` usa Postgres real vía Testcontainers con `spring.jpa.hibernate.ddl-auto: validate` — Hibernate valida TODAS las `@Entity` del classpath contra el esquema migrado en CADA `@SpringBootTest`, no solo en el test de la entidad tocada. Aterrizar la migración sin las entidades JPA correspondientes rompe todo `@SpringBootTest` existente, incluido `IamApiIntegrationTest`. La Task 4 de esta versión fusiona migración + entidades + `IamJpaWriteAdapter` + `IdentidadExternaJpaEntity` precisamente para que cada commit compile y pase `@SpringBootTest` de principio a fin, como exige el resto del plan. El mismo implementador también encontró y corrigió un gap real en el diseño original: `token_refresh` y `token_recuperacion_password` (definidas en `V020__local_authentication_jwt.sql`) tienen FKs directas a `sch_seguridad.usuario` que el diseño original no mencionaba, lo cual habría hecho fallar el `DROP TABLE usuario` final; la migración de la Task 4 ya las incluye.
