package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarEmpresaOperadoraHandler implements ActualizarEmpresaOperadoraUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarEmpresaOperadoraHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(ActualizarEmpresaOperadoraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findEmpresaById(command.tenantId(), command.empresaId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND));
        }

        var updated = OrganizacionApplicationMapper.toDomain(existing.get()).updateDetails(
                command.razonSocial(), command.nombreComercial(), command.direccionFiscal(),
                command.ubigeoFiscal(), command.telefono(), command.email(), command.sitioWeb(),
                command.monedaFuncional(), command.zonaHoraria(), command.permiteVentaOnline(), clock.now());

        return updated.fold(this::persist, this::validationFailure);
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
