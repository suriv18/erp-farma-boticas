package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.query.ListarOrdenesCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerOrdenCompraQuery;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface ConsultarOrdenesCompraUseCase {

    Result<OrdenCompraResult, ApplicationError> obtener(ObtenerOrdenCompraQuery query);

    Result<PaginaResult<OrdenCompraResumenResult>, ApplicationError> listar(ListarOrdenesCompraQuery query);
}
