# UI de Organización y cambio de estado de Empresa/Establecimiento — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar `PATCH .../estado` para Empresa y Establecimiento en el backend y construir en el frontend la UI de gestión (drill-down Empresas → Establecimiento → Almacenes/Terminales) sobre las APIs de `organizacion`.

**Architecture:** Fase 1 añade dos casos de uso CQRS al módulo `organizacion` (handlers que reutilizan `cambiarEstado` / `cambiarEstadoOperativo` del dominio y el `save` existente) y dos endpoints REST. Fase 2 agrega a `features/organizacion` una capa `api/` por entidad (TanStack Query + `apiClient`), schemas zod, formularios en `Modal` con react-hook-form y cuatro páginas (lista de empresas, detalle de empresa, detalle de establecimiento y el resumen existente enlazado). Se agrega un componente `Select` a `@boticas/ui-web`.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith, JUnit 5 + Mockito + AssertJ, PostgreSQL/Testcontainers; React 19.2, React Router 8.3, TanStack Query, react-hook-form 7 + zod 4 + `@hookform/resolvers` 5, Tailwind 4, Vitest 4 + Testing Library + MSW 2.

**Spec:** `docs/superpowers/specs/2026-09-30-ui-organizacion-design.md`

## Global Constraints

- Todo texto visible al usuario y todo mensaje de error va en español.
- Sin comentarios explicativos en el código (backend y frontend).
- Backend: todo archivo nuevo alcanza 100% de líneas y ramas por clase (JaCoCo, gate dentro de `check`). Los comandos Gradle se ejecutan desde `service-botica` con `.\gradlew.bat`.
- Frontend: los umbrales de Vitest son 100% de líneas, ramas, funciones y sentencias sobre todo archivo no listado en `frontend/coverage-baseline.txt`; ningún archivo nuevo se agrega a esa lista.
- Frontend: importar de `react-router` (nunca `react-router-dom`); prohibido `forwardRef` (React 19 recibe `ref` como prop); `verbatimModuleSyntax` exige `import type` para tipos; `exactOptionalPropertyTypes` exige escribir opcionales como `campo?: T | undefined`; `noUncheckedIndexedAccess` activo.
- Frontend: una feature solo importa de otra vía su `index.ts` (por ejemplo `import { useAuthSession } from '../../auth'`).
- Frontend: MSW corre con `onUnhandledRequest: 'error'`; toda petición HTTP en un test necesita su handler.
- El `tenantId` viaja en el body de los `POST` y como query param en `GET`, `PUT` y `PATCH` (excepto `GET /estructura-corporativa`, que lo toma del JWT).
- Valores de enums exactos: Empresa `ACTIVO | SUSPENDIDO | BLOQUEADO`; Establecimiento `ACTIVO | SUSPENDIDO | CLAUSURADO | REMODELACION`; Terminal `ACTIVO | BLOQUEADO | MANTENIMIENTO`; Tipo de establecimiento `BOTICA`; Perfil de operación `ONLINE | STORE_EDGE`; Tipo de almacén `VENTA | GENERAL | CUARENTENA | REFRIGERADO | PSICOTROPICO | MERMA`.
- Límites del DDL: RUC `(10|20)` + 9 dígitos; ubigeo 6 dígitos; anexo SUNAT 4 dígitos; series de terminal 4 caracteres (`B` + 3 alfanuméricos / `F` + 3 alfanuméricos); nombre de almacén ≤ 150; nombre de terminal ≤ 120; número de serie de equipo ≤ 120; impresora ≤ 100.
- Los commits terminan con la línea `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Estructura de archivos

Rutas relativas a la raíz del repositorio. `BE` = `service-botica/modules/organizacion/src`, `FE` = `frontend/apps/erp-web/src/features/organizacion`, `UI` = `frontend/packages/ui-web/src`.

**Fase 1 — backend**

| Archivo | Responsabilidad |
|---|---|
| `BE/main/.../application/mapper/OrganizacionApplicationMapper.java` (modificar) | Añade `toDomain` para reconstruir Empresa y Establecimiento desde su resultado |
| `BE/main/.../application/usecase/command/EnumParser.java` (crear) | Parseo genérico y seguro de enums desde `String` |
| `BE/main/.../application/dto/command/CambiarEstadoEmpresaCommand.java`, `CambiarEstadoEstablecimientoCommand.java` (crear) | Comandos de cambio de estado |
| `BE/main/.../application/port/in/CambiarEstadoEmpresaUseCase.java`, `CambiarEstadoEstablecimientoUseCase.java` (crear) | Puertos de entrada |
| `BE/main/.../application/usecase/command/CambiarEstadoEmpresaHandler.java`, `CambiarEstadoEstablecimientoHandler.java` (crear) | Casos de uso |
| `BE/main/.../api/dto/request/CambiarEstadoRequest.java` (crear) | Body `{ estado }` compartido por ambos endpoints |
| `BE/main/.../api/mapper/OrganizacionApiMapper.java` (modificar) | Mapea el request a los dos comandos |
| `BE/main/.../api/controller/EmpresaOperadoraController.java`, `EstablecimientoController.java` (modificar) | Añaden `PATCH /{id}/estado` |
| `BE/main/.../infrastructure/configuration/OrganizacionModuleConfiguration.java` (modificar) | Registra los dos beans |
| `service-botica/bootstrap-app/src/test/java/.../organizacion/api/OrganizacionApiIntegrationTest.java` (modificar) | Casos de integración HTTP |

**Fase 2 — frontend**

| Archivo | Responsabilidad |
|---|---|
| `UI/select/Select.tsx` (crear) + export en `UI/index.ts` | Select estilizado como `Input` |
| `FE/lib/describe-api-error.ts` | Traduce `ApiError` a mensaje en español |
| `FE/lib/format.ts` | `valueOrDash`, `yesNo` |
| `FE/lib/form-values.ts` | `emptyToUndefined`, `toNumberOrUndefined` |
| `FE/lib/use-tenant-id.ts`, `FE/lib/use-route-param.ts` | Hooks de sesión y de parámetro de ruta |
| `FE/api/pagina.types.ts`, `query-string.ts`, `invalidate.ts` | Tipo de página, armado de query, invalidación |
| `FE/api/{empresas,establecimientos,almacenes,terminales}.types.ts` y `.api.ts` | Tipos y llamadas HTTP por entidad |
| `FE/schemas/campos.ts` y `{empresa,establecimiento,almacen,terminal}.schema.ts` | Validación zod |
| `FE/lib/form-payloads.ts` | Convierte valores de formulario a payloads de API |
| `FE/components/{FormFields,FormError,FormSection,CambiarEstadoDialog}.tsx` | Piezas de formulario compartidas |
| `FE/components/{EmpresaForm,EstablecimientoForm,AlmacenForm,TerminalForm}.tsx` | Formularios |
| `FE/components/{EstablecimientosSection,AlmacenesSection,TerminalesSection}.tsx` | Listas con creación/edición dentro de los detalles |
| `FE/pages/{EmpresasPage,EmpresaDetailPage,EstablecimientoDetailPage}.tsx` | Páginas nuevas |
| `FE/pages/OrganizationPage.tsx`, `FE/routes.tsx`, `FE/index.ts`, `app/feature-routes.test.ts` (modificar) | Integración del resumen y rutas |

---

## FASE 1 — BACKEND

### Task 1: Extraer `toDomain` y `EnumParser` (refactor sin cambio de comportamiento)

Los handlers de actualización reconstruyen el agregado desde el resultado con un bloque idéntico al que necesitará el cambio de estado. Se extrae ese bloque al mapper de aplicación y se agrega un parser genérico de enums.

**Files:**
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/mapper/OrganizacionApplicationMapper.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/EnumParser.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEmpresaOperadoraHandler.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/ActualizarEstablecimientoHandler.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/mapper/OrganizacionApplicationMapperTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/EnumParserTest.java`

**Interfaces:**
- Produces: `OrganizacionApplicationMapper.toDomain(EmpresaOperadoraResult): EmpresaOperadora`; `OrganizacionApplicationMapper.toDomain(EstablecimientoResult): Establecimiento`; `EnumParser.parse(Class<E>, String): Optional<E>`; `EnumParser.allowedValues(Class<E>): String` (paquete `application.usecase.command`, visibilidad de paquete).

- [ ] **Step 1: Escribir el test del mapper (falla por método inexistente)**

```java
package com.softprimesolutions.organizacion.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizacionApplicationMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void restoresAnEmpresaThatMapsBackToTheSameResult() {
        var result = new EmpresaOperadoraResult(
                UUID.randomUUID(), UUID.randomUUID(), "20123456789", "Boticas SAC", "Boticas", "Av. 1",
                "150101", "01444", "a@b.pe", "https://b.pe", "PEN", "America/Lima", true, "SUSPENDIDO",
                CREATED_AT, UPDATED_AT);

        var restored = OrganizacionApplicationMapper.toDomain(result);

        assertThat(OrganizacionApplicationMapper.toResult(restored)).isEqualTo(result);
    }

    @Test
    void restoresAnEstablecimientoThatMapsBackToTheSameResult() {
        var result = new EstablecimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "EST001", "Botica Central", "BOTICA",
                "CAT", "0001", "DIG001", "Av. 2", "150101", "Frente al parque", BigDecimal.ONE, BigDecimal.TEN,
                "01444", "e@b.pe", true, true, true, "STORE_EDGE", "America/Lima", "CLAUSURADO", CREATED_AT,
                UPDATED_AT);

        var restored = OrganizacionApplicationMapper.toDomain(result);

        assertThat(OrganizacionApplicationMapper.toResult(restored)).isEqualTo(result);
    }
}
```

- [ ] **Step 2: Escribir el test de `EnumParser`**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import org.junit.jupiter.api.Test;

class EnumParserTest {

    @Test
    void parsesAnExactEnumName() {
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "SUSPENDIDO"))
                .contains(EstadoEmpresaOperadora.SUSPENDIDO);
    }

    @Test
    void returnsEmptyForUnknownBlankOrNullValues() {
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "FOO")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "suspendido")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, null)).isEmpty();
    }

    @Test
    void listsTheAllowedValuesInDeclarationOrder() {
        assertThat(EnumParser.allowedValues(EstadoEmpresaOperadora.class))
                .isEqualTo("ACTIVO, SUSPENDIDO, BLOQUEADO");
    }
}
```

- [ ] **Step 3: Ejecutar los tests y verificar que fallan**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:test --tests "*OrganizacionApplicationMapperTest" --tests "*EnumParserTest"`
Expected: FAIL de compilación (`cannot find symbol: method toDomain` y `class EnumParser`).

- [ ] **Step 4: Crear `EnumParser`**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

final class EnumParser {

    private EnumParser() {
    }

    static <E extends Enum<E>> Optional<E> parse(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst();
    }

    static <E extends Enum<E>> String allowedValues(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
```

- [ ] **Step 5: Añadir `toDomain` al mapper**

En `OrganizacionApplicationMapper.java` agregar estos imports junto a los existentes:

```java
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.PerfilOperacion;
import com.softprimesolutions.organizacion.domain.model.TipoEstablecimiento;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
```

y agregar estos dos métodos justo después del constructor privado:

```java
    public static EmpresaOperadora toDomain(EmpresaOperadoraResult result) {
        return EmpresaOperadora.restore(
                new EmpresaOperadoraId(result.id()), new TenantId(result.tenantId()), result.ruc(),
                result.razonSocial(), result.nombreComercial(), result.direccionFiscal(),
                result.ubigeoFiscal(), result.telefono(), result.email(), result.sitioWeb(),
                result.monedaFuncional(), result.zonaHoraria(), result.permiteVentaOnline(),
                EstadoEmpresaOperadora.valueOf(result.estado()), result.createdAt(), result.updatedAt());
    }

    public static Establecimiento toDomain(EstablecimientoResult result) {
        return Establecimiento.restore(
                new EstablecimientoId(result.id()), new TenantId(result.tenantId()),
                new EmpresaOperadoraId(result.empresaId()), result.codigo(), result.nombre(),
                TipoEstablecimiento.valueOf(result.tipoEstablecimiento()), result.categoriaRegulatoriaCodigo(),
                result.codigoAnexoSunat(), result.codigoDigemid(), result.direccion(), result.ubigeo(),
                result.referencia(), result.latitud(), result.longitud(), result.telefono(), result.email(),
                result.esPrincipal(), result.permiteVentaOnline(), result.permiteDelivery(),
                PerfilOperacion.valueOf(result.perfilOperacion()), result.zonaHoraria(),
                EstadoEstablecimiento.valueOf(result.estadoOperativo()), result.createdAt(), result.updatedAt());
    }
```

- [ ] **Step 6: Refactorizar `ActualizarEmpresaOperadoraHandler` para usar `toDomain`**

Reemplazar el contenido completo del archivo por:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
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

        var updated = OrganizacionApplicationMapper.toDomain(existing.get()).updateDetails(
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

- [ ] **Step 7: Refactorizar `ActualizarEstablecimientoHandler` para usar `toDomain`**

Reemplazar el contenido completo del archivo por:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarEstablecimientoHandler implements ActualizarEstablecimientoUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarEstablecimientoHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(ActualizarEstablecimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findEstablecimientoById(command.tenantId(), command.establecimientoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }

        var updated = OrganizacionApplicationMapper.toDomain(existing.get()).updateDetails(
                command.nombre(), EstablecimientoEnums.tipoEstablecimiento(command.tipoEstablecimiento()),
                command.categoriaRegulatoriaCodigo(), command.codigoAnexoSunat(), command.codigoDigemid(),
                command.direccion(), command.ubigeo(), command.referencia(), command.latitud(),
                command.longitud(), command.telefono(), command.email(), command.esPrincipal(),
                command.permiteVentaOnline(), command.permiteDelivery(),
                EstablecimientoEnums.perfilOperacion(command.perfilOperacion()), command.zonaHoraria(),
                clock.now());

        return updated.fold(this::persist, this::validationFailure);
    }

    private Result<EstablecimientoResult, ApplicationError> persist(Establecimiento establecimiento) {
        writePort.save(establecimiento);
        return Result.success(OrganizacionApplicationMapper.toResult(establecimiento));
    }

    private Result<EstablecimientoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 8: Ejecutar todo el módulo y verificar que pasa con cobertura**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`. Los tests existentes de `ActualizarEmpresaOperadoraHandlerTest` y `ActualizarEstablecimientoHandlerTest` siguen pasando sin cambios (prueban el comportamiento que no se modificó) y el gate de JaCoCo no reporta violaciones.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/organizacion
git commit -m "refactor(organizacion): extraer toDomain y EnumParser de los handlers de actualizacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Casos de uso de cambio de estado (capa de aplicación)

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CambiarEstadoEmpresaCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/dto/command/CambiarEstadoEstablecimientoCommand.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CambiarEstadoEmpresaUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/port/in/CambiarEstadoEstablecimientoUseCase.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CambiarEstadoEmpresaHandler.java`
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/application/usecase/command/CambiarEstadoEstablecimientoHandler.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CambiarEstadoEmpresaHandlerTest.java`
- Test: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/application/usecase/command/CambiarEstadoEstablecimientoHandlerTest.java`

**Interfaces:**
- Consumes: `OrganizacionApplicationMapper.toDomain(...)`, `EnumParser.parse/allowedValues` (Task 1); `OrganizacionReadPort.findEmpresaById(UUID tenantId, UUID empresaId)`, `findEstablecimientoById(UUID tenantId, UUID establecimientoId)`; `OrganizacionWritePort.save(EmpresaOperadora | Establecimiento)`; dominio `EmpresaOperadora.cambiarEstado(EstadoEmpresaOperadora, Instant)`, `Establecimiento.cambiarEstadoOperativo(EstadoEstablecimiento, Instant)`.
- Produces: `CambiarEstadoEmpresaCommand(UUID empresaId, UUID tenantId, String estado)`; `CambiarEstadoEstablecimientoCommand(UUID establecimientoId, UUID tenantId, String estado)`; `CambiarEstadoEmpresaUseCase.execute(CambiarEstadoEmpresaCommand): Result<EmpresaOperadoraResult, ApplicationError>`; `CambiarEstadoEstablecimientoUseCase.execute(CambiarEstadoEstablecimientoCommand): Result<EstablecimientoResult, ApplicationError>`; `CambiarEstadoEmpresaHandler(OrganizacionReadPort, OrganizacionWritePort, ClockPort)` y `CambiarEstadoEstablecimientoHandler(OrganizacionReadPort, OrganizacionWritePort, ClockPort)`.

- [ ] **Step 1: Crear los comandos y los puertos**

```java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CambiarEstadoEmpresaCommand(UUID empresaId, UUID tenantId, String estado) {
}
```

```java
package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CambiarEstadoEstablecimientoCommand(UUID establecimientoId, UUID tenantId, String estado) {
}
```

```java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoEmpresaUseCase {

    Result<EmpresaOperadoraResult, ApplicationError> execute(CambiarEstadoEmpresaCommand command);
}
```

```java
package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoEstablecimientoUseCase {

    Result<EstablecimientoResult, ApplicationError> execute(CambiarEstadoEstablecimientoCommand command);
}
```

- [ ] **Step 2: Escribir el test de `CambiarEstadoEmpresaHandler` (falla: la clase no existe)**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CambiarEstadoEmpresaHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final CambiarEstadoEmpresaHandler handler =
            new CambiarEstadoEmpresaHandler(readPort, writePort, () -> NOW);

    private EmpresaOperadoraResult existingEmpresa() {
        return new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456789", "Boticas SAC", null, null, null, null, null, null,
                "PEN", "America/Lima", false, "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    @Test
    void changesTheStatusAndPersistsTheAggregate() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        var saved = ArgumentCaptor.forClass(EmpresaOperadora.class);

        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "SUSPENDIDO"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.estado()).isEqualTo("SUSPENDIDO");
            assertThat(empresa.updatedAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort).save(saved.capture());
        assertThat(saved.getValue().estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
    }

    @Test
    void returnsNotFoundWhenTheEmpresaDoesNotExist() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "SUSPENDIDO"));

        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }

    @Test
    void rejectsAnUnknownStatusWithoutReadingTheEmpresa() {
        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "FOO"));

        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            assertThat(error.code()).isEqualTo("ORG_EMPRESA_ESTADO_INVALIDO");
            assertThat(error.message()).contains("ACTIVO, SUSPENDIDO, BLOQUEADO");
            return null;
        });
        verify(readPort, never()).findEmpresaById(any(), any());
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }

    @Test
    void returnsAValidationErrorWhenTheDomainRejectsTheChange() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        ClockPort clockWithoutInstant = () -> null;
        var handlerWithoutInstant = new CambiarEstadoEmpresaHandler(readPort, writePort, clockWithoutInstant);

        var result = handlerWithoutInstant.execute(
                new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "BLOQUEADO"));

        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }
}
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:test --tests "*CambiarEstadoEmpresaHandlerTest"`
Expected: FAIL de compilación (`cannot find symbol: class CambiarEstadoEmpresaHandler`).

- [ ] **Step 4: Implementar `CambiarEstadoEmpresaHandler`**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CambiarEstadoEmpresaHandler implements CambiarEstadoEmpresaUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public CambiarEstadoEmpresaHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(CambiarEstadoEmpresaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var estado = EnumParser.parse(EstadoEmpresaOperadora.class, command.estado());
        if (estado.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_ESTADO_INVALIDO",
                    "El estado indicado no es válido. Valores permitidos: "
                            + EnumParser.allowedValues(EstadoEmpresaOperadora.class) + ".",
                    ErrorCategory.VALIDATION));
        }
        var existing = readPort.findEmpresaById(command.tenantId(), command.empresaId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND));
        }

        return OrganizacionApplicationMapper.toDomain(existing.get())
                .cambiarEstado(estado.get(), clock.now())
                .fold(this::persist, this::validationFailure);
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

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:test --tests "*CambiarEstadoEmpresaHandlerTest"`
Expected: 4 tests PASS.

- [ ] **Step 6: Escribir el test de `CambiarEstadoEstablecimientoHandler` (falla: la clase no existe)**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CambiarEstadoEstablecimientoHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final CambiarEstadoEstablecimientoHandler handler =
            new CambiarEstadoEstablecimientoHandler(readPort, writePort, () -> NOW);

    private EstablecimientoResult existingEstablecimiento() {
        return new EstablecimientoResult(
                ESTABLECIMIENTO_ID, TENANT_ID, UUID.randomUUID(), "EST001", "Botica Central", "BOTICA", null,
                "0001", "DIG001", "Av. 2", "150101", null, BigDecimal.ONE, BigDecimal.TEN, null, null, true,
                false, false, "ONLINE", "America/Lima", "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    @Test
    void changesTheOperationalStatusAndPersistsTheAggregate() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        var saved = ArgumentCaptor.forClass(Establecimiento.class);

        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "REMODELACION"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(establecimiento -> {
            assertThat(establecimiento.estadoOperativo()).isEqualTo("REMODELACION");
            assertThat(establecimiento.updatedAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort).save(saved.capture());
        assertThat(saved.getValue().estadoOperativo()).isEqualTo(EstadoEstablecimiento.REMODELACION);
    }

    @Test
    void returnsNotFoundWhenTheEstablecimientoDoesNotExist() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID)).thenReturn(Optional.empty());

        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "SUSPENDIDO"));

        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
        verify(writePort, never()).save(any(Establecimiento.class));
    }

    @Test
    void rejectsAnUnknownStatusWithoutReadingTheEstablecimiento() {
        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "BLOQUEADO"));

        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_ESTADO_INVALIDO");
            assertThat(error.message()).contains("ACTIVO, SUSPENDIDO, CLAUSURADO, REMODELACION");
            return null;
        });
        verify(readPort, never()).findEstablecimientoById(any(), any());
        verify(writePort, never()).save(any(Establecimiento.class));
    }

    @Test
    void returnsAValidationErrorWhenTheDomainRejectsTheChange() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        ClockPort clockWithoutInstant = () -> null;
        var handlerWithoutInstant =
                new CambiarEstadoEstablecimientoHandler(readPort, writePort, clockWithoutInstant);

        var result = handlerWithoutInstant.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "CLAUSURADO"));

        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
        verify(writePort, never()).save(any(Establecimiento.class));
    }
}
```

- [ ] **Step 7: Implementar `CambiarEstadoEstablecimientoHandler`**

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CambiarEstadoEstablecimientoHandler implements CambiarEstadoEstablecimientoUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public CambiarEstadoEstablecimientoHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(CambiarEstadoEstablecimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var estado = EnumParser.parse(EstadoEstablecimiento.class, command.estado());
        if (estado.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_ESTADO_INVALIDO",
                    "El estado indicado no es válido. Valores permitidos: "
                            + EnumParser.allowedValues(EstadoEstablecimiento.class) + ".",
                    ErrorCategory.VALIDATION));
        }
        var existing = readPort.findEstablecimientoById(command.tenantId(), command.establecimientoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }

        return OrganizacionApplicationMapper.toDomain(existing.get())
                .cambiarEstadoOperativo(estado.get(), clock.now())
                .fold(this::persist, this::validationFailure);
    }

    private Result<EstablecimientoResult, ApplicationError> persist(Establecimiento establecimiento) {
        writePort.save(establecimiento);
        return Result.success(OrganizacionApplicationMapper.toResult(establecimiento));
    }

    private Result<EstablecimientoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 8: Ejecutar todo el módulo con el gate de cobertura**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`, sin violaciones de JaCoCo (los dos handlers y `EnumParser` al 100% de líneas y ramas).

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/organizacion
git commit -m "feat(organizacion): agregar casos de uso de cambio de estado de empresa y establecimiento

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Endpoints `PATCH .../estado` (capa API y configuración)

**Files:**
- Create: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/dto/request/CambiarEstadoRequest.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/mapper/OrganizacionApiMapper.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/EmpresaOperadoraController.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/api/controller/EstablecimientoController.java`
- Modify: `service-botica/modules/organizacion/src/main/java/com/softprimesolutions/organizacion/infrastructure/configuration/OrganizacionModuleConfiguration.java`
- Modify: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/api/OrganizacionApiFixtures.java`
- Modify: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/api/mapper/OrganizacionApiMapperTest.java`
- Modify: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/api/controller/EmpresaOperadoraControllerTest.java`
- Modify: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/api/controller/EstablecimientoControllerTest.java`
- Modify: `service-botica/modules/organizacion/src/test/java/com/softprimesolutions/organizacion/infrastructure/configuration/OrganizacionModuleConfigurationTest.java`

**Interfaces:**
- Consumes: `CambiarEstadoEmpresaUseCase`, `CambiarEstadoEstablecimientoUseCase` y sus comandos (Task 2).
- Produces: `PATCH /api/v1/organizacion/empresas/{empresaId}/estado?tenantId=` y `PATCH /api/v1/organizacion/establecimientos/{establecimientoId}/estado?tenantId=`, body `{ "estado": "<VALOR>" }`, respuesta `200` con `EmpresaOperadoraResponse` / `EstablecimientoResponse`; `CambiarEstadoRequest(String estado)`; `OrganizacionApiMapper.toCambiarEstadoEmpresaCommand(UUID empresaId, UUID tenantId, CambiarEstadoRequest)` y `toCambiarEstadoEstablecimientoCommand(UUID establecimientoId, UUID tenantId, CambiarEstadoRequest)`; controllers con un quinto parámetro de constructor `changeStatus`.

- [ ] **Step 1: Crear el request compartido**

```java
package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(@NotBlank String estado) {
}
```

- [ ] **Step 2: Escribir los tests que fallan — fixtures y mapper**

En `OrganizacionApiFixtures.java` agregar el import y el método (junto a los demás):

```java
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
```

```java
    public static CambiarEstadoRequest cambiarEstadoRequest(String estado) {
        return new CambiarEstadoRequest(estado);
    }
```

En `OrganizacionApiMapperTest.java` agregar los imports:

```java
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
```

y este test dentro de la clase:

```java
    @Test
    void mapsTheStatusChangeRequestToTheCommandOfEachEntity() {
        var empresa = OrganizacionApiMapper.toCambiarEstadoEmpresaCommand(
                EMPRESA, TENANT, cambiarEstadoRequest("SUSPENDIDO"));
        var establecimiento = OrganizacionApiMapper.toCambiarEstadoEstablecimientoCommand(
                ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO"));

        assertThat(empresa).isEqualTo(new CambiarEstadoEmpresaCommand(EMPRESA, TENANT, "SUSPENDIDO"));
        assertThat(establecimiento)
                .isEqualTo(new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO, TENANT, "CLAUSURADO"));
    }
```

- [ ] **Step 3: Escribir los tests que fallan — controllers**

Reemplazar `EmpresaOperadoraControllerTest.java` completo por:

```java
package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.empresaResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class EmpresaOperadoraControllerTest {

    private final EmpresaOperadoraController succeeding = new EmpresaOperadoraController(
            command -> ok(empresaResult()), command -> ok(empresaResult()),
            query -> ok(page(empresaResult())), query -> ok(empresaResult()),
            command -> ok(empresaResult()));
    private final EmpresaOperadoraController failing = new EmpresaOperadoraController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict(),
            command -> conflict());

    @Test
    void createsAnEmpresaAndReturnsItsLocation() {
        assertCreated(succeeding.create(crearEmpresaRequest()), EmpresaOperadoraController.BASE_PATH, EMPRESA,
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(crearEmpresaRequest()));
    }

    @Test
    void listsEmpresasAsAPage() {
        assertOk(succeeding.list(TENANT, "bot", 1, 10), OrganizacionApiMapper.toEmpresaPage(page(empresaResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(TENANT, null, 0, 20));
    }

    @Test
    void getsAnEmpresaById() {
        assertOk(succeeding.getById(EMPRESA, TENANT), OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(EMPRESA, TENANT));
    }

    @Test
    void updatesAnEmpresa() {
        assertOk(succeeding.update(EMPRESA, TENANT, actualizarEmpresaRequest()),
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(EMPRESA, TENANT, actualizarEmpresaRequest()));
    }

    @Test
    void changesTheStatusOfAnEmpresa() {
        assertOk(succeeding.changeStatus(EMPRESA, TENANT, cambiarEstadoRequest("SUSPENDIDO")),
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsStatusChangeFailuresToProblemDetails() {
        assertConflict(failing.changeStatus(EMPRESA, TENANT, cambiarEstadoRequest("SUSPENDIDO")));
    }
}
```

Reemplazar `EstablecimientoControllerTest.java` completo por:

