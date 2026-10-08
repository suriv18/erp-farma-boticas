package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarCondicionesVentaUseCase {
    Result<List<CondicionVentaResult>, ApplicationError> execute(ListarCondicionesVentaQuery query);
}
