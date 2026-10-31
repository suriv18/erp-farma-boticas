package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarUnidadesMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarUnidadesMedidaUseCase {
    Result<List<UnidadMedidaResult>, ApplicationError> execute(ListarUnidadesMedidaQuery query);
}
