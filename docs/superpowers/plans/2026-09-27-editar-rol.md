# Editar Rol — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir editar los datos generales de un rol existente (`code`, `name`, `description`, `roleType`) vía `PUT /api/v1/roles/{roleId}`, agregar `GET /api/v1/roles/{roleId}` para leer un rol individual, y exponer ambos en la UI de `RoleDetailPage` reutilizando `RolForm`.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS pragmático, siguiendo exactamente los patrones ya existentes en `service-botica/modules/security` (Result Pattern sin excepciones de dominio, interfaces `XxxUseCase` sin bus de comandos genérico, CQRS con escritura JPA/JDBC directo y lectura JDBC con `JdbcClient`). Frontend: TanStack Query + react-hook-form/zod, mismo patrón que `RolesPage`/`RolForm` ya usan para creación.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, Spring Data JPA + `JdbcClient`, JUnit 5 (backend). React 19, TanStack Query, react-hook-form + zod, Vitest + Testing Library + MSW (frontend).

## Global Constraints

- Todo archivo fuente nuevo (backend y frontend) debe alcanzar 100% de cobertura de líneas y ramas (gate JaCoCo/Vitest por archivo) — CLAUDE.md, sección "Cobertura de tests".
- Sin comentarios explicativos en el código salvo decisión no obvia — CLAUDE.md, "Estándares de implementación".
- Un módulo solo expone tipos vía su paquete `api`; nunca se importan paquetes internos de otro módulo — CLAUDE.md, "Arquitectura de alto nivel".
- El router y las features de frontend no importan archivos internos de otra feature, solo su `index.ts` — CLAUDE.md, "Frontend: pnpm workspace".
- Backend: ejecutar `.\gradlew.bat check --warning-mode all` antes de dar por terminado el trabajo del módulo `security` — CLAUDE.md, "Comandos".
- Frontend: ejecutar `pnpm check` antes de dar por terminado el trabajo — CLAUDE.md, "Comandos".
- El módulo `security` no usa excepciones de dominio para rechazos de negocio esperables; usa exclusivamente el Result Pattern (`Result<T, ErrorDetail>` en dominio, `Result<T, ApplicationError>` en aplicación) — verificado en `domain/exception/package-info.java` y en el 100% de los mutadores existentes del agregado `Rol`.
- No se usan interfaces `Command`/`CommandHandler`/`Query`/`QueryHandler` genéricas de `shared-application` en este módulo — cada caso de uso define su propia interfaz `@FunctionalInterface XxxUseCase`, implementada por un `XxxHandler`, registrado como `@Bean` en `SecurityModuleConfiguration`.

---

## Task 1: Dominio — `Rol.updateDetails(...)`

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Rol.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/RolTest.java` (nuevo)

**Interfaces:**
- Produces: `Rol.updateDetails(String code, String name, String description, String roleType, Instant updatedAt): Result<Rol, ErrorDetail>` — usado por `ActualizarRolHandler` en Task 2.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/RolTest.java`:

```java
package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RolTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant CREATED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-27T00:00:00Z");

    private Rol createNonSystemRole() {
        return Rol.create(
                new RolId(ROLE_ID), new TenantId(TENANT_ID), "ADMIN_LOCAL", "Administrador local", null,
                "ESTABLECIMIENTO", false, CREATED_AT)
                .getOrElse(error -> null);
    }

    private Rol createSystemRole() {
        return Rol.create(
                new RolId(ROLE_ID), new TenantId(TENANT_ID), "SUPERADMIN", "Superadministrador", null,
                "GLOBAL", true, CREATED_AT)
                .getOrElse(error -> null);
    }

    @Test
    void updatesDetailsWhenDataIsValid() {
        var role = createNonSystemRole();

        var result = role.updateDetails(
                "ADMIN_LOCAL_2", "Administrador local actualizado", "Descripción nueva",
                "ALMACEN", UPDATED_AT);

        assertTrue(result.isSuccess());
        var updated = result.getOrElse(error -> null);
        assertEquals("ADMIN_LOCAL_2", updated.code());
        assertEquals("Administrador local actualizado", updated.name());
        assertEquals("Descripción nueva", updated.description());
        assertEquals(TipoRol.ALMACEN, updated.roleType());
        assertEquals(UPDATED_AT, updated.updatedAt());
        assertEquals(ROLE_ID, updated.id().value());
        assertEquals(Set.of(), updated.permissionCodes());
        assertEquals(EstadoRol.ACTIVO, updated.status());
    }

    @Test
    void rejectsUpdateWhenRoleIsSystemRole() {
        var role = createSystemRole();

        var result = role.updateDetails(
                "SUPERADMIN_2", "Otro nombre", null, "GLOBAL", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_SISTEMA_NO_EDITABLE", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenCodeIsInvalid() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ab", "Nombre válido", null, "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_INVALIDO", result.fold(value -> null, error -> error.code()));
        assertEquals("code", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenNameIsTooShort() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "A", null, "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("name", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenDescriptionExceedsMaxLength() {
        var role = createNonSystemRole();

        var result = role.updateDetails(
                "ADMIN_LOCAL", "Nombre válido", "x".repeat(501), "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("description", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenRoleTypeIsInvalid() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "Nombre válido", null, "NO_EXISTE", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("roleType", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenUpdatedAtIsNull() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "Nombre válido", null, "ESTABLECIMIENTO", null);

        assertTrue(result.isFailure());
        assertEquals("updatedAt", result.fold(value -> null, error -> error.metadata().get("field")));
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.domain.model.RolTest"`
Expected: FAIL — `cannot find symbol: method updateDetails(...)` (el método no existe todavía en `Rol`).

- [ ] **Step 3: Implementar `updateDetails` en `Rol.java`**

Abrir `service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Rol.java` y añadir el método público justo después de `replacePermissions(...)` (antes de `invalid(...)`):

