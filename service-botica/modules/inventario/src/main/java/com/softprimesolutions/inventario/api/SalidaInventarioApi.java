package com.softprimesolutions.inventario.api;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface SalidaInventarioApi {

    String CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";
    String CODIGO_STOCK_INSUFICIENTE = "INV_STOCK_INSUFICIENTE";

    Result<SalidaVentaRegistrada, ApplicationError> registrarSalidaVenta(SalidaVentaSolicitud solicitud);
}