```java
package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.establecimientoResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class EstablecimientoControllerTest {

    private final EstablecimientoController succeeding = new EstablecimientoController(
            command -> ok(establecimientoResult()), command -> ok(establecimientoResult()),
            query -> ok(page(establecimientoResult())), query -> ok(establecimientoResult()),
            command -> ok(establecimientoResult()));
    private final EstablecimientoController failing = new EstablecimientoController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict(),
            command -> conflict());

    @Test
    void createsAnEstablecimientoAndReturnsItsLocation() {
        assertCreated(succeeding.create(crearEstablecimientoRequest()), EstablecimientoController.BASE_PATH,
                ESTABLECIMIENTO, OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(crearEstablecimientoRequest()));
    }

    @Test
    void listsEstablecimientosAsAPage() {
        assertOk(succeeding.list(TENANT, EMPRESA, "cen", 0, 20),
                OrganizacionApiMapper.toEstablecimientoPage(page(establecimientoResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(TENANT, null, null, 0, 20));
    }

    @Test
    void getsAnEstablecimientoById() {
        assertOk(succeeding.getById(ESTABLECIMIENTO, TENANT),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(ESTABLECIMIENTO, TENANT));
    }

    @Test
    void updatesAnEstablecimiento() {
        assertOk(succeeding.update(ESTABLECIMIENTO, TENANT, actualizarEstablecimientoRequest()),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(ESTABLECIMIENTO, TENANT, actualizarEstablecimientoRequest()));
    }

    @Test
    void changesTheStatusOfAnEstablecimiento() {
        assertOk(succeeding.changeStatus(ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO")),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsStatusChangeFailuresToProblemDetails() {
        assertConflict(failing.changeStatus(ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO")));
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que fallan**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:test --tests "*OrganizacionApiMapperTest" --tests "*EmpresaOperadoraControllerTest" --tests "*EstablecimientoControllerTest"`
Expected: FAIL de compilación (`toCambiarEstadoEmpresaCommand`, constructor de 5 parámetros y `changeStatus` no existen).

- [ ] **Step 5: Añadir los métodos al `OrganizacionApiMapper`**

Agregar estos imports:

```java
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
```

y estos métodos justo antes del método privado `toPage`:

```java
    public static CambiarEstadoEmpresaCommand toCambiarEstadoEmpresaCommand(
            UUID empresaId, UUID tenantId, CambiarEstadoRequest request) {
        return new CambiarEstadoEmpresaCommand(empresaId, tenantId, request.estado());
    }

    public static CambiarEstadoEstablecimientoCommand toCambiarEstadoEstablecimientoCommand(
            UUID establecimientoId, UUID tenantId, CambiarEstadoRequest request) {
        return new CambiarEstadoEstablecimientoCommand(establecimientoId, tenantId, request.estado());
    }
```

- [ ] **Step 6: Reemplazar `EmpresaOperadoraController.java` por su versión con `PATCH`**

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
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
@RequestMapping(EmpresaOperadoraController.BASE_PATH)
public class EmpresaOperadoraController {

    static final String BASE_PATH = "/api/v1/organizacion/empresas";

    private final CrearEmpresaOperadoraUseCase createEmpresa;
    private final ActualizarEmpresaOperadoraUseCase updateEmpresa;
    private final ListarEmpresasUseCase listEmpresas;
    private final ObtenerEmpresaUseCase getEmpresa;
    private final CambiarEstadoEmpresaUseCase changeEmpresaStatus;

    public EmpresaOperadoraController(
            CrearEmpresaOperadoraUseCase createEmpresa,
            ActualizarEmpresaOperadoraUseCase updateEmpresa,
            ListarEmpresasUseCase listEmpresas,
            ObtenerEmpresaUseCase getEmpresa,
            CambiarEstadoEmpresaUseCase changeEmpresaStatus) {
        this.createEmpresa = createEmpresa;
        this.updateEmpresa = updateEmpresa;
        this.listEmpresas = listEmpresas;
        this.getEmpresa = getEmpresa;
        this.changeEmpresaStatus = changeEmpresaStatus;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearEmpresaOperadoraRequest request) {
        return createEmpresa.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
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

    @PatchMapping("/{empresaId}/estado")
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID empresaId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return changeEmpresaStatus.execute(
                OrganizacionApiMapper.toCambiarEstadoEmpresaCommand(empresaId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
```

- [ ] **Step 7: Reemplazar `EstablecimientoController.java` por su versión con `PATCH`**

```java
package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEstablecimientosQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstablecimientoQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEstablecimientosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstablecimientoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
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
@RequestMapping(EstablecimientoController.BASE_PATH)
public class EstablecimientoController {

    static final String BASE_PATH = "/api/v1/organizacion/establecimientos";

    private final CrearEstablecimientoUseCase createEstablecimiento;
    private final ActualizarEstablecimientoUseCase updateEstablecimiento;
    private final ListarEstablecimientosUseCase listEstablecimientos;
    private final ObtenerEstablecimientoUseCase getEstablecimiento;
    private final CambiarEstadoEstablecimientoUseCase changeEstablecimientoStatus;

    public EstablecimientoController(
            CrearEstablecimientoUseCase createEstablecimiento,
            ActualizarEstablecimientoUseCase updateEstablecimiento,
            ListarEstablecimientosUseCase listEstablecimientos,
            ObtenerEstablecimientoUseCase getEstablecimiento,
            CambiarEstadoEstablecimientoUseCase changeEstablecimientoStatus) {
        this.createEstablecimiento = createEstablecimiento;
        this.updateEstablecimiento = updateEstablecimiento;
        this.listEstablecimientos = listEstablecimientos;
        this.getEstablecimiento = getEstablecimiento;
        this.changeEstablecimientoStatus = changeEstablecimientoStatus;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.establecimientos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearEstablecimientoRequest request) {
        return createEstablecimiento.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.establecimientos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listEstablecimientos.execute(
                new ListarEstablecimientosQuery(tenantId, empresaId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toEstablecimientoPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{establecimientoId}")
    @PreAuthorize("hasAuthority('organizacion.establecimientos.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID establecimientoId, @RequestParam UUID tenantId) {
        return getEstablecimiento.execute(new ObtenerEstablecimientoQuery(tenantId, establecimientoId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{establecimientoId}")
    @PreAuthorize("hasAuthority('organizacion.establecimientos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID establecimientoId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody ActualizarEstablecimientoRequest request) {
        return updateEstablecimiento.execute(
                OrganizacionApiMapper.toCommand(establecimientoId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PatchMapping("/{establecimientoId}/estado")
    @PreAuthorize("hasAuthority('organizacion.establecimientos.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID establecimientoId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return changeEstablecimientoStatus.execute(
                OrganizacionApiMapper.toCambiarEstadoEstablecimientoCommand(
                        establecimientoId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
```

- [ ] **Step 8: Registrar los beans y ampliar el test de configuración**

En `OrganizacionModuleConfiguration.java` agregar los imports:

```java
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.usecase.command.CambiarEstadoEmpresaHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CambiarEstadoEstablecimientoHandler;
```

y estos beans, junto a los demás de comandos:

```java
    @Bean
    CambiarEstadoEmpresaUseCase cambiarEstadoEmpresaUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new CambiarEstadoEmpresaHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    CambiarEstadoEstablecimientoUseCase cambiarEstadoEstablecimientoUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new CambiarEstadoEstablecimientoHandler(readPort, writePort, organizacionClockPort);
    }
```

En `OrganizacionModuleConfigurationTest.java`, dentro de `wiresEveryCommandUseCase()` agregar al final:

```java
        assertThat(configuration.cambiarEstadoEmpresaUseCase(readPort, writePort, clock)).isNotNull();
        assertThat(configuration.cambiarEstadoEstablecimientoUseCase(readPort, writePort, clock)).isNotNull();
```

- [ ] **Step 9: Ejecutar el módulo con el gate de cobertura**

Run: `cd service-botica; .\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`, sin violaciones de JaCoCo.

- [ ] **Step 10: Commit**

```bash
git add service-botica/modules/organizacion
git commit -m "feat(organizacion): exponer PATCH de estado para empresa y establecimiento

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Pruebas de integración HTTP y actualización de `CLAUDE.md`

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/organizacion/api/OrganizacionApiIntegrationTest.java`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: los endpoints `PATCH` del Task 3 y los helpers privados existentes del test (`createEmpresa`, `createEstablecimiento`, `bearer()`, `BASE`, `TENANT_ID`).

- [ ] **Step 1: Escribir los tests de integración (fallan hasta que corra el contexto con los endpoints nuevos)**

En `OrganizacionApiIntegrationTest.java` agregar el import estático:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```

y estos dos tests dentro de la clase, antes de los métodos privados:

```java
    @Test
    void changesTheStatusOfAnEmpresaAndAnEstablecimiento() throws Exception {
        var empresaId = createEmpresa("20123456789");
        var establecimientoId = createEstablecimiento(empresaId, "EST001", null);

        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", empresaId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        mockMvc.perform(patch(BASE + "/establecimientos/{id}/estado", establecimientoId)
                        .header("Authorization", bearer()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CLAUSURADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("CLAUSURADO"));

        mockMvc.perform(get("/api/v1/estructura-corporativa").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.companies[0].establishments[0].status").value("INACTIVE"));
    }

    @Test
    void rejectsInvalidStatusUnknownResourcesAndMissingPermission() throws Exception {
        var empresaId = createEmpresa("20123456789");
        var establecimientoId = createEstablecimiento(empresaId, "EST001", null);

        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", empresaId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"FOO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", empresaId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/establecimientos/{id}/estado", establecimientoId)
                        .header("Authorization", bearer()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", UUID.randomUUID())
                        .header("Authorization", bearer()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE + "/establecimientos/{id}/estado", UUID.randomUUID())
                        .header("Authorization", bearer()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", empresaId).with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos"))
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 2: Ejecutar el test de integración**

Run: `cd service-botica; .\gradlew.bat :bootstrap-app:test --tests "*OrganizacionApiIntegrationTest"`
Expected: 6 tests PASS (los 4 previos y los 2 nuevos). Requiere Docker en ejecución (Testcontainers). Si un caso devuelve 403 en lugar de lo esperado, confirmar que `V025` está aplicada (los permisos `organizacion.*` vienen de esa migración).

- [ ] **Step 3: Actualizar `CLAUDE.md`**

En la sección "Estado real del proyecto", dentro del punto de `service-botica/`, reemplazar exactamente esta frase:

```
`organizacion` expone CRUD REST real (CQRS + `Result` + `@PreAuthorize`) para Empresa Operadora, Establecimiento, Almacén y Terminal POS bajo `/api/v1/organizacion/*`, más
```

por:

```
`organizacion` expone CRUD REST real (CQRS + `Result` + `@PreAuthorize`) para Empresa Operadora, Establecimiento, Almacén y Terminal POS bajo `/api/v1/organizacion/*` (Empresa y Establecimiento con `PATCH .../estado`; Almacén y Terminal cambian de estado dentro de su `PUT`), más
```

- [ ] **Step 4: Verificar el backend completo**

Run: `cd service-botica; .\gradlew.bat check --warning-mode all`
Expected: `BUILD SUCCESSFUL` (incluye ArchUnit, Spring Modulith y `bootstrap-app:test`).

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app/src/test CLAUDE.md
git commit -m "test(organizacion): cubrir el cambio de estado con integracion HTTP

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## FASE 2 — FRONTEND

Todos los comandos de esta fase se ejecutan en **PowerShell** desde `frontend` y usan `pnpm.cmd`. Las rutas de archivo del plan usan `FE` = `frontend/apps/erp-web/src/features/organizacion` y `UI` = `frontend/packages/ui-web/src`.

### Task 5: Verificar el toolchain del frontend y crear `Select` en `ui-web`

**Files:**
- Create: `frontend/packages/ui-web/src/select/Select.tsx`
- Create: `frontend/packages/ui-web/src/select/Select.test.tsx`
- Modify: `frontend/packages/ui-web/src/index.ts`

**Interfaces:**
- Produces: `Select` y `type SelectProps` exportados desde `@boticas/ui-web`. `Select` acepta todas las props de `<select>` (incluido `ref`) y `className` se fusiona con las clases base.

- [ ] **Step 1: Verificar que las dependencias del frontend son utilizables**

Run (PowerShell): `cd frontend; pnpm.cmd exec vitest --version`
Expected: imprime `4.1.x`. Si falla con `Cannot find module ...\node_modules\vitest\vitest.mjs` (enlaces de `node_modules` inservibles para Node nativo), restaurar con el comando documentado en `CLAUDE.md`, **después de detener cualquier `pnpm dev` en ejecución** porque la reinstalación reemplaza `node_modules`:

Run: `cd frontend; pnpm.cmd install --frozen-lockfile`
Expected: termina sin errores; repetir `pnpm.cmd exec vitest --version`. Si `pnpm` aborta con `ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY`, definir `$env:CI = 'true'` en esa sesión de PowerShell y repetir el comando.

- [ ] **Step 2: Comprobar la línea base verde**

Run: `cd frontend; pnpm.cmd test`
Expected: todas las suites PASS y el umbral de cobertura se cumple. Si algo falla antes de cambiar código, detenerse y reportarlo: no es responsabilidad de este plan.

- [ ] **Step 3: Escribir el test de `Select` (falla: el módulo no existe)**

```tsx
import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { Select } from './Select';

describe('Select', () => {
  it('renderiza un select con sus opciones y las clases base', () => {
    render(
      <Select aria-label="Tipo">
        <option value="a">A</option>
        <option value="b">B</option>
      </Select>
    );
    const select = screen.getByLabelText('Tipo');
    expect(select.tagName).toBe('SELECT');
    expect(screen.getAllByRole('option')).toHaveLength(2);
    expect(select.className).toContain('rounded-xl');
    expect(select.className).toContain('focus:border-primary-600');
  });

  it('acepta ref para integrarse con react-hook-form', () => {
    const ref = createRef<HTMLSelectElement>();
    render(<Select aria-label="Perfil" ref={ref} />);
    expect(ref.current).toBe(screen.getByLabelText('Perfil'));
  });

  it('fusiona className adicional con las clases base', () => {
    render(<Select aria-label="Estado" className="w-40" />);
    const select = screen.getByLabelText('Estado');
    expect(select.className).toContain('w-40');
    expect(select.className).toContain('rounded-xl');
  });

  it('reenvía props estándar como disabled y aria-invalid', () => {
    render(<Select aria-label="Moneda" disabled aria-invalid />);
    const select = screen.getByLabelText('Moneda');
    expect(select).toBeDisabled();
    expect(select).toHaveAttribute('aria-invalid', 'true');
  });
});
```

- [ ] **Step 4: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run packages/ui-web/src/select --project ui-web`
Expected: FAIL (`Failed to resolve import "./Select"`).

- [ ] **Step 5: Implementar `Select`**

```tsx
import type { ComponentPropsWithRef } from 'react';
import { cn } from '../lib/cn';

export type SelectProps = ComponentPropsWithRef<'select'>;

export function Select({ className, ref, ...props }: SelectProps) {
  return (
    <select
      ref={ref}
      className={cn(
        'focus:border-primary-600 focus:ring-primary-100 aria-invalid:border-danger-400 aria-invalid:focus:ring-danger-100 dark:focus:ring-primary-900/40 h-11 w-full rounded-xl border border-neutral-200 bg-white px-4 text-sm text-neutral-900 shadow-sm outline-none transition hover:border-neutral-300 focus:ring-4 disabled:cursor-not-allowed disabled:bg-neutral-50 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:hover:border-neutral-600',
        className
      )}
      {...props}
    />
  );
}
```

- [ ] **Step 6: Exportar desde el índice**

En `frontend/packages/ui-web/src/index.ts`, agregar esta línea después del export de `Input`:

```ts
export { Select, type SelectProps } from './select/Select';
```

- [ ] **Step 7: Ejecutar y verificar que pasa**

Run: `cd frontend; pnpm.cmd exec vitest run packages/ui-web/src/select --project ui-web`
Expected: 4 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/packages/ui-web
git commit -m "feat(ui-web): agregar componente Select

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Utilidades y hooks de la feature `organizacion`

**Files:**
- Create: `frontend/apps/erp-web/src/features/organizacion/lib/describe-api-error.ts` y `describe-api-error.test.ts`
- Create: `frontend/apps/erp-web/src/features/organizacion/lib/format.ts` y `format.test.ts`
- Create: `frontend/apps/erp-web/src/features/organizacion/lib/form-values.ts` y `form-values.test.ts`
- Create: `frontend/apps/erp-web/src/features/organizacion/lib/use-tenant-id.ts` y `use-tenant-id.test.tsx`
- Create: `frontend/apps/erp-web/src/features/organizacion/lib/use-route-param.ts` y `use-route-param.test.tsx`

**Interfaces:**
- Produces: `describeApiError(error: unknown): string`; `valueOrDash(value: string | null): string`; `yesNo(value: boolean): string`; `emptyToUndefined(value: string): string | undefined`; `toNumberOrUndefined(value: string): number | undefined`; `useTenantId(): string` (devuelve `''` si no hay tenant); `useRouteParam(name: string): string`.

- [ ] **Step 1: Escribir los tests (fallan: los módulos no existen)**

`describe-api-error.test.ts`:

```ts
import { ApiError } from '@boticas/api-client';
import { describeApiError } from './describe-api-error';

describe('describeApiError', () => {
  it('usa el detalle del problema cuando existe', () => {
    const error = new ApiError('Conflict', 409, { detail: 'Ya existe una empresa con el RUC indicado.' });
    expect(describeApiError(error)).toBe('Ya existe una empresa con el RUC indicado.');
  });

  it('usa el título del problema cuando no hay detalle', () => {
    expect(describeApiError(new ApiError('Bad Request', 400, { title: 'Solicitud inválida' }))).toBe(
      'Solicitud inválida'
    );
  });

  it('usa el mensaje del error cuando no hay problema', () => {
    expect(describeApiError(new ApiError('Fallo inesperado', 500))).toBe('Fallo inesperado');
  });

  it('explica la falta de permisos en un 403', () => {
    expect(describeApiError(new ApiError('Forbidden', 403))).toBe('No tienes permiso para esta acción.');
  });

  it('explica el recurso inexistente en un 404', () => {
    expect(describeApiError(new ApiError('Not Found', 404))).toBe(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('devuelve un mensaje genérico para errores que no vienen de la API', () => {
    expect(describeApiError(new Error('boom'))).toBe(
      'No se pudo completar la operación. Inténtalo de nuevo.'
    );
  });
});
```

`format.test.ts`:

```ts
import { valueOrDash, yesNo } from './format';

describe('format', () => {
  it('valueOrDash devuelve el valor cuando existe', () => {
    expect(valueOrDash('Av. Principal 100')).toBe('Av. Principal 100');
  });

  it('valueOrDash devuelve un guion para null y cadenas vacías', () => {
    expect(valueOrDash(null)).toBe('—');
    expect(valueOrDash('')).toBe('—');
  });

  it('yesNo traduce booleanos', () => {
    expect(yesNo(true)).toBe('Sí');
    expect(yesNo(false)).toBe('No');
  });
});
```

`form-values.test.ts`:

```ts
import { emptyToUndefined, toNumberOrUndefined } from './form-values';

describe('form-values', () => {
  it('emptyToUndefined recorta y convierte vacíos en undefined', () => {
    expect(emptyToUndefined('  Botica  ')).toBe('Botica');
    expect(emptyToUndefined('')).toBeUndefined();
    expect(emptyToUndefined('   ')).toBeUndefined();
  });

  it('toNumberOrUndefined convierte texto numérico y vacíos', () => {
    expect(toNumberOrUndefined('-12.5')).toBe(-12.5);
    expect(toNumberOrUndefined(' 4 ')).toBe(4);
    expect(toNumberOrUndefined('')).toBeUndefined();
    expect(toNumberOrUndefined('  ')).toBeUndefined();
  });
});
```

`use-tenant-id.test.tsx`:

```tsx
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { AuthSessionContext, type AuthSession } from '../../auth/model/auth-session.context';
import { useTenantId } from './use-tenant-id';

function sessionWith(tenantId: string | null): AuthSession {
  return {
    status: 'authenticated',
    authenticated: true,
    accessToken: 'token',
    tenantId,
    userId: 'user-1',
    authenticate: vi.fn(),
    signOut: vi.fn()
  };
}

function wrapperFor(session: AuthSession) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <AuthSessionContext value={session}>{children}</AuthSessionContext>;
  };
}

describe('useTenantId', () => {
  it('devuelve el tenant de la sesión', () => {
    const { result } = renderHook(() => useTenantId(), { wrapper: wrapperFor(sessionWith('tenant-1')) });
    expect(result.current).toBe('tenant-1');
  });

  it('devuelve cadena vacía cuando la sesión no tiene tenant', () => {
    const { result } = renderHook(() => useTenantId(), { wrapper: wrapperFor(sessionWith(null)) });
    expect(result.current).toBe('');
  });
});
```

`use-route-param.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { useRouteParam } from './use-route-param';

function Probe() {
  return <p>{useRouteParam('empresaId')}</p>;
}

describe('useRouteParam', () => {
  it('lee el parámetro de la ruta activa', () => {
    const router = createMemoryRouter([{ path: '/empresas/:empresaId', Component: Probe }], {
      initialEntries: ['/empresas/abc-123']
    });

    render(<RouterProvider router={router} />);

    expect(screen.getByText('abc-123')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib --project erp-web`
Expected: FAIL (`Failed to resolve import` para cada módulo).

- [ ] **Step 3: Implementar las utilidades**

`describe-api-error.ts`:

```ts
import { ApiError } from '@boticas/api-client';

export function describeApiError(error: unknown): string {
  if (!(error instanceof ApiError)) return 'No se pudo completar la operación. Inténtalo de nuevo.';
  if (error.status === 403) return 'No tienes permiso para esta acción.';
  if (error.status === 404) return 'El recurso no existe o no pertenece a tu organización.';
  return error.problem?.detail ?? error.problem?.title ?? error.message;
}
```

`format.ts`:

```ts
export function valueOrDash(value: string | null): string {
  return value === null || value === '' ? '—' : value;
}

export function yesNo(value: boolean): string {
  return value ? 'Sí' : 'No';
}
```

`form-values.ts`:

```ts
export function emptyToUndefined(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

export function toNumberOrUndefined(value: string): number | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : Number(trimmed);
}
```

`use-tenant-id.ts`:

```ts
import { useAuthSession } from '../../auth';

export function useTenantId(): string {
  return useAuthSession().tenantId ?? '';
}
```

`use-route-param.ts`:

```ts
import { useParams } from 'react-router';

export function useRouteParam(name: string): string {
  return useParams()[name] as string;
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan con cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/lib/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 5 archivos de test PASS y la tabla de cobertura muestra 100% en los cinco archivos de `lib/`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/lib
git commit -m "feat(organizacion): agregar utilidades y hooks compartidos de la UI

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Capa de API por entidad

**Files:**
- Create: `FE/api/pagina.types.ts`
- Create: `FE/api/query-string.ts` y `query-string.test.ts`
- Create: `FE/api/invalidate.ts` y `invalidate.test.ts`
- Create: `FE/api/empresas.types.ts`, `FE/api/empresas.api.ts`, `FE/api/empresas.api.test.ts`
- Create: `FE/api/establecimientos.types.ts`, `FE/api/establecimientos.api.ts`, `FE/api/establecimientos.api.test.ts`
- Create: `FE/api/almacenes.types.ts`, `FE/api/almacenes.api.ts`, `FE/api/almacenes.api.test.ts`
- Create: `FE/api/terminales.types.ts`, `FE/api/terminales.api.ts`, `FE/api/terminales.api.test.ts`

(`FE` = `frontend/apps/erp-web/src/features/organizacion`.)

**Interfaces:**
- Consumes: `apiClient` de `frontend/apps/erp-web/src/app/api`; `ApiClient` de `@boticas/api-client` (`get/post/put/patch`).
- Produces (todos exportados desde sus archivos):
  - `PaginaResponse<T>`; `buildQuery(params): string`; `invalidateOrganizacion(queryClient): Promise<void>`.
  - Empresas: `ESTADOS_EMPRESA`, `EstadoEmpresa`, `Empresa`, `CrearEmpresaPayload`, `ActualizarEmpresaPayload`; `fetchEmpresas(client, {tenantId, search?, page?, size?})`, `empresasQuery(params)`, `fetchEmpresa(client, tenantId, empresaId)`, `empresaQuery(tenantId, empresaId)`, `crearEmpresa(client, payload)`, `actualizarEmpresa(client, empresaId, tenantId, payload)`, `cambiarEstadoEmpresa(client, empresaId, tenantId, estado)`.
  - Establecimientos: `ESTADOS_ESTABLECIMIENTO`, `TIPOS_ESTABLECIMIENTO`, `PERFILES_OPERACION`, `EstadoEstablecimiento`, `TipoEstablecimiento`, `PerfilOperacion`, `Establecimiento`, `CrearEstablecimientoPayload`, `ActualizarEstablecimientoPayload`; `fetchEstablecimientos(client, {tenantId, empresaId?, search?, page?, size?})`, `establecimientosQuery(params)`, `fetchEstablecimiento`, `establecimientoQuery(tenantId, id)`, `crearEstablecimiento`, `actualizarEstablecimiento`, `cambiarEstadoEstablecimiento`.
  - Almacenes: `TIPOS_ALMACEN`, `TipoAlmacen`, `Almacen`, `CrearAlmacenPayload`, `ActualizarAlmacenPayload`; `fetchAlmacenes(client, {tenantId, establecimientoId?, search?, page?, size?})`, `almacenesQuery(params)`, `crearAlmacen`, `actualizarAlmacen`.
  - Terminales: `ESTADOS_TERMINAL`, `EstadoTerminal`, `Terminal`, `CrearTerminalPayload`, `ActualizarTerminalPayload`; `fetchTerminales(...)`, `terminalesQuery(params)`, `crearTerminal`, `actualizarTerminal`.

- [ ] **Step 1: Crear los tipos (no tienen comportamiento; sirven a los tests siguientes)**

`FE/api/pagina.types.ts`:

```ts
export type PaginaResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
};
```

`FE/api/empresas.types.ts`:

```ts
export const ESTADOS_EMPRESA = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

export type EstadoEmpresa = (typeof ESTADOS_EMPRESA)[number];

export type Empresa = {
  id: string;
  tenantId: string;
  ruc: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccionFiscal: string | null;
  ubigeoFiscal: string | null;
  telefono: string | null;
  email: string | null;
  sitioWeb: string | null;
  monedaFuncional: string;
  zonaHoraria: string;
  permiteVentaOnline: boolean;
  estado: EstadoEmpresa;
  createdAt: string;
  updatedAt: string | null;
};

export type ActualizarEmpresaPayload = {
  razonSocial: string;
  nombreComercial?: string | undefined;
  direccionFiscal?: string | undefined;
  ubigeoFiscal?: string | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  sitioWeb?: string | undefined;
  monedaFuncional: string;
  zonaHoraria: string;
  permiteVentaOnline: boolean;
};

export type CrearEmpresaPayload = ActualizarEmpresaPayload & {
  tenantId: string;
  ruc: string;
};
```

`FE/api/establecimientos.types.ts`:

```ts
export const ESTADOS_ESTABLECIMIENTO = ['ACTIVO', 'SUSPENDIDO', 'CLAUSURADO', 'REMODELACION'] as const;
export const TIPOS_ESTABLECIMIENTO = ['BOTICA'] as const;
export const PERFILES_OPERACION = ['ONLINE', 'STORE_EDGE'] as const;

export type EstadoEstablecimiento = (typeof ESTADOS_ESTABLECIMIENTO)[number];
export type TipoEstablecimiento = (typeof TIPOS_ESTABLECIMIENTO)[number];
export type PerfilOperacion = (typeof PERFILES_OPERACION)[number];

export type Establecimiento = {
  id: string;
  tenantId: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  tipoEstablecimiento: TipoEstablecimiento;
  categoriaRegulatoriaCodigo: string | null;
  codigoAnexoSunat: string;
  codigoDigemid: string | null;
  direccion: string | null;
  ubigeo: string | null;
  referencia: string | null;
  latitud: number | null;
  longitud: number | null;
  telefono: string | null;
  email: string | null;
  esPrincipal: boolean;
  permiteVentaOnline: boolean;
  permiteDelivery: boolean;
  perfilOperacion: PerfilOperacion;
  zonaHoraria: string;
  estadoOperativo: EstadoEstablecimiento;
  createdAt: string;
  updatedAt: string | null;
};

export type ActualizarEstablecimientoPayload = {
  nombre: string;
  tipoEstablecimiento: TipoEstablecimiento;
  categoriaRegulatoriaCodigo?: string | undefined;
  codigoAnexoSunat: string;
  codigoDigemid?: string | undefined;
  direccion?: string | undefined;
  ubigeo?: string | undefined;
  referencia?: string | undefined;
  latitud?: number | undefined;
  longitud?: number | undefined;
  telefono?: string | undefined;
  email?: string | undefined;
  esPrincipal: boolean;
  permiteVentaOnline: boolean;
  permiteDelivery: boolean;
  perfilOperacion: PerfilOperacion;
  zonaHoraria: string;
};

export type CrearEstablecimientoPayload = ActualizarEstablecimientoPayload & {
  tenantId: string;
  empresaId: string;
  codigo: string;
};
```

`FE/api/almacenes.types.ts`:

```ts
export const TIPOS_ALMACEN = [
  'VENTA',
  'GENERAL',
  'CUARENTENA',
  'REFRIGERADO',
  'PSICOTROPICO',
  'MERMA'
] as const;

export type TipoAlmacen = (typeof TIPOS_ALMACEN)[number];

export type Almacen = {
  id: string;
  tenantId: string;
  establecimientoId: string;
  codigo: string;
  nombre: string;
  tipo: TipoAlmacen;
  permiteLotes: boolean;
  permiteVencimiento: boolean;
  permiteVenta: boolean;
  permiteDespacho: boolean;
  controlTemperatura: boolean;
  temperaturaMinC: number | null;
  temperaturaMaxC: number | null;
  activo: boolean;
  createdAt: string;
  updatedAt: string | null;
};

type AlmacenDatos = {
  nombre: string;
  tipo: TipoAlmacen;
  permiteLotes: boolean;
  permiteVencimiento: boolean;
  permiteVenta: boolean;
  permiteDespacho: boolean;
  controlTemperatura: boolean;
  temperaturaMinC?: number | undefined;
  temperaturaMaxC?: number | undefined;
};

export type CrearAlmacenPayload = AlmacenDatos & {
  tenantId: string;
  establecimientoId: string;
  codigo: string;
};

export type ActualizarAlmacenPayload = AlmacenDatos & {
  activo: boolean;
};
```

`FE/api/terminales.types.ts`:

```ts
export const ESTADOS_TERMINAL = ['ACTIVO', 'BLOQUEADO', 'MANTENIMIENTO'] as const;

export type EstadoTerminal = (typeof ESTADOS_TERMINAL)[number];

export type Terminal = {
  id: string;
  tenantId: string;
  establecimientoId: string;
  codigo: string;
  nombre: string;
  serieBoletaDefecto: string | null;
  serieFacturaDefecto: string | null;
  numeroSerieEquipo: string | null;
  hostname: string | null;
  ipEquipo: string | null;
  impresoraCodigo: string | null;
  storeEdgeHabilitado: boolean;
  estado: EstadoTerminal;
  createdAt: string;
  updatedAt: string | null;
};

type TerminalDatos = {
  nombre: string;
  serieBoletaDefecto?: string | undefined;
  serieFacturaDefecto?: string | undefined;
  numeroSerieEquipo?: string | undefined;
  hostname?: string | undefined;
  ipEquipo?: string | undefined;
  impresoraCodigo?: string | undefined;
  storeEdgeHabilitado: boolean;
};

export type CrearTerminalPayload = TerminalDatos & {
  tenantId: string;
  establecimientoId: string;
  codigo: string;
};

export type ActualizarTerminalPayload = TerminalDatos & {
  estado: EstadoTerminal;
};
```

- [ ] **Step 2: Escribir los tests de `query-string` e `invalidate` (fallan: módulos inexistentes)**

`FE/api/query-string.test.ts`:

```ts
import { buildQuery } from './query-string';

describe('buildQuery', () => {
  it('serializa cadenas y números', () => {
    expect(buildQuery({ tenantId: 't-1', page: 0, size: 20 })).toBe('tenantId=t-1&page=0&size=20');
  });

  it('omite valores undefined y cadenas vacías', () => {
    expect(buildQuery({ tenantId: 't-1', search: '', empresaId: undefined })).toBe('tenantId=t-1');
  });

  it('codifica caracteres especiales', () => {
    expect(buildQuery({ search: 'a b&c' })).toBe('search=a+b%26c');
  });
});
```

`FE/api/invalidate.test.ts`:

```ts
import { QueryClient } from '@tanstack/react-query';
import { invalidateOrganizacion } from './invalidate';

describe('invalidateOrganizacion', () => {
  it('invalida las consultas de la feature y la estructura corporativa', async () => {
    const queryClient = new QueryClient();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');

    await invalidateOrganizacion(queryClient);

    expect(spy).toHaveBeenCalledWith({ queryKey: ['organizacion'] });
    expect(spy).toHaveBeenCalledWith({ queryKey: ['organization', 'corporate-structure'] });
  });
});
```

- [ ] **Step 3: Implementar `query-string` e `invalidate`**

`FE/api/query-string.ts`:

```ts
export function buildQuery(params: Record<string, string | number | undefined>): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value));
  });
  return query.toString();
}
```

`FE/api/invalidate.ts`:

```ts
import type { QueryClient } from '@tanstack/react-query';

export async function invalidateOrganizacion(queryClient: QueryClient): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['organizacion'] }),
    queryClient.invalidateQueries({ queryKey: ['organization', 'corporate-structure'] })
  ]);
}
```

- [ ] **Step 4: Escribir y ejecutar el test de `empresas.api` (falla: módulo inexistente)**

`FE/api/empresas.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarEmpresa,
  cambiarEstadoEmpresa,
  crearEmpresa,
  empresaQuery,
  empresasQuery,
  fetchEmpresa,
  fetchEmpresas
} from './empresas.api';
import type { Empresa } from './empresas.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleEmpresa: Empresa = {
  id: 'empresa-1',
  tenantId: 'tenant-1',
  ruc: '20123456789',
  razonSocial: 'Boticas SAC',
  nombreComercial: null,
  direccionFiscal: null,
  ubigeoFiscal: null,
  telefono: null,
  email: null,
  sitioWeb: null,
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

describe('empresas.api', () => {
  it('fetchEmpresas consulta con tenantId, page y size por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleEmpresa], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchEmpresas(client, { tenantId: 'tenant-1' });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(receivedUrl?.searchParams.has('search')).toBe(false);
    expect(result.items).toEqual([sampleEmpresa]);
  });

  it('fetchEmpresas envía search, page y size cuando se especifican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 2, size: 50, totalElements: 0 });
      })
    );

    await fetchEmpresas(client, { tenantId: 'tenant-1', search: 'bot', page: 2, size: 50 });

    expect(receivedUrl?.searchParams.get('search')).toBe('bot');
    expect(receivedUrl?.searchParams.get('page')).toBe('2');
    expect(receivedUrl?.searchParams.get('size')).toBe('50');
  });

  it('fetchEmpresa consulta el detalle con el tenantId como query', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/empresas/empresa-1', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json(sampleEmpresa);
      })
    );

    const result = await fetchEmpresa(client, 'tenant-1', 'empresa-1');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(result).toEqual(sampleEmpresa);
  });

  it('crearEmpresa envía el payload y devuelve la empresa creada', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/empresas', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleEmpresa, { status: 201 });
      })
    );
    const payload = {
      tenantId: 'tenant-1',
      ruc: '20123456789',
      razonSocial: 'Boticas SAC',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: false
    };

    const result = await crearEmpresa(client, payload);

    expect(receivedBody).toEqual(payload);
    expect(result).toEqual(sampleEmpresa);
  });

  it('actualizarEmpresa usa PUT con tenantId como query y el payload como body', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/empresas/empresa-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json(sampleEmpresa);
      })
    );
    const payload = {
      razonSocial: 'Boticas del Perú SAC',
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: true
    };

    await actualizarEmpresa(client, 'empresa-1', 'tenant-1', payload);

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual(payload);
  });

  it('cambiarEstadoEmpresa usa PATCH /estado con el estado en el body', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/organizacion/empresas/empresa-1/estado',
        async ({ request }) => {
          receivedUrl = new URL(request.url);
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleEmpresa, estado: 'SUSPENDIDO' });
        }
      )
    );

    const result = await cambiarEstadoEmpresa(client, 'empresa-1', 'tenant-1', 'SUSPENDIDO');

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ estado: 'SUSPENDIDO' });
    expect(result.estado).toBe('SUSPENDIDO');
  });

  it('empresasQuery y empresaQuery definen claves estables por parámetros', () => {
    expect(empresasQuery({ tenantId: 'tenant-1', search: 'bot', page: 1, size: 10 }).queryKey).toEqual([
      'organizacion',
      'empresas',
      'lista',
      'tenant-1',
      'bot',
      1,
      10
    ]);
    expect(empresasQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'empresas',
      'lista',
      'tenant-1',
      '',
      0,
      20
    ]);
    expect(empresaQuery('tenant-1', 'empresa-1').queryKey).toEqual([
      'organizacion',
      'empresas',
      'detalle',
      'tenant-1',
      'empresa-1'
    ]);
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/api/empresas.api.test.ts --project erp-web`
Expected: FAIL (`Failed to resolve import "./empresas.api"`).

- [ ] **Step 5: Implementar `empresas.api.ts`**

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type {
  ActualizarEmpresaPayload,
  CrearEmpresaPayload,
  Empresa,
  EstadoEmpresa
} from './empresas.types';

export type FetchEmpresasParams = {
  tenantId: string;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchEmpresas(
  client: ApiClient,
  params: FetchEmpresasParams
): Promise<PaginaResponse<Empresa>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Empresa>>(`/organizacion/empresas?${query}`);
}

export function empresasQuery(params: FetchEmpresasParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'empresas',
      'lista',
      params.tenantId,
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchEmpresas(apiClient, params)
  });
}

export function fetchEmpresa(client: ApiClient, tenantId: string, empresaId: string): Promise<Empresa> {
  return client.get<Empresa>(`/organizacion/empresas/${empresaId}?${buildQuery({ tenantId })}`);
}

export function empresaQuery(tenantId: string, empresaId: string) {
  return queryOptions({
    queryKey: ['organizacion', 'empresas', 'detalle', tenantId, empresaId],
    queryFn: () => fetchEmpresa(apiClient, tenantId, empresaId)
  });
}

export function crearEmpresa(client: ApiClient, payload: CrearEmpresaPayload): Promise<Empresa> {
  return client.post<Empresa, CrearEmpresaPayload>('/organizacion/empresas', payload);
}

export function actualizarEmpresa(
  client: ApiClient,
  empresaId: string,
  tenantId: string,
  payload: ActualizarEmpresaPayload
): Promise<Empresa> {
  return client.put<Empresa, ActualizarEmpresaPayload>(
    `/organizacion/empresas/${empresaId}?${buildQuery({ tenantId })}`,
    payload
  );
}

export function cambiarEstadoEmpresa(
  client: ApiClient,
  empresaId: string,
  tenantId: string,
  estado: EstadoEmpresa
): Promise<Empresa> {
  return client.patch<Empresa, { estado: EstadoEmpresa }>(
    `/organizacion/empresas/${empresaId}/estado?${buildQuery({ tenantId })}`,
    { estado }
  );
}
```

- [ ] **Step 6: Escribir y ejecutar el test de `establecimientos.api` (falla)**

`FE/api/establecimientos.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import {
  actualizarEstablecimiento,
  cambiarEstadoEstablecimiento,
  crearEstablecimiento,
  establecimientoQuery,
  establecimientosQuery,
  fetchEstablecimiento,
  fetchEstablecimientos
} from './establecimientos.api';
import type { Establecimiento } from './establecimientos.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleEstablecimiento: Establecimiento = {
  id: 'est-1',
  tenantId: 'tenant-1',
  empresaId: 'empresa-1',
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: null,
  codigoAnexoSunat: '0001',
  codigoDigemid: null,
  direccion: null,
  ubigeo: null,
  referencia: null,
  latitud: null,
  longitud: null,
  telefono: null,
  email: null,
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima',
  estadoOperativo: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

const datos = {
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA' as const,
  codigoAnexoSunat: '0001',
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE' as const,
  zonaHoraria: 'America/Lima'
};

describe('establecimientos.api', () => {
  it('fetchEstablecimientos filtra por empresa y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleEstablecimiento], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchEstablecimientos(client, { tenantId: 'tenant-1', empresaId: 'empresa-1' });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedUrl?.searchParams.get('empresaId')).toBe('empresa-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleEstablecimiento]);
  });

  it('fetchEstablecimientos omite empresaId y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 1, size: 100, totalElements: 0 });
      })
    );

    await fetchEstablecimientos(client, { tenantId: 'tenant-1', search: 'cen', page: 1, size: 100 });

    expect(receivedUrl?.searchParams.has('empresaId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('cen');
    expect(receivedUrl?.searchParams.get('size')).toBe('100');
  });

  it('fetchEstablecimiento consulta el detalle', async () => {
    server.use(
      http.get('http://localhost/api/v1/organizacion/establecimientos/est-1', () =>
        HttpResponse.json(sampleEstablecimiento)
      )
    );

    expect(await fetchEstablecimiento(client, 'tenant-1', 'est-1')).toEqual(sampleEstablecimiento);
  });

  it('crearEstablecimiento envía el payload completo', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/establecimientos', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleEstablecimiento, { status: 201 });
      })
    );
    const payload = { ...datos, tenantId: 'tenant-1', empresaId: 'empresa-1', codigo: 'EST001' };

    await crearEstablecimiento(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarEstablecimiento usa PUT con tenantId como query', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/establecimientos/est-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json(sampleEstablecimiento);
      })
    );

    await actualizarEstablecimiento(client, 'est-1', 'tenant-1', datos);

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual(datos);
  });

  it('cambiarEstadoEstablecimiento usa PATCH /estado', async () => {
    let receivedBody: unknown;
    server.use(
      http.patch(
        'http://localhost/api/v1/organizacion/establecimientos/est-1/estado',
        async ({ request }) => {
          receivedBody = await request.json();
          return HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' });
        }
      )
    );

    const result = await cambiarEstadoEstablecimiento(client, 'est-1', 'tenant-1', 'CLAUSURADO');

    expect(receivedBody).toEqual({ estado: 'CLAUSURADO' });
    expect(result.estadoOperativo).toBe('CLAUSURADO');
  });

  it('las consultas definen claves estables por parámetros', () => {
    expect(
      establecimientosQuery({ tenantId: 'tenant-1', empresaId: 'empresa-1', search: 'c', page: 1, size: 5 })
        .queryKey
    ).toEqual(['organizacion', 'establecimientos', 'lista', 'tenant-1', 'empresa-1', 'c', 1, 5]);
    expect(establecimientosQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'establecimientos',
      'lista',
      'tenant-1',
      '',
      '',
      0,
      20
    ]);
    expect(establecimientoQuery('tenant-1', 'est-1').queryKey).toEqual([
      'organizacion',
      'establecimientos',
      'detalle',
      'tenant-1',
      'est-1'
    ]);
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/api/establecimientos.api.test.ts --project erp-web`
Expected: FAIL (`Failed to resolve import "./establecimientos.api"`).

- [ ] **Step 7: Implementar `establecimientos.api.ts`**

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type {
  ActualizarEstablecimientoPayload,
  CrearEstablecimientoPayload,
  EstadoEstablecimiento,
  Establecimiento
} from './establecimientos.types';

export type FetchEstablecimientosParams = {
  tenantId: string;
  empresaId?: string | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchEstablecimientos(
  client: ApiClient,
  params: FetchEstablecimientosParams
): Promise<PaginaResponse<Establecimiento>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    empresaId: params.empresaId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Establecimiento>>(`/organizacion/establecimientos?${query}`);
}

export function establecimientosQuery(params: FetchEstablecimientosParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'establecimientos',
      'lista',
      params.tenantId,
      params.empresaId ?? '',
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchEstablecimientos(apiClient, params)
  });
}

