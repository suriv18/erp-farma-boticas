package com.softprimesolutions.ventas.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class VentasMigrationsIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void seedsTheVentasPermissionsAndGrantsThemToTheFarmalabAdministrator() {
        var permisos = jdbcClient.sql("""
                        SELECT codigo FROM sch_seguridad.permiso
                         WHERE codigo LIKE 'ventas.%' AND es_activo = '1' ORDER BY codigo
                        """).query(String.class).list();
        assertThat(permisos).containsExactly(
                "ventas.turnos.abrir", "ventas.turnos.cerrar", "ventas.turnos.consultar",
                "ventas.ventas.anular", "ventas.ventas.consultar", "ventas.ventas.registrar");

        var concedidos = jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.rol r ON r.id = rp.rol_id
                          JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE r.codigo = 'ADMIN' AND p.codigo LIKE 'ventas.%'
                        """).query(Long.class).single();
        assertThat(concedidos).isEqualTo(6L);
    }

    @Test
    void createsTheOperationSequenceAndWidensTheActorColumns() {
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'secuencia_operacion'
                           AND column_name IN ('tenant_id', 'terminal_id', 'ultimo_numero')
                        """).query(Long.class).single()).isEqualTo(3L);
        for (var tabla : new String[] {"turno_caja", "venta", "venta_linea", "medio_pago"}) {
            for (var columna : new String[] {"created_by", "updated_by"}) {
                assertThat(jdbcClient.sql("""
                                SELECT character_maximum_length FROM information_schema.columns
                                 WHERE table_schema = 'sch_venta' AND table_name = :tabla
                                   AND column_name = :columna
                                """).param("tabla", tabla).param("columna", columna)
                        .query(Integer.class).single()).isEqualTo(36);
            }
        }
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'venta'
                           AND column_name = 'huella_solicitud'
                        """).query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'venta_linea'
                           AND column_name = 'uuid_publico'
                        """).query(Long.class).single()).isEqualTo(1L);
    }

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

    @Test
    void indexesTheSalidasVentaOfASaleByTenantAndDocument() {
        var definicion = jdbcClient.sql("""
                        SELECT indexdef FROM pg_indexes
                         WHERE schemaname = 'sch_inventario' AND tablename = 'movimiento_inventario'
                           AND indexname = 'ix_movimiento_salida_venta_documento'
                        """).query(String.class).single();

        assertThat(definicion)
                .contains("(tenant_id, documento_uuid)")
                .contains("(tipo_movimiento)::text = 'SALIDA_VENTA'::text")
                .contains("(documento_tipo)::text = 'VENTA'::text");
    }

    @Test
    void anAnnulledSaleRequiresItsAuthorAndReason() {
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'ck_venta_anulacion_autor'
                        """).query(Long.class).single()).isEqualTo(1L);

        assertThatThrownBy(() -> insertarVentaAnulada(null, "Error de digitacion"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_venta_anulacion_autor");
        assertThatThrownBy(() -> insertarVentaAnulada(7L, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_venta_anulacion_autor");
        assertThat(insertarVentaAnulada(7L, "Error de digitacion")).isEqualTo(1);
    }

    private Integer insertarVentaAnulada(Long autor, String motivo) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            jdbcClient.sql("SET LOCAL session_replication_role = replica").update();
            return jdbcClient.sql("""
                            INSERT INTO sch_venta.venta
                                (tenant_id, empresa_id, establecimiento_id, terminal_id, turno_caja_id,
                                 vendedor_usuario_id, numero_operacion, idempotency_key, subtotal, total, estado,
                                 anulada_at, anulada_por_usuario_id, motivo_anulacion)
                            VALUES (-1, -1, -1, -1, -1, -1, 'OP-CHECK', 'clave-check', 0, 0, 'ANULADA',
                                    CURRENT_TIMESTAMP, :autor, :motivo)
                            """).param("autor", autor).param("motivo", motivo).update();
        });
    }
}
