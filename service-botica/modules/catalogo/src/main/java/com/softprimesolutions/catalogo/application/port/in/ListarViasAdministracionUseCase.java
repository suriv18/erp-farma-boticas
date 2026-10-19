package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarViasAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarViasAdministracionUseCase {
    Result<List<ViaAdministracionResult>, ApplicationError> execute(ListarViasAdministracionQuery query);
}
