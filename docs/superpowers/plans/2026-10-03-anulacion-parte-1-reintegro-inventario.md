# Anulación, parte 1: API de reintegro en inventario — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer `AnulacionInventarioApi` en `inventario::api`, que reintegra al stock todas las salidas `SALIDA_VENTA` de una venta, con idempotencia y atomicidad con la transacción del llamador.

**Architecture:** Un caso de uso `ReintegrarSalidasDeVentaHandler` localiza en el kardex las salidas de la venta (nueva consulta en `InventarioWritePort`) y, por cada una, registra un ingreso inverso con un tipo nuevo `ANULACION_VENTA` reutilizando `RegistrarMovimientoUseCase`. Una fachada en `api.facade` expone el contrato. La transacción propia usa el `TransaccionPort` de inventario (REQUIRED) y se une a la del llamador.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-anulacion-venta-reintegro-inventario-design.md`. Parte 2 (anulación en `ventas`): `docs/superpowers/plans/2026-10-03-anulacion-parte-2-ventas.md`.

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas (gate JaCoCo por clase en `inventario`).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- Clean Architecture: `domain` sin Spring; los casos de uso devuelven `Result<T, ApplicationError>`; `Result.success(null)` no existe (lanza NPE).
- El reintegro se calcula a partir del kardex (`movimiento_inventario` con `tipo_movimiento='SALIDA_VENTA'`, `documento_tipo='VENTA'`, `documento_uuid=ventaId`); el llamador no envía lotes ni cantidades.
- `TipoMovimiento.ANULACION_VENTA`: ingreso, naturaleza `E`, no manual, tipo de operación SUNAT `05` (POR_VALIDAR), no exige lote vendible y **se acepta aunque el lote no admita ingresos** (recall, vencido); el estado del lote sigue impidiendo venderlo.
- Idempotencia: la clave de cada reverso se deriva del id del movimiento original; reintentar devuelve los mismos reversos sin mover stock.
- Los reversos se aplican ordenados por `skuId`, luego fecha de vencimiento y luego número de lote (el mismo orden de bloqueo que las ventas).
- El llamador debe serializar los reintegros de una misma venta (`ventas` bloquea la venta `FOR UPDATE`); la API no está diseñada para dos reintegros simultáneos de la misma venta sin esa serialización.
- Si el movimiento falla por concurrencia dentro de una transacción externa, la API devuelve `INV_MODIFICACION_CONCURRENTE`; el reintento de la operación completa es del llamador.
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:inventario:test --tests "<clase>"`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan no compila por inferencia de javac (p. ej. `assertThat(result.fold(value -> value, error -> null))`), usar un witness de tipo explícito y anotarlo en el reporte.

Rutas abreviadas: `MAIN` = `service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario`, `TEST` = `service-botica/modules/inventario/src/test/java/com/softprimesolutions/inventario`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`.

---

### Task 1: Fundamentos de dominio y aplicación

**Files:**
- Modify: `MAIN/domain/model/TipoMovimiento.java`
- Modify: `MAIN/application/usecase/command/RegistrarMovimientoHandler.java` (guarda de ingreso, en `aplicar`)
- Modify: `MAIN/application/usecase/command/IdempotencyKeys.java`
- Modify: `MAIN/application/error/InventarioErrors.java`
- Test: `TEST/domain/model/EnumsAndValueObjectsTest.java`, `TEST/application/usecase/command/IdempotencyKeysTest.java`, `TEST/application/error/InventarioErrorsTest.java`, `TEST/application/usecase/command/RegistrarMovimientoHandlerTest.java`

**Interfaces:**
- Produces: `TipoMovimiento.ANULACION_VENTA` y `TipoMovimiento#ignoraEstadoLoteEnIngreso(): boolean`; `IdempotencyKeys.reverso(UUID movimientoOriginalId): String` (package-private); `InventarioErrors.salidasNoEncontradas()` (404 `INV_SALIDAS_NO_ENCONTRADAS`).

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `EnumsAndValueObjectsTest`:

```java
    @Test
    void anulacionVentaIsANonManualIngresoThatIgnoresTheLoteStateOnIngreso() {
        assertThat(TipoMovimiento.desde("ANULACION_VENTA")).contains(TipoMovimiento.ANULACION_VENTA);
        assertThat(TipoMovimiento.ANULACION_VENTA.ingreso()).isTrue();
        assertThat(TipoMovimiento.ANULACION_VENTA.naturaleza()).isEqualTo("E");
        assertThat(TipoMovimiento.ANULACION_VENTA.manual()).isFalse();
        assertThat(TipoMovimiento.ANULACION_VENTA.tipoOperacionSunat()).isEqualTo("05");
        assertThat(TipoMovimiento.ANULACION_VENTA.exigeLoteVendible()).isFalse();
        assertThat(TipoMovimiento.ANULACION_VENTA.ignoraEstadoLoteEnIngreso()).isTrue();
        assertThat(TipoMovimiento.INGRESO_COMPRA.ignoraEstadoLoteEnIngreso()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.ignoraEstadoLoteEnIngreso()).isFalse();
    }
```

Agregar a `IdempotencyKeysTest`:

