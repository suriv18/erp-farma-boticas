package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarRubroComercialHandler implements ActualizarRubroComercialUseCase {

    private final RubroComercialPort writePort;

    public ActualizarRubroComercialHandler(RubroComercialPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<RubroComercialResult, ApplicationError> execute(ActualizarRubroComercialCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var rubro = RubroComercial.create(
                new RubroComercialId(command.rubroComercialId()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.codigo(), command.nombre(), command.descripcion(), command.esFarmaceutico(),
                command.orden());
        return rubro.fold(this::persist, this::validationFailure);
    }

    private Result<RubroComercialResult, ApplicationError> persist(RubroComercial rubro) {
        var outcome = writePort.save(rubro);
        if (outcome == RubroComercialPort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", "El rubro comercial indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == RubroComercialPort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_RUBRO_COMERCIAL_DUPLICADO", "Ya existe un rubro comercial con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(rubro));
    }

    private Result<RubroComercialResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