```java
public Result<Rol, ErrorDetail> updateDetails(
        String code, String name, String description, String roleType, Instant updatedAt) {
    if (this.systemRole) {
        return Result.failure(new ErrorDetail(
                "SEC_ROL_SISTEMA_NO_EDITABLE",
                "Un rol de sistema no puede modificar sus datos generales.",
                Map.of("field", "systemRole")));
    }
    if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

    var normalizedCode = normalize(code);
    normalizedCode = normalizedCode == null ? null : normalizedCode.toUpperCase(Locale.ROOT);
    if (normalizedCode == null || !CODE_PATTERN.matcher(normalizedCode).matches()) {
        return invalid("code", "El código debe usar entre 3 y 80 caracteres A-Z, 0-9 o guion bajo.");
    }

    var normalizedName = normalizeSpaces(name);
    if (normalizedName == null || normalizedName.length() < 2 || normalizedName.length() > 150) {
        return invalid("name", "El nombre debe tener entre 2 y 150 caracteres.");
    }

    var normalizedDescription = normalizeSpaces(description);
    if (normalizedDescription != null && normalizedDescription.length() > 500) {
        return invalid("description", "La descripción no debe exceder 500 caracteres.");
    }

    final TipoRol normalizedRoleType;
    try {
        normalizedRoleType = TipoRol.valueOf(roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
        return invalid("roleType", "El tipo de rol no es válido.");
    }

    return Result.success(new Rol(
            id, tenantId, normalizedCode, normalizedName, normalizedDescription, normalizedRoleType,
            systemRole, permissionCodes, status, createdAt, updatedAt));
}
```

No se requieren imports nuevos (`Result`, `ErrorDetail`, `Map`, `Locale`, `Instant` ya están importados en el archivo para soportar `create`/`replacePermissions`).

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.domain.model.RolTest"`
Expected: PASS — 7 tests verdes.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/domain/model/Rol.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/domain/model/RolTest.java
git commit -m "$(cat <<'EOF'
feat(security): agregar Rol.updateDetails para editar datos generales del rol

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Puerto de escritura — `existsActiveRoleWithCode`

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/RolJpaRepository.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapterExistsActiveRoleWithCodeTest.java` — **no se crea**: este adapter no tiene tests de integración con BD en el repo (`IamJpaWriteAdapter` no aparece con test dedicado; toda la verificación de este método ocurre indirectamente vía `ActualizarRolHandlerTest` en Task 3 con un `StubWritePort`). No se agrega infraestructura de test nueva para JPA en esta tarea.

**Interfaces:**
- Consumes: nada nuevo de tareas previas.
- Produces: `IamWritePort.existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId): boolean` — usado por `ActualizarRolHandler` en Task 3.

- [ ] **Step 1: Añadir el método a la interfaz `IamWritePort`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java`, añadir la firma junto a `roleBelongsToTenant`:

```java
    boolean roleBelongsToTenant(UUID roleId, UUID tenantId);

    boolean existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId);
```

- [ ] **Step 2: Añadir el derived query method a `RolJpaRepository`**

Editar `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/RolJpaRepository.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.RolJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolJpaRepository extends JpaRepository<RolJpaEntity, Long> {
    Optional<RolJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
    boolean existsByTenantIdAndCodigoAndUuidPublicoNot(Long tenantId, String codigo, UUID uuidPublico);
}
```

- [ ] **Step 3: Implementar el método en `IamJpaWriteAdapter`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java`, añadir el método justo después de `roleBelongsToTenant(...)`:

```java
    @Override
    public boolean existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId) {
        return findTenantId(tenantId)
                .map(id -> roleRepository.existsByTenantIdAndCodigoAndUuidPublicoNot(id, code, excludingRoleId))
                .orElse(false);
    }
```

- [ ] **Step 4: Compilar para verificar que el módulo sigue construyendo**

Run: `cd service-botica && .\gradlew.bat :security:compileJava`
Expected: BUILD SUCCESSFUL (no hay test unitario aislado para este método porque requiere Spring Data JPA/BD real; se cubre indirectamente en Task 3).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamWritePort.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/repository/RolJpaRepository.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/write/adapter/IamJpaWriteAdapter.java
git commit -m "$(cat <<'EOF'
feat(security): agregar IamWritePort.existsActiveRoleWithCode

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Caso de uso — `ActualizarRolUseCase` / `ActualizarRolHandler`

**Files:**
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/command/ActualizarRolCommand.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/ActualizarRolUseCase.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/ActualizarRolHandler.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/ActualizarRolHandlerTest.java`

**Interfaces:**
- Consumes: `Rol.updateDetails(...)` (Task 1), `IamWritePort.existsActiveRoleWithCode(...)` (Task 2), `IamWritePort.findRole(UUID)` y `IamWritePort.save(Rol)` (ya existentes), `IamApplicationMapper.toResult(Rol): RolResult` (ya existente), `ClockPort.now(): Instant` (ya existente).
- Produces: `ActualizarRolUseCase.execute(ActualizarRolCommand): Result<RolResult, ApplicationError>` — usado por `RolController` en Task 5.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/ActualizarRolHandlerTest.java`:

```java
package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarRolHandlerTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant NOW = Instant.parse("2026-09-27T20:00:00Z");

    @Test
    void updatesRoleDetailsWhenCodeIsUnchanged() {
        var port = new StubWritePort(true, false);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local actualizado", "Descripción", "ALMACEN"));

        assertTrue(result.isSuccess());
        var updated = result.fold(value -> value, error -> null);
        assertEquals("Administrador local actualizado", updated.name());
        assertEquals("ALMACEN", updated.roleType());
        assertTrue(port.saved);
    }

    @Test
    void updatesRoleDetailsWhenCodeChangesWithoutDuplicate() {
        var port = new StubWritePort(true, false);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL_2", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isSuccess());
        assertEquals("ADMIN_LOCAL_2", result.fold(value -> value.code(), error -> null));
    }

    @Test
    void rejectsUpdateWhenRoleDoesNotExist() {
        var port = new StubWritePort(false, false);
        port.roleFound = false;
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenRoleIsSystemRole() {
        var port = new StubWritePort(true, false);
        port.systemRole = true;
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_SISTEMA_NO_EDITABLE", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenNewCodeIsDuplicate() {
        var port = new StubWritePort(true, true);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL_2", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_CODIGO_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class StubWritePort implements IamWritePort {

        private final boolean allowSave;
        private final boolean duplicateCode;
        private boolean roleFound = true;
        private boolean systemRole;
        private boolean saved;

        private StubWritePort(boolean allowSave, boolean duplicateCode) {
            this.allowSave = allowSave;
            this.duplicateCode = duplicateCode;
        }

        @Override
        public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome save(Rol role) {
            if (!allowSave) throw new IllegalStateException("save no debería invocarse en este escenario");
            saved = true;
            return SaveRolOutcome.UPDATED;
        }

        @Override
        public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Rol> findRole(UUID roleId) {
            if (!roleFound) return Optional.empty();
            return Rol.create(
                    new RolId(roleId), new TenantId(TENANT_ID), "ADMIN_LOCAL", "Administrador local", null,
                    "ESTABLECIMIENTO", systemRole, Instant.parse("2026-09-01T00:00:00Z"))
                    .fold(Optional::of, error -> Optional.empty());
        }

        @Override
        public boolean allPermissionsExist(Set<String> permissionCodes) {
            return false;
        }

        @Override
        public boolean tenantExists(UUID tenantId) {
            return false;
        }

        @Override
        public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
            return false;
        }

        @Override
        public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
            return false;
        }

        @Override
        public boolean existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId) {
            return duplicateCode;
        }

        @Override
        public boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope) {
            return false;
        }

        @Override
        public SaveAssignmentOutcome save(AsignacionRol assignment) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.application.usecase.command.ActualizarRolHandlerTest"`