```java
    @Test
    void eachOriginalMovementHasAStableReversalKey() {
        var id = UUID.fromString("12345678-9abc-4def-8123-456789abcdef");

        assertThat(IdempotencyKeys.reverso(id)).isEqualTo("reverso:12345678-9abc-4def-8123-456789abcdef");
        assertThat(IdempotencyKeys.reverso(id)).isEqualTo(IdempotencyKeys.reverso(id));
        assertThat(IdempotencyKeys.reverso(id)).isNotEqualTo(IdempotencyKeys.reverso(UUID.randomUUID()));
    }
```

Agregar a `InventarioErrorsTest`:

```java
    @Test
    void exposesTheMissingSalidasError() {
        assertThat(InventarioErrors.salidasNoEncontradas().code()).isEqualTo("INV_SALIDAS_NO_ENCONTRADAS");
        assertThat(InventarioErrors.salidasNoEncontradas().category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }
```

Agregar a `RegistrarMovimientoHandlerTest` (usa `lote(EstadoLote, LocalDate)`, `posicion`, `value`, `error`, `DocumentoOrigen` ya importados):

```java
    private static RegistrarMovimientoCommand anulacionVenta() {
        var origen = new DocumentoOrigen("ANULACION_VENTA", UUID.randomUUID(), UUID.randomUUID(), null);
        return new RegistrarMovimientoCommand(
                TENANT, ALMACEN, SKU, LOTE, null, null, "ANULACION_VENTA", new BigDecimal("3"),
                "Anulacion de venta", ACTOR_ID, null, origen);
    }

    @Test
    void anAnulacionVentaRestoresStockEvenWhenTheLoteNoLongerAdmitsIngresos() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.INMOVILIZADO_RECALL, VENCIMIENTO)));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("4", "0", 2)));

        var result = value(handler.execute(anulacionVenta()));

        assertThat(result.tipo()).isEqualTo("ANULACION_VENTA");
        assertThat(result.naturaleza()).isEqualTo("E");
        assertThat(result.stockAnterior()).isEqualByComparingTo("4");
        assertThat(result.stockPosterior()).isEqualByComparingTo("7");
    }

    @Test
    void aManualIngresoOnTheSameNonAdmittingLoteIsStillRejected() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.INMOVILIZADO_RECALL, VENCIMIENTO)));

        assertThat(error(handler.execute(ingresoSobreLote("1"))).code()).isEqualTo("INV_LOTE_NO_ADMITE_INGRESO");
        verify(writePort, never()).registrar(any());
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: FAIL de compilación (`ANULACION_VENTA`, `ignoraEstadoLoteEnIngreso`, `reverso`, `salidasNoEncontradas` no existen).

- [ ] **Step 3: Implementar**

`MAIN/domain/model/TipoMovimiento.java`: reemplazar constantes, campos, constructor y agregar accesor:

```java
public enum TipoMovimiento {
    AJUSTE_INGRESO(true, "E", true, "99", false, false),
    AJUSTE_SALIDA(false, "S", true, "99", false, false),
    INGRESO_COMPRA(true, "E", false, "02", false, false),
    SALIDA_VENTA(false, "S", false, "01", true, false),
    ANULACION_VENTA(true, "E", false, "05", false, true);

    private final boolean ingreso;
    private final String naturaleza;
    private final boolean manual;
    private final String tipoOperacionSunat;
    private final boolean exigeLoteVendible;
    private final boolean ignoraEstadoLoteEnIngreso;

