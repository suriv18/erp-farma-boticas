package com.softprimesolutions.ventas.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class VentasMigrationsIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void seedsTheVentasPermissionsAndGrantsThemToTheFarmalabAdministrator() {
        var permisos = jdbcClient.sql("""
                        SELECT codigo FROM sch_seguridad.permiso
                         WHERE codigo LIKE 'ventas.%' AND es_activo = '1' ORDER BY codigo
                        """).query(String.class).list();
        assertThat(permisos).containsExactly(
                "ventas.turnos.abrir", "ventas.turnos.cerrar", "ventas.turnos.consultar",
                "ventas.ventas.consultar", "ventas.ventas.registrar");

        var concedidos = jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.rol r ON r.id = rp.rol_id
                          JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE r.codigo = 'ADMIN' AND p.codigo LIKE 'ventas.%'
                        """).query(Long.class).single();
        assertThat(concedidos).isEqualTo(5L);
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
}
