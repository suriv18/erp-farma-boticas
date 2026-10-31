# Módulo ventas, parte 2: venta presencial en efectivo — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Registrar ventas presenciales pagadas en efectivo contra el turno abierto de una terminal, descontando stock por FEFO desde `inventario`, con idempotencia, correlativo por terminal y consulta de ventas.

**Architecture:** Un caso de uso `RegistrarVentaHandler` valida el contexto (terminal, turno abierto bloqueado con `FOR SHARE`, almacén, SKUs), construye el agregado `Venta` y, dentro de una única transacción (`TransaccionPort`), inserta venta/líneas/pago y llama a `SalidaInventarioApi` por línea en orden estable de `skuId`. Ante `INV_MODIFICACION_CONCURRENTE` o una carrera de idempotencia reintenta la venta completa (máx. 3), igual que `compras`.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-ventas-turno-caja-venta-simple-design.md`. Requiere la parte 1 completa: `docs/superpowers/plans/2026-10-03-ventas-parte-1-turno-caja.md`, y la API `SalidaInventarioApi` de `inventario` (ya integrada).

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas (gate JaCoCo por clase en `ventas`).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- Clean Architecture: `domain` sin Spring; casos de uso devuelven `Result<T, ApplicationError>`; `Result.success(null)` no existe (lanza NPE).
- Reglas de venta: turno abierto en la terminal; almacén del mismo establecimiento con `permite_lotes` y `permite_venta`; SKU activo; cantidad > 0 con hasta 4 decimales (fracción solo si el SKU `permite_venta_fraccion`); precio unitario >= 0 con hasta 4 decimales; máximo 100 líneas; `total_linea = round(cantidad x precio, 2)`; `subtotal` = suma de líneas; `impuesto = 0` (IGV POR_VALIDAR hasta CPE); `total = subtotal` y > 0; efectivo: `montoRecibido >= total`, `vuelto = recibido - total`.
- `Idempotency-Key` obligatoria (1..160 caracteres); misma clave y huella devuelve la misma venta; huella distinta es conflicto. Hacia inventario la clave por línea es `{Idempotency-Key}:{n° de línea}`.
- Procesar las líneas hacia inventario en orden estable (`skuId`, luego `numeroLinea`) para evitar deadlocks entre ventas con varios SKU.
- `numero_operacion = {codigo de terminal}-{secuencia de 6 dígitos}` con contador por terminal en `sch_venta.secuencia_operacion`, incrementado en la misma transacción (POR_VALIDAR).
- Un fallo en cualquier línea revierte toda la venta, incluidos los descuentos de líneas anteriores (el rollback lo aplica `TransaccionPort`).
- `INV_STOCK_INSUFICIENTE` se propaga con su código (409); `INV_MODIFICACION_CONCURRENTE` se traduce a `VEN_MODIFICACION_CONCURRENTE` y activa el reintento.
- Rutas REST bajo `/api/v1/ventas/ventas`; permisos `ventas.ventas.registrar` y `ventas.ventas.consultar`.
- Los beans de adapters cuyo nombre de clase ya existe en otro módulo (`NumeracionJdbcAdapter` de `compras`) deben llevar nombre explícito (`@Repository("ventasNumeracionAdapter")`) para no provocar `ConflictingBeanDefinitionException` al arrancar el contexto; `SpringTransaccionAdapter` ya quedó como `@Component("ventasTransaccionAdapter")`.
- El medio de pago `EFECTIVO` se crea bajo demanda por tenant (`INSERT ... ON CONFLICT DO NOTHING`), en vez de sembrarlo para los tenants existentes.
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:ventas:test --tests "<clase>"`.
- Commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

Rutas abreviadas: `MAIN` = `service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas`, `TEST` = `service-botica/modules/ventas/src/test/java/com/softprimesolutions/ventas`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`.

---

### Task 1: Dominio de la venta

**Files:**
- Modify: `MAIN/domain/exception/VentasErrorCodes.java`
- Create: `MAIN/domain/model/LoteConsumo.java`, `LineaVenta.java`, `PagoEfectivo.java`, `Venta.java`
- Test: `TEST/domain/model/LineaVentaTest.java`, `TEST/domain/model/PagoEfectivoTest.java`, `TEST/domain/model/VentaTest.java`

**Interfaces:**
- Consumes: `Importes.{montoValido, precioValido, cantidadValida, redondear}`, `Failures.failure`, `Actor` (parte 1).
- Produces:
  - Códigos en `VentasErrorCodes`: `CANTIDAD_INVALIDA="VEN_CANTIDAD_INVALIDA"`, `PRECIO_INVALIDO="VEN_PRECIO_INVALIDO"`, `FRACCION_NO_PERMITIDA="VEN_FRACCION_NO_PERMITIDA"`, `VENTA_SIN_LINEAS="VEN_VENTA_SIN_LINEAS"`, `VENTA_LINEAS_EXCEDIDAS="VEN_VENTA_LINEAS_EXCEDIDAS"`, `TOTAL_INVALIDO="VEN_TOTAL_INVALIDO"`, `MONTO_RECIBIDO_INSUFICIENTE="VEN_MONTO_RECIBIDO_INSUFICIENTE"`.
  - `LoteConsumo(UUID loteId, BigDecimal cantidad)`.
  - `LineaVenta.nueva(UUID id, int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo, boolean permiteFraccion, BigDecimal cantidad, BigDecimal precioUnitario): Result<LineaVenta, ErrorDetail>`; accesores `id()`, `numeroLinea()`, `skuId()`, `descripcion()`, `unidadVentaCodigo()`, `esFraccion()`, `cantidad()`, `precioUnitario()`, `totalLinea()`.
  - `PagoEfectivo(BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto)` con `PagoEfectivo.cobrar(BigDecimal total, BigDecimal recibido): Result<PagoEfectivo, ErrorDetail>`.
  - `Venta.registrar(UUID id, UUID tenantId, UUID terminalId, UUID turnoId, UUID establecimientoId, Actor vendedor, String numeroOperacion, Instant fechaVenta, List<LineaVenta> lineas, BigDecimal montoRecibido): Result<Venta, ErrorDetail>`; accesores `id()`, `tenantId()`, `terminalId()`, `turnoId()`, `establecimientoId()`, `vendedor()`, `numeroOperacion()`, `fechaVenta()`, `lineas()`, `subtotal()`, `total()`, `pago()`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/domain/model/LineaVentaTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LineaVentaTest {

    private static final UUID ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");

    private static Result<LineaVenta, ErrorDetail> nueva(boolean permiteFraccion, BigDecimal cantidad, BigDecimal precio) {
        return LineaVenta.nueva(ID, 3, SKU, "Paracetamol 500 mg", "UND", permiteFraccion, cantidad, precio);
    }

    private static String code(Result<LineaVenta, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void buildsALineWithTheRoundedTotal() {
        var linea = nueva(false, dec("5"), dec("2.50")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(linea.id()).isEqualTo(ID);
        assertThat(linea.numeroLinea()).isEqualTo(3);
        assertThat(linea.skuId()).isEqualTo(SKU);
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(linea.esFraccion()).isFalse();
        assertThat(linea.cantidad()).isEqualTo(dec("5"));
        assertThat(linea.precioUnitario()).isEqualTo(dec("2.50"));
        assertThat(linea.totalLinea()).isEqualTo(dec("12.50"));
    }

    @Test
    void roundsTheLineTotalToTwoDecimalsHalfUp() {
        var linea = nueva(true, dec("3"), dec("0.3333")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(linea.totalLinea()).isEqualTo(dec("1.00"));
    }

    @Test
    void aFractionalQuantityIsAllowedOnlyWhenTheSkuSellsByFraction() {
        var fraccion = nueva(true, dec("0.5"), dec("10")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(fraccion.esFraccion()).isTrue();
        assertThat(fraccion.totalLinea()).isEqualTo(dec("5.00"));
        assertThat(code(nueva(false, dec("0.5"), dec("10")))).isEqualTo(VentasErrorCodes.FRACCION_NO_PERMITIDA);
    }

    @Test
    void rejectsInvalidQuantities() {
        assertThat(code(nueva(false, null, dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(nueva(false, dec("0"), dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(nueva(true, dec("1.00001"), dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
    }

    @Test
    void rejectsInvalidUnitPrices() {
        assertThat(code(nueva(false, dec("1"), null))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
        assertThat(code(nueva(false, dec("1"), dec("-0.01")))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
        assertThat(code(nueva(false, dec("1"), dec("1.00001")))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
    }
}
```

`TEST/domain/model/PagoEfectivoTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import org.junit.jupiter.api.Test;

class PagoEfectivoTest {

    private static String code(Result<PagoEfectivo, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void chargesTheTotalAndReturnsTheChange() {
        var pago = PagoEfectivo.cobrar(dec("12.50"), dec("20"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(pago.monto()).isEqualTo(dec("12.50"));
        assertThat(pago.montoRecibido()).isEqualTo(dec("20.00"));
        assertThat(pago.vuelto()).isEqualTo(dec("7.50"));
    }

    @Test
    void anExactAmountHasNoChange() {
        var pago = PagoEfectivo.cobrar(dec("12.50"), dec("12.50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(pago.vuelto()).isEqualTo(dec("0.00"));
    }

    @Test
    void rejectsAnInvalidOrInsufficientReceivedAmount() {
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), dec("-1")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), dec("12.49"))))
                .isEqualTo(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE);
    }
}
```

`TEST/domain/model/VentaTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VentaTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");

    private static LineaVenta linea(int numero, String cantidad, String precio) {
        return LineaVenta.nueva(UUID.randomUUID(), numero, SKU, "Producto", "UND", false, dec(cantidad), dec(precio))
                .fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static Result<Venta, ErrorDetail> registrar(List<LineaVenta> lineas, BigDecimal recibido) {
        return Venta.registrar(
                VENTA, TENANT, TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR, "POS01-000001", AHORA, lineas, recibido);
    }

    private static String code(Result<Venta, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void registersASaleWithTheSumOfItsLinesAsSubtotalAndTotal() {
        var venta = registrar(List.of(linea(1, "5", "2.50"), linea(2, "2", "10.00")), dec("50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.tenantId()).isEqualTo(TENANT);
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedor()).isEqualTo(ACTOR);
        assertThat(venta.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(venta.fechaVenta()).isEqualTo(AHORA);
        assertThat(venta.lineas()).hasSize(2);
        assertThat(venta.subtotal()).isEqualTo(dec("32.50"));
        assertThat(venta.total()).isEqualTo(dec("32.50"));
        assertThat(venta.pago().montoRecibido()).isEqualTo(dec("50.00"));
        assertThat(venta.pago().vuelto()).isEqualTo(dec("17.50"));
    }

    @Test
    void aSaleNeedsBetweenOneAndOneHundredLines() {
        assertThat(code(registrar(List.of(), dec("10")))).isEqualTo(VentasErrorCodes.VENTA_SIN_LINEAS);
        var demasiadas = IntStream.rangeClosed(1, 101).mapToObj(numero -> linea(numero, "1", "1")).toList();
        assertThat(code(registrar(demasiadas, dec("500")))).isEqualTo(VentasErrorCodes.VENTA_LINEAS_EXCEDIDAS);
        var cien = IntStream.rangeClosed(1, 100).mapToObj(numero -> linea(numero, "1", "1")).toList();
        assertThat(registrar(cien, dec("100")).isSuccess()).isTrue();
    }

    @Test
    void aSaleWithAZeroTotalIsRejected() {
        assertThat(code(registrar(List.of(linea(1, "1", "0")), dec("10")))).isEqualTo(VentasErrorCodes.TOTAL_INVALIDO);
    }

    @Test
    void theCashPaymentIsValidatedAgainstTheTotal() {
        assertThat(code(registrar(List.of(linea(1, "1", "10")), null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(registrar(List.of(linea(1, "1", "10")), dec("9.99"))))
                .isEqualTo(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE);
    }

    @Test
    void theLinesAreImmutable() {
        var venta = registrar(List.of(linea(1, "1", "10")), dec("10"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThatThrownBy(() -> venta.lineas().add(linea(2, "1", "1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*LineaVentaTest" --tests "*PagoEfectivoTest" --tests "*VentaTest"`
Expected: FAIL de compilación (clases de dominio y códigos de error no existen).

- [ ] **Step 3: Implementar**

`MAIN/domain/exception/VentasErrorCodes.java` (agregar las constantes antes del constructor privado):

```java
    public static final String CANTIDAD_INVALIDA = "VEN_CANTIDAD_INVALIDA";
    public static final String PRECIO_INVALIDO = "VEN_PRECIO_INVALIDO";
    public static final String FRACCION_NO_PERMITIDA = "VEN_FRACCION_NO_PERMITIDA";
    public static final String VENTA_SIN_LINEAS = "VEN_VENTA_SIN_LINEAS";
    public static final String VENTA_LINEAS_EXCEDIDAS = "VEN_VENTA_LINEAS_EXCEDIDAS";
    public static final String TOTAL_INVALIDO = "VEN_TOTAL_INVALIDO";
    public static final String MONTO_RECIBIDO_INSUFICIENTE = "VEN_MONTO_RECIBIDO_INSUFICIENTE";
```

`MAIN/domain/model/LoteConsumo.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumo(UUID loteId, BigDecimal cantidad) {
}
```

`MAIN/domain/model/LineaVenta.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.UUID;

public final class LineaVenta {

    private final UUID id;
    private final int numeroLinea;
    private final UUID skuId;
    private final String descripcion;
    private final String unidadVentaCodigo;
    private final boolean esFraccion;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal totalLinea;

    private LineaVenta(
            UUID id, int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo, boolean esFraccion,
            BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal totalLinea) {
        this.id = id;
        this.numeroLinea = numeroLinea;
        this.skuId = skuId;
        this.descripcion = descripcion;
        this.unidadVentaCodigo = unidadVentaCodigo;
        this.esFraccion = esFraccion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.totalLinea = totalLinea;
    }

    public static Result<LineaVenta, ErrorDetail> nueva(
            UUID id, int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo,
            boolean permiteFraccion, BigDecimal cantidad, BigDecimal precioUnitario) {
        if (!Importes.cantidadValida(cantidad)) {
            return failure(VentasErrorCodes.CANTIDAD_INVALIDA,
                    "La cantidad debe ser mayor que cero, no superar 1000000000 y tener hasta 4 decimales.");
        }
        if (!Importes.precioValido(precioUnitario)) {
            return failure(VentasErrorCodes.PRECIO_INVALIDO,
                    "El precio unitario debe estar entre 0 y 1000000000 con hasta 4 decimales.");
        }
        var esFraccion = cantidad.stripTrailingZeros().scale() > 0;
        if (esFraccion && !permiteFraccion) {
            return failure(VentasErrorCodes.FRACCION_NO_PERMITIDA, "El producto no se vende por fraccion.");
        }
        return Result.success(new LineaVenta(
                id, numeroLinea, skuId, descripcion, unidadVentaCodigo, esFraccion, cantidad, precioUnitario,
                Importes.redondear(cantidad.multiply(precioUnitario))));
    }

    public UUID id() { return id; }
    public int numeroLinea() { return numeroLinea; }
    public UUID skuId() { return skuId; }
    public String descripcion() { return descripcion; }
    public String unidadVentaCodigo() { return unidadVentaCodigo; }
    public boolean esFraccion() { return esFraccion; }
    public BigDecimal cantidad() { return cantidad; }
    public BigDecimal precioUnitario() { return precioUnitario; }
    public BigDecimal totalLinea() { return totalLinea; }
}
```

`MAIN/domain/model/PagoEfectivo.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;

public record PagoEfectivo(BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {

    public static Result<PagoEfectivo, ErrorDetail> cobrar(BigDecimal total, BigDecimal recibido) {
        if (!Importes.montoValido(recibido)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El monto recibido debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var recibidoRedondeado = Importes.redondear(recibido);
        if (recibidoRedondeado.compareTo(total) < 0) {
            return failure(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE,
                    "El monto recibido no cubre el total de la venta.");
        }
        return Result.success(new PagoEfectivo(total, recibidoRedondeado, recibidoRedondeado.subtract(total)));
    }
}
```

