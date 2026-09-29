package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerEmpresaHandler implements ObtenerEmpresaUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerEmpresaHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(ObtenerEmpresaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findEmpresaById(query.tenantId(), query.empresaId())
                .<Result<EmpresaOperadoraResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
