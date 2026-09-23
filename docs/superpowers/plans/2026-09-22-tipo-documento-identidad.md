# TipoDocumentoIdentidad Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer CRUD REST completo (`/api/v1/catalogo/tipos-documento-identidad`) para el catálogo SUNAT 06 `sch_catalogo.tipo_documento_identidad` (V018), siguiendo el patrón arquitectónico de los catálogos de soporte ya existentes en el módulo `catalogo`.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS + Result Pattern, dentro del módulo `service-botica/modules/catalogo`. `TipoDocumentoIdentidad` es un VO de dominio inmutable (sin `AggregateRoot`, sin `TenantId` — catálogo global), persistido vía JPA en `CatalogoSoporteJpaWriteAdapter` (ampliando el `CatalogoSoportePort` ya compartido por `CondicionVenta`/`FormaFarmaceutica`/etc.) y leído vía JDBC en `CatalogoJdbcReadAdapter`/`CatalogoJdbcReadRepository` (ampliando `CatalogoReadPort`). A diferencia de `CondicionVenta` (que tiene columna `estado VARCHAR(20)`), la tabla `tipo_documento_identidad` solo tiene `es_activo CHAR(1)` ('0'/'1') — el mapeo booleano↔`EstadoCatalogoSoporte` sigue el patrón ya usado para `rubro_comercial` (ver `CatalogoJdbcReadRepository.java:216-260`, filtro `r.es_activo = CASE WHEN :estado = 'ACTIVO' THEN '1' ELSE '0' END` y mapeo `"1".equals(rs.getString("es_activo")) ? "ACTIVO" : "INACTIVO"`).

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA (escritura), `JdbcClient`/`JdbcTemplate` vía `CatalogoJdbcReadRepository` (lectura), JUnit 5, sin Spring context en los tests (todos usan fakes/stubs en memoria — no hay tests de integración JPA en este módulo).

## Global Constraints

- Java 25 / Spring Boot 4.1 / Gradle 9.5.1, toolchain gestionado por Gradle.
- `sch_catalogo.tipo_documento_identidad` (migración V018, ya aplicada, no se modifica): PK `codigo VARCHAR(2)` con `CHECK (codigo ~ '^[0-9A-Z]$')`, `sigla VARCHAR(30) NOT NULL`, `denominacion VARCHAR(200) NOT NULL`, `max SMALLINT NULL CHECK (max IS NULL OR max > 0)`, `min SMALLINT NULL CHECK (min IS NULL OR min > 0)` con `CHECK (min IS NULL OR max IS NULL OR min <= max)`, `es_activo CHAR(1) NOT NULL DEFAULT '1'`. Sin `tenant_id`, sin columnas de auditoría (`created_by`/`created_at`/`updated_by`/`updated_at`), sin columna `estado`.
- Todo commit debe compilar y pasar tests del módulo `catalogo` antes de continuar a la siguiente tarea: `cd service-botica && .\gradlew.bat :modules:catalogo:test --warning-mode all` (Windows PowerShell) tras cada tarea con cambios de código+test. Verificación final con `check` completo al cerrar el plan.
- No se agregan nuevas authorities RBAC — se reutilizan `catalogo.soporte.gestionar` / `catalogo.soporte.consultar`, ya usadas por `CondicionVentaController`.
- No se pagina el listado — mismo criterio que los demás catálogos de soporte (`CatalogoReadPort.findCondicionesVenta` retorna `List`, no `PaginaResult`).
- Ampliar `CatalogoSoportePort` y `CatalogoReadPort` (interfaces) rompe todos sus implementadores existentes (10 en total: 1 en `main`, 9 en `test`) hasta que se les agreguen los métodos nuevos — cada tarea que amplía una interfaz debe actualizar TODOS sus implementadores en el mismo paso, antes de compilar.

---

### Task 1: Dominio — `TipoDocumentoIdentidad`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java`

**Interfaces:**
- Consumes: `EstadoCatalogoSoporte` (ya existe, `com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte`, valores `ACTIVO`/`INACTIVO`), `ErrorDetail`/`Result` de `shared-kernel`.
- Produces: `TipoDocumentoIdentidad.create(String codigo, String sigla, String denominacion, Integer max, Integer min)` → `Result<TipoDocumentoIdentidad, ErrorDetail>`; `TipoDocumentoIdentidad.restore(String codigo, String sigla, String denominacion, Integer max, Integer min, EstadoCatalogoSoporte estado)` → `TipoDocumentoIdentidad`; getters `codigo()`, `sigla()`, `denominacion()`, `max()`, `min()`, `estado()`. Usado por Task 3 (handlers), Task 5 (write adapter/mapper), Task 8 (test de dominio ya cubierto aquí).

