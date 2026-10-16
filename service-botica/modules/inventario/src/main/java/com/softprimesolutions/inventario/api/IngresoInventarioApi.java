package com.softprimesolutions.inventario.api;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface IngresoInventarioApi {

    String CODIGO_CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";

    Result<IngresoCompraRegistrado, ApplicationError> registrarIngresoCompra(IngresoCompraSolicitud solicitud);
}
