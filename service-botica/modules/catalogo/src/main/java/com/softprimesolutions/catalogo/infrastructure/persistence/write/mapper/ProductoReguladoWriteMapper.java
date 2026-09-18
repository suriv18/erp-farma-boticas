package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoReguladoJpaEntity;

public final class ProductoReguladoWriteMapper {

    private ProductoReguladoWriteMapper() {
    }

    public static ProductoReguladoJpaEntity toEntity(ProductoRegulado producto) {
        return new ProductoReguladoJpaEntity(
                producto.id().value(), producto.tipoProducto(), producto.rubroCodigo(), producto.tipoRegistro(),
                producto.numeroRegistro(), producto.denominacion(), producto.concentracionTexto(),
                producto.presentacionRegulatoria(), producto.formaFarmaceuticaCodigo(),
                producto.viaAdministracionCodigo(), producto.unidadMedidaCodigo(), producto.condicionVentaCodigo(),
                producto.clasificacionAtc(), producto.clasificacionControladaCodigo(), producto.tipoLiberacion(),
                producto.origenFabricacion(), producto.paisOrigen(), producto.subpartidaNacional(),
                producto.titularRegistro(), producto.fabricante(), producto.importador(),
                producto.establecimientoExpendio(), producto.vigenteDesde(), producto.vigenteHasta(),
                producto.estado().name(), producto.fuente(), producto.versionFuente(), producto.createdAt(),
                producto.updatedAt());
    }
}