    TipoMovimiento(
            boolean ingreso, String naturaleza, boolean manual, String tipoOperacionSunat,
            boolean exigeLoteVendible, boolean ignoraEstadoLoteEnIngreso) {
        this.ingreso = ingreso;
        this.naturaleza = naturaleza;
        this.manual = manual;
        this.tipoOperacionSunat = tipoOperacionSunat;
        this.exigeLoteVendible = exigeLoteVendible;
        this.ignoraEstadoLoteEnIngreso = ignoraEstadoLoteEnIngreso;
    }
```
y junto a los demás accesores: `public boolean ignoraEstadoLoteEnIngreso() { return ignoraEstadoLoteEnIngreso; }` (el resto del archivo no cambia).

`MAIN/application/usecase/command/RegistrarMovimientoHandler.java`: en `aplicar`, reemplazar la guarda de ingreso por:

```java
        if (solicitud.tipo().ingreso() && !solicitud.tipo().ignoraEstadoLoteEnIngreso()
                && !resuelto.lote().estado().admiteIngreso()) {
            return Result.failure(InventarioErrors.loteNoAdmiteIngreso());
        }
```

`MAIN/application/usecase/command/IdempotencyKeys.java`: agregar:

```java
    static String reverso(UUID movimientoOriginalId) {
        return "reverso:" + movimientoOriginalId;
    }
```

`MAIN/application/error/InventarioErrors.java`: agregar junto a los demás errores:

```java
    public static ApplicationError salidasNoEncontradas() {
        return notFound("INV_SALIDAS_NO_ENCONTRADAS", "La venta indicada no tiene salidas de inventario que reintegrar.");
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: PASS (todo el módulo, incluidos los tests previos).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): preparar tipo ANULACION_VENTA y errores de reintegro

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Consulta de las salidas de una venta

**Files:**
- Create: `MAIN/application/port/out/SalidaDeVenta.java`
- Modify: `MAIN/application/port/out/InventarioWritePort.java`
- Modify: `MAIN/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapter.java`
- Test: `TEST/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapterTest.java`

**Interfaces:**
- Produces: `SalidaDeVenta(UUID movimientoId, UUID almacenId, UUID skuId, UUID loteId, String numeroLote, LocalDate fechaVencimiento, BigDecimal cantidad)`; `List<SalidaDeVenta> InventarioWritePort#findSalidasDeVenta(UUID tenantId, UUID ventaId)`.

- [ ] **Step 1: Escribir el test que falla**

Agregar a `InventarioJdbcWriteAdapterTest` (importar `SalidaDeVenta` no hace falta; importar `java.util.List` si falta):

```java
    @Test
    void listsTheSalidasVentaOfASaleFromTheKardex() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", UUID.fromString("abababab-abab-4bab-8bab-abababababab"));
        row.put("almacen_uuid", ALMACEN);
        row.put("sku_uuid", SKU);
        row.put("lote_uuid", LOTE);
        row.put("numero_lote", "L-001");
        row.put("fecha_vencimiento", VENCIMIENTO);
        row.put("cantidad", new BigDecimal("3.0000"));
        jdbc.rows("m.tipo_movimiento = 'SALIDA_VENTA'", row);
        var venta = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

        var salidas = adapter.findSalidasDeVenta(TENANT, venta);

        assertThat(salidas).hasSize(1);
        var salida = salidas.getFirst();
        assertThat(salida.movimientoId()).isEqualTo(UUID.fromString("abababab-abab-4bab-8bab-abababababab"));
        assertThat(salida.almacenId()).isEqualTo(ALMACEN);
        assertThat(salida.skuId()).isEqualTo(SKU);
        assertThat(salida.loteId()).isEqualTo(LOTE);
        assertThat(salida.numeroLote()).isEqualTo("L-001");
        assertThat(salida.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(salida.cantidad()).isEqualByComparingTo("3");
        var statement = jdbc.statementContaining("m.tipo_movimiento = 'SALIDA_VENTA'");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("ventaId", venta);
        assertThat(statement.sql()).contains("m.documento_tipo = 'VENTA'").contains("m.documento_uuid = :ventaId");
    }

    @Test
    void aSaleWithoutSalidasYieldsAnEmptyList() {
        assertThat(adapter.findSalidasDeVenta(TENANT, UUID.randomUUID())).isEmpty();
    }
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:inventario:test --tests "*InventarioJdbcWriteAdapterTest"`
Expected: FAIL de compilación (`findSalidasDeVenta` no existe).

- [ ] **Step 3: Implementar**

`MAIN/application/port/out/SalidaDeVenta.java`:

```java
package com.softprimesolutions.inventario.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalidaDeVenta(
        UUID movimientoId,
        UUID almacenId,
        UUID skuId,
        UUID loteId,
        String numeroLote,
        LocalDate fechaVencimiento,
        BigDecimal cantidad) {
}
```

`InventarioWritePort`: agregar

```java
    List<SalidaDeVenta> findSalidasDeVenta(UUID tenantId, UUID ventaId);
```

`InventarioJdbcWriteAdapter`: agregar la constante

```java
    private static final String SALIDAS_DE_VENTA = """
            SELECT m.uuid_publico, a.uuid_publico AS almacen_uuid, k.uuid_publico AS sku_uuid,
                   l.uuid_publico AS lote_uuid, l.numero_lote, l.fecha_vencimiento, m.cantidad
              FROM sch_inventario.movimiento_inventario m
              JOIN sch_admin.tenant t ON t.id = m.tenant_id
              JOIN sch_organizacion.almacen a ON a.id = m.almacen_origen_id AND a.tenant_id = m.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = m.sku_id AND k.tenant_id = m.tenant_id
              JOIN sch_inventario.lote l ON l.id = m.lote_id AND l.tenant_id = m.tenant_id
             WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'SALIDA_VENTA'
               AND m.documento_tipo = 'VENTA' AND m.documento_uuid = :ventaId
            """;
```

y el método:

```java
    @Override
    public List<SalidaDeVenta> findSalidasDeVenta(UUID tenantId, UUID ventaId) {
        return jdbcClient.sql(SALIDAS_DE_VENTA)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new SalidaDeVenta(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "almacen_uuid"),
                        JdbcColumns.uuid(rs, "sku_uuid"), JdbcColumns.uuid(rs, "lote_uuid"),
                        rs.getString("numero_lote"), JdbcColumns.date(rs, "fecha_vencimiento"),
                        rs.getBigDecimal("cantidad")))
                .list();
    }
```
(importar `com.softprimesolutions.inventario.application.port.out.SalidaDeVenta`).

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): consultar las salidas de una venta en el kardex

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Caso de uso de reintegro

**Files:**
- Create: `MAIN/application/dto/command/ReintegrarSalidasDeVentaCommand.java`
- Create: `MAIN/application/port/in/ReintegrarSalidasDeVentaUseCase.java`
- Create: `MAIN/application/usecase/command/ReintegrarSalidasDeVentaHandler.java`
- Test: `TEST/application/usecase/command/ReintegrarSalidasDeVentaHandlerTest.java`