`MAIN/domain/model/Venta.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Venta {

    private static final int MAXIMO_LINEAS = 100;

    private final UUID id;
    private final UUID tenantId;
    private final UUID terminalId;
    private final UUID turnoId;
    private final UUID establecimientoId;
    private final Actor vendedor;
    private final String numeroOperacion;
    private final Instant fechaVenta;
    private final List<LineaVenta> lineas;
    private final BigDecimal subtotal;
    private final PagoEfectivo pago;

    private Venta(
            UUID id, UUID tenantId, UUID terminalId, UUID turnoId, UUID establecimientoId, Actor vendedor,
            String numeroOperacion, Instant fechaVenta, List<LineaVenta> lineas, BigDecimal subtotal,
            PagoEfectivo pago) {
        this.id = id;
        this.tenantId = tenantId;
        this.terminalId = terminalId;
        this.turnoId = turnoId;
        this.establecimientoId = establecimientoId;
        this.vendedor = vendedor;
        this.numeroOperacion = numeroOperacion;
        this.fechaVenta = fechaVenta;
        this.lineas = List.copyOf(lineas);
        this.subtotal = subtotal;
        this.pago = pago;
    }

    public static Result<Venta, ErrorDetail> registrar(
            UUID id, UUID tenantId, UUID terminalId, UUID turnoId, UUID establecimientoId, Actor vendedor,
            String numeroOperacion, Instant fechaVenta, List<LineaVenta> lineas, BigDecimal montoRecibido) {
        if (lineas.isEmpty()) {
            return failure(VentasErrorCodes.VENTA_SIN_LINEAS, "La venta debe tener al menos una linea.");
        }
        if (lineas.size() > MAXIMO_LINEAS) {
            return failure(VentasErrorCodes.VENTA_LINEAS_EXCEDIDAS, "La venta admite hasta 100 lineas.");
        }
        var subtotal = Importes.redondear(
                lineas.stream().map(LineaVenta::totalLinea).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (subtotal.signum() <= 0) {
            return failure(VentasErrorCodes.TOTAL_INVALIDO, "El total de la venta debe ser mayor que cero.");
        }
        return PagoEfectivo.cobrar(subtotal, montoRecibido).map(pago -> new Venta(
                id, tenantId, terminalId, turnoId, establecimientoId, vendedor, numeroOperacion, fechaVenta, lineas,
                subtotal, pago));
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID terminalId() { return terminalId; }
    public UUID turnoId() { return turnoId; }
    public UUID establecimientoId() { return establecimientoId; }
    public Actor vendedor() { return vendedor; }
    public String numeroOperacion() { return numeroOperacion; }
    public Instant fechaVenta() { return fechaVenta; }
    public List<LineaVenta> lineas() { return lineas; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal total() { return subtotal; }
    public PagoEfectivo pago() { return pago; }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*LineaVentaTest" --tests "*PagoEfectivoTest" --tests "*VentaTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): modelo de dominio de la venta en efectivo

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Aplicación de la venta

**Files:**
- Modify: `MAIN/application/error/VentasErrors.java`
- Modify: `MAIN/application/port/out/TurnoWritePort.java`, `ReferenciasVentasPort.java`, `VentasReadPort.java`
- Create: `MAIN/application/dto/command/LineaVentaInput.java`, `RegistrarVentaCommand.java`
- Create: `MAIN/application/dto/query/ObtenerVentaQuery.java`, `ListarVentasQuery.java`
- Create: `MAIN/application/dto/result/LoteConsumidoResult.java`, `LineaVentaResult.java`, `PagoResult.java`, `VentaResult.java`, `VentaResumenResult.java`, `PaginaResult.java`
- Create: `MAIN/application/port/in/RegistrarVentaUseCase.java`, `ConsultarVentasUseCase.java`
- Create: `MAIN/application/port/out/VentaWritePort.java`, `NumeracionPort.java`, `SalidaInventarioPort.java`
- Create: `MAIN/application/usecase/command/RegistrarVentaHandler.java`, `MAIN/application/usecase/query/ConsultarVentasHandler.java`
- Modify (soporte de tests): `TEST/VentasFixtures.java`
- Test: `TEST/application/error/VentasErrorsTest.java`, `TEST/application/usecase/command/RegistrarVentaHandlerTest.java`, `TEST/application/usecase/query/ConsultarVentasHandlerTest.java`

**Interfaces:**
- Consumes: dominio de la Task 1; `TurnoWritePort`, `ReferenciasVentasPort`, `VentasReadPort`, `TransaccionPort`, `GuardadoOutcome`, `VentasErrors` (parte 1); `ClockPort`, `IdentifierGenerator`.
- Produces:
  - `LineaVentaInput(UUID skuId, BigDecimal cantidad, BigDecimal precioUnitario)`; `RegistrarVentaCommand(UUID tenantId, UUID actorId, String idempotencyKey, UUID terminalId, UUID almacenId, List<LineaVentaInput> lineas, BigDecimal montoRecibido)`; `ObtenerVentaQuery(UUID tenantId, UUID ventaId)`; `ListarVentasQuery(UUID tenantId, UUID establecimientoId, Instant desde, Instant hasta, int page, int size)`.
  - Resultados: `LoteConsumidoResult(UUID loteId, BigDecimal cantidad)`; `LineaVentaResult(int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal totalLinea, List<LoteConsumidoResult> lotes)`; `PagoResult(String medioPago, BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto)`; `VentaResult(UUID id, String numeroOperacion, UUID terminalId, UUID turnoId, UUID establecimientoId, UUID vendedorId, Instant fechaVenta, String moneda, BigDecimal subtotal, BigDecimal descuentoTotal, BigDecimal impuestoTotal, BigDecimal total, String estado, List<LineaVentaResult> lineas, PagoResult pago)`; `VentaResumenResult(UUID id, String numeroOperacion, UUID terminalId, Instant fechaVenta, BigDecimal total, String estado)`; `PaginaResult<T>(List<T> items, int page, int size, long totalElements)`.
  - `RegistrarVentaUseCase#execute(RegistrarVentaCommand): Result<VentaResult, ApplicationError>`; `ConsultarVentasUseCase#obtener(ObtenerVentaQuery)`: `Result<VentaResult, ApplicationError>`, `#listar(ListarVentasQuery)`: `Result<PaginaResult<VentaResumenResult>, ApplicationError>`.
  - `VentaWritePort`: `Optional<VentaExistente> findPorIdempotencia(UUID tenantId, String idempotencyKey)`, `GuardadoOutcome insertar(Venta venta, String idempotencyKey, String huella)`, `void registrarLotes(UUID tenantId, UUID ventaLineaId, List<LoteConsumo> lotes)`; `record VentaExistente(UUID id, String huella)`.
  - `TurnoWritePort#bloquearTurnoAbierto(UUID tenantId, UUID terminalId): Optional<TurnoCaja>` (turno `ABIERTO`, bloqueo `FOR SHARE`).
  - `ReferenciasVentasPort` agrega `Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId)` con `AlmacenRef(UUID id, UUID establecimientoId, boolean operable)` y `Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds)` con `SkuVentaRef(UUID id, String descripcion, String unidadVentaCodigo, boolean permiteFraccion, boolean operable)`.
  - `NumeracionPort#siguienteNumeroOperacion(UUID tenantId, UUID terminalId, String codigoTerminal): String`.
  - `SalidaInventarioPort#descontar(SalidaSolicitada): Result<List<LoteConsumo>, ApplicationError>` con `SalidaSolicitada(UUID tenantId, UUID almacenId, UUID skuId, BigDecimal cantidad, UUID ventaId, UUID ventaLineaId, UUID actorId, String idempotencyKey)`.
  - `VentasReadPort` agrega `Optional<VentaResult> findVenta(UUID tenantId, UUID ventaId)` y `PaginaResult<VentaResumenResult> listarVentas(ListarVentasQuery query)`.
  - Errores nuevos en `VentasErrors`: `noHayTurnoAbierto()` (409 `VEN_TURNO_NO_ABIERTO`), `almacenNoEncontrado()` (404 `VEN_ALMACEN_NO_ENCONTRADO`), `almacenNoOperable()` (409 `VEN_ALMACEN_NO_OPERABLE`), `almacenDeOtroEstablecimiento()` (409 `VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO`), `skuNoEncontrado(UUID)` (404 `VEN_SKU_NO_ENCONTRADO`), `skuNoOperable(UUID)` (409 `VEN_SKU_NO_OPERABLE`), `claveIdempotenciaInvalida()` (400 `VEN_IDEMPOTENCY_KEY_INVALID`), `conflictoIdempotencia()` (409 `VEN_IDEMPOTENCY_CONFLICT`), `ventaNoEncontrada()` (404 `VEN_VENTA_NO_ENCONTRADA`), `paginacionInvalida(int, int)` (400 `VEN_PAGINACION_INVALIDA`).

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `TEST/VentasFixtures.java` estos imports:

```java
import com.softprimesolutions.ventas.application.dto.result.LineaVentaResult;
import com.softprimesolutions.ventas.application.dto.result.LoteConsumidoResult;
import com.softprimesolutions.ventas.application.dto.result.PagoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.SkuVentaRef;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.Venta;
import java.util.List;
```

y estos miembros:

```java
    public static final UUID ALMACEN = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");
    public static final UUID OTRO_SKU = UUID.fromString("99999999-9999-4999-8999-999999999999");
    public static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    public static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    public static final UUID LOTE = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    public static SkuVentaRef skuRef(UUID id, boolean operable) {
        return new SkuVentaRef(id, "Paracetamol 500 mg", "UND", false, operable);
    }

    public static Venta venta() {
        var linea = LineaVenta.nueva(LINEA, 1, SKU, "Paracetamol 500 mg", "UND", false, dec("5"), dec("2.50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });
        return Venta.registrar(
                        VENTA, TENANT, TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR, "POS01-000001", AHORA,
                        List.of(linea), dec("20"))
                .fold(value -> value, error -> { throw new AssertionError(error); });
    }

    public static VentaResult ventaResult() {
        var lote = new LoteConsumidoResult(LOTE, dec("5"));
        var linea = new LineaVentaResult(
                1, SKU, "Paracetamol 500 mg", "UND", dec("5"), dec("2.50"), dec("12.50"), List.of(lote));
        return new VentaResult(
                VENTA, "POS01-000001", TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR_ID, AHORA, "PEN", dec("12.50"),
                dec("0.00"), dec("0.00"), dec("12.50"), "CONFIRMADA", List.of(linea),
                new PagoResult("EFECTIVO", dec("12.50"), dec("20.00"), dec("7.50")));
    }
```

Agregar a `TEST/application/error/VentasErrorsTest.java` (importar `java.util.UUID`):

```java
    @Test
    void exposesTheVentaErrors() {
        var id = UUID.fromString("88888888-8888-4888-8888-888888888888");

        assertThat(VentasErrors.noHayTurnoAbierto().code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        assertThat(VentasErrors.noHayTurnoAbierto().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.almacenNoEncontrado().code()).isEqualTo("VEN_ALMACEN_NO_ENCONTRADO");
        assertThat(VentasErrors.almacenNoEncontrado().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.almacenNoOperable().code()).isEqualTo("VEN_ALMACEN_NO_OPERABLE");
        assertThat(VentasErrors.almacenNoOperable().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.almacenDeOtroEstablecimiento().code())
                .isEqualTo("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO");
        assertThat(VentasErrors.skuNoEncontrado(id).code()).isEqualTo("VEN_SKU_NO_ENCONTRADO");
        assertThat(VentasErrors.skuNoEncontrado(id).category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.skuNoEncontrado(id).metadata()).containsEntry("skuId", id);
        assertThat(VentasErrors.skuNoOperable(id).code()).isEqualTo("VEN_SKU_NO_OPERABLE");
        assertThat(VentasErrors.skuNoOperable(id).category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.skuNoOperable(id).metadata()).containsEntry("skuId", id);
        assertThat(VentasErrors.claveIdempotenciaInvalida().code()).isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(VentasErrors.claveIdempotenciaInvalida().category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(VentasErrors.conflictoIdempotencia().code()).isEqualTo("VEN_IDEMPOTENCY_CONFLICT");
        assertThat(VentasErrors.conflictoIdempotencia().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.ventaNoEncontrada().code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        assertThat(VentasErrors.ventaNoEncontrada().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.paginacionInvalida(-1, 500).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(VentasErrors.paginacionInvalida(-1, 500).category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(VentasErrors.paginacionInvalida(-1, 500).metadata())
                .containsEntry("page", -1).containsEntry("size", 500);
    }
```

`TEST/application/usecase/query/ConsultarVentasHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.query;

import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarVentasHandlerTest {

    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final ConsultarVentasHandler handler = new ConsultarVentasHandler(readPort);

    private static ListarVentasQuery query(int page, int size) {
        return new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), page, size);
    }

    private static ApplicationError error(Result<?, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void returnsASaleById() {
        when(readPort.findVenta(TENANT, VENTA)).thenReturn(Optional.of(ventaResult()));

        Result<VentaResult, ApplicationError> result = handler.obtener(new ObtenerVentaQuery(TENANT, VENTA));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(ventaResult());
    }

    @Test
    void aMissingSaleIsNotFound() {
        var error = error(handler.obtener(new ObtenerVentaQuery(TENANT, VENTA)));

        assertThat(error.code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void listsASalesPage() {
        var pagina = new PaginaResult<>(
                List.of(new VentaResumenResult(VENTA, "POS01-000001", TERMINAL, AHORA, dec("12.50"), "CONFIRMADA")),
                0, 20, 1L);
        when(readPort.listarVentas(query(0, 20))).thenReturn(pagina);

        var result = handler.listar(query(0, 20));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(pagina);
    }

    @Test
    void acceptsThePageSizeBoundaries() {
        when(readPort.listarVentas(query(0, 1))).thenReturn(new PaginaResult<>(List.of(), 0, 1, 0L));
        when(readPort.listarVentas(query(3, 100))).thenReturn(new PaginaResult<>(List.of(), 3, 100, 0L));

        assertThat(handler.listar(query(0, 1)).isSuccess()).isTrue();
        assertThat(handler.listar(query(3, 100)).isSuccess()).isTrue();
    }

    @Test
    void rejectsAnOutOfRangePagination() {
        assertThat(error(handler.listar(query(-1, 20))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(error(handler.listar(query(0, 0))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(error(handler.listar(query(0, 101))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
    }

    @Test
    void requiresItsCollaboratorAndTheQueries() {
        assertThatNullPointerException().isThrownBy(() -> new ConsultarVentasHandler(null));
        assertThatNullPointerException().isThrownBy(() -> handler.obtener(null));
        assertThatNullPointerException().isThrownBy(() -> handler.listar(null));
    }
}
```