export function fetchEstablecimiento(
  client: ApiClient,
  tenantId: string,
  establecimientoId: string
): Promise<Establecimiento> {
  return client.get<Establecimiento>(
    `/organizacion/establecimientos/${establecimientoId}?${buildQuery({ tenantId })}`
  );
}

export function establecimientoQuery(tenantId: string, establecimientoId: string) {
  return queryOptions({
    queryKey: ['organizacion', 'establecimientos', 'detalle', tenantId, establecimientoId],
    queryFn: () => fetchEstablecimiento(apiClient, tenantId, establecimientoId)
  });
}

export function crearEstablecimiento(
  client: ApiClient,
  payload: CrearEstablecimientoPayload
): Promise<Establecimiento> {
  return client.post<Establecimiento, CrearEstablecimientoPayload>('/organizacion/establecimientos', payload);
}

export function actualizarEstablecimiento(
  client: ApiClient,
  establecimientoId: string,
  tenantId: string,
  payload: ActualizarEstablecimientoPayload
): Promise<Establecimiento> {
  return client.put<Establecimiento, ActualizarEstablecimientoPayload>(
    `/organizacion/establecimientos/${establecimientoId}?${buildQuery({ tenantId })}`,
    payload
  );
}

export function cambiarEstadoEstablecimiento(
  client: ApiClient,
  establecimientoId: string,
  tenantId: string,
  estado: EstadoEstablecimiento
): Promise<Establecimiento> {
  return client.patch<Establecimiento, { estado: EstadoEstablecimiento }>(
    `/organizacion/establecimientos/${establecimientoId}/estado?${buildQuery({ tenantId })}`,
    { estado }
  );
}
```

- [ ] **Step 8: Escribir y ejecutar los tests de `almacenes.api` y `terminales.api` (fallan)**

`FE/api/almacenes.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { actualizarAlmacen, almacenesQuery, crearAlmacen, fetchAlmacenes } from './almacenes.api';
import type { Almacen } from './almacenes.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleAlmacen: Almacen = {
  id: 'alm-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: null,
  temperaturaMaxC: null,
  activo: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

const datos = {
  nombre: 'Almacén Central',
  tipo: 'GENERAL' as const,
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false
};

describe('almacenes.api', () => {
  it('fetchAlmacenes filtra por establecimiento y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/almacenes', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleAlmacen], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchAlmacenes(client, { tenantId: 'tenant-1', establecimientoId: 'est-1' });

    expect(receivedUrl?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(receivedUrl?.searchParams.get('page')).toBe('0');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleAlmacen]);
  });

  it('fetchAlmacenes omite el establecimiento y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/almacenes', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 3, size: 100, totalElements: 0 });
      })
    );

    await fetchAlmacenes(client, { tenantId: 'tenant-1', search: 'alm', page: 3, size: 100 });

    expect(receivedUrl?.searchParams.has('establecimientoId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('alm');
    expect(receivedUrl?.searchParams.get('page')).toBe('3');
  });

  it('crearAlmacen envía el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/almacenes', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleAlmacen, { status: 201 });
      })
    );
    const payload = { ...datos, tenantId: 'tenant-1', establecimientoId: 'est-1', codigo: 'ALM001' };

    await crearAlmacen(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarAlmacen usa PUT con tenantId como query e incluye activo', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/almacenes/alm-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleAlmacen, activo: false });
      })
    );

    const result = await actualizarAlmacen(client, 'alm-1', 'tenant-1', { ...datos, activo: false });

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual({ ...datos, activo: false });
    expect(result.activo).toBe(false);
  });

  it('almacenesQuery define claves estables por parámetros', () => {
    expect(
      almacenesQuery({ tenantId: 'tenant-1', establecimientoId: 'est-1', search: 'a', page: 2, size: 10 })
        .queryKey
    ).toEqual(['organizacion', 'almacenes', 'lista', 'tenant-1', 'est-1', 'a', 2, 10]);
    expect(almacenesQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'almacenes',
      'lista',
      'tenant-1',
      '',
      '',
      0,
      20
    ]);
  });
});
```

`FE/api/terminales.api.test.ts`:

```ts
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { createApiClient } from '@boticas/api-client';
import { actualizarTerminal, crearTerminal, fetchTerminales, terminalesQuery } from './terminales.api';
import type { Terminal } from './terminales.types';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const client = createApiClient({ baseUrl: 'http://localhost/api/v1' });

const sampleTerminal: Terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

describe('terminales.api', () => {
  it('fetchTerminales filtra por establecimiento y usa paginación por defecto', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/terminales-pos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [sampleTerminal], page: 0, size: 20, totalElements: 1 });
      })
    );

    const result = await fetchTerminales(client, { tenantId: 'tenant-1', establecimientoId: 'est-1' });

    expect(receivedUrl?.searchParams.get('establecimientoId')).toBe('est-1');
    expect(receivedUrl?.searchParams.get('size')).toBe('20');
    expect(result.items).toEqual([sampleTerminal]);
  });

  it('fetchTerminales omite el establecimiento y envía search, page y size si se indican', async () => {
    let receivedUrl: URL | undefined;
    server.use(
      http.get('http://localhost/api/v1/organizacion/terminales-pos', ({ request }) => {
        receivedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 1, size: 100, totalElements: 0 });
      })
    );

    await fetchTerminales(client, { tenantId: 'tenant-1', search: 'caj', page: 1, size: 100 });

    expect(receivedUrl?.searchParams.has('establecimientoId')).toBe(false);
    expect(receivedUrl?.searchParams.get('search')).toBe('caj');
  });

  it('crearTerminal envía el payload', async () => {
    let receivedBody: unknown;
    server.use(
      http.post('http://localhost/api/v1/organizacion/terminales-pos', async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json(sampleTerminal, { status: 201 });
      })
    );
    const payload = {
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      storeEdgeHabilitado: false
    };

    await crearTerminal(client, payload);

    expect(receivedBody).toEqual(payload);
  });

  it('actualizarTerminal usa PUT con tenantId como query e incluye estado', async () => {
    let receivedUrl: URL | undefined;
    let receivedBody: unknown;
    server.use(
      http.put('http://localhost/api/v1/organizacion/terminales-pos/term-1', async ({ request }) => {
        receivedUrl = new URL(request.url);
        receivedBody = await request.json();
        return HttpResponse.json({ ...sampleTerminal, estado: 'BLOQUEADO' });
      })
    );
    const payload = { nombre: 'Caja 1', storeEdgeHabilitado: false, estado: 'BLOQUEADO' as const };

    const result = await actualizarTerminal(client, 'term-1', 'tenant-1', payload);

    expect(receivedUrl?.searchParams.get('tenantId')).toBe('tenant-1');
    expect(receivedBody).toEqual(payload);
    expect(result.estado).toBe('BLOQUEADO');
  });

  it('terminalesQuery define claves estables por parámetros', () => {
    expect(
      terminalesQuery({ tenantId: 'tenant-1', establecimientoId: 'est-1', search: 'c', page: 2, size: 10 })
        .queryKey
    ).toEqual(['organizacion', 'terminales', 'lista', 'tenant-1', 'est-1', 'c', 2, 10]);
    expect(terminalesQuery({ tenantId: 'tenant-1' }).queryKey).toEqual([
      'organizacion',
      'terminales',
      'lista',
      'tenant-1',
      '',
      '',
      0,
      20
    ]);
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/api --project erp-web`
Expected: FAIL para `almacenes.api` y `terminales.api` (módulos inexistentes); `empresas.api`, `establecimientos.api`, `query-string` e `invalidate` PASS.

- [ ] **Step 9: Implementar `almacenes.api.ts` y `terminales.api.ts`**

`FE/api/almacenes.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type { ActualizarAlmacenPayload, Almacen, CrearAlmacenPayload } from './almacenes.types';

export type FetchAlmacenesParams = {
  tenantId: string;
  establecimientoId?: string | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchAlmacenes(
  client: ApiClient,
  params: FetchAlmacenesParams
): Promise<PaginaResponse<Almacen>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    establecimientoId: params.establecimientoId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Almacen>>(`/organizacion/almacenes?${query}`);
}

export function almacenesQuery(params: FetchAlmacenesParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'almacenes',
      'lista',
      params.tenantId,
      params.establecimientoId ?? '',
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchAlmacenes(apiClient, params)
  });
}

export function crearAlmacen(client: ApiClient, payload: CrearAlmacenPayload): Promise<Almacen> {
  return client.post<Almacen, CrearAlmacenPayload>('/organizacion/almacenes', payload);
}

export function actualizarAlmacen(
  client: ApiClient,
  almacenId: string,
  tenantId: string,
  payload: ActualizarAlmacenPayload
): Promise<Almacen> {
  return client.put<Almacen, ActualizarAlmacenPayload>(
    `/organizacion/almacenes/${almacenId}?${buildQuery({ tenantId })}`,
    payload
  );
}
```

`FE/api/terminales.api.ts`:

```ts
import { queryOptions } from '@tanstack/react-query';
import type { ApiClient } from '@boticas/api-client';
import { apiClient } from '../../../app/api';
import type { PaginaResponse } from './pagina.types';
import { buildQuery } from './query-string';
import type { ActualizarTerminalPayload, CrearTerminalPayload, Terminal } from './terminales.types';

export type FetchTerminalesParams = {
  tenantId: string;
  establecimientoId?: string | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
};

export function fetchTerminales(
  client: ApiClient,
  params: FetchTerminalesParams
): Promise<PaginaResponse<Terminal>> {
  const query = buildQuery({
    tenantId: params.tenantId,
    establecimientoId: params.establecimientoId,
    search: params.search,
    page: params.page ?? 0,
    size: params.size ?? 20
  });
  return client.get<PaginaResponse<Terminal>>(`/organizacion/terminales-pos?${query}`);
}

export function terminalesQuery(params: FetchTerminalesParams) {
  return queryOptions({
    queryKey: [
      'organizacion',
      'terminales',
      'lista',
      params.tenantId,
      params.establecimientoId ?? '',
      params.search ?? '',
      params.page ?? 0,
      params.size ?? 20
    ],
    queryFn: () => fetchTerminales(apiClient, params)
  });
}

export function crearTerminal(client: ApiClient, payload: CrearTerminalPayload): Promise<Terminal> {
  return client.post<Terminal, CrearTerminalPayload>('/organizacion/terminales-pos', payload);
}

