# Anulación, parte 2: anulación de venta en ventas — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Anular una venta `CONFIRMADA` mientras su turno sigue abierto: la venta pasa a `ANULADA`, el pago a `REVERSADO` y el stock vuelve a los lotes originales mediante `AnulacionInventarioApi`, con permiso crítico, motivo obligatorio y auditoría.

**Architecture:** Un caso de uso `AnularVentaHandler` bloquea la venta (`FOR UPDATE`) y su turno (`FOR SHARE`) en una consulta, valida las reglas en el dominio (`AnulacionVenta`), reintegra el stock vía un puerto hacia inventario y marca la venta y el pago, todo dentro de `TransaccionPort`. Ante `INV_MODIFICACION_CONCURRENTE` reintenta la anulación completa (máx. 3), igual que la venta. La respuesta es la misma `VentaResult` con un bloque `anulacion`.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-anulacion-venta-reintegro-inventario-design.md`. **Requiere la parte 1 completa** (`docs/superpowers/plans/2026-10-03-anulacion-parte-1-reintegro-inventario.md`): la API `AnulacionInventarioApi` de `inventario`, y los módulos `ventas` (turno y venta) ya implementados.

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas (gate JaCoCo por clase en `ventas`).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- Clean Architecture: `domain` sin Spring; casos de uso devuelven `Result<T, ApplicationError>`; `Result.success(null)` no existe (lanza NPE).
- Reglas: solo se anula una venta `CONFIRMADA` cuyo turno está `ABIERTO`; motivo obligatorio de 1 a 500 caracteres (se recorta); una venta ya anulada responde 409 `VEN_VENTA_ESTADO_INVALIDO`; turno cerrado responde 409 `VEN_TURNO_NO_ABIERTO`; venta inexistente 404 `VEN_VENTA_NO_ENCONTRADA`; motivo inválido 400 `VEN_MOTIVO_INVALIDO`.
- Orden de validación en el dominio: motivo, estado de la venta, estado del turno.
- Bloqueo: la venta con `FOR UPDATE OF v` y el turno con `FOR SHARE OF tc` en la **misma** consulta (un cierre de turno concurrente espera; si el cierre gana, la anulación ve el turno cerrado). Esto serializa además dos anulaciones de la misma venta.
- Un fallo del reintegro revierte toda la anulación (la venta sigue `CONFIRMADA`, el pago `CONFIRMADO`, el stock intacto).
- `INV_MODIFICACION_CONCURRENTE` se traduce a `VEN_MODIFICACION_CONCURRENTE` y activa el reintento; el resto de errores de inventario (p. ej. `INV_ALMACEN_NO_OPERABLE`, `INV_SALIDAS_NO_ENCONTRADAS`) se propaga sin cambios.
- Permiso `ventas.ventas.anular` (crítico), concedido al rol `ADMIN` de FARMALAB. Ruta: `POST /api/v1/ventas/ventas/{ventaId}/anulacion`.
- El turno deja de contar la venta anulada sin cambiar su consulta de totales (ya suma solo ventas `CONFIRMADA` con pago `CONFIRMADO`).
- Beans de adapters cuyo nombre de clase ya exista en otro módulo deben llevar nombre explícito; los de esta parte no tienen homónimos.
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:ventas:test --tests "<clase>"`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan no compila por inferencia de javac (p. ej. `assertThat(result.fold(value -> value, error -> null))`), usar un witness de tipo explícito; si al re-stubear mocks con `when(..)` aparece NPE por un Answer del `@BeforeEach`, usar `doReturn(..).when(..)`. Anotar ambos casos en el reporte.

Rutas abreviadas: `MAIN` = `service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas`, `TEST` = `service-botica/modules/ventas/src/test/java/com/softprimesolutions/ventas`, `MIG` = `service-botica/bootstrap-app/src/main/resources/db/migration`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`.

---

### Task 1: Migración V035

**Files:**
- Create: `MIG/V035__ventas_anulacion.sql`
- Modify: `BTEST/ventas/db/VentasMigrationsIntegrationTest.java`

**Interfaces:**
- Produces: columnas `sch_venta.venta.anulada_at TIMESTAMPTZ`, `anulada_por_usuario_id BIGINT`, `motivo_anulacion VARCHAR(500)` y la restricción `ck_venta_anulacion`; permiso `ventas.ventas.anular` concedido al rol `ADMIN` de FARMALAB.

- [ ] **Step 1: Escribir el test que falla**

En `VentasMigrationsIntegrationTest`, cambiar la aserción de permisos de `seedsTheVentasPermissionsAndGrantsThemToTheFarmalabAdministrator` a seis y el conteo concedido a `6L`:

```java
        assertThat(permisos).containsExactly(
                "ventas.turnos.abrir", "ventas.turnos.cerrar", "ventas.turnos.consultar",
                "ventas.ventas.anular", "ventas.ventas.consultar", "ventas.ventas.registrar");
```
```java
        assertThat(concedidos).isEqualTo(6L);
```

Agregar un test nuevo:

```java
    @Test
    void addsTheAnulacionColumnsAndTheirConsistencyConstraint() {
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'venta'
                           AND column_name IN ('anulada_at', 'anulada_por_usuario_id', 'motivo_anulacion')
                        """).query(Long.class).single()).isEqualTo(3L);
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'ck_venta_anulacion'
                        """).query(Long.class).single()).isEqualTo(1L);
    }
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.db.VentasMigrationsIntegrationTest"`
Expected: FAIL (cinco permisos en vez de seis y columnas inexistentes).

- [ ] **Step 3: Implementar**

`MIG/V035__ventas_anulacion.sql`:

```sql
ALTER TABLE sch_venta.venta
    ADD COLUMN anulada_at TIMESTAMPTZ,
    ADD COLUMN anulada_por_usuario_id BIGINT,
    ADD COLUMN motivo_anulacion VARCHAR(500),
    ADD CONSTRAINT ck_venta_anulacion CHECK ((estado = 'ANULADA') = (anulada_at IS NOT NULL));

INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('ventas.ventas.anular', 'VENTA', 'ANULAR', 'Anular ventas',
     'Permite anular una venta confirmada mientras su turno de caja sigue abierto y reintegrar el stock.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'VENTAS'
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';

INSERT INTO sch_seguridad.rol_permiso
    (tenant_id, rol_id, permiso_id)
SELECT r.tenant_id, r.id, p.id
FROM sch_seguridad.rol r
JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'ventas.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.db.VentasMigrationsIntegrationTest" --tests "com.softprimesolutions.ventas.api.TurnoApiIntegrationTest"`
Expected: PASS (la migración aplica sobre PostgreSQL real y no rompe los tests existentes).

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app
git commit -m "feat(ventas): migracion V035 de anulacion y permiso ventas.ventas.anular

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Dominio de la anulación

**Files:**
- Modify: `MAIN/domain/exception/VentasErrorCodes.java`
- Create: `MAIN/domain/model/EstadoVenta.java`, `MAIN/domain/model/AnulacionVenta.java`
- Modify: `MAIN/application/error/VentasErrors.java` (conjunto `CONFLICTOS`)
- Test: `TEST/domain/model/AnulacionVentaTest.java`, `TEST/application/error/VentasErrorsTest.java`

