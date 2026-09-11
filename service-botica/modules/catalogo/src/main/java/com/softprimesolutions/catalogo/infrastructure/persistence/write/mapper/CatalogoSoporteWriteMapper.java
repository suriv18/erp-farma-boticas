package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ClasificacionControladaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CondicionVentaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.FormaFarmaceuticaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.PrincipioActivoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.UnidadMedidaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ViaAdministracionJpaEntity;

public final class CatalogoSoporteWriteMapper {

    private CatalogoSoporteWriteMapper() {
    }

    public static CondicionVentaJpaEntity toEntity(CondicionVenta condicionVenta) {
        return new CondicionVentaJpaEntity(
                condicionVenta.codigo(), condicionVenta.denominacion(), condicionVenta.requiereReceta(),
                condicionVenta.requiereRetencion(), condicionVenta.fuente(), condicionVenta.versionFuente(),
                condicionVenta.vigenteDesde(), condicionVenta.vigenteHasta(), condicionVenta.estado().name());
    }

    public static FormaFarmaceuticaJpaEntity toEntity(FormaFarmaceutica formaFarmaceutica) {
        return new FormaFarmaceuticaJpaEntity(
                formaFarmaceutica.codigo(), formaFarmaceutica.denominacion(), formaFarmaceutica.fuente(),
                formaFarmaceutica.estado().name());
    }

    public static ViaAdministracionJpaEntity toEntity(ViaAdministracion viaAdministracion) {
        return new ViaAdministracionJpaEntity(
                viaAdministracion.codigo(), viaAdministracion.denominacion(), viaAdministracion.fuente(),
                viaAdministracion.estado().name());
    }

    public static UnidadMedidaJpaEntity toEntity(UnidadMedida unidadMedida) {
        return new UnidadMedidaJpaEntity(
                unidadMedida.codigo(), unidadMedida.denominacion(), unidadMedida.simbolo(),
                unidadMedida.permiteDecimal(), unidadMedida.fuente(), unidadMedida.estado().name());
    }

    public static ClasificacionControladaJpaEntity toEntity(ClasificacionControlada clasificacionControlada) {
        return new ClasificacionControladaJpaEntity(
                clasificacionControlada.codigo(), clasificacionControlada.denominacion(),
                clasificacionControlada.normaFuente(), clasificacionControlada.requiereRecetaEspecial(),
                clasificacionControlada.retieneReceta(), clasificacionControlada.vigenciaRecetaDias(),
                clasificacionControlada.estado().name());
    }

    public static PrincipioActivoJpaEntity toEntity(PrincipioActivo principioActivo) {
        return new PrincipioActivoJpaEntity(
                principioActivo.id().value(), principioActivo.codigoFuente(), principioActivo.denominacion(),
                principioActivo.nombreNormalizado(), principioActivo.fuente(), principioActivo.estado().name());
    }
}