- [ ] **Step 1: Escribir el test que falla para `TipoDocumentoIdentidad`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TipoDocumentoIdentidadTest {

    @Test
    void createsAndNormalizesAValidTipoDocumentoIdentidad() {
        var result = TipoDocumentoIdentidad.create("  1  ", "  DNI  ", "  Documento Nacional de Identidad  ", 8, 8);

        assertTrue(result.isSuccess());
        var tipo = result.getOrElse(error -> null);
        assertEquals("1", tipo.codigo());
        assertEquals("DNI", tipo.sigla());
        assertEquals("Documento Nacional de Identidad", tipo.denominacion());
        assertEquals(8, tipo.max());
        assertEquals(8, tipo.min());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, tipo.estado());
    }

    @Test
    void normalizesCodigoToUpperCase() {
        var result = TipoDocumentoIdentidad.create("a", "CE", "Carnet de extranjería", null, null);

        assertTrue(result.isSuccess());
        assertEquals("A", result.getOrElse(error -> null).codigo());
    }

    @Test
    void acceptsNullMaxAndMin() {
        var result = TipoDocumentoIdentidad.create("7", "PAS", "Pasaporte", null, null);

        assertTrue(result.isSuccess());
        var tipo = result.getOrElse(error -> null);
        assertEquals(null, tipo.max());
        assertEquals(null, tipo.min());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = TipoDocumentoIdentidad.create(null, "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsACodigoLongerThanOneCharacter() {
        var result = TipoDocumentoIdentidad.create("AB", "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsACodigoWithLowercaseLetterAfterTrimOnly() {
        var result = TipoDocumentoIdentidad.create("-", "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAMissingSigla() {
        var result = TipoDocumentoIdentidad.create("1", "  ", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsASiglaLongerThan30Characters() {
        var result = TipoDocumentoIdentidad.create("1", "A".repeat(31), "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "D", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationLongerThan200Characters() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "D".repeat(201), 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAZeroMax() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", 0, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsANegativeMin() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", null, -1);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAMinGreaterThanMax() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", 5, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void restoreDoesNotValidate() {
        var tipo = TipoDocumentoIdentidad.restore("0", "DTSR", "DOC.TRIB.NO.DOM.SIN.RUC", null, null,
                EstadoCatalogoSoporte.INACTIVO);

        assertEquals("0", tipo.codigo());
        assertEquals(EstadoCatalogoSoporte.INACTIVO, tipo.estado());
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla (no compila: `TipoDocumentoIdentidad` no existe)**

Ejecutar desde `service-botica/`:

```
.\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidadTest" --warning-mode all
```

Esperado: FALLA de compilación — `cannot find symbol: class TipoDocumentoIdentidad`.

- [ ] **Step 3: Implementar `TipoDocumentoIdentidad`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Catálogo SUNAT 06: código de tipo de documento de identidad. Catálogo global, sin tenant. */
public final class TipoDocumentoIdentidad {

    private static final Pattern CODIGO_PATTERN = Pattern.compile("^[0-9A-Z]$");
    private static final int SIGLA_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;

    private final String codigo;
    private final String sigla;
    private final String denominacion;
    private final Integer max;
    private final Integer min;
    private final EstadoCatalogoSoporte estado;

    private TipoDocumentoIdentidad(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.denominacion = denominacion;
        this.max = max;
        this.min = min;
        this.estado = estado;
    }

    public static Result<TipoDocumentoIdentidad, ErrorDetail> create(
            String codigo, String sigla, String denominacion, Integer max, Integer min) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || !CODIGO_PATTERN.matcher(normalizedCodigo).matches()) {
            return invalid("codigo", "El código debe ser un único carácter alfanumérico en mayúscula.");
        }

        var normalizedSigla = normalizeNullable(sigla);
        if (normalizedSigla == null || normalizedSigla.length() > SIGLA_MAX_LENGTH) {
            return invalid("sigla", "La sigla es obligatoria y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        if (max != null && max <= 0) {
            return invalid("max", "El máximo debe ser mayor a 0.");
        }

        if (min != null && min <= 0) {
            return invalid("min", "El mínimo debe ser mayor a 0.");
        }

        if (min != null && max != null && min > max) {
            return invalid("min", "El mínimo no puede ser mayor al máximo.");
        }

        return Result.success(new TipoDocumentoIdentidad(
                normalizedCodigo, normalizedSigla, normalizedDenominacion, max, min, EstadoCatalogoSoporte.ACTIVO));
    }

    public static TipoDocumentoIdentidad restore(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoCatalogoSoporte estado) {
        return new TipoDocumentoIdentidad(codigo, sigla, denominacion, max, min, estado);
    }

    private static Result<TipoDocumentoIdentidad, ErrorDetail> invalid(String field, String message) {
        return Result.failure(
                new ErrorDetail("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        var normalized = normalizeSpaces(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public String codigo() { return codigo; }
    public String sigla() { return sigla; }
    public String denominacion() { return denominacion; }
    public Integer max() { return max; }
    public Integer min() { return min; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 4: Ejecutar el test y verificar que pasa**

```
.\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidadTest" --warning-mode all
```

Esperado: BUILD SUCCESSFUL, 13 tests pasan.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java
git commit -m "feat(catalogo): agregar agregado de dominio TipoDocumentoIdentidad"
```

---

### Task 2: DTOs y puertos in de aplicación

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearTipoDocumentoIdentidadCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarTipoDocumentoIdentidadCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearTipoDocumentoIdentidadUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarTipoDocumentoIdentidadUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java`

**Interfaces:**
- Consumes: `Command<R>`/`Query<R>` de `com.softprimesolutions.shared.application.cqrs`; `Result`/`ApplicationError` de `shared-kernel`/`shared-application`.
- Produces: `CrearTipoDocumentoIdentidadCommand(String codigo, String sigla, String denominacion, Integer max, Integer min)`; `ActualizarTipoDocumentoIdentidadCommand(String codigo, String sigla, String denominacion, Integer max, Integer min)`; `ConsultarTipoDocumentoIdentidadQuery(String codigo)`; `ListarTiposDocumentoIdentidadQuery(String estado)`; `TipoDocumentoIdentidadResult(String codigo, String sigla, String denominacion, Integer max, Integer min, String estado)`; las 4 interfaces `@FunctionalInterface` con método `execute(...)`. Usado por Task 3 (handlers), Task 4 (control service — no aplica aquí), Task 6 (API mapper/controller), Task 7 (wiring).

No hay test propio en esta tarea (son DTOs/interfaces sin lógica); se verifican indirectamente en Task 3.

- [ ] **Step 1: Crear `TipoDocumentoIdentidadResult`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record TipoDocumentoIdentidadResult(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
```

- [ ] **Step 2: Crear `CrearTipoDocumentoIdentidadCommand` y `ActualizarTipoDocumentoIdentidadCommand`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearTipoDocumentoIdentidadCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearTipoDocumentoIdentidadCommand(
        String codigo, String sigla, String denominacion, Integer max, Integer min)
        implements Command<TipoDocumentoIdentidadResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarTipoDocumentoIdentidadCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarTipoDocumentoIdentidadCommand(
        String codigo, String sigla, String denominacion, Integer max, Integer min)
        implements Command<TipoDocumentoIdentidadResult> {
}
```

- [ ] **Step 3: Crear `ConsultarTipoDocumentoIdentidadQuery` y `ListarTiposDocumentoIdentidadQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarTipoDocumentoIdentidadQuery(String codigo) implements Query<TipoDocumentoIdentidadResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarTiposDocumentoIdentidadQuery(String estado) implements Query<List<TipoDocumentoIdentidadResult>> {
}
```

- [ ] **Step 4: Crear los 4 puertos in**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearTipoDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(CrearTipoDocumentoIdentidadCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarTipoDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ActualizarTipoDocumentoIdentidadCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ConsultarTipoDocumentoIdentidadQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarTiposDocumentoIdentidadUseCase {
    Result<List<TipoDocumentoIdentidadResult>, ApplicationError> execute(ListarTiposDocumentoIdentidadQuery query);
}
```

- [ ] **Step 5: Compilar el módulo**

```
.\gradlew.bat :modules:catalogo:compileJava --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearTipoDocumentoIdentidadCommand.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarTipoDocumentoIdentidadCommand.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearTipoDocumentoIdentidadUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarTipoDocumentoIdentidadUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java
git commit -m "feat(catalogo): agregar DTOs y puertos in de TipoDocumentoIdentidad"
```

---

### Task 3: Ampliar `CatalogoSoportePort` y `CatalogoReadPort` + handlers

Esta tarea amplía dos interfaces compartidas por 10 implementadores existentes (1 en `main`, 9 en `test`). Todos deben actualizarse en el mismo paso para que el módulo compile.

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java` (implementación real — placeholder temporal aquí, se completa en Task 5)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java` (placeholder temporal, se completa en Task 5)
- Modify (agregar overrides `UnsupportedOperationException` de `CatalogoSoportePort`): `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java`, `.../application/usecase/query/ConsultarUnidadMedidaHandlerTest.java`, `.../application/usecase/query/ConsultarFormaFarmaceuticaHandlerTest.java`, `.../application/usecase/query/ConsultarPrincipioActivoHandlerTest.java`, `.../application/usecase/command/CrearPrincipioActivoHandlerTest.java`, `.../application/usecase/command/CrearCondicionVentaHandlerTest.java`, `.../application/usecase/query/ConsultarCondicionVentaHandlerTest.java`, `.../application/usecase/query/ConsultarViaAdministracionHandlerTest.java`, `.../application/usecase/query/ConsultarClasificacionControladaHandlerTest.java`
- Modify (agregar override `UnsupportedOperationException` de `CatalogoReadPort`): `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandlerTest.java`, `.../application/usecase/query/ListarProductosReguladosHandlerTest.java`, `.../application/usecase/query/ListarCondicionesVentaHandlerTest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearTipoDocumentoIdentidadHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarTipoDocumentoIdentidadHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java` (agregar `toResult(TipoDocumentoIdentidad)`)
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearTipoDocumentoIdentidadHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandlerTest.java`

**Interfaces:**
- Consumes: `TipoDocumentoIdentidad` (Task 1), los 4 puertos in y DTOs (Task 2), `EstadoCatalogoSoporte`.
- Produces: `CatalogoSoportePort.save(TipoDocumentoIdentidad)` → `SaveOutcome`; `CatalogoSoportePort.findTipoDocumentoIdentidadByCodigo(String)` → `Optional<TipoDocumentoIdentidad>`; `CatalogoSoportePort.tipoDocumentoIdentidadExists(String)` → `boolean` (declarado pero no usado por los handlers de esta tarea — se deja consistente con el resto del puerto, que expone `existsById` por catálogo aunque `Crear*Handler` no lo invoque directamente); `CatalogoSoportePort.changeTipoDocumentoIdentidadStatus(String, String, Instant)` → `boolean`; `CatalogoReadPort.findTiposDocumentoIdentidad(String estado)` → `List<TipoDocumentoIdentidadResult>`; `CrearTipoDocumentoIdentidadHandler`, `ActualizarTipoDocumentoIdentidadHandler`, `ConsultarTipoDocumentoIdentidadHandler`, `ListarTiposDocumentoIdentidadHandler` implementando sus respectivos `UseCase`. Usados por Task 4 (control service) y Task 7 (wiring/controller).

- [ ] **Step 1: Escribir los tests que fallan para `CrearTipoDocumentoIdentidadHandler` y `ListarTiposDocumentoIdentidadHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearTipoDocumentoIdentidadHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearTipoDocumentoIdentidadHandlerTest {

    @Test
    void createsATipoDocumentoIdentidadSuccessfully() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new CrearTipoDocumentoIdentidadHandler(writePort);

        var result = handler.execute(new CrearTipoDocumentoIdentidadCommand(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8));

        assertTrue(result.isSuccess());
        assertEquals("1", result.getOrElse(error -> null).codigo());
    }

    @Test
    void failsWithConflictWhenCodigoAlreadyExists() {
        var writePort = new FakeCatalogoSoportePort();
        writePort.outcome = CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO;
        var handler = new CrearTipoDocumentoIdentidadHandler(writePort);

        var result = handler.execute(new CrearTipoDocumentoIdentidadCommand(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8));

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private SaveOutcome outcome = SaveOutcome.CREATED;

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad) { return outcome; }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean condicionVentaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean viaAdministracionExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean unidadMedidaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean clasificacionControladaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean tipoDocumentoIdentidadExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
```

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarTiposDocumentoIdentidadHandlerTest {

    @Test
    void returnsTiposDocumentoIdentidadFromReadPort() {
        var tipo = new TipoDocumentoIdentidadResult("1", "DNI", "Documento Nacional de Identidad", 8, 8, "ACTIVO");
        var readPort = new FakeCatalogoReadPort(List.of(tipo));
        var handler = new ListarTiposDocumentoIdentidadHandler(readPort);

        var result = handler.execute(new ListarTiposDocumentoIdentidadQuery(null));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).size());
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final List<TipoDocumentoIdentidadResult> tipos;

        private FakeCatalogoReadPort(List<TipoDocumentoIdentidadResult> tipos) {
            this.tipos = tipos;
        }

        @Override
        public List<CondicionVentaResult> findCondicionesVenta(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<ViaAdministracionResult> findViasAdministracion(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<UnidadMedidaResult> findUnidadesMedida(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado) { return tipos; }

        @Override
        public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<CategoriaProductoResult> findCategoriasProducto(UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<SkuResumen> findSkus(UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<RubroComercialResult> findRubrosComerciales(UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) { throw new UnsupportedOperationException(); }
    }
}
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan (no compilan)**

```
.\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearTipoDocumentoIdentidadHandlerTest" --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarTiposDocumentoIdentidadHandlerTest" --warning-mode all
```

Esperado: FALLA de compilación (símbolos `TipoDocumentoIdentidad` en el puerto, `CrearTipoDocumentoIdentidadHandler`, `ListarTiposDocumentoIdentidadHandler` no existen).

- [ ] **Step 3: Ampliar `CatalogoSoportePort`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`, agregar el import de `TipoDocumentoIdentidad`:

```java
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
```

Agregar tras `SaveOutcome save(ClasificacionControlada clasificacionControlada);`:

```java
    SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad);
```

Agregar tras `Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo);`:

```java
    Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo);
```

Agregar tras `boolean clasificacionControladaExists(String codigo);`:

```java
    boolean tipoDocumentoIdentidadExists(String codigo);
```

Agregar tras `boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt);`:

```java
    boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt);
```

- [ ] **Step 4: Ampliar `CatalogoReadPort`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`, agregar el import:

```java
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
```

Agregar tras `List<ClasificacionControladaResult> findClasificacionesControladas(String estado);`:

```java
    List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado);
```

- [ ] **Step 5: Agregar overrides temporales en `CatalogoSoporteJpaWriteAdapter` y `CatalogoJdbcReadAdapter`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`, agregar el import de `TipoDocumentoIdentidad` y, al final de la clase (antes del cierre `}`), estos 4 métodos temporales (se reemplazan por la implementación real en Task 5):

```java
    @Override
    public SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        throw new UnsupportedOperationException("Implementado en Task 5");
    }

    @Override
    public Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo) {
        throw new UnsupportedOperationException("Implementado en Task 5");
    }

    @Override
    public boolean tipoDocumentoIdentidadExists(String codigo) {
        throw new UnsupportedOperationException("Implementado en Task 5");
    }

    @Override
    public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) {
        throw new UnsupportedOperationException("Implementado en Task 5");
    }
```

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`, agregar el import de `TipoDocumentoIdentidadResult` y, al final de la clase, este método temporal:

```java
    @Override
    public List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado) {
        throw new UnsupportedOperationException("Implementado en Task 5");
    }
```

- [ ] **Step 6: Agregar overrides `UnsupportedOperationException` a los fakes/stubs de test existentes**

En cada uno de estos 9 archivos, agregar el import `com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad` a la clase/record que implementa `CatalogoSoportePort`, y agregar los 4 métodos con `throw new UnsupportedOperationException();` (mismo bloque en los 9 archivos, insertado junto a los demás overrides `save`/`find`/`exists`/`changeXStatus` existentes del mismo grupo):

```java
        @Override
        public SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tipoDocumentoIdentidadExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
```

Archivos a modificar (verificar el nombre exacto de la clase/record interno y el estilo de formato — una línea vs. multilínea — de cada archivo antes de insertar, para mantener consistencia local):
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandlerTest.java` (agregar aquí también, aunque el fake ya tiene su método `save(CondicionVenta)` propio — este archivo se reemplaza completo por el de esta misma tarea si se desea, pero basta con insertar los 4 métodos nuevos)
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandlerTest.java`

Y para `CatalogoReadPort`, agregar en cada uno de estos 3 archivos el import de `TipoDocumentoIdentidadResult` y el método:

```java
        @Override
        public List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado) {
            throw new UnsupportedOperationException();
        }
```

- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarProductosReguladosHandlerTest.java`
- `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCondicionesVentaHandlerTest.java`

- [ ] **Step 7: Agregar `toResult(TipoDocumentoIdentidad)` a `CatalogoApplicationMapper`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`, agregar el import:

```java
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
```

Agregar tras el método `toResult(ClasificacionControlada clasificacionControlada)`:

```java
    public static TipoDocumentoIdentidadResult toResult(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        return new TipoDocumentoIdentidadResult(
                tipoDocumentoIdentidad.codigo(), tipoDocumentoIdentidad.sigla(), tipoDocumentoIdentidad.denominacion(),
                tipoDocumentoIdentidad.max(), tipoDocumentoIdentidad.min(), tipoDocumentoIdentidad.estado().name());
    }
```

Y agregar el import de `TipoDocumentoIdentidadResult`:

```java
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
```

- [ ] **Step 8: Crear los 4 handlers**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearTipoDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearTipoDocumentoIdentidadHandler implements CrearTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort writePort;

    public CrearTipoDocumentoIdentidadHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(CrearTipoDocumentoIdentidadCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipoDocumentoIdentidad = TipoDocumentoIdentidad.create(
                command.codigo(), command.sigla(), command.denominacion(), command.max(), command.min());
        return tipoDocumentoIdentidad.fold(this::persist, this::validationFailure);
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> persist(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        var outcome = writePort.save(tipoDocumentoIdentidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TIPO_DOCUMENTO_IDENTIDAD_DUPLICADO", "Ya existe un tipo de documento con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(tipoDocumentoIdentidad));
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarTipoDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarTipoDocumentoIdentidadHandler implements ActualizarTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarTipoDocumentoIdentidadHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ActualizarTipoDocumentoIdentidadCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipoDocumentoIdentidad = TipoDocumentoIdentidad.create(
                command.codigo(), command.sigla(), command.denominacion(), command.max(), command.min());
        return tipoDocumentoIdentidad.fold(this::persist, this::validationFailure);
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> persist(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        var outcome = writePort.save(tipoDocumentoIdentidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(tipoDocumentoIdentidad));
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarTipoDocumentoIdentidadHandler implements ConsultarTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarTipoDocumentoIdentidadHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ConsultarTipoDocumentoIdentidadQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findTipoDocumentoIdentidadByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<TipoDocumentoIdentidadResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarTiposDocumentoIdentidadHandler implements ListarTiposDocumentoIdentidadUseCase {

    private final CatalogoReadPort readPort;

    public ListarTiposDocumentoIdentidadHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<TipoDocumentoIdentidadResult>, ApplicationError> execute(ListarTiposDocumentoIdentidadQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findTiposDocumentoIdentidad(query.estado()));
    }
}
```

- [ ] **Step 9: Compilar y ejecutar los tests del módulo**

```
.\gradlew.bat :modules:catalogo:test --warning-mode all
```

Esperado: BUILD SUCCESSFUL. Los tests de `CrearTipoDocumentoIdentidadHandlerTest` y `ListarTiposDocumentoIdentidadHandlerTest` pasan; el resto de tests existentes del módulo siguen pasando (no se rompió ningún fake).

- [ ] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main service-botica/modules/catalogo/src/test
git commit -m "feat(catalogo): agregar capa de aplicacion de TipoDocumentoIdentidad"
```

---

### Task 4: `CatalogoControlUseCase`/`CatalogoControlService` — cambio de estado

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`
- Modify: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java`

**Interfaces:**
- Consumes: `CatalogoSoportePort.changeTipoDocumentoIdentidadStatus` (Task 3).
- Produces: `CatalogoControlUseCase.changeTipoDocumentoIdentidadStatus(String codigo, String status)` → `Result<Unit, ApplicationError>`. Usado por Task 6 (controller).

- [ ] **Step 1: Ampliar el test existente `CatalogoControlServiceTest` con el caso de `TipoDocumentoIdentidad`**

Leer primero `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` completo para ubicar el bloque de test de `changeCondicionVentaStatus` (o el más reciente agregado para `RubroComercial`, commit `88f02ce`) y replicar su estructura. Agregar, en el mismo estilo, estos 2 métodos de test dentro de la clase `CatalogoControlServiceTest`:

```java
    @Test
    void changesTipoDocumentoIdentidadStatusSuccessfully() {
        var soportePort = new FakeCatalogoSoportePort();
        soportePort.changeTipoDocumentoIdentidadStatusResult = true;
        var service = newService(soportePort);

        var result = service.changeTipoDocumentoIdentidadStatus("1", "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void failsWithNotFoundWhenTipoDocumentoIdentidadDoesNotExist() {
        var soportePort = new FakeCatalogoSoportePort();
        soportePort.changeTipoDocumentoIdentidadStatusResult = false;
        var service = newService(soportePort);

        var result = service.changeTipoDocumentoIdentidadStatus("9", "INACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }
```

Ajustar los nombres `newService(...)`/`FakeCatalogoSoportePort` a los que realmente use el archivo existente (confirmar leyendo el archivo — si el helper que construye `CatalogoControlService` tiene otro nombre, usar ese). En el fake local `FakeCatalogoSoportePort` de este archivo (que también implementa `CatalogoSoportePort` — ya cubierto por Task 3 Step 6), agregar el campo:

```java
        private boolean changeTipoDocumentoIdentidadStatusResult = true;
```

y su override real (reemplazando el `throw new UnsupportedOperationException()` agregado en Task 3 Step 6 para este archivo específico):

```java
        @Override
        public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) {
            return changeTipoDocumentoIdentidadStatusResult;
        }
```

- [ ] **Step 2: Ejecutar el test y verificar que falla**

```
.\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest" --warning-mode all
```

Esperado: FALLA — `CatalogoControlUseCase` no declara `changeTipoDocumentoIdentidadStatus` (no compila) o el método no existe en `CatalogoControlService`.

- [ ] **Step 3: Ampliar `CatalogoControlUseCase`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`, agregar tras `changeClasificacionControladaStatus`:

```java
    Result<Unit, ApplicationError> changeTipoDocumentoIdentidadStatus(String codigo, String status);
```

- [ ] **Step 4: Implementar en `CatalogoControlService`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`, agregar tras el método `changeClasificacionControladaStatus`:

```java
    @Override
    public Result<Unit, ApplicationError> changeTipoDocumentoIdentidadStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeTipoDocumentoIdentidadStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento indicado no existe.");
    }
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

```
.\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest" --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar control de estado de TipoDocumentoIdentidad"
```

---

### Task 5: Infraestructura JPA/JDBC

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoSoporteWriteMapper.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java` (reemplaza los 4 métodos temporales de Task 3 con la implementación real)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java` (reemplaza el método temporal de Task 3)

**Interfaces:**
- Consumes: `TipoDocumentoIdentidad` (Task 1), `TipoDocumentoIdentidadResult` (Task 2), `EstadoCatalogoSoporte`.
- Produces: `TipoDocumentoIdentidadJpaEntity` (JPA, PK `codigo` String, campo `esActivo` String CHAR(1) — NO campo `estado`), `TipoDocumentoIdentidadJpaRepository extends JpaRepository<TipoDocumentoIdentidadJpaEntity, String>`, `CatalogoSoporteWriteMapper.toEntity(TipoDocumentoIdentidad)`. Usado por Task 6 (wiring del bean del repositorio se resuelve automáticamente vía inyección en `CatalogoSoporteJpaWriteAdapter`, ya `@Repository`).

No hay test nuevo en esta tarea — se ejercita a través de los handlers ya testeados en Task 3 (con fakes) y el patrón sigue exactamente el de `CondicionVenta`/`RubroComercial` ya cubiertos por tests existentes de esas clases; el módulo no tiene tests de integración JPA reales (confirmado: no existe contexto Spring/H2 en `src/test` de `catalogo`).

- [ ] **Step 1: Crear `TipoDocumentoIdentidadJpaEntity`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tipo_documento_identidad", schema = "sch_catalogo")
public class TipoDocumentoIdentidadJpaEntity {

    @Id
    @Column(length = 2)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String sigla;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "max")
    private Short max;

    @Column(name = "min")
    private Short min;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "es_activo", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String esActivo;

    protected TipoDocumentoIdentidadJpaEntity() {
    }

    public TipoDocumentoIdentidadJpaEntity(
            String codigo, String sigla, String denominacion, Short max, Short min, String esActivo) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.denominacion = denominacion;
        this.max = max;
        this.min = min;
        this.esActivo = esActivo;
    }

    public String getCodigo() { return codigo; }
    public String getSigla() { return sigla; }
    public String getDenominacion() { return denominacion; }
    public Short getMax() { return max; }
    public Short getMin() { return min; }
    public String getEsActivo() { return esActivo; }
}
```

- [ ] **Step 2: Crear `TipoDocumentoIdentidadJpaRepository`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.TipoDocumentoIdentidadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoIdentidadJpaRepository extends JpaRepository<TipoDocumentoIdentidadJpaEntity, String> {
}
```

- [ ] **Step 3: Ampliar `CatalogoSoporteWriteMapper`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoSoporteWriteMapper.java`, agregar los imports:

```java
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.TipoDocumentoIdentidadJpaEntity;
```

Agregar tras el método `toEntity(ClasificacionControlada clasificacionControlada)`:

```java
    public static TipoDocumentoIdentidadJpaEntity toEntity(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        return new TipoDocumentoIdentidadJpaEntity(
                tipoDocumentoIdentidad.codigo(), tipoDocumentoIdentidad.sigla(), tipoDocumentoIdentidad.denominacion(),
                toShort(tipoDocumentoIdentidad.max()), toShort(tipoDocumentoIdentidad.min()),
                tipoDocumentoIdentidad.estado() == EstadoCatalogoSoporte.ACTIVO ? "1" : "0");
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }
```

Agregar el import de `EstadoCatalogoSoporte`:

```java
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
```

- [ ] **Step 4: Implementar los 4 métodos en `CatalogoSoporteJpaWriteAdapter` (reemplazando los temporales de Task 3)**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`:

Agregar el import del repositorio y del `EstadoCatalogoSoporte` (si no está ya importado):

```java
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.TipoDocumentoIdentidadJpaRepository;
```

Agregar el campo y el parámetro de constructor:

```java
    private final TipoDocumentoIdentidadJpaRepository tipoDocumentoIdentidadRepository;
```

En el constructor, agregar el parámetro `TipoDocumentoIdentidadJpaRepository tipoDocumentoIdentidadRepository` a la firma y `this.tipoDocumentoIdentidadRepository = tipoDocumentoIdentidadRepository;` al cuerpo.

Reemplazar los 4 métodos temporales agregados en Task 3 Step 5 por:

```java
    @Override
    @Transactional
    public SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        var existing = tipoDocumentoIdentidadRepository.findById(tipoDocumentoIdentidad.codigo());
        if (existing.isEmpty()) {
            try {
                tipoDocumentoIdentidadRepository.saveAndFlush(
                        CatalogoSoporteWriteMapper.toEntity(tipoDocumentoIdentidad));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.tipo_documento_identidad
                           SET sigla = :sigla, denominacion = :denominacion, max = :max, min = :min
                         WHERE codigo = :codigo
                        """)
                .param("sigla", tipoDocumentoIdentidad.sigla())
                .param("denominacion", tipoDocumentoIdentidad.denominacion())
                .param("max", tipoDocumentoIdentidad.max())
                .param("min", tipoDocumentoIdentidad.min())
                .param("codigo", tipoDocumentoIdentidad.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo) {
        return tipoDocumentoIdentidadRepository.findById(codigo)
                .map(entity -> TipoDocumentoIdentidad.restore(
                        entity.getCodigo(), entity.getSigla(), entity.getDenominacion(),
                        entity.getMax() == null ? null : entity.getMax().intValue(),
                        entity.getMin() == null ? null : entity.getMin().intValue(),
                        "1".equals(entity.getEsActivo()) ? EstadoCatalogoSoporte.ACTIVO : EstadoCatalogoSoporte.INACTIVO));
    }

    @Override
    public boolean tipoDocumentoIdentidadExists(String codigo) {
        return tipoDocumentoIdentidadRepository.existsById(codigo);
    }

    @Override
    @Transactional
    public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) {
        var esActivo = "ACTIVO".equals(status) ? "1" : "0";
        return jdbcClient.sql(
                        "UPDATE sch_catalogo.tipo_documento_identidad SET es_activo = :esActivo WHERE codigo = :codigo")
                .param("esActivo", esActivo).param("codigo", codigo).update() == 1;
    }
```

Actualizar el constructor completo para reflejar el nuevo parámetro (mostrar el bloque final esperado):

```java
    public CatalogoSoporteJpaWriteAdapter(
            CondicionVentaJpaRepository condicionVentaRepository,
            FormaFarmaceuticaJpaRepository formaFarmaceuticaRepository,
            ViaAdministracionJpaRepository viaAdministracionRepository,
            UnidadMedidaJpaRepository unidadMedidaRepository,
            ClasificacionControladaJpaRepository clasificacionControladaRepository,
            TipoDocumentoIdentidadJpaRepository tipoDocumentoIdentidadRepository,
            PrincipioActivoJpaRepository principioActivoRepository,
            JdbcClient jdbcClient) {
        this.condicionVentaRepository = condicionVentaRepository;
        this.formaFarmaceuticaRepository = formaFarmaceuticaRepository;
        this.viaAdministracionRepository = viaAdministracionRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.clasificacionControladaRepository = clasificacionControladaRepository;
        this.tipoDocumentoIdentidadRepository = tipoDocumentoIdentidadRepository;
        this.principioActivoRepository = principioActivoRepository;
        this.jdbcClient = jdbcClient;
    }
```

(Spring resuelve el nuevo parámetro por autowiring de constructor — no requiere cambios en `CatalogoModuleConfiguration`, que no declara este bean explícitamente.)

- [ ] **Step 5: Ampliar `CatalogoJdbcReadRepository`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`, agregar el import:

```java
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
```

Agregar tras el método `findClasificacionesControladas`:

```java
    public List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, sigla, denominacion, max, min, es_activo
                          FROM sch_catalogo.tipo_documento_identidad
                         WHERE :estado = '' OR es_activo = CASE WHEN :estado = 'ACTIVO' THEN '1' ELSE '0' END
                         ORDER BY codigo
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new TipoDocumentoIdentidadResult(
                        rs.getString("codigo"), rs.getString("sigla"), rs.getString("denominacion"),
                        (Integer) rs.getObject("max"), (Integer) rs.getObject("min"),
                        "1".equals(rs.getString("es_activo")) ? "ACTIVO" : "INACTIVO"))
                .list();
    }
```

- [ ] **Step 6: Ampliar `CatalogoJdbcReadAdapter` (reemplazando el método temporal de Task 3)**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`, agregar el import de `TipoDocumentoIdentidadResult` si no está, y reemplazar el método temporal por:

```java
    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado) {
        return repository.findTiposDocumentoIdentidad(estado);
    }
```

- [ ] **Step 7: Compilar el módulo**

```
.\gradlew.bat :modules:catalogo:compileJava --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 8: Ejecutar todos los tests del módulo**

```
.\gradlew.bat :modules:catalogo:test --warning-mode all
```

Esperado: BUILD SUCCESSFUL, todos los tests pasan (incluyendo los de Task 3 y Task 4, que ya cubrían el contrato vía fakes).

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure
git commit -m "feat(catalogo): agregar infraestructura JPA/JDBC de TipoDocumentoIdentidad"
```

---

### Task 6: API REST

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/TipoDocumentoIdentidadRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/TipoDocumentoIdentidadResponse.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/TipoDocumentoIdentidadController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`

**Interfaces:**
- Consumes: los 4 `UseCase` de Task 2/3, `CatalogoControlUseCase.changeTipoDocumentoIdentidadStatus` (Task 4), `CambiarEstadoGlobalRequest` (ya existe, `com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest(String status)`), `CatalogoControllerSupport.problem(ApplicationError)` (ya existe, package-private en `api.controller`).
- Produces: endpoint REST `/api/v1/catalogo/tipos-documento-identidad`.

No hay test nuevo de controller en esta tarea — el módulo `catalogo` no tiene tests `@WebMvcTest`/`MockMvc` para ningún controller existente (confirmado por el patrón de `CondicionVentaController`, sin test equivalente); la cobertura de comportamiento vive en los handlers (Task 3/4), ya testeados.

- [ ] **Step 1: Crear `TipoDocumentoIdentidadRequest`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/TipoDocumentoIdentidadRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TipoDocumentoIdentidadRequest(
        @NotBlank @Size(max = 2) String codigo,
        @NotBlank @Size(max = 30) String sigla,
        @NotBlank @Size(min = 2, max = 200) String denominacion,
        @Positive Integer max,
        @Positive Integer min) {
}
```

- [ ] **Step 2: Crear `TipoDocumentoIdentidadResponse`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/TipoDocumentoIdentidadResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

public record TipoDocumentoIdentidadResponse(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
```

- [ ] **Step 3: Ampliar `CatalogoApiMapper`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`, agregar los imports:

```java
import com.softprimesolutions.catalogo.api.dto.request.TipoDocumentoIdentidadRequest;
import com.softprimesolutions.catalogo.api.dto.response.TipoDocumentoIdentidadResponse;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
```

Agregar, tras el bloque de métodos de `CondicionVenta` (tras `toResponse(CondicionVentaResult result)`):

```java
    public static CrearTipoDocumentoIdentidadCommand toCreateCommand(TipoDocumentoIdentidadRequest request) {
        return new CrearTipoDocumentoIdentidadCommand(
                request.codigo(), request.sigla(), request.denominacion(), request.max(), request.min());
    }

    public static ActualizarTipoDocumentoIdentidadCommand toUpdateCommand(TipoDocumentoIdentidadRequest request) {
        return new ActualizarTipoDocumentoIdentidadCommand(
                request.codigo(), request.sigla(), request.denominacion(), request.max(), request.min());
    }

    public static TipoDocumentoIdentidadResponse toResponse(TipoDocumentoIdentidadResult result) {
        return new TipoDocumentoIdentidadResponse(
                result.codigo(), result.sigla(), result.denominacion(), result.max(), result.min(), result.estado());
    }
```

- [ ] **Step 4: Crear `TipoDocumentoIdentidadController`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/TipoDocumentoIdentidadController.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.TipoDocumentoIdentidadRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/tipos-documento-identidad")
public class TipoDocumentoIdentidadController {

    private final CrearTipoDocumentoIdentidadUseCase crearTipoDocumentoIdentidad;
    private final ActualizarTipoDocumentoIdentidadUseCase actualizarTipoDocumentoIdentidad;
    private final ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidad;
    private final ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidad;
    private final CatalogoControlUseCase control;

    public TipoDocumentoIdentidadController(
            CrearTipoDocumentoIdentidadUseCase crearTipoDocumentoIdentidad,
            ActualizarTipoDocumentoIdentidadUseCase actualizarTipoDocumentoIdentidad,
            ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidad,
            ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidad,
            CatalogoControlUseCase control) {
        this.crearTipoDocumentoIdentidad = crearTipoDocumentoIdentidad;
        this.actualizarTipoDocumentoIdentidad = actualizarTipoDocumentoIdentidad;
        this.consultarTipoDocumentoIdentidad = consultarTipoDocumentoIdentidad;
        this.listarTiposDocumentoIdentidad = listarTiposDocumentoIdentidad;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody TipoDocumentoIdentidadRequest request) {
        return crearTipoDocumentoIdentidad.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody TipoDocumentoIdentidadRequest request) {
        return actualizarTipoDocumentoIdentidad.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeTipoDocumentoIdentidadStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarTipoDocumentoIdentidad.execute(new ConsultarTipoDocumentoIdentidadQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarTiposDocumentoIdentidad.execute(new ListarTiposDocumentoIdentidadQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [ ] **Step 5: Cablear los 4 casos de uso en `CatalogoModuleConfiguration`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`, agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ActualizarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarTipoDocumentoIdentidadHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearTipoDocumentoIdentidadHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarTipoDocumentoIdentidadHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarTiposDocumentoIdentidadHandler;
```

Agregar, tras el bloque `// ---- Soporte: clasificaciones controladas ----` y antes de `// ---- Principios activos ----`:

```java
    // ---- Soporte: tipos de documento de identidad ----

    @Bean
    CrearTipoDocumentoIdentidadUseCase crearTipoDocumentoIdentidadUseCase(CatalogoSoportePort soportePort) {
        return new CrearTipoDocumentoIdentidadHandler(soportePort);
    }

    @Bean
    ActualizarTipoDocumentoIdentidadUseCase actualizarTipoDocumentoIdentidadUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarTipoDocumentoIdentidadHandler(soportePort);
    }

    @Bean
    ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidadUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarTipoDocumentoIdentidadHandler(soportePort);
    }

    @Bean
    ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidadUseCase(CatalogoReadPort readPort) {
        return new ListarTiposDocumentoIdentidadHandler(readPort);
    }
```

- [ ] **Step 6: Compilar el módulo completo**

```
.\gradlew.bat :modules:catalogo:compileJava --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 7: Ejecutar todos los tests del módulo**

```
.\gradlew.bat :modules:catalogo:test --warning-mode all
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java
git commit -m "feat(catalogo): agregar API REST completa de TipoDocumentoIdentidad"
```

---

### Task 7: Verificación final del monolito completo

**Files:** ninguno (solo verificación).

**Interfaces:** N/A.

- [ ] **Step 1: Ejecutar el build completo con ArchUnit y Spring Modulith verify**

Desde `service-botica/`:

```
.\gradlew.bat check --warning-mode all
```

Esperado: BUILD SUCCESSFUL — compila `bootstrap-app` completo (que ensambla todos los módulos, incluyendo el nuevo wiring), pasan todos los tests, ArchUnit no reporta violaciones de capas (el nuevo código sigue el mismo layout de paquetes `domain/application/infrastructure/api` que el resto del módulo `catalogo`), y `ApplicationModules.verify()` de Spring Modulith no reporta ciclos ni dependencias no declaradas (no se agregó ninguna dependencia nueva entre módulos).

- [ ] **Step 2: Si falla, diagnosticar y corregir antes de continuar**

Si `check` falla, leer el reporte de ArchUnit (`bootstrap-app/build/reports/tests` o la salida de consola) o el error de Modulith y corregir en el archivo correspondiente de las tareas anteriores — no hacer commit de un estado roto.

- [ ] **Step 3: Commit final (si hubo correcciones en este paso)**

Solo si el Step 2 requirió cambios:

```bash
git add -A
git commit -m "fix(catalogo): corregir verificacion de arquitectura de TipoDocumentoIdentidad"
```

Si `check` pasó limpio en el Step 1, no hay nada que commitear en esta tarea.

---

## Self-Review

**Spec coverage:**
- Dominio `TipoDocumentoIdentidad` con validaciones del CHECK de BD → Task 1. ✓
- DTOs/puertos in → Task 2. ✓
- Puerto out `CatalogoSoportePort`/`CatalogoReadPort` ampliados + handlers → Task 3. ✓
- `CatalogoControlService.changeTipoDocumentoIdentidadStatus` → Task 4. ✓
- Infraestructura JPA/JDBC con mapeo `es_activo` (no `estado`) → Task 5. ✓
- API REST (`TipoDocumentoIdentidadController`, DTOs, mapper, wiring) → Task 6. ✓
- Tests de dominio y de handlers Crear/Listar → Task 1 y Task 3. ✓
- Verificación de arquitectura (ArchUnit/Modulith) → Task 7. ✓
- Fuera de alcance (spec): `rubro_comercial` no se toca (no aparece en ninguna tarea), sin nuevas authorities (se reutiliza `catalogo.soporte.*` en Task 6), sin paginación (Task 3/5 usan `List`, no `PaginaResult`), sin `AggregateRoot`/`TenantId` (Task 1 usa VO plano). ✓

**Placeholder scan:** sin "TBD"/"TODO"; los únicos placeholders intencionales son los 4 métodos con `UnsupportedOperationException("Implementado en Task 5")` en Task 3 Step 5, que es un patrón deliberado de TDD por capas (la interfaz se amplía en Task 3, la implementación real llega en Task 5) — están explícitamente marcados y resueltos en la tarea siguiente, no son placeholders abandonados.

**Type consistency:** `TipoDocumentoIdentidad.create/restore` usa `Integer max, Integer min` consistentemente en dominio (Task 1), DTOs de aplicación (Task 2), handlers (Task 3), y API request/response (Task 6, `Integer` también en JSON). La entidad JPA (Task 5) usa `Short` internamente por ser `SMALLINT` en BD, con conversión explícita `toShort`/`.intValue()` en el mapper y el adapter — consistente con el tipo de columna real sin filtrar `Short` a capas superiores.

**Scope check:** una sola tarea de negocio (un catálogo), 7 tareas técnicas secuenciales con dependencias claras (dominio → DTOs → puerto+handlers → control → infra → API → verificación). No requiere descomposición adicional.
