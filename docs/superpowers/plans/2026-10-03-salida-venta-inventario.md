# API de salida de inventario para ventas — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer `SalidaInventarioApi` en `inventario::api` para que `ventas` descuente stock con asignación FEFO, idempotencia y atomicidad.

**Architecture:** Un caso de uso nuevo `RegistrarSalidaVentaUseCase` asigna lotes por FEFO (nueva consulta en `InventarioWritePort`) y delega cada tramo en el `RegistrarMovimientoUseCase` existente con un tipo nuevo `SALIDA_VENTA`. Una fachada en `api.facade` (mismo patrón que `IngresoInventarioFacade`) expone el contrato público. La transacción la abre quien llama (`ventas`).

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-salida-venta-inventario-design.md`

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas (gate JaCoCo por clase).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- Clean Architecture: `domain` no depende de Spring; los casos de uso devuelven `Result<T, ApplicationError>`, nunca `null` ni excepciones para casos previstos.
- Solo lotes `HABILITADO` son vendibles (`EstadoLote.vendible()`); lote no vencido: `fecha_vencimiento >= hoy` (UTC, igual que `RegistrarMovimientoHandler`).
- `movimiento_inventario.tipo_movimiento` no tiene CHECK: no hace falta migración.
- Si el movimiento falla por concurrencia dentro de una transacción externa, la API devuelve `INV_MODIFICACION_CONCURRENTE`; el reintento de la operación completa es responsabilidad del llamador (como hace `compras` con `SpringTransaccionAdapter`).
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:inventario:test --tests "<clase>"`.
- Commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

Rutas abreviadas: `MAIN` = `service-botica/modules/inventario/src/main/java/com/softprimesolutions/inventario`, `TEST` = `service-botica/modules/inventario/src/test/java/com/softprimesolutions/inventario`.

---

### Task 1: Fundamentos de dominio y aplicación

Agrega `SALIDA_VENTA`, hace opcional el proveedor del origen, expone `disponible()`, agrega errores, extrae `IdempotencyKeys` (evita duplicar la derivación del UUID) y protege el handler contra lotes no vendibles.

**Files:**
- Modify: `MAIN/domain/model/TipoMovimiento.java`
- Modify: `MAIN/application/dto/command/DocumentoOrigen.java`
- Modify: `MAIN/domain/model/PosicionInventario.java`
- Modify: `MAIN/application/error/InventarioErrors.java`
- Create: `MAIN/application/usecase/command/IdempotencyKeys.java`
- Modify: `MAIN/application/usecase/command/RegistrarMovimientoHandler.java`
- Test: `TEST/domain/model/EnumsAndValueObjectsTest.java`, `TEST/domain/model/PosicionInventarioTest.java`, `TEST/application/error/InventarioErrorsTest.java`, `TEST/application/usecase/command/RegistrarMovimientoHandlerTest.java`
- Create test: `TEST/application/dto/command/DocumentoOrigenTest.java`, `TEST/application/usecase/command/IdempotencyKeysTest.java`

**Interfaces:**
- Produces: `TipoMovimiento.SALIDA_VENTA`, `TipoMovimiento#exigeLoteVendible(): boolean`; `DocumentoOrigen(String tipo, UUID documentoId, UUID lineaId, UUID proveedorId /*nullable*/)`; `PosicionInventario#disponible(): BigDecimal`; `InventarioErrors.loteNoVendible()`, `.stockInsuficiente()`, `.cantidadInvalida()`; `IdempotencyKeys.businessUuid(String): UUID` (null → null) y `IdempotencyKeys.tramo(String clave, int numero): String` (package-private, `application.usecase.command`).

- [ ] **Step 1: Escribir los tests que fallan**

Agregar dentro de `EnumsAndValueObjectsTest`:

```java
    @Test
    void salidaVentaIsANonManualSalidaThatRequiresASellableLote() {
        assertThat(TipoMovimiento.desde("SALIDA_VENTA")).contains(TipoMovimiento.SALIDA_VENTA);
        assertThat(TipoMovimiento.SALIDA_VENTA.ingreso()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.naturaleza()).isEqualTo("S");
        assertThat(TipoMovimiento.SALIDA_VENTA.manual()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.tipoOperacionSunat()).isEqualTo("01");
        assertThat(TipoMovimiento.SALIDA_VENTA.exigeLoteVendible()).isTrue();
        assertThat(TipoMovimiento.AJUSTE_SALIDA.exigeLoteVendible()).isFalse();
    }
```

Agregar dentro de `PosicionInventarioTest`:

```java
    @Test
    void exposesTheAvailableQuantityAsPhysicalMinusReserved() {
        assertThat(InventarioFixtures.posicion("10", "3", 0).disponible()).isEqualByComparingTo("7");
    }
```
(si `InventarioFixtures` no está importado en esa clase, usar el import estático ya existente de `posicion`).

Agregar dentro de `InventarioErrorsTest`:

```java
    @Test
    void exposesTheSalidaVentaErrors() {
        assertThat(InventarioErrors.loteNoVendible().code()).isEqualTo("INV_LOTE_NO_VENDIBLE");
        assertThat(InventarioErrors.loteNoVendible().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(InventarioErrors.stockInsuficiente().code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(InventarioErrors.stockInsuficiente().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(InventarioErrors.cantidadInvalida().code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(InventarioErrors.cantidadInvalida().category()).isEqualTo(ErrorCategory.VALIDATION);
    }
```
(importar `com.softprimesolutions.shared.application.error.ErrorCategory` si falta).

Crear `TEST/application/dto/command/DocumentoOrigenTest.java`:

```java
package com.softprimesolutions.inventario.application.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentoOrigenTest {

    @Test
    void theSupplierIsOptional() {
        var origen = new DocumentoOrigen("VENTA", UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(origen.proveedorId()).isNull();
    }

    @Test
    void typeDocumentAndLineAreRequired() {
        var id = UUID.randomUUID();
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen(null, id, id, null));
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen("VENTA", null, id, null));
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen("VENTA", id, null, null));
    }
}
```