export function actualizarTerminal(
  client: ApiClient,
  terminalId: string,
  tenantId: string,
  payload: ActualizarTerminalPayload
): Promise<Terminal> {
  return client.put<Terminal, ActualizarTerminalPayload>(
    `/organizacion/terminales-pos/${terminalId}?${buildQuery({ tenantId })}`,
    payload
  );
}
```

- [ ] **Step 10: Ejecutar toda la capa con cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/api --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/api/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 6 archivos de test PASS. Cobertura 100% en `query-string.ts`, `invalidate.ts` y los cuatro `.api.ts`; los `.types.ts` pueden aparecer con sus constantes `as const` ejecutadas (100%).

- [ ] **Step 11: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/api
git commit -m "feat(organizacion): agregar capa de API del frontend para las cuatro entidades

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Schemas zod y conversión de formularios a payloads

**Files:**
- Create: `FE/schemas/campos.ts` y `campos.test.ts`
- Create: `FE/schemas/empresa.schema.ts` y `empresa.schema.test.ts`
- Create: `FE/schemas/establecimiento.schema.ts` y `establecimiento.schema.test.ts`
- Create: `FE/schemas/almacen.schema.ts` y `almacen.schema.test.ts`
- Create: `FE/schemas/terminal.schema.ts` y `terminal.schema.test.ts`
- Create: `FE/lib/form-payloads.ts` y `form-payloads.test.ts`

**Interfaces:**
- Consumes: tipos de `FE/api/*.types.ts` (Task 7) y `emptyToUndefined` / `toNumberOrUndefined` (Task 6).
- Produces:
  - `textoOpcional(max: number, etiqueta: string)`, `correoOpcional`, `ubigeoOpcional`, `numeroOpcional(etiqueta: string, limite?: number)`.
  - `empresaSchema` / `EmpresaFormValues`; `establecimientoSchema` / `EstablecimientoFormValues`; `almacenSchema` / `AlmacenFormValues`; `terminalSchema` / `TerminalFormValues` (todos los campos de texto son `string`, los opcionales vacíos son `''`; latitud/longitud/temperaturas son `string`).
  - `toActualizarEmpresaPayload(values)`, `toCrearEmpresaPayload(tenantId, values)`, `toActualizarEstablecimientoPayload(values)`, `toCrearEstablecimientoPayload(tenantId, empresaId, values)`, `toActualizarAlmacenPayload(values)`, `toCrearAlmacenPayload(tenantId, establecimientoId, values)`, `toActualizarTerminalPayload(values)`, `toCrearTerminalPayload(tenantId, establecimientoId, values)`.

- [ ] **Step 1: Escribir el test de `campos` (falla: módulo inexistente)**

`FE/schemas/campos.test.ts`:

```ts
import type { ZodType } from 'zod';
import { correoOpcional, numeroOpcional, textoOpcional, ubigeoOpcional } from './campos';

function messages(schema: ZodType, value: unknown) {
  const result = schema.safeParse(value);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('campos', () => {
  it('textoOpcional acepta vacío y rechaza textos largos con la etiqueta indicada', () => {
    const schema = textoOpcional(5, 'El teléfono');
    expect(messages(schema, '')).toEqual([]);
    expect(messages(schema, '12345')).toEqual([]);
    expect(messages(schema, '123456')).toEqual(['El teléfono no debe exceder 5 caracteres.']);
  });

  it('correoOpcional acepta vacío y correos válidos', () => {
    expect(messages(correoOpcional, '')).toEqual([]);
    expect(messages(correoOpcional, 'contacto@boticas.pe')).toEqual([]);
  });

  it('correoOpcional rechaza formatos inválidos y textos demasiado largos', () => {
    expect(messages(correoOpcional, 'no-es-correo')).toEqual(['El correo no es válido.']);
    expect(messages(correoOpcional, `${'a'.repeat(320)}@b.pe`)).toContain(
      'El correo no debe exceder 320 caracteres.'
    );
  });

  it('ubigeoOpcional acepta vacío y seis dígitos', () => {
    expect(messages(ubigeoOpcional, '')).toEqual([]);
    expect(messages(ubigeoOpcional, '150101')).toEqual([]);
    expect(messages(ubigeoOpcional, '1501')).toEqual(['El ubigeo debe tener 6 dígitos.']);
  });

  it('numeroOpcional sin límite acepta vacío y números, y rechaza texto', () => {
    const schema = numeroOpcional('La temperatura');
    expect(messages(schema, '')).toEqual([]);
    expect(messages(schema, '-18.5')).toEqual([]);
    expect(messages(schema, 'abc')).toEqual(['La temperatura debe ser un número.']);
  });

  it('numeroOpcional con límite valida el rango absoluto', () => {
    const schema = numeroOpcional('La latitud', 90);
    expect(messages(schema, '-12.0464')).toEqual([]);
    expect(messages(schema, '91')).toEqual(['La latitud debe estar entre -90 y 90.']);
    expect(messages(schema, 'x')).toEqual(['La latitud debe estar entre -90 y 90.']);
  });
});
```

- [ ] **Step 2: Implementar `campos.ts`**

```ts
import { z } from 'zod';

export function textoOpcional(max: number, etiqueta: string) {
  return z.string().max(max, `${etiqueta} no debe exceder ${max} caracteres.`);
}

export const correoOpcional = z
  .string()
  .max(320, 'El correo no debe exceder 320 caracteres.')
  .refine((value) => value === '' || z.email().safeParse(value).success, 'El correo no es válido.');

export const ubigeoOpcional = z.string().regex(/^(\d{6})?$/, 'El ubigeo debe tener 6 dígitos.');

export function numeroOpcional(etiqueta: string, limite?: number) {
  return z.string().refine(
    (value) =>
      value === '' ||
      (Number.isFinite(Number(value)) && (limite === undefined || Math.abs(Number(value)) <= limite)),
    limite === undefined
      ? `${etiqueta} debe ser un número.`
      : `${etiqueta} debe estar entre -${limite} y ${limite}.`
  );
}
```

- [ ] **Step 3: Ejecutar `campos` y verificar que pasa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/schemas/campos.test.ts --project erp-web`
Expected: 6 tests PASS.

- [ ] **Step 4: Escribir los tests de los cuatro schemas (fallan: módulos inexistentes)**

`FE/schemas/empresa.schema.test.ts`:

```ts
import { empresaSchema } from './empresa.schema';

const valid = {
  ruc: '20123456789',
  razonSocial: 'Boticas SAC',
  nombreComercial: '',
  direccionFiscal: '',
  ubigeoFiscal: '',
  telefono: '',
  email: '',
  sitioWeb: '',
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false
};

function messages(overrides: Record<string, unknown>) {
  const result = empresaSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('empresaSchema', () => {
  it('acepta una empresa válida con los opcionales vacíos', () => {
    expect(empresaSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta un RUC de persona natural que inicia con 10', () => {
    expect(messages({ ruc: '10123456789' })).toEqual([]);
  });

  it.each([
    [{ ruc: '30123456789' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ ruc: '2012345678' }, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'],
    [{ razonSocial: 'A' }, 'La razón social debe tener al menos 2 caracteres.'],
    [{ razonSocial: 'x'.repeat(301) }, 'La razón social no debe exceder 300 caracteres.'],
    [{ nombreComercial: 'x'.repeat(301) }, 'El nombre comercial no debe exceder 300 caracteres.'],
    [{ direccionFiscal: 'x'.repeat(501) }, 'La dirección fiscal no debe exceder 500 caracteres.'],
    [{ ubigeoFiscal: '123' }, 'El ubigeo debe tener 6 dígitos.'],
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
    [{ email: 'no-es-correo' }, 'El correo no es válido.'],
    [{ sitioWeb: 'x'.repeat(301) }, 'El sitio web no debe exceder 300 caracteres.'],
    [{ monedaFuncional: 'PE' }, 'La moneda debe tener 3 caracteres (ISO 4217).'],
    [{ zonaHoraria: '' }, 'La zona horaria es obligatoria.'],
    [{ zonaHoraria: 'x'.repeat(81) }, 'La zona horaria no debe exceder 80 caracteres.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
```

`FE/schemas/establecimiento.schema.test.ts`:

```ts
import { establecimientoSchema } from './establecimiento.schema';

const valid = {
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: '',
  codigoAnexoSunat: '0001',
  codigoDigemid: '',
  direccion: '',
  ubigeo: '',
  referencia: '',
  latitud: '',
  longitud: '',
  telefono: '',
  email: '',
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima'
};

function messages(overrides: Record<string, unknown>) {
  const result = establecimientoSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('establecimientoSchema', () => {
  it('acepta un establecimiento válido con los opcionales vacíos', () => {
    expect(establecimientoSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta coordenadas dentro de rango y el perfil STORE_EDGE', () => {
    expect(messages({ latitud: '-12.0464', longitud: '-77.0428', perfilOperacion: 'STORE_EDGE' })).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(251) }, 'El nombre no debe exceder 250 caracteres.'],
    [{ tipoEstablecimiento: 'FARMACIA' }, 'Selecciona un tipo de establecimiento.'],
    [{ categoriaRegulatoriaCodigo: 'x'.repeat(51) }, 'La categoría regulatoria no debe exceder 50 caracteres.'],
    [{ codigoAnexoSunat: '12' }, 'El anexo SUNAT debe tener 4 dígitos.'],
    [{ codigoDigemid: 'x'.repeat(11) }, 'El código DIGEMID no debe exceder 10 caracteres.'],
    [{ direccion: 'x'.repeat(501) }, 'La dirección no debe exceder 500 caracteres.'],
    [{ ubigeo: '12' }, 'El ubigeo debe tener 6 dígitos.'],
    [{ referencia: 'x'.repeat(301) }, 'La referencia no debe exceder 300 caracteres.'],
    [{ latitud: '95' }, 'La latitud debe estar entre -90 y 90.'],
    [{ longitud: '181' }, 'La longitud debe estar entre -180 y 180.'],
    [{ telefono: 'x'.repeat(41) }, 'El teléfono no debe exceder 40 caracteres.'],
    [{ email: 'no-es-correo' }, 'El correo no es válido.'],
    [{ perfilOperacion: 'OFFLINE' }, 'Selecciona un perfil de operación.'],
    [{ zonaHoraria: '' }, 'La zona horaria es obligatoria.'],
    [{ zonaHoraria: 'x'.repeat(81) }, 'La zona horaria no debe exceder 80 caracteres.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
```

`FE/schemas/almacen.schema.test.ts`:

```ts
import { almacenSchema } from './almacen.schema';

const valid = {
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: true,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: '',
  temperaturaMaxC: '',
  activo: true
};

function messages(overrides: Record<string, unknown>) {
  const result = almacenSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('almacenSchema', () => {
  it('acepta un almacén válido sin temperaturas', () => {
    expect(almacenSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta un almacén refrigerado con temperaturas', () => {
    expect(
      messages({ tipo: 'REFRIGERADO', controlTemperatura: true, temperaturaMinC: '2', temperaturaMaxC: '8' })
    ).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(151) }, 'El nombre no debe exceder 150 caracteres.'],
    [{ tipo: 'OTRO' }, 'Selecciona un tipo de almacén.'],
    [{ temperaturaMinC: 'frio' }, 'La temperatura mínima debe ser un número.'],
    [{ temperaturaMaxC: 'calor' }, 'La temperatura máxima debe ser un número.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
```

`FE/schemas/terminal.schema.test.ts`:

```ts
import { terminalSchema } from './terminal.schema';

const valid = {
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: '',
  serieFacturaDefecto: '',
  numeroSerieEquipo: '',
  hostname: '',
  ipEquipo: '',
  impresoraCodigo: '',
  storeEdgeHabilitado: false,
  estado: 'ACTIVO'
};

function messages(overrides: Record<string, unknown>) {
  const result = terminalSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('terminalSchema', () => {
  it('acepta un terminal válido con los opcionales vacíos', () => {
    expect(terminalSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta series, IPv4 e IPv6 válidas', () => {
    expect(messages({ serieBoletaDefecto: 'B001', serieFacturaDefecto: 'F0A1', ipEquipo: '10.0.0.15' })).toEqual([]);
    expect(messages({ ipEquipo: '2001:db8::1' })).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(121) }, 'El nombre no debe exceder 120 caracteres.'],
    [{ serieBoletaDefecto: 'F001' }, 'La serie de boleta debe iniciar con B y tener 4 caracteres.'],
    [{ serieFacturaDefecto: 'B001' }, 'La serie de factura debe iniciar con F y tener 4 caracteres.'],
    [{ numeroSerieEquipo: 'x'.repeat(121) }, 'El número de serie no debe exceder 120 caracteres.'],
    [{ hostname: 'x'.repeat(151) }, 'El hostname no debe exceder 150 caracteres.'],
    [{ ipEquipo: '999.1.1.1' }, 'La dirección IP no es válida.'],
    [{ impresoraCodigo: 'x'.repeat(101) }, 'El código de impresora no debe exceder 100 caracteres.'],
    [{ estado: 'OFFLINE' }, 'Selecciona un estado.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/schemas --project erp-web`
Expected: `campos` PASS; los cuatro schemas FAIL (`Failed to resolve import`).

- [ ] **Step 5: Implementar los cuatro schemas**

`FE/schemas/empresa.schema.ts`:

```ts
import { z } from 'zod';
import { correoOpcional, textoOpcional, ubigeoOpcional } from './campos';

export const empresaSchema = z.object({
  ruc: z.string().regex(/^(10|20)\d{9}$/, 'El RUC debe tener 11 dígitos e iniciar con 10 o 20.'),
  razonSocial: z
    .string()
    .min(2, 'La razón social debe tener al menos 2 caracteres.')
    .max(300, 'La razón social no debe exceder 300 caracteres.'),
  nombreComercial: textoOpcional(300, 'El nombre comercial'),
  direccionFiscal: textoOpcional(500, 'La dirección fiscal'),
  ubigeoFiscal: ubigeoOpcional,
  telefono: textoOpcional(40, 'El teléfono'),
  email: correoOpcional,
  sitioWeb: textoOpcional(300, 'El sitio web'),
  monedaFuncional: z.string().length(3, 'La moneda debe tener 3 caracteres (ISO 4217).'),
  zonaHoraria: z
    .string()
    .min(1, 'La zona horaria es obligatoria.')
    .max(80, 'La zona horaria no debe exceder 80 caracteres.'),
  permiteVentaOnline: z.boolean()
});

export type EmpresaFormValues = z.infer<typeof empresaSchema>;
```

`FE/schemas/establecimiento.schema.ts`:

```ts
import { z } from 'zod';
import { PERFILES_OPERACION, TIPOS_ESTABLECIMIENTO } from '../api/establecimientos.types';
import { correoOpcional, numeroOpcional, textoOpcional, ubigeoOpcional } from './campos';

export const establecimientoSchema = z.object({
  codigo: z
    .string()
    .min(1, 'El código es obligatorio.')
    .max(40, 'El código no debe exceder 40 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(250, 'El nombre no debe exceder 250 caracteres.'),
  tipoEstablecimiento: z.enum(TIPOS_ESTABLECIMIENTO, { error: 'Selecciona un tipo de establecimiento.' }),
  categoriaRegulatoriaCodigo: textoOpcional(50, 'La categoría regulatoria'),
  codigoAnexoSunat: z.string().regex(/^\d{4}$/, 'El anexo SUNAT debe tener 4 dígitos.'),
  codigoDigemid: textoOpcional(10, 'El código DIGEMID'),
  direccion: textoOpcional(500, 'La dirección'),
  ubigeo: ubigeoOpcional,
  referencia: textoOpcional(300, 'La referencia'),
  latitud: numeroOpcional('La latitud', 90),
  longitud: numeroOpcional('La longitud', 180),
  telefono: textoOpcional(40, 'El teléfono'),
  email: correoOpcional,
  esPrincipal: z.boolean(),
  permiteVentaOnline: z.boolean(),
  permiteDelivery: z.boolean(),
  perfilOperacion: z.enum(PERFILES_OPERACION, { error: 'Selecciona un perfil de operación.' }),
  zonaHoraria: z
    .string()
    .min(1, 'La zona horaria es obligatoria.')
    .max(80, 'La zona horaria no debe exceder 80 caracteres.')
});

export type EstablecimientoFormValues = z.infer<typeof establecimientoSchema>;
```

`FE/schemas/almacen.schema.ts`:

```ts
import { z } from 'zod';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { numeroOpcional } from './campos';

export const almacenSchema = z.object({
  codigo: z
    .string()
    .min(1, 'El código es obligatorio.')
    .max(40, 'El código no debe exceder 40 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(150, 'El nombre no debe exceder 150 caracteres.'),
  tipo: z.enum(TIPOS_ALMACEN, { error: 'Selecciona un tipo de almacén.' }),
  permiteLotes: z.boolean(),
  permiteVencimiento: z.boolean(),
  permiteVenta: z.boolean(),
  permiteDespacho: z.boolean(),
  controlTemperatura: z.boolean(),
  temperaturaMinC: numeroOpcional('La temperatura mínima'),
  temperaturaMaxC: numeroOpcional('La temperatura máxima'),
  activo: z.boolean()
});

export type AlmacenFormValues = z.infer<typeof almacenSchema>;
```

`FE/schemas/terminal.schema.ts`:

```ts
import { z } from 'zod';
import { ESTADOS_TERMINAL } from '../api/terminales.types';
import { textoOpcional } from './campos';

export const terminalSchema = z.object({
  codigo: z
    .string()
    .min(1, 'El código es obligatorio.')
    .max(40, 'El código no debe exceder 40 caracteres.'),
  nombre: z
    .string()
    .min(2, 'El nombre debe tener al menos 2 caracteres.')
    .max(120, 'El nombre no debe exceder 120 caracteres.'),
  serieBoletaDefecto: z
    .string()
    .regex(/^(B[A-Z0-9]{3})?$/, 'La serie de boleta debe iniciar con B y tener 4 caracteres.'),
  serieFacturaDefecto: z
    .string()
    .regex(/^(F[A-Z0-9]{3})?$/, 'La serie de factura debe iniciar con F y tener 4 caracteres.'),
  numeroSerieEquipo: textoOpcional(120, 'El número de serie'),
  hostname: textoOpcional(150, 'El hostname'),
  ipEquipo: z.union(
    [z.literal(''), z.ipv4(), z.ipv6()],
    { error: 'La dirección IP no es válida.' }
  ),
  impresoraCodigo: textoOpcional(100, 'El código de impresora'),
  storeEdgeHabilitado: z.boolean(),
  estado: z.enum(ESTADOS_TERMINAL, { error: 'Selecciona un estado.' })
});

export type TerminalFormValues = z.infer<typeof terminalSchema>;
```

- [ ] **Step 6: Ejecutar los schemas y verificar que pasan**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/schemas --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/schemas/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: los 5 archivos de test PASS y cobertura 100%. Si algún mensaje de zod difiere del esperado en un caso `it.each`, ajustar el **mensaje del schema** (no el test) hasta coincidir con el texto de la tabla.

- [ ] **Step 7: Escribir el test de los conversores de payload (falla: módulo inexistente)**

`FE/lib/form-payloads.test.ts`:

```ts
import {
  toActualizarAlmacenPayload,
  toActualizarEmpresaPayload,
  toActualizarEstablecimientoPayload,
  toActualizarTerminalPayload,
  toCrearAlmacenPayload,
  toCrearEmpresaPayload,
  toCrearEstablecimientoPayload,
  toCrearTerminalPayload
} from './form-payloads';

const empresaVacia = {
  ruc: '20123456789',
  razonSocial: '  Boticas SAC  ',
  nombreComercial: '',
  direccionFiscal: '',
  ubigeoFiscal: '',
  telefono: '',
  email: '',
  sitioWeb: '',
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false
};

const establecimientoLleno = {
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA' as const,
  categoriaRegulatoriaCodigo: 'CAT',
  codigoAnexoSunat: '0001',
  codigoDigemid: 'DIG001',
  direccion: 'Av. 2',
  ubigeo: '150101',
  referencia: 'Frente al parque',
  latitud: '-12.0464',
  longitud: '-77.0428',
  telefono: '01444',
  email: 'e@b.pe',
  esPrincipal: true,
  permiteVentaOnline: true,
  permiteDelivery: true,
  perfilOperacion: 'STORE_EDGE' as const,
  zonaHoraria: 'America/Lima'
};

describe('form-payloads', () => {
  it('empresa: recorta, omite opcionales vacíos y no envía el RUC al actualizar', () => {
    const payload = toActualizarEmpresaPayload(empresaVacia);

    expect(payload).toEqual({
      razonSocial: 'Boticas SAC',
      nombreComercial: undefined,
      direccionFiscal: undefined,
      ubigeoFiscal: undefined,
      telefono: undefined,
      email: undefined,
      sitioWeb: undefined,
      monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima',
      permiteVentaOnline: false
    });
    expect('ruc' in payload).toBe(false);
  });

  it('empresa: al crear agrega tenantId y RUC', () => {
    expect(toCrearEmpresaPayload('tenant-1', empresaVacia)).toMatchObject({
      tenantId: 'tenant-1',
      ruc: '20123456789',
      razonSocial: 'Boticas SAC'
    });
  });

  it('establecimiento: convierte coordenadas a número y conserva los opcionales llenos', () => {
    const payload = toActualizarEstablecimientoPayload(establecimientoLleno);

    expect(payload).toMatchObject({
      nombre: 'Botica Central',
      categoriaRegulatoriaCodigo: 'CAT',
      codigoDigemid: 'DIG001',
      latitud: -12.0464,
      longitud: -77.0428,
      perfilOperacion: 'STORE_EDGE'
    });
    expect('codigo' in payload).toBe(false);
  });

  it('establecimiento: omite coordenadas vacías y al crear agrega tenant, empresa y código', () => {
    const payload = toCrearEstablecimientoPayload('tenant-1', 'empresa-1', {
      ...establecimientoLleno,
      latitud: '',
      longitud: ''
    });

    expect(payload).toMatchObject({ tenantId: 'tenant-1', empresaId: 'empresa-1', codigo: 'EST001' });
    expect(payload.latitud).toBeUndefined();
    expect(payload.longitud).toBeUndefined();
  });

  const almacenValores = {
    codigo: 'ALM001',
    nombre: 'Almacén Central',
    tipo: 'REFRIGERADO' as const,
    permiteLotes: true,
    permiteVencimiento: true,
    permiteVenta: false,
    permiteDespacho: true,
    controlTemperatura: true,
    temperaturaMinC: '2',
    temperaturaMaxC: '',
    activo: false
  };

  it('almacén: convierte temperaturas y al actualizar incluye activo sin código', () => {
    const payload = toActualizarAlmacenPayload(almacenValores);

    expect(payload).toMatchObject({ temperaturaMinC: 2, activo: false, tipo: 'REFRIGERADO' });
    expect(payload.temperaturaMaxC).toBeUndefined();
    expect('codigo' in payload).toBe(false);
  });

  it('almacén: al crear agrega tenant, establecimiento y código y no envía activo', () => {
    const payload = toCrearAlmacenPayload('tenant-1', 'est-1', almacenValores);

    expect(payload).toMatchObject({ tenantId: 'tenant-1', establecimientoId: 'est-1', codigo: 'ALM001' });
    expect('activo' in payload).toBe(false);
  });

  const terminalValores = {
    codigo: 'POS001',
    nombre: 'Caja 1',
    serieBoletaDefecto: 'B001',
    serieFacturaDefecto: '',
    numeroSerieEquipo: 'SN-1',
    hostname: '',
    ipEquipo: '10.0.0.15',
    impresoraCodigo: '',
    storeEdgeHabilitado: true,
    estado: 'MANTENIMIENTO' as const
  };

  it('terminal: omite opcionales vacíos y al actualizar incluye estado sin código', () => {
    const payload = toActualizarTerminalPayload(terminalValores);

    expect(payload).toMatchObject({
      nombre: 'Caja 1',
      serieBoletaDefecto: 'B001',
      ipEquipo: '10.0.0.15',
      estado: 'MANTENIMIENTO',
      storeEdgeHabilitado: true
    });
    expect(payload.serieFacturaDefecto).toBeUndefined();
    expect(payload.hostname).toBeUndefined();
    expect('codigo' in payload).toBe(false);
  });

  it('terminal: al crear agrega tenant, establecimiento y código y no envía estado', () => {
    const payload = toCrearTerminalPayload('tenant-1', 'est-1', terminalValores);

    expect(payload).toMatchObject({ tenantId: 'tenant-1', establecimientoId: 'est-1', codigo: 'POS001' });
    expect('estado' in payload).toBe(false);
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib/form-payloads.test.ts --project erp-web`
Expected: FAIL (`Failed to resolve import "./form-payloads"`).

- [ ] **Step 8: Implementar `form-payloads.ts`**

```ts
import type { ActualizarAlmacenPayload, CrearAlmacenPayload } from '../api/almacenes.types';
import type { ActualizarEmpresaPayload, CrearEmpresaPayload } from '../api/empresas.types';
import type {
  ActualizarEstablecimientoPayload,
  CrearEstablecimientoPayload
} from '../api/establecimientos.types';
import type { ActualizarTerminalPayload, CrearTerminalPayload } from '../api/terminales.types';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { emptyToUndefined, toNumberOrUndefined } from './form-values';

export function toActualizarEmpresaPayload(values: EmpresaFormValues): ActualizarEmpresaPayload {
  return {
    razonSocial: values.razonSocial.trim(),
    nombreComercial: emptyToUndefined(values.nombreComercial),
    direccionFiscal: emptyToUndefined(values.direccionFiscal),
    ubigeoFiscal: emptyToUndefined(values.ubigeoFiscal),
    telefono: emptyToUndefined(values.telefono),
    email: emptyToUndefined(values.email),
    sitioWeb: emptyToUndefined(values.sitioWeb),
    monedaFuncional: values.monedaFuncional,
    zonaHoraria: values.zonaHoraria,
    permiteVentaOnline: values.permiteVentaOnline
  };
}

export function toCrearEmpresaPayload(tenantId: string, values: EmpresaFormValues): CrearEmpresaPayload {
  return { ...toActualizarEmpresaPayload(values), tenantId, ruc: values.ruc };
}

export function toActualizarEstablecimientoPayload(
  values: EstablecimientoFormValues
): ActualizarEstablecimientoPayload {
  return {
    nombre: values.nombre.trim(),
    tipoEstablecimiento: values.tipoEstablecimiento,
    categoriaRegulatoriaCodigo: emptyToUndefined(values.categoriaRegulatoriaCodigo),
    codigoAnexoSunat: values.codigoAnexoSunat,
    codigoDigemid: emptyToUndefined(values.codigoDigemid),
    direccion: emptyToUndefined(values.direccion),
    ubigeo: emptyToUndefined(values.ubigeo),
    referencia: emptyToUndefined(values.referencia),
    latitud: toNumberOrUndefined(values.latitud),
    longitud: toNumberOrUndefined(values.longitud),
    telefono: emptyToUndefined(values.telefono),
    email: emptyToUndefined(values.email),
    esPrincipal: values.esPrincipal,
    permiteVentaOnline: values.permiteVentaOnline,
    permiteDelivery: values.permiteDelivery,
    perfilOperacion: values.perfilOperacion,
    zonaHoraria: values.zonaHoraria
  };
}

export function toCrearEstablecimientoPayload(
  tenantId: string,
  empresaId: string,
  values: EstablecimientoFormValues
): CrearEstablecimientoPayload {
  return {
    ...toActualizarEstablecimientoPayload(values),
    tenantId,
    empresaId,
    codigo: values.codigo.trim()
  };
}

function almacenDatos(values: AlmacenFormValues) {
  return {
    nombre: values.nombre.trim(),
    tipo: values.tipo,
    permiteLotes: values.permiteLotes,
    permiteVencimiento: values.permiteVencimiento,
    permiteVenta: values.permiteVenta,
    permiteDespacho: values.permiteDespacho,
    controlTemperatura: values.controlTemperatura,
    temperaturaMinC: toNumberOrUndefined(values.temperaturaMinC),
    temperaturaMaxC: toNumberOrUndefined(values.temperaturaMaxC)
  };
}

export function toActualizarAlmacenPayload(values: AlmacenFormValues): ActualizarAlmacenPayload {
  return { ...almacenDatos(values), activo: values.activo };
}

export function toCrearAlmacenPayload(
  tenantId: string,
  establecimientoId: string,
  values: AlmacenFormValues
): CrearAlmacenPayload {
  return { ...almacenDatos(values), tenantId, establecimientoId, codigo: values.codigo.trim() };
}

function terminalDatos(values: TerminalFormValues) {
  return {
    nombre: values.nombre.trim(),
    serieBoletaDefecto: emptyToUndefined(values.serieBoletaDefecto),
    serieFacturaDefecto: emptyToUndefined(values.serieFacturaDefecto),
    numeroSerieEquipo: emptyToUndefined(values.numeroSerieEquipo),
    hostname: emptyToUndefined(values.hostname),
    ipEquipo: emptyToUndefined(values.ipEquipo),
    impresoraCodigo: emptyToUndefined(values.impresoraCodigo),
    storeEdgeHabilitado: values.storeEdgeHabilitado
  };
}

export function toActualizarTerminalPayload(values: TerminalFormValues): ActualizarTerminalPayload {
  return { ...terminalDatos(values), estado: values.estado };
}

export function toCrearTerminalPayload(
  tenantId: string,
  establecimientoId: string,
  values: TerminalFormValues
): CrearTerminalPayload {
  return { ...terminalDatos(values), tenantId, establecimientoId, codigo: values.codigo.trim() };
}
```

- [ ] **Step 9: Ejecutar y verificar cobertura**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib apps/erp-web/src/features/organizacion/schemas --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/{lib,schemas}/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: todo PASS y 100% de cobertura.

- [ ] **Step 10: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/schemas frontend/apps/erp-web/src/features/organizacion/lib
git commit -m "feat(organizacion): agregar schemas de validacion y conversion de formularios a payloads

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 9: `Modal` con tamaño `lg` y scroll interno

El formulario de establecimiento (cuatro secciones) no cabe en el modal actual, que tiene ancho fijo `max-w-lg` y no hace scroll. Se agrega una prop `size` (por defecto `md`, sin cambiar los 14 usos existentes) y `max-h-[90vh] overflow-y-auto`.

**Files:**
- Modify: `frontend/packages/ui-web/src/modal/Modal.tsx`
- Modify: `frontend/packages/ui-web/src/modal/Modal.test.tsx`

**Interfaces:**
- Produces: `Modal` acepta `size?: 'md' | 'lg'` (`md` → `max-w-lg`, `lg` → `max-w-3xl`).

- [ ] **Step 1: Agregar los tests (fallan)**

En `Modal.test.tsx`, dentro del `describe('Modal', ...)`, agregar antes del cierre:

```tsx
  it('usa el ancho md por defecto y permite scroll interno', () => {
    render(
      <Modal open onClose={() => {}} title="Tamaño">
        <p>Contenido</p>
      </Modal>
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog.className).toContain('max-w-lg');
    expect(dialog.className).toContain('overflow-y-auto');
    expect(dialog.className).toContain('max-h-[90vh]');
  });

  it('usa el ancho lg cuando se solicita', () => {
    render(
      <Modal open onClose={() => {}} title="Tamaño grande" size="lg">
        <p>Contenido</p>
      </Modal>
    );
    expect(screen.getByRole('dialog').className).toContain('max-w-3xl');
  });
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run packages/ui-web/src/modal --project ui-web`
Expected: FAIL (`overflow-y-auto` no está en el `className`; el segundo caso no encuentra `max-w-3xl`).

- [ ] **Step 3: Implementar (reemplazar `Modal.tsx` completo)**

```tsx
import type { PropsWithChildren } from 'react';
import { X } from 'lucide-react';
import { cn } from '../lib/cn';

export type ModalProps = PropsWithChildren<{
  open: boolean;
  onClose: () => void;
  title: string;
  size?: 'md' | 'lg';
}>;

const sizes = { md: 'max-w-lg', lg: 'max-w-3xl' } as const;

export function Modal({ open, onClose, title, size = 'md', children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-950/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className={cn(
          'max-h-[90vh] w-full overflow-y-auto rounded-2xl bg-white p-6 shadow-xl dark:bg-neutral-900',
          sizes[size]
        )}
      >
        <div className="flex items-center justify-between">
          <h2 id="modal-title" className="text-lg font-bold text-neutral-950 dark:text-white">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="grid size-8 place-items-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
          >
            <X className="size-4.5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd frontend; pnpm.cmd exec vitest run packages/ui-web --project ui-web`
Expected: todos los tests de `ui-web` PASS (incluido `Modal`).

- [ ] **Step 5: Commit**

```bash
git add frontend/packages/ui-web/src/modal
git commit -m "feat(ui-web): permitir tamaño lg y scroll interno en Modal

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Valores por defecto de los formularios

**Files:**
- Modify: `FE/lib/form-values.ts` y `FE/lib/form-values.test.ts`
- Create: `FE/lib/form-defaults.ts` y `FE/lib/form-defaults.test.ts`

(`FE` = `frontend/apps/erp-web/src/features/organizacion`.)

**Interfaces:**
- Consumes: tipos `Empresa`, `Establecimiento`, `Almacen`, `Terminal` (Task 7) y `*FormValues` (Task 8).
- Produces: `orEmpty(value: string | null): string`; `numberOrEmpty(value: number | null): string`; `EMPRESA_FORM_VACIO`, `ESTABLECIMIENTO_FORM_VACIO`, `ALMACEN_FORM_VACIO`, `TERMINAL_FORM_VACIO`; `toEmpresaFormValues(Empresa)`, `toEstablecimientoFormValues(Establecimiento)`, `toAlmacenFormValues(Almacen)`, `toTerminalFormValues(Terminal)`.

- [ ] **Step 1: Agregar los tests de `orEmpty` y `numberOrEmpty` (fallan)**

En `form-values.test.ts` cambiar el import y agregar el test dentro del `describe`:

```ts
import { emptyToUndefined, numberOrEmpty, orEmpty, toNumberOrUndefined } from './form-values';
```

```ts
  it('orEmpty y numberOrEmpty convierten null en cadena vacía', () => {
    expect(orEmpty('Av. 1')).toBe('Av. 1');
    expect(orEmpty(null)).toBe('');
    expect(numberOrEmpty(-12.5)).toBe('-12.5');
    expect(numberOrEmpty(0)).toBe('0');
    expect(numberOrEmpty(null)).toBe('');
  });
```

- [ ] **Step 2: Escribir el test de `form-defaults` (falla: módulo inexistente)**

`FE/lib/form-defaults.test.ts`:

```ts
import type { Almacen } from '../api/almacenes.types';
import type { Empresa } from '../api/empresas.types';
import type { Establecimiento } from '../api/establecimientos.types';
import type { Terminal } from '../api/terminales.types';
import {
  ALMACEN_FORM_VACIO,
  EMPRESA_FORM_VACIO,
  ESTABLECIMIENTO_FORM_VACIO,
  TERMINAL_FORM_VACIO,
  toAlmacenFormValues,
  toEmpresaFormValues,
  toEstablecimientoFormValues,
  toTerminalFormValues
} from './form-defaults';

describe('form-defaults', () => {
  it('los valores vacíos traen los valores por defecto del DDL', () => {
    expect(EMPRESA_FORM_VACIO).toMatchObject({ monedaFuncional: 'PEN', zonaHoraria: 'America/Lima', ruc: '' });
    expect(ESTABLECIMIENTO_FORM_VACIO).toMatchObject({
      codigoAnexoSunat: '0000',
      tipoEstablecimiento: 'BOTICA',
      perfilOperacion: 'ONLINE',
      zonaHoraria: 'America/Lima'
    });
    expect(ALMACEN_FORM_VACIO).toMatchObject({
      tipo: 'GENERAL',
      permiteLotes: true,
      permiteVencimiento: true,
      permiteVenta: false,
      permiteDespacho: true,
      controlTemperatura: false,
      activo: true
    });
    expect(TERMINAL_FORM_VACIO).toMatchObject({ estado: 'ACTIVO', storeEdgeHabilitado: false });
  });

  it('toEmpresaFormValues convierte nulls en cadenas vacías', () => {
    const empresa: Empresa = {
      id: 'e1', tenantId: 't1', ruc: '20123456789', razonSocial: 'Boticas SAC', nombreComercial: null,
      direccionFiscal: null, ubigeoFiscal: null, telefono: null, email: null, sitioWeb: null,
      monedaFuncional: 'PEN', zonaHoraria: 'America/Lima', permiteVentaOnline: true, estado: 'ACTIVO',
      createdAt: '2026-01-01T00:00:00Z', updatedAt: null
    };

    expect(toEmpresaFormValues(empresa)).toEqual({
      ruc: '20123456789', razonSocial: 'Boticas SAC', nombreComercial: '', direccionFiscal: '',
      ubigeoFiscal: '', telefono: '', email: '', sitioWeb: '', monedaFuncional: 'PEN',
      zonaHoraria: 'America/Lima', permiteVentaOnline: true
    });
    expect(toEmpresaFormValues({ ...empresa, nombreComercial: 'Boticas' }).nombreComercial).toBe('Boticas');
  });

  it('toEstablecimientoFormValues convierte nulls y coordenadas numéricas', () => {
    const establecimiento: Establecimiento = {
      id: 's1', tenantId: 't1', empresaId: 'e1', codigo: 'EST001', nombre: 'Botica Central',
      tipoEstablecimiento: 'BOTICA', categoriaRegulatoriaCodigo: null, codigoAnexoSunat: '0001',
      codigoDigemid: null, direccion: null, ubigeo: null, referencia: null, latitud: -12.0464,
      longitud: null, telefono: null, email: null, esPrincipal: true, permiteVentaOnline: false,
      permiteDelivery: true, perfilOperacion: 'STORE_EDGE', zonaHoraria: 'America/Lima',
      estadoOperativo: 'ACTIVO', createdAt: '2026-01-01T00:00:00Z', updatedAt: null
    };

    expect(toEstablecimientoFormValues(establecimiento)).toEqual({
      codigo: 'EST001', nombre: 'Botica Central', tipoEstablecimiento: 'BOTICA',
      categoriaRegulatoriaCodigo: '', codigoAnexoSunat: '0001', codigoDigemid: '', direccion: '',
      ubigeo: '', referencia: '', latitud: '-12.0464', longitud: '', telefono: '', email: '',
      esPrincipal: true, permiteVentaOnline: false, permiteDelivery: true, perfilOperacion: 'STORE_EDGE',
      zonaHoraria: 'America/Lima'
    });
  });

  it('toAlmacenFormValues convierte temperaturas y conserva el estado activo', () => {
    const almacen: Almacen = {
      id: 'a1', tenantId: 't1', establecimientoId: 's1', codigo: 'ALM001', nombre: 'Frío',
      tipo: 'REFRIGERADO', permiteLotes: true, permiteVencimiento: true, permiteVenta: false,
      permiteDespacho: true, controlTemperatura: true, temperaturaMinC: 2, temperaturaMaxC: 8,
      activo: false, createdAt: '2026-01-01T00:00:00Z', updatedAt: null
    };

    expect(toAlmacenFormValues(almacen)).toEqual({
      codigo: 'ALM001', nombre: 'Frío', tipo: 'REFRIGERADO', permiteLotes: true, permiteVencimiento: true,
      permiteVenta: false, permiteDespacho: true, controlTemperatura: true, temperaturaMinC: '2',
      temperaturaMaxC: '8', activo: false
    });
  });

  it('toTerminalFormValues convierte nulls en cadenas vacías', () => {
    const terminal: Terminal = {
      id: 'p1', tenantId: 't1', establecimientoId: 's1', codigo: 'POS001', nombre: 'Caja 1',
      serieBoletaDefecto: 'B001', serieFacturaDefecto: null, numeroSerieEquipo: null, hostname: null,
      ipEquipo: '10.0.0.15', impresoraCodigo: null, storeEdgeHabilitado: true, estado: 'MANTENIMIENTO',
      createdAt: '2026-01-01T00:00:00Z', updatedAt: null
    };

    expect(toTerminalFormValues(terminal)).toEqual({
      codigo: 'POS001', nombre: 'Caja 1', serieBoletaDefecto: 'B001', serieFacturaDefecto: '',
      numeroSerieEquipo: '', hostname: '', ipEquipo: '10.0.0.15', impresoraCodigo: '',
      storeEdgeHabilitado: true, estado: 'MANTENIMIENTO'
    });
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que fallan**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib --project erp-web`
Expected: FAIL (`orEmpty` / `numberOrEmpty` no exportados y `./form-defaults` inexistente).

- [ ] **Step 4: Implementar**

Agregar al final de `FE/lib/form-values.ts`:

```ts
export function orEmpty(value: string | null): string {
  return value ?? '';
}

export function numberOrEmpty(value: number | null): string {
  return value === null ? '' : String(value);
}
```

`FE/lib/form-defaults.ts`:

```ts
import type { Almacen } from '../api/almacenes.types';
import type { Empresa } from '../api/empresas.types';
import type { Establecimiento } from '../api/establecimientos.types';
import type { Terminal } from '../api/terminales.types';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import type { EmpresaFormValues } from '../schemas/empresa.schema';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { numberOrEmpty, orEmpty } from './form-values';

export const EMPRESA_FORM_VACIO: EmpresaFormValues = {
  ruc: '',
  razonSocial: '',
  nombreComercial: '',
  direccionFiscal: '',
  ubigeoFiscal: '',
  telefono: '',
  email: '',
  sitioWeb: '',
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false
};

export const ESTABLECIMIENTO_FORM_VACIO: EstablecimientoFormValues = {
  codigo: '',
  nombre: '',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: '',
  codigoAnexoSunat: '0000',
  codigoDigemid: '',
  direccion: '',
  ubigeo: '',
  referencia: '',
  latitud: '',
  longitud: '',
  telefono: '',
  email: '',
  esPrincipal: false,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima'
};

export const ALMACEN_FORM_VACIO: AlmacenFormValues = {
  codigo: '',
  nombre: '',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: false,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: '',
  temperaturaMaxC: '',
  activo: true
};

export const TERMINAL_FORM_VACIO: TerminalFormValues = {
  codigo: '',
  nombre: '',
  serieBoletaDefecto: '',
  serieFacturaDefecto: '',
  numeroSerieEquipo: '',
  hostname: '',
  ipEquipo: '',
  impresoraCodigo: '',
  storeEdgeHabilitado: false,
  estado: 'ACTIVO'
};

export function toEmpresaFormValues(empresa: Empresa): EmpresaFormValues {
  return {
    ruc: empresa.ruc,
    razonSocial: empresa.razonSocial,
    nombreComercial: orEmpty(empresa.nombreComercial),
    direccionFiscal: orEmpty(empresa.direccionFiscal),
    ubigeoFiscal: orEmpty(empresa.ubigeoFiscal),
    telefono: orEmpty(empresa.telefono),
    email: orEmpty(empresa.email),
    sitioWeb: orEmpty(empresa.sitioWeb),
    monedaFuncional: empresa.monedaFuncional,
    zonaHoraria: empresa.zonaHoraria,
    permiteVentaOnline: empresa.permiteVentaOnline
  };
}

export function toEstablecimientoFormValues(establecimiento: Establecimiento): EstablecimientoFormValues {
  return {
    codigo: establecimiento.codigo,
    nombre: establecimiento.nombre,
    tipoEstablecimiento: establecimiento.tipoEstablecimiento,
    categoriaRegulatoriaCodigo: orEmpty(establecimiento.categoriaRegulatoriaCodigo),
    codigoAnexoSunat: establecimiento.codigoAnexoSunat,
    codigoDigemid: orEmpty(establecimiento.codigoDigemid),
    direccion: orEmpty(establecimiento.direccion),
    ubigeo: orEmpty(establecimiento.ubigeo),
    referencia: orEmpty(establecimiento.referencia),
    latitud: numberOrEmpty(establecimiento.latitud),
    longitud: numberOrEmpty(establecimiento.longitud),
    telefono: orEmpty(establecimiento.telefono),
    email: orEmpty(establecimiento.email),
    esPrincipal: establecimiento.esPrincipal,
    permiteVentaOnline: establecimiento.permiteVentaOnline,
    permiteDelivery: establecimiento.permiteDelivery,
    perfilOperacion: establecimiento.perfilOperacion,
    zonaHoraria: establecimiento.zonaHoraria
  };
}

export function toAlmacenFormValues(almacen: Almacen): AlmacenFormValues {
  return {
    codigo: almacen.codigo,
    nombre: almacen.nombre,
    tipo: almacen.tipo,
    permiteLotes: almacen.permiteLotes,
    permiteVencimiento: almacen.permiteVencimiento,
    permiteVenta: almacen.permiteVenta,
    permiteDespacho: almacen.permiteDespacho,
    controlTemperatura: almacen.controlTemperatura,
    temperaturaMinC: numberOrEmpty(almacen.temperaturaMinC),
    temperaturaMaxC: numberOrEmpty(almacen.temperaturaMaxC),
    activo: almacen.activo
  };
}

export function toTerminalFormValues(terminal: Terminal): TerminalFormValues {
  return {
    codigo: terminal.codigo,
    nombre: terminal.nombre,
    serieBoletaDefecto: orEmpty(terminal.serieBoletaDefecto),
    serieFacturaDefecto: orEmpty(terminal.serieFacturaDefecto),
    numeroSerieEquipo: orEmpty(terminal.numeroSerieEquipo),
    hostname: orEmpty(terminal.hostname),
    ipEquipo: orEmpty(terminal.ipEquipo),
    impresoraCodigo: orEmpty(terminal.impresoraCodigo),
    storeEdgeHabilitado: terminal.storeEdgeHabilitado,
    estado: terminal.estado
  };
}
```

- [ ] **Step 5: Ejecutar y verificar cobertura completa de `lib/`**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/lib --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/lib/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: todo PASS y 100% de cobertura.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/lib
git commit -m "feat(organizacion): agregar valores por defecto y conversion de entidades a formularios

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Componentes base de formulario

**Files:**
- Create: `FE/components/FormFields.tsx` y `FormFields.test.tsx`
- Create: `FE/components/FormError.tsx` y `FormError.test.tsx`
- Create: `FE/components/FormSection.tsx` y `FormSection.test.tsx`
- Create: `FE/components/DatoItem.tsx` y `DatoItem.test.tsx`
- Create: `FE/components/CambiarEstadoDialog.tsx` y `CambiarEstadoDialog.test.tsx`

**Interfaces:**
- Consumes: `FormField`, `Input`, `Select`, `Modal`, `Button` de `@boticas/ui-web` (Modal con `size` del Task 9, Select del Task 5).
- Produces:
  - `TextField({ id, label, error?, ...InputProps })`, `SelectField({ id, label, error?, children, ...SelectProps })`, `CheckboxField({ id, label, ...InputProps })`.
  - `FormError({ message: string })` (renderiza `role="alert"`).
  - `FormSection({ title, children })` (un `fieldset` con `legend`).
  - `DatoItem({ label: string, children: ReactNode })` (par `dt`/`dd`).
  - `CambiarEstadoDialog<T extends string>({ title, estados: readonly T[], current: T, isSubmitting: boolean, error: string | null, onSubmit: (estado: T) => void, onClose: () => void })`. Siempre está abierto: el padre lo monta y desmonta.

- [ ] **Step 1: Escribir los tests (fallan: módulos inexistentes)**

`FE/components/FormFields.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CheckboxField, SelectField, TextField } from './FormFields';

describe('TextField', () => {
  it('asocia la etiqueta con el input y no marca error', () => {
    render(<TextField id="campo" label="Nombre" />);

    const input = screen.getByLabelText('Nombre');
    expect(input).toHaveAttribute('id', 'campo');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('muestra el error y marca el input como inválido', () => {
    render(<TextField id="campo" label="Nombre" error="Es obligatorio." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Es obligatorio.');
    expect(screen.getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true');
  });

  it('reenvía props del input como readOnly y placeholder', () => {
    render(<TextField id="campo" label="Nombre" readOnly placeholder="Escribe" />);

    const input = screen.getByLabelText('Nombre');
    expect(input).toHaveAttribute('readonly');
    expect(input).toHaveAttribute('placeholder', 'Escribe');
  });
});

describe('SelectField', () => {
  it('renderiza las opciones y permite elegir', async () => {
    const user = userEvent.setup();
    render(
      <SelectField id="tipo" label="Tipo" defaultValue="a">
        <option value="a">A</option>
        <option value="b">B</option>
      </SelectField>
    );

    const select = screen.getByLabelText('Tipo');
    expect(select).toHaveValue('a');
    await user.selectOptions(select, 'b');
    expect(select).toHaveValue('b');
    expect(select).not.toHaveAttribute('aria-invalid');
  });

  it('muestra el error y marca el select como inválido', () => {
    render(
      <SelectField id="tipo" label="Tipo" error="Selecciona un tipo.">
        <option value="a">A</option>
      </SelectField>
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona un tipo.');
    expect(screen.getByLabelText('Tipo')).toHaveAttribute('aria-invalid', 'true');
  });
});

describe('CheckboxField', () => {
  it('asocia la etiqueta y alterna el valor', async () => {
    const user = userEvent.setup();
    render(<CheckboxField id="principal" label="Es principal" />);

    const checkbox = screen.getByLabelText('Es principal');
    expect(checkbox).not.toBeChecked();
    await user.click(checkbox);
    expect(checkbox).toBeChecked();
  });

  it('respeta defaultChecked', () => {
    render(<CheckboxField id="activo" label="Activo" defaultChecked />);

    expect(screen.getByLabelText('Activo')).toBeChecked();
  });
});
```

`FE/components/FormError.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { FormError } from './FormError';

describe('FormError', () => {
  it('muestra el mensaje como alerta', () => {
    render(<FormError message="Ya existe una empresa con el RUC indicado." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe una empresa con el RUC indicado.');
  });
});
```

`FE/components/FormSection.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { FormSection } from './FormSection';

describe('FormSection', () => {
  it('agrupa su contenido bajo un título accesible', () => {
    render(
      <FormSection title="Regulatorio">
        <p>Contenido</p>
      </FormSection>
    );

    const group = screen.getByRole('group', { name: 'Regulatorio' });
    expect(group).toHaveTextContent('Contenido');
  });
});
```

`FE/components/DatoItem.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { DatoItem } from './DatoItem';

describe('DatoItem', () => {
  it('muestra la etiqueta y el valor', () => {
    render(
      <dl>
        <DatoItem label="RUC">20123456789</DatoItem>
      </dl>
    );

    expect(screen.getByText('RUC')).toBeInTheDocument();
    expect(screen.getByText('20123456789')).toBeInTheDocument();
  });
});
```

`FE/components/CambiarEstadoDialog.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CambiarEstadoDialog } from './CambiarEstadoDialog';

const estados = ['ACTIVO', 'SUSPENDIDO', 'BLOQUEADO'] as const;

function renderDialog(overrides: Partial<Parameters<typeof CambiarEstadoDialog<'ACTIVO' | 'SUSPENDIDO' | 'BLOQUEADO'>>[0]> = {}) {
  const onSubmit = vi.fn();
  const onClose = vi.fn();
  render(
    <CambiarEstadoDialog
      title="Cambiar estado de la empresa"
      estados={estados}
      current="ACTIVO"
      isSubmitting={false}
      error={null}
      onSubmit={onSubmit}
      onClose={onClose}
      {...overrides}
    />
  );
  return { onSubmit, onClose, user: userEvent.setup() };
}

describe('CambiarEstadoDialog', () => {
  it('muestra el estado actual seleccionado y deshabilita guardar mientras no cambie', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Cambiar estado de la empresa' })).toBeInTheDocument();
    expect(screen.getByLabelText('Estado')).toHaveValue('ACTIVO');
    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('envía el nuevo estado seleccionado', async () => {
    const { onSubmit, user } = renderDialog();

    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(onSubmit).toHaveBeenCalledWith('SUSPENDIDO');
  });

  it('deshabilita guardar mientras se envía', async () => {
    const { user } = renderDialog({ isSubmitting: true });

    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');

    expect(screen.getByRole('button', { name: 'Guardar estado' })).toBeDisabled();
  });

  it('muestra el error recibido', () => {
    renderDialog({ error: 'No tienes permiso para esta acción.' });

    expect(screen.getByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.');
  });

  it('llama a onClose desde el botón cerrar', async () => {
    const { onClose, user } = renderDialog();

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledOnce();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components --project erp-web`
Expected: FAIL (`Failed to resolve import` para cada componente).

- [ ] **Step 3: Implementar los componentes**

`FE/components/FormFields.tsx`:

```tsx
import type { ComponentPropsWithRef } from 'react';
import { FormField, Input, Select } from '@boticas/ui-web';
import type { InputProps, SelectProps } from '@boticas/ui-web';

type FieldProps = {
  id: string;
  label: string;
  error?: string | undefined;
};

export function TextField({ id, label, error, ...props }: FieldProps & Omit<InputProps, 'id'>) {
  return (
    <FormField label={label} htmlFor={id} error={error}>
      <Input id={id} aria-invalid={error ? true : undefined} {...props} />
    </FormField>
  );
}

export function SelectField({
  id,
  label,
  error,
  children,
  ...props
}: FieldProps & Omit<SelectProps, 'id'>) {
  return (
    <FormField label={label} htmlFor={id} error={error}>
      <Select id={id} aria-invalid={error ? true : undefined} {...props}>
        {children}
      </Select>
    </FormField>
  );
}

export function CheckboxField({
  id,
  label,
  ...props
}: { id: string; label: string } & Omit<ComponentPropsWithRef<'input'>, 'id' | 'type'>) {
  return (
    <label
      htmlFor={id}
      className="flex items-center gap-2 text-sm text-neutral-700 dark:text-neutral-200"
    >
      <input
        id={id}
        type="checkbox"
        className="text-primary-600 size-4 rounded border-neutral-300"
        {...props}
      />
      {label}
    </label>
  );
}
```

`FE/components/FormError.tsx`:

```tsx
export function FormError({ message }: { message: string }) {
  return (
    <p
      role="alert"
      className="border-danger-200 bg-danger-50 text-danger-800 dark:border-danger-800 dark:bg-danger-900/30 dark:text-danger-300 rounded-lg border px-3 py-2 text-sm"
    >
      {message}
    </p>
  );
}
```

`FE/components/FormSection.tsx`:

```tsx
import type { PropsWithChildren } from 'react';

export function FormSection({ title, children }: PropsWithChildren<{ title: string }>) {
  return (
    <fieldset className="space-y-4 rounded-xl border border-neutral-200 p-4 dark:border-neutral-800">
      <legend className="px-2 text-sm font-semibold text-neutral-700 dark:text-neutral-200">
        {title}
      </legend>
      {children}
    </fieldset>
  );
}
```

`FE/components/DatoItem.tsx`:

```tsx
import type { PropsWithChildren } from 'react';

export function DatoItem({ label, children }: PropsWithChildren<{ label: string }>) {
  return (
    <div>
      <dt className="text-xs font-semibold tracking-wide text-neutral-500 uppercase dark:text-neutral-400">
        {label}
      </dt>
      <dd className="mt-1 text-sm text-neutral-900 dark:text-neutral-100">{children}</dd>
    </div>
  );
}
```

`FE/components/CambiarEstadoDialog.tsx`:

```tsx
import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@boticas/ui-web';
import { FormError } from './FormError';

export type CambiarEstadoDialogProps<T extends string> = {
  title: string;
  estados: readonly T[];
  current: T;
  isSubmitting: boolean;
  error: string | null;
  onSubmit: (estado: T) => void;
  onClose: () => void;
};

export function CambiarEstadoDialog<T extends string>({
  title,
  estados,
  current,
  isSubmitting,
  error,
  onSubmit,
  onClose
}: CambiarEstadoDialogProps<T>) {
  const [estado, setEstado] = useState<T>(current);

  return (
    <Modal open onClose={onClose} title={title}>
      <form
        className="space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(estado);
        }}
      >
        <FormField label="Estado" htmlFor="cambiar-estado">
          <Select
            id="cambiar-estado"
            value={estado}
            onChange={(event) => setEstado(event.target.value as T)}
          >
            {estados.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
        </FormField>
        {error ? <FormError message={error} /> : null}
        <Button type="submit" disabled={isSubmitting || estado === current}>
          Guardar estado
        </Button>
      </form>
    </Modal>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 5 archivos de test PASS y 100% de cobertura en los cinco componentes. Si `tsc` se queja del tipo `Partial<Parameters<typeof CambiarEstadoDialog<...>>[0]>` del helper de test, simplificar el tipo a `Partial<CambiarEstadoDialogProps<'ACTIVO' | 'SUSPENDIDO' | 'BLOQUEADO'>>` importando `CambiarEstadoDialogProps`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components
git commit -m "feat(organizacion): agregar componentes base de formulario y dialogo de cambio de estado

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 12: `EmpresaForm`

**Files:**
- Create: `FE/components/EmpresaForm.tsx`
- Test: `FE/components/EmpresaForm.test.tsx`

**Interfaces:**
- Consumes: `empresaSchema`, `EmpresaFormValues` (Task 8); `EMPRESA_FORM_VACIO` (Task 10); `TextField`, `CheckboxField`, `FormError` (Task 11).
- Produces: `EmpresaForm({ defaultValues?, isEdit?, onSubmit(values: EmpresaFormValues), submitLabel, isSubmitting?, error? })`. En modo edición (`isEdit`) el RUC es de solo lectura.

- [ ] **Step 1: Escribir el test (falla: módulo inexistente)**

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EMPRESA_FORM_VACIO } from '../lib/form-defaults';
import { EmpresaForm } from './EmpresaForm';

function renderForm(props: Partial<Parameters<typeof EmpresaForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<EmpresaForm onSubmit={onSubmit} submitLabel="Crear empresa" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('EmpresaForm', () => {
  it('envía los valores válidos con los valores por defecto de moneda y zona horaria', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByLabelText('Permite venta online'));
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...EMPRESA_FORM_VACIO,
      ruc: '20123456789',
      razonSocial: 'Boticas SAC',
      permiteVentaOnline: true
    });
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(await screen.findByText('El RUC debe tener 11 dígitos e iniciar con 10 o 20.')).toBeInTheDocument();
    expect(screen.getByText('La razón social debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en edición precarga los datos y deja el RUC de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...EMPRESA_FORM_VACIO, ruc: '20123456789', razonSocial: 'Boticas SAC' }
    });

    const ruc = screen.getByLabelText('RUC');
    expect(ruc).toHaveValue('20123456789');
    expect(ruc).toHaveAttribute('readonly');
    await user.clear(screen.getByLabelText('Razón social'));
    await user.type(screen.getByLabelText('Razón social'), 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ ruc: '20123456789', razonSocial: 'Boticas del Perú SAC' })
    );
  });

  it('en creación el RUC es editable', () => {
    renderForm();

    expect(screen.getByLabelText('RUC')).not.toHaveAttribute('readonly');
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe una empresa con el RUC indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe una empresa con el RUC indicado.');
    expect(screen.getByRole('button', { name: 'Crear empresa' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EmpresaForm.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EmpresaForm"`).

- [ ] **Step 3: Implementar `EmpresaForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { EMPRESA_FORM_VACIO } from '../lib/form-defaults';
import { empresaSchema, type EmpresaFormValues } from '../schemas/empresa.schema';
import { FormError } from './FormError';
import { CheckboxField, TextField } from './FormFields';

export type EmpresaFormProps = {
  defaultValues?: EmpresaFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: EmpresaFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function EmpresaForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: EmpresaFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<EmpresaFormValues>({
    defaultValues: defaultValues ?? EMPRESA_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(empresaSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id="empresa-ruc"
          label="RUC"
          readOnly={isEdit}
          error={errors.ruc?.message}
          {...register('ruc')}
        />
        <TextField
          id="empresa-razon-social"
          label="Razón social"
          error={errors.razonSocial?.message}
          {...register('razonSocial')}
        />
        <TextField
          id="empresa-nombre-comercial"
          label="Nombre comercial"
          error={errors.nombreComercial?.message}
          {...register('nombreComercial')}
        />
        <TextField
          id="empresa-direccion-fiscal"
          label="Dirección fiscal"
          error={errors.direccionFiscal?.message}
          {...register('direccionFiscal')}
        />
        <TextField
          id="empresa-ubigeo-fiscal"
          label="Ubigeo fiscal"
          error={errors.ubigeoFiscal?.message}
          {...register('ubigeoFiscal')}
        />
        <TextField
          id="empresa-telefono"
          label="Teléfono"
          error={errors.telefono?.message}
          {...register('telefono')}
        />
        <TextField
          id="empresa-email"
          label="Correo"
          error={errors.email?.message}
          {...register('email')}
        />
        <TextField
          id="empresa-sitio-web"
          label="Sitio web"
          error={errors.sitioWeb?.message}
          {...register('sitioWeb')}
        />
        <TextField
          id="empresa-moneda"
          label="Moneda funcional"
          error={errors.monedaFuncional?.message}
          {...register('monedaFuncional')}
        />
        <TextField
          id="empresa-zona-horaria"
          label="Zona horaria"
          error={errors.zonaHoraria?.message}
          {...register('zonaHoraria')}
        />
      </div>
      <CheckboxField
        id="empresa-venta-online"
        label="Permite venta online"
        {...register('permiteVentaOnline')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EmpresaForm.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/EmpresaForm.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 5 tests PASS y 100% en `EmpresaForm.tsx`. Si el envío válido no llama a `onSubmit`, revisar que `RUC` cumple `(10|20)` + 9 dígitos y que los valores por defecto (`PEN`, `America/Lima`) siguen presentes.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components/EmpresaForm.tsx frontend/apps/erp-web/src/features/organizacion/components/EmpresaForm.test.tsx
git commit -m "feat(organizacion): agregar formulario de empresa operadora

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 13: `EstablecimientoForm` (cuatro secciones)

**Files:**
- Create: `FE/components/EstablecimientoForm.tsx`
- Test: `FE/components/EstablecimientoForm.test.tsx`

**Interfaces:**
- Consumes: `establecimientoSchema`, `EstablecimientoFormValues` (Task 8); `ESTABLECIMIENTO_FORM_VACIO` (Task 10); `TIPOS_ESTABLECIMIENTO`, `PERFILES_OPERACION` (Task 7); `TextField`, `SelectField`, `CheckboxField`, `FormSection`, `FormError` (Task 11).
- Produces: `EstablecimientoForm({ defaultValues?, isEdit?, onSubmit(values: EstablecimientoFormValues), submitLabel, isSubmitting?, error? })`. En modo edición el código es de solo lectura.

- [ ] **Step 1: Escribir el test (falla: módulo inexistente)**

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ESTABLECIMIENTO_FORM_VACIO } from '../lib/form-defaults';
import { EstablecimientoForm } from './EstablecimientoForm';

function renderForm(props: Partial<Parameters<typeof EstablecimientoForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<EstablecimientoForm onSubmit={onSubmit} submitLabel="Crear establecimiento" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('EstablecimientoForm', () => {
  it('agrupa los campos en cuatro secciones', () => {
    renderForm();

    ['Identificación', 'Regulatorio', 'Ubicación', 'Operación'].forEach((name) => {
      expect(screen.getByRole('group', { name })).toBeInTheDocument();
    });
  });

  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(onSubmit).toHaveBeenCalledWith({
      ...ESTABLECIMIENTO_FORM_VACIO,
      codigo: 'EST001',
      nombre: 'Botica Central'
    });
  });

  it('permite completar los campos regulatorios, de ubicación y de operación', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'EST002');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Surco');
    await user.click(screen.getByLabelText('Es principal'));
    await user.clear(screen.getByLabelText('Anexo SUNAT'));
    await user.type(screen.getByLabelText('Anexo SUNAT'), '0002');
    await user.type(screen.getByLabelText('Código DIGEMID'), 'DIG002');
    await user.type(screen.getByLabelText('Latitud'), '-12.0464');
    await user.type(screen.getByLabelText('Longitud'), '-77.0428');
    await user.selectOptions(screen.getByLabelText('Perfil de operación'), 'STORE_EDGE');
    await user.click(screen.getByLabelText('Permite delivery'));
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        codigo: 'EST002',
        esPrincipal: true,
        codigoAnexoSunat: '0002',
        codigoDigemid: 'DIG002',
        latitud: '-12.0464',
        longitud: '-77.0428',
        perfilOperacion: 'STORE_EDGE',
        permiteDelivery: true
      })
    );
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Latitud'), '95');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('El nombre debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La latitud debe estar entre -90 y 90.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en edición precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...ESTABLECIMIENTO_FORM_VACIO, codigo: 'EST001', nombre: 'Botica Central' }
    });

    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('EST001');
    expect(codigo).toHaveAttribute('readonly');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ codigo: 'EST001' }));
  });

  it('en creación el código es editable', () => {
    renderForm();

    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un establecimiento con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un establecimiento con el código indicado.');
    expect(screen.getByRole('button', { name: 'Crear establecimiento' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EstablecimientoForm.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EstablecimientoForm"`).

- [ ] **Step 3: Implementar `EstablecimientoForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { PERFILES_OPERACION, TIPOS_ESTABLECIMIENTO } from '../api/establecimientos.types';
import { ESTABLECIMIENTO_FORM_VACIO } from '../lib/form-defaults';
import {
  establecimientoSchema,
  type EstablecimientoFormValues
} from '../schemas/establecimiento.schema';
import { FormError } from './FormError';
import { CheckboxField, SelectField, TextField } from './FormFields';
import { FormSection } from './FormSection';

export type EstablecimientoFormProps = {
  defaultValues?: EstablecimientoFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: EstablecimientoFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function EstablecimientoForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: EstablecimientoFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<EstablecimientoFormValues>({
    defaultValues: defaultValues ?? ESTABLECIMIENTO_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(establecimientoSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <FormSection title="Identificación">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            id="establecimiento-codigo"
            label="Código"
            readOnly={isEdit}
            error={errors.codigo?.message}
            {...register('codigo')}
          />
          <TextField
            id="establecimiento-nombre"
            label="Nombre"
            error={errors.nombre?.message}
            {...register('nombre')}
          />
          <SelectField
            id="establecimiento-tipo"
            label="Tipo de establecimiento"
            error={errors.tipoEstablecimiento?.message}
            {...register('tipoEstablecimiento')}
          >
            {TIPOS_ESTABLECIMIENTO.map((tipo) => (
              <option key={tipo} value={tipo}>
                {tipo}
              </option>
            ))}
          </SelectField>
        </div>
        <CheckboxField
          id="establecimiento-principal"
          label="Es principal"
          {...register('esPrincipal')}
        />
      </FormSection>

      <FormSection title="Regulatorio">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            id="establecimiento-categoria"
            label="Categoría regulatoria"
            error={errors.categoriaRegulatoriaCodigo?.message}
            {...register('categoriaRegulatoriaCodigo')}
          />
          <TextField
            id="establecimiento-anexo"
            label="Anexo SUNAT"
            error={errors.codigoAnexoSunat?.message}
            {...register('codigoAnexoSunat')}
          />
          <TextField
            id="establecimiento-digemid"
            label="Código DIGEMID"
            error={errors.codigoDigemid?.message}
            {...register('codigoDigemid')}
          />
        </div>
      </FormSection>

      <FormSection title="Ubicación">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            id="establecimiento-direccion"
            label="Dirección"
            error={errors.direccion?.message}
            {...register('direccion')}
          />
          <TextField
            id="establecimiento-ubigeo"
            label="Ubigeo"
            error={errors.ubigeo?.message}
            {...register('ubigeo')}
          />
          <TextField
            id="establecimiento-referencia"
            label="Referencia"
            error={errors.referencia?.message}
            {...register('referencia')}
          />
          <TextField
            id="establecimiento-latitud"
            label="Latitud"
            inputMode="decimal"
            error={errors.latitud?.message}
            {...register('latitud')}
          />
          <TextField
            id="establecimiento-longitud"
            label="Longitud"
            inputMode="decimal"
            error={errors.longitud?.message}
            {...register('longitud')}
          />
          <TextField
            id="establecimiento-telefono"
            label="Teléfono"
            error={errors.telefono?.message}
            {...register('telefono')}
          />
          <TextField
            id="establecimiento-email"
            label="Correo"
            error={errors.email?.message}
            {...register('email')}
          />
        </div>
      </FormSection>

      <FormSection title="Operación">
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            id="establecimiento-perfil"
            label="Perfil de operación"
            error={errors.perfilOperacion?.message}
            {...register('perfilOperacion')}
          >
            {PERFILES_OPERACION.map((perfil) => (
              <option key={perfil} value={perfil}>
                {perfil}
              </option>
            ))}
          </SelectField>
          <TextField
            id="establecimiento-zona-horaria"
            label="Zona horaria"
            error={errors.zonaHoraria?.message}
            {...register('zonaHoraria')}
          />
        </div>
        <div className="flex flex-wrap gap-6">
          <CheckboxField
            id="establecimiento-venta-online"
            label="Permite venta online"
            {...register('permiteVentaOnline')}
          />
          <CheckboxField
            id="establecimiento-delivery"
            label="Permite delivery"
            {...register('permiteDelivery')}
          />
        </div>
      </FormSection>

      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EstablecimientoForm.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/EstablecimientoForm.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 7 tests PASS y 100% en `EstablecimientoForm.tsx`. Nota: las etiquetas «Código» y «Código DIGEMID» comparten prefijo; `getByLabelText('Código')` usa coincidencia exacta de cadena, por lo que no es ambiguo.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components/EstablecimientoForm.tsx frontend/apps/erp-web/src/features/organizacion/components/EstablecimientoForm.test.tsx
git commit -m "feat(organizacion): agregar formulario de establecimiento en cuatro secciones

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 14: `AlmacenForm` y `TerminalForm`

**Files:**
- Create: `FE/components/AlmacenForm.tsx` y `AlmacenForm.test.tsx`
- Create: `FE/components/TerminalForm.tsx` y `TerminalForm.test.tsx`

**Interfaces:**
- Consumes: `almacenSchema` / `AlmacenFormValues`, `terminalSchema` / `TerminalFormValues` (Task 8); `ALMACEN_FORM_VACIO`, `TERMINAL_FORM_VACIO` (Task 10); `TIPOS_ALMACEN`, `ESTADOS_TERMINAL` (Task 7); componentes del Task 11.
- Produces: `AlmacenForm({ defaultValues?, isEdit?, onSubmit(values: AlmacenFormValues), submitLabel, isSubmitting?, error? })` (en edición: código de solo lectura y aparece el campo «Almacén activo»); `TerminalForm({ ...mismas props con TerminalFormValues })` (en edición: código de solo lectura y aparece el select «Estado»).

- [ ] **Step 1: Escribir los tests (fallan: módulos inexistentes)**

`FE/components/AlmacenForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ALMACEN_FORM_VACIO } from '../lib/form-defaults';
import { AlmacenForm } from './AlmacenForm';

function renderForm(props: Partial<Parameters<typeof AlmacenForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<AlmacenForm onSubmit={onSubmit} submitLabel="Crear almacén" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('AlmacenForm', () => {
  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith({ ...ALMACEN_FORM_VACIO, codigo: 'ALM001', nombre: 'Almacén Central' });
  });

  it('permite elegir el tipo, los indicadores y las temperaturas', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'ALM002');
    await user.type(screen.getByLabelText('Nombre'), 'Cadena de frío');
    await user.selectOptions(screen.getByLabelText('Tipo de almacén'), 'REFRIGERADO');
    await user.click(screen.getByLabelText('Permite venta'));
    await user.click(screen.getByLabelText('Controla temperatura'));
    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), '2');
    await user.type(screen.getByLabelText('Temperatura máxima (°C)'), '8');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        tipo: 'REFRIGERADO',
        permiteVenta: true,
        controlTemperatura: true,
        temperaturaMinC: '2',
        temperaturaMaxC: '8'
      })
    );
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Temperatura mínima (°C)'), 'frio');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('El nombre debe tener al menos 2 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La temperatura mínima debe ser un número.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en creación no muestra el campo activo y el código es editable', () => {
    renderForm();

    expect(screen.queryByLabelText('Almacén activo')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('en edición muestra activo, precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...ALMACEN_FORM_VACIO, codigo: 'ALM001', nombre: 'Almacén Central' }
    });

    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    const activo = screen.getByLabelText('Almacén activo');
    expect(activo).toBeChecked();
    await user.click(activo);
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ codigo: 'ALM001', activo: false }));
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un almacén con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un almacén con el código indicado.');
    expect(screen.getByRole('button', { name: 'Crear almacén' })).toBeDisabled();
  });
});
```

`FE/components/TerminalForm.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TERMINAL_FORM_VACIO } from '../lib/form-defaults';
import { TerminalForm } from './TerminalForm';

function renderForm(props: Partial<Parameters<typeof TerminalForm>[0]> = {}) {
  const onSubmit = vi.fn();
  render(<TerminalForm onSubmit={onSubmit} submitLabel="Crear terminal" {...props} />);
  return { onSubmit, user: userEvent.setup() };
}

describe('TerminalForm', () => {
  it('envía los valores válidos con los valores por defecto', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(onSubmit).toHaveBeenCalledWith({ ...TERMINAL_FORM_VACIO, codigo: 'POS001', nombre: 'Caja 1' });
  });

  it('permite completar series, equipo, IP e impresora', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Código'), 'POS002');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 2');
    await user.type(screen.getByLabelText('Serie de boleta'), 'B002');
    await user.type(screen.getByLabelText('Serie de factura'), 'F002');
    await user.type(screen.getByLabelText('Número de serie del equipo'), 'SN-002');
    await user.type(screen.getByLabelText('Hostname'), 'caja-2');
    await user.type(screen.getByLabelText('Dirección IP'), '10.0.0.16');
    await user.type(screen.getByLabelText('Código de impresora'), 'IMP02');
    await user.click(screen.getByLabelText('Habilitar store edge'));
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        serieBoletaDefecto: 'B002',
        serieFacturaDefecto: 'F002',
        numeroSerieEquipo: 'SN-002',
        hostname: 'caja-2',
        ipEquipo: '10.0.0.16',
        impresoraCodigo: 'IMP02',
        storeEdgeHabilitado: true
      })
    );
  });

  it('muestra los errores de validación y no envía', async () => {
    const { onSubmit, user } = renderForm();

    await user.type(screen.getByLabelText('Serie de boleta'), 'X001');
    await user.type(screen.getByLabelText('Dirección IP'), '999.1.1.1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByText('El código es obligatorio.')).toBeInTheDocument();
    expect(screen.getByText('La serie de boleta debe iniciar con B y tener 4 caracteres.')).toBeInTheDocument();
    expect(screen.getByText('La dirección IP no es válida.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('en creación no muestra el estado y el código es editable', () => {
    renderForm();

    expect(screen.queryByLabelText('Estado')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Código')).not.toHaveAttribute('readonly');
  });

  it('en edición muestra el estado, precarga los datos y deja el código de solo lectura', async () => {
    const { onSubmit, user } = renderForm({
      isEdit: true,
      submitLabel: 'Guardar cambios',
      defaultValues: { ...TERMINAL_FORM_VACIO, codigo: 'POS001', nombre: 'Caja 1' }
    });

    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    await user.selectOptions(screen.getByLabelText('Estado'), 'MANTENIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'POS001', estado: 'MANTENIMIENTO' })
    );
  });

  it('muestra el error del servidor y deshabilita el envío mientras guarda', () => {
    renderForm({ error: 'Ya existe un terminal con el código indicado.', isSubmitting: true });

    expect(screen.getByRole('alert')).toHaveTextContent('Ya existe un terminal con el código indicado.');
    expect(screen.getByRole('button', { name: 'Crear terminal' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/AlmacenForm.test.tsx apps/erp-web/src/features/organizacion/components/TerminalForm.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import` para ambos formularios).

- [ ] **Step 3: Implementar `AlmacenForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { TIPOS_ALMACEN } from '../api/almacenes.types';
import { ALMACEN_FORM_VACIO } from '../lib/form-defaults';
import { almacenSchema, type AlmacenFormValues } from '../schemas/almacen.schema';
import { FormError } from './FormError';
import { CheckboxField, SelectField, TextField } from './FormFields';

export type AlmacenFormProps = {
  defaultValues?: AlmacenFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: AlmacenFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function AlmacenForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: AlmacenFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<AlmacenFormValues>({
    defaultValues: defaultValues ?? ALMACEN_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(almacenSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id="almacen-codigo"
          label="Código"
          readOnly={isEdit}
          error={errors.codigo?.message}
          {...register('codigo')}
        />
        <TextField
          id="almacen-nombre"
          label="Nombre"
          error={errors.nombre?.message}
          {...register('nombre')}
        />
        <SelectField
          id="almacen-tipo"
          label="Tipo de almacén"
          error={errors.tipo?.message}
          {...register('tipo')}
        >
          {TIPOS_ALMACEN.map((tipo) => (
            <option key={tipo} value={tipo}>
              {tipo}
            </option>
          ))}
        </SelectField>
        <TextField
          id="almacen-temperatura-min"
          label="Temperatura mínima (°C)"
          inputMode="decimal"
          error={errors.temperaturaMinC?.message}
          {...register('temperaturaMinC')}
        />
        <TextField
          id="almacen-temperatura-max"
          label="Temperatura máxima (°C)"
          inputMode="decimal"
          error={errors.temperaturaMaxC?.message}
          {...register('temperaturaMaxC')}
        />
      </div>
      <div className="flex flex-wrap gap-6">
        <CheckboxField id="almacen-lotes" label="Permite lotes" {...register('permiteLotes')} />
        <CheckboxField
          id="almacen-vencimiento"
          label="Permite vencimiento"
          {...register('permiteVencimiento')}
        />
        <CheckboxField id="almacen-venta" label="Permite venta" {...register('permiteVenta')} />
        <CheckboxField
          id="almacen-despacho"
          label="Permite despacho"
          {...register('permiteDespacho')}
        />
        <CheckboxField
          id="almacen-control-temperatura"
          label="Controla temperatura"
          {...register('controlTemperatura')}
        />
        {isEdit ? (
          <CheckboxField id="almacen-activo" label="Almacén activo" {...register('activo')} />
        ) : null}
      </div>
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: Implementar `TerminalForm.tsx`**

```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Button } from '@boticas/ui-web';
import { ESTADOS_TERMINAL } from '../api/terminales.types';
import { TERMINAL_FORM_VACIO } from '../lib/form-defaults';
import { terminalSchema, type TerminalFormValues } from '../schemas/terminal.schema';
import { FormError } from './FormError';
import { CheckboxField, SelectField, TextField } from './FormFields';

export type TerminalFormProps = {
  defaultValues?: TerminalFormValues | undefined;
  isEdit?: boolean;
  onSubmit: (values: TerminalFormValues) => void;
  submitLabel: string;
  isSubmitting?: boolean;
  error?: string | null;
};

export function TerminalForm({
  defaultValues,
  isEdit = false,
  onSubmit,
  submitLabel,
  isSubmitting = false,
  error = null
}: TerminalFormProps) {
  const {
    formState: { errors },
    handleSubmit,
    register
  } = useForm<TerminalFormValues>({
    defaultValues: defaultValues ?? TERMINAL_FORM_VACIO,
    mode: 'onTouched',
    resolver: zodResolver(terminalSchema)
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(event) => {
        void handleSubmit((values) => onSubmit(values))(event);
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id="terminal-codigo"
          label="Código"
          readOnly={isEdit}
          error={errors.codigo?.message}
          {...register('codigo')}
        />
        <TextField
          id="terminal-nombre"
          label="Nombre"
          error={errors.nombre?.message}
          {...register('nombre')}
        />
        <TextField
          id="terminal-serie-boleta"
          label="Serie de boleta"
          error={errors.serieBoletaDefecto?.message}
          {...register('serieBoletaDefecto')}
        />
        <TextField
          id="terminal-serie-factura"
          label="Serie de factura"
          error={errors.serieFacturaDefecto?.message}
          {...register('serieFacturaDefecto')}
        />
        <TextField
          id="terminal-numero-serie"
          label="Número de serie del equipo"
          error={errors.numeroSerieEquipo?.message}
          {...register('numeroSerieEquipo')}
        />
        <TextField
          id="terminal-hostname"
          label="Hostname"
          error={errors.hostname?.message}
          {...register('hostname')}
        />
        <TextField
          id="terminal-ip"
          label="Dirección IP"
          error={errors.ipEquipo?.message}
          {...register('ipEquipo')}
        />
        <TextField
          id="terminal-impresora"
          label="Código de impresora"
          error={errors.impresoraCodigo?.message}
          {...register('impresoraCodigo')}
        />
        {isEdit ? (
          <SelectField
            id="terminal-estado"
            label="Estado"
            error={errors.estado?.message}
            {...register('estado')}
          >
            {ESTADOS_TERMINAL.map((estado) => (
              <option key={estado} value={estado}>
                {estado}
              </option>
            ))}
          </SelectField>
        ) : null}
      </div>
      <CheckboxField
        id="terminal-store-edge"
        label="Habilitar store edge"
        {...register('storeEdgeHabilitado')}
      />
      {error ? <FormError message={error} /> : null}
      <Button type="submit" disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 5: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/**" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: todos los tests de `components/` PASS y 100% de cobertura en todos los archivos.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components
git commit -m "feat(organizacion): agregar formularios de almacen y terminal POS

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 15: Utilidades de test compartidas y `EmpresasPage`

**Files:**
- Create: `frontend/apps/erp-web/src/test/render-route.tsx`
- Create: `frontend/apps/erp-web/src/test/organizacion-fixtures.ts`
- Create: `FE/pages/EmpresasPage.tsx`
- Test: `FE/pages/EmpresasPage.test.tsx`

(`FE` = `frontend/apps/erp-web/src/features/organizacion`.)

**Interfaces:**
- Consumes: `empresasQuery`, `crearEmpresa` (Task 7); `toCrearEmpresaPayload` (Task 8); `EmpresaForm` (Task 12); `invalidateOrganizacion`; `describeApiError`; `valueOrDash`; `useTenantId` (Tasks 6–7); `AuthSessionContext` de la feature `auth`.
- Produces:
  - `renderRoute(path: string, Component: ComponentType, initialEntry: string)` → `{ user, queryClient, router, ...renderResult }`, con `QueryClientProvider` (sin reintentos), sesión autenticada con `tenantId = 'tenant-1'` y un `createMemoryRouter`. Exporta también `TEST_TENANT = 'tenant-1'`.
  - Fixtures `sampleEmpresa`, `sampleEstablecimiento`, `sampleAlmacen`, `sampleTerminal` y `pagina<T>(items, overrides?)` que arma un `PaginaResponse`.
  - `EmpresasPage()` (componente sin props) para la ruta `organizacion/empresas`.

- [ ] **Step 1: Crear las utilidades de test**

`frontend/apps/erp-web/src/test/render-route.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentType } from 'react';
import { createMemoryRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { AuthSessionContext, type AuthSession } from '../features/auth/model/auth-session.context';

export const TEST_TENANT = 'tenant-1';

const session: AuthSession = {
  status: 'authenticated',
  authenticated: true,
  accessToken: 'token',
  tenantId: TEST_TENANT,
  userId: 'user-1',
  authenticate: vi.fn(),
  signOut: vi.fn()
};

export function renderRoute(path: string, Component: ComponentType, initialEntry: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{ path, Component }], { initialEntries: [initialEntry] });
  return {
    user: userEvent.setup(),
    queryClient,
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionContext value={session}>
          <RouterProvider router={router} />
        </AuthSessionContext>
      </QueryClientProvider>
    )
  };
}
```

`frontend/apps/erp-web/src/test/organizacion-fixtures.ts`:

```ts
import type { Almacen } from '../features/organizacion/api/almacenes.types';
import type { Empresa } from '../features/organizacion/api/empresas.types';
import type { Establecimiento } from '../features/organizacion/api/establecimientos.types';
import type { Terminal } from '../features/organizacion/api/terminales.types';

export const sampleEmpresa: Empresa = {
  id: 'empresa-1',
  tenantId: 'tenant-1',
  ruc: '20123456789',
  razonSocial: 'Boticas SAC',
  nombreComercial: null,
  direccionFiscal: null,
  ubigeoFiscal: null,
  telefono: null,
  email: null,
  sitioWeb: null,
  monedaFuncional: 'PEN',
  zonaHoraria: 'America/Lima',
  permiteVentaOnline: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleEstablecimiento: Establecimiento = {
  id: 'est-1',
  tenantId: 'tenant-1',
  empresaId: 'empresa-1',
  codigo: 'EST001',
  nombre: 'Botica Central',
  tipoEstablecimiento: 'BOTICA',
  categoriaRegulatoriaCodigo: null,
  codigoAnexoSunat: '0001',
  codigoDigemid: null,
  direccion: null,
  ubigeo: null,
  referencia: null,
  latitud: null,
  longitud: null,
  telefono: null,
  email: null,
  esPrincipal: true,
  permiteVentaOnline: false,
  permiteDelivery: false,
  perfilOperacion: 'ONLINE',
  zonaHoraria: 'America/Lima',
  estadoOperativo: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleAlmacen: Almacen = {
  id: 'alm-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'ALM001',
  nombre: 'Almacén Central',
  tipo: 'GENERAL',
  permiteLotes: true,
  permiteVencimiento: true,
  permiteVenta: false,
  permiteDespacho: true,
  controlTemperatura: false,
  temperaturaMinC: null,
  temperaturaMaxC: null,
  activo: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export const sampleTerminal: Terminal = {
  id: 'term-1',
  tenantId: 'tenant-1',
  establecimientoId: 'est-1',
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: null,
  serieFacturaDefecto: null,
  numeroSerieEquipo: null,
  hostname: null,
  ipEquipo: null,
  impresoraCodigo: null,
  storeEdgeHabilitado: false,
  estado: 'ACTIVO',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null
};

export function pagina<T>(
  items: T[],
  overrides: Partial<{ page: number; size: number; totalElements: number }> = {}
) {
  return { items, page: 0, size: 20, totalElements: items.length, ...overrides };
}
```

- [ ] **Step 2: Escribir el test de `EmpresasPage` (falla: módulo inexistente)**

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEmpresa } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresasPage } from './EmpresasPage';

const listUrl = '*/api/v1/organizacion/empresas';

