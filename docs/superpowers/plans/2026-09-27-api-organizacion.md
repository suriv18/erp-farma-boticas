# API de Organización (Empresa, Establecimiento, Almacén, Terminal POS) — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar la API REST completa (CRUD + estructura corporativa) para Empresa Operadora, Establecimiento Farmacéutico, Almacén y Terminal POS en el módulo `organizacion` del backend, reemplazando el slice parcial existente.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS pragmático, replicando exactamente el patrón del módulo `security`: puertos de salida amplios (`OrganizacionReadPort`/`OrganizacionWritePort`) en vez de uno por agregado, un adapter JDBC de lectura único y un adapter JPA de escritura único para todo el módulo, mappers dedicados por capa.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, Spring Data JPA, `JdbcClient`, Gradle 9.5.1, JUnit 5, Mockito, H2 (perfil test), PostgreSQL (perfil normal).

## Global Constraints

- Spec de referencia: `docs/superpowers/specs/2026-09-27-api-organizacion-design.md` — toda ambigüedad se resuelve releyendo ese archivo, no improvisando.
- Plantilla arquitectónica de referencia: módulo `service-botica/modules/security/` — mismo patrón exacto de nombres de paquetes, convenciones de VOs (`record XId(UUID value)`), agregados (`create`/`restore`/métodos de comando devolviendo `Result<T, ErrorDetail>`), handlers (`Result<T, ApplicationError>`, `StandardApplicationError` con `ErrorCategory`), adapters (`JdbcClient` + `JpaRepository.saveAndFlush`), y controllers (`@PreAuthorize`, `IamControllerSupport::problem` equivalente).
- Las tablas de `sch_organizacion` YA EXISTEN y están aplicadas (`docs/cadena-farmacias-docs/database/migrations/V002__organizacion_establecimientos.sql`) — NO se crean ni modifican migraciones de esquema. Solo se agrega `V025__seed_organizacion_permissions.sql` (siguiente número libre tras V024) para los permisos RBAC.
- `tenant_id` en las tablas de `sch_organizacion` es `BIGINT` (FK a `sch_admin.tenant.id`), NO `UUID`. El dominio usa `TenantId(UUID value)` como wrapper del `uuid_publico` del tenant — la resolución `UUID → BIGINT` se hace en el adapter de infraestructura vía `JdbcClient`, exactamente como `IamJpaWriteAdapter.findTenantId(UUID)`.
- Todas las tablas usan patrón `id BIGINT IDENTITY` interno + `uuid_publico UUID UNIQUE` como identidad pública — igual que `RolJpaEntity`. El dominio y la API solo conocen el `uuid_publico`.
- Paginación: `PaginaResult<T>`/`PaginaResponse<T>` — mismos records ya usados por `security` (`items`, `page`, `size`, `totalElements`), recreados en `organizacion` (los módulos no comparten paquetes internos entre sí).
- Rutas REST bajo `/api/v1/organizacion/{recurso}`, excepto `/api/v1/estructura-corporativa` (sin prefijo, para no romper el frontend existente).
- Permisos: `organizacion.{recurso}.consultar` / `organizacion.{recurso}.gestionar`, recursos = `empresas`, `establecimientos`, `almacenes`, `terminales-pos`.
- Cobertura 100% líneas/ramas obligatoria para todo archivo nuevo (regla del proyecto, `CLAUDE.md`) — cada tarea debe llegar a 100% antes de darse por completa.
- Los 10 archivos Java existentes en `modules/organizacion/` (agregado `EmpresaOperadora` desalineado, comando/handler/puerto de solo `save`) se ELIMINAN y se reemplazan — no se extienden ni se mantiene compatibilidad con el `RegistrarEmpresaOperadoraCommand` actual.
- Fuera de alcance (no crear código para esto): autorización sanitaria, Director Técnico/QF, personal técnico, horarios, `ubicacion_almacen`. No debe aparecer ningún archivo relacionado a estas entidades en este plan.

---

## Task 1: Limpieza del slice existente + build.gradle + VOs de identidad + dominio `EmpresaOperadora`

**Files:**
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/EmpresaOperadoraRegistrada.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/RegistrarEmpresaOperadoraCommand.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandler.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/EmpresaOperadoraRepository.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/empresa/EmpresaOperadora.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/empresa/EmpresaOperadoraId.java`
- Delete: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/empresa/EstadoEmpresaOperadora.java`
- Delete: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandlerTest.java`
- Modify: `service-botica/modules/organizacion/build.gradle`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/TenantId.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/EmpresaOperadoraId.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/EstadoEmpresaOperadora.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/EmpresaOperadora.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/package-info.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/EmpresaOperadoraTest.java`

**Interfaces:**
- Produces: `TenantId(UUID value)`, `EmpresaOperadoraId(UUID value)` — value objects reutilizados por todas las tareas siguientes.
- Produces: `EmpresaOperadora` con factory `create(EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial, String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono, String email, String sitioWeb, String monedaFuncional, String zonaHoraria, boolean permiteVentaOnline, Instant createdAt)` → `Result<EmpresaOperadora, ErrorDetail>`; factory `restore(...)` (mismos campos + `EstadoEmpresaOperadora estado` + `Instant updatedAt`, sin validar); método `updateDetails(String razonSocial, String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono, String email, String sitioWeb, String monedaFuncional, String zonaHoraria, boolean permiteVentaOnline, Instant updatedAt)` → `Result<EmpresaOperadora, ErrorDetail>`; método `cambiarEstado(EstadoEmpresaOperadora nuevoEstado, Instant updatedAt)` → `Result<EmpresaOperadora, ErrorDetail>`.
- Produces: `EstadoEmpresaOperadora` enum `{ACTIVO, SUSPENDIDO, BLOQUEADO}`.
- Código de error de validación: `"ORG_EMPRESA_INVALIDA"`.

- [ ] **Step 1: Eliminar los 8 archivos del slice existente**

```bash
cd service-botica/modules/organizacion
rm src/main/java/com/softprimesolutions/organizacion/api/EmpresaOperadoraRegistrada.java
rm src/main/java/com/softprimesolutions/organizacion/api/RegistrarEmpresaOperadoraCommand.java
rm src/main/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandler.java
rm src/main/java/com/softprimesolutions/organizacion/application/port/EmpresaOperadoraRepository.java
rm src/main/java/com/softprimesolutions/organizacion/domain/empresa/EmpresaOperadora.java
rm src/main/java/com/softprimesolutions/organizacion/domain/empresa/EmpresaOperadoraId.java
rm src/main/java/com/softprimesolutions/organizacion/domain/empresa/EstadoEmpresaOperadora.java
rm src/test/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandlerTest.java
rmdir src/main/java/com/softprimesolutions/organizacion/domain/empresa
rmdir src/main/java/com/softprimesolutions/organizacion/application/port
```

No borrar `api/package-info.java` ni `package-info.java` raíz — se reutilizan en tareas siguientes.

- [ ] **Step 2: Actualizar `build.gradle` del módulo**

Reemplazar todo el contenido de `service-botica/modules/organizacion/build.gradle` por:

```gradle
plugins { id 'boticas.spring-module'; id 'boticas.quality' }
dependencies {
    api project(':shared-kernel')
    implementation project(':shared-application')
    implementation project(':shared-web')
    implementation project(':shared-persistence')

    implementation 'org.springframework:spring-context'
    implementation 'org.springframework:spring-web'
    implementation 'org.springframework:spring-tx'
    implementation 'org.springframework:spring-jdbc'
    implementation 'org.springframework.data:spring-data-jpa'
    implementation 'jakarta.persistence:jakarta.persistence-api'
    implementation 'jakarta.validation:jakarta.validation-api'
}
```

- [ ] **Step 3: Crear `domain/valueobject/package-info.java`**

```java
package com.softprimesolutions.organizacion.domain.valueobject;
```

- [ ] **Step 4: Crear `TenantId`**

