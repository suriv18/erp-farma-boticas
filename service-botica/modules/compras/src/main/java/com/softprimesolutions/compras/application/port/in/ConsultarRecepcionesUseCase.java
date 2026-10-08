package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface ConsultarRecepcionesUseCase {

    Result<RecepcionResult, ApplicationError> obtener(ObtenerRecepcionQuery query);

    Result<PaginaResult<RecepcionResult>, ApplicationError> listarPorOrden(ListarRecepcionesOrdenQuery query);
}