`TEST/application/usecase/command/RegistrarVentaHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.OTRO_SKU;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.skuRef;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.AlmacenRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort.VentaExistente;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarVentaHandlerTest {

    private static final String CLAVE = "clave-1";

    private final VentaWritePort ventas = mock(VentaWritePort.class);
    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final SalidaInventarioPort inventario = mock(SalidaInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);
    private final RegistrarVentaHandler handler = new RegistrarVentaHandler(
            ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
            () -> AHORA);

    @BeforeEach
    void theContextIsValidAndEveryCollaboratorSucceedsByDefault() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, "POS01", true)));
        when(turnos.bloquearTurnoAbierto(TENANT, TERMINAL))
                .thenReturn(Optional.of(turno(EstadoTurno.ABIERTO, "50.00")));
        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, true)));
        when(referencias.skus(eq(TENANT), anyCollection()))
                .thenReturn(Map.of(SKU, skuRef(SKU, true), OTRO_SKU, skuRef(OTRO_SKU, true)));
        when(numeracion.siguienteNumeroOperacion(TENANT, TERMINAL, "POS01")).thenReturn("POS01-000001");
        when(ventas.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.GUARDADO);
        when(inventario.descontar(any())).thenAnswer(invocation -> {
            SalidaSolicitada solicitada = invocation.getArgument(0);
            return ok(List.of(new LoteConsumo(LOTE, solicitada.cantidad())));
        });
        when(consultas.obtener(any(ObtenerVentaQuery.class))).thenReturn(ok(ventaResult()));
    }

    private static LineaVentaInput linea(UUID sku, String cantidad, String precio) {
        return new LineaVentaInput(sku, cantidad == null ? null : dec(cantidad), dec(precio));
    }

    private static RegistrarVentaCommand command(String clave, String recibido, LineaVentaInput... lineas) {
        return new RegistrarVentaCommand(
                TENANT, ACTOR_ID, clave, TERMINAL, ALMACEN, List.of(lineas), recibido == null ? null : dec(recibido));
    }

    private static RegistrarVentaCommand unaLinea() {
        return command(CLAVE, "20", linea(SKU, "5", "2.50"));
    }

    private static ApplicationError error(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private static VentaResult value(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    @Test
    void registersTheSaleDiscountingTheStockAndRecordingTheConsumedLotes() {
        var result = value(handler.execute(unaLinea()));

        assertThat(result).isEqualTo(ventaResult());
        var ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventas).insertar(ventaCaptor.capture(), eq(CLAVE), any());
        var venta = ventaCaptor.getValue();
        assertThat(venta.tenantId()).isEqualTo(TENANT);
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedor().id()).isEqualTo(ACTOR_ID);
        assertThat(venta.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(venta.fechaVenta()).isEqualTo(AHORA);
        assertThat(venta.total()).isEqualTo(dec("12.50"));
        assertThat(venta.pago().vuelto()).isEqualTo(dec("7.50"));
        var linea = venta.lineas().getFirst();
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.unidadVentaCodigo()).isEqualTo("UND");
        var salidaCaptor = ArgumentCaptor.forClass(SalidaSolicitada.class);
        verify(inventario).descontar(salidaCaptor.capture());
        var salida = salidaCaptor.getValue();
        assertThat(salida.tenantId()).isEqualTo(TENANT);
        assertThat(salida.almacenId()).isEqualTo(ALMACEN);
        assertThat(salida.skuId()).isEqualTo(SKU);
        assertThat(salida.cantidad()).isEqualTo(dec("5"));
        assertThat(salida.ventaId()).isEqualTo(venta.id());
        assertThat(salida.ventaLineaId()).isEqualTo(linea.id());
        assertThat(salida.actorId()).isEqualTo(ACTOR_ID);
        assertThat(salida.idempotencyKey()).isEqualTo(CLAVE + ":1");
        verify(ventas).registrarLotes(TENANT, linea.id(), List.of(new LoteConsumo(LOTE, dec("5"))));
        verify(consultas).obtener(new ObtenerVentaQuery(TENANT, venta.id()));
    }

    @Test
    void discountsTheLinesInSkuOrderEvenWhenTheyArriveInAnotherOne() {
        value(handler.execute(command(CLAVE, "100", linea(OTRO_SKU, "1", "5"), linea(SKU, "1", "5"))));

        var orden = inOrder(inventario);
        orden.verify(inventario).descontar(argThat(salida -> salida.skuId().equals(SKU)
                && salida.idempotencyKey().equals(CLAVE + ":2")));
        orden.verify(inventario).descontar(argThat(salida -> salida.skuId().equals(OTRO_SKU)
                && salida.idempotencyKey().equals(CLAVE + ":1")));
    }

    @Test
    void aRetryWithTheSameKeyAndBodyReturnsTheStoredSaleWithoutTouchingTheStock() {
        value(handler.execute(unaLinea()));
        var huellaCaptor = ArgumentCaptor.forClass(String.class);
        verify(ventas).insertar(any(), eq(CLAVE), huellaCaptor.capture());
        when(ventas.findPorIdempotencia(TENANT, CLAVE))
                .thenReturn(Optional.of(new VentaExistente(VENTA, huellaCaptor.getValue())));
        clearInvocations(ventas, inventario, numeracion);

        var repetida = value(handler.execute(unaLinea()));

        assertThat(repetida).isEqualTo(ventaResult());
        verify(ventas, never()).insertar(any(), any(), any());
        verify(inventario, never()).descontar(any());
        verify(numeracion, never()).siguienteNumeroOperacion(any(), any(), any());
        verify(consultas).obtener(new ObtenerVentaQuery(TENANT, VENTA));
    }

    @Test
    void theSameKeyWithADifferentBodyIsAnIdempotencyConflict() {
        when(ventas.findPorIdempotencia(TENANT, CLAVE)).thenReturn(Optional.of(new VentaExistente(VENTA, "otra")));

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_IDEMPOTENCY_CONFLICT");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void theIdempotencyKeyIsRequiredAndAtMostOneHundredSixtyCharacters() {
        assertThat(error(handler.execute(command(null, "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(command("  ", "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(command("x".repeat(161), "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(handler.execute(command("x".repeat(160), "20", linea(SKU, "5", "2.50"))).isSuccess()).isTrue();
    }

    @Test
    void rejectsAnUnknownOrInoperableTerminal() {
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TERMINAL_NO_ENCONTRADA");

        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, "POS01", false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TERMINAL_NO_OPERABLE");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void aTerminalWithoutAnOpenTurnoCannotSell() {
        when(turnos.bloquearTurnoAbierto(TENANT, TERMINAL)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsAnUnknownInoperableOrForeignWarehouse() {
        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_NO_ENCONTRADO");

        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_NO_OPERABLE");

        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, UUID.randomUUID(), true)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsAnUnknownOrInoperableSku() {
        when(referencias.skus(eq(TENANT), anyCollection())).thenReturn(Map.of());
        var desconocido = error(handler.execute(unaLinea()));
        assertThat(desconocido.code()).isEqualTo("VEN_SKU_NO_ENCONTRADO");
        assertThat(desconocido.metadata()).containsEntry("skuId", SKU);

        when(referencias.skus(eq(TENANT), anyCollection())).thenReturn(Map.of(SKU, skuRef(SKU, false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_SKU_NO_OPERABLE");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsInvalidLinesAnEmptySaleAndAnInsufficientCashAmountWithoutInserting() {
        assertThat(error(handler.execute(command(CLAVE, "20", linea(SKU, "0", "2.50")))).code())
                .isEqualTo("VEN_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(command(CLAVE, "20", linea(SKU, "0.5", "2.50")))).code())
                .isEqualTo("VEN_FRACCION_NO_PERMITIDA");
        assertThat(error(handler.execute(command(CLAVE, "20"))).code()).isEqualTo("VEN_VENTA_SIN_LINEAS");
        assertThat(error(handler.execute(command(CLAVE, "5", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_MONTO_RECIBIDO_INSUFICIENTE");
        assertThat(error(handler.execute(command(CLAVE, null, linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_MONTO_INVALIDO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void anInventoryFailureStopsTheSaleAndPassesTheErrorThrough() {
        var sinStock = new StandardApplicationError("INV_STOCK_INSUFICIENTE", "Sin stock.", ErrorCategory.CONFLICT);
        when(inventario.descontar(any()))
                .thenReturn(ok(List.of(new LoteConsumo(LOTE, dec("1")))))
                .thenReturn(Result.failure(sinStock));

        var failure = error(handler.execute(command(CLAVE, "100", linea(SKU, "1", "5"), linea(OTRO_SKU, "1", "5"))));

        assertThat(failure).isSameAs(sinStock);
        verify(inventario, times(2)).descontar(any());
        verify(ventas, times(1)).registrarLotes(any(), any(), any());
    }

    @Test
    void aConcurrentModificationRetriesTheWholeSale() {
        when(inventario.descontar(any()))
                .thenReturn(Result.<List<LoteConsumo>, ApplicationError>failure(VentasErrors.modificacionConcurrente()))
                .thenReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))));

        var result = value(handler.execute(unaLinea()));

        assertThat(result).isEqualTo(ventaResult());
        verify(ventas, times(2)).insertar(any(), any(), any());
        verify(inventario, times(2)).descontar(any());
    }

    @Test
    void losingTheIdempotencyRaceThreeTimesIsReportedAsAConcurrentModification() {
        when(ventas.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.DUPLICADO);

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo(VentasErrors.CONCURRENCIA);
        verify(ventas, times(3)).insertar(any(), any(), any());
        verify(inventario, never()).descontar(any());
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                null, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, null, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, null, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, null, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, null, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, null, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, null, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, null,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (puertos, DTOs y handlers de venta no existen).

- [ ] **Step 3: Implementar**

`MAIN/application/error/VentasErrors.java` — agregar imports (`java.util.Map`, `java.util.UUID`) y los métodos antes de `notFound`:

```java
    public static ApplicationError noHayTurnoAbierto() {
        return conflict("VEN_TURNO_NO_ABIERTO", "La terminal no tiene un turno abierto.");
    }

    public static ApplicationError almacenNoEncontrado() {
        return notFound("VEN_ALMACEN_NO_ENCONTRADO", "El almacen indicado no existe.");
    }

    public static ApplicationError almacenNoOperable() {
        return conflict("VEN_ALMACEN_NO_OPERABLE", "El almacen debe estar activo, controlar lotes y permitir venta.");
    }

    public static ApplicationError almacenDeOtroEstablecimiento() {
        return conflict("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO",
                "El almacen no pertenece al establecimiento de la terminal.");
    }

    public static ApplicationError skuNoEncontrado(UUID skuId) {
        return new StandardApplicationError(
                "VEN_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND,
                Map.of("skuId", skuId));
    }

    public static ApplicationError skuNoOperable(UUID skuId) {
        return new StandardApplicationError(
                "VEN_SKU_NO_OPERABLE", "El SKU no esta activo comercialmente.", ErrorCategory.CONFLICT,
                Map.of("skuId", skuId));
    }

    public static ApplicationError claveIdempotenciaInvalida() {
        return new StandardApplicationError(
                "VEN_IDEMPOTENCY_KEY_INVALID", "La cabecera Idempotency-Key debe tener entre 1 y 160 caracteres.",
                ErrorCategory.VALIDATION);
    }

    public static ApplicationError conflictoIdempotencia() {
        return conflict("VEN_IDEMPOTENCY_CONFLICT", "La clave de idempotencia ya se uso con una solicitud distinta.");
    }

    public static ApplicationError ventaNoEncontrada() {
        return notFound("VEN_VENTA_NO_ENCONTRADA", "La venta indicada no existe.");
    }

    public static ApplicationError paginacionInvalida(int page, int size) {
        return new StandardApplicationError(
                "VEN_PAGINACION_INVALIDA", "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                ErrorCategory.VALIDATION, Map.of("page", page, "size", size));
    }
```

Además, en `VentasErrors` agregar el overload que traduce un `Result` de dominio:

```java
    public static <T> Result<T, ApplicationError> fromDomain(Result<T, ErrorDetail> result) {
        return result.mapError(VentasErrors::fromDomain);
    }
```
(importar `com.softprimesolutions.shared.kernel.result.Result`). Cubrirlo con un test en `VentasErrorsTest`:

```java
    @Test
    void translatesADomainResultKeepingSuccessesAndMappingFailures() {
        Result<String, ApplicationError> ok = VentasErrors.fromDomain(Result.<String, ErrorDetail>success("x"));
        Result<String, ApplicationError> falla = VentasErrors.fromDomain(
                Result.<String, ErrorDetail>failure(new ErrorDetail(VentasErrorCodes.MONTO_INVALIDO, "m", Map.of())));

        assertThat(ok.fold(value -> value, error -> null)).isEqualTo("x");
        assertThat(falla.fold(value -> null, error -> error.code())).isEqualTo("VEN_MONTO_INVALIDO");
    }
```
(importar `com.softprimesolutions.shared.application.error.ApplicationError` y `com.softprimesolutions.shared.kernel.result.Result` en el test).

`MAIN/application/dto/command/LineaVentaInput.java`:

```java
package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaVentaInput(UUID skuId, BigDecimal cantidad, BigDecimal precioUnitario) {
}
```

`MAIN/application/dto/command/RegistrarVentaCommand.java`:

```java
package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegistrarVentaCommand(
        UUID tenantId,
        UUID actorId,
        String idempotencyKey,
        UUID terminalId,
        UUID almacenId,
        List<LineaVentaInput> lineas,
        BigDecimal montoRecibido) {
}
```

`MAIN/application/dto/query/ObtenerVentaQuery.java`:

```java
package com.softprimesolutions.ventas.application.dto.query;

import java.util.UUID;

public record ObtenerVentaQuery(UUID tenantId, UUID ventaId) {
}
```

`MAIN/application/dto/query/ListarVentasQuery.java`:

```java
package com.softprimesolutions.ventas.application.dto.query;

import java.time.Instant;
import java.util.UUID;

public record ListarVentasQuery(
        UUID tenantId, UUID establecimientoId, Instant desde, Instant hasta, int page, int size) {
}
```

`MAIN/application/dto/result/LoteConsumidoResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumidoResult(UUID loteId, BigDecimal cantidad) {
}
```

`MAIN/application/dto/result/LineaVentaResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineaVentaResult(
        int numeroLinea,
        UUID skuId,
        String descripcion,
        String unidadVentaCodigo,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalLinea,
        List<LoteConsumidoResult> lotes) {
}
```

`MAIN/application/dto/result/PagoResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;

public record PagoResult(String medioPago, BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {
}
```

`MAIN/application/dto/result/VentaResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VentaResult(
        UUID id,
        String numeroOperacion,
        UUID terminalId,
        UUID turnoId,
        UUID establecimientoId,
        UUID vendedorId,
        Instant fechaVenta,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal total,
        String estado,
        List<LineaVentaResult> lineas,
        PagoResult pago) {
}
```

`MAIN/application/dto/result/VentaResumenResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VentaResumenResult(
        UUID id, String numeroOperacion, UUID terminalId, Instant fechaVenta, BigDecimal total, String estado) {
}
```

`MAIN/application/dto/result/PaginaResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.util.List;

public record PaginaResult<T>(List<T> items, int page, int size, long totalElements) {
}
```

`MAIN/application/port/in/RegistrarVentaUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;

@FunctionalInterface
public interface RegistrarVentaUseCase {
    Result<VentaResult, ApplicationError> execute(RegistrarVentaCommand command);
}
```

`MAIN/application/port/in/ConsultarVentasUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;

public interface ConsultarVentasUseCase {

    Result<VentaResult, ApplicationError> obtener(ObtenerVentaQuery query);

    Result<PaginaResult<VentaResumenResult>, ApplicationError> listar(ListarVentasQuery query);
}
```

`MAIN/application/port/out/VentaWritePort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaWritePort {

    Optional<VentaExistente> findPorIdempotencia(UUID tenantId, String idempotencyKey);

    GuardadoOutcome insertar(Venta venta, String idempotencyKey, String huella);

    void registrarLotes(UUID tenantId, UUID ventaLineaId, List<LoteConsumo> lotes);

    record VentaExistente(UUID id, String huella) {
    }
}
```

`MAIN/application/port/out/NumeracionPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import java.util.UUID;

@FunctionalInterface
public interface NumeracionPort {

    String siguienteNumeroOperacion(UUID tenantId, UUID terminalId, String codigoTerminal);
}
```

`MAIN/application/port/out/SalidaInventarioPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface SalidaInventarioPort {

    Result<List<LoteConsumo>, ApplicationError> descontar(SalidaSolicitada solicitud);

    record SalidaSolicitada(
            UUID tenantId,
            UUID almacenId,
            UUID skuId,
            BigDecimal cantidad,
            UUID ventaId,
            UUID ventaLineaId,
            UUID actorId,
            String idempotencyKey) {
    }
}
```

`MAIN/application/port/out/TurnoWritePort.java` — agregar la declaración:

```java
    Optional<TurnoCaja> bloquearTurnoAbierto(UUID tenantId, UUID terminalId);
```

`MAIN/application/port/out/ReferenciasVentasPort.java` (reemplazar completo):

```java
package com.softprimesolutions.ventas.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReferenciasVentasPort {

    Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId);

    Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId);

    Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds);

    record TerminalRef(UUID id, UUID establecimientoId, String codigo, boolean operable) {
    }

    record AlmacenRef(UUID id, UUID establecimientoId, boolean operable) {
    }

    record SkuVentaRef(
            UUID id, String descripcion, String unidadVentaCodigo, boolean permiteFraccion, boolean operable) {
    }
}
```

`MAIN/application/port/out/VentasReadPort.java` (reemplazar completo):

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import java.util.Optional;
import java.util.UUID;

public interface VentasReadPort {

    Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId);

    Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId);

    Optional<VentaResult> findVenta(UUID tenantId, UUID ventaId);

    PaginaResult<VentaResumenResult> listarVentas(ListarVentasQuery query);
}
```

`MAIN/application/usecase/query/ConsultarVentasHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.query;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;

public final class ConsultarVentasHandler implements ConsultarVentasUseCase {

    private static final int TAMANO_MAXIMO = 100;

    private final VentasReadPort readPort;

    public ConsultarVentasHandler(VentasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> obtener(ObtenerVentaQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return readPort.findVenta(query.tenantId(), query.ventaId())
                .<Result<VentaResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(VentasErrors.ventaNoEncontrada()));
    }

    @Override
    public Result<PaginaResult<VentaResumenResult>, ApplicationError> listar(ListarVentasQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        if (query.page() < 0 || query.size() < 1 || query.size() > TAMANO_MAXIMO) {
            return Result.failure(VentasErrors.paginacionInvalida(query.page(), query.size()));
        }
        return Result.success(readPort.listarVentas(query));
    }
}
```