```java
package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/** Identificador público del tenant; la PK BIGINT permanece en persistencia. */
public record TenantId(UUID value) {
    public TenantId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [ ] **Step 5: Crear `EmpresaOperadoraId`**

```java
package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record EmpresaOperadoraId(UUID value) {
    public EmpresaOperadoraId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [ ] **Step 6: Crear `domain/model/package-info.java`**

```java
package com.softprimesolutions.organizacion.domain.model;
```

- [ ] **Step 7: Crear `EstadoEmpresaOperadora`**

```java
package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEmpresaOperadora {
    ACTIVO,
    SUSPENDIDO,
    BLOQUEADO
}
```

- [ ] **Step 8: Escribir el test que falla para `EmpresaOperadora.create` (caso feliz y validaciones)**

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/EmpresaOperadoraTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmpresaOperadoraTest {

    private static final EmpresaOperadoraId ID = new EmpresaOperadoraId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", "Boticas",
                "Av. Siempre Viva 123", "150101", "014445566", "contacto@boticas.pe",
                "https://boticas.pe", "PEN", "America/Lima", true, NOW);

        assertThat(result.isSuccess()).isTrue();
        var empresa = result.fold(e -> e, error -> {
            throw new AssertionError(error.message());
        });
        assertThat(empresa.id()).isEqualTo(ID);
        assertThat(empresa.tenantId()).isEqualTo(TENANT_ID);
        assertThat(empresa.ruc()).isEqualTo("20123456789");
        assertThat(empresa.razonSocial()).isEqualTo("Boticas SAC");
        assertThat(empresa.nombreComercial()).isEqualTo("Boticas");
        assertThat(empresa.monedaFuncional()).isEqualTo("PEN");
        assertThat(empresa.zonaHoraria()).isEqualTo("America/Lima");
        assertThat(empresa.permiteVentaOnline()).isTrue();
        assertThat(empresa.estado()).isEqualTo(EstadoEmpresaOperadora.ACTIVO);
        assertThat(empresa.createdAt()).isEqualTo(NOW);
        assertThat(empresa.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "10123456789", "Juan Perez EIRL", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = EmpresaOperadora.create(
                null, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = EmpresaOperadora.create(
                ID, null, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsRucWithWrongLength() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "2012345678", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(e -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_EMPRESA_INVALIDA");
            assertThat(error.metadata()).containsEntry("field", "ruc");
            return null;
        });
    }

    @Test
    void rejectsRucNotStartingWith10Or20() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "30123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankRazonSocial() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", " ", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsInvalidUbigeoFiscal() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, "15010", null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsMonedaFuncionalWithWrongLength() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "SOLES", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankZonaHoraria() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", " ", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var empresa = EmpresaOperadora.restore(
                ID, TENANT_ID, "20123456789", "Boticas SAC", "Boticas",
                "Av. Siempre Viva 123", "150101", "014445566", "contacto@boticas.pe",
                "https://boticas.pe", "PEN", "America/Lima", true,
                EstadoEmpresaOperadora.SUSPENDIDO, NOW, NOW.plusSeconds(60));

        assertThat(empresa.estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
        assertThat(empresa.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetailsKeepingRucAndId() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var updated = empresa.updateDetails(
                        "Boticas del Peru SAC", "Boticas", "Nueva direccion", "150102",
                        "014445577", "nuevo@boticas.pe", "https://boticas.pe", "PEN",
                        "America/Lima", true, NOW.plusSeconds(120))
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.ruc()).isEqualTo("20123456789");
        assertThat(updated.razonSocial()).isEqualTo("Boticas del Peru SAC");
        assertThat(updated.permiteVentaOnline()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithInvalidRazonSocial() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "A", null, null, null, null, null, null, "PEN", "America/Lima", false,
                NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, "PEN", "America/Lima",
                false, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void changesEstado() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(EstadoEmpresaOperadora.SUSPENDIDO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(e -> {
            assertThat(e.estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(EstadoEmpresaOperadora.SUSPENDIDO, null);

        assertThat(result.isFailure()).isTrue();
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.EmpresaOperadoraTest"`
Expected: FAIL (compilation error — `EmpresaOperadora` no existe todavía)

- [ ] **Step 9: Implementar `EmpresaOperadora`**

```java
package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/** Empresa legal/corporativa que opera una o más boticas. */
public final class EmpresaOperadora extends AggregateRoot {

    private static final Pattern RUC_PATTERN = Pattern.compile("(10|20)[0-9]{9}");
    private static final Pattern UBIGEO_PATTERN = Pattern.compile("[0-9]{6}");

    private final EmpresaOperadoraId id;
    private final TenantId tenantId;
    private final String ruc;
    private final String razonSocial;
    private final String nombreComercial;
    private final String direccionFiscal;
    private final String ubigeoFiscal;
    private final String telefono;
    private final String email;
    private final String sitioWeb;
    private final String monedaFuncional;
    private final String zonaHoraria;
    private final boolean permiteVentaOnline;
    private final EstadoEmpresaOperadora estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private EmpresaOperadora(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, EstadoEmpresaOperadora estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.direccionFiscal = direccionFiscal;
        this.ubigeoFiscal = ubigeoFiscal;
        this.telefono = telefono;
        this.email = email;
        this.sitioWeb = sitioWeb;
        this.monedaFuncional = monedaFuncional;
        this.zonaHoraria = zonaHoraria;
        this.permiteVentaOnline = permiteVentaOnline;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<EmpresaOperadora, ErrorDetail> create(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de empresa es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedRuc = normalize(ruc);
        if (normalizedRuc == null || !RUC_PATTERN.matcher(normalizedRuc).matches()) {
            return invalid("ruc", "El RUC debe tener 11 dígitos e iniciar con 10 o 20.");
        }

        var normalizedRazonSocial = normalizeSpaces(razonSocial);
        if (normalizedRazonSocial == null || normalizedRazonSocial.length() < 2
                || normalizedRazonSocial.length() > 300) {
            return invalid("razonSocial", "La razón social debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreComercial = normalizeSpaces(nombreComercial);
        if (normalizedNombreComercial != null && normalizedNombreComercial.length() > 300) {
            return invalid("nombreComercial", "El nombre comercial no debe exceder 300 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeoFiscal);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeoFiscal", "El ubigeo fiscal debe tener 6 dígitos.");
        }

        var normalizedMoneda = normalize(monedaFuncional);
        if (normalizedMoneda == null || normalizedMoneda.length() != 3) {
            return invalid("monedaFuncional", "La moneda funcional debe tener 3 caracteres (ISO 4217).");
        }

        var normalizedZonaHoraria = normalize(zonaHoraria);
        if (normalizedZonaHoraria == null || normalizedZonaHoraria.isEmpty()) {
            return invalid("zonaHoraria", "La zona horaria es obligatoria.");
        }

        return Result.success(new EmpresaOperadora(
                id, tenantId, normalizedRuc, normalizedRazonSocial, normalizedNombreComercial,
                normalizeSpaces(direccionFiscal), normalizedUbigeo, normalize(telefono),
                normalize(email), normalize(sitioWeb), normalizedMoneda, normalizedZonaHoraria,
                permiteVentaOnline, EstadoEmpresaOperadora.ACTIVO, createdAt, null));
    }

    public static EmpresaOperadora restore(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, EstadoEmpresaOperadora estado, Instant createdAt, Instant updatedAt) {
        return new EmpresaOperadora(
                id, tenantId, ruc, razonSocial, nombreComercial, direccionFiscal, ubigeoFiscal,
                telefono, email, sitioWeb, monedaFuncional, zonaHoraria, permiteVentaOnline,
                estado, createdAt, updatedAt);
    }

    public Result<EmpresaOperadora, ErrorDetail> updateDetails(
            String razonSocial, String nombreComercial, String direccionFiscal, String ubigeoFiscal,
            String telefono, String email, String sitioWeb, String monedaFuncional,
            String zonaHoraria, boolean permiteVentaOnline, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

        var normalizedRazonSocial = normalizeSpaces(razonSocial);
        if (normalizedRazonSocial == null || normalizedRazonSocial.length() < 2
                || normalizedRazonSocial.length() > 300) {
            return invalid("razonSocial", "La razón social debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreComercial = normalizeSpaces(nombreComercial);
        if (normalizedNombreComercial != null && normalizedNombreComercial.length() > 300) {
            return invalid("nombreComercial", "El nombre comercial no debe exceder 300 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeoFiscal);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeoFiscal", "El ubigeo fiscal debe tener 6 dígitos.");
        }

        var normalizedMoneda = normalize(monedaFuncional);
        if (normalizedMoneda == null || normalizedMoneda.length() != 3) {
            return invalid("monedaFuncional", "La moneda funcional debe tener 3 caracteres (ISO 4217).");
        }

        var normalizedZonaHoraria = normalize(zonaHoraria);
        if (normalizedZonaHoraria == null || normalizedZonaHoraria.isEmpty()) {
            return invalid("zonaHoraria", "La zona horaria es obligatoria.");
        }

        return Result.success(new EmpresaOperadora(
                id, tenantId, ruc, normalizedRazonSocial, normalizedNombreComercial,
                normalizeSpaces(direccionFiscal), normalizedUbigeo, normalize(telefono),
                normalize(email), normalize(sitioWeb), normalizedMoneda, normalizedZonaHoraria,
                permiteVentaOnline, estado, createdAt, updatedAt));
    }

    public Result<EmpresaOperadora, ErrorDetail> cambiarEstado(
            EstadoEmpresaOperadora nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estado", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new EmpresaOperadora(
                id, tenantId, ruc, razonSocial, nombreComercial, direccionFiscal, ubigeoFiscal,
                telefono, email, sitioWeb, monedaFuncional, zonaHoraria, permiteVentaOnline,
                nuevoEstado, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_EMPRESA_INVALIDA", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public EmpresaOperadoraId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String ruc() { return ruc; }
    public String razonSocial() { return razonSocial; }
    public String nombreComercial() { return nombreComercial; }
    public String direccionFiscal() { return direccionFiscal; }
    public String ubigeoFiscal() { return ubigeoFiscal; }
    public String telefono() { return telefono; }
    public String email() { return email; }
    public String sitioWeb() { return sitioWeb; }
    public String monedaFuncional() { return monedaFuncional; }
    public String zonaHoraria() { return zonaHoraria; }
    public boolean permiteVentaOnline() { return permiteVentaOnline; }
    public EstadoEmpresaOperadora estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 10: Ejecutar el test y verificar que pasa con 100% cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.EmpresaOperadoraTest" jacocoTestReport`
Expected: PASS, todos los tests verdes. Revisar el reporte JaCoCo de `EmpresaOperadora.java` — debe llegar a 100% líneas/ramas. Si `RUC_PATTERN`/`UBIGEO_PATTERN` u otra rama no llega a 100%, agregar el caso de test faltante antes de continuar.

- [ ] **Step 11: Commit**

```bash
git add service-botica/modules/organizacion/build.gradle
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain
git rm service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/EmpresaOperadoraRegistrada.java
git rm service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/RegistrarEmpresaOperadoraCommand.java
git rm service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandler.java
git rm service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/EmpresaOperadoraRepository.java
git rm service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/RegistrarEmpresaOperadoraHandlerTest.java
git commit -m "feat(organizacion): reescribir agregado EmpresaOperadora alineado a la tabla real"
```

---

## Task 2: Dominio `Establecimiento`, `Almacen`, `TerminalPos`

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/EstablecimientoId.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/AlmacenId.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/valueobject/TerminalPosId.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/TipoEstablecimiento.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/PerfilOperacion.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/EstadoEstablecimiento.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/Establecimiento.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/TipoAlmacen.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/Almacen.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/EstadoTerminalPos.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain/model/TerminalPos.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/EstablecimientoTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/AlmacenTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/TerminalPosTest.java`

**Interfaces:**
- Consumes: `TenantId`, `EmpresaOperadoraId` (Task 1).
- Produces: `EstablecimientoId`, `AlmacenId`, `TerminalPosId` (VOs `record XId(UUID value)`).
- Produces: `Establecimiento` con factory `create(EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo, String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo, String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo, String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email, boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery, PerfilOperacion perfilOperacion, String zonaHoraria, Instant createdAt)` → `Result<Establecimiento, ErrorDetail>`; `restore(...)` (+ `EstadoEstablecimiento estadoOperativo`, `Instant updatedAt`); `updateDetails(...)` (todos los campos excepto `empresaId`/`codigo`); `cambiarEstadoOperativo(EstadoEstablecimiento, Instant)`.
- Produces: `TipoEstablecimiento` enum `{BOTICA}`; `PerfilOperacion` enum `{ONLINE, STORE_EDGE}`; `EstadoEstablecimiento` enum `{ACTIVO, SUSPENDIDO, CLAUSURADO, REMODELACION}`.
- Produces: `Almacen` con factory `create(AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo, String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento, boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura, BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, Instant createdAt)` → `Result<Almacen, ErrorDetail>`; `restore(...)` (+ `boolean activo`, `Instant updatedAt`); `updateDetails(...)`; `activar(Instant)`; `desactivar(Instant)`.
- Produces: `TipoAlmacen` enum `{VENTA, GENERAL, CUARENTENA, REFRIGERADO, PSICOTROPICO, MERMA}`.
- Produces: `TerminalPos` con factory `create(TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo, String nombre, String serieBoletaDefecto, String serieFacturaDefecto, String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo, boolean storeEdgeHabilitado, Instant createdAt)` → `Result<TerminalPos, ErrorDetail>`; `restore(...)` (+ `EstadoTerminalPos estado`, `Instant updatedAt`); `updateDetails(...)`; `cambiarEstado(EstadoTerminalPos, Instant)`.
- Produces: `EstadoTerminalPos` enum `{ACTIVO, BLOQUEADO, MANTENIMIENTO}`.
- Códigos de error: `"ORG_ESTABLECIMIENTO_INVALIDO"`, `"ORG_ALMACEN_INVALIDO"`, `"ORG_TERMINAL_INVALIDO"`.

- [ ] **Step 1: Crear los 3 VOs de identidad**

`domain/valueobject/EstablecimientoId.java`:
```java
package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record EstablecimientoId(UUID value) {
    public EstablecimientoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

`domain/valueobject/AlmacenId.java`:
```java
package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record AlmacenId(UUID value) {
    public AlmacenId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

`domain/valueobject/TerminalPosId.java`:
```java
package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TerminalPosId(UUID value) {
    public TerminalPosId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [ ] **Step 2: Crear los enums de `Establecimiento`**

`domain/model/TipoEstablecimiento.java`:
```java
package com.softprimesolutions.organizacion.domain.model;

public enum TipoEstablecimiento {
    BOTICA
}
```

`domain/model/PerfilOperacion.java`:
```java
package com.softprimesolutions.organizacion.domain.model;

public enum PerfilOperacion {
    ONLINE,
    STORE_EDGE
}
```

`domain/model/EstadoEstablecimiento.java`:
```java
package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEstablecimiento {
    ACTIVO,
    SUSPENDIDO,
    CLAUSURADO,
    REMODELACION
}
```

- [ ] **Step 3: Escribir el test que falla para `Establecimiento`**

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/EstablecimientoTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstablecimientoTest {

    private static final EstablecimientoId ID = new EstablecimientoId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EmpresaOperadoraId EMPRESA_ID = new EmpresaOperadoraId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    private static Result_ create() {
        return null;
    }

    @Test
    void createsWithValidData() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", "DIG-001", "Av. Principal 100", "150101", "Cerca al parque",
                new BigDecimal("-12.0464000"), new BigDecimal("-77.0428000"), "014445566",
                "sede@boticas.pe", true, true, false, PerfilOperacion.ONLINE, "America/Lima", NOW);

        assertThat(result.isSuccess()).isTrue();
        var establecimiento = result.fold(e -> e, error -> { throw new AssertionError(error.message()); });
        assertThat(establecimiento.id()).isEqualTo(ID);
        assertThat(establecimiento.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(establecimiento.codigo()).isEqualTo("EST-01");
        assertThat(establecimiento.tipoEstablecimiento()).isEqualTo(TipoEstablecimiento.BOTICA);
        assertThat(establecimiento.codigoAnexoSunat()).isEqualTo("0000");
        assertThat(establecimiento.esPrincipal()).isTrue();
        assertThat(establecimiento.estadoOperativo()).isEqualTo(EstadoEstablecimiento.ACTIVO);
        assertThat(establecimiento.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-02", "Sede Norte", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.STORE_EDGE, "America/Lima", NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = Establecimiento.create(
                null, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = Establecimiento.create(
                ID, null, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEmpresaId() {
        var result = Establecimiento.create(
                ID, TENANT_ID, null, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, " ", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(e -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsCodigoTooLong() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "E".repeat(41), "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooShort() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "A", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsInvalidCodigoAnexoSunat() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsCodigoDigemidTooLong() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", "D".repeat(11), null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsInvalidUbigeo() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, "15010", null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTipoEstablecimiento() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", null,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullPerfilOperacion() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, null, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var establecimiento = Establecimiento.restore(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima",
                EstadoEstablecimiento.CLAUSURADO, NOW, NOW.plusSeconds(60));

        assertThat(establecimiento.estadoOperativo()).isEqualTo(EstadoEstablecimiento.CLAUSURADO);
        assertThat(establecimiento.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetailsKeepingEmpresaIdAndCodigo() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var updated = establecimiento.updateDetails(
                        "Sede Central Remodelada", TipoEstablecimiento.BOTICA, null, "0000", "DIG-002",
                        "Nueva direccion", "150102", null, null, null, null, "sede2@boticas.pe",
                        true, true, true, PerfilOperacion.STORE_EDGE, "America/Lima", NOW.plusSeconds(120))
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(updated.codigo()).isEqualTo("EST-01");
        assertThat(updated.nombre()).isEqualTo("Sede Central Remodelada");
        assertThat(updated.perfilOperacion()).isEqualTo(PerfilOperacion.STORE_EDGE);
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void changesEstadoOperativoWithoutErasingHistory() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(
                EstadoEstablecimiento.CLAUSURADO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(e -> {
            assertThat(e.estadoOperativo()).isEqualTo(EstadoEstablecimiento.CLAUSURADO);
            assertThat(e.codigo()).isEqualTo("EST-01");
            assertThat(e.createdAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(EstadoEstablecimiento.CLAUSURADO, null);

        assertThat(result.isFailure()).isTrue();
    }
}
```

Eliminar el método auxiliar `create()`/`Result_` sin usar que quedó de plantilla (no debe compilar con él presente) — es un placeholder de copia, bórralo antes de guardar el archivo final.

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.EstablecimientoTest"`
Expected: FAIL (compilation error)

- [ ] **Step 4: Implementar `Establecimiento`**

```java
package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/** Sede física (botica) donde opera una empresa operadora. */
public final class Establecimiento extends AggregateRoot {

    private static final Pattern ANEXO_SUNAT_PATTERN = Pattern.compile("[0-9]{4}");
    private static final Pattern UBIGEO_PATTERN = Pattern.compile("[0-9]{6}");

    private final EstablecimientoId id;
    private final TenantId tenantId;
    private final EmpresaOperadoraId empresaId;
    private final String codigo;
    private final String nombre;
    private final TipoEstablecimiento tipoEstablecimiento;
    private final String categoriaRegulatoriaCodigo;
    private final String codigoAnexoSunat;
    private final String codigoDigemid;
    private final String direccion;
    private final String ubigeo;
    private final String referencia;
    private final BigDecimal latitud;
    private final BigDecimal longitud;
    private final String telefono;
    private final String email;
    private final boolean esPrincipal;
    private final boolean permiteVentaOnline;
    private final boolean permiteDelivery;
    private final PerfilOperacion perfilOperacion;
    private final String zonaHoraria;
    private final EstadoEstablecimiento estadoOperativo;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Establecimiento(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, EstadoEstablecimiento estadoOperativo,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoEstablecimiento = tipoEstablecimiento;
        this.categoriaRegulatoriaCodigo = categoriaRegulatoriaCodigo;
        this.codigoAnexoSunat = codigoAnexoSunat;
        this.codigoDigemid = codigoDigemid;
        this.direccion = direccion;
        this.ubigeo = ubigeo;
        this.referencia = referencia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.telefono = telefono;
        this.email = email;
        this.esPrincipal = esPrincipal;
        this.permiteVentaOnline = permiteVentaOnline;
        this.permiteDelivery = permiteDelivery;
        this.perfilOperacion = perfilOperacion;
        this.zonaHoraria = zonaHoraria;
        this.estadoOperativo = estadoOperativo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Establecimiento, ErrorDetail> create(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del establecimiento es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (empresaId == null) return invalid("empresaId", "La empresa operadora es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");
        if (tipoEstablecimiento == null) {
            return invalid("tipoEstablecimiento", "El tipo de establecimiento es obligatorio.");
        }
        if (perfilOperacion == null) {
            return invalid("perfilOperacion", "El perfil de operación es obligatorio.");
        }

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.isEmpty() || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var normalizedAnexo = normalize(codigoAnexoSunat);
        if (normalizedAnexo == null || !ANEXO_SUNAT_PATTERN.matcher(normalizedAnexo).matches()) {
            return invalid("codigoAnexoSunat", "El código de anexo SUNAT debe tener 4 dígitos.");
        }

        var normalizedDigemid = normalize(codigoDigemid);
        if (normalizedDigemid != null && normalizedDigemid.length() > 10) {
            return invalid("codigoDigemid", "El código DIGEMID no debe exceder 10 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeo);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeo", "El ubigeo debe tener 6 dígitos.");
        }

        return Result.success(new Establecimiento(
                id, tenantId, empresaId, normalizedCodigo, normalizedNombre, tipoEstablecimiento,
                normalize(categoriaRegulatoriaCodigo), normalizedAnexo, normalizedDigemid,
                normalizeSpaces(direccion), normalizedUbigeo, normalizeSpaces(referencia), latitud,
                longitud, normalize(telefono), normalize(email), esPrincipal, permiteVentaOnline,
                permiteDelivery, perfilOperacion, normalize(zonaHoraria), EstadoEstablecimiento.ACTIVO,
                createdAt, null));
    }

    public static Establecimiento restore(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, EstadoEstablecimiento estadoOperativo,
            Instant createdAt, Instant updatedAt) {
        return new Establecimiento(
                id, tenantId, empresaId, codigo, nombre, tipoEstablecimiento, categoriaRegulatoriaCodigo,
                codigoAnexoSunat, codigoDigemid, direccion, ubigeo, referencia, latitud, longitud,
                telefono, email, esPrincipal, permiteVentaOnline, permiteDelivery, perfilOperacion,
                zonaHoraria, estadoOperativo, createdAt, updatedAt);
    }

    public Result<Establecimiento, ErrorDetail> updateDetails(
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        if (tipoEstablecimiento == null) {
            return invalid("tipoEstablecimiento", "El tipo de establecimiento es obligatorio.");
        }
        if (perfilOperacion == null) {
            return invalid("perfilOperacion", "El perfil de operación es obligatorio.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var normalizedAnexo = normalize(codigoAnexoSunat);
        if (normalizedAnexo == null || !ANEXO_SUNAT_PATTERN.matcher(normalizedAnexo).matches()) {
            return invalid("codigoAnexoSunat", "El código de anexo SUNAT debe tener 4 dígitos.");
        }

        var normalizedDigemid = normalize(codigoDigemid);
        if (normalizedDigemid != null && normalizedDigemid.length() > 10) {
            return invalid("codigoDigemid", "El código DIGEMID no debe exceder 10 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeo);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeo", "El ubigeo debe tener 6 dígitos.");
        }

        return Result.success(new Establecimiento(
                id, tenantId, empresaId, codigo, normalizedNombre, tipoEstablecimiento,
                normalize(categoriaRegulatoriaCodigo), normalizedAnexo, normalizedDigemid,
                normalizeSpaces(direccion), normalizedUbigeo, normalizeSpaces(referencia), latitud,
                longitud, normalize(telefono), normalize(email), esPrincipal, permiteVentaOnline,
                permiteDelivery, perfilOperacion, normalize(zonaHoraria), estadoOperativo,
                createdAt, updatedAt));
    }

    public Result<Establecimiento, ErrorDetail> cambiarEstadoOperativo(
            EstadoEstablecimiento nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estadoOperativo", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Establecimiento(
                id, tenantId, empresaId, codigo, nombre, tipoEstablecimiento, categoriaRegulatoriaCodigo,
                codigoAnexoSunat, codigoDigemid, direccion, ubigeo, referencia, latitud, longitud,
                telefono, email, esPrincipal, permiteVentaOnline, permiteDelivery, perfilOperacion,
                zonaHoraria, nuevoEstado, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_ESTABLECIMIENTO_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public EstablecimientoId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EmpresaOperadoraId empresaId() { return empresaId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public TipoEstablecimiento tipoEstablecimiento() { return tipoEstablecimiento; }
    public String categoriaRegulatoriaCodigo() { return categoriaRegulatoriaCodigo; }
    public String codigoAnexoSunat() { return codigoAnexoSunat; }
    public String codigoDigemid() { return codigoDigemid; }
    public String direccion() { return direccion; }
    public String ubigeo() { return ubigeo; }
    public String referencia() { return referencia; }
    public BigDecimal latitud() { return latitud; }
    public BigDecimal longitud() { return longitud; }
    public String telefono() { return telefono; }
    public String email() { return email; }
    public boolean esPrincipal() { return esPrincipal; }
    public boolean permiteVentaOnline() { return permiteVentaOnline; }
    public boolean permiteDelivery() { return permiteDelivery; }
    public PerfilOperacion perfilOperacion() { return perfilOperacion; }
    public String zonaHoraria() { return zonaHoraria; }
    public EstadoEstablecimiento estadoOperativo() { return estadoOperativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 5: Ejecutar el test de `Establecimiento` y verificar 100% cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.EstablecimientoTest" jacocoTestReport`
Expected: PASS. Revisar cobertura de `Establecimiento.java`; agregar casos si falta alguna rama (p.ej. `categoriaRegulatoriaCodigo` no nulo, `latitud`/`longitud` no nulos en el camino feliz ya cubiertos por `createsWithValidData`).

- [ ] **Step 6: Crear `TipoAlmacen` y escribir el test que falla para `Almacen`**

`domain/model/TipoAlmacen.java`:
```java
package com.softprimesolutions.organizacion.domain.model;

public enum TipoAlmacen {
    VENTA,
    GENERAL,
    CUARENTENA,
    REFRIGERADO,
    PSICOTROPICO,
    MERMA
}
```

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/AlmacenTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlmacenTest {

    private static final AlmacenId ID = new AlmacenId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EstablecimientoId ESTABLECIMIENTO_ID = new EstablecimientoId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.REFRIGERADO,
                true, true, true, true, true, new BigDecimal("2.0"), new BigDecimal("8.0"), NOW);

        assertThat(result.isSuccess()).isTrue();
        var almacen = result.fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(almacen.id()).isEqualTo(ID);
        assertThat(almacen.establecimientoId()).isEqualTo(ESTABLECIMIENTO_ID);
        assertThat(almacen.tipo()).isEqualTo(TipoAlmacen.REFRIGERADO);
        assertThat(almacen.controlTemperatura()).isTrue();
        assertThat(almacen.temperaturaMinC()).isEqualTo(new BigDecimal("2.0"));
        assertThat(almacen.activo()).isTrue();
        assertThat(almacen.updatedAt()).isNull();
    }

    @Test
    void createsWithoutTemperatureControl() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-02", "Almacén general", TipoAlmacen.GENERAL,
                true, false, true, true, false, null, null, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = Almacen.create(
                null, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = Almacen.create(
                ID, null, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEstablecimientoId() {
        var result = Almacen.create(
                ID, TENANT_ID, null, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, " ", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(a -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_ALMACEN_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsBlankNombre() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", " ", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTipo() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", null,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var almacen = Almacen.restore(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, false, NOW, NOW.plusSeconds(60));

        assertThat(almacen.activo()).isFalse();
        assertThat(almacen.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetails() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var updated = almacen.updateDetails(
                        "Almacén central renovado", TipoAlmacen.CUARENTENA, false, false, false, false,
                        true, new BigDecimal("2.0"), new BigDecimal("8.0"), NOW.plusSeconds(120))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.nombre()).isEqualTo("Almacén central renovado");
        assertThat(updated.tipo()).isEqualTo(TipoAlmacen.CUARENTENA);
        assertThat(updated.controlTemperatura()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                "Almacén", TipoAlmacen.VENTA, true, true, true, true, false, null, null, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void deactivatesAndReactivates() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var deactivated = almacen.desactivar(NOW.plusSeconds(60))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(deactivated.activo()).isFalse();

        var reactivated = deactivated.activar(NOW.plusSeconds(120))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(reactivated.activo()).isTrue();
    }

    @Test
    void rejectsActivarWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(almacen.activar(null).isFailure()).isTrue();
    }

    @Test
    void rejectsDesactivarWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(almacen.desactivar(null).isFailure()).isTrue();
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.AlmacenTest"`
Expected: FAIL (compilation error)

- [ ] **Step 7: Implementar `Almacen`**

```java
package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Ubicación de almacenamiento física dentro de un establecimiento. */
public final class Almacen extends AggregateRoot {

    private final AlmacenId id;
    private final TenantId tenantId;
    private final EstablecimientoId establecimientoId;
    private final String codigo;
    private final String nombre;
    private final TipoAlmacen tipo;
    private final boolean permiteLotes;
    private final boolean permiteVencimiento;
    private final boolean permiteVenta;
    private final boolean permiteDespacho;
    private final boolean controlTemperatura;
    private final BigDecimal temperaturaMinC;
    private final BigDecimal temperaturaMaxC;
    private final boolean activo;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Almacen(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, boolean activo,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.permiteLotes = permiteLotes;
        this.permiteVencimiento = permiteVencimiento;
        this.permiteVenta = permiteVenta;
        this.permiteDespacho = permiteDespacho;
        this.controlTemperatura = controlTemperatura;
        this.temperaturaMinC = temperaturaMinC;
        this.temperaturaMaxC = temperaturaMaxC;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Almacen, ErrorDetail> create(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del almacén es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (establecimientoId == null) return invalid("establecimientoId", "El establecimiento es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");
        if (tipo == null) return invalid("tipo", "El tipo de almacén es obligatorio.");

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.isEmpty() || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new Almacen(
                id, tenantId, establecimientoId, normalizedCodigo, normalizedNombre, tipo,
                permiteLotes, permiteVencimiento, permiteVenta, permiteDespacho, controlTemperatura,
                temperaturaMinC, temperaturaMaxC, true, createdAt, null));
    }

    public static Almacen restore(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, boolean activo,
            Instant createdAt, Instant updatedAt) {
        return new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                activo, createdAt, updatedAt);
    }

    public Result<Almacen, ErrorDetail> updateDetails(
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        if (tipo == null) return invalid("tipo", "El tipo de almacén es obligatorio.");

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, normalizedNombre, tipo, permiteLotes,
                permiteVencimiento, permiteVenta, permiteDespacho, controlTemperatura,
                temperaturaMinC, temperaturaMaxC, activo, createdAt, updatedAt));
    }

    public Result<Almacen, ErrorDetail> activar(Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                true, createdAt, updatedAt));
    }

    public Result<Almacen, ErrorDetail> desactivar(Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                false, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_ALMACEN_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public AlmacenId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EstablecimientoId establecimientoId() { return establecimientoId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public TipoAlmacen tipo() { return tipo; }
    public boolean permiteLotes() { return permiteLotes; }
    public boolean permiteVencimiento() { return permiteVencimiento; }
    public boolean permiteVenta() { return permiteVenta; }
    public boolean permiteDespacho() { return permiteDespacho; }
    public boolean controlTemperatura() { return controlTemperatura; }
    public BigDecimal temperaturaMinC() { return temperaturaMinC; }
    public BigDecimal temperaturaMaxC() { return temperaturaMaxC; }
    public boolean activo() { return activo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 8: Ejecutar el test de `Almacen` y verificar 100% cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.AlmacenTest" jacocoTestReport`
Expected: PASS con 100% cobertura de `Almacen.java`.

- [ ] **Step 9: Crear `EstadoTerminalPos` y escribir el test que falla para `TerminalPos`**

`domain/model/EstadoTerminalPos.java`:
```java
package com.softprimesolutions.organizacion.domain.model;

public enum EstadoTerminalPos {
    ACTIVO,
    BLOQUEADO,
    MANTENIMIENTO
}
```

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain/model/TerminalPosTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalPosTest {

    private static final TerminalPosId ID = new TerminalPosId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EstablecimientoId ESTABLECIMIENTO_ID = new EstablecimientoId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", "B001", "F001",
                "SN-12345", "pos-01", "192.168.1.10", "PR-01", true, NOW);

        assertThat(result.isSuccess()).isTrue();
        var terminal = result.fold(t -> t, error -> { throw new AssertionError(error.message()); });
        assertThat(terminal.id()).isEqualTo(ID);
        assertThat(terminal.establecimientoId()).isEqualTo(ESTABLECIMIENTO_ID);
        assertThat(terminal.codigo()).isEqualTo("CR-01");
        assertThat(terminal.storeEdgeHabilitado()).isTrue();
        assertThat(terminal.estado()).isEqualTo(EstadoTerminalPos.ACTIVO);
        assertThat(terminal.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-02", "Caja 2", null, null,
                null, null, null, null, false, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = TerminalPos.create(
                null, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = TerminalPos.create(
                ID, null, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEstablecimientoId() {
        var result = TerminalPos.create(
                ID, TENANT_ID, null, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, " ", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(t -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsBlankNombre() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", " ", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var terminal = TerminalPos.restore(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, EstadoTerminalPos.MANTENIMIENTO, NOW, NOW.plusSeconds(60));

        assertThat(terminal.estado()).isEqualTo(EstadoTerminalPos.MANTENIMIENTO);
        assertThat(terminal.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetails() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var updated = terminal.updateDetails(
                        "Caja principal", "B002", "F002", "SN-99999", "pos-02", "192.168.1.20",
                        "PR-02", true, NOW.plusSeconds(120))
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.nombre()).isEqualTo("Caja principal");
        assertThat(updated.storeEdgeHabilitado()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithBlankNombre() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                " ", null, null, null, null, null, null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                "Caja 1", null, null, null, null, null, null, false, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void changesEstado() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.cambiarEstado(EstadoTerminalPos.BLOQUEADO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(t -> {
            assertThat(t.estado()).isEqualTo(EstadoTerminalPos.BLOQUEADO);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(terminal.cambiarEstado(null, NOW.plusSeconds(60)).isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(terminal.cambiarEstado(EstadoTerminalPos.BLOQUEADO, null).isFailure()).isTrue();
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.domain.model.TerminalPosTest"`
Expected: FAIL (compilation error)

- [ ] **Step 10: Implementar `TerminalPos`**

```java
package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;

/** Terminal de punto de venta (caja) asociado a un establecimiento. */
public final class TerminalPos extends AggregateRoot {

    private final TerminalPosId id;
    private final TenantId tenantId;
    private final EstablecimientoId establecimientoId;
    private final String codigo;
    private final String nombre;
    private final String serieBoletaDefecto;
    private final String serieFacturaDefecto;
    private final String numeroSerieEquipo;
    private final String hostname;
    private final String ipEquipo;
    private final String impresoraCodigo;
    private final boolean storeEdgeHabilitado;
    private final EstadoTerminalPos estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private TerminalPos(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, EstadoTerminalPos estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.serieBoletaDefecto = serieBoletaDefecto;
        this.serieFacturaDefecto = serieFacturaDefecto;
        this.numeroSerieEquipo = numeroSerieEquipo;
        this.hostname = hostname;
        this.ipEquipo = ipEquipo;
        this.impresoraCodigo = impresoraCodigo;
        this.storeEdgeHabilitado = storeEdgeHabilitado;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<TerminalPos, ErrorDetail> create(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del terminal es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (establecimientoId == null) return invalid("establecimientoId", "El establecimiento es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.isEmpty() || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, normalizedCodigo, normalizedNombre,
                normalize(serieBoletaDefecto), normalize(serieFacturaDefecto), normalize(numeroSerieEquipo),
                normalize(hostname), normalize(ipEquipo), normalize(impresoraCodigo), storeEdgeHabilitado,
                EstadoTerminalPos.ACTIVO, createdAt, null));
    }

    public static TerminalPos restore(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, EstadoTerminalPos estado, Instant createdAt, Instant updatedAt) {
        return new TerminalPos(
                id, tenantId, establecimientoId, codigo, nombre, serieBoletaDefecto, serieFacturaDefecto,
                numeroSerieEquipo, hostname, ipEquipo, impresoraCodigo, storeEdgeHabilitado, estado,
                createdAt, updatedAt);
    }

    public Result<TerminalPos, ErrorDetail> updateDetails(
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, codigo, normalizedNombre, normalize(serieBoletaDefecto),
                normalize(serieFacturaDefecto), normalize(numeroSerieEquipo), normalize(hostname),
                normalize(ipEquipo), normalize(impresoraCodigo), storeEdgeHabilitado, estado,
                createdAt, updatedAt));
    }

    public Result<TerminalPos, ErrorDetail> cambiarEstado(EstadoTerminalPos nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estado", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, codigo, nombre, serieBoletaDefecto, serieFacturaDefecto,
                numeroSerieEquipo, hostname, ipEquipo, impresoraCodigo, storeEdgeHabilitado, nuevoEstado,
                createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_TERMINAL_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public TerminalPosId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EstablecimientoId establecimientoId() { return establecimientoId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String serieBoletaDefecto() { return serieBoletaDefecto; }
    public String serieFacturaDefecto() { return serieFacturaDefecto; }
    public String numeroSerieEquipo() { return numeroSerieEquipo; }
    public String hostname() { return hostname; }
    public String ipEquipo() { return ipEquipo; }
    public String impresoraCodigo() { return impresoraCodigo; }
    public boolean storeEdgeHabilitado() { return storeEdgeHabilitado; }
    public EstadoTerminalPos estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 11: Ejecutar todos los tests de dominio de Task 2 y verificar 100% cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test jacocoTestReport`
Expected: PASS, todos verdes. Revisar el reporte JaCoCo — `Establecimiento.java`, `Almacen.java`, `TerminalPos.java` deben llegar a 100% líneas/ramas.

- [ ] **Step 12: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/domain
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/domain
git commit -m "feat(organizacion): agregar dominio de Establecimiento, Almacen y TerminalPos"
```

---

## Task 3: Capa de aplicación — DTOs, puertos de salida y resultados paginados

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/PaginaResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/EmpresaOperadoraResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/EstablecimientoResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/AlmacenResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/TerminalPosResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/NodoResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/EstablecimientoNodoResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/EmpresaNodoResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/result/EstructuraCorporativaResult.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CrearEmpresaOperadoraCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/ActualizarEmpresaOperadoraCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CrearEstablecimientoCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/ActualizarEstablecimientoCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CrearAlmacenCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/ActualizarAlmacenCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CrearTerminalPosCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/ActualizarTerminalPosCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ListarEmpresasQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ObtenerEmpresaQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ListarEstablecimientosQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ObtenerEstablecimientoQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ListarAlmacenesQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ObtenerAlmacenQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ListarTerminalesQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ObtenerTerminalQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/query/ObtenerEstructuraCorporativaQuery.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/out/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/out/OrganizacionReadPort.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/out/OrganizacionWritePort.java`

**Interfaces:**
- Consumes: nada de fuera del módulo salvo `shared-kernel`/`shared-application`.
- Produces: todos los DTOs de comando/query/resultado y los dos puertos de salida (`OrganizacionReadPort`, `OrganizacionWritePort`), consumidos por Task 4 (handlers), Task 5 (adapters de infraestructura) y Task 6 (controllers vía mapper).

Esta tarea es puramente de tipos (records + interfaces), sin lógica — no requiere tests propios (no hay comportamiento que probar; los DTOs son records simples validados solo por su firma de tipos, igual que en `security`, donde `CrearRolCommand`, `RolResult`, `PaginaResult` no tienen test dedicado).

- [ ] **Step 1: Crear los `package-info.java`**

```java
// application/package-info.java
package com.softprimesolutions.organizacion.application;
```
```java
// application/dto/package-info.java
package com.softprimesolutions.organizacion.application.dto;
```
```java
// application/dto/result/package-info.java
package com.softprimesolutions.organizacion.application.dto.result;
```
```java
// application/dto/command/package-info.java
package com.softprimesolutions.organizacion.application.dto.command;
```
```java
// application/dto/query/package-info.java
package com.softprimesolutions.organizacion.application.dto.query;
```
```java
// application/port/package-info.java
package com.softprimesolutions.organizacion.application.port;
```
```java
// application/port/out/package-info.java
package com.softprimesolutions.organizacion.application.port.out;
```

- [ ] **Step 2: Crear `PaginaResult`**

```java
package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;

public record PaginaResult<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResult {
        items = List.copyOf(items);
    }
}
```

- [ ] **Step 3: Crear los 4 `{X}Result`**

```java
// application/dto/result/EmpresaOperadoraResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record EmpresaOperadoraResult(
        UUID id,
        UUID tenantId,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// application/dto/result/EstablecimientoResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstablecimientoResult(
        UUID id,
        UUID tenantId,
        UUID empresaId,
        String codigo,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria,
        String estadoOperativo,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// application/dto/result/AlmacenResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlmacenResult(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// application/dto/result/TerminalPosResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record TerminalPosResult(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 4: Crear los DTOs del árbol de estructura corporativa**

```java
// application/dto/result/NodoResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.util.UUID;

public record NodoResult(UUID id, String code, String name, String status) {
}
```

```java
// application/dto/result/EstablecimientoNodoResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;
import java.util.UUID;

public record EstablecimientoNodoResult(
        UUID id,
        String code,
        String name,
        String status,
        String timeZone,
        List<NodoResult> warehouses,
        List<NodoResult> cashRegisters) {

    public EstablecimientoNodoResult {
        warehouses = List.copyOf(warehouses);
        cashRegisters = List.copyOf(cashRegisters);
    }
}
```

```java
// application/dto/result/EmpresaNodoResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;
import java.util.UUID;

public record EmpresaNodoResult(
        UUID id,
        String legalName,
        String tradeName,
        String status,
        List<EstablecimientoNodoResult> establishments) {

    public EmpresaNodoResult {
        establishments = List.copyOf(establishments);
    }
}
```

```java
// application/dto/result/EstructuraCorporativaResult.java
package com.softprimesolutions.organizacion.application.dto.result;

import java.time.Instant;
import java.util.List;

public record EstructuraCorporativaResult(Instant asOf, List<EmpresaNodoResult> companies) {

    public EstructuraCorporativaResult {
        companies = List.copyOf(companies);
    }
}
```

- [ ] **Step 5: Crear los comandos (Crear/Actualizar × 4 agregados)**

```java
// application/dto/command/CrearEmpresaOperadoraCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CrearEmpresaOperadoraCommand(
        UUID tenantId,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline) {
}
```

```java
// application/dto/command/ActualizarEmpresaOperadoraCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record ActualizarEmpresaOperadoraCommand(
        UUID empresaId,
        UUID tenantId,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline) {
}
```

```java
// application/dto/command/CrearEstablecimientoCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearEstablecimientoCommand(
        UUID tenantId,
        UUID empresaId,
        String codigo,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria) {
}
```

```java
// application/dto/command/ActualizarEstablecimientoCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarEstablecimientoCommand(
        UUID establecimientoId,
        UUID tenantId,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria) {
}
```

```java
// application/dto/command/CrearAlmacenCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearAlmacenCommand(
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC) {
}
```

```java
// application/dto/command/ActualizarAlmacenCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarAlmacenCommand(
        UUID almacenId,
        UUID tenantId,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo) {
}
```

```java
// application/dto/command/CrearTerminalPosCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CrearTerminalPosCommand(
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado) {
}
```

```java
// application/dto/command/ActualizarTerminalPosCommand.java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record ActualizarTerminalPosCommand(
        UUID terminalId,
        UUID tenantId,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado) {
}
```

- [ ] **Step 6: Crear las queries (Listar/Obtener × 4 agregados + estructura corporativa)**

```java
// application/dto/query/ListarEmpresasQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarEmpresasQuery(UUID tenantId, String search, int page, int size) {
}
```

```java
// application/dto/query/ObtenerEmpresaQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerEmpresaQuery(UUID tenantId, UUID empresaId) {
}
```

```java
// application/dto/query/ListarEstablecimientosQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarEstablecimientosQuery(UUID tenantId, UUID empresaId, String search, int page, int size) {
}
```

```java
// application/dto/query/ObtenerEstablecimientoQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerEstablecimientoQuery(UUID tenantId, UUID establecimientoId) {
}
```

```java
// application/dto/query/ListarAlmacenesQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarAlmacenesQuery(UUID tenantId, UUID establecimientoId, String search, int page, int size) {
}
```

```java
// application/dto/query/ObtenerAlmacenQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerAlmacenQuery(UUID tenantId, UUID almacenId) {
}
```

```java
// application/dto/query/ListarTerminalesQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarTerminalesQuery(UUID tenantId, UUID establecimientoId, String search, int page, int size) {
}
```

```java
// application/dto/query/ObtenerTerminalQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerTerminalQuery(UUID tenantId, UUID terminalId) {
}
```

```java
// application/dto/query/ObtenerEstructuraCorporativaQuery.java
package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerEstructuraCorporativaQuery(UUID tenantId) {
}
```

- [ ] **Step 7: Crear `OrganizacionReadPort`**

```java
package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import java.util.Optional;
import java.util.UUID;

public interface OrganizacionReadPort {

    PaginaResult<EmpresaOperadoraResult> findEmpresas(UUID tenantId, String search, int page, int size);

    Optional<EmpresaOperadoraResult> findEmpresaById(UUID tenantId, UUID empresaId);

    PaginaResult<EstablecimientoResult> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int page, int size);

    Optional<EstablecimientoResult> findEstablecimientoById(UUID tenantId, UUID establecimientoId);

    PaginaResult<AlmacenResult> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int page, int size);

    Optional<AlmacenResult> findAlmacenById(UUID tenantId, UUID almacenId);

    PaginaResult<TerminalPosResult> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int page, int size);

    Optional<TerminalPosResult> findTerminalById(UUID tenantId, UUID terminalId);

    EstructuraCorporativaResult findEstructuraCorporativa(UUID tenantId);
}
```

- [ ] **Step 8: Crear `OrganizacionWritePort`**

```java
package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import java.util.UUID;

public interface OrganizacionWritePort {

    SaveEmpresaOutcome save(EmpresaOperadora empresa);

    SaveEstablecimientoOutcome save(Establecimiento establecimiento);

    SaveAlmacenOutcome save(Almacen almacen);

    SaveTerminalOutcome save(TerminalPos terminal);

    boolean tenantExists(UUID tenantId);

    boolean empresaBelongsToTenant(UUID empresaId, UUID tenantId);

    boolean establecimientoBelongsToTenant(UUID establecimientoId, UUID tenantId);

    enum SaveEmpresaOutcome { CREATED, UPDATED, TENANT_NOT_FOUND, DUPLICATE_RUC }

    enum SaveEstablecimientoOutcome { CREATED, UPDATED, EMPRESA_NOT_FOUND, DUPLICATE_CODIGO, DUPLICATE_DIGEMID }

    enum SaveAlmacenOutcome { CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, DUPLICATE_CODIGO }

    enum SaveTerminalOutcome { CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, DUPLICATE_CODIGO }
}
```

- [ ] **Step 9: Compilar el módulo para verificar que todos los tipos son consistentes**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application
git commit -m "feat(organizacion): agregar DTOs de aplicacion y puertos de salida"
```

---

## Task 4: Casos de uso y handlers — Empresa Operadora y Establecimiento

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CrearEmpresaOperadoraUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ActualizarEmpresaOperadoraUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ListarEmpresasUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ObtenerEmpresaUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CrearEstablecimientoUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ActualizarEstablecimientoUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ListarEstablecimientosUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ObtenerEstablecimientoUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CrearEmpresaOperadoraHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEmpresaOperadoraHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ListarEmpresasHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEmpresaHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CrearEstablecimientoHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEstablecimientoHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ListarEstablecimientosHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEstablecimientoHandler.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearEmpresaOperadoraHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEmpresaOperadoraHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ListarEmpresasHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEmpresaHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearEstablecimientoHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEstablecimientoHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ListarEstablecimientosHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEstablecimientoHandlerTest.java`

**Interfaces:**
- Consumes: `OrganizacionReadPort`, `OrganizacionWritePort`, DTOs de Task 3; `EmpresaOperadora`/`Establecimiento` de Tasks 1-2; `ClockPort`, `IdentifierGenerator`, `Result`, `StandardApplicationError`, `ErrorCategory`, `ApplicationError` de `shared-application`/`shared-kernel`.
- Produces: 8 casos de uso (`Crear/Actualizar/Listar/Obtener` × 2 agregados) + sus handlers, consumidos por Task 6 (`OrganizacionModuleConfiguration`) y Task 8 (controllers).
- Cada `UseCase` sigue la forma `@FunctionalInterface interface XUseCase { Result<Y, ApplicationError> execute(Z input); }`, igual que `CrearRolUseCase`.

- [ ] **Step 1: Crear `application/port/in/package-info.java`, `application/usecase/package-info.java`, `application/usecase/command/package-info.java`, `application/usecase/query/package-info.java`**

```java
// application/port/in/package-info.java
package com.softprimesolutions.organizacion.application.port.in;
```
```java
// application/usecase/package-info.java
package com.softprimesolutions.organizacion.application.usecase;
```
```java
// application/usecase/command/package-info.java
package com.softprimesolutions.organizacion.application.usecase.command;
```
```java
// application/usecase/query/package-info.java
package com.softprimesolutions.organizacion.application.usecase.query;
```

- [ ] **Step 2: Crear los 4 puertos de entrada de Empresa**

```java
// application/port/in/CrearEmpresaOperadoraUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearEmpresaOperadoraUseCase {
    Result<EmpresaOperadoraResult, ApplicationError> execute(CrearEmpresaOperadoraCommand command);
}
```

```java
// application/port/in/ActualizarEmpresaOperadoraUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarEmpresaOperadoraUseCase {
    Result<EmpresaOperadoraResult, ApplicationError> execute(ActualizarEmpresaOperadoraCommand command);
}
```

```java
// application/port/in/ListarEmpresasUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarEmpresasUseCase {
    Result<PaginaResult<EmpresaOperadoraResult>, ApplicationError> execute(ListarEmpresasQuery query);
}
```

```java
// application/port/in/ObtenerEmpresaUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerEmpresaUseCase {
    Result<EmpresaOperadoraResult, ApplicationError> execute(ObtenerEmpresaQuery query);
}
```

- [ ] **Step 3: Crear los 4 puertos de entrada de Establecimiento**

```java
// application/port/in/CrearEstablecimientoUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearEstablecimientoUseCase {
    Result<EstablecimientoResult, ApplicationError> execute(CrearEstablecimientoCommand command);
}
```

```java
// application/port/in/ActualizarEstablecimientoUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarEstablecimientoUseCase {
    Result<EstablecimientoResult, ApplicationError> execute(ActualizarEstablecimientoCommand command);
}
```

```java
// application/port/in/ListarEstablecimientosUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarEstablecimientosQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarEstablecimientosUseCase {
    Result<PaginaResult<EstablecimientoResult>, ApplicationError> execute(ListarEstablecimientosQuery query);
}
```

```java
// application/port/in/ObtenerEstablecimientoUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstablecimientoQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerEstablecimientoUseCase {
    Result<EstablecimientoResult, ApplicationError> execute(ObtenerEstablecimientoQuery query);
}
```

- [ ] **Step 4: Escribir el test que falla para `CrearEmpresaOperadoraHandler`**

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearEmpresaOperadoraHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearEmpresaOperadoraHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "11111111-1111-1111-1111-111111111111");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearEmpresaOperadoraHandler handler =
            new CrearEmpresaOperadoraHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();

    private CrearEmpresaOperadoraCommand validCommand() {
        return new CrearEmpresaOperadoraCommand(
                TENANT_ID, "20123456789", "Boticas SAC", "Boticas", null, null, null, null, null,
                "PEN", "America/Lima", false);
    }

    @Test
    void createsEmpresaWhenValid() {
        when(writePort.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.ruc()).isEqualTo("20123456789");
            assertThat(empresa.tenantId()).isEqualTo(TENANT_ID);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearEmpresaOperadoraCommand(
                TENANT_ID, "30123456789", "Boticas SAC", null, null, null, null, null, null,
                "PEN", "America/Lima", false);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenTenantMissing() {
        when(writePort.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.TENANT_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenRucDuplicated() {
        when(writePort.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.DUPLICATE_RUC);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.command.CrearEmpresaOperadoraHandlerTest"`
Expected: FAIL (compilation error)

- [ ] **Step 5: Implementar `CrearEmpresaOperadoraHandler`**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearEmpresaOperadoraHandler implements CrearEmpresaOperadoraUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearEmpresaOperadoraHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(CrearEmpresaOperadoraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var empresa = EmpresaOperadora.create(
                new EmpresaOperadoraId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.ruc(), command.razonSocial(), command.nombreComercial(),
                command.direccionFiscal(), command.ubigeoFiscal(), command.telefono(),
                command.email(), command.sitioWeb(), command.monedaFuncional(),
                command.zonaHoraria(), command.permiteVentaOnline(), clock.now());
        return empresa.fold(this::persist, this::validationFailure);
    }

    private Result<EmpresaOperadoraResult, ApplicationError> persist(EmpresaOperadora empresa) {
        var outcome = writePort.save(empresa);
        if (outcome == OrganizacionWritePort.SaveEmpresaOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveEmpresaOutcome.DUPLICATE_RUC) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_RUC_DUPLICADO", "Ya existe una empresa con el RUC indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(empresa));
    }

    private Result<EmpresaOperadoraResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

**Nota:** este handler referencia `OrganizacionApplicationMapper.toResult(EmpresaOperadora)`, que se crea en el Step 6 junto con los demás métodos del mapper necesarios para esta tarea.

- [ ] **Step 6: Crear `application/mapper/package-info.java` y `OrganizacionApplicationMapper` (versión inicial con Empresa)**

```java
// application/mapper/package-info.java
package com.softprimesolutions.organizacion.application.mapper;
```

```java
// application/mapper/OrganizacionApplicationMapper.java
package com.softprimesolutions.organizacion.application.mapper;

import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;

public final class OrganizacionApplicationMapper {

    private OrganizacionApplicationMapper() {
    }

    public static EmpresaOperadoraResult toResult(EmpresaOperadora empresa) {
        return new EmpresaOperadoraResult(
                empresa.id().value(), empresa.tenantId().value(), empresa.ruc(), empresa.razonSocial(),
                empresa.nombreComercial(), empresa.direccionFiscal(), empresa.ubigeoFiscal(),
                empresa.telefono(), empresa.email(), empresa.sitioWeb(), empresa.monedaFuncional(),
                empresa.zonaHoraria(), empresa.permiteVentaOnline(), empresa.estado().name(),
                empresa.createdAt(), empresa.updatedAt());
    }

    public static EstablecimientoResult toResult(Establecimiento establecimiento) {
        return new EstablecimientoResult(
                establecimiento.id().value(), establecimiento.tenantId().value(),
                establecimiento.empresaId().value(), establecimiento.codigo(), establecimiento.nombre(),
                establecimiento.tipoEstablecimiento().name(), establecimiento.categoriaRegulatoriaCodigo(),
                establecimiento.codigoAnexoSunat(), establecimiento.codigoDigemid(),
                establecimiento.direccion(), establecimiento.ubigeo(), establecimiento.referencia(),
                establecimiento.latitud(), establecimiento.longitud(), establecimiento.telefono(),
                establecimiento.email(), establecimiento.esPrincipal(), establecimiento.permiteVentaOnline(),
                establecimiento.permiteDelivery(), establecimiento.perfilOperacion().name(),
                establecimiento.zonaHoraria(), establecimiento.estadoOperativo().name(),
                establecimiento.createdAt(), establecimiento.updatedAt());
    }
}
```

(`toResult(Almacen)`/`toResult(TerminalPos)` se agregan en Task 5, cuando se implementan esos handlers — evita un mapper con métodos sin uso todavía en esta tarea.)

- [ ] **Step 7: Ejecutar el test de `CrearEmpresaOperadoraHandler` y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.command.CrearEmpresaOperadoraHandlerTest"`
Expected: PASS

- [ ] **Step 8: Escribir el test y luego implementar `ActualizarEmpresaOperadoraHandler`**

Test `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEmpresaOperadoraHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarEmpresaOperadoraHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final ActualizarEmpresaOperadoraHandler handler =
            new ActualizarEmpresaOperadoraHandler(readPort, writePort, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private EmpresaOperadoraResult existingEmpresa() {
        return new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456789", "Boticas SAC", null, null, null, null, null,
                null, "PEN", "America/Lima", false, "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private ActualizarEmpresaOperadoraCommand validCommand() {
        return new ActualizarEmpresaOperadoraCommand(
                EMPRESA_ID, TENANT_ID, "Boticas del Peru SAC", "Boticas", null, null, null, null, null,
                "PEN", "America/Lima", true);
    }

    @Test
    void updatesWhenExists() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        when(writePort.save(any())).thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.UPDATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.razonSocial()).isEqualTo("Boticas del Peru SAC");
            assertThat(empresa.ruc()).isEqualTo("20123456789");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsNotFoundWhenEmpresaMissing() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        var invalidCommand = new ActualizarEmpresaOperadoraCommand(
                EMPRESA_ID, TENANT_ID, "A", null, null, null, null, null, null,
                "PEN", "America/Lima", false);

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.command.ActualizarEmpresaOperadoraHandlerTest"`
Expected: FAIL (compilation error)

Implementar `application/usecase/command/ActualizarEmpresaOperadoraHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarEmpresaOperadoraHandler implements ActualizarEmpresaOperadoraUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarEmpresaOperadoraHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(ActualizarEmpresaOperadoraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findEmpresaById(command.tenantId(), command.empresaId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        var current = existing.get();
        var empresa = EmpresaOperadora.restore(
                new EmpresaOperadoraId(current.id()), new TenantId(current.tenantId()), current.ruc(),
                current.razonSocial(), current.nombreComercial(), current.direccionFiscal(),
                current.ubigeoFiscal(), current.telefono(), current.email(), current.sitioWeb(),
                current.monedaFuncional(), current.zonaHoraria(), current.permiteVentaOnline(),
                EstadoEmpresaOperadora.valueOf(current.estado()), current.createdAt(), current.updatedAt());

        var updated = empresa.updateDetails(
                command.razonSocial(), command.nombreComercial(), command.direccionFiscal(),
                command.ubigeoFiscal(), command.telefono(), command.email(), command.sitioWeb(),
                command.monedaFuncional(), command.zonaHoraria(), command.permiteVentaOnline(), clock.now());

        return updated.fold(this::persist, this::validationFailure);
    }

    private Result<EmpresaOperadoraResult, ApplicationError> persist(EmpresaOperadora empresa) {
        writePort.save(empresa);
        return Result.success(OrganizacionApplicationMapper.toResult(empresa));
    }

    private Result<EmpresaOperadoraResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.command.ActualizarEmpresaOperadoraHandlerTest"`
Expected: PASS

- [ ] **Step 9: Escribir el test y luego implementar `ListarEmpresasHandler`**

Test `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ListarEmpresasHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarEmpresasHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ListarEmpresasHandler handler = new ListarEmpresasHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void listsWithValidPagination() {
        when(readPort.findEmpresas(TENANT_ID, "", 0, 20))
                .thenReturn(new PaginaResult<>(List.of(), 0, 20, 0));

        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 20));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNegativePage() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", -1, 20));

        assertThat(result.isFailure()).isTrue();
        result.fold(page -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void rejectsSizeBelowOne() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 0));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsSizeAboveHundred() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 101));

        assertThat(result.isFailure()).isTrue();
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ListarEmpresasHandlerTest"`
Expected: FAIL (compilation error)

Implementar `application/usecase/query/ListarEmpresasHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarEmpresasHandler implements ListarEmpresasUseCase {

    private final OrganizacionReadPort readPort;

    public ListarEmpresasHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<EmpresaOperadoraResult>, ApplicationError> execute(ListarEmpresasQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "ORG_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findEmpresas(query.tenantId(), query.search(), query.page(), query.size()));
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ListarEmpresasHandlerTest"`
Expected: PASS

- [ ] **Step 10: Escribir el test y luego implementar `ObtenerEmpresaHandler`**

Test `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEmpresaHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerEmpresaHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerEmpresaHandler handler = new ObtenerEmpresaHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Test
    void returnsEmpresaWhenFound() {
        var empresaResult = new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456789", "Boticas SAC", null, null, null, null, null,
                null, "PEN", "America/Lima", false, "ACTIVO", Instant.now(), null);
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(empresaResult));

        var result = handler.execute(new ObtenerEmpresaQuery(TENANT_ID, EMPRESA_ID));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsNotFoundWhenMissing() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new ObtenerEmpresaQuery(TENANT_ID, EMPRESA_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ObtenerEmpresaHandlerTest"`
Expected: FAIL (compilation error)

Implementar `application/usecase/query/ObtenerEmpresaHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerEmpresaHandler implements ObtenerEmpresaUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerEmpresaHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(ObtenerEmpresaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findEmpresaById(query.tenantId(), query.empresaId())
                .<Result<EmpresaOperadoraResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ObtenerEmpresaHandlerTest"`
Expected: PASS

- [ ] **Step 11: Repetir Steps 4-10 para Establecimiento**

Crear, en el mismo orden test-primero-implementación-después, siguiendo exactamente los mismos patrones ya usados para Empresa (mismas firmas de mock, misma estructura de casos "válido"/"NOT_FOUND"/"VALIDATION"/"CONFLICT" donde aplique):

1. `CrearEstablecimientoHandlerTest` + `CrearEstablecimientoHandler` — construye `Establecimiento.create(...)` con `EstablecimientoId` nuevo, `EmpresaOperadoraId`/`TenantId` desde el command; `persist` mapea `SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND`→`ORG_EMPRESA_NO_ENCONTRADA`/NOT_FOUND, `DUPLICATE_CODIGO`→`ORG_ESTABLECIMIENTO_CODIGO_DUPLICADO`/CONFLICT, `DUPLICATE_DIGEMID`→`ORG_ESTABLECIMIENTO_DIGEMID_DUPLICADO`/CONFLICT.
2. `ActualizarEstablecimientoHandlerTest` + `ActualizarEstablecimientoHandler` — lee con `readPort.findEstablecimientoById`, reconstruye vía `Establecimiento.restore(...)`, llama `updateDetails(...)`, persiste.
3. `ListarEstablecimientosHandlerTest` + `ListarEstablecimientosHandler` — misma validación de paginación `page>=0`/`1<=size<=100`, delega a `readPort.findEstablecimientos(tenantId, empresaId, search, page, size)`.
4. `ObtenerEstablecimientoHandlerTest` + `ObtenerEstablecimientoHandler` — delega a `readPort.findEstablecimientoById`, NOT_FOUND si vacío.

Agregar en `OrganizacionApplicationMapper` ningún método nuevo (ya se agregó `toResult(Establecimiento)` en el Step 6) — solo usarlo desde estos handlers.

Código de error de validación reusa `ORG_ESTABLECIMIENTO_INVALIDO` (ya viene del dominio vía `ErrorDetail`), y el de "no encontrado" es `ORG_ESTABLECIMIENTO_NO_ENCONTRADO`.

- [ ] **Step 12: Ejecutar todos los tests de Task 4 con cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test jacocoTestReport`
Expected: PASS, 100% cobertura en los 8 handlers y en `OrganizacionApplicationMapper` (sus dos métodos usados). Si algún outcome enum no tiene test dedicado (p.ej. `UPDATED` en el `save` de creación, que no debería devolverse ahí), revisar que la rama sea alcanzable o simplificar el `if` para no dejar código muerto.

- [ ] **Step 13: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application
git commit -m "feat(organizacion): agregar casos de uso de Empresa Operadora y Establecimiento"
```

---

## Task 5: Casos de uso — Almacén, Terminal POS y Estructura Corporativa

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CrearAlmacenUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ActualizarAlmacenUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ListarAlmacenesUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ObtenerAlmacenUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CrearTerminalPosUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ActualizarTerminalPosUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ListarTerminalesUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ObtenerTerminalUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/ObtenerEstructuraCorporativaUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CrearAlmacenHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarAlmacenHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ListarAlmacenesHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerAlmacenHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CrearTerminalPosHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarTerminalPosHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ListarTerminalesHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerTerminalHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEstructuraCorporativaHandler.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/mapper/OrganizacionApplicationMapper.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearAlmacenHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarAlmacenHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ListarAlmacenesHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerAlmacenHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearTerminalPosHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarTerminalPosHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ListarTerminalesHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerTerminalHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEstructuraCorporativaHandlerTest.java`

**Interfaces:**
- Consumes: `OrganizacionReadPort`/`OrganizacionWritePort` (Task 3), `Almacen`/`TerminalPos` (Task 2), `OrganizacionApplicationMapper.toResult(EmpresaOperadora)`/`toResult(Establecimiento)` (Task 4).
- Produces: 8 casos de uso + handlers de Almacén/TerminalPos (mismo patrón exacto de Task 4) + `ObtenerEstructuraCorporativaUseCase`/`Handler`; `OrganizacionApplicationMapper` gana `toResult(Almacen)` y `toResult(TerminalPos)`.

- [ ] **Step 1: Crear los 8 puertos de entrada de Almacén y TerminalPos**

```java
// application/port/in/CrearAlmacenUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearAlmacenUseCase {
    Result<AlmacenResult, ApplicationError> execute(CrearAlmacenCommand command);
}
```

```java
// application/port/in/ActualizarAlmacenUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarAlmacenUseCase {
    Result<AlmacenResult, ApplicationError> execute(ActualizarAlmacenCommand command);
}
```

```java
// application/port/in/ListarAlmacenesUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarAlmacenesQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarAlmacenesUseCase {
    Result<PaginaResult<AlmacenResult>, ApplicationError> execute(ListarAlmacenesQuery query);
}
```

```java
// application/port/in/ObtenerAlmacenUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerAlmacenQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerAlmacenUseCase {
    Result<AlmacenResult, ApplicationError> execute(ObtenerAlmacenQuery query);
}
```

```java
// application/port/in/CrearTerminalPosUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearTerminalPosUseCase {
    Result<TerminalPosResult, ApplicationError> execute(CrearTerminalPosCommand command);
}
```

```java
// application/port/in/ActualizarTerminalPosUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarTerminalPosUseCase {
    Result<TerminalPosResult, ApplicationError> execute(ActualizarTerminalPosCommand command);
}
```

```java
// application/port/in/ListarTerminalesUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarTerminalesQuery;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarTerminalesUseCase {
    Result<PaginaResult<TerminalPosResult>, ApplicationError> execute(ListarTerminalesQuery query);
}
```

```java
// application/port/in/ObtenerTerminalUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerTerminalQuery;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerTerminalUseCase {
    Result<TerminalPosResult, ApplicationError> execute(ObtenerTerminalQuery query);
}
```

```java
// application/port/in/ObtenerEstructuraCorporativaUseCase.java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerEstructuraCorporativaUseCase {
    Result<EstructuraCorporativaResult, ApplicationError> execute(ObtenerEstructuraCorporativaQuery query);
}
```

- [ ] **Step 2: Agregar `toResult(Almacen)` y `toResult(TerminalPos)` a `OrganizacionApplicationMapper`**

Editar `application/mapper/OrganizacionApplicationMapper.java` (agregar imports y métodos, sin tocar lo ya existente de Empresa/Establecimiento):

```java
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
```

```java
    public static AlmacenResult toResult(Almacen almacen) {
        return new AlmacenResult(
                almacen.id().value(), almacen.tenantId().value(), almacen.establecimientoId().value(),
                almacen.codigo(), almacen.nombre(), almacen.tipo().name(), almacen.permiteLotes(),
                almacen.permiteVencimiento(), almacen.permiteVenta(), almacen.permiteDespacho(),
                almacen.controlTemperatura(), almacen.temperaturaMinC(), almacen.temperaturaMaxC(),
                almacen.activo(), almacen.createdAt(), almacen.updatedAt());
    }

    public static TerminalPosResult toResult(TerminalPos terminal) {
        return new TerminalPosResult(
                terminal.id().value(), terminal.tenantId().value(), terminal.establecimientoId().value(),
                terminal.codigo(), terminal.nombre(), terminal.serieBoletaDefecto(),
                terminal.serieFacturaDefecto(), terminal.numeroSerieEquipo(), terminal.hostname(),
                terminal.ipEquipo(), terminal.impresoraCodigo(), terminal.storeEdgeHabilitado(),
                terminal.estado().name(), terminal.createdAt(), terminal.updatedAt());
    }
