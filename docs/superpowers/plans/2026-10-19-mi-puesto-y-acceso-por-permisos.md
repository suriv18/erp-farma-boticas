# Mi puesto de trabajo y acceso por permisos — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que un cajero recorra login → Caja → POS → cierre de turno con permisos mínimos (`GET /api/v1/organizacion/mi-puesto`), y que la interfaz solo muestre lo que sus permisos permiten.

**Architecture:** `security` publica `AlcanceUsuario` (alcance organizacional del usuario) y agrega `permissions` a `GET /auth/me`; `organizacion` consume `security::api` (mismo patrón que `ventas`), filtra su estructura por ese alcance y publica `mi-puesto`. El frontend lee `mi-puesto` en Caja y POS, filtra menú y rutas por `permissions` y acepta usuario o correo en el login.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1; React 19.2, TanStack Query, zod, Vitest + MSW, Playwright, pnpm.

Spec: `docs/superpowers/specs/2026-10-19-mi-puesto-y-acceso-por-permisos-design.md`.

## Global Constraints

- Todo archivo fuente **nuevo** (backend y frontend, código y no tests) debe tener 100 % de cobertura de líneas y ramas (JaCoCo por clase; Vitest por archivo). No se agregan archivos a `jacoco-baseline.txt` ni a `coverage-baseline.txt`.
- Sin comentarios explicativos en el código; sin código duplicado; lambdas donde sea idiomático.
- Arquitectura: Clean Architecture + DDD + Ports & Adapters + CQRS con `Result<T, ApplicationError>`; un módulo solo expone tipos por su paquete `api`; toda dependencia entre módulos se declara en `package-info.java` (`allowedDependencies`) **y** en `build.gradle`.
- El tenant y el usuario salen siempre del JWT (`tid`, `sub`).
- `mi-puesto` y `/auth/me` solo exigen `isAuthenticated()`; la operación sigue protegida por `@PreAuthorize` y `AutorizacionAmbito`.
- Rutas: `GET /api/v1/organizacion/mi-puesto`; `GET /api/v1/auth/me` agrega `permissions`.
- Frontend: features por dominio con `index.ts`; las features no importan archivos internos de otras; `forwardRef` y React Router 8 bloqueados por ESLint.
- Sin migraciones de esquema (V041/V042 siguen reservadas para el diseño del 15-oct).
- Comandos backend desde `service-botica/` (PowerShell): `.\gradlew.bat ...`; frontend desde `frontend/` (`pnpm.cmd` si `pnpm.ps1` está bloqueado).
- Commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

Rutas abreviadas: `SEC` = `service-botica/modules/security/src/main/java/com/softprimesolutions/security`, `SECT` = `service-botica/modules/security/src/test/java/com/softprimesolutions/security`, `ORG` = `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion`, `ORGT` = `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`, `SRC` = `frontend/apps/erp-web/src`, `E2E` = `frontend/e2e`.

---

### Task 1: Alcance del usuario en `security.api`

**Files:**
- Create: `SEC/api/AlcanceUsuario.java`
- Create: `SEC/api/AlcanceResultado.java`
- Create: `SEC/infrastructure/persistence/read/adapter/AlcanceUsuarioJdbcAdapter.java`
- Modify: `SEC/infrastructure/persistence/read/adapter/AutorizacionAmbitoJdbcAdapter.java`
- Test: `SECT/api/AlcanceResultadoTest.java`
- Test: `SECT/infrastructure/persistence/read/adapter/AlcanceUsuarioJdbcAdapterTest.java`

**Interfaces:**
- Produces: `security.api.AlcanceUsuario#alcanceDe(UUID tenantId, UUID usuarioId): AlcanceResultado`; `security.api.AlcanceResultado(boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales)` (UUID públicos, conjuntos inmutables).

- [ ] **Step 1: Escribir los tests que fallan**

`SECT/api/AlcanceResultadoTest.java`:

```java
package com.softprimesolutions.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlcanceResultadoTest {

    @Test
    void keepsAnImmutableCopyOfEveryScopeSet() {
        var empresa = UUID.randomUUID();
        var origen = new HashSet<>(Set.of(empresa));

        var alcance = new AlcanceResultado(true, origen, Set.of(), Set.of(), Set.of());
        origen.clear();

        assertThat(alcance.global()).isTrue();
        assertThat(alcance.empresas()).containsExactly(empresa);
        assertThatThrownBy(() -> alcance.empresas().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

`SECT/infrastructure/persistence/read/adapter/AlcanceUsuarioJdbcAdapterTest.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.security.infrastructure.persistence.read.adapter.AlcanceUsuarioJdbcAdapter.Fila;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class AlcanceUsuarioJdbcAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID EMPRESA = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO = UUID.randomUUID();
    private static final UUID ALMACEN = UUID.randomUUID();
    private static final UUID TERMINAL = UUID.randomUUID();

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    private final AlcanceUsuarioJdbcAdapter adapter = new AlcanceUsuarioJdbcAdapter(jdbc);

    @SuppressWarnings("unchecked")
    private void respondWith(List<Fila> filas) {
        var query = (JdbcClient.MappedQuerySpec<Fila>) mock(JdbcClient.MappedQuerySpec.class);
        when(query.list()).thenReturn(filas);
        when(jdbc.sql(anyString())).thenReturn(statement);
        when(statement.query(ArgumentMatchers.<RowMapper<Fila>>any())).thenReturn(query);
    }

    @Test
    void collectsTheResourcesOfEveryAssignmentByScopeType() {
        respondWith(List.of(
                new Fila("EMPRESA", EMPRESA, null, null, null),
                new Fila("ESTABLECIMIENTO", EMPRESA, ESTABLECIMIENTO, null, null),
                new Fila("ALMACEN", EMPRESA, ESTABLECIMIENTO, ALMACEN, null),
                new Fila("TERMINAL", EMPRESA, ESTABLECIMIENTO, null, TERMINAL)));

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isFalse();
        assertThat(alcance.empresas()).containsExactly(EMPRESA);
        assertThat(alcance.establecimientos()).containsExactly(ESTABLECIMIENTO);
        assertThat(alcance.almacenes()).containsExactly(ALMACEN);
        assertThat(alcance.terminales()).containsExactly(TERMINAL);
        verify(statement).param("tenantId", TENANT);
        verify(statement).param("usuarioId", USUARIO);
    }

    @Test
    void reportsAGlobalAssignmentWithoutAnyResource() {
        respondWith(List.of(new Fila("GLOBAL", null, null, null, null)));

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isTrue();
        assertThat(alcance.empresas()).isEmpty();
        assertThat(alcance.terminales()).isEmpty();
    }

    @Test
    void returnsAnEmptyScopeWhenThereAreNoCurrentAssignments() {
        respondWith(List.of());

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isFalse();
        assertThat(alcance.establecimientos()).isEmpty();
    }

    @Test
    void mapsEveryColumnOfTheAssignmentRow() throws SQLException {
        var rs = mock(ResultSet.class);
        when(rs.getString("tipo_ambito")).thenReturn("TERMINAL");
        when(rs.getObject("empresa_uuid", UUID.class)).thenReturn(EMPRESA);
        when(rs.getObject("establecimiento_uuid", UUID.class)).thenReturn(ESTABLECIMIENTO);
        when(rs.getObject("almacen_uuid", UUID.class)).thenReturn(null);
        when(rs.getObject("terminal_uuid", UUID.class)).thenReturn(TERMINAL);

        assertThat(AlcanceUsuarioJdbcAdapter.fila(rs, 0))
                .isEqualTo(new Fila("TERMINAL", EMPRESA, ESTABLECIMIENTO, null, TERMINAL));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:security:test --tests "com.softprimesolutions.security.api.AlcanceResultadoTest" --tests "com.softprimesolutions.security.infrastructure.persistence.read.adapter.AlcanceUsuarioJdbcAdapterTest"`
Expected: FAIL de compilación (`AlcanceResultado`, `AlcanceUsuarioJdbcAdapter` no existen).

- [ ] **Step 3: Implementar**

`SEC/api/AlcanceUsuario.java`:

```java
package com.softprimesolutions.security.api;

import java.util.UUID;

public interface AlcanceUsuario {

    AlcanceResultado alcanceDe(UUID tenantId, UUID usuarioId);
}
```

`SEC/api/AlcanceResultado.java`:

```java
package com.softprimesolutions.security.api;

import java.util.Set;
import java.util.UUID;

public record AlcanceResultado(
        boolean global,
        Set<UUID> empresas,
        Set<UUID> establecimientos,
        Set<UUID> almacenes,
        Set<UUID> terminales) {

    public AlcanceResultado {
        empresas = Set.copyOf(empresas);
        establecimientos = Set.copyOf(establecimientos);
        almacenes = Set.copyOf(almacenes);
        terminales = Set.copyOf(terminales);
    }
}
```

`SEC/infrastructure/persistence/read/adapter/AutorizacionAmbitoJdbcAdapter.java`: reemplazar en `PERMITE` las líneas de estado y vigencia por el fragmento compartido. Reemplazar

```java
                   AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO' AND a.estado = 'ACTIVO'
                   AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                   AND a.vigente_desde <= CURRENT_TIMESTAMP
                   AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= CURRENT_TIMESTAMP)
                   AND (a.tipo_ambito = 'GLOBAL'
```

por

```java
                   AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
                   AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                   AND %s
                   AND (a.tipo_ambito = 'GLOBAL'
```

y el cierre del text block `""";` de `PERMITE` por `""".formatted(ASIGNACION_VIGENTE);`. Agregar antes de `PERMITE`:

```java
    static final String ASIGNACION_VIGENTE = """
            a.estado = 'ACTIVO'
            AND a.vigente_desde <= CURRENT_TIMESTAMP
            AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= CURRENT_TIMESTAMP)
            """;

```

`SEC/infrastructure/persistence/read/adapter/AlcanceUsuarioJdbcAdapter.java`:

```java
package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import static java.util.stream.Collectors.toSet;

import com.softprimesolutions.security.api.AlcanceResultado;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AlcanceUsuarioJdbcAdapter implements AlcanceUsuario {

    private static final String ASIGNACIONES = """
            SELECT a.tipo_ambito, e.uuid_publico AS empresa_uuid, s.uuid_publico AS establecimiento_uuid,
                   w.uuid_publico AS almacen_uuid, p.uuid_publico AS terminal_uuid
              FROM sch_seguridad.usuario_rol_ambito a
              JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
              JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
         LEFT JOIN sch_organizacion.empresa_operadora e ON e.id = a.empresa_id
         LEFT JOIN sch_organizacion.establecimiento_farmaceutico s ON s.id = a.establecimiento_id
         LEFT JOIN sch_organizacion.almacen w ON w.id = a.almacen_id
         LEFT JOIN sch_organizacion.terminal_pos p ON p.id = a.terminal_id
             WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :usuarioId
               AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
               AND %s
            """.formatted(AutorizacionAmbitoJdbcAdapter.ASIGNACION_VIGENTE);

    record Fila(String tipo, UUID empresa, UUID establecimiento, UUID almacen, UUID terminal) {
    }

    private final JdbcClient jdbc;

    public AlcanceUsuarioJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public AlcanceResultado alcanceDe(UUID tenantId, UUID usuarioId) {
        var filas = jdbc.sql(ASIGNACIONES)
                .param("tenantId", tenantId)
                .param("usuarioId", usuarioId)
                .query(AlcanceUsuarioJdbcAdapter::fila)
                .list();
        return new AlcanceResultado(
                filas.stream().anyMatch(fila -> "GLOBAL".equals(fila.tipo())),
                idsDe(filas, "EMPRESA", Fila::empresa),
                idsDe(filas, "ESTABLECIMIENTO", Fila::establecimiento),
                idsDe(filas, "ALMACEN", Fila::almacen),
                idsDe(filas, "TERMINAL", Fila::terminal));
    }

    static Fila fila(ResultSet rs, int rowNumber) throws SQLException {
        return new Fila(
                rs.getString("tipo_ambito"),
                rs.getObject("empresa_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class),
                rs.getObject("almacen_uuid", UUID.class),
                rs.getObject("terminal_uuid", UUID.class));
    }

    private static Set<UUID> idsDe(List<Fila> filas, String tipo, Function<Fila, UUID> id) {
        return filas.stream().filter(fila -> tipo.equals(fila.tipo())).map(id).collect(toSet());
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:security:check`
Expected: PASS (incluye `AutorizacionAmbitoJdbcAdapterTest` sin cambios y el gate JaCoCo).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/security
git commit -m "feat(security): consulta de alcance organizacional del usuario en la API publica

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Permisos efectivos en `GET /auth/me`

**Files:**
- Modify: `SEC/application/dto/result/PerfilResult.java`
- Modify: `SEC/infrastructure/persistence/read/mapper/IamReadMapper.java:38-47`
- Modify: `SEC/application/usecase/query/ObtenerMiPerfilHandler.java`
- Modify: `SEC/infrastructure/configuration/SecurityModuleConfiguration.java:116-119`
- Modify (tests): `SECT/application/usecase/query/ObtenerMiPerfilHandlerTest.java`, `SECT/api/controller/PerfilControllerTest.java`, `SECT/infrastructure/persistence/read/mapper/IamReadMapperPerfilTest.java`, `BTEST/security/api/IamApiIntegrationTest.java` (assert de `/auth/me`, ~línea 540)

**Interfaces:**
- Consumes: `SecurityControlPort#findEffectivePermissions(UUID tenantId, UUID userId, Instant at): Set<String>` (ordenado por código), `ClockPort#now(): Instant`.
- Produces: `PerfilResult(userId, tenantId, displayName, username, email, List<Asignacion> assignments, List<String> permissions)` y `PerfilResult#conPermisos(Collection<String>)`.

- [ ] **Step 1: Escribir los tests que fallan**

Reemplazar completo `SECT/application/usecase/query/ObtenerMiPerfilHandlerTest.java`:

```java
package com.softprimesolutions.security.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.security.application.port.out.SecurityControlPort;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerMiPerfilHandlerTest {

    private static final UUID USER_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");
    private static final ClockPort CLOCK = () -> NOW;

    private final SecurityControlPort controlPort = mock(SecurityControlPort.class);

    @Test
    void returnsTheProfileWithTheEffectivePermissionsOfTheAuthenticatedUser() {
        var profile = new PerfilResult(
                USER_ID, TENANT_ID, "Luis Paredes Soto", "crit001.manana1", "crit001.manana1@boticas.pe",
                List.of(new PerfilResult.Asignacion(
                        "CAJERO", "Cajero de establecimiento", "ESTABLECIMIENTO", "Botica Crítica Norte")),
                List.of());
        when(controlPort.findEffectivePermissions(TENANT_ID, USER_ID, NOW))
                .thenReturn(new LinkedHashSet<>(List.of("ventas.turnos.abrir", "ventas.ventas.registrar")));
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.of(profile)), controlPort, CLOCK);

        var result = handler.execute(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID));

        assertTrue(result.isSuccess());
        assertEquals(
                profile.conPermisos(List.of("ventas.turnos.abrir", "ventas.ventas.registrar")),
                result.getOrElse(error -> null));
    }

    @Test
    void rejectsWhenTheAuthenticatedUserDoesNotExist() {
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.empty()), controlPort, CLOCK);

        var result = handler.execute(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID));

        assertTrue(result.isFailure());
        assertEquals("SEC_USUARIO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresItsCollaborators() {
        var readPort = new StubReadPort(Optional.empty());
        assertThrows(NullPointerException.class, () -> new ObtenerMiPerfilHandler(null, controlPort, CLOCK));
        assertThrows(NullPointerException.class, () -> new ObtenerMiPerfilHandler(readPort, null, CLOCK));
        assertThrows(NullPointerException.class, () -> new ObtenerMiPerfilHandler(readPort, controlPort, null));
        var handler = new ObtenerMiPerfilHandler(readPort, controlPort, CLOCK);
        assertThrows(NullPointerException.class, () -> handler.execute(null));
    }

    @Test
    void keepsAnEmptyPermissionListForAUserWithoutAssignments() {
        var profile = new PerfilResult(USER_ID, TENANT_ID, "Luis", "luis", "luis@boticas.pe", List.of(), List.of());
        when(controlPort.findEffectivePermissions(TENANT_ID, USER_ID, NOW)).thenReturn(Set.of());
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.of(profile)), controlPort, CLOCK);

        var result = handler.execute(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID));

        assertEquals(List.of(), result.getOrElse(error -> null).permissions());
    }

    private record StubReadPort(Optional<PerfilResult> profile) implements IamReadPort {

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
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PerfilResult> findProfile(UUID tenantId, UUID userId) {
            return profile;
        }
    }
}
```

En `SECT/api/controller/PerfilControllerTest.java`, la construcción del perfil pasa a:

```java
        var profile = new PerfilResult(USER_ID, TENANT_ID, "Luis Paredes Soto", "luis", "luis@boticas.pe", List.of(), List.of());
```

En `SECT/infrastructure/persistence/read/mapper/IamReadMapperPerfilTest.java`, agregar `, List.of()` como último argumento del `new PerfilResult(...)` esperado en el primer test (después de la lista de `Asignacion`) y cambiar la última aserción del segundo test a:

```java
        assertEquals(List.of(), result.assignments());
        assertEquals(List.of(), result.permissions());
```

En `BTEST/security/api/IamApiIntegrationTest.java`, en el bloque `/api/v1/auth/me` del test del cajero `TIENDA`, agregar después de `$.assignments[0].scopeName`:

```java
                .andExpect(jsonPath("$.permissions.length()").value(2))
                .andExpect(jsonPath("$.permissions[0]").value("seguridad.sesiones.consultar"))
                .andExpect(jsonPath("$.permissions[1]").value("seguridad.usuarios.gestionar"));
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:security:test --tests "*ObtenerMiPerfilHandlerTest"`
Expected: FAIL de compilación (`PerfilResult` de 6 parámetros, constructor del handler de un parámetro).

- [ ] **Step 3: Implementar**

`SEC/application/dto/result/PerfilResult.java`:

```java
package com.softprimesolutions.security.application.dto.result;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record PerfilResult(
        UUID userId,
        UUID tenantId,
        String displayName,
        String username,
        String email,
        List<Asignacion> assignments,
        List<String> permissions) {

    public PerfilResult conPermisos(Collection<String> permisos) {
        return new PerfilResult(
                userId, tenantId, displayName, username, email, assignments, List.copyOf(permisos));
    }

    public record Asignacion(String roleCode, String roleName, String scopeType, String scopeName) {
    }
}
```

`SEC/infrastructure/persistence/read/mapper/IamReadMapper.java` (método `toResult(PerfilProjection)`): el `return` pasa a

```java
        return new PerfilResult(
                value.userId(), value.tenantId(), value.displayName(), value.username(), value.email(),
                assignments, List.of());
```

`SEC/application/usecase/query/ObtenerMiPerfilHandler.java`:

```java
package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.security.application.port.in.ObtenerMiPerfilUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.security.application.port.out.SecurityControlPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerMiPerfilHandler implements ObtenerMiPerfilUseCase {

    private final IamReadPort readPort;
    private final SecurityControlPort controlPort;
    private final ClockPort clock;

    public ObtenerMiPerfilHandler(IamReadPort readPort, SecurityControlPort controlPort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.controlPort = Objects.requireNonNull(controlPort, "controlPort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<PerfilResult, ApplicationError> execute(ObtenerMiPerfilQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findProfile(query.tenantId(), query.userId())
                .map(perfil -> perfil.conPermisos(
                        controlPort.findEffectivePermissions(query.tenantId(), query.userId(), clock.now())))
                .map(Result::<PerfilResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_USUARIO_NO_ENCONTRADO", "El usuario autenticado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

`SEC/infrastructure/configuration/SecurityModuleConfiguration.java` (bean `obtenerMiPerfilUseCase`):

```java
    @Bean
    ObtenerMiPerfilUseCase obtenerMiPerfilUseCase(
            IamReadPort readPort, SecurityControlPort controlPort, ClockPort iamClockPort) {
        return new ObtenerMiPerfilHandler(readPort, controlPort, iamClockPort);
    }
```

Si `SecurityModuleConfigurationTest` invoca `obtenerMiPerfilUseCase(readPort)`, actualizar esa llamada con los tres argumentos (`mock(SecurityControlPort.class)` y un `ClockPort` lambda).

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:security:check` y luego `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.api.IamApiIntegrationTest"`
Expected: PASS (el segundo requiere Docker para Testcontainers).

- [ ] **Step 5: Commit**

```bash
git add service-botica
git commit -m "feat(security): permisos efectivos del usuario en GET /auth/me

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Dependencia `organizacion → security` y lectura de recursos del puesto

**Files:**
- Modify: `service-botica/modules/security/src/main/java/com/softprimesolutions/security/package-info.java`
- Modify: `ORG/package-info.java`
- Modify: `service-botica/modules/organizacion/build.gradle`
- Create: `ORG/application/dto/result/AlmacenPuestoResult.java`, `TerminalPuestoResult.java`, `EstablecimientoPuestoResult.java`, `MiPuestoResult.java`
- Modify: `ORG/application/port/out/OrganizacionReadPort.java`
- Modify: `ORG/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapter.java`
- Test: `ORGT/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapterTest.java`

**Interfaces:**
- Produces: `OrganizacionReadPort#findEstablecimientosPuesto(UUID tenantId): List<EstablecimientoPuestoResult>` (establecimientos con `estado_operativo = 'ACTIVO'` cuya empresa está en estado `ACTIVO`, con todos sus almacenes activos y terminales activos —`es_activo`—, sin filtrar por alcance); `EstablecimientoPuestoResult(UUID id, UUID empresaId, String codigo, String nombre, List<AlmacenPuestoResult> almacenes, List<TerminalPuestoResult> terminales)`; `AlmacenPuestoResult(UUID id, String codigo, String nombre, String tipo, boolean permiteVenta)`; `TerminalPuestoResult(UUID id, String codigo, String nombre, String estado, boolean operable)`; `MiPuestoResult(List<EstablecimientoPuestoResult> establecimientos)`.

- [ ] **Step 1: Escribir el test que falla**

Agregar al final de `ORGT/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapterTest.java` (antes de la `}` de cierre de la clase), reutilizando los helpers `empresa`, `establecimiento`, `almacen` y `terminal` ya presentes; añadir los imports `com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult`, `EstablecimientoPuestoResult` y `TerminalPuestoResult`:

```java
    @Test
    void listsTheActiveEstablishmentsOfActiveCompaniesWithTheirSellableWarehousesAndOperableTerminals() {
        var empresaActiva = UUID.randomUUID();
        var empresaBloqueada = UUID.randomUUID();
        var estActivo = UUID.randomUUID();
        var estSuspendido = UUID.randomUUID();
        var estDeEmpresaBloqueada = UUID.randomUUID();
        var almacenVenta = UUID.randomUUID();
        var terminalActiva = UUID.randomUUID();
        var terminalMantenimiento = UUID.randomUUID();
        when(repository.findAllEmpresasActivas(TENANT)).thenReturn(List.of(
                empresa(empresaActiva, "A SAC"), empresa(empresaBloqueada, "B SAC", "BLOQUEADO")));
        when(repository.findAllEstablecimientosActivos(TENANT)).thenReturn(List.of(
                establecimiento(estActivo, empresaActiva, "A1"),
                establecimiento(estSuspendido, empresaActiva, "A2", "SUSPENDIDO"),
                establecimiento(estDeEmpresaBloqueada, empresaBloqueada, "B1")));
        when(repository.findAllAlmacenes(TENANT)).thenReturn(List.of(
                almacen(almacenVenta, estActivo, "Venta", true), almacen(UUID.randomUUID(), estActivo, "Inactivo", false)));
        when(repository.findAllTerminalesActivos(TENANT)).thenReturn(List.of(
                terminal(terminalActiva, estActivo, "Caja 1"),
                terminal(terminalMantenimiento, estActivo, "Caja 2", "MANTENIMIENTO")));

        var puesto = adapter.findEstablecimientosPuesto(TENANT);

        assertThat(puesto).singleElement().satisfies(establecimiento -> {
            assertThat(establecimiento.id()).isEqualTo(estActivo);
            assertThat(establecimiento.empresaId()).isEqualTo(empresaActiva);
            assertThat(establecimiento.almacenes()).extracting(AlmacenPuestoResult::id).containsExactly(almacenVenta);
            assertThat(establecimiento.terminales()).extracting(TerminalPuestoResult::id)
                    .containsExactly(terminalActiva, terminalMantenimiento);
            assertThat(establecimiento.terminales()).extracting(TerminalPuestoResult::operable)
                    .containsExactly(true, false);
            assertThat(establecimiento.terminales()).extracting(TerminalPuestoResult::estado)
                    .containsExactly("ACTIVO", "MANTENIMIENTO");
        });
    }
```

(`almacen(id, estId, nombre, activo)` ya existe en la clase y deja `permiteVenta = true` y `tipo` fijos; el almacén inactivo se excluye por `activo = false`. Ajustar el import de `EstablecimientoPuestoResult` solo si el IDE lo marca como no usado.)

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:organizacion:test --tests "*OrganizacionJdbcReadAdapterTest"`
Expected: FAIL de compilación (`findEstablecimientosPuesto` no existe).

- [ ] **Step 3: Implementar**

`security/package-info.java`: `allowedDependencies = {}`.

`ORG/package-info.java`: `allowedDependencies = {"security::api"}`.

`organizacion/build.gradle`: agregar tras `implementation project(':shared-persistence')`:

```groovy
    implementation project(':modules:security')
```

Crear los records (cada uno en `ORG/application/dto/result/`, paquete `com.softprimesolutions.organizacion.application.dto.result`):

```java
public record AlmacenPuestoResult(UUID id, String codigo, String nombre, String tipo, boolean permiteVenta) {
}
```

```java
public record TerminalPuestoResult(UUID id, String codigo, String nombre, String estado, boolean operable) {
}
```

```java
public record EstablecimientoPuestoResult(
        UUID id,
        UUID empresaId,
        String codigo,
        String nombre,
        List<AlmacenPuestoResult> almacenes,
        List<TerminalPuestoResult> terminales) {

    public EstablecimientoPuestoResult {
        almacenes = List.copyOf(almacenes);
        terminales = List.copyOf(terminales);
    }
}
```

```java
public record MiPuestoResult(List<EstablecimientoPuestoResult> establecimientos) {

    public MiPuestoResult {
        establecimientos = List.copyOf(establecimientos);
    }
}
```

(Cada archivo con su `package` e imports de `java.util.List`/`java.util.UUID`. Los records con constructor compacto necesitan sus propios tests en la Task 4.)

`ORG/application/port/out/OrganizacionReadPort.java`: agregar el import de `EstablecimientoPuestoResult`, `java.util.List` y el método

```java
    List<EstablecimientoPuestoResult> findEstablecimientosPuesto(UUID tenantId);
```

`ORG/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapter.java`: agregar imports de `AlmacenPuestoResult`, `EstablecimientoPuestoResult`, `TerminalPuestoResult` y el método:

```java
    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoPuestoResult> findEstablecimientosPuesto(UUID tenantId) {
        var empresasActivas = repository.findAllEmpresasActivas(tenantId).stream()
                .filter(empresa -> "ACTIVO".equals(empresa.estado()))
                .map(EmpresaProjection::uuidPublico)
                .collect(toSet());
        var almacenes = groupedBy(
                repository.findAllAlmacenes(tenantId).stream().filter(AlmacenProjection::activo).toList(),
                AlmacenProjection::establecimientoUuid,
                almacen -> new AlmacenPuestoResult(
                        almacen.uuidPublico(), almacen.codigo(), almacen.nombre(), almacen.tipo(),
                        almacen.permiteVenta()));
        var terminales = groupedBy(
                repository.findAllTerminalesActivos(tenantId), TerminalProjection::establecimientoUuid,
                terminal -> new TerminalPuestoResult(
                        terminal.uuidPublico(), terminal.codigo(), terminal.nombre(), terminal.estado(),
                        "ACTIVO".equals(terminal.estado())));
        return repository.findAllEstablecimientosActivos(tenantId).stream()
                .filter(establecimiento -> "ACTIVO".equals(establecimiento.estadoOperativo()))
                .filter(establecimiento -> empresasActivas.contains(establecimiento.empresaUuid()))
                .map(establecimiento -> new EstablecimientoPuestoResult(
                        establecimiento.uuidPublico(), establecimiento.empresaUuid(), establecimiento.codigo(),
                        establecimiento.nombre(),
                        almacenes.getOrDefault(establecimiento.uuidPublico(), List.of()),
                        terminales.getOrDefault(establecimiento.uuidPublico(), List.of())))
                .toList();
    }
```

Agregar `import static java.util.stream.Collectors.toSet;` junto a los imports estáticos existentes de `Collectors`. Agregar el import de `EmpresaProjection` si no está.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:organizacion:compileTestJava` — corregir cualquier doble de prueba que implemente `OrganizacionReadPort` a mano (casi todos usan `mock(...)`; si alguno es una clase o record, agregarle `findEstablecimientosPuesto` lanzando `UnsupportedOperationException`). Luego: `.\gradlew.bat :modules:organizacion:test :architectureTest`
Expected: PASS (los records nuevos sin test propio se cubren en la Task 4; si el gate JaCoCo de `check` falla ahora por ellos, ejecutar solo `test` en esta tarea).

- [ ] **Step 5: Commit**

```bash
git add service-botica
git commit -m "feat(organizacion): lectura de establecimientos, almacenes y terminales del puesto

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Caso de uso `ObtenerMiPuesto`

**Files:**
- Create: `ORG/application/dto/query/ObtenerMiPuestoQuery.java`
- Create: `ORG/application/port/in/ObtenerMiPuestoUseCase.java`
- Create: `ORG/application/port/out/AlcancePuestoPort.java`
- Create: `ORG/application/dto/result/AlcancePuesto.java`
- Create: `ORG/application/usecase/query/ObtenerMiPuestoHandler.java`
- Create: `ORG/infrastructure/client/SecurityAlcanceAdapter.java`
- Modify: `ORG/infrastructure/configuration/OrganizacionModuleConfiguration.java` (bean al final, antes de `}`)
- Test: `ORGT/application/dto/result/PuestoResultsTest.java`, `ORGT/application/usecase/query/ObtenerMiPuestoHandlerTest.java`, `ORGT/infrastructure/client/SecurityAlcanceAdapterTest.java`, `ORGT/infrastructure/configuration/OrganizacionModuleConfigurationTest.java`

**Interfaces:**
- Consumes: `OrganizacionReadPort#findEstablecimientosPuesto`, `security.api.AlcanceUsuario#alcanceDe`.
- Produces: `ObtenerMiPuestoUseCase#execute(ObtenerMiPuestoQuery): Result<MiPuestoResult, ApplicationError>`; `ObtenerMiPuestoQuery(UUID tenantId, UUID usuarioId)`; `AlcancePuestoPort#alcanceDe(UUID tenantId, UUID usuarioId): AlcancePuesto`; `AlcancePuesto(boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales)`.

- [ ] **Step 1: Escribir los tests que fallan**

`ORGT/application/usecase/query/ObtenerMiPuestoHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPuestoResult;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerMiPuestoHandlerTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID EMPRESA_A = UUID.randomUUID();
    private static final UUID EMPRESA_B = UUID.randomUUID();
    private static final UUID EST_A = UUID.randomUUID();
    private static final UUID EST_B = UUID.randomUUID();
    private static final UUID ALM_A = UUID.randomUUID();
    private static final UUID TERM_A1 = UUID.randomUUID();
    private static final UUID TERM_A2 = UUID.randomUUID();

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final AlcancePuestoPort alcancePort = mock(AlcancePuestoPort.class);
    private final ObtenerMiPuestoHandler handler = new ObtenerMiPuestoHandler(readPort, alcancePort);

    private static AlcancePuesto alcance(
            boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales) {
        return new AlcancePuesto(global, empresas, establecimientos, almacenes, terminales);
    }

    private void withScope(AlcancePuesto alcance) {
        when(readPort.findEstablecimientosPuesto(TENANT)).thenReturn(List.of(
                new EstablecimientoPuestoResult(EST_A, EMPRESA_A, "A", "Botica A",
                        List.of(new AlmacenPuestoResult(ALM_A, "ALM", "Almacen", "GENERAL", true)),
                        List.of(new TerminalPuestoResult(TERM_A1, "T1", "Caja 1", "ACTIVO", true),
                                new TerminalPuestoResult(TERM_A2, "T2", "Caja 2", "MANTENIMIENTO", false))),
                new EstablecimientoPuestoResult(EST_B, EMPRESA_B, "B", "Botica B", List.of(), List.of())));
        when(alcancePort.alcanceDe(TENANT, USUARIO)).thenReturn(alcance);
    }

    private List<EstablecimientoPuestoResult> run() {
        return handler.execute(new ObtenerMiPuestoQuery(TENANT, USUARIO)).getOrElse(error -> null)
                .establecimientos();
    }

    @Test
    void aGlobalActorSeesEveryEstablishmentWithAllItsResources() {
        withScope(alcance(true, Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(run()).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A, EST_B);
        assertThat(run().get(0).terminales()).hasSize(2);
    }

    @Test
    void aCompanyAssignmentShowsEveryEstablishmentOfThatCompany() {
        withScope(alcance(false, Set.of(EMPRESA_B), Set.of(), Set.of(), Set.of()));

        assertThat(run()).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_B);
    }

    @Test
    void anEstablishmentAssignmentShowsThatEstablishmentWithAllItsResources() {
        withScope(alcance(false, Set.of(), Set.of(EST_A), Set.of(), Set.of()));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).almacenes()).hasSize(1);
        assertThat(visibles.get(0).terminales()).hasSize(2);
    }

    @Test
    void aTerminalAssignmentShowsOnlyThatTerminalAndNoWarehouses() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(), Set.of(TERM_A2)));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).terminales()).extracting(TerminalPuestoResult::id).containsExactly(TERM_A2);
        assertThat(visibles.get(0).almacenes()).isEmpty();
    }

    @Test
    void aWarehouseAssignmentShowsOnlyThatWarehouseAndNoTerminals() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(ALM_A), Set.of()));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).almacenes()).extracting(AlmacenPuestoResult::id).containsExactly(ALM_A);
        assertThat(visibles.get(0).terminales()).isEmpty();
    }

    @Test
    void anActorWithoutAssignmentsSeesNothing() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(run()).isEmpty();
    }

    @Test
    void requiresItsCollaboratorsAndTheQuery() {
        assertThatThrownBy(() -> new ObtenerMiPuestoHandler(null, alcancePort)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ObtenerMiPuestoHandler(readPort, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> handler.execute(null)).isInstanceOf(NullPointerException.class);
    }
}
```

`ORGT/application/dto/result/PuestoResultsTest.java`:

```java
package com.softprimesolutions.organizacion.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PuestoResultsTest {

    @Test
    void theEstablishmentKeepsImmutableCopiesOfItsResources() {
        var almacenes = new ArrayList<>(List.of(
                new AlmacenPuestoResult(UUID.randomUUID(), "A", "Almacen", "GENERAL", true)));
        var terminales = new ArrayList<>(List.of(
                new TerminalPuestoResult(UUID.randomUUID(), "T", "Caja", "ACTIVO", true)));

        var establecimiento = new EstablecimientoPuestoResult(
                UUID.randomUUID(), UUID.randomUUID(), "E", "Botica", almacenes, terminales);
        almacenes.clear();
        terminales.clear();

        assertThat(establecimiento.almacenes()).hasSize(1);
        assertThat(establecimiento.terminales()).hasSize(1);
        assertThatThrownBy(() -> establecimiento.almacenes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theResultKeepsAnImmutableCopyOfItsEstablishments() {
        var lista = new ArrayList<EstablecimientoPuestoResult>();

        var resultado = new MiPuestoResult(lista);
        lista.add(new EstablecimientoPuestoResult(UUID.randomUUID(), UUID.randomUUID(), "E", "B", List.of(), List.of()));

        assertThat(resultado.establecimientos()).isEmpty();
    }

    @Test
    void theScopeKeepsImmutableCopiesOfItsSets() {
        var alcance = new AlcancePuesto(false, Set.of(), Set.of(), Set.of(), Set.of());

        assertThatThrownBy(() -> alcance.empresas().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(alcance.global()).isFalse();
    }
}
```

`ORGT/infrastructure/client/SecurityAlcanceAdapterTest.java`:

```java
package com.softprimesolutions.organizacion.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.security.api.AlcanceResultado;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecurityAlcanceAdapterTest {

    @Test
    void translatesTheSecurityScopeIntoTheOrganizationScope() {
        var tenant = UUID.randomUUID();
        var usuario = UUID.randomUUID();
        var empresa = UUID.randomUUID();
        var terminal = UUID.randomUUID();
        var seguridad = mock(AlcanceUsuario.class);
        when(seguridad.alcanceDe(tenant, usuario))
                .thenReturn(new AlcanceResultado(true, Set.of(empresa), Set.of(), Set.of(), Set.of(terminal)));

        var alcance = new SecurityAlcanceAdapter(seguridad).alcanceDe(tenant, usuario);

        assertThat(alcance).isEqualTo(new AlcancePuesto(true, Set.of(empresa), Set.of(), Set.of(), Set.of(terminal)));
    }
}
```

En `ORGT/infrastructure/configuration/OrganizacionModuleConfigurationTest.java`, en `wiresEveryQueryUseCase`, agregar (con `import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;` y un campo `private final AlcancePuestoPort alcancePort = mock(AlcancePuestoPort.class);`):

```java
        assertThat(configuration.obtenerMiPuestoUseCase(readPort, alcancePort)).isNotNull();
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:organizacion:test --tests "*ObtenerMiPuestoHandlerTest"`
Expected: FAIL de compilación.

- [ ] **Step 3: Implementar**

`ORG/application/dto/query/ObtenerMiPuestoQuery.java`:

```java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerMiPuestoQuery(UUID tenantId, UUID usuarioId) {
}
```

`ORG/application/port/in/ObtenerMiPuestoUseCase.java`:

```java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerMiPuestoUseCase {
    Result<MiPuestoResult, ApplicationError> execute(ObtenerMiPuestoQuery query);
}
```

`ORG/application/dto/result/AlcancePuesto.java`:

```java
package com.softprimesolutions.organizacion.application.dto.result;

import java.util.Set;
import java.util.UUID;

public record AlcancePuesto(
        boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales) {

    public AlcancePuesto {
        empresas = Set.copyOf(empresas);
        establecimientos = Set.copyOf(establecimientos);
        almacenes = Set.copyOf(almacenes);
        terminales = Set.copyOf(terminales);
    }
}
```

`ORG/application/port/out/AlcancePuestoPort.java`:

```java
package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import java.util.UUID;

@FunctionalInterface
public interface AlcancePuestoPort {
    AlcancePuesto alcanceDe(UUID tenantId, UUID usuarioId);
}
```

`ORG/application/usecase/query/ObtenerMiPuestoHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPuestoResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerMiPuestoUseCase;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerMiPuestoHandler implements ObtenerMiPuestoUseCase {

    private final OrganizacionReadPort readPort;
    private final AlcancePuestoPort alcancePort;

    public ObtenerMiPuestoHandler(OrganizacionReadPort readPort, AlcancePuestoPort alcancePort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.alcancePort = Objects.requireNonNull(alcancePort, "alcancePort es obligatorio");
    }

    @Override
    public Result<MiPuestoResult, ApplicationError> execute(ObtenerMiPuestoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        var alcance = alcancePort.alcanceDe(query.tenantId(), query.usuarioId());
        var visibles = readPort.findEstablecimientosPuesto(query.tenantId()).stream()
                .filter(establecimiento -> esVisible(alcance, establecimiento))
                .map(establecimiento -> recortar(alcance, establecimiento))
                .toList();
        return Result.success(new MiPuestoResult(visibles));
    }

    private static boolean cubreEstablecimiento(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        return alcance.global()
                || alcance.empresas().contains(establecimiento.empresaId())
                || alcance.establecimientos().contains(establecimiento.id());
    }

    private static boolean esVisible(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        return cubreEstablecimiento(alcance, establecimiento)
                || establecimiento.almacenes().stream().anyMatch(almacen -> alcance.almacenes().contains(almacen.id()))
                || establecimiento.terminales().stream().anyMatch(terminal -> alcance.terminales().contains(terminal.id()));
    }

    private static EstablecimientoPuestoResult recortar(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        if (cubreEstablecimiento(alcance, establecimiento)) {
            return establecimiento;
        }
        return new EstablecimientoPuestoResult(
                establecimiento.id(), establecimiento.empresaId(), establecimiento.codigo(), establecimiento.nombre(),
                establecimiento.almacenes().stream()
                        .filter(almacen -> alcance.almacenes().contains(almacen.id()))
                        .toList(),
                establecimiento.terminales().stream()
                        .filter(terminal -> alcance.terminales().contains(terminal.id()))
                        .toList());
    }
}
```

Quitar los imports `AlmacenPuestoResult` y `TerminalPuestoResult` del handler si el compilador los marca sin uso (las lambdas infieren los tipos).

`ORG/infrastructure/client/SecurityAlcanceAdapter.java`:

```java
package com.softprimesolutions.organizacion.infrastructure.client;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlcanceAdapter implements AlcancePuestoPort {

    private final AlcanceUsuario alcanceUsuario;

    public SecurityAlcanceAdapter(AlcanceUsuario alcanceUsuario) {
        this.alcanceUsuario = alcanceUsuario;
    }

    @Override
    public AlcancePuesto alcanceDe(UUID tenantId, UUID usuarioId) {
        var alcance = alcanceUsuario.alcanceDe(tenantId, usuarioId);
        return new AlcancePuesto(
                alcance.global(), alcance.empresas(), alcance.establecimientos(), alcance.almacenes(),
                alcance.terminales());
    }
}
```

`ORG/infrastructure/configuration/OrganizacionModuleConfiguration.java`: agregar los imports `com.softprimesolutions.organizacion.application.port.in.ObtenerMiPuestoUseCase`, `com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort`, `com.softprimesolutions.organizacion.application.usecase.query.ObtenerMiPuestoHandler` y el bean antes de la `}` final:

```java
    @Bean
    ObtenerMiPuestoUseCase obtenerMiPuestoUseCase(OrganizacionReadPort readPort, AlcancePuestoPort alcancePort) {
        return new ObtenerMiPuestoHandler(readPort, alcancePort);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:organizacion:check :architectureTest`
Expected: PASS (JaCoCo 100 % en las clases nuevas de las Tasks 3 y 4; Modulith sin ciclos).

- [ ] **Step 5: Commit**

```bash
git add service-botica
git commit -m "feat(organizacion): caso de uso mi-puesto filtrado por el alcance del usuario

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Endpoint `GET /api/v1/organizacion/mi-puesto` y pruebas de integración

**Files:**
- Create: `ORG/api/dto/response/MiPuestoResponse.java`
- Create: `ORG/api/mapper/MiPuestoApiMapper.java`
- Create: `ORG/api/controller/MiPuestoController.java`
- Modify: `ORG/api/controller/OrganizacionControllerSupport.java` (agregar `actorOf`)
- Test: `ORGT/api/controller/MiPuestoControllerTest.java`, `ORGT/api/mapper/MiPuestoApiMapperTest.java`
- Modify (test): `BTEST/organizacion/api/OrganizacionApiIntegrationTest.java`

**Interfaces:**
- Consumes: `ObtenerMiPuestoUseCase`, `OrganizacionControllerSupport.tenantOf(Jwt)`.
- Produces: respuesta `{ "establecimientos": [{ "id", "codigo", "nombre", "almacenes": [{ "id","codigo","nombre","tipo","permiteVenta" }], "terminales": [{ "id","codigo","nombre","estado","operable" }] }] }`.

- [ ] **Step 1: Escribir los tests que fallan**

`ORGT/api/mapper/MiPuestoApiMapperTest.java`:

```java
package com.softprimesolutions.organizacion.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPuestoResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MiPuestoApiMapperTest {

    @Test
    void mapsTheWorkstationWithoutExposingTheCompany() {
        var establecimiento = UUID.randomUUID();
        var almacen = UUID.randomUUID();
        var terminal = UUID.randomUUID();
        var result = new MiPuestoResult(List.of(new EstablecimientoPuestoResult(
                establecimiento, UUID.randomUUID(), "E1", "Botica Central",
                List.of(new AlmacenPuestoResult(almacen, "ALM", "Almacen", "GENERAL", true)),
                List.of(new TerminalPuestoResult(terminal, "T1", "Caja 1", "MANTENIMIENTO", false)))));

        var response = MiPuestoApiMapper.toResponse(result);

        assertThat(response.establecimientos()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(establecimiento);
            assertThat(item.codigo()).isEqualTo("E1");
            assertThat(item.nombre()).isEqualTo("Botica Central");
            assertThat(item.almacenes()).singleElement().satisfies(a -> {
                assertThat(a.id()).isEqualTo(almacen);
                assertThat(a.tipo()).isEqualTo("GENERAL");
                assertThat(a.permiteVenta()).isTrue();
            });
            assertThat(item.terminales()).singleElement().satisfies(t -> {
                assertThat(t.id()).isEqualTo(terminal);
                assertThat(t.estado()).isEqualTo("MANTENIMIENTO");
                assertThat(t.operable()).isFalse();
            });
        });
    }
}
```

`ORGT/api/controller/MiPuestoControllerTest.java`:

```java
package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.api.mapper.MiPuestoApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class MiPuestoControllerTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final MiPuestoResult RESULT = new MiPuestoResult(List.of());

    private static Jwt token() {
        return Jwt.withTokenValue("token").header("alg", "none").subject(USUARIO.toString())
                .claim("tid", TENANT.toString()).build();
    }

    @Test
    void resolvesTheTenantAndTheUserFromTheTokenAndReturnsTheWorkstation() {
        var queries = new ArrayList<ObtenerMiPuestoQuery>();
        var controller = new MiPuestoController(query -> {
            queries.add(query);
            return ok(RESULT);
        });

        var response = controller.get(token());

        assertOk(response, MiPuestoApiMapper.toResponse(RESULT));
        assertThat(queries).containsExactly(new ObtenerMiPuestoQuery(TENANT, USUARIO));
    }

    @Test
    void mapsFailuresToProblemDetails() {
        var controller = new MiPuestoController(query -> conflict());

        assertConflict(controller.get(token()));
    }
}
```

En `BTEST/organizacion/api/OrganizacionApiIntegrationTest.java` agregar al final de la clase (antes de los helpers `private`), con los imports `static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath` y `status` ya presentes:

```java
    private void setTerminalEstado(UUID terminalId, String estado) {
        jdbcClient.sql("UPDATE sch_organizacion.terminal_pos SET estado = :estado WHERE uuid_publico = :id")
                .param("estado", estado).param("id", terminalId).update();
    }

    private void scopeUserToTerminal(UUID userId, UUID terminalId) {
        jdbcClient.sql("""
                        UPDATE sch_seguridad.usuario_rol_ambito a
                           SET tipo_ambito = 'TERMINAL',
                               terminal_id = (SELECT tp.id FROM sch_organizacion.terminal_pos tp
                                               WHERE tp.uuid_publico = :terminalId)
                         WHERE a.membership_id = (SELECT m.id FROM sch_seguridad.membership m
                                                   WHERE m.uuid_publico = :userId)
                        """).param("terminalId", terminalId).param("userId", userId).update();
    }

    private void revokeAssignments(UUID userId) {
        jdbcClient.sql("""
                        UPDATE sch_seguridad.usuario_rol_ambito a SET estado = 'REVOCADO'
                         WHERE a.membership_id = (SELECT m.id FROM sch_seguridad.membership m
                                                   WHERE m.uuid_publico = :userId)
                        """).param("userId", userId).update();
    }

    @Test
    void anEstablishmentCashierSeesOnlyItsOwnEstablishmentWithDisabledTerminalsMarked() throws Exception {
        var empresaA = createEmpresa("20123456786");
        var estA = createEstablecimiento(empresaA, "EST001", "DIG001");
        var almacenA = createAlmacen(estA);
        var terminalActiva = createTerminal(estA);
        postTerminal(estA, "POS002", "B002", "F002").andExpect(status().isCreated());
        jdbcClient.sql("UPDATE sch_organizacion.terminal_pos SET estado = 'MANTENIMIENTO' WHERE codigo = 'POS002'")
                .update();
        var empresaB = createEmpresa("20123456794");
        createEstablecimiento(empresaB, "EST002", "DIG002");
        var cajero = realLogin.login(TENANT_ID, "cajero.a", "^ventas\\.", estA);

        mockMvc.perform(get(BASE + "/mi-puesto").header("Authorization", cajero.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientos.length()").value(1))
                .andExpect(jsonPath("$.establecimientos[0].id").value(estA.toString()))
                .andExpect(jsonPath("$.establecimientos[0].almacenes[0].id").value(almacenA.toString()))
                .andExpect(jsonPath("$.establecimientos[0].almacenes[0].permiteVenta").value(true))
                .andExpect(jsonPath("$.establecimientos[0].terminales.length()").value(2))
                .andExpect(jsonPath("$.establecimientos[0].terminales[?(@.id == '%s')].operable"
                        .formatted(terminalActiva)).value(true))
                .andExpect(jsonPath("$.establecimientos[0].terminales[?(@.codigo == 'POS002')].operable")
                        .value(false))
                .andExpect(jsonPath("$.establecimientos[0].terminales[?(@.codigo == 'POS002')].estado")
                        .value("MANTENIMIENTO"));
    }

    @Test
    void aTerminalScopedCashierSeesOnlyThatTerminal() throws Exception {
        var empresa = createEmpresa("20123456786");
        var est = createEstablecimiento(empresa, "EST001", "DIG001");
        createAlmacen(est);
        var terminal = createTerminal(est);
        postTerminal(est, "POS002", "B002", "F002").andExpect(status().isCreated());
        var cajero = realLogin.login(TENANT_ID, "cajero.t", "^ventas\\.", est);
        scopeUserToTerminal(cajero.userId(), terminal);

        mockMvc.perform(get(BASE + "/mi-puesto").header("Authorization", cajero.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientos.length()").value(1))
                .andExpect(jsonPath("$.establecimientos[0].terminales.length()").value(1))
                .andExpect(jsonPath("$.establecimientos[0].terminales[0].id").value(terminal.toString()))
                .andExpect(jsonPath("$.establecimientos[0].almacenes.length()").value(0));
    }

    @Test
    void aGlobalAdministratorSeesEveryEstablishmentOfTheTenant() throws Exception {
        var empresaA = createEmpresa("20123456786");
        createEstablecimiento(empresaA, "EST001", "DIG001");
        var empresaB = createEmpresa("20123456794");
        createEstablecimiento(empresaB, "EST002", "DIG002");

        mockMvc.perform(get(BASE + "/mi-puesto").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientos.length()").value(2));
    }

    @Test
    void aUserWhoseAssignmentsWereRevokedSeesAnEmptyWorkstation() throws Exception {
        var empresa = createEmpresa("20123456786");
        var est = createEstablecimiento(empresa, "EST001", "DIG001");
        var cajero = realLogin.login(TENANT_ID, "cajero.r", "^ventas\\.", est);
        revokeAssignments(cajero.userId());

        mockMvc.perform(get(BASE + "/mi-puesto").header("Authorization", cajero.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establecimientos.length()").value(0));
    }

    @Test
    void theWorkstationRequiresAuthentication() throws Exception {
        mockMvc.perform(get(BASE + "/mi-puesto")).andExpect(status().isUnauthorized());
    }
```

Eliminar el helper `setTerminalEstado` si no se usa (el primer test hace el `UPDATE` inline); mantener solo `scopeUserToTerminal` y `revokeAssignments`.

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:organizacion:test --tests "*MiPuestoControllerTest"`
Expected: FAIL de compilación.

- [ ] **Step 3: Implementar**

`ORG/api/dto/response/MiPuestoResponse.java`:

```java
package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record MiPuestoResponse(List<EstablecimientoPuesto> establecimientos) {

    public record EstablecimientoPuesto(
            UUID id, String codigo, String nombre, List<AlmacenPuesto> almacenes, List<TerminalPuesto> terminales) {
    }

    public record AlmacenPuesto(UUID id, String codigo, String nombre, String tipo, boolean permiteVenta) {
    }

    public record TerminalPuesto(UUID id, String codigo, String nombre, String estado, boolean operable) {
    }
}
```

`ORG/api/mapper/MiPuestoApiMapper.java`:

```java
package com.softprimesolutions.organizacion.api.mapper;

import com.softprimesolutions.organizacion.api.dto.response.MiPuestoResponse;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;

public final class MiPuestoApiMapper {

    private MiPuestoApiMapper() {
    }

    public static MiPuestoResponse toResponse(MiPuestoResult result) {
        return new MiPuestoResponse(result.establecimientos().stream()
                .map(establecimiento -> new MiPuestoResponse.EstablecimientoPuesto(
                        establecimiento.id(), establecimiento.codigo(), establecimiento.nombre(),
                        establecimiento.almacenes().stream()
                                .map(almacen -> new MiPuestoResponse.AlmacenPuesto(
                                        almacen.id(), almacen.codigo(), almacen.nombre(), almacen.tipo(),
                                        almacen.permiteVenta()))
                                .toList(),
                        establecimiento.terminales().stream()
                                .map(terminal -> new MiPuestoResponse.TerminalPuesto(
                                        terminal.id(), terminal.codigo(), terminal.nombre(), terminal.estado(),
                                        terminal.operable()))
                                .toList()))
                .toList());
    }
}
```

`ORG/api/controller/OrganizacionControllerSupport.java`: agregar el método

```java
    static UUID actorOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
```

`ORG/api/controller/MiPuestoController.java`:

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.mapper.MiPuestoApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.port.in.ObtenerMiPuestoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizacion/mi-puesto")
public class MiPuestoController {

    private final ObtenerMiPuestoUseCase getWorkstation;

    public MiPuestoController(ObtenerMiPuestoUseCase getWorkstation) {
        this.getWorkstation = getWorkstation;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt) {
        var query = new ObtenerMiPuestoQuery(
                OrganizacionControllerSupport.tenantOf(jwt), OrganizacionControllerSupport.actorOf(jwt));
        return getWorkstation.execute(query).fold(
                result -> ResponseEntity.ok(MiPuestoApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:organizacion:check` y `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.organizacion.api.OrganizacionApiIntegrationTest"`
Expected: PASS. Si una aserción de JSONPath con filtro `[?(@...)]` devuelve una lista en lugar de un escalar, usar `.value(org.hamcrest.Matchers.contains(true))`.

Luego el gate completo: `.\gradlew.bat check --warning-mode all`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add service-botica
git commit -m "feat(organizacion): endpoint GET /organizacion/mi-puesto

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Frontend — `mi-puesto` en Caja y POS

**Files:**
- Create: `SRC/features/organizacion/api/mi-puesto.types.ts`
- Create: `SRC/features/organizacion/api/mi-puesto.api.ts`
- Create: `SRC/features/organizacion/lib/use-mi-puesto.ts`
- Create: `SRC/features/organizacion/lib/resolver-puesto.ts`
- Create: `SRC/test/mi-puesto-fixtures.ts`
- Modify: `SRC/features/organizacion/components/TerminalSelector.tsx`
- Modify: `SRC/features/organizacion/index.ts`
- Modify: `SRC/features/pos/components/PuestoVenta.tsx`, `SRC/features/pos/pages/PosPage.tsx`
- Test: `SRC/features/organizacion/lib/resolver-puesto.test.ts`, `SRC/features/organizacion/lib/use-mi-puesto.test.tsx`, `SRC/features/organizacion/components/TerminalSelector.test.tsx` (reescribir), `SRC/features/pos/components/PuestoVenta.test.tsx`, `SRC/features/pos/pages/PosPage.test.tsx`, `SRC/features/caja/pages/CashRegisterPage.test.tsx`
- Modify: `E2E/support/caja-api.ts`, `E2E/support/pos-api.ts`

**Interfaces:**
- Produces: `miPuestoQuery`; `useMiPuesto(): { establecimientos: MiPuestoEstablecimiento[]; isPending: boolean; isError: boolean }`; `resolverPuesto(establecimientos, actual): { establecimientoId; terminalId } | null`; tipos `MiPuesto`, `MiPuestoEstablecimiento`, `MiPuestoAlmacen`, `MiPuestoTerminal`; fixtures `sampleMiPuesto` y `miPuestoHandler()`.

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/test/mi-puesto-fixtures.ts`:

```ts
import { http, HttpResponse } from 'msw';
import type { MiPuesto } from '../features/organizacion/api/mi-puesto.types';

export const sampleMiPuesto: MiPuesto = {
  establecimientos: [
    {
      id: 'est-1',
      codigo: 'EST001',
      nombre: 'Botica Central',
      almacenes: [
        { id: 'alm-1', codigo: 'ALM001', nombre: 'Almacén Central', tipo: 'GENERAL', permiteVenta: true },
        { id: 'alm-2', codigo: 'ALM002', nombre: 'Almacén Frío', tipo: 'REFRIGERADO', permiteVenta: true },
        { id: 'alm-9', codigo: 'ALM009', nombre: 'Almacén Merma', tipo: 'MERMA', permiteVenta: false }
      ],
      terminales: [
        { id: 'term-1', codigo: 'POS001', nombre: 'Caja 1', estado: 'ACTIVO', operable: true },
        { id: 'term-2', codigo: 'POS002', nombre: 'Caja 2', estado: 'ACTIVO', operable: true }
      ]
    },
    {
      id: 'est-2',
      codigo: 'EST002',
      nombre: 'Botica Norte',
      almacenes: [
        { id: 'alm-3', codigo: 'ALM003', nombre: 'Almacén Norte', tipo: 'GENERAL', permiteVenta: true }
      ],
      terminales: [
        { id: 'term-3', codigo: 'POS003', nombre: 'Caja 3', estado: 'MANTENIMIENTO', operable: false }
      ]
    }
  ]
};

export const miPuestoHandler = (puesto: MiPuesto = sampleMiPuesto) =>
  http.get('*/api/v1/organizacion/mi-puesto', () => HttpResponse.json(puesto));
```

`SRC/features/organizacion/lib/resolver-puesto.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { sampleMiPuesto } from '../../../test/mi-puesto-fixtures';
import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';
import { resolverPuesto } from './resolver-puesto';

const [central, norte] = sampleMiPuesto.establecimientos as [
  MiPuestoEstablecimiento,
  MiPuestoEstablecimiento
];
const conUnaTerminal: MiPuestoEstablecimiento = { ...central, terminales: [central.terminales[0]!] };
const sinOperables: MiPuestoEstablecimiento = { ...norte };

describe('resolverPuesto', () => {
  it('no cambia nada si hay varios establecimientos y ninguno elegido', () => {
    expect(resolverPuesto([central, norte], { establecimientoId: '', terminalId: '' })).toBeNull();
  });

  it('elige el único establecimiento y su única terminal operable', () => {
    expect(resolverPuesto([conUnaTerminal], { establecimientoId: '', terminalId: '' })).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1'
    });
  });

  it('elige el único establecimiento sin terminal cuando ninguna es operable', () => {
    expect(resolverPuesto([sinOperables], { establecimientoId: '', terminalId: '' })).toEqual({
      establecimientoId: 'est-2',
      terminalId: ''
    });
  });

  it('descarta un establecimiento recordado que ya no está en el alcance', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-x', terminalId: 'term-1' })).toEqual({
      establecimientoId: '',
      terminalId: ''
    });
  });

  it('no hace nada si no hay establecimiento elegido y no hay ninguno disponible', () => {
    expect(resolverPuesto([], { establecimientoId: '', terminalId: '' })).toBeNull();
  });

  it('descarta una terminal recordada que ya no es operable', () => {
    expect(resolverPuesto([sinOperables], { establecimientoId: 'est-2', terminalId: 'term-3' })).toEqual({
      establecimientoId: 'est-2',
      terminalId: ''
    });
  });

  it('conserva la terminal recordada cuando sigue siendo operable', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-1', terminalId: 'term-2' })).toBeNull();
  });

  it('elige la única terminal operable del establecimiento recordado', () => {
    expect(resolverPuesto([conUnaTerminal], { establecimientoId: 'est-1', terminalId: '' })).toEqual({
      establecimientoId: 'est-1',
      terminalId: 'term-1'
    });
  });

  it('deja la terminal vacía cuando hay varias operables y ninguna elegida', () => {
    expect(resolverPuesto([central], { establecimientoId: 'est-1', terminalId: '' })).toBeNull();
  });
});
```

`SRC/features/organizacion/lib/use-mi-puesto.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { miPuestoHandler } from '../../../test/mi-puesto-fixtures';
import { renderRoute } from '../../../test/render-route';
import { useMiPuesto } from './use-mi-puesto';

function Probe() {
  const { establecimientos, isPending, isError } = useMiPuesto();
  if (isPending) return <p>cargando</p>;
  if (isError) return <p>error</p>;
  return <p>{establecimientos.map(({ nombre }) => nombre).join(',')}</p>;
}

describe('useMiPuesto', () => {
  it('expone los establecimientos del puesto del usuario', async () => {
    server.use(miPuestoHandler());
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('Botica Central,Botica Norte')).toBeInTheDocument();
  });

  it('expone el error cuando la consulta falla', async () => {
    server.use(http.get('*/api/v1/organizacion/mi-puesto', () => new HttpResponse(null, { status: 500 })));
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('error')).toBeInTheDocument();
  });
});
```

Reemplazar completo `SRC/features/organizacion/components/TerminalSelector.test.tsx`:

```tsx
import { screen, waitFor } from '@testing-library/react';
import { server } from '../../../test/mocks/server';
import { miPuestoHandler, sampleMiPuesto } from '../../../test/mi-puesto-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalSelector } from './TerminalSelector';

beforeEach(() => {
  server.use(miPuestoHandler());
});

function renderSelector(establecimientoId: string, terminalId: string, onChange = vi.fn()) {
  const Selector = () => (
    <TerminalSelector
      establecimientoId={establecimientoId}
      terminalId={terminalId}
      onChange={onChange}
    />
  );
  return { onChange, ...renderRoute('/x', Selector, '/x') };
}

describe('TerminalSelector', () => {
  it('lista los establecimientos del puesto y, al elegir uno, notifica el cambio sin terminal', async () => {
    const { onChange, user } = renderSelector('', '');

    await screen.findByRole('option', { name: 'Botica Central' });
    await user.selectOptions(screen.getByLabelText('Establecimiento'), 'est-1');

    expect(onChange).toHaveBeenCalledWith('est-1', '');
  });

  it('deshabilita las terminales no operables indicando el motivo', async () => {
    renderSelector('est-2', '');

    const enMantenimiento = await screen.findByRole('option', { name: 'Caja 3 (en mantenimiento)' });

    expect(enMantenimiento).toBeDisabled();
  });

  it('indica el estado en minúsculas cuando el motivo no tiene un texto propio', async () => {
    server.use(
      miPuestoHandler({
        establecimientos: [
          {
            ...sampleMiPuesto.establecimientos[1]!,
            terminales: [
              { id: 'term-9', codigo: 'POS009', nombre: 'Caja 9', estado: 'INACTIVA', operable: false }
            ]
          }
        ]
      })
    );
    renderSelector('est-2', '');

    expect(await screen.findByRole('option', { name: 'Caja 9 (inactiva)' })).toBeDisabled();
  });

  it('muestra las terminales operables y notifica la elegida', async () => {
    const { onChange, user } = renderSelector('est-1', '');

    expect(await screen.findByRole('option', { name: 'Caja 1' })).toBeEnabled();
    await user.selectOptions(screen.getByLabelText('Terminal'), 'term-1');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });

  it('descarta un establecimiento recordado que ya no está en el alcance', async () => {
    const { onChange } = renderSelector('est-x', 'term-x');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('', ''));
  });

  it('deshabilita el selector de terminal mientras no hay establecimiento', async () => {
    renderSelector('', '');

    expect(await screen.findByLabelText('Terminal')).toBeDisabled();
  });

  it('preselecciona el único establecimiento y su única terminal operable', async () => {
    server.use(
      miPuestoHandler({
        establecimientos: [
          {
            ...sampleMiPuesto.establecimientos[0]!,
            terminales: [sampleMiPuesto.establecimientos[0]!.terminales[0]!]
          }
        ]
      })
    );
    const { onChange } = renderSelector('', '');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('est-1', 'term-1'));
  });
});
```

Agregar a `SRC/features/pos/components/PuestoVenta.test.tsx` un caso para el almacén sin venta y adaptar el `beforeEach` (Step 3). Caso nuevo:

```tsx
  it('lista solo los almacenes habilitados para venta y avisa cuando no hay ninguno', async () => {
    server.use(
      miPuestoHandler({
        establecimientos: [
          {
            ...sampleMiPuesto.establecimientos[0]!,
            almacenes: [sampleMiPuesto.establecimientos[0]!.almacenes[2]!]
          }
        ]
      })
    );
    renderPuesto({ establecimientoId: 'est-1', terminalId: 'term-1', almacenId: '' });

    expect(
      await screen.findByText('Este establecimiento no tiene almacenes habilitados para venta.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Almacén Merma' })).not.toBeInTheDocument();
  });
```

(`renderPuesto` es el helper que ya usa ese archivo para montar `PuestoVenta`; si tiene otro nombre, usar el existente y agregar los imports de `miPuestoHandler` y `sampleMiPuesto`.)

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion`
Expected: FAIL (módulos `mi-puesto.types`, `use-mi-puesto`, `resolver-puesto` inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/organizacion/api/mi-puesto.types.ts`:

```ts
export type MiPuestoAlmacen = {
  id: string;
  codigo: string;
  nombre: string;
  tipo: string;
  permiteVenta: boolean;
};

export type MiPuestoTerminal = {
  id: string;
  codigo: string;
  nombre: string;
  estado: string;
  operable: boolean;
};

export type MiPuestoEstablecimiento = {
  id: string;
  codigo: string;
  nombre: string;
  almacenes: MiPuestoAlmacen[];
  terminales: MiPuestoTerminal[];
};

export type MiPuesto = {
  establecimientos: MiPuestoEstablecimiento[];
};
```

`SRC/features/organizacion/api/mi-puesto.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import { apiClient } from '../../../app/api';
import type { MiPuesto } from './mi-puesto.types';

export const miPuestoQuery = queryOptions({
  queryKey: ['organization', 'mi-puesto'],
  queryFn: () => apiClient.get<MiPuesto>('/organizacion/mi-puesto')
});
```

`SRC/features/organizacion/lib/use-mi-puesto.ts`:

```ts
import { useQuery } from '@tanstack/react-query';
import { miPuestoQuery } from '../api/mi-puesto.api';
import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';

type MiPuestoEstado = {
  establecimientos: MiPuestoEstablecimiento[];
  isPending: boolean;
  isError: boolean;
};

export function useMiPuesto(): MiPuestoEstado {
  const { data, isPending, isError } = useQuery(miPuestoQuery);
  return { establecimientos: data?.establecimientos ?? [], isPending, isError };
}
```

`SRC/features/organizacion/lib/resolver-puesto.ts`:

```ts
import type { MiPuestoEstablecimiento } from '../api/mi-puesto.types';

type Seleccion = { establecimientoId: string; terminalId: string };

export function resolverPuesto(
  establecimientos: MiPuestoEstablecimiento[],
  actual: Seleccion
): Seleccion | null {
  const unico = actual.establecimientoId === '' && establecimientos.length === 1;
  const elegido = unico
    ? establecimientos[0]
    : establecimientos.find(({ id }) => id === actual.establecimientoId);
  if (!elegido) {
    return actual.establecimientoId === '' ? null : { establecimientoId: '', terminalId: '' };
  }
  const operables = elegido.terminales.filter(({ operable }) => operable);
  const [primera, ...resto] = operables;
  const vigente = operables.some(({ id }) => id === actual.terminalId);
  const terminalId = vigente ? actual.terminalId : primera && resto.length === 0 ? primera.id : '';
  return elegido.id === actual.establecimientoId && terminalId === actual.terminalId
    ? null
    : { establecimientoId: elegido.id, terminalId };
}
```

`SRC/features/organizacion/components/TerminalSelector.tsx`:

```tsx
import { useEffect } from 'react';
import { SelectField } from '../../../shared/components/FormFields';
import { useMiPuesto } from '../lib/use-mi-puesto';
import { resolverPuesto } from '../lib/resolver-puesto';

const MOTIVO_NO_OPERABLE: Record<string, string> = {
  BLOQUEADO: 'bloqueada',
  MANTENIMIENTO: 'en mantenimiento'
};

type TerminalSelectorProps = {
  establecimientoId: string;
  terminalId: string;
  onChange: (establecimientoId: string, terminalId: string) => void;
};

export function TerminalSelector({
  establecimientoId,
  terminalId,
  onChange
}: TerminalSelectorProps) {
  const { establecimientos, isPending } = useMiPuesto();
  const terminales =
    establecimientos.find(({ id }) => id === establecimientoId)?.terminales ?? [];

  useEffect(() => {
    if (isPending) return;
    const siguiente = resolverPuesto(establecimientos, { establecimientoId, terminalId });
    if (siguiente) onChange(siguiente.establecimientoId, siguiente.terminalId);
  }, [isPending, establecimientos, establecimientoId, terminalId, onChange]);

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <SelectField
        id="puesto-establecimiento"
        label="Establecimiento"
        value={establecimientoId}
        onChange={(event) => onChange(event.target.value, '')}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="puesto-terminal"
        label="Terminal"
        value={terminalId}
        disabled={establecimientoId === ''}
        onChange={(event) => onChange(establecimientoId, event.target.value)}
      >
        <option value="">Selecciona una terminal</option>
        {terminales.map(({ id, nombre, estado, operable }) => (
          <option key={id} value={id} disabled={!operable}>
            {operable
              ? nombre
              : `${nombre} (${MOTIVO_NO_OPERABLE[estado] ?? estado.toLowerCase()})`}
          </option>
        ))}
      </SelectField>
    </div>
  );
}
```

`SRC/features/organizacion/index.ts`: agregar

```ts
export { miPuestoQuery } from './api/mi-puesto.api';
export type {
  MiPuesto,
  MiPuestoAlmacen,
  MiPuestoEstablecimiento,
  MiPuestoTerminal
} from './api/mi-puesto.types';
export { useMiPuesto } from './lib/use-mi-puesto';
```

`SRC/features/pos/components/PuestoVenta.tsx` (reemplazo completo; deja de usar `useEstablecimientos` y `almacenesDeEstablecimiento`):

```tsx
import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import {
  TerminalSelector,
  cambiarTerminal,
  useMiPuesto,
  type PuestoTrabajo
} from '../../organizacion';

type PuestoVentaProps = {
  puesto: PuestoTrabajo;
  onChange: (puesto: PuestoTrabajo) => void;
};

export function PuestoVenta({ puesto, onChange }: PuestoVentaProps) {
  const { establecimientos, isPending } = useMiPuesto();
  const almacenes = (
    establecimientos.find(({ id }) => id === puesto.establecimientoId)?.almacenes ?? []
  ).filter(({ permiteVenta }) => permiteVenta);
  const sinAlmacenes = !isPending && puesto.establecimientoId !== '' && almacenes.length === 0;

  return (
    <Card className="grid gap-4 p-5 md:grid-cols-3 md:[&>*:first-child]:col-span-2">
      <TerminalSelector
        establecimientoId={puesto.establecimientoId}
        terminalId={puesto.terminalId}
        onChange={(establecimientoId, terminalId) =>
          onChange(cambiarTerminal(puesto, establecimientoId, terminalId))
        }
      />
      <SelectField
        id="pos-almacen"
        label="Almacén"
        value={puesto.almacenId}
        disabled={puesto.establecimientoId === ''}
        onChange={(event) => onChange({ ...puesto, almacenId: event.target.value })}
      >
        <option value="">Selecciona un almacén</option>
        {almacenes.map(({ id, nombre }) => (
          <option key={id} value={id}>
            {nombre}
          </option>
        ))}
      </SelectField>
      {sinAlmacenes ? (
        <p role="alert" className="text-danger-600 dark:text-danger-400 text-sm md:col-span-3">
          Este establecimiento no tiene almacenes habilitados para venta.
        </p>
      ) : null}
    </Card>
  );
}
```

`SRC/features/pos/pages/PosPage.tsx`: reemplazar `useEstablecimientos` por `useMiPuesto` en el import desde `'../../organizacion'`, la línea `const establecimientos = useEstablecimientos();` por `const { establecimientos } = useMiPuesto();` y, en la línea ~163, `?.name ?? ''` por `?.nombre ?? ''`.

Adaptar los tests existentes: en `PuestoVenta.test.tsx` (`beforeEach`), `PosPage.test.tsx` (`mockEntorno`) y `CashRegisterPage.test.tsx` (`beforeEach`) reemplazar el par de handlers `http.get('*/api/v1/estructura-corporativa', ...)` + `http.get('*/api/v1/organizacion/terminales-pos', ...)` por `miPuestoHandler()` (import desde `'../../../test/mi-puesto-fixtures'`) y quitar los imports que queden sin uso (`sampleEstructura`, `pagina`, `sampleTerminal`, `http`/`HttpResponse` si ya no se usan). Los nombres de los establecimientos y terminales de `sampleMiPuesto` coinciden con los que esos tests ya esperaban (`Botica Central`, `Caja 1`, `Caja 2`, `Almacén Central`).

`E2E/support/caja-api.ts` y `E2E/support/pos-api.ts`: reemplazar los `page.route` de `**/api/v1/estructura-corporativa` y `/organizacion/terminales-pos` por

```ts
  await page.route('**/api/v1/organizacion/mi-puesto', (route) =>
    json(route, 200, sampleMiPuesto)
  );
```

con `import { sampleMiPuesto } from '../../apps/erp-web/src/test/mi-puesto-fixtures';` y eliminar el `terminal`/`pagina` locales y el import de `sampleEstructura` si quedan sin uso.

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm --filter @boticas/erp-web test -- src/features/organizacion src/features/pos src/features/caja` y luego `pnpm check` (lint + typecheck + test con cobertura + build) y `pnpm test:e2e` si el repo lo expone (ver `frontend/package.json`; si no, `pnpm exec playwright test e2e/caja.spec.ts e2e/pos.spec.ts`).
Expected: PASS; sin archivos nuevos por debajo del 100 %.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(frontend): Caja y POS leen el puesto de trabajo desde mi-puesto

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Frontend — acceso por permisos (menú, rutas, 403 y botones)

**Files:**
- Modify: `SRC/features/auth/api/profile.types.ts`, `SRC/features/auth/model/useProfile.ts`, `SRC/features/auth/index.ts`
- Create: `SRC/features/auth/lib/permisos.ts`, `SRC/features/auth/model/usePermisos.ts`
- Create: `SRC/shared/layout/acceso-secciones.ts`, `SRC/shared/layout/RequireSectionAccess.tsx`, `SRC/shared/pages/ForbiddenPage.tsx`
- Modify: `SRC/shared/layout/AppShell.tsx`, `SRC/app/router.tsx`
- Modify: `SRC/features/seguridad/pages/UsersPage.tsx`, `RolesPage.tsx`, `SRC/features/organizacion/pages/EmpresasPage.tsx`
- Modify (fixtures y tests): `SRC/test/mocks/handlers.ts`, `SRC/shared/layout/AppShell.test.tsx`, `SRC/features/auth/api/profile.api.test.ts`, `SRC/features/auth/model/AuthSessionProvider.test.tsx`, tests de las tres páginas con botón
- Test: `SRC/features/auth/lib/permisos.test.ts`, `SRC/features/auth/model/usePermisos.test.tsx`, `SRC/shared/layout/acceso-secciones.test.ts`, `SRC/shared/layout/RequireSectionAccess.test.tsx`, `SRC/shared/pages/ForbiddenPage.test.tsx`
- Modify: `E2E/support/login.ts`

**Interfaces:**
- Consumes: `GET /auth/me` con `permissions: string[]` (Task 2).
- Produces: `tienePermiso(permisos: readonly string[], requerido: string): boolean` (un requerido que termina en `.` es prefijo; si no, coincidencia exacta); `useProfile()` con `permissions: string[]` y `permissionsReady: boolean`; `usePermisos(): { puede(requerido: string): boolean }`; `puedeAcceder(permisos, pathname): boolean`; `RequireSectionAccess` (layout route); `ForbiddenPage`.

Mapa de secciones (decisión del spec; `clientes` es un placeholder sin permisos propios y se vincula a ventas):

| Sección | Permiso requerido |
|---|---|
| dashboard, perfil | ninguno |
| catalogo | `catalogo.` |
| inventario | `inventario.` |
| compras | `compras.` |
| ventas | `ventas.ventas.consultar` |
| pos | `ventas.ventas.registrar` |
| caja | `ventas.turnos.` |
| clientes | `ventas.ventas.` |
| seguridad | `seguridad.` |
| organizacion | `organizacion.` |

- [ ] **Step 1: Escribir los tests que fallan**

`SRC/features/auth/lib/permisos.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { tienePermiso } from './permisos';

describe('tienePermiso', () => {
  const permisos = ['ventas.turnos.abrir', 'catalogo.skus.consultar'];

  it('acepta una coincidencia exacta', () => {
    expect(tienePermiso(permisos, 'ventas.turnos.abrir')).toBe(true);
  });

  it('rechaza un permiso que no se tiene', () => {
    expect(tienePermiso(permisos, 'ventas.turnos.cerrar')).toBe(false);
  });

  it('trata un requerido terminado en punto como prefijo', () => {
    expect(tienePermiso(permisos, 'ventas.')).toBe(true);
    expect(tienePermiso(permisos, 'seguridad.')).toBe(false);
  });

  it('no concede un prefijo a quien no tiene permisos', () => {
    expect(tienePermiso([], 'ventas.')).toBe(false);
  });
});
```

`SRC/shared/layout/acceso-secciones.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { puedeAcceder, seccionDe } from './acceso-secciones';

describe('acceso por sección', () => {
  const cajero = ['ventas.turnos.abrir', 'ventas.ventas.registrar', 'ventas.ventas.consultar'];

  it('obtiene la sección del primer segmento de la ruta', () => {
    expect(seccionDe('/caja')).toBe('caja');
    expect(seccionDe('/seguridad/usuarios/1')).toBe('seguridad');
    expect(seccionDe('/')).toBe('');
  });

  it('permite sin permisos el resumen y el perfil', () => {
    expect(puedeAcceder([], '/dashboard')).toBe(true);
    expect(puedeAcceder([], '/perfil')).toBe(true);
  });

  it('permite una ruta desconocida para que responda la página 404', () => {
    expect(puedeAcceder([], '/no-existe')).toBe(true);
  });

  it('un cajero accede a caja, POS y ventas pero no a seguridad ni organización', () => {
    expect(puedeAcceder(cajero, '/caja')).toBe(true);
    expect(puedeAcceder(cajero, '/pos')).toBe(true);
    expect(puedeAcceder(cajero, '/ventas')).toBe(true);
    expect(puedeAcceder(cajero, '/seguridad')).toBe(false);
    expect(puedeAcceder(cajero, '/organizacion/empresas')).toBe(false);
  });
});
```

`SRC/features/auth/model/usePermisos.test.tsx`:

```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { renderRoute } from '../../../test/render-route';
import { usePermisos } from './usePermisos';

function Probe() {
  const { puede } = usePermisos();
  return <p>{puede('ventas.turnos.') ? 'puede' : 'no puede'}</p>;
}

describe('usePermisos', () => {
  it('evalúa los permisos del perfil autenticado', async () => {
    server.use(
      http.get('*/api/v1/auth/me', () =>
        HttpResponse.json({
          userId: 'user-1',
          tenantId: 'tenant-1',
          displayName: 'Luis',
          username: 'luis',
          email: null,
          assignments: [],
          permissions: ['ventas.turnos.abrir']
        })
      )
    );
    renderRoute('/x', Probe, '/x');

    expect(await screen.findByText('puede')).toBeInTheDocument();
  });

  it('no concede nada mientras el perfil no ha cargado', () => {
    renderRoute('/x', Probe, '/x');

    expect(screen.getByText('no puede')).toBeInTheDocument();
  });
});
```

`SRC/shared/layout/RequireSectionAccess.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthSessionContext } from '../../features/auth/model/auth-session.context';
import { server } from '../../test/mocks/server';
import { RequireSectionAccess } from './RequireSectionAccess';

const session = {
  status: 'authenticated' as const,
  authenticated: true,
  accessToken: 'token',
  tenantId: 'tenant-1',
  userId: 'user-1',
  passwordChangeRequired: false,
  authenticate: vi.fn(),
  changePassword: vi.fn(),
  signOut: vi.fn()
};

const perfil = (permissions: string[]) => ({
  userId: 'user-1',
  tenantId: 'tenant-1',
  displayName: 'Luis',
  username: 'luis',
  email: null,
  assignments: [],
  permissions
});

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionContext value={session}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route element={<RequireSectionAccess />}>
              <Route path="/caja" element={<p>pantalla de caja</p>} />
              <Route path="/seguridad" element={<p>pantalla de seguridad</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthSessionContext>
    </QueryClientProvider>
  );
}

describe('RequireSectionAccess', () => {
  it('muestra la sección cuando el usuario tiene el permiso', async () => {
    server.use(http.get('*/api/v1/auth/me', () => HttpResponse.json(perfil(['ventas.turnos.abrir']))));
    renderAt('/caja');

    expect(await screen.findByText('pantalla de caja')).toBeInTheDocument();
  });

  it('muestra la página 403 cuando falta el permiso', async () => {
    server.use(http.get('*/api/v1/auth/me', () => HttpResponse.json(perfil(['ventas.turnos.abrir']))));
    renderAt('/seguridad');

    expect(await screen.findByRole('heading', { name: 'No tienes acceso a esta sección' })).toBeInTheDocument();
    expect(screen.queryByText('pantalla de seguridad')).not.toBeInTheDocument();
  });

  it('no muestra nada mientras carga el perfil', () => {
    server.use(http.get('*/api/v1/auth/me', () => new Promise(() => undefined)));
    const { container } = renderAt('/caja');

    expect(container).toBeEmptyDOMElement();
  });
});
```

`SRC/shared/pages/ForbiddenPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { ForbiddenPage } from './ForbiddenPage';

describe('ForbiddenPage', () => {
  it('explica la falta de acceso y ofrece volver al resumen', () => {
    render(
      <MemoryRouter>
        <ForbiddenPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: 'No tienes acceso a esta sección' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir al resumen' })).toHaveAttribute('href', '/dashboard');
  });
});
```

Agregar a `AppShell.test.tsx` (usando el helper `renderShell` y `server.use` del archivo; el fixture `cajero` recibe `permissions: ['ventas.turnos.abrir', 'ventas.ventas.registrar', 'ventas.ventas.consultar']`):

```tsx
  it('muestra en el menú solo las secciones permitidas por los permisos del usuario', async () => {
    server.use(http.get('*/api/v1/auth/me', () => HttpResponse.json(cajero)));
    renderShell();

    expect(await screen.findByRole('link', { name: 'Caja' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Punto de venta' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Resumen' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Seguridad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Organización' })).not.toBeInTheDocument();
  });
```

Para cada una de las tres páginas con botón (`UsersPage`, `RolesPage`, `EmpresasPage`) agregar un test «oculta el botón de crear sin el permiso de gestión» que registra `/auth/me` sin permisos y verifica `queryByRole('button', { name: ... })` ausente tras cargar la lista; los tests existentes siguen usando el handler por defecto con todos los permisos.

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- src/features/auth src/shared`
Expected: FAIL (módulos nuevos inexistentes).

- [ ] **Step 3: Implementar**

`SRC/features/auth/api/profile.types.ts`: agregar `permissions: string[];` al final de `PerfilUsuario`.

`SRC/features/auth/lib/permisos.ts`:

```ts
export const tienePermiso = (permisos: readonly string[], requerido: string): boolean =>
  requerido.endsWith('.')
    ? permisos.some((permiso) => permiso.startsWith(requerido))
    : permisos.includes(requerido);
```

`SRC/features/auth/model/useProfile.ts`: agregar al tipo `ProfileView` los campos `permissions: string[]` y `permissionsReady: boolean`; en el caso sin datos devolver `permissions: []` y `permissionsReady: !isLoading`; en el caso con datos `permissions: data.permissions` y `permissionsReady: true`.

`SRC/features/auth/model/usePermisos.ts`:

```ts
import { tienePermiso } from '../lib/permisos';
import { useProfile } from './useProfile';

export function usePermisos() {
  const { permissions } = useProfile();
  return { puede: (requerido: string) => tienePermiso(permissions, requerido) };
}
```

`SRC/features/auth/index.ts`: agregar

```ts
export { usePermisos } from './model/usePermisos';
export { tienePermiso } from './lib/permisos';
```

`SRC/shared/layout/acceso-secciones.ts`:

```ts
import { tienePermiso } from '../../features/auth';

const ACCESO_POR_SECCION: Record<string, string | null> = {
  dashboard: null,
  perfil: null,
  catalogo: 'catalogo.',
  inventario: 'inventario.',
  compras: 'compras.',
  ventas: 'ventas.ventas.consultar',
  pos: 'ventas.ventas.registrar',
  caja: 'ventas.turnos.',
  clientes: 'ventas.ventas.',
  seguridad: 'seguridad.',
  organizacion: 'organizacion.'
};

export const seccionDe = (pathname: string): string => pathname.split('/')[1] ?? '';

export function puedeAcceder(permisos: readonly string[], pathname: string): boolean {
  const requerido = ACCESO_POR_SECCION[seccionDe(pathname)];
  return requerido === undefined || requerido === null || tienePermiso(permisos, requerido);
}
```

`SRC/shared/pages/ForbiddenPage.tsx`:

```tsx
import { Link } from 'react-router';
import { Card } from '@boticas/ui-web';

export function ForbiddenPage() {
  return (
    <Card className="mx-auto max-w-xl p-8 text-center">
      <p className="text-primary-700 dark:text-primary-400 text-sm font-bold">403</p>
      <h1 className="mt-2 text-2xl font-bold text-neutral-950 dark:text-white">
        No tienes acceso a esta sección
      </h1>
      <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
        Tu rol no incluye los permisos necesarios. Si lo necesitas, pide acceso a tu administrador.
      </p>
      <Link
        className="text-primary-700 hover:text-primary-900 dark:text-primary-400 dark:hover:text-primary-200 mt-6 inline-flex text-sm font-semibold"
        to="/dashboard"
      >
        Ir al resumen
      </Link>
    </Card>
  );
}
```

`SRC/shared/layout/RequireSectionAccess.tsx`:

```tsx
import { Outlet, useLocation } from 'react-router';
import { useProfile } from '../../features/auth';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { puedeAcceder } from './acceso-secciones';

export function RequireSectionAccess() {
  const { pathname } = useLocation();
  const { permissions, permissionsReady } = useProfile();

  if (!permissionsReady) {
    return null;
  }

  return puedeAcceder(permissions, pathname) ? <Outlet /> : <ForbiddenPage />;
}
```

`SRC/shared/layout/AppShell.tsx`: importar `puedeAcceder` de `./acceso-secciones`; `SidebarContent` pasa a recibir `items: NavigationItem[]` y mapear `items.map(...)` en lugar de `navigation.map(...)`; en `AppShell`, tras `const profile = useProfile();`:

```tsx
  const items = navigation.filter(({ to }) => puedeAcceder(profile.permissions, to));
```

y ambos `<SidebarContent ... />` reciben `items={items}`.

`SRC/app/router.tsx`: importar `RequireSectionAccess` de `'../shared/layout/RequireSectionAccess'` y envolver las rutas hijas de `AppShell`:

```tsx
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          {
            element: <RequireSectionAccess />,
            children: [...erpFeatureRoutes, { path: '*', element: <NotFoundPage /> }]
          }
        ]
```

Botones condicionados (con `import { usePermisos } from '../../auth'`... **no**: las features no importan archivos internos de otras, pero sí el `index.ts`; usar `import { usePermisos } from '../../auth';`):

- `UsersPage.tsx`: `const { puede } = usePermisos();` y renderizar el `<Button>` de «Nuevo usuario» solo si `puede('seguridad.usuarios.gestionar')` (`actions={puede(...) ? (<Button ...>Nuevo usuario</Button>) : undefined}`).
- `RolesPage.tsx`: «Nuevo rol» solo si `puede('seguridad.roles.gestionar')`.
- `EmpresasPage.tsx`: «Nueva empresa» solo si `puede('organizacion.empresas.gestionar')`.

Fixtures y mocks: en `SRC/test/mocks/handlers.ts`, el handler por defecto de `*/api/v1/auth/me` agrega

```ts
      permissions: [
        'catalogo.skus.consultar',
        'inventario.posiciones.consultar',
        'compras.ordenes.consultar',
        'ventas.ventas.consultar',
        'ventas.ventas.registrar',
        'ventas.turnos.abrir',
        'seguridad.usuarios.gestionar',
        'seguridad.roles.gestionar',
        'organizacion.empresas.gestionar'
      ]
```

Agregar `permissions` a los perfiles de `AppShell.test.tsx` (`cajero`), `profile.api.test.ts` y `AuthSessionProvider.test.tsx` (el compilador de TypeScript marca cada uno).

`E2E/support/login.ts`: en `mockAuthApi`, agregar

```ts
  await page.route('**/api/v1/auth/me', (route) =>
    json(route, 200, {
      userId: session.userId,
      tenantId: session.tenantId,
      displayName: 'María Rojas',
      username: 'mrojas',
      email: 'maria.rojas@boticas.pe',
      assignments: [],
      permissions: [
        'catalogo.skus.consultar',
        'inventario.posiciones.consultar',
        'compras.ordenes.consultar',
        'ventas.ventas.consultar',
        'ventas.ventas.registrar',
        'ventas.turnos.abrir',
        'seguridad.usuarios.gestionar',
        'seguridad.roles.gestionar',
        'organizacion.empresas.gestionar'
      ]
    })
  );
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm check` y la suite Playwright (`pnpm exec playwright test`).
Expected: PASS. Si algún test existente de página falla por la carga asíncrona del botón, cambiar su `getByRole` por `await findByRole`. Si `feature-routes.test.ts` o los e2e de layout asumen el menú completo, usar el perfil por defecto (con permisos) que ya devuelve el mock.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat(frontend): menu, rutas y botones por permiso con pagina 403

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Frontend — login con usuario o correo

**Files:**
- Modify: `SRC/features/auth/schemas/login.schema.ts`
- Modify: `SRC/features/auth/components/LoginForm.tsx`
- Modify: `SRC/features/auth/model/AuthSessionProvider.tsx:94`
- Test: `SRC/features/auth/schemas/login.schema.test.ts` (crear), `SRC/features/auth/pages/LoginPage.test.tsx`, `SRC/features/auth/model/AuthSessionProvider.test.tsx`, `E2E/support/login.ts` (revisar)

**Interfaces:**
- Produces: `LoginCredentials = { login: string; password: string; remember: boolean }`.

- [ ] **Step 1: Escribir el test que falla**

`SRC/features/auth/schemas/login.schema.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { loginSchema } from './login.schema';

const base = { password: 'Boticas2026!', remember: false };

describe('loginSchema', () => {
  it('acepta un nombre de usuario sin arroba', () => {
    expect(loginSchema.safeParse({ ...base, login: 'crit001.manana1' }).success).toBe(true);
  });

  it('acepta un correo válido', () => {
    expect(loginSchema.safeParse({ ...base, login: 'maria.rojas@boticas.pe' }).success).toBe(true);
  });

  it('rechaza un valor con arroba que no es un correo válido', () => {
    const resultado = loginSchema.safeParse({ ...base, login: 'maria@' });

    expect(resultado.success).toBe(false);
    expect(resultado.error?.issues[0]?.message).toBe('Ingresa un correo electrónico válido.');
  });

  it('rechaza un valor vacío', () => {
    const resultado = loginSchema.safeParse({ ...base, login: '   ' });

    expect(resultado.success).toBe(false);
    expect(resultado.error?.issues[0]?.message).toBe('Ingresa tu usuario o correo electrónico.');
  });

  it('exige una contraseña de al menos 8 caracteres', () => {
    expect(loginSchema.safeParse({ ...base, login: 'luis', password: 'corta' }).success).toBe(false);
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `pnpm --filter @boticas/erp-web test -- src/features/auth/schemas`
Expected: FAIL (el schema aún usa `email`).

- [ ] **Step 3: Implementar**

`SRC/features/auth/schemas/login.schema.ts`:

```ts
import { z } from 'zod';

const esCorreoValido = (valor: string) => !valor.includes('@') || z.email().safeParse(valor).success;

export const loginSchema = z.object({
  login: z
    .string()
    .trim()
    .min(1, 'Ingresa tu usuario o correo electrónico.')
    .refine(esCorreoValido, 'Ingresa un correo electrónico válido.'),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.'),
  remember: z.boolean()
});

export type LoginCredentials = z.infer<typeof loginSchema>;
```

`SRC/features/auth/components/LoginForm.tsx`: en el bloque del campo de usuario: `htmlFor="login"`, `id="login"`, `type="text"`, `register('login')`, `errors.login` (en `aria-describedby`, `aria-invalid`, el `id="login-error"` y el mensaje), y `placeholder="ej. jperez o jperez@farmavita.pe"`; en `defaultValues` y en cualquier otra referencia, renombrar `email` por `login`.

`SRC/features/auth/model/AuthSessionProvider.tsx` (línea ~94): `login: credentials.login,`.

Tests existentes: en `LoginPage.test.tsx` el mensaje de envío vacío esperado pasa de `Ingresa un correo electrónico válido.` a `Ingresa tu usuario o correo electrónico.`; agregar un caso «inicia sesión con un nombre de usuario» que escribe `crit001.manana1` en el campo `Usuario o correo electrónico` y verifica que llega al resumen; en `AuthSessionProvider.test.tsx` y cualquier otro uso de `LoginCredentials` renombrar `email:` por `login:` (`rg -n "email:" frontend/apps/erp-web/src/features/auth` para localizarlos). `E2E/support/login.ts` no requiere cambios (rellena por etiqueta).

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `pnpm check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "fix(frontend): el login acepta usuario o correo electronico

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Verificación extremo a extremo y documentación

**Files:**
- Modify: `CLAUDE.md` (secciones del módulo `organizacion`, `security` y frontend)
- Modify: `docs/superpowers/specs/2026-10-15-organizacion-superadmin-delegacion-design.md` (§8, marcar D como resuelto por este plan)

- [ ] **Step 1: Gate completo**

Run (desde `service-botica/`): `.\gradlew.bat check --warning-mode all`; (desde `frontend/`): `pnpm check` y `pnpm exec playwright test`.
Expected: todo en verde.

- [ ] **Step 2: Reconstruir y levantar**

Run (raíz del repo): `docker compose build app; docker compose up -d app`; `pnpm dev` en `frontend/` (puerto 3000).
Expected: contenedores `healthy`; `http://localhost:3000` carga el login.

- [ ] **Step 3: Verificación manual en el navegador**

Con la contraseña de pruebas de los cajeros `crit001.*` (conocida por el dueño del producto; no se escribe en el repo):

1. Iniciar sesión como `crit001.manana1` **con el nombre de usuario** (sin `@`). Esperado: entra a Resumen; el menú muestra solo Resumen, Catálogo, Inventario, Ventas, Punto de venta, Caja y Clientes; no aparecen Seguridad ni Organización; abrir `/seguridad` a mano muestra la página 403.
2. `/caja`: el selector ofrece solo «Botica Crítica Norte» y sus terminales `CRIT-POS`/`CRIT-POS2` (ambas operables); abrir turno con fondo inicial en `CRIT-POS`.
3. `/pos`: elegir terminal y almacén `CRIT-ALM` (permite venta), agregar un SKU con stock, cobrar en efectivo. Esperado: venta registrada.
4. `/caja`: cerrar el turno con arqueo. Esperado: turno cerrado con diferencia calculada.
5. Cerrar sesión; iniciar como `crit001.tarde1`; abrir turno en `CRIT-POS2`. Esperado: no ve el turno de la mañana.
6. Iniciar como `ui001.manana1` en `/caja`: la terminal `UI-POS` aparece deshabilitada con «(en mantenimiento)».
7. Iniciar como `e2e.inventario`/`wmsullcaray` (ADMIN global): ven todos los establecimientos en el selector y el menú completo.

Si algún paso falla, no se da la tarea por terminada: abrir el diagnóstico con `systematic-debugging` antes de seguir.

- [ ] **Step 4: Documentación**

`CLAUDE.md`: en el bloque de `organizacion` agregar que expone `GET /api/v1/organizacion/mi-puesto` (alcance del usuario vía `security.api.AlcanceUsuario`; autenticado, sin permiso `organizacion.*`) y que ahora depende de `security::api`; en `security`, que `GET /auth/me` devuelve `permissions` y publica `AlcanceUsuario`; en `frontend/`, que Caja y POS leen `mi-puesto`, que el menú, las rutas (`RequireSectionAccess`) y los botones principales de Seguridad y Organización dependen de `permissions`, y que el login acepta usuario o correo. En el spec del 15-oct §8, anotar junto a «D» que lo resuelve `docs/superpowers/plans/2026-10-19-mi-puesto-y-acceso-por-permisos.md`.

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md docs
git commit -m "docs: mi-puesto y acceso por permisos en CLAUDE.md y el spec de organizacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-review del plan

- **Cobertura del spec:** §2.1 → Task 1; §2.2 y §2.3 → Tasks 3–5; §2.4 → Task 2; §2.5 sin migración (confirmado en Constraints); §3.1 → Task 6; §3.2 → Task 7; §3.3 → Task 8; §4 Pruebas → pruebas dentro de cada tarea más la verificación manual de la Task 9; datos de prueba (`CRIT-ALM`) ya corregidos antes del plan.
- **Consistencia de tipos:** `AlcanceResultado` (security) ↔ `AlcancePuesto` (organizacion) comparten los cinco campos; `EstablecimientoPuestoResult`/`TerminalPuestoResult`/`AlmacenPuestoResult` se usan con la misma firma en Tasks 3–5; `MiPuesto*` del frontend refleja `MiPuestoResponse`; `PerfilResult.permissions` ↔ `PerfilUsuario.permissions` ↔ `useProfile().permissions`.
- **Riesgos conocidos:** (a) los tests de página existentes pueden requerir `findByRole` por la carga asíncrona del perfil; (b) los filtros JSONPath con `[?(...)]` devuelven listas, el plan indica el matcher alternativo; (c) `useEstablecimientos` y `almacenesDeEstablecimiento` quedan exportados y sin uso en Caja/POS, y compras/ventas siguen dependiendo de `/estructura-corporativa` (fuera de alcance, el cajero seguirá recibiendo 403 en esas pantallas hasta migrarlas).