**Interfaces:**
- Produces: códigos `VentasErrorCodes.{MOTIVO_INVALIDO="VEN_MOTIVO_INVALIDO", VENTA_ESTADO_INVALIDO="VEN_VENTA_ESTADO_INVALIDO", TURNO_NO_ABIERTO="VEN_TURNO_NO_ABIERTO"}`; `EstadoVenta {CONFIRMADA, ANULADA, PARCIALMENTE_DEVUELTA, DEVUELTA}`; `AnulacionVenta.validar(EstadoVenta estadoVenta, EstadoTurno estadoTurno, String motivo): Result<String, ErrorDetail>` (devuelve el motivo recortado).

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/domain/model/AnulacionVentaTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import org.junit.jupiter.api.Test;

class AnulacionVentaTest {

    private static String code(Result<String, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void acceptsAConfirmedSaleOfAnOpenTurnoAndReturnsTheTrimmedReason() {
        var motivo = AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "  Error de cobro  ")
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(motivo).isEqualTo("Error de cobro");
    }

    @Test
    void theReasonIsRequiredAndAtMostFiveHundredCharacters() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, null)))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "   ")))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "x".repeat(501))))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "x".repeat(500)).isSuccess())
                .isTrue();
    }

    @Test
    void onlyAConfirmedSaleCanBeAnnulled() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.DEVUELTA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.PARCIALMENTE_DEVUELTA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
    }

    @Test
    void theTurnoOfTheSaleMustStillBeOpen() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.CERRADO, "motivo")))
                .isEqualTo(VentasErrorCodes.TURNO_NO_ABIERTO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.EN_ARQUEO, "motivo")))
                .isEqualTo(VentasErrorCodes.TURNO_NO_ABIERTO);
    }

    @Test
    void theReasonIsValidatedBeforeTheStates() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.CERRADO, " ")))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.CERRADO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
    }
}
```

Agregar a `VentasErrorsTest`:

```java
    @Test
    void classifiesTheAnulacionStateErrorsAsConflictsAndTheReasonAsValidation() {
        var venta = VentasErrors.fromDomain(new ErrorDetail(VentasErrorCodes.VENTA_ESTADO_INVALIDO, "e", Map.of()));
        var turno = VentasErrors.fromDomain(new ErrorDetail(VentasErrorCodes.TURNO_NO_ABIERTO, "t", Map.of()));
        var motivo = VentasErrors.fromDomain(new ErrorDetail(VentasErrorCodes.MOTIVO_INVALIDO, "m", Map.of()));

        assertThat(venta.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(turno.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(motivo.category()).isEqualTo(ErrorCategory.VALIDATION);
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*AnulacionVentaTest" --tests "*VentasErrorsTest"`
Expected: FAIL de compilación (`EstadoVenta`, `AnulacionVenta` y los códigos no existen).

- [ ] **Step 3: Implementar**

`VentasErrorCodes`: agregar antes del constructor privado:

```java
    public static final String MOTIVO_INVALIDO = "VEN_MOTIVO_INVALIDO";
    public static final String VENTA_ESTADO_INVALIDO = "VEN_VENTA_ESTADO_INVALIDO";
    public static final String TURNO_NO_ABIERTO = "VEN_TURNO_NO_ABIERTO";
```

`MAIN/domain/model/EstadoVenta.java`:

```java
package com.softprimesolutions.ventas.domain.model;

public enum EstadoVenta {
    CONFIRMADA,
    ANULADA,
    PARCIALMENTE_DEVUELTA,
    DEVUELTA
}
```

`MAIN/domain/model/AnulacionVenta.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Optional;
import java.util.function.Predicate;

public final class AnulacionVenta {

    private static final int MOTIVO_MAXIMO = 500;

    private AnulacionVenta() {
    }

    public static Result<String, ErrorDetail> validar(
            EstadoVenta estadoVenta, EstadoTurno estadoTurno, String motivo) {
        var normalizado = Optional.ofNullable(motivo).map(String::trim).filter(Predicate.not(String::isEmpty));
        if (normalizado.filter(valor -> valor.length() <= MOTIVO_MAXIMO).isEmpty()) {
            return failure(VentasErrorCodes.MOTIVO_INVALIDO, "El motivo de anulacion debe tener entre 1 y 500 caracteres.");
        }
        if (estadoVenta != EstadoVenta.CONFIRMADA) {
            return failure(VentasErrorCodes.VENTA_ESTADO_INVALIDO, "Solo se puede anular una venta confirmada.");
        }
        if (estadoTurno != EstadoTurno.ABIERTO) {
            return failure(VentasErrorCodes.TURNO_NO_ABIERTO, "Solo se puede anular una venta mientras su turno esta abierto.");
        }
        return Result.success(normalizado.get());
    }
}
```

`VentasErrors`: reemplazar el conjunto:

```java
    private static final Set<String> CONFLICTOS = Set.of(
            VentasErrorCodes.TURNO_ESTADO_INVALIDO, VentasErrorCodes.VENTA_ESTADO_INVALIDO,
            VentasErrorCodes.TURNO_NO_ABIERTO);
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*AnulacionVentaTest" --tests "*VentasErrorsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): reglas de dominio de la anulacion de venta

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Aplicación de la anulación

**Files:**
- Create: `MAIN/application/dto/command/AnularVentaCommand.java`, `MAIN/application/dto/result/AnulacionResult.java`
- Modify: `MAIN/application/dto/result/VentaResult.java` (campo nuevo `anulacion`)
- Create: `MAIN/application/port/in/AnularVentaUseCase.java`
- Create: `MAIN/application/port/out/AnulacionWritePort.java`, `MAIN/application/port/out/ReintegroInventarioPort.java`
- Create: `MAIN/application/usecase/command/AnularVentaHandler.java`
- Modify (call sites de `new VentaResult(`): ver Step 3
- Test: `TEST/application/usecase/command/AnularVentaHandlerTest.java`

**Interfaces:**
- Consumes: `AnulacionVenta`, `EstadoVenta`, `EstadoTurno`, `Actor`, `LoteConsumo`, `VentasErrors`, `ConsultarVentasUseCase`, `ObtenerVentaQuery`, `TransaccionPort`, `ClockPort`.
- Produces:
  - `AnularVentaCommand(UUID tenantId, UUID actorId, UUID ventaId, String motivo)`.
  - `AnulacionResult(Instant anuladaAt, UUID anuladaPorId, String motivo)`; `VentaResult` agrega **como último campo** `AnulacionResult anulacion` (null mientras la venta no esté anulada).
  - `AnularVentaUseCase#execute(AnularVentaCommand): Result<VentaResult, ApplicationError>`.
  - `AnulacionWritePort`: `Optional<VentaParaAnular> bloquearVenta(UUID tenantId, UUID ventaId)`, `boolean marcarAnulada(UUID tenantId, UUID ventaId, Actor actor, String motivo, Instant ahora)`; `record VentaParaAnular(UUID id, UUID turnoId, EstadoVenta estado, EstadoTurno estadoTurno)`.
  - `ReintegroInventarioPort#reintegrar(UUID tenantId, UUID ventaId, UUID actorId): Result<List<LoteConsumo>, ApplicationError>`.

- [ ] **Step 1: Escribir el test que falla**

`TEST/application/usecase/command/AnularVentaHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort.VentaParaAnular;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnularVentaHandlerTest {

    private final AnulacionWritePort ventas = mock(AnulacionWritePort.class);
    private final ReintegroInventarioPort inventario = mock(ReintegroInventarioPort.class);
    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);
    private final AnularVentaHandler handler =
            new AnularVentaHandler(ventas, inventario, consultas, TRANSACCION_DIRECTA, () -> AHORA);

    @BeforeEach
    void theSaleIsConfirmedInAnOpenTurnoAndEveryCollaboratorSucceedsByDefault() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO)));
        when(inventario.reintegrar(TENANT, VENTA, ACTOR_ID)).thenReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))));
        when(ventas.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).thenReturn(true);
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(ok(ventaResult()));
    }

    private static AnularVentaCommand command(String motivo) {
        return new AnularVentaCommand(TENANT, ACTOR_ID, VENTA, motivo);
    }

    private static ApplicationError error(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void reintegratesTheStockThenMarksTheSaleAndReturnsTheStoredOne() {
        var result = handler.execute(command("  Error de cobro  "));

        assertThat(result.<VentaResult>fold(value -> value, error -> null)).isEqualTo(ventaResult());
        var orden = inOrder(ventas, inventario);
        orden.verify(ventas).bloquearVenta(TENANT, VENTA);
        orden.verify(inventario).reintegrar(TENANT, VENTA, ACTOR_ID);
        orden.verify(ventas).marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);
    }

    @Test
    void anUnknownSaleIsNotFound() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void anAlreadyAnnulledSaleIsAConflictAndTheStockIsNotTouched() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.ANULADA, EstadoTurno.ABIERTO)));

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_VENTA_ESTADO_INVALIDO");
        verify(inventario, never()).reintegrar(any(), any(), any());
        verify(ventas, never()).marcarAnulada(any(), any(), any(), any(), any());
    }

    @Test
    void aSaleOfAClosedTurnoCannotBeAnnulled() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.CONFIRMADA, EstadoTurno.CERRADO)));

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void aMissingOrTooLongReasonIsRejectedBeforeTouchingTheStock() {
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MOTIVO_INVALIDO");
        assertThat(error(handler.execute(command("x".repeat(501)))).code()).isEqualTo("VEN_MOTIVO_INVALIDO");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void anInventoryFailurePassesThroughAndTheSaleIsNotMarked() {
        when(inventario.reintegrar(TENANT, VENTA, ACTOR_ID)).thenReturn(Result.failure(CONFLICTO));

        assertThat(error(handler.execute(command("Error de cobro")))).isSameAs(CONFLICTO);
        verify(ventas, never()).marcarAnulada(any(), any(), any(), any(), any());
    }

    @Test
    void aLostUpdateIsRetriedThreeTimesAndReportedAsAConcurrentModification() {
        when(ventas.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).thenReturn(false);

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo(VentasErrors.CONCURRENCIA);
        verify(ventas, times(3)).bloquearVenta(TENANT, VENTA);
    }

    @Test
    void aConcurrencyErrorFromInventoryRetriesTheWholeAnnulment() {
        doReturnConcurrenciaLuegoExito();

        var result = handler.execute(command("Error de cobro"));

        assertThat(result.<VentaResult>fold(value -> value, error -> null)).isEqualTo(ventaResult());
        verify(ventas, times(2)).bloquearVenta(TENANT, VENTA);
        verify(ventas, times(1)).marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);
    }

    private void doReturnConcurrenciaLuegoExito() {
        doReturn(Result.<List<LoteConsumo>, ApplicationError>failure(VentasErrors.modificacionConcurrente()))
                .doReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))))
                .when(inventario).reintegrar(TENANT, VENTA, ACTOR_ID);
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(null, inventario, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, null, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, null, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, consultas, TRANSACCION_DIRECTA, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:ventas:test --tests "*AnularVentaHandlerTest"`
Expected: FAIL de compilación (comando, puertos, handler y campo `anulacion` no existen).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/command/AnularVentaCommand.java`:

```java
package com.softprimesolutions.ventas.application.dto.command;

import java.util.UUID;

public record AnularVentaCommand(UUID tenantId, UUID actorId, UUID ventaId, String motivo) {
}
```

`MAIN/application/dto/result/AnulacionResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record AnulacionResult(Instant anuladaAt, UUID anuladaPorId, String motivo) {
}
```

`MAIN/application/dto/result/VentaResult.java`: agregar al final de la lista de componentes `AnulacionResult anulacion`:

```java
        List<LineaVentaResult> lineas,
        PagoResult pago,
        AnulacionResult anulacion) {
```

Actualizar **todos** los call sites de `new VentaResult(` agregando `null` como último argumento: ejecutar `grep -rn "new VentaResult(" service-botica` y corregir cada uno (como mínimo `TEST/VentasFixtures.ventaResult()` y, en el Task 4, `VentasJdbcReadAdapter#armar`). En `VentasFixtures` agregar además un helper:

```java
    public static VentaResult ventaAnuladaResult() {
        var base = ventaResult();
        return new VentaResult(
                base.id(), base.numeroOperacion(), base.terminalId(), base.turnoId(), base.establecimientoId(),
                base.vendedorId(), base.fechaVenta(), base.moneda(), base.subtotal(), base.descuentoTotal(),
                base.impuestoTotal(), base.total(), "ANULADA", base.lineas(), base.pago(),
                new AnulacionResult(AHORA, ACTOR_ID, "Error de cobro"));
    }
```
(importar `AnulacionResult`).

`MAIN/application/port/in/AnularVentaUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;

@FunctionalInterface
public interface AnularVentaUseCase {
    Result<VentaResult, ApplicationError> execute(AnularVentaCommand command);
}
```

`MAIN/application/port/out/AnulacionWritePort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AnulacionWritePort {

    Optional<VentaParaAnular> bloquearVenta(UUID tenantId, UUID ventaId);

    boolean marcarAnulada(UUID tenantId, UUID ventaId, Actor actor, String motivo, Instant ahora);

    record VentaParaAnular(UUID id, UUID turnoId, EstadoVenta estado, EstadoTurno estadoTurno) {
    }
}
```

`MAIN/application/port/out/ReintegroInventarioPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface ReintegroInventarioPort {

    Result<List<LoteConsumo>, ApplicationError> reintegrar(UUID tenantId, UUID ventaId, UUID actorId);
}
```

`MAIN/application/usecase/command/AnularVentaHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort.VentaParaAnular;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.domain.model.AnulacionVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;
import java.util.UUID;

public final class AnularVentaHandler implements AnularVentaUseCase {

    private static final int MAX_INTENTOS = 3;

    private final AnulacionWritePort ventas;
    private final ReintegroInventarioPort inventario;
    private final ConsultarVentasUseCase consultas;
    private final TransaccionPort transaccion;
    private final ClockPort clock;

    public AnularVentaHandler(
            AnulacionWritePort ventas, ReintegroInventarioPort inventario, ConsultarVentasUseCase consultas,
            TransaccionPort transaccion, ClockPort clock) {
        this.ventas = Objects.requireNonNull(ventas, "ventas es obligatorio");
        this.inventario = Objects.requireNonNull(inventario, "inventario es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(AnularVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var resultado = intentar(command);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(command);
        }
        return resultado;
    }

    private Result<VentaResult, ApplicationError> intentar(AnularVentaCommand command) {
        return transaccion.ejecutar(() -> anular(command))
                .flatMap(ventaId -> consultas.obtener(new ObtenerVentaQuery(command.tenantId(), ventaId)));
    }

    private Result<UUID, ApplicationError> anular(AnularVentaCommand command) {
        var venta = ventas.bloquearVenta(command.tenantId(), command.ventaId());
        if (venta.isEmpty()) return Result.failure(VentasErrors.ventaNoEncontrada());
        return VentasErrors.fromDomain(AnulacionVenta.validar(
                        venta.get().estado(), venta.get().estadoTurno(), command.motivo()))
                .flatMap(motivo -> reintegrarYMarcar(command, venta.get(), motivo));
    }

    private Result<UUID, ApplicationError> reintegrarYMarcar(
            AnularVentaCommand command, VentaParaAnular venta, String motivo) {
        return inventario.reintegrar(command.tenantId(), venta.id(), command.actorId())
                .flatMap(lotes -> marcar(command, venta, motivo));
    }

    private Result<UUID, ApplicationError> marcar(AnularVentaCommand command, VentaParaAnular venta, String motivo) {
        var marcada = ventas.marcarAnulada(
                command.tenantId(), venta.id(), new Actor(command.actorId()), motivo, clock.now());
        if (!marcada) return Result.failure(VentasErrors.modificacionConcurrente());
        return Result.success(venta.id());
    }

    private static boolean esConcurrencia(Result<VentaResult, ApplicationError> resultado) {
        return resultado.fold(venta -> false, error -> VentasErrors.CONCURRENCIA.equals(error.code()));
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:ventas:test --tests "*AnularVentaHandlerTest"`
Expected: PASS. Luego `.\gradlew.bat :modules:ventas:compileTestJava` para confirmar que todos los call sites de `new VentaResult(` quedaron actualizados (si queda alguno sin el argumento `anulacion`, corregirlo con `null`).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): caso de uso de anulacion de venta con reintegro de stock

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Infraestructura de la anulación

**Files:**
- Modify: `service-botica/modules/ventas/build.gradle` (solo si hiciera falta; la dependencia `:modules:inventario` ya existe)
- Create: `MAIN/infrastructure/persistence/write/adapter/AnulacionJdbcWriteAdapter.java`
- Create: `MAIN/infrastructure/client/InventarioReintegroAdapter.java`
- Modify: `MAIN/infrastructure/persistence/read/adapter/VentasJdbcReadAdapter.java` (cabecera con datos de anulación)
- Modify: `MAIN/infrastructure/configuration/VentasModuleConfiguration.java`
- Test: `TEST/infrastructure/persistence/write/adapter/AnulacionJdbcWriteAdapterTest.java`, `TEST/infrastructure/client/InventarioReintegroAdapterTest.java`, `TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java`, `TEST/infrastructure/configuration/VentasModuleConfigurationTest.java`, `TEST/infrastructure/persistence/Rows.java`

**Interfaces:**
- Consumes: puertos de la Task 3; `AnulacionInventarioApi`, `ReintegroVentaSolicitud`, `ReintegroVentaRegistrado`, `MovimientoReintegrado` de `inventario::api`; `JdbcEscrituras.exigirUnaFila`.
- Produces: `AnulacionJdbcWriteAdapter` (`@Repository`), `InventarioReintegroAdapter` (`@Component`); `VentasJdbcReadAdapter#findVenta` devuelve `VentaResult.anulacion` (null si la venta no está anulada); bean `anularVentaUseCase`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/infrastructure/persistence/write/adapter/AnulacionJdbcWriteAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class AnulacionJdbcWriteAdapterTest {

    private static final String BLOQUEAR = "FOR UPDATE OF v FOR SHARE OF tc";
    private static final String MARCAR_VENTA = "UPDATE sch_venta.venta";
    private static final String REVERSAR_PAGO = "UPDATE sch_venta.pago_venta";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final AnulacionJdbcWriteAdapter adapter = new AnulacionJdbcWriteAdapter(jdbc.client());

    @Test
    void locksTheSaleForUpdateAndItsTurnoForShareInOneQuery() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("turno_uuid", TURNO);
        row.put("venta_estado", "CONFIRMADA");
        row.put("turno_estado", "ABIERTO");
        jdbc.rows(BLOQUEAR, row);

        var venta = adapter.bloquearVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.estadoTurno()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(jdbc.statementContaining(BLOQUEAR).params())
                .containsEntry("tenantId", TENANT).containsEntry("ventaId", VENTA);
    }

    @Test
    void aMissingSaleIsEmpty() {
        assertThat(adapter.bloquearVenta(TENANT, VENTA)).isEmpty();
    }

    @Test
    void marksTheSaleAnnulledAndReversesItsConfirmedPayment() {
        var marcada = adapter.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);

        assertThat(marcada).isTrue();
        var venta = jdbc.statementContaining(MARCAR_VENTA);
        assertThat(venta.sql()).contains("estado = 'ANULADA'").contains("AND estado = 'CONFIRMADA'");
        assertThat(venta.params())
                .containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT)
                .containsEntry("actorId", ACTOR_ID).containsEntry("actor", ACTOR.codigo())
                .containsEntry("motivo", "Error de cobro").containsEntry("fecha", MOMENTO);
        var pago = jdbc.statementContaining(REVERSAR_PAGO);
        assertThat(pago.sql()).contains("estado = 'REVERSADO'").contains("AND estado = 'CONFIRMADO'");
        assertThat(pago.params()).containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT);
    }

    @Test
    void whenTheSaleIsNoLongerConfirmedItReportsFalseWithoutTouchingThePayment() {
        jdbc.updates(MARCAR_VENTA, 0);

        assertThat(adapter.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).isFalse();
        assertThat(jdbc.statements()).noneMatch(statement -> statement.sql().contains(REVERSAR_PAGO));
    }
}
```

`TEST/infrastructure/client/InventarioReintegroAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.client;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.MovimientoReintegrado;
import com.softprimesolutions.inventario.api.ReintegroVentaRegistrado;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventarioReintegroAdapterTest {

    @Test
    void translatesTheRequestAndMapsTheReintegratedLotes() {
        var recibida = new AtomicReference<ReintegroVentaSolicitud>();
        var adapter = new InventarioReintegroAdapter(solicitud -> {
            recibida.set(solicitud);
            return Result.success(new ReintegroVentaRegistrado(List.of(
                    new MovimientoReintegrado(UUID.randomUUID(), LOTE, dec("3"), dec("10")))));
        });

        var lotes = adapter.reintegrar(TENANT, VENTA, ACTOR_ID)
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(lotes).containsExactly(new LoteConsumo(LOTE, dec("3")));
        assertThat(recibida.get().tenantId()).isEqualTo(TENANT);
        assertThat(recibida.get().ventaId()).isEqualTo(VENTA);
        assertThat(recibida.get().actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void anInventoryConcurrencyErrorBecomesTheSalesConcurrencyError() {
        var concurrencia = new StandardApplicationError(
                AnulacionInventarioApi.CODIGO_CONCURRENCIA, "Concurrencia.", ErrorCategory.CONFLICT);
        var adapter = new InventarioReintegroAdapter(solicitud -> Result.failure(concurrencia));

        ApplicationError error = adapter.reintegrar(TENANT, VENTA, ACTOR_ID).fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(VentasErrors.CONCURRENCIA);
    }

    @Test
    void anyOtherInventoryErrorPassesThroughUntouched() {
        var adapter = new InventarioReintegroAdapter(solicitud -> Result.failure(CONFLICTO));

        ApplicationError error = adapter.reintegrar(TENANT, VENTA, ACTOR_ID).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }
}
```

Agregar a `TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java` (importar `Instant`, `ACTOR_ID` ya importado):

```java
    @Test
    void anAnnulledSaleExposesWhoWhenAndWhyItWasAnnulled() {
        var cabecera = Rows.ventaCabecera();
        cabecera.put("estado", "ANULADA");
        cabecera.put("anulada_at", Rows.MOMENTO);
        cabecera.put("anulada_por_uuid", ACTOR_ID);
        cabecera.put("motivo_anulacion", "Error de cobro");
        jdbc.rows(CABECERA, cabecera);
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        var venta = adapter.findVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.estado()).isEqualTo("ANULADA");
        assertThat(venta.anulacion().anuladaAt()).isEqualTo(Rows.MOMENTO.toInstant());
        assertThat(venta.anulacion().anuladaPorId()).isEqualTo(ACTOR_ID);
        assertThat(venta.anulacion().motivo()).isEqualTo("Error de cobro");
    }

    @Test
    void aSaleThatWasNeverAnnulledHasNoAnulacionBlock() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        assertThat(adapter.findVenta(TENANT, VENTA).orElseThrow().anulacion()).isNull();
    }
