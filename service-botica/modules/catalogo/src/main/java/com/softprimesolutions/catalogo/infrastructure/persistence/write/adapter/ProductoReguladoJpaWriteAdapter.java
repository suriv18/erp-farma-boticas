package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.EstadoRegulatorio;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivoAsociado;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoPrincipioActivoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.ProductoReguladoWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ProductoPrincipioActivoJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ProductoReguladoJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProductoReguladoJpaWriteAdapter implements ProductoReguladoPort {

    private final ProductoReguladoJpaRepository productoReguladoRepository;
    private final ProductoPrincipioActivoJpaRepository principioActivoAsociadoRepository;
    private final JdbcClient jdbcClient;

    public ProductoReguladoJpaWriteAdapter(
            ProductoReguladoJpaRepository productoReguladoRepository,
            ProductoPrincipioActivoJpaRepository principioActivoAsociadoRepository,
            JdbcClient jdbcClient) {
        this.productoReguladoRepository = productoReguladoRepository;
        this.principioActivoAsociadoRepository = principioActivoAsociadoRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveOutcome save(ProductoRegulado producto) {
        if (producto.formaFarmaceuticaCodigo() != null
                && !existsInSupportTable("forma_farmaceutica", producto.formaFarmaceuticaCodigo())) {
            return SaveOutcome.FORMA_FARMACEUTICA_NOT_FOUND;
        }
        if (producto.viaAdministracionCodigo() != null
                && !existsInSupportTable("via_administracion", producto.viaAdministracionCodigo())) {
            return SaveOutcome.VIA_ADMINISTRACION_NOT_FOUND;
        }
        if (producto.unidadMedidaCodigo() != null
                && !existsInSupportTable("unidad_medida", producto.unidadMedidaCodigo())) {
            return SaveOutcome.UNIDAD_MEDIDA_NOT_FOUND;
        }
        if (producto.condicionVentaCodigo() != null
                && !existsInSupportTable("condicion_venta", producto.condicionVentaCodigo())) {
            return SaveOutcome.CONDICION_VENTA_NOT_FOUND;
        }
        if (producto.clasificacionControladaCodigo() != null
                && !existsInSupportTable("clasificacion_controlada", producto.clasificacionControladaCodigo())) {
            return SaveOutcome.CLASIFICACION_CONTROLADA_NOT_FOUND;
        }
        for (var asociado : producto.principiosActivos()) {
            if (findPrincipioActivoInternalId(asociado.principioActivoId().value()).isEmpty()) {
                return SaveOutcome.PRINCIPIO_ACTIVO_NOT_FOUND;
            }
        }

        var existing = productoReguladoRepository.findByUuidPublico(producto.id().value());
        Long productoInternalId;
        if (existing.isEmpty()) {
            var saved = productoReguladoRepository.saveAndFlush(ProductoReguladoWriteMapper.toEntity(producto));
            productoInternalId = saved.getId();
        } else {
            jdbcClient.sql("""
                            UPDATE sch_catalogo.producto_regulado
                               SET tipo_producto = :tipoProducto, rubro_codigo = :rubroCodigo,
                                   tipo_registro = :tipoRegistro, numero_registro = :numeroRegistro,
                                   denominacion = :denominacion, concentracion_texto = :concentracionTexto,
                                   presentacion_regulatoria = :presentacionRegulatoria,
                                   forma_farmaceutica_codigo = :formaFarmaceuticaCodigo,
                                   via_administracion_codigo = :viaAdministracionCodigo,
                                   unidad_medida_codigo = :unidadMedidaCodigo,
                                   condicion_venta_codigo = :condicionVentaCodigo,
                                   clasificacion_atc = :clasificacionAtc,
                                   clasificacion_controlada_codigo = :clasificacionControladaCodigo,
                                   tipo_liberacion = :tipoLiberacion, origen_fabricacion = :origenFabricacion,
                                   pais_origen = :paisOrigen, subpartida_nacional = :subpartidaNacional,
                                   titular_registro = :titularRegistro, fabricante = :fabricante,
                                   importador = :importador, establecimiento_expendio = :establecimientoExpendio,
                                   vigente_desde = :vigenteDesde, vigente_hasta = :vigenteHasta,
                                   fuente = :fuente, version_fuente = :versionFuente, updated_at = :updatedAt
                             WHERE uuid_publico = :productoReguladoId
                            """)
                    .param("tipoProducto", producto.tipoProducto())
                    .param("rubroCodigo", producto.rubroCodigo())
                    .param("tipoRegistro", producto.tipoRegistro())
                    .param("numeroRegistro", producto.numeroRegistro())
                    .param("denominacion", producto.denominacion())
                    .param("concentracionTexto", producto.concentracionTexto())
                    .param("presentacionRegulatoria", producto.presentacionRegulatoria())
                    .param("formaFarmaceuticaCodigo", producto.formaFarmaceuticaCodigo())
                    .param("viaAdministracionCodigo", producto.viaAdministracionCodigo())
                    .param("unidadMedidaCodigo", producto.unidadMedidaCodigo())
                    .param("condicionVentaCodigo", producto.condicionVentaCodigo())
                    .param("clasificacionAtc", producto.clasificacionAtc())
                    .param("clasificacionControladaCodigo", producto.clasificacionControladaCodigo())
                    .param("tipoLiberacion", producto.tipoLiberacion())
                    .param("origenFabricacion", producto.origenFabricacion())
                    .param("paisOrigen", producto.paisOrigen())
                    .param("subpartidaNacional", producto.subpartidaNacional())
                    .param("titularRegistro", producto.titularRegistro())
                    .param("fabricante", producto.fabricante())
                    .param("importador", producto.importador())
                    .param("establecimientoExpendio", producto.establecimientoExpendio())
                    .param("vigenteDesde", producto.vigenteDesde())
                    .param("vigenteHasta", producto.vigenteHasta())
                    .param("fuente", producto.fuente())
                    .param("versionFuente", producto.versionFuente())
                    .param("updatedAt", producto.updatedAt())
                    .param("productoReguladoId", producto.id().value())
                    .update();
            productoInternalId = existing.get().getId();
        }

        principioActivoAsociadoRepository.deleteByProductoReguladoId(productoInternalId);
        for (var asociado : producto.principiosActivos()) {
            var principioActivoInternalId = findPrincipioActivoInternalId(asociado.principioActivoId().value())
                    .orElseThrow();
            principioActivoAsociadoRepository.save(new ProductoPrincipioActivoJpaEntity(
                    productoInternalId, principioActivoInternalId, asociado.concentracionTexto(),
                    asociado.cantidad(), asociado.unidadMedidaCodigo(), asociado.esPrincipal(),
                    asociado.orden()));
        }

        return existing.isEmpty() ? SaveOutcome.CREATED : SaveOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoRegulado> findById(UUID productoReguladoId) {
        var entity = productoReguladoRepository.findByUuidPublico(productoReguladoId);
        if (entity.isEmpty()) return Optional.empty();

        var asociaciones = principioActivoAsociadoRepository.findByProductoReguladoId(entity.get().getId())
                .stream()
                .map(association -> new PrincipioActivoAsociado(
                        new PrincipioActivoId(findPrincipioActivoUuid(association.getPrincipioActivoId())),
                        association.getConcentracionTexto(), association.getCantidad(),
                        association.getUnidadMedidaCodigo(), association.isEsPrincipal(),
                        association.getOrden()))
                .toList();

        return Optional.of(ProductoRegulado.restore(
                new ProductoReguladoId(entity.get().getUuidPublico()), entity.get().getTipoProducto(),
                entity.get().getRubroCodigo(), entity.get().getTipoRegistro(), entity.get().getNumeroRegistro(),
                entity.get().getDenominacion(), entity.get().getConcentracionTexto(),
                entity.get().getPresentacionRegulatoria(), entity.get().getFormaFarmaceuticaCodigo(),
                entity.get().getViaAdministracionCodigo(), entity.get().getUnidadMedidaCodigo(),
                entity.get().getCondicionVentaCodigo(), entity.get().getClasificacionAtc(),
                entity.get().getClasificacionControladaCodigo(), entity.get().getTipoLiberacion(),
                entity.get().getOrigenFabricacion(), entity.get().getPaisOrigen(),
                entity.get().getSubpartidaNacional(), entity.get().getTitularRegistro(),
                entity.get().getFabricante(), entity.get().getImportador(),
                entity.get().getEstablecimientoExpendio(), entity.get().getVigenteDesde(),
                entity.get().getVigenteHasta(), entity.get().getFuente(), entity.get().getVersionFuente(),
                asociaciones, EstadoRegulatorio.valueOf(entity.get().getEstadoRegulatorio()),
                entity.get().getCreatedAt(), entity.get().getUpdatedAt()));
    }

    @Override
    @Transactional
    public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) {
        return jdbcClient.sql("""
                        UPDATE sch_catalogo.producto_regulado SET estado_regulatorio = :status, updated_at = :changedAt
                         WHERE uuid_publico = :productoReguladoId
                        """)
                .param("status", status).param("changedAt", changedAt).param("productoReguladoId", productoReguladoId)
                .update() == 1;
    }

    private boolean existsInSupportTable(String tableName, String codigo) {
        return jdbcClient.sql("SELECT COUNT(*) FROM sch_catalogo." + tableName + " WHERE codigo = :codigo")
                .param("codigo", codigo).query(Long.class).single() > 0;
    }

    private Optional<Long> findPrincipioActivoInternalId(UUID principioActivoUuid) {
        return jdbcClient.sql("SELECT id FROM sch_catalogo.principio_activo WHERE uuid_publico = :principioActivoUuid")
                .param("principioActivoUuid", principioActivoUuid).query(Long.class).optional();
    }

    private UUID findPrincipioActivoUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_catalogo.principio_activo WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }
}