```

- [ ] **Step 3: Escribir tests e implementar los 4 handlers de Almacén (mismo patrón de Task 4 Step 11.1-11.4, adaptado a Almacén)**

`CrearAlmacenHandlerTest` (`service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CrearAlmacenHandlerTest.java`):

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearAlmacenHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "22222222-2222-2222-2222-222222222222");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearAlmacenHandler handler = new CrearAlmacenHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    private CrearAlmacenCommand validCommand() {
        return new CrearAlmacenCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);
    }

    @Test
    void createsAlmacenWhenValid() {
        when(writePort.save(any())).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearAlmacenCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, " ", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenEstablecimientoMissing() {
        when(writePort.save(any())).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenCodigoDuplicated() {
        when(writePort.save(any())).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.DUPLICATE_CODIGO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }
}
```

Implementar `application/usecase/command/CrearAlmacenHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.TipoAlmacen;
import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearAlmacenHandler implements CrearAlmacenUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearAlmacenHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<AlmacenResult, ApplicationError> execute(CrearAlmacenCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        TipoAlmacen tipo;
        try {
            tipo = TipoAlmacen.valueOf(command.tipo() == null ? "" : command.tipo());
        } catch (IllegalArgumentException exception) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ALMACEN_INVALIDO", "El tipo de almacén no es válido.", ErrorCategory.VALIDATION));
        }
        var almacen = Almacen.create(
                new AlmacenId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.establecimientoId() == null ? null : new EstablecimientoId(command.establecimientoId()),
                command.codigo(), command.nombre(), tipo, command.permiteLotes(), command.permiteVencimiento(),
                command.permiteVenta(), command.permiteDespacho(), command.controlTemperatura(),
                command.temperaturaMinC(), command.temperaturaMaxC(), clock.now());
        return almacen.fold(this::persist, this::validationFailure);
    }

    private Result<AlmacenResult, ApplicationError> persist(Almacen almacen) {
        var outcome = writePort.save(almacen);
        if (outcome == OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveAlmacenOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ALMACEN_CODIGO_DUPLICADO", "Ya existe un almacén con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(almacen));
    }

    private Result<AlmacenResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.command.CrearAlmacenHandlerTest"`