```

Agregar a `VentasModuleConfigurationTest` (campos nuevos `AnulacionWritePort anulaciones = mock(AnulacionWritePort.class)` y `ReintegroInventarioPort reintegro = mock(ReintegroInventarioPort.class)`, con sus imports):

```java
    @Test
    void wiresTheAnularVentaUseCase() {
        var consultas = configuration.consultarVentasUseCase(readPort);

        assertThat(configuration.anularVentaUseCase(anulaciones, reintegro, consultas, TRANSACCION_DIRECTA, clock))
                .isNotNull();
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (adapters nuevos, `anulacion` en la lectura y bean `anularVentaUseCase` no existen).

- [ ] **Step 3: Implementar**

`MAIN/infrastructure/persistence/write/adapter/AnulacionJdbcWriteAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.infrastructure.persistence.JdbcEscrituras.exigirUnaFila;

import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AnulacionJdbcWriteAdapter implements AnulacionWritePort {

    private static final String BLOQUEAR = """
            SELECT v.uuid_publico, tc.uuid_publico AS turno_uuid, v.estado AS venta_estado,
                   tc.estado AS turno_estado
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_venta.turno_caja tc ON tc.id = v.turno_caja_id AND tc.tenant_id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND v.es_activo = '1'
               FOR UPDATE OF v FOR SHARE OF tc
            """;
    private static final String MARCAR_VENTA = """
            UPDATE sch_venta.venta
               SET estado = 'ANULADA', anulada_at = :fecha,
                   anulada_por_usuario_id = (SELECT m.id FROM sch_seguridad.membership m
                                              WHERE m.uuid_publico = :actorId AND m.tenant_id = venta.tenant_id),
                   motivo_anulacion = :motivo, version_lock = version_lock + 1, updated_at = :fecha,
                   updated_by = :actor
             WHERE uuid_publico = :ventaId AND estado = 'CONFIRMADA'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;
    private static final String REVERSAR_PAGO = """
            UPDATE sch_venta.pago_venta
               SET estado = 'REVERSADO'
             WHERE estado = 'CONFIRMADO'
               AND venta_id = (SELECT v.id FROM sch_venta.venta v
                                 JOIN sch_admin.tenant t ON t.id = v.tenant_id
                                WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId)
            """;

    private final JdbcClient jdbcClient;

    public AnulacionJdbcWriteAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<VentaParaAnular> bloquearVenta(UUID tenantId, UUID ventaId) {
        return jdbcClient.sql(BLOQUEAR)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new VentaParaAnular(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "turno_uuid"),
                        EstadoVenta.valueOf(rs.getString("venta_estado")),
                        EstadoTurno.valueOf(rs.getString("turno_estado"))))
                .optional();
    }

    @Override
    public boolean marcarAnulada(UUID tenantId, UUID ventaId, Actor actor, String motivo, Instant ahora) {
        var actualizadas = jdbcClient.sql(MARCAR_VENTA)
                .param("ventaId", ventaId)
                .param("tenantId", tenantId)
                .param("actorId", actor.id())
                .param("actor", actor.codigo())
                .param("motivo", motivo)
                .param("fecha", JdbcColumns.offset(ahora))
                .update();
        if (actualizadas != 1) return false;
        jdbcClient.sql(REVERSAR_PAGO)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .update();
        return true;
    }
}
```

Quitar el import estático `exigirUnaFila` si queda sin uso (el adapter no lo necesita; no dejarlo importado).

`MAIN/infrastructure/client/InventarioReintegroAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InventarioReintegroAdapter implements ReintegroInventarioPort {

    private final AnulacionInventarioApi inventario;

    public InventarioReintegroAdapter(AnulacionInventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Result<List<LoteConsumo>, ApplicationError> reintegrar(UUID tenantId, UUID ventaId, UUID actorId) {
        return inventario.reintegrarSalidasDeVenta(new ReintegroVentaSolicitud(tenantId, ventaId, actorId))
                .fold(
                        registrado -> Result.<List<LoteConsumo>, ApplicationError>success(registrado.movimientos()
                                .stream()
                                .map(movimiento -> new LoteConsumo(movimiento.loteId(), movimiento.cantidad()))
                                .toList()),
                        error -> Result.failure(AnulacionInventarioApi.CODIGO_CONCURRENCIA.equals(error.code())
                                ? VentasErrors.modificacionConcurrente()
                                : error));
    }
}
```

`VentasJdbcReadAdapter`: (a) en la constante `CABECERA`, agregar columnas y join:

```java
            SELECT v.uuid_publico, v.numero_operacion, tp.uuid_publico AS terminal_uuid,
                   tc.uuid_publico AS turno_uuid, es.uuid_publico AS establecimiento_uuid,
                   m.uuid_publico AS vendedor_uuid, v.fecha_venta, v.moneda, v.subtotal, v.descuento_total,
                   v.impuesto_total, v.total, v.estado, v.anulada_at, ma.uuid_publico AS anulada_por_uuid,
                   v.motivo_anulacion
              FROM sch_venta.venta v
              ...
              JOIN sch_seguridad.membership m ON m.id = v.vendedor_usuario_id
              LEFT JOIN sch_seguridad.membership ma ON ma.id = v.anulada_por_usuario_id
```
(el resto de la consulta no cambia); (b) en el `record Cabecera` agregar al final `AnulacionResult anulacion`; (c) en el mapeo del `Cabecera` pasar al final

```java
                        Optional.ofNullable(JdbcColumns.instant(rs, "anulada_at")).map(anuladaAt -> new AnulacionResult(
                                anuladaAt, JdbcColumns.uuid(rs, "anulada_por_uuid"), rs.getString("motivo_anulacion")))
                                .orElse(null)
```
(importar `AnulacionResult`; `rs` es el `ResultSet` del lambda, por lo que el `Optional.map` debe capturarlo: si el compilador exige manejar `SQLException` dentro de la lambda del `map`, extraer a un método privado `static AnulacionResult anulacionDe(ResultSet rs) throws SQLException` que haga `var anuladaAt = JdbcColumns.instant(rs, "anulada_at"); return anuladaAt == null ? null : new AnulacionResult(...)`, y cubrir ambas ramas con los dos tests nuevos); (d) en `armar`, pasar `cabecera.anulacion()` como último argumento de `new VentaResult(...)`.

`VentasModuleConfiguration`: agregar imports (`AnularVentaUseCase`, `AnulacionWritePort`, `ReintegroInventarioPort`, `AnularVentaHandler`) y bean:

```java
    @Bean
    AnularVentaUseCase anularVentaUseCase(
            AnulacionWritePort anulaciones, ReintegroInventarioPort reintegro,
            ConsultarVentasUseCase consultarVentasUseCase, TransaccionPort transaccion,
            ClockPort ventasClockPort) {
        return new AnularVentaHandler(anulaciones, reintegro, consultarVentasUseCase, transaccion, ventasClockPort);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:check`
Expected: BUILD SUCCESSFUL (tests, JaCoCo 100% por clase y ArchUnit del módulo). Si JaCoCo reporta ramas sin cubrir en `anulacionDe` o en `Cabecera`, agregar el test que falta.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): adapters JDBC y de inventario para anular ventas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: API REST de la anulación

