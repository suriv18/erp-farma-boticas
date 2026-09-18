package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaProductoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.MarcaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuComercialJpaEntity;
import java.time.Instant;

public final class CatalogoComercialWriteMapper {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private CatalogoComercialWriteMapper() {
    }

    public static MarcaJpaEntity toEntity(Marca marca, Long tenantId) {
        return new MarcaJpaEntity(
                marca.id().value(), tenantId, marca.codigo(), marca.nombre(), marca.descripcion(),
                marca.estado().name(), SYSTEM_ACTOR, Instant.now());
    }

    public static CategoriaProductoJpaEntity toEntity(
            CategoriaProducto categoria, Long tenantId, Long categoriaPadreId) {
        return new CategoriaProductoJpaEntity(
                categoria.id().value(), tenantId, categoriaPadreId, categoria.codigo(), categoria.nombre(),
                categoria.descripcion(), categoria.nivel(), categoria.orden(), categoria.estado().name(),
                SYSTEM_ACTOR, Instant.now());
    }

    public static SkuComercialJpaEntity toEntity(
            SKUComercial sku, Long tenantId, Long productoReguladoId, Long categoriaId, Long marcaId) {
        return new SkuComercialJpaEntity(
                sku.id().value(), tenantId, productoReguladoId, categoriaId, marcaId, sku.tipoSku().name(),
                sku.codigoInterno(), sku.descripcionComercial(), sku.nombreCorto(), sku.presentacionComercial(),
                sku.unidadVentaCodigo(), sku.contenido(), sku.unidadContenidoCodigo(), sku.pesoGramos(),
                sku.altoCm(), sku.anchoCm(), sku.largoCm(), sku.permiteVentaFraccion(), sku.factorFraccion(),
                sku.unidadFraccionCodigo(), sku.requiereLote(), sku.requiereVencimiento(), sku.afectoIgv(),
                sku.stockMinimoDefault(), sku.stockMaximoDefault(), sku.imagenUri(), sku.estado().name(),
                sku.createdBy(), sku.createdAt(), sku.updatedBy(), sku.updatedAt());
    }
}
