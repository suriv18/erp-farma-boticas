package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearMarcaCommand;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearMarcaHandler implements CrearMarcaUseCase {

    private final CatalogoComercialPort writePort;
    private final IdentifierGenerator identifierGenerator;

    public CrearMarcaHandler(CatalogoComercialPort writePort, IdentifierGenerator identifierGenerator) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
    }

    @Override
    public Result<MarcaResult, ApplicationError> execute(CrearMarcaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var marca = Marca.create(
                new MarcaId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.codigo(), command.nombre(), command.descripcion());
        return marca.fold(this::persist, this::validationFailure);
    }

    private Result<MarcaResult, ApplicationError> persist(Marca marca) {
        var outcome = writePort.save(marca);
        if (outcome == CatalogoComercialPort.SaveMarcaOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
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
