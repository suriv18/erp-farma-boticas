package com.softprimesolutions.clientes.db;

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
class ClientesMigrationsIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void seedsTheClientesPermissionsAndGrantsThemToTheFarmalabAdministrator() {
        var permisos = jdbcClient.sql("""
                        SELECT codigo FROM sch_seguridad.permiso
                         WHERE codigo LIKE 'clientes.%' AND es_activo = '1' ORDER BY codigo
                        """).query(String.class).list();
        assertThat(permisos).containsExactly(
                "clientes.clientes.bloquear", "clientes.clientes.consultar", "clientes.clientes.editar",
                "clientes.clientes.registrar", "clientes.consentimientos.gestionar");

        var concedidos = jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.rol r ON r.id = rp.rol_id
                          JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE r.codigo = 'ADMIN' AND p.codigo LIKE 'clientes.%'
                        """).query(Long.class).single();
        assertThat(concedidos).isEqualTo(5L);
    }

    @Test
    void seedsTheConsentPurposesAsSeparateCatalogEntries() {
        var finalidades = jdbcClient.sql("""
                        SELECT codigo FROM sch_catalogo.finalidad_consentimiento
                         WHERE es_activo = '1' ORDER BY codigo
                        """).query(String.class).list();

        assertThat(finalidades).containsExactly("FIDELIZACION", "IDENTIFICACION_VENTA", "MARKETING");
    }

    @Test
    void acceptsAClientWithAValidDocumentAndAnAnonymousOne() {
        assertThat(insertarCliente("NATURAL", "1", "12345678", "ACTIVO", null)).isEqualTo(1);
        assertThat(insertarCliente("JURIDICO", "6", "20123456789", "ACTIVO", null)).isEqualTo(1);
        assertThat(insertarCliente("SIN_IDENTIFICAR", null, null, "ACTIVO", null)).isEqualTo(1);
    }

    @Test
    void rejectsADocumentWithTheWrongLengthForItsType() {
        assertThatThrownBy(() -> insertarCliente("NATURAL", "1", "1234567", "ACTIVO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_documento_dni");
        assertThatThrownBy(() -> insertarCliente("JURIDICO", "6", "2012345678", "ACTIVO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_documento_ruc");
    }

    @Test
    void requiresTheDocumentTypeAndNumberTogetherAndNoneForAnAnonymousClient() {
        assertThatThrownBy(() -> insertarCliente("NATURAL", "1", null, "ACTIVO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_documento_completo");
        assertThatThrownBy(() -> insertarCliente("NATURAL", null, "12345678", "ACTIVO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_documento_completo");
        assertThatThrownBy(() -> insertarCliente("SIN_IDENTIFICAR", "1", "12345678", "ACTIVO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_sin_identificar");
    }

    @Test
    void aBlockedClientRequiresItsReason() {
        assertThatThrownBy(() -> insertarCliente("NATURAL", "1", "12345678", "BLOQUEADO", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_motivo_bloqueo");
        assertThatThrownBy(() -> insertarCliente("NATURAL", "1", "12345678", "BLOQUEADO", "  "))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_motivo_bloqueo");
        assertThat(insertarCliente("NATURAL", "1", "12345678", "BLOQUEADO", "Fraude reiterado"))
                .isEqualTo(1);
    }

    @Test
    void rejectsADocumentTypeOutsideTheSunatCatalog() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            return jdbcClient.sql("""
                            INSERT INTO sch_venta.cliente (tenant_id, tipo_cliente, tipo_documento, numero_documento)
                            VALUES (:tenant, 'NATURAL', 'Z', '123')
                            """).param("tenant", tenantFarmalab()).update();
        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cliente_tipo_documento");
    }

    @Test
    void aRevokedConsentRequiresItsRevocationDateAndAuthor() {
        assertThatThrownBy(() -> insertarConsentimiento("REVOCADO", false, "usuario-1"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_consent_revocacion");
        assertThatThrownBy(() -> insertarConsentimiento("OTORGADO", true, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_consent_revocacion");
        assertThatThrownBy(() -> insertarConsentimiento("REVOCADO", true, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cliente_consent_revocado_por");
        assertThat(insertarConsentimiento("REVOCADO", true, "usuario-1")).isEqualTo(1);
        assertThat(insertarConsentimiento("OTORGADO", false, null)).isEqualTo(1);
    }

    @Test
    void rejectsAConsentPurposeOutsideTheCatalog() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            var clienteId = insertarClienteReal();
            return jdbcClient.sql("""
                            INSERT INTO sch_venta.cliente_consentimiento
                                (tenant_id, cliente_id, finalidad_codigo, texto_version)
                            VALUES (:tenant, :cliente, 'INEXISTENTE', 'v1')
                            """).param("tenant", tenantFarmalab()).param("cliente", clienteId).update();
        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cliente_consent_finalidad");
    }

    @Test
    void aClientWithConsentsCannotBeDeletedSoTheEvidenceIsKept() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            var clienteId = insertarClienteReal();
            jdbcClient.sql("""
                            INSERT INTO sch_venta.cliente_consentimiento
                                (tenant_id, cliente_id, finalidad_codigo, texto_version)
                            VALUES (:tenant, :cliente, 'MARKETING', 'v1')
                            """).param("tenant", tenantFarmalab()).param("cliente", clienteId).update();
            return jdbcClient.sql("DELETE FROM sch_venta.cliente WHERE id = :id")
                    .param("id", clienteId).update();
        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_cliente_consent_cliente");
    }

    @Test
    void widensTheActorColumnsToTheSessionIdentifierLength() {
        for (var tabla : new String[] {"cliente", "cliente_consentimiento"}) {
            assertThat(jdbcClient.sql("""
                            SELECT character_maximum_length FROM information_schema.columns
                             WHERE table_schema = 'sch_venta' AND table_name = :tabla
                               AND column_name = 'created_by'
                            """).param("tabla", tabla).query(Integer.class).single()).isEqualTo(36);
        }
    }

    private Long tenantFarmalab() {
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE codigo = 'FARMALAB'")
                .query(Long.class).single();
    }

    private Long insertarClienteReal() {
        return jdbcClient.sql("""
                        INSERT INTO sch_venta.cliente (tenant_id, tipo_cliente, tipo_documento, numero_documento)
                        VALUES (:tenant, 'NATURAL', '1', '87654321') RETURNING id
                        """).param("tenant", tenantFarmalab()).query(Long.class).single();
    }

    private Integer insertarCliente(
            String tipoCliente, String tipoDocumento, String numeroDocumento, String estado, String motivo) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            jdbcClient.sql("SET LOCAL session_replication_role = replica").update();
            return jdbcClient.sql("""
                            INSERT INTO sch_venta.cliente
                                (tenant_id, tipo_cliente, tipo_documento, numero_documento, estado, motivo_bloqueo)
                            VALUES (-1, :tipoCliente, :tipoDocumento, :numeroDocumento, :estado, :motivo)
                            """)
                    .param("tipoCliente", tipoCliente)
                    .param("tipoDocumento", tipoDocumento)
                    .param("numeroDocumento", numeroDocumento)
                    .param("estado", estado)
                    .param("motivo", motivo)
                    .update();
        });
    }

    private Integer insertarConsentimiento(String estado, boolean conFechaRevocacion, String revocadoPor) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            jdbcClient.sql("SET LOCAL session_replication_role = replica").update();
            return jdbcClient.sql("""
                            INSERT INTO sch_venta.cliente_consentimiento
                                (tenant_id, cliente_id, finalidad_codigo, texto_version, estado,
                                 revocado_at, revocado_by)
                            VALUES (-1, -1, 'MARKETING', 'v1', :estado,
                                    CASE WHEN :conFecha THEN CURRENT_TIMESTAMP END, :revocadoPor)
                            """)
                    .param("estado", estado)
                    .param("conFecha", conFechaRevocacion)
                    .param("revocadoPor", revocadoPor)
                    .update();
        });
    }
}