Expected: FAIL — `cannot find symbol: class ActualizarRolCommand` / `class ActualizarRolHandler`.

- [ ] **Step 3: Crear `ActualizarRolCommand.java`**

```java
package com.softprimesolutions.security.application.dto.command;

import java.util.UUID;

public record ActualizarRolCommand(UUID roleId, String code, String name, String description, String roleType) {
}
```

- [ ] **Step 4: Crear `ActualizarRolUseCase.java`**

```java
package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarRolUseCase {
    Result<RolResult, ApplicationError> execute(ActualizarRolCommand command);
}
```

- [ ] **Step 5: Crear `ActualizarRolHandler.java`**

```java
package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.ActualizarRolUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarRolHandler implements ActualizarRolUseCase {

    private final IamWritePort writePort;
    private final ClockPort clock;

    public ActualizarRolHandler(IamWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(ActualizarRolCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var role = writePort.findRole(command.roleId());
        if (role.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        var existingRole = role.get();
        var normalizedCode = command.code() == null ? null : command.code().trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedCode != null && !normalizedCode.equals(existingRole.code())
                && writePort.existsActiveRoleWithCode(
                        existingRole.tenantId().value(), normalizedCode, command.roleId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_CODIGO_DUPLICADO", "Ya existe un rol con el código indicado.", ErrorCategory.CONFLICT));
        }
        return existingRole.updateDetails(command.code(), command.name(), command.description(), command.roleType(), clock.now())
                .fold(this::persist, this::validationFailure);
    }

    private Result<RolResult, ApplicationError> persist(Rol role) {
        writePort.save(role);
        return Result.success(IamApplicationMapper.toResult(role));
    }

    private Result<RolResult, ApplicationError> validationFailure(ErrorDetail error) {
        var category = "SEC_ROL_SISTEMA_NO_EDITABLE".equals(error.code())
                ? ErrorCategory.CONFLICT
                : ErrorCategory.VALIDATION;
        return Result.failure(new StandardApplicationError(error.code(), error.message(), category, error.metadata()));
    }
}
```

- [ ] **Step 6: Registrar el handler en `SecurityModuleConfiguration`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/configuration/SecurityModuleConfiguration.java`, añadir el import y el `@Bean` junto a `reemplazarPermisosRolUseCase`:

```java
import com.softprimesolutions.security.application.port.in.ActualizarRolUseCase;
import com.softprimesolutions.security.application.usecase.command.ActualizarRolHandler;
```

```java
    @Bean
    ActualizarRolUseCase actualizarRolUseCase(IamWritePort writePort, ClockPort iamClockPort) {
        return new ActualizarRolHandler(writePort, iamClockPort);
    }
```

- [ ] **Step 7: Ejecutar el test para verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.application.usecase.command.ActualizarRolHandlerTest"`
Expected: PASS — 5 tests verdes.

- [ ] **Step 8: Ejecutar el módulo completo para verificar que no rompió nada (Spring Modulith + ArchUnit)**

Run: `cd service-botica && .\gradlew.bat :security:check --warning-mode all`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/command/ActualizarRolCommand.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/ActualizarRolUseCase.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/command/ActualizarRolHandler.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/configuration/SecurityModuleConfiguration.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/command/ActualizarRolHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(security): agregar caso de uso ActualizarRol

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Caso de uso de lectura — `ObtenerRolUseCase` / `ObtenerRolHandler`

**Files:**
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/query/ObtenerRolQuery.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/ObtenerRolUseCase.java`
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/query/ObtenerRolHandler.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamReadPort.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/adapter/IamJdbcReadAdapter.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/repository/IamJdbcReadRepository.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/query/ObtenerRolHandlerTest.java`