`MAIN/application/usecase/command/RegistrarVentaHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.AlmacenRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.SkuVentaRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort.VentaExistente;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.model.Venta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RegistrarVentaHandler implements RegistrarVentaUseCase {

    private static final int MAX_INTENTOS = 3;
    private static final int CLAVE_IDEMPOTENCIA_MAX = 160;

    private final VentaWritePort ventas;
    private final TurnoWritePort turnos;
    private final ReferenciasVentasPort referencias;
    private final SalidaInventarioPort inventario;
    private final NumeracionPort numeracion;
    private final ConsultarVentasUseCase consultas;
    private final TransaccionPort transaccion;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public RegistrarVentaHandler(
            VentaWritePort ventas, TurnoWritePort turnos, ReferenciasVentasPort referencias,
            SalidaInventarioPort inventario, NumeracionPort numeracion, ConsultarVentasUseCase consultas,
            TransaccionPort transaccion, IdentifierGenerator identifiers, ClockPort clock) {
        this.ventas = Objects.requireNonNull(ventas, "ventas es obligatorio");
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.inventario = Objects.requireNonNull(inventario, "inventario es obligatorio");
        this.numeracion = Objects.requireNonNull(numeracion, "numeracion es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(RegistrarVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (claveInvalida(command.idempotencyKey())) {
            return Result.failure(VentasErrors.claveIdempotenciaInvalida());
        }
        var solicitud = new Solicitud(command, huella(command));
        var resultado = intentar(solicitud);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(solicitud);
        }
        return resultado;
    }

    private Result<VentaResult, ApplicationError> intentar(Solicitud solicitud) {
        var command = solicitud.command();
        var previa = ventas.findPorIdempotencia(command.tenantId(), command.idempotencyKey());
        if (previa.isPresent()) return repetir(command, previa.get(), solicitud.huella());
        return transaccion.ejecutar(() -> registrar(solicitud))
                .flatMap(ventaId -> consultas.obtener(new ObtenerVentaQuery(command.tenantId(), ventaId)));
    }

    private Result<VentaResult, ApplicationError> repetir(
            RegistrarVentaCommand command, VentaExistente previa, String huella) {
        if (!previa.huella().equals(huella)) return Result.failure(VentasErrors.conflictoIdempotencia());
        return consultas.obtener(new ObtenerVentaQuery(command.tenantId(), previa.id()));
    }

    private Result<UUID, ApplicationError> registrar(Solicitud solicitud) {
        var command = solicitud.command();
        return validarContexto(command)
                .flatMap(contexto -> construir(command, contexto))
                .flatMap(venta -> persistir(solicitud, venta));
    }

    private Result<Contexto, ApplicationError> validarContexto(RegistrarVentaCommand command) {
        var terminal = referencias.terminal(command.tenantId(), command.terminalId());
        if (terminal.isEmpty()) return Result.failure(VentasErrors.terminalNoEncontrada());
        if (!terminal.get().operable()) return Result.failure(VentasErrors.terminalNoOperable());
        var turno = turnos.bloquearTurnoAbierto(command.tenantId(), command.terminalId());
        if (turno.isEmpty()) return Result.failure(VentasErrors.noHayTurnoAbierto());
        var almacen = referencias.almacen(command.tenantId(), command.almacenId());
        if (almacen.isEmpty()) return Result.failure(VentasErrors.almacenNoEncontrado());
        var errorAlmacen = errorDeAlmacen(almacen.get(), terminal.get());
        if (errorAlmacen.isPresent()) return Result.failure(errorAlmacen.get());
        var skus = referencias.skus(
                command.tenantId(),
                command.lineas().stream().map(LineaVentaInput::skuId).collect(Collectors.toSet()));
        var errorSku = command.lineas().stream()
                .map(linea -> errorDeSku(linea.skuId(), skus.get(linea.skuId())))
                .flatMap(Optional::stream)
                .findFirst();
        if (errorSku.isPresent()) return Result.failure(errorSku.get());
        return Result.success(new Contexto(terminal.get(), turno.get(), skus));
    }

    private static Optional<ApplicationError> errorDeAlmacen(AlmacenRef almacen, TerminalRef terminal) {
        if (!almacen.operable()) return Optional.of(VentasErrors.almacenNoOperable());
        if (!almacen.establecimientoId().equals(terminal.establecimientoId())) {
            return Optional.of(VentasErrors.almacenDeOtroEstablecimiento());
        }
        return Optional.empty();
    }

    private static Optional<ApplicationError> errorDeSku(UUID skuId, SkuVentaRef sku) {
        if (sku == null) return Optional.of(VentasErrors.skuNoEncontrado(skuId));
        if (!sku.operable()) return Optional.of(VentasErrors.skuNoOperable(skuId));
        return Optional.empty();
    }

    private Result<Venta, ApplicationError> construir(RegistrarVentaCommand command, Contexto contexto) {
        return lineas(command, contexto.skus()).flatMap(lineas -> VentasErrors.fromDomain(Venta.registrar(
                identifiers.next(), command.tenantId(), contexto.terminal().id(), contexto.turno().id(),
                contexto.terminal().establecimientoId(), new Actor(command.actorId()),
                numeracion.siguienteNumeroOperacion(
                        command.tenantId(), contexto.terminal().id(), contexto.terminal().codigo()),
                clock.now(), lineas, command.montoRecibido())));
    }

    private Result<List<LineaVenta>, ApplicationError> lineas(
            RegistrarVentaCommand command, Map<UUID, SkuVentaRef> skus) {
        Result<List<LineaVenta>, ApplicationError> acumuladas = Result.success(List.of());
        for (var indice = 0; indice < command.lineas().size(); indice++) {
            var entrada = command.lineas().get(indice);
            var numero = indice + 1;
            acumuladas = acumuladas.flatMap(previas -> linea(numero, entrada, skus.get(entrada.skuId()))
                    .map(nueva -> Stream.concat(previas.stream(), Stream.of(nueva)).toList()));
        }
        return acumuladas;
    }

    private Result<LineaVenta, ApplicationError> linea(int numero, LineaVentaInput entrada, SkuVentaRef sku) {
        return VentasErrors.fromDomain(LineaVenta.nueva(
                identifiers.next(), numero, entrada.skuId(), sku.descripcion(), sku.unidadVentaCodigo(),
                sku.permiteFraccion(), entrada.cantidad(), entrada.precioUnitario()));
    }

    private Result<UUID, ApplicationError> persistir(Solicitud solicitud, Venta venta) {
        var command = solicitud.command();
        if (ventas.insertar(venta, command.idempotencyKey(), solicitud.huella()) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(VentasErrors.modificacionConcurrente());
        }
        Result<UUID, ApplicationError> acumulado = Result.success(venta.id());
        for (var linea : enOrdenDeBloqueo(venta)) {
            acumulado = acumulado.flatMap(ventaId -> descontar(command, venta, linea).map(lotes -> ventaId));
        }
        return acumulado;
    }

    private static List<LineaVenta> enOrdenDeBloqueo(Venta venta) {
        return venta.lineas().stream()
                .sorted(Comparator.comparing(LineaVenta::skuId).thenComparingInt(LineaVenta::numeroLinea))
                .toList();
    }

    private Result<List<LoteConsumo>, ApplicationError> descontar(
            RegistrarVentaCommand command, Venta venta, LineaVenta linea) {
        return inventario.descontar(new SalidaSolicitada(
                        command.tenantId(), command.almacenId(), linea.skuId(), linea.cantidad(), venta.id(),
                        linea.id(), command.actorId(), command.idempotencyKey() + ":" + linea.numeroLinea()))
                .map(lotes -> {
                    ventas.registrarLotes(command.tenantId(), linea.id(), lotes);
                    return lotes;
                });
    }

    private static boolean esConcurrencia(Result<VentaResult, ApplicationError> resultado) {
        return resultado.fold(venta -> false, error -> VentasErrors.CONCURRENCIA.equals(error.code()));
    }

    private static boolean claveInvalida(String clave) {
        return clave == null || clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX;
    }

    private static String huella(RegistrarVentaCommand command) {
        var lineas = command.lineas().stream()
                .map(linea -> String.join(":", text(linea.skuId()), plano(linea.cantidad()), plano(linea.precioUnitario())))
                .collect(Collectors.joining(","));
        var canonico = String.join("|",
                text(command.terminalId()), text(command.almacenId()), lineas, plano(command.montoRecibido()),
                text(command.actorId()));
        return UUID.nameUUIDFromBytes(canonico.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String plano(BigDecimal valor) {
        return Optional.ofNullable(valor).map(numero -> numero.stripTrailingZeros().toPlainString()).orElse("");
    }

    private static String text(Object valor) {
        return Objects.toString(valor, "");
    }

    private record Contexto(TerminalRef terminal, TurnoCaja turno, Map<UUID, SkuVentaRef> skus) {
    }

    private record Solicitud(RegistrarVentaCommand command, String huella) {
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: PASS (dominio + aplicación de turno y venta).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): casos de uso de registro y consulta de ventas

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Infraestructura de la venta

**Files:**
- Modify: `service-botica/modules/ventas/build.gradle`
- Modify: `MAIN/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapter.java`, `ReferenciasVentasJdbcAdapter.java`
- Modify: `MAIN/infrastructure/persistence/read/adapter/VentasJdbcReadAdapter.java`
- Modify: `MAIN/infrastructure/configuration/VentasModuleConfiguration.java`
- Create: `MAIN/infrastructure/persistence/write/adapter/VentaJdbcWriteAdapter.java`, `NumeracionJdbcAdapter.java`
- Create: `MAIN/infrastructure/client/InventarioSalidaAdapter.java`
- Modify (soporte de tests): `TEST/infrastructure/persistence/Rows.java`
- Test: `TEST/infrastructure/persistence/write/adapter/VentaJdbcWriteAdapterTest.java`, `NumeracionJdbcAdapterTest.java`, `TurnoJdbcWriteAdapterTest.java`, `ReferenciasVentasJdbcAdapterTest.java`, `TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java`, `TEST/infrastructure/client/InventarioSalidaAdapterTest.java`, `TEST/infrastructure/configuration/VentasModuleConfigurationTest.java`

**Interfaces:**
- Consumes: puertos de la Task 2; `SalidaInventarioApi`, `SalidaVentaSolicitud`, `SalidaVentaRegistrada`, `LoteConsumido` de `inventario::api`.
- Produces: adapters para `VentaWritePort`, `NumeracionPort`, `SalidaInventarioPort`; `TurnoJdbcWriteAdapter#bloquearTurnoAbierto`; `ReferenciasVentasJdbcAdapter#almacen/#skus`; `VentasJdbcReadAdapter#findVenta/#listarVentas`; beans `registrarVentaUseCase` y `consultarVentasUseCase`.

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `TEST/infrastructure/persistence/Rows.java` (importar `ALMACEN`, `LINEA`, `LOTE`, `SKU`, `VENTA` desde `VentasFixtures`):

```java
    public static Map<String, Object> ventaCabecera() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("numero_operacion", "POS01-000001");
        row.put("terminal_uuid", TERMINAL);
        row.put("turno_uuid", TURNO);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("vendedor_uuid", ACTOR_ID);
        row.put("fecha_venta", MOMENTO);
        row.put("moneda", "PEN");
        row.put("subtotal", dec("12.50"));
        row.put("descuento_total", dec("0.00"));
        row.put("impuesto_total", dec("0.00"));
        row.put("total", dec("12.50"));
        row.put("estado", "CONFIRMADA");
        return row;
    }

    public static Map<String, Object> ventaLinea() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", LINEA);
        row.put("numero_linea", 1);
        row.put("sku_uuid", SKU);
        row.put("descripcion_snapshot", "Paracetamol 500 mg");
        row.put("unidad_venta_codigo", "UND");
        row.put("cantidad", dec("5.0000"));
        row.put("precio_unitario", dec("2.5000"));
        row.put("total_linea", dec("12.50"));
        return row;
    }

    public static Map<String, Object> ventaLote() {
        var row = new HashMap<String, Object>();
        row.put("linea_uuid", LINEA);
        row.put("lote_uuid", LOTE);
        row.put("cantidad", dec("5.0000"));
        return row;
    }

    public static Map<String, Object> ventaPago() {
        var row = new HashMap<String, Object>();
        row.put("medio_codigo", "EFECTIVO");
        row.put("monto", dec("12.50"));
        row.put("monto_recibido", dec("20.00"));
        row.put("vuelto", dec("7.50"));
        return row;
    }

    public static Map<String, Object> ventaResumen() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("numero_operacion", "POS01-000001");
        row.put("terminal_uuid", TERMINAL);
        row.put("fecha_venta", MOMENTO);
        row.put("total", dec("12.50"));
        row.put("estado", "CONFIRMADA");
        return row;
    }
```

`TEST/infrastructure/persistence/write/adapter/NumeracionJdbcAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import org.junit.jupiter.api.Test;

class NumeracionJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final NumeracionJdbcAdapter adapter = new NumeracionJdbcAdapter(jdbc.client());

    @Test
    void formatsTheNextTerminalSequenceWithSixDigits() {
        jdbc.scalar("ON CONFLICT (tenant_id, terminal_id)", 7L);

        assertThat(adapter.siguienteNumeroOperacion(TENANT, TERMINAL, "POS01")).isEqualTo("POS01-000007");
        assertThat(jdbc.statementContaining("ON CONFLICT (tenant_id, terminal_id)").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }
}
```

`TEST/infrastructure/persistence/write/adapter/VentaJdbcWriteAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.venta;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class VentaJdbcWriteAdapterTest {

    private static final String INSERT_VENTA = "INSERT INTO sch_venta.venta\n";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final VentaJdbcWriteAdapter adapter =
            new VentaJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void findsASaleByItsIdempotencyKeyWithItsRequestFingerprint() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("huella_solicitud", "huella-1");
        jdbc.rows("v.idempotency_key = :idempotencyKey", row);

        var existente = adapter.findPorIdempotencia(TENANT, "clave-1").orElseThrow();

        assertThat(existente.id()).isEqualTo(VENTA);
        assertThat(existente.huella()).isEqualTo("huella-1");
        assertThat(jdbc.statementContaining("v.idempotency_key = :idempotencyKey").params())
                .containsEntry("tenantId", TENANT).containsEntry("idempotencyKey", "clave-1");
    }

    @Test
    void aMissingKeyIsEmpty() {
        assertThat(adapter.findPorIdempotencia(TENANT, "clave-1")).isEmpty();
    }

    @Test
    void insertsTheSaleItsLinesAndTheCashPaymentEnsuringTheCashMethod() {
        var outcome = adapter.insertar(venta(), "clave-1", "huella-1");

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        var cabecera = jdbc.statementContaining(INSERT_VENTA).params();
        assertThat(cabecera)
                .containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO)
                .containsEntry("vendedorId", ACTOR_ID).containsEntry("numero", "POS01-000001")
                .containsEntry("idempotencyKey", "clave-1").containsEntry("huella", "huella-1")
                .containsEntry("fecha", MOMENTO).containsEntry("subtotal", dec("12.50"))
                .containsEntry("total", dec("12.50")).containsEntry("actor", ACTOR.codigo());
        var linea = jdbc.statementContaining("INSERT INTO sch_venta.venta_linea\n").params();
        assertThat(linea)
                .containsEntry("lineaId", LINEA).containsEntry("ventaId", VENTA).containsEntry("numeroLinea", 1)
                .containsEntry("skuId", SKU).containsEntry("descripcion", "Paracetamol 500 mg")
                .containsEntry("unidad", "UND").containsEntry("esFraccion", false)
                .containsEntry("cantidad", dec("5")).containsEntry("precio", dec("2.50"))
                .containsEntry("totalLinea", dec("12.50"));
        assertThat(jdbc.statementContaining("INSERT INTO sch_venta.medio_pago").params())
                .containsEntry("tenantId", TENANT).containsEntry("actor", ACTOR.codigo());
        var pago = jdbc.statementContaining("INSERT INTO sch_venta.pago_venta").params();
        assertThat(pago)
                .containsEntry("ventaId", VENTA).containsEntry("monto", dec("12.50"))
                .containsEntry("montoRecibido", dec("20.00")).containsEntry("vuelto", dec("7.50"))
                .containsEntry("fecha", MOMENTO);
    }

    @Test
    void aUniqueViolationOrASaleThatInsertsNoRowIsReportedAsDuplicate() {
        jdbc.failsWith(INSERT_VENTA, new DuplicateKeyException("uk_venta_idempotency"));
        assertThat(adapter.insertar(venta(), "clave-1", "huella-1")).isEqualTo(GuardadoOutcome.DUPLICADO);

        var sinFilas = new JdbcClientStub().updates(INSERT_VENTA, 0);
        var otro = new VentaJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());
        assertThat(otro.insertar(venta(), "clave-1", "huella-1")).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void recordsEachConsumedLoteOfALine() {
        adapter.registrarLotes(TENANT, LINEA, List.of(new LoteConsumo(LOTE, dec("5"))));

        assertThat(jdbc.statementContaining("INSERT INTO sch_venta.venta_linea_lote").params())
                .containsEntry("lineaId", LINEA).containsEntry("loteId", LOTE).containsEntry("cantidad", dec("5"))
                .containsEntry("tenantId", TENANT);
    }

    @Test
    void failsLoudlyWhenALoteRowCannotBeInserted() {
        var sinFilas = new JdbcClientStub().updates("INSERT INTO sch_venta.venta_linea_lote", 0);
        var otro = new VentaJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());

        assertThatThrownBy(() -> otro.registrarLotes(TENANT, LINEA, List.of(new LoteConsumo(LOTE, dec("5")))))
                .isInstanceOf(RuntimeException.class);
    }
}
```

Agregar a `TEST/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapterTest.java`:

```java
    @Test
    void locksTheOpenTurnoOfATerminalForShareSoACloseWaitsForInFlightSales() {
        jdbc.rows("FOR SHARE OF tc", Rows.turno());

        var turno = adapter.bloquearTurnoAbierto(TENANT, TERMINAL).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        var statement = jdbc.statementContaining("FOR SHARE OF tc");
        assertThat(statement.sql()).contains("tc.estado = 'ABIERTO'");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
        assertThat(new TurnoJdbcWriteAdapter(new JdbcClientStub().client(), TransactionOperations.withoutTransaction())
                .bloquearTurnoAbierto(TENANT, TERMINAL)).isEmpty();
    }
```

Agregar a `TEST/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapterTest.java` (importar `ALMACEN`, `SKU`, `OTRO_SKU`, `java.util.List`):

