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
class MigrationV037Test {

    @Autowired
    private DataSource dataSource;

    @Test
    void addsTheNullableReferenceSalePriceColumnWithTheExpectedPrecision() {
        var columna = JdbcClient.create(dataSource).sql("""
                        SELECT is_nullable, numeric_precision, numeric_scale
                          FROM information_schema.columns
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'sku_comercial'
                           AND column_name = 'precio_venta_referencia'
                        """).query().singleRow();

        assertThat(columna).containsEntry("is_nullable", "YES");
        assertThat(((Number) columna.get("numeric_precision")).intValue()).isEqualTo(18);
        assertThat(((Number) columna.get("numeric_scale")).intValue()).isEqualTo(4);
    }

    @Test
    void forbidsNegativeReferencePrices() {
        assertThat(JdbcClient.create(dataSource).sql("""
                        SELECT pg_get_constraintdef(oid) FROM pg_constraint
                         WHERE conname = 'ck_sku_precio_venta_referencia'
                        """).query(String.class).single())
                .contains("precio_venta_referencia IS NULL").contains(">= (0)::numeric");
    }
}
