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
    void createsCategoriaAndProductoTablesWithExpectedConstraints() {
        var jdbc = JdbcClient.create(dataSource);

        var categoriaExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'categoria'
                        """)
                .query(Long.class).single();
        assertThat(categoriaExists).isEqualTo(1L);

        var productoExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'producto'
                        """)
                .query(Long.class).single();
        assertThat(productoExists).isEqualTo(1L);

        var categoriaUniqueNombre = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'uk_cat_categoria_tenant_nombre'
                        """)
                .query(Long.class).single();
        assertThat(categoriaUniqueNombre).isEqualTo(1L);

        var productoFkCategoria = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'fk_cat_producto_categoria'
                        """)
                .query(Long.class).single();
        assertThat(productoFkCategoria).isEqualTo(1L);

        var productoMedicamentoCheck = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'ck_cat_producto_medicamento_requiere_datos'
                        """)
                .query(Long.class).single();
        assertThat(productoMedicamentoCheck).isEqualTo(1L);

        var permisosNuevos = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso
                         WHERE codigo IN ('catalogo.categorias.gestionar', 'catalogo.categorias.consultar',
                                           'catalogo.productos.gestionar', 'catalogo.productos.consultar')
                        """)
                .query(Long.class).single();
        assertThat(permisosNuevos).isEqualTo(4L);
    }
}