**Interfaces:**
- Consumes: `InventarioWritePort#findSalidasDeVenta`, `SalidaDeVenta`, `RegistrarMovimientoUseCase`, `TransaccionPort`, `IdempotencyKeys.reverso`, `InventarioErrors.salidasNoEncontradas`, `DocumentoOrigen`.
- Produces: `ReintegrarSalidasDeVentaCommand(UUID tenantId, UUID ventaId, UUID actorId)`; `ReintegrarSalidasDeVentaUseCase#execute(ReintegrarSalidasDeVentaCommand): Result<List<MovimientoResult>, ApplicationError>` (un movimiento por salida, en orden `skuId`, `fechaVencimiento`, `numeroLote`); constantes `ReintegrarSalidasDeVentaHandler.DOCUMENTO_ANULACION = "ANULACION_VENTA"` y `MOTIVO_ANULACION = "Anulacion de venta"`.

- [ ] **Step 1: Escribir el test que falla**

`TEST/application/usecase/command/ReintegrarSalidasDeVentaHandlerTest.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.OTRO_SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.SalidaDeVenta;
import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReintegrarSalidasDeVentaHandlerTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LOTE_A = UUID.fromString("c1c1c1c1-c1c1-4c1c-8c1c-c1c1c1c1c1c1");
    private static final UUID LOTE_B = UUID.fromString("c2c2c2c2-c2c2-4c2c-8c2c-c2c2c2c2c2c2");
    private static final UUID LOTE_C = UUID.fromString("c3c3c3c3-c3c3-4c3c-8c3c-c3c3c3c3c3c3");

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final List<RegistrarMovimientoCommand> enviados = new ArrayList<>();
    private final RegistrarMovimientoUseCase registrarMovimiento = command -> {
        enviados.add(command);
        return Result.success(new MovimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), command.loteId(), "ANULACION_VENTA", "E", command.cantidad(),
                new BigDecimal("1"), new BigDecimal("9"), AHORA));
    };
    private final TransaccionPort transaccion = new TransaccionPort() {
        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            return trabajo.get();
        }
    };
    private final ReintegrarSalidasDeVentaHandler handler =
            new ReintegrarSalidasDeVentaHandler(writePort, registrarMovimiento, transaccion);

    private static SalidaDeVenta salida(UUID movimiento, UUID sku, UUID lote, String numeroLote, LocalDate vence, String cantidad) {
        return new SalidaDeVenta(movimiento, ALMACEN, sku, lote, numeroLote, vence, new BigDecimal(cantidad));
    }

    private static ReintegrarSalidasDeVentaCommand command() {
        return new ReintegrarSalidasDeVentaCommand(TENANT, VENTA, ACTOR_ID);
    }

    private static List<MovimientoResult> value(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static ApplicationError error(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void revertsEachSalidaWithItsOwnStableKeyAndTheSaleAsOriginDocument() {
        var original = UUID.fromString("abababab-abab-4bab-8bab-abababababab");
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(original, SKU, LOTE_A, "L-A", LocalDate.of(2027, 1, 1), "3")));

        var movimientos = value(handler.execute(command()));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A);
        var enviado = enviados.getFirst();
        assertThat(enviado.tipo()).isEqualTo("ANULACION_VENTA");
        assertThat(enviado.tenantId()).isEqualTo(TENANT);
        assertThat(enviado.almacenId()).isEqualTo(ALMACEN);
        assertThat(enviado.skuId()).isEqualTo(SKU);
        assertThat(enviado.loteId()).isEqualTo(LOTE_A);
        assertThat(enviado.cantidad()).isEqualByComparingTo("3");
        assertThat(enviado.motivo()).isEqualTo(ReintegrarSalidasDeVentaHandler.MOTIVO_ANULACION);
        assertThat(enviado.actorId()).isEqualTo(ACTOR_ID);
        assertThat(enviado.idempotencyKey()).isEqualTo(IdempotencyKeys.reverso(original));
        assertThat(enviado.origen().tipo()).isEqualTo(ReintegrarSalidasDeVentaHandler.DOCUMENTO_ANULACION);
        assertThat(enviado.origen().documentoId()).isEqualTo(VENTA);
        assertThat(enviado.origen().lineaId()).isEqualTo(original);
        assertThat(enviado.origen().proveedorId()).isNull();
    }

    @Test
    void appliesTheReversalsInSkuThenExpiryThenLoteNumberOrder() {
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(UUID.randomUUID(), OTRO_SKU, LOTE_C, "L-C", LocalDate.of(2027, 1, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_B, "L-B", LocalDate.of(2027, 6, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_A, "L-A", LocalDate.of(2027, 6, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_C, "L-Z", LocalDate.of(2026, 12, 1), "1")));

        value(handler.execute(command()));

        assertThat(enviados).extracting(RegistrarMovimientoCommand::loteId)
                .containsExactly(LOTE_C, LOTE_A, LOTE_B, LOTE_C);
        assertThat(enviados).extracting(RegistrarMovimientoCommand::skuId)
                .containsExactly(SKU, SKU, SKU, OTRO_SKU);
    }

    @Test
    void aSaleWithoutSalidasIsNotFound() {
        assertThat(error(handler.execute(command())).code()).isEqualTo("INV_SALIDAS_NO_ENCONTRADAS");
        assertThat(enviados).isEmpty();
    }

    @Test
    void stopsAtTheFirstReversalThatFailsAndPassesItsErrorThrough() {
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(UUID.randomUUID(), SKU, LOTE_A, "L-A", LocalDate.of(2027, 1, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_B, "L-B", LocalDate.of(2027, 2, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_C, "L-C", LocalDate.of(2027, 3, 1), "1")));
        var llamadas = new ArrayList<UUID>();
        var fallaElSegundo = new ReintegrarSalidasDeVentaHandler(writePort, command -> {
            llamadas.add(command.loteId());
            return llamadas.size() == 2
                    ? Result.failure(CONFLICTO)
                    : Result.success(new MovimientoResult(
                            UUID.randomUUID(), UUID.randomUUID(), command.loteId(), "ANULACION_VENTA", "E",
                            command.cantidad(), new BigDecimal("1"), new BigDecimal("2"), AHORA));
        }, transaccion);

        var failure = error(fallaElSegundo.execute(command()));

        assertThat(failure).isSameAs(CONFLICTO);
        assertThat(llamadas).containsExactly(LOTE_A, LOTE_B);
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(null, registrarMovimiento, transaccion));
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(writePort, null, transaccion));
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(writePort, registrarMovimiento, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:inventario:test --tests "*ReintegrarSalidasDeVentaHandlerTest"`
Expected: FAIL de compilación (comando, caso de uso y handler no existen).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/command/ReintegrarSalidasDeVentaCommand.java`:

```java
package com.softprimesolutions.inventario.application.dto.command;