```java
    @Test
    void findsAWarehouseWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ALMACEN);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("operable", true);
        jdbc.rows("FROM sch_organizacion.almacen a", row);

        var almacen = adapter.almacen(TENANT, ALMACEN).orElseThrow();

        assertThat(almacen.id()).isEqualTo(ALMACEN);
        assertThat(almacen.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(almacen.operable()).isTrue();
        var statement = jdbc.statementContaining("FROM sch_organizacion.almacen a");
        assertThat(statement.sql()).contains("a.permite_lotes AND a.permite_venta");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("almacenId", ALMACEN);
        assertThat(new ReferenciasVentasJdbcAdapter(new JdbcClientStub().client()).almacen(TENANT, ALMACEN)).isEmpty();
    }

    @Test
    void findsTheSellableDataOfTheSkusIndexedById() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", SKU);
        row.put("descripcion_comercial", "Paracetamol 500 mg");
        row.put("unidad_venta_codigo", "UND");
        row.put("permite_venta_fraccion", true);
        row.put("operable", true);
        jdbc.rows("FROM sch_catalogo.sku_comercial k", row);

        var skus = adapter.skus(TENANT, List.of(SKU, OTRO_SKU));

        assertThat(skus).containsOnlyKeys(SKU);
        var sku = skus.get(SKU);
        assertThat(sku.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(sku.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(sku.permiteFraccion()).isTrue();
        assertThat(sku.operable()).isTrue();
        assertThat(jdbc.statementContaining("FROM sch_catalogo.sku_comercial k").params())
                .containsEntry("tenantId", TENANT).containsEntry("skuIds", List.of(SKU, OTRO_SKU));
    }

    @Test
    void noSkusMeansNoQuery() {
        assertThat(adapter.skus(TENANT, List.of())).isEmpty();
        assertThat(jdbc.statements()).isEmpty();
    }
```

Agregar a `TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java` (importar `VENTA`, `LINEA`, `LOTE`, `SKU`, `AHORA`, `ListarVentasQuery`, `java.util.List`):

```java
    private static final String CABECERA = "tc.uuid_publico AS turno_uuid";
    private static final String LINEAS = "FROM sch_venta.venta_linea vl";
    private static final String LOTES = "FROM sch_venta.venta_linea_lote vll";
    private static final String PAGO = "FROM sch_venta.pago_venta p";

    @Test
    void assemblesASaleWithItsLinesLotesAndPayment() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(LOTES, Rows.ventaLote());
        jdbc.rows(PAGO, Rows.ventaPago());

        var venta = adapter.findVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedorId()).isEqualTo(ACTOR_ID);
        assertThat(venta.moneda()).isEqualTo("PEN");
        assertThat(venta.total()).isEqualTo(dec("12.50"));
        assertThat(venta.estado()).isEqualTo("CONFIRMADA");
        assertThat(venta.lineas()).hasSize(1);
        var linea = venta.lineas().getFirst();
        assertThat(linea.numeroLinea()).isEqualTo(1);
        assertThat(linea.skuId()).isEqualTo(SKU);
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.totalLinea()).isEqualTo(dec("12.50"));
        assertThat(linea.lotes()).hasSize(1);
        assertThat(linea.lotes().getFirst().loteId()).isEqualTo(LOTE);
        assertThat(linea.lotes().getFirst().cantidad()).isEqualTo(dec("5.0000"));
        assertThat(venta.pago().medioPago()).isEqualTo("EFECTIVO");
        assertThat(venta.pago().vuelto()).isEqualTo(dec("7.50"));
        assertThat(jdbc.statementContaining(CABECERA).params())
                .containsEntry("tenantId", TENANT).containsEntry("ventaId", VENTA);
    }

    @Test
    void aLineWithoutLotesKeepsAnEmptyList() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        assertThat(adapter.findVenta(TENANT, VENTA).orElseThrow().lineas().getFirst().lotes()).isEmpty();
    }

    @Test
    void aMissingSaleIsEmptyWithoutLoadingItsDetail() {
        assertThat(adapter.findVenta(TENANT, VENTA)).isEmpty();
        assertThat(jdbc.statements()).hasSize(1);
    }

    @Test
    void listsTheSalesPageWithItsFiltersAndTotal() {
        jdbc.scalar("SELECT COUNT(*)", 3L);
        jdbc.rows("ORDER BY v.fecha_venta DESC", Rows.ventaResumen());
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 2, 10);

        var pagina = adapter.listarVentas(query);

        assertThat(pagina.page()).isEqualTo(2);
        assertThat(pagina.size()).isEqualTo(10);
        assertThat(pagina.totalElements()).isEqualTo(3L);
        assertThat(pagina.items()).hasSize(1);
        assertThat(pagina.items().getFirst().numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(pagina.items().getFirst().total()).isEqualTo(dec("12.50"));
        var lista = jdbc.statementContaining("ORDER BY v.fecha_venta DESC").params();
        assertThat(lista).containsEntry("tenantId", TENANT).containsEntry("establecimientoId", ESTABLECIMIENTO)
                .containsEntry("desde", Rows.MOMENTO).containsEntry("hasta", Rows.MOMENTO.plusSeconds(60))
                .containsEntry("limit", 10).containsEntry("offset", 20);
    }

    @Test
    void listingWithoutFiltersPassesNullDates() {
        jdbc.scalar("SELECT COUNT(*)", 0L);

        var pagina = adapter.listarVentas(new ListarVentasQuery(TENANT, null, null, null, 0, 20));

        assertThat(pagina.items()).isEmpty();
        assertThat(jdbc.statementContaining("ORDER BY v.fecha_venta DESC").params())
                .containsEntry("desde", null).containsEntry("hasta", null);
    }
```

`TEST/infrastructure/client/InventarioSalidaAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.client;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.LoteConsumido;
import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaRegistrada;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventarioSalidaAdapterTest {

    private static final SalidaSolicitada SOLICITUD = new SalidaSolicitada(
            TENANT, ALMACEN, SKU, dec("5"), VENTA, LINEA, ACTOR_ID, "clave-1:1");

    @Test
    void translatesTheRequestAndMapsTheConsumedLotes() {
        var recibida = new AtomicReference<SalidaVentaSolicitud>();
        var adapter = new InventarioSalidaAdapter(solicitud -> {
            recibida.set(solicitud);
            return Result.success(new SalidaVentaRegistrada(List.of(
                    new LoteConsumido(UUID.randomUUID(), LOTE, dec("3"), dec("7")),
                    new LoteConsumido(UUID.randomUUID(), UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                            dec("2"), dec("8")))));
        });

        var lotes = adapter.descontar(SOLICITUD).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(lotes).containsExactly(
                new LoteConsumo(LOTE, dec("3")),
                new LoteConsumo(UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"), dec("2")));
        var solicitud = recibida.get();
        assertThat(solicitud.tenantId()).isEqualTo(TENANT);
        assertThat(solicitud.almacenId()).isEqualTo(ALMACEN);
        assertThat(solicitud.skuId()).isEqualTo(SKU);
        assertThat(solicitud.cantidad()).isEqualTo(dec("5"));
        assertThat(solicitud.ventaId()).isEqualTo(VENTA);
        assertThat(solicitud.ventaLineaId()).isEqualTo(LINEA);
        assertThat(solicitud.actorId()).isEqualTo(ACTOR_ID);
        assertThat(solicitud.idempotencyKey()).isEqualTo("clave-1:1");
    }

    @Test
    void anInventoryConcurrencyErrorBecomesTheSalesConcurrencyError() {
        var concurrencia = new StandardApplicationError(
                SalidaInventarioApi.CODIGO_CONCURRENCIA, "Concurrencia.", ErrorCategory.CONFLICT);
        var adapter = new InventarioSalidaAdapter(solicitud -> Result.failure(concurrencia));

        ApplicationError error = adapter.descontar(SOLICITUD).fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(VentasErrors.CONCURRENCIA);
    }

    @Test
    void anyOtherInventoryErrorPassesThroughUntouched() {
        var adapter = new InventarioSalidaAdapter(solicitud -> Result.failure(CONFLICTO));

        ApplicationError error = adapter.descontar(SOLICITUD).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }
}
```

Reemplazar `TEST/infrastructure/configuration/VentasModuleConfigurationTest.java` completo:

```java
package com.softprimesolutions.ventas.infrastructure.configuration;

import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VentasModuleConfigurationTest {

    private final VentasModuleConfiguration configuration = new VentasModuleConfiguration();
    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final VentaWritePort ventas = mock(VentaWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final SalidaInventarioPort inventario = mock(SalidaInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final ClockPort clock = configuration.ventasClockPort();
    private final IdentifierGenerator identifiers = configuration.ventasIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresTheTurnoUseCases() {
        var consultas = configuration.consultarTurnosUseCase(readPort);

        assertThat(consultas).isNotNull();
        assertThat(configuration.abrirTurnoUseCase(turnos, referencias, consultas, identifiers, clock)).isNotNull();
        assertThat(configuration.cerrarTurnoUseCase(turnos, consultas, TRANSACCION_DIRECTA, clock)).isNotNull();
    }

    @Test
    void wiresTheVentaUseCases() {
        var consultas = configuration.consultarVentasUseCase(readPort);

        assertThat(consultas).isNotNull();
        assertThat(configuration.registrarVentaUseCase(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, identifiers,
                clock)).isNotNull();
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (adapters nuevos, `bloquearTurnoAbierto`, `almacen`, `skus`, `findVenta`, `listarVentas`, beans y la dependencia `inventario` no existen).

- [ ] **Step 3: Implementar**

`service-botica/modules/ventas/build.gradle`: agregar tras `implementation project(':shared-persistence')`:

```groovy
    implementation project(':modules:inventario')
```

`MAIN/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapter.java`: agregar la constante junto a las demás y el método (importar nada nuevo):

```java
    private static final String ABIERTO_PARA_COMPARTIR = TurnoRows.SELECT
            + " AND tp.uuid_publico = :terminalId AND tc.estado = 'ABIERTO' FOR SHARE OF tc";
```

```java
    @Override
    public Optional<TurnoCaja> bloquearTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(ABIERTO_PARA_COMPARTIR)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> TurnoRows.map(rs, tenantId))
                .optional();
    }
```

`MAIN/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapter.java`: agregar imports (`java.util.Collection`, `java.util.Map`, `java.util.function.Function`, `java.util.stream.Collectors`), constantes y métodos:

```java
    private static final String ALMACEN = """
            SELECT a.uuid_publico, s.uuid_publico AS establecimiento_uuid,
                   (a.es_activo = '1' AND a.permite_lotes AND a.permite_venta) AS operable
              FROM sch_organizacion.almacen a
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico s
                ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId
            """;
    private static final String SKUS = """
            SELECT k.uuid_publico, k.descripcion_comercial, k.unidad_venta_codigo, k.permite_venta_fraccion,
                   (k.es_activo = '1' AND k.estado_comercial = 'ACTIVO') AS operable
              FROM sch_catalogo.sku_comercial k
              JOIN sch_admin.tenant t ON t.id = k.tenant_id
             WHERE t.uuid_publico = :tenantId AND k.uuid_publico IN (:skuIds)
            """;
```

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId) {
        return jdbcClient.sql(ALMACEN)
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .query((rs, rowNumber) -> new AlmacenRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                        rs.getBoolean("operable")))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) return Map.of();
        return jdbcClient.sql(SKUS)
                .param("tenantId", tenantId)
                .param("skuIds", skuIds)
                .query((rs, rowNumber) -> new SkuVentaRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("descripcion_comercial"),
                        rs.getString("unidad_venta_codigo"), rs.getBoolean("permite_venta_fraccion"),
                        rs.getBoolean("operable")))
                .list()
                .stream()
                .collect(Collectors.toMap(SkuVentaRef::id, Function.identity()));
    }
```

`MAIN/infrastructure/persistence/write/adapter/NumeracionJdbcAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository("ventasNumeracionAdapter")
public class NumeracionJdbcAdapter implements NumeracionPort {

    private static final String SIGUIENTE = """
            INSERT INTO sch_venta.secuencia_operacion (tenant_id, terminal_id, ultimo_numero)
            SELECT tp.tenant_id, tp.id, 1
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            ON CONFLICT (tenant_id, terminal_id)
            DO UPDATE SET ultimo_numero = sch_venta.secuencia_operacion.ultimo_numero + 1
            RETURNING ultimo_numero
            """;

    private final JdbcClient jdbcClient;

    public NumeracionJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public String siguienteNumeroOperacion(UUID tenantId, UUID terminalId, String codigoTerminal) {
        var numero = jdbcClient.sql(SIGUIENTE)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query(Long.class)
                .single();
        return String.format("%s-%06d", codigoTerminal, numero);
    }
}
```

`MAIN/infrastructure/persistence/write/adapter/VentaJdbcWriteAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class VentaJdbcWriteAdapter implements VentaWritePort {

    private static final String POR_IDEMPOTENCIA = """
            SELECT v.uuid_publico, v.huella_solicitud
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.idempotency_key = :idempotencyKey AND v.es_activo = '1'
            """;
    private static final String INSERTAR_VENTA = """
            INSERT INTO sch_venta.venta
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, terminal_id, turno_caja_id,
                 vendedor_usuario_id, numero_operacion, idempotency_key, huella_solicitud, fecha_venta, moneda,
                 subtotal, descuento_total, impuesto_total, redondeo, total, estado, created_at, created_by)
            SELECT :ventaId, tc.tenant_id, tc.empresa_id, tc.establecimiento_id, tc.terminal_id, tc.id,
                   (SELECT m.id FROM sch_seguridad.membership m
                     WHERE m.uuid_publico = :vendedorId AND m.tenant_id = tc.tenant_id),
                   :numero, :idempotencyKey, :huella, :fecha, 'PEN', :subtotal, 0, 0, 0, :total, 'CONFIRMADA',
                   :fecha, :actor
              FROM sch_venta.turno_caja tc
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
             WHERE t.uuid_publico = :tenantId AND tc.uuid_publico = :turnoId AND tc.estado = 'ABIERTO'
            """;
    private static final String INSERTAR_LINEA = """
            INSERT INTO sch_venta.venta_linea
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, venta_id, numero_linea, sku_id,
                 descripcion_snapshot, unidad_venta_codigo, es_fraccion, cantidad, precio_unitario, descuento,
                 impuesto, total_linea, created_at, created_by)
            SELECT :lineaId, v.tenant_id, v.empresa_id, v.establecimiento_id, v.id, :numeroLinea, k.id,
                   :descripcion, :unidad, :esFraccion, :cantidad, :precio, 0, 0, :totalLinea, :fecha, :actor
              FROM sch_venta.venta v
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = v.tenant_id AND k.uuid_publico = :skuId
             WHERE v.uuid_publico = :ventaId
            """;
    private static final String ASEGURAR_EFECTIVO = """
            INSERT INTO sch_venta.medio_pago
                (tenant_id, codigo, nombre, tipo, requiere_referencia, permite_vuelto, created_by)
            SELECT t.id, 'EFECTIVO', 'Efectivo', 'EFECTIVO', FALSE, TRUE, :actor
              FROM sch_admin.tenant t
             WHERE t.uuid_publico = :tenantId
            ON CONFLICT (tenant_id, codigo) WHERE es_activo = '1' DO NOTHING
            """;
    private static final String INSERTAR_PAGO = """
            INSERT INTO sch_venta.pago_venta
                (tenant_id, empresa_id, establecimiento_id, venta_id, medio_pago_id, monto, monto_recibido, vuelto,
                 moneda, estado, pagado_at)
            SELECT v.tenant_id, v.empresa_id, v.establecimiento_id, v.id,
                   (SELECT mp.id FROM sch_venta.medio_pago mp
                     WHERE mp.tenant_id = v.tenant_id AND mp.codigo = 'EFECTIVO' AND mp.es_activo = '1'),
                   :monto, :montoRecibido, :vuelto, 'PEN', 'CONFIRMADO', :fecha
              FROM sch_venta.venta v
             WHERE v.uuid_publico = :ventaId
            """;
    private static final String INSERTAR_LOTE = """
            INSERT INTO sch_venta.venta_linea_lote (tenant_id, venta_linea_id, lote_id, cantidad)
            SELECT vl.tenant_id, vl.id, l.id, :cantidad
              FROM sch_venta.venta_linea vl
              JOIN sch_inventario.lote l ON l.tenant_id = vl.tenant_id AND l.uuid_publico = :loteId
              JOIN sch_admin.tenant t ON t.id = vl.tenant_id
             WHERE t.uuid_publico = :tenantId AND vl.uuid_publico = :lineaId
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public VentaJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public Optional<VentaExistente> findPorIdempotencia(UUID tenantId, String idempotencyKey) {
        return jdbcClient.sql(POR_IDEMPOTENCIA)
                .param("tenantId", tenantId)
                .param("idempotencyKey", idempotencyKey)
                .query((rs, rowNumber) -> new VentaExistente(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("huella_solicitud")))
                .optional();
    }

    @Override
    public GuardadoOutcome insertar(Venta venta, String idempotencyKey, String huella) {
        try {
            transaction.executeWithoutResult(status -> persistir(venta, idempotencyKey, huella));
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException | FilaNoInsertada exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    @Override
    public void registrarLotes(UUID tenantId, UUID ventaLineaId, List<LoteConsumo> lotes) {
        lotes.forEach(lote -> exigirUnaFila(jdbcClient.sql(INSERTAR_LOTE)
                .param("tenantId", tenantId)
                .param("lineaId", ventaLineaId)
                .param("loteId", lote.loteId())
                .param("cantidad", lote.cantidad())
                .update()));
    }

    private void persistir(Venta venta, String idempotencyKey, String huella) {
        exigirUnaFila(insertarVenta(venta, idempotencyKey, huella));
        venta.lineas().forEach(linea -> exigirUnaFila(insertarLinea(venta, linea)));
        asegurarEfectivo(venta);
        exigirUnaFila(insertarPago(venta));
    }

    private int insertarVenta(Venta venta, String idempotencyKey, String huella) {
        return jdbcClient.sql(INSERTAR_VENTA)
                .param("ventaId", venta.id())
                .param("tenantId", venta.tenantId())
                .param("turnoId", venta.turnoId())
                .param("vendedorId", venta.vendedor().id())
                .param("numero", venta.numeroOperacion())
                .param("idempotencyKey", idempotencyKey)
                .param("huella", huella)
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .param("subtotal", venta.subtotal())
                .param("total", venta.total())
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private int insertarLinea(Venta venta, LineaVenta linea) {
        return jdbcClient.sql(INSERTAR_LINEA)
                .param("lineaId", linea.id())
                .param("ventaId", venta.id())
                .param("numeroLinea", linea.numeroLinea())
                .param("skuId", linea.skuId())
                .param("descripcion", linea.descripcion())
                .param("unidad", linea.unidadVentaCodigo())
                .param("esFraccion", linea.esFraccion())
                .param("cantidad", linea.cantidad())
                .param("precio", linea.precioUnitario())
                .param("totalLinea", linea.totalLinea())
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private void asegurarEfectivo(Venta venta) {
        jdbcClient.sql(ASEGURAR_EFECTIVO)
                .param("tenantId", venta.tenantId())
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private int insertarPago(Venta venta) {
        return jdbcClient.sql(INSERTAR_PAGO)
                .param("ventaId", venta.id())
                .param("monto", venta.pago().monto())
                .param("montoRecibido", venta.pago().montoRecibido())
                .param("vuelto", venta.pago().vuelto())
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .update();
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) throw new FilaNoInsertada();
    }

    private static final class FilaNoInsertada extends RuntimeException {

        private static final long serialVersionUID = 1L;

        FilaNoInsertada() {
            super(null, null, false, false);
        }
    }
}
```