function renderPage() {
  return renderRoute('/organizacion/empresas', EmpresasPage, '/organizacion/empresas');
}

describe('EmpresasPage', () => {
  it('lista las empresas con enlace al detalle, nombre comercial y estado', async () => {
    server.use(
      http.get(listUrl, () =>
        HttpResponse.json(
          pagina([
            sampleEmpresa,
            {
              ...sampleEmpresa,
              id: 'empresa-2',
              razonSocial: 'Inversiones Andinas SAC',
              nombreComercial: 'Andinas',
              estado: 'SUSPENDIDO'
            }
          ])
        )
      )
    );

    renderPage();

    const link = await screen.findByRole('link', { name: 'Boticas SAC' });
    expect(link).toHaveAttribute('href', '/organizacion/empresas/empresa-1');
    expect(screen.getByRole('link', { name: 'Inversiones Andinas SAC' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-2'
    );
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('Andinas')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('SUSPENDIDO')).toBeInTheDocument();
  });

  it('muestra el estado de carga y luego el mensaje de lista vacía', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('Aún no hay empresas registradas.')).toBeInTheDocument();
  });

  it('informa cuando no se puede cargar el listado', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));

    renderPage();

    expect(await screen.findByText('No se pudo cargar el listado de empresas.')).toBeInTheDocument();
  });

  it('busca en el servidor, pagina y cambia el tamaño de página', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([sampleEmpresa], {
            page: Number(received.get('page')),
            size: Number(received.get('size')),
            totalElements: 120
          })
        );
      })
    );
    const { user } = renderPage();

    await screen.findByText('Boticas SAC');
    expect(received.get('tenantId')).toBe('tenant-1');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(received.get('page')).toBe('1'));
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filas por página' }), '50');
    await waitFor(() => {
      expect(received.get('size')).toBe('50');
      expect(received.get('page')).toBe('0');
    });
    await user.type(screen.getByLabelText('Buscar empresa'), 'bot');
    await waitFor(() => expect(received.get('search')).toBe('bot'));
    expect(received.get('page')).toBe('0');
  });

  it('crea una empresa y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleEmpresa] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleEmpresa, { status: 201 });
      })
    );
    const { user } = renderPage();
    await screen.findByText('Aún no hay empresas registradas.');

    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(await screen.findByRole('link', { name: 'Boticas SAC' })).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      ruc: '20123456789',
      razonSocial: 'Boticas SAC',
      monedaFuncional: 'PEN'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe una empresa con el RUC indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderPage();
    await screen.findByText('Aún no hay empresas registradas.');

    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    await user.type(screen.getByLabelText('RUC'), '20123456789');
    await user.type(screen.getByLabelText('Razón social'), 'Boticas SAC');
    await user.click(screen.getByRole('button', { name: 'Crear empresa' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una empresa con el RUC indicado.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EmpresasPage.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EmpresasPage"`).

- [ ] **Step 4: Implementar `EmpresasPage.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, DataTable, EstadoBadge, ListFilters, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { crearEmpresa, empresasQuery } from '../api/empresas.api';
import type { Empresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { EmpresaForm } from '../components/EmpresaForm';
import { describeApiError } from '../lib/describe-api-error';
import { valueOrDash } from '../lib/format';
import { toCrearEmpresaPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EmpresaFormValues } from '../schemas/empresa.schema';

export function EmpresasPage() {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data, isPending, isError } = useQuery({
    ...empresasQuery({ tenantId, search, page, size }),
    enabled: tenantId !== ''
  });

  const createMutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      crearEmpresa(apiClient, toCrearEmpresaPayload(tenantId, values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Empresas"
        context="Organización / Empresas"
        description="Administra las empresas operadoras del tenant."
        actions={<Button onClick={() => setCreateOpen(true)}>Nueva empresa</Button>}
      />

      <ListFilters
        label="Buscar empresa"
        placeholder="RUC, razón social o nombre comercial"
        value={search}
        onValueChange={(value) => {
          setSearch(value);
          setPage(0);
        }}
      />

      <div className="mt-6">
        <DataTable<Empresa>
          columns={[
            { header: 'RUC', cell: (row) => row.ruc },
            {
              header: 'Razón social',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/empresas/${row.id}`}
                >
                  {row.razonSocial}
                </Link>
              )
            },
            { header: 'Nombre comercial', cell: (row) => valueOrDash(row.nombreComercial) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Aún no hay empresas registradas."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar el listado de empresas."
          pagination={{
            page,
            size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nueva empresa" size="lg">
        <EmpresaForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear empresa"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </div>
  );
}
```

- [ ] **Step 5: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EmpresasPage.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/{features/organizacion/pages/EmpresasPage.tsx,test/render-route.tsx,test/organizacion-fixtures.ts}" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 6 tests PASS y 100% en `EmpresasPage.tsx`, `render-route.tsx` y `organizacion-fixtures.ts`. Si `organizacion-fixtures.ts` marca ramas sin cubrir en `pagina` (parámetro por defecto), es porque este archivo aún no lo usa sin `overrides`; se cubre en las tareas siguientes y en el `pnpm check` final — no relajar el umbral.

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/erp-web/src/test frontend/apps/erp-web/src/features/organizacion/pages
git commit -m "feat(organizacion): agregar pagina de listado de empresas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 16: `EstablecimientosSection` y `EmpresaDetailPage`

**Files:**
- Create: `FE/components/EstablecimientosSection.tsx` y `EstablecimientosSection.test.tsx`
- Create: `FE/pages/EmpresaDetailPage.tsx` y `EmpresaDetailPage.test.tsx`

**Interfaces:**
- Consumes: `establecimientosQuery`, `crearEstablecimiento` (Task 7); `empresaQuery`, `actualizarEmpresa`, `cambiarEstadoEmpresa`, `ESTADOS_EMPRESA`; `toCrearEstablecimientoPayload`, `toActualizarEmpresaPayload` (Task 8); `toEmpresaFormValues` (Task 10); `DatoItem`, `CambiarEstadoDialog`, `FormError` (Task 11); `EmpresaForm` (Task 12); `EstablecimientoForm` (Task 13); `useRouteParam` (Task 6); `renderRoute` y fixtures (Task 15).
- Produces: `EstablecimientosSection({ empresaId: string })` (lista con «Nuevo establecimiento»); `EmpresaDetailPage()` para la ruta `organizacion/empresas/:empresaId`.

- [ ] **Step 1: Escribir el test de `EstablecimientosSection` (falla: módulo inexistente)**

```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientosSection } from './EstablecimientosSection';

