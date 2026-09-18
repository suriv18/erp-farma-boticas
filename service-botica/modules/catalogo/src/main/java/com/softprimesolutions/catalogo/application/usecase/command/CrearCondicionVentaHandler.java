package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearCondicionVentaHandler implements CrearCondicionVentaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearCondicionVentaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<CondicionVentaResult, ApplicationError> execute(CrearCondicionVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var condicionVenta = CondicionVenta.create(
                command.codigo(), command.denominacion(), command.requiereReceta(), command.requiereRetencion(),
                command.fuente(), command.versionFuente(), command.vigenteDesde(), command.vigenteHasta());
        return condicionVenta.fold(this::persist, this::validationFailure);
    }

    private Result<CondicionVentaResult, ApplicationError> persist(CondicionVenta condicionVenta) {
        var outcome = writePort.save(condicionVenta);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CONDICION_VENTA_DUPLICADA", "Ya existe una condición de venta con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(condicionVenta));
    }

    private Result<CondicionVentaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
