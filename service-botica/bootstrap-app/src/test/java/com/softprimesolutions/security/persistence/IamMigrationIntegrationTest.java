package com.softprimesolutions.security.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class IamMigrationIntegrationTest {

    @Test
    void packagesTheCanonicalSecuritySchemaAndRemovesTheLegacyIamSchema() throws Exception {
        var canonical = new ClassPathResource("migrations/V013__seguridad_iam.sql");
        assertTrue(canonical.exists());

        var sql = canonical.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.usuario"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.rol_permiso"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.usuario_rol_ambito"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.sesion_usuario"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.dispositivo_tienda"));

        assertFalse(new ClassPathResource("db/migration/V1__create_iam_authorization.sql").exists());
        assertTrue(new ClassPathResource(
                "db/migration/V018__seed_security_administration_permissions.sql").exists());

        var localAuth = new ClassPathResource("db/migration/V020__local_authentication_jwt.sql");
        assertTrue(localAuth.exists());
        var localAuthSql = localAuth.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(localAuthSql.contains("CREATE TABLE sch_seguridad.credencial_local"));
        assertTrue(localAuthSql.contains("CREATE TABLE sch_seguridad.token_refresh"));
        assertTrue(localAuthSql.contains("CREATE TABLE sch_seguridad.token_recuperacion_password"));
        assertTrue(localAuthSql.contains("token_hash CHAR(64)"));
        assertFalse(localAuthSql.contains("refresh_token VARCHAR"));
        assertFalse(localAuthSql.contains("reset_token VARCHAR"));
    }
}
