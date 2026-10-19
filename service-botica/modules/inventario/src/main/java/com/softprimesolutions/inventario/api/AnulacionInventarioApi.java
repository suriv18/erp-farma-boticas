package com.softprimesolutions.inventario.api;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface AnulacionInventarioApi {

    String CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";
    String CODIGO_SALIDAS_NO_ENCONTRADAS = "INV_SALIDAS_NO_ENCONTRADAS";

    Result<ReintegroVentaRegistrado, ApplicationError> reintegrarSalidasDeVenta(ReintegroVentaSolicitud solicitud);
}
