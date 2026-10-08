package com.softprimesolutions.security.db;

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
class MigrationV021Test {

    @Autowired
    private DataSource dataSource;

    @Test
    void createsIdentidadAndMembershipTablesAndDropsUsuario() {
        var jdbc = JdbcClient.create(dataSource);

        var identidadExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'identidad'
                        """)
                .query(Long.class).single();
        assertThat(identidadExists).isEqualTo(1L);

        var membershipExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'membership'
                        """)
                .query(Long.class).single();
        assertThat(membershipExists).isEqualTo(1L);

        var usuarioExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'usuario'
                        """)
                .query(Long.class).single();
        assertThat(usuarioExists).isEqualTo(0L);

        var credencialLocalHasMembershipId = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'credencial_local'
                           AND column_name = 'membership_id'
                        """)
                .query(Long.class).single();
        assertThat(credencialLocalHasMembershipId).isEqualTo(1L);

        var identidadExternaHasIdentidadId = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_seguridad' AND table_name = 'identidad_externa'
                           AND column_name = 'identidad_id'
                        """)
                .query(Long.class).single();
        assertThat(identidadExternaHasIdentidadId).isEqualTo(1L);

        var emailUniqueGlobal = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_indexes
                         WHERE schemaname = 'sch_seguridad' AND tablename = 'identidad'
                           AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%email%'
                        """)
                .query(Long.class).single();
        assertThat(emailUniqueGlobal).isEqualTo(1L);
    }
}
