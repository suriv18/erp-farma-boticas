package com.softprimesolutions.catalogo.infrastructure.persistence.read.repository;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoJdbcReadRepository {

    private final JdbcClient jdbcClient;

    public CatalogoJdbcReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<CondicionVentaResult> findCondicionesVenta(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, denominacion, requiere_receta, requiere_retencion, fuente,
                               version_fuente, vigente_desde, vigente_hasta, estado
                          FROM sch_farmacia.condicion_venta
                         WHERE :estado = '' OR estado = :estado
                         ORDER BY denominacion
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new CondicionVentaResult(
                        rs.getString("codigo"), rs.getString("denominacion"), rs.getBoolean("requiere_receta"),
                        rs.getBoolean("requiere_retencion"), rs.getString("fuente"), rs.getString("version_fuente"),
                        rs.getObject("vigente_desde", java.time.LocalDate.class),
                        rs.getObject("vigente_hasta", java.time.LocalDate.class), rs.getString("estado")))
                .list();
    }

    public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, denominacion, fuente, estado FROM sch_farmacia.forma_farmaceutica
                         WHERE :estado = '' OR estado = :estado
                         ORDER BY denominacion
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new FormaFarmaceuticaResult(
                        rs.getString("codigo"), rs.getString("denominacion"), rs.getString("fuente"),
                        rs.getString("estado")))
                .list();
    }

    public List<ViaAdministracionResult> findViasAdministracion(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, denominacion, fuente, estado FROM sch_farmacia.via_administracion
                         WHERE :estado = '' OR estado = :estado
                         ORDER BY denominacion
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new ViaAdministracionResult(
                        rs.getString("codigo"), rs.getString("denominacion"), rs.getString("fuente"),
                        rs.getString("estado")))
                .list();
    }

    public List<UnidadMedidaResult> findUnidadesMedida(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, denominacion, simbolo, permite_decimal, fuente, estado
                          FROM sch_farmacia.unidad_medida
                         WHERE :estado = '' OR estado = :estado
                         ORDER BY denominacion
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new UnidadMedidaResult(
                        rs.getString("codigo"), rs.getString("denominacion"), rs.getString("simbolo"),
                        rs.getBoolean("permite_decimal"), rs.getString("fuente"), rs.getString("estado")))
                .list();
    }

    public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT codigo, denominacion, norma_fuente, requiere_receta_especial, retiene_receta,
                               vigencia_receta_dias, estado
                          FROM sch_farmacia.clasificacion_controlada
                         WHERE :estado = '' OR estado = :estado
                         ORDER BY denominacion
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new ClasificacionControladaResult(
                        rs.getString("codigo"), rs.getString("denominacion"), rs.getString("norma_fuente"),
                        rs.getBoolean("requiere_receta_especial"), rs.getBoolean("retiene_receta"),
                        (Integer) rs.getObject("vigencia_receta_dias"), rs.getString("estado")))
                .list();
    }

    public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) {
        var textFilter = normalizeSearch(texto);
        var statusFilter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT uuid_publico, codigo_fuente, denominacion, nombre_normalizado, fuente, estado
                          FROM sch_farmacia.principio_activo
                         WHERE (:texto = '' OR LOWER(denominacion) LIKE :pattern)
                           AND (:estado = '' OR estado = :estado)
                         ORDER BY denominacion
                        """)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("estado", statusFilter)
                .query((rs, rowNumber) -> new PrincipioActivoResult(
                        rs.getObject("uuid_publico", UUID.class), rs.getString("codigo_fuente"),
                        rs.getString("denominacion"), rs.getString("nombre_normalizado"), rs.getString("fuente"),
                        rs.getString("estado")))
                .list();
    }

    public List<MarcaResult> findMarcas(UUID tenantId, String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT m.uuid_publico, t.uuid_publico AS tenant_uuid, m.codigo, m.nombre,
                               m.descripcion, m.estado
                          FROM sch_farmacia.marca m
                          JOIN sch_farmacia.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND (:estado = '' OR m.estado = :estado)
                         ORDER BY m.nombre
                        """)
                .param("tenantId", tenantId)
                .param("estado", filter)
                .query((rs, rowNumber) -> new MarcaResult(
                        rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getString("estado")))
                .list();
    }

    public List<CategoriaProductoResult> findCategoriasProducto(UUID tenantId, UUID categoriaPadreId, String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT c.uuid_publico, t.uuid_publico AS tenant_uuid,
                               padre.uuid_publico AS categoria_padre_uuid, c.codigo, c.nombre, c.descripcion,
                               c.nivel, c.orden, c.estado
                          FROM sch_farmacia.categoria_producto c
                          JOIN sch_farmacia.tenant t ON t.id = c.tenant_id
                          LEFT JOIN sch_farmacia.categoria_producto padre ON padre.id = c.categoria_padre_id
                         WHERE t.uuid_publico = :tenantId
                           AND (:categoriaPadreId IS NULL OR padre.uuid_publico = :categoriaPadreId)
                           AND (:estado = '' OR c.estado = :estado)
                         ORDER BY c.nivel, c.orden, c.nombre
                        """)
                .param("tenantId", tenantId)
                .param("categoriaPadreId", categoriaPadreId)
                .param("estado", filter)
                .query((rs, rowNumber) -> new CategoriaProductoResult(
                        rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                        rs.getObject("categoria_padre_uuid", UUID.class), rs.getString("codigo"),
                        rs.getString("nombre"), rs.getString("descripcion"), rs.getInt("nivel"),
                        rs.getInt("orden"), rs.getString("estado")))
                .list();
    }

    private static final String PRODUCTO_REGULADO_FILTER = """
             WHERE (:texto = '' OR LOWER(denominacion) LIKE :pattern)
               AND (:condicionVentaCodigo = '' OR condicion_venta_codigo = :condicionVentaCodigo)
               AND (:estadoRegulatorio = '' OR estado_regulatorio = :estadoRegulatorio)
            """;

    public List<ProductoReguladoResumen> findProductosRegulados(
            String texto, String condicionVentaCodigo, String estadoRegulatorio, int offset, int limit) {
        var textFilter = normalizeSearch(texto);
        return jdbcClient.sql("""
                        SELECT uuid_publico, denominacion, condicion_venta_codigo, estado_regulatorio
                          FROM sch_farmacia.producto_regulado
                        """ + PRODUCTO_REGULADO_FILTER + " ORDER BY denominacion LIMIT :limit OFFSET :offset")
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("condicionVentaCodigo", condicionVentaCodigo == null ? "" : condicionVentaCodigo)
                .param("estadoRegulatorio", estadoRegulatorio == null ? "" : estadoRegulatorio)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> new ProductoReguladoResumen(
                        rs.getObject("uuid_publico", UUID.class), rs.getString("denominacion"),
                        rs.getString("condicion_venta_codigo"), rs.getString("estado_regulatorio")))
                .list();
    }

    public long countProductosRegulados(String texto, String condicionVentaCodigo, String estadoRegulatorio) {
        var textFilter = normalizeSearch(texto);
        return jdbcClient.sql("SELECT COUNT(*) FROM sch_farmacia.producto_regulado" + PRODUCTO_REGULADO_FILTER)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("condicionVentaCodigo", condicionVentaCodigo == null ? "" : condicionVentaCodigo)
                .param("estadoRegulatorio", estadoRegulatorio == null ? "" : estadoRegulatorio)
                .query(Long.class).single();
    }

    private static final String SKU_FROM = """
            FROM sch_farmacia.sku_comercial s
            JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
            """;

    private static final String SKU_FILTER = """
             WHERE t.uuid_publico = :tenantId
               AND (:texto = '' OR LOWER(s.descripcion_comercial) LIKE :pattern
                    OR LOWER(s.codigo_interno) LIKE :pattern)
               AND (:categoriaId IS NULL OR s.categoria_id = (
                       SELECT id FROM sch_farmacia.categoria_producto WHERE uuid_publico = :categoriaId))
               AND (:marcaId IS NULL OR s.marca_id = (
                       SELECT id FROM sch_farmacia.marca WHERE uuid_publico = :marcaId))
               AND (:tipoSku = '' OR s.tipo_sku = :tipoSku)
               AND (:estado = '' OR s.estado_comercial = :estado)
            """;

    public List<SkuResumen> findSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
            int offset, int limit) {
        var textFilter = normalizeSearch(texto);
        return jdbcClient.sql("SELECT s.uuid_publico, s.codigo_interno, s.descripcion_comercial, s.tipo_sku, "
                        + "s.estado_comercial " + SKU_FROM + SKU_FILTER
                        + " ORDER BY s.descripcion_comercial LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("categoriaId", categoriaId)
                .param("marcaId", marcaId)
                .param("tipoSku", tipoSku == null ? "" : tipoSku)
                .param("estado", estado == null ? "" : estado)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> new SkuResumen(
                        rs.getObject("uuid_publico", UUID.class), rs.getString("codigo_interno"),
                        rs.getString("descripcion_comercial"), rs.getString("tipo_sku"),
                        rs.getString("estado_comercial")))
                .list();
    }

    public long countSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado) {
        var textFilter = normalizeSearch(texto);
        return jdbcClient.sql("SELECT COUNT(*) " + SKU_FROM + SKU_FILTER)
                .param("tenantId", tenantId)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("categoriaId", categoriaId)
                .param("marcaId", marcaId)
                .param("tipoSku", tipoSku == null ? "" : tipoSku)
                .param("estado", estado == null ? "" : estado)
                .query(Long.class).single();
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }
}