const listUrl = '*/api/v1/organizacion/establecimientos';

function renderSection() {
  return renderRoute('/empresa', () => <EstablecimientosSection empresaId="empresa-1" />, '/empresa');
}

describe('EstablecimientosSection', () => {
  it('lista los establecimientos de la empresa con enlace al detalle', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(pagina([sampleEstablecimiento]));
      })
    );

    renderSection();

    const link = await screen.findByRole('link', { name: 'Botica Central' });
    expect(link).toHaveAttribute('href', '/organizacion/establecimientos/est-1');
    expect(screen.getByText('EST001')).toBeInTheDocument();
    expect(screen.getByText('ONLINE')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(received.get('empresaId')).toBe('empresa-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('Esta empresa aún no tiene establecimientos.')).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los establecimientos.')).toBeInTheDocument();
  });

  it('crea un establecimiento de la empresa y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () =>
        HttpResponse.json(pagina(created ? [sampleEstablecimiento] : []))
      ),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleEstablecimiento, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      empresaId: 'empresa-1',
      codigo: 'EST001',
      nombre: 'Botica Central',
      perfilOperacion: 'ONLINE'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un establecimiento con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Esta empresa aún no tiene establecimientos.');

    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    await user.type(screen.getByLabelText('Código'), 'EST001');
    await user.type(screen.getByLabelText('Nombre'), 'Botica Central');
    await user.click(screen.getByRole('button', { name: 'Crear establecimiento' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya existe un establecimiento con el código indicado.'
    );
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo establecimiento' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Implementar `EstablecimientosSection.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, DataTable, EstadoBadge, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { invalidateOrganizacion } from '../api/invalidate';
import { crearEstablecimiento, establecimientosQuery } from '../api/establecimientos.api';
import type { Establecimiento } from '../api/establecimientos.types';
import { describeApiError } from '../lib/describe-api-error';
import { toCrearEstablecimientoPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';
import { EstablecimientoForm } from './EstablecimientoForm';

export function EstablecimientosSection({ empresaId }: { empresaId: string }) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);

  const { data, isPending, isError } = useQuery({
    ...establecimientosQuery({ tenantId, empresaId, size: 100 }),
    enabled: tenantId !== ''
  });

  const createMutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      crearEstablecimiento(apiClient, toCrearEstablecimientoPayload(tenantId, empresaId, values)),
    onSuccess: () => {
      setCreateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  return (
    <section aria-label="Establecimientos" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Establecimientos</h2>
        <Button onClick={() => setCreateOpen(true)}>Nuevo establecimiento</Button>
      </div>

      <div className="mt-4">
        <DataTable<Establecimiento>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            {
              header: 'Nombre',
              cell: (row) => (
                <Link
                  className="text-primary-700 dark:text-primary-400 font-semibold hover:underline"
                  to={`/organizacion/establecimientos/${row.id}`}
                >
                  {row.nombre}
                </Link>
              )
            },
            { header: 'Perfil', cell: (row) => row.perfilOperacion },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estadoOperativo} /> }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Esta empresa aún no tiene establecimientos."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los establecimientos."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo establecimiento" size="lg">
        <EstablecimientoForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear establecimiento"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>
    </section>
  );
}
```

- [ ] **Step 3: Ejecutar el test de la sección y verificar cobertura**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/EstablecimientosSection.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/EstablecimientosSection.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 4 tests PASS y 100% en `EstablecimientosSection.tsx`.

- [ ] **Step 4: Escribir el test de `EmpresaDetailPage` (falla: módulo inexistente)**

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleEmpresa, sampleEstablecimiento } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EmpresaDetailPage } from './EmpresaDetailPage';

const detailUrl = '*/api/v1/organizacion/empresas/empresa-1';
const statusUrl = '*/api/v1/organizacion/empresas/empresa-1/estado';

function mockDefaultHandlers() {
  server.use(
    http.get(detailUrl, () => HttpResponse.json(sampleEmpresa)),
    http.get('*/api/v1/organizacion/establecimientos', () =>
      HttpResponse.json(pagina([sampleEstablecimiento]))
    )
  );
}

function renderPage() {
  return renderRoute(
    '/organizacion/empresas/:empresaId',
    EmpresaDetailPage,
    '/organizacion/empresas/empresa-1'
  );
}

describe('EmpresaDetailPage', () => {
  it('muestra los datos de la empresa y sus establecimientos', async () => {
    mockDefaultHandlers();

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Boticas SAC' })).toBeInTheDocument();
    expect(screen.getByText('20123456789')).toBeInTheDocument();
    expect(screen.getByText('PEN')).toBeInTheDocument();
    expect(screen.getByText('America/Lima')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    expect(screen.getByText('No')).toBeInTheDocument();
    expect(screen.getAllByText('ACTIVO').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'Organización / Empresas' })).toHaveAttribute(
      'href',
      '/organizacion/empresas'
    );
    expect(await screen.findByRole('link', { name: 'Botica Central' })).toBeInTheDocument();
  });

  it('muestra el estado de carga inicial', () => {
    mockDefaultHandlers();

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('informa cuando la empresa no existe', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ title: 'Not Found', detail: 'La empresa indicada no existe.' }, { status: 404 })
      )
    );

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('edita la empresa sin enviar el RUC', async () => {
    mockDefaultHandlers();
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(detailUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEmpresa, razonSocial: 'Boticas del Perú SAC' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('RUC')).toHaveAttribute('readonly');
    const razonSocial = screen.getByLabelText('Razón social');
    await user.clear(razonSocial);
    await user.type(razonSocial, 'Boticas del Perú SAC');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ razonSocial: 'Boticas del Perú SAC', monedaFuncional: 'PEN' });
    expect('ruc' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.put(detailUrl, () =>
        HttpResponse.json({ title: 'Bad Request', detail: 'La razón social no es válida.' }, { status: 400 })
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('La razón social no es válida.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('cambia el estado de la empresa', async () => {
    mockDefaultHandlers();
    let body: unknown;
    server.use(
      http.patch(statusUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleEmpresa, estado: 'SUSPENDIDO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDIDO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ estado: 'SUSPENDIDO' });
  });

  it('muestra el error al cambiar el estado y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(http.patch(statusUrl, () => HttpResponse.json({ title: 'Forbidden' }, { status: 403 })));
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Boticas SAC' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'BLOQUEADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 5: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EmpresaDetailPage"`).

