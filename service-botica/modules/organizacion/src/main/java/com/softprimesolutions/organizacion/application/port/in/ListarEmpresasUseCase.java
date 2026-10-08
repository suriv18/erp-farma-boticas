package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarEmpresasUseCase {
    Result<PaginaResult<EmpresaOperadoraResult>, ApplicationError> execute(ListarEmpresasQuery query);
}
