package com.softprimesolutions.compras.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.command.CrearProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.ItemRecepcionInput;
import com.softprimesolutions.compras.application.dto.command.LineaOrdenCompraInput;
import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.dto.command.TransicionOrden;
import com.softprimesolutions.compras.application.dto.command.TransicionarOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.compras.application.port.in.CrearOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.in.CrearProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.RegistrarRecepcionUseCase;
import com.softprimesolutions.compras.application.port.in.TransicionarOrdenCompraUseCase;
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
class ComprasConcurrencyIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("3b5d7f9a-2c4e-4a6b-8d0f-1e3a5c7b9d2f");
    private static final String UNIDAD = "UNDCCO";

    @Autowired
    private CrearProveedorUseCase crearProveedor;

    @Autowired
    private CrearOrdenCompraUseCase crearOrden;

    @Autowired
    private TransicionarOrdenCompraUseCase transicionarOrden;

    @Autowired
    private RegistrarRecepcionUseCase registrarRecepcion;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID actor = UUID.randomUUID();
    private UUID almacenId;
    private UUID ordenId;

    @BeforeEach
    void createCommittedMasterDataAndAnIssuedOrderOfTen() {
        cleanUp();
        var skuId = UUID.randomUUID();
        almacenId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'CMPCONC', 'Tenant compras concurrencia', 'tenant-compras-conc', 'test')
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.empresa_operadora (tenant_id, ruc, razon_social)
                        SELECT id, '20123456786', 'Compras Concurrencia SAC' FROM sch_admin.tenant
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
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-CCO-1', 'Producto de compra', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();
        var establecimientoId = jdbcClient.sql("""
                        SELECT s.uuid_publico FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query(UUID.class).single();

        var proveedorId = ok(crearProveedor.execute(new CrearProveedorCommand(TENANT_ID, actor, new ProveedorInput(
                null, "20100070970", "Laboratorios Concurrentes SAC", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null)))).id();
        ordenId = ok(crearOrden.execute(new CrearOrdenCompraCommand(
                TENANT_ID, actor, proveedorId, establecimientoId, null, null, null, null, null, null,
                List.of(new LineaOrdenCompraInput(
                        skuId, new BigDecimal("10"), UNIDAD, new BigDecimal("5"), null, null, null, null))))).id();
        ok(transicionarOrden.execute(new TransicionarOrdenCompraCommand(
                TENANT_ID, ordenId, TransicionOrden.APROBAR, null, actor)));
        ok(transicionarOrden.execute(new TransicionarOrdenCompraCommand(
                TENANT_ID, ordenId, TransicionOrden.EMITIR, null, actor)));
    }

    @AfterEach
    void cleanUp() {
        for (var table : List.of(
                "sch_inventario.movimiento_inventario", "sch_inventario.posicion_inventario",
                "sch_inventario.lote", "sch_abastecimiento.recepcion_compra_linea",
                "sch_abastecimiento.recepcion_compra", "sch_abastecimiento.orden_compra_linea",
                "sch_abastecimiento.orden_compra", "sch_abastecimiento.proveedor", "sch_catalogo.sku_comercial",
                "sch_organizacion.almacen", "sch_organizacion.establecimiento_farmaceutico",
                "sch_organizacion.empresa_operadora")) {
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
    void aReceptionWithAnExpiredItemRollsBackTheStockOfTheValidOnes() {
        var vencimiento = LocalDate.now().plusYears(1);
        var items = List.of(
                item("LOTE-OK", vencimiento, "3"),
                item("LOTE-VENCIDO", LocalDate.now().minusDays(1), "2"));

        var resultado = registrarRecepcion.execute(command(items, null));

        assertThat(resultado.fold(recepcion -> "OK", ApplicationError::code)).isEqualTo("INV_LOTE_VENCIDO");
        assertThat(count("sch_abastecimiento.recepcion_compra")).isZero();
        assertThat(count("sch_abastecimiento.recepcion_compra_linea")).isZero();
        assertThat(count("sch_inventario.lote")).isZero();
        assertThat(count("sch_inventario.posicion_inventario")).isZero();
        assertThat(count("sch_inventario.movimiento_inventario")).isZero();
        assertThat(estadoOrden()).isEqualTo("EMITIDA");
    }

    @Test
    void twoSimultaneousReceptionsOfEightAgainstAnOrderOfTenNeverExceedIt() throws Exception {
        var results = runConcurrently(2, index -> command(List.of(item("LOTE-A", vencimiento(), "8")), null));

        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(results.stream().filter(Result::isFailure)
                .map(result -> result.fold(recepcion -> "OK", ApplicationError::code)).toList())
                .containsExactly("COM_RECEPCION_EXCEDE_PENDIENTE");
        assertThat(physicalStock()).isEqualByComparingTo("8");
        assertThat(count("sch_abastecimiento.recepcion_compra")).isEqualTo(1);
        assertThat(estadoOrden()).isEqualTo("PARCIALMENTE_RECIBIDA");
    }

    @Test
    void manySimultaneousSmallReceptionsAllocateExactlyTheOrderedQuantity() throws Exception {
        var results = runConcurrently(8, index -> command(List.of(item("LOTE-B", vencimiento(), "2")), null));

        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(5);
        assertThat(results.stream().filter(Result::isFailure)
                .map(result -> result.fold(recepcion -> "OK", ApplicationError::code)).distinct().toList())
                .isNotEmpty()
                .isSubsetOf("COM_RECEPCION_EXCEDE_PENDIENTE", "COM_RECEPCION_ORDEN_NO_RECEPCIONABLE");
        assertThat(physicalStock()).isEqualByComparingTo("10");
        assertThat(count("sch_abastecimiento.recepcion_compra")).isEqualTo(5);
        assertThat(count("sch_inventario.movimiento_inventario")).isEqualTo(5);
        assertThat(count("sch_inventario.lote")).isEqualTo(1);
        assertThat(estadoOrden()).isEqualTo("RECIBIDA");
    }

    @Test
    void concurrentRetriesWithTheSameIdempotencyKeyCreateOneReceptionAndReturnTheSameResult() throws Exception {
        var results = runConcurrently(5, index -> command(List.of(item("LOTE-C", vencimiento(), "3")), "clave-conc"));

        var recepciones = results.stream()
                .map(result -> result.fold(recepcion -> recepcion, error -> { throw new AssertionError(error); }))
                .toList();
        assertThat(recepciones).extracting(RecepcionResult::id).containsOnly(recepciones.getFirst().id());
        assertThat(count("sch_abastecimiento.recepcion_compra")).isEqualTo(1);
        assertThat(count("sch_inventario.movimiento_inventario")).isEqualTo(1);
        assertThat(physicalStock()).isEqualByComparingTo("3");
    }

    private RegistrarRecepcionCommand command(List<ItemRecepcionInput> items, String clave) {
        return new RegistrarRecepcionCommand(
                TENANT_ID, actor, clave, ordenId, almacenId, null, null, null, null, null, null, null, null, items);
    }

    private static ItemRecepcionInput item(String lote, LocalDate vencimiento, String cantidad) {
        return new ItemRecepcionInput(
                1, lote, null, vencimiento, new BigDecimal(cantidad), null, null, null, null);
    }

    private static LocalDate vencimiento() {
        return LocalDate.now().plusYears(1);
    }

    private List<Result<RecepcionResult, ApplicationError>> runConcurrently(
            int threads, java.util.function.IntFunction<RegistrarRecepcionCommand> comando) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        try {
            var futures = new ArrayList<Future<Result<RecepcionResult, ApplicationError>>>();
            for (var index = 0; index < threads; index++) {
                var solicitud = comando.apply(index);
                Callable<Result<RecepcionResult, ApplicationError>> recepcion = () -> {
                    start.await();
                    return registrarRecepcion.execute(solicitud);
                };
                futures.add(pool.submit(recepcion));
            }
            start.countDown();
            var results = new ArrayList<Result<RecepcionResult, ApplicationError>>();
            for (var future : futures) results.add(future.get());
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table
                + " WHERE tenant_id IN (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)")
                .param("tenantId", TENANT_ID).query(Long.class).single();
    }

    private BigDecimal physicalStock() {
        return jdbcClient.sql("""
                        SELECT COALESCE(SUM(p.cantidad_fisica), 0) FROM sch_inventario.posicion_inventario p
                          JOIN sch_admin.tenant t ON t.id = p.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query(BigDecimal.class).single();
    }

    private String estadoOrden() {
        return jdbcClient.sql("SELECT estado FROM sch_abastecimiento.orden_compra WHERE uuid_publico = :ordenId")
                .param("ordenId", ordenId).query(String.class).single();
    }

    private static <T> T ok(Result<T, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }
}
