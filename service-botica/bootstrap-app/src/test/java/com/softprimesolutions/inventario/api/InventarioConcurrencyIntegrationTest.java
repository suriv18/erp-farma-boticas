package com.softprimesolutions.inventario.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class InventarioConcurrencyIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("9c2d4e6f-1a3b-4c5d-8e7f-0a1b2c3d4e5f");
    private static final String UNIDAD = "UNDCON";

    @Autowired
    private RegistrarMovimientoUseCase registrarMovimiento;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID actor = UUID.randomUUID();
    private UUID almacenId;
    private UUID skuId;
    private UUID loteId;

    @BeforeEach
    void createCommittedMasterDataAndAnInitialStockOfTen() {
        cleanUp();
        skuId = UUID.randomUUID();
        almacenId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'INVCONC', 'Tenant concurrencia', 'tenant-concurrencia', 'test')
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.empresa_operadora (tenant_id, ruc, razon_social)
                        SELECT id, '20123456786', 'Concurrencia SAC' FROM sch_admin.tenant
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
                            (uuid_publico, tenant_id, empresa_id, establecimiento_id, codigo, nombre)
                        SELECT :almacenId, s.tenant_id, s.empresa_id, s.id, 'ALM001', 'Almacen'
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("almacenId", almacenId).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-CON-1', 'Producto concurrente', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();

        var inicial = registrarMovimiento.execute(new RegistrarMovimientoCommand(
                TENANT_ID, almacenId, skuId, null, "L-CON", LocalDate.now().plusYears(1), "AJUSTE_INGRESO",
                new BigDecimal("10"), "Saldo inicial", UUID.randomUUID(), null));
        loteId = inicial.fold(MovimientoResult::loteId, error -> { throw new AssertionError(error); });
    }

    @AfterEach
    void cleanUp() {
        for (var table : List.of(
                "sch_inventario.movimiento_inventario", "sch_inventario.posicion_inventario",
                "sch_inventario.lote", "sch_catalogo.sku_comercial", "sch_organizacion.almacen",
                "sch_organizacion.establecimiento_farmaceutico", "sch_organizacion.empresa_operadora")) {
            jdbcClient.sql("DELETE FROM " + table
                    + " WHERE tenant_id IN (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)")
                    .param("tenantId", TENANT_ID).update();
        }
        jdbcClient.sql("DELETE FROM sch_admin.tenant WHERE uuid_publico = :tenantId")
                .param("tenantId", TENANT_ID).update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida WHERE codigo = :codigo")
                .param("codigo", UNIDAD).update();
    }

    @Test
    void twoSimultaneousSalidasOfEightFromTenNeverLeaveNegativeStock() throws Exception {
        var results = runConcurrently(2, "8");

        var successes = results.stream().filter(Result::isSuccess).count();
        var failureCodes = results.stream()
                .map(result -> result.fold(value -> "OK", ApplicationError::code)).filter(code -> !code.equals("OK"))
                .toList();
        assertThat(successes).isEqualTo(1);
        assertThat(failureCodes).containsExactly("INV_STOCK_INSUFICIENTE");
        assertThat(physicalStock()).isEqualByComparingTo("2");
        assertThat(salidasInKardex()).isEqualTo(1);
    }

    @Test
    void manySimultaneousSmallSalidasKeepStockAndKardexConsistent() throws Exception {
        var results = runConcurrently(12, "1");

        var successes = results.stream().filter(Result::isSuccess).count();
        assertThat(successes).isBetween(1L, 10L);
        assertThat(physicalStock()).isEqualByComparingTo(BigDecimal.valueOf(10 - successes));
        assertThat(salidasInKardex()).isEqualTo(successes);
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario
                         WHERE naturaleza = 'S' AND stock_posterior < 0 AND lote_id IN (
                               SELECT id FROM sch_inventario.lote WHERE uuid_publico = :loteId)
                        """).param("loteId", loteId).query(Long.class).single()).isZero();
        assertThat(jdbcClient.sql("""
                        SELECT version_lock FROM sch_inventario.posicion_inventario p
                          JOIN sch_inventario.lote l ON l.id = p.lote_id WHERE l.uuid_publico = :loteId
                        """).param("loteId", loteId).query(Long.class).single()).isEqualTo(successes);
    }

    @Test
    void concurrentRetriesWithTheSameIdempotencyKeyApplyTheMovementOnceAndReturnTheSameResult()
            throws Exception {
        var results = runConcurrently(6, "3", "clave-concurrente");

        var movimientos = results.stream()
                .map(result -> result.fold(movimiento -> movimiento, error -> { throw new AssertionError(error); }))
                .toList();
        assertThat(movimientos).extracting(MovimientoResult::id).containsOnly(movimientos.getFirst().id());
        assertThat(movimientos).extracting(MovimientoResult::stockPosterior).allSatisfy(
                stock -> assertThat(stock).isEqualByComparingTo("7"));
        assertThat(physicalStock()).isEqualByComparingTo("7");
        assertThat(salidasInKardex()).isEqualTo(1);
    }

    private List<Result<MovimientoResult, ApplicationError>> runConcurrently(int threads, String cantidad)
            throws Exception {
        return runConcurrently(threads, cantidad, null);
    }

    private List<Result<MovimientoResult, ApplicationError>> runConcurrently(
            int threads, String cantidad, String clave)
            throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Result<MovimientoResult, ApplicationError>>>();
            for (var index = 0; index < threads; index++) {
                Callable<Result<MovimientoResult, ApplicationError>> salida = () -> {
                    start.await();
                    return registrarMovimiento.execute(new RegistrarMovimientoCommand(
                            TENANT_ID, almacenId, skuId, loteId, null, null, "AJUSTE_SALIDA",
                            new BigDecimal(cantidad), "Salida concurrente", actor, clave));
                };
                futures.add(pool.submit(salida));
            }
            start.countDown();
            var results = new ArrayList<Result<MovimientoResult, ApplicationError>>();
            for (var future : futures) results.add(future.get());
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private BigDecimal physicalStock() {
        return jdbcClient.sql("""
                        SELECT p.cantidad_fisica FROM sch_inventario.posicion_inventario p
                          JOIN sch_inventario.lote l ON l.id = p.lote_id WHERE l.uuid_publico = :loteId
                        """).param("loteId", loteId).query(BigDecimal.class).single();
    }

    private long salidasInKardex() {
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario m
                          JOIN sch_inventario.lote l ON l.id = m.lote_id
                         WHERE l.uuid_publico = :loteId AND m.naturaleza = 'S'
                        """).param("loteId", loteId).query(Long.class).single();
    }
}