**Files:**
- Create: `MAIN/api/dto/request/AnularVentaRequest.java`, `MAIN/api/dto/response/AnulacionResponse.java`
- Modify: `MAIN/api/dto/response/VentaResponse.java` (campo `anulacion`)
- Modify: `MAIN/api/mapper/VentasApiMapper.java`
- Modify: `MAIN/api/controller/VentaController.java`
- Test: `TEST/api/mapper/VentasApiMapperTest.java`, `TEST/api/controller/VentaControllerTest.java`

**Interfaces:**
- Produces: `POST /api/v1/ventas/ventas/{ventaId}/anulacion` (200, `VentaResponse`); `VentaResponse` agrega **al final** `AnulacionResponse anulacion`; `VentasApiMapper.toCommand(UUID tenantId, UUID actorId, UUID ventaId, AnularVentaRequest)`; `VentaController(RegistrarVentaUseCase, AnularVentaUseCase, ConsultarVentasUseCase)` (el segundo parámetro es nuevo).

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `VentasApiMapperTest` (importar `AnularVentaRequest`, `ventaAnuladaResult`):

```java
    @Test
    void mapsTheAnulacionRequestToACommand() {
        var command = VentasApiMapper.toCommand(TENANT, ACTOR_ID, VENTA, new AnularVentaRequest("Error de cobro"));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.ventaId()).isEqualTo(VENTA);
        assertThat(command.motivo()).isEqualTo("Error de cobro");
    }

    @Test
    void mapsTheAnulacionBlockOfAnAnnulledSale() {
        var response = VentasApiMapper.toResponse(ventaAnuladaResult());

        assertThat(response.estado()).isEqualTo("ANULADA");
        assertThat(response.anulacion().anuladaAt()).isEqualTo(AHORA);
        assertThat(response.anulacion().anuladaPorId()).isEqualTo(ACTOR_ID);
        assertThat(response.anulacion().motivo()).isEqualTo("Error de cobro");
    }

    @Test
    void aSaleThatWasNeverAnnulledHasNoAnulacionBlockInTheResponse() {
        assertThat(VentasApiMapper.toResponse(ventaResult()).anulacion()).isNull();
    }
```

