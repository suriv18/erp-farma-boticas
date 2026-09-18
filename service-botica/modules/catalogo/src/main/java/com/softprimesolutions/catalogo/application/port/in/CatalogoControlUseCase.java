package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.UUID;

public interface CatalogoControlUseCase {

    Result<Unit, ApplicationError> changeCondicionVentaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeFormaFarmaceuticaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeViaAdministracionStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeUnidadMedidaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeClasificacionControladaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changePrincipioActivoStatus(UUID principioActivoId, String status);

    Result<Unit, ApplicationError> changeMarcaStatus(UUID tenantId, UUID marcaId, String status);

    Result<Unit, ApplicationError> changeCategoriaProductoStatus(UUID tenantId, UUID categoriaId, String status);

    Result<Unit, ApplicationError> changeProductoReguladoStatus(UUID productoReguladoId, String status);

    Result<Unit, ApplicationError> changeSkuStatus(UUID tenantId, UUID skuId, String status);
}
