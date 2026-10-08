# Organización: reglas de negocio del backend (Plan A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el backend de `organizacion` rechace RUC con dígito verificador inválido, series de comprobantes repetidas dentro de una empresa, altas de hijos bajo un padre no operativo y almacenes con temperaturas incoherentes.

**Architecture:** Las reglas puras (RUC, temperaturas, qué estados admiten altas) viven en el dominio, sin Spring. Las reglas que dependen de otros registros (series por empresa, estado del padre) se aplican en `OrganizacionJpaWriteAdapter`, con el mismo patrón que `DUPLICATE_CODIGO`: el puerto de escritura devuelve un resultado (`SaveXxxOutcome`) y el handler lo traduce a `ApplicationError`. Una migración Flyway `V026` refuerza la unicidad de series en la base de datos.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JPA + `JdbcClient`, Flyway, JUnit 5 + Mockito + AssertJ, Testcontainers (PostgreSQL), JaCoCo con umbral de 100% por clase.

## Global Constraints

Tomadas del spec `docs/superpowers/specs/2026-09-30-organizacion-reglas-y-validaciones-design.md`:

- Las series de comprobantes (boleta y factura) son únicas **por empresa (RUC)** entre cajas activas (`es_activo = '1'`). La caja que se edita se excluye de la comprobación.
- Mensaje de serie repetida: `La serie B001 ya está asignada a otra caja de esta empresa.` (con la serie real). Respuesta `409`.
- Validación del RUC con módulo 11, además del formato actual (11 dígitos, inicia con 10 o 20). Mensaje: `El RUC no es válido: el dígito verificador no coincide.` Solo aplica al alta.
- Empresa `SUSPENDIDO` o `BLOQUEADO` no admite establecimientos nuevos. Establecimiento `SUSPENDIDO` o `CLAUSURADO` no admite almacenes ni terminales nuevos. `REMODELACION` sí los admite. Editar lo existente sigue permitido. Respuesta `409`.
- Almacén: si hay ambas temperaturas, mín. ≤ máx.; si controla temperatura, ambas son obligatorias; si es `REFRIGERADO`, debe controlar temperatura. Respuesta `400`.
- Cada archivo nuevo o modificado debe quedar con 100% de cobertura de líneas y ramas (JaCoCo por clase).
- Sin comentarios explicativos en el código; sin código duplicado; Clean Architecture + DDD + Ports & Adapters + CQRS con `Result`.
- Mensajes en español, como el resto del módulo.

## Mapa de archivos

Rutas relativas a `service-botica/`. `ORG` = `modules/organizacion/src/main/java/com/softprimesolutions/organizacion`. `ORGT` = `modules/organizacion/src/test/java/com/softprimesolutions/organizacion`. `MIG` = `bootstrap-app/src/main/resources/db/migration`. `IT` = `bootstrap-app/src/test/java/com/softprimesolutions/organizacion/api/OrganizacionApiIntegrationTest.java`.

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `ORG/domain/model/EmpresaOperadora.java` | Modificar | Validar el dígito verificador del RUC |
| `ORG/domain/model/Almacen.java` | Modificar | Validar coherencia de temperaturas |
| `ORG/domain/model/EstadoEmpresaOperadora.java` | Modificar | `admiteAltasDeHijos()` |
| `ORG/domain/model/EstadoEstablecimiento.java` | Modificar | `admiteAltasDeHijos()` |
| `ORG/application/port/out/OrganizacionWritePort.java` | Modificar | Nuevos resultados de guardado |
| `ORG/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java` | Modificar | Estado del padre y series por empresa |
| `ORG/application/usecase/command/TerminalSaveErrors.java` | Crear | Traducir resultados de series a `ApplicationError` (compartido por crear y editar terminal) |
| `ORG/application/usecase/command/CrearEstablecimientoHandler.java` | Modificar | Mapear `EMPRESA_NO_OPERATIVA` |
| `ORG/application/usecase/command/CrearAlmacenHandler.java` | Modificar | Mapear `ESTABLECIMIENTO_NO_OPERATIVO` |
| `ORG/application/usecase/command/CrearTerminalPosHandler.java` | Modificar | Mapear padre no operativo y series |
| `ORG/application/usecase/command/ActualizarTerminalPosHandler.java` | Modificar | Mapear series al editar |
| `MIG/V026__terminal_pos_series_unicas_por_empresa.sql` | Crear | Índices únicos parciales de series |
| `IT` | Modificar | Casos HTTP de extremo a extremo |

---

### Task 1: RUC con dígito verificador (módulo 11)

**Files:**
- Modify: `ORG/domain/model/EmpresaOperadora.java`
- Modify (test): `ORGT/domain/model/EmpresaOperadoraTest.java`
- Modify (sustitución de RUC en tests): todos los `.java` bajo `service-botica/` que usan `20123456789`, `10123456789` o `20123456780`

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces: `EmpresaOperadora.create(...)` devuelve `Result.failure` con `ErrorDetail` (campo `ruc`, mensaje `El RUC no es válido: el dígito verificador no coincide.`) cuando el dígito no coincide. Los RUC válidos de referencia para tests de todas las tareas: `20123456786` (dígito 6), `20123456794` (dígito 4), `10123456781` (dígito 1), `20000000010` (dígito 0) y `20000000061` (dígito 1).

- [ ] **Step 1: Escribir los tests que fallan**

En `ORGT/domain/model/EmpresaOperadoraTest.java`, añadir estos métodos dentro de la clase, justo antes de la llave final:

```java
    @Test
    void rejectsRucWithInvalidCheckDigit() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.message()).isEqualTo("El RUC no es válido: el dígito verificador no coincide.");
            assertThat(error.metadata()).containsEntry("field", "ruc");
            return null;
        });
    }

    @Test
    void acceptsRucWhenTheCheckDigitIsZeroOrOne() {
        var withZero = EmpresaOperadora.create(
                ID, TENANT_ID, "20000000010", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        var withOne = EmpresaOperadora.create(
                ID, TENANT_ID, "20000000061", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(withZero.isSuccess()).isTrue();
        assertThat(withOne.isSuccess()).isTrue();
    }
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run (PowerShell, desde `service-botica`): `.\gradlew.bat :modules:organizacion:test --tests "*EmpresaOperadoraTest"`
Expected: FAIL en `rejectsRucWithInvalidCheckDigit` (el RUC `20123456789` hoy se acepta).

- [ ] **Step 3: Implementar la validación**

En `ORG/domain/model/EmpresaOperadora.java`:

1. Debajo de `UBIGEO_PATTERN`, añadir la constante:

```java
    private static final int[] RUC_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