- [ ] **Step 6: Implementar `EmpresaDetailPage.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarEmpresa, cambiarEstadoEmpresa, empresaQuery } from '../api/empresas.api';
import { ESTADOS_EMPRESA, type EstadoEmpresa } from '../api/empresas.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { CambiarEstadoDialog } from '../components/CambiarEstadoDialog';
import { DatoItem } from '../components/DatoItem';
import { EmpresaForm } from '../components/EmpresaForm';
import { EstablecimientosSection } from '../components/EstablecimientosSection';
import { FormError } from '../components/FormError';
import { describeApiError } from '../lib/describe-api-error';
import { toEmpresaFormValues } from '../lib/form-defaults';
import { toActualizarEmpresaPayload } from '../lib/form-payloads';
import { valueOrDash, yesNo } from '../lib/format';
import { useRouteParam } from '../lib/use-route-param';
import { useTenantId } from '../lib/use-tenant-id';
import type { EmpresaFormValues } from '../schemas/empresa.schema';

export function EmpresaDetailPage() {
  const empresaId = useRouteParam('empresaId');
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [stateOpen, setStateOpen] = useState(false);

  const result = useQuery({ ...empresaQuery(tenantId, empresaId), enabled: tenantId !== '' });

  const updateMutation = useMutation({
    mutationFn: (values: EmpresaFormValues) =>
      actualizarEmpresa(apiClient, empresaId, tenantId, toActualizarEmpresaPayload(values)),
    onSuccess: () => {
      setEditOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const stateMutation = useMutation({
    mutationFn: (estado: EstadoEmpresa) => cambiarEstadoEmpresa(apiClient, empresaId, tenantId, estado),
    onSuccess: () => {
      setStateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeEdit = () => {
    setEditOpen(false);
    updateMutation.reset();
  };

  const closeState = () => {
    setStateOpen(false);
    stateMutation.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const empresa = result.data;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={empresa.razonSocial}
        context={<Link to="/organizacion/empresas">Organización / Empresas</Link>}
        description={`RUC ${empresa.ruc}`}
        actions={
          <>
            <Button variant="secondary" onClick={() => setEditOpen(true)}>
              Editar
            </Button>
            <Button variant="secondary" onClick={() => setStateOpen(true)}>
              Cambiar estado
            </Button>
          </>
        }
      />

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoBadge status={empresa.estado} />
          </DatoItem>
          <DatoItem label="RUC">{empresa.ruc}</DatoItem>
          <DatoItem label="Nombre comercial">{valueOrDash(empresa.nombreComercial)}</DatoItem>
          <DatoItem label="Dirección fiscal">{valueOrDash(empresa.direccionFiscal)}</DatoItem>
          <DatoItem label="Ubigeo fiscal">{valueOrDash(empresa.ubigeoFiscal)}</DatoItem>
          <DatoItem label="Teléfono">{valueOrDash(empresa.telefono)}</DatoItem>
          <DatoItem label="Correo">{valueOrDash(empresa.email)}</DatoItem>
          <DatoItem label="Sitio web">{valueOrDash(empresa.sitioWeb)}</DatoItem>
          <DatoItem label="Moneda funcional">{empresa.monedaFuncional}</DatoItem>
          <DatoItem label="Zona horaria">{empresa.zonaHoraria}</DatoItem>
          <DatoItem label="Venta online">{yesNo(empresa.permiteVentaOnline)}</DatoItem>
        </dl>
      </Card>

      <EstablecimientosSection empresaId={empresaId} />

      <Modal open={editOpen} onClose={closeEdit} title="Editar empresa" size="lg">
        <EmpresaForm
          isEdit
          defaultValues={toEmpresaFormValues(empresa)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
        />
      </Modal>

      {stateOpen ? (
        <CambiarEstadoDialog
          title="Cambiar estado de la empresa"
          estados={ESTADOS_EMPRESA}
          current={empresa.estado}
          isSubmitting={stateMutation.isPending}
          error={stateMutation.isError ? describeApiError(stateMutation.error) : null}
          onSubmit={(estado) => stateMutation.mutate(estado)}
          onClose={closeState}
        />
      ) : null}
    </div>
  );
}
```

- [ ] **Step 7: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx apps/erp-web/src/features/organizacion/components/EstablecimientosSection.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/{pages/EmpresaDetailPage.tsx,components/EstablecimientosSection.tsx}" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 11 tests PASS y 100% en ambos archivos. Nota de test: el mensaje de la página cuando la empresa no existe se muestra con `FormError` (`role="alert"`), por eso la aserción usa `findByRole('alert')`.

- [ ] **Step 8: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components/EstablecimientosSection.tsx frontend/apps/erp-web/src/features/organizacion/components/EstablecimientosSection.test.tsx frontend/apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.tsx frontend/apps/erp-web/src/features/organizacion/pages/EmpresaDetailPage.test.tsx
git commit -m "feat(organizacion): agregar detalle de empresa con establecimientos y cambio de estado

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 17: `AlmacenesSection` y `TerminalesSection`

**Files:**
- Create: `FE/components/AlmacenesSection.tsx` y `AlmacenesSection.test.tsx`
- Create: `FE/components/TerminalesSection.tsx` y `TerminalesSection.test.tsx`

**Interfaces:**
- Consumes: `almacenesQuery`, `crearAlmacen`, `actualizarAlmacen`, `terminalesQuery`, `crearTerminal`, `actualizarTerminal` (Task 7); `toCrearAlmacenPayload`, `toActualizarAlmacenPayload`, `toCrearTerminalPayload`, `toActualizarTerminalPayload` (Task 8); `toAlmacenFormValues`, `toTerminalFormValues` (Task 10); `AlmacenForm`, `TerminalForm` (Task 14); `renderRoute` y fixtures (Task 15).
- Produces: `AlmacenesSection({ establecimientoId: string })` y `TerminalesSection({ establecimientoId: string })`: tabla con «Nuevo …», y edición desde el ícono de lápiz de cada fila («Editar {nombre}»).

- [ ] **Step 1: Escribir el test de `AlmacenesSection` (falla: módulo inexistente)**

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleAlmacen } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { AlmacenesSection } from './AlmacenesSection';

const listUrl = '*/api/v1/organizacion/almacenes';
const itemUrl = '*/api/v1/organizacion/almacenes/alm-1';

function renderSection() {
  return renderRoute(
    '/establecimiento',
    () => <AlmacenesSection establecimientoId="est-1" />,
    '/establecimiento'
  );
}