Expected: PASS

Repetir el mismo patrón (test primero, luego implementación) para:

- **`ActualizarAlmacenHandlerTest`/`ActualizarAlmacenHandler`**: lee con `readPort.findAlmacenById`, reconstruye vía `Almacen.restore(...)` (usando `TipoAlmacen.valueOf(current.tipo())`), llama `updateDetails(...)`; si `command.activo()` difiere del actual, encadena `activar(clock.now())`/`desactivar(clock.now())` después de `updateDetails`. NOT_FOUND si no existe.
- **`ListarAlmacenesHandlerTest`/`ListarAlmacenesHandler`**: valida paginación igual que `ListarEmpresasHandler`, delega a `readPort.findAlmacenes(tenantId, establecimientoId, search, page, size)`.
- **`ObtenerAlmacenHandlerTest`/`ObtenerAlmacenHandler`**: delega a `readPort.findAlmacenById`, NOT_FOUND si vacío — mismo patrón exacto de `ObtenerEmpresaHandler`.

- [ ] **Step 4: Escribir tests e implementar los 4 handlers de Terminal POS (mismo patrón)**

`CrearTerminalPosHandler` sigue exactamente la forma de `CrearAlmacenHandler`, pero sin conversión de enum en el comando (el `estado` inicial siempre es `ACTIVO`, fijado por el dominio en `create`, no viene del command):

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearTerminalPosHandler implements CrearTerminalPosUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearTerminalPosHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TerminalPosResult, ApplicationError> execute(CrearTerminalPosCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var terminal = TerminalPos.create(
                new TerminalPosId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.establecimientoId() == null ? null : new EstablecimientoId(command.establecimientoId()),
                command.codigo(), command.nombre(), command.serieBoletaDefecto(),
                command.serieFacturaDefecto(), command.numeroSerieEquipo(), command.hostname(),
                command.ipEquipo(), command.impresoraCodigo(), command.storeEdgeHabilitado(), clock.now());
        return terminal.fold(this::persist, this::validationFailure);
    }

    private Result<TerminalPosResult, ApplicationError> persist(TerminalPos terminal) {
        var outcome = writePort.save(terminal);
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_TERMINAL_CODIGO_DUPLICADO", "Ya existe un terminal con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
    }

    private Result<TerminalPosResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

`CrearTerminalPosHandlerTest` sigue exactamente la forma de `CrearAlmacenHandlerTest` (mismos 4 casos: éxito, validación, NOT_FOUND, CONFLICT), sustituyendo los tipos y el outcome enum por los de `SaveTerminalOutcome`.

Repetir el mismo patrón test-primero para:

- **`ActualizarTerminalPosHandlerTest`/`ActualizarTerminalPosHandler`**: lee con `readPort.findTerminalById`, reconstruye vía `TerminalPos.restore(...)` (usando `EstadoTerminalPos.valueOf(current.estado())`), llama `updateDetails(...)`; si `command.estado()` difiere, encadena `cambiarEstado(EstadoTerminalPos.valueOf(command.estado()), clock.now())`.
- **`ListarTerminalesHandlerTest`/`ListarTerminalesHandler`**: valida paginación, delega a `readPort.findTerminales(tenantId, establecimientoId, search, page, size)`.
- **`ObtenerTerminalHandlerTest`/`ObtenerTerminalHandler`**: delega a `readPort.findTerminalById`, NOT_FOUND si vacío.

- [ ] **Step 5: Escribir el test y luego implementar `ObtenerEstructuraCorporativaHandler`**

Test `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/query/ObtenerEstructuraCorporativaHandlerTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerEstructuraCorporativaHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerEstructuraCorporativaHandler handler =
            new ObtenerEstructuraCorporativaHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void returnsStructureFromReadPort() {
        var expected = new EstructuraCorporativaResult(Instant.now(), List.of());
        when(readPort.findEstructuraCorporativa(TENANT_ID)).thenReturn(expected);

        var result = handler.execute(new ObtenerEstructuraCorporativaQuery(TENANT_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(structure -> {
            assertThat(structure).isEqualTo(expected);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstructuraCorporativaHandlerTest"`
Expected: FAIL (compilation error)

Implementar `application/usecase/query/ObtenerEstructuraCorporativaHandler.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerEstructuraCorporativaHandler implements ObtenerEstructuraCorporativaUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerEstructuraCorporativaHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<EstructuraCorporativaResult, ApplicationError> execute(
            ObtenerEstructuraCorporativaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findEstructuraCorporativa(query.tenantId()));
    }
}
```

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstructuraCorporativaHandlerTest"`
Expected: PASS

- [ ] **Step 6: Ejecutar todos los tests del módulo con cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test jacocoTestReport`
Expected: PASS, 100% cobertura en los 16 handlers nuevos de esta tarea y en los 2 métodos nuevos de `OrganizacionApplicationMapper`.

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application
git commit -m "feat(organizacion): agregar casos de uso de Almacen, Terminal POS y estructura corporativa"
```

---

