package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CambiarEstadoEmpresaHandler implements CambiarEstadoEmpresaUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public CambiarEstadoEmpresaHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(CambiarEstadoEmpresaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var estado = EnumParser.parse(EstadoEmpresaOperadora.class, command.estado());
        if (estado.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_ESTADO_INVALIDO",
                    "El estado indicado no es válido. Valores permitidos: "
                            + EnumParser.allowedValues(EstadoEmpresaOperadora.class) + ".",
                    ErrorCategory.VALIDATION));
        }
        var existing = readPort.findEmpresaById(command.tenantId(), command.empresaId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND));
        }

        return OrganizacionApplicationMapper.toDomain(existing.get())
                .cambiarEstado(estado.get(), clock.now())
                .fold(this::persist, this::validationFailure);
    }

    private Result<EmpresaOperadoraResult, ApplicationError> persist(EmpresaOperadora empresa) {
        writePort.save(empresa);
        return Result.success(OrganizacionApplicationMapper.toResult(empresa));
    }

    private Result<EmpresaOperadoraResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
