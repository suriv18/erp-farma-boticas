package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.UUID;

public interface CatalogoSoportePort {

    SaveOutcome save(CondicionVenta condicionVenta);

    SaveOutcome save(FormaFarmaceutica formaFarmaceutica);

    SaveOutcome save(ViaAdministracion viaAdministracion);

    SaveOutcome save(UnidadMedida unidadMedida);

    SaveOutcome save(ClasificacionControlada clasificacionControlada);

    SavePrincipioActivoOutcome save(PrincipioActivo principioActivo);

    boolean condicionVentaExists(String codigo);

    boolean formaFarmaceuticaExists(String codigo);

    boolean viaAdministracionExists(String codigo);

    boolean unidadMedidaExists(String codigo);

    boolean clasificacionControladaExists(String codigo);

    boolean principioActivoExists(UUID principioActivoId);

    boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt);

    boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt);

    boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt);

    boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt);

    boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt);

    boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt);

    enum SaveOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, NOT_FOUND }

    enum SavePrincipioActivoOutcome { CREATED, UPDATED, NOT_FOUND, DUPLICATE_DENOMINACION }
}