```

2. En `create`, reemplazar el bloque de validación del RUC:

```java
        var normalizedRuc = normalize(ruc);
        if (normalizedRuc == null || !RUC_PATTERN.matcher(normalizedRuc).matches()) {
            return invalid("ruc", "El RUC debe tener 11 dígitos e iniciar con 10 o 20.");
        }
```

por:

```java
        var normalizedRuc = normalize(ruc);
        if (normalizedRuc == null || !RUC_PATTERN.matcher(normalizedRuc).matches()) {
            return invalid("ruc", "El RUC debe tener 11 dígitos e iniciar con 10 o 20.");
        }
        if (!hasValidCheckDigit(normalizedRuc)) {
            return invalid("ruc", "El RUC no es válido: el dígito verificador no coincide.");
        }
```

3. Añadir el método privado junto a los otros helpers estáticos (por ejemplo, justo antes de `normalize`):

```java
    private static boolean hasValidCheckDigit(String ruc) {
        var sum = 0;
        for (var index = 0; index < RUC_WEIGHTS.length; index++) {
            sum += Character.digit(ruc.charAt(index), 10) * RUC_WEIGHTS[index];
        }
        return (11 - sum % 11) % 10 == Character.digit(ruc.charAt(10), 10);
    }
```

- [ ] **Step 4: Sustituir los RUC inválidos que usan los tests existentes**

Los tests y fixtures actuales usan RUC de dígito verificador incorrecto, que ahora se rechazan. Sustituirlos de forma global (Git Bash, desde `service-botica`):

```bash
grep -rl "20123456789\|10123456789\|20123456780" --include=*.java . | xargs sed -i 's/20123456789/20123456786/g; s/10123456789/10123456781/g; s/20123456780/20123456794/g'
```

Hay que revertir la sustitución en un solo sitio: el test `rejectsRucWithInvalidCheckDigit` que se acaba de añadir en el Step 1 debe seguir usando `20123456789`. Restaurarlo a mano:

```java
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
```

(la línea dentro de `rejectsRucWithInvalidCheckDigit`; el comando `sed` la habrá convertido en `20123456786`).

- [ ] **Step 5: Ejecutar el módulo y verificar que pasa con cobertura**

Run: `.\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`, incluido `jacocoTestCoverageVerification`.

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/organizacion service-botica/bootstrap-app/src/test
git commit -m "feat(organizacion): validar el digito verificador del RUC

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Almacén con temperaturas coherentes

**Files:**
- Modify: `ORG/domain/model/Almacen.java`
- Modify (test): `ORGT/domain/model/AlmacenTest.java`

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces: `Almacen.create(...)` y `Almacen.updateDetails(...)` devuelven `Result.failure` con `ErrorDetail` (código `ORG_ALMACEN_INVALIDO`, campo `controlTemperatura` o `temperaturaMinC`) cuando las temperaturas son incoherentes.

- [ ] **Step 1: Escribir los tests que fallan**

En `ORGT/domain/model/AlmacenTest.java`, añadir dentro de la clase, antes de la llave final:

```java
    private static Almacen baseAlmacen() {
        return Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-09", "Almacén base", TipoAlmacen.GENERAL,
                        true, true, true, true, false, null, null, NOW)
                .fold(almacen -> almacen, error -> { throw new AssertionError(error.message()); });
    }

    private static void assertRejected(
            com.softprimesolutions.shared.kernel.result.Result<Almacen, com.softprimesolutions.shared.kernel.error.ErrorDetail> result,
            String field, String message) {
        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.message()).isEqualTo(message);
            assertThat(error.metadata()).containsEntry("field", field);
            return null;
        });
    }

    @Test
    void rejectsRefrigeradoWithoutTemperatureControl() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-10", "Cámara fría", TipoAlmacen.REFRIGERADO,
                true, true, true, true, false, new BigDecimal("2"), new BigDecimal("8"), NOW);

        assertRejected(result, "controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
    }

    @Test
    void rejectsTemperatureControlWithoutMinimum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-11", "Sin mínima", TipoAlmacen.GENERAL,
                true, true, true, true, true, null, new BigDecimal("8"), NOW);

        assertRejected(result, "temperaturaMinC",
                "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
    }

    @Test
    void rejectsTemperatureControlWithoutMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-12", "Sin máxima", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("2"), null, NOW);

        assertRejected(result, "temperaturaMinC",
                "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
    }

    @Test
    void rejectsMinimumGreaterThanMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-13", "Rango invertido", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("8"), new BigDecimal("2"), NOW);

        assertRejected(result, "temperaturaMinC", "La temperatura mínima no puede ser mayor que la máxima.");
    }

    @Test
    void acceptsEqualMinimumAndMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-14", "Rango puntual", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("5"), new BigDecimal("5"), NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void acceptsASingleTemperatureWhenControlIsOff() {
        var onlyMaximum = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-15", "Solo máxima", TipoAlmacen.GENERAL,
                true, true, true, true, false, null, new BigDecimal("8"), NOW);
        var onlyMinimum = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-16", "Solo mínima", TipoAlmacen.GENERAL,
                true, true, true, true, false, new BigDecimal("2"), null, NOW);

        assertThat(onlyMaximum.isSuccess()).isTrue();
        assertThat(onlyMinimum.isSuccess()).isTrue();
    }

    @Test
    void updateDetailsRejectsRefrigeradoWithoutTemperatureControl() {
        var result = baseAlmacen().updateDetails(
                "Cámara fría", TipoAlmacen.REFRIGERADO, true, true, true, true, false, null, null, NOW);

        assertRejected(result, "controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
    }

    @Test
    void updateDetailsAcceptsACoherentTemperatureRange() {
        var result = baseAlmacen().updateDetails(
                "Cámara fría", TipoAlmacen.REFRIGERADO, true, true, true, true, true,
                new BigDecimal("2"), new BigDecimal("8"), NOW);

        assertThat(result.isSuccess()).isTrue();
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:organizacion:test --tests "*AlmacenTest"`
Expected: FAIL en los tests de rechazo (hoy el dominio no valida estas combinaciones).

- [ ] **Step 3: Implementar las reglas en `Almacen`**

En `ORG/domain/model/Almacen.java`:

1. En `create`, justo después del bloque que valida `normalizedNombre` y antes de `return Result.success(...)`, añadir:

```java
        var temperaturaError = temperaturaError(tipo, controlTemperatura, temperaturaMinC, temperaturaMaxC);
        if (temperaturaError != null) return Result.failure(temperaturaError);
```

2. En `updateDetails`, exactamente en el mismo punto (después de validar `normalizedNombre`, antes del `return Result.success(...)`), añadir las mismas dos líneas.

3. Reemplazar el método `invalid` actual:

```java
    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_ALMACEN_INVALIDO", message, Map.of("field", field)));
    }
```

por:

```java
    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(error(field, message));
    }

    private static ErrorDetail error(String field, String message) {
        return new ErrorDetail("ORG_ALMACEN_INVALIDO", message, Map.of("field", field));
    }

    private static ErrorDetail temperaturaError(
            TipoAlmacen tipo, boolean controlTemperatura, BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC) {
        if (tipo == TipoAlmacen.REFRIGERADO && !controlTemperatura) {
            return error("controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
        }
        if (controlTemperatura && (temperaturaMinC == null || temperaturaMaxC == null)) {
            return error("temperaturaMinC",
                    "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
        }
        if (temperaturaMinC != null && temperaturaMaxC != null && temperaturaMinC.compareTo(temperaturaMaxC) > 0) {
            return error("temperaturaMinC", "La temperatura mínima no puede ser mayor que la máxima.");
        }
        return null;
    }
```

- [ ] **Step 4: Ejecutar el módulo con el gate de cobertura**

Run: `.\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`. Si algún test existente construye una combinación ahora inválida (control de temperatura activo sin temperaturas, o `REFRIGERADO` sin control), corregir el dato de ese test para que sea coherente; los tests actuales usan `controlTemperatura=false` con temperaturas nulas, que sigue siendo válido.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/organizacion
git commit -m "feat(organizacion): validar coherencia de temperaturas del almacen

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Altas bloqueadas según el estado del padre

**Files:**
- Modify: `ORG/domain/model/EstadoEmpresaOperadora.java`, `ORG/domain/model/EstadoEstablecimiento.java`
- Create (test): `ORGT/domain/model/EstadoEmpresaOperadoraTest.java`, `ORGT/domain/model/EstadoEstablecimientoTest.java`
- Modify: `ORG/application/port/out/OrganizacionWritePort.java`
- Modify: `ORG/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java`
- Modify (test): `ORGT/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java`
- Modify: `ORG/application/usecase/command/CrearEstablecimientoHandler.java`, `CrearAlmacenHandler.java`, `CrearTerminalPosHandler.java`
- Modify (test): `ORGT/application/usecase/command/CrearEstablecimientoHandlerTest.java`, `CrearAlmacenHandlerTest.java`, `CrearTerminalPosHandlerTest.java`

**Interfaces:**
- Consumes: nada de tareas previas.
- Produces:
  - `EstadoEmpresaOperadora.admiteAltasDeHijos()` y `EstadoEstablecimiento.admiteAltasDeHijos()`: `boolean`.
  - `SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA`, `SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO`, `SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO`.
  - Errores de aplicación `ORG_EMPRESA_NO_OPERATIVA` y `ORG_ESTABLECIMIENTO_NO_OPERATIVO` con categoría `CONFLICT`.

- [ ] **Step 1: Escribir los tests de los enums (fallan: el método no existe)**

`ORGT/domain/model/EstadoEmpresaOperadoraTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoEmpresaOperadoraTest {

    @Test
    void onlyActivoAdmitsChildAdditions() {
        assertThat(EstadoEmpresaOperadora.ACTIVO.admiteAltasDeHijos()).isTrue();
        assertThat(EstadoEmpresaOperadora.SUSPENDIDO.admiteAltasDeHijos()).isFalse();
        assertThat(EstadoEmpresaOperadora.BLOQUEADO.admiteAltasDeHijos()).isFalse();
    }
}
```

`ORGT/domain/model/EstadoEstablecimientoTest.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoEstablecimientoTest {

    @Test
    void activoAndRemodelacionAdmitChildAdditions() {
        assertThat(EstadoEstablecimiento.ACTIVO.admiteAltasDeHijos()).isTrue();
        assertThat(EstadoEstablecimiento.REMODELACION.admiteAltasDeHijos()).isTrue();
    }

    @Test
    void suspendidoAndClausuradoDoNotAdmitChildAdditions() {
        assertThat(EstadoEstablecimiento.SUSPENDIDO.admiteAltasDeHijos()).isFalse();
        assertThat(EstadoEstablecimiento.CLAUSURADO.admiteAltasDeHijos()).isFalse();
    }
}
```

- [ ] **Step 2: Implementar los enums**

`ORG/domain/model/EstadoEmpresaOperadora.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEmpresaOperadora {
    ACTIVO,
    SUSPENDIDO,
    BLOQUEADO;

    public boolean admiteAltasDeHijos() {
        return this == ACTIVO;
    }
}
```

`ORG/domain/model/EstadoEstablecimiento.java`:

```java
package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEstablecimiento {
    ACTIVO,
    SUSPENDIDO,
    CLAUSURADO,
    REMODELACION;

    public boolean admiteAltasDeHijos() {
        return this == ACTIVO || this == REMODELACION;
    }
}
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*EstadoEmpresaOperadoraTest" --tests "*EstadoEstablecimientoTest"`
Expected: PASS.

- [ ] **Step 3: Ampliar el puerto de escritura**

En `ORG/application/port/out/OrganizacionWritePort.java`, reemplazar las tres declaraciones de enums por:

```java
    enum SaveEstablecimientoOutcome {
        CREATED, UPDATED, EMPRESA_NOT_FOUND, EMPRESA_NO_OPERATIVA, DUPLICATE_CODIGO, DUPLICATE_DIGEMID
    }

    enum SaveAlmacenOutcome {
        CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, ESTABLECIMIENTO_NO_OPERATIVO, DUPLICATE_CODIGO
    }

    enum SaveTerminalOutcome {
        CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, ESTABLECIMIENTO_NO_OPERATIVO, DUPLICATE_CODIGO
    }
```

- [ ] **Step 4: Escribir los tests del adaptador (fallan)**

En `ORGT/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java`:

1. Junto a los otros campos `resolved...`, añadir:

```java
    private String resolvedEmpresaEstado = "ACTIVO";
    private String resolvedEstablecimientoEstado = "ACTIVO";
```

2. En el método `lookup`, añadir estas dos líneas **al inicio** (antes de la línea `if (sql.contains("sch_admin.tenant WHERE"))`):

```java
        if (sql.contains("SELECT e.estado")) return Optional.of(resolvedEmpresaEstado);
        if (sql.contains("SELECT s.estado_operativo")) return Optional.of(resolvedEstablecimientoEstado);
```

3. Añadir estos tests antes de `tenantExistsReflectsLookupResult`:

```java
    @Test
    void returnsEmpresaNoOperativaWhenEmpresaIsSuspended() {
        resolvedEmpresaEstado = "SUSPENDIDO";
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA);
        verify(establecimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesEstablecimientoEvenWhenEmpresaIsBlocked() {
        resolvedEmpresaEstado = "BLOQUEADO";
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.of(mock(EstablecimientoJpaEntity.class)));

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.UPDATED);
    }

    @Test
    void returnsEstablecimientoNoOperativoForAlmacenWhenEstablecimientoIsSuspended() {
        resolvedEstablecimientoEstado = "SUSPENDIDO";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO);
        verify(almacenRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsAlmacenWhenEstablecimientoIsInRemodelacion() {
        resolvedEstablecimientoEstado = "REMODELACION";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.CREATED);
    }

    @Test
    void updatesAlmacenEvenWhenEstablecimientoIsClosed() {
        resolvedEstablecimientoEstado = "CLAUSURADO";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value()))
                .thenReturn(Optional.of(mock(AlmacenJpaEntity.class)));

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.UPDATED);
    }

    @Test
    void returnsEstablecimientoNoOperativoForTerminalWhenEstablecimientoIsClosed() {
        resolvedEstablecimientoEstado = "CLAUSURADO";
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesTerminalEvenWhenEstablecimientoIsSuspended() {
        resolvedEstablecimientoEstado = "SUSPENDIDO";
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value()))
                .thenReturn(Optional.of(mock(TerminalPosJpaEntity.class)));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.UPDATED);
    }
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*OrganizacionJpaWriteAdapterTest"`
Expected: FAIL de compilación o de aserción (el adaptador aún no consulta el estado).

- [ ] **Step 5: Implementar las comprobaciones en el adaptador**

En `ORG/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java`:

1. Añadir imports:

```java
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
```

2. En `save(Establecimiento)`, reemplazar:

```java
        var existing = establecimientoRepository.findByUuidPublico(establecimiento.id().value());
        if (existing.isEmpty()) {
            if (establecimientoRepository.existsByTenantIdAndCodigo(tenantId.get(), establecimiento.codigo())) {
```

por:

```java
        var existing = establecimientoRepository.findByUuidPublico(establecimiento.id().value());
        if (existing.isEmpty()) {
            if (!findEmpresaEstado(establecimiento.tenantId().value(), establecimiento.empresaId().value())
                    .admiteAltasDeHijos()) {
                return SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA;
            }
            if (establecimientoRepository.existsByTenantIdAndCodigo(tenantId.get(), establecimiento.codigo())) {
```

3. En `save(Almacen)`, reemplazar:

```java
        var existing = almacenRepository.findByUuidPublico(almacen.id().value());
        if (existing.isEmpty() && almacenRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                tenantId.get(), parent.get().establecimientoId(), almacen.codigo())) {
            return SaveAlmacenOutcome.DUPLICATE_CODIGO;
        }
```

por:

```java
        var existing = almacenRepository.findByUuidPublico(almacen.id().value());
        if (existing.isEmpty()) {
            if (!findEstablecimientoEstado(almacen.tenantId().value(), almacen.establecimientoId().value())
                    .admiteAltasDeHijos()) {
                return SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO;
            }
            if (almacenRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                    tenantId.get(), parent.get().establecimientoId(), almacen.codigo())) {
                return SaveAlmacenOutcome.DUPLICATE_CODIGO;
            }
        }
```

4. En `save(TerminalPos)`, reemplazar:

```java
        var existing = terminalRepository.findByUuidPublico(terminal.id().value());
        if (existing.isEmpty() && terminalRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                tenantId.get(), parent.get().establecimientoId(), terminal.codigo())) {
            return SaveTerminalOutcome.DUPLICATE_CODIGO;
        }
```

por:

```java
        var existing = terminalRepository.findByUuidPublico(terminal.id().value());
        if (existing.isEmpty()) {
            if (!findEstablecimientoEstado(terminal.tenantId().value(), terminal.establecimientoId().value())
                    .admiteAltasDeHijos()) {
                return SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO;
            }
            if (terminalRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                    tenantId.get(), parent.get().establecimientoId(), terminal.codigo())) {
                return SaveTerminalOutcome.DUPLICATE_CODIGO;
            }
        }
```

5. Añadir estos dos métodos privados justo antes de `private static OffsetDateTime toOffsetDateTime`:

```java
    private EstadoEmpresaOperadora findEmpresaEstado(UUID tenantUuid, UUID empresaUuid) {
        return jdbcClient.sql("""
                        SELECT e.estado
                          FROM sch_organizacion.empresa_operadora e
                          JOIN sch_admin.tenant t ON t.id = e.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :empresaUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("empresaUuid", empresaUuid)
                .query(String.class)
                .optional()
                .map(EstadoEmpresaOperadora::valueOf)
                .orElseThrow();
    }

    private EstadoEstablecimiento findEstablecimientoEstado(UUID tenantUuid, UUID establecimientoUuid) {
        return jdbcClient.sql("""
                        SELECT s.estado_operativo
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND s.uuid_publico = :establecimientoUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("establecimientoUuid", establecimientoUuid)
                .query(String.class)
                .optional()
                .map(EstadoEstablecimiento::valueOf)
                .orElseThrow();
    }
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*OrganizacionJpaWriteAdapterTest"`
Expected: PASS.

- [ ] **Step 6: Escribir los tests de los handlers (fallan)**

`ORGT/application/usecase/command/CrearEstablecimientoHandlerTest.java`, añadir antes de la llave final:

```java
    @Test
    void returnsConflictWhenEmpresaNoOperativa() {
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_EMPRESA_NO_OPERATIVA");
            assertThat(error.message()).isEqualTo(
                    "La empresa no está operativa (suspendida o bloqueada); no admite establecimientos nuevos.");
            return null;
        });
    }
```

`ORGT/application/usecase/command/CrearAlmacenHandlerTest.java`, añadir antes de la llave final:

```java
    @Test
    void returnsConflictWhenEstablecimientoNoOperativo() {
        when(writePort.save(any(com.softprimesolutions.organizacion.domain.model.Almacen.class)))
                .thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_NO_OPERATIVO");
            assertThat(error.message()).isEqualTo(
                    "El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos.");
            return null;
        });
    }
```

`ORGT/application/usecase/command/CrearTerminalPosHandlerTest.java`, añadir antes de la llave final:

```java
    @Test
    void returnsConflictWhenEstablecimientoNoOperativo() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_NO_OPERATIVO");
            assertThat(error.message()).isEqualTo(
                    "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos.");
            return null;
        });
    }
```

- [ ] **Step 7: Implementar el mapeo en los handlers**

`ORG/application/usecase/command/CrearEstablecimientoHandler.java`, en `persist`, añadir justo después del bloque de `EMPRESA_NOT_FOUND`:

```java
        if (outcome == OrganizacionWritePort.SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_OPERATIVA",
                    "La empresa no está operativa (suspendida o bloqueada); no admite establecimientos nuevos.",
                    ErrorCategory.CONFLICT));
        }
```

`ORG/application/usecase/command/CrearAlmacenHandler.java`, en `persist`, añadir justo después del bloque de `ESTABLECIMIENTO_NOT_FOUND`:

```java
        if (outcome == OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_OPERATIVO",
                    "El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos.",
                    ErrorCategory.CONFLICT));
        }
```

`ORG/application/usecase/command/CrearTerminalPosHandler.java`, en `persist`, añadir justo después del bloque de `ESTABLECIMIENTO_NOT_FOUND`:

```java
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_OPERATIVO",
                    "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos.",
                    ErrorCategory.CONFLICT));
        }
```

- [ ] **Step 8: Ejecutar el módulo con el gate de cobertura**

Run: `.\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/organizacion
git commit -m "feat(organizacion): bloquear altas bajo un padre no operativo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Series únicas por empresa

**Files:**
- Modify: `ORG/application/port/out/OrganizacionWritePort.java`
- Modify: `ORG/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java`
- Modify (test): `ORGT/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java`
- Create: `ORG/application/usecase/command/TerminalSaveErrors.java`
- Create (test): `ORGT/application/usecase/command/TerminalSaveErrorsTest.java`
- Modify: `ORG/application/usecase/command/CrearTerminalPosHandler.java`, `ActualizarTerminalPosHandler.java`
- Modify (test): `ORGT/application/usecase/command/CrearTerminalPosHandlerTest.java`, `ActualizarTerminalPosHandlerTest.java`
- Create: `MIG/V026__terminal_pos_series_unicas_por_empresa.sql`

**Interfaces:**
- Consumes: de la Task 3, `SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO` y el patrón `existing.isEmpty()` del adaptador.
- Produces:
  - `SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA` y `DUPLICATE_SERIE_FACTURA`.
  - `TerminalSaveErrors.seriesConflict(SaveTerminalOutcome outcome, TerminalPos terminal): Optional<ApplicationError>` (paquete privado).
  - Errores de aplicación `ORG_TERMINAL_SERIE_BOLETA_DUPLICADA` y `ORG_TERMINAL_SERIE_FACTURA_DUPLICADA` con categoría `CONFLICT`.

- [ ] **Step 1: Ampliar el puerto**

En `ORG/application/port/out/OrganizacionWritePort.java`, reemplazar `SaveTerminalOutcome` por:

```java
    enum SaveTerminalOutcome {
        CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, ESTABLECIMIENTO_NO_OPERATIVO, DUPLICATE_CODIGO,
        DUPLICATE_SERIE_BOLETA, DUPLICATE_SERIE_FACTURA
    }
```

- [ ] **Step 2: Escribir los tests del adaptador (fallan)**

En `ORGT/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapterTest.java`:

1. Junto a los otros campos `resolved...`, añadir:

```java
    private boolean serieBoletaEnUso = false;
    private boolean serieFacturaEnUso = false;
```

2. En `lookup`, añadir estas dos líneas al inicio (antes de las de estado añadidas en la Task 3):

```java
        if (sql.contains("serie_boleta_defecto = :serie")) return Optional.of(serieBoletaEnUso);
        if (sql.contains("serie_factura_defecto = :serie")) return Optional.of(serieFacturaEnUso);
```

3. Añadir el helper de terminal sin series, junto a `terminal(UUID id)`:

```java
    private TerminalPos terminalSinSeries(UUID id) {
        return TerminalPos.create(
                        new TerminalPosId(id), new TenantId(TENANT_UUID), new EstablecimientoId(ESTABLECIMIENTO_UUID),
                        "POS002", "Caja 2", null, null, "SN-002", "host-2", "192.168.0.11", "IMP02", false, NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }
```

4. Añadir estos tests antes de `tenantExistsReflectsLookupResult`:

```java
    @Test
    void returnsDuplicateSerieBoletaWhenAnotherTerminalOfTheEmpresaUsesIt() {
        serieBoletaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateSerieFacturaWhenAnotherTerminalOfTheEmpresaUsesIt() {
        serieFacturaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateSerieWhenUpdatingATerminalToAUsedSerie() {
        serieBoletaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value()))
                .thenReturn(Optional.of(mock(TerminalPosJpaEntity.class)));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
    }

    @Test
    void skipsTheSerieCheckWhenTheTerminalHasNoSeries() {
        serieBoletaEnUso = true;
        serieFacturaEnUso = true;
        var terminal = terminalSinSeries(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.CREATED);
    }

    @Test
    void returnsDuplicateSerieBoletaWhenInsertViolatesTheBoletaIndex() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.saveAndFlush(any(TerminalPosJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("violates unique constraint uk_terminal_pos_serie_boleta"));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
    }

    @Test
    void returnsDuplicateSerieFacturaWhenInsertViolatesTheFacturaIndex() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.saveAndFlush(any(TerminalPosJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("violates unique constraint uk_terminal_pos_serie_factura"));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);
    }
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*OrganizacionJpaWriteAdapterTest"`
Expected: FAIL (el adaptador aún no comprueba series).

- [ ] **Step 3: Implementar las series en el adaptador**

En `ORG/infrastructure/persistence/write/adapter/OrganizacionJpaWriteAdapter.java`:

1. Añadir las constantes al inicio de la clase, debajo de `public class OrganizacionJpaWriteAdapter implements OrganizacionWritePort {`:

```java
    private static final String SERIE_BOLETA_EN_USO = """
            SELECT EXISTS (
                SELECT 1 FROM sch_organizacion.terminal_pos
                 WHERE tenant_id = :tenantId AND empresa_id = :empresaId AND es_activo = '1'
                   AND serie_boleta_defecto = :serie AND uuid_publico <> :terminalId)
            """;

    private static final String SERIE_FACTURA_EN_USO = """
            SELECT EXISTS (
                SELECT 1 FROM sch_organizacion.terminal_pos
                 WHERE tenant_id = :tenantId AND empresa_id = :empresaId AND es_activo = '1'
                   AND serie_factura_defecto = :serie AND uuid_publico <> :terminalId)
            """;
```

2. En `save(TerminalPos)`, inmediatamente después del bloque `if (existing.isEmpty()) { ... }` de la Task 3 y antes de `try {`, añadir:

```java
        if (serieEnUso(SERIE_BOLETA_EN_USO, tenantId.get(), parent.get().empresaId(),
                terminal.serieBoletaDefecto(), terminal.id().value())) {
            return SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA;
        }
        if (serieEnUso(SERIE_FACTURA_EN_USO, tenantId.get(), parent.get().empresaId(),
                terminal.serieFacturaDefecto(), terminal.id().value())) {
            return SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA;
        }
```

3. En ese mismo método, reemplazar el `catch` final:

```java
        } catch (DataIntegrityViolationException exception) {
            return SaveTerminalOutcome.DUPLICATE_CODIGO;
        }
```

por:

```java
        } catch (DataIntegrityViolationException exception) {
            return terminalViolation(exception);
        }
```

4. Añadir estos dos métodos privados junto a los demás helpers:

```java
    private boolean serieEnUso(String sql, Long tenantId, Long empresaId, String serie, UUID terminalId) {
        if (serie == null) return false;
        return jdbcClient.sql(sql)
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .param("serie", serie)
                .param("terminalId", terminalId)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    private static SaveTerminalOutcome terminalViolation(DataIntegrityViolationException exception) {
        var message = String.valueOf(exception.getMessage());
        if (message.contains("uk_terminal_pos_serie_boleta")) return SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA;
        if (message.contains("uk_terminal_pos_serie_factura")) return SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA;
        return SaveTerminalOutcome.DUPLICATE_CODIGO;
    }
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*OrganizacionJpaWriteAdapterTest"`
Expected: PASS.

- [ ] **Step 4: Crear el traductor de errores de series (test primero)**

`ORGT/application/usecase/command/TerminalSaveErrorsTest.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveTerminalOutcome;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalSaveErrorsTest {

    private static final TerminalPos TERMINAL = TerminalPos.create(
                    new TerminalPosId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                    new EstablecimientoId(UUID.randomUUID()), "POS-01", "Caja 1", "B001", "F002",
                    "SN-0001", "host-01", "192.168.0.10", "PRN-01", true, Instant.parse("2026-09-27T00:00:00Z"))
            .fold(terminal -> terminal, error -> { throw new AssertionError(error.message()); });

    @Test
    void translatesADuplicateBoletaSerie() {
        var error = TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA, TERMINAL);

        assertThat(error).isPresent();
        assertThat(error.get().code()).isEqualTo("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA");
        assertThat(error.get().message()).isEqualTo("La serie B001 ya está asignada a otra caja de esta empresa.");
        assertThat(error.get().category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void translatesADuplicateFacturaSerie() {
        var error = TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA, TERMINAL);

        assertThat(error).isPresent();
        assertThat(error.get().code()).isEqualTo("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA");
        assertThat(error.get().message()).isEqualTo("La serie F002 ya está asignada a otra caja de esta empresa.");
        assertThat(error.get().category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void ignoresOutcomesThatAreNotSerieConflicts() {
        assertThat(TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.CREATED, TERMINAL)).isEmpty();
    }
}
```

`ORG/application/usecase/command/TerminalSaveErrors.java`:

```java
package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveTerminalOutcome;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import java.util.Optional;

final class TerminalSaveErrors {

    private TerminalSaveErrors() {
    }

    static Optional<ApplicationError> seriesConflict(SaveTerminalOutcome outcome, TerminalPos terminal) {
        return switch (outcome) {
            case DUPLICATE_SERIE_BOLETA -> Optional.of(
                    serieDuplicada("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA", terminal.serieBoletaDefecto()));
            case DUPLICATE_SERIE_FACTURA -> Optional.of(
                    serieDuplicada("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA", terminal.serieFacturaDefecto()));
            default -> Optional.empty();
        };
    }

    private static ApplicationError serieDuplicada(String code, String serie) {
        return new StandardApplicationError(
                code, "La serie " + serie + " ya está asignada a otra caja de esta empresa.",
                ErrorCategory.CONFLICT);
    }
}
```

Run: `.\gradlew.bat :modules:organizacion:test --tests "*TerminalSaveErrorsTest"`
Expected: PASS.

- [ ] **Step 5: Escribir los tests de handlers (fallan)**

`ORGT/application/usecase/command/CrearTerminalPosHandlerTest.java`, añadir antes de la llave final:

```java
    @Test
    void returnsConflictWhenSerieBoletaIsAlreadyAssignedInTheEmpresa() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA");
            assertThat(error.message()).isEqualTo("La serie B001 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }

    @Test
    void returnsConflictWhenSerieFacturaIsAlreadyAssignedInTheEmpresa() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA");
            assertThat(error.message()).isEqualTo("La serie F001 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }
```

`ORGT/application/usecase/command/ActualizarTerminalPosHandlerTest.java`, añadir antes de la llave final:

```java
    @Test
    void returnsConflictWhenTheNewSerieBoletaBelongsToAnotherTerminal() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);

        var result = handler.execute(validCommand("ACTIVO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.message()).isEqualTo("La serie B002 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }

    @Test
    void returnsConflictWhenTheNewSerieFacturaBelongsToAnotherTerminal() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);

        var result = handler.execute(validCommand("ACTIVO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.message()).isEqualTo("La serie F002 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }
```

- [ ] **Step 6: Implementar el mapeo en los handlers**

`ORG/application/usecase/command/CrearTerminalPosHandler.java`, en `persist`, reemplazar la última línea:

```java
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
```

por:

```java
        var seriesConflict = TerminalSaveErrors.seriesConflict(outcome, terminal);
        if (seriesConflict.isPresent()) return Result.failure(seriesConflict.get());
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
```

`ORG/application/usecase/command/ActualizarTerminalPosHandler.java`, reemplazar el método `persist`:

```java
    private Result<TerminalPosResult, ApplicationError> persist(TerminalPos terminal) {
        writePort.save(terminal);
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
    }
```

por:

```java
    private Result<TerminalPosResult, ApplicationError> persist(TerminalPos terminal) {
        var outcome = writePort.save(terminal);
        var seriesConflict = TerminalSaveErrors.seriesConflict(outcome, terminal);
        if (seriesConflict.isPresent()) return Result.failure(seriesConflict.get());
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
    }
```

Nota: en el test existente `changesEstadoWhenCommandEstadoDiffersFromCurrent` y `updatesWhenExists`, `writePort.save` ya devuelve `UPDATED`; si algún test de este handler deja el mock sin respuesta (devolvería `null` y el `switch` fallaría), añadir `when(writePort.save(any(TerminalPos.class))).thenReturn(OrganizacionWritePort.SaveTerminalOutcome.UPDATED);` a ese test.

- [ ] **Step 7: Crear la migración `V026`**

`MIG/V026__terminal_pos_series_unicas_por_empresa.sql`:

```sql
CREATE UNIQUE INDEX uk_terminal_pos_serie_boleta
    ON sch_organizacion.terminal_pos (tenant_id, empresa_id, serie_boleta_defecto)
    WHERE es_activo = '1' AND serie_boleta_defecto IS NOT NULL;

CREATE UNIQUE INDEX uk_terminal_pos_serie_factura
    ON sch_organizacion.terminal_pos (tenant_id, empresa_id, serie_factura_defecto)
    WHERE es_activo = '1' AND serie_factura_defecto IS NOT NULL;
```

- [ ] **Step 8: Ejecutar el módulo con el gate de cobertura**

Run: `.\gradlew.bat :modules:organizacion:check`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/organizacion service-botica/bootstrap-app/src/main/resources/db/migration/V026__terminal_pos_series_unicas_por_empresa.sql
git commit -m "feat(organizacion): exigir series de comprobantes unicas por empresa

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Pruebas de integración HTTP y verificación completa

**Files:**
- Modify (test): `IT` (`OrganizacionApiIntegrationTest.java`)
- Modify: `docs/superpowers/specs/2026-09-30-organizacion-reglas-y-validaciones-design.md`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: todo lo producido por las Tasks 1 a 4. RUC válidos de la Task 1. La migración `V026` corre dentro del contenedor PostgreSQL de Testcontainers.
- Produces: cobertura HTTP de cada regla nueva y `gradlew check` verde.

- [ ] **Step 1: Añadir los helpers de la prueba de integración**

En `IT`, añadir estos métodos privados junto a `createTerminal` (antes de `private UUID created(...)`):

```java
    private org.springframework.test.web.servlet.ResultActions postTerminal(
            UUID establecimientoId, String codigo, String serieBoleta, String serieFactura) throws Exception {
        return mockMvc.perform(post(BASE + "/terminales-pos").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"tenantId":"%s","establecimientoId":"%s","codigo":"%s","nombre":"Caja",
                         "serieBoletaDefecto":"%s","serieFacturaDefecto":"%s"}
                        """.formatted(TENANT_ID, establecimientoId, codigo, serieBoleta, serieFactura)));
    }

    private org.springframework.test.web.servlet.ResultActions postAlmacen(
            UUID establecimientoId, String codigo, String tipo, boolean controlTemperatura, String min, String max)
            throws Exception {
        return mockMvc.perform(post(BASE + "/almacenes").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"tenantId":"%s","establecimientoId":"%s","codigo":"%s","nombre":"Almacen",
                         "tipo":"%s","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                         "permiteDespacho":true,"controlTemperatura":%s,"temperaturaMinC":%s,
                         "temperaturaMaxC":%s}
                        """.formatted(TENANT_ID, establecimientoId, codigo, tipo, controlTemperatura, min, max)));
    }
```

- [ ] **Step 2: Añadir los tests de integración**

En `IT`, añadir antes de `private org.springframework.test.web.servlet.ResultActions patchEstado(...)`:

```java
    @Test
    void rejectsAnEmpresaWhoseRucHasAnInvalidCheckDigit() throws Exception {
        mockMvc.perform(post(BASE + "/empresas").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(empresaJson("20123456789")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El RUC no es válido: el dígito verificador no coincide."));
    }

    @Test
    void rejectsRepeatedSeriesWithinAnEmpresaButAllowsThemInAnotherOne() throws Exception {
        var empresaA = createEmpresa("20123456786");
        var sedeUno = createEstablecimiento(empresaA, "EST001", null);
        var sedeDos = createEstablecimiento(empresaA, "EST002", null);
        var empresaB = createEmpresa("20123456794");
        var sedeTres = createEstablecimiento(empresaB, "EST003", null);

        postTerminal(sedeUno, "POS001", "B001", "F001").andExpect(status().isCreated());

        postTerminal(sedeDos, "POS002", "B001", "F002").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie B001 ya está asignada a otra caja de esta empresa."));
        postTerminal(sedeDos, "POS002", "B002", "F001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie F001 ya está asignada a otra caja de esta empresa."));
        postTerminal(sedeTres, "POS001", "B001", "F001").andExpect(status().isCreated());
    }

    @Test
    void rejectsEditingATerminalToASerieUsedByAnotherTerminalOfTheEmpresa() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isCreated());
        var segundaCaja = UUID.fromString(JsonPath.read(
                postTerminal(sedeId, "POS002", "B002", "F002").andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(put(BASE + "/terminales-pos/{id}", segundaCaja).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Caja 2","serieBoletaDefecto":"B001","serieFacturaDefecto":"F002",
                                 "storeEdgeHabilitado":false,"estado":"ACTIVO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie B001 ya está asignada a otra caja de esta empresa."));
        mockMvc.perform(put(BASE + "/terminales-pos/{id}", segundaCaja).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Caja 2","serieBoletaDefecto":"B002","serieFacturaDefecto":"F002",
                                 "storeEdgeHabilitado":false,"estado":"ACTIVO"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void blocksNewEstablecimientosWhenTheEmpresaIsNotOperational() throws Exception {
        var empresaId = createEmpresa("20123456786");
        patchEstado("/empresas/{id}/estado", empresaId, "SUSPENDIDO").andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/establecimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(establecimientoJson(empresaId, "EST001", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "La empresa no está operativa (suspendida o bloqueada); no admite establecimientos nuevos."));

        patchEstado("/empresas/{id}/estado", empresaId, "ACTIVO").andExpect(status().isOk());
        createEstablecimiento(empresaId, "EST001", null);
    }

    @Test
    void blocksNewAlmacenesAndTerminalesWhenTheEstablecimientoIsClosedButAllowsThemInRemodelacion()
            throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);

        patchEstado("/establecimientos/{id}/estado", sedeId, "CLAUSURADO").andExpect(status().isOk());
        postAlmacen(sedeId, "ALM001", "GENERAL", false, "null", "null").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos."));
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos."));

        patchEstado("/establecimientos/{id}/estado", sedeId, "REMODELACION").andExpect(status().isOk());
        postAlmacen(sedeId, "ALM001", "GENERAL", false, "null", "null").andExpect(status().isCreated());
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isCreated());
    }

    @Test
    void rejectsAlmacenesWithIncoherentTemperatures() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);

        postAlmacen(sedeId, "ALM001", "REFRIGERADO", false, "2", "8").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Un almacén refrigerado debe controlar temperatura."));
        postAlmacen(sedeId, "ALM002", "GENERAL", true, "null", "8").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Indica la temperatura mínima y máxima cuando el almacén controla temperatura."));
        postAlmacen(sedeId, "ALM003", "REFRIGERADO", true, "8", "2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La temperatura mínima no puede ser mayor que la máxima."));
        postAlmacen(sedeId, "ALM004", "REFRIGERADO", true, "2", "8").andExpect(status().isCreated());
    }
```

- [ ] **Step 3: Ejecutar la prueba de integración**

Requiere Docker activo (Testcontainers).

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.organizacion.api.OrganizacionApiIntegrationTest"`
Expected: todos PASS, incluida la aplicación de `V026` sobre PostgreSQL real. Si alguna aserción de `$.detail` falla porque el cuerpo de error usa otro campo, abrir el `ProblemDetail` devuelto (`andDo(print())`) y ajustar la ruta JSON al campo que contiene el mensaje; el resto de la aserción no cambia.

- [ ] **Step 4: Alinear el spec con lo implementado**

En `docs/superpowers/specs/2026-09-30-organizacion-reglas-y-validaciones-design.md`, sección «A3. Altas según el estado del padre», reemplazar la línea:

```
- El adaptador ya carga al padre al guardar un hijo, así que aplica la regla ahí y devuelve `409`. Se añaden los resultados `EMPRESA_NO_OPERATIVA` y `ESTABLECIMIENTO_NO_OPERATIVO`.
- Mensaje: «El establecimiento está CLAUSURADO; no admite almacenes nuevos.»
```

por:

```
- El adaptador consulta el estado del padre al crear un hijo y devuelve `409`. Se añaden los resultados `EMPRESA_NO_OPERATIVA` y `ESTABLECIMIENTO_NO_OPERATIVO`.
- Mensaje: «El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos.» (sin nombrar el estado concreto).
```

- [ ] **Step 5: Actualizar `CLAUDE.md`**

En `CLAUDE.md`, sección «Estado real del proyecto», punto de `service-botica/`, en la frase de `organizacion`, después de `los permisos organizacion.* los siembra V025 y se conceden al rol ADMIN de FARMALAB.` añadir:

```
Aplica reglas de negocio propias: RUC con dígito verificador (módulo 11), series de comprobantes únicas por empresa (índices `V026`), altas de hijos bloqueadas bajo un padre `SUSPENDIDO`/`BLOQUEADO` (empresa) o `SUSPENDIDO`/`CLAUSURADO` (establecimiento) y almacenes con temperaturas coherentes; las reglas de series, estado del padre y refrigerado están marcadas POR_VALIDAR.
```

- [ ] **Step 6: Verificar el backend completo**

Run: `.\gradlew.bat check --warning-mode all`
Expected: `BUILD SUCCESSFUL` (ArchUnit, Spring Modulith, JaCoCo y todos los tests).

- [ ] **Step 7: Comprobar series duplicadas en la base local antes de desplegar**

La migración `V026` falla si la base de desarrollo ya tiene series repetidas dentro de una empresa, y una migración fallida impide arrancar el backend. Listar los duplicados (solo lectura):

```powershell
docker compose exec -T postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "SELECT empresa_id, serie_boleta_defecto, count(*) FROM sch_organizacion.terminal_pos WHERE es_activo = '1' AND serie_boleta_defecto IS NOT NULL GROUP BY 1,2 HAVING count(*) > 1; SELECT empresa_id, serie_factura_defecto, count(*) FROM sch_organizacion.terminal_pos WHERE es_activo = '1' AND serie_factura_defecto IS NOT NULL GROUP BY 1,2 HAVING count(*) > 1;"
```

Expected: sin filas. Si hay filas (hoy existe la caja de prueba `CRIT-POS2`), **detenerse y pedir confirmación explícita al usuario** antes de borrar o modificar cualquiera de esas filas; no ejecutar ningún `DELETE` sin ella.

- [ ] **Step 8: Commit**

```bash
git add service-botica/bootstrap-app/src/test CLAUDE.md docs/superpowers/specs/2026-09-30-organizacion-reglas-y-validaciones-design.md
git commit -m "test(organizacion): cubrir por HTTP las nuevas reglas de negocio

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-review

**Cobertura del spec (Plan A):**
- A1 series únicas por empresa → Task 4 (adaptador, `V026`, handlers, traductor) y Task 5 (HTTP, incluida edición).
- A2 dígito verificador del RUC → Task 1.
- A3 altas según estado del padre → Task 3 (con la tabla de estados en los enums) y Task 5.
- A4 almacén coherente → Task 2 y Task 5.
- Pruebas del plan A (unitarias, `OrganizacionApiIntegrationTest`, `gradlew check`) → Tasks 1 a 5.
- Riesgo de datos de `V026` → Task 5, Step 7.

**Tipos consistentes entre tareas:** `SaveTerminalOutcome` queda con `ESTABLECIMIENTO_NO_OPERATIVO` (Task 3) y `DUPLICATE_SERIE_BOLETA`/`DUPLICATE_SERIE_FACTURA` (Task 4); `TerminalSaveErrors.seriesConflict` solo reconoce los dos últimos; `admiteAltasDeHijos()` se llama igual en ambos enums y en el adaptador.

**Desviación del spec (documentada):** el spec decía que el mensaje nombra el estado concreto («está CLAUSURADO»). El plan usa un mensaje fijo («no está operativo (suspendido o clausurado)») porque el adaptador devuelve un resultado y no el estado. El Step 4 de la Task 5 actualiza el spec.