describe('AlmacenesSection', () => {
  it('lista los almacenes del establecimiento con su estado', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([
            sampleAlmacen,
            { ...sampleAlmacen, id: 'alm-2', codigo: 'ALM002', nombre: 'Almacén Norte', tipo: 'CUARENTENA', activo: false }
          ])
        );
      })
    );

    renderSection();

    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(screen.getByText('ALM001')).toBeInTheDocument();
    expect(screen.getByText('GENERAL')).toBeInTheDocument();
    expect(screen.getByText('CUARENTENA')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('INACTIVO')).toBeInTheDocument();
    expect(received.get('establecimientoId')).toBe('est-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('Este establecimiento aún no tiene almacenes.')).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los almacenes.')).toBeInTheDocument();
  });

  it('crea un almacén del establecimiento y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleAlmacen] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleAlmacen, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'ALM001',
      nombre: 'Almacén Central',
      tipo: 'GENERAL'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un almacén con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene almacenes.');

    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    await user.type(screen.getByLabelText('Código'), 'ALM001');
    await user.type(screen.getByLabelText('Nombre'), 'Almacén Central');
    await user.click(screen.getByRole('button', { name: 'Crear almacén' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un almacén con el código indicado.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo almacén' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('edita un almacén precargando sus datos y permite desactivarlo', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleAlmacen]))),
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleAlmacen, activo: false });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Almacén Central');

    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('ALM001');
    expect(codigo).toHaveAttribute('readonly');
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Almacén Principal');
    await user.click(screen.getByLabelText('Almacén activo'));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Almacén Principal', tipo: 'GENERAL', activo: false });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleAlmacen]))),
      http.put(itemUrl, () =>
        HttpResponse.json({ title: 'Bad Request', detail: 'El nombre no es válido.' }, { status: 400 })
      )
    );
    const { user } = renderSection();
    await screen.findByText('Almacén Central');

    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar Almacén Central' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Implementar `AlmacenesSection.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { actualizarAlmacen, almacenesQuery, crearAlmacen } from '../api/almacenes.api';
import type { Almacen } from '../api/almacenes.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { describeApiError } from '../lib/describe-api-error';
import { toAlmacenFormValues } from '../lib/form-defaults';
import { toActualizarAlmacenPayload, toCrearAlmacenPayload } from '../lib/form-payloads';
import { useTenantId } from '../lib/use-tenant-id';
import type { AlmacenFormValues } from '../schemas/almacen.schema';
import { AlmacenForm } from './AlmacenForm';

export function AlmacenesSection({ establecimientoId }: { establecimientoId: string }) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Almacen | null>(null);

  const { data, isPending, isError } = useQuery({
    ...almacenesQuery({ tenantId, establecimientoId, size: 100 }),
    enabled: tenantId !== ''
  });

  const onSaved = () => {
    setCreateOpen(false);
    setEditing(null);
    void invalidateOrganizacion(queryClient);
  };

  const createMutation = useMutation({
    mutationFn: (values: AlmacenFormValues) =>
      crearAlmacen(apiClient, toCrearAlmacenPayload(tenantId, establecimientoId, values)),
    onSuccess: onSaved
  });

  const updateMutation = useMutation({
    mutationFn: ({ almacenId, values }: { almacenId: string; values: AlmacenFormValues }) =>
      actualizarAlmacen(apiClient, almacenId, tenantId, toActualizarAlmacenPayload(values)),
    onSuccess: onSaved
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  const closeEdit = () => {
    setEditing(null);
    updateMutation.reset();
  };

  return (
    <section aria-label="Almacenes" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Almacenes</h2>
        <Button onClick={() => setCreateOpen(true)}>Nuevo almacén</Button>
      </div>

      <div className="mt-4">
        <DataTable<Almacen>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Tipo', cell: (row) => row.tipo },
            {
              header: 'Estado',
              cell: (row) => <EstadoBadge status={row.activo ? 'ACTIVO' : 'INACTIVO'} />
            },
            {
              header: 'Acciones',
              cell: (row) => (
                <IconButton
                  icon={Pencil}
                  label={`Editar ${row.nombre}`}
                  onClick={() => setEditing(row)}
                />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Este establecimiento aún no tiene almacenes."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los almacenes."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo almacén" size="lg">
        <AlmacenForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear almacén"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>

      <Modal open={editing !== null} onClose={closeEdit} title="Editar almacén" size="lg">
        {editing ? (
          <AlmacenForm
            isEdit
            defaultValues={toAlmacenFormValues(editing)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
            error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
            onSubmit={(values) => updateMutation.mutate({ almacenId: editing.id, values })}
          />
        ) : null}
      </Modal>
    </section>
  );
}
```

- [ ] **Step 3: Ejecutar y verificar cobertura de `AlmacenesSection`**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/AlmacenesSection.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/AlmacenesSection.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 6 tests PASS y 100% en `AlmacenesSection.tsx`.

- [ ] **Step 4: Escribir el test de `TerminalesSection` (falla: módulo inexistente)**

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { pagina, sampleTerminal } from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { TerminalesSection } from './TerminalesSection';

const listUrl = '*/api/v1/organizacion/terminales-pos';
const itemUrl = '*/api/v1/organizacion/terminales-pos/term-1';

function renderSection() {
  return renderRoute(
    '/establecimiento',
    () => <TerminalesSection establecimientoId="est-1" />,
    '/establecimiento'
  );
}

describe('TerminalesSection', () => {
  it('lista los terminales del establecimiento con IP y estado', async () => {
    let received = new URLSearchParams();
    server.use(
      http.get(listUrl, ({ request }) => {
        received = new URL(request.url).searchParams;
        return HttpResponse.json(
          pagina([
            sampleTerminal,
            {
              ...sampleTerminal,
              id: 'term-2',
              codigo: 'POS002',
              nombre: 'Caja 2',
              ipEquipo: '10.0.0.16',
              estado: 'BLOQUEADO'
            }
          ])
        );
      })
    );

    renderSection();

    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
    expect(screen.getByText('POS001')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('10.0.0.16')).toBeInTheDocument();
    expect(screen.getByText('ACTIVO')).toBeInTheDocument();
    expect(screen.getByText('BLOQUEADO')).toBeInTheDocument();
    expect(received.get('establecimientoId')).toBe('est-1');
    expect(received.get('size')).toBe('100');
  });

  it('muestra carga, lista vacía y error de carga', async () => {
    server.use(http.get(listUrl, () => HttpResponse.json(pagina([]))));
    const first = renderSection();
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
    expect(await screen.findByText('Este establecimiento aún no tiene terminales.')).toBeInTheDocument();
    first.unmount();

    server.use(http.get(listUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));
    renderSection();
    expect(await screen.findByText('No se pudo cargar los terminales.')).toBeInTheDocument();
  });

  it('crea un terminal del establecimiento y refresca el listado', async () => {
    let created: unknown;
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina(created ? [sampleTerminal] : []))),
      http.post(listUrl, async ({ request }) => {
        created = await request.json();
        return HttpResponse.json(sampleTerminal, { status: 201 });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.type(screen.getByLabelText('Dirección IP'), '10.0.0.15');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
    expect(created).toMatchObject({
      tenantId: 'tenant-1',
      establecimientoId: 'est-1',
      codigo: 'POS001',
      nombre: 'Caja 1',
      ipEquipo: '10.0.0.15'
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('muestra el error del servidor al crear y lo limpia al cerrar el modal', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([]))),
      http.post(listUrl, () =>
        HttpResponse.json(
          { title: 'Conflict', detail: 'Ya existe un terminal con el código indicado.' },
          { status: 409 }
        )
      )
    );
    const { user } = renderSection();
    await screen.findByText('Este establecimiento aún no tiene terminales.');

    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    await user.type(screen.getByLabelText('Código'), 'POS001');
    await user.type(screen.getByLabelText('Nombre'), 'Caja 1');
    await user.click(screen.getByRole('button', { name: 'Crear terminal' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un terminal con el código indicado.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Nuevo terminal' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('edita un terminal precargando sus datos y permite cambiar su estado', async () => {
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleTerminal]))),
      http.put(itemUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleTerminal, estado: 'MANTENIMIENTO' });
      })
    );
    const { user } = renderSection();
    await screen.findByText('Caja 1');

    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    const codigo = screen.getByLabelText('Código');
    expect(codigo).toHaveValue('POS001');
    expect(codigo).toHaveAttribute('readonly');
    await user.selectOptions(screen.getByLabelText('Estado'), 'MANTENIMIENTO');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Caja 1', estado: 'MANTENIMIENTO' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    server.use(
      http.get(listUrl, () => HttpResponse.json(pagina([sampleTerminal]))),
      http.put(itemUrl, () =>
        HttpResponse.json({ title: 'Bad Request', detail: 'El terminal no es válido.' }, { status: 400 })
      )
    );
    const { user } = renderSection();
    await screen.findByText('Caja 1');

    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El terminal no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar Caja 1' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 5: Implementar `TerminalesSection.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { Button, DataTable, EstadoBadge, IconButton, Modal } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import { invalidateOrganizacion } from '../api/invalidate';
import { actualizarTerminal, crearTerminal, terminalesQuery } from '../api/terminales.api';
import type { Terminal } from '../api/terminales.types';
import { describeApiError } from '../lib/describe-api-error';
import { toTerminalFormValues } from '../lib/form-defaults';
import { toActualizarTerminalPayload, toCrearTerminalPayload } from '../lib/form-payloads';
import { valueOrDash } from '../lib/format';
import { useTenantId } from '../lib/use-tenant-id';
import type { TerminalFormValues } from '../schemas/terminal.schema';
import { TerminalForm } from './TerminalForm';

export function TerminalesSection({ establecimientoId }: { establecimientoId: string }) {
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Terminal | null>(null);

  const { data, isPending, isError } = useQuery({
    ...terminalesQuery({ tenantId, establecimientoId, size: 100 }),
    enabled: tenantId !== ''
  });

  const onSaved = () => {
    setCreateOpen(false);
    setEditing(null);
    void invalidateOrganizacion(queryClient);
  };

  const createMutation = useMutation({
    mutationFn: (values: TerminalFormValues) =>
      crearTerminal(apiClient, toCrearTerminalPayload(tenantId, establecimientoId, values)),
    onSuccess: onSaved
  });

  const updateMutation = useMutation({
    mutationFn: ({ terminalId, values }: { terminalId: string; values: TerminalFormValues }) =>
      actualizarTerminal(apiClient, terminalId, tenantId, toActualizarTerminalPayload(values)),
    onSuccess: onSaved
  });

  const closeCreate = () => {
    setCreateOpen(false);
    createMutation.reset();
  };

  const closeEdit = () => {
    setEditing(null);
    updateMutation.reset();
  };

  return (
    <section aria-label="Terminales POS" className="mt-8">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-bold text-neutral-950 dark:text-white">Terminales POS</h2>
        <Button onClick={() => setCreateOpen(true)}>Nuevo terminal</Button>
      </div>

      <div className="mt-4">
        <DataTable<Terminal>
          columns={[
            { header: 'Código', cell: (row) => row.codigo },
            { header: 'Nombre', cell: (row) => row.nombre },
            { header: 'Dirección IP', cell: (row) => valueOrDash(row.ipEquipo) },
            { header: 'Estado', cell: (row) => <EstadoBadge status={row.estado} /> },
            {
              header: 'Acciones',
              cell: (row) => (
                <IconButton
                  icon={Pencil}
                  label={`Editar ${row.nombre}`}
                  onClick={() => setEditing(row)}
                />
              )
            }
          ]}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          emptyMessage="Este establecimiento aún no tiene terminales."
          isLoading={isPending}
          isError={isError}
          errorMessage="No se pudo cargar los terminales."
        />
      </div>

      <Modal open={createOpen} onClose={closeCreate} title="Nuevo terminal" size="lg">
        <TerminalForm
          onSubmit={(values) => createMutation.mutate(values)}
          submitLabel="Crear terminal"
          isSubmitting={createMutation.isPending}
          error={createMutation.isError ? describeApiError(createMutation.error) : null}
        />
      </Modal>

      <Modal open={editing !== null} onClose={closeEdit} title="Editar terminal" size="lg">
        {editing ? (
          <TerminalForm
            isEdit
            defaultValues={toTerminalFormValues(editing)}
            submitLabel="Guardar cambios"
            isSubmitting={updateMutation.isPending}
            error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
            onSubmit={(values) => updateMutation.mutate({ terminalId: editing.id, values })}
          />
        ) : null}
      </Modal>
    </section>
  );
}
```

- [ ] **Step 6: Ejecutar ambas secciones con cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/components/AlmacenesSection.test.tsx apps/erp-web/src/features/organizacion/components/TerminalesSection.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/components/{AlmacenesSection,TerminalesSection}.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 12 tests PASS y 100% en ambos archivos.

- [ ] **Step 7: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/components/AlmacenesSection.tsx frontend/apps/erp-web/src/features/organizacion/components/AlmacenesSection.test.tsx frontend/apps/erp-web/src/features/organizacion/components/TerminalesSection.tsx frontend/apps/erp-web/src/features/organizacion/components/TerminalesSection.test.tsx
git commit -m "feat(organizacion): agregar secciones de almacenes y terminales POS

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 18: `EstablecimientoDetailPage`

**Files:**
- Create: `FE/pages/EstablecimientoDetailPage.tsx`
- Test: `FE/pages/EstablecimientoDetailPage.test.tsx`

**Interfaces:**
- Consumes: `establecimientoQuery`, `actualizarEstablecimiento`, `cambiarEstadoEstablecimiento`, `ESTADOS_ESTABLECIMIENTO`, `EstadoEstablecimiento` (Task 7); `toActualizarEstablecimientoPayload` (Task 8); `toEstablecimientoFormValues`, `numberOrEmpty` (Task 10); `EstablecimientoForm` (Task 13); `AlmacenesSection`, `TerminalesSection` (Task 17); `DatoItem`, `CambiarEstadoDialog`, `FormError` (Task 11); `useRouteParam`, `useTenantId`; `renderRoute` y fixtures (Task 15).
- Produces: `EstablecimientoDetailPage()` para la ruta `organizacion/establecimientos/:establecimientoId`.

- [ ] **Step 1: Escribir el test (falla: módulo inexistente)**

```tsx
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import {
  pagina,
  sampleAlmacen,
  sampleEstablecimiento,
  sampleTerminal
} from '../../../test/organizacion-fixtures';
import { renderRoute } from '../../../test/render-route';
import { EstablecimientoDetailPage } from './EstablecimientoDetailPage';

const detailUrl = '*/api/v1/organizacion/establecimientos/est-1';
const statusUrl = '*/api/v1/organizacion/establecimientos/est-1/estado';

function mockDefaultHandlers() {
  server.use(
    http.get(detailUrl, () => HttpResponse.json(sampleEstablecimiento)),
    http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([sampleAlmacen]))),
    http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([sampleTerminal])))
  );
}

function renderPage() {
  return renderRoute(
    '/organizacion/establecimientos/:establecimientoId',
    EstablecimientoDetailPage,
    '/organizacion/establecimientos/est-1'
  );
}

describe('EstablecimientoDetailPage', () => {
  it('muestra los datos del establecimiento, sus almacenes y sus terminales', async () => {
    mockDefaultHandlers();

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Botica Central' })).toBeInTheDocument();
    expect(screen.getByText('EST001')).toBeInTheDocument();
    expect(screen.getByText('0001')).toBeInTheDocument();
    expect(screen.getByText('BOTICA')).toBeInTheDocument();
    expect(screen.getByText('ONLINE')).toBeInTheDocument();
    expect(screen.getByText('Sí')).toBeInTheDocument();
    expect(screen.getAllByText('No').length).toBeGreaterThan(0);
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: 'Organización / Empresa' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-1'
    );
    expect(await screen.findByText('Almacén Central')).toBeInTheDocument();
    expect(await screen.findByText('Caja 1')).toBeInTheDocument();
  });

  it('muestra las coordenadas cuando existen', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json({ ...sampleEstablecimiento, latitud: -12.0464, longitud: -77.0428 })
      ),
      http.get('*/api/v1/organizacion/almacenes', () => HttpResponse.json(pagina([]))),
      http.get('*/api/v1/organizacion/terminales-pos', () => HttpResponse.json(pagina([])))
    );

    renderPage();

    expect(await screen.findByText('-12.0464')).toBeInTheDocument();
    expect(screen.getByText('-77.0428')).toBeInTheDocument();
  });

  it('muestra el estado de carga inicial', () => {
    mockDefaultHandlers();

    renderPage();

    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });

  it('informa cuando el establecimiento no existe', async () => {
    server.use(
      http.get(detailUrl, () =>
        HttpResponse.json(
          { title: 'Not Found', detail: 'El establecimiento indicado no existe.' },
          { status: 404 }
        )
      )
    );

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El recurso no existe o no pertenece a tu organización.'
    );
  });

  it('edita el establecimiento sin enviar el código', async () => {
    mockDefaultHandlers();
    let query = new URLSearchParams();
    let body: Record<string, unknown> = {};
    server.use(
      http.put(detailUrl, async ({ request }) => {
        query = new URL(request.url).searchParams;
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...sampleEstablecimiento, nombre: 'Botica Principal' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('Código')).toHaveAttribute('readonly');
    const nombre = screen.getByLabelText('Nombre');
    await user.clear(nombre);
    await user.type(nombre, 'Botica Principal');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(query.get('tenantId')).toBe('tenant-1');
    expect(body).toMatchObject({ nombre: 'Botica Principal', perfilOperacion: 'ONLINE' });
    expect('codigo' in body).toBe(false);
  });

  it('muestra el error del servidor al editar y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(
      http.put(detailUrl, () =>
        HttpResponse.json({ title: 'Bad Request', detail: 'El nombre no es válido.' }, { status: 400 })
      )
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Editar' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El nombre no es válido.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('cambia el estado del establecimiento', async () => {
    mockDefaultHandlers();
    let body: unknown;
    server.use(
      http.patch(statusUrl, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...sampleEstablecimiento, estadoOperativo: 'CLAUSURADO' });
      })
    );
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'CLAUSURADO');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(body).toEqual({ estado: 'CLAUSURADO' });
  });

  it('muestra el error al cambiar el estado y lo limpia al cerrar', async () => {
    mockDefaultHandlers();
    server.use(http.patch(statusUrl, () => HttpResponse.json({ title: 'Forbidden' }, { status: 403 })));
    const { user } = renderPage();
    await screen.findByRole('heading', { name: 'Botica Central' });

    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    await user.selectOptions(screen.getByLabelText('Estado'), 'REMODELACION');
    await user.click(screen.getByRole('button', { name: 'Guardar estado' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.');
    await user.click(screen.getByRole('button', { name: 'Cerrar' }));
    await user.click(screen.getByRole('button', { name: 'Cambiar estado' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.test.tsx --project erp-web`
Expected: FAIL (`Failed to resolve import "./EstablecimientoDetailPage"`).

- [ ] **Step 3: Implementar `EstablecimientoDetailPage.tsx`**

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { Button, Card, EstadoBadge, Modal, PageHeader } from '@boticas/ui-web';
import { apiClient } from '../../../app/api';
import {
  actualizarEstablecimiento,
  cambiarEstadoEstablecimiento,
  establecimientoQuery
} from '../api/establecimientos.api';
import {
  ESTADOS_ESTABLECIMIENTO,
  type EstadoEstablecimiento
} from '../api/establecimientos.types';
import { invalidateOrganizacion } from '../api/invalidate';
import { AlmacenesSection } from '../components/AlmacenesSection';
import { CambiarEstadoDialog } from '../components/CambiarEstadoDialog';
import { DatoItem } from '../components/DatoItem';
import { EstablecimientoForm } from '../components/EstablecimientoForm';
import { FormError } from '../components/FormError';
import { TerminalesSection } from '../components/TerminalesSection';
import { describeApiError } from '../lib/describe-api-error';
import { toEstablecimientoFormValues } from '../lib/form-defaults';
import { toActualizarEstablecimientoPayload } from '../lib/form-payloads';
import { numberOrEmpty } from '../lib/form-values';
import { valueOrDash, yesNo } from '../lib/format';
import { useRouteParam } from '../lib/use-route-param';
import { useTenantId } from '../lib/use-tenant-id';
import type { EstablecimientoFormValues } from '../schemas/establecimiento.schema';

export function EstablecimientoDetailPage() {
  const establecimientoId = useRouteParam('establecimientoId');
  const tenantId = useTenantId();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [stateOpen, setStateOpen] = useState(false);

  const result = useQuery({
    ...establecimientoQuery(tenantId, establecimientoId),
    enabled: tenantId !== ''
  });

  const updateMutation = useMutation({
    mutationFn: (values: EstablecimientoFormValues) =>
      actualizarEstablecimiento(
        apiClient,
        establecimientoId,
        tenantId,
        toActualizarEstablecimientoPayload(values)
      ),
    onSuccess: () => {
      setEditOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const stateMutation = useMutation({
    mutationFn: (estado: EstadoEstablecimiento) =>
      cambiarEstadoEstablecimiento(apiClient, establecimientoId, tenantId, estado),
    onSuccess: () => {
      setStateOpen(false);
      void invalidateOrganizacion(queryClient);
    }
  });

  const closeEdit = () => {
    setEditOpen(false);
    updateMutation.reset();
  };

  const closeState = () => {
    setStateOpen(false);
    stateMutation.reset();
  };

  if (result.isPending) {
    return <p className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</p>;
  }
  if (result.isError) return <FormError message={describeApiError(result.error)} />;

  const establecimiento = result.data;

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title={establecimiento.nombre}
        context={
          <Link to={`/organizacion/empresas/${establecimiento.empresaId}`}>
            Organización / Empresa
          </Link>
        }
        description={`Código ${establecimiento.codigo}`}
        actions={
          <>
            <Button variant="secondary" onClick={() => setEditOpen(true)}>
              Editar
            </Button>
            <Button variant="secondary" onClick={() => setStateOpen(true)}>
              Cambiar estado
            </Button>
          </>
        }
      />

      <Card className="mt-6 p-6">
        <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <DatoItem label="Estado">
            <EstadoBadge status={establecimiento.estadoOperativo} />
          </DatoItem>
          <DatoItem label="Código">{establecimiento.codigo}</DatoItem>
          <DatoItem label="Tipo">{establecimiento.tipoEstablecimiento}</DatoItem>
          <DatoItem label="Perfil de operación">{establecimiento.perfilOperacion}</DatoItem>
          <DatoItem label="Anexo SUNAT">{establecimiento.codigoAnexoSunat}</DatoItem>
          <DatoItem label="Código DIGEMID">{valueOrDash(establecimiento.codigoDigemid)}</DatoItem>
          <DatoItem label="Categoría regulatoria">
            {valueOrDash(establecimiento.categoriaRegulatoriaCodigo)}
          </DatoItem>
          <DatoItem label="Dirección">{valueOrDash(establecimiento.direccion)}</DatoItem>
          <DatoItem label="Ubigeo">{valueOrDash(establecimiento.ubigeo)}</DatoItem>
          <DatoItem label="Referencia">{valueOrDash(establecimiento.referencia)}</DatoItem>
          <DatoItem label="Latitud">{valueOrDash(numberOrEmpty(establecimiento.latitud))}</DatoItem>
          <DatoItem label="Longitud">{valueOrDash(numberOrEmpty(establecimiento.longitud))}</DatoItem>
          <DatoItem label="Teléfono">{valueOrDash(establecimiento.telefono)}</DatoItem>
          <DatoItem label="Correo">{valueOrDash(establecimiento.email)}</DatoItem>
          <DatoItem label="Zona horaria">{establecimiento.zonaHoraria}</DatoItem>
          <DatoItem label="Es principal">{yesNo(establecimiento.esPrincipal)}</DatoItem>
          <DatoItem label="Venta online">{yesNo(establecimiento.permiteVentaOnline)}</DatoItem>
          <DatoItem label="Delivery">{yesNo(establecimiento.permiteDelivery)}</DatoItem>
        </dl>
      </Card>

      <AlmacenesSection establecimientoId={establecimientoId} />
      <TerminalesSection establecimientoId={establecimientoId} />

      <Modal open={editOpen} onClose={closeEdit} title="Editar establecimiento" size="lg">
        <EstablecimientoForm
          isEdit
          defaultValues={toEstablecimientoFormValues(establecimiento)}
          submitLabel="Guardar cambios"
          isSubmitting={updateMutation.isPending}
          error={updateMutation.isError ? describeApiError(updateMutation.error) : null}
          onSubmit={(values) => updateMutation.mutate(values)}
        />
      </Modal>

      {stateOpen ? (
        <CambiarEstadoDialog
          title="Cambiar estado del establecimiento"
          estados={ESTADOS_ESTABLECIMIENTO}
          current={establecimiento.estadoOperativo}
          isSubmitting={stateMutation.isPending}
          error={stateMutation.isError ? describeApiError(stateMutation.error) : null}
          onSubmit={(estado) => stateMutation.mutate(estado)}
          onClose={closeState}
        />
      ) : null}
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar y verificar cobertura completa**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.test.tsx --project erp-web --coverage --coverage.include="apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.tsx" --coverage.thresholds.lines=100 --coverage.thresholds.branches=100 --coverage.thresholds.functions=100 --coverage.thresholds.statements=100`
Expected: 8 tests PASS y 100% en `EstablecimientoDetailPage.tsx`. La aserción `getByText('Sí')` asume que solo `esPrincipal` es `true` en `sampleEstablecimiento`; el resto de indicadores son `No`.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.tsx frontend/apps/erp-web/src/features/organizacion/pages/EstablecimientoDetailPage.test.tsx
git commit -m "feat(organizacion): agregar detalle de establecimiento con almacenes, terminales y cambio de estado

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 19: Integración — rutas, resumen enlazado y compuerta de cobertura

**Files:**
- Modify: `FE/routes.tsx`
- Create: `FE/routes.test.ts`
- Modify: `FE/pages/OrganizationPage.tsx`
- Modify: `FE/pages/OrganizationPage.test.tsx`
- Modify: `frontend/apps/erp-web/src/app/feature-routes.test.ts`
- Modify: `frontend/coverage-baseline.txt`

**Interfaces:**
- Consumes: `EmpresasPage`, `EmpresaDetailPage`, `EstablecimientoDetailPage` (Tasks 15, 16, 18); `renderRoute` (Task 15).
- Produces: rutas `organizacion`, `organizacion/empresas`, `organizacion/empresas/:empresaId`, `organizacion/establecimientos/:establecimientoId` cargadas con lazy loading; `OrganizationPage` con enlaces a los detalles y botón «Gestionar empresas»; `OrganizationPage.tsx` y `routes.tsx` fuera del baseline de cobertura (ahora al 100%).

- [ ] **Step 1: Escribir el test de rutas (falla: aún no existen las rutas nuevas)**

`FE/routes.test.ts`:

```ts
import { organizationRoutes } from './routes';

describe('organizationRoutes', () => {
  it('declara las cuatro rutas de la feature', () => {
    expect(organizationRoutes.map(({ path }) => path)).toEqual([
      'organizacion',
      'organizacion/empresas',
      'organizacion/empresas/:empresaId',
      'organizacion/establecimientos/:establecimientoId'
    ]);
  });

  it.each(organizationRoutes.map((route) => [route.path, route] as const))(
    'carga la página de %s con lazy loading',
    async (_path, route) => {
      const loaded = await route.lazy();

      expect(typeof loaded.Component).toBe('function');
    }
  );
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/routes.test.ts --project erp-web`
Expected: FAIL (la lista de rutas solo contiene `organizacion`).

- [ ] **Step 2: Reemplazar `routes.tsx`**

```tsx
import type { RouteObject } from 'react-router';

export const organizationRoutes = [
  {
    path: 'organizacion',
    lazy: async () => {
      const { OrganizationPage } = await import('./pages/OrganizationPage');
      return { Component: OrganizationPage };
    }
  },
  {
    path: 'organizacion/empresas',
    lazy: async () => {
      const { EmpresasPage } = await import('./pages/EmpresasPage');
      return { Component: EmpresasPage };
    }
  },
  {
    path: 'organizacion/empresas/:empresaId',
    lazy: async () => {
      const { EmpresaDetailPage } = await import('./pages/EmpresaDetailPage');
      return { Component: EmpresaDetailPage };
    }
  },
  {
    path: 'organizacion/establecimientos/:establecimientoId',
    lazy: async () => {
      const { EstablecimientoDetailPage } = await import('./pages/EstablecimientoDetailPage');
      return { Component: EstablecimientoDetailPage };
    }
  }
] satisfies RouteObject[];
```

- [ ] **Step 3: Actualizar el test de composición de rutas de la app**

En `frontend/apps/erp-web/src/app/feature-routes.test.ts`, en la lista `paths`, reemplazar la línea `'organizacion',` por:

```ts
      'organizacion',
      'organizacion/empresas',
      'organizacion/empresas/:empresaId',
      'organizacion/establecimientos/:establecimientoId',
```

- [ ] **Step 4: Ejecutar rutas y composición**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/routes.test.ts apps/erp-web/src/app/feature-routes.test.ts --project erp-web`
Expected: PASS (4 cargas lazy + composición).

- [ ] **Step 5: Reescribir el test de `OrganizationPage` (falla: aún no hay enlaces)**

Reemplazar `FE/pages/OrganizationPage.test.tsx` por:

```tsx
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { renderRoute } from '../../../test/render-route';
import { OrganizationPage } from './OrganizationPage';

const structureUrl = '*/api/v1/estructura-corporativa';

const structure = {
  asOf: '2026-08-31T20:00:00-05:00',
  companies: [
    {
      id: 'empresa-1',
      legalName: 'Boticas del Pacífico S.A.C.',
      tradeName: 'Boticas Pacífico',
      status: 'ACTIVE',
      establishments: [
        {
          id: 'est-1',
          code: 'LIM-001',
          name: 'Botica Miraflores',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [{ id: 'alm-1', code: 'ALM-01', name: 'Almacén principal', status: 'ACTIVE' }],
          cashRegisters: [{ id: 'caj-1', code: 'CAJ-01', name: 'Caja principal', status: 'ACTIVE' }]
        },
        {
          id: 'est-2',
          code: 'LIM-002',
          name: 'Botica Surco',
          status: 'SUSPENDED',
          timeZone: 'America/Lima',
          warehouses: [],
          cashRegisters: []
        }
      ]
    },
    {
      id: 'empresa-2',
      legalName: 'Inversiones Andinas S.A.C.',
      tradeName: null,
      status: 'INACTIVE',
      establishments: [
        {
          id: 'est-3',
          code: 'CUS-001',
          name: 'Botica Cusco',
          status: 'INACTIVE',
          timeZone: 'America/Lima',
          warehouses: [],
          cashRegisters: []
        }
      ]
    }
  ]
};

function renderPage() {
  return renderRoute('/organizacion', OrganizationPage, '/organizacion');
}

describe('OrganizationPage', () => {
  it('muestra la estructura corporativa con enlaces a los detalles', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    expect(screen.getByRole('heading', { name: 'Organización' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Boticas Pacífico' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-1'
    );
    expect(screen.getByRole('link', { name: 'Inversiones Andinas S.A.C.' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-2'
    );
    expect(screen.getByRole('link', { name: 'Botica Miraflores' })).toHaveAttribute(
      'href',
      '/organizacion/establecimientos/est-1'
    );
    expect(screen.getByRole('link', { name: 'Gestionar empresas' })).toHaveAttribute(
      'href',
      '/organizacion/empresas'
    );
    expect(screen.getByText(/Almacén principal/u)).toBeInTheDocument();
    expect(screen.getByText('Caja principal')).toBeInTheDocument();
    expect(screen.getAllByText('Sin almacenes visibles').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Sin cajas visibles').length).toBeGreaterThan(0);
    expect(screen.getByText(/Fecha de corte/u)).toBeInTheDocument();
  });

  it('traduce los estados activo, suspendido e inactivo', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    await screen.findByRole('link', { name: 'Boticas Pacífico' });
    expect(screen.getAllByText('Activo').length).toBe(2);
    expect(screen.getAllByText('Suspendido').length).toBe(1);
    expect(screen.getAllByText('Inactivo').length).toBe(2);
  });

  it('muestra los contadores de la estructura', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    expect(screen.getAllByText('—').length).toBe(4);
    await screen.findByRole('link', { name: 'Boticas Pacífico' });
    expect(screen.queryByText('—')).not.toBeInTheDocument();
    const summary = within(screen.getByRole('region', { name: 'Resumen de estructura' }));
    expect(summary.getByText('Empresas').nextElementSibling).toHaveTextContent('2');
    expect(summary.getByText('Establecimientos').nextElementSibling).toHaveTextContent('3');
    expect(summary.getByText('Almacenes').nextElementSibling).toHaveTextContent('1');
    expect(summary.getByText('Cajas').nextElementSibling).toHaveTextContent('1');
  });

  it('informa cuando no se puede obtener la estructura', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 })));

    renderPage();

    expect(await screen.findByText(/No fue posible obtener la estructura corporativa/u)).toBeInTheDocument();
  });
});
```

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/OrganizationPage.test.tsx --project erp-web`
Expected: FAIL (no hay enlaces `Boticas Pacífico`, `Botica Miraflores` ni «Gestionar empresas»).

- [ ] **Step 6: Modificar `OrganizationPage.tsx`**

Cuatro ediciones sobre el archivo existente:

1. Imports. Reemplazar:

```tsx
import { Badge, Card } from '@boticas/ui-web';
```

por:

```tsx
import { Link } from 'react-router';
import { Badge, Card, buttonClassName } from '@boticas/ui-web';
```

2. Encabezado. Reemplazar este bloque:

```tsx
      <div>
        <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
          Foundation / Core maestro
        </p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
          Organización
        </h1>
        <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
          Estructura corporativa efectiva dentro del ámbito autorizado de la sesión.
        </p>
      </div>
```

por:

```tsx
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <p className="text-primary-700 dark:text-primary-400 text-sm font-semibold">
            Foundation / Core maestro
          </p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-neutral-950 dark:text-white">
            Organización
          </h1>
          <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
            Estructura corporativa efectiva dentro del ámbito autorizado de la sesión.
          </p>
        </div>
        <Link className={buttonClassName('primary')} to="/organizacion/empresas">
          Gestionar empresas
        </Link>
      </div>
```

3. Título de la empresa. Reemplazar:

```tsx
                  <h2 className="font-bold text-neutral-950 dark:text-white">
                    {company.tradeName ?? company.legalName}
                  </h2>
```

por:

```tsx
                  <h2 className="font-bold text-neutral-950 dark:text-white">
                    <Link className="hover:underline" to={`/organizacion/empresas/${company.id}`}>
                      {company.tradeName ?? company.legalName}
                    </Link>
                  </h2>
```

4. Título del establecimiento. Reemplazar:

```tsx
                        <h3 className="mt-1 font-semibold text-neutral-900 dark:text-neutral-50">
                          {establishment.name}
                        </h3>
```

por:

```tsx
                        <h3 className="mt-1 font-semibold text-neutral-900 dark:text-neutral-50">
                          <Link
                            className="hover:underline"
                            to={`/organizacion/establecimientos/${establishment.id}`}
                          >
                            {establishment.name}
                          </Link>
                        </h3>
```

- [ ] **Step 7: Ejecutar el test de `OrganizationPage`**

Run: `cd frontend; pnpm.cmd exec vitest run apps/erp-web/src/features/organizacion/pages/OrganizationPage.test.tsx --project erp-web`
Expected: 4 tests PASS. Si el test de contadores falla en `nextElementSibling`, confirmar que en `OrganizationPage.tsx` la etiqueta (`<p>` con el nombre) y el valor (`<p>` del número) son hermanos dentro del mismo `<div>` de la tarjeta del resumen.

- [ ] **Step 8: Sacar los dos archivos del baseline de cobertura**

En `frontend/coverage-baseline.txt`, eliminar exactamente estas dos líneas (el resto del archivo no se toca):

```
apps/erp-web/src/features/organizacion/pages/OrganizationPage.tsx
apps/erp-web/src/features/organizacion/routes.tsx
```

- [ ] **Step 9: Formatear los archivos nuevos y modificados**

Run: `cd frontend; pnpm.cmd exec prettier --write apps/erp-web/src/features/organizacion apps/erp-web/src/test/render-route.tsx apps/erp-web/src/test/organizacion-fixtures.ts apps/erp-web/src/app/feature-routes.test.ts packages/ui-web/src/select packages/ui-web/src/modal packages/ui-web/src/index.ts`
Expected: Prettier reescribe únicamente estos archivos. Revisar con `git diff --stat` que no aparezcan archivos ajenos a la feature.

- [ ] **Step 10: Ejecutar la compuerta completa del frontend**

Run: `cd frontend; pnpm.cmd check`
Expected: `lint`, `typecheck`, `test` (con cobertura al 100% global) y `build` terminan sin errores. Si `typecheck` reporta errores de tipos en tests (por ejemplo el tipado de `Partial<Parameters<...>>`), corregirlos con `import type` de las props exportadas. Si la cobertura global falla, el reporte lista el archivo y las líneas; agregar el caso de test faltante en el archivo correspondiente (no modificar umbrales ni baseline).

- [ ] **Step 11: Commit**

```bash
git add frontend
git commit -m "feat(organizacion): integrar rutas de gestion y enlazar el resumen con los detalles

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 20: Verificación de extremo a extremo y documentación

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Reconstruir y levantar el backend con los endpoints nuevos**

Run (PowerShell, desde la raíz del repositorio): `docker compose build app; docker compose up -d`
Expected: contenedores `app` y `postgres` en estado `healthy`. Verificar: `docker compose ps`.

- [ ] **Step 2: Levantar el frontend contra el backend real**

Run (PowerShell): `cd frontend; pnpm.cmd dev`
Expected: Vite en `http://localhost:3000` y `frontend/apps/erp-web/.env.development` con `VITE_API_MODE=http` y `DEV_API_TARGET` apuntando al puerto publicado del backend (`HOST_PORT` del `.env` raíz, 8085 en este entorno). Si ya hay un servidor Vite corriendo, reutilizarlo.

- [ ] **Step 3: Recorrer el flujo completo en el navegador**

Iniciar sesión con el superadmin del entorno y verificar, en este orden:

1. `/organizacion` muestra el resumen con el botón «Gestionar empresas».
2. «Gestionar empresas» → lista de empresas → «Nueva empresa»: crear una con RUC de prueba `20999999992` y razón social «PRUEBA UI Boticas SAC». Debe aparecer en la lista.
3. Abrir el detalle de la empresa → «Editar»: cambiar el nombre comercial y guardar. Verificar que el RUC es de solo lectura.
4. «Cambiar estado» → `SUSPENDIDO` → guardar. La insignia debe cambiar; volver a `ACTIVO`.
5. «Nuevo establecimiento»: código `UI-001`, nombre «Botica UI Central», anexo `0000`. Abrir su detalle.
6. En el detalle del establecimiento: «Editar» (cambiar la referencia), «Cambiar estado» → `REMODELACION` y luego `ACTIVO`.
7. «Nuevo almacén» (`UI-ALM`, tipo `REFRIGERADO`, temperaturas 2 y 8) → editarlo y desmarcar «Almacén activo».
8. «Nuevo terminal» (`UI-POS`, series `B001`/`F001`, IP `10.0.0.77`) → editarlo y cambiar su estado a `MANTENIMIENTO`.
9. Volver a `/organizacion`: el árbol debe mostrar la empresa, el establecimiento, el almacén inactivo y el terminal, con enlaces a cada detalle.
10. Repetir el paso 2 con el mismo RUC: debe mostrarse el error de RUC duplicado dentro del modal.

Expected: cada paso funciona sin errores en la consola del navegador (pestaña Network sin respuestas 4xx/5xx inesperadas).

- [ ] **Step 4: Limpiar los datos de prueba (pedir confirmación al usuario antes de ejecutar)**

Los datos creados en el paso 3 y los de la verificación previa (`PRUEBA E2E`) quedan en la base local. Con confirmación explícita del usuario, borrarlos:

```powershell
docker compose exec -T postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "DELETE FROM sch_organizacion.terminal_pos WHERE codigo IN ('UI-POS','E2E-POS'); DELETE FROM sch_organizacion.almacen WHERE codigo IN ('UI-ALM','E2E-ALM'); DELETE FROM sch_organizacion.establecimiento_farmaceutico WHERE codigo IN ('UI-001','E2E-001'); DELETE FROM sch_organizacion.empresa_operadora WHERE razon_social IN ('PRUEBA UI Boticas SAC','PRUEBA E2E Boticas SAC');"
```

Expected: `DELETE` con las filas afectadas por tabla; `/organizacion` vuelve a mostrar la estructura vacía.

- [ ] **Step 5: Actualizar `CLAUDE.md`**

En la sección «Estado real del proyecto», dentro del punto de `frontend/`, reemplazar exactamente este fragmento:

```
organización consulta la estructura corporativa (backend ya implementado, pero el frontend sigue en modo mock por defecto y no se ha verificado contra el backend real)
```

por:

```
`features/organizacion/` tiene UI real de gestión (empresas → detalle de empresa → detalle de establecimiento con almacenes y terminales POS, formularios completos y cambio de estado) integrada contra `packages/api-client`, sin handlers de MSW para sus endpoints
```

- [ ] **Step 6: Verificación final de ambos lados**

Run: `cd service-botica; .\gradlew.bat check --warning-mode all` y `cd ..\frontend; pnpm.cmd check`
Expected: ambos terminan con éxito.

- [ ] **Step 7: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: actualizar estado real del proyecto tras implementar la UI de organizacion

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```