Nota: `registrarLotes` no captura `FilaNoInsertada`; un lote que no se puede insertar es un estado inesperado que debe abortar la transacción con una excepción.

`MAIN/infrastructure/client/InventarioSalidaAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventarioSalidaAdapter implements SalidaInventarioPort {

    private final SalidaInventarioApi inventario;

    public InventarioSalidaAdapter(SalidaInventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Result<List<LoteConsumo>, ApplicationError> descontar(SalidaSolicitada solicitud) {
        return inventario.registrarSalidaVenta(new SalidaVentaSolicitud(
                        solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), solicitud.cantidad(),
                        solicitud.ventaId(), solicitud.ventaLineaId(), solicitud.actorId(),
                        solicitud.idempotencyKey()))
                .fold(
                        registrada -> Result.<List<LoteConsumo>, ApplicationError>success(registrada.lotes().stream()
                                .map(lote -> new LoteConsumo(lote.loteId(), lote.cantidad()))
                                .toList()),
                        error -> Result.failure(SalidaInventarioApi.CODIGO_CONCURRENCIA.equals(error.code())
                                ? VentasErrors.modificacionConcurrente()
                                : error));
    }
}
```

`MAIN/infrastructure/persistence/read/adapter/VentasJdbcReadAdapter.java` (reemplazar completo):

```java
package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.result.LineaVentaResult;
import com.softprimesolutions.ventas.application.dto.result.LoteConsumidoResult;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.PagoResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.mapper.VentasApplicationMapper;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VentasJdbcReadAdapter implements VentasReadPort {

    private static final String CABECERA = """
            SELECT v.uuid_publico, v.numero_operacion, tp.uuid_publico AS terminal_uuid,
                   tc.uuid_publico AS turno_uuid, es.uuid_publico AS establecimiento_uuid,
                   m.uuid_publico AS vendedor_uuid, v.fecha_venta, v.moneda, v.subtotal, v.descuento_total,
                   v.impuesto_total, v.total, v.estado
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = v.terminal_id AND tp.tenant_id = v.tenant_id
              JOIN sch_venta.turno_caja tc ON tc.id = v.turno_caja_id AND tc.tenant_id = v.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = v.establecimiento_id AND es.tenant_id = v.tenant_id
              JOIN sch_seguridad.membership m ON m.id = v.vendedor_usuario_id
             WHERE t.uuid_publico = :tenantId AND v.es_activo = '1' AND v.uuid_publico = :ventaId
            """;
    private static final String LINEAS = """
            SELECT vl.uuid_publico, vl.numero_linea, k.uuid_publico AS sku_uuid, vl.descripcion_snapshot,
                   vl.unidad_venta_codigo, vl.cantidad, vl.precio_unitario, vl.total_linea
              FROM sch_venta.venta_linea vl
              JOIN sch_venta.venta v ON v.tenant_id = vl.tenant_id AND v.id = vl.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = vl.tenant_id AND k.id = vl.sku_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND vl.es_activo = '1'
             ORDER BY vl.numero_linea
            """;
    private static final String LOTES = """
            SELECT vl.uuid_publico AS linea_uuid, l.uuid_publico AS lote_uuid, vll.cantidad
              FROM sch_venta.venta_linea_lote vll
              JOIN sch_venta.venta_linea vl ON vl.tenant_id = vll.tenant_id AND vl.id = vll.venta_linea_id
              JOIN sch_venta.venta v ON v.tenant_id = vl.tenant_id AND v.id = vl.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_inventario.lote l ON l.tenant_id = vll.tenant_id AND l.id = vll.lote_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId
             ORDER BY vl.numero_linea, l.fecha_vencimiento, l.numero_lote
            """;
    private static final String PAGO = """
            SELECT mp.codigo AS medio_codigo, p.monto, p.monto_recibido, p.vuelto
              FROM sch_venta.pago_venta p
              JOIN sch_venta.venta v ON v.tenant_id = p.tenant_id AND v.id = p.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_venta.medio_pago mp ON mp.tenant_id = p.tenant_id AND mp.id = p.medio_pago_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND p.es_activo = '1'
             ORDER BY p.id
            """;
    private static final String VENTAS_FROM = """
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = v.terminal_id AND tp.tenant_id = v.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = v.establecimiento_id AND es.tenant_id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.es_activo = '1'
               AND (CAST(:establecimientoId AS uuid) IS NULL OR es.uuid_publico = CAST(:establecimientoId AS uuid))
               AND (CAST(:desde AS timestamptz) IS NULL OR v.fecha_venta >= CAST(:desde AS timestamptz))
               AND (CAST(:hasta AS timestamptz) IS NULL OR v.fecha_venta < CAST(:hasta AS timestamptz))
            """;
    private static final String VENTAS_CONTEO = "SELECT COUNT(*)" + VENTAS_FROM;
    private static final String VENTAS_PAGINA = """
            SELECT v.uuid_publico, v.numero_operacion, tp.uuid_publico AS terminal_uuid, v.fecha_venta, v.total,
                   v.estado
            """ + VENTAS_FROM + " ORDER BY v.fecha_venta DESC, v.id DESC LIMIT :limit OFFSET :offset";

    private final JdbcClient jdbcClient;

    public VentasJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TurnoRows.POR_ID)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TurnoRows.ABIERTO_POR_TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VentaResult> findVenta(UUID tenantId, UUID ventaId) {
        return jdbcClient.sql(CABECERA)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new Cabecera(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero_operacion"),
                        JdbcColumns.uuid(rs, "terminal_uuid"), JdbcColumns.uuid(rs, "turno_uuid"),
                        JdbcColumns.uuid(rs, "establecimiento_uuid"), JdbcColumns.uuid(rs, "vendedor_uuid"),
                        JdbcColumns.instant(rs, "fecha_venta"), rs.getString("moneda"),
                        rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento_total"),
                        rs.getBigDecimal("impuesto_total"), rs.getBigDecimal("total"), rs.getString("estado")))
                .optional()
                .map(cabecera -> armar(tenantId, ventaId, cabecera));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<VentaResumenResult> listarVentas(ListarVentasQuery query) {
        var total = jdbcClient.sql(VENTAS_CONTEO)
                .param("tenantId", query.tenantId())
                .param("establecimientoId", query.establecimientoId())
                .param("desde", JdbcColumns.offset(query.desde()))
                .param("hasta", JdbcColumns.offset(query.hasta()))
                .query(Long.class)
                .single();
        var items = jdbcClient.sql(VENTAS_PAGINA)
                .param("tenantId", query.tenantId())
                .param("establecimientoId", query.establecimientoId())
                .param("desde", JdbcColumns.offset(query.desde()))
                .param("hasta", JdbcColumns.offset(query.hasta()))
                .param("limit", query.size())
                .param("offset", query.page() * query.size())
                .query((rs, rowNumber) -> new VentaResumenResult(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero_operacion"),
                        JdbcColumns.uuid(rs, "terminal_uuid"), JdbcColumns.instant(rs, "fecha_venta"),
                        rs.getBigDecimal("total"), rs.getString("estado")))
                .list();
        return new PaginaResult<>(items, query.page(), query.size(), total);
    }

    private VentaResult armar(UUID tenantId, UUID ventaId, Cabecera cabecera) {
        var lotes = jdbcClient.sql(LOTES)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> Map.entry(
                        JdbcColumns.uuid(rs, "linea_uuid"),
                        new LoteConsumidoResult(JdbcColumns.uuid(rs, "lote_uuid"), rs.getBigDecimal("cantidad"))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        var lineas = jdbcClient.sql(LINEAS)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> {
                    var lineaId = JdbcColumns.uuid(rs, "uuid_publico");
                    return new LineaVentaResult(
                            rs.getInt("numero_linea"), JdbcColumns.uuid(rs, "sku_uuid"),
                            rs.getString("descripcion_snapshot"), rs.getString("unidad_venta_codigo"),
                            rs.getBigDecimal("cantidad"), rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("total_linea"), lotes.getOrDefault(lineaId, List.of()));
                })
                .list();
        var pago = jdbcClient.sql(PAGO)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new PagoResult(
                        rs.getString("medio_codigo"), rs.getBigDecimal("monto"), rs.getBigDecimal("monto_recibido"),
                        rs.getBigDecimal("vuelto")))
                .list()
                .getFirst();
        return new VentaResult(
                cabecera.id(), cabecera.numeroOperacion(), cabecera.terminalId(), cabecera.turnoId(),
                cabecera.establecimientoId(), cabecera.vendedorId(), cabecera.fechaVenta(), cabecera.moneda(),
                cabecera.subtotal(), cabecera.descuentoTotal(), cabecera.impuestoTotal(), cabecera.total(),
                cabecera.estado(), lineas, pago);
    }

    private record Cabecera(
            UUID id, String numeroOperacion, UUID terminalId, UUID turnoId, UUID establecimientoId, UUID vendedorId,
            java.time.Instant fechaVenta, String moneda, java.math.BigDecimal subtotal,
            java.math.BigDecimal descuentoTotal, java.math.BigDecimal impuestoTotal, java.math.BigDecimal total,
            String estado) {
    }
}
```

Nota: el `ResultSet` simulado por `JdbcClientStub` devuelve el valor tal cual para `getInt`, por lo que `Rows.ventaLinea()` guarda `numero_linea` como `Integer` y no hace falta modificar el stub.

`MAIN/infrastructure/configuration/VentasModuleConfiguration.java`: agregar imports (`ConsultarVentasUseCase`, `RegistrarVentaUseCase`, `NumeracionPort`, `SalidaInventarioPort`, `VentaWritePort`, `RegistrarVentaHandler`, `ConsultarVentasHandler`) y beans:

```java
    @Bean
    ConsultarVentasUseCase consultarVentasUseCase(VentasReadPort readPort) {
        return new ConsultarVentasHandler(readPort);
    }

    @Bean
    RegistrarVentaUseCase registrarVentaUseCase(
            VentaWritePort ventas, TurnoWritePort turnos, ReferenciasVentasPort referencias,
            SalidaInventarioPort inventario, NumeracionPort numeracion,
            ConsultarVentasUseCase consultarVentasUseCase, TransaccionPort transaccion,
            IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultarVentasUseCase, transaccion,
                ventasIdentifierGenerator, ventasClockPort);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: PASS. Si falla `VentasJdbcReadAdapterTest` por `getInt`, aplicar la nota del Step 3.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): adapters JDBC de venta, numeracion y salida de inventario

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: API REST de la venta

**Files:**
- Create: `MAIN/api/dto/request/LineaVentaRequest.java`, `PagoEfectivoRequest.java`, `VentaRequest.java`
- Create: `MAIN/api/dto/response/LoteConsumidoResponse.java`, `LineaVentaResponse.java`, `PagoResponse.java`, `VentaResponse.java`, `VentaResumenResponse.java`, `PaginaResponse.java`
- Modify: `MAIN/api/mapper/VentasApiMapper.java`
- Create: `MAIN/api/controller/VentaController.java`
- Test: `TEST/api/mapper/VentasApiMapperTest.java`, `TEST/api/controller/VentaControllerTest.java`

**Interfaces:**
- Consumes: casos de uso de la Task 2.
- Produces: `POST /api/v1/ventas/ventas` (201, cabecera `Idempotency-Key`), `GET /api/v1/ventas/ventas/{ventaId}` (200), `GET /api/v1/ventas/ventas?establecimientoId&desde&hasta&page&size` (200). `VentasApiMapper.toCommand(UUID tenantId, UUID actorId, String idempotencyKey, VentaRequest)`, `toResponse(VentaResult)`, `toResponse(PaginaResult<VentaResumenResult>)`.

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `TEST/api/mapper/VentasApiMapperTest.java` (importar `ALMACEN`, `SKU`, `VENTA`, `ventaResult`, `LineaVentaRequest`, `PagoEfectivoRequest`, `VentaRequest`, `VentaResumenResult`, `PaginaResult`, `java.util.List`):

```java
    @Test
    void mapsTheSaleRequestToACommandWithTheKeyTenantAndActor() {
        var request = new VentaRequest(
                TERMINAL, ALMACEN, List.of(new LineaVentaRequest(SKU, dec("5"), dec("2.50"))),
                new PagoEfectivoRequest(dec("20")));

        var command = VentasApiMapper.toCommand(TENANT, ACTOR_ID, "clave-1", request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("clave-1");
        assertThat(command.terminalId()).isEqualTo(TERMINAL);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.montoRecibido()).isEqualTo(dec("20"));
        assertThat(command.lineas()).hasSize(1);
        assertThat(command.lineas().getFirst().skuId()).isEqualTo(SKU);
        assertThat(command.lineas().getFirst().cantidad()).isEqualTo(dec("5"));
        assertThat(command.lineas().getFirst().precioUnitario()).isEqualTo(dec("2.50"));
    }

    @Test
    void mapsASaleResultToItsResponse() {
        var response = VentasApiMapper.toResponse(ventaResult());

        assertThat(response.id()).isEqualTo(VENTA);
        assertThat(response.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(response.turnoId()).isEqualTo(TURNO);
        assertThat(response.total()).isEqualTo(dec("12.50"));
        assertThat(response.lineas()).hasSize(1);
        assertThat(response.lineas().getFirst().skuId()).isEqualTo(SKU);
        assertThat(response.lineas().getFirst().lotes()).hasSize(1);
        assertThat(response.lineas().getFirst().lotes().getFirst().cantidad()).isEqualTo(dec("5"));
        assertThat(response.pago().medioPago()).isEqualTo("EFECTIVO");
        assertThat(response.pago().vuelto()).isEqualTo(dec("7.50"));
    }

    @Test
    void mapsASalesPageToItsResponse() {
        var pagina = new PaginaResult<>(
                List.of(new VentaResumenResult(VENTA, "POS01-000001", TERMINAL, AHORA, dec("12.50"), "CONFIRMADA")),
                1, 20, 21L);

        var response = VentasApiMapper.toResponse(pagina);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(21L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(VENTA);
        assertThat(response.items().getFirst().numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(response.items().getFirst().total()).isEqualTo(dec("12.50"));
    }
```

`TEST/api/controller/VentaControllerTest.java`:

```java
package com.softprimesolutions.ventas.api.controller;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.api.dto.request.LineaVentaRequest;
import com.softprimesolutions.ventas.api.dto.request.PagoEfectivoRequest;
import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.api.dto.response.PaginaResponse;
import com.softprimesolutions.ventas.api.dto.response.VentaResponse;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class VentaControllerTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);

    private static VentaRequest request() {
        return new VentaRequest(
                TERMINAL, ALMACEN, List.of(new LineaVentaRequest(SKU, dec("5"), dec("2.50"))),
                new PagoEfectivoRequest(dec("20")));
    }

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void registersASaleWithTheKeyTenantAndActorAndAnswersCreated() {
        var received = new AtomicReference<RegistrarVentaCommand>();
        var controller = new VentaController(command -> {
            received.set(command);
            return ok(ventaResult());
        }, consultas);

        var response = controller.register(JWT, "clave-1", request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((VentaResponse) response.getBody()).id()).isEqualTo(VENTA);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().idempotencyKey()).isEqualTo("clave-1");
        assertThat(received.get().terminalId()).isEqualTo(TERMINAL);
    }

    @Test
    void aMissingKeyIsPassedAsNullSoTheUseCaseRejectsIt() {
        var received = new AtomicReference<RegistrarVentaCommand>();
        var controller = new VentaController(command -> {
            received.set(command);
            return conflict();
        }, consultas);

        assertConflict(controller.register(JWT, null, request()));
        assertThat(received.get().idempotencyKey()).isNull();
    }

    @Test
    void getsASaleById() {
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(ok(ventaResult()));
        var controller = new VentaController(command -> conflict(), consultas);

        var response = controller.getById(JWT, VENTA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((VentaResponse) response.getBody()).numeroOperacion()).isEqualTo("POS01-000001");
    }

    @Test
    void aMissingSaleBecomesAProblem() {
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(conflict());
        var controller = new VentaController(command -> conflict(), consultas);

        assertConflict(controller.getById(JWT, VENTA));
    }

    @Test
    void listsSalesWithTheFiltersAndPagination() {
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 1, 10);
        when(consultas.listar(query)).thenReturn(ok(new PaginaResult<>(List.of(), 1, 10, 0L)));
        var controller = new VentaController(command -> conflict(), consultas);

        var response = controller.list(JWT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 1, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var pagina = (PaginaResponse<?>) response.getBody();
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(10);
    }

    @Test
    void aListFailureBecomesAProblem() {
        when(consultas.listar(new ListarVentasQuery(TENANT, null, null, null, 0, 500))).thenReturn(conflict());
        var controller = new VentaController(command -> conflict(), consultas);

        assertConflict(controller.list(JWT, null, null, null, 0, 500));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*VentasApiMapperTest" --tests "*VentaControllerTest"`
Expected: FAIL de compilación (DTOs, mapper y controller de venta no existen).

- [ ] **Step 3: Implementar**

`MAIN/api/dto/request/LineaVentaRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaVentaRequest(
        @NotNull UUID skuId,
        @NotNull @DecimalMin("0.0001") BigDecimal cantidad,
        @NotNull @DecimalMin("0.00") BigDecimal precioUnitario) {
}
```

`MAIN/api/dto/request/PagoEfectivoRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoEfectivoRequest(@NotNull @DecimalMin("0.00") BigDecimal montoRecibido) {
}
```

`MAIN/api/dto/request/VentaRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record VentaRequest(
        @NotNull UUID terminalId,
        @NotNull UUID almacenId,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull LineaVentaRequest> lineas,
        @NotNull @Valid PagoEfectivoRequest pago) {
}
```

`MAIN/api/dto/response/LoteConsumidoResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumidoResponse(UUID loteId, BigDecimal cantidad) {
}
```

`MAIN/api/dto/response/LineaVentaResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineaVentaResponse(
        int numeroLinea,
        UUID skuId,
        String descripcion,
        String unidadVentaCodigo,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalLinea,
        List<LoteConsumidoResponse> lotes) {
}
```

`MAIN/api/dto/response/PagoResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;

public record PagoResponse(String medioPago, BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {
}
```

`MAIN/api/dto/response/VentaResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VentaResponse(
        UUID id,
        String numeroOperacion,
        UUID terminalId,
        UUID turnoId,
        UUID establecimientoId,
        UUID vendedorId,
        Instant fechaVenta,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal total,
        String estado,
        List<LineaVentaResponse> lineas,
        PagoResponse pago) {
}
```

`MAIN/api/dto/response/VentaResumenResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VentaResumenResponse(
        UUID id, String numeroOperacion, UUID terminalId, Instant fechaVenta, BigDecimal total, String estado) {
}
```

`MAIN/api/dto/response/PaginaResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.util.List;

public record PaginaResponse<T>(List<T> items, int page, int size, long totalElements) {
}
```

`MAIN/api/mapper/VentasApiMapper.java`: agregar imports (`LineaVentaRequest`, `VentaRequest`, `LineaVentaResponse`, `LoteConsumidoResponse`, `PagoResponse`, `PaginaResponse`, `VentaResponse`, `VentaResumenResponse`, `LineaVentaInput`, `RegistrarVentaCommand`, `LineaVentaResult`, `LoteConsumidoResult`, `PaginaResult`, `PagoResult`, `VentaResult`, `VentaResumenResult`) y métodos:

```java
    public static RegistrarVentaCommand toCommand(
            UUID tenantId, UUID actorId, String idempotencyKey, VentaRequest request) {
        return new RegistrarVentaCommand(
                tenantId, actorId, idempotencyKey, request.terminalId(), request.almacenId(),
                request.lineas().stream().map(VentasApiMapper::toInput).toList(),
                request.pago().montoRecibido());
    }

    public static VentaResponse toResponse(VentaResult result) {
        return new VentaResponse(
                result.id(), result.numeroOperacion(), result.terminalId(), result.turnoId(),
                result.establecimientoId(), result.vendedorId(), result.fechaVenta(), result.moneda(),
                result.subtotal(), result.descuentoTotal(), result.impuestoTotal(), result.total(), result.estado(),
                result.lineas().stream().map(VentasApiMapper::toResponse).toList(), toResponse(result.pago()));
    }

    public static PaginaResponse<VentaResumenResponse> toResponse(PaginaResult<VentaResumenResult> pagina) {
        return new PaginaResponse<>(
                pagina.items().stream().map(VentasApiMapper::toResponse).toList(), pagina.page(), pagina.size(),
                pagina.totalElements());
    }

    private static LineaVentaInput toInput(LineaVentaRequest request) {
        return new LineaVentaInput(request.skuId(), request.cantidad(), request.precioUnitario());
    }

    private static LineaVentaResponse toResponse(LineaVentaResult result) {
        return new LineaVentaResponse(
                result.numeroLinea(), result.skuId(), result.descripcion(), result.unidadVentaCodigo(),
                result.cantidad(), result.precioUnitario(), result.totalLinea(),
                result.lotes().stream().map(VentasApiMapper::toResponse).toList());
    }

    private static LoteConsumidoResponse toResponse(LoteConsumidoResult result) {
        return new LoteConsumidoResponse(result.loteId(), result.cantidad());
    }

    private static PagoResponse toResponse(PagoResult result) {
        return new PagoResponse(result.medioPago(), result.monto(), result.montoRecibido(), result.vuelto());
    }

    private static VentaResumenResponse toResponse(VentaResumenResult result) {
        return new VentaResumenResponse(
                result.id(), result.numeroOperacion(), result.terminalId(), result.fechaVenta(), result.total(),
                result.estado());
    }
```

`MAIN/api/controller/VentaController.java`:

```java
package com.softprimesolutions.ventas.api.controller;

import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.api.mapper.VentasApiMapper;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(VentaController.BASE_PATH)
public class VentaController {

    static final String BASE_PATH = "/api/v1/ventas/ventas";

    private final RegistrarVentaUseCase registerVenta;
    private final ConsultarVentasUseCase queryVentas;

    public VentaController(RegistrarVentaUseCase registerVenta, ConsultarVentasUseCase queryVentas) {
        this.registerVenta = registerVenta;
        this.queryVentas = queryVentas;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ventas.ventas.registrar')")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody VentaRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), idempotencyKey, request);
        return registerVenta.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(VentasApiMapper.toResponse(result)),
                VentasControllerSupport::problem);
    }

    @GetMapping("/{ventaId}")
    @PreAuthorize("hasAuthority('ventas.ventas.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId) {
        return queryVentas.obtener(new ObtenerVentaQuery(VentasControllerSupport.tenantOf(jwt), ventaId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ventas.ventas.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID establecimientoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarVentasQuery(
                VentasControllerSupport.tenantOf(jwt), establecimientoId, desde, hasta, page, size);
        return queryVentas.listar(query)
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:check`
Expected: PASS (tests unitarios, cobertura JaCoCo al 100% por clase, ArchUnit del módulo). Si JaCoCo reporta ramas sin cubrir, agregar el test faltante; no relajar el umbral.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): API REST de la venta

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Integración HTTP de la venta contra PostgreSQL

**Files:**
- Create: `BTEST/ventas/api/VentaApiIntegrationTest.java`

**Interfaces:**
- Consumes: endpoints de venta y turno, API REST de `organizacion` e `inventario` para preparar datos, `RealLogin`.

- [ ] **Step 1: Escribir el test**

`BTEST/ventas/api/VentaApiIntegrationTest.java`:

```java
package com.softprimesolutions.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.testsupport.RealLogin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class VentaApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("c8f2e3d4-5b6a-4f7e-8d9c-2b3c4d5e6f70");
    private static final String ORG = "/api/v1/organizacion";
    private static final String INV = "/api/v1/inventario";
    private static final String VENTAS = "/api/v1/ventas/ventas";
    private static final String TURNOS = "/api/v1/ventas/turnos";
    private static final String UNIDAD = "UNDVEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private String bearer;
    private String noPermissions;
    private UUID userId;
    private UUID establecimientoId;
    private UUID almacenId;
    private UUID terminalId;
    private UUID skuId;

    @BeforeEach
    void createTheTenantTheMasterDataAndAnOpenTurno() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'VENTVTA', 'Tenant ventas', 'tenant-ventas-venta', 'test')
                        """).param("tenantId", TENANT_ID).update();
        var realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "venta.admin", "^(organizacion|inventario|ventas)\\.");
        userId = session.userId();
        bearer = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        var empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Venta SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        establecimientoId = created(post(ORG + "/establecimientos").content("""
                {"tenantId":"%s","empresaId":"%s","codigo":"EST001","nombre":"Botica Central",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0001","esPrincipal":true,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId)));
        almacenId = created(post(ORG + "/almacenes").content("""
                {"tenantId":"%s","establecimientoId":"%s","codigo":"ALM001","nombre":"Almacen Central",
                 "tipo":"GENERAL","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                 "permiteDespacho":true,"controlTemperatura":false}
                """.formatted(TENANT_ID, establecimientoId)));
        terminalId = created(post(ORG + "/terminales-pos").content("""
                {"establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B001","serieFacturaDefecto":"F001","storeEdgeHabilitado":false}
                """.formatted(establecimientoId)));

        skuId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-VEN-1', 'Paracetamol 500 mg', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();

        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":100}".formatted(terminalId)))
                .andExpect(status().isCreated());
    }

    @Test
    void registersASaleConsumingTheEarliestExpiryLotesFirstAndRecordingEverything() throws Exception {
        var antiguo = ingresar("3", "L-ANT", LocalDate.now().plusMonths(6));
        var nuevo = ingresar("10", "L-NEW", LocalDate.now().plusYears(1));

        var cuerpo = vender("clave-1", "5", "2.50", "20")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("POS001-000001"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.vendedorId").value(userId.toString()))
                .andExpect(jsonPath("$.subtotal").value(12.5))
                .andExpect(jsonPath("$.impuestoTotal").value(0.0))
                .andExpect(jsonPath("$.total").value(12.5))
                .andExpect(jsonPath("$.lineas[0].numeroLinea").value(1))
                .andExpect(jsonPath("$.lineas[0].descripcion").value("Paracetamol 500 mg"))
                .andExpect(jsonPath("$.lineas[0].totalLinea").value(12.5))
                .andExpect(jsonPath("$.lineas[0].lotes[0].loteId").value(antiguo.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[0].cantidad").value(3.0))
                .andExpect(jsonPath("$.lineas[0].lotes[1].loteId").value(nuevo.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[1].cantidad").value(2.0))
                .andExpect(jsonPath("$.pago.medioPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pago.monto").value(12.5))
                .andExpect(jsonPath("$.pago.montoRecibido").value(20.0))
                .andExpect(jsonPath("$.pago.vuelto").value(7.5))
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer)
                        .param("almacenId", almacenId.toString()).param("skuId", skuId.toString()))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-ANT')].cantidadFisica").value(0))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-NEW')].cantidadFisica").value(8));

        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_movimiento, m.tipo_operacion_sunat, m.documento_tipo, m.documento_uuid,
                               m.actor, m.cantidad
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'SALIDA_VENTA'
                         ORDER BY m.id
                        """).param("tenantId", TENANT_ID).query().listOfRows();
        assertThat(kardex).hasSize(2);
        assertThat(kardex).allSatisfy(fila -> {
            assertThat(fila).containsEntry("tipo_operacion_sunat", "01").containsEntry("documento_tipo", "VENTA")
                    .containsEntry("actor", userId.toString());
            assertThat(fila.get("documento_uuid")).hasToString(ventaId.toString());
        });
        var fila = jdbcClient.sql("""
                        SELECT v.created_by, v.numero_operacion, v.idempotency_key,
                               (SELECT COUNT(*) FROM sch_venta.venta_linea_lote vll
                                  JOIN sch_venta.venta_linea vl ON vl.id = vll.venta_linea_id
                                 WHERE vl.venta_id = v.id) AS lotes,
                               (SELECT p.vuelto FROM sch_venta.pago_venta p WHERE p.venta_id = v.id) AS vuelto
                          FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", ventaId).query().singleRow();
        assertThat(fila).containsEntry("created_by", userId.toString()).containsEntry("lotes", 2L)
                .containsEntry("idempotency_key", "clave-1");
        assertThat((BigDecimal) fila.get("vuelto")).isEqualByComparingTo("7.50");
    }

    @Test
    void numbersTheSalesSequentiallyPerTerminal() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        vender("clave-a", "1", "5", "10").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("POS001-000001"));
        vender("clave-b", "1", "5", "10").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("POS001-000002"));
    }

    @Test
    void aRetryWithTheSameKeyReturnsTheSameSaleWithoutDiscountingTwiceAndADifferentBodyConflicts() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        var primera = vender("clave-1", "4", "5", "50").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var segunda = vender("clave-1", "4", "5", "50").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(segunda, "$.id")).isEqualTo(JsonPath.read(primera, "$.id"));
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(6));
        vender("clave-1", "5", "5", "50").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void getsAndListsTheSales() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "2", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(get(VENTAS + "/{id}", ventaId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ventaId.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[0].cantidad").value(2.0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer)
                        .param("establecimientoId", establecimientoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ventaId.toString()))
                .andExpect(jsonPath("$.items[0].total").value(10.0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer)
                        .param("establecimientoId", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer).param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_PAGINACION_INVALIDA"));
        mockMvc.perform(get(VENTAS + "/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_NO_ENCONTRADA"));
    }

    @Test
    void closingTheTurnoCountsTheCashSalesInTheSystemTotal() throws Exception {
        ingresar("20", "L-001", LocalDate.now().plusYears(1));
        vender("clave-1", "2", "5", "10").andExpect(status().isCreated());
        vender("clave-2", "3", "5", "20").andExpect(status().isCreated());
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":124}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentasSistema").value(25.0))
                .andExpect(jsonPath("$.totalSistema").value(125.0))
                .andExpect(jsonPath("$.diferencia").value(-1.0));
        vender("clave-3", "1", "5", "10").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ABIERTO"));
    }

    @Test
    void validatesTheRequestAndItsReferences() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        vender(null, "1", "5", "10").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_IDEMPOTENCY_KEY_INVALID"));
        vender("clave-1", "1", "-1", "10").andExpect(status().isBadRequest());
        vender("clave-1", "0", "5", "10").andExpect(status().isBadRequest());
        vender("clave-1", "1", "5", "4").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_MONTO_RECIBIDO_INSUFICIENTE"));
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"almacenId\":\"%s\",\"lineas\":[],\"pago\":{\"montoRecibido\":1}}"
                                .formatted(terminalId, almacenId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(UUID.randomUUID(), "1", "5", "10")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_SKU_NO_ENCONTRADO"));
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(skuId, "0.5", "5", "10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_FRACCION_NO_PERMITIDA"));
    }

    @Test
    void deniesAccessWithoutThePermissionOrTheToken() throws Exception {
        mockMvc.perform(post(VENTAS).with(csrf()).header("Authorization", noPermissions)
                        .header("Idempotency-Key", "clave-1").contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(skuId, "1", "5", "10")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(VENTAS).header("Authorization", noPermissions)).andExpect(status().isForbidden());
        mockMvc.perform(get(VENTAS)).andExpect(status().isUnauthorized());
    }

    private ResultActions vender(String clave, String cantidad, String precio, String recibido) throws Exception {
        var request = post(VENTAS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(ventaJson(skuId, cantidad, precio, recibido));
        return mockMvc.perform(clave == null ? request : request.header("Idempotency-Key", clave));
    }

    private String ventaJson(UUID sku, String cantidad, String precio, String recibido) {
        return """
                {"terminalId":"%s","almacenId":"%s",
                 "lineas":[{"skuId":"%s","cantidad":%s,"precioUnitario":%s}],
                 "pago":{"montoRecibido":%s}}
                """.formatted(terminalId, almacenId, sku, cantidad, precio, recibido);
    }

    private UUID ingresar(String cantidad, String numeroLote, LocalDate vencimiento) throws Exception {
        var cuerpo = mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"almacenId":"%s","skuId":"%s","tipo":"AJUSTE_INGRESO","cantidad":%s,
                                 "motivo":"Saldo inicial","numeroLote":"%s","fechaVencimiento":"%s"}
                                """.formatted(almacenId, skuId, cantidad, numeroLote, vencimiento)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(cuerpo, "$.loteId"));
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
```

- [ ] **Step 2: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.api.VentaApiIntegrationTest"` (requiere Docker).
Expected: PASS. Observaciones conocidas: el test se ejecuta dentro de la transacción del propio test (`@Transactional`), por lo que no puede comprobar rollbacks reales (eso lo cubre la Task 6); las validaciones de 400 en `vender("clave-1", "1", "-1", "10")` vienen de Bean Validation (`@DecimalMin`) y no del dominio. Si `numbersTheSalesSequentiallyPerTerminal` ve un número distinto de `000001` por datos residuales, confirmar que el tenant del test es nuevo (`TENANT_ID` distinto de los demás tests) antes de tocar la aserción.

- [ ] **Step 3: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/ventas
git commit -m "test(ventas): validar la venta en efectivo contra PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Atomicidad y concurrencia con datos confirmados

**Files:**
- Create: `BTEST/ventas/api/VentaConcurrencyIntegrationTest.java`
- Modify: `CLAUDE.md` (estado real del backend)

**Interfaces:**
- Consumes: beans `RegistrarVentaUseCase`, `AbrirTurnoUseCase`, `CerrarTurnoUseCase`, `RegistrarMovimientoUseCase` (inventario) y `JdbcClient`; sin MockMvc ni `@Transactional` (los datos se confirman para ejercitar rollbacks y concurrencia reales).

- [ ] **Step 1: Escribir el test**

`BTEST/ventas/api/VentaConcurrencyIntegrationTest.java`:

```java
package com.softprimesolutions.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class VentaConcurrencyIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("d9a3f4e5-6c7b-4a8f-9e0d-3c4d5e6f7a81");
    private static final UUID IDENTIDAD_ID = UUID.fromString("e1b4a5f6-7d8c-4b9a-8f1e-4d5e6f7a8b92");
    private static final String UNIDAD = "UNDVCO";

    @Autowired
    private RegistrarVentaUseCase registrarVenta;

    @Autowired
    private AbrirTurnoUseCase abrirTurno;

    @Autowired
    private CerrarTurnoUseCase cerrarTurno;

    @Autowired
    private RegistrarMovimientoUseCase registrarMovimiento;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID actor = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID terminalId = UUID.randomUUID();
    private final UUID skuA = UUID.randomUUID();
    private final UUID skuB = UUID.randomUUID();

    @BeforeEach
    void createCommittedMasterDataStockAndAnOpenTurno() {
        cleanUp();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'VENCONC', 'Tenant venta concurrente', 'tenant-venta-conc', 'test')
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.empresa_operadora (tenant_id, ruc, razon_social)
                        SELECT id, '20123456786', 'Venta Concurrente SAC' FROM sch_admin.tenant
                         WHERE uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.establecimiento_farmaceutico
                            (tenant_id, empresa_id, codigo, nombre)
                        SELECT e.tenant_id, e.id, 'EST001', 'Botica' FROM sch_organizacion.empresa_operadora e
                          JOIN sch_admin.tenant t ON t.id = e.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.almacen
                            (uuid_publico, tenant_id, empresa_id, establecimiento_id, codigo, nombre, permite_venta)
                        SELECT :almacenId, s.tenant_id, s.empresa_id, s.id, 'ALM001', 'Almacen', TRUE
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("almacenId", almacenId).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.terminal_pos
                            (uuid_publico, tenant_id, empresa_id, establecimiento_id, codigo, nombre)
                        SELECT :terminalId, s.tenant_id, s.empresa_id, s.id, 'POS01', 'Caja 1'
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("terminalId", terminalId).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad (uuid_publico, email, username, nombres, created_at)
                        VALUES (:identidadId, 'venta.conc@example.test', 'venta.conc', 'venta.conc',
                                CURRENT_TIMESTAMP)
                        """).param("identidadId", IDENTIDAD_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar, requiere_cambio_credencial,
                             mfa_requerido, estado, created_at)
                        SELECT :actorId, t.id, i.id, 'venta.conc', FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("actorId", actor).param("tenantId", TENANT_ID)
                .param("identidadId", IDENTIDAD_ID).update();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        insertSku(skuA, "SKU-VC-A");
        insertSku(skuB, "SKU-VC-B");
        ingresar(skuA, "10");
        ingresar(skuB, "10");
        abrirTurno.execute(new AbrirTurnoCommand(TENANT_ID, actor, terminalId, new BigDecimal("100")))
                .fold(turno -> turno, error -> { throw new AssertionError(error); });
    }

    @AfterEach
    void cleanUp() {
        for (var table : List.of(
                "sch_venta.pago_venta", "sch_venta.venta_linea_lote", "sch_venta.venta_linea", "sch_venta.venta",
                "sch_venta.turno_caja", "sch_venta.secuencia_operacion", "sch_venta.medio_pago",
                "sch_inventario.movimiento_inventario", "sch_inventario.posicion_inventario",
                "sch_inventario.lote", "sch_catalogo.sku_comercial", "sch_organizacion.terminal_pos",
                "sch_organizacion.almacen", "sch_organizacion.establecimiento_farmaceutico",
                "sch_organizacion.empresa_operadora", "sch_seguridad.membership")) {
            jdbcClient.sql("DELETE FROM " + table
                    + " WHERE tenant_id IN (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)")
                    .param("tenantId", TENANT_ID).update();
        }
        jdbcClient.sql("DELETE FROM sch_seguridad.identidad WHERE uuid_publico = :identidadId")
                .param("identidadId", IDENTIDAD_ID).update();
        jdbcClient.sql("DELETE FROM sch_admin.tenant WHERE uuid_publico = :tenantId")
                .param("tenantId", TENANT_ID).update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida WHERE codigo = :codigo")
                .param("codigo", UNIDAD).update();
    }

    @Test
    void aSaleThatFailsOnALaterLineLeavesNoTracesNorDiscountsAnEarlierLine() {
        var resultado = vender("clave-1", linea(skuA, "3"), linea(skuB, "99"));

        assertThat(resultado.fold(venta -> "OK", ApplicationError::code)).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(stock(skuA)).isEqualByComparingTo("10");
        assertThat(stock(skuB)).isEqualByComparingTo("10");
        assertThat(count("sch_venta.venta")).isZero();
        assertThat(count("sch_venta.venta_linea")).isZero();
        assertThat(count("sch_venta.pago_venta")).isZero();
        assertThat(count("sch_venta.secuencia_operacion")).isZero();
        assertThat(salidasVentaEnKardex()).isZero();
    }

    @Test
    void aRetryWithTheSameKeyReturnsTheSameSaleAndDiscountsOnce() {
        var primera = value(vender("clave-1", linea(skuA, "4")));
        var segunda = value(vender("clave-1", linea(skuA, "4")));

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(stock(skuA)).isEqualByComparingTo("6");
        assertThat(count("sch_venta.venta")).isEqualTo(1L);
    }

    @Test
    void twoSimultaneousSalesOfEightFromTenNeverOversell() throws Exception {
        var resultados = concurrently(List.of(
                () -> vender("clave-a", linea(skuA, "8")), () -> vender("clave-b", linea(skuA, "8"))));

        assertThat(resultados.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(resultados.stream().map(resultado -> resultado.fold(venta -> "OK", ApplicationError::code))
                .filter(codigo -> !codigo.equals("OK")).toList()).containsExactly("INV_STOCK_INSUFICIENTE");
        assertThat(stock(skuA)).isEqualByComparingTo("2");
        assertThat(count("sch_venta.venta")).isEqualTo(1L);
        assertThat(salidasVentaEnKardex()).isEqualTo(1L);
    }

    @Test
    void salesTouchingTheSameSkusInOppositeOrderNeverDeadlockAndGetUniqueNumbers() throws Exception {
        var tareas = new ArrayList<Callable<Result<VentaResult, ApplicationError>>>();
        for (var indice = 0; indice < 6; indice++) {
            var clave = "clave-" + indice;
            var enOrden = indice % 2 == 0;
            tareas.add(() -> enOrden
                    ? vender(clave, linea(skuA, "1"), linea(skuB, "1"))
                    : vender(clave, linea(skuB, "1"), linea(skuA, "1")));
        }

        var resultados = concurrently(tareas);

        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.isSuccess()).isTrue());
        assertThat(stock(skuA)).isEqualByComparingTo("4");
        assertThat(stock(skuB)).isEqualByComparingTo("4");
        assertThat(jdbcClient.sql("SELECT COUNT(DISTINCT numero_operacion) FROM sch_venta.venta")
                .query(Long.class).single()).isEqualTo(6L);
    }

    @Test
    void closingTheTurnoAfterSalesCountsThemInTheSystemTotal() {
        value(vender("clave-1", linea(skuA, "2")));
        value(vender("clave-2", linea(skuB, "3")));
        var turno = jdbcClient.sql("""
                        SELECT tc.uuid_publico FROM sch_venta.turno_caja tc
                          JOIN sch_admin.tenant t ON t.id = tc.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query(UUID.class).single();

        var cerrado = cerrarTurno.execute(new CerrarTurnoCommand(TENANT_ID, actor, turno, new BigDecimal("150"), null))
                .fold(valor -> valor, error -> { throw new AssertionError(error); });

        assertThat(cerrado.totalVentasSistema()).isEqualByComparingTo("50.00");
        assertThat(cerrado.totalSistema()).isEqualByComparingTo("150.00");
        assertThat(cerrado.diferencia()).isEqualByComparingTo("0.00");
        assertThat(vender("clave-3", linea(skuA, "1")).fold(venta -> "OK", ApplicationError::code))
                .isEqualTo("VEN_TURNO_NO_ABIERTO");
    }

    private LineaVentaInput linea(UUID sku, String cantidad) {
        return new LineaVentaInput(sku, new BigDecimal(cantidad), new BigDecimal("10"));
    }

    private Result<VentaResult, ApplicationError> vender(String clave, LineaVentaInput... lineas) {
        return registrarVenta.execute(new RegistrarVentaCommand(
                TENANT_ID, actor, clave, terminalId, almacenId, List.of(lineas), new BigDecimal("1000")));
    }

    private static VentaResult value(Result<VentaResult, ApplicationError> resultado) {
        return resultado.fold(venta -> venta, error -> { throw new AssertionError(error); });
    }

    private List<Result<VentaResult, ApplicationError>> concurrently(
            List<Callable<Result<VentaResult, ApplicationError>>> tareas) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(tareas.size());
        try {
            var futuros = new ArrayList<Future<Result<VentaResult, ApplicationError>>>();
            for (var tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    start.await();
                    return tarea.call();
                }));
            }
            start.countDown();
            var resultados = new ArrayList<Result<VentaResult, ApplicationError>>();
            for (var futuro : futuros) resultados.add(futuro.get());
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private void insertSku(UUID sku, String codigo) {
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', :codigo, 'Producto concurrente', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", sku).param("codigo", codigo).param("unidad", UNIDAD)
                .param("tenantId", TENANT_ID).update();
    }

    private void ingresar(UUID sku, String cantidad) {
        registrarMovimiento.execute(new RegistrarMovimientoCommand(
                        TENANT_ID, almacenId, sku, null, "L-" + sku.toString().substring(0, 4),
                        LocalDate.now().plusYears(1), "AJUSTE_INGRESO", new BigDecimal(cantidad), "Saldo inicial",
                        actor, null))
                .fold(MovimientoResult::loteId, error -> { throw new AssertionError(error); });
    }

    private BigDecimal stock(UUID sku) {
        return jdbcClient.sql("""
                        SELECT COALESCE(SUM(p.cantidad_fisica), 0) FROM sch_inventario.posicion_inventario p
                          JOIN sch_catalogo.sku_comercial k ON k.id = p.sku_id
                         WHERE k.uuid_publico = :skuId
                        """).param("skuId", sku).query(BigDecimal.class).single();
    }

    private long count(String tabla) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + tabla
                        + " x WHERE x.tenant_id IN (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)")
                .param("tenantId", TENANT_ID).query(Long.class).single();
    }

    private long salidasVentaEnKardex() {
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'SALIDA_VENTA'
                        """).param("tenantId", TENANT_ID).query(Long.class).single();
    }
}
```

Nota: todas las tablas limpiadas, incluidas `sch_venta.secuencia_operacion` y `sch_venta.medio_pago`, tienen `tenant_id`.

- [ ] **Step 2: Ejecutar los tests**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.api.VentaConcurrencyIntegrationTest"` (requiere Docker).
Expected: PASS. Si `twoSimultaneousSalesOfEightFromTenNeverOversell` devuelve `INV_MODIFICACION_CONCURRENTE` o `VEN_MODIFICACION_CONCURRENTE` en el perdedor en lugar de `INV_STOCK_INSUFICIENTE`, repetir 5 veces: si es intermitente, el reintento agotado es un hallazgo real a reportar con la salida del test (no relajar la aserción en silencio). Si `salesTouchingTheSameSkusInOppositeOrder...` produce un deadlock, es un defecto del orden de bloqueo en `RegistrarVentaHandler#enOrdenDeBloqueo`: reportarlo con la traza.

- [ ] **Step 3: Actualizar el estado real en `CLAUDE.md`**

Reemplazar la frase `Los demás módulos de negocio (ventas, etc.) aún no tienen controladores, adapters de persistencia ni migraciones propias.` por:

`ventas` expone turno de caja (abrir, consultar turno actual/por id, cerrar con arqueo simple) y venta presencial en efectivo (`POST /api/v1/ventas/ventas` con `Idempotency-Key`, consulta y listado) bajo `/api/v1/ventas/*`, con descuento de stock FEFO vía `SalidaInventarioApi`, correlativo por terminal y reintento ante concurrencia (migraciones `V033`–`V034`, `VentaApiIntegrationTest` y `VentaConcurrencyIntegrationTest`); IGV en 0, sin CPE, receta, promociones, clientes, otros medios de pago ni anulación (POR_VALIDAR). Los demás módulos de negocio aún no tienen controladores, adapters de persistencia ni migraciones propias.

- [ ] **Step 4: Verificación completa del repositorio**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (compilación, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo al 100% por clase en `ventas`).

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/ventas CLAUDE.md
git commit -m "test(ventas): atomicidad y concurrencia de la venta con datos confirmados

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (parte 2):** registro de venta con las reglas del spec (turno abierto, almacén del mismo establecimiento, SKU activo, cantidad/precio/fracción, máximo 100 líneas, totales, efectivo y vuelto) — Tasks 1–2; idempotencia por `Idempotency-Key` con huella y reintento ante carrera — Task 2; correlativo por terminal en la misma transacción — Tasks 2–3; descuento FEFO por línea vía `SalidaInventarioApi`, con orden estable por `skuId` y rollback completo — Tasks 2, 3 y 6; consulta y listado — Tasks 2–4; permisos `ventas.ventas.*` — Tasks 4 y 5 (sembrados en la parte 1); integración HTTP y concurrencia (sobreventa, deadlock entre SKUs en orden opuesto, rollback entre líneas, cierre de turno con ventas) — Tasks 5–6.

**Desviaciones respecto al spec (decisiones de diseño):**
1. El spec decía "medio de pago `EFECTIVO` por tenant" sembrado por migración; aquí se crea bajo demanda (`ON CONFLICT DO NOTHING`) porque los tenants se crean dinámicamente.
2. El spec no mencionaba `uuid_publico` en `venta_linea`; la parte 1 lo agrega en V033 porque `SalidaVentaSolicitud.ventaLineaId` y `venta_linea_lote` necesitan un identificador estable de línea.
3. `VentaResult` no incluye el almacén: la tabla `venta` no lo almacena (el movimiento de inventario sí).
4. El turno se bloquea con `FOR SHARE` al vender para que un cierre concurrente (`FOR UPDATE`) espere a las ventas en curso y no deje ventas fuera del total del turno; no estaba en el spec.

**Riesgos conocidos a vigilar al ejecutar:** (a) el stub JDBC debe soportar `getInt` (nota en Task 3); (b) si `INV_MODIFICACION_CONCURRENTE` aparece bajo contención, el `FOR UPDATE` de inventario debería evitarlo — el test de concurrencia lo detectaría; (c) `ventas` y `inventario` duplican `TransaccionPort`/`SpringTransaccionAdapter` con `compras` (seguimiento ya registrado).

**Consistencia de tipos:** `Venta`/`LineaVenta`/`PagoEfectivo` (Task 1) coinciden con los accesores usados por `VentaJdbcWriteAdapter`, `RegistrarVentaHandler` y `VentasFixtures`; `VentaResult` (15 campos) coincide entre Tasks 2, 3 y 4; `SalidaSolicitada` (8 campos) coincide entre Tasks 2 y 3; `VentaWritePort`/`TurnoWritePort#bloquearTurnoAbierto`/`ReferenciasVentasPort` coinciden entre Tasks 2 y 3; las firmas de `VentasApiMapper` coinciden con las del controller.