Crear `TEST/application/usecase/command/IdempotencyKeysTest.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyKeysTest {

    @Test
    void aMissingKeyHasNoBusinessUuid() {
        assertThat(IdempotencyKeys.businessUuid(null)).isNull();
    }

    @Test
    void theBusinessUuidIsDeterministicAndIgnoresSurroundingBlanks() {
        var esperado = UUID.nameUUIDFromBytes("idempotency:abc".getBytes(StandardCharsets.UTF_8));

        assertThat(IdempotencyKeys.businessUuid("abc")).isEqualTo(esperado);
        assertThat(IdempotencyKeys.businessUuid("  abc ")).isEqualTo(esperado);
    }

    @Test
    void eachTramoHasItsOwnStableKeyOfFixedLength() {
        var primero = IdempotencyKeys.tramo("venta-1", 1);

        assertThat(primero).isEqualTo(IdempotencyKeys.tramo(" venta-1 ", 1)).hasSize(36);
        assertThat(primero).isNotEqualTo(IdempotencyKeys.tramo("venta-1", 2));
        assertThat(primero).isNotEqualTo(IdempotencyKeys.tramo("venta-2", 1));
    }
}
```

Agregar dentro de `RegistrarMovimientoHandlerTest` (usa `lote`, `loteHabilitado`, `posicion`, `error`, `value` ya existentes):

```java
    private static RegistrarMovimientoCommand salidaVenta() {
        var origen = new DocumentoOrigen("VENTA", UUID.randomUUID(), UUID.randomUUID(), null);
        return new RegistrarMovimientoCommand(
                TENANT, ALMACEN, SKU, LOTE, null, null, "SALIDA_VENTA", new BigDecimal("3"), "Venta", ACTOR_ID,
                null, origen);
    }

    @Test
    void aSalidaVentaDiscountsStockFromASellableLote() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));

        var result = value(handler.execute(salidaVenta()));

        assertThat(result.tipo()).isEqualTo("SALIDA_VENTA");
        assertThat(result.naturaleza()).isEqualTo("S");
        assertThat(result.stockPosterior()).isEqualByComparingTo("7");
    }

    @Test
    void aSalidaVentaOnANonSellableLoteIsRejectedWithoutRegistering() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.BLOQUEADO, VENCIMIENTO)));

        assertThat(error(handler.execute(salidaVenta())).code()).isEqualTo("INV_LOTE_NO_VENDIBLE");
        verify(writePort, never()).registrar(any());
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan (no compilan)**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: FAIL de compilación (`SALIDA_VENTA`, `exigeLoteVendible`, `disponible`, `loteNoVendible`, `IdempotencyKeys` no existen).

- [ ] **Step 3: Implementar**

`TipoMovimiento.java` (reemplazar enum y campos):

```java
public enum TipoMovimiento {
    AJUSTE_INGRESO(true, "E", true, "99", false),
    AJUSTE_SALIDA(false, "S", true, "99", false),
    INGRESO_COMPRA(true, "E", false, "02", false),
    SALIDA_VENTA(false, "S", false, "01", true);

    private final boolean ingreso;
    private final String naturaleza;
    private final boolean manual;
    private final String tipoOperacionSunat;
    private final boolean exigeLoteVendible;

    TipoMovimiento(
            boolean ingreso, String naturaleza, boolean manual, String tipoOperacionSunat,
            boolean exigeLoteVendible) {
        this.ingreso = ingreso;
        this.naturaleza = naturaleza;
        this.manual = manual;
        this.tipoOperacionSunat = tipoOperacionSunat;
        this.exigeLoteVendible = exigeLoteVendible;
    }
```
y agregar al final de los accesores: `public boolean exigeLoteVendible() { return exigeLoteVendible; }` (el resto del archivo no cambia).

`DocumentoOrigen.java`: eliminar la línea `Objects.requireNonNull(proveedorId, "proveedorId es obligatorio");`.

`PosicionInventario.java`: agregar junto a los accesores:

```java
    public BigDecimal disponible() { return cantidadFisica.subtract(cantidadReservada); }
```

`InventarioErrors.java`: agregar tras `loteNoAdmiteIngreso()`:

```java
    public static ApplicationError loteNoVendible() {
        return conflict("INV_LOTE_NO_VENDIBLE", "El estado del lote no permite venderlo.");
    }

    public static ApplicationError stockInsuficiente() {
        return conflict(InventarioErrorCodes.STOCK_INSUFICIENTE,
                "El stock disponible no alcanza para la salida solicitada.");
    }

    public static ApplicationError cantidadInvalida() {
        return validation(InventarioErrorCodes.CANTIDAD_INVALIDA,
                "La cantidad debe ser mayor que cero y admite hasta 4 decimales.");
    }
```

Crear `MAIN/application/usecase/command/IdempotencyKeys.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    static UUID businessUuid(String clave) {
        return Optional.ofNullable(clave)
                .map(valor -> uuidDe("idempotency:" + valor.trim()))
                .orElse(null);
    }

    static String tramo(String clave, int numero) {
        return uuidDe("tramo:" + clave.trim() + "#" + numero).toString();
    }

    private static UUID uuidDe(String texto) {
        return UUID.nameUUIDFromBytes(texto.getBytes(StandardCharsets.UTF_8));
    }
}
```

`RegistrarMovimientoHandler.java`: (a) en `execute`, sustituir `businessUuid(command)` por `IdempotencyKeys.businessUuid(command.idempotencyKey())`; (b) eliminar el método privado `businessUuid(RegistrarMovimientoCommand)`; (c) en `aplicar`, tras el `if` de `admiteIngreso`, agregar:

```java
        if (solicitud.tipo().exigeLoteVendible() && !resuelto.lote().estado().vendible()) {
            return Result.failure(InventarioErrors.loteNoVendible());
        }