import java.util.UUID;

public record ReintegrarSalidasDeVentaCommand(UUID tenantId, UUID ventaId, UUID actorId) {
}
```

`MAIN/application/port/in/ReintegrarSalidasDeVentaUseCase.java`:

```java
package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ReintegrarSalidasDeVentaUseCase {
    Result<List<MovimientoResult>, ApplicationError> execute(ReintegrarSalidasDeVentaCommand command);
}
```

`MAIN/application/usecase/command/ReintegrarSalidasDeVentaHandler.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.in.ReintegrarSalidasDeVentaUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.SalidaDeVenta;
import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ReintegrarSalidasDeVentaHandler implements ReintegrarSalidasDeVentaUseCase {

    static final String DOCUMENTO_ANULACION = "ANULACION_VENTA";
    static final String MOTIVO_ANULACION = "Anulacion de venta";

    private static final Comparator<SalidaDeVenta> ORDEN_DE_BLOQUEO = Comparator
            .comparing(SalidaDeVenta::skuId)
            .thenComparing(SalidaDeVenta::fechaVencimiento)
            .thenComparing(SalidaDeVenta::numeroLote);

    private final InventarioWritePort writePort;
    private final RegistrarMovimientoUseCase registrarMovimiento;
    private final TransaccionPort transaccion;

    public ReintegrarSalidasDeVentaHandler(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimiento,
            TransaccionPort transaccion) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.registrarMovimiento = Objects.requireNonNull(registrarMovimiento, "registrarMovimiento es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
    }

    @Override
    public Result<List<MovimientoResult>, ApplicationError> execute(ReintegrarSalidasDeVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return transaccion.ejecutar(() -> reintegrar(command));
    }

    private Result<List<MovimientoResult>, ApplicationError> reintegrar(ReintegrarSalidasDeVentaCommand command) {
        var salidas = writePort.findSalidasDeVenta(command.tenantId(), command.ventaId()).stream()
                .sorted(ORDEN_DE_BLOQUEO)
                .toList();
        if (salidas.isEmpty()) return Result.failure(InventarioErrors.salidasNoEncontradas());
        Result<List<MovimientoResult>, ApplicationError> acumulado = Result.success(List.of());
        for (var salida : salidas) {
            acumulado = acumulado.flatMap(previos -> registrarMovimiento.execute(movimiento(command, salida))
                    .map(registrado -> Stream.concat(previos.stream(), Stream.of(registrado)).toList()));
        }
        return acumulado;
    }

    private static RegistrarMovimientoCommand movimiento(
            ReintegrarSalidasDeVentaCommand command, SalidaDeVenta salida) {
        var origen = new DocumentoOrigen(DOCUMENTO_ANULACION, command.ventaId(), salida.movimientoId(), null);
        return new RegistrarMovimientoCommand(
                command.tenantId(), salida.almacenId(), salida.skuId(), salida.loteId(), null, null,
                TipoMovimiento.ANULACION_VENTA.name(), salida.cantidad(), MOTIVO_ANULACION, command.actorId(),
                IdempotencyKeys.reverso(salida.movimientoId()), origen);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:inventario:test --tests "*ReintegrarSalidasDeVentaHandlerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): caso de uso de reintegro de las salidas de una venta

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Contrato público, fachada y wiring

**Files:**
- Create: `MAIN/api/AnulacionInventarioApi.java`, `ReintegroVentaSolicitud.java`, `ReintegroVentaRegistrado.java`, `MovimientoReintegrado.java`
- Create: `MAIN/api/facade/AnulacionInventarioFacade.java`
- Modify: `MAIN/infrastructure/configuration/InventarioModuleConfiguration.java`
- Test: `TEST/api/facade/AnulacionInventarioFacadeTest.java`, `TEST/infrastructure/configuration/InventarioModuleConfigurationTest.java`

**Interfaces:**
- Produces (usado por `ventas`): `AnulacionInventarioApi#reintegrarSalidasDeVenta(ReintegroVentaSolicitud): Result<ReintegroVentaRegistrado, ApplicationError>`; `ReintegroVentaSolicitud(UUID tenantId, UUID ventaId, UUID actorId)`; `ReintegroVentaRegistrado(List<MovimientoReintegrado> movimientos)`; `MovimientoReintegrado(UUID movimientoId, UUID loteId, BigDecimal cantidad, BigDecimal stockPosterior)`; constantes `AnulacionInventarioApi.CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE"` y `CODIGO_SALIDAS_NO_ENCONTRADAS = "INV_SALIDAS_NO_ENCONTRADAS"`; bean `AnulacionInventarioApi`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/api/facade/AnulacionInventarioFacadeTest.java`:

```java
package com.softprimesolutions.inventario.api.facade;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnulacionInventarioFacadeTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static ReintegroVentaSolicitud solicitud() {
        return new ReintegroVentaSolicitud(TENANT, VENTA, ACTOR_ID);
    }

    @Test
    void translatesTheRequestIntoACommandAndTheMovementsIntoReintegratedLotes() {
        var received = new AtomicReference<ReintegrarSalidasDeVentaCommand>();
        var movimientoId = UUID.randomUUID();
        var facade = new AnulacionInventarioFacade(command -> {
            received.set(command);
            return Result.success(List.of(new MovimientoResult(
                    movimientoId, POSICION, LOTE, "ANULACION_VENTA", "E", new BigDecimal("3"),
                    new BigDecimal("7"), new BigDecimal("10"), AHORA)));
        });

        var registrado = facade.reintegrarSalidasDeVenta(solicitud())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().ventaId()).isEqualTo(VENTA);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(registrado.movimientos()).hasSize(1);
        var movimiento = registrado.movimientos().getFirst();
        assertThat(movimiento.movimientoId()).isEqualTo(movimientoId);
        assertThat(movimiento.loteId()).isEqualTo(LOTE);
        assertThat(movimiento.cantidad()).isEqualByComparingTo("3");
        assertThat(movimiento.stockPosterior()).isEqualByComparingTo("10");
    }

    @Test
    void passesTheUseCaseErrorThrough() {
        var facade = new AnulacionInventarioFacade(command -> Result.failure(CONFLICTO));

        ApplicationError error = facade.reintegrarSalidasDeVenta(solicitud()).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }

    @Test
    void requiresItsCollaboratorAndTheRequest() {
        assertThatNullPointerException().isThrownBy(() -> new AnulacionInventarioFacade(null));
        var facade = new AnulacionInventarioFacade(command -> Result.failure(CONFLICTO));
        assertThatNullPointerException().isThrownBy(() -> facade.reintegrarSalidasDeVenta(null));
    }

    @Test
    void exposesTheErrorCodesOfTheInventoryErrors() {
        assertThat(AnulacionInventarioApi.CODIGO_CONCURRENCIA).isEqualTo(InventarioErrors.CONCURRENCIA);
        assertThat(AnulacionInventarioApi.CODIGO_SALIDAS_NO_ENCONTRADAS)
                .isEqualTo(InventarioErrors.salidasNoEncontradas().code());
    }
}
```

En `InventarioModuleConfigurationTest.wiresEveryCommandUseCase` agregar al final (`transaccion` es el `TransaccionPort` ya usado por la prueba de salida; si el test no lo tiene en un campo, crearlo con `mock(TransaccionPort.class)`):

```java
        var reintegrar = configuration.reintegrarSalidasDeVentaUseCase(writePort, registrarMovimiento, transaccion);
        assertThat(reintegrar).isNotNull();
        assertThat(configuration.anulacionInventarioApi(reintegrar)).isNotNull();
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:inventario:test --tests "*AnulacionInventarioFacadeTest" --tests "*InventarioModuleConfigurationTest"`
Expected: FAIL de compilación.

- [ ] **Step 3: Implementar**

`MAIN/api/MovimientoReintegrado.java`:

```java
package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record MovimientoReintegrado(UUID movimientoId, UUID loteId, BigDecimal cantidad, BigDecimal stockPosterior) {
}
```

`MAIN/api/ReintegroVentaRegistrado.java`:

```java
package com.softprimesolutions.inventario.api;