En `VentaControllerTest`: actualizar **todas** las construcciones `new VentaController(registerLambda, consultas)` a `new VentaController(registerLambda, command -> conflict(), consultas)` (el segundo argumento es el caso de uso de anulación) y agregar:

```java
    @Test
    void annulsASaleWithTheTenantAndActorOfTheTokenAndAnswersOk() {
        var received = new AtomicReference<AnularVentaCommand>();
        var controller = new VentaController(command -> conflict(), command -> {
            received.set(command);
            return ok(ventaAnuladaResult());
        }, consultas);

        var response = controller.annul(JWT, VENTA, new AnularVentaRequest("Error de cobro"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((VentaResponse) response.getBody()).estado()).isEqualTo("ANULADA");
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().ventaId()).isEqualTo(VENTA);
        assertThat(received.get().motivo()).isEqualTo("Error de cobro");
    }

    @Test
    void anAnulacionFailureBecomesAProblem() {
        var controller = new VentaController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.annul(JWT, VENTA, new AnularVentaRequest("Error de cobro")));
    }
```
(importar `AnularVentaCommand`, `AnularVentaRequest`, `ventaAnuladaResult`).

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*VentasApiMapperTest" --tests "*VentaControllerTest"`
Expected: FAIL de compilación.

- [ ] **Step 3: Implementar**

