package com.softprimesolutions.organizacion.infrastructure.persistence.read.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EmpresaProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizacionJdbcReadRepository {

    private static final String EMPRESA_SELECT = """
            SELECT e.uuid_publico, t.uuid_publico AS tenant_uuid, e.ruc, e.razon_social,
                   e.nombre_comercial, e.direccion_fiscal, e.ubigeo_fiscal, e.telefono, e.email,
                   e.sitio_web, e.moneda_funcional, e.zona_horaria, e.permite_venta_online,
                   e.estado, e.created_at, e.updated_at
            FROM sch_organizacion.empresa_operadora e
            JOIN sch_admin.tenant t ON t.id = e.tenant_id
            """;
    private static final String EMPRESA_FROM = """
            FROM sch_organizacion.empresa_operadora e
            JOIN sch_admin.tenant t ON t.id = e.tenant_id
            """;
    private static final String EMPRESA_SEARCH = """
            WHERE t.uuid_publico = :tenantId AND e.es_activo = '1'
              AND (:search = '' OR LOWER(e.razon_social) LIKE :pattern OR LOWER(e.ruc) LIKE :pattern
                OR LOWER(COALESCE(e.nombre_comercial, '')) LIKE :pattern)
            """;

    private static final String ESTABLECIMIENTO_SELECT = """
            SELECT s.uuid_publico, t.uuid_publico AS tenant_uuid, e.uuid_publico AS empresa_uuid,
                   s.codigo, s.nombre, s.tipo_establecimiento, s.categoria_regulatoria_codigo,
                   s.codigo_anexo_sunat, s.codigo_digemid, s.direccion, s.ubigeo, s.referencia,
                   s.latitud, s.longitud, s.telefono, s.email, s.es_principal,
                   s.permite_venta_online, s.permite_delivery, s.perfil_operacion,
                   s.zona_horaria, s.estado_operativo, s.created_at, s.updated_at
            FROM sch_organizacion.establecimiento_farmaceutico s
            JOIN sch_organizacion.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
            JOIN sch_admin.tenant t ON t.id = s.tenant_id
            """;
    private static final String ESTABLECIMIENTO_FROM = """
            FROM sch_organizacion.establecimiento_farmaceutico s
            JOIN sch_organizacion.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
            JOIN sch_admin.tenant t ON t.id = s.tenant_id
            """;
    private static final String ESTABLECIMIENTO_SEARCH = """
            WHERE t.uuid_publico = :tenantId AND s.es_activo = '1'
              AND (CAST(:empresaId AS uuid) IS NULL OR e.uuid_publico = CAST(:empresaId AS uuid))
              AND (:search = '' OR LOWER(s.codigo) LIKE :pattern OR LOWER(s.nombre) LIKE :pattern)
            """;

    private static final String ALMACEN_SELECT = """
            SELECT a.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                   a.codigo, a.nombre, a.tipo, a.permite_lotes, a.permite_vencimiento,
                   a.permite_venta, a.permite_despacho, a.control_temperatura,
                   a.temperatura_min_c, a.temperatura_max_c, a.es_activo, a.created_at, a.updated_at
            FROM sch_organizacion.almacen a
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
            JOIN sch_admin.tenant t ON t.id = a.tenant_id
            """;
    private static final String ALMACEN_FROM = """
            FROM sch_organizacion.almacen a
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
            JOIN sch_admin.tenant t ON t.id = a.tenant_id
            """;
    private static final String ALMACEN_SEARCH = """
            WHERE t.uuid_publico = :tenantId
              AND (CAST(:establecimientoId AS uuid) IS NULL OR s.uuid_publico = CAST(:establecimientoId AS uuid))
              AND (:search = '' OR LOWER(a.codigo) LIKE :pattern OR LOWER(a.nombre) LIKE :pattern)
            """;

    private static final String TERMINAL_SELECT = """
            SELECT p.uuid_publico, t.uuid_publico AS tenant_uuid, s.uuid_publico AS establecimiento_uuid,
                   p.codigo, p.nombre, p.serie_boleta_defecto, p.serie_factura_defecto,
                   p.numero_serie_equipo, p.hostname, CAST(p.ip_equipo AS text) AS ip_equipo,
                   p.impresora_codigo, p.store_edge_habilitado, p.estado, p.created_at, p.updated_at
            FROM sch_organizacion.terminal_pos p
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
            JOIN sch_admin.tenant t ON t.id = p.tenant_id
            """;
    private static final String TERMINAL_FROM = """
            FROM sch_organizacion.terminal_pos p
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
            JOIN sch_admin.tenant t ON t.id = p.tenant_id
            """;
    private static final String TERMINAL_SEARCH = """
            WHERE t.uuid_publico = :tenantId AND p.es_activo = '1'
              AND (CAST(:establecimientoId AS uuid) IS NULL OR s.uuid_publico = CAST(:establecimientoId AS uuid))
              AND (:search = '' OR LOWER(p.codigo) LIKE :pattern OR LOWER(p.nombre) LIKE :pattern)
            """;

    private static final String PAGE = "LIMIT :limit OFFSET :offset";

    private final JdbcClient jdbcClient;

    public OrganizacionJdbcReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<EmpresaProjection> findEmpresas(UUID tenantId, String search, int offset, int limit) {
        return page(searchStatement(
                EMPRESA_SELECT + EMPRESA_SEARCH + "ORDER BY e.razon_social, e.id " + PAGE, tenantId, search),
                this::mapEmpresa, offset, limit);
    }

    public long countEmpresas(UUID tenantId, String search) {
        return count(searchStatement("SELECT COUNT(*) " + EMPRESA_FROM + EMPRESA_SEARCH, tenantId, search));
    }

    public Optional<EmpresaProjection> findEmpresaById(UUID tenantId, UUID empresaId) {
        return jdbcClient.sql(EMPRESA_SELECT + "WHERE t.uuid_publico = :tenantId AND e.uuid_publico = :empresaId")
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .query(this::mapEmpresa)
                .optional();
    }

    public List<EmpresaProjection> findAllEmpresasActivas(UUID tenantId) {
        return jdbcClient.sql(EMPRESA_SELECT
                        + "WHERE t.uuid_publico = :tenantId AND e.es_activo = '1' ORDER BY e.razon_social, e.id")
                .param("tenantId", tenantId)
                .query(this::mapEmpresa)
                .list();
    }

    public List<EstablecimientoProjection> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int offset, int limit) {
        var statement = searchStatement(
                ESTABLECIMIENTO_SELECT + ESTABLECIMIENTO_SEARCH + "ORDER BY s.nombre, s.id " + PAGE,
                tenantId, search).param("empresaId", empresaId);
        return page(statement, this::mapEstablecimiento, offset, limit);
    }

    public long countEstablecimientos(UUID tenantId, UUID empresaId, String search) {
        return count(searchStatement(
                "SELECT COUNT(*) " + ESTABLECIMIENTO_FROM + ESTABLECIMIENTO_SEARCH, tenantId, search)
                .param("empresaId", empresaId));
    }

    public Optional<EstablecimientoProjection> findEstablecimientoById(UUID tenantId, UUID establecimientoId) {
        return jdbcClient.sql(ESTABLECIMIENTO_SELECT
                        + "WHERE t.uuid_publico = :tenantId AND s.uuid_publico = :establecimientoId")
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .query(this::mapEstablecimiento)
                .optional();
    }

    public List<EstablecimientoProjection> findAllEstablecimientosActivos(UUID tenantId) {
        return jdbcClient.sql(ESTABLECIMIENTO_SELECT
                        + "WHERE t.uuid_publico = :tenantId AND s.es_activo = '1' ORDER BY s.nombre, s.id")
                .param("tenantId", tenantId)
                .query(this::mapEstablecimiento)
                .list();
    }

    public List<AlmacenProjection> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int offset, int limit) {
        var statement = searchStatement(
                ALMACEN_SELECT + ALMACEN_SEARCH + "ORDER BY a.nombre, a.id " + PAGE, tenantId, search)
                .param("establecimientoId", establecimientoId);
        return page(statement, this::mapAlmacen, offset, limit);
    }

    public long countAlmacenes(UUID tenantId, UUID establecimientoId, String search) {
        return count(searchStatement("SELECT COUNT(*) " + ALMACEN_FROM + ALMACEN_SEARCH, tenantId, search)
                .param("establecimientoId", establecimientoId));
    }

    public Optional<AlmacenProjection> findAlmacenById(UUID tenantId, UUID almacenId) {
        return jdbcClient.sql(ALMACEN_SELECT + "WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId")
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .query(this::mapAlmacen)
                .optional();
    }

    public List<AlmacenProjection> findAllAlmacenes(UUID tenantId) {
        return jdbcClient.sql(ALMACEN_SELECT + "WHERE t.uuid_publico = :tenantId ORDER BY a.nombre, a.id")
                .param("tenantId", tenantId)
                .query(this::mapAlmacen)
                .list();
    }

    public List<TerminalProjection> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int offset, int limit) {
        var statement = searchStatement(
                TERMINAL_SELECT + TERMINAL_SEARCH + "ORDER BY p.nombre, p.id " + PAGE, tenantId, search)
                .param("establecimientoId", establecimientoId);
        return page(statement, this::mapTerminal, offset, limit);
    }

    public long countTerminales(UUID tenantId, UUID establecimientoId, String search) {
        return count(searchStatement("SELECT COUNT(*) " + TERMINAL_FROM + TERMINAL_SEARCH, tenantId, search)
                .param("establecimientoId", establecimientoId));
    }

    public Optional<TerminalProjection> findTerminalById(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TERMINAL_SELECT + "WHERE t.uuid_publico = :tenantId AND p.uuid_publico = :terminalId")
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query(this::mapTerminal)
                .optional();
    }

    public List<TerminalProjection> findAllTerminalesActivos(UUID tenantId) {
        return jdbcClient.sql(TERMINAL_SELECT
                        + "WHERE t.uuid_publico = :tenantId AND p.es_activo = '1' ORDER BY p.nombre, p.id")
                .param("tenantId", tenantId)
                .query(this::mapTerminal)
                .list();
    }

    private JdbcClient.StatementSpec searchStatement(String sql, UUID tenantId, String search) {
        var filter = normalizeSearch(search);
        return jdbcClient.sql(sql)
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%');
    }

    private static <T> List<T> page(JdbcClient.StatementSpec statement, RowMapper<T> mapper, int offset, int limit) {
        return statement.param("limit", limit).param("offset", offset).query(mapper).list();
    }

    private static long count(JdbcClient.StatementSpec statement) {
        return statement.query(Long.class).single();
    }

    private EmpresaProjection mapEmpresa(ResultSet rs, int rowNumber) throws SQLException {
        return new EmpresaProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getString("ruc"), rs.getString("razon_social"), rs.getString("nombre_comercial"),
                rs.getString("direccion_fiscal"), rs.getString("ubigeo_fiscal"), rs.getString("telefono"),
                rs.getString("email"), rs.getString("sitio_web"), rs.getString("moneda_funcional"),
                rs.getString("zona_horaria"), rs.getBoolean("permite_venta_online"), rs.getString("estado"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private EstablecimientoProjection mapEstablecimiento(ResultSet rs, int rowNumber) throws SQLException {
        return new EstablecimientoProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("empresa_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("tipo_establecimiento"), rs.getString("categoria_regulatoria_codigo"),
                rs.getString("codigo_anexo_sunat"), rs.getString("codigo_digemid"), rs.getString("direccion"),
                rs.getString("ubigeo"), rs.getString("referencia"), rs.getBigDecimal("latitud"),
                rs.getBigDecimal("longitud"), rs.getString("telefono"), rs.getString("email"),
                rs.getBoolean("es_principal"), rs.getBoolean("permite_venta_online"),
                rs.getBoolean("permite_delivery"), rs.getString("perfil_operacion"),
                rs.getString("zona_horaria"), rs.getString("estado_operativo"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private AlmacenProjection mapAlmacen(ResultSet rs, int rowNumber) throws SQLException {
        return new AlmacenProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("tipo"), rs.getBoolean("permite_lotes"), rs.getBoolean("permite_vencimiento"),
                rs.getBoolean("permite_venta"), rs.getBoolean("permite_despacho"),
                rs.getBoolean("control_temperatura"), rs.getBigDecimal("temperatura_min_c"),
                rs.getBigDecimal("temperatura_max_c"), "1".equals(rs.getString("es_activo")),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private TerminalProjection mapTerminal(ResultSet rs, int rowNumber) throws SQLException {
        return new TerminalProjection(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("serie_boleta_defecto"), rs.getString("serie_factura_defecto"),
                rs.getString("numero_serie_equipo"), rs.getString("hostname"), rs.getString("ip_equipo"),
                rs.getString("impresora_codigo"), rs.getBoolean("store_edge_habilitado"), rs.getString("estado"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