import java.util.List;

public record ReintegroVentaRegistrado(List<MovimientoReintegrado> movimientos) {
}
```

`MAIN/api/ReintegroVentaSolicitud.java`:

```java
package com.softprimesolutions.inventario.api;

import java.util.UUID;

public record ReintegroVentaSolicitud(UUID tenantId, UUID ventaId, UUID actorId) {
}
```

`MAIN/api/AnulacionInventarioApi.java`:

```java
package com.softprimesolutions.inventario.api;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface AnulacionInventarioApi {

    String CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";
    String CODIGO_SALIDAS_NO_ENCONTRADAS = "INV_SALIDAS_NO_ENCONTRADAS";

    Result<ReintegroVentaRegistrado, ApplicationError> reintegrarSalidasDeVenta(ReintegroVentaSolicitud solicitud);
}
```

`MAIN/api/facade/AnulacionInventarioFacade.java`:

```java
package com.softprimesolutions.inventario.api.facade;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.MovimientoReintegrado;
import com.softprimesolutions.inventario.api.ReintegroVentaRegistrado;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.port.in.ReintegrarSalidasDeVentaUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class AnulacionInventarioFacade implements AnulacionInventarioApi {

    private final ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVenta;

    public AnulacionInventarioFacade(ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVenta) {
        this.reintegrarSalidasDeVenta =
                Objects.requireNonNull(reintegrarSalidasDeVenta, "reintegrarSalidasDeVenta es obligatorio");
    }

    @Override
    public Result<ReintegroVentaRegistrado, ApplicationError> reintegrarSalidasDeVenta(
            ReintegroVentaSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "solicitud es obligatoria");
        var command = new ReintegrarSalidasDeVentaCommand(
                solicitud.tenantId(), solicitud.ventaId(), solicitud.actorId());
        return reintegrarSalidasDeVenta.execute(command).map(movimientos -> new ReintegroVentaRegistrado(
                movimientos.stream().map(movimiento -> new MovimientoReintegrado(
                        movimiento.id(), movimiento.loteId(), movimiento.cantidad(), movimiento.stockPosterior()))
                        .toList()));
    }
}
```

`InventarioModuleConfiguration`: agregar imports (`AnulacionInventarioApi`, `AnulacionInventarioFacade`, `ReintegrarSalidasDeVentaUseCase`, `ReintegrarSalidasDeVentaHandler`) y beans:

```java
    @Bean
    ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVentaUseCase(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimientoUseCase,
            TransaccionPort inventarioTransaccionPort) {
        return new ReintegrarSalidasDeVentaHandler(writePort, registrarMovimientoUseCase, inventarioTransaccionPort);
    }

    @Bean
    AnulacionInventarioApi anulacionInventarioApi(ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVentaUseCase) {
        return new AnulacionInventarioFacade(reintegrarSalidasDeVentaUseCase);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:inventario:check`
Expected: PASS (tests, JaCoCo 100% por clase y ArchUnit del módulo).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): publicar AnulacionInventarioApi para ventas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Integración contra PostgreSQL y verificación completa

**Files:**
- Modify: `BTEST/inventario/api/InventarioConcurrencyIntegrationTest.java`

**Interfaces:**
- Consumes: bean `AnulacionInventarioApi`, `SalidaInventarioApi`, helpers existentes `physicalStock()`, campos `TENANT_ID`, `almacenId`, `skuId`, `loteId` (lote `L-CON` con stock 10, vence en 1 año), `actor`, `registrarMovimiento`, `salidaInventario`, `jdbcClient`, y el helper `venta(String cantidad, String clave)` que llama a `SalidaInventarioApi` con un `ventaId` aleatorio.

- [ ] **Step 1: Escribir los tests**

En `InventarioConcurrencyIntegrationTest` agregar el campo e imports (`AnulacionInventarioApi`, `ReintegroVentaSolicitud`, `ReintegroVentaRegistrado`, `SalidaVentaSolicitud`):

```java
    @Autowired
    private AnulacionInventarioApi anulacionInventario;
```

Agregar tests y helpers:

```java
    @Test
    void reintegratingASaleRestoresEveryConsumedLoteAndRecordsTheAnulacionInTheKardex() {
        var antiguo = registrarMovimiento.execute(new RegistrarMovimientoCommand(
                TENANT_ID, almacenId, skuId, null, "L-ANT", LocalDate.now().plusMonths(6), "AJUSTE_INGRESO",
                new BigDecimal("3"), "Lote por vencer", actor, null))
                .fold(MovimientoResult::loteId, error -> { throw new AssertionError(error); });
        var ventaId = UUID.randomUUID();
        vender(ventaId, "5", "venta-anulable");
        assertThat(stockDe(antiguo)).isEqualByComparingTo("0");
        assertThat(stockDe(loteId)).isEqualByComparingTo("8");

        var reintegro = reintegrar(ventaId).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(reintegro.movimientos()).extracting(MovimientoReintegrado::loteId).containsExactly(antiguo, loteId);
        assertThat(stockDe(antiguo)).isEqualByComparingTo("3");
        assertThat(stockDe(loteId)).isEqualByComparingTo("10");
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario m
                         WHERE m.tipo_movimiento = 'ANULACION_VENTA' AND m.documento_tipo = 'ANULACION_VENTA'
                           AND m.documento_uuid = :ventaId AND m.tipo_operacion_sunat = '05'
                           AND m.naturaleza = 'E' AND m.actor = :actor
                        """).param("ventaId", ventaId).param("actor", actor.toString())
                .query(Long.class).single()).isEqualTo(2L);
    }

    @Test
    void reintegratingTheSameSaleTwiceReturnsTheSameMovementsWithoutRestoringStockAgain() {
        var ventaId = UUID.randomUUID();
        vender(ventaId, "4", "venta-doble");

        var primero = reintegrar(ventaId).fold(value -> value, error -> { throw new AssertionError(error); });
        var segundo = reintegrar(ventaId).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(segundo.movimientos()).extracting(MovimientoReintegrado::movimientoId)
                .containsExactlyElementsOf(primero.movimientos().stream().map(MovimientoReintegrado::movimientoId).toList());
        assertThat(physicalStock()).isEqualByComparingTo("10");
    }

    @Test
    void aLoteBlockedAfterTheSaleStillReceivesTheStockButStaysUnsellable() {
        var ventaId = UUID.randomUUID();
        vender(ventaId, "4", "venta-bloqueada");
        jdbcClient.sql("UPDATE sch_inventario.lote SET estado_lote = 'INMOVILIZADO_RECALL' WHERE uuid_publico = :loteId")
                .param("loteId", loteId).update();

        var reintegro = reintegrar(ventaId);

        assertThat(reintegro.isSuccess()).isTrue();
        assertThat(physicalStock()).isEqualByComparingTo("10");
        assertThat(venta(new BigDecimal("1"), "venta-nueva-sin-stock-vendible").fold(value -> "OK", ApplicationError::code))
                .isEqualTo(SalidaInventarioApi.CODIGO_STOCK_INSUFICIENTE);
    }

    @Test
    void aSaleWithoutSalidasIsReportedAsNotFoundWithoutMovingStock() {
        var error = reintegrar(UUID.randomUUID()).fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(AnulacionInventarioApi.CODIGO_SALIDAS_NO_ENCONTRADAS);
        assertThat(physicalStock()).isEqualByComparingTo("10");
    }

    private Result<SalidaVentaRegistrada, ApplicationError> vender(UUID ventaId, String cantidad, String clave) {
        return salidaInventario.registrarSalidaVenta(new SalidaVentaSolicitud(
                TENANT_ID, almacenId, skuId, new BigDecimal(cantidad), ventaId, UUID.randomUUID(), actor, clave));
    }

    private Result<ReintegroVentaRegistrado, ApplicationError> reintegrar(UUID ventaId) {
        return anulacionInventario.reintegrarSalidasDeVenta(new ReintegroVentaSolicitud(TENANT_ID, ventaId, actor));
    }

    private BigDecimal stockDe(UUID lote) {
        return jdbcClient.sql("""
                        SELECT p.cantidad_fisica FROM sch_inventario.posicion_inventario p
                          JOIN sch_inventario.lote l ON l.id = p.lote_id WHERE l.uuid_publico = :loteId
                        """).param("loteId", lote).query(BigDecimal.class).single();
    }
```

Notas: el helper `venta(String, String)` ya existe en la clase (usa un `ventaId` aleatorio); `vender(UUID, ...)` lo reemplaza para los tests que necesitan conocer el `ventaId`. Si `vender(...)` devuelve un `Result` que no se usa en los tests de arriba, está bien: el éxito se afirma por el stock. Importar `com.softprimesolutions.inventario.api.MovimientoReintegrado` y `SalidaVentaRegistrada`.

- [ ] **Step 2: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.inventario.api.InventarioConcurrencyIntegrationTest"` (requiere Docker).
Expected: PASS. Si el reintegro sobre el lote `INMOVILIZADO_RECALL` falla con `INV_LOTE_NO_ADMITE_INGRESO`, la guarda de la Task 1 no se aplicó: revisar `RegistrarMovimientoHandler#aplicar`. Si el SQL de `SALIDAS_DE_VENTA` falla por una columna (`almacen_origen_id`, `documento_uuid`), corregir el SQL contra `V005__inventario_lotes_transferencias.sql` y el INSERT de `INSERTAR_MOVIMIENTO` del adapter, y reportarlo.

- [ ] **Step 3: Verificación completa**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (compilación, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo al 100% en archivos nuevos). Si JaCoCo reporta ramas sin cubrir, agregar el test que falta, no relajar el umbral.

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app
git commit -m "test(inventario): validar el reintegro de ventas contra PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (sección de inventario):** contrato público (Task 4); localización de salidas en el kardex y reversos por movimiento (Tasks 2–3); idempotencia por clave derivada del id original y replay (Tasks 1, 3, 5); tipo `ANULACION_VENTA` con SUNAT `05`, no manual, sin exigir lote vendible y aceptado aunque el lote no admita ingresos (Task 1); orden de bloqueo por SKU/vencimiento/lote (Task 3); transacción propia REQUIRED (Task 3 + wiring Task 4); `INV_SALIDAS_NO_ENCONTRADAS` (Tasks 1, 3, 5). La serialización por venta queda a cargo de `ventas` (parte 2) y se documenta en Global Constraints.

**Riesgo conocido:** un segundo reintegro simultáneo de la misma venta, sin la serialización de `ventas`, chocaría con `business_uuid` dentro de la transacción de PostgreSQL; por eso no hay prueba de concurrencia de doble reintegro en esta parte y sí en `ventas` (doble anulación simultánea, parte 2).

**Consistencia de tipos:** `SalidaDeVenta` (7 campos) coincide entre Tasks 2 y 3; `ReintegrarSalidasDeVentaCommand` (3 campos) coincide entre Tasks 3 y 4; `ReintegroVentaSolicitud`/`ReintegroVentaRegistrado`/`MovimientoReintegrado` coinciden entre Tasks 4 y 5; el handler usa `IdempotencyKeys.reverso` y los códigos de error definidos en Task 1.