```
(`StandardCharsets` sigue usándose en `huella`.)

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: PASS (incluye todos los tests previos de inventario; el compilador marcará cualquier uso del constructor de `TipoMovimiento` fuera del enum — no debería haberlo).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): preparar tipo SALIDA_VENTA y guardas de lote vendible

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Consulta FEFO en el puerto de escritura

**Files:**
- Modify: `MAIN/application/port/out/InventarioWritePort.java`
- Modify: `MAIN/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapter.java:45-55`
- Modify: `TEST/infrastructure/persistence/JdbcClientStub.java` (agregar sobrecarga `rows` con lista)
- Test: `TEST/infrastructure/persistence/write/adapter/InventarioJdbcWriteAdapterTest.java`

**Interfaces:**
- Consumes: `PosicionInventario` y `mapPosicion` existentes.
- Produces: `List<PosicionInventario> InventarioWritePort#findPosicionesVendiblesFefo(UUID tenantId, UUID almacenId, UUID skuId, LocalDate hoy)` — posiciones con lote `HABILITADO`, `fecha_vencimiento >= hoy`, `cantidad_fisica > cantidad_reservada`, ordenadas por `fecha_vencimiento, numero_lote`.

- [ ] **Step 1: Escribir el test que falla**

En `JdbcClientStub` agregar:

```java
    public JdbcClientStub rows(String fragment, List<Map<String, Object>> rows) {
        rules.add(new Rule(fragment, rows, List.of(), 1, null));
        return this;
    }
```

En `InventarioJdbcWriteAdapterTest` agregar (importar `java.util.List` y `HOY` estático de `InventarioFixtures`):

```java
    @Test
    void listsTheSellablePositionsOfASkuInFefoOrderWithItsFilters() {
        var segunda = posicionRow();
        segunda.put("uuid_publico", UUID.fromString("abababab-abab-4bab-8bab-abababababab"));
        segunda.put("cantidad_fisica", new BigDecimal("3.0000"));
        segunda.put("cantidad_reservada", new BigDecimal("0.0000"));
        jdbc.rows("ORDER BY l.fecha_vencimiento", List.of(posicionRow(), segunda));

        var posiciones = adapter.findPosicionesVendiblesFefo(TENANT, ALMACEN, SKU, HOY);

        assertThat(posiciones).hasSize(2);
        assertThat(posiciones.get(0).id().value()).isEqualTo(POSICION);
        assertThat(posiciones.get(0).disponible()).isEqualByComparingTo("8");
        assertThat(posiciones.get(1).disponible()).isEqualByComparingTo("3");
        var statement = jdbc.statementContaining("ORDER BY l.fecha_vencimiento");
        assertThat(statement.params())
                .containsEntry("tenantId", TENANT).containsEntry("almacenId", ALMACEN)
                .containsEntry("skuId", SKU).containsEntry("hoy", HOY);
        assertThat(statement.sql())
                .contains("l.estado_lote = 'HABILITADO'")
                .contains("l.fecha_vencimiento >= :hoy")
                .contains("p.cantidad_fisica > p.cantidad_reservada");
    }
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:inventario:test --tests "*InventarioJdbcWriteAdapterTest"`
Expected: FAIL de compilación (`findPosicionesVendiblesFefo` no existe).

- [ ] **Step 3: Implementar**

Puerto: agregar `import java.util.List;` y

```java
    List<PosicionInventario> findPosicionesVendiblesFefo(UUID tenantId, UUID almacenId, UUID skuId, LocalDate hoy);
```

Adapter: reemplazar la constante `POSICION_DISPONIBLE` por una base compartida y dos derivadas:

```java
    private static final String POSICION_SELECT = """
            SELECT p.uuid_publico, l.uuid_publico AS lote_uuid, a.uuid_publico AS almacen_uuid,
                   k.uuid_publico AS sku_uuid, p.cantidad_fisica, p.cantidad_reservada, p.version_lock
              FROM sch_inventario.posicion_inventario p
              JOIN sch_admin.tenant t ON t.id = p.tenant_id
              JOIN sch_organizacion.almacen a ON a.id = p.almacen_id AND a.tenant_id = p.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = p.sku_id AND k.tenant_id = p.tenant_id
              JOIN sch_inventario.lote l ON l.id = p.lote_id AND l.tenant_id = p.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId
               AND p.estado_inventario = 'DISPONIBLE' AND p.ubicacion_id IS NULL AND p.es_activo = '1'
            """;
    private static final String POSICION_DISPONIBLE = POSICION_SELECT + " AND l.uuid_publico = :loteId";
    private static final String POSICION_VENDIBLE_FEFO = POSICION_SELECT + """
               AND k.uuid_publico = :skuId AND l.estado_lote = 'HABILITADO' AND l.fecha_vencimiento >= :hoy
               AND p.cantidad_fisica > p.cantidad_reservada
             ORDER BY l.fecha_vencimiento, l.numero_lote
            """;
```
y el método (importar `java.util.List`):

```java
    @Override
    public List<PosicionInventario> findPosicionesVendiblesFefo(
            UUID tenantId, UUID almacenId, UUID skuId, LocalDate hoy) {
        return jdbcClient.sql(POSICION_VENDIBLE_FEFO)
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .param("skuId", skuId)
                .param("hoy", hoy)
                .query((rs, rowNumber) -> mapPosicion(rs))
                .list();
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:inventario:test --tests "*InventarioJdbcWriteAdapterTest"`
Expected: PASS (incluidos los tests previos de `findPosicion`; si alguno usa un fragmento SQL que cambió de posición, ajustar solo el fragmento del test, no la consulta).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): consultar posiciones vendibles en orden FEFO

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Caso de uso de salida por venta

**Files:**
- Create: `MAIN/application/dto/command/RegistrarSalidaVentaCommand.java`
- Create: `MAIN/application/port/in/RegistrarSalidaVentaUseCase.java`
- Create: `MAIN/application/usecase/command/RegistrarSalidaVentaHandler.java`
- Test: `TEST/application/usecase/command/RegistrarSalidaVentaHandlerTest.java`

