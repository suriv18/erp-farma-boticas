package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.query.ListarProveedoresQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerProveedorQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface ConsultarProveedoresUseCase {

    Result<ProveedorResult, ApplicationError> obtener(ObtenerProveedorQuery query);

    Result<PaginaResult<ProveedorResult>, ApplicationError> listar(ListarProveedoresQuery query);
}