## Task 6: Infraestructura de escritura — entidades JPA, repositorios y `OrganizacionJpaWriteAdapter`

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/entity/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/entity/EmpresaOperadoraJpaEntity.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/entity/EstablecimientoJpaEntity.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/entity/AlmacenJpaEntity.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/entity/TerminalPosJpaEntity.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/repository/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/repository/EmpresaOperadoraJpaRepository.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/repository/EstablecimientoJpaRepository.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/repository/AlmacenJpaRepository.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/repository/TerminalPosJpaRepository.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/mapper/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/mapper/OrganizacionWriteMapper.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/adapter/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java`

**Interfaces:**
- Consumes: `OrganizacionWritePort` (Task 3), agregados de dominio (Tasks 1-2).
- Produces: `OrganizacionJpaWriteAdapter implements OrganizacionWritePort`, registrado como `@Repository` — consumido por Task 8 (`OrganizacionModuleConfiguration`).

Este test es de integración con H2 (perfil test del proyecto), siguiendo el patrón de `@DataJpaTest`/similar ya usado en el proyecto para adapters JPA. Antes de escribirlo, revisar cómo `security` prueba `IamJpaWriteAdapter` (si tiene test) para replicar el mismo tipo de configuración de test (in-memory H2 + Flyway deshabilitado, `ddl-auto: create`, según `bootstrap-app/src/test/resources/application-test.yaml`); si `security` no tiene un test de este adapter específico, usar `@SpringBootTest` mínimo apuntando solo a este módulo con un `@Import` del adapter y sus repos, más un `JdbcClient` de test.

- [ ] **Step 1: Crear los `package-info.java` de infraestructura**

```java
// infrastructure/package-info.java
package com.softprimesolutions.organizacion.infrastructure;
```
```java
// infrastructure/persistence/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence;
```
```java
// infrastructure/persistence/write/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write;
```
```java
// infrastructure/persistence/write/entity/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;
```
```java
// infrastructure/persistence/write/repository/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;
```
```java
// infrastructure/persistence/write/mapper/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.mapper;
```
```java
// infrastructure/persistence/write/adapter/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.adapter;
```

- [ ] **Step 2: Crear las 4 entidades JPA**

```java
// infrastructure/persistence/write/entity/EmpresaOperadoraJpaEntity.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresa_operadora", schema = "sch_organizacion")
public class EmpresaOperadoraJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 300)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 300)
    private String nombreComercial;

    @Column(name = "direccion_fiscal", length = 500)
    private String direccionFiscal;

    @Column(name = "ubigeo_fiscal", length = 6)
    private String ubigeoFiscal;

    @Column(length = 40)
    private String telefono;

    @Column
    private String email;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "moneda_funcional", nullable = false, length = 3)
    private String monedaFuncional;

    @Column(name = "zona_horaria", nullable = false, length = 80)
    private String zonaHoraria;

    @Column(name = "permite_venta_online", nullable = false)
    private boolean permiteVentaOnline;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected EmpresaOperadoraJpaEntity() {
    }

    public EmpresaOperadoraJpaEntity(
            UUID uuidPublico, Long tenantId, String ruc, String razonSocial, String nombreComercial,
            String direccionFiscal, String ubigeoFiscal, String telefono, String email, String sitioWeb,
            String monedaFuncional, String zonaHoraria, boolean permiteVentaOnline, String estado,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.direccionFiscal = direccionFiscal;
        this.ubigeoFiscal = ubigeoFiscal;
        this.telefono = telefono;
        this.email = email;
        this.sitioWeb = sitioWeb;
        this.monedaFuncional = monedaFuncional;
        this.zonaHoraria = zonaHoraria;
        this.permiteVentaOnline = permiteVentaOnline;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getRuc() { return ruc; }
    public String getRazonSocial() { return razonSocial; }
    public String getNombreComercial() { return nombreComercial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public String getUbigeoFiscal() { return ubigeoFiscal; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getSitioWeb() { return sitioWeb; }
    public String getMonedaFuncional() { return monedaFuncional; }
    public String getZonaHoraria() { return zonaHoraria; }
    public boolean isPermiteVentaOnline() { return permiteVentaOnline; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
// infrastructure/persistence/write/entity/EstablecimientoJpaEntity.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "establecimiento_farmaceutico", schema = "sch_organizacion")
public class EstablecimientoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String nombre;

    @Column(name = "tipo_establecimiento", nullable = false, length = 40)
    private String tipoEstablecimiento;

    @Column(name = "categoria_regulatoria_codigo", length = 50)
    private String categoriaRegulatoriaCodigo;

    @Column(name = "codigo_anexo_sunat", nullable = false, length = 4)
    private String codigoAnexoSunat;

    @Column(name = "codigo_digemid", length = 10)
    private String codigoDigemid;

    @Column(length = 500)
    private String direccion;

    @Column(length = 6)
    private String ubigeo;

    @Column(length = 300)
    private String referencia;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(length = 40)
    private String telefono;

    @Column
    private String email;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "permite_venta_online", nullable = false)
    private boolean permiteVentaOnline;

    @Column(name = "permite_delivery", nullable = false)
    private boolean permiteDelivery;

    @Column(name = "perfil_operacion", nullable = false, length = 30)
    private String perfilOperacion;

    @Column(name = "zona_horaria", nullable = false, length = 80)
    private String zonaHoraria;

    @Column(name = "estado_operativo", nullable = false, length = 20)
    private String estadoOperativo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected EstablecimientoJpaEntity() {
    }

    public EstablecimientoJpaEntity(
            UUID uuidPublico, Long tenantId, Long empresaId, String codigo, String nombre,
            String tipoEstablecimiento, String categoriaRegulatoriaCodigo, String codigoAnexoSunat,
            String codigoDigemid, String direccion, String ubigeo, String referencia, BigDecimal latitud,
            BigDecimal longitud, String telefono, String email, boolean esPrincipal,
            boolean permiteVentaOnline, boolean permiteDelivery, String perfilOperacion,
            String zonaHoraria, String estadoOperativo, Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoEstablecimiento = tipoEstablecimiento;
        this.categoriaRegulatoriaCodigo = categoriaRegulatoriaCodigo;
        this.codigoAnexoSunat = codigoAnexoSunat;
        this.codigoDigemid = codigoDigemid;
        this.direccion = direccion;
        this.ubigeo = ubigeo;
        this.referencia = referencia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.telefono = telefono;
        this.email = email;
        this.esPrincipal = esPrincipal;
        this.permiteVentaOnline = permiteVentaOnline;
        this.permiteDelivery = permiteDelivery;
        this.perfilOperacion = perfilOperacion;
        this.zonaHoraria = zonaHoraria;
        this.estadoOperativo = estadoOperativo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getTipoEstablecimiento() { return tipoEstablecimiento; }
    public String getCategoriaRegulatoriaCodigo() { return categoriaRegulatoriaCodigo; }
    public String getCodigoAnexoSunat() { return codigoAnexoSunat; }
    public String getCodigoDigemid() { return codigoDigemid; }
    public String getDireccion() { return direccion; }
    public String getUbigeo() { return ubigeo; }
    public String getReferencia() { return referencia; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public boolean isPermiteVentaOnline() { return permiteVentaOnline; }
    public boolean isPermiteDelivery() { return permiteDelivery; }
    public String getPerfilOperacion() { return perfilOperacion; }
    public String getZonaHoraria() { return zonaHoraria; }
    public String getEstadoOperativo() { return estadoOperativo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
// infrastructure/persistence/write/entity/AlmacenJpaEntity.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "almacen", schema = "sch_organizacion")
public class AlmacenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "establecimiento_id", nullable = false)
    private Long establecimientoId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "permite_lotes", nullable = false)
    private boolean permiteLotes;

    @Column(name = "permite_vencimiento", nullable = false)
    private boolean permiteVencimiento;

    @Column(name = "permite_venta", nullable = false)
    private boolean permiteVenta;

    @Column(name = "permite_despacho", nullable = false)
    private boolean permiteDespacho;

    @Column(name = "control_temperatura", nullable = false)
    private boolean controlTemperatura;

    @Column(name = "temperatura_min_c", precision = 5, scale = 2)
    private BigDecimal temperaturaMinC;

    @Column(name = "temperatura_max_c", precision = 5, scale = 2)
    private BigDecimal temperaturaMaxC;

    @Column(name = "es_activo", nullable = false, length = 1)
    private String esActivo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AlmacenJpaEntity() {
    }

    public AlmacenJpaEntity(
            UUID uuidPublico, Long tenantId, Long empresaId, Long establecimientoId, String codigo,
            String nombre, String tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, String esActivo,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.permiteLotes = permiteLotes;
        this.permiteVencimiento = permiteVencimiento;
        this.permiteVenta = permiteVenta;
        this.permiteDespacho = permiteDespacho;
        this.controlTemperatura = controlTemperatura;
        this.temperaturaMinC = temperaturaMinC;
        this.temperaturaMaxC = temperaturaMaxC;
        this.esActivo = esActivo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getEstablecimientoId() { return establecimientoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public boolean isPermiteLotes() { return permiteLotes; }
    public boolean isPermiteVencimiento() { return permiteVencimiento; }
    public boolean isPermiteVenta() { return permiteVenta; }
    public boolean isPermiteDespacho() { return permiteDespacho; }
    public boolean isControlTemperatura() { return controlTemperatura; }
    public BigDecimal getTemperaturaMinC() { return temperaturaMinC; }
    public BigDecimal getTemperaturaMaxC() { return temperaturaMaxC; }
    public String getEsActivo() { return esActivo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

**Nota sobre `AlmacenJpaEntity`:** la tabla `almacen` real tiene `empresa_id` y `establecimiento_id` como columnas NOT NULL (confirmado en `V002__organizacion_establecimientos.sql`, para permitir el índice único compuesto `(tenant_id, establecimiento_id, codigo)`); el dominio `Almacen` no expone `empresaId` porque se puede derivar de `establecimientoId` — el adapter (Step 5) resuelve y completa ambos valores en el insert.

```java
// infrastructure/persistence/write/entity/TerminalPosJpaEntity.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "terminal_pos", schema = "sch_organizacion")
public class TerminalPosJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "establecimiento_id", nullable = false)
    private Long establecimientoId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "serie_boleta_defecto", length = 10)
    private String serieBoletaDefecto;

    @Column(name = "serie_factura_defecto", length = 10)
    private String serieFacturaDefecto;

    @Column(name = "numero_serie_equipo", length = 100)
    private String numeroSerieEquipo;

    @Column(length = 150)
    private String hostname;

    @Column(name = "ip_equipo")
    private String ipEquipo;

    @Column(name = "impresora_codigo", length = 60)
    private String impresoraCodigo;

    @Column(name = "store_edge_habilitado", nullable = false)
    private boolean storeEdgeHabilitado;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected TerminalPosJpaEntity() {
    }

    public TerminalPosJpaEntity(
            UUID uuidPublico, Long tenantId, Long empresaId, Long establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto, String numeroSerieEquipo,
            String hostname, String ipEquipo, String impresoraCodigo, boolean storeEdgeHabilitado,
            String estado, Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.serieBoletaDefecto = serieBoletaDefecto;
        this.serieFacturaDefecto = serieFacturaDefecto;
        this.numeroSerieEquipo = numeroSerieEquipo;
        this.hostname = hostname;
        this.ipEquipo = ipEquipo;
        this.impresoraCodigo = impresoraCodigo;
        this.storeEdgeHabilitado = storeEdgeHabilitado;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getEstablecimientoId() { return establecimientoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getSerieBoletaDefecto() { return serieBoletaDefecto; }
    public String getSerieFacturaDefecto() { return serieFacturaDefecto; }
    public String getNumeroSerieEquipo() { return numeroSerieEquipo; }
    public String getHostname() { return hostname; }
    public String getIpEquipo() { return ipEquipo; }
    public String getImpresoraCodigo() { return impresoraCodigo; }
    public boolean isStoreEdgeHabilitado() { return storeEdgeHabilitado; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 3: Crear los 4 `JpaRepository`**

```java
// infrastructure/persistence/write/repository/EmpresaOperadoraJpaRepository.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EmpresaOperadoraJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaOperadoraJpaRepository extends JpaRepository<EmpresaOperadoraJpaEntity, Long> {

    Optional<EmpresaOperadoraJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndRuc(Long tenantId, String ruc);
}
```

```java
// infrastructure/persistence/write/repository/EstablecimientoJpaRepository.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EstablecimientoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoJpaRepository extends JpaRepository<EstablecimientoJpaEntity, Long> {

    Optional<EstablecimientoJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);

    boolean existsByTenantIdAndCodigoDigemid(Long tenantId, String codigoDigemid);
}
```

```java
// infrastructure/persistence/write/repository/AlmacenJpaRepository.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.AlmacenJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenJpaRepository extends JpaRepository<AlmacenJpaEntity, Long> {

    Optional<AlmacenJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndEstablecimientoIdAndCodigo(Long tenantId, Long establecimientoId, String codigo);
}
```

```java
// infrastructure/persistence/write/repository/TerminalPosJpaRepository.java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.TerminalPosJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalPosJpaRepository extends JpaRepository<TerminalPosJpaEntity, Long> {

    Optional<TerminalPosJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndEstablecimientoIdAndCodigo(Long tenantId, Long establecimientoId, String codigo);
}
```

- [ ] **Step 4: Crear `OrganizacionWriteMapper`**

```java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.mapper;

import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.AlmacenJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EmpresaOperadoraJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EstablecimientoJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.TerminalPosJpaEntity;

public final class OrganizacionWriteMapper {

    private OrganizacionWriteMapper() {
    }

    public static EmpresaOperadoraJpaEntity toEntity(EmpresaOperadora empresa, Long tenantId) {
        return new EmpresaOperadoraJpaEntity(
                empresa.id().value(), tenantId, empresa.ruc(), empresa.razonSocial(),
                empresa.nombreComercial(), empresa.direccionFiscal(), empresa.ubigeoFiscal(),
                empresa.telefono(), empresa.email(), empresa.sitioWeb(), empresa.monedaFuncional(),
                empresa.zonaHoraria(), empresa.permiteVentaOnline(), empresa.estado().name(),
                empresa.createdAt(), empresa.updatedAt());
    }

    public static EstablecimientoJpaEntity toEntity(
            Establecimiento establecimiento, Long tenantId, Long empresaId) {
        return new EstablecimientoJpaEntity(
                establecimiento.id().value(), tenantId, empresaId, establecimiento.codigo(),
                establecimiento.nombre(), establecimiento.tipoEstablecimiento().name(),
                establecimiento.categoriaRegulatoriaCodigo(), establecimiento.codigoAnexoSunat(),
                establecimiento.codigoDigemid(), establecimiento.direccion(), establecimiento.ubigeo(),
                establecimiento.referencia(), establecimiento.latitud(), establecimiento.longitud(),
                establecimiento.telefono(), establecimiento.email(), establecimiento.esPrincipal(),
                establecimiento.permiteVentaOnline(), establecimiento.permiteDelivery(),
                establecimiento.perfilOperacion().name(), establecimiento.zonaHoraria(),
                establecimiento.estadoOperativo().name(), establecimiento.createdAt(),
                establecimiento.updatedAt());
    }

    public static AlmacenJpaEntity toEntity(
            Almacen almacen, Long tenantId, Long empresaId, Long establecimientoId) {
        return new AlmacenJpaEntity(
                almacen.id().value(), tenantId, empresaId, establecimientoId, almacen.codigo(),
                almacen.nombre(), almacen.tipo().name(), almacen.permiteLotes(),
                almacen.permiteVencimiento(), almacen.permiteVenta(), almacen.permiteDespacho(),
                almacen.controlTemperatura(), almacen.temperaturaMinC(), almacen.temperaturaMaxC(),
                almacen.activo() ? "1" : "0", almacen.createdAt(), almacen.updatedAt());
    }

    public static TerminalPosJpaEntity toEntity(
            TerminalPos terminal, Long tenantId, Long empresaId, Long establecimientoId) {
        return new TerminalPosJpaEntity(
                terminal.id().value(), tenantId, empresaId, establecimientoId, terminal.codigo(),
                terminal.nombre(), terminal.serieBoletaDefecto(), terminal.serieFacturaDefecto(),
                terminal.numeroSerieEquipo(), terminal.hostname(), terminal.ipEquipo(),
                terminal.impresoraCodigo(), terminal.storeEdgeHabilitado(), terminal.estado().name(),
                terminal.createdAt(), terminal.updatedAt());
    }
}
```

- [ ] **Step 5: Implementar `OrganizacionJpaWriteAdapter`**

Sigue el patrón de `IamJpaWriteAdapter`: resuelve `tenant_id`/FKs padre por `uuid_publico` vía `JdbcClient`, usa `JpaRepository.saveAndFlush` para creación y `JdbcClient` directo para actualización, captura `DataIntegrityViolationException` como fallback de duplicado.

```java
package com.softprimesolutions.organizacion.infrastructure.persistence.write.adapter;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.mapper.OrganizacionWriteMapper;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.AlmacenJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EmpresaOperadoraJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EstablecimientoJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.TerminalPosJpaRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrganizacionJpaWriteAdapter implements OrganizacionWritePort {

    private final EmpresaOperadoraJpaRepository empresaRepository;
    private final EstablecimientoJpaRepository establecimientoRepository;
    private final AlmacenJpaRepository almacenRepository;
    private final TerminalPosJpaRepository terminalRepository;
    private final JdbcClient jdbcClient;

    public OrganizacionJpaWriteAdapter(
            EmpresaOperadoraJpaRepository empresaRepository,
            EstablecimientoJpaRepository establecimientoRepository,
            AlmacenJpaRepository almacenRepository,
            TerminalPosJpaRepository terminalRepository,
            JdbcClient jdbcClient) {
        this.empresaRepository = empresaRepository;
        this.establecimientoRepository = establecimientoRepository;
        this.almacenRepository = almacenRepository;
        this.terminalRepository = terminalRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveEmpresaOutcome save(EmpresaOperadora empresa) {
        var tenantId = findTenantId(empresa.tenantId().value());
        if (tenantId.isEmpty()) return SaveEmpresaOutcome.TENANT_NOT_FOUND;
        var existing = empresaRepository.findByUuidPublico(empresa.id().value());
        if (existing.isEmpty() && empresaRepository.existsByTenantIdAndRuc(tenantId.get(), empresa.ruc())) {
            return SaveEmpresaOutcome.DUPLICATE_RUC;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.empresa_operadora
                                   SET razon_social = :razonSocial, nombre_comercial = :nombreComercial,
                                       direccion_fiscal = :direccionFiscal, ubigeo_fiscal = :ubigeoFiscal,
                                       telefono = :telefono, email = :email, sitio_web = :sitioWeb,
                                       moneda_funcional = :monedaFuncional, zona_horaria = :zonaHoraria,
                                       permite_venta_online = :permiteVentaOnline, estado = :estado,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :empresaId
                                """)
                        .param("razonSocial", empresa.razonSocial())
                        .param("nombreComercial", empresa.nombreComercial())
                        .param("direccionFiscal", empresa.direccionFiscal())
                        .param("ubigeoFiscal", empresa.ubigeoFiscal())
                        .param("telefono", empresa.telefono())
                        .param("email", empresa.email())
                        .param("sitioWeb", empresa.sitioWeb())
                        .param("monedaFuncional", empresa.monedaFuncional())
                        .param("zonaHoraria", empresa.zonaHoraria())
                        .param("permiteVentaOnline", empresa.permiteVentaOnline())
                        .param("estado", empresa.estado().name())
                        .param("updatedAt", toOffsetDateTime(empresa.updatedAt()))
                        .param("empresaId", empresa.id().value())
                        .update();
                return SaveEmpresaOutcome.UPDATED;
            }
            empresaRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(empresa, tenantId.get()));
            return SaveEmpresaOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveEmpresaOutcome.DUPLICATE_RUC;
        }
    }

    @Override
    @Transactional
    public SaveEstablecimientoOutcome save(Establecimiento establecimiento) {
        var tenantId = findTenantId(establecimiento.tenantId().value());
        var empresaId = findEmpresaId(establecimiento.tenantId().value(), establecimiento.empresaId().value());
        if (tenantId.isEmpty() || empresaId.isEmpty()) return SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND;

        var existing = establecimientoRepository.findByUuidPublico(establecimiento.id().value());
        if (existing.isEmpty()) {
            if (establecimientoRepository.existsByTenantIdAndCodigo(tenantId.get(), establecimiento.codigo())) {
                return SaveEstablecimientoOutcome.DUPLICATE_CODIGO;
            }
            if (establecimiento.codigoDigemid() != null && establecimientoRepository
                    .existsByTenantIdAndCodigoDigemid(tenantId.get(), establecimiento.codigoDigemid())) {
                return SaveEstablecimientoOutcome.DUPLICATE_DIGEMID;
            }
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.establecimiento_farmaceutico
                                   SET nombre = :nombre, tipo_establecimiento = :tipoEstablecimiento,
                                       categoria_regulatoria_codigo = :categoriaRegulatoriaCodigo,
                                       codigo_anexo_sunat = :codigoAnexoSunat, codigo_digemid = :codigoDigemid,
                                       direccion = :direccion, ubigeo = :ubigeo, referencia = :referencia,
                                       latitud = :latitud, longitud = :longitud, telefono = :telefono,
                                       email = :email, es_principal = :esPrincipal,
                                       permite_venta_online = :permiteVentaOnline,
                                       permite_delivery = :permiteDelivery, perfil_operacion = :perfilOperacion,
                                       zona_horaria = :zonaHoraria, estado_operativo = :estadoOperativo,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :establecimientoId
                                """)
                        .param("nombre", establecimiento.nombre())
                        .param("tipoEstablecimiento", establecimiento.tipoEstablecimiento().name())
                        .param("categoriaRegulatoriaCodigo", establecimiento.categoriaRegulatoriaCodigo())
                        .param("codigoAnexoSunat", establecimiento.codigoAnexoSunat())
                        .param("codigoDigemid", establecimiento.codigoDigemid())
                        .param("direccion", establecimiento.direccion())
                        .param("ubigeo", establecimiento.ubigeo())
                        .param("referencia", establecimiento.referencia())
                        .param("latitud", establecimiento.latitud())
                        .param("longitud", establecimiento.longitud())
                        .param("telefono", establecimiento.telefono())
                        .param("email", establecimiento.email())
                        .param("esPrincipal", establecimiento.esPrincipal())
                        .param("permiteVentaOnline", establecimiento.permiteVentaOnline())
                        .param("permiteDelivery", establecimiento.permiteDelivery())
                        .param("perfilOperacion", establecimiento.perfilOperacion().name())
                        .param("zonaHoraria", establecimiento.zonaHoraria())
                        .param("estadoOperativo", establecimiento.estadoOperativo().name())
                        .param("updatedAt", toOffsetDateTime(establecimiento.updatedAt()))
                        .param("establecimientoId", establecimiento.id().value())
                        .update();
                return SaveEstablecimientoOutcome.UPDATED;
            }
            establecimientoRepository.saveAndFlush(
                    OrganizacionWriteMapper.toEntity(establecimiento, tenantId.get(), empresaId.get()));
            return SaveEstablecimientoOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveEstablecimientoOutcome.DUPLICATE_CODIGO;
        }
    }

    @Override
    @Transactional
    public SaveAlmacenOutcome save(Almacen almacen) {
        var tenantId = findTenantId(almacen.tenantId().value());
        var parent = findEstablecimientoConEmpresa(almacen.tenantId().value(), almacen.establecimientoId().value());
        if (tenantId.isEmpty() || parent.isEmpty()) return SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND;

        var existing = almacenRepository.findByUuidPublico(almacen.id().value());
        if (existing.isEmpty() && almacenRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                tenantId.get(), parent.get().establecimientoId(), almacen.codigo())) {
            return SaveAlmacenOutcome.DUPLICATE_CODIGO;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.almacen
                                   SET nombre = :nombre, tipo = :tipo, permite_lotes = :permiteLotes,
                                       permite_vencimiento = :permiteVencimiento, permite_venta = :permiteVenta,
                                       permite_despacho = :permiteDespacho, control_temperatura = :controlTemperatura,
                                       temperatura_min_c = :temperaturaMinC, temperatura_max_c = :temperaturaMaxC,
                                       es_activo = :esActivo, updated_at = :updatedAt
                                 WHERE uuid_publico = :almacenId
                                """)
                        .param("nombre", almacen.nombre())
                        .param("tipo", almacen.tipo().name())
                        .param("permiteLotes", almacen.permiteLotes())
                        .param("permiteVencimiento", almacen.permiteVencimiento())
                        .param("permiteVenta", almacen.permiteVenta())
                        .param("permiteDespacho", almacen.permiteDespacho())
                        .param("controlTemperatura", almacen.controlTemperatura())
                        .param("temperaturaMinC", almacen.temperaturaMinC())
                        .param("temperaturaMaxC", almacen.temperaturaMaxC())
                        .param("esActivo", almacen.activo() ? "1" : "0")
                        .param("updatedAt", toOffsetDateTime(almacen.updatedAt()))
                        .param("almacenId", almacen.id().value())
                        .update();
                return SaveAlmacenOutcome.UPDATED;
            }
            almacenRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(
                    almacen, tenantId.get(), parent.get().empresaId(), parent.get().establecimientoId()));
            return SaveAlmacenOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveAlmacenOutcome.DUPLICATE_CODIGO;
        }
    }

    @Override
    @Transactional
    public SaveTerminalOutcome save(TerminalPos terminal) {
        var tenantId = findTenantId(terminal.tenantId().value());
        var parent = findEstablecimientoConEmpresa(terminal.tenantId().value(), terminal.establecimientoId().value());
        if (tenantId.isEmpty() || parent.isEmpty()) return SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND;

        var existing = terminalRepository.findByUuidPublico(terminal.id().value());
        if (existing.isEmpty() && terminalRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                tenantId.get(), parent.get().establecimientoId(), terminal.codigo())) {
            return SaveTerminalOutcome.DUPLICATE_CODIGO;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.terminal_pos
                                   SET nombre = :nombre, serie_boleta_defecto = :serieBoletaDefecto,
                                       serie_factura_defecto = :serieFacturaDefecto,
                                       numero_serie_equipo = :numeroSerieEquipo, hostname = :hostname,
                                       ip_equipo = CAST(:ipEquipo AS inet), impresora_codigo = :impresoraCodigo,
                                       store_edge_habilitado = :storeEdgeHabilitado, estado = :estado,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :terminalId
                                """)
                        .param("nombre", terminal.nombre())
                        .param("serieBoletaDefecto", terminal.serieBoletaDefecto())
                        .param("serieFacturaDefecto", terminal.serieFacturaDefecto())
                        .param("numeroSerieEquipo", terminal.numeroSerieEquipo())
                        .param("hostname", terminal.hostname())
                        .param("ipEquipo", terminal.ipEquipo())
                        .param("impresoraCodigo", terminal.impresoraCodigo())
                        .param("storeEdgeHabilitado", terminal.storeEdgeHabilitado())
                        .param("estado", terminal.estado().name())
                        .param("updatedAt", toOffsetDateTime(terminal.updatedAt()))
                        .param("terminalId", terminal.id().value())
                        .update();
                return SaveTerminalOutcome.UPDATED;
            }
            terminalRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(
                    terminal, tenantId.get(), parent.get().empresaId(), parent.get().establecimientoId()));
            return SaveTerminalOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveTerminalOutcome.DUPLICATE_CODIGO;
        }
    }

    @Override
    public boolean tenantExists(UUID tenantId) {
        return findTenantId(tenantId).isPresent();
    }

    @Override
    public boolean empresaBelongsToTenant(UUID empresaId, UUID tenantId) {
        return findEmpresaId(tenantId, empresaId).isPresent();
    }

    @Override
    public boolean establecimientoBelongsToTenant(UUID establecimientoId, UUID tenantId) {
        return establecimientoRepository.findByUuidPublico(establecimientoId)
                .map(establecimiento -> findTenantId(tenantId)
                        .filter(establecimiento.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<Long> findEmpresaId(UUID tenantUuid, UUID empresaUuid) {
        if (tenantUuid == null || empresaUuid == null) return Optional.empty();
        return jdbcClient.sql("""
                        SELECT e.id
                          FROM sch_organizacion.empresa_operadora e
                          JOIN sch_admin.tenant t ON t.id = e.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :empresaUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("empresaUuid", empresaUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<EstablecimientoConEmpresa> findEstablecimientoConEmpresa(
            UUID tenantUuid, UUID establecimientoUuid) {
        if (tenantUuid == null || establecimientoUuid == null) return Optional.empty();
        return jdbcClient.sql("""
                        SELECT s.id AS establecimiento_id, s.empresa_id AS empresa_id
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND s.uuid_publico = :establecimientoUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("establecimientoUuid", establecimientoUuid)
                .query((rs, rowNumber) -> new EstablecimientoConEmpresa(
                        rs.getLong("establecimiento_id"), rs.getLong("empresa_id")))
                .optional();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record EstablecimientoConEmpresa(Long establecimientoId, Long empresaId) {
    }
}
```

- [ ] **Step 6: Escribir el test de integración del adapter**

Antes de escribir este test, revisar `bootstrap-app/src/test/resources/application-test.yaml` y localizar un test de integración JPA/JDBC existente en `service-botica/modules/security/src/test/java/` (buscar `@SpringBootTest` o `@DataJpaTest` sobre un adapter) para replicar exactamente la misma anotación de arranque de contexto, ya que este adapter necesita un contexto Spring real (JPA + JdbcClient + H2) y no puede probarse con mocks de Mockito puro.

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java` con al menos estos casos (adaptar el arranque de contexto al patrón encontrado):

- `save(EmpresaOperadora)` crea una fila nueva cuando el tenant existe y el RUC no está duplicado.
- `save(EmpresaOperadora)` devuelve `TENANT_NOT_FOUND` cuando el tenant no existe.
- `save(EmpresaOperadora)` devuelve `DUPLICATE_RUC` cuando ya existe una empresa activa con el mismo RUC en el tenant.
- `save(EmpresaOperadora)` con un `id` ya existente actualiza la fila y devuelve `UPDATED`.
- `save(Establecimiento)` crea una fila nueva cuando la empresa existe.
- `save(Establecimiento)` devuelve `EMPRESA_NOT_FOUND` cuando la empresa no existe.
- `save(Establecimiento)` devuelve `DUPLICATE_CODIGO` cuando el código ya existe en el tenant.
- `save(Establecimiento)` devuelve `DUPLICATE_DIGEMID` cuando el código DIGEMID ya existe en el tenant.
- `save(Almacen)` crea una fila nueva cuando el establecimiento existe.
- `save(Almacen)` devuelve `ESTABLECIMIENTO_NOT_FOUND` cuando el establecimiento no existe.
- `save(Almacen)` devuelve `DUPLICATE_CODIGO` cuando el código ya existe en el establecimiento.
- `save(TerminalPos)` crea una fila nueva cuando el establecimiento existe.
- `save(TerminalPos)` devuelve `DUPLICATE_CODIGO` cuando el código ya existe en el establecimiento.
- `tenantExists`, `empresaBelongsToTenant`, `establecimientoBelongsToTenant` — casos verdadero/falso.

Cada test de "crea fila nueva" debe insertar primero (vía `JdbcClient` de test o fixtures SQL) un tenant, y para los casos de Establecimiento/Almacén/Terminal, también una empresa/establecimiento padre, replicando el estilo de fixtures que use el test de referencia de `security` encontrado en este mismo Step.

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.infrastructure.persistence.write.adapter.OrganizacionJpaWriteAdapterTest"`
Expected: PASS tras implementar. Iterar hasta que todos los casos listados pasen y JaCoCo reporte 100% de líneas/ramas para `OrganizacionJpaWriteAdapter.java` y `OrganizacionWriteMapper.java`.

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/write
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/package-info.java
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/package-info.java
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure
git commit -m "feat(organizacion): agregar infraestructura de escritura JPA para los 4 agregados"
```

---

## Task 7: Infraestructura de lectura — proyecciones JDBC y `OrganizacionJdbcReadAdapter`

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/projection/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/projection/EmpresaProjection.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/projection/EstablecimientoProjection.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/projection/AlmacenProjection.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/projection/TerminalProjection.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/repository/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/repository/OrganizacionJdbcReadRepository.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/mapper/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/mapper/OrganizacionReadMapper.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/adapter/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapter.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapterTest.java`

**Interfaces:**
- Consumes: `OrganizacionReadPort` (Task 3), resultados de aplicación (Task 3).
- Produces: `OrganizacionJdbcReadAdapter implements OrganizacionReadPort`, registrado como `@Repository` — consumido por Task 8.

- [ ] **Step 1: Crear los `package-info.java`**

```java
// infrastructure/persistence/read/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read;
```
```java
// infrastructure/persistence/read/projection/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;
```
```java
// infrastructure/persistence/read/repository/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.repository;
```
```java
// infrastructure/persistence/read/mapper/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.mapper;
```
```java
// infrastructure/persistence/read/adapter/package-info.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.adapter;
```

- [ ] **Step 2: Crear las 4 proyecciones**

```java
// infrastructure/persistence/read/projection/EmpresaProjection.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record EmpresaProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// infrastructure/persistence/read/projection/EstablecimientoProjection.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstablecimientoProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID empresaUuid,
        String codigo,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria,
        String estadoOperativo,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// infrastructure/persistence/read/projection/AlmacenProjection.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlmacenProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID establecimientoUuid,
        String codigo,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// infrastructure/persistence/read/projection/TerminalProjection.java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record TerminalProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID establecimientoUuid,
        String codigo,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 3: Implementar `OrganizacionJdbcReadRepository`**

Sigue el patrón de `IamJdbcReadRepository`: constantes `{X}_FILTER` con el `FROM`+`WHERE` reutilizado entre `find`/`count`, filtro `search` case-insensitive con `LIKE`, paginación `LIMIT/OFFSET`.

```java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EmpresaProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizacionJdbcReadRepository {

    private static final String EMPRESA_FILTER = """
            FROM sch_organizacion.empresa_operadora e
            JOIN sch_admin.tenant t ON t.id = e.tenant_id
            WHERE t.uuid_publico = :tenantId AND e.es_activo = '1'
              AND (:search = '' OR LOWER(e.razon_social) LIKE :pattern OR LOWER(e.ruc) LIKE :pattern
                OR LOWER(COALESCE(e.nombre_comercial, '')) LIKE :pattern)
            """;
    private static final String ESTABLECIMIENTO_FILTER = """
            FROM sch_organizacion.establecimiento_farmaceutico s
            JOIN sch_organizacion.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
            JOIN sch_admin.tenant t ON t.id = s.tenant_id
            WHERE t.uuid_publico = :tenantId AND s.es_activo = '1'
              AND (:empresaId IS NULL OR e.uuid_publico = :empresaId)
              AND (:search = '' OR LOWER(s.codigo) LIKE :pattern OR LOWER(s.nombre) LIKE :pattern)
            """;
    private static final String ALMACEN_FILTER = """
            FROM sch_organizacion.almacen a
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
            JOIN sch_admin.tenant t ON t.id = a.tenant_id
            WHERE t.uuid_publico = :tenantId AND a.es_activo = '1'
              AND (:establecimientoId IS NULL OR s.uuid_publico = :establecimientoId)
              AND (:search = '' OR LOWER(a.codigo) LIKE :pattern OR LOWER(a.nombre) LIKE :pattern)
            """;
    private static final String TERMINAL_FILTER = """
            FROM sch_organizacion.terminal_pos p
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
            JOIN sch_admin.tenant t ON t.id = p.tenant_id
            WHERE t.uuid_publico = :tenantId AND p.es_activo = '1'
              AND (:establecimientoId IS NULL OR s.uuid_publico = :establecimientoId)
              AND (:search = '' OR LOWER(p.codigo) LIKE :pattern OR LOWER(p.nombre) LIKE :pattern)
            """;

    private final JdbcClient jdbcClient;

    public OrganizacionJdbcReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<EmpresaProjection> findEmpresas(UUID tenantId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT e.uuid_publico, t.uuid_publico AS tenant_uuid, e.ruc, e.razon_social,
                               e.nombre_comercial, e.direccion_fiscal, e.ubigeo_fiscal, e.telefono, e.email,
                               e.sitio_web, e.moneda_funcional, e.zona_horaria, e.permite_venta_online,
                               e.estado, e.created_at, e.updated_at
                        """ + EMPRESA_FILTER + " ORDER BY e.razon_social, e.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapEmpresa)
                .list();
    }

    public long countEmpresas(UUID tenantId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + EMPRESA_FILTER)
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public Optional<EmpresaProjection> findEmpresaById(UUID tenantId, UUID empresaId) {
        return jdbcClient.sql("""
                        SELECT e.uuid_publico, t.uuid_publico AS tenant_uuid, e.ruc, e.razon_social,
                               e.nombre_comercial, e.direccion_fiscal, e.ubigeo_fiscal, e.telefono, e.email,
                               e.sitio_web, e.moneda_funcional, e.zona_horaria, e.permite_venta_online,
                               e.estado, e.created_at, e.updated_at
                        FROM sch_organizacion.empresa_operadora e
                        JOIN sch_admin.tenant t ON t.id = e.tenant_id
                        WHERE t.uuid_publico = :tenantId AND e.uuid_publico = :empresaId
                        """)
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .query(this::mapEmpresa)
                .optional();
    }

    public List<EstablecimientoProjection> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT s.uuid_publico, t.uuid_publico AS tenant_uuid, e.uuid_publico AS empresa_uuid,
                               s.codigo, s.nombre, s.tipo_establecimiento, s.categoria_regulatoria_codigo,
                               s.codigo_anexo_sunat, s.codigo_digemid, s.direccion, s.ubigeo, s.referencia,
                               s.latitud, s.longitud, s.telefono, s.email, s.es_principal,
                               s.permite_venta_online, s.permite_delivery, s.perfil_operacion,
                               s.zona_horaria, s.estado_operativo, s.created_at, s.updated_at
                        """ + ESTABLECIMIENTO_FILTER + " ORDER BY s.nombre, s.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapEstablecimiento)
                .list();
    }

    public long countEstablecimientos(UUID tenantId, UUID empresaId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + ESTABLECIMIENTO_FILTER)
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public Optional<EstablecimientoProjection> findEstablecimientoById(UUID tenantId, UUID establecimientoId) {
        return jdbcClient.sql("""
                        SELECT s.uuid_publico, t.uuid_publico AS tenant_uuid, e.uuid_publico AS empresa_uuid,
                               s.codigo, s.nombre, s.tipo_establecimiento, s.categoria_regulatoria_codigo,
                               s.codigo_anexo_sunat, s.codigo_digemid, s.direccion, s.ubigeo, s.referencia,
                               s.latitud, s.longitud, s.telefono, s.email, s.es_principal,
                               s.permite_venta_online, s.permite_delivery, s.perfil_operacion,
                               s.zona_horaria, s.estado_operativo, s.created_at, s.updated_at
                        FROM sch_organizacion.establecimiento_farmaceutico s
                        JOIN sch_organizacion.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
                        JOIN sch_admin.tenant t ON t.id = s.tenant_id
                        WHERE t.uuid_publico = :tenantId AND s.uuid_publico = :establecimientoId
                        """)
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .query(this::mapEstablecimiento)
                .optional();
    }

    public List<AlmacenProjection> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT a.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               a.codigo, a.nombre, a.tipo, a.permite_lotes, a.permite_vencimiento,
                               a.permite_venta, a.permite_despacho, a.control_temperatura,
                               a.temperatura_min_c, a.temperatura_max_c, a.es_activo, a.created_at, a.updated_at
                        """ + ALMACEN_FILTER + " ORDER BY a.nombre, a.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapAlmacen)
                .list();
    }

    public long countAlmacenes(UUID tenantId, UUID establecimientoId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + ALMACEN_FILTER)
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public Optional<AlmacenProjection> findAlmacenById(UUID tenantId, UUID almacenId) {
        return jdbcClient.sql("""
                        SELECT a.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               a.codigo, a.nombre, a.tipo, a.permite_lotes, a.permite_vencimiento,
                               a.permite_venta, a.permite_despacho, a.control_temperatura,
                               a.temperatura_min_c, a.temperatura_max_c, a.es_activo, a.created_at, a.updated_at
                        FROM sch_organizacion.almacen a
                        JOIN sch_organizacion.establecimiento_farmaceutico s
                          ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
                        JOIN sch_admin.tenant t ON t.id = a.tenant_id
                        WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId
                        """)
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .query(this::mapAlmacen)
                .optional();
    }

    public List<TerminalProjection> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT p.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               p.codigo, p.nombre, p.serie_boleta_defecto, p.serie_factura_defecto,
                               p.numero_serie_equipo, p.hostname, p.ip_equipo::text AS ip_equipo,
                               p.impresora_codigo, p.store_edge_habilitado, p.estado, p.created_at, p.updated_at
                        """ + TERMINAL_FILTER + " ORDER BY p.nombre, p.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapTerminal)
                .list();
    }

    public long countTerminales(UUID tenantId, UUID establecimientoId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + TERMINAL_FILTER)
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public Optional<TerminalProjection> findTerminalById(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql("""
                        SELECT p.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               p.codigo, p.nombre, p.serie_boleta_defecto, p.serie_factura_defecto,
                               p.numero_serie_equipo, p.hostname, p.ip_equipo::text AS ip_equipo,
                               p.impresora_codigo, p.store_edge_habilitado, p.estado, p.created_at, p.updated_at
                        FROM sch_organizacion.terminal_pos p
                        JOIN sch_organizacion.establecimiento_farmaceutico s
                          ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
                        JOIN sch_admin.tenant t ON t.id = p.tenant_id
                        WHERE t.uuid_publico = :tenantId AND p.uuid_publico = :terminalId
                        """)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query(this::mapTerminal)
                .optional();
    }

    public List<EmpresaProjection> findAllEmpresasActivas(UUID tenantId) {
        return jdbcClient.sql("""
                        SELECT e.uuid_publico, t.uuid_publico AS tenant_uuid, e.ruc, e.razon_social,
                               e.nombre_comercial, e.direccion_fiscal, e.ubigeo_fiscal, e.telefono, e.email,
                               e.sitio_web, e.moneda_funcional, e.zona_horaria, e.permite_venta_online,
                               e.estado, e.created_at, e.updated_at
                        FROM sch_organizacion.empresa_operadora e
                        JOIN sch_admin.tenant t ON t.id = e.tenant_id
                        WHERE t.uuid_publico = :tenantId AND e.es_activo = '1'
                        ORDER BY e.razon_social, e.id
                        """)
                .param("tenantId", tenantId)
                .query(this::mapEmpresa)
                .list();
    }

    public List<EstablecimientoProjection> findAllEstablecimientosActivos(UUID tenantId) {
        return jdbcClient.sql("""
                        SELECT s.uuid_publico, t.uuid_publico AS tenant_uuid, e.uuid_publico AS empresa_uuid,
                               s.codigo, s.nombre, s.tipo_establecimiento, s.categoria_regulatoria_codigo,
                               s.codigo_anexo_sunat, s.codigo_digemid, s.direccion, s.ubigeo, s.referencia,
                               s.latitud, s.longitud, s.telefono, s.email, s.es_principal,
                               s.permite_venta_online, s.permite_delivery, s.perfil_operacion,
                               s.zona_horaria, s.estado_operativo, s.created_at, s.updated_at
                        FROM sch_organizacion.establecimiento_farmaceutico s
                        JOIN sch_organizacion.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
                        JOIN sch_admin.tenant t ON t.id = s.tenant_id
                        WHERE t.uuid_publico = :tenantId AND s.es_activo = '1'
                        ORDER BY s.nombre, s.id
                        """)
                .param("tenantId", tenantId)
                .query(this::mapEstablecimiento)
                .list();
    }

    public List<AlmacenProjection> findAllAlmacenesActivos(UUID tenantId) {
        return jdbcClient.sql("""
                        SELECT a.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               a.codigo, a.nombre, a.tipo, a.permite_lotes, a.permite_vencimiento,
                               a.permite_venta, a.permite_despacho, a.control_temperatura,
                               a.temperatura_min_c, a.temperatura_max_c, a.es_activo, a.created_at, a.updated_at
                        FROM sch_organizacion.almacen a
                        JOIN sch_organizacion.establecimiento_farmaceutico s
                          ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
                        JOIN sch_admin.tenant t ON t.id = a.tenant_id
                        WHERE t.uuid_publico = :tenantId AND a.es_activo = '1'
                        ORDER BY a.nombre, a.id
                        """)
                .param("tenantId", tenantId)
                .query(this::mapAlmacen)
                .list();
    }

    public List<TerminalProjection> findAllTerminalesActivos(UUID tenantId) {
        return jdbcClient.sql("""
                        SELECT p.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                               p.codigo, p.nombre, p.serie_boleta_defecto, p.serie_factura_defecto,
                               p.numero_serie_equipo, p.hostname, p.ip_equipo::text AS ip_equipo,
                               p.impresora_codigo, p.store_edge_habilitado, p.estado, p.created_at, p.updated_at
                        FROM sch_organizacion.terminal_pos p
                        JOIN sch_organizacion.establecimiento_farmaceutico s
                          ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
                        JOIN sch_admin.tenant t ON t.id = p.tenant_id
                        WHERE t.uuid_publico = :tenantId AND p.es_activo = '1'
                        ORDER BY p.nombre, p.id
                        """)
                .param("tenantId", tenantId)
                .query(this::mapTerminal)
                .list();
    }

    private EmpresaProjection mapEmpresa(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new EmpresaProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getString("ruc"), rs.getString("razon_social"), rs.getString("nombre_comercial"),
                rs.getString("direccion_fiscal"), rs.getString("ubigeo_fiscal"), rs.getString("telefono"),
                rs.getString("email"), rs.getString("sitio_web"), rs.getString("moneda_funcional"),
                rs.getString("zona_horaria"), rs.getBoolean("permite_venta_online"), rs.getString("estado"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private EstablecimientoProjection mapEstablecimiento(
            java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new EstablecimientoProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("empresa_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("tipo_establecimiento"), rs.getString("categoria_regulatoria_codigo"),
                rs.getString("codigo_anexo_sunat"), rs.getString("codigo_digemid"), rs.getString("direccion"),
                rs.getString("ubigeo"), rs.getString("referencia"), rs.getBigDecimal("latitud"),
                rs.getBigDecimal("longitud"), rs.getString("telefono"), rs.getString("email"),
                rs.getBoolean("es_principal"), rs.getBoolean("permite_venta_online"),
                rs.getBoolean("permite_delivery"), rs.getString("perfil_operacion"),
                rs.getString("zona_horaria"), rs.getString("estado_operativo"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private AlmacenProjection mapAlmacen(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new AlmacenProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("tipo"), rs.getBoolean("permite_lotes"), rs.getBoolean("permite_vencimiento"),
                rs.getBoolean("permite_venta"), rs.getBoolean("permite_despacho"),
                rs.getBoolean("control_temperatura"), rs.getBigDecimal("temperatura_min_c"),
                rs.getBigDecimal("temperatura_max_c"), "1".equals(rs.getString("es_activo")),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private TerminalProjection mapTerminal(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new TerminalProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("serie_boleta_defecto"), rs.getString("serie_factura_defecto"),
                rs.getString("numero_serie_equipo"), rs.getString("hostname"), rs.getString("ip_equipo"),
                rs.getString("impresora_codigo"), rs.getBoolean("store_edge_habilitado"), rs.getString("estado"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static java.time.Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
```

**Nota:** los métodos `findEstablecimientos`/`findAlmacenes`/`findTerminales` reciben un filtro `empresaId`/`establecimientoId` que puede ser `null` (listar todos bajo el tenant) — el `WHERE (:param IS NULL OR ... = :param)` de `{X}_FILTER` ya contempla ese caso. Verificar que el driver JDBC/H2 acepte el parámetro `null` tipado como `UUID` sin lanzar excepción (si H2 en test lo rechaza, castear explícitamente con `(UUID) null` al llamar `.param(...)`, ya que `JdbcClient` a veces requiere el tipo explícito para parámetros nulos).

- [ ] **Step 4: Implementar `OrganizacionReadMapper`**

```java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.mapper;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EmpresaProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;

public final class OrganizacionReadMapper {

    private OrganizacionReadMapper() {
    }

    public static EmpresaOperadoraResult toResult(EmpresaProjection projection) {
        return new EmpresaOperadoraResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.ruc(), projection.razonSocial(),
                projection.nombreComercial(), projection.direccionFiscal(), projection.ubigeoFiscal(),
                projection.telefono(), projection.email(), projection.sitioWeb(), projection.monedaFuncional(),
                projection.zonaHoraria(), projection.permiteVentaOnline(), projection.estado(),
                projection.createdAt(), projection.updatedAt());
    }

    public static EstablecimientoResult toResult(EstablecimientoProjection projection) {
        return new EstablecimientoResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.empresaUuid(),
                projection.codigo(), projection.nombre(), projection.tipoEstablecimiento(),
                projection.categoriaRegulatoriaCodigo(), projection.codigoAnexoSunat(),
                projection.codigoDigemid(), projection.direccion(), projection.ubigeo(),
                projection.referencia(), projection.latitud(), projection.longitud(), projection.telefono(),
                projection.email(), projection.esPrincipal(), projection.permiteVentaOnline(),
                projection.permiteDelivery(), projection.perfilOperacion(), projection.zonaHoraria(),
                projection.estadoOperativo(), projection.createdAt(), projection.updatedAt());
    }

    public static AlmacenResult toResult(AlmacenProjection projection) {
        return new AlmacenResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.establecimientoUuid(),
                projection.codigo(), projection.nombre(), projection.tipo(), projection.permiteLotes(),
                projection.permiteVencimiento(), projection.permiteVenta(), projection.permiteDespacho(),
                projection.controlTemperatura(), projection.temperaturaMinC(), projection.temperaturaMaxC(),
                projection.activo(), projection.createdAt(), projection.updatedAt());
    }

    public static TerminalPosResult toResult(TerminalProjection projection) {
        return new TerminalPosResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.establecimientoUuid(),
                projection.codigo(), projection.nombre(), projection.serieBoletaDefecto(),
                projection.serieFacturaDefecto(), projection.numeroSerieEquipo(), projection.hostname(),
                projection.ipEquipo(), projection.impresoraCodigo(), projection.storeEdgeHabilitado(),
                projection.estado(), projection.createdAt(), projection.updatedAt());
    }
}
```

- [ ] **Step 5: Implementar `OrganizacionJdbcReadAdapter`, incluyendo el ensamblado del árbol de estructura corporativa**

```java
package com.softprimesolutions.organizacion.infrastructure.persistence.read.adapter;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.NodoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.mapper.OrganizacionReadMapper;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.repository.OrganizacionJdbcReadRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrganizacionJdbcReadAdapter implements OrganizacionReadPort {

    private final OrganizacionJdbcReadRepository repository;

    public OrganizacionJdbcReadAdapter(OrganizacionJdbcReadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<EmpresaOperadoraResult> findEmpresas(UUID tenantId, String search, int page, int size) {
        var items = repository.findEmpresas(tenantId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countEmpresas(tenantId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmpresaOperadoraResult> findEmpresaById(UUID tenantId, UUID empresaId) {
        return repository.findEmpresaById(tenantId, empresaId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<EstablecimientoResult> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int page, int size) {
        var items = repository.findEstablecimientos(tenantId, empresaId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(
                items, page, size, repository.countEstablecimientos(tenantId, empresaId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstablecimientoResult> findEstablecimientoById(UUID tenantId, UUID establecimientoId) {
        return repository.findEstablecimientoById(tenantId, establecimientoId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<AlmacenResult> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int page, int size) {
        var items = repository.findAlmacenes(tenantId, establecimientoId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countAlmacenes(tenantId, establecimientoId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlmacenResult> findAlmacenById(UUID tenantId, UUID almacenId) {
        return repository.findAlmacenById(tenantId, almacenId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<TerminalPosResult> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int page, int size) {
        var items = repository.findTerminales(tenantId, establecimientoId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countTerminales(tenantId, establecimientoId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerminalPosResult> findTerminalById(UUID tenantId, UUID terminalId) {
        return repository.findTerminalById(tenantId, terminalId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public EstructuraCorporativaResult findEstructuraCorporativa(UUID tenantId) {
        var empresas = repository.findAllEmpresasActivas(tenantId);
        var establecimientos = repository.findAllEstablecimientosActivos(tenantId);
        var almacenes = repository.findAllAlmacenesActivos(tenantId);
        var terminales = repository.findAllTerminalesActivos(tenantId);

        var companies = empresas.stream()
                .map(empresa -> new EmpresaNodoResult(
                        empresa.uuidPublico(), empresa.razonSocial(), empresa.nombreComercial(),
                        empresa.estado(),
                        establecimientos.stream()
                                .filter(est -> est.empresaUuid().equals(empresa.uuidPublico()))
                                .sorted(Comparator.comparing(
                                        com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection::nombre))
                                .map(establecimiento -> toEstablecimientoNodo(establecimiento, almacenes, terminales))
                                .toList()))
                .toList();

        return new EstructuraCorporativaResult(Instant.now(), companies);
    }

    private EstablecimientoNodoResult toEstablecimientoNodo(
            com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection establecimiento,
            List<com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection> almacenes,
            List<com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection> terminales) {
        var warehouses = almacenes.stream()
                .filter(almacen -> almacen.establecimientoUuid().equals(establecimiento.uuidPublico()))
                .sorted(Comparator.comparing(
                        com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection::nombre))
                .map(almacen -> new NodoResult(
                        almacen.uuidPublico(), almacen.codigo(), almacen.nombre(),
                        almacen.activo() ? "ACTIVE" : "INACTIVE"))
                .toList();
        var cashRegisters = terminales.stream()
                .filter(terminal -> terminal.establecimientoUuid().equals(establecimiento.uuidPublico()))
                .sorted(Comparator.comparing(
                        com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection::nombre))
                .map(terminal -> new NodoResult(
                        terminal.uuidPublico(), terminal.codigo(), terminal.nombre(), terminal.estado()))
                .toList();
        return new EstablecimientoNodoResult(
                establecimiento.uuidPublico(), establecimiento.codigo(), establecimiento.nombre(),
                establecimiento.estadoOperativo(), establecimiento.zonaHoraria(), warehouses, cashRegisters);
    }
}
```

**Nota:** los imports totalmente calificados en línea (`com.softprimesolutions...Projection::nombre`) están escritos así en el código de arriba para que quepan en el snippet — al escribir el archivo real, usar imports normales al inicio del archivo para `EstablecimientoProjection`, `AlmacenProjection`, `TerminalProjection` en vez de nombres calificados inline (esto es solo una limitación de formato del plan, no una instrucción de dejar el código así).

- [ ] **Step 6: Escribir el test de integración del adapter de lectura**

Mismo enfoque que Task 6 Step 6: revisar cómo `security` prueba `IamJdbcReadAdapter` (o el repositorio JDBC equivalente) y replicar la configuración de arranque de contexto (H2 + JdbcClient real).

Crear `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/persistence/read/adapter/OrganizacionJdbcReadAdapterTest.java` cubriendo:

- `findEmpresas` respeta paginación, filtro `search` y excluye empresas con `es_activo='0'`.
- `findEmpresaById` devuelve vacío si no existe o pertenece a otro tenant.
- `findEstablecimientos` filtra correctamente por `empresaId` cuando se pasa, y devuelve todos los del tenant cuando `empresaId` es `null`.
- `findEstablecimientoById`, `findAlmacenes`/`findAlmacenById`, `findTerminales`/`findTerminalById` — mismo patrón.
- `findEstructuraCorporativa` devuelve el árbol completo correctamente anidado (empresa → establecimientos → almacenes/terminales), con más de una empresa y más de un establecimiento por empresa, verificando que cada almacén/terminal aparece bajo el establecimiento correcto y no se filtran ni duplican.

Fixtures: insertar datos directamente vía `JdbcClient` de test o scripts SQL de setup, replicando el estilo ya usado en el test de referencia de `security` localizado en el Step 6 de Task 6.

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test --tests "com.softprimesolutions.organizacion.infrastructure.persistence.read.adapter.OrganizacionJdbcReadAdapterTest"`
Expected: PASS tras implementar. Iterar hasta 100% de cobertura de líneas/ramas en `OrganizacionJdbcReadAdapter.java`, `OrganizacionJdbcReadRepository.java` y `OrganizacionReadMapper.java`.

- [ ] **Step 7: Ejecutar el módulo completo con cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test jacocoTestReport`
Expected: PASS. Revisar el reporte agregado — todos los archivos de `infrastructure/persistence/read/` deben llegar a 100%.

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/persistence/read
git add service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/persistence/read
git commit -m "feat(organizacion): agregar infraestructura de lectura JDBC y estructura corporativa"
```

---

## Task 8: Capa API — controllers, DTOs, mapper y `OrganizacionModuleConfiguration`

**Files:**
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/package-info.java` (verificar que solo declara el paquete, sin referencias a lo eliminado en Task 1)
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/mapper/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/PaginaResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/CrearEmpresaOperadoraRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/ActualizarEmpresaOperadoraRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/EmpresaOperadoraResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/CrearEstablecimientoRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/ActualizarEstablecimientoRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/EstablecimientoResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/CrearAlmacenRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/ActualizarAlmacenRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/AlmacenResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/CrearTerminalPosRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/ActualizarTerminalPosRequest.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/TerminalPosResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/NodoResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/EstablecimientoNodoResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/EmpresaNodoResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/response/EstructuraCorporativaResponse.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/mapper/OrganizacionApiMapper.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/OrganizacionControllerSupport.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/EmpresaOperadoraController.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/EstablecimientoController.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/AlmacenController.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/TerminalPosController.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/EstructuraCorporativaController.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/configuration/package-info.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/configuration/OrganizacionModuleConfiguration.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/api/OrganizacionApiIntegrationTest.java`

**Interfaces:**
- Consumes: los 17 casos de uso (Tasks 4-5), `OrganizacionJpaWriteAdapter`/`OrganizacionJdbcReadAdapter` (Tasks 6-7), `IdentifierGenerator`/`ClockPort` de `shared-application`.
- Produces: 5 controllers REST montados en `/api/v1/organizacion/*` y `/api/v1/estructura-corporativa`.

- [ ] **Step 1: Crear los `package-info.java` de la capa API**

```java
// api/controller/package-info.java
package com.softprimesolutions.organizacion.api.controller;
```
```java
// api/dto/package-info.java
package com.softprimesolutions.organizacion.api.dto;
```
```java
// api/dto/request/package-info.java
package com.softprimesolutions.organizacion.api.dto.request;
```
```java
// api/dto/response/package-info.java
package com.softprimesolutions.organizacion.api.dto.response;
```
```java
// api/mapper/package-info.java
package com.softprimesolutions.organizacion.api.mapper;
```
```java
// infrastructure/configuration/package-info.java
package com.softprimesolutions.organizacion.infrastructure.configuration;
```

Verificar que `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/package-info.java` (ya existente, no borrado en Task 1) solo contiene la declaración de paquete y no importa nada de los tipos eliminados; si tiene anotaciones o javadoc referenciando el slice viejo, limpiarlo.

- [ ] **Step 2: Crear `PaginaResponse`**

```java
package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;

public record PaginaResponse<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResponse {
        items = List.copyOf(items);
    }
}
```

- [ ] **Step 3: Crear los DTOs de request/response de Empresa Operadora**

```java
// api/dto/request/CrearEmpresaOperadoraRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearEmpresaOperadoraRequest(
        @NotNull UUID tenantId,
        @NotNull @Size(min = 11, max = 11) String ruc,
        @NotNull @Size(min = 2, max = 300) String razonSocial,
        @Size(max = 300) String nombreComercial,
        @Size(max = 500) String direccionFiscal,
        @Size(max = 6) String ubigeoFiscal,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        @Size(max = 300) String sitioWeb,
        @Size(min = 3, max = 3) String monedaFuncional,
        @Size(max = 80) String zonaHoraria,
        boolean permiteVentaOnline) {
}
```

```java
// api/dto/request/ActualizarEmpresaOperadoraRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEmpresaOperadoraRequest(
        @NotNull @Size(min = 2, max = 300) String razonSocial,
        @Size(max = 300) String nombreComercial,
        @Size(max = 500) String direccionFiscal,
        @Size(max = 6) String ubigeoFiscal,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        @Size(max = 300) String sitioWeb,
        @Size(min = 3, max = 3) String monedaFuncional,
        @Size(max = 80) String zonaHoraria,
        boolean permiteVentaOnline) {
}
```

```java
// api/dto/response/EmpresaOperadoraResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EmpresaOperadoraResponse(
        UUID id,
        UUID tenantId,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 4: Crear los DTOs de request/response de Establecimiento**

```java
// api/dto/request/CrearEstablecimientoRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearEstablecimientoRequest(
        @NotNull UUID empresaId,
        @NotNull @Size(min = 1, max = 40) String codigo,
        @NotNull @Size(min = 2, max = 250) String nombre,
        @NotNull String tipoEstablecimiento,
        @Size(max = 50) String categoriaRegulatoriaCodigo,
        @Size(min = 4, max = 4) String codigoAnexoSunat,
        @Size(max = 10) String codigoDigemid,
        @Size(max = 500) String direccion,
        @Size(max = 6) String ubigeo,
        @Size(max = 300) String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        @NotNull String perfilOperacion,
        @Size(max = 80) String zonaHoraria) {
}
```

```java
// api/dto/request/ActualizarEstablecimientoRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarEstablecimientoRequest(
        @NotNull @Size(min = 2, max = 250) String nombre,
        @NotNull String tipoEstablecimiento,
        @Size(max = 50) String categoriaRegulatoriaCodigo,
        @Size(min = 4, max = 4) String codigoAnexoSunat,
        @Size(max = 10) String codigoDigemid,
        @Size(max = 500) String direccion,
        @Size(max = 6) String ubigeo,
        @Size(max = 300) String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        @NotNull String perfilOperacion,
        @Size(max = 80) String zonaHoraria) {
}
```

```java
// api/dto/response/EstablecimientoResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstablecimientoResponse(
        UUID id,
        UUID tenantId,
        UUID empresaId,
        String codigo,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria,
        String estadoOperativo,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 5: Crear los DTOs de request/response de Almacén y Terminal POS**

```java
// api/dto/request/CrearAlmacenRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearAlmacenRequest(
        @NotNull UUID establecimientoId,
        @NotNull @Size(min = 1, max = 40) String codigo,
        @NotNull @Size(min = 2, max = 250) String nombre,
        @NotNull String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC) {
}
```

```java
// api/dto/request/ActualizarAlmacenRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarAlmacenRequest(
        @NotNull @Size(min = 2, max = 250) String nombre,
        @NotNull String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo) {
}
```

```java
// api/dto/response/AlmacenResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlmacenResponse(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo,
        Instant createdAt,
        Instant updatedAt) {
}
```

```java
// api/dto/request/CrearTerminalPosRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearTerminalPosRequest(
        @NotNull UUID establecimientoId,
        @NotNull @Size(min = 1, max = 40) String codigo,
        @NotNull @Size(min = 2, max = 250) String nombre,
        @Size(max = 10) String serieBoletaDefecto,
        @Size(max = 10) String serieFacturaDefecto,
        @Size(max = 100) String numeroSerieEquipo,
        @Size(max = 150) String hostname,
        String ipEquipo,
        @Size(max = 60) String impresoraCodigo,
        boolean storeEdgeHabilitado) {
}
```

```java
// api/dto/request/ActualizarTerminalPosRequest.java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarTerminalPosRequest(
        @NotNull @Size(min = 2, max = 250) String nombre,
        @Size(max = 10) String serieBoletaDefecto,
        @Size(max = 10) String serieFacturaDefecto,
        @Size(max = 100) String numeroSerieEquipo,
        @Size(max = 150) String hostname,
        String ipEquipo,
        @Size(max = 60) String impresoraCodigo,
        boolean storeEdgeHabilitado,
        @NotNull String estado) {
}
```

```java
// api/dto/response/TerminalPosResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TerminalPosResponse(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 6: Crear los DTOs de respuesta del árbol de estructura corporativa**

```java
// api/dto/response/NodoResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.util.UUID;

public record NodoResponse(UUID id, String code, String name, String status) {
}
```

```java
// api/dto/response/EstablecimientoNodoResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record EstablecimientoNodoResponse(
        UUID id,
        String code,
        String name,
        String status,
        String timeZone,
        List<NodoResponse> warehouses,
        List<NodoResponse> cashRegisters) {

    public EstablecimientoNodoResponse {
        warehouses = List.copyOf(warehouses);
        cashRegisters = List.copyOf(cashRegisters);
    }
}
```

```java
// api/dto/response/EmpresaNodoResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record EmpresaNodoResponse(
        UUID id,
        String legalName,
        String tradeName,
        String status,
        List<EstablecimientoNodoResponse> establishments) {

    public EmpresaNodoResponse {
        establishments = List.copyOf(establishments);
    }
}
```

```java
// api/dto/response/EstructuraCorporativaResponse.java
package com.softprimesolutions.organizacion.api.dto.response;

import java.time.Instant;
import java.util.List;

public record EstructuraCorporativaResponse(Instant asOf, List<EmpresaNodoResponse> companies) {

    public EstructuraCorporativaResponse {
        companies = List.copyOf(companies);
    }
}
```

**Nota crítica de compatibilidad:** los nombres de campo en JSON deben ser exactamente `legalName`, `tradeName`, `status`, `establishments`, `id`, `code`, `name`, `timeZone`, `warehouses`, `cashRegisters`, `asOf`, `companies` — igual a los tipos TypeScript `CompanyStructure`/`EstablishmentStructure`/`OrganizationalNode`/`CorporateStructure` en `frontend/apps/erp-web/src/features/organizacion/api/organization.api.ts`. Jackson serializa records por el nombre del accessor, así que los nombres de los parámetros del record ya deben coincidir tal cual (no hay `@JsonProperty` en ningún DTO existente del proyecto, seguir la misma convención).

- [ ] **Step 7: Implementar `OrganizacionApiMapper`**

```java
package com.softprimesolutions.organizacion.api.mapper;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.response.AlmacenResponse;
import com.softprimesolutions.organizacion.api.dto.response.EmpresaNodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EmpresaOperadoraResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstablecimientoNodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstablecimientoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstructuraCorporativaResponse;
import com.softprimesolutions.organizacion.api.dto.response.NodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.PaginaResponse;
import com.softprimesolutions.organizacion.api.dto.response.TerminalPosResponse;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.NodoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import java.util.UUID;

public final class OrganizacionApiMapper {

    private OrganizacionApiMapper() {
    }

    public static CrearEmpresaOperadoraCommand toCommand(CrearEmpresaOperadoraRequest request) {
        return new CrearEmpresaOperadoraCommand(
                request.tenantId(), request.ruc(), request.razonSocial(), request.nombreComercial(),
                request.direccionFiscal(), request.ubigeoFiscal(), request.telefono(), request.email(),
                request.sitioWeb(), request.monedaFuncional(), request.zonaHoraria(),
                request.permiteVentaOnline());
    }

    public static ActualizarEmpresaOperadoraCommand toCommand(
            UUID empresaId, UUID tenantId, ActualizarEmpresaOperadoraRequest request) {
        return new ActualizarEmpresaOperadoraCommand(
                empresaId, tenantId, request.razonSocial(), request.nombreComercial(),
                request.direccionFiscal(), request.ubigeoFiscal(), request.telefono(), request.email(),
                request.sitioWeb(), request.monedaFuncional(), request.zonaHoraria(),
                request.permiteVentaOnline());
    }

    public static EmpresaOperadoraResponse toResponse(EmpresaOperadoraResult result) {
        return new EmpresaOperadoraResponse(
                result.id(), result.tenantId(), result.ruc(), result.razonSocial(), result.nombreComercial(),
                result.direccionFiscal(), result.ubigeoFiscal(), result.telefono(), result.email(),
                result.sitioWeb(), result.monedaFuncional(), result.zonaHoraria(), result.permiteVentaOnline(),
                result.estado(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<EmpresaOperadoraResponse> toEmpresaPage(PaginaResult<EmpresaOperadoraResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static CrearEstablecimientoCommand toCommand(UUID tenantId, CrearEstablecimientoRequest request) {
        return new CrearEstablecimientoCommand(
                tenantId, request.empresaId(), request.codigo(), request.nombre(),
                request.tipoEstablecimiento(), request.categoriaRegulatoriaCodigo(),
                request.codigoAnexoSunat(), request.codigoDigemid(), request.direccion(), request.ubigeo(),
                request.referencia(), request.latitud(), request.longitud(), request.telefono(),
                request.email(), request.esPrincipal(), request.permiteVentaOnline(),
                request.permiteDelivery(), request.perfilOperacion(), request.zonaHoraria());
    }

    public static ActualizarEstablecimientoCommand toCommand(
            UUID establecimientoId, UUID tenantId, ActualizarEstablecimientoRequest request) {
        return new ActualizarEstablecimientoCommand(
                establecimientoId, tenantId, request.nombre(), request.tipoEstablecimiento(),
                request.categoriaRegulatoriaCodigo(), request.codigoAnexoSunat(), request.codigoDigemid(),
                request.direccion(), request.ubigeo(), request.referencia(), request.latitud(),
                request.longitud(), request.telefono(), request.email(), request.esPrincipal(),
                request.permiteVentaOnline(), request.permiteDelivery(), request.perfilOperacion(),
                request.zonaHoraria());
    }

    public static EstablecimientoResponse toResponse(EstablecimientoResult result) {
        return new EstablecimientoResponse(
                result.id(), result.tenantId(), result.empresaId(), result.codigo(), result.nombre(),
                result.tipoEstablecimiento(), result.categoriaRegulatoriaCodigo(), result.codigoAnexoSunat(),
                result.codigoDigemid(), result.direccion(), result.ubigeo(), result.referencia(),
                result.latitud(), result.longitud(), result.telefono(), result.email(), result.esPrincipal(),
                result.permiteVentaOnline(), result.permiteDelivery(), result.perfilOperacion(),
                result.zonaHoraria(), result.estadoOperativo(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<EstablecimientoResponse> toEstablecimientoPage(
            PaginaResult<EstablecimientoResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static CrearAlmacenCommand toCommand(UUID tenantId, CrearAlmacenRequest request) {
        return new CrearAlmacenCommand(
                tenantId, request.establecimientoId(), request.codigo(), request.nombre(), request.tipo(),
                request.permiteLotes(), request.permiteVencimiento(), request.permiteVenta(),
                request.permiteDespacho(), request.controlTemperatura(), request.temperaturaMinC(),
                request.temperaturaMaxC());
    }

    public static ActualizarAlmacenCommand toCommand(
            UUID almacenId, UUID tenantId, ActualizarAlmacenRequest request) {
        return new ActualizarAlmacenCommand(
                almacenId, tenantId, request.nombre(), request.tipo(), request.permiteLotes(),
                request.permiteVencimiento(), request.permiteVenta(), request.permiteDespacho(),
                request.controlTemperatura(), request.temperaturaMinC(), request.temperaturaMaxC(),
                request.activo());
    }

    public static AlmacenResponse toResponse(AlmacenResult result) {
        return new AlmacenResponse(
                result.id(), result.tenantId(), result.establecimientoId(), result.codigo(), result.nombre(),
                result.tipo(), result.permiteLotes(), result.permiteVencimiento(), result.permiteVenta(),
                result.permiteDespacho(), result.controlTemperatura(), result.temperaturaMinC(),
                result.temperaturaMaxC(), result.activo(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<AlmacenResponse> toAlmacenPage(PaginaResult<AlmacenResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static CrearTerminalPosCommand toCommand(UUID tenantId, CrearTerminalPosRequest request) {
        return new CrearTerminalPosCommand(
                tenantId, request.establecimientoId(), request.codigo(), request.nombre(),
                request.serieBoletaDefecto(), request.serieFacturaDefecto(), request.numeroSerieEquipo(),
                request.hostname(), request.ipEquipo(), request.impresoraCodigo(),
                request.storeEdgeHabilitado());
    }

    public static ActualizarTerminalPosCommand toCommand(
            UUID terminalId, UUID tenantId, ActualizarTerminalPosRequest request) {
        return new ActualizarTerminalPosCommand(
                terminalId, tenantId, request.nombre(), request.serieBoletaDefecto(),
                request.serieFacturaDefecto(), request.numeroSerieEquipo(), request.hostname(),
                request.ipEquipo(), request.impresoraCodigo(), request.storeEdgeHabilitado(),
                request.estado());
    }

    public static TerminalPosResponse toResponse(TerminalPosResult result) {
        return new TerminalPosResponse(
                result.id(), result.tenantId(), result.establecimientoId(), result.codigo(), result.nombre(),
                result.serieBoletaDefecto(), result.serieFacturaDefecto(), result.numeroSerieEquipo(),
                result.hostname(), result.ipEquipo(), result.impresoraCodigo(), result.storeEdgeHabilitado(),
                result.estado(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<TerminalPosResponse> toTerminalPage(PaginaResult<TerminalPosResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static EstructuraCorporativaResponse toResponse(EstructuraCorporativaResult result) {
        return new EstructuraCorporativaResponse(
                result.asOf(), result.companies().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static EmpresaNodoResponse toResponse(EmpresaNodoResult result) {
        return new EmpresaNodoResponse(
                result.id(), result.legalName(), result.tradeName(), result.status(),
                result.establishments().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static EstablecimientoNodoResponse toResponse(EstablecimientoNodoResult result) {
        return new EstablecimientoNodoResponse(
                result.id(), result.code(), result.name(), result.status(), result.timeZone(),
                result.warehouses().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.cashRegisters().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static NodoResponse toResponse(NodoResult result) {
        return new NodoResponse(result.id(), result.code(), result.name(), result.status());
    }
}
```

- [ ] **Step 8: Crear `OrganizacionControllerSupport`**

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

final class OrganizacionControllerSupport {

    private OrganizacionControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
```

- [ ] **Step 9: Implementar `EmpresaOperadoraController`**

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/organizacion/empresas")
public class EmpresaOperadoraController {

    private final CrearEmpresaOperadoraUseCase createEmpresa;
    private final ActualizarEmpresaOperadoraUseCase updateEmpresa;
    private final ListarEmpresasUseCase listEmpresas;
    private final ObtenerEmpresaUseCase getEmpresa;

    public EmpresaOperadoraController(
            CrearEmpresaOperadoraUseCase createEmpresa,
            ActualizarEmpresaOperadoraUseCase updateEmpresa,
            ListarEmpresasUseCase listEmpresas,
            ObtenerEmpresaUseCase getEmpresa) {
        this.createEmpresa = createEmpresa;
        this.updateEmpresa = updateEmpresa;
        this.listEmpresas = listEmpresas;
        this.getEmpresa = getEmpresa;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearEmpresaOperadoraRequest request) {
        return createEmpresa.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/organizacion/empresas/" + result.id()))
                        .body(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listEmpresas.execute(new ListarEmpresasQuery(tenantId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toEmpresaPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID empresaId, @RequestParam UUID tenantId) {
        return getEmpresa.execute(new ObtenerEmpresaQuery(tenantId, empresaId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID empresaId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody ActualizarEmpresaOperadoraRequest request) {
        return updateEmpresa.execute(OrganizacionApiMapper.toCommand(empresaId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
```

- [ ] **Step 10: Implementar `EstablecimientoController`, `AlmacenController`, `TerminalPosController`**

Mismo patrón exacto de `EmpresaOperadoraController`, con estas diferencias:

**`EstablecimientoController`** (`@RequestMapping("/api/v1/organizacion/establecimientos")`): el método `list` agrega `@RequestParam(required = false) UUID empresaId` antes de `search`, y lo pasa como segundo argumento a `new ListarEstablecimientosQuery(tenantId, empresaId, search, page, size)`. Permisos: `organizacion.establecimientos.consultar`/`.gestionar`.

**`AlmacenController`** (`@RequestMapping("/api/v1/organizacion/almacenes")`): el método `list` agrega `@RequestParam(required = false) UUID establecimientoId`. Permisos: `organizacion.almacenes.consultar`/`.gestionar`.

**`TerminalPosController`** (`@RequestMapping("/api/v1/organizacion/terminales-pos")`): mismo patrón que `AlmacenController`. Permisos: `organizacion.terminales-pos.consultar`/`.gestionar`.

Escribir los 3 archivos completos siguiendo la estructura de `EmpresaOperadoraController` (constructor con los 4 casos de uso correspondientes, `create`/`list`/`getById`/`update`), ajustando únicamente: la ruta base, el nombre del path variable (`establecimientoId`/`almacenId`/`terminalId`), los tipos de Command/Query/Response/UseCase, y los códigos de permiso.

- [ ] **Step 11: Implementar `EstructuraCorporativaController`**

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estructura-corporativa")
public class EstructuraCorporativaController {

    private final ObtenerEstructuraCorporativaUseCase getStructure;

    public EstructuraCorporativaController(ObtenerEstructuraCorporativaUseCase getStructure) {
        this.getStructure = getStructure;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> get(@RequestParam UUID tenantId) {
        return getStructure.execute(new ObtenerEstructuraCorporativaQuery(tenantId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
```

**Nota de compatibilidad con el frontend:** `AsignacionRolForm.tsx` usa `corporateStructureQuery` de `frontend/apps/erp-web/src/features/organizacion/api/organization.api.ts`, que llama `apiClient.get('/estructura-corporativa')` (el cliente ya agrega el prefijo `/api/v1`). Verificar en `frontend/apps/erp-web/src/app/api.ts` (o donde esté configurado `apiClient`) cuál es exactamente el `baseURL` usado, para confirmar que la ruta final coincide con `/api/v1/estructura-corporativa` sin parámetro `tenantId` en query — si el frontend no envía `tenantId` como query param (revisar `organization.api.ts`, que hoy NO lo envía), este controller necesita resolver el tenant desde el `Authentication`/JWT en vez de `@RequestParam`, igual que hace `security` en otros endpoints donde el tenant se infiere del token. Si esto ocurre, ajustar la firma del método `get` a `get(Authentication authentication)` y extraer el tenant desde ahí (revisar `LocalJwtAuthenticationConverter`/`SessionAwareJwtDecoder` para el claim exacto de tenant) — este es un punto de verificación obligatorio antes de dar la tarea por completa, no una suposición a dejar sin resolver.

- [ ] **Step 12: Implementar `OrganizacionModuleConfiguration`**

```java
package com.softprimesolutions.organizacion.infrastructure.configuration;

import com.softprimesolutions.organizacion.application.port.in.ActualizarAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarAlmacenesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEstablecimientosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarTerminalesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerTerminalUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarEmpresaOperadoraHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarTerminalPosHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearEmpresaOperadoraHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearTerminalPosHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarAlmacenesHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarEmpresasHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarEstablecimientosHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarTerminalesHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEmpresaHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstructuraCorporativaHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerTerminalHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizacionModuleConfiguration {

    @Bean
    Clock organizacionClock() {
        return Clock.systemUTC();
    }

    @Bean
    ClockPort organizacionClockPort(Clock organizacionClock) {
        return () -> Instant.now(organizacionClock);
    }

    @Bean
    IdentifierGenerator organizacionIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CrearEmpresaOperadoraUseCase crearEmpresaOperadoraUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearEmpresaOperadoraHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarEmpresaOperadoraUseCase actualizarEmpresaOperadoraUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarEmpresaOperadoraHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarEmpresasUseCase listarEmpresasUseCase(OrganizacionReadPort readPort) {
        return new ListarEmpresasHandler(readPort);
    }

    @Bean
    ObtenerEmpresaUseCase obtenerEmpresaUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEmpresaHandler(readPort);
    }

    @Bean
    CrearEstablecimientoUseCase crearEstablecimientoUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearEstablecimientoHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarEstablecimientoUseCase actualizarEstablecimientoUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarEstablecimientoHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarEstablecimientosUseCase listarEstablecimientosUseCase(OrganizacionReadPort readPort) {
        return new ListarEstablecimientosHandler(readPort);
    }

    @Bean
    ObtenerEstablecimientoUseCase obtenerEstablecimientoUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEstablecimientoHandler(readPort);
    }

    @Bean
    CrearAlmacenUseCase crearAlmacenUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearAlmacenHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarAlmacenUseCase actualizarAlmacenUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarAlmacenHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarAlmacenesUseCase listarAlmacenesUseCase(OrganizacionReadPort readPort) {
        return new ListarAlmacenesHandler(readPort);
    }

    @Bean
    ObtenerAlmacenUseCase obtenerAlmacenUseCase(OrganizacionReadPort readPort) {
        return new ObtenerAlmacenHandler(readPort);
    }

    @Bean
    CrearTerminalPosUseCase crearTerminalPosUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearTerminalPosHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarTerminalPosUseCase actualizarTerminalPosUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarTerminalPosHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarTerminalesUseCase listarTerminalesUseCase(OrganizacionReadPort readPort) {
        return new ListarTerminalesHandler(readPort);
    }

    @Bean
    ObtenerTerminalUseCase obtenerTerminalUseCase(OrganizacionReadPort readPort) {
        return new ObtenerTerminalHandler(readPort);
    }

    @Bean
    ObtenerEstructuraCorporativaUseCase obtenerEstructuraCorporativaUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEstructuraCorporativaHandler(readPort);
    }
}
```

**Nota:** los handlers `Actualizar{X}Handler` referenciados aquí (`ActualizarAlmacenHandler`, `ActualizarTerminalPosHandler`) reciben `(OrganizacionReadPort, OrganizacionWritePort, ClockPort)` — confirmar que las firmas de constructor escritas en Task 5 Step 3-4 (Almacén/TerminalPos) usan exactamente estos 3 parámetros en ese orden, igual que `ActualizarEmpresaOperadoraHandler` de Task 4 Step 8; si algún handler de Task 5 quedó con una firma distinta, ajustar aquí para que compile, o corregir el handler para mantener consistencia (preferir corregir el handler, no esta configuración, ya que la firma estándar del proyecto es `(readPort, writePort, clock)` para todo `Actualizar*Handler`).

- [ ] **Step 13: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:compileJava`
Expected: BUILD SUCCESSFUL. Corregir cualquier import faltante o firma inconsistente encontrada.

- [ ] **Step 14: Escribir el test de integración HTTP del módulo**

Antes de escribir este test, leer `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/api/IamApiIntegrationTest.java` completo (citado en `CLAUDE.md` como referencia) para replicar exactamente su patrón de arranque (`@SpringBootTest(webEnvironment = RANDOM_PORT)` o `@AutoConfigureMockMvc`, perfil `test`, cómo se autentica una petición real vía login en vez de inyectar authorities con `SecurityMockMvcRequestPostProcessors.user(...)` — el CLAUDE.md advierte explícitamente que probar autorización real vía login es el patrón correcto, no un atajo).

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/organizacion/api/OrganizacionApiIntegrationTest.java` (ubicado en `bootstrap-app` porque ahí vive el contexto Spring completo, igual que `IamApiIntegrationTest`) cubriendo al menos:

- Login real (usuario de prueba con permisos `organizacion.empresas.gestionar`/`.consultar`) → `POST /api/v1/organizacion/empresas` crea una empresa y devuelve 201 con `Location` header.
- `POST /api/v1/organizacion/empresas` con RUC duplicado devuelve 409.
- `GET /api/v1/organizacion/empresas?tenantId=...` devuelve la página con la empresa creada.
- `POST /api/v1/organizacion/establecimientos` con la empresa creada devuelve 201.
- `POST /api/v1/organizacion/establecimientos` con `empresaId` inexistente devuelve 404.
- `POST /api/v1/organizacion/almacenes` y `POST /api/v1/organizacion/terminales-pos` sobre el establecimiento creado devuelven 201.
- `GET /api/v1/estructura-corporativa?tenantId=...` (o sin el parámetro, según lo resuelto en el Step 11) devuelve el árbol completo con la empresa, establecimiento, almacén y terminal recién creados anidados correctamente.
- Una petición sin el permiso `organizacion.empresas.gestionar` a `POST /api/v1/organizacion/empresas` devuelve 403.

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.organizacion.api.OrganizacionApiIntegrationTest"`
Expected: PASS tras implementar. Esto requiere que la migración `V025` (Task 9) ya exista con los permisos `organizacion.*` sembrados, así que si Task 9 aún no se ejecutó, este test fallará por falta de permisos — coordinar el orden real de ejecución con quien implemente (recomendado: completar Task 9 antes de este Step si el test falla por esa causa).

- [ ] **Step 15: Ejecutar el build completo del módulo y del bootstrap-app con cobertura**

Run: `cd service-botica && .\gradlew.bat :modules:organizacion:test :bootstrap-app:test jacocoTestReport`
Expected: PASS, 100% cobertura en todos los archivos nuevos de `api/` e `infrastructure/configuration/`.

- [ ] **Step 16: Commit**

```bash
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api
git add service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/configuration
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/organizacion
git commit -m "feat(organizacion): agregar controllers REST, DTOs de API y configuracion del modulo"
```

---

## Task 9: Migración de permisos RBAC y verificación end-to-end

**Files:**
- Create: `service-botica/bootstrap-app/src/main/resources/db/migration/V025__seed_organizacion_permissions.sql`

**Interfaces:**
- Consumes: `sch_seguridad.modulo_sistema` código `'ORGANIZACION'` (ya sembrado en `V016__navegacion_dinamica_rbac.sql`, no se crea aquí).
- Produces: 8 filas en `sch_seguridad.permiso`, consumidas por los `@PreAuthorize` de Task 8 y asignables a roles vía el flujo ya existente de `AsignacionRolForm`/`RolController`.

- [ ] **Step 1: Confirmar el siguiente número de migración libre**

Run: `ls service-botica/bootstrap-app/src/main/resources/db/migration | sort -V | tail -5`
Expected: la más reciente es `V024__ajustar_longitud_tipo_documento.sql` (o superior si otro trabajo ya avanzó desde entonces — en ese caso, usar el siguiente número real, no asumir V025 a ciegas).

- [ ] **Step 2: Crear la migración de permisos**

Crear `service-botica/bootstrap-app/src/main/resources/db/migration/V025__seed_organizacion_permissions.sql` (ajustar el número si el Step 1 encontró uno distinto):

```sql
INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('organizacion.empresas.consultar', 'EMPRESA_OPERADORA', 'CONSULTAR', 'Consultar empresas operadoras',
     'Permite consultar las empresas operadoras del tenant.', FALSE),
    ('organizacion.empresas.gestionar', 'EMPRESA_OPERADORA', 'GESTIONAR', 'Gestionar empresas operadoras',
     'Permite crear, editar y cambiar el estado de empresas operadoras.', TRUE),
    ('organizacion.establecimientos.consultar', 'ESTABLECIMIENTO', 'CONSULTAR', 'Consultar establecimientos',
     'Permite consultar los establecimientos farmacéuticos del tenant.', FALSE),
    ('organizacion.establecimientos.gestionar', 'ESTABLECIMIENTO', 'GESTIONAR', 'Gestionar establecimientos',
     'Permite crear, editar y cambiar el estado de establecimientos farmacéuticos.', TRUE),
    ('organizacion.almacenes.consultar', 'ALMACEN', 'CONSULTAR', 'Consultar almacenes',
     'Permite consultar los almacenes del tenant.', FALSE),
    ('organizacion.almacenes.gestionar', 'ALMACEN', 'GESTIONAR', 'Gestionar almacenes',
     'Permite crear, editar y cambiar el estado de almacenes.', TRUE),
    ('organizacion.terminales-pos.consultar', 'TERMINAL_POS', 'CONSULTAR', 'Consultar terminales POS',
     'Permite consultar los terminales de punto de venta del tenant.', FALSE),
    ('organizacion.terminales-pos.gestionar', 'TERMINAL_POS', 'GESTIONAR', 'Gestionar terminales POS',
     'Permite crear, editar y cambiar el estado de terminales de punto de venta.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'ORGANIZACION'
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
```

- [ ] **Step 3: Reconstruir y levantar el entorno Docker/Postgres local para aplicar la migración**

Run:
```bash
cd service-botica
docker compose build app
docker compose up -d
```
Expected: build exitoso, contenedores `erp-botica-app-1` y `erp-botica-postgres-1` corriendo. Revisar logs (`docker compose logs app --tail=100`) para confirmar que Flyway aplicó `V025` sin errores y que el contexto Spring Modulith arrancó (`ApplicationModules.verify()` no falla).

- [ ] **Step 4: Verificar en base de datos que los 8 permisos quedaron sembrados**

Run (ajustar credenciales/puerto según `docker-compose.yml` del proyecto):
```bash
docker compose exec postgres psql -U postgres -d erp_botica -c "SELECT codigo, estado FROM sch_seguridad.permiso WHERE codigo LIKE 'organizacion.%' ORDER BY codigo;"
```
Expected: 8 filas, todas con `estado = 'ACTIVO'`.

- [ ] **Step 5: Ejecutar el test de integración HTTP de Task 8 (ahora que los permisos existen) y el build completo del backend**

Run: `cd service-botica && .\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL — incluye `ArchUnit`, `ApplicationModules.verify()`, todos los tests unitarios/integración del módulo `organizacion` y de `OrganizacionApiIntegrationTest`, con 100% de cobertura JaCoCo en todo archivo nuevo.

- [ ] **Step 6: Verificación end-to-end en navegador — asignar un rol con ámbito ESTABLECIMIENTO usando datos reales**

Con el backend levantado (Step 3) y el frontend en modo NO-mock apuntando a él (`pnpm dev` con `VITE_*` configurado para desactivar MSW, o el flujo ya usado en la sesión para probar contra backend real):

1. Loguearse como un usuario con permisos de organización y de seguridad.
2. Crear una empresa operadora nueva vía `POST /api/v1/organizacion/empresas` (con `curl`/Postman/Insomnia, ya que el frontend aún no tiene UI propia para esto — fuera de alcance de este plan) con un RUC de prueba válido.
3. Crear un establecimiento para esa empresa vía `POST /api/v1/organizacion/establecimientos`.
4. Crear un almacén y un terminal para ese establecimiento.
5. En el navegador, ir al formulario de "Nuevo usuario" (`/seguridad/usuarios/nuevo`) o al diálogo "Asignar rol" de un usuario existente, activar "Asignar rol ahora", seleccionar ámbito `ESTABLECIMIENTO`, y confirmar que la empresa y el establecimiento recién creados por API aparecen en los selects de `AsignacionRolForm` (esto prueba que `GET /api/v1/estructura-corporativa` devuelve datos reales con el shape correcto, reemplazando lo que antes servía MSW).
6. Completar la asignación de rol y confirmar que no hay errores de red ni de deserialización en la consola del navegador.

Si el paso 5 falla porque el frontend sigue apuntando a MSW, revisar `frontend/.env.development` (o el archivo de configuración de modo mock del proyecto) y desactivarlo temporalmente para esta verificación, sin commitear ese cambio de configuración local.

- [ ] **Step 7: Commit**

```bash
git add service-botica/bootstrap-app/src/main/resources/db/migration/V025__seed_organizacion_permissions.sql
git commit -m "feat(organizacion): sembrar permisos RBAC del modulo organizacion"
```

- [ ] **Step 8: Actualizar `CLAUDE.md`**

Editar la sección "Estado real del proyecto" de `CLAUDE.md` para reflejar que `organizacion` ahora expone CRUD REST real para Empresa Operadora, Establecimiento, Almacén y Terminal POS (siguiendo el mismo estilo de descripción ya usado ahí para `catalogo`), y que `GET /api/v1/estructura-corporativa` ya no depende de datos mock. No modificar ninguna otra sección del archivo.

```bash
git add CLAUDE.md
git commit -m "docs: actualizar estado real del proyecto tras implementar API de organizacion"
```
