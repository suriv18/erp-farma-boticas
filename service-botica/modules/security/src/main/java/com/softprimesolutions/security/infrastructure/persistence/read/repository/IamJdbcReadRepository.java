package com.softprimesolutions.security.infrastructure.persistence.read.repository;

import com.softprimesolutions.security.infrastructure.persistence.read.projection.PermisoProjection;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.RolProjection;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.UsuarioProjection;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IamJdbcReadRepository {

    private static final String USER_FILTER = """
            FROM sch_seguridad.usuario u
            JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
            LEFT JOIN sch_seguridad.identidad_externa ie
              ON ie.usuario_id = u.id
             AND ie.id = (SELECT MIN(ie2.id) FROM sch_seguridad.identidad_externa ie2 WHERE ie2.usuario_id = u.id)
            WHERE t.uuid_publico = :tenantId
              AND (:search = ''
                OR LOWER(COALESCE(u.nombre_mostrar, '')) LIKE :pattern
                OR LOWER(COALESCE(u.email::text, '')) LIKE :pattern
                OR LOWER(COALESCE(u.username::text, '')) LIKE :pattern
                OR LOWER(COALESCE(u.numero_documento, '')) LIKE :pattern)
            """;
    private static final String ROLE_FILTER = """
            FROM sch_seguridad.rol r
            JOIN sch_farmacia.tenant t ON t.id = r.tenant_id
            WHERE t.uuid_publico = :tenantId
              AND (:search = '' OR LOWER(r.codigo) LIKE :pattern OR LOWER(r.nombre) LIKE :pattern)
            """;

    private final JdbcClient jdbcClient;

    public IamJdbcReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<UsuarioProjection> findUsers(UUID tenantId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT u.uuid_publico, t.uuid_publico AS tenant_uuid,
                               ie.provider, ie.issuer, ie.subject, ie.email_claim,
                               u.tipo_documento, u.numero_documento, u.nombres, u.apellidos,
                               u.username, u.email, u.nombre_mostrar, u.telefono,
                               u.requiere_cambio_credencial, u.mfa_requerido,
                               u.estado, u.created_at, u.updated_at
                        """ + USER_FILTER + " ORDER BY u.nombre_mostrar, u.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> new UsuarioProjection(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("provider"),
                        rs.getString("issuer"),
                        rs.getString("subject"),
                        rs.getString("email_claim"),
                        rs.getString("tipo_documento"),
                        rs.getString("numero_documento"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("nombre_mostrar"),
                        rs.getString("telefono"),
                        rs.getBoolean("requiere_cambio_credencial"),
                        rs.getBoolean("mfa_requerido"),
                        rs.getString("estado"),
                        toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                        toInstant(rs.getObject("updated_at", OffsetDateTime.class))))
                .list();
    }

    public long countUsers(UUID tenantId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + USER_FILTER)
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public List<RolProjection> findRoles(UUID tenantId, String search, int offset, int limit) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT r.uuid_publico, t.uuid_publico AS tenant_uuid,
                               r.codigo, r.nombre, r.descripcion, r.tipo_rol, r.es_sistema,
                               r.estado, r.created_at, r.updated_at
                        """ + ROLE_FILTER + " ORDER BY r.nombre, r.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> {
                    var roleId = rs.getObject("uuid_publico", UUID.class);
                    return new RolProjection(
                            roleId,
                            rs.getObject("tenant_uuid", UUID.class),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getString("tipo_rol"),
                            rs.getBoolean("es_sistema"),
                            findPermissionCodes(roleId),
                            rs.getString("estado"),
                            toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                            toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
                })
                .list();
    }

    public long countRoles(UUID tenantId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("SELECT COUNT(*) " + ROLE_FILTER)
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query(Long.class)
                .single();
    }

    public List<PermisoProjection> findPermissions(String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql("""
                        SELECT m.codigo AS modulo_codigo, m.nombre AS modulo_nombre,
                               p.codigo, p.recurso, p.accion, p.nombre, p.descripcion,
                               p.es_critico, p.estado
                        FROM sch_seguridad.permiso p
                        JOIN sch_seguridad.modulo_sistema m ON m.id = p.modulo_id
                        WHERE (:search = ''
                          OR LOWER(p.codigo) LIKE :pattern
                          OR LOWER(p.nombre) LIKE :pattern
                          OR LOWER(m.codigo) LIKE :pattern)
                        ORDER BY m.orden, p.codigo
                        """)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .query((rs, rowNumber) -> new PermisoProjection(
                        rs.getString("modulo_codigo"), rs.getString("modulo_nombre"),
                        rs.getString("codigo"), rs.getString("recurso"), rs.getString("accion"),
                        rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getBoolean("es_critico"), rs.getString("estado")))
                .list();
    }

    private LinkedHashSet<String> findPermissionCodes(UUID roleId) {
        return jdbcClient.sql("""
                        SELECT p.codigo
                        FROM sch_seguridad.rol_permiso rp
                        JOIN sch_seguridad.rol r ON r.id = rp.rol_id
                        JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                        WHERE r.uuid_publico = :roleId AND rp.estado = 'ACTIVO'
                        ORDER BY p.codigo
                        """)
                .param("roleId", roleId)
                .query(String.class)
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static java.time.Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
