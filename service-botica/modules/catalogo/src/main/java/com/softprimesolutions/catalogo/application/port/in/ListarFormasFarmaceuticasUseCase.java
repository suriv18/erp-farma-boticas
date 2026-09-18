package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarFormasFarmaceuticasQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarFormasFarmaceuticasUseCase {
    Result<List<FormaFarmaceuticaResult>, ApplicationError> execute(ListarFormasFarmaceuticasQuery query);
}