`MAIN/api/dto/request/AnularVentaRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequest(@NotBlank @Size(max = 500) String motivo) {
}
```

`MAIN/api/dto/response/AnulacionResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnulacionResponse(Instant anuladaAt, UUID anuladaPorId, String motivo) {
}
```

`VentaResponse`: agregar al final `AnulacionResponse anulacion`:

```java
        List<LineaVentaResponse> lineas,
        PagoResponse pago,
        AnulacionResponse anulacion) {
```

`VentasApiMapper`: agregar imports (`AnularVentaRequest`, `AnulacionResponse`, `AnularVentaCommand`, `AnulacionResult`, `java.util.Optional`), el método de comando:

```java
    public static AnularVentaCommand toCommand(UUID tenantId, UUID actorId, UUID ventaId, AnularVentaRequest request) {
        return new AnularVentaCommand(tenantId, actorId, ventaId, request.motivo());
    }
```

cambiar `toResponse(VentaResult)` para pasar al final `Optional.ofNullable(result.anulacion()).map(VentasApiMapper::toResponse).orElse(null)` y agregar:

```java
    private static AnulacionResponse toResponse(AnulacionResult result) {
        return new AnulacionResponse(result.anuladaAt(), result.anuladaPorId(), result.motivo());
    }
```

`VentaController`: cambiar el constructor y agregar el endpoint (importar `AnularVentaUseCase` y `AnularVentaRequest`):

