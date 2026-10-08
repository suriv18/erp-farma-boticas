package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarPrincipioActivoUseCase {
    Result<List<PrincipioActivoResult>, ApplicationError> execute(ListarPrincipioActivoQuery query);
}
