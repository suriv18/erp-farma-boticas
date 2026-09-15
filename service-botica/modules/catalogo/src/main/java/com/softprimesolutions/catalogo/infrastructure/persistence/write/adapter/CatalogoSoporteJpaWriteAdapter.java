package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoSoporteWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ClasificacionControladaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.CondicionVentaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.FormaFarmaceuticaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.PrincipioActivoJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.UnidadMedidaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ViaAdministracionJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoSoporteJpaWriteAdapter implements CatalogoSoportePort {

    private final CondicionVentaJpaRepository condicionVentaRepository;
    private final FormaFarmaceuticaJpaRepository formaFarmaceuticaRepository;
    private final ViaAdministracionJpaRepository viaAdministracionRepository;
    private final UnidadMedidaJpaRepository unidadMedidaRepository;
    private final ClasificacionControladaJpaRepository clasificacionControladaRepository;
    private final PrincipioActivoJpaRepository principioActivoRepository;
    private final JdbcClient jdbcClient;

    public CatalogoSoporteJpaWriteAdapter(
            CondicionVentaJpaRepository condicionVentaRepository,
            FormaFarmaceuticaJpaRepository formaFarmaceuticaRepository,
            ViaAdministracionJpaRepository viaAdministracionRepository,
            UnidadMedidaJpaRepository unidadMedidaRepository,
            ClasificacionControladaJpaRepository clasificacionControladaRepository,
            PrincipioActivoJpaRepository principioActivoRepository,
            JdbcClient jdbcClient) {
        this.condicionVentaRepository = condicionVentaRepository;
        this.formaFarmaceuticaRepository = formaFarmaceuticaRepository;
        this.viaAdministracionRepository = viaAdministracionRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.clasificacionControladaRepository = clasificacionControladaRepository;
        this.principioActivoRepository = principioActivoRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveOutcome save(CondicionVenta condicionVenta) {
        var existing = condicionVentaRepository.findById(condicionVenta.codigo());
        if (existing.isEmpty()) {
            try {
                condicionVentaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(condicionVenta));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.condicion_venta
                           SET denominacion = :denominacion, requiere_receta = :requiereReceta,
                               requiere_retencion = :requiereRetencion, fuente = :fuente,
                               version_fuente = :versionFuente, vigente_desde = :vigenteDesde,
                               vigente_hasta = :vigenteHasta
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", condicionVenta.denominacion())
                .param("requiereReceta", condicionVenta.requiereReceta())
                .param("requiereRetencion", condicionVenta.requiereRetencion())
                .param("fuente", condicionVenta.fuente())
                .param("versionFuente", condicionVenta.versionFuente())
                .param("vigenteDesde", condicionVenta.vigenteDesde())
                .param("vigenteHasta", condicionVenta.vigenteHasta())
                .param("codigo", condicionVenta.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
        var existing = formaFarmaceuticaRepository.findById(formaFarmaceutica.codigo());
        if (existing.isEmpty()) {
            try {
                formaFarmaceuticaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(formaFarmaceutica));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.forma_farmaceutica SET denominacion = :denominacion, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", formaFarmaceutica.denominacion())
                .param("fuente", formaFarmaceutica.fuente())
                .param("codigo", formaFarmaceutica.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(ViaAdministracion viaAdministracion) {
        var existing = viaAdministracionRepository.findById(viaAdministracion.codigo());
        if (existing.isEmpty()) {
            try {
                viaAdministracionRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(viaAdministracion));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.via_administracion SET denominacion = :denominacion, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", viaAdministracion.denominacion())
                .param("fuente", viaAdministracion.fuente())
                .param("codigo", viaAdministracion.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(UnidadMedida unidadMedida) {
        var existing = unidadMedidaRepository.findById(unidadMedida.codigo());
        if (existing.isEmpty()) {
            try {
                unidadMedidaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(unidadMedida));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.unidad_medida
                           SET denominacion = :denominacion, simbolo = :simbolo,
                               permite_decimal = :permiteDecimal, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", unidadMedida.denominacion())
                .param("simbolo", unidadMedida.simbolo())
                .param("permiteDecimal", unidadMedida.permiteDecimal())
                .param("fuente", unidadMedida.fuente())
                .param("codigo", unidadMedida.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
        var existing = clasificacionControladaRepository.findById(clasificacionControlada.codigo());
        if (existing.isEmpty()) {
            try {
                clasificacionControladaRepository.saveAndFlush(
                        CatalogoSoporteWriteMapper.toEntity(clasificacionControlada));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.clasificacion_controlada
                           SET denominacion = :denominacion, norma_fuente = :normaFuente,
                               requiere_receta_especial = :requiereRecetaEspecial,
                               retiene_receta = :retieneReceta, vigencia_receta_dias = :vigenciaRecetaDias
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", clasificacionControlada.denominacion())
                .param("normaFuente", clasificacionControlada.normaFuente())
                .param("requiereRecetaEspecial", clasificacionControlada.requiereRecetaEspecial())
                .param("retieneReceta", clasificacionControlada.retieneReceta())
                .param("vigenciaRecetaDias", clasificacionControlada.vigenciaRecetaDias())
                .param("codigo", clasificacionControlada.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
        var existing = principioActivoRepository.findByUuidPublico(principioActivo.id().value());
        if (existing.isEmpty()) {
            principioActivoRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(principioActivo));
            return SavePrincipioActivoOutcome.CREATED;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.principio_activo
                           SET codigo_fuente = :codigoFuente, denominacion = :denominacion,
                               nombre_normalizado = :nombreNormalizado, fuente = :fuente
                         WHERE uuid_publico = :principioActivoId
                        """)
                .param("codigoFuente", principioActivo.codigoFuente())
                .param("denominacion", principioActivo.denominacion())
                .param("nombreNormalizado", principioActivo.nombreNormalizado())
                .param("fuente", principioActivo.fuente())
                .param("principioActivoId", principioActivo.id().value())
                .update();
        return SavePrincipioActivoOutcome.UPDATED;
    }

    @Override
    public boolean condicionVentaExists(String codigo) {
        return condicionVentaRepository.existsById(codigo);
    }

    @Override
    public boolean formaFarmaceuticaExists(String codigo) {
        return formaFarmaceuticaRepository.existsById(codigo);
    }

    @Override
    public boolean viaAdministracionExists(String codigo) {
        return viaAdministracionRepository.existsById(codigo);
    }

    @Override
    public boolean unidadMedidaExists(String codigo) {
        return unidadMedidaRepository.existsById(codigo);
    }

    @Override
    public boolean clasificacionControladaExists(String codigo) {
        return clasificacionControladaRepository.existsById(codigo);
    }

    @Override
    public boolean principioActivoExists(UUID principioActivoId) {
        return principioActivoRepository.findByUuidPublico(principioActivoId).isPresent();
    }

    @Override
    @Transactional
    public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.condicion_venta SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.forma_farmaceutica SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.via_administracion SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.unidad_medida SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.clasificacion_controlada SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_catalogo.principio_activo SET estado = :status WHERE uuid_publico = :principioActivoId")
                .param("status", status).param("principioActivoId", principioActivoId).update() == 1;
    }
}
