package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarMarcaCommand;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarMarcaHandler implements ActualizarMarcaUseCase {

    private final CatalogoComercialPort writePort;

    public ActualizarMarcaHandler(CatalogoComercialPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<MarcaResult, ApplicationError> execute(ActualizarMarcaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var marca = Marca.create(
                new MarcaId(command.marcaId()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.codigo(), command.nombre(), command.descripcion());
        return marca.fold(this::persist, this::validationFailure);
    }

    private Result<MarcaResult, ApplicationError> persist(Marca marca) {
        var outcome = writePort.save(marca);
        if (outcome == CatalogoComercialPort.SaveMarcaOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_MARCA_NO_ENCONTRADA", "La marca indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoComercialPort.SaveMarcaOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_MARCA_DUPLICADA", "Ya existe una marca con el código indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(marca));
    }

    private Result<MarcaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