```java
    private final RegistrarVentaUseCase registerVenta;
    private final AnularVentaUseCase annulVenta;
    private final ConsultarVentasUseCase queryVentas;

    public VentaController(
            RegistrarVentaUseCase registerVenta, AnularVentaUseCase annulVenta, ConsultarVentasUseCase queryVentas) {
        this.registerVenta = registerVenta;
        this.annulVenta = annulVenta;
        this.queryVentas = queryVentas;
    }
```

```java
    @PostMapping("/{ventaId}/anulacion")
    @PreAuthorize("hasAuthority('ventas.ventas.anular')")
    public ResponseEntity<?> annul(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId,
            @Valid @RequestBody AnularVentaRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), ventaId, request);
        return annulVenta.execute(command).fold(
                result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)), VentasControllerSupport::problem);
    }
```

Actualizar además cualquier otro test del módulo que construya `new VentaResponse(` (agregar `null` al final) o `new VentaController(` (tercer argumento).

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:check`
Expected: BUILD SUCCESSFUL (tests, JaCoCo 100%, ArchUnit).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): API REST de anulacion de venta

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Integración HTTP, atomicidad y concurrencia

**Files:**
- Modify: `BTEST/ventas/api/VentaApiIntegrationTest.java`
- Modify: `BTEST/ventas/api/VentaConcurrencyIntegrationTest.java`
- Modify: `CLAUDE.md` (frase de estado de `ventas`)

**Interfaces:**
- Consumes: endpoint de anulación, helpers existentes de ambos tests (`vender`, `ingresar`, `ventaJson`, `bearer`, `created`, y en el de concurrencia `vender(clave, lineas...)`, `linea`, `stock`, `count`, `salidasVentaEnKardex`, `concurrently`, `registrarVenta`, `abrirTurno`, `cerrarTurno`, `jdbcClient`, `actor`, `skuA`, `skuB`, `almacenId`, `terminalId`), bean `AnularVentaUseCase` (inyectar en el test de concurrencia).

- [ ] **Step 1: Agregar los tests HTTP**

En `VentaApiIntegrationTest` agregar (el prefijo `ANULAR = VENTAS + "/{id}/anulacion"` se define como constante):

```java
    private static final String ANULAR = VENTAS + "/{id}/anulacion";

    @Test
    void annulsASaleRestoringTheStockToTheOriginalLotesAndReversingThePayment() throws Exception {
        var antiguo = ingresar("3", "L-ANT", LocalDate.now().plusMonths(6));
        var nuevo = ingresar("10", "L-NEW", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-anular", "5", "2.50", "20").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  Error de cobro  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ventaId.toString()))
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.anulacion.motivo").value("Error de cobro"))
                .andExpect(jsonPath("$.anulacion.anuladaPorId").value(userId.toString()))
                .andExpect(jsonPath("$.anulacion.anuladaAt").exists());

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer)
                        .param("almacenId", almacenId.toString()).param("skuId", skuId.toString()))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-ANT')].cantidadFisica").value(3.0))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-NEW')].cantidadFisica").value(10.0));
        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_operacion_sunat, m.naturaleza, m.documento_tipo, m.actor
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'ANULACION_VENTA'
                        """).param("tenantId", TENANT_ID).query().listOfRows();
        assertThat(kardex).hasSize(2).allSatisfy(fila -> assertThat(fila)
                .containsEntry("tipo_operacion_sunat", "05").containsEntry("naturaleza", "E")
                .containsEntry("documento_tipo", "ANULACION_VENTA").containsEntry("actor", userId.toString()));
        var fila = jdbcClient.sql("""
                        SELECT v.estado, v.motivo_anulacion, v.updated_by,
                               (SELECT p.estado FROM sch_venta.pago_venta p WHERE p.venta_id = v.id) AS pago_estado
                          FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", ventaId).query().singleRow();
        assertThat(fila).containsEntry("estado", "ANULADA").containsEntry("pago_estado", "REVERSADO")
                .containsEntry("motivo_anulacion", "Error de cobro").containsEntry("updated_by", userId.toString());
        assertThat(antiguo).isNotEqualTo(nuevo);
    }

    @Test
    void anAnnulledSaleIsNotCountedByTheTurnoAndCannotBeAnnulledAgain() throws Exception {
        ingresar("20", "L-001", LocalDate.now().plusYears(1));
        var anulada = vender("clave-1", "2", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        vender("clave-2", "3", "5", "20").andExpect(status().isCreated());
        var ventaId = UUID.fromString(JsonPath.read(anulada, "$.id"));
        var anulacion = post(ANULAR, ventaId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Cliente se arrepintio\"}");

        mockMvc.perform(anulacion).andExpect(status().isOk());
        mockMvc.perform(anulacion).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_ESTADO_INVALIDO"));

        mockMvc.perform(get(VENTAS + "/{id}", ventaId).header("Authorization", bearer))
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":115}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentasSistema").value(15.0))
                .andExpect(jsonPath("$.totalSistema").value(115.0))
                .andExpect(jsonPath("$.diferencia").value(0.0));
    }

    @Test
    void aSaleOfAClosedTurnoCannotBeAnnulled() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "1", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":105}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Tarde\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ABIERTO"));
    }

    @Test
    void validatesTheAnulacionRequestAndItsReferencesAndEnforcesThePermission() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "1", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ANULAR, UUID.randomUUID()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_NO_ENCONTRADA"));
        mockMvc.perform(post(ANULAR, ventaId).with(csrf()).header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ANULAR, ventaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 2: Agregar los tests de concurrencia**

En `VentaConcurrencyIntegrationTest` agregar el campo `@Autowired private AnularVentaUseCase anularVenta;` (import `com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase`, `AnularVentaCommand`) y:

```java
    @Test
    void anAnulacionThatFailsInInventoryLeavesTheSaleConfirmedAndTheStockUntouched() {
        var venta = value(vender("clave-1", linea(skuA, "3")));
        assertThat(stock(skuA)).isEqualByComparingTo("7");
        jdbcClient.sql("UPDATE sch_organizacion.almacen SET es_activo = '0' WHERE uuid_publico = :almacenId")
                .param("almacenId", almacenId).update();

        var resultado = anular(venta.id(), "Error de cobro");

        assertThat(resultado.fold(valor -> "OK", ApplicationError::code)).isEqualTo("INV_ALMACEN_NO_OPERABLE");
        assertThat(jdbcClient.sql("""
                        SELECT v.estado FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", venta.id()).query(String.class).single()).isEqualTo("CONFIRMADA");
        assertThat(jdbcClient.sql("""
                        SELECT p.estado FROM sch_venta.pago_venta p
                          JOIN sch_venta.venta v ON v.id = p.venta_id WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", venta.id()).query(String.class).single()).isEqualTo("CONFIRMADO");
        assertThat(stock(skuA)).isEqualByComparingTo("7");
        assertThat(count("sch_inventario.movimiento_inventario x WHERE x.tipo_movimiento = 'ANULACION_VENTA'"))
                .isZero();
    }

    @Test
    void twoSimultaneousAnnulmentsOfTheSameSaleRestoreTheStockExactlyOnce() throws Exception {
        var venta = value(vender("clave-1", linea(skuA, "4")));

        var resultados = concurrently(List.of(
                () -> anular(venta.id(), "Primera"), () -> anular(venta.id(), "Segunda")));

        assertThat(resultados.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(resultados.stream().map(resultado -> resultado.fold(valor -> "OK", ApplicationError::code))
                .filter(codigo -> !codigo.equals("OK")).toList()).containsExactly("VEN_VENTA_ESTADO_INVALIDO");
        assertThat(stock(skuA)).isEqualByComparingTo("10");
        assertThat(salidasVentaEnKardex()).isEqualTo(1L);
    }

    @Test
    void anAnulacionRacingAgainstTheCloseOfTheTurnoNeverLeavesAnAnnulledSaleCountedInTheTotal() throws Exception {
        for (var ronda = 0; ronda < 5; ronda++) {
            var venta = value(vender("clave-ronda-" + ronda, linea(skuA, "1")));
            var turno = jdbcClient.sql("""
                            SELECT tc.uuid_publico FROM sch_venta.turno_caja tc
                              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
                             WHERE t.uuid_publico = :tenantId AND tc.estado = 'ABIERTO'
                            """).param("tenantId", TENANT_ID).query(UUID.class).single();

            var resultados = concurrently(List.of(
                    () -> anular(venta.id(), "Carrera"),
                    () -> cerrarTurno.execute(new CerrarTurnoCommand(
                                    TENANT_ID, actor, turno, new BigDecimal("1000"), null))
                            .fold(cerrado -> Result.<VentaResult, ApplicationError>success(venta), Result::failure)));

            assertThat(resultados.get(1).isSuccess()).isTrue();
            var anulada = jdbcClient.sql("SELECT v.estado FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId")
                    .param("ventaId", venta.id()).query(String.class).single().equals("ANULADA");
            var totalDelTurno = jdbcClient.sql("""
                            SELECT tc.total_ventas_sistema FROM sch_venta.turno_caja tc WHERE tc.uuid_publico = :turnoId
                            """).param("turnoId", turno).query(BigDecimal.class).single();
            assertThat(totalDelTurno).isEqualByComparingTo(anulada ? "0" : "10");
            abrirTurno.execute(new AbrirTurnoCommand(TENANT_ID, actor, terminalId, new BigDecimal("100")))
                    .fold(abierto -> abierto, error -> { throw new AssertionError(error); });
        }
    }

    @Test
    void annullingASaleWhileAnotherSellsTheSameLotesNeverDeadlocks() throws Exception {
        var venta = value(vender("clave-base", linea(skuA, "4"), linea(skuB, "4")));

        var resultados = concurrently(List.of(
                () -> anular(venta.id(), "Anular"),
                () -> vender("clave-otra", linea(skuB, "2"), linea(skuA, "2"))));

        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.isSuccess()).isTrue());
        assertThat(stock(skuA)).isEqualByComparingTo("8");
        assertThat(stock(skuB)).isEqualByComparingTo("8");
    }

    private Result<VentaResult, ApplicationError> anular(UUID ventaId, String motivo) {
        return anularVenta.execute(new AnularVentaCommand(TENANT_ID, actor, ventaId, motivo));
    }
```

- [ ] **Step 3: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.api.VentaApiIntegrationTest" --tests "com.softprimesolutions.ventas.api.VentaConcurrencyIntegrationTest"` (requiere Docker).
Expected: PASS. Si algún test de concurrencia falla por un defecto real (`INV_MODIFICACION_CONCURRENTE`, deadlock, un 500 por transacción abortada), repetir hasta 5 veces, diagnosticar con la salida real y: corregirlo en un commit aparte `fix(ventas): ...` si el arreglo es pequeño y localizado; reportar con evidencia, sin debilitar aserciones, si implica un cambio de diseño. Si `anAnulacionRacingAgainstTheCloseOfTheTurno...` falla en la rama "venta anulada pero total 10", es un defecto del bloqueo `FOR SHARE OF tc` y debe reportarse.

- [ ] **Step 4: Actualizar `CLAUDE.md` y verificar todo**

En `CLAUDE.md`, dentro de la frase de `ventas` ya existente, reemplazar `IGV en 0, sin CPE, receta, promociones, clientes, otros medios de pago ni anulación (POR_VALIDAR)` por `anulación de venta mientras el turno sigue abierto (`POST /api/v1/ventas/ventas/{id}/anulacion`, permiso crítico `ventas.ventas.anular`, reintegro de stock vía `AnulacionInventarioApi`, migración `V035`); IGV en 0, sin CPE, receta, promociones, clientes, otros medios de pago ni devolución (POR_VALIDAR)`. Commit solo de ese cambio puntual (si `CLAUDE.md` tiene otras modificaciones sin commitear, aislarlas con `git add -p`).

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (compilación, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo al 100% en archivos nuevos).

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app CLAUDE.md
git commit -m "test(ventas): anulacion de venta contra PostgreSQL, atomicidad y concurrencia

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (sección ventas):** endpoint y permiso crítico con motivo obligatorio y auditoría (Tasks 1, 3, 5); reglas de venta `CONFIRMADA` y turno abierto, 409 de estado y de turno, 404 (Tasks 2–3, 5–6); bloqueo `FOR UPDATE` de la venta y `FOR SHARE` del turno en una consulta (Task 4); transacción única con reintegro, venta `ANULADA` y pago `REVERSADO` (Tasks 3–4, 6); el turno deja de contar la venta sin tocar su consulta (Task 6, cierre de turno); reintento ante `INV_MODIFICACION_CONCURRENTE` (Tasks 3–4); respuesta con bloque `anulacion` (Tasks 3–5); migración `V035` con columnas, restricción de consistencia y permiso (Task 1); pruebas HTTP, atomicidad (fallo de inventario deja la venta `CONFIRMADA`), doble anulación simultánea, anulación contra cierre de turno y contra una venta sobre los mismos lotes (Task 6).

**Desviación respecto al spec:** el spec decía que el control de concurrencia del cierre se apoyaba en el mismo `FOR SHARE` de la venta; aquí queda explícito que el `FOR SHARE OF tc` y el `FOR UPDATE OF v` van en una sola consulta, de modo que PostgreSQL reevalúa el estado del turno si el cierre gana la carrera.

**Riesgos conocidos a vigilar al ejecutar:**
(a) `VentaResult` y `VentaResponse` cambian de firma: hay que actualizar todos los call sites (`grep "new VentaResult("`/`"new VentaResponse("`); (b) el test de carrera con el cierre usa 5 rondas y reabre turno en cada una; (c) la prueba "fallo en inventario" desactiva el almacén por SQL; (d) un reintento tras un fallo de unicidad no aplica aquí (la anulación no inserta filas únicas), por lo que no hay riesgo de transacción abortada.

**Consistencia de tipos:** `VentaParaAnular` (4 campos) y `AnulacionWritePort` coinciden entre Tasks 3 y 4; `AnularVentaCommand` (4 campos) entre Tasks 3 y 5; `AnulacionResult`/`AnulacionResponse` (3 campos) entre Tasks 3–5; `ReintegroInventarioPort#reintegrar` coincide con `InventarioReintegroAdapter`; el constructor `VentaController(register, annul, query)` coincide entre Task 5 y sus tests.
