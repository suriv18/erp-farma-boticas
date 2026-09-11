package com.softprimesolutions.catalogo.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class MigrationV022Test {

    @Autowired
    private DataSource dataSource;

    @Test
    void seedsTwelveCatalogoPermissionsUnderTheExistingModule() {
        var jdbc = JdbcClient.create(dataSource);

        var permissionCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso p
                         JOIN sch_seguridad.modulo_sistema m ON m.id = p.modulo_id
                        WHERE m.codigo = 'CATALOGO'
                          AND p.codigo IN (
                            'catalogo.soporte.consultar', 'catalogo.soporte.gestionar',
                            'catalogo.principios-activos.consultar', 'catalogo.principios-activos.gestionar',
                            'catalogo.marcas.consultar', 'catalogo.marcas.gestionar',
                            'catalogo.categorias.consultar', 'catalogo.categorias.gestionar',
                            'catalogo.productos-regulados.consultar', 'catalogo.productos-regulados.gestionar',
                            'catalogo.skus.consultar', 'catalogo.skus.gestionar'
                          )
                        """)
                .query(Long.class).single();
        assertThat(permissionCount).isEqualTo(12L);

        var moduleCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO'
                        """)
                .query(Long.class).single();
        assertThat(moduleCount).isEqualTo(1L);
    }
}
