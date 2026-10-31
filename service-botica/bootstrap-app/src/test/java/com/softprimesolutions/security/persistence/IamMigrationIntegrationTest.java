package com.softprimesolutions.security.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class IamMigrationIntegrationTest {

    private static final Pattern TOKEN_HASH_COLUMN = Pattern.compile("token_hash\\s+CHAR\\(64\\)");

    @Test
    void packagesTheCanonicalSecuritySchemaAndRemovesTheLegacyIamSchema() throws Exception {
        var canonical = new ClassPathResource("migrations/V013__seguridad_iam.sql");
        assertTrue(canonical.exists());

        var sql = canonical.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.contains("CREATE TABLE sch_seguridad.usuario ("));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.identidad"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.membership"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.rol_permiso"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.usuario_rol_ambito"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.sesion_usuario"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.dispositivo_tienda"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.credencial_local"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.token_refresh"));
        assertTrue(sql.contains("CREATE TABLE sch_seguridad.token_recuperacion_password"));
        assertTrue(TOKEN_HASH_COLUMN.matcher(sql).find());
        assertFalse(sql.contains("refresh_token VARCHAR"));
        assertFalse(sql.contains("reset_token VARCHAR"));

        assertFalse(new ClassPathResource("db/migration/V1__create_iam_authorization.sql").exists());
        assertFalse(new ClassPathResource(
                "db/migration/V021__separar_identidad_membership.sql").exists());
        assertTrue(new ClassPathResource(
                "db/migration/V019__seed_security_administration_permissions.sql").exists());
    }
}