**Interfaces:**
- Consumes: `InventarioWritePort#findPosicionesVendiblesFefo`, `#findMovimientoPorBusinessUuid`; `RegistrarMovimientoUseCase#execute(RegistrarMovimientoCommand)`; `IdempotencyKeys`; `InventarioErrors.{claveIdempotenciaInvalida, cantidadInvalida, stockInsuficiente, conflictoIdempotencia}`; `DocumentoOrigen`.
- Produces: `RegistrarSalidaVentaCommand(UUID tenantId, UUID almacenId, UUID skuId, BigDecimal cantidad, UUID ventaId, UUID ventaLineaId, UUID actorId, String idempotencyKey)`; `RegistrarSalidaVentaUseCase#execute(RegistrarSalidaVentaCommand): Result<List<MovimientoResult>, ApplicationError>` (un `MovimientoResult` por lote consumido, en orden FEFO); constantes `RegistrarSalidaVentaHandler.DOCUMENTO_VENTA = "VENTA"` y `MOTIVO_VENTA = "Venta"`.

- [ ] **Step 1: Escribir el test que falla**

Crear `TEST/application/usecase/command/RegistrarSalidaVentaHandlerTest.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarSalidaVentaHandlerTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID LOTE_A = UUID.fromString("c1c1c1c1-c1c1-4c1c-8c1c-c1c1c1c1c1c1");
    private static final UUID LOTE_B = UUID.fromString("c2c2c2c2-c2c2-4c2c-8c2c-c2c2c2c2c2c2");
    private static final String CLAVE = "venta-1";

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final List<RegistrarMovimientoCommand> enviados = new ArrayList<>();
    private final RegistrarMovimientoUseCase registrarMovimiento = command -> {
        enviados.add(command);
        return Result.success(movimiento(command.loteId(), command.cantidad().toPlainString(), "0"));
    };
    private final RegistrarSalidaVentaHandler handler =
            new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, () -> AHORA);

    private static PosicionInventario posicion(UUID lote, String fisica, String reservada) {
        return PosicionInventario.restore(
                new PosicionId(UUID.randomUUID()), lote, ALMACEN, SKU, new BigDecimal(fisica),
                new BigDecimal(reservada), 0L);
    }

    private static MovimientoResult movimiento(UUID lote, String cantidad, String posterior) {
        return new MovimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), lote, "SALIDA_VENTA", "S", new BigDecimal(cantidad),
                new BigDecimal("10"), new BigDecimal(posterior), AHORA);
    }

    private static RegistrarSalidaVentaCommand salida(String cantidad, String clave) {
        return new RegistrarSalidaVentaCommand(
                TENANT, ALMACEN, SKU, cantidad == null ? null : new BigDecimal(cantidad), VENTA, LINEA, ACTOR_ID,
                clave);
    }

    private static List<MovimientoResult> value(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static ApplicationError error(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private void fefo(PosicionInventario... posiciones) {
        when(writePort.findPosicionesVendiblesFefo(TENANT, ALMACEN, SKU, HOY)).thenReturn(List.of(posiciones));
    }

    @Test
    void consumesTheEarliestExpiryFirstAndStopsOnceTheQuantityIsCovered() {
        fefo(posicion(LOTE_A, "5", "0"), posicion(LOTE_B, "10", "0"));

        var movimientos = value(handler.execute(salida("3", CLAVE)));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A);
        assertThat(enviados).hasSize(1);
        var command = enviados.getFirst();
        assertThat(command.tipo()).isEqualTo("SALIDA_VENTA");
        assertThat(command.loteId()).isEqualTo(LOTE_A);
        assertThat(command.cantidad()).isEqualByComparingTo("3");
        assertThat(command.motivo()).isEqualTo(RegistrarSalidaVentaHandler.MOTIVO_VENTA);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.skuId()).isEqualTo(SKU);
        assertThat(command.idempotencyKey()).isEqualTo(IdempotencyKeys.tramo(CLAVE, 1));
        assertThat(command.origen().tipo()).isEqualTo(RegistrarSalidaVentaHandler.DOCUMENTO_VENTA);
        assertThat(command.origen().documentoId()).isEqualTo(VENTA);
        assertThat(command.origen().lineaId()).isEqualTo(LINEA);
        assertThat(command.origen().proveedorId()).isNull();
    }

    @Test
    void splitsTheQuantityAcrossLotesUsingOnlyTheAvailableOfEach() {
        fefo(posicion(LOTE_A, "5", "1"), posicion(LOTE_B, "10", "0"));

        var movimientos = value(handler.execute(salida("6", CLAVE)));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A, LOTE_B);
        assertThat(enviados).extracting(RegistrarMovimientoCommand::cantidad)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("4"), new BigDecimal("2"));
        assertThat(enviados).extracting(RegistrarMovimientoCommand::idempotencyKey)
                .containsExactly(IdempotencyKeys.tramo(CLAVE, 1), IdempotencyKeys.tramo(CLAVE, 2));
    }

    @Test
    void failsWithoutMovingStockWhenTheAvailableDoesNotCoverTheQuantity() {
        fefo(posicion(LOTE_A, "5", "1"), posicion(LOTE_B, "2", "0"));

        assertThat(error(handler.execute(salida("7", CLAVE))).code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(enviados).isEmpty();
    }

    @Test
    void failsWhenThereAreNoSellablePositions() {
        assertThat(error(handler.execute(salida("1", CLAVE))).code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(enviados).isEmpty();
    }

    @Test
    void stopsAtTheFirstMovementThatFailsAndPassesItsErrorThrough() {
        fefo(posicion(LOTE_A, "5", "0"), posicion(LOTE_B, "10", "0"));
        var llamadas = new ArrayList<UUID>();
        var fallaElSegundo = new RegistrarSalidaVentaHandler(writePort, command -> {
            llamadas.add(command.loteId());
            return llamadas.size() == 1
                    ? Result.success(movimiento(command.loteId(), "5", "0"))
                    : Result.failure(CONFLICTO);
        }, () -> AHORA);

        var failure = error(fallaElSegundo.execute(salida("8", CLAVE)));

        assertThat(failure).isSameAs(CONFLICTO);
        assertThat(llamadas).containsExactly(LOTE_A, LOTE_B);
    }

    @Test
    void aRetryReturnsTheMovementsAlreadyRegisteredWithoutQueryingOrMovingStock() {
        var primero = movimiento(LOTE_A, "5", "0");
        var segundo = movimiento(LOTE_B, "1", "9");
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 1))))
                .thenReturn(Optional.of(new MovimientoRegistrado(primero, "h1")));
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 2))))
                .thenReturn(Optional.of(new MovimientoRegistrado(segundo, "h2")));

        var movimientos = value(handler.execute(salida("6", CLAVE)));

        assertThat(movimientos).containsExactly(primero, segundo);
        assertThat(enviados).isEmpty();
        verify(writePort, never()).findPosicionesVendiblesFefo(any(), any(), any(), any());
    }

    @Test
    void aRetryWithADifferentQuantityIsAnIdempotencyConflict() {
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 1))))
                .thenReturn(Optional.of(new MovimientoRegistrado(movimiento(LOTE_A, "5", "0"), "h1")));

        assertThat(error(handler.execute(salida("6", CLAVE))).code()).isEqualTo("INV_IDEMPOTENCY_CONFLICT");
        assertThat(enviados).isEmpty();
    }

    @Test
    void theIdempotencyKeyIsRequiredAndAtMostTwoHundredCharacters() {
        assertThat(error(handler.execute(salida("1", null))).code()).isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(salida("1", "  "))).code()).isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(salida("1", "x".repeat(201)))).code())
                .isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(enviados).isEmpty();
    }

    @Test
    void theQuantityMustBePositiveWithAtMostFourDecimals() {
        assertThat(error(handler.execute(salida(null, CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("0", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("-1", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("1.00001", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(enviados).isEmpty();
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(null, registrarMovimiento, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(writePort, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :modules:inventario:test --tests "*RegistrarSalidaVentaHandlerTest"`
Expected: FAIL de compilación (`RegistrarSalidaVentaCommand`, `RegistrarSalidaVentaHandler` no existen).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/command/RegistrarSalidaVentaCommand.java`:

```java
package com.softprimesolutions.inventario.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarSalidaVentaCommand(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        BigDecimal cantidad,
        UUID ventaId,
        UUID ventaLineaId,
        UUID actorId,
        String idempotencyKey) {
}
```

`MAIN/application/port/in/RegistrarSalidaVentaUseCase.java`:

```java
package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface RegistrarSalidaVentaUseCase {
    Result<List<MovimientoResult>, ApplicationError> execute(RegistrarSalidaVentaCommand command);
}
```

`MAIN/application/usecase/command/RegistrarSalidaVentaHandler.java`:

```java
package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.in.RegistrarSalidaVentaUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class RegistrarSalidaVentaHandler implements RegistrarSalidaVentaUseCase {

    static final String DOCUMENTO_VENTA = "VENTA";
    static final String MOTIVO_VENTA = "Venta";

    private static final int CLAVE_IDEMPOTENCIA_MAX = 200;
    private static final int ESCALA_MAXIMA = 4;

    private final InventarioWritePort writePort;
    private final RegistrarMovimientoUseCase registrarMovimiento;
    private final ClockPort clock;

    public RegistrarSalidaVentaHandler(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimiento, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.registrarMovimiento = Objects.requireNonNull(registrarMovimiento, "registrarMovimiento es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<List<MovimientoResult>, ApplicationError> execute(RegistrarSalidaVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (claveInvalida(command.idempotencyKey())) {
            return Result.failure(InventarioErrors.claveIdempotenciaInvalida());
        }
        if (cantidadInvalida(command.cantidad())) return Result.failure(InventarioErrors.cantidadInvalida());
        var previos = movimientosPrevios(command);
        return previos.isEmpty() ? asignar(command) : repetir(command, previos);
    }

    private List<MovimientoResult> movimientosPrevios(RegistrarSalidaVentaCommand command) {
        return IntStream.iterate(1, numero -> numero + 1)
                .mapToObj(numero -> writePort.findMovimientoPorBusinessUuid(
                        command.tenantId(),
                        IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(command.idempotencyKey(), numero))))
                .takeWhile(Optional::isPresent)
                .map(Optional::get)
                .map(MovimientoRegistrado::resultado)
                .toList();
    }

    private static Result<List<MovimientoResult>, ApplicationError> repetir(
            RegistrarSalidaVentaCommand command, List<MovimientoResult> previos) {
        var total = previos.stream().map(MovimientoResult::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(command.cantidad()) == 0
                ? Result.success(previos)
                : Result.failure(InventarioErrors.conflictoIdempotencia());
    }

    private Result<List<MovimientoResult>, ApplicationError> asignar(RegistrarSalidaVentaCommand command) {
        var hoy = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        var posiciones = writePort.findPosicionesVendiblesFefo(
                command.tenantId(), command.almacenId(), command.skuId(), hoy);
        var disponible = posiciones.stream().map(PosicionInventario::disponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponible.compareTo(command.cantidad()) < 0) return Result.failure(InventarioErrors.stockInsuficiente());
        return consumir(command, tramos(posiciones, command.cantidad()));
    }

    private static List<Tramo> tramos(List<PosicionInventario> posiciones, BigDecimal cantidad) {
        var tramos = new ArrayList<Tramo>();
        var pendiente = cantidad;
        for (var posicion : posiciones) {
            if (pendiente.signum() == 0) break;
            var tomar = posicion.disponible().min(pendiente);
            tramos.add(new Tramo(posicion.loteId(), tomar));
            pendiente = pendiente.subtract(tomar);
        }
        return tramos;
    }

    private Result<List<MovimientoResult>, ApplicationError> consumir(
            RegistrarSalidaVentaCommand command, List<Tramo> tramos) {
        Result<List<MovimientoResult>, ApplicationError> acumulado = Result.success(List.of());
        for (var indice = 0; indice < tramos.size(); indice++) {
            var tramo = tramos.get(indice);
            var numero = indice + 1;
            acumulado = acumulado.flatMap(previos -> registrarMovimiento.execute(movimiento(command, tramo, numero))
                    .map(registrado -> Stream.concat(previos.stream(), Stream.of(registrado)).toList()));
        }
        return acumulado;
    }

    private static RegistrarMovimientoCommand movimiento(
            RegistrarSalidaVentaCommand command, Tramo tramo, int numero) {
        var origen = new DocumentoOrigen(
                DOCUMENTO_VENTA, command.ventaId(), command.ventaLineaId(), null);
        return new RegistrarMovimientoCommand(
                command.tenantId(), command.almacenId(), command.skuId(), tramo.loteId(), null, null,
                TipoMovimiento.SALIDA_VENTA.name(), tramo.cantidad(), MOTIVO_VENTA, command.actorId(),
                IdempotencyKeys.tramo(command.idempotencyKey(), numero), origen);
    }

    private static boolean claveInvalida(String clave) {
        return clave == null || clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX;
    }

    private static boolean cantidadInvalida(BigDecimal cantidad) {
        return cantidad == null || cantidad.signum() <= 0 || cantidad.stripTrailingZeros().scale() > ESCALA_MAXIMA;
    }

    private record Tramo(UUID loteId, BigDecimal cantidad) {
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :modules:inventario:test --tests "*RegistrarSalidaVentaHandlerTest"`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): caso de uso de salida por venta con asignacion FEFO

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Contrato público, fachada y wiring

**Files:**
- Create: `MAIN/api/SalidaInventarioApi.java`, `MAIN/api/SalidaVentaSolicitud.java`, `MAIN/api/SalidaVentaRegistrada.java`, `MAIN/api/LoteConsumido.java`
- Create: `MAIN/api/facade/SalidaInventarioFacade.java`
- Modify: `MAIN/infrastructure/configuration/InventarioModuleConfiguration.java`
- Test: `TEST/api/facade/SalidaInventarioFacadeTest.java`, `TEST/infrastructure/configuration/InventarioModuleConfigurationTest.java`

**Interfaces:**
- Consumes: `RegistrarSalidaVentaUseCase`, `RegistrarSalidaVentaCommand`, `MovimientoResult`.
- Produces (usado por `ventas`): `SalidaInventarioApi#registrarSalidaVenta(SalidaVentaSolicitud): Result<SalidaVentaRegistrada, ApplicationError>`; `SalidaVentaSolicitud(UUID tenantId, UUID almacenId, UUID skuId, BigDecimal cantidad, UUID ventaId, UUID ventaLineaId, UUID actorId, String idempotencyKey)`; `SalidaVentaRegistrada(List<LoteConsumido> lotes)`; `LoteConsumido(UUID movimientoId, UUID loteId, BigDecimal cantidad, BigDecimal stockPosterior)`; constantes `SalidaInventarioApi.CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE"` y `CODIGO_STOCK_INSUFICIENTE = "INV_STOCK_INSUFICIENTE"`; bean `SalidaInventarioApi`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/api/facade/SalidaInventarioFacadeTest.java`:

```java
package com.softprimesolutions.inventario.api.facade;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SalidaInventarioFacadeTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    private static SalidaVentaSolicitud solicitud() {
        return new SalidaVentaSolicitud(
                TENANT, ALMACEN, SKU, new BigDecimal("6"), VENTA, LINEA, ACTOR_ID, "venta:1");
    }

    @Test
    void translatesTheRequestIntoACommandAndTheMovementsIntoConsumedLotes() {
        var received = new AtomicReference<RegistrarSalidaVentaCommand>();
        var movimientoId = UUID.randomUUID();
        var facade = new SalidaInventarioFacade(command -> {
            received.set(command);
            return Result.success(List.of(new MovimientoResult(
                    movimientoId, POSICION, LOTE, "SALIDA_VENTA", "S", new BigDecimal("6"),
                    new BigDecimal("10"), new BigDecimal("4"), AHORA)));
        });

        var registrada = facade.registrarSalidaVenta(solicitud())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        var command = received.get();
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.skuId()).isEqualTo(SKU);
        assertThat(command.cantidad()).isEqualByComparingTo("6");
        assertThat(command.ventaId()).isEqualTo(VENTA);
        assertThat(command.ventaLineaId()).isEqualTo(LINEA);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("venta:1");
        assertThat(registrada.lotes()).hasSize(1);
        var lote = registrada.lotes().getFirst();
        assertThat(lote.movimientoId()).isEqualTo(movimientoId);
        assertThat(lote.loteId()).isEqualTo(LOTE);
        assertThat(lote.cantidad()).isEqualByComparingTo("6");
        assertThat(lote.stockPosterior()).isEqualByComparingTo("4");
    }

    @Test
    void passesTheUseCaseErrorThrough() {
        var facade = new SalidaInventarioFacade(command -> Result.failure(CONFLICTO));

        ApplicationError error = facade.registrarSalidaVenta(solicitud()).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }

    @Test
    void requiresItsCollaboratorAndTheRequest() {
        assertThatNullPointerException().isThrownBy(() -> new SalidaInventarioFacade(null));
        var facade = new SalidaInventarioFacade(command -> Result.failure(CONFLICTO));
        assertThatNullPointerException().isThrownBy(() -> facade.registrarSalidaVenta(null));
    }

    @Test
    void exposesTheErrorCodesOfTheInventoryErrors() {
        assertThat(SalidaInventarioApi.CODIGO_CONCURRENCIA).isEqualTo(InventarioErrors.CONCURRENCIA);
        assertThat(SalidaInventarioApi.CODIGO_STOCK_INSUFICIENTE)
                .isEqualTo(InventarioErrors.stockInsuficiente().code());
    }
}
```

En `InventarioModuleConfigurationTest.wiresEveryCommandUseCase` agregar al final (importar `com.softprimesolutions.inventario.api.SalidaInventarioApi` no es necesario, basta `assertThat(...).isNotNull()`):

```java
        var registrarSalidaVenta = configuration.registrarSalidaVentaUseCase(writePort, registrarMovimiento, clock);
        assertThat(registrarSalidaVenta).isNotNull();
        assertThat(configuration.salidaInventarioApi(registrarSalidaVenta)).isNotNull();
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:inventario:test --tests "*SalidaInventarioFacadeTest" --tests "*InventarioModuleConfigurationTest"`
Expected: FAIL de compilación (tipos de la API no existen).

- [ ] **Step 3: Implementar**

`MAIN/api/LoteConsumido.java`:

```java
package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumido(UUID movimientoId, UUID loteId, BigDecimal cantidad, BigDecimal stockPosterior) {
}
```

`MAIN/api/SalidaVentaRegistrada.java`:

```java
package com.softprimesolutions.inventario.api;

import java.util.List;

public record SalidaVentaRegistrada(List<LoteConsumido> lotes) {
}
```

`MAIN/api/SalidaVentaSolicitud.java`:

```java
package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SalidaVentaSolicitud(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        BigDecimal cantidad,
        UUID ventaId,
        UUID ventaLineaId,
        UUID actorId,
        String idempotencyKey) {
}
```

`MAIN/api/SalidaInventarioApi.java`:

```java
package com.softprimesolutions.inventario.api;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface SalidaInventarioApi {

    String CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";
    String CODIGO_STOCK_INSUFICIENTE = "INV_STOCK_INSUFICIENTE";

    Result<SalidaVentaRegistrada, ApplicationError> registrarSalidaVenta(SalidaVentaSolicitud solicitud);
}
```

`MAIN/api/facade/SalidaInventarioFacade.java`:

```java
package com.softprimesolutions.inventario.api.facade;

import com.softprimesolutions.inventario.api.LoteConsumido;
import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaRegistrada;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.port.in.RegistrarSalidaVentaUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class SalidaInventarioFacade implements SalidaInventarioApi {

    private final RegistrarSalidaVentaUseCase registrarSalidaVenta;

    public SalidaInventarioFacade(RegistrarSalidaVentaUseCase registrarSalidaVenta) {
        this.registrarSalidaVenta = Objects.requireNonNull(registrarSalidaVenta, "registrarSalidaVenta es obligatorio");
    }

    @Override
    public Result<SalidaVentaRegistrada, ApplicationError> registrarSalidaVenta(SalidaVentaSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "solicitud es obligatoria");
        var command = new RegistrarSalidaVentaCommand(
                solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), solicitud.cantidad(),
                solicitud.ventaId(), solicitud.ventaLineaId(), solicitud.actorId(), solicitud.idempotencyKey());
        return registrarSalidaVenta.execute(command).map(movimientos -> new SalidaVentaRegistrada(
                movimientos.stream().map(movimiento -> new LoteConsumido(
                        movimiento.id(), movimiento.loteId(), movimiento.cantidad(), movimiento.stockPosterior()))
                        .toList()));
    }
}
```

`InventarioModuleConfiguration.java`: agregar imports (`SalidaInventarioApi`, `SalidaInventarioFacade`, `RegistrarSalidaVentaUseCase`, `RegistrarSalidaVentaHandler`) y beans:

```java
    @Bean
    RegistrarSalidaVentaUseCase registrarSalidaVentaUseCase(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimientoUseCase,
            ClockPort inventarioClockPort) {
        return new RegistrarSalidaVentaHandler(writePort, registrarMovimientoUseCase, inventarioClockPort);
    }

    @Bean
    SalidaInventarioApi salidaInventarioApi(RegistrarSalidaVentaUseCase registrarSalidaVentaUseCase) {
        return new SalidaInventarioFacade(registrarSalidaVentaUseCase);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:inventario:test`
Expected: PASS (todo el módulo).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/inventario
git commit -m "feat(inventario): publicar SalidaInventarioApi para ventas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Integración contra PostgreSQL y verificación completa

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/inventario/api/InventarioConcurrencyIntegrationTest.java`

**Interfaces:**
- Consumes: bean `SalidaInventarioApi`, `SalidaVentaSolicitud`, `SalidaVentaRegistrada`, `LoteConsumido`; helpers existentes `physicalStock()`, `salidasInKardex()`, campos `TENANT_ID`, `almacenId`, `skuId`, `loteId` (lote `L-CON` con stock 10, vence en 1 año), `actor`, `registrarMovimiento`, `jdbcClient`.

- [ ] **Step 1: Escribir los tests**

Agregar el campo e imports (`SalidaInventarioApi`, `SalidaVentaSolicitud`, `LoteConsumido`, `java.util.concurrent.Future`):

```java
    @Autowired
    private SalidaInventarioApi salidaInventario;
```

Agregar tests y helpers a la clase:

```java
    @Test
    void aSalidaVentaConsumesTheEarliestExpiryLoteFirstAndThenTheNext() {
        var antiguo = registrarMovimiento.execute(new RegistrarMovimientoCommand(
                TENANT_ID, almacenId, skuId, null, "L-ANT", LocalDate.now().plusMonths(6), "AJUSTE_INGRESO",
                new BigDecimal("3"), "Lote por vencer", actor, null))
                .fold(MovimientoResult::loteId, error -> { throw new AssertionError(error); });

        var registrada = venta("5", "venta-fefo").fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(registrada.lotes()).extracting(LoteConsumido::loteId).containsExactly(antiguo, loteId);
        assertThat(registrada.lotes()).extracting(LoteConsumido::cantidad)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("3"), new BigDecimal("2"));
        assertThat(physicalStock()).isEqualByComparingTo("8");
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario
                         WHERE tipo_movimiento = 'SALIDA_VENTA' AND documento_tipo = 'VENTA'
                           AND tipo_operacion_sunat = '01' AND naturaleza = 'S'
                        """).query(Long.class).single()).isEqualTo(2L);
    }

    @Test
    void aSalidaVentaIgnoresBlockedLotesAndFailsWhenNothingSellableCoversTheQuantity() {
        jdbcClient.sql("UPDATE sch_inventario.lote SET estado_lote = 'BLOQUEADO' WHERE uuid_publico = :loteId")
                .param("loteId", loteId).update();

        var error = venta("1", "venta-bloqueada").fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(SalidaInventarioApi.CODIGO_STOCK_INSUFICIENTE);
        assertThat(physicalStock()).isEqualByComparingTo("10");
    }

    @Test
    void retryingTheSameSalidaVentaReturnsTheSameLotesWithoutMovingStockAgain() {
        var primera = venta("4", "venta-reintento").fold(value -> value, error -> { throw new AssertionError(error); });
        var segunda = venta("4", "venta-reintento").fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(segunda).isEqualTo(primera);
        assertThat(physicalStock()).isEqualByComparingTo("6");
        assertThat(salidasInKardex()).isEqualTo(1);
    }

    @Test
    void twoSimultaneousSalidasVentaOfEightFromTenNeverOversell() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<Result<SalidaVentaRegistrada, ApplicationError>>>();
            for (var index = 0; index < 2; index++) {
                var clave = "venta-concurrente-" + index;
                Callable<Result<SalidaVentaRegistrada, ApplicationError>> tarea = () -> {
                    start.await();
                    return venta("8", clave);
                };
                futures.add(pool.submit(tarea));
            }
            start.countDown();
            var results = new ArrayList<Result<SalidaVentaRegistrada, ApplicationError>>();
            for (var future : futures) results.add(future.get());

            assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
            assertThat(results.stream().map(result -> result.fold(value -> "OK", ApplicationError::code))
                    .filter(code -> !code.equals("OK")).toList())
                    .containsExactly(SalidaInventarioApi.CODIGO_STOCK_INSUFICIENTE);
            assertThat(physicalStock()).isEqualByComparingTo("2");
            assertThat(salidasInKardex()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private Result<SalidaVentaRegistrada, ApplicationError> venta(String cantidad, String clave) {
        return salidaInventario.registrarSalidaVenta(new SalidaVentaSolicitud(
                TENANT_ID, almacenId, skuId, new BigDecimal(cantidad), UUID.randomUUID(), UUID.randomUUID(),
                actor, clave));
    }
```

Nota: `retryingTheSameSalidaVentaReturnsTheSameLotes...` usa ventaId/lineaId distintos en el segundo intento (la clave es lo que identifica el reintento), por lo que se verifica justamente que la idempotencia no depende de ellos.

- [ ] **Step 2: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.inventario.api.InventarioConcurrencyIntegrationTest"`
Expected: PASS (requiere Docker para Testcontainers). Si `twoSimultaneousSalidasVenta...` obtiene `INV_MODIFICACION_CONCURRENTE` en el perdedor en lugar de `INV_STOCK_INSUFICIENTE`, es el reintento interno agotado: reproducir 5 veces; si es intermitente, relajar la aserción a "el perdedor falla con uno de los dos códigos y el stock es 2", sin tocar la lógica de producción.

- [ ] **Step 3: Verificación completa (gate de arquitectura y cobertura)**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL — compila, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo (100% en archivos nuevos). Si JaCoCo reporta ramas sin cubrir en `RegistrarSalidaVentaHandler` o `IdempotencyKeys`, agregar el test faltante para esa rama (no relajar el umbral).

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app
git commit -m "test(inventario): validar salida por venta contra PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:** contrato público (Task 4) · replay por origen/clave (Task 3, `movimientosPrevios`/`repetir`) · FEFO con `HABILITADO`, no vencido, `fisica - reservada > 0` (Task 2) · `STOCK_INSUFICIENTE` previo sin mover nada (Task 3) · `SALIDA_VENTA` naturaleza `S`, SUNAT `01`, no manual, origen `VENTA` (Tasks 1 y 3) · `DocumentoOrigen.proveedorId` opcional (Task 1) · bean en `InventarioModuleConfiguration` (Task 4) · transacción externa y concurrencia (Global Constraints + Task 5) · pruebas unitarias e integración (Tasks 1–5). Sin migración (confirmado: sin CHECK en `tipo_movimiento`).

**Desviación respecto a la spec (decisión de diseño, justificada):** la spec decía clave por tramo `idempotencyKey#n`. Se usa `IdempotencyKeys.tramo(clave, n)`, un UUID determinista de 36 caracteres, para que la clave derivada nunca supere el límite de 200 que valida `RegistrarMovimientoHandler`. También se agregó la guarda `exigeLoteVendible` en `RegistrarMovimientoHandler` (cierra la carrera "lote bloqueado entre la consulta FEFO y el movimiento"); no estaba en la spec.

**Consistencia de tipos:** `RegistrarSalidaVentaCommand` (8 campos) coincide en Tasks 3 y 4; `findPosicionesVendiblesFefo(tenantId, almacenId, skuId, hoy)` igual en Tasks 2 y 3; `IdempotencyKeys.tramo/businessUuid` igual en Tasks 1 y 3; `SalidaVentaSolicitud`/`SalidaVentaRegistrada`/`LoteConsumido` igual en Tasks 4 y 5.

**Siguiente plan (fuera de este documento):** módulo `ventas` (turno de caja + venta simple) consumiendo `SalidaInventarioApi`; requiere su propia spec/plan y reintento de la venta completa ante `CODIGO_CONCURRENCIA`.
