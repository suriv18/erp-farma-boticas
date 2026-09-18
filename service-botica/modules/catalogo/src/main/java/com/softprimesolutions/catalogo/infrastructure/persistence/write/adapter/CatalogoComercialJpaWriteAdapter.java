package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.CodigoBarraSku;
import com.softprimesolutions.catalogo.domain.model.EstadoComercialSku;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.TipoSku;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuCodigoBarraJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoComercialWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.CategoriaProductoJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.MarcaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.SkuCodigoBarraJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.SkuComercialJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoComercialJpaWriteAdapter implements CatalogoComercialPort {

    private final MarcaJpaRepository marcaRepository;
    private final CategoriaProductoJpaRepository categoriaRepository;
    private final SkuComercialJpaRepository skuRepository;
    private final SkuCodigoBarraJpaRepository codigoBarraRepository;
    private final JdbcClient jdbcClient;
    private final EntityManager entityManager;

    public CatalogoComercialJpaWriteAdapter(
            MarcaJpaRepository marcaRepository,
            CategoriaProductoJpaRepository categoriaRepository,
            SkuComercialJpaRepository skuRepository,
            SkuCodigoBarraJpaRepository codigoBarraRepository,
            JdbcClient jdbcClient,
            EntityManager entityManager) {
        this.marcaRepository = marcaRepository;
        this.categoriaRepository = categoriaRepository;
        this.skuRepository = skuRepository;
        this.codigoBarraRepository = codigoBarraRepository;
        this.jdbcClient = jdbcClient;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public SaveMarcaOutcome save(Marca marca) {
        var tenantId = findTenantId(marca.tenantId() == null ? null : marca.tenantId().value());
        if (tenantId.isEmpty()) return SaveMarcaOutcome.TENANT_NOT_FOUND;

        var existing = marcaRepository.findByUuidPublico(marca.id().value());
        if (existing.isEmpty()) {
            if (marcaRepository.existsByTenantIdAndCodigo(tenantId.get(), marca.codigo())) {
                return SaveMarcaOutcome.DUPLICATE_CODIGO;
            }
            try {
                marcaRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(marca, tenantId.get()));
                return SaveMarcaOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveMarcaOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(marca.codigo())
                && marcaRepository.existsByTenantIdAndCodigo(tenantId.get(), marca.codigo())) {
            return SaveMarcaOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.marca
                           SET codigo = :codigo, nombre = :nombre, descripcion = :descripcion,
                               updated_by = :updatedBy, updated_at = :updatedAt
                         WHERE uuid_publico = :marcaId
                        """)
                .param("codigo", marca.codigo())
                .param("nombre", marca.nombre())
                .param("descripcion", marca.descripcion())
                .param("updatedBy", "SYSTEM")
                .param("updatedAt", toOffsetDateTime(Instant.now()))
                .param("marcaId", marca.id().value())
                .update();
        return SaveMarcaOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveCategoriaOutcome save(CategoriaProducto categoria) {
        var tenantId = findTenantId(categoria.tenantId() == null ? null : categoria.tenantId().value());
        if (tenantId.isEmpty()) return SaveCategoriaOutcome.TENANT_NOT_FOUND;

        Long categoriaPadreInternalId = null;
        if (categoria.categoriaPadreId() != null) {
            var padreInternalId = findCategoriaInternalId(categoria.categoriaPadreId().value());
            if (padreInternalId.isEmpty()) return SaveCategoriaOutcome.CATEGORIA_PADRE_NOT_FOUND;
            categoriaPadreInternalId = padreInternalId.get();
        }

        var existing = categoriaRepository.findByUuidPublico(categoria.id().value());
        if (existing.isEmpty()) {
            if (categoriaRepository.existsByTenantIdAndCodigo(tenantId.get(), categoria.codigo())) {
                return SaveCategoriaOutcome.DUPLICATE_CODIGO;
            }
            try {
                categoriaRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(
                        categoria, tenantId.get(), categoriaPadreInternalId));
                return SaveCategoriaOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveCategoriaOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(categoria.codigo())
                && categoriaRepository.existsByTenantIdAndCodigo(tenantId.get(), categoria.codigo())) {
            return SaveCategoriaOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.categoria_producto
                           SET categoria_padre_id = :categoriaPadreId, codigo = :codigo, nombre = :nombre,
                               descripcion = :descripcion, nivel = :nivel, orden = :orden,
                               updated_by = :updatedBy, updated_at = :updatedAt
                         WHERE uuid_publico = :categoriaId
                        """)
                .param("categoriaPadreId", categoriaPadreInternalId)
                .param("codigo", categoria.codigo())
                .param("nombre", categoria.nombre())
                .param("descripcion", categoria.descripcion())
                .param("nivel", categoria.nivel())
                .param("orden", categoria.orden())
                .param("updatedBy", "SYSTEM")
                .param("updatedAt", toOffsetDateTime(Instant.now()))
                .param("categoriaId", categoria.id().value())
                .update();
        return SaveCategoriaOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveSkuOutcome save(SKUComercial sku) {
        var tenantId = findTenantId(sku.tenantId() == null ? null : sku.tenantId().value());
        if (tenantId.isEmpty()) return SaveSkuOutcome.TENANT_NOT_FOUND;

        Long productoReguladoInternalId = null;
        if (sku.productoReguladoId() != null) {
            var internalId = findProductoReguladoInternalId(sku.productoReguladoId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.PRODUCTO_REGULADO_NOT_FOUND;
            productoReguladoInternalId = internalId.get();
        }

        Long categoriaInternalId = null;
        if (sku.categoriaId() != null) {
            var internalId = findCategoriaInternalId(sku.categoriaId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.CATEGORIA_NOT_FOUND;
            categoriaInternalId = internalId.get();
        }

        Long marcaInternalId = null;
        if (sku.marcaId() != null) {
            var internalId = findMarcaInternalId(sku.marcaId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.MARCA_NOT_FOUND;
            marcaInternalId = internalId.get();
        }

        var existing = skuRepository.findByUuidPublico(sku.id().value());
        Long skuInternalId;
        if (existing.isEmpty()) {
            if (skuRepository.existsByTenantIdAndCodigoInterno(tenantId.get(), sku.codigoInterno())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
            try {
                var saved = skuRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(
                        sku, tenantId.get(), productoReguladoInternalId, categoriaInternalId, marcaInternalId));
                skuInternalId = saved.getId();
            } catch (DataIntegrityViolationException exception) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
        } else {
            var entity = existing.get();
            if (!entity.getCodigoInterno().equals(sku.codigoInterno())
                    && skuRepository.existsByTenantIdAndCodigoInterno(tenantId.get(), sku.codigoInterno())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
            jdbcClient.sql("""
                            UPDATE sch_catalogo.sku_comercial
                               SET producto_regulado_id = :productoReguladoId, categoria_id = :categoriaId,
                                   marca_id = :marcaId, tipo_sku = :tipoSku, codigo_interno = :codigoInterno,
                                   descripcion_comercial = :descripcionComercial, nombre_corto = :nombreCorto,
                                   presentacion_comercial = :presentacionComercial,
                                   unidad_venta_codigo = :unidadVentaCodigo, contenido = :contenido,
                                   unidad_contenido_codigo = :unidadContenidoCodigo, peso_gramos = :pesoGramos,
                                   alto_cm = :altoCm, ancho_cm = :anchoCm, largo_cm = :largoCm,
                                   permite_venta_fraccion = :permiteVentaFraccion,
                                   factor_fraccion = :factorFraccion,
                                   unidad_fraccion_codigo = :unidadFraccionCodigo, requiere_lote = :requiereLote,
                                   requiere_vencimiento = :requiereVencimiento, afecto_igv = :afectoIgv,
                                   stock_minimo_default = :stockMinimoDefault,
                                   stock_maximo_default = :stockMaximoDefault, imagen_uri = :imagenUri,
                                   updated_by = :updatedBy, updated_at = :updatedAt
                             WHERE uuid_publico = :skuId
                            """)
                    .param("productoReguladoId", productoReguladoInternalId)
                    .param("categoriaId", categoriaInternalId)
                    .param("marcaId", marcaInternalId)
                    .param("tipoSku", sku.tipoSku().name())
                    .param("codigoInterno", sku.codigoInterno())
                    .param("descripcionComercial", sku.descripcionComercial())
                    .param("nombreCorto", sku.nombreCorto())
                    .param("presentacionComercial", sku.presentacionComercial())
                    .param("unidadVentaCodigo", sku.unidadVentaCodigo())
                    .param("contenido", sku.contenido())
                    .param("unidadContenidoCodigo", sku.unidadContenidoCodigo())
                    .param("pesoGramos", sku.pesoGramos())
                    .param("altoCm", sku.altoCm())
                    .param("anchoCm", sku.anchoCm())
                    .param("largoCm", sku.largoCm())
                    .param("permiteVentaFraccion", sku.permiteVentaFraccion())
                    .param("factorFraccion", sku.factorFraccion())
                    .param("unidadFraccionCodigo", sku.unidadFraccionCodigo())
                    .param("requiereLote", sku.requiereLote())
                    .param("requiereVencimiento", sku.requiereVencimiento())
                    .param("afectoIgv", sku.afectoIgv())
                    .param("stockMinimoDefault", sku.stockMinimoDefault())
                    .param("stockMaximoDefault", sku.stockMaximoDefault())
                    .param("imagenUri", sku.imagenUri())
                    .param("updatedBy", sku.updatedBy())
                    .param("updatedAt", toOffsetDateTime(sku.updatedAt()))
                    .param("skuId", sku.id().value())
                    .update();
            skuInternalId = entity.getId();
        }

        codigoBarraRepository.deleteBySkuId(skuInternalId);
        for (var codigo : sku.codigosBarra()) {
            if (codigoBarraRepository.existsByTenantIdAndCodigoBarra(tenantId.get(), codigo.codigoBarra())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_BARRA;
            }
            codigoBarraRepository.save(new SkuCodigoBarraJpaEntity(
                    tenantId.get(), skuInternalId, codigo.tipoCodigo(), codigo.codigoBarra(),
                    codigo.esPrincipal(), codigo.vigenteDesde(), codigo.vigenteHasta(), codigo.estado().name(),
                    "SYSTEM", Instant.now()));
        }
        return existing.isEmpty() ? SaveSkuOutcome.CREATED : SaveSkuOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = skuRepository.findByUuidPublico(skuId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }

        var codigos = codigoBarraRepository.findBySkuId(entity.get().getId()).stream()
                .map(codigoEntity -> new CodigoBarraSku(
                        codigoEntity.getCodigoBarra(), codigoEntity.getTipoCodigo(), codigoEntity.isEsPrincipal(),
                        codigoEntity.getVigenteDesde(), codigoEntity.getVigenteHasta(),
                        EstadoCatalogoSoporte.valueOf(codigoEntity.getEstado())))
                .toList();

        var productoReguladoId = entity.get().getProductoReguladoId() == null ? null
                : findProductoReguladoUuid(entity.get().getProductoReguladoId());
        var categoriaId = entity.get().getCategoriaId() == null ? null
                : findCategoriaUuid(entity.get().getCategoriaId());
        var marcaId = entity.get().getMarcaId() == null ? null : findMarcaUuid(entity.get().getMarcaId());

        var sku = SKUComercial.restore(
                new SkuId(entity.get().getUuidPublico()), new TenantId(tenantId),
                productoReguladoId == null ? null : new ProductoReguladoId(productoReguladoId),
                categoriaId == null ? null : new CategoriaProductoId(categoriaId),
                marcaId == null ? null : new MarcaId(marcaId),
                TipoSku.valueOf(entity.get().getTipoSku()), entity.get().getCodigoInterno(),
                entity.get().getDescripcionComercial(), entity.get().getNombreCorto(),
                entity.get().getPresentacionComercial(), entity.get().getUnidadVentaCodigo(),
                entity.get().getContenido(), entity.get().getUnidadContenidoCodigo(),
                entity.get().getPesoGramos(), entity.get().getAltoCm(), entity.get().getAnchoCm(),
                entity.get().getLargoCm(), entity.get().isPermiteVentaFraccion(), entity.get().getFactorFraccion(),
                entity.get().getUnidadFraccionCodigo(), entity.get().isRequiereLote(),
                entity.get().isRequiereVencimiento(), entity.get().isAfectoIgv(),
                entity.get().getStockMinimoDefault(), entity.get().getStockMaximoDefault(),
                entity.get().getImagenUri(), codigos,
                EstadoComercialSku.valueOf(entity.get().getEstadoComercial()),
                entity.get().getCreatedBy(), entity.get().getCreatedAt(), entity.get().getUpdatedBy(),
                entity.get().getUpdatedAt());
        return Optional.of(sku);
    }

    @Override
    public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_catalogo.categoria_producto
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("tenantId", tenantInternalId.get()).param("categoriaId", categoriaId)
                .query(Long.class).single() > 0;
    }

    @Override
    public boolean marcaExists(UUID tenantId, UUID marcaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_catalogo.marca WHERE tenant_id = :tenantId AND uuid_publico = :marcaId
                        """)
                .param("tenantId", tenantInternalId.get()).param("marcaId", marcaId)
                .query(Long.class).single() > 0;
    }

    @Override
    @Transactional
    public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        var updated = jdbcClient.sql("""
                        UPDATE sch_catalogo.marca SET estado = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :marcaId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("marcaId", marcaId)
                .update() == 1;
        if (updated) entityManager.clear();
        return updated;
    }

    @Override
    @Transactional
    public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        var updated = jdbcClient.sql("""
                        UPDATE sch_catalogo.categoria_producto SET estado = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("categoriaId", categoriaId)
                .update() == 1;
        if (updated) entityManager.clear();
        return updated;
    }

    @Override
    @Transactional
    public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        var updated = jdbcClient.sql("""
                        UPDATE sch_catalogo.sku_comercial SET estado_comercial = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :skuId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("skuId", skuId)
                .update() == 1;
        if (updated) entityManager.clear();
        return updated;
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid).query(Long.class).optional();
    }

    private Optional<Long> findCategoriaInternalId(UUID categoriaUuid) {
        if (categoriaUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_catalogo.categoria_producto WHERE uuid_publico = :categoriaUuid")
                .param("categoriaUuid", categoriaUuid).query(Long.class).optional();
    }

    private Optional<Long> findMarcaInternalId(UUID marcaUuid) {
        if (marcaUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_catalogo.marca WHERE uuid_publico = :marcaUuid")
                .param("marcaUuid", marcaUuid).query(Long.class).optional();
    }

    private Optional<Long> findProductoReguladoInternalId(UUID productoReguladoUuid) {
        if (productoReguladoUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_catalogo.producto_regulado WHERE uuid_publico = :productoReguladoUuid")
                .param("productoReguladoUuid", productoReguladoUuid).query(Long.class).optional();
    }

    private UUID findProductoReguladoUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_catalogo.producto_regulado WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }

    private UUID findCategoriaUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_catalogo.categoria_producto WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }

    private UUID findMarcaUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_catalogo.marca WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
