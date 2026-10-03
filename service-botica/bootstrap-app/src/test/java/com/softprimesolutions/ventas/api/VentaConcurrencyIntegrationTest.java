package com.softprimesolutions.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
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
    private AnularVentaUseCase anularVenta;

    @Autowired
    private RegistrarMovimientoUseCase registrarMovimiento;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID actor = UUID.randomUUID();
    private final UUID almacenId = UUID.randomUUID();
    private final UUID terminalId = UUID.randomUUID();
    private final UUID skuA = UUID.fromString("1aaaaaaa-0000-4000-8000-000000000001");
    private final UUID skuB = UUID.fromString("2bbbbbbb-0000-4000-8000-000000000002");

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
        assertThat(skuA).as("la linea valida se descuenta antes que la que falla").isLessThan(skuB);

        var resultado = vender("clave-1", linea(skuB, "95"), linea(skuA, "3"));

        assertThat(resultado.fold(venta -> "OK", ApplicationError::code)).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(stock(skuA)).isEqualByComparingTo("10");
        assertThat(stock(skuB)).isEqualByComparingTo("10");
        assertThat(count("sch_venta.venta")).isZero();
        assertThat(count("sch_venta.venta_linea")).isZero();
        assertThat(count("sch_venta.pago_venta")).isZero();
        assertThat(count("sch_venta.secuencia_operacion")).isZero();
        assertThat(movimientosEnKardex("SALIDA_VENTA")).isZero();
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
    void simultaneousRequestsWithTheSameKeyAndBodyReturnTheSameSaleAndDiscountOnce() throws Exception {
        var tareas = new ArrayList<Callable<Result<VentaResult, ApplicationError>>>();
        for (var indice = 0; indice < 4; indice++) tareas.add(() -> vender("clave-unica", linea(skuA, "4")));

        var resultados = concurrently(tareas);

        var ids = resultados.stream().map(resultado -> value(resultado).id()).distinct().toList();
        assertThat(ids).hasSize(1);
        assertThat(stock(skuA)).isEqualByComparingTo("6");
        assertThat(count("sch_venta.venta")).isEqualTo(1L);
        assertThat(movimientosEnKardex("SALIDA_VENTA")).isEqualTo(1L);
    }

    @Test
    void aSaleInFlightWhileTheTurnoClosesIsEitherCountedOrRejected() throws Exception {
        for (var ronda = 0; ronda < 5; ronda++) {
            var turno = turnoAbierto();
            var clave = "clave-cierre-" + ronda;

            var resultados = concurrently(List.<Callable<String>>of(
                    () -> vender(clave, linea(skuA, "1")).fold(venta -> "OK", ApplicationError::code),
                    () -> cerrar(turno).fold(cerrado -> "OK", ApplicationError::code)));

            assertThat(resultados.get(1)).isEqualTo("OK");
            assertThat(resultados.get(0)).isIn("OK", "VEN_TURNO_NO_ABIERTO");
            var esperado = resultados.get(0).equals("OK") ? "10.00" : "0.00";
            assertThat(totalVentasDelTurno(turno)).isEqualByComparingTo(esperado);
            assertThat(ventasConfirmadasDelTurno(turno)).isEqualByComparingTo(esperado);
            abrirTurno.execute(new AbrirTurnoCommand(TENANT_ID, actor, terminalId, new BigDecimal("100")))
                    .fold(abierto -> abierto, error -> { throw new AssertionError(error); });
        }
    }

    @Test
    void twoSimultaneousSalesOfEightFromTenNeverOversell() throws Exception {
        var resultados = concurrently(List.<Callable<Result<VentaResult, ApplicationError>>>of(
                () -> vender("clave-a", linea(skuA, "8")), () -> vender("clave-b", linea(skuA, "8"))));

        assertThat(resultados.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(resultados.stream().map(resultado -> resultado.fold(venta -> "OK", ApplicationError::code))
                .filter(codigo -> !codigo.equals("OK")).toList()).containsExactly("INV_STOCK_INSUFICIENTE");
        assertThat(stock(skuA)).isEqualByComparingTo("2");
        assertThat(count("sch_venta.venta")).isEqualTo(1L);
        assertThat(movimientosEnKardex("SALIDA_VENTA")).isEqualTo(1L);
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

        var cerrado = cerrarTurno.execute(
                        new CerrarTurnoCommand(TENANT_ID, actor, turnoAbierto(), new BigDecimal("150"), null))
                .fold(valor -> valor, error -> { throw new AssertionError(error); });

        assertThat(cerrado.totalVentasSistema()).isEqualByComparingTo("50.00");
        assertThat(cerrado.totalSistema()).isEqualByComparingTo("150.00");
        assertThat(cerrado.diferencia()).isEqualByComparingTo("0.00");
        assertThat(vender("clave-3", linea(skuA, "1")).fold(venta -> "OK", ApplicationError::code))
                .isEqualTo("VEN_TURNO_NO_ABIERTO");
    }

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
        assertThat(movimientosEnKardex("ANULACION_VENTA")).isZero();
    }

    @Test
    void twoSimultaneousAnnulmentsOfTheSameSaleRestoreTheStockExactlyOnce() throws Exception {
        var venta = value(vender("clave-1", linea(skuA, "4")));

        var resultados = concurrently(List.<Callable<Result<VentaResult, ApplicationError>>>of(
                () -> anular(venta.id(), "Primera"), () -> anular(venta.id(), "Segunda")));

        assertThat(resultados.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(resultados.stream().map(resultado -> resultado.fold(valor -> "OK", ApplicationError::code))
                .filter(codigo -> !codigo.equals("OK")).toList()).containsExactly("VEN_VENTA_ESTADO_INVALIDO");
        assertThat(stock(skuA)).isEqualByComparingTo("10");
        assertThat(movimientosEnKardex("SALIDA_VENTA")).isEqualTo(1L);
        assertThat(movimientosEnKardex("ANULACION_VENTA")).isEqualTo(1L);
    }

    @Test
    void anAnulacionRacingAgainstTheCloseOfTheTurnoNeverLeavesAnAnnulledSaleCountedInTheTotal() throws Exception {
        for (var ronda = 0; ronda < 5; ronda++) {
            var venta = value(vender("clave-ronda-" + ronda, linea(skuA, "1")));
            var turno = turnoAbierto();

            var resultados = concurrently(List.<Callable<String>>of(
                    () -> anular(venta.id(), "Carrera").fold(anulada -> "OK", ApplicationError::code),
                    () -> cerrar(turno).fold(cerrado -> "OK", ApplicationError::code)));

            assertThat(resultados.get(1)).isEqualTo("OK");
            assertThat(resultados.get(0)).isIn("OK", "VEN_TURNO_NO_ABIERTO");
            var estado = jdbcClient.sql("SELECT v.estado FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId")
                    .param("ventaId", venta.id()).query(String.class).single();
            assertThat(estado).isEqualTo(resultados.get(0).equals("OK") ? "ANULADA" : "CONFIRMADA");
            var esperado = estado.equals("ANULADA") ? "0.00" : "10.00";
            assertThat(totalVentasDelTurno(turno)).isEqualByComparingTo(esperado);
            assertThat(ventasConfirmadasDelTurno(turno)).isEqualByComparingTo(esperado);
            abrirTurno.execute(new AbrirTurnoCommand(TENANT_ID, actor, terminalId, new BigDecimal("100")))
                    .fold(abierto -> abierto, error -> { throw new AssertionError(error); });
        }
    }

    @Test
    void annullingASaleWhileAnotherSellsTheSameLotesNeverDeadlocks() throws Exception {
        var venta = value(vender("clave-base", linea(skuA, "4"), linea(skuB, "4")));

        var resultados = concurrently(List.<Callable<Result<VentaResult, ApplicationError>>>of(
                () -> anular(venta.id(), "Anular"),
                () -> vender("clave-otra", linea(skuB, "2"), linea(skuA, "2"))));

        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.isSuccess()).isTrue());
        assertThat(stock(skuA)).isEqualByComparingTo("8");
        assertThat(stock(skuB)).isEqualByComparingTo("8");
    }

    private Result<VentaResult, ApplicationError> anular(UUID ventaId, String motivo) {
        return anularVenta.execute(new AnularVentaCommand(TENANT_ID, actor, ventaId, motivo));
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

    private Result<TurnoResult, ApplicationError> cerrar(UUID turno) {
        return cerrarTurno.execute(new CerrarTurnoCommand(TENANT_ID, actor, turno, new BigDecimal("100"), null));
    }

    private UUID turnoAbierto() {
        return jdbcClient.sql("""
                        SELECT tc.uuid_publico FROM sch_venta.turno_caja tc
                          JOIN sch_admin.tenant t ON t.id = tc.tenant_id
                         WHERE t.uuid_publico = :tenantId AND tc.estado = 'ABIERTO'
                        """).param("tenantId", TENANT_ID).query(UUID.class).single();
    }

    private BigDecimal totalVentasDelTurno(UUID turno) {
        return jdbcClient.sql("SELECT total_ventas_sistema FROM sch_venta.turno_caja WHERE uuid_publico = :turnoId")
                .param("turnoId", turno).query(BigDecimal.class).single();
    }

    private BigDecimal ventasConfirmadasDelTurno(UUID turno) {
        return jdbcClient.sql("""
                        SELECT COALESCE(SUM(v.total), 0) FROM sch_venta.venta v
                          JOIN sch_venta.turno_caja tc ON tc.id = v.turno_caja_id
                         WHERE tc.uuid_publico = :turnoId AND v.estado = 'CONFIRMADA'
                        """).param("turnoId", turno).query(BigDecimal.class).single();
    }

    private <T> List<T> concurrently(List<Callable<T>> tareas) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(tareas.size());
        try {
            var futuros = new ArrayList<Future<T>>();
            for (var tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    start.await();
                    return tarea.call();
                }));
            }
            start.countDown();
            var resultados = new ArrayList<T>();
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

    private long movimientosEnKardex(String tipo) {
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = :tipo
                        """).param("tenantId", TENANT_ID).param("tipo", tipo).query(Long.class).single();
    }
}