**Interfaces:**
- Consumes: `RolResult` (ya existente, de `application/dto/result/`).
- Produces: `ObtenerRolUseCase.execute(ObtenerRolQuery): Result<RolResult, ApplicationError>` — usado por `RolController` en Task 5. `IamReadPort.findRoleById(UUID tenantId, UUID roleId): Optional<RolResult>`.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/query/ObtenerRolHandlerTest.java`:

```java
package com.softprimesolutions.security.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerRolHandlerTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Test
    void returnsRoleWhenFound() {
        var sampleRole = new RolResult(
                ROLE_ID, TENANT_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO",
                false, Set.of(), "ACTIVO", Instant.parse("2026-09-01T00:00:00Z"), null);
        var handler = new ObtenerRolHandler(new StubReadPort(Optional.of(sampleRole)));

        var result = handler.execute(new ObtenerRolQuery(TENANT_ID, ROLE_ID));

        assertTrue(result.isSuccess());
        assertEquals("ADMIN_LOCAL", result.fold(value -> value.code(), error -> null));
    }

    @Test
    void rejectsWhenRoleIsNotFound() {
        var handler = new ObtenerRolHandler(new StubReadPort(Optional.empty()));

        var result = handler.execute(new ObtenerRolQuery(TENANT_ID, ROLE_ID));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private record StubReadPort(Optional<RolResult> role) implements IamReadPort {

        @Override
        public PaginaResult<UsuarioResult> findUsers(UUID tenantId, String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<RolResult> findRoles(UUID tenantId, String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<PermisoResult> findPermissions(String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RolResult> findRoleById(UUID tenantId, UUID roleId) {
            return role;
        }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.application.usecase.query.ObtenerRolHandlerTest"`
Expected: FAIL — `cannot find symbol: class ObtenerRolQuery` / `class ObtenerRolHandler` / `method findRoleById`.

- [ ] **Step 3: Añadir `findRoleById` a `IamReadPort`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamReadPort.java`, añadir el método:

```java
    java.util.Optional<RolResult> findRoleById(UUID tenantId, UUID roleId);
```

(Se añade con el import completo `java.util.Optional` para no reordenar los imports existentes del archivo; alternativamente añadir `import java.util.Optional;` arriba y usar `Optional<RolResult>` — usar esta segunda forma para mantener el estilo del resto del archivo.)

Archivo completo resultante:

```java
package com.softprimesolutions.security.application.port.out;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import java.util.Optional;
import java.util.UUID;

public interface IamReadPort {

    PaginaResult<UsuarioResult> findUsers(UUID tenantId, String search, int page, int size);

    PaginaResult<RolResult> findRoles(UUID tenantId, String search, int page, int size);

    PaginaResult<PermisoResult> findPermissions(String search, int page, int size);

    Optional<RolResult> findRoleById(UUID tenantId, UUID roleId);
}
```

- [ ] **Step 4: Crear `ObtenerRolQuery.java`**

```java
package com.softprimesolutions.security.application.dto.query;

import java.util.UUID;

public record ObtenerRolQuery(UUID tenantId, UUID roleId) {
}
```

- [ ] **Step 5: Crear `ObtenerRolUseCase.java`**

```java
package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerRolUseCase {
    Result<RolResult, ApplicationError> execute(ObtenerRolQuery query);
}
```

- [ ] **Step 6: Crear `ObtenerRolHandler.java`**

```java
package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.port.in.ObtenerRolUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerRolHandler implements ObtenerRolUseCase {

    private final IamReadPort readPort;

    public ObtenerRolHandler(IamReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(ObtenerRolQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findRoleById(query.tenantId(), query.roleId())
                .map(Result::<RolResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

- [ ] **Step 7: Implementar `findRoleById` en `IamJdbcReadRepository`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/repository/IamJdbcReadRepository.java`, añadir el método justo después de `countRoles(...)`:

```java
    public java.util.Optional<RolProjection> findRoleById(UUID tenantId, UUID roleId) {
        return jdbcClient.sql("""
                        SELECT r.uuid_publico, t.uuid_publico AS tenant_uuid,
                               r.codigo, r.nombre, r.descripcion, r.tipo_rol, r.es_sistema,
                               r.estado, r.created_at, r.updated_at
                        FROM sch_seguridad.rol r
                        JOIN sch_admin.tenant t ON t.id = r.tenant_id
                        WHERE t.uuid_publico = :tenantId AND r.uuid_publico = :roleId
                        """)
                .param("tenantId", tenantId)
                .param("roleId", roleId)
                .query((rs, rowNumber) -> new RolProjection(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("tipo_rol"),
                        rs.getBoolean("es_sistema"),
                        findPermissionCodes(roleId),
                        rs.getString("estado"),
                        toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                        toInstant(rs.getObject("updated_at", OffsetDateTime.class))))
                .optional();
    }
```

- [ ] **Step 8: Implementar `findRoleById` en `IamJdbcReadAdapter`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/adapter/IamJdbcReadAdapter.java`, añadir el método junto a `findRoles`:

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<RolResult> findRoleById(UUID tenantId, UUID roleId) {
        return repository.findRoleById(tenantId, roleId).map(IamReadMapper::toResult);
    }
```

Añadir el import `import java.util.Optional;` al inicio del archivo.

- [ ] **Step 9: Registrar el handler en `SecurityModuleConfiguration`**

Añadir el import y el `@Bean` junto a `listarRolesUseCase`:

```java
import com.softprimesolutions.security.application.port.in.ObtenerRolUseCase;
import com.softprimesolutions.security.application.usecase.query.ObtenerRolHandler;
```

```java
    @Bean
    ObtenerRolUseCase obtenerRolUseCase(IamReadPort readPort) {
        return new ObtenerRolHandler(readPort);
    }
```

- [ ] **Step 10: Ejecutar el test para verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :security:test --tests "com.softprimesolutions.security.application.usecase.query.ObtenerRolHandlerTest"`
Expected: PASS — 2 tests verdes.

- [ ] **Step 11: Ejecutar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :security:check --warning-mode all`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 12: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/dto/query/ObtenerRolQuery.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/in/ObtenerRolUseCase.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/usecase/query/ObtenerRolHandler.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/application/port/out/IamReadPort.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/adapter/IamJdbcReadAdapter.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/persistence/read/repository/IamJdbcReadRepository.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/infrastructure/configuration/SecurityModuleConfiguration.java service-botica/modules/security/src/test/java/com/softprimesolutions/security/application/usecase/query/ObtenerRolHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(security): agregar caso de uso ObtenerRol (GET rol individual)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Endpoints REST — `PUT /roles/{roleId}` y `GET /roles/{roleId}`

**Files:**
- Create: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/ActualizarRolRequest.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java`
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/RolController.java`
- Test: `service-botica/modules/security/src/test/java/com/softprimesolutions/security/api/mapper/IamApiMapperTest.java` — **no existe hoy** (`grep` no encontró test de `IamApiMapper`); no se crea uno nuevo en esta tarea porque el mapper es cubierto indirectamente por el flujo del controller. Si el gate de cobertura 100% lo exige para el nuevo método `toCommand(UUID, ActualizarRolRequest)`, se añade un test mínimo en Step 6.

**Interfaces:**
- Consumes: `ActualizarRolUseCase` (Task 3), `ObtenerRolUseCase` (Task 4), `IamControllerSupport.problem(ApplicationError)` (ya existente).
- Produces: endpoints HTTP `PUT /api/v1/roles/{roleId}` y `GET /api/v1/roles/{roleId}` — consumidos por el frontend en Task 6.

- [ ] **Step 1: Crear `ActualizarRolRequest.java`**

```java
package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ActualizarRolRequest(
        @NotBlank @Size(min = 3, max = 80) String code,
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 500) String description,
        @NotBlank String roleType) {
}
```

(No lleva `UUID` en el import — se deja fuera; solo se usa si se requiriera, pero este DTO no tiene `tenantId`. Eliminar el `import java.util.UUID;` de esta clase ya que no se usa.)

```java
package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarRolRequest(
        @NotBlank @Size(min = 3, max = 80) String code,
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 500) String description,
        @NotBlank String roleType) {
}
```

- [ ] **Step 2: Añadir `toCommand` para editar en `IamApiMapper`**

En `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java`, añadir el import y el método junto a `toCommand(CrearRolRequest)`:

```java
import com.softprimesolutions.security.api.dto.request.ActualizarRolRequest;
import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
```

```java
    public static ActualizarRolCommand toCommand(UUID roleId, ActualizarRolRequest request) {
        return new ActualizarRolCommand(roleId, request.code(), request.name(), request.description(), request.roleType());
    }
```

- [ ] **Step 3: Añadir los endpoints a `RolController`**

Reescribir `service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/RolController.java` completo:

```java
package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.dto.request.ActualizarRolRequest;
import com.softprimesolutions.security.api.dto.request.CrearRolRequest;
import com.softprimesolutions.security.api.dto.request.ReemplazarPermisosRolRequest;
import com.softprimesolutions.security.api.mapper.IamApiMapper;
import com.softprimesolutions.security.application.dto.query.ListarRolesQuery;
import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.port.in.ActualizarRolUseCase;
import com.softprimesolutions.security.application.port.in.CrearRolUseCase;
import com.softprimesolutions.security.application.port.in.ListarRolesUseCase;
import com.softprimesolutions.security.application.port.in.ObtenerRolUseCase;
import com.softprimesolutions.security.application.port.in.ReemplazarPermisosRolUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/roles")
public class RolController {

    private final CrearRolUseCase createRole;
    private final ListarRolesUseCase listRoles;
    private final ObtenerRolUseCase getRole;
    private final ActualizarRolUseCase updateRole;
    private final ReemplazarPermisosRolUseCase replacePermissions;

    public RolController(
            CrearRolUseCase createRole,
            ListarRolesUseCase listRoles,
            ObtenerRolUseCase getRole,
            ActualizarRolUseCase updateRole,
            ReemplazarPermisosRolUseCase replacePermissions) {
        this.createRole = createRole;
        this.listRoles = listRoles;
        this.getRole = getRole;
        this.updateRole = updateRole;
        this.replacePermissions = replacePermissions;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('seguridad.roles.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearRolRequest request) {
        return createRole.execute(IamApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/roles/" + result.id()))
                        .body(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('seguridad.roles.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listRoles.execute(new ListarRolesQuery(tenantId, search, page, size)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toRolPage(result)),
                IamControllerSupport::problem);
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('seguridad.roles.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID roleId, @RequestParam UUID tenantId) {
        return getRole.execute(new ObtenerRolQuery(tenantId, roleId)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('seguridad.roles.gestionar')")
    public ResponseEntity<?> update(@PathVariable UUID roleId, @Valid @RequestBody ActualizarRolRequest request) {
        return updateRole.execute(IamApiMapper.toCommand(roleId, request)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('seguridad.permisos.asignar')")
    public ResponseEntity<?> replacePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody ReemplazarPermisosRolRequest request,
            Authentication authentication) {
        return replacePermissions.execute(IamApiMapper.toCommand(roleId, request, authentication.getName())).fold(
                result -> ResponseEntity.ok(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }
}
```

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :security:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Ejecutar el suite completo del módulo (incluye ArchUnit + Modulith verify)**

Run: `cd service-botica && .\gradlew.bat :security:check --warning-mode all`
Expected: BUILD SUCCESSFUL. Si el gate de cobertura JaCoCo falla por líneas no cubiertas en `RolController`/`IamApiMapper` (los nuevos métodos `getById`, `update`, `toCommand(UUID, ActualizarRolRequest)`), proceder al Step 6.

- [ ] **Step 6: Si el gate de cobertura exige test directo del mapper, añadirlo**

Crear `service-botica/modules/security/src/test/java/com/softprimesolutions/security/api/mapper/IamApiMapperActualizarRolTest.java`:

```java
package com.softprimesolutions.security.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softprimesolutions.security.api.dto.request.ActualizarRolRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IamApiMapperActualizarRolTest {

    @Test
    void mapsActualizarRolRequestToCommand() {
        var roleId = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
        var request = new ActualizarRolRequest("ADMIN_LOCAL", "Administrador local", "Descripción", "ESTABLECIMIENTO");

        var command = IamApiMapper.toCommand(roleId, request);

        assertEquals(roleId, command.roleId());
        assertEquals("ADMIN_LOCAL", command.code());
        assertEquals("Administrador local", command.name());
        assertEquals("Descripción", command.description());
        assertEquals("ESTABLECIMIENTO", command.roleType());
    }
}
```

Ejecutar de nuevo: `cd service-botica && .\gradlew.bat :security:check --warning-mode all` → Expected: BUILD SUCCESSFUL.

Nota: el propio `RolController` (endpoints `getById`/`update`) requiere cobertura de líneas/ramas también. Este módulo no tiene tests `*ApiIntegrationTest` (confirmado — no existe ninguno en `security`), así que la cobertura de controller se resuelve mediante los tests unitarios de `ActualizarRolHandler`/`ObtenerRolHandler` ya escritos (Tasks 3 y 4) más este test de mapper; si JaCoCo exige cobertura línea por línea del propio `RolController.getById`/`update` (código muy simple de delegación), y el build sigue fallando, ejecutar `cd service-botica && .\gradlew.bat :security:jacocoTestReport` y revisar el reporte HTML en `service-botica/modules/security/build/reports/jacoco/test/html/index.html` para identificar la línea exacta no cubierta antes de añadir cualquier test adicional — no añadir tests especulativos sin confirmar primero qué línea falta.

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/dto/request/ActualizarRolRequest.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/mapper/IamApiMapper.java service-botica/modules/security/src/main/java/com/softprimesolutions/security/api/controller/RolController.java
git add service-botica/modules/security/src/test/java/com/softprimesolutions/security/api/mapper/IamApiMapperActualizarRolTest.java 2>/dev/null
git commit -m "$(cat <<'EOF'
feat(security): exponer PUT y GET /api/v1/roles/{roleId}

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Backend completo — validación final

**Files:** ninguno nuevo; solo verificación.

**Interfaces:** ninguna nueva.

- [ ] **Step 1: Ejecutar el build completo del backend**

Run: `cd service-botica && .\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL — todos los módulos compilan, todos los tests pasan, ArchUnit y Spring Modulith `verify()` no reportan violaciones (en particular: `RolController` sigue exponiendo solo `api/`, `ActualizarRolHandler`/`ObtenerRolHandler` viven en `application/usecase/` como el resto).

- [ ] **Step 2: Si falla por cobertura, generar el reporte JaCoCo y corregir**

Run: `cd service-botica && .\gradlew.bat :security:jacocoTestReport`

Abrir `service-botica/modules/security/build/reports/jacoco/test/html/index.html`, ubicar la clase con cobertura <100%, y añadir el test unitario faltante siguiendo el mismo patrón `StubWritePort`/`StubReadPort` ya usado en Tasks 3 y 4. No continuar a Task 7 hasta que este paso pase limpio.

- [ ] **Step 3: No commit en esta tarea** (es solo verificación; cualquier fix se commitea como parte del ciclo TDD del archivo corregido).

---

## Task 7: Frontend — tipos y funciones de API (`roles.types.ts` + `roles.api.ts`)

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/api/roles.types.ts`
- Modify: `frontend/apps/erp-web/src/features/seguridad/api/roles.api.ts`
- Test: `frontend/apps/erp-web/src/features/seguridad/api/roles.api.test.ts`

**Interfaces:**
- Consumes: `ApiClient` de `@boticas/api-client` (`client.get`, `client.put` — ya existentes), `apiClient` de `../../../app/api` (ya existente).
- Produces: `ActualizarRolPayload` (tipo), `fetchRolById(client, roleId, tenantId): Promise<Rol>`, `rolQuery(roleId, tenantId)`, `actualizarRol(client, roleId, payload): Promise<Rol>` — usados por `RoleDetailPage.tsx` en Task 8.

- [ ] **Step 1: Escribir los tests que fallan**

Abrir `frontend/apps/erp-web/src/features/seguridad/api/roles.api.test.ts` y añadir al final del `describe('roles.api', ...)` (antes del cierre `});`):

```ts
  it('fetchRolById consulta /roles/{id} con tenantId', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/roles/rol-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json(sampleRol);
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await fetchRolById(client, 'rol-1', 'tenant-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual(sampleRol);
  });

  it('actualizarRol envia PUT con el payload de edicion', async () => {
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/roles/rol-1', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleRol, name: 'Administrador local editado' });
      })
    );

    const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });
    const result = await actualizarRol(client, 'rol-1', {
      code: 'ADMIN_LOCAL',
      name: 'Administrador local editado',
      roleType: 'ESTABLECIMIENTO'
    });

    expect(receivedBody).toEqual({
      code: 'ADMIN_LOCAL',
      name: 'Administrador local editado',
      roleType: 'ESTABLECIMIENTO'
    });
    expect(result.name).toBe('Administrador local editado');
  });
```

Actualizar el import al inicio del archivo para incluir las nuevas funciones:

```ts
import {
  actualizarRol,
  cambiarEstadoRol,
  crearRol,
  fetchRolById,
  fetchRoles,
  reemplazarPermisosRol,
  rolesQuery
} from './roles.api';
```

- [ ] **Step 2: Ejecutar los tests para verificar que fallan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad/api/roles.api.test.ts`
Expected: FAIL — `fetchRolById is not a function` / `actualizarRol is not a function` (no exportados todavía).

- [ ] **Step 3: Añadir el tipo `ActualizarRolPayload` a `roles.types.ts`**

Al final de `frontend/apps/erp-web/src/features/seguridad/api/roles.types.ts`, añadir:

```ts
export type ActualizarRolPayload = {
  code: string;
  name: string;
  description?: string | undefined;
  roleType: string;
};
```

- [ ] **Step 4: Añadir `fetchRolById`, `rolQuery` y `actualizarRol` a `roles.api.ts`**

En `frontend/apps/erp-web/src/features/seguridad/api/roles.api.ts`, actualizar el import de tipos:

```ts
import type { ActualizarRolPayload, CrearRolPayload, PaginaResponse, Rol } from './roles.types';
```

Y añadir, después de `crearRol`:

```ts
export function fetchRolById(client: ApiClient, roleId: string, tenantId: string): Promise<Rol> {
  return client.get<Rol>(`/roles/${roleId}?tenantId=${encodeURIComponent(tenantId)}`);
}

export function rolQuery(roleId: string, tenantId: string) {
  return queryOptions({
    queryKey: ['seguridad', 'roles', roleId],
    queryFn: () => fetchRolById(apiClient, roleId, tenantId)
  });
}

export function actualizarRol(client: ApiClient, roleId: string, payload: ActualizarRolPayload): Promise<Rol> {
  return client.put<Rol, ActualizarRolPayload>(`/roles/${roleId}`, payload);
}
```

- [ ] **Step 5: Ejecutar los tests para verificar que pasan**

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad/api/roles.api.test.ts`
Expected: PASS — todos los casos verdes (incluidos los 2 nuevos).

- [ ] **Step 6: Ejecutar lint + typecheck**

Run: `pnpm lint && pnpm typecheck`
Expected: sin errores.

- [ ] **Step 7: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/api/roles.types.ts frontend/apps/erp-web/src/features/seguridad/api/roles.api.ts frontend/apps/erp-web/src/features/seguridad/api/roles.api.test.ts
git commit -m "$(cat <<'EOF'
feat(seguridad): agregar fetchRolById y actualizarRol al cliente de roles

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Frontend — botón "Editar rol" en `RoleDetailPage`

**Files:**
- Modify: `frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx`
- Create: `frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.test.tsx`

**Interfaces:**
- Consumes: `rolQuery`, `actualizarRol` (Task 7), `RolForm` (ya existente, `defaultValues`/`submitLabel`/`onSubmit`/`isSubmitting`), `Modal`, `Button`, `Card`, `EstadoBadge` de `@boticas/ui-web` (ya existentes), `ConfirmActionDialog`, `PermisosChecklist` (ya existentes en `../components/`).
- Produces: ninguna nueva interfaz pública — es la última pieza consumidora.

- [ ] **Step 1: Escribir el test que falla**

Crear `frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext } from '../../auth/model/auth-session.context';
import { server } from '../../../test/mocks/server';
import { RoleDetailPage } from './RoleDetailPage';

const authenticatedSession = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

const sampleRol = {
  id: 'rol-1',
  tenantId: 'tenant-1',
  code: 'ADMIN_LOCAL',
  name: 'Administrador local',
  description: null,
  roleType: 'ESTABLECIMIENTO',
  systemRole: false,
  permissionCodes: [],
  status: 'ACTIVO',
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: null
};

const systemRol = { ...sampleRol, id: 'rol-2', code: 'SUPERADMIN', systemRole: true };

function renderPage(roleId = 'rol-1') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return {
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={authenticatedSession}>
          <MemoryRouter initialEntries={[`/seguridad/roles/${roleId}`]}>
            <Routes>
              <Route path="/seguridad/roles/:roleId" element={<RoleDetailPage />} />
            </Routes>
          </MemoryRouter>
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}

describe('RoleDetailPage', () => {
  it('muestra el detalle del rol obtenido por GET individual', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/catalogo/*', () => HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0 })),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    renderPage();

    expect(await screen.findByText('Administrador local')).toBeInTheDocument();
    expect(screen.getByText('ADMIN_LOCAL')).toBeInTheDocument();
  });

  it('edita el rol y refresca el detalle', async () => {
    let currentRol = sampleRol;
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(currentRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.put('*/api/v1/roles/rol-1', async ({ request }) => {
        const body = (await request.json()) as { name: string };
        currentRol = { ...currentRol, name: body.name };
        return HttpResponse.json(currentRol);
      })
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    const nameInput = within(dialog).getByLabelText('Nombre');
    await user.clear(nameInput);
    await user.type(nameInput, 'Administrador local editado');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Administrador local editado' })).toBeInTheDocument());
  });

  it('no muestra el boton editar para un rol de sistema', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-2', () => HttpResponse.json(systemRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 }))
    );

    renderPage('rol-2');
    await screen.findByText('Superadministrador'.length ? systemRol.name : systemRol.name);

    expect(screen.queryByRole('button', { name: 'Editar rol' })).not.toBeInTheDocument();
  });

  it('muestra el error del servidor cuando el code editado esta duplicado', async () => {
    server.use(
      http.get('*/api/v1/roles/rol-1', () => HttpResponse.json(sampleRol)),
      http.get('*/api/v1/permisos', () => HttpResponse.json({ items: [], page: 0, size: 100, totalElements: 0 })),
      http.put('*/api/v1/roles/rol-1', () =>
        HttpResponse.json({ detail: 'Ya existe un rol con el código indicado.' }, { status: 409 })
      )
    );

    const { user } = renderPage();
    await screen.findByText('Administrador local');

    await user.click(screen.getByRole('button', { name: 'Editar rol' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(dialog).findByText('Ya existe un rol con el código indicado.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad/pages/RoleDetailPage.test.tsx`
Expected: FAIL — no existe el botón "Editar rol" ni el modal de edición; el primer test también puede fallar porque `RoleDetailPage` hoy usa `rolesQuery({ size: 100 })` en vez del GET individual mockeado.

- [ ] **Step 3: Leer el archivo actual antes de modificarlo**

Leer `frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx` completo (herramienta Read) para confirmar los imports exactos vigentes antes de reescribirlo — el contenido de referencia ya fue verificado y es el mostrado en el Step 4.

- [ ] **Step 4: Reescribir `RoleDetailPage.tsx`**

Reemplazar el contenido completo de `frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx`:

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { Button, Card, EstadoBadge, Modal } from '@boticas/ui-web';
import { ApiError } from '@boticas/api-client';
import { useAuthSession } from '../../auth';
import { apiClient } from '../../../app/api';
import { actualizarRol, cambiarEstadoRol, reemplazarPermisosRol, rolQuery } from '../api/roles.api';
import { permisosQuery } from '../api/permisos.api';
import { ConfirmActionDialog } from '../components/ConfirmActionDialog';
import { PermisosChecklist } from '../components/PermisosChecklist';
import { RolForm } from '../components/RolForm';
import type { RolFormValues } from '../schemas/rol.schema';

export function RoleDetailPage() {
  const { roleId } = useParams<{ roleId: string }>();
  const { tenantId } = useAuthSession();
  const queryClient = useQueryClient();
  const [confirmDeactivateOpen, setConfirmDeactivateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const rolResult = useQuery({
    ...rolQuery(roleId ?? '', tenantId ?? ''),
    enabled: Boolean(roleId) && Boolean(tenantId)
  });
  const rol = rolResult.data;

  const permisosResult = useQuery(permisosQuery({ size: 100 }));

  const [selectedCodes, setSelectedCodes] = useState<Set<string> | null>(null);
  const effectiveCodes = selectedCodes ?? new Set(rol?.permissionCodes ?? []);

  const savePermissionsMutation = useMutation({
    mutationFn: (codes: string[]) => reemplazarPermisosRol(apiClient, roleId ?? '', codes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (status: string) =>
      cambiarEstadoRol(apiClient, roleId ?? '', tenantId ?? '', status),
    onSuccess: () => {
      setConfirmDeactivateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    }
  });

  const updateMutation = useMutation({
    mutationFn: (values: RolFormValues) =>
      actualizarRol(apiClient, roleId ?? '', {
        code: values.code,
        name: values.name,
        description: values.description,
        roleType: values.roleType
      }),
    onSuccess: () => {
      setEditOpen(false);
      setEditError(null);
      void queryClient.invalidateQueries({ queryKey: ['seguridad', 'roles'] });
    },
    onError: (error: unknown) => {
      setEditError(error instanceof ApiError ? error.message : 'No se pudo actualizar el rol.');
    }
  });

  if (!rol) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando rol…</p>;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Seguridad / Roles
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            {rol.name}
          </h1>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">{rol.code}</p>
        </div>
        <EstadoBadge status={rol.status} />
      </div>

      <div className="mt-4 flex justify-end gap-2">
        {!rol.systemRole && (
          <Button
            variant="secondary"
            onClick={() => {
              setEditError(null);
              setEditOpen(true);
            }}
          >
            Editar rol
          </Button>
        )}
      </div>

      <Card className="mt-6 p-5">
        <h2 className="font-bold text-neutral-950 dark:text-white">Permisos</h2>
        <div className="mt-4">
          <PermisosChecklist
            permisos={permisosResult.data?.items ?? []}
            selectedCodes={effectiveCodes}
            onChange={setSelectedCodes}
          />
        </div>
        <div className="mt-4 flex justify-end">
          <Button
            onClick={() => savePermissionsMutation.mutate([...effectiveCodes])}
            disabled={savePermissionsMutation.isPending}
          >
            Guardar permisos
          </Button>
        </div>
      </Card>

      <div className="mt-6 flex justify-end">
        <Button variant="secondary" onClick={() => setConfirmDeactivateOpen(true)}>
          {rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        </Button>
      </div>

      <ConfirmActionDialog
        open={confirmDeactivateOpen}
        title={rol.status === 'ACTIVO' ? 'Desactivar rol' : 'Activar rol'}
        description={
          rol.status === 'ACTIVO'
            ? 'Los usuarios con este rol perderán los permisos asociados.'
            : 'El rol volverá a estar disponible para asignación.'
        }
        confirmLabel={rol.status === 'ACTIVO' ? 'Desactivar' : 'Activar'}
        tone={rol.status === 'ACTIVO' ? 'danger' : 'default'}
        isPending={toggleStatusMutation.isPending}
        onConfirm={() =>
          toggleStatusMutation.mutate(rol.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')
        }
        onCancel={() => setConfirmDeactivateOpen(false)}
      />

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Editar rol">
        <RolForm
          defaultValues={{
            code: rol.code,
            name: rol.name,
            description: rol.description ?? '',
            roleType: rol.roleType as RolFormValues['roleType'],
            systemRole: rol.systemRole
          }}
          onSubmit={(values) => updateMutation.mutate(values)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
        />
        {editError && (
          <p className="mt-3 text-sm text-red-600 dark:text-red-400" role="alert">
            {editError}
          </p>
        )}
      </Modal>
    </div>
  );
}
```

- [ ] **Step 5: Ejecutar el test para verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad/pages/RoleDetailPage.test.tsx`
Expected: PASS — 4 tests verdes. Si `ApiError` no se exporta desde `@boticas/api-client`, verificar el export real con `grep -rn "export.*ApiError" frontend/packages/api-client/src/index.ts` y ajustar el import.

- [ ] **Step 6: Ejecutar toda la suite de la feature seguridad para detectar regresiones**

Run: `pnpm --filter @boticas/erp-web test -- src/features/seguridad`
Expected: PASS — todos los tests de la feature (incluidos `RolesPage.test.tsx`, `RolForm.test.tsx`, `roles.api.test.ts`) siguen verdes.

- [ ] **Step 7: Ejecutar el check completo del frontend**

Run: `pnpm check`
Expected: lint, typecheck, tests y build sin errores. Si el gate de cobertura por archivo de Vitest falla en `RoleDetailPage.tsx` por alguna rama no cubierta, añadir el caso puntual faltante siguiendo el mismo patrón `server.use(http...)` ya usado en este archivo de test — no añadir tests especulativos, verificar primero con `pnpm --filter @boticas/erp-web test -- --coverage src/features/seguridad/pages/RoleDetailPage.test.tsx` qué línea/rama exacta falta.

- [ ] **Step 8: Commit**

```bash
git add frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.tsx frontend/apps/erp-web/src/features/seguridad/pages/RoleDetailPage.test.tsx
git commit -m "$(cat <<'EOF'
feat(seguridad): agregar edicion de rol en RoleDetailPage

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: Verificación final end-to-end

**Files:** ninguno nuevo.

- [ ] **Step 1: Backend — build completo**

Run: `cd service-botica && .\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Frontend — build completo**

Run: `pnpm check`
Expected: sin errores (lint + typecheck + test + build).

- [ ] **Step 3: Smoke manual opcional (si hay backend + Postgres local disponibles)**

Levantar backend: `cd service-botica && .\gradlew.bat :bootstrap-app:bootRun` (requiere `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` configurados y PostgreSQL corriendo). Levantar frontend con mocks desactivados: `cd frontend && pnpm dev` contra el backend local. Navegar a `/seguridad/roles`, entrar al detalle de un rol no-sistema, click en "Editar rol", cambiar el nombre, guardar, y confirmar que el título de la página se actualiza sin recargar. Este paso es opcional y depende de infraestructura local disponible — no bloquea el cierre de la tarea si no se dispone de Postgres en el entorno actual.

- [ ] **Step 4: No commit** (verificación pura).

---

## Self-Review (completado durante la escritura de este plan)

**Cobertura del spec:**
- Mutador de dominio `updateDetails` con Result Pattern → Task 1. ✓
- Regla de negocio "rol de sistema no editable" en el dominio → Task 1 (`SEC_ROL_SISTEMA_NO_EDITABLE`). ✓
- Regla de negocio "code duplicado" → Task 2 (puerto) + Task 3 (handler). ✓
- Endpoint `PUT /api/v1/roles/{roleId}` → Task 5. ✓
- Endpoint `GET /api/v1/roles/{roleId}` → Task 4 (caso de uso) + Task 5 (controller). ✓
- Frontend `actualizarRol`/`fetchRolById`/`rolQuery` → Task 7. ✓
- Botón "Editar rol" oculto para roles de sistema, modal con `RolForm`, manejo de error 409 inline → Task 8. ✓
- Fuera de alcance (cambio de estado, reemplazo de permisos, ámbitos de asignación, migraciones, E2E Playwright): no se tocan en ninguna tarea — verificado. ✓

**Escaneo de placeholders:** sin "TBD"/"TODO" en ningún step; todo código mostrado es completo y compilable con los imports reales verificados contra el código fuente actual.

**Consistencia de tipos:** `ActualizarRolCommand(roleId, code, name, description, roleType)` se usa idéntico en Task 3 (handler), Task 5 (`IamApiMapper.toCommand`) — mismos nombres de campo en los tres lugares. `ActualizarRolPayload` (frontend, Task 7) y `RolFormValues` (ya existente) se mapean campo a campo en Task 8 sin reinterpretación. `rolQuery(roleId, tenantId)` se define en Task 7 y se consume con la misma firma en Task 8.
